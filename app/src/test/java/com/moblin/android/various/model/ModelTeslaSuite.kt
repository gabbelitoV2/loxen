package com.moblin.android.various.model

import com.moblin.android.common.various.formatSpeed
import com.moblin.android.integrations.tesla.TeslaTestBench
import com.moblin.android.integrations.tesla.TeslaVehicle
import com.moblin.android.integrations.tesla.TeslaVehicleState
import com.moblin.android.integrations.tesla.protobuf.CarServer_ChargeState
import com.moblin.android.integrations.tesla.protobuf.CarServer_DriveState
import com.moblin.android.integrations.tesla.protobuf.CarServer_MediaState
import com.moblin.android.integrations.tesla.protobuf.CarServer_VehicleAction
import com.moblin.android.integrations.tesla.protobuf.CarServer_Void
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_Domain
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_MessageFault_E
import com.moblin.android.integrations.tesla.protobuf.VCSEC_ClosureMoveType_E
import com.moblin.android.integrations.tesla.protobuf.VCSEC_UnsignedMessage
import com.moblin.android.integrations.tesla.teslaGeneratePrivateKey
import com.moblin.android.integrations.tesla.teslaVin
import com.moblin.android.platform.corebluetooth.RecordingBluetoothGatt
import com.moblin.android.platform.cryptokit.P256
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [RecordingBluetoothGatt::class])
class ModelTeslaSuite {
    private val bench = TeslaTestBench()
    private val fake = bench.fake
    private lateinit var model: Model

    @Before
    fun setUp() {
        bench.setUp()
        model = Model()
        bench.runMain()
    }

    @After
    fun tearDown() {
        model.stopTeslaVehicle()
        bench.tearDown()
    }

    private fun configure() {
        model.database.tesla.enabled = true
        model.database.tesla.vin = teslaVin
        model.database.tesla.privateKey = teslaGeneratePrivateKey().pemRepresentation
        model.database.tesla.bluetoothPeripheralName = "S0123456789abcdefC"
        model.database.tesla.bluetoothPeripheralId = bench.peripheralId
    }

    private fun connected(): TeslaVehicle {
        configure()
        model.reloadTeslaVehicle()
        bench.runMain()
        bench.connectFake()
        assertEquals(TeslaVehicleState.connected, model.tesla.vehicleState.value)
        assertTrue(model.tesla.vehicleVehicleSecurityConnected.value)
        assertTrue(model.tesla.vehicleInfotainmentConnected.value)
        return assertNotNull(model.tesla.vehicle)
    }

    @Test
    fun nothingIsStartedUntilTheVehicleIsFullyConfigured() {
        model.reloadTeslaVehicle()
        bench.runMain()
        assertNull(model.tesla.vehicle)
        assertNull(model.tesla.vehicleState.value)
        configure()
        model.database.tesla.enabled = false
        model.reloadTeslaVehicle()
        assertNull(model.tesla.vehicle)
        configure()
        model.database.tesla.vin = ""
        model.reloadTeslaVehicle()
        assertNull(model.tesla.vehicle)
        configure()
        model.database.tesla.privateKey = ""
        model.reloadTeslaVehicle()
        assertNull(model.tesla.vehicle)
        configure()
        model.database.tesla.bluetoothPeripheralId = null
        model.reloadTeslaVehicle()
        assertNull(model.tesla.vehicle)
        bench.runMain()
        assertTrue(bench.gatts().isEmpty())
    }

    @Test
    fun aBrokenKeyLeavesTheVehicleIdle() {
        configure()
        model.database.tesla.privateKey = "broken"
        model.reloadTeslaVehicle()
        bench.runMain()
        assertNull(model.tesla.vehicle)
        assertEquals(TeslaVehicleState.idle, model.tesla.vehicleState.value)
        assertTrue(bench.gatts().isEmpty())
    }

    @Test
    fun theModelFollowsTheVehicleThroughConnectAndBothHandshakes() {
        configure()
        model.reloadTeslaVehicle()
        bench.runMain()
        assertEquals(TeslaVehicleState.connecting, model.tesla.vehicleState.value)
        assertFalse(model.tesla.vehicleVehicleSecurityConnected.value)
        fake.respondToSessionInfoRequests = false
        bench.connectFake()
        assertEquals(TeslaVehicleState.connected, model.tesla.vehicleState.value)
        assertEquals("Connected to your Tesla", model.toast.toast.value.title)
        assertFalse(model.tesla.vehicleVehicleSecurityConnected.value)
        fake.respondToSessionInfoRequests = true
        fake.respondToSessionInfoRequest(fake.sessionInfoRequests.single())
        bench.pump()
        assertTrue(model.tesla.vehicleVehicleSecurityConnected.value)
        assertTrue(model.tesla.vehicleInfotainmentConnected.value)
    }

    @Test
    fun aReconnectClearsTheHandshakeFlagsUntilTheyCompleteAgain() {
        connected()
        fake.respondToSessionInfoRequests = false
        fake.disconnect()
        bench.runMain()
        assertEquals(TeslaVehicleState.connecting, model.tesla.vehicleState.value)
        assertFalse(model.tesla.vehicleVehicleSecurityConnected.value)
        assertFalse(model.tesla.vehicleInfotainmentConnected.value)
        fake.respondToSessionInfoRequests = true
        bench.connectFake()
        assertEquals(TeslaVehicleState.connected, model.tesla.vehicleState.value)
        assertTrue(model.tesla.vehicleInfotainmentConnected.value)
    }

    @Test
    fun anIdleVehicleIsReplacedByANewOneThatConnectsAgain() {
        val first = connected()
        fake.nextFault = UniversalMessage_MessageFault_E.rrorBusy
        model.teslaHonk()
        bench.pump()
        val second = assertNotNull(model.tesla.vehicle)
        assertNotSame(first, second)
        assertEquals(TeslaVehicleState.connecting, model.tesla.vehicleState.value)
        assertFalse(model.tesla.vehicleInfotainmentConnected.value)
        assertEquals(2, bench.gatts().size)
        bench.connectFake()
        assertSame(second, model.tesla.vehicle)
        assertTrue(model.tesla.vehicleInfotainmentConnected.value)
        model.teslaHonk()
        bench.pump()
        assertEquals(UniversalMessage_MessageFault_E.rrorNone, fake.signedRequests.last().fault)
    }

    @Test
    fun theButtonsAndChatBotActionsReachTheVehicle() {
        connected()
        model.teslaHonk()
        model.teslaFlashLights()
        model.mediaNextTrack()
        model.mediaPreviousTrack()
        model.mediaTogglePlayback()
        model.teslaOpenTrunk()
        model.teslaCloseTrunk()
        bench.pump()
        val infotainment = fake.signedRequests.filter { it.domain == UniversalMessage_Domain.infotainment }
        assertEquals(5, infotainment.size)
        assertIs<CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlHonkHornAction>(infotainment[0].vehicleActionMsg())
        assertIs<CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlFlashLightsAction>(infotainment[1].vehicleActionMsg())
        assertIs<CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextTrack>(infotainment[2].vehicleActionMsg())
        assertIs<CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousTrack>(infotainment[3].vehicleActionMsg())
        assertIs<CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPlayAction>(infotainment[4].vehicleActionMsg())
        val vehicleSecurity = fake.signedRequests.filter { it.domain == UniversalMessage_Domain.vehicleSecurity }
        assertEquals(
            listOf(VCSEC_ClosureMoveType_E.closureMoveTypeOpen, VCSEC_ClosureMoveType_E.closureMoveTypeClose),
            vehicleSecurity.map { it.unsignedMessage().closureMoveRequest.rearTrunk },
        )
        assertTrue(fake.signedRequests.all { it.fault == UniversalMessage_MessageFault_E.rrorNone })
    }

    @Test
    fun addingTheKeyShowsTheInstructionsAndSendsTheSavedKey() {
        connected()
        model.teslaAddKeyToVehicle()
        bench.pump()
        assertEquals("Tap Locks → Add Key in your Tesla and tap your key card", model.toast.toast.value.title)
        val envelope = fake.addKeyRequests.single()
        val message = VCSEC_UnsignedMessage(serializedBytes = envelope.signedMessage.protobufMessageAsBytes)
        val expected = P256.KeyAgreement.PrivateKey(pemRepresentation = model.database.tesla.privateKey)
            .publicKey.x963Representation
        assertContentEquals(expected, message.whitelistOperation.addKeyToWhitelistAndAddPermissions.key.publicKeyRaw)
    }

    @Test
    fun pollingStoresTheStatesAndTheTextWidgetShowsThem() {
        connected()
        assertEquals("-", model.textEffectTeslaBatteryLevel())
        assertEquals("-", model.textEffectTeslaDrive())
        assertEquals("-", model.textEffectTeslaMedia())
        fake.chargeState.batteryLevel = 80
        fake.chargeState.chargerPower = 11
        fake.chargeState.minutesToChargeLimit = 45
        fake.driveState.shiftState.d = CarServer_Void()
        fake.driveState.speed = 30u
        fake.driveState.power = 25
        fake.mediaState.nowPlayingArtist = "Kraftwerk"
        fake.mediaState.nowPlayingTitle = "Autobahn"
        model.teslaGetChargeState()
        model.teslaGetDriveState()
        model.teslaGetMediaState()
        bench.pump()
        assertEquals(fake.chargeState, model.tesla.chargeState)
        assertEquals(fake.driveState, model.tesla.driveState)
        assertEquals(fake.mediaState, model.tesla.mediaState)
        assertEquals("80% 11 kW 45 minutes left", model.textEffectTeslaBatteryLevel())
        assertEquals("D ${formatSpeed(speed = 30 * 0.44704)} 25 kW", model.textEffectTeslaDrive())
        assertEquals("Kraftwerk - Autobahn", model.textEffectTeslaMedia())
    }

    @Test
    fun stoppingForgetsTheVehicleAndItsStates() {
        val vehicle = connected()
        val gatt = bench.gatt()
        fake.chargeState.batteryLevel = 70
        model.teslaGetChargeState()
        bench.pump()
        assertEquals(70, model.tesla.chargeState.batteryLevel)
        model.stopTeslaVehicle()
        bench.runMain()
        assertNull(model.tesla.vehicle)
        assertNull(model.tesla.vehicleState.value)
        assertFalse(model.tesla.vehicleVehicleSecurityConnected.value)
        assertFalse(model.tesla.vehicleInfotainmentConnected.value)
        assertEquals(CarServer_ChargeState(), model.tesla.chargeState)
        assertEquals(CarServer_DriveState(), model.tesla.driveState)
        assertEquals(CarServer_MediaState(), model.tesla.mediaState)
        assertTrue(bench.recording(gatt).isClosed)
        assertNull(vehicle.delegate)
        model.teslaHonk()
        assertEquals(1, bench.gatts().size)
    }

    @Test
    fun theBatteryTextFollowsWhichValuesTheVehicleSent() {
        val state = CarServer_ChargeState()
        model.tesla.chargeState = state
        assertEquals("-", model.textEffectTeslaBatteryLevel())
        state.batteryLevel = 0
        assertEquals("0%", model.textEffectTeslaBatteryLevel())
        state.batteryLevel = 64
        state.minutesToChargeLimit = 0
        assertEquals("64% 0 minutes left", model.textEffectTeslaBatteryLevel())
        state.chargerPower = 7
        assertEquals("64% 7 kW 0 minutes left", model.textEffectTeslaBatteryLevel())
        state.chargerPower = 0
        state.optionalMinutesToChargeLimit = null
        assertEquals("64%", model.textEffectTeslaBatteryLevel())
    }

    @Test
    fun theDriveTextShowsTheGearAndOnlyOutsideParkTheSpeedAndPower() {
        val state = CarServer_DriveState()
        model.tesla.driveState = state
        assertEquals("-", model.textEffectTeslaDrive())
        state.speed = 10u
        state.power = -5
        state.shiftState.p = CarServer_Void()
        assertEquals("P", model.textEffectTeslaDrive())
        val speed = formatSpeed(speed = 10 * 0.44704)
        state.shiftState.r = CarServer_Void()
        assertEquals("R $speed -5 kW", model.textEffectTeslaDrive())
        state.shiftState.n = CarServer_Void()
        assertEquals("N $speed -5 kW", model.textEffectTeslaDrive())
        state.shiftState.sna = CarServer_Void()
        assertEquals("SNA $speed -5 kW", model.textEffectTeslaDrive())
        state.shiftState.invalid = CarServer_Void()
        assertEquals("- $speed -5 kW", model.textEffectTeslaDrive())
        state.optionalSpeed = null
        state.optionalPower = null
        state.shiftState.d = CarServer_Void()
        assertEquals("D", model.textEffectTeslaDrive())
    }

    @Test
    fun theMediaTextNeedsBothArtistAndTitle() {
        val state = CarServer_MediaState()
        model.tesla.mediaState = state
        state.nowPlayingTitle = "Autobahn"
        assertEquals("-", model.textEffectTeslaMedia())
        state.nowPlayingArtist = ""
        assertEquals("Autobahn", model.textEffectTeslaMedia())
        state.nowPlayingArtist = "Kraftwerk"
        assertEquals("Kraftwerk - Autobahn", model.textEffectTeslaMedia())
    }
}
