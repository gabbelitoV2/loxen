package com.moblin.android.integrations.gopro

import android.annotation.SuppressLint
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
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumLens
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumLiveStreamStatus
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumProvisioning
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumResultGeneric
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumScanning
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_NotifProvisioningState
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_NotifStartScanning
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_NotifyLiveStreamStatus
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_ResponseConnect
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_ResponseGeneric
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_ResponseGetApEntries
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_ResponseStartScanning
import com.moblin.android.various.MainTimer
import com.moblin.android.various.settings.SettingsGoProLaunchLiveStreamResolution
import com.moblin.android.various.settings.SettingsGoProLens
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.moblin.android.platform.corebluetooth.identifier

enum class GoProDeviceState {
    idle,
    discovering,
    connecting,
    pairing,
    settingUpWifi,
    wifiSetupFailed,
    configuring,
    startingStream,
    streaming,
    stoppingStream,
    failed,
}

private const val startLiveStreamTimeout = 90.0
private const val stopLiveStreamTimeout = 8.0
private const val keepAliveInterval = 3.0
private const val pairingFallbackTimeout = 1.0
private const val wifiScanTimeout = 30.0
private const val wifiProvisioningDefaultTimeout = 20
private const val wifiProvisioningTimeoutMargin = 5.0
private const val statusPollInterval = 1.0
private const val startShutterDelay = 2.0
private const val batteryPollKeepAlives = 10
private val goProNotificationDescriptorId: UUID =
    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

interface GoProDeviceDelegate {
    fun goProDeviceStreamingState(device: GoProDevice, state: GoProDeviceState)
}

@SuppressLint("MissingPermission")
class GoProDevice(private val context: Context) {
    var delegate: GoProDeviceDelegate? = null

    private var wifiSsid = ""
    private var wifiPassword = ""
    private var rtmpUrl = ""
    private var resolution: SettingsGoProLaunchLiveStreamResolution =
        SettingsGoProLaunchLiveStreamResolution.r1080p
    private var bitrate: UInt = 6_000_000u
    private var lens: SettingsGoProLens = SettingsGoProLens.auto
    private var deviceId: UUID? = null
    private var centralManager: com.moblin.android.platform.corebluetooth.CBCentralManager? = null
    private var devicePeripheral: BluetoothDevice? = null
    private var gatt: BluetoothGatt? = null
    private var characteristics: MutableMap<UUID, BluetoothGattCharacteristic> = mutableMapOf()
    private var subscribedCharacteristics: MutableSet<UUID> = mutableSetOf()
    private var accumulators: MutableMap<UUID, GoProBleMessageAccumulator> = mutableMapOf()
    private var pendingWrites: MutableList<Pair<BluetoothGattCharacteristic, ByteArray>> =
        mutableListOf()
    private var writeInProgress = false
    private var state: GoProDeviceState = GoProDeviceState.idle
    private var didBeginSetup = false
    private var didScheduleShutterStart = false
    private var waitingForShutterOffBeforeConfigure = false
    private var keepAliveCount = 0
    private var batteryPercentage: Int? = null
    private var scanId: Int? = null
    private var scanTotalEntries = 0
    private var scanFetchedEntries = 0
    private var scanMatch: OpenGopro_ResponseGetApEntries.ScanEntry? = null
    private var supportedLenses: List<OpenGopro_EnumLens>? = null

    private val operationTimeoutTimer = MainTimer()
    private val wifiTimeoutTimer = MainTimer()
    private val pairingFallbackTimer = MainTimer()
    private val statusPollTimer = MainTimer()
    private val startShutterTimer = MainTimer()
    private val stopTimer = MainTimer()
    private val keepAliveTimer = MainTimer()

    private val mainScope = CoroutineScope(Dispatchers.Main)
    private var scanCallback: ScanCallback? = null
    private var adapterStateReceiverRegistered = false

    private val adapterStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            centralManagerDidUpdateState()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            mainScope.launch {
                when {
                    newState == BluetoothProfile.STATE_CONNECTED &&
                        status == BluetoothGatt.GATT_SUCCESS ->
                        centralManagerDidConnect(gatt)

                    newState == BluetoothProfile.STATE_DISCONNECTED ->
                        centralManagerDidDisconnectPeripheral(status)

                    else -> centralManagerDidFailToConnect()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            mainScope.launch {
                peripheralDidDiscoverServices(status)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            mainScope.launch {
                peripheralDidUpdateNotificationState(descriptor.characteristic, status)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            mainScope.launch {
                peripheralDidUpdateValue(characteristic)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            mainScope.launch {
                peripheralDidWriteValue(status)
            }
        }
    }

    fun startLiveStream(
        wifiSsid: String,
        wifiPassword: String,
        rtmpUrl: String,
        resolution: SettingsGoProLaunchLiveStreamResolution,
        bitrate: UInt,
        lens: SettingsGoProLens,
        deviceId: UUID
    ) {
        this.wifiSsid = wifiSsid
        this.wifiPassword = wifiPassword
        this.rtmpUrl = rtmpUrl
        this.resolution = resolution
        this.bitrate = bitrate
        this.lens = lens
        this.deviceId = deviceId
        resetConnection()
        setState(GoProDeviceState.discovering)
        operationTimeoutTimer.startSingleShot(startLiveStreamTimeout) {
            fail()
        }
        centralManager =
            com.moblin.android.platform.corebluetooth.CBCentralManager(delegate = { centralManagerDidUpdateState() }, queue = null)
        Unit
    }

    fun stopLiveStream() {
        if (state == GoProDeviceState.idle) {
            return
        }
        operationTimeoutTimer.stop()
        statusPollTimer.stop()
        startShutterTimer.stop()
        val wasStreaming =
            state == GoProDeviceState.startingStream || state == GoProDeviceState.streaming
        setState(GoProDeviceState.stoppingStream)
        if (wasStreaming && characteristics[goProCommandId] != null) {
            send(goProSetShutterMessage(false), to = goProCommandId)
            stopTimer.startSingleShot(stopLiveStreamTimeout) {
                reset()
            }
        } else {
            reset()
        }
    }

    fun getState(): GoProDeviceState {
        return state
    }

    fun getBatteryPercentage(): Int? {
        return batteryPercentage
    }

    private fun resetConnection() {
        operationTimeoutTimer.stop()
        wifiTimeoutTimer.stop()
        pairingFallbackTimer.stop()
        statusPollTimer.stop()
        startShutterTimer.stop()
        stopTimer.stop()
        keepAliveTimer.stop()
        scanCallback?.let { callback ->
            runCatching {
                centralManager?.stopScan()
            }
        }
        scanCallback = null
        unregisterAdapterStateReceiver()
        if (devicePeripheral != null) {
            gatt?.let { g ->
                centralManager?.cancelPeripheralConnection(g)
            }
        }
        gatt = null
        centralManager?.delegate = null; centralManager = null
        devicePeripheral = null
        characteristics.clear()
        subscribedCharacteristics.clear()
        accumulators.clear()
        pendingWrites.clear()
        writeInProgress = false
        didBeginSetup = false
        didScheduleShutterStart = false
        waitingForShutterOffBeforeConfigure = false
        keepAliveCount = 0
        batteryPercentage = null
        scanId = null
        scanTotalEntries = 0
        scanFetchedEntries = 0
        scanMatch = null
        supportedLenses = null
    }

    private fun reset() {
        resetConnection()
        setState(GoProDeviceState.idle)
    }

    private fun fail(state: GoProDeviceState = GoProDeviceState.failed) {
        resetConnection()
        setState(state)
    }

    private fun setState(state: GoProDeviceState) {
        if (this.state == state) {
            return
        }
        this.state = state
        delegate?.goProDeviceStreamingState(this, state)
    }

    private fun beginSetup() {
        if (didBeginSetup) {
            return
        }
        didBeginSetup = true
        keepAliveTimer.startPeriodic(keepAliveInterval) {
            sendKeepAlive()
        }
        setState(GoProDeviceState.pairing)
        send(goProPairingCompleteMessage(), to = goProNetworkManagementId)
        pairingFallbackTimer.startSingleShot(pairingFallbackTimeout) {
            startWifiScan()
        }
    }

    private fun startWifiScan() {
        if (state != GoProDeviceState.pairing) {
            return
        }
        pairingFallbackTimer.stop()
        setState(GoProDeviceState.settingUpWifi)
        send(goProStartScanMessage(), to = goProNetworkManagementId)
        wifiTimeoutTimer.startSingleShot(wifiScanTimeout) {
            fail(GoProDeviceState.wifiSetupFailed)
        }
    }

    private fun requestNextApEntries() {
        val scanId = scanId ?: return
        if (scanFetchedEntries >= scanTotalEntries) {
            connectToScannedWifi()
            return
        }
        send(
            goProGetApEntriesMessage(
                scanId,
                scanFetchedEntries,
                minOf(
                    scanTotalEntries - scanFetchedEntries,
                    goProMaximumApEntriesPerRequest
                )
            ),
            to = goProNetworkManagementId
        )
    }

    private fun connectToScannedWifi() {
        val scanMatch = scanMatch
        if (scanMatch == null) {
            fail(GoProDeviceState.wifiSetupFailed)
            return
        }
        if (scanMatch.isUnsupportedType()) {
            fail(GoProDeviceState.wifiSetupFailed)
            return
        }
        if (scanMatch.isConfigured()) {
            send(
                goProConnectToProvisionedWifiMessage(wifiSsid),
                to = goProNetworkManagementId
            )
        } else {
            send(
                goProConnectToWifiMessage(wifiSsid, wifiPassword),
                to = goProNetworkManagementId
            )
        }
    }

    private fun configureLiveStream() {
        if (state != GoProDeviceState.settingUpWifi) {
            return
        }
        wifiTimeoutTimer.stop()
        setState(GoProDeviceState.configuring)
        send(goProRegisterLiveStreamStatusMessage(), to = goProQueryId)
        waitingForShutterOffBeforeConfigure = true
        send(goProSetShutterMessage(false), to = goProCommandId)
    }

    private fun sendLiveStreamConfiguration() {
        var lens = lens
        val lensValue = lens.toProtobuf()
        val supportedLenses = supportedLenses
        if (lensValue != null && supportedLenses != null && !supportedLenses.contains(lensValue)) {
            lens = SettingsGoProLens.auto
        }
        send(
            goProSetLiveStreamModeMessage(
                rtmpUrl,
                resolution,
                bitrate,
                lens
            ),
            to = goProCommandId
        )
        statusPollTimer.startPeriodic(statusPollInterval, statusPollInterval) {
            send(goProGetLiveStreamStatusMessage(), to = goProQueryId)
        }
    }

    private fun startShutterWhenReady() {
        if (state != GoProDeviceState.configuring || didScheduleShutterStart) {
            return
        }
        didScheduleShutterStart = true
        setState(GoProDeviceState.startingStream)
        startShutterTimer.startSingleShot(startShutterDelay) {
            send(goProSetShutterMessage(true), to = goProCommandId)
        }
    }

    private fun sendKeepAlive() {
        if (characteristics[goProSettingsId] == null) {
            return
        }
        send(goProKeepAliveMessage(), to = goProSettingsId)
        keepAliveCount += 1
        if (state == GoProDeviceState.streaming && keepAliveCount % batteryPollKeepAlives == 0) {
            send(goProGetBatteryPercentageMessage(), to = goProQueryId)
        }
    }

    private fun send(payload: ByteArray, to: UUID) {
        val characteristic = characteristics[to] ?: return
        for (packet in goProBlePackets(payload)) {
            pendingWrites.add(Pair(characteristic, packet))
        }
        writeNextPacketIfNeeded()
    }

    private fun writeNextPacketIfNeeded() {
        if (writeInProgress) {
            return
        }
        val next = pendingWrites.firstOrNull() ?: return
        val gatt = this.gatt ?: return
        writeInProgress = true
        next.first.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        next.first.value = next.second
        com.moblin.android.platform.corebluetooth.bluetoothCall { gatt.writeCharacteristic(next.first) }
    }

    private fun processMessage(message: ByteArray, characteristic: UUID) {
        if (message.size < 2) {
            return
        }
        when (characteristic) {
            goProNetworkManagementResponseId -> processNetworkMessage(message)
            goProCommandResponseId -> processCommandMessage(message)
            goProQueryResponseId -> processQueryMessage(message)
            else -> Unit
        }
    }

    private fun processNetworkMessage(message: ByteArray) {
        val payload = message.copyOfRange(2, message.size)
        when (message[0].toUByte() to message[1].toUByte()) {
            goProPairingFeatureId to goProPairingFinishResponseId -> startWifiScan()
            goProNetworkFeatureId to goProStartScanResponseId -> processStartScanResponse(payload)
            goProNetworkFeatureId to goProScanningNotificationId ->
                processScanningNotification(payload)

            goProNetworkFeatureId to goProGetApEntriesResponseId ->
                processGetApEntriesResponse(payload)

            goProNetworkFeatureId to goProConnectResponseId,
            goProNetworkFeatureId to goProConnectNewResponseId ->
                processConnectResponse(payload)

            goProNetworkFeatureId to goProProvisioningNotificationId ->
                processProvisioningNotification(payload)

            else -> Unit
        }
    }

    private fun processStartScanResponse(payload: ByteArray) {
        val response = runCatching { OpenGopro_ResponseStartScanning(serializedBytes = payload) }.getOrNull()
            ?: return
        if (response.result != OpenGopro_EnumResultGeneric.resultSuccess) {
            fail(GoProDeviceState.wifiSetupFailed)
        }
    }

    private fun processScanningNotification(payload: ByteArray) {
        val notification = runCatching { OpenGopro_NotifStartScanning(serializedBytes = payload) }.getOrNull()
            ?: return
        if (state != GoProDeviceState.settingUpWifi) {
            return
        }
        when (notification.scanningState) {
            OpenGopro_EnumScanning.scanningSuccess -> startFetchingApEntries(notification)
            OpenGopro_EnumScanning.scanningAbortedBySystem,
            OpenGopro_EnumScanning.scanningCancelledByUser ->
                fail(GoProDeviceState.wifiSetupFailed)

            else -> Unit
        }
    }

    private fun startFetchingApEntries(notification: OpenGopro_NotifStartScanning) {
        if (scanId != null) {
            return
        }
        if (notification.totalEntries <= 0) {
            fail(GoProDeviceState.wifiSetupFailed)
            return
        }
        scanId = notification.scanID
        scanTotalEntries = notification.totalEntries
        scanFetchedEntries = 0
        requestNextApEntries()
    }

    private fun processGetApEntriesResponse(payload: ByteArray) {
        val response = runCatching { OpenGopro_ResponseGetApEntries(serializedBytes = payload) }.getOrNull()
            ?: return
        if (state != GoProDeviceState.settingUpWifi || scanId == null) {
            return
        }
        if (response.result != OpenGopro_EnumResultGeneric.resultSuccess) {
            fail(GoProDeviceState.wifiSetupFailed)
            return
        }
        if (scanMatch == null) {
            scanMatch = response.entries.firstOrNull { it.ssid == wifiSsid }
        }
        scanFetchedEntries += response.entries.size
        if (scanMatch != null || response.entries.isEmpty()) {
            connectToScannedWifi()
        } else {
            requestNextApEntries()
        }
    }

    private fun processConnectResponse(payload: ByteArray) {
        val response = runCatching { OpenGopro_ResponseConnect(serializedBytes = payload) }.getOrNull()
            ?: return
        if (response.result != OpenGopro_EnumResultGeneric.resultSuccess) {
            fail(GoProDeviceState.wifiSetupFailed)
            return
        }
        val timeoutSeconds =
            if (response.hasTimeoutSeconds) response.timeoutSeconds
            else wifiProvisioningDefaultTimeout
        wifiTimeoutTimer.startSingleShot(
            timeoutSeconds.toDouble() + wifiProvisioningTimeoutMargin
        ) {
            fail(GoProDeviceState.wifiSetupFailed)
        }
        handleProvisioningState(response.provisioningState)
    }

    private fun processProvisioningNotification(payload: ByteArray) {
        val notification = runCatching { OpenGopro_NotifProvisioningState(serializedBytes = payload) }.getOrNull()
            ?: return
        handleProvisioningState(notification.provisioningState)
    }

    private fun handleProvisioningState(provisioningState: OpenGopro_EnumProvisioning) {
        when (provisioningState) {
            OpenGopro_EnumProvisioning.provisioningSuccessNewAp,
            OpenGopro_EnumProvisioning.provisioningSuccessOldAp -> configureLiveStream()

            OpenGopro_EnumProvisioning.provisioningAbortedBySystem,
            OpenGopro_EnumProvisioning.provisioningCancelledByUser,
            OpenGopro_EnumProvisioning.provisioningErrorFailedToAssociate,
            OpenGopro_EnumProvisioning.provisioningErrorPasswordAuth,
            OpenGopro_EnumProvisioning.provisioningErrorEulaBlocking,
            OpenGopro_EnumProvisioning.provisioningErrorNoInternet,
            OpenGopro_EnumProvisioning.provisioningErrorUnsupportedType ->
                fail(GoProDeviceState.wifiSetupFailed)

            else -> Unit
        }
    }

    private fun processCommandMessage(message: ByteArray) {
        val featureId = message[0].toUByte()
        val actionId = message[1].toUByte()
        when {
            featureId == goProLiveStreamCommandFeatureId &&
                actionId == goProSetLiveStreamModeResponseId ->
                processSetLiveStreamModeResponse(message.copyOfRange(2, message.size))

            featureId == goProShutterCommandId -> processShutterResponse(actionId)
            else -> Unit
        }
    }

    private fun processSetLiveStreamModeResponse(payload: ByteArray) {
        val response = runCatching { OpenGopro_ResponseGeneric(serializedBytes = payload) }.getOrNull()
            ?: return
        if (response.result != OpenGopro_EnumResultGeneric.resultSuccess) {
            fail()
        }
    }

    private fun processShutterResponse(status: UByte) {
        if (state == GoProDeviceState.configuring && waitingForShutterOffBeforeConfigure) {
            waitingForShutterOffBeforeConfigure = false
            sendLiveStreamConfiguration()
        } else if (state == GoProDeviceState.stoppingStream) {
            reset()
        } else if (status != goProResponseSuccessStatus) {
            fail()
        }
    }

    private fun processQueryMessage(message: ByteArray) {
        val payload = message.copyOfRange(2, message.size)
        when (message[0].toUByte() to message[1].toUByte()) {
            goProLiveStreamQueryFeatureId to goProGetLiveStreamStatusResponseId ->
                processLiveStreamStatus(payload, true)

            goProLiveStreamQueryFeatureId to goProLiveStreamStatusNotificationId ->
                processLiveStreamStatus(payload, false)

            goProGetStatusQueryId to goProResponseSuccessStatus ->
                processStatusResponse(payload)

            else -> Unit
        }
    }

    private fun processLiveStreamStatus(payload: ByteArray, isResponse: Boolean) {
        val status = runCatching { OpenGopro_NotifyLiveStreamStatus(serializedBytes = payload) }.getOrNull()
            ?: return
        if (isResponse && supportedLenses == null) {
            supportedLenses =
                if (status.liveStreamLensSupported) status.liveStreamLensSupportedArray
                else emptyList()
        }
        handleLiveStreamStatus(status.liveStreamStatus)
    }

    private fun processStatusResponse(payload: ByteArray) {
        if (payload.size < 3 ||
            payload[0].toUByte() != goProBatteryPercentageStatusId ||
            payload[1].toUByte() < 1u
        ) {
            return
        }
        batteryPercentage = payload[2].toUByte().toInt()
    }

    private fun handleLiveStreamStatus(liveStreamStatus: OpenGopro_EnumLiveStreamStatus) {
        when (liveStreamStatus) {
            OpenGopro_EnumLiveStreamStatus.liveStreamStateReady -> startShutterWhenReady()
            OpenGopro_EnumLiveStreamStatus.liveStreamStateStreaming,
            OpenGopro_EnumLiveStreamStatus.liveStreamStateReconnecting -> handleStreaming()

            OpenGopro_EnumLiveStreamStatus.liveStreamStateIdle,
            OpenGopro_EnumLiveStreamStatus.liveStreamStateCompleteStayOn -> handleNotStreaming()

            OpenGopro_EnumLiveStreamStatus.liveStreamStateFailedStayOn,
            OpenGopro_EnumLiveStreamStatus.liveStreamStateUnavailable -> fail()

            else -> Unit
        }
    }

    private fun handleStreaming() {
        setState(GoProDeviceState.streaming)
        operationTimeoutTimer.stop()
        statusPollTimer.stop()
        send(goProGetBatteryPercentageMessage(), to = goProQueryId)
    }

    private fun handleNotStreaming() {
        if (state == GoProDeviceState.stoppingStream) {
            reset()
        }
    }

    private fun registerAdapterStateReceiver() {
        if (adapterStateReceiverRegistered) {
            return
        }
        context.registerReceiver(
            adapterStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        adapterStateReceiverRegistered = true
    }

    private fun unregisterAdapterStateReceiver() {
        if (!adapterStateReceiverRegistered) {
            return
        }
        runCatching {
            context.unregisterReceiver(adapterStateReceiver)
        }
        adapterStateReceiverRegistered = false
    }

    fun centralManagerDidUpdateState() {
        val central = centralManager ?: return
        if (central.state == com.moblin.android.platform.corebluetooth.CBManagerState.unknown || central.state == com.moblin.android.platform.corebluetooth.CBManagerState.resetting) { return }; if (central.state != com.moblin.android.platform.corebluetooth.CBManagerState.poweredOn) {
            fail()
            return
        }
        if (deviceId == null) {
            fail()
            return
        }
        scanForDevice()
    }

    fun centralManagerDidFailToConnect() {
        fail()
    }

    fun centralManagerDidConnect(peripheral: BluetoothGatt) {
        com.moblin.android.platform.corebluetooth.bluetoothCall { peripheral.discoverServices() }
    }

    fun centralManagerDidDisconnectPeripheral(status: Int) {
        if (state != GoProDeviceState.idle &&
            state != GoProDeviceState.failed &&
            state != GoProDeviceState.wifiSetupFailed
        ) {
            fail()
        }
    }

    private fun scanForDevice() {
        if (scanCallback != null) {
            return
        }
        val scanner = centralManager
        if (scanner == null) {
            fail()
            return
        }
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (result.device.identifier != deviceId) return
                mainScope.launch {
                    runCatching {
                        this@GoProDevice.scanCallback?.let { scanner.stopScan() }
                    }
                    this@GoProDevice.scanCallback = null
                    connectToPeripheral(result.device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                mainScope.launch {
                    fail()
                }
            }
        }
        scanCallback = callback
        runCatching {
            scanner.scanForPeripherals(withServices = null, callback = callback)
        }.onFailure {
            scanCallback = null
            fail()
        }
    }

    private fun connectToPeripheral(peripheral: BluetoothDevice) {
        devicePeripheral = peripheral
        setState(GoProDeviceState.connecting)
        gatt = centralManager?.connect(peripheral, callback = gattCallback)
    }

    fun peripheralDidDiscoverServices(status: Int) {
        val gatt = this.gatt ?: return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            fail()
            return
        }
        for (service in gatt.services ?: emptyList()) {
            peripheralDidDiscoverCharacteristics(service, status)
        }
    }

    fun peripheralDidDiscoverCharacteristics(service: BluetoothGattService, status: Int) {
        val gatt = this.gatt ?: return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            fail()
            return
        }
        val notifyIds: Set<UUID> = setOf(
            goProCommandResponseId,
            goProSettingsResponseId,
            goProQueryResponseId,
            goProNetworkManagementResponseId
        )
        for (characteristic in service.characteristics ?: emptyList()) {
            characteristics[characteristic.uuid] = characteristic
            if (notifyIds.contains(characteristic.uuid)) {
                accumulators[characteristic.uuid] = GoProBleMessageAccumulator()
                com.moblin.android.platform.corebluetooth.bluetoothCall { gatt.setCharacteristicNotification(characteristic, true) }
                val descriptor = characteristic.getDescriptor(goProNotificationDescriptorId)
                if (descriptor != null) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    com.moblin.android.platform.corebluetooth.bluetoothCall { gatt.writeDescriptor(descriptor) }
                }
            }
        }
    }

    fun peripheralDidUpdateNotificationState(
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            fail()
            return
        }
        subscribedCharacteristics.add(characteristic.uuid)
        val required: Set<UUID> = setOf(
            goProCommandResponseId,
            goProQueryResponseId,
            goProNetworkManagementResponseId
        )
        if (subscribedCharacteristics.containsAll(required) &&
            characteristics[goProCommandId] != null &&
            characteristics[goProSettingsId] != null &&
            characteristics[goProQueryId] != null &&
            characteristics[goProNetworkManagementId] != null
        ) {
            beginSetup()
        }
    }

    fun peripheralDidUpdateValue(characteristic: BluetoothGattCharacteristic) {
        val packet = characteristic.value ?: return
        val message = accumulators[characteristic.uuid]?.append(packet) ?: return
        processMessage(message, characteristic.uuid)
    }

    fun peripheralDidWriteValue(status: Int) {
        writeInProgress = false
        if (status != BluetoothGatt.GATT_SUCCESS) {
            fail()
            return
        }
        if (pendingWrites.isNotEmpty()) {
            pendingWrites.removeAt(0)
        }
        writeNextPacketIfNeeded()
    }
}
