package com.moblin.android.integrations.tesla

import com.moblin.android.integrations.tesla.protobuf.CarServer_ChargeState
import com.moblin.android.integrations.tesla.protobuf.CarServer_DriveState
import com.moblin.android.integrations.tesla.protobuf.CarServer_MediaState
import com.moblin.android.integrations.tesla.protobuf.CarServer_ShiftState
import com.moblin.android.integrations.tesla.protobuf.CarServer_VehicleAction
import com.moblin.android.integrations.tesla.protobuf.CarServer_Void
import com.moblin.android.integrations.tesla.protobuf.Keys_Role
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_Domain
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_MessageFault_E
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_RoutableMessage
import com.moblin.android.integrations.tesla.protobuf.VCSEC_ClosureMoveType_E
import com.moblin.android.integrations.tesla.protobuf.VCSEC_KeyFormFactor
import com.moblin.android.integrations.tesla.protobuf.VCSEC_SignatureType
import com.moblin.android.integrations.tesla.protobuf.VCSEC_UnsignedMessage
import com.moblin.android.platform.corebluetooth.RecordingBluetoothGatt
import com.moblin.android.platform.cryptokit.P256
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
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
class TeslaVehicleSuite {
    private val bench = TeslaTestBench()
    private val fake = bench.fake
    private val delegate = TeslaVehicleEvents()
    private val privateKeyPem = teslaGeneratePrivateKey().pemRepresentation
    private val clientPublicKey = P256.KeyAgreement.PrivateKey(pemRepresentation = privateKeyPem)
        .publicKey.x963Representation
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

    private fun connectedVehicle(): TeslaVehicle {
        val vehicle = assertNotNull(
            TeslaVehicle(vin = teslaVin, privateKeyPem = privateKeyPem, peripheralId = bench.peripheralId),
        )
        this.vehicle = vehicle
        vehicle.delegate = delegate
        vehicle.start()
        bench.runMain()
        bench.connectFake()
        assertEquals(listOf("state connecting", "state connected", "vehicleSecurity", "infotainment"), delegate.events)
        return vehicle
    }

    private fun sessionInfo(domain: UniversalMessage_Domain) = fake.sentSessionInfos.last { info ->
        info.epoch.contentEquals(fake.epochs.getValue(domain))
    }

    private fun assertSigned(request: TeslaSignedRequest, domain: UniversalMessage_Domain, counter: UInt) {
        assertEquals(domain, request.domain)
        assertEquals(UniversalMessage_MessageFault_E.rrorNone, request.fault)
        assertEquals(counter, request.counter)
        assertContentEquals(fake.epochs.getValue(domain), request.epoch)
        assertContentEquals(clientPublicKey, request.message.signatureData.signerIdentity.publicKey)
        assertEquals(2u, request.message.flags)
        assertEquals(12, request.message.signatureData.aesGcmPersonalizedData.nonce.size)
        assertEquals(16, request.message.signatureData.aesGcmPersonalizedData.tag.size)
        assertEquals(16, request.message.uuid.size)
        assertEquals(16, request.message.fromDestination.routingAddress.size)
    }

    private fun assertStillConnected() {
        assertEquals("infotainment", delegate.events.last())
    }

    @Test
    fun honkFlashLightsAndMediaActionsAreSignedForInfotainment() {
        val vehicle = connectedVehicle()
        val counter = sessionInfo(UniversalMessage_Domain.infotainment).counter
        vehicle.honk()
        bench.pump()
        vehicle.flashLights()
        bench.pump()
        vehicle.mediaNextTrack()
        vehicle.mediaPreviousTrack()
        vehicle.mediaTogglePlayback()
        bench.pump()
        assertEquals(5, fake.signedRequests.size)
        fake.signedRequests.forEachIndexed { index, request ->
            assertSigned(request, UniversalMessage_Domain.infotainment, counter + 1u + index.toUInt())
        }
        val actions = fake.signedRequests.map { it.vehicleActionMsg() }
        assertIs<CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlHonkHornAction>(actions[0])
        assertIs<CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlFlashLightsAction>(actions[1])
        assertIs<CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextTrack>(actions[2])
        assertIs<CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousTrack>(actions[3])
        assertIs<CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPlayAction>(actions[4])
        assertStillConnected()
    }

    @Test
    fun theTrunkIsOpenedAndClosedThroughVehicleSecurity() {
        val vehicle = connectedVehicle()
        val counter = sessionInfo(UniversalMessage_Domain.vehicleSecurity).counter
        vehicle.openTrunk()
        bench.pump()
        vehicle.closeTrunk()
        bench.pump()
        assertEquals(2, fake.signedRequests.size)
        assertSigned(fake.signedRequests[0], UniversalMessage_Domain.vehicleSecurity, counter + 1u)
        assertSigned(fake.signedRequests[1], UniversalMessage_Domain.vehicleSecurity, counter + 2u)
        val open = fake.signedRequests[0].unsignedMessage()
        assertEquals(VCSEC_ClosureMoveType_E.closureMoveTypeOpen, open.closureMoveRequest.rearTrunk)
        assertEquals(VCSEC_ClosureMoveType_E.closureMoveTypeNone, open.closureMoveRequest.frontTrunk)
        assertEquals(
            VCSEC_ClosureMoveType_E.closureMoveTypeClose,
            fake.signedRequests[1].unsignedMessage().closureMoveRequest.rearTrunk,
        )
        assertStillConnected()
    }

    @Test
    fun theRequestExpiresFifteenSecondsAfterTheVehicleClock() {
        val vehicle = connectedVehicle()
        val clockTime = sessionInfo(UniversalMessage_Domain.infotainment).clockTime
        vehicle.honk()
        bench.pump()
        assertEquals(clockTime + 15u, fake.signedRequests[0].expiresAt)
        bench.advance(100)
        vehicle.honk()
        bench.pump()
        assertEquals(clockTime + 115u, fake.signedRequests[1].expiresAt)
        assertEquals(UniversalMessage_MessageFault_E.rrorNone, fake.signedRequests[1].fault)
        assertStillConnected()
    }

    @Test
    fun chargeDriveAndMediaStatesAreDecryptedFromTheResponses() {
        val vehicle = connectedVehicle()
        fake.chargeState.batteryLevel = 80
        fake.chargeState.chargerPower = 11
        fake.chargeState.minutesToChargeLimit = 45
        fake.driveState.shiftState.d = CarServer_Void()
        fake.driveState.speed = 30u
        fake.driveState.power = 25
        fake.mediaState.nowPlayingArtist = "Kraftwerk"
        fake.mediaState.nowPlayingTitle = "Autobahn"
        fake.responseFlags = 0x10u
        var chargeState: CarServer_ChargeState? = null
        var driveState: CarServer_DriveState? = null
        var mediaState: CarServer_MediaState? = null
        vehicle.getChargeState { chargeState = it }
        bench.pump()
        vehicle.getDriveState { driveState = it }
        bench.pump()
        vehicle.getMediaState { mediaState = it }
        bench.pump()
        assertEquals(fake.chargeState, chargeState)
        assertEquals(80, chargeState!!.batteryLevel)
        assertEquals(fake.driveState, driveState)
        assertIs<CarServer_ShiftState.OneOf_Type.d>(driveState!!.shiftState.type)
        assertEquals(30u, driveState!!.speed)
        assertEquals(fake.mediaState, mediaState)
        assertEquals("Autobahn", mediaState!!.nowPlayingTitle)
        val requested = fake.signedRequests.map { request ->
            val getVehicleData = request.carServerAction().vehicleAction.getVehicleData
            listOf(getVehicleData.hasGetChargeState, getVehicleData.hasGetDriveState, getVehicleData.hasGetMediaState)
        }
        assertEquals(
            listOf(listOf(true, false, false), listOf(false, true, false), listOf(false, false, true)),
            requested,
        )
        assertStillConnected()
    }

    @Test
    fun responsesSplitIntoTinyNotificationsOrSentInOneAreReassembled() {
        fake.notificationLength = 3
        val vehicle = connectedVehicle()
        fake.chargeState.batteryLevel = 55
        var chargeState: CarServer_ChargeState? = null
        vehicle.getChargeState { chargeState = it }
        bench.pump()
        assertEquals(55, chargeState?.batteryLevel)
        fake.notificationLength = 512
        fake.chargeState.batteryLevel = 56
        vehicle.getChargeState { chargeState = it }
        bench.pump()
        assertEquals(56, chargeState?.batteryLevel)
        assertStillConnected()
    }

    @Test
    fun actionsBeforeTheSessionAreDroppedLikeIos() {
        val vehicle = assertNotNull(
            TeslaVehicle(vin = teslaVin, privateKeyPem = privateKeyPem, peripheralId = bench.peripheralId),
        )
        this.vehicle = vehicle
        vehicle.delegate = delegate
        vehicle.start()
        bench.runMain()
        vehicle.honk()
        vehicle.openTrunk()
        fake.respondToSessionInfoRequests = false
        bench.connectFake()
        vehicle.honk()
        vehicle.openTrunk()
        bench.pump()
        assertTrue(fake.signedRequests.isEmpty())
        fake.respondToSessionInfoRequests = true
        fake.respondToSessionInfoRequest(fake.sessionInfoRequests.single())
        bench.pump()
        assertEquals(listOf("state connecting", "state connected", "vehicleSecurity", "infotainment"), delegate.events)
        assertTrue(fake.signedRequests.isEmpty())
        vehicle.honk()
        bench.pump()
        assertEquals(1, fake.signedRequests.size)
    }

    @Test
    fun theAddKeyRequestAsksTheVehicleForAnOwnerCloudKey() {
        val vehicle = connectedVehicle()
        val newKey = teslaGeneratePrivateKey()
        val newPublicKey = newKey.publicKey.x963Representation
        vehicle.addKeyRequestWithRole(privateKeyPem = newKey.pemRepresentation)
        bench.pump()
        val expected = hexBytes("0a5412508201" + "4d" + "2a47" + "0a43" + "0a41") + newPublicKey +
            hexBytes("2002" + "32020809" + "1802")
        assertEquals(expected.hex(), fake.frames.last().hex())
        val envelope = fake.addKeyRequests.single()
        assertEquals(VCSEC_SignatureType.presentKey, envelope.signedMessage.signatureType)
        val message = VCSEC_UnsignedMessage(serializedBytes = envelope.signedMessage.protobufMessageAsBytes)
        val permissionChange = message.whitelistOperation.addKeyToWhitelistAndAddPermissions
        assertContentEquals(newPublicKey, permissionChange.key.publicKeyRaw)
        assertEquals(Keys_Role.owner, permissionChange.keyRole)
        assertEquals(VCSEC_KeyFormFactor.cloudKey, message.whitelistOperation.metadataForKey.keyFormFactor)
        assertTrue(fake.signedRequests.isEmpty())
        vehicle.addKeyRequestWithRole(privateKeyPem = "broken")
        bench.pump()
        assertEquals(1, fake.addKeyRequests.size)
        assertStillConnected()
    }

    @Test
    fun aFaultResponseResetsTheVehicleLikeIos() {
        val vehicle = connectedVehicle()
        val gatt = bench.gatt()
        fake.nextFault = UniversalMessage_MessageFault_E.rrorBusy
        vehicle.honk()
        bench.pump()
        assertEquals(UniversalMessage_MessageFault_E.rrorBusy, fake.signedRequests.single().fault)
        assertEquals("state idle", delegate.events.last())
        assertTrue(bench.recording(gatt).isClosed)
        vehicle.honk()
        bench.pump()
        assertEquals(1, fake.signedRequests.size)
    }

    @Test
    fun aVehicleSecurityFaultResetsTheVehicleLikeIos() {
        val vehicle = connectedVehicle()
        fake.nextFault = UniversalMessage_MessageFault_E.rrorRemoteAccessDisabled
        vehicle.openTrunk()
        bench.pump()
        assertEquals("state idle", delegate.events.last())
    }

    @Test
    fun aResponseThatFailsAuthenticationResetsTheVehicle() {
        val vehicle = connectedVehicle()
        fake.tamperWithNextResponse = true
        var chargeState: CarServer_ChargeState? = null
        vehicle.getChargeState { chargeState = it }
        bench.pump()
        assertNull(chargeState)
        assertEquals(UniversalMessage_MessageFault_E.rrorNone, fake.signedRequests.single().fault)
        assertEquals("state idle", delegate.events.last())
    }

    @Test
    fun anExpiredSessionNeedsANewHandshakeWithTheNewEpoch() {
        val vehicle = connectedVehicle()
        vehicle.honk()
        bench.pump()
        val oldEpoch = fake.epochs.getValue(UniversalMessage_Domain.infotainment)
        fake.rotateEpoch(UniversalMessage_Domain.infotainment)
        vehicle.honk()
        bench.pump()
        assertEquals(UniversalMessage_MessageFault_E.rrorIncorrectEpoch, fake.signedRequests[1].fault)
        assertContentEquals(oldEpoch, fake.signedRequests[1].epoch)
        assertEquals("state idle", delegate.events.last())
        vehicle.start()
        bench.runMain()
        bench.connectFake()
        assertEquals(listOf("state connecting", "state connected", "vehicleSecurity", "infotainment"), delegate.events.takeLast(4))
        vehicle.honk()
        bench.pump()
        val request = fake.signedRequests.last()
        assertSigned(request, UniversalMessage_Domain.infotainment, fake.signedRequests[0].counter + 1u)
        assertStillConnected()
    }

    @Test
    fun aVehicleClockThatMovedOnMakesTheRequestExpire() {
        val vehicle = connectedVehicle()
        fake.clockBase += 3600u
        vehicle.honk()
        bench.pump()
        assertEquals(UniversalMessage_MessageFault_E.rrorTimeExpired, fake.signedRequests.single().fault)
        assertEquals("state idle", delegate.events.last())
    }

    @Test
    fun aResponseToAnUnknownAddressIsIgnored() {
        connectedVehicle()
        val message = UniversalMessage_RoutableMessage()
        message.toDestination.routingAddress = ByteArray(16) { it.toByte() }
        message.fromDestination.domain = UniversalMessage_Domain.infotainment
        message.signedMessageStatus.signedMessageFault = UniversalMessage_MessageFault_E.rrorBusy
        fake.send(message)
        bench.pump()
        assertStillConnected()
    }

    @Test
    fun aMessageThatIsNotProtobufResetsTheVehicle() {
        connectedVehicle()
        fake.sendFrame(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))
        bench.pump()
        assertEquals("state idle", delegate.events.last())
    }

    @Test
    fun theSessionInfoTagIsNotCheckedLikeIos() {
        fake.tamperWithSessionInfoTag = true
        val vehicle = connectedVehicle()
        vehicle.honk()
        bench.pump()
        assertEquals(UniversalMessage_MessageFault_E.rrorNone, fake.signedRequests.single().fault)
    }
}
