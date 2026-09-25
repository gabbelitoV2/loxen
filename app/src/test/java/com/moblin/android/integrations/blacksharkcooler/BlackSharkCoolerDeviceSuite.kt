package com.moblin.android.integrations.blacksharkcooler

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
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Looper
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.common.various.RgbColor
import com.moblin.android.platform.blacksharklib.BlackSharkLib
import com.moblin.android.platform.core.ProcessInfo
import com.moblin.android.platform.corebluetooth.BluetoothAuthorization
import com.moblin.android.platform.corebluetooth.RecordingBluetoothGatt
import com.moblin.android.platform.corebluetooth.bluetoothIdentifier
import java.time.Duration
import java.util.Collections
import java.util.UUID
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothDevice
import org.robolectric.shadows.ShadowBluetoothLeScanner

private const val coolerAddress = "C0:FF:EE:00:00:0B"

private fun uuid16(value: String): UUID = UUID.fromString("0000$value-0000-1000-8000-00805F9B34FB")

private fun hex(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

private fun zeros(count: Int): String = "00".repeat(count)

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [RecordingBluetoothGatt::class])
class BlackSharkCoolerDeviceSuite : BlackSharkCoolerDeviceDelegate {
    private lateinit var application: Application
    private val states = Collections.synchronizedList(mutableListOf<BlackSharkCoolerDeviceState>())
    private val statuses = Collections.synchronizedList(mutableListOf<String>())
    private val coolerService = UUID.fromString("0000A0A0-3C17-D293-8E48-14FE2E4DA212")
    private val readId = uuid16("A002")
    private val writeId = uuid16("A001")
    private val clientConfiguration = uuid16("2902")
    private var answeredCalls = 0

    override fun blackSharkCoolerDeviceState(device: BlackSharkCoolerDevice, state: BlackSharkCoolerDeviceState) {
        states.add(state)
    }

    override fun blackSharkCoolerDeviceStatus(device: BlackSharkCoolerDevice, status: BlackSharkLib.CoolingState) {
        statuses.add("${status.model} ${status.phoneTemperature} ${status.heatsinkTemperature} ${status.fanRPM} ${status.powerLevel}")
    }

    @Before
    fun setUp() {
        BluetoothAuthorization.reset()
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        shadowOf(application.packageManager).setSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE, true)
        shadowOf(adapter()).setState(BluetoothAdapter.STATE_ON)
        setThermalStatus(PowerManager.THERMAL_STATUS_NONE)
        Shadow.extract<ShadowBluetoothDevice>(coolerDevice()).setType(BluetoothDevice.DEVICE_TYPE_LE)
    }

    @After
    fun tearDown() {
        BluetoothAuthorization.reset()
    }

    private fun setThermalStatus(status: Int) {
        shadowOf(application.getSystemService(PowerManager::class.java)).setCurrentThermalStatus(status)
    }

    private fun adapter(): BluetoothAdapter = application.getSystemService(BluetoothManager::class.java).adapter

    private fun coolerDevice(): BluetoothDevice = adapter().getRemoteDevice(coolerAddress)

    private fun gatts(): List<BluetoothGatt> = Shadow.extract<ShadowBluetoothDevice>(coolerDevice()).bluetoothGatts

    private fun gatt(): BluetoothGatt = gatts().last()

    private fun recording(): RecordingBluetoothGatt = Shadow.extract(gatt())

    private fun callback(): BluetoothGattCallback = recording().gattCallback

    private fun queue(): CoroutineDispatcher {
        val field = Class.forName("com.moblin.android.integrations.blacksharkcooler.BlackSharkCoolerDeviceKt")
            .getDeclaredField("blackSharkCoolerDeviceDispatchQueue")
        field.isAccessible = true
        return when (val value = field.get(null)) {
            is CoroutineDispatcher -> value
            is CoroutineScope -> value.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
            else -> error("Unexpected Black Shark cooler queue $value")
        }
    }

    private fun <T> onQueue(block: () -> T): T = runBlocking(queue()) { block() }

    private fun settle() {
        repeat(5) {
            shadowOf(Looper.getMainLooper()).idle()
            onQueue {}
        }
    }

    private fun calls(): List<String> = onQueue { recording().calls.toList() }

    private fun services(): List<BluetoothGattService> {
        val service = BluetoothGattService(coolerService, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val permissions = BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        val read = BluetoothGattCharacteristic(
            readId,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ,
        )
        read.addDescriptor(BluetoothGattDescriptor(clientConfiguration, permissions))
        service.addCharacteristic(read)
        service.addCharacteristic(
            BluetoothGattCharacteristic(
                writeId,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE,
            ),
        )
        val battery = BluetoothGattService(uuid16("180F"), BluetoothGattService.SERVICE_TYPE_PRIMARY)
        return listOf(battery, service)
    }

    private fun gattCharacteristic(uuid: UUID): BluetoothGattCharacteristic {
        return recording().discovered.firstNotNullOf { it.getCharacteristic(uuid) }
    }

    private fun write(value: String): String = "writeCharacteristic A001 $value ${BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE}"

    private fun startAndConnect(name: String): BlackSharkCoolerDevice {
        Shadow.extract<ShadowBluetoothDevice>(coolerDevice()).setName(name)
        val device = BlackSharkCoolerDevice()
        device.delegate = this
        device.start(deviceId = bluetoothIdentifier(coolerAddress))
        settle()
        assertEquals(listOf(BlackSharkCoolerDeviceState.discovering, BlackSharkCoolerDeviceState.connecting), states)
        onQueue { callback().onConnectionStateChange(gatt(), BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED) }
        settle()
        onQueue { callback().onMtuChanged(gatt(), 247, BluetoothGatt.GATT_SUCCESS) }
        settle()
        assertEquals(listOf("requestMtu 517", "discoverServices"), calls())
        onQueue {
            recording().discovered = services()
            callback().onServicesDiscovered(gatt(), BluetoothGatt.GATT_SUCCESS)
        }
        settle()
        assertEquals(
            listOf("setCharacteristicNotification A002 true", "writeDescriptor A002 0100"),
            calls().drop(2),
        )
        assertEquals(BlackSharkCoolerDeviceState.connected, states.last())
        onQueue {
            callback().onDescriptorWrite(
                gatt(),
                gattCharacteristic(readId).getDescriptor(clientConfiguration),
                BluetoothGatt.GATT_SUCCESS,
            )
        }
        settle()
        answeredCalls = 4
        return device
    }

    private fun takeWrites(count: Int, ignorePolls: Boolean = true): List<String> {
        val polls = setOf(write("0506000000"), write("0506200000"))
        val writes = mutableListOf<String>()
        val deadline = System.currentTimeMillis() + 5_000
        while (writes.size < count) {
            assertTrue(System.currentTimeMillis() < deadline, "Timed out with writes $writes")
            settle()
            val fresh = calls().drop(answeredCalls)
            answeredCalls += fresh.size
            for (call in fresh) {
                if (!ignorePolls || call !in polls) {
                    writes.add(call)
                }
                onQueue {
                    callback().onCharacteristicWrite(gatt(), gattCharacteristic(writeId), BluetoothGatt.GATT_SUCCESS)
                }
            }
            if (fresh.isEmpty()) {
                Thread.sleep(10)
            }
        }
        return writes
    }

    private fun assertNoWrites() {
        settle()
        val polls = setOf(write("0506000000"), write("0506200000"))
        assertEquals(emptyList(), calls().drop(answeredCalls).filter { it !in polls })
    }

    private fun notifyCooler(message: String) {
        onQueue { callback().onCharacteristicChanged(gatt(), gattCharacteristic(readId), hex(message)) }
        settle()
    }

    @Test
    fun aPro4IsPolledAndGetsCoolingAndFanForTheThermalState() {
        val device = startAndConnect(name = "Black Shark MagCooler 4Pro")
        assertEquals(listOf(write("0506000000")), takeWrites(1, ignorePolls = false))
        notifyCooler("8a0600000108001c0000")
        assertEquals(listOf("pro4 8 28 null null"), statuses.toList())
        assertEquals(listOf(write("05050000fb"), write("050200005a")), takeWrites(2))
        setThermalStatus(PowerManager.THERMAL_STATUS_MODERATE)
        notifyCooler("8a0600000108001c0000")
        assertEquals(listOf(write("0505000014"), write("0502000032")), takeWrites(2))
        setThermalStatus(PowerManager.THERMAL_STATUS_SEVERE)
        notifyCooler("8a0600000108001c0000")
        assertEquals(listOf(write("0505000000"), write("0502000000")), takeWrites(2))
        onQueue { device.adjustCoolerProfilePro4(ProcessInfo.ThermalState.fair) }
        assertEquals(listOf(write("0505000050"), write("0502000050")), takeWrites(2))
        assertEquals(ProcessInfo.ThermalState.critical, ProcessInfo.processInfo.thermalState)
    }

    @Test
    fun aPro5IsPolledWithItsOwnCommandAndUsesCustomMode() {
        val device = startAndConnect(name = "Black Shark MagCooler 5Pro")
        assertEquals(listOf(write("0506200000")), takeWrites(1, ignorePolls = false))
        notifyCooler("89062000023258111c")
        assertEquals(listOf("pro5 2 50 4440 28"), statuses.toList())
        assertEquals(listOf(write("0507000001")), takeWrites(1))
        setThermalStatus(PowerManager.THERMAL_STATUS_LIGHT)
        notifyCooler("89062000ff3358111c")
        assertEquals(listOf(write("060500000401")), takeWrites(1))
        setThermalStatus(PowerManager.THERMAL_STATUS_CRITICAL)
        notifyCooler("89062000023258111c")
        assertEquals(listOf(write("060500000405")), takeWrites(1))
        onQueue { device.setCustomModePro5(2) }
        assertEquals(listOf(write("060500000402")), takeWrites(1))
    }

    @Test
    fun unknownFramesAndOtherCharacteristicsAreIgnored() {
        startAndConnect(name = "Black Shark MagCooler 4Pro")
        takeWrites(1, ignorePolls = false)
        notifyCooler("860210001414")
        notifyCooler("af0120000102")
        assertEquals(emptyList(), statuses.toList())
        assertNoWrites()
    }

    @Test
    fun ledCommandsAreThrottledToOneEvery80Milliseconds() {
        val device = startAndConnect(name = "Black Shark MagCooler 4Pro")
        takeWrites(1, ignorePolls = false)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        onQueue { device.setLedColor(color = RgbColor(red = 255, green = 100, blue = 0), brightness = 50) }
        assertEquals(listOf(write("2f01200006" + "00ffffff0001" + "7f3200" + zeros(33))), takeWrites(1))
        onQueue { device.setLedColor(color = RgbColor(red = 0, green = 0, blue = 255), brightness = 100) }
        assertNoWrites()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        onQueue { device.setLedColor(color = RgbColor(red = 0, green = 0, blue = 255), brightness = 100) }
        assertEquals(listOf(write("2f01200006" + "00ffffff0001" + "0000ff" + zeros(33))), takeWrites(1))
        onQueue { device.turnLedOff() }
        assertEquals(listOf(write("2f01200001" + "00ffffff0001" + "000000" + zeros(33))), takeWrites(1))
    }

    @Test
    fun theCoolerIsPolledEveryTwoSeconds() {
        startAndConnect(name = "Black Shark MagCooler 4Pro")
        assertEquals(listOf(write("0506000000")), takeWrites(1, ignorePolls = false))
        Thread.sleep(2_200)
        assertEquals(listOf(write("0506000000")), takeWrites(1, ignorePolls = false))
    }

    @Test
    fun aDroppedConnectionIsReconnectedAndStopDisconnects() {
        val device = startAndConnect(name = "Black Shark MagCooler 4Pro")
        takeWrites(1, ignorePolls = false)
        onQueue { callback().onConnectionStateChange(gatt(), 8, BluetoothProfile.STATE_DISCONNECTED) }
        settle()
        assertEquals(
            listOf(
                BlackSharkCoolerDeviceState.discovering,
                BlackSharkCoolerDeviceState.connecting,
                BlackSharkCoolerDeviceState.connected,
                BlackSharkCoolerDeviceState.discovering,
                BlackSharkCoolerDeviceState.connecting,
            ),
            states,
        )
        assertEquals(2, gatts().size)
        assertTrue(Shadow.extract<RecordingBluetoothGatt>(gatts().first()).isClosed)
        device.stop()
        settle()
        assertEquals(BlackSharkCoolerDeviceState.disconnected, states.last())
        assertTrue(recording().isClosed)
    }

    @Test
    fun aCoolerAndroidHasForgottenIsFoundByScanningAndGetsItsModelFromTheAdvertisedName() {
        Shadow.extract<ShadowBluetoothDevice>(coolerDevice()).setType(BluetoothDevice.DEVICE_TYPE_UNKNOWN)
        Shadow.extract<ShadowBluetoothDevice>(coolerDevice()).setName(null)
        val device = BlackSharkCoolerDevice()
        device.delegate = this
        device.start(deviceId = bluetoothIdentifier(coolerAddress))
        settle()
        assertEquals(listOf(BlackSharkCoolerDeviceState.discovering, BlackSharkCoolerDeviceState.connecting), states)
        assertEquals(0, gatts().size)
        val scanner = Shadow.extract<ShadowBluetoothLeScanner>(adapter().bluetoothLeScanner)
        val search = scanner.activeScans.single()
        assertEquals(listOf(coolerAddress), search.scanFilters().map { it.deviceAddress })
        val structure = byteArrayOf(0x09) + "Black Shark MagCooler 4Pro".toByteArray()
        val parse = ScanRecord::class.java.getDeclaredMethod("parseFromBytes", ByteArray::class.java)
        val record = parse.invoke(null, byteArrayOf(structure.size.toByte()) + structure) as ScanRecord
        search.scanCallback()!!.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, ScanResult(coolerDevice(), record, -50, 0))
        settle()
        assertEquals(emptyList(), scanner.activeScans)
        assertEquals(1, gatts().size)
        onQueue { callback().onConnectionStateChange(gatt(), BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED) }
        settle()
        onQueue { callback().onMtuChanged(gatt(), 247, BluetoothGatt.GATT_SUCCESS) }
        settle()
        onQueue {
            recording().discovered = services()
            callback().onServicesDiscovered(gatt(), BluetoothGatt.GATT_SUCCESS)
        }
        settle()
        onQueue {
            callback().onDescriptorWrite(
                gatt(),
                gattCharacteristic(readId).getDescriptor(clientConfiguration),
                BluetoothGatt.GATT_SUCCESS,
            )
        }
        settle()
        answeredCalls = 4
        assertEquals(listOf(write("0506000000")), takeWrites(1, ignorePolls = false))
        assertEquals(BlackSharkCoolerDeviceState.connected, states.last())
        device.stop()
        settle()
    }
}
