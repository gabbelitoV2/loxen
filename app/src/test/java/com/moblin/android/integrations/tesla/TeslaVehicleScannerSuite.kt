package com.moblin.android.integrations.tesla

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.platform.corebluetooth.BluetoothAuthorization
import com.moblin.android.platform.corebluetooth.bluetoothIdentifier
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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

private const val vehicleAddress = "4C:FC:AA:00:00:01"
private const val otherVehicleAddress = "4C:FC:AA:00:00:02"
private const val vehicleName = "S0123456789abcdefC"
private const val otherVehicleName = "Sfedcba9876543210C"

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
        runMain()
        controller?.pause()?.stop()?.destroy()
        BluetoothAuthorization.reset()
    }

    private fun adapter(): BluetoothAdapter = application.getSystemService(BluetoothManager::class.java).adapter

    private fun leScanner(): ShadowBluetoothLeScanner = Shadow.extract(adapter().bluetoothLeScanner)

    private fun scanCallbacks(): Set<ScanCallback> = leScanner().scanCallbacks

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun startActivity(): ComponentActivity {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        this.controller = controller
        BluetoothAuthorization.install(controller.get())
        return controller.get()
    }

    private fun answerPermissionRequest(activity: ComponentActivity, granted: Boolean) {
        val request = shadowOf(activity).lastRequestedPermission
        assertEquals(permissions.toList(), request.requestedPermissions.toList())
        val result = if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        if (granted) {
            shadowOf(application).grantPermissions(*permissions)
        }
        @Suppress("DEPRECATION")
        activity.onRequestPermissionsResult(
            request.requestCode,
            request.requestedPermissions,
            IntArray(request.requestedPermissions.size) { result },
        )
        runMain()
    }

    private fun localName(name: String): ByteArray = byteArrayOf(0x09) + name.toByteArray()

    private fun scanRecord(vararg structures: ByteArray): ScanRecord {
        val bytes = structures.fold(ByteArray(0)) { all, structure -> all + byteArrayOf(structure.size.toByte()) + structure }
        val parse = ScanRecord::class.java.getDeclaredMethod("parseFromBytes", ByteArray::class.java)
        return parse.invoke(null, bytes) as ScanRecord
    }

    private fun advertise(callback: ScanCallback, address: String, record: ScanRecord?, timestamp: Long) {
        val result = ScanResult(adapter().getRemoteDevice(address), record, -60, timestamp)
        callback.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
        runMain()
    }

    @Test
    fun selectingAVehicleBeforeBluetoothIsAllowedAsksForItAndScansForAllPeripheralsOnceGranted() {
        val activity = startActivity()
        val scanner = TeslaVehicleScanner.shared
        scanner.startScanningForDevices()
        runMain()
        assertEquals(emptySet(), scanCallbacks())
        answerPermissionRequest(activity, granted = true)
        assertTrue(leScanner().activeScans.single().scanFilters().isNullOrEmpty())
        scanner.stopScanningForDevices()
        assertEquals(emptySet(), scanCallbacks())
    }

    @Test
    fun aDeniedPermissionNeverScansAndIsNotAskedAgain() {
        val activity = startActivity()
        val scanner = TeslaVehicleScanner.shared
        scanner.startScanningForDevices()
        runMain()
        val request = shadowOf(activity).lastRequestedPermission
        answerPermissionRequest(activity, granted = false)
        scanner.stopScanningForDevices()
        scanner.startScanningForDevices()
        runMain()
        assertEquals(emptySet(), scanCallbacks())
        assertEquals(request, shadowOf(activity).lastRequestedPermission)
    }

    @Test
    fun onlyPeripheralsWhoseWholeLocalNameIsAVehicleNameAreListedOnceEach() {
        shadowOf(application).grantPermissions(*permissions)
        val scanner = TeslaVehicleScanner.shared
        scanner.startScanningForDevices()
        runMain()
        val callback = leScanner().activeScans.single().scanCallback()!!
        val flags = byteArrayOf(0x01, 0x06)
        advertise(callback, "4C:FC:AA:00:00:10", null, 0)
        advertise(callback, "4C:FC:AA:00:00:11", scanRecord(flags), 1)
        advertise(callback, "4C:FC:AA:00:00:12", scanRecord(flags, localName("S0123456789ABCDEFC")), 2)
        advertise(callback, "4C:FC:AA:00:00:13", scanRecord(flags, localName("xS0123456789abcdefC")), 3)
        advertise(callback, "4C:FC:AA:00:00:14", scanRecord(flags, localName("S0123456789abcdefCx")), 4)
        advertise(callback, "4C:FC:AA:00:00:15", scanRecord(flags, localName("S0123456789abcdeC")), 5)
        advertise(callback, "4C:FC:AA:00:00:16", scanRecord(flags, localName("Model 3")), 6)
        advertise(callback, vehicleAddress, scanRecord(flags, localName(vehicleName)), 7)
        advertise(callback, vehicleAddress, scanRecord(flags, localName(vehicleName), byteArrayOf(0x0A, 0x04)), 8)
        advertise(callback, otherVehicleAddress, scanRecord(flags, localName(otherVehicleName)), 9)
        val peripherals = scanner.discoveredPeripherals.value
        assertEquals(
            listOf(bluetoothIdentifier(vehicleAddress), bluetoothIdentifier(otherVehicleAddress)),
            peripherals.map { it.identifier },
        )
        assertEquals(listOf(vehicleName, otherVehicleName), peripherals.map { it.name })
    }

    @Test
    fun stoppingDropsLateResultsAndStartingAgainBeginsWithAnEmptyList() {
        shadowOf(application).grantPermissions(*permissions)
        val scanner = TeslaVehicleScanner.shared
        scanner.startScanningForDevices()
        runMain()
        val callback = leScanner().activeScans.single().scanCallback()!!
        advertise(callback, vehicleAddress, scanRecord(localName(vehicleName)), 0)
        assertEquals(1, scanner.discoveredPeripherals.value.size)
        scanner.stopScanningForDevices()
        assertEquals(emptySet(), scanCallbacks())
        advertise(callback, otherVehicleAddress, scanRecord(localName(otherVehicleName)), 1)
        assertEquals(listOf(bluetoothIdentifier(vehicleAddress)), scanner.discoveredPeripherals.value.map { it.identifier })
        scanner.startScanningForDevices()
        assertEquals(emptyList(), scanner.discoveredPeripherals.value)
        runMain()
        advertise(leScanner().activeScans.single().scanCallback()!!, otherVehicleAddress, scanRecord(localName(otherVehicleName)), 2)
        assertEquals(listOf(bluetoothIdentifier(otherVehicleAddress)), scanner.discoveredPeripherals.value.map { it.identifier })
    }
}
