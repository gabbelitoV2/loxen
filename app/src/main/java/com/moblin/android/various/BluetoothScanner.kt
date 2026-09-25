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

    val discoveredPeripherals = MutableStateFlow<List<BluetoothDevice>>(emptyList())

    private var centralManager: com.moblin.android.platform.corebluetooth.CBCentralManager? = null
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
            Unit
        }
    }

    fun startScanningForDevices() {
        discoveredPeripherals.value = emptyList()
        val adapter = bluetoothAdapter ?: bluetoothManager()?.adapter
        bluetoothAdapter = adapter
        if (adapter == null) {
            Log.e(tag, "No bluetooth adapter available")
            return
        }
        Unit
        centralManager?.delegate = null; centralManager = com.moblin.android.platform.corebluetooth.CBCentralManager(delegate = { central -> centralManagerDidUpdateState(central) }, queue = null)
        if (adapter.state == BluetoothAdapter.STATE_ON) {
        }
    }

    fun stopScanningForDevices() {
        centralManager?.stopScan()
        centralManager?.delegate = null; centralManager = null
        Unit
    }

    private fun centralManagerDidUpdateState(central: com.moblin.android.platform.corebluetooth.CBCentralManager) {
        if (central.state == com.moblin.android.platform.corebluetooth.CBManagerState.poweredOn) {
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
        scanner.scanForPeripherals(filters, settings, scanCallback)
    }

    private fun didDiscover(device: BluetoothDevice, rssi: Int) {
        val current = discoveredPeripherals.value
        if (current.any { it.address == device.address }) {
            return
        }
        discoveredPeripherals.value = current + device
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
