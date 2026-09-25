package com.moblin.android.view.settings.gopro

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.moblin.android.LocalModel
import com.moblin.android.platform.corelocation.LocationAuthorization
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsGoProDevice
import com.moblin.android.various.settings.SettingsWiFi
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GoProBleDeviceSsidSuite {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        LocationAuthorization.reset()
    }

    @After
    fun tearDown() {
        LocationAuthorization.reset()
    }

    @Test
    fun theSsidAndItsSavedPasswordAreOnlyTakenOnSubmitLikeIos() {
        val model = Model()
        model.database.savedWifiNetworks = listOf(
            SettingsWiFi(ssid = "Home", password = "wrong"),
            SettingsWiFi(ssid = "Home5G", password = "right"),
        )
        val device = SettingsGoProDevice()
        rule.setContent {
            CompositionLocalProvider(LocalModel provides model) {
                GoProBleDeviceSettingsView(model = model, device = device)
            }
        }
        rule.onNodeWithText("SSID").performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextInput("Home")
        rule.waitForIdle()
        assertEquals("", device.wifiSsid)
        assertEquals("", device.wifiPassword)
        rule.onNode(hasSetTextAction()).performTextInput("5G")
        rule.onNode(hasSetTextAction()).performImeAction()
        rule.waitForIdle()
        assertEquals("Home5G", device.wifiSsid)
        assertEquals("right", device.wifiPassword)
        rule.onNode(hasSetTextAction()).assertDoesNotExist()
    }
}
