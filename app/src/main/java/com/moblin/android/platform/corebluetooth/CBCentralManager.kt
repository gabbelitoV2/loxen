package com.moblin.android.platform.corebluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

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

    fun centralManagerDidDiscover(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        advertisementData: Map<String, Any>,
        rssi: Int,
    ) {}

    fun centralManagerDidConnect(central: CBCentralManager, peripheral: CBPeripheral) {}

    fun centralManagerDidFailToConnect(central: CBCentralManager, peripheral: CBPeripheral, error: Throwable?) {}

    fun centralManagerDidDisconnectPeripheral(central: CBCentralManager, peripheral: CBPeripheral, error: Throwable?) {}

    fun centralManagerDidDisconnectPeripheral(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        timestamp: Double,
        isReconnecting: Boolean,
        error: Throwable?,
    ) {
        centralManagerDidDisconnectPeripheral(central, peripheral, error)
    }
}

internal class SerialDispatchQueue(scope: CoroutineScope?) {
    private val dispatcher = scope?.let {
        it.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher ?: Dispatchers.Default
    }
    private val handler = Handler(Looper.getMainLooper())
    private val pending = ArrayDeque<() -> Unit>()
    private val running = Any()
    private val runNextRunnable = Runnable { runNext() }

    fun async(block: () -> Unit) {
        synchronized(this) {
            pending.addLast(block)
        }
        try {
            if (dispatcher == null) {
                handler.post(runNextRunnable)
            } else {
                dispatcher.dispatch(EmptyCoroutineContext, runNextRunnable)
            }
        } catch (error: RuntimeException) {
            Log.w(TAG, "Delegate queue rejected work: ${error.message}")
            synchronized(this) {
                pending.remove(block)
            }
        }
    }

    private fun runNext() {
        synchronized(running) {
            val block = synchronized(this) { pending.removeFirstOrNull() } ?: return
            try {
                block()
            } catch (error: Throwable) {
                Log.e(TAG, "Delegate failed", error)
            }
        }
    }
}

class CBCentralManager(
    delegate: CBCentralManagerDelegate?,
    queue: CoroutineScope? = null,
    @Suppress("UNUSED_PARAMETER") options: Map<String, Any>? = null,
) {
    @Volatile
    var delegate: CBCentralManagerDelegate? = delegate

    @Volatile
    var state = CBManagerState.unknown
        private set

    @Volatile
    var isScanning = false
        private set

    private val dispatchQueue = SerialDispatchQueue(queue)
    private val peripherals = HashMap<String, CBPeripheral>()
    private var scanCallback: ScanCallback? = null
    private var reportedState: CBManagerState? = null
    private var allowDuplicates = false
    private val reportedAdvertisements = HashMap<String, ByteArray?>()

    @Volatile
    private var scanGeneration = 0

    @Volatile
    internal var released = false
        private set

    private val discoveryCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            discovered(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { discovered(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "Scan failed with error $errorCode")
            synchronized(this@CBCentralManager) {
                if (scanCallback === this) {
                    scanCallback = null
                    isScanning = false
                }
            }
        }
    }

    init {
        BluetoothAuthorization.add(this)
    }

    fun scanForPeripherals(withServices: List<CBUUID>?, options: Map<String, Any>? = null) {
        val filters = withServices?.map { ScanFilter.Builder().setServiceUuid(ParcelUuid(it.uuid)).build() }
        synchronized(this) {
            allowDuplicates = options?.get(CBCentralManagerScanOptionAllowDuplicatesKey) == true
            reportedAdvertisements.clear()
        }
        startScan(filters, null, discoveryCallback)
    }

    fun stopScan() {
        synchronized(this) {
            scanGeneration += 1
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
    }

    fun retrievePeripherals(withIdentifiers: List<UUID>): List<CBPeripheral> {
        val adapter = BluetoothAuthorization.adapter() ?: return emptyList()
        return withIdentifiers.mapNotNull { identifier ->
            val address = bluetoothAddress(identifier.toString()) ?: return@mapNotNull null
            synchronized(peripherals) { peripherals[address] }
                ?: runCatching { adapter.getRemoteDevice(address) }.getOrNull()?.let { peripheral(it, scanned = false) }
        }
    }

    fun connect(peripheral: CBPeripheral, options: Map<String, Any>? = null) {
        if (released) {
            return
        }
        val currentState = BluetoothAuthorization.state()
        if (currentState != CBManagerState.poweredOn) {
            Log.i(TAG, "Not connecting while $currentState")
            return
        }
        adopt(peripheral)
        peripheral.connect(autoReconnect = options?.get(CBConnectPeripheralOptionEnableAutoReconnect) == true)
    }

    fun cancelPeripheralConnection(peripheral: CBPeripheral) {
        peripheral.cancelConnection()
    }

    internal fun scanForPeripherals(withServices: List<UUID>?, callback: ScanCallback) {
        val filters = withServices?.map { ScanFilter.Builder().setServiceUuid(ParcelUuid(it)).build() }
        startScan(filters, null, callback)
    }

    internal fun scanForPeripherals(filters: List<ScanFilter>?, settings: ScanSettings?, callback: ScanCallback) {
        startScan(filters, settings, callback)
    }

    @JvmName("retrieveBluetoothDevices")
    internal fun retrievePeripherals(withIdentifiers: List<String>): List<BluetoothDevice> {
        val adapter = BluetoothAuthorization.adapter() ?: return emptyList()
        return withIdentifiers
            .mapNotNull { bluetoothAddress(it) }
            .mapNotNull { runCatching { adapter.getRemoteDevice(it) }.getOrNull() }
    }

    internal fun connect(
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

    internal fun cancelPeripheralConnection(peripheral: BluetoothGatt?) {
        peripheral ?: return
        bluetoothCall { peripheral.disconnect() }
        bluetoothCall { peripheral.close() }
    }

    private fun startScan(filters: List<ScanFilter>?, settings: ScanSettings?, callback: ScanCallback) {
        synchronized(this) {
            if (released) {
                return
            }
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
    }

    private fun discovered(result: ScanResult) {
        val device = result.device ?: return
        val record = result.scanRecord
        val generation = synchronized(this) {
            if (released || scanCallback !== discoveryCallback) {
                return
            }
            val bytes = record?.bytes
            val address = device.address
            if (!allowDuplicates && reportedAdvertisements.containsKey(address) &&
                reportedAdvertisements[address].contentEquals(bytes)
            ) {
                return
            }
            reportedAdvertisements[address] = bytes
            scanGeneration
        }
        val peripheral = peripheral(device, scanned = true)
        record?.deviceName?.let { peripheral.advertisedName = it }
        val advertisementData = advertisementData(result)
        val rssi = result.rssi
        dispatch {
            if (generation == scanGeneration) {
                delegate?.centralManagerDidDiscover(this, peripheral, advertisementData, rssi)
            }
        }
    }

    private fun peripheral(device: BluetoothDevice, scanned: Boolean): CBPeripheral {
        synchronized(peripherals) {
            val address = device.address.uppercase(Locale.ROOT)
            val existing = peripherals[address]
            if (existing != null) {
                if (scanned) {
                    existing.device = device
                    existing.scanned = true
                }
                return existing
            }
            return CBPeripheral(this, device).also {
                it.scanned = scanned
                peripherals[address] = it
            }
        }
    }

    private fun adopt(peripheral: CBPeripheral) {
        val replaced = synchronized(peripherals) {
            val address = peripheral.device.address.uppercase(Locale.ROOT)
            val existing = peripherals[address]
            if (existing === peripheral && peripheral.central === this) {
                return
            }
            peripherals[address] = peripheral
            peripheral.central = this
            existing?.takeIf { it !== peripheral }
        }
        replaced?.invalidate()
    }

    internal fun dispatch(block: () -> Unit) {
        dispatchQueue.async {
            if (!released) {
                block()
            }
        }
    }

    internal fun update(newState: CBManagerState) {
        synchronized(this) {
            if (released || newState == reportedState) {
                return
            }
            reportedState = newState
            if (newState != CBManagerState.poweredOn) {
                scanGeneration += 1
                scanCallback = null
                isScanning = false
            }
        }
        if (newState != CBManagerState.poweredOn) {
            synchronized(peripherals) { peripherals.values.toList() }
                .filter { it.central === this }
                .forEach { it.invalidate() }
        }
        dispatch {
            state = newState
            delegate?.centralManagerDidUpdateState(this)
        }
    }

    internal fun release() {
        synchronized(this) {
            if (released) {
                return
            }
            stopScan()
            released = true
            delegate = null
        }
        BluetoothAuthorization.remove(this)
        synchronized(peripherals) { peripherals.values.toList() }
            .filter { it.central === this }
            .forEach { it.invalidateNow() }
    }

    companion object {
        val authorization: CBManagerAuthorization
            get() = BluetoothAuthorization.authorization()

        fun holder(): ReadWriteProperty<Any?, CBCentralManager?> = CBCentralManagerHolder()
    }
}

private class CBCentralManagerHolder : ReadWriteProperty<Any?, CBCentralManager?> {
    @Volatile
    private var manager: CBCentralManager? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): CBCentralManager? {
        return manager
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: CBCentralManager?) {
        val old = manager
        manager = value
        if (old != null && old !== value) {
            old.release()
        }
    }
}

internal fun advertisementData(result: ScanResult): Map<String, Any> {
    val data = LinkedHashMap<String, Any>()
    val record = result.scanRecord
    if (record != null) {
        record.deviceName?.let { data[CBAdvertisementDataLocalNameKey] = it }
        manufacturerData(record)?.let { data[CBAdvertisementDataManufacturerDataKey] = it }
        record.serviceUuids?.takeIf { it.isNotEmpty() }?.let { uuids ->
            data[CBAdvertisementDataServiceUUIDsKey] = uuids.map { CBUUID(it.uuid) }
        }
        record.serviceData?.takeIf { it.isNotEmpty() }?.let { serviceData ->
            data[CBAdvertisementDataServiceDataKey] = serviceData.entries.associate { (uuid, value) ->
                CBUUID(uuid.uuid) to value
            }
        }
        if (record.txPowerLevel != Int.MIN_VALUE) {
            data[CBAdvertisementDataTxPowerLevelKey] = record.txPowerLevel
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            record.serviceSolicitationUuids?.takeIf { it.isNotEmpty() }?.let { uuids ->
                data[CBAdvertisementDataSolicitedServiceUUIDsKey] = uuids.map { CBUUID(it.uuid) }
            }
        }
    }
    data[CBAdvertisementDataIsConnectable] = result.isConnectable
    return data
}

private fun manufacturerData(record: ScanRecord): ByteArray? {
    val bytes = record.bytes
    if (bytes != null) {
        var index = 0
        while (index + 1 < bytes.size) {
            val length = bytes[index].toInt() and 0xFF
            if (length == 0 || index + length >= bytes.size) {
                break
            }
            if (bytes[index + 1].toInt() and 0xFF == 0xFF) {
                return bytes.copyOfRange(index + 2, index + 1 + length)
            }
            index += length + 1
        }
    }
    val specific = record.manufacturerSpecificData ?: return null
    if (specific.size() == 0) {
        return null
    }
    val companyId = specific.keyAt(0)
    return byteArrayOf(companyId.toByte(), (companyId shr 8).toByte()) + specific.valueAt(0)
}

internal val BluetoothDevice.identifier: UUID
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

internal inline fun <T> bluetoothCall(fallback: T, call: () -> T): T {
    return try {
        call()
    } catch (error: SecurityException) {
        bluetoothCallNotAllowed(error)
        fallback
    }
}

internal inline fun bluetoothCall(call: () -> Unit) {
    bluetoothCall(Unit, call)
}

@PublishedApi
internal fun bluetoothCallNotAllowed(error: SecurityException) {
    Log.w(TAG, "Bluetooth call not allowed: ${error.message}")
}
