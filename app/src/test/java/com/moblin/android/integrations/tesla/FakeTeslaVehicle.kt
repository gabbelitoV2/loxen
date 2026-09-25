package com.moblin.android.integrations.tesla

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.os.SystemClock
import com.moblin.android.integrations.tesla.protobuf.CarServer_Action
import com.moblin.android.integrations.tesla.protobuf.CarServer_ChargeState
import com.moblin.android.integrations.tesla.protobuf.CarServer_DriveState
import com.moblin.android.integrations.tesla.protobuf.CarServer_MediaState
import com.moblin.android.integrations.tesla.protobuf.CarServer_OperationStatus_E
import com.moblin.android.integrations.tesla.protobuf.CarServer_Response
import com.moblin.android.integrations.tesla.protobuf.CarServer_VehicleAction
import com.moblin.android.integrations.tesla.protobuf.Signatures_SessionInfo
import com.moblin.android.integrations.tesla.protobuf.Signatures_Session_Info_Status
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_Domain
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_MessageFault_E
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_OperationStatus_E
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_RoutableMessage
import com.moblin.android.integrations.tesla.protobuf.VCSEC_FromVCSECMessage
import com.moblin.android.integrations.tesla.protobuf.VCSEC_ToVCSECMessage
import com.moblin.android.integrations.tesla.protobuf.VCSEC_UnsignedMessage
import com.moblin.android.platform.corebluetooth.RecordingBluetoothGatt
import com.moblin.android.platform.swiftprotobuf.serializedData
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.robolectric.shadow.api.Shadow

internal val teslaServiceId: UUID = UUID.fromString("00000211-b2d1-43f0-9b88-960cebf8b91e")
internal val teslaToVehicleId: UUID = UUID.fromString("00000212-b2d1-43f0-9b88-960cebf8b91e")
internal val teslaFromVehicleId: UUID = UUID.fromString("00000213-b2d1-43f0-9b88-960cebf8b91e")
private val clientConfigurationId: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
private val genericAccessId: UUID = UUID.fromString("00001800-0000-1000-8000-00805F9B34FB")
private val deviceNameId: UUID = UUID.fromString("00002A00-0000-1000-8000-00805F9B34FB")

private val subjectPublicKeyInfoHeader = hexBytes("3059301306072a8648ce3d020106082a8648ce3d030107034200")

internal fun hexBytes(text: String): ByteArray = text.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

internal fun ByteArray.hex(): String = joinToString("") { "%02x".format(it.toInt() and 0xFF) }

private fun uint32Be(value: UInt): ByteArray = ByteBuffer.allocate(4).putInt(value.toInt()).array()

private fun fixed32(value: BigInteger): ByteArray {
    val bytes = value.toByteArray().takeLast(32).toByteArray()
    return ByteArray(32 - bytes.size) + bytes
}

internal fun uncompressedPublicKey(key: ECPublicKey): ByteArray {
    return byteArrayOf(0x04) + fixed32(key.w.affineX) + fixed32(key.w.affineY)
}

internal class TeslaSignedRequest(
    val domain: UniversalMessage_Domain,
    val message: UniversalMessage_RoutableMessage,
    val fault: UniversalMessage_MessageFault_E,
    val plaintext: ByteArray?,
) {
    val counter: UInt
        get() = message.signatureData.aesGcmPersonalizedData.counter

    val epoch: ByteArray
        get() = message.signatureData.aesGcmPersonalizedData.epoch

    val expiresAt: UInt
        get() = message.signatureData.aesGcmPersonalizedData.expiresAt

    fun carServerAction(): CarServer_Action = CarServer_Action(serializedBytes = plaintext!!)

    fun vehicleActionMsg(): CarServer_VehicleAction.OneOf_VehicleActionMsg? = carServerAction().vehicleAction.vehicleActionMsg

    fun unsignedMessage(): VCSEC_UnsignedMessage = VCSEC_UnsignedMessage(serializedBytes = plaintext!!)
}

class FakeTeslaVehicle(private val vin: String) {
    private val random = SecureRandom()
    private val privateKey: PrivateKey
    val publicKeyBytes: ByteArray
    val epochs = mutableMapOf(
        UniversalMessage_Domain.vehicleSecurity to randomBytes(16),
        UniversalMessage_Domain.infotainment to randomBytes(16),
    )
    private val counters = mutableMapOf(
        UniversalMessage_Domain.vehicleSecurity to 41u,
        UniversalMessage_Domain.infotainment to 1000u,
    )
    var clockBase: UInt = 7_200u
    private val clockStartedAt = SystemClock.elapsedRealtime()
    var respondToSessionInfoRequests = true
    var respondToSignedRequests = true
    var tamperWithSessionInfoTag = false
    var tamperWithNextResponse = false
    var nextFault: UniversalMessage_MessageFault_E? = null
    var notificationLength = 20
    var responseFlags: UInt = 0u
    var mtu = 185
    val chargeState = CarServer_ChargeState()
    val driveState = CarServer_DriveState()
    val mediaState = CarServer_MediaState()
    val writes = mutableListOf<String>()
    val frames = mutableListOf<ByteArray>()
    val sessionInfoRequests = mutableListOf<UniversalMessage_RoutableMessage>()
    internal val signedRequests = mutableListOf<TeslaSignedRequest>()
    val addKeyRequests = mutableListOf<VCSEC_ToVCSECMessage>()
    val sentSessionInfos = mutableListOf<Signatures_SessionInfo>()
    var gatt: BluetoothGatt? = null
        private set
    private var handledCalls = 0
    private var receiveBuffer = ByteArray(0)

    init {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = generator.generateKeyPair()
        privateKey = keyPair.private
        publicKeyBytes = uncompressedPublicKey(keyPair.public as ECPublicKey)
    }

    private fun randomBytes(count: Int): ByteArray = ByteArray(count).also { random.nextBytes(it) }

    fun clockTime(): UInt = clockBase + ((SystemClock.elapsedRealtime() - clockStartedAt) / 1000).toUInt()

    fun counter(domain: UniversalMessage_Domain): UInt = counters.getValue(domain)

    fun rotateEpoch(domain: UniversalMessage_Domain) {
        epochs[domain] = randomBytes(16)
    }

    fun services(): List<BluetoothGattService> {
        val tesla = BluetoothGattService(teslaServiceId, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        tesla.addCharacteristic(
            BluetoothGattCharacteristic(
                teslaToVehicleId,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE,
            ),
        )
        val fromVehicle = BluetoothGattCharacteristic(
            teslaFromVehicleId,
            BluetoothGattCharacteristic.PROPERTY_INDICATE,
            BluetoothGattCharacteristic.PERMISSION_READ,
        )
        fromVehicle.addDescriptor(
            BluetoothGattDescriptor(
                clientConfigurationId,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
            ),
        )
        tesla.addCharacteristic(fromVehicle)
        val genericAccess = BluetoothGattService(genericAccessId, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        genericAccess.addCharacteristic(
            BluetoothGattCharacteristic(
                deviceNameId,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
            ),
        )
        return listOf(genericAccess, tesla)
    }

    private fun recording(gatt: BluetoothGatt): RecordingBluetoothGatt = Shadow.extract(gatt)

    private fun callback(gatt: BluetoothGatt): BluetoothGattCallback = recording(gatt).gattCallback

    private fun characteristic(gatt: BluetoothGatt, id: UUID): BluetoothGattCharacteristic {
        return recording(gatt).discovered.firstNotNullOf { it.getCharacteristic(id) }
    }

    fun connect(gatt: BluetoothGatt, runMain: () -> Unit) {
        this.gatt = gatt
        handledCalls = 0
        receiveBuffer = ByteArray(0)
        val recording = recording(gatt)
        callback(gatt).onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        runMain()
        check(recording.calls == listOf("requestMtu 517")) { "Unexpected calls ${recording.calls}" }
        callback(gatt).onMtuChanged(gatt, mtu, BluetoothGatt.GATT_SUCCESS)
        runMain()
        check(recording.calls == listOf("requestMtu 517", "discoverServices")) { "Unexpected calls ${recording.calls}" }
        recording.discovered = services()
        callback(gatt).onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS)
        runMain()
        handledCalls = 2
    }

    fun disconnect(status: Int = 0x08) {
        val gatt = gatt ?: return
        callback(gatt).onConnectionStateChange(gatt, status, BluetoothProfile.STATE_DISCONNECTED)
    }

    fun pump(runMain: () -> Unit) {
        while (true) {
            runMain()
            val gatt = gatt ?: return
            val calls = recording(gatt).calls
            if (handledCalls >= calls.size) {
                return
            }
            while (handledCalls < calls.size) {
                handle(gatt, calls[handledCalls])
                handledCalls += 1
            }
        }
    }

    private fun handle(gatt: BluetoothGatt, call: String) {
        writes.add(call)
        val parts = call.split(" ")
        when (parts[0]) {
            "writeDescriptor" -> {
                val descriptor = characteristic(gatt, teslaFromVehicleId).getDescriptor(clientConfigurationId)
                callback(gatt).onDescriptorWrite(gatt, descriptor, BluetoothGatt.GATT_SUCCESS)
            }
            "writeCharacteristic" -> {
                check(UUID.fromString(parts[1]) == teslaToVehicleId) { "Write to ${parts[1]}" }
                check(parts[3] == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT.toString()) { "Write type ${parts[3]}" }
                val block = hexBytes(parts[2])
                check(block.size <= 20) { "Block of ${block.size} bytes" }
                receiveBuffer += block
                callback(gatt).onCharacteristicWrite(gatt, characteristic(gatt, teslaToVehicleId), BluetoothGatt.GATT_SUCCESS)
                handleReceiveBuffer()
            }
        }
    }

    private fun handleReceiveBuffer() {
        while (receiveBuffer.size >= 2) {
            val length = ((receiveBuffer[0].toInt() and 0xFF) shl 8) or (receiveBuffer[1].toInt() and 0xFF)
            if (receiveBuffer.size < 2 + length) {
                return
            }
            val frame = receiveBuffer.copyOfRange(2, 2 + length)
            receiveBuffer = receiveBuffer.copyOfRange(2 + length, receiveBuffer.size)
            frames.add(frame)
            handleFrame(frame)
        }
    }

    private fun handleFrame(frame: ByteArray) {
        val message = UniversalMessage_RoutableMessage(serializedBytes = frame)
        if (!message.hasToDestination) {
            addKeyRequests.add(VCSEC_ToVCSECMessage(serializedBytes = frame))
            return
        }
        when (message.payload) {
            is UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfoRequest -> handleSessionInfoRequest(message)
            is UniversalMessage_RoutableMessage.OneOf_Payload.protobufMessageAsBytes -> if (respondToSignedRequests) {
                handleSignedRequest(message)
            }
            else -> error("Unexpected payload ${message.payload}")
        }
    }

    fun sessionKey(clientPublicKey: ByteArray): ByteArray {
        val publicKey = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(subjectPublicKeyInfoHeader + clientPublicKey))
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)
        return MessageDigest.getInstance("SHA-1").digest(agreement.generateSecret()).copyOf(16)
    }

    private fun metadataHash(fields: List<Pair<Int, ByteArray>>, message: ByteArray = ByteArray(0)): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(metadata(fields) + message)
    }

    private fun metadata(fields: List<Pair<Int, ByteArray>>): ByteArray {
        val output = ByteArrayOutputStream()
        for ((tag, value) in fields) {
            output.write(tag)
            output.write(value.size)
            output.write(value)
        }
        output.write(0xFF)
        return output.toByteArray()
    }

    fun sessionInfoTag(key: ByteArray, challenge: ByteArray, encodedInfo: ByteArray): ByteArray {
        val sessionInfoKey = hmacSha256(key, "session info".toByteArray())
        val fields = listOf(0 to byteArrayOf(6), 2 to vin.toByteArray(), 6 to challenge)
        return hmacSha256(sessionInfoKey, metadata(fields) + encodedInfo)
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    fun requestMetadataHash(message: UniversalMessage_RoutableMessage): ByteArray {
        val data = message.signatureData.aesGcmPersonalizedData
        val fields = mutableListOf(
            0 to byteArrayOf(5),
            1 to byteArrayOf(message.toDestination.domain.rawValue.toByte()),
            2 to vin.toByteArray(),
            3 to data.epoch,
            4 to uint32Be(data.expiresAt),
            5 to uint32Be(data.counter),
        )
        if (message.flags > 0u) {
            fields.add(7 to uint32Be(message.flags))
        }
        return metadataHash(fields)
    }

    private fun responseMetadataHash(
        domain: UniversalMessage_Domain,
        counter: UInt,
        flags: UInt,
        requestTag: ByteArray,
        fault: UniversalMessage_MessageFault_E,
    ): ByteArray {
        return metadataHash(
            listOf(
                0 to byteArrayOf(9),
                1 to byteArrayOf(domain.rawValue.toByte()),
                2 to vin.toByteArray(),
                5 to uint32Be(counter),
                7 to uint32Be(flags),
                8 to byteArrayOf(5) + requestTag,
                9 to uint32Be(fault.rawValue.toUInt()),
            ),
        )
    }

    private fun handleSessionInfoRequest(request: UniversalMessage_RoutableMessage) {
        sessionInfoRequests.add(request)
        if (respondToSessionInfoRequests) {
            respondToSessionInfoRequest(request)
        }
    }

    fun respondToSessionInfoRequest(request: UniversalMessage_RoutableMessage) {
        val domain = request.toDestination.domain
        val info = Signatures_SessionInfo()
        info.counter = counters.getValue(domain)
        info.publicKey = publicKeyBytes
        info.epoch = epochs.getValue(domain)
        info.clockTime = clockTime()
        info.status = Signatures_Session_Info_Status.ok
        sentSessionInfos.add(info)
        val encoded = info.serializedData()
        val key = sessionKey(request.sessionInfoRequest.publicKey)
        val response = UniversalMessage_RoutableMessage()
        response.toDestination.routingAddress = request.fromDestination.routingAddress
        response.fromDestination.domain = domain
        response.sessionInfo = encoded
        response.requestUuid = request.uuid
        val tag = sessionInfoTag(key, request.uuid, encoded)
        if (tamperWithSessionInfoTag) {
            tag[0] = (tag[0].toInt() xor 1).toByte()
        }
        response.signatureData.sessionInfoTag.tag = tag
        send(response)
    }

    private fun handleSignedRequest(request: UniversalMessage_RoutableMessage) {
        val domain = request.toDestination.domain
        val data = request.signatureData.aesGcmPersonalizedData
        val key = sessionKey(request.signatureData.signerIdentity.publicKey)
        var plaintext: ByteArray? = null
        var fault = when {
            !data.epoch.contentEquals(epochs.getValue(domain)) -> UniversalMessage_MessageFault_E.rrorIncorrectEpoch
            data.counter <= counters.getValue(domain) -> UniversalMessage_MessageFault_E.rrorInvalidTokenOrCounter
            data.expiresAt < clockTime() -> UniversalMessage_MessageFault_E.rrorTimeExpired
            else -> {
                plaintext = runCatching {
                    decrypt(key, data.nonce, request.protobufMessageAsBytes, data.tag, requestMetadataHash(request))
                }.getOrNull()
                if (plaintext == null) {
                    UniversalMessage_MessageFault_E.rrorInvalidSignature
                } else {
                    UniversalMessage_MessageFault_E.rrorNone
                }
            }
        }
        nextFault?.let {
            fault = it
            nextFault = null
        }
        signedRequests.add(TeslaSignedRequest(domain, request, fault, plaintext))
        val response = UniversalMessage_RoutableMessage()
        response.toDestination.routingAddress = request.fromDestination.routingAddress
        response.fromDestination.domain = domain
        response.requestUuid = request.uuid
        if (fault != UniversalMessage_MessageFault_E.rrorNone) {
            response.signedMessageStatus.operationStatus = UniversalMessage_OperationStatus_E.rror
            response.signedMessageStatus.signedMessageFault = fault
            send(response)
            return
        }
        counters[domain] = data.counter
        val reply = when (domain) {
            UniversalMessage_Domain.infotainment -> carServerResponse(CarServer_Action(serializedBytes = plaintext!!))
            else -> VCSEC_FromVCSECMessage().serializedData()
        }
        response.flags = responseFlags
        val nonce = randomBytes(12)
        val aad = responseMetadataHash(domain, data.counter, responseFlags, data.tag, fault)
        val sealed = encrypt(key, nonce, reply, aad)
        if (tamperWithNextResponse) {
            tamperWithNextResponse = false
            sealed[0] = (sealed[0].toInt() xor 1).toByte()
        }
        response.protobufMessageAsBytes = sealed.copyOfRange(0, sealed.size - 16)
        response.signatureData.aesGcmResponseData.nonce = nonce
        response.signatureData.aesGcmResponseData.counter = data.counter
        response.signatureData.aesGcmResponseData.tag = sealed.copyOfRange(sealed.size - 16, sealed.size)
        send(response)
    }

    private fun carServerResponse(action: CarServer_Action): ByteArray {
        val response = CarServer_Response()
        response.actionStatus.result = CarServer_OperationStatus_E.operationstatusOk
        val getVehicleData = action.vehicleAction.vehicleActionMsg as?
            CarServer_VehicleAction.OneOf_VehicleActionMsg.getVehicleData
        if (getVehicleData != null) {
            if (getVehicleData.value.hasGetChargeState) {
                response.vehicleData.chargeState = chargeState
            }
            if (getVehicleData.value.hasGetDriveState) {
                response.vehicleData.driveState = driveState
            }
            if (getVehicleData.value.hasGetMediaState) {
                response.vehicleData.mediaState = mediaState
            }
        }
        return response.serializedData()
    }

    private fun decrypt(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray, tag: ByteArray, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(ciphertext + tag)
    }

    private fun encrypt(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(plaintext)
    }

    fun send(message: UniversalMessage_RoutableMessage) {
        sendFrame(message.serializedData())
    }

    fun sendFrame(payload: ByteArray) {
        val gatt = gatt ?: return
        val data = byteArrayOf((payload.size shr 8).toByte(), payload.size.toByte()) + payload
        val characteristic = characteristic(gatt, teslaFromVehicleId)
        for (offset in data.indices step notificationLength) {
            val chunk = data.copyOfRange(offset, minOf(offset + notificationLength, data.size))
            callback(gatt).onCharacteristicChanged(gatt, characteristic, chunk)
        }
    }
}
