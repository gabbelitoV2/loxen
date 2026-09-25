package com.moblin.android.various

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
import com.moblin.android.platform.corebluetooth.BluetoothAuthorization
import com.moblin.android.platform.corebluetooth.CBUUID
import com.moblin.android.platform.corebluetooth.bluetoothIdentifier
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

private const val heartRateAddress = "C8:00:00:00:00:01"
private const val cyclingPowerAddress = "C8:00:00:00:00:02"
private const val coolerAddress = "C8:00:00:00:00:03"

@RunWith(RobolectricTestRunner::class)
class BluetoothScannerSuite {
    private lateinit var application: Application
    private val scanners = mutableListOf<BluetoothScanner>()

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
        scanners.forEach { it.stopScanningForDevices() }
        runMain()
        BluetoothAuthorization.reset()
    }

    private fun scanner(serviceIds: List<CBUUID>): BluetoothScanner {
        return BluetoothScanner(serviceIds = serviceIds).also { scanners.add(it) }
    }

    private fun adapter(): BluetoothAdapter = application.getSystemService(BluetoothManager::class.java).adapter

    private fun leScanner(): ShadowBluetoothLeScanner = Shadow.extract(adapter().bluetoothLeScanner)

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }

    private fun localName(name: String): ByteArray = byteArrayOf(0x09) + name.toByteArray()

    private fun scanRecord(vararg structures: ByteArray): ScanRecord {
        val bytes = structures.fold(ByteArray(0)) { all, structure -> all + byteArrayOf(structure.size.toByte()) + structure }
        val parse = ScanRecord::class.java.getDeclaredMethod("parseFromBytes", ByteArray::class.java)
        return parse.invoke(null, bytes) as ScanRecord
    }

    private fun advertise(callback: ScanCallback, address: String, record: ScanRecord, timestamp: Long) {
        val result = ScanResult(adapter().getRemoteDevice(address), record, -55, timestamp)
        callback.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
        runMain()
    }

    @Test
    fun scansForTheServiceIdsAndListsEachPeripheralOnceInDiscoveryOrder() {
        val scanner = scanner(listOf(CBUUID(string = "180D"), CBUUID(string = "1818")))
        scanner.startScanningForDevices()
        assertEquals(emptyList(), scanner.discoveredPeripherals.value)
        runMain()
        val scan = leScanner().activeScans.single()
        assertEquals(
            listOf(
                ParcelUuid.fromString("0000180D-0000-1000-8000-00805F9B34FB"),
                ParcelUuid.fromString("00001818-0000-1000-8000-00805F9B34FB"),
            ),
            scan.scanFilters().map { it.serviceUuid },
        )
        val callback = scan.scanCallback()!!
        advertise(callback, heartRateAddress, scanRecord(bytes(0x01, 0x06), localName("Polar H10"), bytes(0x03, 0x0D, 0x18)), 0)
        advertise(callback, cyclingPowerAddress, scanRecord(bytes(0x01, 0x06), bytes(0x03, 0x18, 0x18)), 1)
        advertise(callback, heartRateAddress, scanRecord(bytes(0x01, 0x06), localName("Polar H10"), bytes(0x0A, 0x04)), 2)
        val peripherals = scanner.discoveredPeripherals.value
        assertEquals(
            listOf(bluetoothIdentifier(heartRateAddress), bluetoothIdentifier(cyclingPowerAddress)),
            peripherals.map { it.identifier },
        )
        assertEquals(listOf("Polar H10", null), peripherals.map { it.name })
    }

    @Test
    fun noServiceIdsListsEveryPeripheralLikeCoreBluetooth() {
        val scanner = scanner(emptyList())
        scanner.startScanningForDevices()
        runMain()
        val scan = leScanner().activeScans.single()
        assertTrue(scan.scanFilters().isNullOrEmpty())
        advertise(scan.scanCallback()!!, coolerAddress, scanRecord(localName("Black Shark FunCooler 3 Pro")), 0)
        advertise(scan.scanCallback()!!, heartRateAddress, scanRecord(localName("Polar H10")), 1)
        assertEquals(
            listOf("Black Shark FunCooler 3 Pro", "Polar H10"),
            scanner.discoveredPeripherals.value.map { it.name },
        )
    }

    @Test
    fun stoppingEndsTheScanAndDropsLateResultsAndStartingAgainClearsTheList() {
        val scanner = scanner(listOf(CBUUID(string = "180D")))
        scanner.startScanningForDevices()
        runMain()
        val callback = leScanner().activeScans.single().scanCallback()!!
        advertise(callback, heartRateAddress, scanRecord(localName("Polar H10")), 0)
        callback.onScanResult(
            ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
            ScanResult(adapter().getRemoteDevice(cyclingPowerAddress), scanRecord(localName("Assioma")), -60, 1),
        )
        scanner.stopScanningForDevices()
        assertTrue(leScanner().activeScans.isEmpty())
        runMain()
        advertise(callback, coolerAddress, scanRecord(localName("Other")), 2)
        assertEquals(listOf(bluetoothIdentifier(heartRateAddress)), scanner.discoveredPeripherals.value.map { it.identifier })
        scanner.startScanningForDevices()
        assertEquals(emptyList(), scanner.discoveredPeripherals.value)
        runMain()
        advertise(leScanner().activeScans.single().scanCallback()!!, heartRateAddress, scanRecord(localName("Polar H10")), 3)
        assertEquals(listOf(bluetoothIdentifier(heartRateAddress)), scanner.discoveredPeripherals.value.map { it.identifier })
    }

    @Test
    fun startingTwiceReplacesTheScanInsteadOfAddingOne() {
        val scanner = scanner(listOf(CBUUID(string = "180D")))
        scanner.startScanningForDevices()
        runMain()
        val first = leScanner().activeScans.single().scanCallback()
        scanner.startScanningForDevices()
        runMain()
        val second = leScanner().activeScans.single().scanCallback()
        assertTrue(first !== second)
        first!!.onScanResult(
            ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
            ScanResult(adapter().getRemoteDevice(heartRateAddress), scanRecord(localName("Polar H10")), -60, 0),
        )
        runMain()
        assertEquals(emptyList(), scanner.discoveredPeripherals.value)
    }

    @Test
    fun twoScannersScanIndependently() {
        val workout = scanner(listOf(CBUUID(string = "180D")))
        val cooler = scanner(emptyList())
        workout.startScanningForDevices()
        cooler.startScanningForDevices()
        runMain()
        assertEquals(2, leScanner().activeScans.size)
        workout.stopScanningForDevices()
        val remaining = leScanner().activeScans.single()
        assertTrue(remaining.scanFilters().isNullOrEmpty())
        advertise(remaining.scanCallback()!!, coolerAddress, scanRecord(localName("Black Shark")), 0)
        assertEquals(emptyList(), workout.discoveredPeripherals.value)
        assertEquals(listOf(bluetoothIdentifier(coolerAddress)), cooler.discoveredPeripherals.value.map { it.identifier })
    }
}
