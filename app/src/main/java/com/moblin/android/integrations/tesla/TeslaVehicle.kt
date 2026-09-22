package com.moblin.android.integrations.tesla

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.media.haishinkit.util.ByteWriter
import com.moblin.android.various.MainTimer
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val TAG = "TeslaVehicle"

private val vehicleServiceUuid: UUID = UUID.fromString("00000211-b2d1-43f0-9b88-960cebf8b91e")
private val toVehicleUuid: UUID = UUID.fromString("00000212-b2d1-43f0-9b88-960cebf8b91e")
private val fromVehicleUuid: UUID = UUID.fromString("00000213-b2d1-43f0-9b88-960cebf8b91e")
private val clientCharacteristicConfigUuid: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

private val ecPublicKeyDerHeader = byteArrayOf(
    0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2A, 0x86.toByte(),
    0x48, 0xCE.toByte(), 0x3D, 0x02, 0x01, 0x06, 0x08, 0x2A,
    0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x03, 0x01, 0x07, 0x03,
    0x42, 0x00,
)

private val secureRandom = SecureRandom()

private fun randomData(length: Int): ByteArray {
    val data = ByteArray(length)
    secureRandom.nextBytes(data)
    return data
}

private fun ByteArray.toHexString(): String {
    val builder = StringBuilder(size * 2)
    for (byte in this) {
        val value = byte.toInt() and 0xFF
        builder.append(value.toString(16).padStart(2, '0'))
    }
    return builder.toString()
}

private fun decodePem(pem: String): ByteArray {
    val body = pem.lines()
        .filter { !it.startsWith("-----") && it.isNotBlank() }
        .joinToString("")
    return Base64.getDecoder().decode(body)
}

private fun privateKeyFromPem(pem: String): PrivateKey {
    return KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(decodePem(pem)))
}

private fun derContent(bytes: ByteArray, index: Int): IntRange {
    val first = bytes[index].toInt() and 0xFF
    var i = index + 1
    val length = if (first and 0x80 == 0) {
        first
    } else {
        var value = 0
        repeat(first and 0x7F) {
            value = (value shl 8) or (bytes[i].toInt() and 0xFF)
            i += 1
        }
        value
    }
    return i until i + length
}

private fun derElementEnd(bytes: ByteArray, index: Int): Int = derContent(bytes, index).last + 1

private fun ecPublicKeyPointFromDer(der: ByteArray): ByteArray {
    val pkcs8 = derContent(der, 0)
    var index = derElementEnd(der, pkcs8.first)
    index = derElementEnd(der, index)
    val octet = derContent(der, index)
    val inner = der.copyOfRange(octet.first, octet.last + 1)
    val ecPrivateKey = derContent(inner, 0)
    var i = derElementEnd(inner, ecPrivateKey.first)
    i = derElementEnd(inner, i)
    while (i <= ecPrivateKey.last) {
        val tag = inner[i].toInt() and 0xFF
        val content = derContent(inner, i)
        if (tag == 0xA1) {
            val bitString = derContent(inner, content.first)
            val value = inner.copyOfRange(bitString.first, bitString.last + 1)
            return value.copyOfRange(1, value.size)
        }
        i = content.last + 1
    }
    throw IllegalStateException("Vehicle public key missing from private key")
}

private fun ecPublicKeyBytesFromPem(pem: String): ByteArray {
    return ecPublicKeyPointFromDer(decodePem(pem))
}

private fun createSymmetricKey(clientPrivateKey: PrivateKey, vehiclePublicKey: ByteArray): SecretKey {
    val publicKey = ecPublicKeyFromBytes(vehiclePublicKey)
    val keyAgreement = KeyAgreement.getInstance("ECDH")
    keyAgreement.init(clientPrivateKey)
    keyAgreement.doPhase(publicKey, true)
    val sharedData = keyAgreement.generateSecret()
    val sharedSecret = MessageDigest.getInstance("SHA-1").digest(sharedData).copyOf(16)
    return SecretKeySpec(sharedSecret, "AES")
}

fun ecPublicKeyFromBytes(bytes: ByteArray): ECPublicKey {
    return KeyFactory.getInstance("EC")
        .generatePublic(X509EncodedKeySpec(ecPublicKeyDerHeader + bytes)) as ECPublicKey
}

fun ECPublicKey.toBytes(): ByteArray {
    val x = w.affineX.toFixedBytes(32)
    val y = w.affineY.toFixedBytes(32)
    return byteArrayOf(0x04) + x + y
}

private fun BigInteger.toFixedBytes(size: Int): ByteArray {
    val bytes = toByteArray()
    return when {
        bytes.size == size -> bytes
        bytes.size > size -> bytes.copyOfRange(bytes.size - size, bytes.size)
        else -> ByteArray(size - bytes.size) + bytes
    }
}

enum class TeslaVehicleState {
    idle,
    connecting,
    connected,
}

private data class Job(
    val address: ByteArray,
    val playload: ByteArray,
    val onCompleted: (UniversalMessage_RoutableMessage, UniversalMessage_RoutableMessage) -> Unit,
)

private class VehicleDomain(private val clientPrivateKey: PrivateKey) {
    private var sessionInfo: Signatures_SessionInfo? = null
    private var localClock: Long = System.nanoTime()
    private val jobs: ArrayDeque<Job> = ArrayDeque()
    private var symmetricKey: SecretKey? = null

    fun updateSesionInfo(sessionInfo: Signatures_SessionInfo) {
        symmetricKey = createSymmetricKey(clientPrivateKey, sessionInfo.publicKey)
        this.sessionInfo = sessionInfo
        localClock = System.nanoTime()
    }

    fun getSymmetricKey(): SecretKey {
        return symmetricKey ?: throw IllegalStateException("Symmetric key missing")
    }

    fun appendJob(
        address: ByteArray,
        payload: ByteArray,
        onCompleted: (UniversalMessage_RoutableMessage, UniversalMessage_RoutableMessage) -> Unit,
    ) {
        jobs.addLast(Job(address, payload, onCompleted))
    }

    fun removeJobs() {
        jobs.clear()
    }

    fun tryGetNextJob(): Job? {
        if (!hasSessionInfo()) {
            return null
        }
        return jobs.removeFirstOrNull()
    }

    fun hasSessionInfo(): Boolean {
        return sessionInfo != null
    }

    fun epoch(): ByteArray {
        return sessionInfo?.epoch ?: ByteArray(0)
    }

    fun nextCounter(): Int {
        val info = sessionInfo ?: return 0
        info.counter = info.counter + 1
        return info.counter
    }

    fun expiresAt(): Int {
        val clockTime = sessionInfo?.clockTime ?: 0
        val elapsed = ((System.nanoTime() - localClock) / 1_000_000_000L).toInt()
        return clockTime + elapsed + 15
    }
}

fun teslaGeneratePrivateKey(): PrivateKey {
    val generator = KeyPairGenerator.getInstance("EC")
    generator.initialize(ECGenParameterSpec("secp256r1"))
    return generator.generateKeyPair().private
}

interface TeslaVehicleDelegate {
    fun teslaVehicleState(vehicle: TeslaVehicle, state: TeslaVehicleState)
    fun teslaVehicleVehicleSecurityConnected(vehicle: TeslaVehicle)
    fun teslaVehicleInfotainmentConnected(vehicle: TeslaVehicle)
}

class TeslaVehicle private constructor(
    private val vin: String,
    private val peripheralId: UUID,
    private val clientPrivateKey: PrivateKey,
    private val clientPublicKeyBytes: ByteArray,
) : BluetoothGattCallback() {
    private var centralManager: BluetoothAdapter? = null
    private var vehiclePeripheral: BluetoothDevice? = null
    private var vehicleGatt: BluetoothGatt? = null
    private var toVehicleCharacteristic: BluetoothGattCharacteristic? = null
    private var fromVehicleCharacteristic: BluetoothGattCharacteristic? = null
    private var state: TeslaVehicleState = TeslaVehicleState.idle
    private val responseHandlers: MutableMap<String, (UniversalMessage_RoutableMessage) -> Unit> = mutableMapOf()
    private var receiveBuffer = ByteArray(0)
    private val vehicleDomains: MutableMap<UniversalMessage_Domain, VehicleDomain> = mutableMapOf()
    var delegate: TeslaVehicleDelegate? = null
    private val vehicleSecurityHandshakeTimer = MainTimer()
    private val infotainmentHandshakeTimer = MainTimer()
    private val pendingWriteBlocks: ArrayDeque<ByteArray> = ArrayDeque()

    companion object {
        var applicationContext: Context? = null

        operator fun invoke(
            vin: String,
            privateKeyPem: String,
            peripheralId: UUID,
            handshake: Boolean = true,
        ): TeslaVehicle? {
            val clientPrivateKey = runCatching { privateKeyFromPem(privateKeyPem) }.getOrElse {
                Log.i(TAG, "tesla-vehicle: Error $it")
                return null
            }
            val clientPublicKeyBytes = runCatching { ecPublicKeyBytesFromPem(privateKeyPem) }.getOrElse {
                Log.i(TAG, "tesla-vehicle: Error $it")
                return null
            }
            return TeslaVehicle(vin, peripheralId, clientPrivateKey, clientPublicKeyBytes)
        }
    }

    fun start() {
        reset()
        centralManager = BluetoothAdapter.getDefaultAdapter()
        startSession()
        centralManagerDidUpdateState(centralManager)
    }

    fun stop() {
        reset()
    }

    private fun reset() {
        resetSession()
        vehicleGatt?.close()
        vehicleGatt = null
        centralManager = null
        vehiclePeripheral = null
        setState(TeslaVehicleState.idle)
    }

    private fun startSession() {
        vehicleDomains[UniversalMessage_Domain.vehicleSecurity] = VehicleDomain(clientPrivateKey)
        vehicleDomains[UniversalMessage_Domain.infotainment] = VehicleDomain(clientPrivateKey)
    }

    private fun resetSession() {
        vehicleSecurityHandshakeTimer.stop()
        infotainmentHandshakeTimer.stop()
        toVehicleCharacteristic = null
        fromVehicleCharacteristic = null
        responseHandlers.clear()
        receiveBuffer = ByteArray(0)
        vehicleDomains.clear()
        pendingWriteBlocks.clear()
    }

    fun centralManagerDidUpdateState(central: BluetoothAdapter?) {
        if (central?.isEnabled == true) {
            connect(central)
        }
    }

    private fun connect(central: BluetoothAdapter) {
        val context = applicationContext
        if (context == null) {
            Log.i(TAG, "tesla-vehicle: Vehicle not found")
            return
        }
        val peripheral = runCatching { central.getRemoteDevice(peripheralId.toString()) }.getOrNull()
        if (peripheral == null) {
            Log.i(TAG, "tesla-vehicle: Vehicle not found")
            return
        }
        vehiclePeripheral = peripheral
        vehicleGatt = peripheral.connectGatt(context, true, this)
        setState(TeslaVehicleState.connecting)
    }

    fun addKeyRequestWithRole(privateKeyPem: String) {
        runCatching {
            val clientPublicKeyBytes = ecPublicKeyBytesFromPem(privateKeyPem)
            val message = VCSEC_UnsignedMessage()
            message.whitelistOperation.addKeyToWhitelistAndAddPermissions.key.publicKeyRaw = clientPublicKeyBytes
            message.whitelistOperation.addKeyToWhitelistAndAddPermissions.keyRole = VCSEC_KeyRole.owner
            message.whitelistOperation.metadataForKey.keyFormFactor = VCSEC_KeyFormFactor.cloudKey
            val encoded = message.toByteArray()
            val envelope = VCSEC_ToVCSECMessage()
            envelope.signedMessage.protobufMessageAsBytes = encoded
            envelope.signedMessage.signatureType = VCSEC_SignatureType.presentKey
            sendData(envelope.toByteArray())
        }.onFailure {
            Log.i(TAG, "tesla-vehicle: Add key error $it")
        }
    }

    fun openTrunk() {
        val closureMoveRequest = VCSEC_ClosureMoveRequest()
        closureMoveRequest.rearTrunk = VCSEC_ClosureMoveType.closureMoveTypeOpen
        executeClosureMoveAction(closureMoveRequest) {
            Log.i(TAG, "tesla-vehicle: Open trunk response")
        }
    }

    fun closeTrunk() {
        val closureMoveRequest = VCSEC_ClosureMoveRequest()
        closureMoveRequest.rearTrunk = VCSEC_ClosureMoveType.closureMoveTypeClose
        executeClosureMoveAction(closureMoveRequest) {
            Log.i(TAG, "tesla-vehicle: Close trunk response")
        }
    }

    fun honk() {
        val action = CarServer_Action()
        action.vehicleAction.vehicleControlHonkHornAction = CarServer_VehicleAction_VehicleControlHonkHornAction()
        executeCarServerAction(action) { _ ->
            Log.d(TAG, "tesla-vehicle: Honk response")
        }
    }

    fun flashLights() {
        val action = CarServer_Action()
        action.vehicleAction.vehicleControlFlashLightsAction = CarServer_VehicleAction_VehicleControlFlashLightsAction()
        executeCarServerAction(action) { _ ->
            Log.d(TAG, "tesla-vehicle: Flash lights response")
        }
    }

    fun mediaNextTrack() {
        val action = CarServer_Action()
        action.vehicleAction.mediaNextTrack = CarServer_VehicleAction_MediaNextTrack()
        executeCarServerAction(action) { _ ->
            Log.d(TAG, "tesla-vehicle: Media next track response")
        }
    }

    fun mediaPreviousTrack() {
        val action = CarServer_Action()
        action.vehicleAction.mediaPreviousTrack = CarServer_VehicleAction_MediaPreviousTrack()
        executeCarServerAction(action) { _ ->
            Log.d(TAG, "tesla-vehicle: Media previous track response")
        }
    }

    fun mediaTogglePlayback() {
        val action = CarServer_Action()
        action.vehicleAction.mediaPlayAction = CarServer_VehicleAction_MediaPlayAction()
        executeCarServerAction(action) { _ ->
            Log.d(TAG, "tesla-vehicle: Media toggle playback response")
        }
    }

    fun getChargeState(onCompleted: (CarServer_ChargeState) -> Unit) {
        val action = CarServer_Action()
        action.vehicleAction.getVehicleData.getChargeState = CarServer_VehicleAction_GetVehicleData_GetChargeState()
        executeCarServerAction(action) { response ->
            onCompleted(response.vehicleData.chargeState)
        }
    }

    fun getDriveState(onCompleted: (CarServer_DriveState) -> Unit) {
        val action = CarServer_Action()
        action.vehicleAction.getVehicleData.getDriveState = CarServer_VehicleAction_GetVehicleData_GetDriveState()
        executeCarServerAction(action) { response ->
            onCompleted(response.vehicleData.driveState)
        }
    }

    fun getMediaState(onCompleted: (CarServer_MediaState) -> Unit) {
        val action = CarServer_Action()
        action.vehicleAction.getVehicleData.getMediaState = CarServer_VehicleAction_GetVehicleData_GetMediaState()
        executeCarServerAction(action) { response ->
            onCompleted(response.vehicleData.mediaState)
        }
    }

    private fun executeClosureMoveAction(
        closureMoveRequest: VCSEC_ClosureMoveRequest,
        onCompleted: () -> Unit,
    ) {
        if (state != TeslaVehicleState.connected) {
            return
        }
        val vehicleDomain = vehicleDomains[UniversalMessage_Domain.vehicleSecurity] ?: return
        runCatching {
            val unsignedMessage = VCSEC_UnsignedMessage()
            unsignedMessage.closureMoveRequest = closureMoveRequest
            val payload = unsignedMessage.toByteArray()
            vehicleDomain.appendJob(getNextAddress(), payload) { _, _ ->
                onCompleted()
            }
            trySendNextJob(UniversalMessage_Domain.vehicleSecurity)
        }.onFailure {
            Log.i(TAG, "tesla-vehicle: Execute closure move action error $it")
        }
    }

    private fun executeCarServerAction(
        action: CarServer_Action,
        onCompleted: (CarServer_Response) -> Unit,
    ) {
        if (state != TeslaVehicleState.connected) {
            return
        }
        val vehicleDomain = vehicleDomains[UniversalMessage_Domain.infotainment] ?: return
        runCatching {
            val payload = action.toByteArray()
            vehicleDomain.appendJob(getNextAddress(), payload) { request, response ->
                val aesGcmResponseData = response.signatureData.aesGcmResponseData
                val metadataHash = createResponseMetadata(request, response)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    vehicleDomain.getSymmetricKey(),
                    GCMParameterSpec(128, aesGcmResponseData.nonce),
                )
                cipher.updateAAD(metadataHash)
                val decoded = cipher.doFinal(response.protobufMessageAsBytes + aesGcmResponseData.tag)
                val carServerResponse = CarServer_Response.parseFrom(decoded)
                if (carServerResponse.actionStatus.result != CarServer_OperationStatus_E.operationstatusOk) {
                    Log.i(TAG, "tesla-vehicle: Car server response status not ok")
                }
                onCompleted(carServerResponse)
            }
            trySendNextJob(UniversalMessage_Domain.infotainment)
        }.onFailure {
            Log.i(TAG, "tesla-vehicle: Execute car server action error $it")
        }
    }

    private fun setState(state: TeslaVehicleState) {
        if (state == this.state) {
            return
        }
        Log.i(TAG, "tesla-vehicle: State change ${this.state} -> $state")
        this.state = state
        delegate?.teslaVehicleState(this, state)
    }

    private fun getNextAddress(): ByteArray {
        return randomData(16)
    }

    private fun startVehicleSecurityHandshake() {
        sendSessionInfoRequest(UniversalMessage_Domain.vehicleSecurity)
        vehicleSecurityHandshakeTimer.startSingleShot(10.0) {
            runCatching {
                startVehicleSecurityHandshake()
            }.onFailure {
                Log.i(TAG, "tesla-vehicle: Failed to start vehicle security handshake with error $it")
            }
        }
    }

    private fun startInfotainmentHandshake() {
        sendSessionInfoRequest(UniversalMessage_Domain.infotainment)
        infotainmentHandshakeTimer.startSingleShot(10.0) {
            runCatching {
                startInfotainmentHandshake()
            }.onFailure {
                Log.i(TAG, "tesla-vehicle: Failed to start infotainment handshake with error $it")
            }
        }
    }

    private fun sendSessionInfoRequest(domain: UniversalMessage_Domain) {
        val address = getNextAddress()
        val uuid = randomData(16)
        val message = UniversalMessage_RoutableMessage()
        message.toDestination.domain = domain
        message.fromDestination.routingAddress = address
        message.sessionInfoRequest.publicKey = clientPublicKeyBytes
        message.uuid = uuid
        responseHandlers[address.toHexString()] = { response ->
            runCatching {
                handleSessionInfoResponse(message, response)
            }.onFailure {
                Log.d(TAG, "tesla-vehicle: Session info failed with $it")
            }
        }
        sendMessage(message)
    }

    private fun handleSessionInfoResponse(
        request: UniversalMessage_RoutableMessage,
        response: UniversalMessage_RoutableMessage,
    ) {
        val domain = response.fromDestination.domain
        val sessionInfo = Signatures_SessionInfo.parseFrom(response.sessionInfo)
        val vehicleDomain = vehicleDomains[domain] ?: return
        vehicleDomain.updateSesionInfo(sessionInfo)
        vehicleDomain.removeJobs()
        when (domain) {
            UniversalMessage_Domain.vehicleSecurity -> {
                vehicleSecurityHandshakeTimer.stop()
                delegate?.teslaVehicleVehicleSecurityConnected(this)
                startInfotainmentHandshake()
            }
            UniversalMessage_Domain.infotainment -> {
                infotainmentHandshakeTimer.stop()
                delegate?.teslaVehicleInfotainmentConnected(this)
            }
            else -> Unit
        }
    }

    private fun startJob(domain: UniversalMessage_Domain, job: Job) {
        val request = UniversalMessage_RoutableMessage()
        request.toDestination.domain = domain
        request.fromDestination.routingAddress = job.address
        request.uuid = randomData(16)
        request.flags = 1 shl UniversalMessage_Flags.flagEncryptResponse.number
        sign(request, job.playload)
        responseHandlers[job.address.toHexString()] = { response ->
            handleJobResponse(job, request, response)
        }
        sendMessage(request)
    }

    private fun handleJobResponse(
        job: Job,
        request: UniversalMessage_RoutableMessage,
        response: UniversalMessage_RoutableMessage,
    ) {
        job.onCompleted(request, response)
        if (response.signedMessageStatus.signedMessageFault != Signatures_SignedMessageFault.rrorNone) {
            throw IllegalStateException("Request was not successful. Response ${response.toString()}")
        }
    }

    private fun trySendNextJob(domain: UniversalMessage_Domain) {
        while (true) {
            val job = vehicleDomains[domain]?.tryGetNextJob() ?: break
            startJob(domain, job)
        }
    }

    private fun handleData(data: ByteArray) {
        receiveBuffer += data
        val reader = ByteReader(receiveBuffer)
        val size = reader.readUInt16().toInt()
        if (reader.bytesAvailable < size) {
            return
        }
        val payload = reader.readBytes(size)
        if (reader.bytesAvailable > 0) {
            receiveBuffer = reader.readBytes(reader.bytesAvailable)
        } else {
            receiveBuffer = ByteArray(0)
        }
        val message = UniversalMessage_RoutableMessage.parseFrom(payload)
        if (message.toDestination.subDestinationCase != UniversalMessage_Destination.SubDestinationCase.ROUTING_ADDRESS) {
            return
        }
        val address = message.toDestination.routingAddress
        val responseHandler = responseHandlers.remove(address.toHexString()) ?: return
        responseHandler(message)
    }

    private fun sendMessage(message: UniversalMessage_RoutableMessage) {
        sendData(message.toByteArray())
    }

    private fun sendData(message: ByteArray) {
        val writer = ByteWriter()
        writer.writeUInt16(message.size.toUShort())
        writer.writeBytes(message)
        val data = writer.data
        val blockLength = 20
        var offset = 0
        while (offset < data.size) {
            pendingWriteBlocks.addLast(data.copyOfRange(offset, minOf(offset + blockLength, data.size)))
            offset += blockLength
        }
        writeNextBlock()
    }

    private fun writeNextBlock() {
        val gatt = vehicleGatt ?: return
        val characteristic = toVehicleCharacteristic ?: return
        val block = pendingWriteBlocks.removeFirstOrNull() ?: return
        characteristic.value = block
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        gatt.writeCharacteristic(characteristic)
    }

    private fun sign(message: UniversalMessage_RoutableMessage, payload: ByteArray) {
        val vehicleDomain = vehicleDomains[message.toDestination.domain]
            ?: throw IllegalStateException("Cannot sign for missing vehicle domain")
        message.signatureData.signerIdentity.publicKey = clientPublicKeyBytes
        message.signatureData.aesGcmPersonalizedData.epoch = vehicleDomain.epoch()
        message.signatureData.aesGcmPersonalizedData.counter = vehicleDomain.nextCounter()
        message.signatureData.aesGcmPersonalizedData.expiresAt = vehicleDomain.expiresAt()
        val key = vehicleDomain.getSymmetricKey()
        val metadataHash = createRequestMetadata(message)
        val nonce = randomData(12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
        cipher.updateAAD(metadataHash)
        val encrypted = cipher.doFinal(payload)
        message.signatureData.aesGcmPersonalizedData.nonce = nonce
        message.signatureData.aesGcmPersonalizedData.tag = encrypted.copyOfRange(encrypted.size - 16, encrypted.size)
        message.protobufMessageAsBytes = encrypted.copyOfRange(0, encrypted.size - 16)
    }

    private fun createRequestMetadata(message: UniversalMessage_RoutableMessage): ByteArray {
        val metadata = Metadata()
        metadata.addUInt8(Signatures_Tag.signatureType, Signatures_SignatureType.aesGcmPersonalized.number)
        metadata.addUInt8(Signatures_Tag.domain, message.toDestination.domain.number)
        metadata.add(Signatures_Tag.personalization, vin.toByteArray(Charsets.UTF_8))
        metadata.add(Signatures_Tag.epoch, message.signatureData.aesGcmPersonalizedData.epoch)
        metadata.addUInt32(Signatures_Tag.expiresAt, message.signatureData.aesGcmPersonalizedData.expiresAt)
        metadata.addUInt32(Signatures_Tag.counter, message.signatureData.aesGcmPersonalizedData.counter)
        metadata.addUInt32(Signatures_Tag.flags, message.flags)
        return metadata.finalize(ByteArray(0))
    }

    private fun createResponseMetadata(
        request: UniversalMessage_RoutableMessage,
        response: UniversalMessage_RoutableMessage,
    ): ByteArray {
        val metadata = Metadata()
        metadata.addUInt8(Signatures_Tag.signatureType, Signatures_SignatureType.aesGcmResponse.number)
        metadata.addUInt8(Signatures_Tag.domain, response.fromDestination.domain.number)
        metadata.add(Signatures_Tag.personalization, vin.toByteArray(Charsets.UTF_8))
        metadata.addUInt32(Signatures_Tag.counter, response.signatureData.aesGcmResponseData.counter)
        metadata.addUInt32(Signatures_Tag.flags, response.flags)
        val requestId = byteArrayOf(Signatures_SignatureType.aesGcmPersonalized.number.toByte()) +
            request.signatureData.aesGcmPersonalizedData.tag
        metadata.add(Signatures_Tag.requestHash, requestId)
        metadata.addUInt32(Signatures_Tag.fault, response.signedMessageStatus.signedMessageFault.number)
        return metadata.finalize(ByteArray(0))
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "tesla-vehicle: Connect failure")
            reset()
            return
        }
        when (newState) {
            BluetoothProfile.STATE_CONNECTED -> {
                Log.d(TAG, "tesla-vehicle: Connected")
                vehicleGatt = gatt
                vehiclePeripheral = gatt.device
                gatt.discoverServices()
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
                Log.d(TAG, "tesla-vehicle: Disconnected (reconnecting: false)")
                reset()
            }
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        val service = if (status == BluetoothGatt.GATT_SUCCESS) gatt.getService(vehicleServiceUuid) else null
        if (service == null) {
            Log.i(TAG, "tesla-vehicle: No services found")
            return
        }
        for (characteristic in service.characteristics) {
            if (characteristic.uuid == toVehicleUuid) {
                toVehicleCharacteristic = characteristic
            } else if (characteristic.uuid == fromVehicleUuid) {
                fromVehicleCharacteristic = characteristic
                gatt.setCharacteristicNotification(characteristic, true)
                val descriptor = characteristic.getDescriptor(clientCharacteristicConfigUuid)
                if (descriptor != null) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                }
            }
        }
        setState(TeslaVehicleState.connected)
        runCatching {
            startVehicleSecurityHandshake()
        }.onFailure {
            Log.i(TAG, "tesla-vehicle: Failed to start handshake $it")
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val value = characteristic.value ?: return
        runCatching {
            handleData(value)
        }.onFailure {
            Log.i(TAG, "tesla-vehicle: Message handling error $it")
            reset()
        }
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        writeNextBlock()
    }
}

private class Metadata {
    private val writer = ByteWriter()
    private var lastTag: Signatures_Tag? = null

    fun add(tag: Signatures_Tag, value: ByteArray) {
        val previous = lastTag
        if (previous != null && tag.number <= previous.number) {
            throw IllegalStateException("Must be added in increasing tags order")
        }
        if (value.size > 255) {
            throw IllegalStateException("Metadata value too long ${value.size}")
        }
        lastTag = tag
        writer.writeUInt8(tag.number.toUByte())
        writer.writeUInt8(value.size.toUByte())
        writer.writeBytes(value)
    }

    fun addUInt8(tag: Signatures_Tag, value: Int) {
        add(tag, byteArrayOf(value.toByte()))
    }

    fun addUInt32(tag: Signatures_Tag, value: Int) {
        add(
            tag,
            byteArrayOf(
                (value ushr 24).toByte(),
                (value ushr 16).toByte(),
                (value ushr 8).toByte(),
                value.toByte(),
            ),
        )
    }

    fun finalize(message: ByteArray): ByteArray {
        writer.writeUInt8(Signatures_Tag.end.number.toUByte())
        writer.writeBytes(message)
        return MessageDigest.getInstance("SHA-256").digest(writer.data)
    }
}
