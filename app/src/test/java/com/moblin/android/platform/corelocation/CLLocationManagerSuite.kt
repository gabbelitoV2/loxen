package com.moblin.android.platform.corelocation

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class CLLocationManagerSuite {
    private lateinit var application: Application
    private var controller: ActivityController<ComponentActivity>? = null
    private val reported = mutableListOf<CLAuthorizationStatus>()
    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    private inner class RecordingDelegate : CLLocationManagerDelegate {
        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            reported.add(manager.authorizationStatus)
        }
    }

    @Before
    fun setUp() {
        LocationAuthorization.reset()
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).denyPermissions(*locationPermissions)
    }

    @After
    fun tearDown() {
        controller?.pause()?.stop()?.destroy()
        LocationAuthorization.reset()
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun startActivity(): ComponentActivity {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        this.controller = controller
        LocationAuthorization.install(controller.get())
        return controller.get()
    }

    private fun managerWithDelegate(): CLLocationManager {
        val manager = CLLocationManager()
        manager.delegate = RecordingDelegate()
        return manager
    }

    private fun answerPermissionRequest(activity: ComponentActivity, granted: Boolean) {
        val request = shadowOf(activity).lastRequestedPermission
        val result = if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        if (granted) {
            shadowOf(application).grantPermissions(*request.requestedPermissions)
        }
        @Suppress("DEPRECATION")
        activity.onRequestPermissionsResult(
            request.requestCode,
            request.requestedPermissions,
            IntArray(request.requestedPermissions.size) { result },
        )
        runMain()
    }

    @Test
    fun theDelegateGetsTheFirstCallbackAfterItIsSetNotDuring() {
        val manager = CLLocationManager()
        var managerWasReady = false
        manager.delegate = object : CLLocationManagerDelegate {
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                managerWasReady = true
                reported.add(manager.authorizationStatus)
            }
        }
        assertEquals(emptyList(), reported)
        runMain()
        assertEquals(true, managerWasReady)
        assertEquals(listOf(CLAuthorizationStatus.notDetermined), reported)
        runMain()
        assertEquals(1, reported.size)
    }

    @Test
    fun aGrantedLocationPermissionIsWhenInUseAuthorization() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        assertEquals(CLAuthorizationStatus.authorizedWhenInUse, CLLocationManager().authorizationStatus)
        shadowOf(application).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        assertEquals(CLAuthorizationStatus.authorizedWhenInUse, CLLocationManager().authorizationStatus)
    }

    @Test
    fun requestingAsksForFineAndCoarseLocationAndReportsTheGrant() {
        val activity = startActivity()
        val manager = managerWithDelegate()
        runMain()
        manager.requestWhenInUseAuthorization()
        assertEquals(
            locationPermissions.toList(),
            shadowOf(activity).lastRequestedPermission.requestedPermissions.toList(),
        )
        answerPermissionRequest(activity, granted = true)
        assertEquals(
            listOf(CLAuthorizationStatus.notDetermined, CLAuthorizationStatus.authorizedWhenInUse),
            reported,
        )
    }

    @Test
    fun aDeniedRequestIsDeniedAndIsNotAskedAgainLikeIos() {
        val activity = startActivity()
        val manager = managerWithDelegate()
        runMain()
        manager.requestWhenInUseAuthorization()
        answerPermissionRequest(activity, granted = false)
        assertEquals(listOf(CLAuthorizationStatus.notDetermined, CLAuthorizationStatus.denied), reported)
        val firstRequest = shadowOf(activity).lastRequestedPermission
        manager.requestWhenInUseAuthorization()
        runMain()
        assertEquals(firstRequest, shadowOf(activity).lastRequestedPermission)
        assertEquals(CLAuthorizationStatus.denied, CLLocationManager().authorizationStatus)
    }

    @Test
    fun aPermissionDeniedInAnEarlierRunIsDenied() {
        val activity = startActivity()
        shadowOf(application.packageManager)
            .setShouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION, true)
        assertEquals(CLAuthorizationStatus.denied, CLLocationManager().authorizationStatus)
        CLLocationManager().requestWhenInUseAuthorization()
        assertNull(shadowOf(activity).lastRequestedPermission)
    }

    @Test
    fun aCancelledRequestStaysNotDetermined() {
        val activity = startActivity()
        val manager = managerWithDelegate()
        runMain()
        manager.requestWhenInUseAuthorization()
        val request = shadowOf(activity).lastRequestedPermission
        @Suppress("DEPRECATION")
        activity.onRequestPermissionsResult(request.requestCode, emptyArray(), IntArray(0))
        runMain()
        assertEquals(CLAuthorizationStatus.notDetermined, manager.authorizationStatus)
        assertEquals(listOf(CLAuthorizationStatus.notDetermined), reported)
    }

    @Test
    fun withoutAnActivityRequestingDoesNothingAndDoesNotCrash() {
        val manager = managerWithDelegate()
        runMain()
        manager.requestWhenInUseAuthorization()
        runMain()
        assertEquals(listOf(CLAuthorizationStatus.notDetermined), reported)
    }

    @Test
    fun aPermissionGrantedInSettingsIsReportedWhenTheAppResumes() {
        startActivity()
        managerWithDelegate()
        runMain()
        controller!!.pause()
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        controller!!.resume()
        runMain()
        assertEquals(
            listOf(CLAuthorizationStatus.notDetermined, CLAuthorizationStatus.authorizedWhenInUse),
            reported,
        )
        controller!!.pause()
        controller!!.resume()
        runMain()
        assertEquals(2, reported.size)
    }

    private fun setLocationEnabled(enabled: Boolean) {
        shadowOf(application.getSystemService(LocationManager::class.java)).setLocationEnabled(enabled)
    }

    @Test
    fun locationServicesTurnedOffAreDeniedLikeIosAndNothingIsAsked() {
        val activity = startActivity()
        setLocationEnabled(false)
        val manager = managerWithDelegate()
        runMain()
        assertEquals(listOf(CLAuthorizationStatus.denied), reported)
        manager.requestWhenInUseAuthorization()
        runMain()
        assertNull(shadowOf(activity).lastRequestedPermission)
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        assertEquals(CLAuthorizationStatus.denied, manager.authorizationStatus)
        controller!!.pause()
        setLocationEnabled(true)
        controller!!.resume()
        runMain()
        assertEquals(listOf(CLAuthorizationStatus.denied, CLAuthorizationStatus.authorizedWhenInUse), reported)
    }

    @Test
    fun aManagerWithoutDelegateIsNeverReported() {
        startActivity()
        val manager = CLLocationManager()
        runMain()
        manager.requestWhenInUseAuthorization()
        runMain()
        assertEquals(emptyList(), reported)
    }
}
