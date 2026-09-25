package com.moblin.android.various.model

import com.moblin.android.platform.corebluetooth.CBCentralManager
import com.moblin.android.platform.corebluetooth.CBManagerAuthorization

const val bluetoothNotAllowedMessage = "⚠️ Moblin is not allowed to use Bluetooth"

fun Model.centralManagerDidUpdateState(central: CBCentralManager) {
    bluetoothAllowed.value = CBCentralManager.authorization == CBManagerAuthorization.allowedAlways
}
