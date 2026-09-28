package com.moblin.android.view.utils

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.platform.corelocation.LocationAuthorization
import com.moblin.android.platform.networkextension.FakeWifi
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WiFiSsidEditViewSuite {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var application: Application
    private lateinit var wifi: FakeWifi
    private var value by mutableStateOf("")
    private val submitted = mutableListOf<String>()
    private var dismissed = 0
    private val locationWarning = "Allow Loxen to access your location"

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        wifi = FakeWifi(application)
        LocationAuthorization.reset()
        LocationAuthorization.install(rule.activity)
    }

    @After
    fun tearDown() {
        LocationAuthorization.reset()
    }

    private fun show(initial: String) {
        value = initial
        rule.setContent {
            WiFiSsidEditView(
                value = value,
                onValueChange = { value = it },
                onSubmit = { submitted.add(it) },
                onDismiss = { dismissed += 1 },
            )
        }
        rule.waitForIdle()
    }

    private fun answerLocationRequest(granted: Boolean) {
        val request = shadowOf(rule.activity).lastRequestedPermission
        val result = if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        if (granted) {
            shadowOf(application).grantPermissions(*request.requestedPermissions)
        }
        @Suppress("DEPRECATION")
        rule.activity.onRequestPermissionsResult(
            request.requestCode,
            request.requestedPermissions,
            IntArray(request.requestedPermissions.size) { result },
        )
        rule.waitForIdle()
    }

    private fun currentNetworkRow(ssid: String) =
        rule.onNode(hasText(ssid) and hasClickAction() and !hasSetTextAction(), useUnmergedTree = false)

    @Test
    fun theCurrentNetworkIsOfferedAndFillsAnEmptyField() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        wifi.connect()
        show(initial = "")
        assertEquals(1, wifi.answer("Home"))
        rule.waitForIdle()
        rule.onNodeWithText("Current network", ignoreCase = true).assertExists()
        rule.onNodeWithText("The WiFi network this device is currently connected to.").assertExists()
        assertEquals("Home", value)
        assertNull(shadowOf(rule.activity).lastRequestedPermission)
    }

    @Test
    fun tappingTheCurrentNetworkSubmitsItAndDismisses() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        wifi.connect()
        show(initial = "Office")
        wifi.answer("Home")
        rule.waitForIdle()
        assertEquals("Office", value)
        currentNetworkRow("Home").performClick()
        rule.waitForIdle()
        assertEquals(listOf("Home"), submitted)
        assertEquals("Home", value)
        assertEquals(1, dismissed)
    }

    @Test
    fun withoutWifiNothingIsOffered() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        show(initial = "")
        assertEquals(0, wifi.pendingRequests().size)
        rule.onNodeWithText("Current network", ignoreCase = true).assertDoesNotExist()
        rule.onNodeWithText(locationWarning, substring = true).assertDoesNotExist()
        assertEquals("", value)
    }

    @Test
    fun locationIsAskedForOnceAndAGrantShowsTheCurrentNetwork() {
        wifi.connect()
        show(initial = "")
        val request = shadowOf(rule.activity).lastRequestedPermission
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            request.requestedPermissions.toList(),
        )
        assertEquals(0, wifi.pendingRequests().size)
        answerLocationRequest(granted = true)
        assertEquals(1, wifi.answer("Home"))
        rule.waitForIdle()
        rule.onNodeWithText("Current network", ignoreCase = true).assertExists()
        assertEquals("Home", value)
    }

    @Test
    fun aDeniedLocationShowsTheWarningInsteadOfTheNetwork() {
        wifi.connect()
        show(initial = "")
        answerLocationRequest(granted = false)
        rule.onNodeWithText(locationWarning, substring = true).assertExists()
        rule.onNodeWithText("Current network", ignoreCase = true).assertDoesNotExist()
        assertEquals(0, wifi.pendingRequests().size)
        assertEquals("", value)
    }
}
