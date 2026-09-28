package com.moblin.android.various.model

import com.moblin.android.platform.corebluetooth.CBCentralManager
import com.moblin.android.platform.corebluetooth.CBManagerAuthorization
import com.moblin.android.localized

val bluetoothNotAllowedMessage = com.moblin.android.localized("⚠️ Moblin is not allowed to use Bluetooth")

fun Model.centralManagerDidUpdateState(central: CBCentralManager) {
    bluetoothAllowed.value = CBCentralManager.authorization == CBManagerAuthorization.allowedAlways
}
