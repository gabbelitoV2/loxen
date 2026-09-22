package com.moblin.android.various.model

import com.moblin.android.integrations.dji.djidevice.DjiDevice
import com.moblin.android.integrations.dji.djidevice.DjiDeviceDelegate
import com.moblin.android.integrations.dji.djidevice.DjiDeviceState
import com.moblin.android.localized
import com.moblin.android.various.settings.SettingsDjiDevice
import com.moblin.android.various.settings.SettingsDjiDeviceUrlType
import com.moblin.android.view.settings.djidevices.rtmpServerStreamUrl
import java.util.UUID
import com.moblin.android.AppDelegate

fun Model.startDjiDeviceLiveStream(device: SettingsDjiDevice) {
    if (!djiDevices.containsKey(device.id)) {
        val djiDevice = DjiDevice(context = AppDelegate.context)
        djiDevice.delegate = ModelDjiDeviceDelegate(this)
        djiDevices[device.id] = djiDevice
    }
    val djiDevice = djiDevices[device.id] ?: return
    device.isStarted = true
    startDjiDeviceLiveStreamInternal(djiDevice = djiDevice, device = device)
}

fun Model.stopDjiDeviceLiveStream(device: SettingsDjiDevice) {
    device.isStarted = false
    device.autoRestartStreamTimer.stop()
    djiDevices[device.id]?.stopLiveStream()
}

fun Model.restartDjiLiveStreamIfNeededAfterDelay(device: SettingsDjiDevice) {
    startDjiDeviceRestartTimer(device = device, timeout = 5.0)
}

fun Model.markDjiIsStreamingIfNeeded(rtmpServerStreamId: UUID) {
    for (device in database.djiDevices.devices) {
        if (device.rtmpUrlType != SettingsDjiDeviceUrlType.server ||
            device.serverRtmpStreamId != rtmpServerStreamId
        ) {
            continue
        }
        device.autoRestartStreamTimer.stop()
    }
}

fun Model.setCurrentDjiDevice(device: SettingsDjiDevice) {
    currentDjiDeviceSettings = device
    statusTopRight.djiDeviceStreamingState.value = djiDevices[device.id]?.getState()
}

fun Model.reloadDjiDevices() {
    for (deviceId in djiDevices.keys.toList()) {
        val device = database.djiDevices.devices.firstOrNull { it.id == deviceId } ?: continue
        if (!device.isStarted) {
            continue
        }
        val djiDevice = djiDevices[device.id] ?: continue
        if (djiDevice.getState() == DjiDeviceState.streaming) {
            continue
        }
        startDjiDeviceLiveStream(device = device)
    }
}

fun Model.reloadDjiDevices(enabledDeviceIds: Set<UUID>) {
    for (device in database.djiDevices.devices) {
        if (enabledDeviceIds.contains(device.id)) {
            if (!device.isStarted) {
                startDjiDeviceLiveStream(device = device)
            }
        } else {
            stopDjiDeviceLiveStream(device = device)
        }
    }
}

fun Model.reloadDjiDevicesAfterSettingsImport() {
    for ((deviceId, djiDevice) in djiDevices.toMap()) {
        if (database.djiDevices.devices.none { it.id == deviceId }) {
            djiDevice.stopLiveStream()
            djiDevices.remove(deviceId)
        }
    }
    autoStartDjiDevices()
}

fun Model.autoStartDjiDevices() {
    for (device in database.djiDevices.devices) {
        if (device.isStarted) {
            startDjiDeviceLiveStream(device = device)
        }
    }
}

fun Model.removeDjiDevices(offsets: Set<Int>) {
    val indices = offsets.sortedDescending()
    for (index in indices) {
        val device = database.djiDevices.devices[index]
        stopDjiDeviceLiveStream(device = device)
        djiDevices.remove(device.id)
    }
    for (index in indices) {
        database.djiDevices.devices =
            database.djiDevices.devices.toMutableList().also { it.removeAt(index) }
    }
}

fun Model.updateDjiDevicesStatus() {
    val statuses = mutableListOf<String>()
    for (device in database.djiDevices.devices) {
        val djiDevice = djiDevices[device.id] ?: continue
        if (djiDevice.getState() != DjiDeviceState.streaming) {
            continue
        }
        val (status, ok) = formatDeviceStatus(
            name = device.name,
            batteryPercentage = djiDevice.getBatteryPercentage(),
            thermalState = null
        )
        statuses.add(status)
        if (!ok && database.chat.botEnabled && database.chat.botSendLowBatteryWarning) {
            sendChatMessage(message = "Moblin bot: $lowBatteryMessage: $status")
        }
    }
    val status = statuses.joinToString(", ")
    if (status != statusTopRight.djiDevicesStatus.value) {
        statusTopRight.djiDevicesStatus.value = status
    }
}

private fun Model.startDjiDeviceLiveStreamInternal(
    djiDevice: DjiDevice,
    device: SettingsDjiDevice
) {
    val rtmpUrl: String? = when (device.rtmpUrlType) {
        SettingsDjiDeviceUrlType.server ->
            device.serverRtmpUrl ?: automaticDjiServerRtmpUrl(device = device)
        SettingsDjiDeviceUrlType.custom -> device.customRtmpUrl
    }
    val deviceId = device.bluetoothPeripheralId ?: return
    if (rtmpUrl != null) {
        djiDevice.startLiveStream(
            wifiSsid = device.wifiSsid,
            wifiPassword = device.wifiPassword,
            rtmpUrl = rtmpUrl,
            resolution = device.resolution,
            fps = device.fps,
            bitrate = device.bitrate,
            videoCodec = device.videoCodec,
            imageStabilization = device.imageStabilization,
            deviceId = deviceId,
            model = device.model
        )
        startDjiDeviceTimer(device = device)
    } else {
        startDjiDeviceRestartTimer(device = device, timeout = 3.0)
    }
}

fun Model.automaticDjiServerRtmpUrl(device: SettingsDjiDevice): String? {
    val stream = getRtmpStream(id = device.serverRtmpStreamId) ?: return null
    val status = statusOther.ipStatuses.value.firstOrNull {
        it.interfaceType.name == "wifi" && it.ipType.name == "ipv4"
    } ?: return null
    return rtmpServerStreamUrl(
        address = status.ipType.formatAddress(status.ip),
        port = database.rtmpServer.port,
        streamKey = stream.streamKey
    )
}

private fun Model.startDjiDeviceTimer(device: SettingsDjiDevice) {
    device.autoRestartStreamTimer.startSingleShot(45.0) {
        makeErrorToast(
            title = localized("Failed to start live stream from DJI device ${device.name}")
        )
        restartDjiLiveStreamIfNeeded(device = device)
    }
}

private fun Model.startDjiDeviceRestartTimer(device: SettingsDjiDevice, timeout: Double) {
    device.autoRestartStreamTimer.startSingleShot(timeout) {
        restartDjiLiveStreamIfNeeded(device = device)
    }
}

private fun Model.restartDjiLiveStreamIfNeeded(device: SettingsDjiDevice) {
    when (device.rtmpUrlType) {
        SettingsDjiDeviceUrlType.server -> {
            if (!device.autoRestartStream) {
                stopDjiDeviceLiveStream(device = device)
                return
            }
        }
        SettingsDjiDeviceUrlType.custom -> return
    }
    val djiDevice = djiDevices[device.id] ?: return
    if (!device.isStarted) {
        return
    }
    startDjiDeviceLiveStreamInternal(djiDevice = djiDevice, device = device)
}

private fun Model.getDjiDeviceSettings(djiDevice: DjiDevice): SettingsDjiDevice? {
    return database.djiDevices.devices.firstOrNull { djiDevices[it.id] === djiDevice }
}

fun Model.djiDeviceStreamingState(device: DjiDevice, state: DjiDeviceState) {
    val settingsDevice = getDjiDeviceSettings(djiDevice = device) ?: return
    settingsDevice.state = state
    if (settingsDevice === currentDjiDeviceSettings) {
        statusTopRight.djiDeviceStreamingState.value = state
    }
    when (state) {
        DjiDeviceState.connecting -> {
            startDjiDeviceTimer(device = settingsDevice)
            makeToast(title = localized("Connecting to DJI device ${settingsDevice.name}"))
        }
        DjiDeviceState.streaming -> {
            if (settingsDevice.rtmpUrlType == SettingsDjiDeviceUrlType.custom) {
                settingsDevice.autoRestartStreamTimer.stop()
                makeToast(
                    title = localized("DJI device ${settingsDevice.name} streaming to custom URL")
                )
            }
        }
        DjiDeviceState.wifiSetupFailed -> {
            makeErrorToast(
                title = localized("WiFi setup failed for DJI device ${settingsDevice.name}"),
                subTitle = localized("Please check the WiFi settings")
            )
        }
        else -> {}
    }
}

private class ModelDjiDeviceDelegate(private val model: Model) : DjiDeviceDelegate {
    override fun djiDeviceStreamingState(device: DjiDevice, state: DjiDeviceState) {
        model.djiDeviceStreamingState(device = device, state = state)
    }
}
