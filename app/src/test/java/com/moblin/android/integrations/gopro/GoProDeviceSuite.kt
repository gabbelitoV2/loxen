package com.moblin.android.integrations.gopro

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
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumLens
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumLiveStreamStatus
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumProvisioning
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumResultGeneric
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumScanEntryFlags
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumScanning
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_NotifProvisioningState
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_NotifStartScanning
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_NotifyLiveStreamStatus
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_ResponseConnect
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_ResponseGetApEntries
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_ResponseStartScanning
import com.moblin.android.platform.corebluetooth.BluetoothAuthorization
import com.moblin.android.platform.corebluetooth.CBUUID
import com.moblin.android.platform.corebluetooth.RecordingBluetoothGatt
import com.moblin.android.platform.corebluetooth.bluetoothIdentifier
import com.moblin.android.platform.swiftprotobuf.serializedData
import com.moblin.android.various.settings.SettingsGoProLaunchLiveStreamResolution
import com.moblin.android.various.settings.SettingsGoProLens
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
import org.robolectric.shadows.ShadowBluetoothLeScanner

private const val cameraAddress = "C0:FF:EE:00:00:47"
private const val otherCameraAddress = "C0:FF:EE:00:00:48"
private const val rtmpUrl = "rtmp://192.168.0.2:1935/live/key"
private val clientConfigurationId: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
private val batteryServiceId = CBUUID(string = "180F")
private val batteryLevelId = CBUUID(string = "2A19")

private fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }

private fun String.bytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }

private fun written(id: CBUUID, payload: ByteArray): String = "${id.uuidString} ${payload.hex()}"

private class GoProStateRecorder : GoProDeviceDelegate {
    val states = mutableListOf<GoProDeviceState>()

    override fun goProDeviceStreamingState(device: GoProDevice, state: GoProDeviceState) {
        states.add(state)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [RecordingBluetoothGatt::class])
class GoProDeviceSuite {
    private lateinit var application: Application
    private val recorder = GoProStateRecorder()
    private var device: GoProDevice? = null
    private val services by lazy { cameraServices() }
    private var answered = 0
    private val accumulators = mutableMapOf<String, GoProBleMessageAccumulator>()
    private val sent = mutableListOf<String>()

    @Before
    fun setUp() {
        BluetoothAuthorization.reset()
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).grantPermissions(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        shadowOf(application.packageManager).setSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE, true)
        shadowOf(adapter()).setState(BluetoothAdapter.STATE_ON)
        for (address in listOf(cameraAddress, otherCameraAddress)) {
            Shadow.extract<ShadowBluetoothDevice>(adapter().getRemoteDevice(address)).setType(BluetoothDevice.DEVICE_TYPE_LE)
        }
    }

    @After
    fun tearDown() {
        device?.stopLiveStream()
        runMain()
        BluetoothAuthorization.reset()
    }

    private fun adapter(): BluetoothAdapter = application.getSystemService(BluetoothManager::class.java).adapter

    private fun gatts(address: String = cameraAddress): List<BluetoothGatt> {
        return Shadow.extract<ShadowBluetoothDevice>(adapter().getRemoteDevice(address)).bluetoothGatts
    }

    private fun gatt(): BluetoothGatt = gatts().last()

    private fun recording(): RecordingBluetoothGatt = Shadow.extract(gatt())

    private fun callback(): BluetoothGattCallback = recording().gattCallback

    private fun calls(): List<String> = recording().calls

    private fun scanCallbacks(): Set<ScanCallback> {
        val scanner = adapter().bluetoothLeScanner ?: return emptySet()
        return Shadow.extract<ShadowBluetoothLeScanner>(scanner).scanCallbacks
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun advance(seconds: Long) {
        repeat(seconds.toInt()) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            pump()
        }
    }

    private fun characteristic(id: CBUUID, properties: Int): BluetoothGattCharacteristic {
        val characteristic = BluetoothGattCharacteristic(id.uuid, properties, BluetoothGattCharacteristic.PERMISSION_WRITE)
        if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            val permissions = BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            characteristic.addDescriptor(BluetoothGattDescriptor(clientConfigurationId, permissions))
        }
        return characteristic
    }

    private fun service(id: CBUUID, vararg characteristics: Pair<CBUUID, Int>): BluetoothGattService {
        val service = BluetoothGattService(id.uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        for ((characteristicId, properties) in characteristics) {
            service.addCharacteristic(characteristic(characteristicId, properties))
        }
        return service
    }

    private fun cameraServices(): List<BluetoothGattService> {
        val write = BluetoothGattCharacteristic.PROPERTY_WRITE
        val notify = BluetoothGattCharacteristic.PROPERTY_NOTIFY
        return listOf(
            service(batteryServiceId, batteryLevelId to notify),
            service(
                goProControlServiceId,
                goProCommandId to write,
                goProCommandResponseId to notify,
                goProSettingsId to write,
                goProSettingsResponseId to notify,
                goProQueryId to write,
                goProQueryResponseId to notify,
            ),
            service(
                goProCameraManagementServiceId,
                goProNetworkManagementId to write,
                goProNetworkManagementResponseId to notify,
            ),
        )
    }

    private fun gattCharacteristic(id: CBUUID): BluetoothGattCharacteristic {
        return services.firstNotNullOf { it.getCharacteristic(id.uuid) }
    }

    private fun descriptor(id: CBUUID): BluetoothGattDescriptor {
        return gattCharacteristic(id).getDescriptor(clientConfigurationId)
    }

    private fun start(
        wifiSsid: String = "Moblin",
        lens: SettingsGoProLens = SettingsGoProLens.auto,
        deviceId: UUID = bluetoothIdentifier(cameraAddress),
    ): GoProDevice {
        val device = GoProDevice()
        device.delegate = recorder
        this.device = device
        device.startLiveStream(
            wifiSsid = wifiSsid,
            wifiPassword = "secret",
            rtmpUrl = rtmpUrl,
            resolution = SettingsGoProLaunchLiveStreamResolution.r1080p,
            bitrate = 6_000_000u,
            lens = lens,
            deviceId = deviceId,
        )
        runMain()
        return device
    }

    private fun connect() {
        recording().discovered = services
        callback().onConnectionStateChange(gatt(), BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        runMain()
    }

    private fun pump() {
        while (true) {
            runMain()
            val calls = calls()
            if (answered >= calls.size) {
                return
            }
            val call = calls[answered].split(" ")
            answered += 1
            when (call[0]) {
                "requestMtu" -> callback().onMtuChanged(gatt(), 517, BluetoothGatt.GATT_SUCCESS)
                "discoverServices" -> callback().onServicesDiscovered(gatt(), BluetoothGatt.GATT_SUCCESS)
                "writeDescriptor" -> callback().onDescriptorWrite(
                    gatt(),
                    descriptor(CBUUID(string = call[1])),
                    BluetoothGatt.GATT_SUCCESS,
                )
                "writeCharacteristic" -> {
                    assertEquals("2", call[3])
                    val message = accumulators.getOrPut(call[1]) { GoProBleMessageAccumulator() }
                        .append(packet = call[2].bytes())
                    if (message != null) {
                        sent.add("${call[1]} ${message.hex()}")
                    }
                    callback().onCharacteristicWrite(
                        gatt(),
                        gattCharacteristic(CBUUID(string = call[1])),
                        BluetoothGatt.GATT_SUCCESS,
                    )
                }
            }
        }
    }

    private fun notify(id: CBUUID, payload: ByteArray) {
        for (packet in goProBlePackets(payload = payload)) {
            callback().onCharacteristicChanged(gatt(), gattCharacteristic(id), packet)
        }
        pump()
    }

    private fun startAndPair(wifiSsid: String = "Moblin", lens: SettingsGoProLens = SettingsGoProLens.auto): GoProDevice {
        val device = start(wifiSsid = wifiSsid, lens = lens)
        connect()
        pump()
        assertEquals(GoProDeviceState.pairing, device.getState())
        assertEquals(listOf(written(goProNetworkManagementId, goProPairingCompleteMessage())), sent)
        sent.clear()
        return device
    }

    private fun startScanning() {
        notify(goProNetworkManagementResponseId, bytes(0x03, 0x81, 0x08, 0x00))
        assertEquals(listOf(written(goProNetworkManagementId, goProStartScanMessage())), sent)
        sent.clear()
        val response = OpenGopro_ResponseStartScanning()
        response.result = OpenGopro_EnumResultGeneric.resultSuccess
        response.scanningState = OpenGopro_EnumScanning.scanningStarted
        notify(goProNetworkManagementResponseId, bytes(0x02, 0x82) + response.serializedData())
    }

    private fun scanDone(totalEntries: Int) {
        val notification = OpenGopro_NotifStartScanning()
        notification.scanningState = OpenGopro_EnumScanning.scanningSuccess
        notification.scanID = 3
        notification.totalEntries = totalEntries
        notification.totalConfiguredSsid = 1
        notify(goProNetworkManagementResponseId, bytes(0x02, 0x0B) + notification.serializedData())
    }

    private fun apEntries(vararg entries: Pair<String, Boolean>) {
        val response = OpenGopro_ResponseGetApEntries()
        response.result = OpenGopro_EnumResultGeneric.resultSuccess
        response.scanID = 3
        for ((ssid, configured) in entries) {
            val entry = OpenGopro_ResponseGetApEntries.ScanEntry()
            entry.ssid = ssid
            entry.signalStrengthBars = 3
            entry.signalFrequencyMhz = 2437
            entry.scanEntryFlags = if (configured) OpenGopro_EnumScanEntryFlags.scanFlagConfigured.rawValue else 0
            response.entries.add(entry)
        }
        notify(goProNetworkManagementResponseId, bytes(0x02, 0x83) + response.serializedData())
    }

    private fun connectResponse(actionId: Int, provisioningState: OpenGopro_EnumProvisioning, timeoutSeconds: Int) {
        val response = OpenGopro_ResponseConnect()
        response.result = OpenGopro_EnumResultGeneric.resultSuccess
        response.provisioningState = provisioningState
        response.timeoutSeconds = timeoutSeconds
        notify(goProNetworkManagementResponseId, bytes(0x02, actionId) + response.serializedData())
    }

    private fun liveStreamStatus(
        actionId: Int,
        status: OpenGopro_EnumLiveStreamStatus,
        supportedLenses: List<OpenGopro_EnumLens>? = null,
    ) {
        val notification = OpenGopro_NotifyLiveStreamStatus()
        notification.liveStreamStatus = status
        if (supportedLenses != null) {
            notification.liveStreamLensSupported = true
            notification.liveStreamLensSupportedArray = supportedLenses.toMutableList()
        }
        notify(goProQueryResponseId, bytes(0xF5, actionId) + notification.serializedData())
    }

    private fun joinConfiguredWifi() {
        startScanning()
        scanDone(totalEntries = 2)
        assertEquals(
            listOf(written(goProNetworkManagementId, goProGetApEntriesMessage(scanId = 3, startIndex = 0, maximumEntries = 2))),
            sent,
        )
        sent.clear()
        apEntries("Other" to false, "Moblin" to true)
        assertEquals(listOf(written(goProNetworkManagementId, goProConnectToProvisionedWifiMessage(ssid = "Moblin"))), sent)
        sent.clear()
        connectResponse(0x84, OpenGopro_EnumProvisioning.provisioningSuccessOldAp, 20)
    }

    private fun configure(supportedLenses: List<OpenGopro_EnumLens>) {
        assertEquals(
            listOf(
                written(goProQueryId, goProRegisterLiveStreamStatusMessage()),
                written(goProCommandId, goProSetShutterMessage(on = false)),
            ),
            sent,
        )
        sent.clear()
        liveStreamStatus(0xF4, OpenGopro_EnumLiveStreamStatus.liveStreamStateIdle, supportedLenses)
        notify(goProCommandResponseId, bytes(0x01, 0x00))
    }

    private fun liveStreamMode(lens: SettingsGoProLens): String {
        return written(
            goProCommandId,
            goProSetLiveStreamModeMessage(
                url = rtmpUrl,
                resolution = SettingsGoProLaunchLiveStreamResolution.r1080p,
                bitrate = 6_000_000u,
                lens = lens,
            ),
        )
    }

    @Test
    fun connectsToTheSelectedCameraByIdentifierWithoutScanning() {
        val device = start()
        assertEquals(1, gatts(cameraAddress).size)
        assertEquals(0, gatts(otherCameraAddress).size)
        assertEquals(emptySet(), scanCallbacks())
        assertEquals(listOf(GoProDeviceState.discovering, GoProDeviceState.connecting), recorder.states)
        assertEquals(GoProDeviceState.connecting, device.getState())
    }

    @Test
    fun aCameraThatIsNotABluetoothAddressFailsLikeAMissingPeripheral() {
        val device = start(deviceId = UUID.fromString("5C1D6F0E-0000-4000-8000-00000000A1B2"))
        assertEquals(GoProDeviceState.failed, device.getState())
        assertEquals(0, gatts(cameraAddress).size)
    }

    @Test
    fun subscribesToTheResponseCharacteristicsOneAtATimeAndThenPairs() {
        val device = start()
        connect()
        assertEquals(listOf("requestMtu 517"), calls())
        callback().onMtuChanged(gatt(), 517, BluetoothGatt.GATT_SUCCESS)
        runMain()
        assertEquals(listOf("requestMtu 517", "discoverServices"), calls())
        callback().onServicesDiscovered(gatt(), BluetoothGatt.GATT_SUCCESS)
        runMain()
        val responses = listOf(
            goProCommandResponseId,
            goProSettingsResponseId,
            goProQueryResponseId,
            goProNetworkManagementResponseId,
        )
        for ((index, id) in responses.withIndex()) {
            assertEquals(
                listOf("setCharacteristicNotification ${id.uuidString} true", "writeDescriptor ${id.uuidString} 0100"),
                calls().drop(2 + 2 * index),
            )
            assertEquals(GoProDeviceState.connecting, device.getState())
            callback().onDescriptorWrite(gatt(), descriptor(id), BluetoothGatt.GATT_SUCCESS)
            runMain()
        }
        assertEquals(GoProDeviceState.pairing, device.getState())
        val packet = goProBlePackets(payload = goProPairingCompleteMessage()).single()
        assertEquals("writeCharacteristic ${goProNetworkManagementId.uuidString} ${packet.hex()} 2", calls().last())
        assertEquals(11, calls().size)
        assertTrue(calls().none { batteryLevelId.uuidString in it })
    }

    @Test
    fun streamsAfterPairingWifiAndLiveStreamSetupAndStopsWithTheShutter() {
        val device = startAndPair(lens = SettingsGoProLens.linear)
        joinConfiguredWifi()
        assertEquals(GoProDeviceState.configuring, device.getState())
        configure(supportedLenses = listOf(OpenGopro_EnumLens.lensWide, OpenGopro_EnumLens.lensLinear))
        assertEquals(listOf(liveStreamMode(SettingsGoProLens.linear)), sent)
        sent.clear()
        advance(1)
        assertEquals(listOf(written(goProQueryId, goProGetLiveStreamStatusMessage())), sent)
        sent.clear()
        liveStreamStatus(0xF5, OpenGopro_EnumLiveStreamStatus.liveStreamStateReady)
        assertEquals(GoProDeviceState.startingStream, device.getState())
        assertFalse(sent.contains(written(goProCommandId, goProSetShutterMessage(on = true))))
        advance(2)
        assertEquals(1, sent.count { it == written(goProCommandId, goProSetShutterMessage(on = true)) })
        liveStreamStatus(0xF5, OpenGopro_EnumLiveStreamStatus.liveStreamStateStreaming)
        assertEquals(GoProDeviceState.streaming, device.getState())
        assertEquals(written(goProQueryId, goProGetBatteryPercentageMessage()), sent.last())
        assertNull(device.getBatteryPercentage())
        notify(goProQueryResponseId, bytes(0x13, 0x00, 0x46, 0x01, 0x55))
        assertEquals(85, device.getBatteryPercentage())
        sent.clear()
        advance(5)
        assertFalse(sent.contains(written(goProQueryId, goProGetLiveStreamStatusMessage())))
        sent.clear()
        device.stopLiveStream()
        pump()
        assertEquals(GoProDeviceState.stoppingStream, device.getState())
        assertEquals(listOf(written(goProCommandId, goProSetShutterMessage(on = false))), sent)
        assertFalse(recording().isClosed)
        notify(goProCommandResponseId, bytes(0x01, 0x00))
        assertEquals(GoProDeviceState.idle, device.getState())
        assertTrue(recording().isClosed)
        assertEquals(
            listOf(
                GoProDeviceState.discovering,
                GoProDeviceState.connecting,
                GoProDeviceState.pairing,
                GoProDeviceState.settingUpWifi,
                GoProDeviceState.configuring,
                GoProDeviceState.startingStream,
                GoProDeviceState.streaming,
                GoProDeviceState.stoppingStream,
                GoProDeviceState.idle,
            ),
            recorder.states,
        )
    }

    @Test
    fun aLensTheCameraDoesNotSupportFallsBackToAuto() {
        startAndPair(lens = SettingsGoProLens.superView)
        joinConfiguredWifi()
        configure(supportedLenses = listOf(OpenGopro_EnumLens.lensWide, OpenGopro_EnumLens.lensLinear))
        assertEquals(listOf(liveStreamMode(SettingsGoProLens.auto)), sent)
    }

    @Test
    fun keepAlivesGoToTheSettingsCharacteristicAndEveryTenthAlsoAsksForTheBatteryWhileStreaming() {
        startAndPair()
        joinConfiguredWifi()
        configure(supportedLenses = emptyList())
        liveStreamStatus(0xF5, OpenGopro_EnumLiveStreamStatus.liveStreamStateStreaming)
        sent.clear()
        advance(3)
        assertEquals(listOf(written(goProSettingsId, goProKeepAliveMessage())), sent)
        advance(24)
        assertEquals(9, sent.count { it == written(goProSettingsId, goProKeepAliveMessage()) })
        assertEquals(0, sent.count { it == written(goProQueryId, goProGetBatteryPercentageMessage()) })
        advance(3)
        assertEquals(
            listOf(written(goProSettingsId, goProKeepAliveMessage()), written(goProQueryId, goProGetBatteryPercentageMessage())),
            sent.takeLast(2),
        )
    }

    @Test
    fun pairingFallsBackToTheWifiScanAfterOneSecond() {
        val device = startAndPair()
        advance(1)
        assertEquals(GoProDeviceState.settingUpWifi, device.getState())
        assertEquals(listOf(written(goProNetworkManagementId, goProStartScanMessage())), sent)
    }

    @Test
    fun aNewNetworkIsJoinedWithThePasswordAndFailsWhenProvisioningTimesOut() {
        val device = startAndPair()
        startScanning()
        scanDone(totalEntries = 1)
        sent.clear()
        apEntries("Moblin" to false)
        assertEquals(
            listOf(written(goProNetworkManagementId, goProConnectToWifiMessage(ssid = "Moblin", password = "secret"))),
            sent,
        )
        connectResponse(0x85, OpenGopro_EnumProvisioning.provisioningStarted, 30)
        advance(34)
        assertEquals(GoProDeviceState.settingUpWifi, device.getState())
        advance(1)
        assertEquals(GoProDeviceState.wifiSetupFailed, device.getState())
        assertTrue(recording().isClosed)
    }

    @Test
    fun aProvisioningNotificationFinishesTheWifiSetup() {
        val device = startAndPair()
        startScanning()
        scanDone(totalEntries = 1)
        apEntries("Moblin" to false)
        connectResponse(0x85, OpenGopro_EnumProvisioning.provisioningStarted, 30)
        val notification = OpenGopro_NotifProvisioningState()
        notification.provisioningState = OpenGopro_EnumProvisioning.provisioningSuccessNewAp
        notify(goProNetworkManagementResponseId, bytes(0x02, 0x0C) + notification.serializedData())
        assertEquals(GoProDeviceState.configuring, device.getState())
        advance(40)
        assertEquals(GoProDeviceState.configuring, device.getState())
    }

    @Test
    fun accessPointsAreFetchedAHundredAtATime() {
        val device = startAndPair(wifiSsid = "Last")
        startScanning()
        scanDone(totalEntries = 150)
        assertEquals(
            listOf(written(goProNetworkManagementId, goProGetApEntriesMessage(scanId = 3, startIndex = 0, maximumEntries = 100))),
            sent,
        )
        sent.clear()
        apEntries(*Array(100) { "Other $it" to false })
        assertEquals(
            listOf(written(goProNetworkManagementId, goProGetApEntriesMessage(scanId = 3, startIndex = 100, maximumEntries = 50))),
            sent,
        )
        sent.clear()
        apEntries(*Array(49) { "More $it" to false }, "Last" to true)
        assertEquals(listOf(written(goProNetworkManagementId, goProConnectToProvisionedWifiMessage(ssid = "Last"))), sent)
        assertEquals(GoProDeviceState.settingUpWifi, device.getState())
    }

    @Test
    fun wifiSetupFailsWhenTheCameraDoesNotSeeTheNetwork() {
        val device = startAndPair(wifiSsid = "Missing")
        startScanning()
        scanDone(totalEntries = 2)
        apEntries("Other" to false, "Moblin" to true)
        assertEquals(GoProDeviceState.wifiSetupFailed, device.getState())
        assertTrue(recording().isClosed)
    }

    @Test
    fun aLostConnectionFailsTheStream() {
        val device = startAndPair()
        callback().onConnectionStateChange(gatt(), 8, BluetoothProfile.STATE_DISCONNECTED)
        runMain()
        assertEquals(GoProDeviceState.failed, device.getState())
        assertTrue(recording().isClosed)
    }

    @Test
    fun aRejectedWriteFailsTheStream() {
        val device = start()
        connect()
        callback().onMtuChanged(gatt(), 517, BluetoothGatt.GATT_SUCCESS)
        runMain()
        callback().onServicesDiscovered(gatt(), BluetoothGatt.GATT_SUCCESS)
        runMain()
        for (id in listOf(goProCommandResponseId, goProSettingsResponseId, goProQueryResponseId, goProNetworkManagementResponseId)) {
            callback().onDescriptorWrite(gatt(), descriptor(id), BluetoothGatt.GATT_SUCCESS)
            runMain()
        }
        assertEquals(GoProDeviceState.pairing, device.getState())
        callback().onCharacteristicWrite(
            gatt(),
            gattCharacteristic(goProNetworkManagementId),
            BluetoothGatt.GATT_WRITE_NOT_PERMITTED,
        )
        runMain()
        assertEquals(GoProDeviceState.failed, device.getState())
        assertTrue(recording().isClosed)
    }

    @Test
    fun turningBluetoothOffFailsTheStream() {
        val device = startAndPair()
        shadowOf(adapter()).setState(BluetoothAdapter.STATE_OFF)
        application.sendBroadcast(
            Intent(BluetoothAdapter.ACTION_STATE_CHANGED).putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF),
        )
        runMain()
        assertEquals(GoProDeviceState.failed, device.getState())
    }

    @Test
    fun theWifiScanTimesOutAfterThirtySeconds() {
        val device = startAndPair()
        startScanning()
        advance(29)
        assertEquals(GoProDeviceState.settingUpWifi, device.getState())
        advance(1)
        assertEquals(GoProDeviceState.wifiSetupFailed, device.getState())
    }

    @Test
    fun theStreamFailsWhenTheCameraIsNotStreamingNinetySecondsAfterTheStart() {
        val device = startAndPair()
        joinConfiguredWifi()
        configure(supportedLenses = emptyList())
        advance(89)
        assertEquals(GoProDeviceState.configuring, device.getState())
        advance(1)
        assertEquals(GoProDeviceState.failed, device.getState())
        assertTrue(recording().isClosed)
    }

    @Test
    fun aCameraThatNeverConnectsFailsAfterNinetySeconds() {
        val device = start()
        advance(89)
        assertEquals(GoProDeviceState.connecting, device.getState())
        advance(1)
        assertEquals(GoProDeviceState.failed, device.getState())
    }
}
