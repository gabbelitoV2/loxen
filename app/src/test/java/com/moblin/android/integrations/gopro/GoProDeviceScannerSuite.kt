package com.moblin.android.integrations.gopro

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
import android.os.ParcelUuid
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.localized
import com.moblin.android.platform.corebluetooth.BluetoothAuthorization
import com.moblin.android.platform.corebluetooth.bluetoothIdentifier
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothLeScanner

private const val namedCameraAddress = "C0:FF:EE:00:00:51"
private const val unnamedCameraAddress = "C0:FF:EE:00:00:52"

@RunWith(RobolectricTestRunner::class)
class GoProDeviceScannerSuite {
    private lateinit var application: Application
    private val scanner = GoProDeviceScanner()

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
        scanner.stopScanningForDevices()
        runMain()
        BluetoothAuthorization.reset()
    }

    private fun adapter(): BluetoothAdapter = application.getSystemService(BluetoothManager::class.java).adapter

    private fun leScanner(): ShadowBluetoothLeScanner = Shadow.extract(adapter().bluetoothLeScanner)

    private fun scanCallbacks(): Set<ScanCallback> = leScanner().scanCallbacks

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }

    private fun scanRecord(vararg structures: ByteArray): ScanRecord {
        val bytes = structures.fold(ByteArray(0)) { all, structure -> all + byteArrayOf(structure.size.toByte()) + structure }
        val parse = ScanRecord::class.java.getDeclaredMethod("parseFromBytes", ByteArray::class.java)
        return parse.invoke(null, bytes) as ScanRecord
    }

    private fun advertise(address: String, record: ScanRecord, timestamp: Long) {
        val result = ScanResult(adapter().getRemoteDevice(address), record, -50, timestamp)
        scanCallbacks().single().onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
        runMain()
    }

    @Test
    fun listsEachCameraAdvertisingTheControlServiceOnce() {
        scanner.startScanningForDevices()
        runMain()
        assertEquals(
            listOf(ParcelUuid.fromString("0000FEA6-0000-1000-8000-00805F9B34FB")),
            leScanner().activeScans.single().scanFilters().map { it.serviceUuid },
        )
        val flags = bytes(0x01, 0x06)
        val controlService = bytes(0x03, 0xA6, 0xFE)
        advertise(namedCameraAddress, scanRecord(flags, byteArrayOf(0x09) + "GoPro 1234".toByteArray(), controlService), 0)
        advertise(namedCameraAddress, scanRecord(flags, controlService), 1)
        advertise(unnamedCameraAddress, scanRecord(flags, controlService), 2)
        val devices = scanner.discoveredDevices.value
        assertEquals(listOf("GoPro 1234", localized("Unknown")), devices.map { it.name })
        assertEquals(
            listOf(bluetoothIdentifier(namedCameraAddress), bluetoothIdentifier(unnamedCameraAddress)),
            devices.map { it.peripheral.identifier },
        )
        scanner.stopScanningForDevices()
        assertEquals(emptySet(), scanCallbacks())
        scanner.startScanningForDevices()
        assertEquals(emptyList(), scanner.discoveredDevices.value)
        runMain()
        assertEquals(1, scanCallbacks().size)
    }
}
