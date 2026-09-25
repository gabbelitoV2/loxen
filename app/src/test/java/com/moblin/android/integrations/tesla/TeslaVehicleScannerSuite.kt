package com.moblin.android.integrations.tesla

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.platform.corebluetooth.BluetoothAuthorization
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothLeScanner

@RunWith(RobolectricTestRunner::class)
class TeslaVehicleScannerSuite {
    private lateinit var application: Application
    private var controller: ActivityController<ComponentActivity>? = null
    private val permissions = arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)

    @Before
    fun setUp() {
        BluetoothAuthorization.reset()
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).denyPermissions(*permissions)
        shadowOf(application.packageManager).setSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE, true)
        shadowOf(adapter()).setState(BluetoothAdapter.STATE_ON)
    }

    @After
    fun tearDown() {
        TeslaVehicleScanner.shared.stopScanningForDevices()
        controller?.pause()?.stop()?.destroy()
        BluetoothAuthorization.reset()
    }

    private fun adapter(): BluetoothAdapter = application.getSystemService(BluetoothManager::class.java).adapter

    private fun scanCallbacks(): Set<ScanCallback> {
        val scanner = adapter().bluetoothLeScanner ?: return emptySet()
        return Shadow.extract<ShadowBluetoothLeScanner>(scanner).scanCallbacks
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun selectingAVehicleBeforeBluetoothIsAllowedAsksForItAndScansOnceGranted() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        this.controller = controller
        val activity = controller.get()
        BluetoothAuthorization.install(activity)
        val scanner = TeslaVehicleScanner.shared
        scanner.startScanningForDevices(activity)
        runMain()
        assertEquals(emptySet(), scanCallbacks())
        val request = shadowOf(activity).lastRequestedPermission
        assertEquals(permissions.toList(), request.requestedPermissions.toList())
        shadowOf(application).grantPermissions(*permissions)
        @Suppress("DEPRECATION")
        activity.onRequestPermissionsResult(
            request.requestCode,
            request.requestedPermissions,
            IntArray(request.requestedPermissions.size) { PackageManager.PERMISSION_GRANTED },
        )
        runMain()
        assertEquals(setOf<ScanCallback>(scanner), scanCallbacks())
        scanner.stopScanningForDevices()
        assertEquals(emptySet(), scanCallbacks())
    }

    @Test
    fun aDeniedPermissionNeverScans() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        this.controller = controller
        val activity = controller.get()
        BluetoothAuthorization.install(activity)
        val scanner = TeslaVehicleScanner.shared
        scanner.startScanningForDevices(activity)
        runMain()
        val request = shadowOf(activity).lastRequestedPermission
        @Suppress("DEPRECATION")
        activity.onRequestPermissionsResult(
            request.requestCode,
            request.requestedPermissions,
            IntArray(request.requestedPermissions.size) { PackageManager.PERMISSION_DENIED },
        )
        runMain()
        scanner.stopScanningForDevices()
        scanner.startScanningForDevices(activity)
        runMain()
        assertEquals(emptySet(), scanCallbacks())
        assertEquals(request, shadowOf(activity).lastRequestedPermission)
    }
}
