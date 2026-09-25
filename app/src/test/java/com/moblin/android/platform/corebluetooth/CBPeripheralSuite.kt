package com.moblin.android.platform.corebluetooth

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import java.time.Duration
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothDevice
import org.robolectric.shadows.ShadowBluetoothGatt
import org.robolectric.shadows.ShadowBluetoothLeScanner

private const val peripheralAddress = "C0:FF:EE:00:00:01"
private const val forgottenAddress = "F4:12:FA:00:00:02"

private fun uuid16(value: String): UUID = UUID.fromString("0000$value-0000-1000-8000-00805F9B34FB")

private fun hex(bytes: ByteArray?): String = bytes?.joinToString("") { "%02x".format(it) } ?: "nil"

private fun short(uuid: UUID): String = CBUUID(uuid).uuidString

@Implements(BluetoothGatt::class)
class RecordingBluetoothGatt : ShadowBluetoothGatt() {
    val calls = mutableListOf<String>()
    var discovered: List<BluetoothGattService> = emptyList()
    var accept = true
    var refuse = false

    private fun record(call: String): Boolean {
        if (refuse) {
            throw SecurityException("Need android.permission.BLUETOOTH_CONNECT permission for AttributionSource")
        }
        calls.add(call)
        return accept
    }

    @Implementation
    public override fun discoverServices(): Boolean = record("discoverServices")

    @Implementation
    public override fun requestMtu(mtu: Int): Boolean = record("requestMtu $mtu")

    @Implementation
    public override fun getServices(): List<BluetoothGattService> = discovered

    @Implementation
    public override fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enable: Boolean,
    ): Boolean = record("setCharacteristicNotification ${short(characteristic.uuid)} $enable")

    @Suppress("DEPRECATION")
    @Implementation
    public override fun writeDescriptor(descriptor: BluetoothGattDescriptor): Boolean {
        return record("writeDescriptor ${short(descriptor.characteristic.uuid)} ${hex(descriptor.value)}")
    }

    @Implementation(minSdk = Build.VERSION_CODES.TIRAMISU)
    fun writeDescriptor(descriptor: BluetoothGattDescriptor, value: ByteArray): Int {
        return if (record("writeDescriptor ${short(descriptor.characteristic.uuid)} ${hex(value)}")) 0 else 201
    }

    @Suppress("DEPRECATION")
    @Implementation
    public override fun writeCharacteristic(characteristic: BluetoothGattCharacteristic): Boolean {
        val call = "writeCharacteristic ${short(characteristic.uuid)} ${hex(characteristic.value)} ${characteristic.writeType}"
        return record(call)
    }

    @Implementation(minSdk = Build.VERSION_CODES.TIRAMISU)
    public override fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        writeType: Int,
    ): Int {
        return if (record("writeCharacteristic ${short(characteristic.uuid)} ${hex(value)} $writeType")) 0 else 201
    }

    @Implementation
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic): Boolean {
        return record("readCharacteristic ${short(characteristic.uuid)}")
    }

    @Implementation
    fun readDescriptor(descriptor: BluetoothGattDescriptor): Boolean {
        return record("readDescriptor ${short(descriptor.uuid)}")
    }

    @Implementation
    fun readRemoteRssi(): Boolean = record("readRemoteRssi")
}

private class Recorder : CBCentralManagerDelegate, CBPeripheralDelegate {
    val events = mutableListOf<String>()
    var onDiscoverServices: ((CBPeripheral) -> Unit)? = null
    var onDiscoverCharacteristics: ((CBPeripheral, CBService) -> Unit)? = null

    private fun error(error: Throwable?): String = when (error) {
        null -> "nil"
        is CBError -> "CBError.${error.code}"
        is CBATTError -> "CBATTError.${error.code}"
        else -> error.javaClass.simpleName
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        events.add("didUpdateState ${central.state}")
    }

    override fun centralManagerDidConnect(central: CBCentralManager, peripheral: CBPeripheral) {
        events.add("didConnect")
    }

    override fun centralManagerDidFailToConnect(central: CBCentralManager, peripheral: CBPeripheral, error: Throwable?) {
        events.add("didFailToConnect ${error(error)}")
    }

    override fun centralManagerDidDisconnectPeripheral(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        error: Throwable?,
    ) {
        events.add("didDisconnectPeripheral ${error(error)}")
    }

    override fun peripheralDidDiscoverServices(peripheral: CBPeripheral, error: Throwable?) {
        events.add("didDiscoverServices ${peripheral.services?.map { it.uuid.uuidString }} ${error(error)}")
        onDiscoverServices?.invoke(peripheral)
    }

    override fun peripheralDidDiscoverCharacteristicsFor(peripheral: CBPeripheral, service: CBService, error: Throwable?) {
        events.add(
            "didDiscoverCharacteristicsFor ${service.uuid} ${service.characteristics?.map { it.uuid.uuidString }} " +
                error(error),
        )
        onDiscoverCharacteristics?.invoke(peripheral, service)
    }

    override fun peripheralDidUpdateValueFor(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: Throwable?,
    ) {
        events.add("didUpdateValueFor ${characteristic.uuid} ${hex(characteristic.value)} ${error(error)}")
    }

    override fun peripheralDidWriteValueFor(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: Throwable?,
    ) {
        events.add("didWriteValueFor ${characteristic.uuid} ${error(error)}")
    }

    override fun peripheralDidUpdateNotificationStateFor(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: Throwable?,
    ) {
        events.add("didUpdateNotificationStateFor ${characteristic.uuid} ${characteristic.isNotifying} ${error(error)}")
    }

    override fun peripheralDidReadRSSI(peripheral: CBPeripheral, rssi: Int, error: Throwable?) {
        events.add("didReadRSSI $rssi ${error(error)}")
    }

    override fun peripheralIsReadyToSendWriteWithoutResponse(peripheral: CBPeripheral) {
        events.add("isReadyToSendWriteWithoutResponse")
    }

    override fun peripheralDidUpdateValueFor(peripheral: CBPeripheral, descriptor: CBDescriptor, error: Throwable?) {
        events.add("didUpdateValueFor descriptor ${descriptor.uuid} ${descriptor.value} ${error(error)}")
    }

    override fun peripheralDidDiscoverDescriptorsFor(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: Throwable?,
    ) {
        events.add("didDiscoverDescriptorsFor ${characteristic.uuid} ${characteristic.descriptors?.map { it.uuid.uuidString }}")
    }
}

private class ReconnectingRecorder(private val recorder: Recorder) : CBCentralManagerDelegate by recorder {
    override fun centralManagerDidDisconnectPeripheral(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        timestamp: Double,
        isReconnecting: Boolean,
        error: Throwable?,
    ) {
        recorder.events.add("didDisconnectPeripheral isReconnecting $isReconnecting ${error?.javaClass?.simpleName}")
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [RecordingBluetoothGatt::class])
class CBPeripheralSuite {
    private lateinit var application: Application
    private val recorder = Recorder()
    private val heartRateService = uuid16("180D")
    private val heartRateMeasurement = uuid16("2A37")
    private val bodySensorLocation = uuid16("2A38")
    private val controlPoint = uuid16("2A39")
    private val indication = uuid16("2A3A")
    private val clientConfiguration = uuid16("2902")

    @Before
    fun setUp() {
        BluetoothAuthorization.reset()
        gattSdkInt = Build.VERSION.SDK_INT
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        shadowOf(application.packageManager).setSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE, true)
        shadowOf(adapter()).setState(BluetoothAdapter.STATE_ON)
        Shadow.extract<ShadowBluetoothDevice>(device()).setType(BluetoothDevice.DEVICE_TYPE_LE)
    }

    @After
    fun tearDown() {
        gattSdkInt = Build.VERSION.SDK_INT
        BluetoothAuthorization.reset()
    }

    private fun adapter(): BluetoothAdapter = application.getSystemService(BluetoothManager::class.java).adapter

    private fun device(): BluetoothDevice = adapter().getRemoteDevice(peripheralAddress)

    private fun gatts(): List<BluetoothGatt> = Shadow.extract<ShadowBluetoothDevice>(device()).bluetoothGatts

    private fun gatt(): BluetoothGatt = gatts().last()

    private fun recording(gatt: BluetoothGatt = gatt()): RecordingBluetoothGatt = Shadow.extract(gatt)

    private fun callback(gatt: BluetoothGatt = gatt()): BluetoothGattCallback = recording(gatt).gattCallback

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun advance(seconds: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))
    }

    private fun setAdapterState(state: Int) {
        shadowOf(adapter()).setState(state)
        application.sendBroadcast(
            Intent(BluetoothAdapter.ACTION_STATE_CHANGED).putExtra(BluetoothAdapter.EXTRA_STATE, state),
        )
        runMain()
    }

    private fun services(): List<BluetoothGattService> {
        val service = BluetoothGattService(heartRateService, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val permissions = BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        for ((uuid, properties) in listOf(
            heartRateMeasurement to BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            bodySensorLocation to (BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE),
            controlPoint to (
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                    BluetoothGattCharacteristic.PROPERTY_INDICATE
                ),
            indication to BluetoothGattCharacteristic.PROPERTY_INDICATE,
        )) {
            val characteristic = BluetoothGattCharacteristic(uuid, properties, BluetoothGattCharacteristic.PERMISSION_READ)
            if (properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                characteristic.addDescriptor(BluetoothGattDescriptor(clientConfiguration, permissions))
            }
            service.addCharacteristic(characteristic)
        }
        return listOf(service, BluetoothGattService(uuid16("180F"), BluetoothGattService.SERVICE_TYPE_PRIMARY))
    }

    private fun manager(queue: CoroutineScope? = null, delegate: CBCentralManagerDelegate = recorder): CBCentralManager {
        val central = CBCentralManager(delegate = delegate, queue = queue)
        runMain()
        return central
    }

    private fun connect(central: CBCentralManager, options: Map<String, Any>? = null): CBPeripheral {
        val peripheral = central.retrievePeripherals(withIdentifiers = listOf(bluetoothIdentifier(peripheralAddress))).single()
        peripheral.delegate = recorder
        central.connect(peripheral, options = options)
        callback().onConnectionStateChange(gatt(), BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        runMain()
        return peripheral
    }

    private fun connectAndDiscover(central: CBCentralManager): Pair<CBPeripheral, CBService> {
        val peripheral = connect(central)
        callback().onMtuChanged(gatt(), 247, BluetoothGatt.GATT_SUCCESS)
        peripheral.discoverServices(null)
        recording().discovered = services()
        callback().onServicesDiscovered(gatt(), BluetoothGatt.GATT_SUCCESS)
        runMain()
        val service = peripheral.services!!.first()
        peripheral.discoverCharacteristics(null, `for` = service)
        runMain()
        recorder.events.clear()
        recording().calls.clear()
        return peripheral to service
    }

    private fun CBService.characteristic(uuid: UUID): CBCharacteristic {
        return characteristics!!.single { it.uuid == CBUUID(uuid) }
    }

    private fun gattCharacteristic(uuid: UUID): BluetoothGattCharacteristic {
        return recording().discovered.first().getCharacteristic(uuid)
    }

    @Test
    fun connectingRequestsTheLargestMtuFirstAndReportsDidConnect() {
        val central = manager()
        val peripheral = central.retrievePeripherals(withIdentifiers = listOf(bluetoothIdentifier(peripheralAddress))).single()
        peripheral.delegate = recorder
        assertEquals(CBPeripheralState.disconnected, peripheral.state)
        central.connect(peripheral)
        assertEquals(CBPeripheralState.connecting, peripheral.state)
        assertEquals(1, gatts().size)
        callback().onConnectionStateChange(gatt(), BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        assertEquals(CBPeripheralState.connected, peripheral.state)
        assertEquals(listOf("requestMtu 517"), recording().calls)
        assertEquals(20, peripheral.maximumWriteValueLength(`for` = CBCharacteristicWriteType.withoutResponse))
        assertEquals(512, peripheral.maximumWriteValueLength(`for` = CBCharacteristicWriteType.withResponse))
        peripheral.discoverServices(listOf(CBUUID(string = "180D")))
        assertEquals(listOf("requestMtu 517"), recording().calls)
        runMain()
        assertEquals(listOf("didUpdateState poweredOn", "didConnect"), recorder.events)
        callback().onMtuChanged(gatt(), 247, BluetoothGatt.GATT_SUCCESS)
        assertEquals(244, peripheral.maximumWriteValueLength(`for` = CBCharacteristicWriteType.withoutResponse))
        assertEquals(listOf("requestMtu 517", "discoverServices"), recording().calls)
        recording().discovered = services()
        callback().onServicesDiscovered(gatt(), BluetoothGatt.GATT_SUCCESS)
        assertNull(peripheral.services)
        runMain()
        assertEquals("didDiscoverServices [180D] nil", recorder.events.last())
        assertEquals(listOf(CBUUID(string = "180D")), peripheral.services!!.map { it.uuid })
        assertSame(peripheral, peripheral.services!!.single().peripheral)
    }

    @Test
    fun anMtuRequestThatIsNeverAnsweredDoesNotBlockTheQueue() {
        val central = manager()
        val peripheral = connect(central)
        peripheral.discoverServices(null)
        assertEquals(listOf("requestMtu 517"), recording().calls)
        advance(5)
        assertEquals(listOf("requestMtu 517", "discoverServices"), recording().calls)
        assertEquals(20, peripheral.maximumWriteValueLength(`for` = CBCharacteristicWriteType.withoutResponse))
    }

    @Test
    fun discoveredServicesAndCharacteristicsAreFilteredLikeCoreBluetooth() {
        val central = manager()
        val peripheral = connect(central)
        callback().onMtuChanged(gatt(), 185, BluetoothGatt.GATT_SUCCESS)
        recorder.onDiscoverServices = { discovered ->
            for (service in discovered.services.orEmpty()) {
                discovered.discoverCharacteristics(listOf(CBUUID(string = "2A37"), CBUUID(string = "2A38")), `for` = service)
            }
        }
        peripheral.discoverServices(null)
        recording().discovered = services()
        callback().onServicesDiscovered(gatt(), BluetoothGatt.GATT_SUCCESS)
        runMain()
        assertEquals(
            listOf(
                "didUpdateState poweredOn",
                "didConnect",
                "didDiscoverServices [180D, 180F] nil",
                "didDiscoverCharacteristicsFor 180D [2A37, 2A38] nil",
                "didDiscoverCharacteristicsFor 180F [] nil",
            ),
            recorder.events,
        )
        val service = peripheral.services!!.first()
        val measurement = service.characteristic(heartRateMeasurement)
        assertSame(service, measurement.service)
        assertTrue(measurement.properties.contains(CBCharacteristicProperties.notify))
        assertFalse(measurement.properties.contains(CBCharacteristicProperties.read))
        recorder.onDiscoverServices = null
        peripheral.discoverCharacteristics(listOf(CBUUID(string = "2A39")), `for` = service)
        runMain()
        assertEquals("didDiscoverCharacteristicsFor 180D [2A37, 2A38, 2A39] nil", recorder.events.last())
        assertSame(measurement, service.characteristic(heartRateMeasurement))
        peripheral.discoverDescriptors(`for` = measurement)
        runMain()
        assertEquals("didDiscoverDescriptorsFor 2A37 [2902]", recorder.events.last())
        assertEquals(listOf("requestMtu 517", "discoverServices"), recording().calls)
    }

    @Test
    fun operationsReachAndroidOneAtATimeInOrder() {
        val central = manager()
        val (peripheral, service) = connectAndDiscover(central)
        val location = service.characteristic(bodySensorLocation)
        peripheral.writeValue(byteArrayOf(1), `for` = location, type = CBCharacteristicWriteType.withResponse)
        peripheral.readValue(`for` = location)
        peripheral.writeValue(byteArrayOf(2), `for` = location, type = CBCharacteristicWriteType.withResponse)
        peripheral.readRSSI()
        assertEquals(listOf("writeCharacteristic 2A38 01 2"), recording().calls)
        callback().onCharacteristicWrite(gatt(), gattCharacteristic(bodySensorLocation), BluetoothGatt.GATT_SUCCESS)
        assertEquals(listOf("writeCharacteristic 2A38 01 2", "readCharacteristic 2A38"), recording().calls)
        callback().onCharacteristicRead(
            gatt(),
            gattCharacteristic(bodySensorLocation),
            byteArrayOf(7),
            BluetoothGatt.GATT_SUCCESS,
        )
        assertEquals("writeCharacteristic 2A38 02 2", recording().calls.last())
        callback().onCharacteristicWrite(
            gatt(),
            gattCharacteristic(bodySensorLocation),
            BluetoothGatt.GATT_WRITE_NOT_PERMITTED,
        )
        assertEquals("readRemoteRssi", recording().calls.last())
        callback().onReadRemoteRssi(gatt(), -58, BluetoothGatt.GATT_SUCCESS)
        assertNull(location.value)
        runMain()
        assertEquals(
            listOf(
                "didWriteValueFor 2A38 nil",
                "didUpdateValueFor 2A38 07 nil",
                "didWriteValueFor 2A38 CBATTError.writeNotPermitted",
                "didReadRSSI -58 nil",
            ),
            recorder.events,
        )
        assertContentEquals(byteArrayOf(7), location.value)
    }

    @Test
    fun setNotifyValueEnablesNotificationsThroughTheClientConfigurationDescriptorOneAtATime() {
        val central = manager()
        val (peripheral, service) = connectAndDiscover(central)
        val measurement = service.characteristic(heartRateMeasurement)
        val both = service.characteristic(controlPoint)
        val indicated = service.characteristic(indication)
        for (characteristic in listOf(measurement, both, indicated)) {
            peripheral.setNotifyValue(true, `for` = characteristic)
        }
        assertEquals(listOf("setCharacteristicNotification 2A37 true", "writeDescriptor 2A37 0100"), recording().calls)
        val descriptor = gattCharacteristic(heartRateMeasurement).getDescriptor(clientConfiguration)
        @Suppress("DEPRECATION")
        assertNull(descriptor.value)
        callback().onDescriptorWrite(gatt(), descriptor, BluetoothGatt.GATT_SUCCESS)
        assertEquals(
            listOf("setCharacteristicNotification 2A39 true", "writeDescriptor 2A39 0100"),
            recording().calls.drop(2),
        )
        callback().onDescriptorWrite(gatt(), gattCharacteristic(controlPoint).getDescriptor(clientConfiguration), 0)
        assertEquals(
            listOf("setCharacteristicNotification 2A3A true", "writeDescriptor 2A3A 0200"),
            recording().calls.drop(4),
        )
        callback().onDescriptorWrite(
            gatt(),
            gattCharacteristic(indication).getDescriptor(clientConfiguration),
            BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION,
        )
        assertFalse(measurement.isNotifying)
        runMain()
        assertEquals(
            listOf(
                "didUpdateNotificationStateFor 2A37 true nil",
                "didUpdateNotificationStateFor 2A39 true nil",
                "didUpdateNotificationStateFor 2A3A false CBATTError.insufficientAuthentication",
            ),
            recorder.events,
        )
        assertTrue(measurement.isNotifying)
        callback().onCharacteristicChanged(gatt(), gattCharacteristic(heartRateMeasurement), byteArrayOf(0, 72))
        callback().onCharacteristicChanged(gatt(), gattCharacteristic(heartRateMeasurement), byteArrayOf(0, 73))
        runMain()
        assertEquals(
            listOf("didUpdateValueFor 2A37 0048 nil", "didUpdateValueFor 2A37 0049 nil"),
            recorder.events.drop(3),
        )
        assertContentEquals(byteArrayOf(0, 73), measurement.value)
        peripheral.setNotifyValue(false, `for` = measurement)
        assertEquals(
            listOf("setCharacteristicNotification 2A37 false", "writeDescriptor 2A37 0000"),
            recording().calls.drop(6),
        )
        callback().onDescriptorWrite(gatt(), descriptor, BluetoothGatt.GATT_SUCCESS)
        runMain()
        assertEquals("didUpdateNotificationStateFor 2A37 false nil", recorder.events.last())
    }

    @Test
    fun setNotifyValueOnACharacteristicThatCannotNotifyFailsWithoutTouchingAndroid() {
        val central = manager()
        val (peripheral, service) = connectAndDiscover(central)
        peripheral.setNotifyValue(true, `for` = service.characteristic(bodySensorLocation))
        runMain()
        assertEquals(emptyList(), recording().calls)
        assertEquals(listOf("didUpdateNotificationStateFor 2A38 false CBATTError.requestNotSupported"), recorder.events)
    }

    @Test
    fun androidTwelveAndOlderUseTheValueOfTheAttribute() {
        gattSdkInt = Build.VERSION_CODES.S_V2
        val central = manager()
        val (peripheral, service) = connectAndDiscover(central)
        peripheral.setNotifyValue(true, `for` = service.characteristic(heartRateMeasurement))
        assertEquals(listOf("setCharacteristicNotification 2A37 true", "writeDescriptor 2A37 0100"), recording().calls)
        val descriptor = gattCharacteristic(heartRateMeasurement).getDescriptor(clientConfiguration)
        @Suppress("DEPRECATION")
        assertContentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, descriptor.value)
        callback().onDescriptorWrite(gatt(), descriptor, 0)
        val location = service.characteristic(bodySensorLocation)
        peripheral.writeValue(byteArrayOf(9, 8), `for` = location, type = CBCharacteristicWriteType.withoutResponse)
        assertEquals("writeCharacteristic 2A38 0908 1", recording().calls.last())
        assertEquals(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE, gattCharacteristic(bodySensorLocation).writeType)
        callback().onCharacteristicWrite(gatt(), gattCharacteristic(bodySensorLocation), 0)
        peripheral.readValue(`for` = location)
        @Suppress("DEPRECATION")
        gattCharacteristic(bodySensorLocation).value = byteArrayOf(5)
        @Suppress("DEPRECATION")
        callback().onCharacteristicRead(gatt(), gattCharacteristic(bodySensorLocation), 0)
        @Suppress("DEPRECATION")
        gattCharacteristic(heartRateMeasurement).value = byteArrayOf(0, 60)
        @Suppress("DEPRECATION")
        callback().onCharacteristicChanged(gatt(), gattCharacteristic(heartRateMeasurement))
        runMain()
        assertEquals(
            listOf(
                "didUpdateNotificationStateFor 2A37 true nil",
                "didUpdateValueFor 2A38 05 nil",
                "didUpdateValueFor 2A37 003c nil",
            ),
            recorder.events,
        )
    }

    @Test
    fun writesWithoutResponseFollowAndroidFlowControl() {
        val central = manager()
        val (peripheral, service) = connectAndDiscover(central)
        val point = service.characteristic(controlPoint)
        assertTrue(peripheral.canSendWriteWithoutResponse)
        for (packet in 1..3) {
            peripheral.writeValue(byteArrayOf(packet.toByte()), `for` = point, type = CBCharacteristicWriteType.withoutResponse)
        }
        assertEquals(listOf("writeCharacteristic 2A39 01 1"), recording().calls)
        assertFalse(peripheral.canSendWriteWithoutResponse)
        callback().onCharacteristicWrite(gatt(), gattCharacteristic(controlPoint), BluetoothGatt.GATT_SUCCESS)
        assertEquals("writeCharacteristic 2A39 02 1", recording().calls.last())
        callback().onCharacteristicWrite(gatt(), gattCharacteristic(controlPoint), BluetoothGatt.GATT_SUCCESS)
        assertEquals("writeCharacteristic 2A39 03 1", recording().calls.last())
        runMain()
        assertEquals(emptyList(), recorder.events)
        callback().onCharacteristicWrite(gatt(), gattCharacteristic(controlPoint), BluetoothGatt.GATT_SUCCESS)
        runMain()
        assertEquals(listOf("isReadyToSendWriteWithoutResponse"), recorder.events)
        assertTrue(peripheral.canSendWriteWithoutResponse)
    }

    @Test
    fun aLostCallbackTimesOutAndTheQueueMovesOn() {
        val central = manager()
        val (peripheral, service) = connectAndDiscover(central)
        val location = service.characteristic(bodySensorLocation)
        val point = service.characteristic(controlPoint)
        peripheral.writeValue(byteArrayOf(1), `for` = point, type = CBCharacteristicWriteType.withoutResponse)
        peripheral.writeValue(byteArrayOf(2), `for` = location, type = CBCharacteristicWriteType.withResponse)
        peripheral.readValue(`for` = location)
        advance(4)
        assertEquals(listOf("writeCharacteristic 2A39 01 1"), recording().calls)
        advance(1)
        assertEquals(listOf("writeCharacteristic 2A39 01 1", "writeCharacteristic 2A38 02 2"), recording().calls)
        advance(29)
        assertEquals(2, recording().calls.size)
        advance(1)
        assertEquals("readCharacteristic 2A38", recording().calls.last())
        assertEquals(listOf("didWriteValueFor 2A38 CBError.connectionTimeout"), recorder.events)
        callback().onCharacteristicWrite(gatt(), gattCharacteristic(bodySensorLocation), BluetoothGatt.GATT_SUCCESS)
        callback().onCharacteristicRead(gatt(), gattCharacteristic(bodySensorLocation), byteArrayOf(3), 0)
        runMain()
        assertEquals(
            listOf("didWriteValueFor 2A38 CBError.connectionTimeout", "didUpdateValueFor 2A38 03 nil"),
            recorder.events,
        )
    }

    @Test
    fun aDisconnectMidQueueDropsPendingOperationsAndClosesTheGatt() {
        val central = manager()
        val (peripheral, service) = connectAndDiscover(central)
        val measurement = service.characteristic(heartRateMeasurement)
        val location = service.characteristic(bodySensorLocation)
        peripheral.setNotifyValue(true, `for` = measurement)
        peripheral.writeValue(byteArrayOf(1), `for` = location, type = CBCharacteristicWriteType.withResponse)
        peripheral.readValue(`for` = location)
        callback().onDescriptorWrite(gatt(), gattCharacteristic(heartRateMeasurement).getDescriptor(clientConfiguration), 0)
        runMain()
        assertTrue(measurement.isNotifying)
        val connection = gatt()
        callback(connection).onConnectionStateChange(connection, 0x13, BluetoothProfile.STATE_DISCONNECTED)
        assertTrue(recording(connection).isClosed)
        assertEquals(CBPeripheralState.disconnected, peripheral.state)
        callback(connection).onCharacteristicWrite(connection, gattCharacteristic(bodySensorLocation), 0)
        callback(connection).onCharacteristicChanged(connection, gattCharacteristic(heartRateMeasurement), byteArrayOf(1))
        peripheral.readValue(`for` = location)
        runMain()
        assertEquals(
            listOf(
                "didUpdateNotificationStateFor 2A37 true nil",
                "didDisconnectPeripheral CBError.peripheralDisconnected",
            ),
            recorder.events,
        )
        assertEquals(
            listOf("setCharacteristicNotification 2A37 true", "writeDescriptor 2A37 0100", "writeCharacteristic 2A38 01 2"),
            recording(connection).calls,
        )
        assertNull(peripheral.services)
        assertFalse(measurement.isNotifying)
        assertEquals(1, gatts().size)
    }

    @Test
    fun cancelPeripheralConnectionClosesTheGattAndReportsNoError() {
        val central = manager()
        val (peripheral, _) = connectAndDiscover(central)
        central.cancelPeripheralConnection(peripheral)
        assertTrue(recording().isClosed)
        assertEquals(CBPeripheralState.disconnected, peripheral.state)
        runMain()
        assertEquals(listOf("didDisconnectPeripheral nil"), recorder.events)
        central.cancelPeripheralConnection(peripheral)
        runMain()
        assertEquals(listOf("didDisconnectPeripheral nil"), recorder.events)
    }

    @Test
    fun aConnectAttemptThatFailsIsRetriedLikeAConnectThatNeverTimesOut() {
        val central = manager()
        val peripheral = central.retrievePeripherals(withIdentifiers = listOf(bluetoothIdentifier(peripheralAddress))).single()
        central.connect(peripheral)
        val first = gatt()
        callback(first).onConnectionStateChange(first, 133, BluetoothProfile.STATE_DISCONNECTED)
        assertTrue(recording(first).isClosed)
        assertEquals(CBPeripheralState.connecting, peripheral.state)
        advance(1)
        assertEquals(2, gatts().size)
        val second = gatt()
        assertNotSame(first, second)
        callback(second).onConnectionStateChange(second, 133, BluetoothProfile.STATE_DISCONNECTED)
        advance(1)
        assertEquals(2, gatts().size)
        advance(1)
        assertEquals(3, gatts().size)
        callback().onConnectionStateChange(gatt(), 0, BluetoothProfile.STATE_CONNECTED)
        runMain()
        assertEquals(listOf("didUpdateState poweredOn", "didConnect"), recorder.events)
        assertTrue(gatts().dropLast(1).all { recording(it).isClosed })
        central.cancelPeripheralConnection(peripheral)
        advance(60)
        assertEquals(3, gatts().size)
    }

    @Test
    fun autoReconnectReportsIsReconnectingAndConnectsAgain() {
        val central = manager(delegate = ReconnectingRecorder(recorder))
        val peripheral = connect(central, options = mapOf(CBConnectPeripheralOptionEnableAutoReconnect to true))
        val first = gatt()
        callback(first).onConnectionStateChange(first, 0x08, BluetoothProfile.STATE_DISCONNECTED)
        assertTrue(recording(first).isClosed)
        assertEquals(2, gatts().size)
        assertEquals(CBPeripheralState.connecting, peripheral.state)
        callback().onConnectionStateChange(gatt(), 0, BluetoothProfile.STATE_CONNECTED)
        runMain()
        assertEquals(
            listOf("didUpdateState poweredOn", "didConnect", "didDisconnectPeripheral isReconnecting true CBError", "didConnect"),
            recorder.events,
        )
    }

    @Test
    fun theDefaultDisconnectCallbackForwardsToTheShortOne() {
        val central = manager()
        connect(central)
        callback().onConnectionStateChange(gatt(), 0x08, BluetoothProfile.STATE_DISCONNECTED)
        runMain()
        assertEquals("didDisconnectPeripheral CBError.connectionTimeout", recorder.events.last())
    }

    @Test
    fun droppingTheManagerDisconnectsAndSilencesItLikeReleasingItOnIos() {
        var central: CBCentralManager? by CBCentralManager.holder()
        central = manager()
        val (peripheral, _) = connectAndDiscover(central!!)
        val connection = gatt()
        central = null
        assertTrue(recording(connection).isClosed)
        assertEquals(CBPeripheralState.disconnected, peripheral.state)
        assertNull(peripheral.services)
        callback(connection).onCharacteristicChanged(connection, gattCharacteristic(heartRateMeasurement), byteArrayOf(1))
        setAdapterState(BluetoothAdapter.STATE_OFF)
        assertEquals(emptyList(), recorder.events)
        central = manager()
        val replaced = central
        central = manager()
        assertTrue(replaced!!.released)
        assertFalse(central!!.released)
    }

    @Test
    fun turningBluetoothOffInvalidatesConnectedPeripherals() {
        val central = manager()
        val (peripheral, _) = connectAndDiscover(central)
        setAdapterState(BluetoothAdapter.STATE_OFF)
        assertTrue(recording().isClosed)
        assertEquals(CBPeripheralState.disconnected, peripheral.state)
        assertEquals(listOf("didUpdateState poweredOff"), recorder.events)
        assertNull(peripheral.services)
    }

    @Test
    fun delegateCallbacksRunOnTheQueueOfTheManagerInOrder() {
        val work = mutableListOf<Runnable>()
        val dispatcher = object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                work.add(block)
            }
        }
        val queue = CoroutineScope(dispatcher)
        val central = CBCentralManager(delegate = recorder, queue = queue)
        runMain()
        work.removeFirst().run()
        val peripheral = central.retrievePeripherals(withIdentifiers = listOf(bluetoothIdentifier(peripheralAddress))).single()
        peripheral.delegate = recorder
        central.connect(peripheral)
        callback().onConnectionStateChange(gatt(), 0, BluetoothProfile.STATE_CONNECTED)
        queue.launch { recorder.events.add("work queued after didConnect") }
        callback().onMtuChanged(gatt(), 247, 0)
        peripheral.discoverServices(null)
        recording().discovered = services()
        callback().onServicesDiscovered(gatt(), 0)
        assertEquals(listOf("didUpdateState poweredOn"), recorder.events)
        assertEquals(3, work.size)
        while (work.isNotEmpty()) {
            work.removeFirst().run()
        }
        assertEquals(
            listOf(
                "didUpdateState poweredOn",
                "didConnect",
                "work queued after didConnect",
                "didDiscoverServices [180D, 180F] nil",
            ),
            recorder.events,
        )
    }

    @Test
    fun securityExceptionsFromAndroidBecomeErrors() {
        val central = manager()
        val (peripheral, service) = connectAndDiscover(central)
        recording().refuse = true
        peripheral.writeValue(byteArrayOf(1), `for` = service.characteristic(bodySensorLocation), type = CBCharacteristicWriteType.withResponse)
        peripheral.setNotifyValue(true, `for` = service.characteristic(heartRateMeasurement))
        runMain()
        assertEquals(
            listOf("didWriteValueFor 2A38 CBError.unknown", "didUpdateNotificationStateFor 2A37 false CBError.unknown"),
            recorder.events,
        )
    }

    @Test
    @Config(shadows = [RefusingBluetoothDevice::class, RecordingBluetoothGatt::class])
    fun aConnectionRefusedByTheSystemFailsToConnect() {
        val central = manager()
        val peripheral = central.retrievePeripherals(withIdentifiers = listOf(bluetoothIdentifier(peripheralAddress))).single()
        central.connect(peripheral)
        runMain()
        assertEquals(CBPeripheralState.disconnected, peripheral.state)
        assertEquals(listOf("didUpdateState poweredOn", "didFailToConnect CBError.connectionFailed"), recorder.events)
    }

    @Test
    fun operationsWhileNotConnectedAreIgnoredLikeCoreBluetooth() {
        val central = manager()
        val peripheral = central.retrievePeripherals(withIdentifiers = listOf(bluetoothIdentifier(peripheralAddress))).single()
        peripheral.delegate = recorder
        peripheral.discoverServices(null)
        central.connect(peripheral)
        peripheral.readRSSI()
        runMain()
        assertEquals(emptyList(), recording().calls)
        assertEquals(listOf("didUpdateState poweredOn"), recorder.events)
    }

    @Test
    fun attributesFromAnOlderConnectionAreRejected() {
        val central = manager()
        val (peripheral, service) = connectAndDiscover(central)
        val stale = service.characteristic(bodySensorLocation)
        callback().onConnectionStateChange(gatt(), 0x13, BluetoothProfile.STATE_DISCONNECTED)
        connect(central)
        callback().onMtuChanged(gatt(), 247, 0)
        recorder.events.clear()
        peripheral.readValue(`for` = stale)
        runMain()
        assertEquals(listOf("requestMtu 517"), recording().calls)
        val error = recorder.events.single()
        assertEquals("didUpdateValueFor 2A38 nil CBError.invalidHandle", error)
    }

    @Test
    fun readingADescriptorReportsItsValue() {
        val central = manager()
        val (peripheral, service) = connectAndDiscover(central)
        val measurement = service.characteristic(heartRateMeasurement)
        peripheral.discoverDescriptors(`for` = measurement)
        runMain()
        val descriptor = measurement.descriptors!!.single()
        assertSame(measurement, descriptor.characteristic)
        peripheral.readValue(`for` = descriptor)
        assertEquals("readDescriptor 2902", recording().calls.last())
        callback().onDescriptorRead(
            gatt(),
            gattCharacteristic(heartRateMeasurement).getDescriptor(clientConfiguration),
            0,
            byteArrayOf(1, 0),
        )
        runMain()
        assertEquals("didUpdateValueFor descriptor 2902 1 nil", recorder.events.last())
        assertIs<Int>(descriptor.value)
    }

    private fun forgottenDevice(): BluetoothDevice {
        val device = adapter().getRemoteDevice(forgottenAddress)
        Shadow.extract<ShadowBluetoothDevice>(device).setType(BluetoothDevice.DEVICE_TYPE_UNKNOWN)
        Shadow.extract<ShadowBluetoothDevice>(device).setName(null)
        return device
    }

    private fun forgottenGatts(): List<BluetoothGatt> {
        return Shadow.extract<ShadowBluetoothDevice>(adapter().getRemoteDevice(forgottenAddress)).bluetoothGatts
    }

    private fun scans(): List<ShadowBluetoothLeScanner.ScanParams> {
        val scanner = adapter().bluetoothLeScanner ?: return emptyList()
        return Shadow.extract<ShadowBluetoothLeScanner>(scanner).activeScans
    }

    private fun advertisement(name: String): ScanRecord {
        val structure = byteArrayOf(0x09) + name.toByteArray()
        val parse = ScanRecord::class.java.getDeclaredMethod("parseFromBytes", ByteArray::class.java)
        return parse.invoke(null, byteArrayOf(structure.size.toByte()) + structure) as ScanRecord
    }

    @Test
    fun theGenericAccessAndGenericAttributeServicesAreHiddenLikeOnIos() {
        val central = manager()
        val peripheral = connect(central)
        callback().onMtuChanged(gatt(), 247, BluetoothGatt.GATT_SUCCESS)
        peripheral.discoverServices(null)
        val genericAccess = BluetoothGattService(uuid16("1800"), BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val genericAttribute = BluetoothGattService(uuid16("1801"), BluetoothGattService.SERVICE_TYPE_PRIMARY)
        recording().discovered = listOf(genericAccess, genericAttribute) + services()
        callback().onServicesDiscovered(gatt(), BluetoothGatt.GATT_SUCCESS)
        runMain()
        assertEquals("didDiscoverServices [180D, 180F] nil", recorder.events.last())
        assertEquals(CBUUID(string = "180D"), peripheral.services!!.first().uuid)
        peripheral.discoverServices(listOf(CBUUID(string = "1800"), CBUUID(string = "1801")))
        runMain()
        assertEquals("didDiscoverServices [180D, 180F] nil", recorder.events.last())
    }

    @Test
    fun aDeviceAndroidHasForgottenIsFoundByScanningBeforeConnecting() {
        val device = forgottenDevice()
        val central = manager()
        val peripheral = central.retrievePeripherals(withIdentifiers = listOf(bluetoothIdentifier(forgottenAddress))).single()
        peripheral.delegate = recorder
        assertNull(peripheral.name)
        central.connect(peripheral)
        assertEquals(CBPeripheralState.connecting, peripheral.state)
        assertEquals(0, forgottenGatts().size)
        val search = scans().single()
        assertEquals(listOf(forgottenAddress), search.scanFilters().map { it.deviceAddress })
        search.scanCallback()!!.onScanResult(
            ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
            ScanResult(device, advertisement("TICKR 1A2B"), -60, 0),
        )
        assertEquals(0, forgottenGatts().size)
        runMain()
        assertEquals(emptyList(), scans())
        assertEquals(1, forgottenGatts().size)
        assertEquals("TICKR 1A2B", peripheral.name)
        val first = forgottenGatts().single()
        callback(first).onConnectionStateChange(first, 147, BluetoothProfile.STATE_DISCONNECTED)
        advance(1)
        assertEquals(emptyList(), scans())
        assertEquals(2, forgottenGatts().size)
        val second = forgottenGatts().last()
        callback(second).onConnectionStateChange(second, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        runMain()
        assertEquals(listOf("didUpdateState poweredOn", "didConnect"), recorder.events)
        assertEquals(listOf("requestMtu 517"), recording(second).calls)
    }

    @Test
    fun aSearchThatFindsNothingConnectsByAddressAndSearchesAgainAfterThatFails() {
        val device = forgottenDevice()
        val central = manager()
        val peripheral = central.retrievePeripherals(withIdentifiers = listOf(bluetoothIdentifier(forgottenAddress))).single()
        central.connect(peripheral)
        scans().single().scanCallback()!!.onScanResult(
            ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
            ScanResult(adapter().getRemoteDevice(peripheralAddress), advertisement("Other"), -60, 0),
        )
        advance(9)
        assertEquals(0, forgottenGatts().size)
        assertEquals(1, scans().size)
        advance(1)
        assertEquals(emptyList(), scans())
        assertEquals(1, forgottenGatts().size)
        assertEquals(CBPeripheralState.connecting, peripheral.state)
        val first = forgottenGatts().single()
        advance(30)
        callback(first).onConnectionStateChange(first, 147, BluetoothProfile.STATE_DISCONNECTED)
        assertTrue(recording(first).isClosed)
        advance(1)
        assertEquals(1, scans().size)
        assertEquals(1, forgottenGatts().size)
        scans().single().scanCallback()!!.onScanFailed(ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED)
        runMain()
        assertEquals(emptyList(), scans())
        assertEquals(2, forgottenGatts().size)
        val second = forgottenGatts().last()
        callback(second).onConnectionStateChange(second, 133, BluetoothProfile.STATE_DISCONNECTED)
        advance(1)
        assertEquals(emptyList(), scans())
        advance(1)
        assertEquals(1, scans().size)
        central.cancelPeripheralConnection(peripheral)
        assertEquals(emptyList(), scans())
        assertEquals(CBPeripheralState.disconnected, peripheral.state)
        advance(60)
        assertEquals(emptyList(), scans())
        assertEquals(2, forgottenGatts().size)
        assertNull(peripheral.name)
        assertEquals(BluetoothDevice.DEVICE_TYPE_UNKNOWN, device.type)
    }

    @Test
    fun releasingTheManagerOrTurningBluetoothOffWhileSearchingStopsTheSearch() {
        forgottenDevice()
        var central: CBCentralManager? by CBCentralManager.holder()
        central = manager()
        val peripheral = central!!.retrievePeripherals(withIdentifiers = listOf(bluetoothIdentifier(forgottenAddress))).single()
        central!!.connect(peripheral)
        val search = scans().single().scanCallback()!!
        central = null
        assertEquals(emptyList(), scans())
        search.onScanResult(
            ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
            ScanResult(adapter().getRemoteDevice(forgottenAddress), advertisement("TICKR"), -60, 0),
        )
        advance(20)
        assertEquals(0, forgottenGatts().size)
        central = manager()
        val again = central!!.retrievePeripherals(withIdentifiers = listOf(bluetoothIdentifier(forgottenAddress))).single()
        central!!.connect(again)
        assertEquals(1, scans().size)
        setAdapterState(BluetoothAdapter.STATE_OFF)
        advance(20)
        assertEquals(0, forgottenGatts().size)
        assertEquals(CBPeripheralState.disconnected, again.state)
    }
}
