package com.moblin.android.integrations.tesla

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TeslaVehicleScanner private constructor() : ScanCallback() {

    companion object {
        private val teslaVehicleNameRegex = Regex("S[0-9a-f]{16}C")

        val shared = TeslaVehicleScanner()
    }

    val discoveredPeripherals = MutableStateFlow<List<BluetoothDevice>>(emptyList())

    private var centralManager: BluetoothLeScanner? = null

    @SuppressLint("MissingPermission")
    fun startScanningForDevices(context: Context) {
        discoveredPeripherals.value = emptyList()
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = manager?.adapter
        centralManager = adapter?.bluetoothLeScanner
        if (adapter != null) {
            centralManagerDidUpdateState(adapter.state)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanningForDevices() {
        centralManager?.stopScan(this)
        centralManager = null
    }

    @SuppressLint("MissingPermission")
    fun centralManagerDidUpdateState(state: Int) {
        if (state == BluetoothAdapter.STATE_ON) {
            centralManager?.startScan(this)
        }
    }

    override fun onScanResult(callbackType: Int, result: ScanResult) {
        val peripheral = result.device
        val localName = result.scanRecord?.deviceName ?: return
        if (!teslaVehicleNameRegex.matches(localName)) {
            return
        }
        if (discoveredPeripherals.value.any { it.address == peripheral.address }) {
            return
        }
        discoveredPeripherals.value = discoveredPeripherals.value + peripheral
    }
}
