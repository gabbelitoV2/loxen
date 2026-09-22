package com.moblin.android.various.model

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

val bluetoothNotAllowedMessage = "⚠️ Moblin is not allowed to use Bluetooth"

fun isBluetoothAllowed(context: Context): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Manifest.permission.BLUETOOTH_CONNECT
    } else {
        Manifest.permission.BLUETOOTH
    }
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

fun Model.centralManagerDidUpdateState(context: Context, central: BluetoothAdapter?) {
    bluetoothAllowed = isBluetoothAllowed(context)
}
