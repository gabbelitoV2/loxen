package com.moblin.android.platform.corebluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "CoreBluetooth"

enum class CBManagerState {
    unknown,
    resetting,
    unsupported,
    unauthorized,
    poweredOff,
    poweredOn,
}

enum class CBManagerAuthorization {
    notDetermined,
    restricted,
    denied,
    allowedAlways,
}

fun interface CBCentralManagerDelegate {
    fun centralManagerDidUpdateState(central: CBCentralManager)
}

class CBCentralManager(delegate: CBCentralManagerDelegate?, private val queue: CoroutineScope? = null) {
    @Volatile
    var delegate: CBCentralManagerDelegate? = delegate

    @Volatile
    var state = CBManagerState.unknown
        private set

    @Volatile
    var isScanning = false
        private set

    private var scanCallback: ScanCallback? = null

    init {
        BluetoothAuthorization.add(this)
    }

    fun scanForPeripherals(withServices: List<UUID>?, callback: ScanCallback) {
        val filters = withServices?.map { ScanFilter.Builder().setServiceUuid(ParcelUuid(it)).build() }
        scanForPeripherals(filters, null, callback)
    }

    @Synchronized
    fun scanForPeripherals(filters: List<ScanFilter>?, settings: ScanSettings?, callback: ScanCallback) {
        val currentState = BluetoothAuthorization.state()
        if (currentState != CBManagerState.poweredOn) {
            Log.i(TAG, "Not scanning while $currentState")
            return
        }
        val scanner = BluetoothAuthorization.adapter()?.bluetoothLeScanner
        if (scanner == null) {
            Log.i(TAG, "Not scanning without a scanner")
            return
        }
        stopScan()
        val scanSettings = settings ?: ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        try {
            scanner.startScan(filters, scanSettings, callback)
            scanCallback = callback
            isScanning = true
        } catch (error: SecurityException) {
            Log.w(TAG, "Scan not allowed: ${error.message}")
        } catch (error: IllegalStateException) {
            Log.w(TAG, "Scan failed: ${error.message}")
        }
    }

    @Synchronized
    fun stopScan() {
        val callback = scanCallback ?: return
        scanCallback = null
        isScanning = false
        val scanner = BluetoothAuthorization.adapter()?.bluetoothLeScanner ?: return
        try {
            scanner.stopScan(callback)
        } catch (error: SecurityException) {
            Log.w(TAG, "Stop scan not allowed: ${error.message}")
        } catch (error: IllegalStateException) {
            Log.w(TAG, "Stop scan failed: ${error.message}")
        }
    }

    fun retrievePeripherals(withIdentifiers: List<String>): List<BluetoothDevice> {
        val adapter = BluetoothAuthorization.adapter() ?: return emptyList()
        return withIdentifiers
            .mapNotNull { bluetoothAddress(it) }
            .mapNotNull { runCatching { adapter.getRemoteDevice(it) }.getOrNull() }
    }

    fun connect(
        peripheral: BluetoothDevice,
        callback: BluetoothGattCallback,
        autoConnect: Boolean = false,
    ): BluetoothGatt? {
        val currentState = BluetoothAuthorization.state()
        if (currentState != CBManagerState.poweredOn) {
            Log.i(TAG, "Not connecting while $currentState")
            return null
        }
        val context = BluetoothAuthorization.applicationContext() ?: return null
        return try {
            peripheral.connectGatt(context, autoConnect, callback, BluetoothDevice.TRANSPORT_LE)
        } catch (error: SecurityException) {
            Log.w(TAG, "Connect not allowed: ${error.message}")
            null
        }
    }

    fun cancelPeripheralConnection(peripheral: BluetoothGatt?) {
        peripheral ?: return
        bluetoothCall { peripheral.disconnect() }
        bluetoothCall { peripheral.close() }
    }

    internal fun update(newState: CBManagerState) {
        if (newState == state) {
            return
        }
        state = newState
        if (newState != CBManagerState.poweredOn) {
            synchronized(this) {
                scanCallback = null
                isScanning = false
            }
        }
        val queue = queue
        if (queue == null) {
            delegate?.centralManagerDidUpdateState(this)
        } else {
            queue.launch {
                delegate?.centralManagerDidUpdateState(this@CBCentralManager)
            }
        }
    }

    companion object {
        val authorization: CBManagerAuthorization
            get() = BluetoothAuthorization.authorization()
    }
}

val BluetoothDevice.identifier: UUID
    get() = bluetoothIdentifier(address)

internal fun bluetoothIdentifier(address: String): UUID {
    return UUID(0L, address.replace(":", "").toLongOrNull(16) ?: 0L)
}

internal fun bluetoothAddress(identifier: String): String? {
    val address = identifier.uppercase(Locale.ROOT)
    if (BluetoothAdapter.checkBluetoothAddress(address)) {
        return address
    }
    val uuid = runCatching { UUID.fromString(identifier) }.getOrNull() ?: return null
    if (uuid.mostSignificantBits != 0L || uuid.leastSignificantBits ushr 48 != 0L) {
        return null
    }
    return String.format(Locale.ROOT, "%012X", uuid.leastSignificantBits).chunked(2).joinToString(":")
}

inline fun <T> bluetoothCall(fallback: T, call: () -> T): T {
    return try {
        call()
    } catch (error: SecurityException) {
        bluetoothCallNotAllowed(error)
        fallback
    }
}

inline fun bluetoothCall(call: () -> Unit) {
    bluetoothCall(Unit, call)
}

@PublishedApi
internal fun bluetoothCallNotAllowed(error: SecurityException) {
    Log.w(TAG, "Bluetooth call not allowed: ${error.message}")
}
