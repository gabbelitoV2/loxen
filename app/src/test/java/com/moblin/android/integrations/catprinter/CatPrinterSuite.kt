package com.moblin.android.integrations.catprinter

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
import android.content.pm.PackageManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.platform.corebluetooth.BluetoothAuthorization
import com.moblin.android.platform.corebluetooth.CBPeripheral
import com.moblin.android.platform.corebluetooth.RecordingBluetoothGatt
import com.moblin.android.platform.corebluetooth.bluetoothIdentifier
import com.moblin.android.platform.coreimage.CIColor
import com.moblin.android.platform.coreimage.CIImage
import java.util.Collections
import java.util.UUID
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

private const val printerAddress = "C0:FF:EE:00:00:0C"

private fun uuid16(value: String): UUID = UUID.fromString("0000$value-0000-1000-8000-00805F9B34FB")

private fun hex(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

private fun hex(value: ByteArray): String = value.joinToString("") { "%02x".format(it) }

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [RecordingBluetoothGatt::class])
class CatPrinterSuite : CatPrinterDelegate {
    private lateinit var application: Application
    private val states = Collections.synchronizedList(mutableListOf<CatPrinterState>())
    private val printerService = UUID.fromString("0000af30-0000-1000-8000-00805f9b34fb")
    private val printId = uuid16("AE01")
    private val notifyId = uuid16("AE02")
    private val dataId = uuid16("AE03")
    private val clientConfiguration = uuid16("2902")
    private var answeredCalls = 0

    override fun catPrinterState(catPrinter: CatPrinter, state: CatPrinterState) {
        states.add(state)
    }

    @Before
    fun setUp() {
        BluetoothAuthorization.reset()
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        shadowOf(application.packageManager).setSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE, true)
        shadowOf(adapter()).setState(BluetoothAdapter.STATE_ON)
        Shadow.extract<ShadowBluetoothDevice>(printerDevice()).setType(BluetoothDevice.DEVICE_TYPE_LE)
    }

    @After
    fun tearDown() {
        BluetoothAuthorization.reset()
    }

    private fun adapter(): BluetoothAdapter = application.getSystemService(BluetoothManager::class.java).adapter

    private fun printerDevice(): BluetoothDevice = adapter().getRemoteDevice(printerAddress)

    private fun gatts(): List<BluetoothGatt> = Shadow.extract<ShadowBluetoothDevice>(printerDevice()).bluetoothGatts

    private fun gatt(): BluetoothGatt = gatts().last()

    private fun recording(): RecordingBluetoothGatt = Shadow.extract(gatt())

    private fun callback(): BluetoothGattCallback = recording().gattCallback

    private fun queue(): CoroutineDispatcher {
        val field = Class.forName("com.moblin.android.integrations.catprinter.CatPrinterKt")
            .getDeclaredField("catPrinterDispatchQueue")
        field.isAccessible = true
        return when (val value = field.get(null)) {
            is CoroutineDispatcher -> value
            is CoroutineScope -> value.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
            else -> error("Unexpected cat printer queue $value")
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

    private fun privateField(target: Any, name: String): Any? {
        return target.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(target)
    }

    private fun services(): List<BluetoothGattService> {
        val service = BluetoothGattService(printerService, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val permissions = BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        for ((uuid, properties) in listOf(
            printId to (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE),
            notifyId to BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            dataId to BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
        )) {
            val characteristic = BluetoothGattCharacteristic(uuid, properties, BluetoothGattCharacteristic.PERMISSION_READ)
            if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                characteristic.addDescriptor(BluetoothGattDescriptor(clientConfiguration, permissions))
            }
            service.addCharacteristic(characteristic)
        }
        val genericAccess = BluetoothGattService(uuid16("1800"), BluetoothGattService.SERVICE_TYPE_PRIMARY)
        genericAccess.addCharacteristic(
            BluetoothGattCharacteristic(
                uuid16("2A00"),
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
            ),
        )
        val genericAttribute = BluetoothGattService(uuid16("1801"), BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val serviceChanged = BluetoothGattCharacteristic(
            uuid16("2A05"),
            BluetoothGattCharacteristic.PROPERTY_INDICATE,
            BluetoothGattCharacteristic.PERMISSION_READ,
        )
        serviceChanged.addDescriptor(BluetoothGattDescriptor(clientConfiguration, permissions))
        genericAttribute.addCharacteristic(serviceChanged)
        return listOf(genericAccess, genericAttribute, service)
    }

    private fun gattCharacteristic(uuid: UUID): BluetoothGattCharacteristic {
        return recording().discovered.firstNotNullOf { it.getCharacteristic(uuid) }
    }

    private fun startAndConnect(name: String? = null): CatPrinter {
        if (name != null) {
            Shadow.extract<ShadowBluetoothDevice>(printerDevice()).setName(name)
        }
        val printer = CatPrinter()
        printer.delegate = this
        printer.start(deviceId = bluetoothIdentifier(printerAddress), meowSoundEnabled = false)
        settle()
        assertEquals(listOf(CatPrinterState.discovering, CatPrinterState.connecting), states)
        assertEquals(1, gatts().size)
        onQueue { callback().onConnectionStateChange(gatt(), BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED) }
        settle()
        onQueue { callback().onMtuChanged(gatt(), 185, BluetoothGatt.GATT_SUCCESS) }
        settle()
        assertEquals(listOf("requestMtu 517", "discoverServices"), calls())
        onQueue {
            recording().discovered = services()
            callback().onServicesDiscovered(gatt(), BluetoothGatt.GATT_SUCCESS)
        }
        settle()
        assertEquals(
            listOf("setCharacteristicNotification AE02 true", "writeDescriptor AE02 0100"),
            calls().drop(2),
        )
        assertEquals(CatPrinterState.connected, printer.getState())
        onQueue {
            callback().onDescriptorWrite(
                gatt(),
                gattCharacteristic(notifyId).getDescriptor(clientConfiguration),
                BluetoothGatt.GATT_SUCCESS,
            )
        }
        settle()
        answeredCalls = calls().size
        return printer
    }

    private fun newWrites(): List<Pair<String, String>> {
        val calls = calls()
        val fresh = calls.drop(answeredCalls)
        answeredCalls = calls.size
        return fresh.map {
            val parts = it.split(" ")
            assertEquals("writeCharacteristic", parts[0], "Unexpected call $it")
            assertEquals(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE.toString(), parts[3], "Unexpected write type in $it")
            parts[1] to parts[2]
        }
    }

    private fun acknowledge(writes: List<Pair<String, String>>) {
        for ((characteristic, _) in writes) {
            onQueue {
                callback().onCharacteristicWrite(
                    gatt(),
                    gattCharacteristic(uuid16(characteristic)),
                    BluetoothGatt.GATT_SUCCESS,
                )
            }
            settle()
        }
    }

    private fun collectWrites(timeoutMs: Long = 10_000, done: (List<Pair<String, String>>) -> Boolean): List<Pair<String, String>> {
        val writes = mutableListOf<Pair<String, String>>()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!done(writes)) {
            assertTrue(System.currentTimeMillis() < deadline, "Timed out with writes $writes")
            settle()
            val fresh = newWrites()
            writes.addAll(fresh)
            acknowledge(fresh)
            if (fresh.isEmpty()) {
                Thread.sleep(10)
            }
        }
        return writes
    }

    private fun notifyPrinter(message: String) {
        onQueue { callback().onCharacteristicChanged(gatt(), gattCharacteristic(notifyId), hex(message)) }
        settle()
    }

    private fun printDirectly(printer: CatPrinter, mxw01: Boolean, image: List<UByteArray>, feedPaperDelay: Double?) {
        val jobClass = Class.forName("com.moblin.android.integrations.catprinter.PrintJob")
        val constructor = jobClass.declaredConstructors.first { it.parameterCount == 3 }
        constructor.isAccessible = true
        val job = constructor.newInstance(CIImage(color = CIColor(0.0, 0.0, 0.0)), feedPaperDelay, CatPrinterPrintMode.blackAndWhite)
        val method = CatPrinter::class.java.declaredMethods.single {
            it.name == if (mxw01) "tryPrintNextMxw01" else "tryPrintNextDefault"
        }
        method.isAccessible = true
        onQueue {
            val peripheral = privateField(printer, "peripheral") as CBPeripheral
            method.invoke(printer, job, image, peripheral)
        }
        settle()
    }

    private fun image(rows: Int): List<UByteArray> {
        return List(rows) { row -> UByteArray(catPrinterWidthPixels) { if ((it + row) % 5 == 0) 1u else 0u } }
    }

    @Test
    fun connectsSubscribesToNotificationsAndReportsConnected() {
        val printer = startAndConnect(name = "GB02")
        assertEquals(
            listOf(CatPrinterState.discovering, CatPrinterState.connecting, CatPrinterState.connected),
            states,
        )
        assertEquals(emptyList(), newWrites())
        printer.stop()
        settle()
        assertEquals(CatPrinterState.disconnected, printer.getState())
        assertTrue(recording().isClosed)
    }

    @Test
    fun defaultPrinterWritesTheJobInMtuSizedChunksAfterTheDeviceStateReply() {
        val printer = startAndConnect(name = "GB02")
        val image = image(rows = 3)
        printDirectly(printer, mxw01 = false, image = image, feedPaperDelay = null)
        val request = collectWrites { it.isNotEmpty() }
        assertEquals(listOf("AE01" to "5178a30001000000ff"), request)
        notifyPrinter("5178ae0001000000ff")
        assertEquals(emptyList(), newWrites())
        notifyPrinter("5178a30001000000ff")
        val expected = hex(catPrinterPackPrintImageCommands(image, feedPaper = true, printMode = CatPrinterPrintMode.blackAndWhite))
        val chunks = collectWrites { writes -> writes.joinToString("") { it.second }.length >= expected.length }
        assertTrue(chunks.all { it.first == "AE01" })
        assertEquals(expected, chunks.joinToString("") { it.second })
        assertEquals(182, chunks.first().second.length / 2)
        Thread.sleep(300)
        settle()
        assertEquals(emptyList(), newWrites())
        assertNull(privateField(printer, "currentJob"))
    }

    @Test
    fun defaultPrinterFeedsPaperAfterTheDelayInsteadOfInTheJob() {
        val printer = startAndConnect(name = "GB02")
        val image = image(rows = 1)
        printDirectly(printer, mxw01 = false, image = image, feedPaperDelay = 0.3)
        collectWrites { it.isNotEmpty() }
        notifyPrinter("5178a30001000000ff")
        val expected = hex(catPrinterPackPrintImageCommands(image, feedPaper = false, printMode = CatPrinterPrintMode.blackAndWhite))
        val chunks = collectWrites { writes -> writes.joinToString("") { it.second }.length >= expected.length }
        assertEquals(expected, chunks.joinToString("") { it.second })
        val feed = collectWrites { it.isNotEmpty() }
        assertEquals(listOf("AE01" to "5178a10002003200d3ff"), feed)
        assertNull(privateField(printer, "currentJob"))
    }

    @Test
    fun mxw01AsksForStatusThenPrintsThroughTheDataCharacteristic() {
        val printer = startAndConnect(name = "MXW01")
        val image = image(rows = 2)
        printDirectly(printer, mxw01 = true, image = image, feedPaperDelay = null)
        assertEquals(listOf("AE01" to "2221a10001000000ff"), collectWrites { it.isNotEmpty() })
        notifyPrinter("2221a90001000000ff")
        assertEquals(emptyList(), newWrites())
        notifyPrinter("2221a1000800010203040506000471ff")
        assertEquals(listOf("AE01" to "2221a90004005a00300099ff"), collectWrites { it.isNotEmpty() })
        notifyPrinter("2221a90001000000ff")
        val expected = hex(catPrinterPackPrintImageCommandsMxw01(image, CatPrinterPrintMode.blackAndWhite))
        val chunks = collectWrites { writes -> writes.joinToString("") { it.second }.length >= expected.length }
        assertTrue(chunks.all { it.first == "AE03" })
        assertEquals(182, chunks.first().second.length / 2)
        assertEquals(expected, chunks.joinToString("") { it.second })
        notifyPrinter("2221aa000200070853ff")
        assertNull(privateField(printer, "currentJob"))
        assertEquals(emptyList(), newWrites())
    }

    @Test
    fun mxw01PrintRequestRejectionStopsTheJob() {
        val printer = startAndConnect(name = "MXW01")
        printDirectly(printer, mxw01 = true, image = image(rows = 1), feedPaperDelay = null)
        collectWrites { it.isNotEmpty() }
        notifyPrinter("2221a1000800010203040506000471ff")
        collectWrites { it.isNotEmpty() }
        notifyPrinter("2221a90001000107ff")
        Thread.sleep(300)
        settle()
        assertEquals(emptyList(), newWrites())
        assertNotNull(privateField(printer, "currentJob"))
    }

    @Test
    fun aDroppedConnectionStartsDiscoveringAndConnectingAgain() {
        val printer = startAndConnect()
        onQueue { callback().onConnectionStateChange(gatt(), 19, BluetoothProfile.STATE_DISCONNECTED) }
        settle()
        assertEquals(
            listOf(
                CatPrinterState.discovering,
                CatPrinterState.connecting,
                CatPrinterState.connected,
                CatPrinterState.discovering,
                CatPrinterState.connecting,
            ),
            states,
        )
        assertEquals(2, gatts().size)
        assertTrue(Shadow.extract<RecordingBluetoothGatt>(gatts().first()).isClosed)
        assertEquals(CatPrinterState.connecting, printer.getState())
    }

    @Test
    fun anUnknownDeviceStaysDiscovering() {
        val printer = CatPrinter()
        printer.delegate = this
        printer.start(deviceId = null, meowSoundEnabled = false)
        settle()
        assertEquals(listOf(CatPrinterState.discovering), states)
        assertTrue(gatts().isEmpty())
        assertEquals(CatPrinterState.discovering, printer.getState())
    }
}
