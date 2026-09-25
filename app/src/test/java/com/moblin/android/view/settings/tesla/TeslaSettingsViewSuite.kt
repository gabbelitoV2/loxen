package com.moblin.android.view.settings.tesla

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.moblin.android.LocalModel
import com.moblin.android.integrations.tesla.TeslaTestBench
import com.moblin.android.integrations.tesla.TeslaVehicleState
import com.moblin.android.integrations.tesla.protobuf.CarServer_VehicleAction
import com.moblin.android.integrations.tesla.protobuf.UniversalMessage_Domain
import com.moblin.android.integrations.tesla.protobuf.VCSEC_ClosureMoveType_E
import com.moblin.android.integrations.tesla.protobuf.VCSEC_UnsignedMessage
import com.moblin.android.integrations.tesla.teslaVin
import com.moblin.android.platform.corebluetooth.RecordingBluetoothGatt
import com.moblin.android.platform.cryptokit.P256
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.reloadTeslaVehicle
import com.moblin.android.various.model.stopTeslaVehicle
import java.util.Base64
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [RecordingBluetoothGatt::class], qualifiers = "w400dp-h2000dp")
class TeslaSettingsViewSuite {
    @get:Rule
    val rule = createComposeRule()

    private val bench = TeslaTestBench()
    private val fake = bench.fake
    private lateinit var model: Model

    @Before
    fun setUp() {
        bench.setUp()
        model = Model()
        model.database.tesla.vin = teslaVin
        model.database.tesla.bluetoothPeripheralName = "S0123456789abcdefC"
        model.database.tesla.bluetoothPeripheralId = bench.peripheralId
    }

    @After
    fun tearDown() {
        model.stopTeslaVehicle()
        bench.tearDown()
    }

    private fun show() {
        rule.setContent {
            CompositionLocalProvider(LocalModel provides model) {
                TeslaSettingsView(model = model, tesla = model.tesla)
            }
        }
        rule.waitForIdle()
    }

    private fun button(title: String) = rule.onNode(hasText(title) and hasClickAction())

    private fun openConfiguration() {
        rule.onNodeWithText("Configuration").performClick()
        rule.waitForIdle()
    }

    @Test
    fun generatingANewKeyStoresItInApplesPemFormatAndStartsTheVehicle() {
        show()
        rule.onNodeWithText("Disconnected").assertExists()
        openConfiguration()
        button("Generate new key").performClick()
        rule.waitForIdle()
        val pem = model.database.tesla.privateKey
        val lines = pem.split("\n")
        assertEquals("-----BEGIN PRIVATE KEY-----", lines.first())
        assertEquals("-----END PRIVATE KEY-----", lines.last())
        assertTrue(lines.drop(1).dropLast(1).dropLast(1).all { it.length == 64 })
        assertTrue(lines[lines.size - 2].length in 1..64)
        assertTrue(!pem.endsWith("\n"))
        val der = Base64.getDecoder().decode(lines.drop(1).dropLast(1).joinToString(""))
        assertEquals(138, der.size)
        assertEquals("308187020100301306072a8648ce3d020106082a8648ce3d030107046d306b0201010420", der.copyOf(36).hex())
        val key = P256.KeyAgreement.PrivateKey(pemRepresentation = pem)
        assertEquals(key.pemRepresentation, pem)
        assertContentEquals(key.publicKey.x963Representation, der.copyOfRange(der.size - 65, der.size))
        val first = assertNotNull(model.tesla.vehicle)
        bench.runMain()
        assertEquals(TeslaVehicleState.connecting, model.tesla.vehicleState.value)
        button("Generate new key").performClick()
        rule.waitForIdle()
        assertNotEquals(pem, model.database.tesla.privateKey)
        assertNotSame(first, model.tesla.vehicle)
    }

    @Test
    fun theKeyCanOnlyBeAddedToAConnectedVehicle() {
        show()
        openConfiguration()
        button("Add key to vehicle").assertIsNotEnabled()
        button("Generate new key").performClick()
        rule.waitForIdle()
        bench.runMain()
        button("Add key to vehicle").assertIsNotEnabled()
        bench.connectFake()
        rule.waitForIdle()
        assertEquals(TeslaVehicleState.connected, model.tesla.vehicleState.value)
        button("Add key to vehicle").assertIsEnabled()
        button("Add key to vehicle").performClick()
        rule.waitForIdle()
        bench.pump()
        assertEquals("Tap Locks → Add Key in your Tesla and tap your key card", model.toast.toast.value.title)
        val envelope = fake.addKeyRequests.single()
        val message = VCSEC_UnsignedMessage(serializedBytes = envelope.signedMessage.protobufMessageAsBytes)
        val expected = P256.KeyAgreement.PrivateKey(pemRepresentation = model.database.tesla.privateKey)
            .publicKey.x963Representation
        assertContentEquals(expected, message.whitelistOperation.addKeyToWhitelistAndAddPermissions.key.publicKeyRaw)
    }

    @Test
    fun theActionsAreEnabledByTheirHandshakesAndReachTheVehicle() {
        model.database.tesla.privateKey = P256.KeyAgreement.PrivateKey().pemRepresentation
        model.reloadTeslaVehicle()
        show()
        bench.runMain()
        rule.waitForIdle()
        rule.onNodeWithText("Connecting").assertExists()
        button("Honk").assertIsNotEnabled()
        button("Open trunk").assertIsNotEnabled()
        fake.respondToSessionInfoRequests = false
        bench.connectFake()
        rule.waitForIdle()
        rule.onNodeWithText("Connected").assertExists()
        button("Honk").assertIsNotEnabled()
        button("Open trunk").assertIsNotEnabled()
        fake.respondToSessionInfoRequests = true
        fake.respondToSessionInfoRequest(fake.sessionInfoRequests.single())
        bench.pump()
        rule.waitForIdle()
        for (title in listOf("Flash lights", "Honk", "Open trunk", "Close trunk", "Next media track", "Previous media track", "Toggle media playback")) {
            button(title).assertIsEnabled()
            button(title).performClick()
            rule.waitForIdle()
            bench.pump()
        }
        val infotainment = fake.signedRequests.filter { it.domain == UniversalMessage_Domain.infotainment }
        assertIs<CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlFlashLightsAction>(infotainment[0].vehicleActionMsg())
        assertIs<CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlHonkHornAction>(infotainment[1].vehicleActionMsg())
        assertIs<CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextTrack>(infotainment[2].vehicleActionMsg())
        assertIs<CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousTrack>(infotainment[3].vehicleActionMsg())
        assertIs<CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPlayAction>(infotainment[4].vehicleActionMsg())
        val vehicleSecurity = fake.signedRequests.filter { it.domain == UniversalMessage_Domain.vehicleSecurity }
        assertEquals(
            listOf(VCSEC_ClosureMoveType_E.closureMoveTypeOpen, VCSEC_ClosureMoveType_E.closureMoveTypeClose),
            vehicleSecurity.map { it.unsignedMessage().closureMoveRequest.rearTrunk },
        )
    }

    private fun ByteArray.hex(): String = joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}
