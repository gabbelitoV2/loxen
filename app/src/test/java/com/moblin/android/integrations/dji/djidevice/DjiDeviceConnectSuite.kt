package com.moblin.android.integrations.dji.djidevice

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
import com.moblin.android.integrations.dji.DjiMessage
import com.moblin.android.platform.corebluetooth.BluetoothAuthorization
import com.moblin.android.platform.corebluetooth.RecordingBluetoothGatt
import com.moblin.android.platform.corebluetooth.bluetoothIdentifier
import com.moblin.android.various.settings.SettingsDjiDeviceImageStabilization
import com.moblin.android.various.settings.SettingsDjiDeviceModel
import com.moblin.android.various.settings.SettingsDjiDeviceResolution
import com.moblin.android.various.settings.SettingsDjiDeviceVideoCodec
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBluetoothDevice

private const val cameraAddress = "60:60:1F:00:00:04"

private fun uuid16(value: String): UUID = UUID.fromString("0000$value-0000-1000-8000-00805F9B34FB")

private fun hex(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

private const val pairRequest =
    "553304c202079280400745203238346165356238643736623333373561303461363431376164373162656133046d626c6ece5a"
private const val stopRequest = "551304030208c8ea40028e01011a0001029219"
private const val preparingRequest = "550e04660208128c4002e11a11df"
private const val wifiRequest = "551904e40207198c40074704486f6d6506736563726574efd6"
private const val configureRequest = "5513040302012d8c40028e0101080001012bfd"
private const val startRequest =
    "553b04b402082c8c400878002b000a7017020003000000200072746d703a2f2f3139322e3136382e312e323a313933352f6c6976652f646a69982b"
private const val pairedResponse = "550f04a202079280400745000141ba"
private const val stopResponse = "550e04660208c8ea40028e00b170"
private const val preparingResponse = "550e04660208128c4002e100ca60"
private const val wifiResponse = "550f04a20207198c40074700001bdf"
private const val configureResponse = "550e046602012d8c40028e00373d"
private const val startResponse = "550e046602082c8c400878008723"
private const val statusMessage = "552204ea00000100000d0200000000000000000000000000000000000000004d5179"

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [RecordingBluetoothGatt::class])
class DjiDeviceConnectSuite : DjiDeviceDelegate {
    private lateinit var application: Application
    private val states = mutableListOf<DjiDeviceState>()
    private val djiService = uuid16("FFF0")
    private val fff3 = uuid16("FFF3")
    private val fff4 = uuid16("FFF4")
    private val fff5 = uuid16("FFF5")
    private val deviceInformation = uuid16("180A")
    private val manufacturerName = uuid16("2A29")
    private val clientConfiguration = uuid16("2902")

    override fun djiDeviceStreamingState(device: DjiDevice, state: DjiDeviceState) {
        states.add(state)
    }

    @Before
    fun setUp() {
        BluetoothAuthorization.reset()
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        shadowOf(application.packageManager).setSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE, true)
        shadowOf(adapter()).setState(BluetoothAdapter.STATE_ON)
        Shadow.extract<ShadowBluetoothDevice>(camera()).setType(BluetoothDevice.DEVICE_TYPE_LE)
    }

    @After
    fun tearDown() {
        BluetoothAuthorization.reset()
    }

    private fun adapter(): BluetoothAdapter = application.getSystemService(BluetoothManager::class.java).adapter

    private fun camera(): BluetoothDevice = adapter().getRemoteDevice(cameraAddress)

    private fun gatts(): List<BluetoothGatt> = Shadow.extract<ShadowBluetoothDevice>(camera()).bluetoothGatts

    private fun gatt(): BluetoothGatt = gatts().last()

    private fun recording(): RecordingBluetoothGatt = Shadow.extract(gatt())

    private fun callback(): BluetoothGattCallback = recording().gattCallback

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun advance(seconds: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds))
    }

    private fun services(): List<BluetoothGattService> {
        val permissions = BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        val dji = BluetoothGattService(djiService, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        for ((uuid, properties) in listOf(
            fff3 to BluetoothGattCharacteristic.PROPERTY_READ,
            fff4 to BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            fff5 to (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE),
        )) {
            val characteristic = BluetoothGattCharacteristic(uuid, properties, BluetoothGattCharacteristic.PERMISSION_READ)
            if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                characteristic.addDescriptor(BluetoothGattDescriptor(clientConfiguration, permissions))
            }
            dji.addCharacteristic(characteristic)
        }
        val information = BluetoothGattService(deviceInformation, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        information.addCharacteristic(
            BluetoothGattCharacteristic(
                manufacturerName,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ,
            ),
        )
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
        return listOf(genericAccess, genericAttribute, information, dji)
    }

    private fun gattCharacteristic(uuid: UUID): BluetoothGattCharacteristic {
        return recording().discovered.firstNotNullOf { it.getCharacteristic(uuid) }
    }

    private fun start(
        model: SettingsDjiDeviceModel = SettingsDjiDeviceModel.osmoAction4,
        imageStabilization: SettingsDjiDeviceImageStabilization = SettingsDjiDeviceImageStabilization.rockSteady,
    ): DjiDevice {
        val device = DjiDevice()
        device.delegate = this
        device.startLiveStream(
            wifiSsid = "Home",
            wifiPassword = "secret",
            rtmpUrl = "rtmp://192.168.1.2:1935/live/dji",
            resolution = SettingsDjiDeviceResolution.r1080p,
            fps = 30,
            bitrate = 6_000_000u,
            videoCodec = SettingsDjiDeviceVideoCodec.h265hevc,
            imageStabilization = imageStabilization,
            deviceId = bluetoothIdentifier(cameraAddress),
            model = model,
        )
        runMain()
        return device
    }

    private fun connectAndPair(device: DjiDevice) {
        assertEquals(DjiDeviceState.connecting, device.getState())
        callback().onConnectionStateChange(gatt(), BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        runMain()
        callback().onMtuChanged(gatt(), 185, BluetoothGatt.GATT_SUCCESS)
        assertEquals(listOf("requestMtu 517", "discoverServices"), recording().calls)
        recording().discovered = services()
        callback().onServicesDiscovered(gatt(), BluetoothGatt.GATT_SUCCESS)
        runMain()
        assertEquals(
            listOf("setCharacteristicNotification FFF4 true", "writeDescriptor FFF4 0100"),
            recording().calls.drop(2),
        )
        assertEquals(DjiDeviceState.connecting, device.getState())
        callback().onDescriptorWrite(
            gatt(),
            gattCharacteristic(fff4).getDescriptor(clientConfiguration),
            BluetoothGatt.GATT_SUCCESS,
        )
        runMain()
        assertEquals(DjiDeviceState.checkingIfPaired, device.getState())
        recording().calls.subList(0, 4).clear()
        expectWrites(pairRequest)
    }

    private fun write(message: String): String = "writeCharacteristic FFF5 $message 1"

    private fun written() {
        callback().onCharacteristicWrite(gatt(), gattCharacteristic(fff5), BluetoothGatt.GATT_SUCCESS)
    }

    private fun receive(message: String) {
        callback().onCharacteristicChanged(gatt(), gattCharacteristic(fff4), hex(message))
        runMain()
    }

    private fun expectWrites(vararg messages: String) {
        for (message in messages) {
            assertEquals(listOf(write(message)), recording().calls)
            recording().calls.clear()
            written()
        }
        assertEquals(emptyList(), recording().calls)
    }

    @Test
    fun anOsmoAction4GoesThroughPairingWifiConfigurationAndStartsStreaming() {
        val device = start()
        assertEquals(listOf(DjiDeviceState.discovering, DjiDeviceState.connecting), states)
        assertEquals(1, gatts().size)
        connectAndPair(device)
        receive(pairedResponse)
        assertEquals(DjiDeviceState.cleaningUp, device.getState())
        expectWrites(stopRequest)
        receive(stopResponse)
        assertEquals(DjiDeviceState.preparingStream, device.getState())
        expectWrites(preparingRequest)
        receive(preparingResponse)
        assertEquals(DjiDeviceState.settingUpWifi, device.getState())
        expectWrites(wifiRequest)
        receive(wifiResponse)
        assertEquals(DjiDeviceState.configuring, device.getState())
        expectWrites(configureRequest)
        receive(configureResponse)
        assertEquals(DjiDeviceState.startingStream, device.getState())
        expectWrites(startRequest)
        receive(startResponse)
        assertEquals(DjiDeviceState.streaming, device.getState())
        assertNull(device.getBatteryPercentage())
        receive(statusMessage)
        assertEquals(77, device.getBatteryPercentage())
        assertEquals(emptyList(), recording().calls)
        assertEquals(
            listOf(
                DjiDeviceState.discovering,
                DjiDeviceState.connecting,
                DjiDeviceState.checkingIfPaired,
                DjiDeviceState.cleaningUp,
                DjiDeviceState.preparingStream,
                DjiDeviceState.settingUpWifi,
                DjiDeviceState.configuring,
                DjiDeviceState.startingStream,
                DjiDeviceState.streaming,
            ),
            states,
        )
        advance(120)
        assertEquals(DjiDeviceState.streaming, device.getState())
        device.stopLiveStream()
        assertEquals(DjiDeviceState.stoppingStream, device.getState())
        expectWrites(stopRequest)
        receive(stopResponse)
        assertEquals(DjiDeviceState.idle, device.getState())
        assertTrue(recording().isClosed)
        assertNull(device.getBatteryPercentage())
    }

    @Test
    fun aCameraThatIsNotPairedYetIsAskedToPairAndContinuesOnItsNextMessage() {
        val device = start()
        connectAndPair(device)
        receive(DjiMessage(target = 0x0702u, id = 0x8092u, type = 0x450740u, payload = byteArrayOf(0, 0)).encode().hexString())
        assertEquals(DjiDeviceState.pairing, device.getState())
        assertEquals(emptyList(), recording().calls)
        receive(DjiMessage(target = 0x0702u, id = 0x1234u, type = 0x450740u, payload = byteArrayOf(1)).encode().hexString())
        assertEquals(DjiDeviceState.cleaningUp, device.getState())
        expectWrites(stopRequest)
    }

    @Test
    fun responsesToOtherTransactionsAndCorruptMessagesAreIgnored() {
        val device = start()
        connectAndPair(device)
        receive(stopResponse)
        assertEquals(DjiDeviceState.checkingIfPaired, device.getState())
        val corrupt = hex(pairedResponse)
        corrupt[corrupt.size - 1] = (corrupt[corrupt.size - 1] + 1).toByte()
        callback().onCharacteristicChanged(gatt(), gattCharacteristic(fff4), corrupt)
        runMain()
        assertEquals(DjiDeviceState.checkingIfPaired, device.getState())
        val badHeader = hex(pairedResponse)
        badHeader[3] = 0xC0.toByte()
        callback().onCharacteristicChanged(gatt(), gattCharacteristic(fff4), badHeader)
        runMain()
        assertEquals(DjiDeviceState.checkingIfPaired, device.getState())
        assertEquals(emptyList(), recording().calls)
        receive(pairedResponse)
        assertEquals(DjiDeviceState.cleaningUp, device.getState())
    }

    @Test
    fun aFailedWifiSetupDisconnectsAndReportsWifiSetupFailed() {
        val device = start()
        connectAndPair(device)
        receive(pairedResponse)
        expectWrites(stopRequest)
        receive(stopResponse)
        expectWrites(preparingRequest)
        receive(preparingResponse)
        expectWrites(wifiRequest)
        receive(DjiMessage(target = 0x0702u, id = 0x8C19u, type = 0x470740u, payload = byteArrayOf(0, 1)).encode().hexString())
        assertEquals(DjiDeviceState.wifiSetupFailed, device.getState())
        assertEquals(listOf(DjiDeviceState.idle, DjiDeviceState.wifiSetupFailed), states.takeLast(2))
        assertTrue(recording().isClosed)
        assertEquals(emptyList(), recording().calls)
    }

    @Test
    fun newProtocolCamerasGetTheirOwnConfigurationAndAStartConfirmation() {
        val device = start(model = SettingsDjiDeviceModel.osmoAction5Pro)
        connectAndPair(device)
        receive(pairedResponse)
        expectWrites(stopRequest)
        receive(stopResponse)
        expectWrites(preparingRequest)
        receive(preparingResponse)
        expectWrites(wifiRequest)
        receive(wifiResponse)
        val configure = DjiMessage(
            target = 0x0102u,
            id = 0x8C2Du,
            type = 0x8E0240u,
            payload = hex("01011a000101"),
        ).encode().hexString()
        expectWrites(configure)
        receive(configureResponse)
        val start = DjiMessage(
            target = 0x0802u,
            id = 0x8C2Cu,
            type = 0x780840u,
            payload = hex("002b000a7017020003000000200072746d703a2f2f3139322e3136382e312e323a313933352f6c6976652f646a69"),
        ).encode().hexString()
        val confirm = DjiMessage(
            target = 0x0802u,
            id = 0xEAC8u,
            type = 0x8E0240u,
            payload = hex("01011a000101"),
        ).encode().hexString()
        expectWrites(start, confirm)
        assertEquals(DjiDeviceState.startingStream, device.getState())
    }

    @Test
    fun anOsmoPocket3StartsStreamingRightAfterTheWifiSetup() {
        val device = start(model = SettingsDjiDeviceModel.osmoPocket3)
        connectAndPair(device)
        receive(pairedResponse)
        expectWrites(stopRequest)
        receive(stopResponse)
        expectWrites(preparingRequest)
        receive(preparingResponse)
        expectWrites(wifiRequest)
        receive(wifiResponse)
        assertEquals(DjiDeviceState.startingStream, device.getState())
        expectWrites(startRequest)
    }

    @Test
    fun theStartTimerGivesUpAfterOneMinute() {
        val device = start()
        assertEquals(DjiDeviceState.connecting, device.getState())
        advance(59)
        assertEquals(DjiDeviceState.connecting, device.getState())
        assertFalse(recording().isClosed)
        advance(2)
        assertEquals(DjiDeviceState.idle, device.getState())
        assertTrue(recording().isClosed)
    }

    @Test
    fun stoppingGivesUpAfterTenSecondsWithoutAnAnswer() {
        val device = start()
        connectAndPair(device)
        receive(pairedResponse)
        expectWrites(stopRequest)
        device.stopLiveStream()
        assertEquals(DjiDeviceState.stoppingStream, device.getState())
        expectWrites(stopRequest)
        advance(9)
        assertEquals(DjiDeviceState.stoppingStream, device.getState())
        advance(2)
        assertEquals(DjiDeviceState.idle, device.getState())
        assertTrue(recording().isClosed)
    }

    @Test
    fun aDisconnectResetsTheDevice() {
        val device = start()
        connectAndPair(device)
        callback().onConnectionStateChange(gatt(), 19, BluetoothProfile.STATE_DISCONNECTED)
        runMain()
        assertEquals(DjiDeviceState.idle, device.getState())
        assertTrue(recording().isClosed)
        assertEquals(1, gatts().size)
    }

    @Test
    fun stoppingAnIdleDeviceDoesNothing() {
        val device = DjiDevice()
        device.delegate = this
        device.stopLiveStream()
        runMain()
        assertEquals(DjiDeviceState.idle, device.getState())
        assertEquals(emptyList(), states)
    }
}

private fun ByteArray.hexString(): String = joinToString("") { "%02x".format(it) }
