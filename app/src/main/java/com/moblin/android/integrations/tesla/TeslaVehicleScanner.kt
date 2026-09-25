package com.moblin.android.integrations.tesla

import com.moblin.android.platform.corebluetooth.CBAdvertisementDataLocalNameKey
import com.moblin.android.platform.corebluetooth.CBCentralManager
import com.moblin.android.platform.corebluetooth.CBCentralManagerDelegate
import com.moblin.android.platform.corebluetooth.CBManagerState
import com.moblin.android.platform.corebluetooth.CBPeripheral
import kotlinx.coroutines.flow.MutableStateFlow

open class TeslaVehicleScanner : CBCentralManagerDelegate {
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
            central.scanForPeripherals(withServices = null)
        }
    }

    override fun centralManagerDidDiscover(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        advertisementData: Map<String, Any>,
        rssi: Int,
    ) {
        val localName = advertisementData[CBAdvertisementDataLocalNameKey] as? String ?: return
        if (!Regex("S[0-9a-f]{16}C").matches(localName)) {
            return
        }
        if (discoveredPeripherals.value.any { it == peripheral }) {
            return
        }
        discoveredPeripherals.value = discoveredPeripherals.value + peripheral
    }

    companion object {
        val shared by lazy { TeslaVehicleScanner() }
    }
}
