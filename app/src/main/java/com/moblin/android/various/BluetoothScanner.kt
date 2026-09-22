package com.moblin.android.various

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BluetoothScanner(
    private val context: Context,
    private val serviceIds: List<UUID>
) {
    private val mainScope = CoroutineScope(Dispatchers.Main)

    private val _discoveredPeripherals = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredPeripherals: StateFlow<List<BluetoothDevice>> = _discoveredPeripherals.asStateFlow()

    private var centralManager: BluetoothLeScanner? = null
    private var bluetoothAdapter: BluetoothAdapter? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            didDiscover(result.device, result.rssi)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { result ->
                didDiscover(result.device, result.rssi)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(tag, "centralManager did fail with error $errorCode")
        }
    }

    private val adapterStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context?, intent: Intent?) {
            if (intent == null || intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) {
                return
            }
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            centralManagerDidUpdateState(state)
        }
    }

    fun startScanningForDevices() {
        _discoveredPeripherals.value = emptyList()
        val adapter = bluetoothAdapter ?: bluetoothManager()?.adapter
        bluetoothAdapter = adapter
        if (adapter == null) {
            Log.e(tag, "No bluetooth adapter available")
            return
        }
        registerAdapterStateReceiver()
        centralManager = adapter.bluetoothLeScanner
        if (adapter.state == BluetoothAdapter.STATE_ON) {
            centralManagerDidUpdateState(adapter.state)
        }
    }

    fun stopScanningForDevices() {
        centralManager?.stopScan(scanCallback)
        centralManager = null
        unregisterAdapterStateReceiver()
    }

    private fun centralManagerDidUpdateState(state: Int) {
        if (state == BluetoothAdapter.STATE_ON) {
            startScan()
        }
    }

    private fun startScan() {
        val scanner = centralManager ?: return
        if (!hasScanPermission()) {
            Log.e(tag, "Missing bluetooth scan permission")
            return
        }
        val filters = serviceIds.map { serviceId ->
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(serviceId))
                .build()
        }
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(filters, settings, scanCallback)
    }

    private fun didDiscover(device: BluetoothDevice, rssi: Int) {
        val current = _discoveredPeripherals.value
        if (current.any { it.address == device.address }) {
            return
        }
        _discoveredPeripherals.value = current + device
    }

    private fun hasScanPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun bluetoothManager(): BluetoothManager? {
        return context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    private fun registerAdapterStateReceiver() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(adapterStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(adapterStateReceiver, filter)
        }
    }

    private fun unregisterAdapterStateReceiver() {
        runCatching { context.unregisterReceiver(adapterStateReceiver) }
            .onFailure { error -> Log.e(tag, "Failed to unregister receiver: ${error.message}") }
    }

    companion object {
        private const val tag = "BluetoothScanner"
    }
}
