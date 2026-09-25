package com.moblin.android.integrations.tesla

import android.util.Log
import com.moblin.android.common.various.hexString
import com.moblin.android.common.various.setUInt32Be
import com.moblin.android.common.various.utf8Data
import com.moblin.android.integrations.tesla.protobuf.CarServer_Action
import com.moblin.android.integrations.tesla.protobuf.CarServer_ChargeState
import com.moblin.android.integrations.tesla.protobuf.CarServer_DriveState
import com.moblin.android.integrations.tesla.protobuf.CarServer_GetChargeState
import com.moblin.android.integrations.tesla.protobuf.CarServer_GetDriveState
import com.moblin.android.integrations.tesla.protobuf.CarServer_GetMediaState
import com.moblin.android.integrations.tesla.protobuf.CarServer_MediaNextTrack
import com.moblin.android.integrations.tesla.protobuf.CarServer_MediaPlayAction
import com.moblin.android.integrations.tesla.protobuf.CarServer_MediaPreviousTrack
import com.moblin.android.integrations.tesla.protobuf.CarServer_MediaState
import com.moblin.android.integrations.tesla.protobuf.CarServer_OperationStatus_E
import com.moblin.android.integrations.tesla.protobuf.CarServer_Response
import com.moblin.android.integrations.tesla.protobuf.CarServer_VehicleControlFlashLightsAction
import com.moblin.android.integrations.tesla.protobuf.CarServer_VehicleControlHonkHornAction
import com.moblin.android.integrations.tesla.protobuf.Keys_Role
import com.moblin.android.integrations.tesla.protobuf.Signatures_SessionInfo
import com.moblin.android.integrations.tesla.protobuf.Signatures_SignatureType
import com.moblin.android.integrations.tesla.protobuf.Signatures_Tag
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_Destination
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_Domain
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_Flags
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_MessageFault_E
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_RoutableMessage
import com.moblin.android.integrations.tesla.protobuf.VCSEC_ClosureMoveRequest
import com.moblin.android.integrations.tesla.protobuf.VCSEC_ClosureMoveType_E
import com.moblin.android.integrations.tesla.protobuf.VCSEC_KeyFormFactor
import com.moblin.android.integrations.tesla.protobuf.VCSEC_SignatureType
import com.moblin.android.integrations.tesla.protobuf.VCSEC_ToVCSECMessage
import com.moblin.android.integrations.tesla.protobuf.VCSEC_UnsignedMessage
import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.media.haishinkit.util.ByteWriter
import com.moblin.android.platform.core.ContinuousClock
import com.moblin.android.platform.corebluetooth.CBCentralManager
import com.moblin.android.platform.corebluetooth.CBCentralManagerDelegate
import com.moblin.android.platform.corebluetooth.CBCharacteristic
import com.moblin.android.platform.corebluetooth.CBCharacteristicWriteType
import com.moblin.android.platform.corebluetooth.CBConnectPeripheralOptionEnableAutoReconnect
import com.moblin.android.platform.corebluetooth.CBManagerState
import com.moblin.android.platform.corebluetooth.CBPeripheral
import com.moblin.android.platform.corebluetooth.CBPeripheralDelegate
import com.moblin.android.platform.corebluetooth.CBService
import com.moblin.android.platform.corebluetooth.CBUUID
import com.moblin.android.platform.cryptokit.AES
import com.moblin.android.platform.cryptokit.Insecure
import com.moblin.android.platform.cryptokit.P256
import com.moblin.android.platform.cryptokit.SHA256
import com.moblin.android.platform.cryptokit.SymmetricKey
import com.moblin.android.platform.swiftprotobuf.serializedData
import com.moblin.android.various.MainTimer
import com.moblin.android.various.utils.randomBytes
import java.util.UUID
import kotlin.time.DurationUnit

private const val TAG = "TeslaVehicle"

private val vehicleServiceUuid = CBUUID(string = "00000211-b2d1-43f0-9b88-960cebf8b91e")
private val toVehicleUuid = CBUUID(string = "00000212-b2d1-43f0-9b88-960cebf8b91e")
private val fromVehicleUuid = CBUUID(string = "00000213-b2d1-43f0-9b88-960cebf8b91e")

enum class TeslaVehicleState {
    idle,
    connecting,
    connected,
}

private fun createSymmetricKey(
    clientPrivateKey: P256.KeyAgreement.PrivateKey,
    vehiclePublicKey: ByteArray,
): SymmetricKey {
    val publicKey = P256.KeyAgreement.PublicKey(bytes = vehiclePublicKey)
    val shared = clientPrivateKey.sharedSecretFromKeyAgreement(with = publicKey)
    val sharedData = shared.withUnsafeBytes { buffer -> buffer.copyOf() }
    val sharedSecret = Insecure.SHA1.hash(data = sharedData).prefix(16)
    return SymmetricKey(data = sharedSecret)
}

operator fun P256.KeyAgreement.PublicKey.Companion.invoke(bytes: ByteArray): P256.KeyAgreement.PublicKey {
    val derStuff = byteArrayOf(
        0x30.toByte(), 0x59.toByte(), 0x30.toByte(), 0x13.toByte(), 0x06.toByte(), 0x07.toByte(), 0x2A.toByte(), 0x86.toByte(),
        0x48.toByte(), 0xCE.toByte(), 0x3D.toByte(), 0x02.toByte(), 0x01.toByte(), 0x06.toByte(), 0x08.toByte(), 0x2A.toByte(),
        0x86.toByte(), 0x48.toByte(), 0xCE.toByte(), 0x3D.toByte(), 0x03.toByte(), 0x01.toByte(), 0x07.toByte(), 0x03.toByte(),
        0x42.toByte(), 0x00.toByte(),
    )
    return P256.KeyAgreement.PublicKey(derRepresentation = derStuff + bytes)
}

fun P256.KeyAgreement.PublicKey.toBytes(): ByteArray = derRepresentation.copyOfRange(26, derRepresentation.size)

private class Job(
    val address: ByteArray,
    val playload: ByteArray,
    val onCompleted: (UniversalMessage_RoutableMessage, UniversalMessage_RoutableMessage) -> Unit,
)

private class VehicleDomain(private val clientPrivateKey: P256.KeyAgreement.PrivateKey) {
    private var sessionInfo: Signatures_SessionInfo? = null
    private var localClock: ContinuousClock.Instant = ContinuousClock.now
    private var jobs = ArrayDeque<Job>()
    private var symmetricKey: SymmetricKey? = null

    fun updateSesionInfo(sessionInfo: Signatures_SessionInfo) {
        symmetricKey = createSymmetricKey(
            clientPrivateKey = clientPrivateKey,
            vehiclePublicKey = sessionInfo.publicKey,
        )
        this.sessionInfo = sessionInfo
        localClock = ContinuousClock.now
    }

    fun getSymmetricKey(): SymmetricKey {
        return symmetricKey ?: throw Exception("Symmetric key missing")
    }

    fun appendJob(
        address: ByteArray,
        payload: ByteArray,
        onCompleted: (UniversalMessage_RoutableMessage, UniversalMessage_RoutableMessage) -> Unit,
    ) {
        jobs.addLast(Job(address = address, playload = payload, onCompleted = onCompleted))
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

    fun hasSessionInfo(): Boolean = sessionInfo != null

    fun epoch(): ByteArray = sessionInfo?.epoch ?: ByteArray(0)

    fun nextCounter(): UInt {
        sessionInfo?.let { it.counter += 1u }
        return sessionInfo?.counter ?: 0u
    }

    fun expiresAt(): UInt {
        val clockTime = sessionInfo?.clockTime ?: 0u
        val elapsed = localClock.duration(to = ContinuousClock.now).toDouble(DurationUnit.SECONDS).toUInt()
        return clockTime + elapsed + 15u
    }
}

fun teslaGeneratePrivateKey(): P256.KeyAgreement.PrivateKey = P256.KeyAgreement.PrivateKey()

interface TeslaVehicleDelegate {
    fun teslaVehicleState(vehicle: TeslaVehicle, state: TeslaVehicleState)
    fun teslaVehicleVehicleSecurityConnected(vehicle: TeslaVehicle)
    fun teslaVehicleInfotainmentConnected(vehicle: TeslaVehicle)
}

class TeslaVehicle private constructor(
    private val vin: String,
    private val peripheralId: UUID,
    private val clientPrivateKey: P256.KeyAgreement.PrivateKey,
) : CBCentralManagerDelegate, CBPeripheralDelegate {
    private val clientPublicKeyBytes: ByteArray = clientPrivateKey.publicKey.toBytes()
    private var centralManager: CBCentralManager? by CBCentralManager.holder()
    private var vehiclePeripheral: CBPeripheral? = null
    private var toVehicleCharacteristic: CBCharacteristic? = null
    private var fromVehicleCharacteristic: CBCharacteristic? = null
    private var state: TeslaVehicleState = TeslaVehicleState.idle
    private var responseHandlers = mutableMapOf<String, (UniversalMessage_RoutableMessage) -> Unit>()
    private var receiveBuffer = ByteArray(0)
    private var vehicleDomains = mutableMapOf<UniversalMessage_Domain, VehicleDomain>()
    var delegate: TeslaVehicleDelegate? = null
    private val vehicleSecurityHandshakeTimer = MainTimer()
    private val infotainmentHandshakeTimer = MainTimer()

    companion object {
        operator fun invoke(
            vin: String,
            privateKeyPem: String,
            peripheralId: UUID,
            handshake: Boolean = true,
        ): TeslaVehicle? {
            val clientPrivateKey = try {
                P256.KeyAgreement.PrivateKey(pemRepresentation = privateKeyPem)
            } catch (error: Exception) {
                Log.i(TAG, "tesla-vehicle: Error $error")
                return null
            }
            return TeslaVehicle(vin = vin, peripheralId = peripheralId, clientPrivateKey = clientPrivateKey)
        }
    }

    fun start() {
        reset()
        centralManager = CBCentralManager(delegate = this, queue = null)
        startSession()
    }

    fun stop() {
        reset()
    }

    private fun reset() {
        resetSession()
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
    }

    private fun connect(central: CBCentralManager) {
        val peripheral = central.retrievePeripherals(withIdentifiers = listOf(peripheralId)).firstOrNull()
        if (peripheral == null) {
            Log.i(TAG, "tesla-vehicle: Vehicle not found")
            return
        }
        vehiclePeripheral = peripheral
        peripheral.delegate = this
        val options = mutableMapOf<String, Any>()
        options[CBConnectPeripheralOptionEnableAutoReconnect] = true
        central.connect(peripheral, options = options)
        setState(TeslaVehicleState.connecting)
    }

    fun addKeyRequestWithRole(privateKeyPem: String) {
        try {
            val privateKey = P256.KeyAgreement.PrivateKey(pemRepresentation = privateKeyPem)
            val clientPublicKeyBytes = privateKey.publicKey.toBytes()
            val message = VCSEC_UnsignedMessage()
            message.whitelistOperation.addKeyToWhitelistAndAddPermissions.key.publicKeyRaw = clientPublicKeyBytes
            message.whitelistOperation.addKeyToWhitelistAndAddPermissions.keyRole = Keys_Role.owner
            message.whitelistOperation.metadataForKey.keyFormFactor = VCSEC_KeyFormFactor.cloudKey
            var encoded = message.serializedData()
            val envelope = VCSEC_ToVCSECMessage()
            envelope.signedMessage.protobufMessageAsBytes = encoded
            envelope.signedMessage.signatureType = VCSEC_SignatureType.presentKey
            encoded = envelope.serializedData()
            sendData(encoded)
        } catch (error: Exception) {
            Log.i(TAG, "tesla-vehicle: Add key error $error")
        }
    }

    fun openTrunk() {
        val closureMoveRequest = VCSEC_ClosureMoveRequest()
        closureMoveRequest.rearTrunk = VCSEC_ClosureMoveType_E.closureMoveTypeOpen
        executeClosureMoveAction(closureMoveRequest) {
            Log.i(TAG, "tesla-vehicle: Open trunk response")
        }
    }

    fun closeTrunk() {
        val closureMoveRequest = VCSEC_ClosureMoveRequest()
        closureMoveRequest.rearTrunk = VCSEC_ClosureMoveType_E.closureMoveTypeClose
        executeClosureMoveAction(closureMoveRequest) {
            Log.i(TAG, "tesla-vehicle: Close trunk response")
        }
    }

    fun honk() {
        val action = CarServer_Action()
        action.vehicleAction.vehicleControlHonkHornAction = CarServer_VehicleControlHonkHornAction()
        executeCarServerAction(action) { _ ->
            Log.d(TAG, "tesla-vehicle: Honk response")
        }
    }

    fun flashLights() {
        val action = CarServer_Action()
        action.vehicleAction.vehicleControlFlashLightsAction = CarServer_VehicleControlFlashLightsAction()
        executeCarServerAction(action) { _ ->
            Log.d(TAG, "tesla-vehicle: Flash lights response")
        }
    }

    fun mediaNextTrack() {
        val action = CarServer_Action()
        action.vehicleAction.mediaNextTrack = CarServer_MediaNextTrack()
        executeCarServerAction(action) { _ ->
            Log.d(TAG, "tesla-vehicle: Media next track response")
        }
    }

    fun mediaPreviousTrack() {
        val action = CarServer_Action()
        action.vehicleAction.mediaPreviousTrack = CarServer_MediaPreviousTrack()
        executeCarServerAction(action) { _ ->
            Log.d(TAG, "tesla-vehicle: Media previous track response")
        }
    }

    fun mediaTogglePlayback() {
        val action = CarServer_Action()
        action.vehicleAction.mediaPlayAction = CarServer_MediaPlayAction()
        executeCarServerAction(action) { _ ->
            Log.d(TAG, "tesla-vehicle: Media toggle playback response")
        }
    }

    fun getChargeState(onCompleted: (CarServer_ChargeState) -> Unit) {
        val action = CarServer_Action()
        action.vehicleAction.getVehicleData.getChargeState = CarServer_GetChargeState()
        executeCarServerAction(action) { response ->
            onCompleted(response.vehicleData.chargeState)
        }
    }

    fun getDriveState(onCompleted: (CarServer_DriveState) -> Unit) {
        val action = CarServer_Action()
        action.vehicleAction.getVehicleData.getDriveState = CarServer_GetDriveState()
        executeCarServerAction(action) { response ->
            onCompleted(response.vehicleData.driveState)
        }
    }

    fun getMediaState(onCompleted: (CarServer_MediaState) -> Unit) {
        val action = CarServer_Action()
        action.vehicleAction.getVehicleData.getMediaState = CarServer_GetMediaState()
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
        val unsignedMessage = VCSEC_UnsignedMessage()
        unsignedMessage.closureMoveRequest = closureMoveRequest
        try {
            val payload = unsignedMessage.serializedData()
            vehicleDomain.appendJob(address = getNextAddress(), payload = payload) { _, _ ->
                onCompleted()
            }
            trySendNextJob(UniversalMessage_Domain.vehicleSecurity)
        } catch (error: Exception) {
            Log.i(TAG, "tesla-vehicle: Execute closure move action error $error")
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
        try {
            val payload = action.serializedData()
            vehicleDomain.appendJob(address = getNextAddress(), payload = payload) { request, response ->
                val aesGcmResponseData = response.signatureData.aesGcmResponseData
                val sealedBox = AES.GCM.SealedBox(
                    nonce = AES.GCM.Nonce(data = aesGcmResponseData.nonce),
                    ciphertext = response.protobufMessageAsBytes,
                    tag = aesGcmResponseData.tag,
                )
                val metadataHash = createResponseMetadata(request, response)
                val decoded = AES.GCM.open(
                    sealedBox,
                    using = vehicleDomain.getSymmetricKey(),
                    authenticating = metadataHash,
                )
                val response = CarServer_Response(serializedBytes = decoded)
                if (response.actionStatus.result != CarServer_OperationStatus_E.operationstatusOk) {
                    Log.i(TAG, "tesla-vehicle: Car server response status not ok")
                }
                onCompleted(response)
            }
            trySendNextJob(UniversalMessage_Domain.infotainment)
        } catch (error: Exception) {
            Log.i(TAG, "tesla-vehicle: Execute car server action error $error")
        }
    }

    private fun setState(state: TeslaVehicleState) {
        if (state == this.state) {
            return
        }
        Log.i(TAG, "tesla-vehicle: State change ${this.state} -> $state")
        this.state = state
        delegate?.teslaVehicleState(vehicle = this, state = state)
    }

    private fun getNextAddress(): ByteArray = randomBytes(16)

    private fun startVehicleSecurityHandshake() {
        sendSessionInfoRequest(UniversalMessage_Domain.vehicleSecurity)
        vehicleSecurityHandshakeTimer.startSingleShot(timeout = 10.0) {
            try {
                startVehicleSecurityHandshake()
            } catch (error: Exception) {
                Log.i(TAG, "tesla-vehicle: Failed to start vehicle security handshake with error $error")
            }
        }
    }

    private fun startInfotainmentHandshake() {
        sendSessionInfoRequest(UniversalMessage_Domain.infotainment)
        infotainmentHandshakeTimer.startSingleShot(timeout = 10.0) {
            try {
                startInfotainmentHandshake()
            } catch (error: Exception) {
                Log.i(TAG, "tesla-vehicle: Failed to start infotainment handshake with error $error")
            }
        }
    }

    private fun sendSessionInfoRequest(domain: UniversalMessage_Domain) {
        val address = getNextAddress()
        val uuid = randomBytes(16)
        val message = UniversalMessage_RoutableMessage()
        message.toDestination.domain = domain
        message.fromDestination.routingAddress = address
        message.sessionInfoRequest.publicKey = clientPublicKeyBytes
        message.uuid = uuid
        responseHandlers[address.hexString()] = { response ->
            try {
                handleSessionInfoResponse(message, response)
            } catch (error: Exception) {
                Log.d(TAG, "tesla-vehicle: Session info failed with $error")
            }
        }
        sendMessage(message)
    }

    private fun handleSessionInfoResponse(
        request: UniversalMessage_RoutableMessage,
        response: UniversalMessage_RoutableMessage,
    ) {
        val domain = response.fromDestination.domain
        val sessionInfo = Signatures_SessionInfo(serializedBytes = response.sessionInfo)
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
            else -> {}
        }
    }

    private fun startJob(domain: UniversalMessage_Domain, job: Job) {
        val request = UniversalMessage_RoutableMessage()
        request.toDestination.domain = domain
        request.fromDestination.routingAddress = job.address
        request.uuid = randomBytes(16)
        request.flags = 1u shl UniversalMessage_Flags.flagEncryptResponse.rawValue
        sign(request, job.playload)
        responseHandlers[job.address.hexString()] = { response ->
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
        if (response.signedMessageStatus.signedMessageFault != UniversalMessage_MessageFault_E.rrorNone) {
            throw Exception("Request was not successful. Response $response")
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
        val size = reader.readUInt16()
        if (reader.bytesAvailable < size.toInt()) {
            return
        }
        val payload = reader.readBytes(size.toInt())
        if (reader.bytesAvailable > 0) {
            receiveBuffer = reader.readBytes(reader.bytesAvailable)
        } else {
            receiveBuffer = ByteArray(0)
        }
        val message = UniversalMessage_RoutableMessage(serializedBytes = payload)
        val address = (message.toDestination.subDestination as? UniversalMessage_Destination.OneOf_SubDestination.routingAddress)?.value
            ?: return
        val responseHandler = responseHandlers.remove(address.hexString()) ?: return
        responseHandler(message)
    }

    private fun sendMessage(message: UniversalMessage_RoutableMessage) {
        sendData(message.serializedData())
    }

    private fun sendData(message: ByteArray) {
        val toVehicleCharacteristic = this.toVehicleCharacteristic ?: return
        val writer = ByteWriter()
        writer.writeUInt16(message.size.toUShort())
        writer.writeBytes(message)
        val data = writer.data
        val blockLength = 20
        for (offset in 0 until data.size step blockLength) {
            val block = data.copyOfRange(offset, minOf(offset + blockLength, data.size))
            vehiclePeripheral?.writeValue(block, `for` = toVehicleCharacteristic, type = CBCharacteristicWriteType.withResponse)
        }
    }

    private fun sign(message: UniversalMessage_RoutableMessage, payload: ByteArray) {
        val vehicleDomain = vehicleDomains[message.toDestination.domain]
            ?: throw Exception("Cannot sign for missing vehicle domain")
        message.signatureData.signerIdentity.publicKey = clientPublicKeyBytes
        message.signatureData.aesGcmPersonalizedData.epoch = vehicleDomain.epoch()
        message.signatureData.aesGcmPersonalizedData.counter = vehicleDomain.nextCounter()
        message.signatureData.aesGcmPersonalizedData.expiresAt = vehicleDomain.expiresAt()
        val key = vehicleDomain.getSymmetricKey()
        val metadataHash = createRequestMetadata(message)
        val encrypted = AES.GCM.seal(payload, using = key, authenticating = metadataHash)
        message.signatureData.aesGcmPersonalizedData.nonce = encrypted.nonce.toByteArray()
        message.signatureData.aesGcmPersonalizedData.tag = encrypted.tag
        message.protobufMessageAsBytes = encrypted.ciphertext
    }

    private fun createRequestMetadata(message: UniversalMessage_RoutableMessage): ByteArray {
        val metadata = Metadata()
        metadata.addUInt8(
            tag = Signatures_Tag.signatureType,
            value = Signatures_SignatureType.aesGcmPersonalized.rawValue.toUByte(),
        )
        metadata.addUInt8(Signatures_Tag.domain, message.toDestination.domain.rawValue.toUByte())
        metadata.add(Signatures_Tag.personalization, vin.utf8Data)
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
        metadata.addUInt8(Signatures_Tag.signatureType, Signatures_SignatureType.aesGcmResponse.rawValue.toUByte())
        metadata.addUInt8(Signatures_Tag.domain, response.fromDestination.domain.rawValue.toUByte())
        metadata.add(Signatures_Tag.personalization, vin.utf8Data)
        metadata.addUInt32(Signatures_Tag.counter, response.signatureData.aesGcmResponseData.counter)
        metadata.addUInt32(Signatures_Tag.flags, response.flags)
        var requestId = byteArrayOf(Signatures_SignatureType.aesGcmPersonalized.rawValue.toByte())
        requestId += request.signatureData.aesGcmPersonalizedData.tag
        metadata.add(Signatures_Tag.requestHash, requestId)
        metadata.addUInt32(Signatures_Tag.fault, response.signedMessageStatus.signedMessageFault.rawValue.toUInt())
        return metadata.finalize(ByteArray(0))
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        when (central.state) {
            CBManagerState.poweredOn -> connect(central)
            else -> {}
        }
    }

    override fun centralManagerDidFailToConnect(central: CBCentralManager, peripheral: CBPeripheral, error: Throwable?) {
        Log.d(TAG, "tesla-vehicle: Connect failure")
        reset()
    }

    override fun centralManagerDidConnect(central: CBCentralManager, peripheral: CBPeripheral) {
        Log.d(TAG, "tesla-vehicle: Connected")
        peripheral.discoverServices(listOf(vehicleServiceUuid))
    }

    override fun centralManagerDidDisconnectPeripheral(central: CBCentralManager, peripheral: CBPeripheral, error: Throwable?) {
        Log.d(TAG, "tesla-vehicle: Disconnected")
        reset()
    }

    override fun centralManagerDidDisconnectPeripheral(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        timestamp: Double,
        isReconnecting: Boolean,
        error: Throwable?,
    ) {
        Log.d(TAG, "tesla-vehicle: Disconnected (reconnecting: $isReconnecting)")
        if (!isReconnecting) {
            reset()
            return
        }
        resetSession()
        startSession()
        setState(TeslaVehicleState.connecting)
    }

    override fun peripheralDidDiscoverServices(peripheral: CBPeripheral, error: Throwable?) {
        val peripheralServices = peripheral.services
        if (peripheralServices == null) {
            Log.i(TAG, "tesla-vehicle: No services found")
            return
        }
        for (service in peripheralServices) {
            peripheral.discoverCharacteristics(listOf(toVehicleUuid, fromVehicleUuid), `for` = service)
        }
    }

    override fun peripheralDidDiscoverCharacteristicsFor(
        peripheral: CBPeripheral,
        service: CBService,
        error: Throwable?,
    ) {
        for (characteristic in service.characteristics.orEmpty()) {
            if (characteristic.uuid == toVehicleUuid) {
                toVehicleCharacteristic = characteristic
            } else if (characteristic.uuid == fromVehicleUuid) {
                fromVehicleCharacteristic = characteristic
                peripheral.setNotifyValue(true, `for` = characteristic)
            }
        }
        setState(TeslaVehicleState.connected)
        try {
            startVehicleSecurityHandshake()
        } catch (error: Exception) {
            Log.i(TAG, "tesla-vehicle: Failed to start handshake $error")
        }
    }

    override fun peripheralDidUpdateValueFor(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: Throwable?,
    ) {
        val value = characteristic.value ?: return
        try {
            handleData(value)
        } catch (error: Exception) {
            Log.i(TAG, "tesla-vehicle: Message handling error $error")
            reset()
        }
    }
}

private class Metadata {
    private val writer = ByteWriter()
    private var lastTag: Signatures_Tag? = null

    fun add(tag: Signatures_Tag, value: ByteArray) {
        val lastTag = this.lastTag
        if (lastTag != null) {
            if (tag.rawValue <= lastTag.rawValue) {
                throw Exception("Must be added in increasing tags order")
            }
        }
        if (value.size > 255) {
            throw Exception("Metadata value too long ${value.size}")
        }
        this.lastTag = tag
        writer.writeUInt8(tag.rawValue.toUByte())
        writer.writeUInt8(value.size.toUByte())
        writer.writeBytes(value)
    }

    fun addUInt8(tag: Signatures_Tag, value: UByte) {
        add(tag, byteArrayOf(value.toByte()))
    }

    fun addUInt32(tag: Signatures_Tag, value: UInt) {
        val data = ByteArray(4)
        data.setUInt32Be(value = value)
        add(tag, data)
    }

    fun finalize(message: ByteArray): ByteArray {
        writer.writeUInt8(Signatures_Tag.end.rawValue.toUByte())
        writer.writeBytes(message)
        return SHA256.hash(data = writer.data).toByteArray()
    }
}
