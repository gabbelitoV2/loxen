package com.moblin.android.integrations.dji.djidevice

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import com.moblin.android.various.settings.SettingsDjiDeviceModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DjiDiscoveredDevice(
    val peripheral: BluetoothDevice,
    val model: SettingsDjiDeviceModel,
)

class DjiDeviceScanner {
    companion object {
        val shared = DjiDeviceScanner()
        private const val tag = "DjiDeviceScanner"
    }

    val discoveredDevices = MutableStateFlow<List<DjiDiscoveredDevice>>(emptyList())

    private var centralManager: com.moblin.android.platform.corebluetooth.CBCentralManager? = null
    private var scanner: com.moblin.android.platform.corebluetooth.CBCentralManager? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val manufacturerSpecificData = result.scanRecord?.manufacturerSpecificData ?: return
            if (manufacturerSpecificData.size() == 0) {
                return
            }
            val companyId = manufacturerSpecificData.keyAt(0)
            val payload = manufacturerSpecificData.valueAt(0)
            val manufacturerData = ByteArray(2 + payload.size)
            manufacturerData[0] = (companyId and 0xFF).toByte()
            manufacturerData[1] = ((companyId shr 8) and 0xFF).toByte()
            payload.copyInto(manufacturerData, destinationOffset = 2)
            didDiscover(result.device, manufacturerData)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(tag, "dji-scanner: Scan failed with error code $errorCode")
        }
    }

    fun startScanningForDevices() {
        discoveredDevices.value = emptyList()
        centralManager?.delegate = null; centralManager = com.moblin.android.platform.corebluetooth.CBCentralManager(delegate = { central -> centralManagerDidUpdateState(central) }, queue = null)
        Unit
    }

    fun stopScanningForDevices() {
        scanner?.stopScan()
        scanner = null
        centralManager?.delegate = null; centralManager = null
    }

    fun centralManagerDidUpdateState(central: com.moblin.android.platform.corebluetooth.CBCentralManager?) {
        if (central?.state == com.moblin.android.platform.corebluetooth.CBManagerState.poweredOn) {
            scanner = central
            scanner?.scanForPeripherals(
                null,
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                scanCallback,
            )
        }
    }

    fun didDiscover(peripheral: BluetoothDevice, manufacturerData: ByteArray) {
        if (!isDjiDevice(manufacturerData)) {
            return
        }
        if (discoveredDevices.value.any { it.peripheral == peripheral }) {
            return
        }
        val model = djiModelFromManufacturerData(manufacturerData)
        Log.i(
            tag,
            "dji-scanner: Manufacturer data ${manufacturerData.joinToString("") { "%02x".format(it) }} for " +
                "peripheral id ${peripheral.address} and model $model",
        )
        discoveredDevices.value = discoveredDevices.value + DjiDiscoveredDevice(peripheral, model)
    }
}
