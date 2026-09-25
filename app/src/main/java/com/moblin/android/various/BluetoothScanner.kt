package com.moblin.android.various

import com.moblin.android.platform.corebluetooth.CBCentralManager
import com.moblin.android.platform.corebluetooth.CBCentralManagerDelegate
import com.moblin.android.platform.corebluetooth.CBManagerState
import com.moblin.android.platform.corebluetooth.CBPeripheral
import com.moblin.android.platform.corebluetooth.CBUUID
import kotlinx.coroutines.flow.MutableStateFlow

open class BluetoothScanner(private val serviceIds: List<CBUUID>) : CBCentralManagerDelegate {
    open val discoveredPeripherals = MutableStateFlow<List<CBPeripheral>>(emptyList())

    private var centralManager: CBCentralManager? by CBCentralManager.holder()

    open fun startScanningForDevices() {
        discoveredPeripherals.value = emptyList()
        centralManager = CBCentralManager(delegate = this, queue = null)
    }

    open fun stopScanningForDevices() {
        centralManager?.stopScan()
        centralManager = null
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        if (central.state == CBManagerState.poweredOn) {
            central.scanForPeripherals(withServices = serviceIds)
        }
    }

    override fun centralManagerDidDiscover(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        advertisementData: Map<String, Any>,
        rssi: Int,
    ) {
        if (discoveredPeripherals.value.any { it == peripheral }) {
            return
        }
        discoveredPeripherals.value = discoveredPeripherals.value + peripheral
    }
}
