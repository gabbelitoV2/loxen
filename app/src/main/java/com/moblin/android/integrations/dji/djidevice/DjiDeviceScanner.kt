package com.moblin.android.integrations.dji.djidevice

import android.util.Log
import com.moblin.android.common.various.hexString
import com.moblin.android.platform.corebluetooth.CBAdvertisementDataManufacturerDataKey
import com.moblin.android.platform.corebluetooth.CBCentralManager
import com.moblin.android.platform.corebluetooth.CBCentralManagerDelegate
import com.moblin.android.platform.corebluetooth.CBManagerState
import com.moblin.android.platform.corebluetooth.CBPeripheral
import com.moblin.android.various.settings.SettingsDjiDeviceModel
import kotlinx.coroutines.flow.MutableStateFlow

data class DjiDiscoveredDevice(
    val peripheral: CBPeripheral,
    val model: SettingsDjiDeviceModel,
)

open class DjiDeviceScanner : CBCentralManagerDelegate {
    val discoveredDevices = MutableStateFlow<List<DjiDiscoveredDevice>>(emptyList())
    private var centralManager: CBCentralManager? by CBCentralManager.holder()

    open fun startScanningForDevices() {
        discoveredDevices.value = emptyList()
        centralManager = CBCentralManager(delegate = this, queue = null)
    }

    open fun stopScanningForDevices() {
        centralManager?.stopScan()
        centralManager = null
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        if (central.state == CBManagerState.poweredOn) {
            central.scanForPeripherals(withServices = null, options = null)
        }
    }

    override fun centralManagerDidDiscover(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        advertisementData: Map<String, Any>,
        rssi: Int,
    ) {
        val manufacturerData =
            advertisementData[CBAdvertisementDataManufacturerDataKey] as? ByteArray ?: return
        if (!isDjiDevice(manufacturerData = manufacturerData)) {
            return
        }
        if (discoveredDevices.value.any { it.peripheral == peripheral }) {
            return
        }
        val model = djiModelFromManufacturerData(data = manufacturerData)
        Log.i(
            TAG,
            "dji-scanner: Manufacturer data ${manufacturerData.hexString()} for " +
                "peripheral id ${peripheral.identifier} and model $model",
        )
        discoveredDevices.value =
            discoveredDevices.value + DjiDiscoveredDevice(peripheral = peripheral, model = model)
    }

    companion object {
        val shared by lazy { DjiDeviceScanner() }

        private const val TAG = "DjiDeviceScanner"
    }
}
