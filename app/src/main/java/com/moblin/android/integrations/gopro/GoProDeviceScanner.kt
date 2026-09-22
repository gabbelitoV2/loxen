package com.moblin.android.integrations.gopro

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.ParcelUuid
import com.moblin.android.localized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GoProDiscoveredDevice(
    val peripheral: BluetoothDevice,
    val name: String
)

class GoProDeviceScanner {

    companion object {
        val shared: GoProDeviceScanner by lazy { GoProDeviceScanner() }
    }

    val discoveredDevices = MutableStateFlow<List<GoProDiscoveredDevice>>(emptyList())

    private var centralManager: BluetoothLeScanner? = null
    private var scanContext: Context? = null

    private val adapterStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) {
                return
            }
            val adapter = (context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                ?: return
            centralManagerDidUpdateState(adapter)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val found = result ?: return
            centralManagerDidDiscover(found.device, found.scanRecord, found.rssi)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanningForDevices(context: Context) {
        discoveredDevices.value = emptyList()
        val appContext = context.applicationContext
        scanContext = appContext
        appContext.registerReceiver(
            adapterStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        val adapter = (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            ?: return
        centralManagerDidUpdateState(adapter)
    }

    @SuppressLint("MissingPermission")
    fun stopScanningForDevices() {
        centralManager?.stopScan(scanCallback)
        centralManager = null
        scanContext?.let { context ->
            runCatching { context.unregisterReceiver(adapterStateReceiver) }
        }
        scanContext = null
    }

    @SuppressLint("MissingPermission")
    fun centralManagerDidUpdateState(central: BluetoothAdapter) {
        if (!central.isEnabled) {
            return
        }
        val scanner = central.bluetoothLeScanner
        if (scanner == null) {
            return
        }
        centralManager = scanner
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(goProControlServiceId))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(listOf(filter), settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun centralManagerDidDiscover(
        peripheral: BluetoothDevice,
        advertisementData: ScanRecord?,
        rssi: Int
    ) {
        if (discoveredDevices.value.any { it.peripheral.address == peripheral.address }) {
            return
        }
        val name = advertisementData?.deviceName
            ?: peripheral.name
            ?: localized("Unknown")
        discoveredDevices.value = discoveredDevices.value + GoProDiscoveredDevice(peripheral, name)
    }
}
