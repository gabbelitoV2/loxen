package com.moblin.android.platform.uikit

import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Looper
import androidx.activity.ComponentActivity
import com.moblin.android.MoblinApp
import com.moblin.android.various.model.Model
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class InterfaceOrientationSuite {
    private lateinit var controller: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var model: Model

    @Before
    fun setUp() {
        model = Model()
        MoblinApp.globalModel = model
        controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        activity = controller.get()
        InterfaceOrientation.install(activity)
        model.stream.value.portrait = false
        model.database.portrait = false
    }

    @After
    fun tearDown() {
        InterfaceOrientation.uninstall()
        if (!activity.isDestroyed) {
            controller.pause().stop().destroy()
        }
        shadowOf(Looper.getMainLooper()).idle()
        MoblinApp.globalModel = null
    }

    private fun turnWindow(orientation: Int) {
        val configuration = Configuration(activity.resources.configuration)
        configuration.orientation = orientation
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(configuration, activity.resources.displayMetrics)
        activity.onConfigurationChanged(configuration)
    }

    @Test
    fun landscapeRequestsBothLandscapeSides() {
        model.setDisplayPortrait(portrait = false)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, activity.requestedOrientation)
    }

    @Test
    fun portraitUiRequestsPortrait() {
        model.setDisplayPortrait(portrait = true)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
    }

    @Test
    fun portraitStreamRequestsPortrait() {
        model.stream.value.portrait = true
        model.updateOrientationLock()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
        model.stream.value.portrait = false
        model.updateOrientationLock()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, activity.requestedOrientation)
    }

    @Test
    fun layoutFollowsTheWindowWhenTheLockIsIgnored() {
        turnWindow(Configuration.ORIENTATION_PORTRAIT)
        model.setDisplayPortrait(portrait = false)
        assertTrue(model.orientation.isPortrait.value)
        turnWindow(Configuration.ORIENTATION_LANDSCAPE)
        assertFalse(model.orientation.isPortrait.value)
        turnWindow(Configuration.ORIENTATION_PORTRAIT)
        assertTrue(model.orientation.isPortrait.value)
    }

    @Test
    fun portraitUiInALandscapeWindowUsesTheLandscapeLayout() {
        turnWindow(Configuration.ORIENTATION_LANDSCAPE)
        model.setDisplayPortrait(portrait = true)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
        assertFalse(model.orientation.isPortrait.value)
        turnWindow(Configuration.ORIENTATION_PORTRAIT)
        assertTrue(model.orientation.isPortrait.value)
    }

    @Test
    fun withoutAWindowTheRequestedOrientationIsUsed() {
        InterfaceOrientation.uninstall()
        assertTrue(InterfaceOrientation.isPortrait(requested = true))
        assertFalse(InterfaceOrientation.isPortrait(requested = false))
        assertTrue(InterfaceOrientation.isPortrait(Configuration.ORIENTATION_UNDEFINED, requested = true))
        turnWindow(Configuration.ORIENTATION_LANDSCAPE)
        model.setDisplayPortrait(portrait = true)
        assertTrue(model.orientation.isPortrait.value)
    }

    @Test
    fun pictureInPictureKeepsTheLayout() {
        model.setDisplayPortrait(portrait = false)
        assertTrue(model.orientation.isPortrait.value)
        assertTrue(activity.enterPictureInPictureMode(PictureInPictureParams.Builder().build()))
        turnWindow(Configuration.ORIENTATION_LANDSCAPE)
        assertTrue(model.orientation.isPortrait.value)
        val shadow = shadowOf(activity)
        val field = shadow.javaClass.getDeclaredField("isInPictureInPictureMode")
        field.isAccessible = true
        field.setBoolean(shadow, false)
        turnWindow(Configuration.ORIENTATION_LANDSCAPE)
        assertFalse(model.orientation.isPortrait.value)
    }

    @Test
    fun mainActivityTurnsAndResizesWithoutBeingRecreated() {
        val info = activity.packageManager.getActivityInfo(
            ComponentName(activity, "com.moblin.android.MainActivity"),
            0,
        )
        val needed = ActivityInfo.CONFIG_ORIENTATION or ActivityInfo.CONFIG_SCREEN_SIZE or
            ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE or ActivityInfo.CONFIG_SCREEN_LAYOUT or
            ActivityInfo.CONFIG_DENSITY
        assertEquals(needed, info.configChanges and needed)
    }

    @Test
    fun manifestKeepsAndroid16Defaults() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        for (optOut in listOf(
            "PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY",
            "windowOptOutEdgeToEdgeEnforcement",
            "enableOnBackInvokedCallback=\"false\"",
        )) {
            assertFalse(manifest.contains(optOut), optOut)
        }
    }

    @Config(qualifiers = "land")
    @Test
    fun startsWithTheWindowShape() {
        model.updateIsPortrait()
        assertFalse(model.orientation.isPortrait.value)
        model.database.portrait = true
        model.updateIsPortrait()
        assertFalse(model.orientation.isPortrait.value)
    }
}
