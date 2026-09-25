package com.moblin.android.integrations.gopro

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
import com.moblin.android.platform.corebluetooth.CBCentralManager
import com.moblin.android.platform.corebluetooth.CBCentralManagerDelegate
import com.moblin.android.platform.corebluetooth.CBCharacteristic
import com.moblin.android.platform.corebluetooth.CBCharacteristicWriteType
import com.moblin.android.platform.corebluetooth.CBManagerState
import com.moblin.android.platform.corebluetooth.CBPeripheral
import com.moblin.android.platform.corebluetooth.CBPeripheralDelegate
import com.moblin.android.platform.corebluetooth.CBService
import com.moblin.android.platform.corebluetooth.CBUUID
import com.moblin.android.various.MainTimer
import com.moblin.android.various.settings.SettingsGoProLaunchLiveStreamResolution
import com.moblin.android.various.settings.SettingsGoProLens
import java.util.UUID

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

interface GoProDeviceDelegate {
    fun goProDeviceStreamingState(device: GoProDevice, state: GoProDeviceState)
}

class GoProDevice : CBCentralManagerDelegate, CBPeripheralDelegate {
    var delegate: GoProDeviceDelegate? = null

    private var wifiSsid = ""
    private var wifiPassword = ""
    private var rtmpUrl = ""
    private var resolution: SettingsGoProLaunchLiveStreamResolution =
        SettingsGoProLaunchLiveStreamResolution.r1080p
    private var bitrate: UInt = 6_000_000u
    private var lens: SettingsGoProLens = SettingsGoProLens.auto
    private var deviceId: UUID? = null
    private var centralManager: CBCentralManager? by CBCentralManager.holder()
    private var devicePeripheral: CBPeripheral? = null
    private var characteristics: MutableMap<CBUUID, CBCharacteristic> = mutableMapOf()
    private var subscribedCharacteristics: MutableSet<CBUUID> = mutableSetOf()
    private var accumulators: MutableMap<CBUUID, GoProBleMessageAccumulator> = mutableMapOf()
    private var pendingWrites: MutableList<PendingWrite> = mutableListOf()
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
    private var supportedLenses: MutableList<OpenGopro_EnumLens>? = null

    private val operationTimeoutTimer = MainTimer()
    private val wifiTimeoutTimer = MainTimer()
    private val pairingFallbackTimer = MainTimer()
    private val statusPollTimer = MainTimer()
    private val startShutterTimer = MainTimer()
    private val stopTimer = MainTimer()
    private val keepAliveTimer = MainTimer()

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
        operationTimeoutTimer.startSingleShot(timeout = startLiveStreamTimeout) { fail() }
        centralManager = CBCentralManager(delegate = this, queue = null)
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
            send(goProSetShutterMessage(on = false), to = goProCommandId)
            stopTimer.startSingleShot(timeout = stopLiveStreamTimeout) { reset() }
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
        val peripheral = devicePeripheral
        if (peripheral != null) {
            centralManager?.cancelPeripheralConnection(peripheral)
        }
        centralManager = null
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
        delegate?.goProDeviceStreamingState(this, state = state)
    }

    private fun beginSetup() {
        if (didBeginSetup) {
            return
        }
        didBeginSetup = true
        keepAliveTimer.startPeriodic(interval = keepAliveInterval) { sendKeepAlive() }
        setState(GoProDeviceState.pairing)
        send(goProPairingCompleteMessage(), to = goProNetworkManagementId)
        pairingFallbackTimer.startSingleShot(timeout = pairingFallbackTimeout) { startWifiScan() }
    }

    private fun startWifiScan() {
        if (state != GoProDeviceState.pairing) {
            return
        }
        pairingFallbackTimer.stop()
        setState(GoProDeviceState.settingUpWifi)
        send(goProStartScanMessage(), to = goProNetworkManagementId)
        wifiTimeoutTimer.startSingleShot(timeout = wifiScanTimeout) {
            fail(state = GoProDeviceState.wifiSetupFailed)
        }
    }

    private fun requestNextApEntries() {
        val scanId = scanId
        if (scanId == null) {
            return
        }
        if (scanFetchedEntries >= scanTotalEntries) {
            connectToScannedWifi()
            return
        }
        send(
            goProGetApEntriesMessage(
                scanId = scanId,
                startIndex = scanFetchedEntries,
                maximumEntries = minOf(
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
            fail(state = GoProDeviceState.wifiSetupFailed)
            return
        }
        if (scanMatch.isUnsupportedType()) {
            fail(state = GoProDeviceState.wifiSetupFailed)
            return
        }
        if (scanMatch.isConfigured()) {
            send(
                goProConnectToProvisionedWifiMessage(ssid = wifiSsid),
                to = goProNetworkManagementId
            )
        } else {
            send(
                goProConnectToWifiMessage(ssid = wifiSsid, password = wifiPassword),
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
        send(goProSetShutterMessage(on = false), to = goProCommandId)
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
                url = rtmpUrl,
                resolution = resolution,
                bitrate = bitrate,
                lens = lens
            ),
            to = goProCommandId
        )
        statusPollTimer.startPeriodic(
            interval = statusPollInterval,
            initial = statusPollInterval
        ) {
            send(goProGetLiveStreamStatusMessage(), to = goProQueryId)
        }
    }

    private fun startShutterWhenReady() {
        if (state != GoProDeviceState.configuring || didScheduleShutterStart) {
            return
        }
        didScheduleShutterStart = true
        setState(GoProDeviceState.startingStream)
        startShutterTimer.startSingleShot(timeout = startShutterDelay) {
            send(goProSetShutterMessage(on = true), to = goProCommandId)
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

    private fun send(payload: ByteArray, to: CBUUID) {
        val characteristic = characteristics[to]
        if (characteristic == null) {
            return
        }
        for (packet in goProBlePackets(payload = payload)) {
            pendingWrites.add(PendingWrite(characteristic = characteristic, packet = packet))
        }
        writeNextPacketIfNeeded()
    }

    private fun writeNextPacketIfNeeded() {
        if (writeInProgress) {
            return
        }
        val next = pendingWrites.firstOrNull()
        if (next == null) {
            return
        }
        val devicePeripheral = devicePeripheral
        if (devicePeripheral == null) {
            return
        }
        writeInProgress = true
        devicePeripheral.writeValue(
            data = next.packet,
            `for` = next.characteristic,
            type = CBCharacteristicWriteType.withResponse
        )
    }

    private fun processMessage(message: ByteArray, from: CBUUID) {
        if (message.size < 2) {
            return
        }
        when (from) {
            goProNetworkManagementResponseId -> processNetworkMessage(message)
            goProCommandResponseId -> processCommandMessage(message)
            goProQueryResponseId -> processQueryMessage(message)
            else -> {}
        }
    }

    private fun processNetworkMessage(message: ByteArray) {
        val payload = message.copyOfRange(2, message.size)
        when (message[0].toUByte() to message[1].toUByte()) {
            goProPairingFeatureId to goProPairingFinishResponseId ->
                startWifiScan()
            goProNetworkFeatureId to goProStartScanResponseId ->
                processStartScanResponse(payload)
            goProNetworkFeatureId to goProScanningNotificationId ->
                processScanningNotification(payload)
            goProNetworkFeatureId to goProGetApEntriesResponseId ->
                processGetApEntriesResponse(payload)
            goProNetworkFeatureId to goProConnectResponseId,
            goProNetworkFeatureId to goProConnectNewResponseId ->
                processConnectResponse(payload)
            goProNetworkFeatureId to goProProvisioningNotificationId ->
                processProvisioningNotification(payload)
            else -> {}
        }
    }

    private fun processStartScanResponse(payload: ByteArray) {
        val response = runCatching {
            OpenGopro_ResponseStartScanning(serializedBytes = payload)
        }.getOrNull() ?: return
        if (response.result != OpenGopro_EnumResultGeneric.resultSuccess) {
            fail(state = GoProDeviceState.wifiSetupFailed)
        }
    }

    private fun processScanningNotification(payload: ByteArray) {
        val notification = runCatching {
            OpenGopro_NotifStartScanning(serializedBytes = payload)
        }.getOrNull() ?: return
        if (state != GoProDeviceState.settingUpWifi) {
            return
        }
        when (notification.scanningState) {
            OpenGopro_EnumScanning.scanningSuccess ->
                startFetchingApEntries(notification)
            OpenGopro_EnumScanning.scanningAbortedBySystem,
            OpenGopro_EnumScanning.scanningCancelledByUser ->
                fail(state = GoProDeviceState.wifiSetupFailed)
            else -> {}
        }
    }

    private fun startFetchingApEntries(notification: OpenGopro_NotifStartScanning) {
        if (scanId != null) {
            return
        }
        if (notification.totalEntries <= 0) {
            fail(state = GoProDeviceState.wifiSetupFailed)
            return
        }
        scanId = notification.scanID
        scanTotalEntries = notification.totalEntries
        scanFetchedEntries = 0
        requestNextApEntries()
    }

    private fun processGetApEntriesResponse(payload: ByteArray) {
        val response = runCatching {
            OpenGopro_ResponseGetApEntries(serializedBytes = payload)
        }.getOrNull() ?: return
        if (state != GoProDeviceState.settingUpWifi || scanId == null) {
            return
        }
        if (response.result != OpenGopro_EnumResultGeneric.resultSuccess) {
            fail(state = GoProDeviceState.wifiSetupFailed)
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
        val response = runCatching {
            OpenGopro_ResponseConnect(serializedBytes = payload)
        }.getOrNull() ?: return
        if (response.result != OpenGopro_EnumResultGeneric.resultSuccess) {
            fail(state = GoProDeviceState.wifiSetupFailed)
            return
        }
        val timeoutSeconds = if (response.hasTimeoutSeconds) {
            response.timeoutSeconds
        } else {
            wifiProvisioningDefaultTimeout
        }
        wifiTimeoutTimer.startSingleShot(
            timeout = timeoutSeconds.toDouble() + wifiProvisioningTimeoutMargin
        ) {
            fail(state = GoProDeviceState.wifiSetupFailed)
        }
        handleProvisioningState(response.provisioningState)
    }

    private fun processProvisioningNotification(payload: ByteArray) {
        val notification = runCatching {
            OpenGopro_NotifProvisioningState(serializedBytes = payload)
        }.getOrNull() ?: return
        handleProvisioningState(notification.provisioningState)
    }

    private fun handleProvisioningState(provisioningState: OpenGopro_EnumProvisioning) {
        when (provisioningState) {
            OpenGopro_EnumProvisioning.provisioningSuccessNewAp,
            OpenGopro_EnumProvisioning.provisioningSuccessOldAp ->
                configureLiveStream()
            OpenGopro_EnumProvisioning.provisioningAbortedBySystem,
            OpenGopro_EnumProvisioning.provisioningCancelledByUser,
            OpenGopro_EnumProvisioning.provisioningErrorFailedToAssociate,
            OpenGopro_EnumProvisioning.provisioningErrorPasswordAuth,
            OpenGopro_EnumProvisioning.provisioningErrorEulaBlocking,
            OpenGopro_EnumProvisioning.provisioningErrorNoInternet,
            OpenGopro_EnumProvisioning.provisioningErrorUnsupportedType ->
                fail(state = GoProDeviceState.wifiSetupFailed)
            else -> {}
        }
    }

    private fun processCommandMessage(message: ByteArray) {
        val featureId = message[0].toUByte()
        val commandId = message[1].toUByte()
        when (featureId to commandId) {
            goProLiveStreamCommandFeatureId to goProSetLiveStreamModeResponseId ->
                processSetLiveStreamModeResponse(message.copyOfRange(2, message.size))
            goProShutterCommandId to commandId ->
                processShutterResponse(commandId)
            else -> {}
        }
    }

    private fun processSetLiveStreamModeResponse(payload: ByteArray) {
        val response = runCatching {
            OpenGopro_ResponseGeneric(serializedBytes = payload)
        }.getOrNull() ?: return
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
        when (message[0].toUByte() to message[1].toUByte()) {
            goProLiveStreamQueryFeatureId to goProGetLiveStreamStatusResponseId ->
                processLiveStreamStatus(message.copyOfRange(2, message.size), isResponse = true)
            goProLiveStreamQueryFeatureId to goProLiveStreamStatusNotificationId ->
                processLiveStreamStatus(message.copyOfRange(2, message.size), isResponse = false)
            goProGetStatusQueryId to goProResponseSuccessStatus ->
                processStatusResponse(message.copyOfRange(2, message.size))
            else -> {}
        }
    }

    private fun processLiveStreamStatus(payload: ByteArray, isResponse: Boolean) {
        val status = runCatching {
            OpenGopro_NotifyLiveStreamStatus(serializedBytes = payload)
        }.getOrNull() ?: return
        if (isResponse && supportedLenses == null) {
            supportedLenses = if (status.liveStreamLensSupported) {
                status.liveStreamLensSupportedArray
            } else {
                mutableListOf()
            }
        }
        handleLiveStreamStatus(status.liveStreamStatus)
    }

    private fun processStatusResponse(payload: ByteArray) {
        if (payload.size < 3 ||
            payload[0].toUByte() != goProBatteryPercentageStatusId ||
            payload[1].toUByte() < 1.toUByte()
        ) {
            return
        }
        batteryPercentage = payload[2].toInt() and 0xFF
    }

    private fun handleLiveStreamStatus(liveStreamStatus: OpenGopro_EnumLiveStreamStatus) {
        when (liveStreamStatus) {
            OpenGopro_EnumLiveStreamStatus.liveStreamStateReady ->
                startShutterWhenReady()
            OpenGopro_EnumLiveStreamStatus.liveStreamStateStreaming,
            OpenGopro_EnumLiveStreamStatus.liveStreamStateReconnecting ->
                handleStreaming()
            OpenGopro_EnumLiveStreamStatus.liveStreamStateIdle,
            OpenGopro_EnumLiveStreamStatus.liveStreamStateCompleteStayOn ->
                handleNotStreaming()
            OpenGopro_EnumLiveStreamStatus.liveStreamStateFailedStayOn,
            OpenGopro_EnumLiveStreamStatus.liveStreamStateUnavailable ->
                fail()
            else -> {}
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

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        if (central.state != CBManagerState.poweredOn) {
            if (central.state != CBManagerState.unknown && central.state != CBManagerState.resetting) {
                fail()
            }
            return
        }
        val deviceId = deviceId
        if (deviceId == null) {
            fail()
            return
        }
        val peripheral = central.retrievePeripherals(withIdentifiers = listOf(deviceId)).firstOrNull()
        if (peripheral == null) {
            fail()
            return
        }
        devicePeripheral = peripheral
        peripheral.delegate = this
        setState(GoProDeviceState.connecting)
        central.connect(peripheral)
    }

    override fun centralManagerDidFailToConnect(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        error: Throwable?
    ) {
        fail()
    }

    override fun centralManagerDidConnect(central: CBCentralManager, peripheral: CBPeripheral) {
        peripheral.discoverServices(listOf(goProControlServiceId, goProCameraManagementServiceId))
    }

    override fun centralManagerDidDisconnectPeripheral(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        error: Throwable?
    ) {
        if (state != GoProDeviceState.idle &&
            state != GoProDeviceState.failed &&
            state != GoProDeviceState.wifiSetupFailed
        ) {
            fail()
        }
    }

    override fun peripheralDidDiscoverServices(peripheral: CBPeripheral, error: Throwable?) {
        if (error != null) {
            fail()
            return
        }
        for (service in peripheral.services.orEmpty()) {
            peripheral.discoverCharacteristics(null, `for` = service)
        }
    }

    override fun peripheralDidDiscoverCharacteristicsFor(
        peripheral: CBPeripheral,
        service: CBService,
        error: Throwable?
    ) {
        if (error != null) {
            fail()
            return
        }
        val notifyIds: Set<CBUUID> = setOf(
            goProCommandResponseId,
            goProSettingsResponseId,
            goProQueryResponseId,
            goProNetworkManagementResponseId,
        )
        for (characteristic in service.characteristics.orEmpty()) {
            characteristics[characteristic.uuid] = characteristic
            if (notifyIds.contains(characteristic.uuid)) {
                accumulators[characteristic.uuid] = GoProBleMessageAccumulator()
                peripheral.setNotifyValue(true, `for` = characteristic)
            }
        }
    }

    override fun peripheralDidUpdateNotificationStateFor(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: Throwable?
    ) {
        if (error != null || !characteristic.isNotifying) {
            fail()
            return
        }
        subscribedCharacteristics.add(characteristic.uuid)
        val required: Set<CBUUID> = setOf(
            goProCommandResponseId,
            goProQueryResponseId,
            goProNetworkManagementResponseId,
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

    override fun peripheralDidUpdateValueFor(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: Throwable?
    ) {
        val packet = characteristic.value
        if (error != null || packet == null) {
            return
        }
        val message = accumulators[characteristic.uuid]?.append(packet = packet) ?: return
        processMessage(message, from = characteristic.uuid)
    }

    override fun peripheralDidWriteValueFor(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: Throwable?
    ) {
        writeInProgress = false
        if (error != null) {
            fail()
            return
        }
        if (pendingWrites.isNotEmpty()) {
            pendingWrites.removeAt(0)
        }
        writeNextPacketIfNeeded()
    }

    private data class PendingWrite(
        val characteristic: CBCharacteristic,
        val packet: ByteArray
    )
}
