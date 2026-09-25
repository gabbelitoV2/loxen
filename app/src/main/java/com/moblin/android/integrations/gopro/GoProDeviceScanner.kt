package com.moblin.android.integrations.gopro

import com.moblin.android.localized
import com.moblin.android.platform.corebluetooth.CBAdvertisementDataLocalNameKey
import com.moblin.android.platform.corebluetooth.CBCentralManager
import com.moblin.android.platform.corebluetooth.CBCentralManagerDelegate
import com.moblin.android.platform.corebluetooth.CBManagerState
import com.moblin.android.platform.corebluetooth.CBPeripheral
import kotlinx.coroutines.flow.MutableStateFlow

data class GoProDiscoveredDevice(val peripheral: CBPeripheral, val name: String)

class GoProDeviceScanner : CBCentralManagerDelegate {
    val discoveredDevices = MutableStateFlow<List<GoProDiscoveredDevice>>(emptyList())
    private var centralManager: CBCentralManager? by CBCentralManager.holder()

    fun startScanningForDevices() {
        discoveredDevices.value = emptyList()
        centralManager = CBCentralManager(delegate = this, queue = null)
    }

    fun stopScanningForDevices() {
        centralManager?.stopScan()
        centralManager = null
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        if (central.state != CBManagerState.poweredOn) {
            return
        }
        central.scanForPeripherals(withServices = listOf(goProControlServiceId))
    }

    override fun centralManagerDidDiscover(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        advertisementData: Map<String, Any>,
        rssi: Int,
    ) {
        if (discoveredDevices.value.any { it.peripheral.identifier == peripheral.identifier }) {
            return
        }
        val name = (advertisementData[CBAdvertisementDataLocalNameKey] as? String)
            ?: peripheral.name
            ?: localized("Unknown")
        discoveredDevices.value = discoveredDevices.value +
            GoProDiscoveredDevice(peripheral = peripheral, name = name)
    }

    companion object {
        val shared by lazy { GoProDeviceScanner() }
    }
}
