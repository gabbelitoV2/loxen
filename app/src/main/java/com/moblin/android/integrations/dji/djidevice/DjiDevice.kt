package com.moblin.android.integrations.dji.djidevice

import com.moblin.android.platform.log.Log
import com.moblin.android.common.various.hexString
import com.moblin.android.integrations.dji.DjiMessage
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
import com.moblin.android.various.settings.SettingsDjiDevice
import com.moblin.android.various.settings.SettingsDjiDeviceImageStabilization
import com.moblin.android.various.settings.SettingsDjiDeviceModel
import com.moblin.android.various.settings.SettingsDjiDeviceResolution
import com.moblin.android.various.settings.SettingsDjiDeviceUrlType
import com.moblin.android.various.settings.SettingsDjiDeviceVideoCodec
import java.util.UUID

private val pairTransactionId: UShort = 0x8092u.toUShort()
private val stopStreamingTransactionId: UShort = 0xEAC8u.toUShort()
private val preparingToLivestreamTransactionId: UShort = 0x8C12u.toUShort()
private val setupWifiTransactionId: UShort = 0x8C19u.toUShort()
private val startStreamingTransactionId: UShort = 0x8C2Cu.toUShort()
private val configureTransactionId: UShort = 0x8C2Du.toUShort()

private val pairTarget: UShort = 0x0702u.toUShort()
private val stopStreamingTarget: UShort = 0x0802u.toUShort()
private val preparingToLivestreamTarget: UShort = 0x0802u.toUShort()
private val setupWifiTarget: UShort = 0x0702u.toUShort()
private val configureTarget: UShort = 0x0102u.toUShort()
private val startStreamingTarget: UShort = 0x0802u.toUShort()

private val pairType: UInt = 0x450740u
private val stopStreamingType: UInt = 0x8E0240u
private val preparingToLivestreamType: UInt = 0xE10240u
private val setupWifiType: UInt = 0x470740u
private val configureType: UInt = 0x8E0240u
private val startStreamingType: UInt = 0x780840u
private val statusType: UInt = 0x020D00u

private val fff4Id = CBUUID(string = "FFF4")
private val fff5Id = CBUUID(string = "FFF5")

private val pairPinCode = "mbln"

enum class DjiDeviceState {
    idle,
    discovering,
    connecting,
    checkingIfPaired,
    pairing,
    cleaningUp,
    preparingStream,
    settingUpWifi,
    wifiSetupFailed,
    configuring,
    startingStream,
    streaming,
    stoppingStream,
}

interface DjiDeviceDelegate {
    fun djiDeviceStreamingState(device: DjiDevice, state: DjiDeviceState)
}

open class DjiDevice : CBCentralManagerDelegate, CBPeripheralDelegate {
    private var wifiSsid: String? = null
    private var wifiPassword: String? = null
    private var rtmpUrl: String? = null
    private var resolution: SettingsDjiDeviceResolution? = null
    private var fps: Int = 30
    private var bitrate: UInt = 6_000_000u
    private var videoCodec: SettingsDjiDeviceVideoCodec = SettingsDjiDeviceVideoCodec.h265hevc
    private var imageStabilization: SettingsDjiDeviceImageStabilization? = null
    private var deviceId: UUID? = null
    private var centralManager: CBCentralManager? by CBCentralManager.holder()
    private var cameraPeripheral: CBPeripheral? = null
    private var fff5Characteristic: CBCharacteristic? = null
    private var state: DjiDeviceState = DjiDeviceState.idle
    open var delegate: DjiDeviceDelegate? = null
    private val startStreamingTimer = MainTimer()
    private val stopStreamingTimer = MainTimer()
    private var model: SettingsDjiDeviceModel = SettingsDjiDeviceModel.unknown
    private var batteryPercentage: Int? = null

    open fun startLiveStream(
        wifiSsid: String,
        wifiPassword: String,
        rtmpUrl: String,
        resolution: SettingsDjiDeviceResolution,
        fps: Int,
        bitrate: UInt,
        videoCodec: SettingsDjiDeviceVideoCodec,
        imageStabilization: SettingsDjiDeviceImageStabilization,
        deviceId: UUID,
        model: SettingsDjiDeviceModel,
    ) {
        Log.d("DjiDevice", "dji-device: Start live stream for $model")
        this.wifiSsid = wifiSsid
        this.wifiPassword = wifiPassword
        this.rtmpUrl = rtmpUrl
        this.resolution = resolution
        this.fps = fps
        this.bitrate = bitrate
        this.videoCodec = videoCodec
        this.imageStabilization = imageStabilization
        this.deviceId = deviceId
        this.model = model
        reset()
        startStartStreamingTimer()
        setState(state = DjiDeviceState.discovering)
        centralManager = CBCentralManager(delegate = this, queue = null)
    }

    open fun stopLiveStream() {
        if (state == DjiDeviceState.idle) {
            return
        }
        Log.d("DjiDevice", "dji-device: Stop live stream")
        stopStartStreamingTimer()
        startStopStreamingTimer()
        sendStopStream()
        setState(state = DjiDeviceState.stoppingStream)
    }

    open fun getBatteryPercentage(): Int? = batteryPercentage

    private fun reset() {
        stopStartStreamingTimer()
        stopStopStreamingTimer()
        centralManager = null
        cameraPeripheral = null
        fff5Characteristic = null
        batteryPercentage = null
        setState(state = DjiDeviceState.idle)
    }

    private fun startStartStreamingTimer() {
        startStreamingTimer.startSingleShot(timeout = 60.0) {
            startStreamingTimerExpired()
        }
    }

    private fun stopStartStreamingTimer() {
        startStreamingTimer.stop()
    }

    private fun startStreamingTimerExpired() {
        reset()
    }

    private fun startStopStreamingTimer() {
        stopStreamingTimer.startSingleShot(timeout = 10.0) {
            stopStreamingTimerExpired()
        }
    }

    private fun stopStopStreamingTimer() {
        stopStreamingTimer.stop()
    }

    private fun stopStreamingTimerExpired() {
        reset()
    }

    private fun setState(state: DjiDeviceState) {
        if (state == this.state) {
            return
        }
        Log.d("DjiDevice", "dji-device: State change ${this.state} -> $state")
        this.state = state
        delegate?.djiDeviceStreamingState(this, state)
    }

    open fun getState(): DjiDeviceState = state

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        when (central.state) {
            CBManagerState.poweredOn -> connect(central)
            else -> {}
        }
    }

    private fun connect(central: CBCentralManager) {
        val deviceId = this.deviceId ?: run {
            Log.i("DjiDevice", "dji-device: Device not found")
            return
        }
        val peripheral = central.retrievePeripherals(withIdentifiers = listOf(deviceId)).firstOrNull()
            ?: run {
                Log.i("DjiDevice", "dji-device: Device not found")
                return
            }
        cameraPeripheral = peripheral
        peripheral.delegate = this
        central.connect(peripheral)
        startStartStreamingTimer()
        setState(state = DjiDeviceState.connecting)
    }

    override fun centralManagerDidFailToConnect(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        error: Throwable?,
    ) {}

    override fun centralManagerDidConnect(central: CBCentralManager, peripheral: CBPeripheral) {
        peripheral.discoverServices(null)
    }

    override fun centralManagerDidDisconnectPeripheral(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        error: Throwable?,
    ) {
        reset()
    }

    override fun peripheralDidDiscoverServices(peripheral: CBPeripheral, error: Throwable?) {
        val peripheralServices = peripheral.services ?: return
        for (service in peripheralServices) {
            peripheral.discoverCharacteristics(null, `for` = service)
        }
    }

    override fun peripheralDidDiscoverCharacteristicsFor(
        peripheral: CBPeripheral,
        service: CBService,
        error: Throwable?,
    ) {
        for (characteristic in service.characteristics.orEmpty()) {
            if (characteristic.uuid == fff5Id) {
                fff5Characteristic = characteristic
            }
            peripheral.setNotifyValue(true, `for` = characteristic)
        }
    }

    override fun peripheralDidUpdateValueFor(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: Throwable?,
    ) {
        val value = characteristic.value ?: return
        val message = runCatching { DjiMessage(data = value) }.getOrNull() ?: run {
            Log.i("DjiDevice", "dji-device: Discarding corrupt message ${value.hexString()}")
            return
        }
        when (state) {
            DjiDeviceState.checkingIfPaired -> processCheckingIfPaired(response = message)
            DjiDeviceState.pairing -> processPairing()
            DjiDeviceState.cleaningUp -> processCleaningUp(response = message)
            DjiDeviceState.preparingStream -> processPreparingStream(response = message)
            DjiDeviceState.settingUpWifi -> processSettingUpWifi(response = message)
            DjiDeviceState.configuring -> processConfiguring(response = message)
            DjiDeviceState.startingStream -> processStartingStream(response = message)
            DjiDeviceState.streaming -> processStreaming(message = message)
            DjiDeviceState.stoppingStream -> processStoppingStream(response = message)
            DjiDeviceState.connecting -> {}
            else -> Log.i("DjiDevice", "dji-device: Received message in unexpected state '$state'")
        }
    }

    private fun sendStopStream() {
        val payload = DjiStopStreamingMessagePayload
        writeMessage(
            message = DjiMessage(
                target = stopStreamingTarget,
                id = stopStreamingTransactionId,
                type = stopStreamingType,
                payload = payload.encode(),
            ),
        )
    }

    private fun processCheckingIfPaired(response: DjiMessage) {
        if (response.id != pairTransactionId) {
            return
        }
        if (response.payload.contentEquals(byteArrayOf(0, 1))) {
            processPairing()
        } else {
            setState(state = DjiDeviceState.pairing)
        }
    }

    private fun processPairing() {
        sendStopStream()
        setState(state = DjiDeviceState.cleaningUp)
    }

    private fun processCleaningUp(response: DjiMessage) {
        if (response.id != stopStreamingTransactionId) {
            return
        }
        val payload = DjiPreparingToLivestreamMessagePayload
        writeMessage(
            message = DjiMessage(
                target = preparingToLivestreamTarget,
                id = preparingToLivestreamTransactionId,
                type = preparingToLivestreamType,
                payload = payload.encode(),
            ),
        )
        setState(state = DjiDeviceState.preparingStream)
    }

    private fun processPreparingStream(response: DjiMessage) {
        if (response.id != preparingToLivestreamTransactionId) {
            return
        }
        val wifiSsid = this.wifiSsid ?: return
        val wifiPassword = this.wifiPassword ?: return
        val payload = DjiSetupWifiMessagePayload(wifiSsid = wifiSsid, wifiPassword = wifiPassword)
        writeMessage(
            message = DjiMessage(
                target = setupWifiTarget,
                id = setupWifiTransactionId,
                type = setupWifiType,
                payload = payload.encode(),
            ),
        )
        setState(state = DjiDeviceState.settingUpWifi)
    }

    private fun processSettingUpWifi(response: DjiMessage) {
        if (response.id != setupWifiTransactionId) {
            return
        }
        if (!response.payload.contentEquals(byteArrayOf(0x00, 0x00))) {
            reset()
            setState(state = DjiDeviceState.wifiSetupFailed)
            return
        }
        when (model) {
            SettingsDjiDeviceModel.osmoAction2, SettingsDjiDeviceModel.osmoAction3 -> sendStartStreaming()
            SettingsDjiDeviceModel.osmoAction4, SettingsDjiDeviceModel.osmoAction6 -> {
                val imageStabilization = this.imageStabilization ?: return
                val payload = DjiConfigureMessagePayload(imageStabilization = imageStabilization, oa5 = false)
                writeMessage(
                    message = DjiMessage(
                        target = configureTarget,
                        id = configureTransactionId,
                        type = configureType,
                        payload = payload.encode(),
                    ),
                )
                setState(state = DjiDeviceState.configuring)
            }
            SettingsDjiDeviceModel.osmoAction5Pro, SettingsDjiDeviceModel.osmo360 -> {
                val imageStabilization = this.imageStabilization ?: return
                val payload = DjiConfigureMessagePayload(imageStabilization = imageStabilization, oa5 = true)
                writeMessage(
                    message = DjiMessage(
                        target = configureTarget,
                        id = configureTransactionId,
                        type = configureType,
                        payload = payload.encode(),
                    ),
                )
                setState(state = DjiDeviceState.configuring)
            }
            SettingsDjiDeviceModel.osmoPocket3 -> sendStartStreaming()
            SettingsDjiDeviceModel.osmoPocket4, SettingsDjiDeviceModel.osmoPocket4Pro -> sendStartStreaming()
            SettingsDjiDeviceModel.unknown -> sendStartStreaming()
        }
    }

    private fun processConfiguring(response: DjiMessage) {
        if (response.id != configureTransactionId) {
            return
        }
        sendStartStreaming()
    }

    private fun sendStartStreaming() {
        val rtmpUrl = this.rtmpUrl ?: return
        val resolution = this.resolution ?: return
        val bitrateKbps = ((bitrate / 1000u) and 0xFFFFu).toUShort()
        when (model) {
            SettingsDjiDeviceModel.osmoPocket4, SettingsDjiDeviceModel.osmoPocket4Pro -> {
                val payload = DjiStartStreamingMessagePayload2(
                    rtmpUrl = rtmpUrl,
                    resolution = resolution,
                    fps = fps,
                    bitrateKbps = bitrateKbps,
                    codec = videoCodec.toDjiCodec(),
                    enhancedRtmp = videoCodec.toDjiEnhancedRtmp(),
                    middle = DjiStartStreamingMessagePayload2.osmoPocket4Middle,
                )
                writeMessage(
                    message = DjiMessage(
                        target = startStreamingTarget,
                        id = startStreamingTransactionId,
                        type = startStreamingType,
                        payload = payload.encode(),
                    ),
                )
            }
            SettingsDjiDeviceModel.osmoAction6 -> {
                val payload = DjiStartStreamingMessagePayload2(
                    rtmpUrl = rtmpUrl,
                    resolution = resolution,
                    fps = fps,
                    bitrateKbps = bitrateKbps,
                    codec = videoCodec.toDjiCodec(),
                    enhancedRtmp = videoCodec.toDjiEnhancedRtmp(),
                    middle = DjiStartStreamingMessagePayload2.osmoAction6Middle,
                )
                writeMessage(
                    message = DjiMessage(
                        target = startStreamingTarget,
                        id = startStreamingTransactionId,
                        type = startStreamingType,
                        payload = payload.encode(),
                    ),
                )
            }
            else -> {
                val payload = DjiStartStreamingMessagePayload(
                    rtmpUrl = rtmpUrl,
                    resolution = resolution,
                    fps = fps,
                    bitrateKbps = bitrateKbps,
                )
                writeMessage(
                    message = DjiMessage(
                        target = startStreamingTarget,
                        id = startStreamingTransactionId,
                        type = startStreamingType,
                        payload = payload.encode(),
                    ),
                )
            }
        }
        if (model.hasNewProtocol()) {
            val confirmStartStreamPayload = DjiConfirmStartStreamingMessagePayload
            writeMessage(
                message = DjiMessage(
                    target = stopStreamingTarget,
                    id = stopStreamingTransactionId,
                    type = stopStreamingType,
                    payload = confirmStartStreamPayload.encode(),
                ),
            )
        }
        setState(state = DjiDeviceState.startingStream)
    }

    private fun processStartingStream(response: DjiMessage) {
        if (response.id != startStreamingTransactionId) {
            return
        }
        setState(state = DjiDeviceState.streaming)
        stopStartStreamingTimer()
    }

    private fun processStreaming(message: DjiMessage) {
        when (message.type) {
            statusType -> {
                val payload = DjiStatusMessagePayload(message.payload)
                if (payload != null) {
                    batteryPercentage = payload.batteryPercentage.toInt()
                }
            }
            else -> {}
        }
    }

    private fun processStoppingStream(response: DjiMessage) {
        if (response.id != stopStreamingTransactionId) {
            return
        }
        reset()
    }

    private fun writeMessage(message: DjiMessage) {
        Log.d("DjiDevice", "dji-device: Send ${message.format()}")
        writeValue(value = message.encode())
    }

    private fun writeValue(value: ByteArray) {
        val fff5Characteristic = this.fff5Characteristic ?: return
        cameraPeripheral?.writeValue(
            data = value,
            `for` = fff5Characteristic,
            type = CBCharacteristicWriteType.withoutResponse,
        )
    }

    override fun peripheralDidUpdateNotificationStateFor(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: Throwable?,
    ) {
        if (state != DjiDeviceState.connecting) {
            return
        }
        if (characteristic.uuid != fff4Id) {
            return
        }
        val payload = DjiPairMessagePayload(pairPinCode = pairPinCode)
        val request = DjiMessage(
            target = pairTarget,
            id = pairTransactionId,
            type = pairType,
            payload = payload.encode(),
        )
        writeMessage(message = request)
        setState(state = DjiDeviceState.checkingIfPaired)
    }

    override fun peripheralIsReadyToSendWriteWithoutResponse(peripheral: CBPeripheral) {}
}

fun SettingsDjiDevice.canStartLive(isConnectedToIpv4WiFi: Boolean): Boolean {
    if (bluetoothPeripheralId == null) {
        return false
    }
    if (wifiSsid.isEmpty()) {
        return false
    }
    when (rtmpUrlType) {
        SettingsDjiDeviceUrlType.server -> {
            val serverRtmpUrl = serverRtmpUrl
            if (serverRtmpUrl != null) {
                if (serverRtmpUrl.isEmpty()) {
                    return false
                }
            } else {
                return isConnectedToIpv4WiFi
            }
        }
        SettingsDjiDeviceUrlType.custom -> {
            if (customRtmpUrl.isEmpty()) {
                return false
            }
        }
    }
    return true
}
