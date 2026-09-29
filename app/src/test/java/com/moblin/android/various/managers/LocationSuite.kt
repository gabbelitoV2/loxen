package com.moblin.android.various.managers

import android.Manifest
import android.app.Application
import android.location.LocationManager
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.platform.corelocation.LocationAuthorization
import com.moblin.android.various.settings.SettingsLocationDesiredAccuracy
import com.moblin.android.various.settings.SettingsLocationDistanceFilter
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

@RunWith(RobolectricTestRunner::class)
class LocationSuite {
    private lateinit var application: Application
    private var controller: ActivityController<ComponentActivity>? = null
    private var location: Location? = null

    @Before
    fun setUp() {
        LocationAuthorization.reset()
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        LocationAuthorization.install(controller!!.get())
    }

    @After
    fun tearDown() {
        location?.stop()
        controller?.pause()?.stop()?.destroy()
        LocationAuthorization.reset()
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun setLocationEnabled(enabled: Boolean) {
        shadowOf(application.getSystemService(LocationManager::class.java)).setLocationEnabled(enabled)
    }

    private fun start(location: Location) {
        location.start(
            accuracy = SettingsLocationDesiredAccuracy.best,
            distanceFilter = SettingsLocationDistanceFilter.none,
        ) {}
        runMain()
    }

    @Test
    fun startingAgainAfterLocationWasTurnedOnClearsTheWarning() {
        setLocationEnabled(false)
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        val location = Location(application).also { this.location = it }
        start(location)
        assertTrue(location.isDenied.value)
        setLocationEnabled(true)
        start(location)
        assertFalse(location.isDenied.value)
    }
}
