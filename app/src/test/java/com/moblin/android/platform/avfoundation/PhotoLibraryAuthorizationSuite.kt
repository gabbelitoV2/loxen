package com.moblin.android.platform.avfoundation

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
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
class PhotoLibraryAuthorizationSuite {
    private lateinit var application: Application
    private var controller: ActivityController<ComponentActivity>? = null
    private val reported = mutableListOf<PHAuthorizationStatus>()

    @Before
    fun setUp() {
        PhotoLibraryAuthorization.reset()
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).denyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    @After
    fun tearDown() {
        controller?.pause()?.stop()?.destroy()
        PhotoLibraryAuthorization.reset()
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun startActivity(): ComponentActivity {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        this.controller = controller
        PhotoLibraryAuthorization.install(controller.get())
        return controller.get()
    }

    private fun request() {
        PHPhotoLibrary.requestAuthorization(`for` = PHAccessLevel.readWrite) { reported.add(it) }
    }

    private fun answer(activity: ComponentActivity, granted: Boolean) {
        val request = shadowOf(activity).lastRequestedPermission
        if (granted) {
            shadowOf(application).grantPermissions(*request.requestedPermissions)
        }
        val result = if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        @Suppress("DEPRECATION")
        activity.onRequestPermissionsResult(
            request.requestCode,
            request.requestedPermissions,
            IntArray(request.requestedPermissions.size) { result },
        )
        runMain()
    }

    @Test
    fun scopedStorageNeedsNoPermissionAndIsAuthorizedAtOnce() {
        PhotoLibraryAuthorization.sdkInt = Build.VERSION_CODES.Q
        val activity = startActivity()
        request()
        runMain()
        assertEquals(listOf(PHAuthorizationStatus.authorized), reported)
        assertNull(shadowOf(activity).lastRequestedPermission)
    }

    @Test
    fun beforeScopedStorageItAsksToWriteExternalStorage() {
        PhotoLibraryAuthorization.sdkInt = Build.VERSION_CODES.P
        val activity = startActivity()
        request()
        runMain()
        assertEquals(emptyList(), reported)
        assertEquals(
            listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            shadowOf(activity).lastRequestedPermission.requestedPermissions.toList(),
        )
        answer(activity, granted = true)
        assertEquals(listOf(PHAuthorizationStatus.authorized), reported)
        request()
        runMain()
        assertEquals(listOf(PHAuthorizationStatus.authorized, PHAuthorizationStatus.authorized), reported)
    }

    @Test
    fun aDeniedRequestIsDeniedAndIsNotAskedAgainLikeIos() {
        PhotoLibraryAuthorization.sdkInt = Build.VERSION_CODES.O
        val activity = startActivity()
        request()
        request()
        runMain()
        answer(activity, granted = false)
        assertEquals(listOf(PHAuthorizationStatus.denied, PHAuthorizationStatus.denied), reported)
        val firstRequest = shadowOf(activity).lastRequestedPermission
        request()
        runMain()
        assertEquals(firstRequest, shadowOf(activity).lastRequestedPermission)
        assertEquals(PHAuthorizationStatus.denied, reported.last())
    }

    @Test
    fun aCancelledRequestStaysNotDetermined() {
        PhotoLibraryAuthorization.sdkInt = Build.VERSION_CODES.P
        val activity = startActivity()
        request()
        runMain()
        val request = shadowOf(activity).lastRequestedPermission
        @Suppress("DEPRECATION")
        activity.onRequestPermissionsResult(request.requestCode, emptyArray(), IntArray(0))
        runMain()
        assertEquals(listOf(PHAuthorizationStatus.notDetermined), reported)
    }

    @Test
    fun withoutAnActivityTheHandlerStillRuns() {
        PhotoLibraryAuthorization.sdkInt = Build.VERSION_CODES.P
        request()
        runMain()
        assertEquals(listOf(PHAuthorizationStatus.notDetermined), reported)
    }
}
