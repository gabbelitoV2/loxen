package com.moblin.android.integrations.dji.djidevice

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
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.platform.corebluetooth.BluetoothAuthorization
import com.moblin.android.platform.corebluetooth.bluetoothIdentifier
import com.moblin.android.various.settings.SettingsDjiDeviceModel
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothLeScanner

@RunWith(RobolectricTestRunner::class)
class DjiDeviceScannerSuite {
    private lateinit var application: Application

    @Before
    fun setUp() {
        BluetoothAuthorization.reset()
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        shadowOf(application.packageManager).setSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE, true)
        shadowOf(adapter()).setState(BluetoothAdapter.STATE_ON)
    }

    @After
    fun tearDown() {
        BluetoothAuthorization.reset()
    }

    private fun adapter(): BluetoothAdapter = application.getSystemService(BluetoothManager::class.java).adapter

    private fun scanner(): ShadowBluetoothLeScanner = Shadow.extract(adapter().bluetoothLeScanner)

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun bytes(vararg values: Int): ByteArray = values.map { it.toByte() }.toByteArray()

    private fun scanRecord(vararg structures: ByteArray): ScanRecord {
        val bytes = structures.fold(ByteArray(0)) { all, structure -> all + byteArrayOf(structure.size.toByte()) + structure }
        val parse = ScanRecord::class.java.getDeclaredMethod("parseFromBytes", ByteArray::class.java)
        return parse.invoke(null, bytes) as ScanRecord
    }

    private fun advertise(callback: ScanCallback, address: String, vararg structures: ByteArray) {
        val device = adapter().getRemoteDevice(address)
        callback.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, ScanResult(device, scanRecord(*structures), -60, 0))
        runMain()
    }

    @Test
    fun onlyDjiDevicesAreListedOnceEachWithTheirModel() {
        val djiScanner = DjiDeviceScanner()
        djiScanner.startScanningForDevices()
        runMain()
        val scan = scanner().activeScans.single()
        assertTrue(scan.scanFilters().isNullOrEmpty())
        val callback = scan.scanCallback()!!
        advertise(callback, "60:60:1F:00:00:04", bytes(0x01, 0x06), bytes(0xFF, 0xAA, 0x08, 0x14, 0x00, 0x01))
        advertise(callback, "60:60:1F:00:00:20", bytes(0xFF, 0xAA, 0xF7, 0x20, 0x00))
        advertise(callback, "60:60:1F:00:00:99", bytes(0xFF, 0xAA, 0x08, 0x99, 0x00))
        advertise(callback, "4C:00:00:00:00:01", bytes(0xFF, 0x4C, 0x00, 0x14, 0x00))
        advertise(callback, "4C:00:00:00:00:02", bytes(0x01, 0x06))
        advertise(callback, "60:60:1F:00:00:04", bytes(0xFF, 0xAA, 0x08, 0x14, 0x00, 0x02))
        val devices = djiScanner.discoveredDevices.value
        assertEquals(
            listOf(
                bluetoothIdentifier("60:60:1F:00:00:04"),
                bluetoothIdentifier("60:60:1F:00:00:20"),
                bluetoothIdentifier("60:60:1F:00:00:99"),
            ),
            devices.map { it.peripheral.identifier },
        )
        assertEquals(
            listOf(SettingsDjiDeviceModel.osmoAction4, SettingsDjiDeviceModel.osmoPocket3, SettingsDjiDeviceModel.unknown),
            devices.map { it.model },
        )
        djiScanner.stopScanningForDevices()
        assertTrue(scanner().activeScans.isEmpty())
        advertise(callback, "60:60:1F:00:00:18", bytes(0xFF, 0xAA, 0x08, 0x18, 0x00))
        assertEquals(3, djiScanner.discoveredDevices.value.size)
        djiScanner.startScanningForDevices()
        assertEquals(emptyList(), djiScanner.discoveredDevices.value)
        runMain()
        advertise(scanner().activeScans.single().scanCallback()!!, "60:60:1F:00:00:18", bytes(0xFF, 0xAA, 0x08, 0x18, 0x00))
        assertEquals(listOf(SettingsDjiDeviceModel.osmoAction6), djiScanner.discoveredDevices.value.map { it.model })
        djiScanner.stopScanningForDevices()
    }
}
