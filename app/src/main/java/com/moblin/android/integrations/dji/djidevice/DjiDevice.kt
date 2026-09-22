package com.moblin.android.integrations.dji.djidevice

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.moblin.android.integrations.dji.DjiMessage
import com.moblin.android.various.MainTimer
import com.moblin.android.various.settings.SettingsDjiDevice
import com.moblin.android.various.settings.SettingsDjiDeviceImageStabilization
import com.moblin.android.various.settings.SettingsDjiDeviceModel
import com.moblin.android.various.settings.SettingsDjiDeviceResolution
import com.moblin.android.various.settings.SettingsDjiDeviceUrlType
import com.moblin.android.various.settings.SettingsDjiDeviceVideoCodec
import java.util.UUID

private const val tag = "DjiDevice"

private val pairTransactionId: Int = 0x8092
private val stopStreamingTransactionId: Int = 0xEAC8
private val preparingToLivestreamTransactionId: Int = 0x8C12
private val setupWifiTransactionId: Int = 0x8C19
private val startStreamingTransactionId: Int = 0x8C2C
private val configureTransactionId: Int = 0x8C2D

private val pairTarget: Int = 0x0702
private val stopStreamingTarget: Int = 0x0802
private val preparingToLivestreamTarget: Int = 0x0802
private val setupWifiTarget: Int = 0x0702
private val configureTarget: Int = 0x0102
private val startStreamingTarget: Int = 0x0802

private val pairType: Int = 0x450740
private val stopStreamingType: Int = 0x8E0240
private val preparingToLivestreamType: Int = 0xE10240
private val setupWifiType: Int = 0x470740
private val configureType: Int = 0x8E0240
private val startStreamingType: Int = 0x780840
private val statusType: Int = 0x020D00

private val fff4Id: UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")
private val fff5Id: UUID = UUID.fromString("0000fff5-0000-1000-8000-00805f9b34fb")
private val cccdId: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

private const val pairPinCode = "mbln"

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

class DjiDevice(private val context: Context) {
    private var wifiSsid: String? = null
    private var wifiPassword: String? = null
    private var rtmpUrl: String? = null
    private var resolution: SettingsDjiDeviceResolution? = null
    private var fps: Int = 30
    private var bitrate: UInt = 6_000_000u
    private var videoCodec: SettingsDjiDeviceVideoCodec = SettingsDjiDeviceVideoCodec.h265hevc
    private var imageStabilization: SettingsDjiDeviceImageStabilization? = null
    private var deviceId: UUID? = null
    private var centralManager: BluetoothAdapter? = null
    private var cameraPeripheral: BluetoothGatt? = null
    private var fff5Characteristic: BluetoothGattCharacteristic? = null
    private var state: DjiDeviceState = DjiDeviceState.idle
    var delegate: DjiDeviceDelegate? = null
    private val startStreamingTimer = MainTimer()
    private val stopStreamingTimer = MainTimer()
    private var model: SettingsDjiDeviceModel = SettingsDjiDeviceModel.unknown
    private var batteryPercentage: Int? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                centralManagerDidFailToConnect(gatt)
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> centralManagerDidConnect(gatt)
                BluetoothProfile.STATE_DISCONNECTED -> centralManagerDidDisconnectPeripheral(gatt)
                else -> {}
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            peripheralDidDiscoverServices(gatt)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            peripheralDidUpdateValueFor(characteristic)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                peripheralDidUpdateNotificationStateFor(descriptor.characteristic)
            }
        }
    }

    fun startLiveStream(
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
        Log.d(tag, "dji-device: Start live stream for $model")
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
        setState(DjiDeviceState.discovering)
        centralManager =
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        centralManager?.let { centralManagerDidUpdateState(it) }
    }

    fun stopLiveStream() {
        if (state == DjiDeviceState.idle) {
            return
        }
        Log.d(tag, "dji-device: Stop live stream")
        stopStartStreamingTimer()
        startStopStreamingTimer()
        sendStopStream()
        setState(DjiDeviceState.stoppingStream)
    }

    fun getBatteryPercentage(): Int? {
        return batteryPercentage
    }

    private fun reset() {
        stopStartStreamingTimer()
        stopStopStreamingTimer()
        centralManager = null
        cameraPeripheral?.close()
        cameraPeripheral = null
        fff5Characteristic = null
        batteryPercentage = null
        setState(DjiDeviceState.idle)
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
        Log.d(tag, "dji-device: State change ${this.state} -> $state")
        this.state = state
        delegate?.djiDeviceStreamingState(this, state)
    }

    fun getState(): DjiDeviceState {
        return state
    }

    fun centralManagerDidUpdateState(central: BluetoothAdapter) {
        if (central.isEnabled) {
            connect(central)
        }
    }

    private fun connect(central: BluetoothAdapter) {
        val deviceId = this.deviceId ?: run {
            Log.i(tag, "dji-device: Device not found")
            return
        }
        val peripheral = runCatching { central.getRemoteDevice(deviceId.toString()) }.getOrNull()
        if (peripheral == null) {
            Log.i(tag, "dji-device: Device not found")
            return
        }
        cameraPeripheral = peripheral.connectGatt(context, false, gattCallback)
        startStartStreamingTimer()
        setState(DjiDeviceState.connecting)
    }

    fun centralManagerDidFailToConnect(gatt: BluetoothGatt) {}

    fun centralManagerDidConnect(gatt: BluetoothGatt) {
        gatt.discoverServices()
    }

    fun centralManagerDidDisconnectPeripheral(gatt: BluetoothGatt) {
        reset()
    }

    fun peripheralDidDiscoverServices(gatt: BluetoothGatt) {
        val peripheralServices = gatt.services ?: return
        for (service in peripheralServices) {
            peripheralDidDiscoverCharacteristicsFor(gatt, service)
        }
    }

    fun peripheralDidDiscoverCharacteristicsFor(
        gatt: BluetoothGatt,
        service: BluetoothGattService,
    ) {
        for (characteristic in service.characteristics) {
            if (characteristic.uuid == fff5Id) {
                fff5Characteristic = characteristic
            }
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(cccdId)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    fun peripheralDidUpdateValueFor(characteristic: BluetoothGattCharacteristic) {
        val value = characteristic.value ?: return
        val message = runCatching { DjiMessage(data = value) }.getOrNull()
        if (message == null) {
            val hex = value.joinToString(separator = "") { "%02x".format(it) }
            Log.i(tag, "dji-device: Discarding corrupt message $hex")
            return
        }
        when (state) {
            DjiDeviceState.checkingIfPaired -> processCheckingIfPaired(message)
            DjiDeviceState.pairing -> processPairing()
            DjiDeviceState.cleaningUp -> processCleaningUp(message)
            DjiDeviceState.preparingStream -> processPreparingStream(message)
            DjiDeviceState.settingUpWifi -> processSettingUpWifi(message)
            DjiDeviceState.configuring -> processConfiguring(message)
            DjiDeviceState.startingStream -> processStartingStream(message)
            DjiDeviceState.streaming -> processStreaming(message)
            DjiDeviceState.stoppingStream -> processStoppingStream(message)
            DjiDeviceState.connecting -> {}
            else -> Log.i(tag, "dji-device: Received message in unexpected state '$state'")
        }
    }

    private fun sendStopStream() {
        val payload = byteArrayOf(0x01, 0x01, 0x1A, 0x00, 0x01, 0x02)
        writeMessage(
            DjiMessage(
                target = stopStreamingTarget,
                id = stopStreamingTransactionId,
                type = stopStreamingType,
                payload = payload,
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
            setState(DjiDeviceState.pairing)
        }
    }

    private fun processPairing() {
        sendStopStream()
        setState(DjiDeviceState.cleaningUp)
    }

    private fun processCleaningUp(response: DjiMessage) {
        if (response.id != stopStreamingTransactionId) {
            return
        }
        val payload = byteArrayOf(0x1A)
        writeMessage(
            DjiMessage(
                target = preparingToLivestreamTarget,
                id = preparingToLivestreamTransactionId,
                type = preparingToLivestreamType,
                payload = payload,
            ),
        )
        setState(DjiDeviceState.preparingStream)
    }

    private fun processPreparingStream(response: DjiMessage) {
        if (response.id != preparingToLivestreamTransactionId) {
            return
        }
        val wifiSsid = this.wifiSsid ?: return
        val wifiPassword = this.wifiPassword ?: return
        val payload = DjiSetupWifiMessagePayload(
            wifiSsid = wifiSsid,
            wifiPassword = wifiPassword,
        )
        writeMessage(
            DjiMessage(
                target = setupWifiTarget,
                id = setupWifiTransactionId,
                type = setupWifiType,
                payload = payload.encode(),
            ),
        )
        setState(DjiDeviceState.settingUpWifi)
    }

    private fun processSettingUpWifi(response: DjiMessage) {
        if (response.id != setupWifiTransactionId) {
            return
        }
        if (!response.payload.contentEquals(byteArrayOf(0x00, 0x00))) {
            reset()
            setState(DjiDeviceState.wifiSetupFailed)
            return
        }
        when (model) {
            SettingsDjiDeviceModel.osmoAction2,
            SettingsDjiDeviceModel.osmoAction3,
            -> sendStartStreaming()
            SettingsDjiDeviceModel.osmoAction4, SettingsDjiDeviceModel.osmoAction6 -> {
                val imageStabilization = this.imageStabilization ?: return
                val payload = DjiConfigureMessagePayload(
                    imageStabilization = imageStabilization,
                    oa5 = false,
                )
                writeMessage(
                    DjiMessage(
                        target = configureTarget,
                        id = configureTransactionId,
                        type = configureType,
                        payload = payload.encode(),
                    ),
                )
                setState(DjiDeviceState.configuring)
            }
            SettingsDjiDeviceModel.osmoAction5Pro, SettingsDjiDeviceModel.osmo360 -> {
                val imageStabilization = this.imageStabilization ?: return
                val payload = DjiConfigureMessagePayload(
                    imageStabilization = imageStabilization,
                    oa5 = true,
                )
                writeMessage(
                    DjiMessage(
                        target = configureTarget,
                        id = configureTransactionId,
                        type = configureType,
                        payload = payload.encode(),
                    ),
                )
                setState(DjiDeviceState.configuring)
            }
            SettingsDjiDeviceModel.osmoPocket3 -> sendStartStreaming()
            SettingsDjiDeviceModel.osmoPocket4 -> sendStartStreaming()
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
        val bitrateKbps: UShort = ((bitrate / 1000u) and 0xFFFFu).toUShort()
        when (model) {
            SettingsDjiDeviceModel.osmoPocket4 -> {
                val payload = DjiStartStreamingMessagePayload2(
                    rtmpUrl = rtmpUrl,
                    resolution = resolution,
                    fps = fps,
                    bitrateKbps = bitrateKbps,
                    codec = videoCodec.toDjiCodec(),
                    enhancedRtmp = videoCodec.toDjiEnhancedRtmp(),
                    header = DjiStartStreamingMessagePayload2.osmoPocket4Header,
                    middle = DjiStartStreamingMessagePayload2.osmoPocket4Middle,
                )
                writeMessage(
                    DjiMessage(
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
                    header = DjiStartStreamingMessagePayload2.osmoAction6Header,
                    middle = DjiStartStreamingMessagePayload2.osmoAction6Middle,
                )
                writeMessage(
                    DjiMessage(
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
                    oa5 = model.hasNewProtocol(),
                )
                writeMessage(
                    DjiMessage(
                        target = startStreamingTarget,
                        id = startStreamingTransactionId,
                        type = startStreamingType,
                        payload = payload.encode(),
                    ),
                )
            }
        }
        if (model.hasNewProtocol()) {
            val confirmStartStreamPayload = byteArrayOf(0x01, 0x01, 0x1A, 0x00, 0x01, 0x01)
            writeMessage(
                DjiMessage(
                    target = stopStreamingTarget,
                    id = stopStreamingTransactionId,
                    type = stopStreamingType,
                    payload = confirmStartStreamPayload,
                ),
            )
        }
        setState(DjiDeviceState.startingStream)
    }

    private fun processStartingStream(response: DjiMessage) {
        if (response.id != startStreamingTransactionId) {
            return
        }
        setState(DjiDeviceState.streaming)
        stopStartStreamingTimer()
    }

    private fun processStreaming(message: DjiMessage) {
        when (message.type) {
            statusType -> {
                val payload = runCatching {
                    DjiStatusMessagePayload(message.payload)
                }.getOrNull()
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
        Log.d(tag, "dji-device: Send ${message.format()}")
        writeValue(message.encode())
    }

    private fun writeValue(value: ByteArray) {
        val fff5Characteristic = this.fff5Characteristic ?: return
        val cameraPeripheral = this.cameraPeripheral ?: return
        fff5Characteristic.value = value
        fff5Characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        cameraPeripheral.writeCharacteristic(fff5Characteristic)
    }

    fun peripheralDidUpdateNotificationStateFor(characteristic: BluetoothGattCharacteristic) {
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
        writeMessage(request)
        setState(DjiDeviceState.checkingIfPaired)
    }

    fun peripheralIsReadyToSendWriteWithoutResponse(gatt: BluetoothGatt) {}
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
            val serverRtmpUrl = this.serverRtmpUrl
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
