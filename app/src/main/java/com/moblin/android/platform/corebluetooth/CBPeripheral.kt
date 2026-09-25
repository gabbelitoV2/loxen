package com.moblin.android.platform.corebluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.util.UUID

private const val TAG = "CoreBluetooth"

internal const val defaultAttMtu = 23
internal const val requestedAttMtu = 517
internal const val maximumAttributeValueLength = 512
internal const val operationTimeoutMs = 30_000L
internal const val quickOperationTimeoutMs = 5_000L
internal const val minimumReconnectDelayMs = 1_000L
internal const val maximumReconnectDelayMs = 30_000L
internal const val longConnectAttemptMs = 10_000L
internal const val searchTimeoutMs = 10_000L

internal val clientCharacteristicConfigurationId: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

internal val hiddenServiceIds: Set<UUID> = setOf(
    UUID.fromString("00001800-0000-1000-8000-00805F9B34FB"),
    UUID.fromString("00001801-0000-1000-8000-00805F9B34FB"),
)

@Volatile
internal var gattSdkInt = Build.VERSION.SDK_INT

internal fun cfAbsoluteTimeNow(): Double {
    return System.currentTimeMillis() / 1000.0 - 978_307_200.0
}

enum class CBPeripheralState {
    disconnected,
    connecting,
    connected,
    disconnecting,
}

interface CBPeripheralDelegate {
    fun peripheralDidDiscoverServices(peripheral: CBPeripheral, error: Throwable?) {}

    fun peripheralDidDiscoverCharacteristicsFor(peripheral: CBPeripheral, service: CBService, error: Throwable?) {}

    fun peripheralDidDiscoverDescriptorsFor(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: Throwable?,
    ) {}

    fun peripheralDidUpdateValueFor(peripheral: CBPeripheral, characteristic: CBCharacteristic, error: Throwable?) {}

    fun peripheralDidUpdateValueFor(peripheral: CBPeripheral, descriptor: CBDescriptor, error: Throwable?) {}

    fun peripheralDidWriteValueFor(peripheral: CBPeripheral, characteristic: CBCharacteristic, error: Throwable?) {}

    fun peripheralDidWriteValueFor(peripheral: CBPeripheral, descriptor: CBDescriptor, error: Throwable?) {}

    fun peripheralDidUpdateNotificationStateFor(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: Throwable?,
    ) {}

    fun peripheralDidReadRSSI(peripheral: CBPeripheral, rssi: Int, error: Throwable?) {}

    fun peripheralIsReadyToSendWriteWithoutResponse(peripheral: CBPeripheral) {}

    fun peripheralDidModifyServices(peripheral: CBPeripheral, invalidatedServices: List<CBService>) {}
}

class CBPeripheral internal constructor(central: CBCentralManager, device: BluetoothDevice) {
    @Volatile
    internal var central: CBCentralManager = central

    @Volatile
    internal var device: BluetoothDevice = device

    @Volatile
    internal var advertisedName: String? = null

    @Volatile
    internal var scanned = false

    val identifier: UUID = bluetoothIdentifier(device.address)

    @Volatile
    var delegate: CBPeripheralDelegate? = null

    val name: String?
        get() = bluetoothCall(null) { device.name } ?: advertisedName

    @Volatile
    var state = CBPeripheralState.disconnected
        private set

    @Volatile
    var services: List<CBService>? = null
        private set

    val canSendWriteWithoutResponse: Boolean
        get() = synchronized(lock) {
            val canSend = queuedWritesWithoutResponse == 0
            if (!canSend) {
                readyToSendWanted = true
            }
            canSend
        }

    private sealed interface Start {
        object Pending : Start

        object Done : Start

        class Failed(val error: Throwable) : Start
    }

    private val lock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private val reconnectRunnable = Runnable { reconnect() }
    private val searchTimeoutRunnable = Runnable { searchTimedOut() }
    private var gatt: BluetoothGatt? = null
    private var search: ScanCallback? = null
    private var searchFirst = false
    private var wanted = false
    private var autoReconnect = false
    private var attemptStartedAt = 0L
    private var reconnectDelayMs = 0L
    private var gattServicesDiscovered = false
    private val operations = ArrayDeque<Operation>()
    private var current: Operation? = null
    private var starting = false
    private var mtu = defaultAttMtu
    private var queuedWritesWithoutResponse = 0
    private var readyToSendWanted = false
    private val serviceObjects = HashMap<BluetoothGattService, CBService>()
    private val characteristicObjects = HashMap<BluetoothGattCharacteristic, CBCharacteristic>()
    private val descriptorObjects = HashMap<BluetoothGattDescriptor, CBDescriptor>()

    fun discoverServices(serviceUUIDs: List<CBUUID>?) {
        enqueue(DiscoverServices(serviceUUIDs?.toList()))
    }

    fun discoverCharacteristics(characteristicUUIDs: List<CBUUID>?, `for`: CBService) {
        enqueue(DiscoverCharacteristics(characteristicUUIDs?.toList(), `for`))
    }

    fun discoverDescriptors(`for`: CBCharacteristic) {
        enqueue(DiscoverDescriptors(`for`))
    }

    fun readValue(`for`: CBCharacteristic) {
        enqueue(ReadCharacteristic(`for`))
    }

    fun readValue(`for`: CBDescriptor) {
        enqueue(ReadDescriptor(`for`))
    }

    fun writeValue(data: ByteArray, `for`: CBCharacteristic, type: CBCharacteristicWriteType) {
        enqueue(WriteCharacteristic(`for`, data.copyOf(), type))
    }

    fun writeValue(data: ByteArray, `for`: CBDescriptor) {
        enqueue(WriteDescriptor(`for`, data.copyOf()))
    }

    fun setNotifyValue(enabled: Boolean, `for`: CBCharacteristic) {
        enqueue(SetNotifyValue(`for`, enabled))
    }

    fun maximumWriteValueLength(`for`: CBCharacteristicWriteType): Int {
        return when (`for`) {
            CBCharacteristicWriteType.withResponse -> maximumAttributeValueLength
            CBCharacteristicWriteType.withoutResponse -> synchronized(lock) { mtu - 3 }
        }
    }

    fun readRSSI() {
        enqueue(ReadRssi())
    }

    override fun equals(other: Any?): Boolean {
        return other is CBPeripheral && other.identifier == identifier
    }

    override fun hashCode(): Int {
        return identifier.hashCode()
    }

    override fun toString(): String {
        return "CBPeripheral(identifier: $identifier, name: $advertisedName, state: $state)"
    }

    internal fun connect(autoReconnect: Boolean) {
        synchronized(lock) {
            this.autoReconnect = autoReconnect
            if (wanted) {
                return
            }
            wanted = true
            reconnectDelayMs = 0
            state = CBPeripheralState.connecting
            searchFirst = !scanned && !isKnownToAndroid()
            startConnectAttempt()
        }
    }

    internal fun cancelConnection() {
        synchronized(lock) {
            val wasActive = wanted || state != CBPeripheralState.disconnected
            wanted = false
            handler.removeCallbacks(reconnectRunnable)
            closeGatt()
            state = CBPeripheralState.disconnected
            if (!wasActive) {
                return
            }
            val central = central
            val timestamp = cfAbsoluteTimeNow()
            central.dispatch {
                central.delegate?.centralManagerDidDisconnectPeripheral(central, this, timestamp, false, null)
            }
        }
    }

    internal fun invalidate() {
        synchronized(lock) {
            wanted = false
            handler.removeCallbacks(reconnectRunnable)
            closeGatt()
            state = CBPeripheralState.disconnected
        }
    }

    internal fun invalidateNow() {
        val notifying = synchronized(lock) {
            invalidate()
            characteristicObjects.values.toList()
        }
        services = null
        notifying.forEach { it.isNotifying = false }
    }

    private fun startConnectAttempt() {
        closeGatt()
        if (searchFirst && !scanned && startSearch()) {
            return
        }
        connectGatt()
    }

    private fun isKnownToAndroid(): Boolean {
        return bluetoothCall(BluetoothDevice.DEVICE_TYPE_LE) { device.type } != BluetoothDevice.DEVICE_TYPE_UNKNOWN
    }

    private fun startSearch(): Boolean {
        val scanner = BluetoothAuthorization.adapter()?.bluetoothLeScanner ?: return false
        val callback = SearchCallback()
        search = callback
        try {
            val filters = listOf(ScanFilter.Builder().setDeviceAddress(device.address).build())
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
            scanner.startScan(filters, settings, callback)
        } catch (error: RuntimeException) {
            Log.i(TAG, "$identifier: Search failed: ${error.message}")
            search = null
            return false
        }
        Log.i(TAG, "$identifier: Searching, as Android does not know the device yet")
        handler.postDelayed(searchTimeoutRunnable, searchTimeoutMs)
        return true
    }

    private fun stopSearch() {
        val callback = search ?: return
        search = null
        handler.removeCallbacks(searchTimeoutRunnable)
        val scanner = BluetoothAuthorization.adapter()?.bluetoothLeScanner ?: return
        try {
            scanner.stopScan(callback)
        } catch (error: RuntimeException) {
            Log.i(TAG, "$identifier: Stop search failed: ${error.message}")
        }
    }

    private fun searchFound(callback: ScanCallback, result: ScanResult) {
        synchronized(lock) {
            val found = result.device ?: return
            if (search !== callback || !found.address.equals(device.address, ignoreCase = true)) {
                return
            }
            stopSearch()
            device = found
            result.scanRecord?.deviceName?.let { advertisedName = it }
            scanned = true
            connectGatt()
        }
    }

    private fun searchEnded(callback: ScanCallback?) {
        synchronized(lock) {
            if (callback == null || search !== callback) {
                return
            }
            Log.i(TAG, "$identifier: Not found by searching, connecting by address")
            stopSearch()
            connectGatt()
        }
    }

    private fun searchTimedOut() {
        synchronized(lock) {
            searchEnded(search)
        }
    }

    private fun connectGatt() {
        attemptStartedAt = SystemClock.elapsedRealtime()
        val context = BluetoothAuthorization.applicationContext()
        val newGatt = if (context == null) {
            null
        } else {
            gattCall(null) { device.connectGatt(context, false, GattCallback(), BluetoothDevice.TRANSPORT_LE) }
        }
        if (newGatt != null) {
            gatt = newGatt
            return
        }
        Log.i(TAG, "$identifier: Connect failed")
        wanted = false
        state = CBPeripheralState.disconnected
        val central = central
        val error = CBError(CBError.Code.connectionFailed)
        central.dispatch {
            central.delegate?.centralManagerDidFailToConnect(central, this, error)
        }
    }

    private fun reconnect() {
        synchronized(lock) {
            if (!wanted || gatt != null || search != null) {
                return
            }
            startConnectAttempt()
        }
    }

    private fun scheduleReconnect() {
        val elapsed = SystemClock.elapsedRealtime() - attemptStartedAt
        reconnectDelayMs = if (elapsed >= longConnectAttemptMs) {
            minimumReconnectDelayMs
        } else {
            (reconnectDelayMs * 2).coerceIn(minimumReconnectDelayMs, maximumReconnectDelayMs)
        }
        handler.postDelayed(reconnectRunnable, reconnectDelayMs)
    }

    private fun closeGatt() {
        stopSearch()
        val gatt = gatt ?: return
        this.gatt = null
        current?.let { handler.removeCallbacks(it.timeout) }
        current = null
        operations.clear()
        queuedWritesWithoutResponse = 0
        readyToSendWanted = false
        gattServicesDiscovered = false
        mtu = defaultAttMtu
        val characteristics = characteristicObjects.values.toList()
        serviceObjects.clear()
        characteristicObjects.clear()
        descriptorObjects.clear()
        dispatch {
            services = null
            characteristics.forEach { it.isNotifying = false }
        }
        gattCall(Unit) { gatt.disconnect() }
        gattCall(Unit) { gatt.close() }
    }

    private fun connectionStateChanged(gatt: BluetoothGatt, status: Int, newState: Int) {
        synchronized(lock) {
            if (gatt !== this.gatt) {
                return
            }
            val central = central
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                if (state == CBPeripheralState.connected) {
                    return
                }
                state = CBPeripheralState.connected
                reconnectDelayMs = 0
                central.dispatch {
                    central.delegate?.centralManagerDidConnect(central, this)
                }
                operations.addFirst(RequestMtu())
                startNextOperation()
                return
            }
            if (status == BluetoothGatt.GATT_SUCCESS && newState != BluetoothProfile.STATE_DISCONNECTED) {
                return
            }
            val wasConnected = state == CBPeripheralState.connected
            closeGatt()
            if (!wanted) {
                state = CBPeripheralState.disconnected
                return
            }
            if (!wasConnected) {
                Log.i(TAG, "$identifier: Connect attempt ended with status $status, retrying")
                state = CBPeripheralState.connecting
                scheduleReconnect()
                return
            }
            val error = disconnectionError(status)
            val reconnecting = autoReconnect
            val timestamp = cfAbsoluteTimeNow()
            state = if (reconnecting) CBPeripheralState.connecting else CBPeripheralState.disconnected
            wanted = reconnecting
            central.dispatch {
                central.delegate?.centralManagerDidDisconnectPeripheral(central, this, timestamp, reconnecting, error)
            }
            if (reconnecting) {
                reconnectDelayMs = 0
                startConnectAttempt()
            }
        }
    }

    private fun dispatch(block: CBPeripheral.() -> Unit) {
        central.dispatch { this@CBPeripheral.block() }
    }

    private fun enqueue(operation: Operation): Boolean {
        synchronized(lock) {
            if (state != CBPeripheralState.connected || gatt == null) {
                Log.i(TAG, "$identifier: ${operation.javaClass.simpleName} ignored while $state")
                return false
            }
            operations.addLast(operation)
            operation.enqueued()
            startNextOperation()
            return true
        }
    }

    private fun startNextOperation() {
        if (starting) {
            return
        }
        starting = true
        try {
            while (current == null) {
                val gatt = gatt ?: return
                val operation = operations.removeFirstOrNull() ?: return
                current = operation
                handler.postDelayed(operation.timeout, operation.timeoutMs)
                val result = try {
                    operation.start(gatt)
                } catch (error: RuntimeException) {
                    Start.Failed(CBError(CBError.Code.unknown, error.message))
                }
                if (current !== operation) {
                    continue
                }
                when (result) {
                    Start.Pending -> Unit
                    Start.Done -> complete(operation, null)
                    is Start.Failed -> complete(operation, result.error)
                }
            }
        } finally {
            starting = false
        }
    }

    private fun complete(operation: Operation, error: Throwable?) {
        if (current !== operation) {
            return
        }
        current = null
        handler.removeCallbacks(operation.timeout)
        if (error != null) {
            Log.i(TAG, "$identifier: ${operation.javaClass.simpleName} failed: ${error.message}")
        }
        operation.finish(error)
        startNextOperation()
    }

    private fun timedOut(operation: Operation) {
        synchronized(lock) {
            complete(operation, CBError(CBError.Code.connectionTimeout, "${operation.javaClass.simpleName} timed out"))
        }
    }

    private fun serviceObject(service: BluetoothGattService): CBService {
        return serviceObjects.getOrPut(service) { CBService(service, this) }
    }

    private fun characteristicObject(characteristic: BluetoothGattCharacteristic): CBCharacteristic {
        return characteristicObjects.getOrPut(characteristic) {
            CBCharacteristic(characteristic, characteristic.service?.let { serviceObject(it) })
        }
    }

    private fun descriptorObject(descriptor: BluetoothGattDescriptor): CBDescriptor {
        return descriptorObjects.getOrPut(descriptor) {
            CBDescriptor(descriptor, descriptor.characteristic?.let { characteristicObject(it) })
        }
    }

    private fun isCurrent(service: CBService): Boolean {
        return serviceObjects[service.service] === service
    }

    private fun isCurrent(characteristic: CBCharacteristic): Boolean {
        return characteristicObjects[characteristic.characteristic] === characteristic
    }

    private fun isCurrent(descriptor: CBDescriptor): Boolean {
        return descriptorObjects[descriptor.descriptor] === descriptor
    }

    private fun invalidHandle(): Start {
        return Start.Failed(CBError(CBError.Code.invalidHandle, "The attribute is not from the current connection"))
    }

    private fun failed(call: String): Start {
        return Start.Failed(CBError(CBError.Code.unknown, "$call was not accepted"))
    }

    private fun writeCharacteristic(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        writeType: Int,
    ): Boolean {
        if (gattSdkInt >= Build.VERSION_CODES.TIRAMISU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return gattCall(-1) { gatt.writeCharacteristic(characteristic, value, writeType) } ==
                BluetoothStatusCodes.SUCCESS
        }
        return gattCall(false) {
            characteristic.writeType = writeType
            @Suppress("DEPRECATION")
            characteristic.value = value
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
    }

    private fun writeDescriptor(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, value: ByteArray): Boolean {
        if (gattSdkInt >= Build.VERSION_CODES.TIRAMISU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return gattCall(-1) { gatt.writeDescriptor(descriptor, value) } == BluetoothStatusCodes.SUCCESS
        }
        return gattCall(false) {
            @Suppress("DEPRECATION")
            descriptor.value = value
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }
    }

    private abstract inner class Operation {
        open val timeoutMs = operationTimeoutMs
        val timeout = Runnable { timedOut(this@Operation) }

        open fun enqueued() {}

        abstract fun start(gatt: BluetoothGatt): Start

        abstract fun finish(error: Throwable?)
    }

    private inner class RequestMtu : Operation() {
        override val timeoutMs = quickOperationTimeoutMs

        override fun start(gatt: BluetoothGatt): Start {
            return if (gattCall(false) { gatt.requestMtu(requestedAttMtu) }) Start.Pending else Start.Done
        }

        override fun finish(error: Throwable?) {}
    }

    private inner class DiscoverServices(private val serviceUUIDs: List<CBUUID>?) : Operation() {
        var discovered: List<CBService> = emptyList()

        override fun start(gatt: BluetoothGatt): Start {
            if (gattServicesDiscovered) {
                found(gatt)
                return Start.Done
            }
            return if (gattCall(false) { gatt.discoverServices() }) Start.Pending else failed("discoverServices")
        }

        fun found(gatt: BluetoothGatt) {
            gattServicesDiscovered = true
            discovered = gattCall(emptyList<BluetoothGattService>()) { gatt.services.orEmpty() }
                .filter { it.uuid !in hiddenServiceIds }
                .map { serviceObject(it) }
        }

        override fun finish(error: Throwable?) {
            val discovered = discovered
            val matched = discovered.filter { serviceUUIDs == null || it.uuid in serviceUUIDs }
            dispatch {
                if (error == null) {
                    val known = services.orEmpty().toSet() + matched
                    services = discovered.filter { it in known }
                }
                delegate?.peripheralDidDiscoverServices(this, error)
            }
        }
    }

    private inner class DiscoverCharacteristics(
        private val characteristicUUIDs: List<CBUUID>?,
        private val service: CBService,
    ) : Operation() {
        private var discovered: List<CBCharacteristic> = emptyList()

        override fun start(gatt: BluetoothGatt): Start {
            if (!isCurrent(service)) {
                return invalidHandle()
            }
            discovered = service.service.characteristics.orEmpty().map { characteristicObject(it) }
            return Start.Done
        }

        override fun finish(error: Throwable?) {
            val discovered = discovered
            val matched = discovered.filter { characteristicUUIDs == null || it.uuid in characteristicUUIDs }
            val service = service
            dispatch {
                if (error == null) {
                    val known = service.characteristics.orEmpty().toSet() + matched
                    service.characteristics = discovered.filter { it in known }
                }
                delegate?.peripheralDidDiscoverCharacteristicsFor(this, service, error)
            }
        }
    }

    private inner class DiscoverDescriptors(private val characteristic: CBCharacteristic) : Operation() {
        private var discovered: List<CBDescriptor> = emptyList()

        override fun start(gatt: BluetoothGatt): Start {
            if (!isCurrent(characteristic)) {
                return invalidHandle()
            }
            discovered = characteristic.characteristic.descriptors.orEmpty().map { descriptorObject(it) }
            return Start.Done
        }

        override fun finish(error: Throwable?) {
            val discovered = discovered
            val characteristic = characteristic
            dispatch {
                if (error == null) {
                    characteristic.descriptors = discovered
                }
                delegate?.peripheralDidDiscoverDescriptorsFor(this, characteristic, error)
            }
        }
    }

    private inner class ReadCharacteristic(val characteristic: CBCharacteristic) : Operation() {
        var value: ByteArray? = null

        override fun start(gatt: BluetoothGatt): Start {
            if (!isCurrent(characteristic)) {
                return invalidHandle()
            }
            return if (gattCall(false) { gatt.readCharacteristic(characteristic.characteristic) }) {
                Start.Pending
            } else {
                failed("readCharacteristic")
            }
        }

        override fun finish(error: Throwable?) {
            val value = value
            val characteristic = characteristic
            dispatch {
                if (error == null) {
                    characteristic.value = value
                }
                delegate?.peripheralDidUpdateValueFor(this, characteristic, error)
            }
        }
    }

    private inner class WriteCharacteristic(
        val characteristic: CBCharacteristic,
        private val data: ByteArray,
        private val type: CBCharacteristicWriteType,
    ) : Operation() {
        override val timeoutMs = if (type == CBCharacteristicWriteType.withResponse) {
            operationTimeoutMs
        } else {
            quickOperationTimeoutMs
        }

        override fun enqueued() {
            if (type == CBCharacteristicWriteType.withoutResponse) {
                queuedWritesWithoutResponse += 1
            }
        }

        override fun start(gatt: BluetoothGatt): Start {
            if (!isCurrent(characteristic)) {
                return invalidHandle()
            }
            val writeType = if (type == CBCharacteristicWriteType.withResponse) {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            return if (writeCharacteristic(gatt, characteristic.characteristic, data, writeType)) {
                Start.Pending
            } else {
                failed("writeCharacteristic")
            }
        }

        override fun finish(error: Throwable?) {
            val characteristic = characteristic
            if (type == CBCharacteristicWriteType.withResponse) {
                dispatch {
                    delegate?.peripheralDidWriteValueFor(this, characteristic, error)
                }
                return
            }
            queuedWritesWithoutResponse = (queuedWritesWithoutResponse - 1).coerceAtLeast(0)
            if (queuedWritesWithoutResponse == 0 && readyToSendWanted) {
                readyToSendWanted = false
                dispatch {
                    delegate?.peripheralIsReadyToSendWriteWithoutResponse(this)
                }
            }
        }
    }

    private inner class SetNotifyValue(val characteristic: CBCharacteristic, private val enabled: Boolean) :
        Operation() {
        var descriptor: BluetoothGattDescriptor? = null

        override fun start(gatt: BluetoothGatt): Start {
            if (!isCurrent(characteristic)) {
                return invalidHandle()
            }
            val properties = characteristic.characteristic.properties
            val notify = properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
            val indicate = properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
            if (!notify && !indicate) {
                return Start.Failed(CBATTError(CBATTError.Code.requestNotSupported))
            }
            if (!gattCall(false) { gatt.setCharacteristicNotification(characteristic.characteristic, enabled) }) {
                return failed("setCharacteristicNotification")
            }
            val descriptor = characteristic.characteristic.getDescriptor(clientCharacteristicConfigurationId)
                ?: return Start.Done
            this.descriptor = descriptor
            val value = when {
                !enabled -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                notify -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                else -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            }
            return if (writeDescriptor(gatt, descriptor, value)) Start.Pending else failed("writeDescriptor")
        }

        override fun finish(error: Throwable?) {
            val characteristic = characteristic
            val enabled = enabled
            dispatch {
                if (error == null) {
                    characteristic.isNotifying = enabled
                }
                delegate?.peripheralDidUpdateNotificationStateFor(this, characteristic, error)
            }
        }
    }

    private inner class ReadDescriptor(val descriptor: CBDescriptor) : Operation() {
        var value: ByteArray? = null

        override fun start(gatt: BluetoothGatt): Start {
            if (!isCurrent(descriptor)) {
                return invalidHandle()
            }
            return if (gattCall(false) { gatt.readDescriptor(descriptor.descriptor) }) {
                Start.Pending
            } else {
                failed("readDescriptor")
            }
        }

        override fun finish(error: Throwable?) {
            val value = value
            val descriptor = descriptor
            dispatch {
                if (error == null && value != null) {
                    descriptor.value = descriptorValue(descriptor.uuid, value)
                }
                delegate?.peripheralDidUpdateValueFor(this, descriptor, error)
            }
        }
    }

    private inner class WriteDescriptor(val descriptor: CBDescriptor, private val data: ByteArray) : Operation() {
        override fun start(gatt: BluetoothGatt): Start {
            if (!isCurrent(descriptor)) {
                return invalidHandle()
            }
            return if (writeDescriptor(gatt, descriptor.descriptor, data)) Start.Pending else failed("writeDescriptor")
        }

        override fun finish(error: Throwable?) {
            val descriptor = descriptor
            dispatch {
                delegate?.peripheralDidWriteValueFor(this, descriptor, error)
            }
        }
    }

    private inner class ReadRssi : Operation() {
        var rssi = 0

        override fun start(gatt: BluetoothGatt): Start {
            return if (gattCall(false) { gatt.readRemoteRssi() }) Start.Pending else failed("readRemoteRssi")
        }

        override fun finish(error: Throwable?) {
            val rssi = rssi
            dispatch {
                delegate?.peripheralDidReadRSSI(this, rssi, error)
            }
        }
    }

    private fun mtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
        synchronized(lock) {
            if (gatt !== this.gatt) {
                return
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                this.mtu = mtu.coerceIn(defaultAttMtu, requestedAttMtu)
            }
            (current as? RequestMtu)?.let { complete(it, gattError(status)) }
        }
    }

    private fun servicesDiscovered(gatt: BluetoothGatt, status: Int) {
        synchronized(lock) {
            if (gatt !== this.gatt) {
                return
            }
            val operation = current as? DiscoverServices ?: return
            val error = gattError(status)
            if (error == null) {
                operation.found(gatt)
            }
            complete(operation, error)
        }
    }

    private fun serviceChanged(gatt: BluetoothGatt) {
        synchronized(lock) {
            if (gatt !== this.gatt) {
                return
            }
            gattServicesDiscovered = false
            dispatch {
                val invalidated = services.orEmpty()
                services = null
                delegate?.peripheralDidModifyServices(this, invalidated)
            }
        }
    }

    private fun characteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int,
    ) {
        synchronized(lock) {
            if (gatt !== this.gatt) {
                return
            }
            val operation = current as? ReadCharacteristic ?: return
            if (operation.characteristic.characteristic !== characteristic) {
                return
            }
            operation.value = value
            complete(operation, gattError(status))
        }
    }

    private fun characteristicWritten(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        synchronized(lock) {
            if (gatt !== this.gatt) {
                return
            }
            val operation = current as? WriteCharacteristic ?: return
            if (operation.characteristic.characteristic !== characteristic) {
                return
            }
            complete(operation, gattError(status))
        }
    }

    private fun characteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        val changed = synchronized(lock) {
            if (gatt !== this.gatt) {
                return
            }
            characteristicObject(characteristic)
        }
        dispatch {
            changed.value = value
            delegate?.peripheralDidUpdateValueFor(this, changed, null)
        }
    }

    private fun descriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, value: ByteArray, status: Int) {
        synchronized(lock) {
            if (gatt !== this.gatt) {
                return
            }
            val operation = current as? ReadDescriptor ?: return
            if (operation.descriptor.descriptor !== descriptor) {
                return
            }
            operation.value = value
            complete(operation, gattError(status))
        }
    }

    private fun descriptorWritten(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        synchronized(lock) {
            if (gatt !== this.gatt) {
                return
            }
            when (val operation = current) {
                is SetNotifyValue -> if (operation.descriptor === descriptor) {
                    complete(operation, gattError(status))
                }
                is WriteDescriptor -> if (operation.descriptor.descriptor === descriptor) {
                    complete(operation, gattError(status))
                }
                else -> Unit
            }
        }
    }

    private fun rssiRead(gatt: BluetoothGatt, rssi: Int, status: Int) {
        synchronized(lock) {
            if (gatt !== this.gatt) {
                return
            }
            val operation = current as? ReadRssi ?: return
            operation.rssi = rssi
            complete(operation, gattError(status))
        }
    }

    private inner class SearchCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handler.post { searchFound(this@SearchCallback, result) }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            for (result in results) {
                handler.post { searchFound(this@SearchCallback, result) }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.i(TAG, "$identifier: Search failed with error $errorCode")
            handler.post { searchEnded(this@SearchCallback) }
        }
    }

    private inner class GattCallback : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            connectionStateChanged(gatt, status, newState)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            mtuChanged(gatt, mtu, status)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            servicesDiscovered(gatt, status)
        }

        override fun onServiceChanged(gatt: BluetoothGatt) {
            serviceChanged(gatt)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            characteristicRead(gatt, characteristic, value, status)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            @Suppress("DEPRECATION")
            characteristicRead(gatt, characteristic, characteristic.value?.copyOf() ?: ByteArray(0), status)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            characteristicWritten(gatt, characteristic, status)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            characteristicChanged(gatt, characteristic, value)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            characteristicChanged(gatt, characteristic, characteristic.value?.copyOf() ?: ByteArray(0))
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
            value: ByteArray,
        ) {
            descriptorRead(gatt, descriptor, value, status)
        }

        @Deprecated("Deprecated in Java")
        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            @Suppress("DEPRECATION")
            descriptorRead(gatt, descriptor, descriptor.value?.copyOf() ?: ByteArray(0), status)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            descriptorWritten(gatt, descriptor, status)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            rssiRead(gatt, rssi, status)
        }
    }
}

internal inline fun <T> gattCall(fallback: T, call: () -> T): T {
    return try {
        bluetoothCall(fallback, call)
    } catch (error: RuntimeException) {
        gattCallFailed(error)
        fallback
    }
}

@PublishedApi
internal fun gattCallFailed(error: RuntimeException) {
    Log.w(TAG, "Bluetooth call failed: ${error.message}")
}
