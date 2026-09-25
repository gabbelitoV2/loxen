package com.moblin.android.integrations.tesla

import com.moblin.android.integrations.tesla.protobuf.CarServer_ChargeState
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_RoutableMessage
import com.moblin.android.platform.corebluetooth.RecordingBluetoothGatt
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val clientPem = """
    -----BEGIN PRIVATE KEY-----
    MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgyI8B9RDZrD9wopLa
    ojFt5UTpqriv6EBJxiqcV4YtFDOhRANCAATa0LZTlCIc+bBR4f7KV4fQmN/mN/yQ
    ue+UXQw3clgRgFJxoEYc24JS1h8cRW+j5Zqx9FszrM9fWDieBXe4mQuz
    -----END PRIVATE KEY-----
""".trimIndent()
private val clientPublicKey = hexBytes(
    "04dad0b65394221cf9b051e1feca5787d098dfe637fc90b9ef945d0c3772581180" +
        "5271a0461cdb8252d61f1c456fa3e59ab1f45b33accf5f58389e0577b8990bb3",
)
private val sessionKey = hexBytes("f18d89be1f0206d14f29f942842be1c5")
private val epoch = hexBytes("000102030405060708090a0b0c0d0e0f")
private val sessionInfo = hexBytes(
    "0807124104d12dfb5289c8d4f81208b70270398c342296970a0bccb74c736fc7554494bf6356fbf3ca366cc23e8157854c13c5" +
        "8d6aac23f046ada30f8353e74f33039872ab1a10000102030405060708090a0b0c0d0e0f25e8030000",
)

private fun varint(value: Long): ByteArray {
    val output = ByteArrayOutputStream()
    var rest = value
    while (rest >= 0x80) {
        output.write(((rest and 0x7F) or 0x80).toInt())
        rest = rest shr 7
    }
    output.write(rest.toInt())
    return output.toByteArray()
}

private fun bytesField(field: Int, value: ByteArray): ByteArray {
    return varint(((field shl 3) or 2).toLong()) + varint(value.size.toLong()) + value
}

private fun varintField(field: Int, value: Long): ByteArray = varint((field shl 3).toLong()) + varint(value)

private fun fixed32Field(field: Int, value: Int): ByteArray {
    return varint(((field shl 3) or 5).toLong()) + ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
}

private fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

private fun responseMetadataHash(requestTag: ByteArray): ByteArray {
    return sha256(
        hexBytes("000109" + "010103" + "0211" + "35594a3345314541374b46333137303030" + "050400000009" + "070400000010" + "081105") +
            requestTag + hexBytes("090400000000" + "ff"),
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [RecordingBluetoothGatt::class])
class TeslaProtocolVectorSuite {
    private val bench = TeslaTestBench()
    private val fake = bench.fake
    private val delegate = TeslaVehicleEvents()
    private var vehicle: TeslaVehicle? = null

    @Before
    fun setUp() {
        bench.setUp()
    }

    @After
    fun tearDown() {
        vehicle?.delegate = null
        vehicle?.stop()
        bench.tearDown()
    }

    private fun gcm(mode: Int, nonce: ByteArray, aad: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(mode, SecretKeySpec(sessionKey, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(data)
    }

    private fun answerSessionInfoRequest(frame: ByteArray, domain: Int) {
        val request = UniversalMessage_RoutableMessage(serializedBytes = frame)
        val address = request.fromDestination.routingAddress
        val expected = bytesField(6, varintField(1, domain.toLong())) +
            bytesField(7, bytesField(2, address)) +
            bytesField(14, bytesField(1, clientPublicKey)) +
            bytesField(51, request.uuid)
        assertEquals(expected.hex(), frame.hex())
        fake.sendFrame(
            bytesField(6, bytesField(2, address)) +
                bytesField(7, varintField(1, domain.toLong())) +
                bytesField(15, sessionInfo) +
                bytesField(50, request.uuid),
        )
        bench.pump()
    }

    private fun openSignedRequest(frame: ByteArray, domain: Int, counter: Int, metadataHash: String): ByteArray {
        val request = UniversalMessage_RoutableMessage(serializedBytes = frame)
        val data = request.signatureData.aesGcmPersonalizedData
        val expected = bytesField(6, varintField(1, domain.toLong())) +
            bytesField(7, bytesField(2, request.fromDestination.routingAddress)) +
            bytesField(10, request.protobufMessageAsBytes) +
            bytesField(
                13,
                bytesField(1, bytesField(1, clientPublicKey)) +
                    bytesField(
                        5,
                        bytesField(1, epoch) +
                            bytesField(2, data.nonce) +
                            varintField(3, counter.toLong()) +
                            fixed32Field(4, 1015) +
                            bytesField(5, data.tag),
                    ),
            ) +
            bytesField(51, request.uuid) +
            varintField(52, 2)
        assertEquals(expected.hex(), frame.hex())
        assertEquals(12, data.nonce.size)
        assertEquals(16, data.tag.size)
        return gcm(Cipher.DECRYPT_MODE, data.nonce, hexBytes(metadataHash), request.protobufMessageAsBytes + data.tag)
    }

    @Test
    fun requestsAndResponsesFollowTeslaVehicleCommandVectors() {
        assertEquals(
            "f0fdc3980593850148f7b08fbd06b7bacce263731ea520608e411d1fe0a67a59",
            responseMetadataHash(ByteArray(16) { (0xA0 + it).toByte() }).hex(),
        )
        fake.respondToSessionInfoRequests = false
        fake.respondToSignedRequests = false
        val vehicle = assertNotNull(TeslaVehicle(vin = teslaVin, privateKeyPem = clientPem, peripheralId = bench.peripheralId))
        this.vehicle = vehicle
        vehicle.delegate = delegate
        vehicle.start()
        bench.runMain()
        bench.connectFake()
        answerSessionInfoRequest(fake.frames[0], 2)
        answerSessionInfoRequest(fake.frames[1], 3)
        assertEquals(listOf("state connecting", "state connected", "vehicleSecurity", "infotainment"), delegate.events)
        vehicle.honk()
        bench.pump()
        val honk = openSignedRequest(
            fake.frames[2],
            3,
            8,
            "3c4b3b21ce630a0e27410282cedab45ddc1cad278adc2977a1e2bfe1e52abd4f",
        )
        assertEquals("1203da0100", honk.hex())
        vehicle.openTrunk()
        bench.pump()
        val openTrunk = openSignedRequest(
            fake.frames[3],
            2,
            8,
            "af11ee707c46a2240460083cc546e7a437fa74c3a44ec45f157a6d29fd7144c8",
        )
        assertEquals("22022803", openTrunk.hex())
        var chargeState: CarServer_ChargeState? = null
        vehicle.getChargeState { chargeState = it }
        bench.pump()
        val getChargeState = openSignedRequest(
            fake.frames[4],
            3,
            9,
            "b403ff9b2b6b4dcb339f4a063274d95b58deaa96bbd7390d69747e397949c6f9",
        )
        assertEquals("12040a021200", getChargeState.hex())
        val request = UniversalMessage_RoutableMessage(serializedBytes = fake.frames[4])
        val requestTag = request.signatureData.aesGcmPersonalizedData.tag
        val nonce = hexBytes("f0f1f2f3f4f5f6f7f8f9fafb")
        val sealed = gcm(Cipher.ENCRYPT_MODE, nonce, responseMetadataHash(requestTag), hexBytes("0a0012051a0390074d"))
        fake.sendFrame(
            bytesField(6, bytesField(2, request.fromDestination.routingAddress)) +
                bytesField(7, varintField(1, 3)) +
                bytesField(10, sealed.copyOfRange(0, sealed.size - 16)) +
                bytesField(
                    13,
                    bytesField(
                        9,
                        bytesField(1, nonce) + varintField(2, 9) + bytesField(3, sealed.copyOfRange(sealed.size - 16, sealed.size)),
                    ),
                ) +
                bytesField(50, request.uuid) +
                varintField(52, 0x10),
        )
        bench.pump()
        assertEquals(77, chargeState?.batteryLevel)
        assertEquals("infotainment", delegate.events.last())
    }
}
