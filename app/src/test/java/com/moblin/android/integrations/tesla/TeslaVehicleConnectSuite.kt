package com.moblin.android.integrations.tesla

import android.bluetooth.BluetoothAdapter
import com.moblin.android.integrations.tesla.protobuf.CarServer_VehicleAction
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_Domain
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_MessageFault_E
import com.moblin.android.platform.corebluetooth.RecordingBluetoothGatt
import com.moblin.android.platform.cryptokit.P256
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [RecordingBluetoothGatt::class])
class TeslaVehicleConnectSuite {
    private val bench = TeslaTestBench()
    private val fake = bench.fake
    private val delegate = TeslaVehicleEvents()
    private val privateKeyPem = teslaGeneratePrivateKey().pemRepresentation
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

    private fun startVehicle(pem: String = privateKeyPem): TeslaVehicle {
        val vehicle = assertNotNull(TeslaVehicle(vin = teslaVin, privateKeyPem = pem, peripheralId = bench.peripheralId))
        this.vehicle = vehicle
        vehicle.delegate = delegate
        vehicle.start()
        bench.runMain()
        return vehicle
    }

    private fun clientPublicKey(pem: String = privateKeyPem): ByteArray {
        return P256.KeyAgreement.PrivateKey(pemRepresentation = pem).publicKey.x963Representation
    }

    @Test
    fun theVehicleIsConnectedAndBothHandshakesComplete() {
        startVehicle()
        assertEquals(listOf("state connecting"), delegate.events)
        assertEquals(1, bench.gatts().size)
        fake.connect(bench.gatt()) { bench.runMain() }
        assertEquals(listOf("state connecting", "state connected"), delegate.events)
        assertEquals(bench.connectionCalls(), bench.recording().calls)
        bench.pump()
        assertEquals(
            listOf("state connecting", "state connected", "vehicleSecurity", "infotainment"),
            delegate.events,
        )
        assertEquals(
            listOf(UniversalMessage_Domain.vehicleSecurity, UniversalMessage_Domain.infotainment),
            fake.sessionInfoRequests.map { it.toDestination.domain },
        )
        for (request in fake.sessionInfoRequests) {
            assertContentEquals(clientPublicKey(), request.sessionInfoRequest.publicKey)
            assertEquals(65, request.sessionInfoRequest.publicKey.size)
            assertEquals(16, request.fromDestination.routingAddress.size)
            assertEquals(16, request.uuid.size)
            assertFalse(request.hasSignedMessageStatus)
            assertEquals(0u, request.flags)
        }
        assertFalse(
            fake.sessionInfoRequests[0].fromDestination.routingAddress
                .contentEquals(fake.sessionInfoRequests[1].fromDestination.routingAddress),
        )
    }

    @Test
    fun requestsAreFramedWithABigEndianLengthAndWrittenIn20ByteBlocksWithResponse() {
        startVehicle()
        bench.connectFake()
        val toVehicle = bench.uuidString(teslaToVehicleId)
        val writes = fake.writes.filter { it.startsWith("writeCharacteristic") }
        assertTrue(writes.all { it.startsWith("writeCharacteristic $toVehicle ") && it.endsWith(" 2") })
        val blocks = writes.map { hexBytes(it.split(" ")[2]) }
        val expected = fake.frames.flatMap { frame ->
            val data = byteArrayOf((frame.size shr 8).toByte(), frame.size.toByte()) + frame
            data.indices.step(20).map { data.copyOfRange(it, minOf(it + 20, data.size)) }
        }
        assertEquals(expected.map { it.hex() }, blocks.map { it.hex() })
        assertEquals(2, fake.frames.size)
        assertTrue(fake.frames[0].size > 20)
    }

    @Test
    fun theHandshakeIsSentAgainEveryTenSecondsUntilTheVehicleAnswers() {
        fake.respondToSessionInfoRequests = false
        startVehicle()
        bench.connectFake()
        assertEquals(1, fake.sessionInfoRequests.size)
        bench.advance(9)
        bench.pump()
        assertEquals(1, fake.sessionInfoRequests.size)
        bench.advance(1)
        bench.pump()
        assertEquals(2, fake.sessionInfoRequests.size)
        assertEquals(UniversalMessage_Domain.vehicleSecurity, fake.sessionInfoRequests[1].toDestination.domain)
        assertFalse(
            fake.sessionInfoRequests[0].fromDestination.routingAddress
                .contentEquals(fake.sessionInfoRequests[1].fromDestination.routingAddress),
        )
        fake.respondToSessionInfoRequests = true
        fake.respondToSessionInfoRequest(fake.sessionInfoRequests[1])
        bench.pump()
        assertEquals(
            listOf("state connecting", "state connected", "vehicleSecurity", "infotainment"),
            delegate.events,
        )
        bench.advance(30)
        bench.pump()
        assertEquals(3, fake.sessionInfoRequests.size)
    }

    @Test
    fun theInfotainmentHandshakeIsRetriedOnItsOwnTimer() {
        fake.respondToSessionInfoRequests = false
        startVehicle()
        bench.connectFake()
        fake.respondToSessionInfoRequest(fake.sessionInfoRequests[0])
        bench.pump()
        assertEquals(listOf("state connecting", "state connected", "vehicleSecurity"), delegate.events)
        assertEquals(
            listOf(UniversalMessage_Domain.vehicleSecurity, UniversalMessage_Domain.infotainment),
            fake.sessionInfoRequests.map { it.toDestination.domain },
        )
        bench.advance(10)
        bench.pump()
        assertEquals(3, fake.sessionInfoRequests.size)
        assertEquals(UniversalMessage_Domain.infotainment, fake.sessionInfoRequests.last().toDestination.domain)
        fake.respondToSessionInfoRequest(fake.sessionInfoRequests.last())
        bench.pump()
        assertEquals("infotainment", delegate.events.last())
        bench.advance(30)
        bench.pump()
        assertEquals(3, fake.sessionInfoRequests.size)
    }

    @Test
    fun aLostConnectionIsReconnectedByTheShimAndBothHandshakesRunAgain() {
        val vehicle = startVehicle()
        bench.connectFake()
        val first = bench.gatt()
        fake.disconnect()
        bench.runMain()
        assertEquals("state connecting", delegate.events.last())
        assertTrue(bench.recording(first).isClosed)
        assertEquals(2, bench.gatts().size)
        bench.connectFake()
        assertEquals(
            listOf(
                "state connecting",
                "state connected",
                "vehicleSecurity",
                "infotainment",
                "state connecting",
                "state connected",
                "vehicleSecurity",
                "infotainment",
            ),
            delegate.events,
        )
        assertEquals(4, fake.sessionInfoRequests.size)
        vehicle.honk()
        bench.pump()
        val request = fake.signedRequests.single()
        assertEquals(UniversalMessage_MessageFault_E.rrorNone, request.fault)
        assertEquals(fake.sentSessionInfos.last().counter + 1u, request.counter)
        assertIs<CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlHonkHornAction>(request.vehicleActionMsg())
        assertEquals("infotainment", delegate.events.last())
    }

    @Test
    fun aResponseToTheLostConnectionIsIgnoredAfterReconnecting() {
        startVehicle()
        fake.respondToSessionInfoRequests = false
        bench.connectFake()
        val stale = fake.sessionInfoRequests.single()
        fake.disconnect()
        bench.runMain()
        bench.connectFake()
        assertEquals(2, fake.sessionInfoRequests.size)
        fake.respondToSessionInfoRequest(stale)
        bench.pump()
        assertEquals(listOf("state connecting", "state connected", "state connecting", "state connected"), delegate.events)
        fake.respondToSessionInfoRequest(fake.sessionInfoRequests[1])
        bench.pump()
        assertEquals("vehicleSecurity", delegate.events.last())
    }

    @Test
    fun bluetoothTurnedOffAndOnConnectsAgain() {
        startVehicle()
        bench.connectFake()
        val first = bench.gatt()
        bench.setAdapterState(BluetoothAdapter.STATE_OFF)
        assertTrue(bench.recording(first).isClosed)
        bench.setAdapterState(BluetoothAdapter.STATE_ON)
        assertEquals("state connecting", delegate.events.last())
        assertEquals(2, bench.gatts().size)
        bench.connectFake()
        assertEquals(listOf("state connected", "vehicleSecurity", "infotainment"), delegate.events.takeLast(3))
    }

    @Test
    fun stopClosesTheConnectionAndIgnoresLateResponses() {
        val vehicle = startVehicle()
        bench.connectFake()
        val gatt = bench.gatt()
        vehicle.stop()
        bench.runMain()
        assertEquals("state idle", delegate.events.last())
        assertTrue(bench.recording(gatt).isClosed)
        val count = delegate.events.size
        fake.respondToSessionInfoRequest(fake.sessionInfoRequests[0])
        bench.runMain()
        assertEquals(count, delegate.events.size)
        vehicle.honk()
        bench.pump()
        assertTrue(fake.signedRequests.isEmpty())
    }

    @Test
    fun anUnknownVehicleIsNeverConnected() {
        val vehicle = assertNotNull(
            TeslaVehicle(vin = teslaVin, privateKeyPem = privateKeyPem, peripheralId = UUID.randomUUID()),
        )
        this.vehicle = vehicle
        vehicle.delegate = delegate
        vehicle.start()
        bench.runMain()
        assertTrue(delegate.events.isEmpty())
        assertTrue(bench.gatts().isEmpty())
    }

    @Test
    fun anInvalidPrivateKeyGivesNoVehicle() {
        assertNull(TeslaVehicle(vin = teslaVin, privateKeyPem = "", peripheralId = bench.peripheralId))
        assertNull(TeslaVehicle(vin = teslaVin, privateKeyPem = "not a key", peripheralId = bench.peripheralId))
        val publicPem = P256.KeyAgreement.PrivateKey().publicKey.pemRepresentation
        assertNull(TeslaVehicle(vin = teslaVin, privateKeyPem = publicPem, peripheralId = bench.peripheralId))
    }

    @Test
    fun keysSavedByEarlierAndroidBuildsAreStillUsed() {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = generator.generateKeyPair()
        val encoded = Base64.getEncoder().encodeToString(keyPair.private.encoded).chunked(64).joinToString("\n")
        val pem = "-----BEGIN PRIVATE KEY-----\n$encoded\n-----END PRIVATE KEY-----\n"
        startVehicle(pem)
        bench.connectFake()
        assertEquals("infotainment", delegate.events.last())
        val expected = uncompressedPublicKey(keyPair.public as ECPublicKey)
        assertContentEquals(expected, fake.sessionInfoRequests[0].sessionInfoRequest.publicKey)
    }

    @Test
    fun aSec1PrivateKeyIsAccepted() {
        val raw = P256.KeyAgreement.PrivateKey().rawRepresentation
        val publicKey = P256.KeyAgreement.PrivateKey(rawRepresentation = raw).publicKey.x963Representation
        val der = hexBytes("30770201010420") + raw + hexBytes("a00a06082a8648ce3d030107a144034200") + publicKey
        val body = Base64.getEncoder().encodeToString(der).chunked(64).joinToString("\n")
        val pem = "-----BEGIN EC PRIVATE KEY-----\n$body\n-----END EC PRIVATE KEY-----"
        startVehicle(pem)
        bench.connectFake()
        assertContentEquals(publicKey, fake.sessionInfoRequests[0].sessionInfoRequest.publicKey)
        assertEquals("infotainment", delegate.events.last())
    }
}
