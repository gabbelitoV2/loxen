package com.moblin.android.integrations.workoutdevice

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
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.platform.corebluetooth.BluetoothAuthorization
import com.moblin.android.platform.corebluetooth.CBUUID
import com.moblin.android.platform.corebluetooth.bluetoothIdentifier
import com.moblin.android.platform.corebluetooth.gattSdkInt
import java.util.Collections
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
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

private const val strapAddress = "C0:FF:EE:00:00:42"
private const val notify = BluetoothGattCharacteristic.PROPERTY_NOTIFY
private const val read = BluetoothGattCharacteristic.PROPERTY_READ

private fun uuid16(value: String): UUID = UUID.fromString("0000$value-0000-1000-8000-00805F9B34FB")

private fun hex(bytes: ByteArray?): String = bytes?.joinToString("") { "%02x".format(it) } ?: "nil"

private fun short(uuid: UUID): String = CBUUID(uuid).uuidString

@Implements(BluetoothGatt::class)
class WorkoutDeviceBluetoothGatt : ShadowBluetoothGatt() {
    private val recorded = Collections.synchronizedList(mutableListOf<String>())

    @Volatile
    var discovered: List<BluetoothGattService> = emptyList()

    val calls: List<String>
        get() = synchronized(recorded) { recorded.toList() }

    private fun record(call: String): Boolean {
        recorded.add(call)
        return true
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
        record("writeDescriptor ${short(descriptor.characteristic.uuid)} ${hex(value)}")
        return 0
    }
}

private class WorkoutDeviceEvents : WorkoutDeviceDelegate {
    val events = LinkedBlockingQueue<String>()

    override fun workoutDeviceState(device: WorkoutDevice, state: WorkoutDeviceState) {
        events.add("state $state")
    }

    override fun workoutDeviceHeartRate(device: WorkoutDevice, heartRate: Int) {
        events.add("heartRate $heartRate")
    }

    override fun workoutDeviceCyclingPower(device: WorkoutDevice, power: Int, cadence: Int?) {
        events.add("cyclingPower $power $cadence")
    }

    override fun workoutDeviceCyclingSpeedCadence(device: WorkoutDevice, speed: Double?, cadence: Int?) {
        events.add("cyclingSpeedCadence $speed $cadence")
    }

    override fun workoutDeviceRunningMetrics(device: WorkoutDevice, metrics: WorkoutDeviceRunningMetrics) {
        events.add("running ${metrics.speed} ${metrics.cadence} ${metrics.distance}")
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [WorkoutDeviceBluetoothGatt::class])
class WorkoutDeviceConnectSuite {
    private lateinit var application: Application
    private val delegate = WorkoutDeviceEvents()
    private var device: WorkoutDevice? = null

    @Before
    fun setUp() {
        BluetoothAuthorization.reset()
        gattSdkInt = Build.VERSION.SDK_INT
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        shadowOf(application.packageManager).setSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE, true)
        shadowOf(adapter()).setState(BluetoothAdapter.STATE_ON)
        Shadow.extract<ShadowBluetoothDevice>(strap()).setType(BluetoothDevice.DEVICE_TYPE_LE)
    }

    @After
    fun tearDown() {
        device?.let { device ->
            device.stop()
            waitUntil("the device to stop") { device.getState() == WorkoutDeviceState.disconnected }
        }
        runMain()
        gattSdkInt = Build.VERSION.SDK_INT
        BluetoothAuthorization.reset()
    }

    private fun adapter(): BluetoothAdapter = application.getSystemService(BluetoothManager::class.java).adapter

    private fun strap(): BluetoothDevice = adapter().getRemoteDevice(strapAddress)

    private fun gatts(): List<BluetoothGatt> = Shadow.extract<ShadowBluetoothDevice>(strap()).bluetoothGatts.toList()

    private fun gatt(): BluetoothGatt = gatts().last()

    private fun shadow(gatt: BluetoothGatt = gatt()): WorkoutDeviceBluetoothGatt = Shadow.extract(gatt)

    private fun callback(gatt: BluetoothGatt = gatt()): BluetoothGattCallback = shadow(gatt).gattCallback

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun waitUntil(what: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!condition()) {
            if (System.nanoTime() > deadline) {
                fail("Timed out waiting for $what")
            }
            runMain()
            Thread.sleep(2)
        }
    }

    private fun pumpFor(milliseconds: Long) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(milliseconds)
        while (System.nanoTime() < deadline) {
            runMain()
            Thread.sleep(2)
        }
    }

    private fun nextEvent(): String {
        var event: String? = null
        waitUntil("a delegate call") {
            event = delegate.events.poll()
            event != null
        }
        return event!!
    }

    private fun expectEvents(vararg expected: String) {
        assertEquals(expected.toList(), expected.map { nextEvent() })
    }

    private fun expectNoEvent() {
        pumpFor(50)
        assertNull(delegate.events.poll())
    }

    private fun expectCalls(expected: List<String>, gatt: BluetoothGatt = gatt()) {
        waitUntil(expected.last()) { shadow(gatt).calls.size >= expected.size }
        assertEquals(expected, shadow(gatt).calls)
    }

    private fun service(uuid: String, vararg characteristics: Pair<String, Int>): BluetoothGattService {
        val service = BluetoothGattService(uuid16(uuid), BluetoothGattService.SERVICE_TYPE_PRIMARY)
        for ((characteristicUuid, properties) in characteristics) {
            val characteristic = BluetoothGattCharacteristic(
                uuid16(characteristicUuid),
                properties,
                BluetoothGattCharacteristic.PERMISSION_READ,
            )
            if (properties and notify != 0) {
                characteristic.addDescriptor(
                    BluetoothGattDescriptor(
                        uuid16("2902"),
                        BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
                    ),
                )
            }
            service.addCharacteristic(characteristic)
        }
        return service
    }

    private fun characteristic(uuid: String, gatt: BluetoothGatt = gatt()): BluetoothGattCharacteristic {
        return shadow(gatt).discovered.firstNotNullOf { it.getCharacteristic(uuid16(uuid)) }
    }

    private fun acknowledgeNotifications(uuid: String, gatt: BluetoothGatt = gatt()) {
        val descriptor = characteristic(uuid, gatt).getDescriptor(uuid16("2902"))
        callback(gatt).onDescriptorWrite(gatt, descriptor, BluetoothGatt.GATT_SUCCESS)
    }

    private fun notifyValue(uuid: String, value: ByteArray, gatt: BluetoothGatt = gatt()) {
        callback(gatt).onCharacteristicChanged(gatt, characteristic(uuid, gatt), value)
    }

    private fun heartRateService(): BluetoothGattService = service("180D", "2A37" to notify, "2A38" to read)

    private fun startDevice(): WorkoutDevice {
        val device = WorkoutDevice(wheelCircumference = 2105)
        this.device = device
        device.delegate = delegate
        device.start(deviceId = bluetoothIdentifier(strapAddress))
        expectEvents("state discovering", "state connecting")
        waitUntil("connectGatt") { gatts().size == 1 }
        return device
    }

    private fun connectAndDiscover(vararg services: BluetoothGattService, gatt: BluetoothGatt = gatt()) {
        callback(gatt).onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        expectCalls(listOf("requestMtu 517"), gatt)
        callback(gatt).onMtuChanged(gatt, 247, BluetoothGatt.GATT_SUCCESS)
        expectCalls(listOf("requestMtu 517", "discoverServices"), gatt)
        shadow(gatt).discovered = services.toList()
        callback(gatt).onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS)
    }

    private val discovery = listOf("requestMtu 517", "discoverServices")

    private fun subscription(uuid: String): List<String> {
        return listOf("setCharacteristicNotification $uuid true", "writeDescriptor $uuid 0100")
    }

    @Test
    fun serviceAndCharacteristicIdsAreTheBluetoothSigOnes() {
        assertEquals("180D", workoutDeviceHeartRateServiceId.uuidString)
        assertEquals("2A37", workoutDeviceHeartRateMeasurementCharacteristicId.uuidString)
        assertEquals("1818", workoutDeviceCyclingPowerServiceId.uuidString)
        assertEquals("2A63", workoutDeviceCyclingPowerMeasurementCharacteristicId.uuidString)
        assertEquals("2A64", workoutDeviceCyclingPowerVectorCharacteristicId.uuidString)
        assertEquals("1816", workoutDeviceCyclingSpeedCadenceServiceId.uuidString)
        assertEquals("2A5B", workoutDeviceCyclingSpeedCadenceMeasurementCharacteristicId.uuidString)
        assertEquals("1814", workoutDeviceRunningServiceId.uuidString)
        assertEquals("2A53", workoutDeviceRunningMeasurementCharacteristicId.uuidString)
    }

    @Test
    fun aHeartRateStrapIsSubscribedThroughTheShimAndReportsBothValueFormats() {
        startDevice()
        connectAndDiscover(heartRateService(), service("180F", "2A19" to (read or notify)))
        expectEvents("state connected")
        expectCalls(discovery + subscription("2A37"))
        acknowledgeNotifications("2A37")
        notifyValue("2A37", byteArrayOf(0x00, 72))
        notifyValue("2A37", byteArrayOf(0x01, 0x2C, 0x01))
        expectEvents("heartRate 72", "heartRate 300")
        assertEquals(WorkoutDeviceState.connected, device!!.getState())
        assertEquals(discovery + subscription("2A37"), shadow().calls)
    }

    @Test
    fun everyMeasurementIsSubscribedOneDescriptorWriteAtATime() {
        startDevice()
        connectAndDiscover(
            heartRateService(),
            service("1818", "2A63" to notify, "2A64" to notify, "2A65" to read),
            service("1816", "2A5B" to notify, "2A5C" to read),
            service("1814", "2A53" to notify, "2A54" to read),
        )
        expectEvents("state connected")
        var expected = discovery
        for (uuid in listOf("2A37", "2A63", "2A5B", "2A53")) {
            expected = expected + subscription(uuid)
            expectCalls(expected)
            acknowledgeNotifications(uuid)
        }
        notifyValue("2A63", byteArrayOf(0x00, 0x00, 0x96.toByte(), 0x00))
        notifyValue("2A64", byteArrayOf(0x00, 0x10, 0x00))
        notifyValue("2A5B", byteArrayOf(0x02, 10, 0x00, 0x00, 0x04))
        notifyValue("2A53", byteArrayOf(0x02, 0x00, 0x03, 0xAA.toByte(), 0x39, 0x30, 0x00, 0x00))
        notifyValue("2A37", byteArrayOf(0x00, 90))
        expectEvents(
            "cyclingPower 50 null",
            "cyclingSpeedCadence null null",
            "running 3.0 170 1234.5",
            "heartRate 90",
        )
        expectNoEvent()
        assertEquals(expected, shadow().calls)
    }

    @Test
    fun theWheelCircumferenceIsUsedForSpeed() {
        val device = startDevice()
        connectAndDiscover(service("1816", "2A5B" to notify))
        expectEvents("state connected")
        expectCalls(discovery + subscription("2A5B"))
        acknowledgeNotifications("2A5B")
        device.setWheelCircumference(millimeters = 2000)
        notifyValue("2A5B", byteArrayOf(0x01, 100, 0x00, 0x00, 0x00, 0x00, 0x04))
        notifyValue("2A5B", byteArrayOf(0x01, 105, 0x00, 0x00, 0x00, 0x00, 0x08))
        expectEvents("cyclingSpeedCadence 0.0 null", "cyclingSpeedCadence 10.0 null")
    }

    @Test
    fun aTruncatedMeasurementIsDroppedAndTheNextOneIsReported() {
        startDevice()
        connectAndDiscover(heartRateService())
        expectEvents("state connected")
        expectCalls(discovery + subscription("2A37"))
        acknowledgeNotifications("2A37")
        notifyValue("2A37", byteArrayOf(0x01, 0x2C))
        notifyValue("2A37", byteArrayOf())
        notifyValue("2A37", byteArrayOf(0x00, 61))
        expectEvents("heartRate 61")
        expectNoEvent()
    }

    @Test
    fun aDroppedConnectionIsReconnectedWithANewManager() {
        startDevice()
        connectAndDiscover(heartRateService())
        expectEvents("state connected")
        expectCalls(discovery + subscription("2A37"))
        acknowledgeNotifications("2A37")
        val first = gatt()
        callback(first).onConnectionStateChange(first, 0x08, BluetoothProfile.STATE_DISCONNECTED)
        expectEvents("state discovering", "state connecting")
        waitUntil("a second connectGatt") { gatts().size == 2 }
        assertTrue(shadow(first).isClosed)
        notifyValue("2A37", byteArrayOf(0x00, 50), first)
        expectNoEvent()
        val second = gatt()
        connectAndDiscover(heartRateService(), gatt = second)
        expectEvents("state connected")
        expectCalls(discovery + subscription("2A37"), second)
        acknowledgeNotifications("2A37", second)
        notifyValue("2A37", byteArrayOf(0x00, 80), second)
        expectEvents("heartRate 80")
        assertFalse(shadow(second).isClosed)
    }

    @Test
    fun stopClosesTheConnectionAndSilencesTheDevice() {
        val device = startDevice()
        connectAndDiscover(heartRateService())
        expectEvents("state connected")
        expectCalls(discovery + subscription("2A37"))
        acknowledgeNotifications("2A37")
        device.stop()
        expectEvents("state disconnected")
        assertTrue(shadow().isClosed)
        notifyValue("2A37", byteArrayOf(0x00, 70))
        expectNoEvent()
        assertEquals(WorkoutDeviceState.disconnected, device.getState())
        assertEquals(1, gatts().size)
    }

    @Test
    fun bluetoothTurnedOffAndOnAgainConnectsAgainLikeIos() {
        startDevice()
        connectAndDiscover(heartRateService())
        expectEvents("state connected")
        expectCalls(discovery + subscription("2A37"))
        acknowledgeNotifications("2A37")
        val first = gatt()
        setAdapterState(BluetoothAdapter.STATE_OFF)
        assertTrue(shadow(first).isClosed)
        expectNoEvent()
        setAdapterState(BluetoothAdapter.STATE_ON)
        expectEvents("state connecting")
        waitUntil("a second connectGatt") { gatts().size == 2 }
        connectAndDiscover(heartRateService(), gatt = gatt())
        expectEvents("state connected")
        expectCalls(discovery + subscription("2A37"))
    }

    @Test
    fun aDeviceWithoutAnIdentifierIsNeverConnected() {
        val device = WorkoutDevice(wheelCircumference = 2105)
        this.device = device
        device.delegate = delegate
        device.start(deviceId = null)
        expectEvents("state discovering")
        pumpFor(200)
        device.stop()
        expectEvents("state disconnected")
        assertEquals(0, gatts().size)
    }

    private fun setAdapterState(state: Int) {
        shadowOf(adapter()).setState(state)
        application.sendBroadcast(
            Intent(BluetoothAdapter.ACTION_STATE_CHANGED).putExtra(BluetoothAdapter.EXTRA_STATE, state),
        )
        runMain()
    }
}
