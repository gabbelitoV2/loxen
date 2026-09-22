package com.moblin.android.various.model

import com.moblin.android.integrations.gopro.GoProDevice
import com.moblin.android.integrations.gopro.GoProDeviceDelegate
import com.moblin.android.integrations.gopro.GoProDeviceState
import com.moblin.android.localized
import com.moblin.android.various.settings.SettingsDjiDeviceUrlType
import com.moblin.android.various.settings.SettingsGoProDevice
import com.moblin.android.view.settings.djidevices.rtmpServerStreamUrl
import java.util.UUID

fun Model.startGoProDeviceLiveStream(device: SettingsGoProDevice) {
    if (!goProDevices.containsKey(device.id)) {
        val goProDevice = GoProDevice(TODO("Missing Android context for GoProDevice"))
        goProDevice.delegate = object : GoProDeviceDelegate {
            override fun goProDeviceStreamingState(device: GoProDevice, state: GoProDeviceState) {
                this@startGoProDeviceLiveStream.goProDeviceStreamingState(device, state)
            }
        }
        goProDevices[device.id] = goProDevice
    }
    val goProDevice = goProDevices[device.id] ?: return
    device.isStarted = true
    startGoProDeviceLiveStreamInternal(goProDevice, device)
}

fun Model.stopGoProDeviceLiveStream(device: SettingsGoProDevice) {
    device.isStarted = false
    device.autoRestartStreamTimer.stop()
    goProDevices[device.id]?.stopLiveStream()
}

fun Model.removeGoProDevices(offsets: Set<Int>) {
    for (offset in offsets) {
        val device = database.goPro.devices[offset]
        stopGoProDeviceLiveStream(device)
        goProDevices.remove(device.id)
    }
    for (offset in offsets.sortedDescending()) {
        database.goPro.devices.removeAt(offset)
    }
}

fun Model.autoStartGoProDevices() {
    for (device in database.goPro.devices) {
        if (!device.isStarted) {
            continue
        }
        startGoProDeviceLiveStream(device)
    }
}

fun Model.reloadGoProDevicesAfterSettingsImport() {
    for ((deviceId, goProDevice) in goProDevices.toMap()) {
        if (database.goPro.devices.any { it.id == deviceId }) {
            continue
        }
        goProDevice.stopLiveStream()
        goProDevices.remove(deviceId)
    }
    autoStartGoProDevices()
}

fun Model.restartGoProLiveStreamIfNeededAfterDelay(device: SettingsGoProDevice) {
    device.autoRestartStreamTimer.startSingleShot(5.0) {
        restartGoProLiveStreamIfNeeded(device)
    }
}

fun Model.markGoProIsStreamingIfNeeded(rtmpServerStreamId: UUID) {
    for (device in database.goPro.devices) {
        if (device.rtmpUrlType != SettingsDjiDeviceUrlType.server ||
            device.serverRtmpStreamId != rtmpServerStreamId
        ) {
            continue
        }
        device.autoRestartStreamTimer.stop()
    }
}

fun Model.automaticServerRtmpUrl(device: SettingsGoProDevice): String? {
    val stream = getRtmpStream(device.serverRtmpStreamId) ?: return null
    return rtmpServerStreamUrl(
        address = TODO("getServerAddress"),
        port = database.rtmpServer.port,
        streamKey = stream.streamKey
    )
}

private fun Model.startGoProDeviceLiveStreamInternal(
    goProDevice: GoProDevice,
    device: SettingsGoProDevice
) {
    val rtmpUrl: String? = when (device.rtmpUrlType) {
        SettingsDjiDeviceUrlType.server ->
            device.serverRtmpUrl ?: automaticServerRtmpUrl(device)
        SettingsDjiDeviceUrlType.custom -> device.customRtmpUrl
        else -> null
    }
    val deviceId = device.bluetoothPeripheralId ?: return
    if (rtmpUrl == null) {
        restartGoProLiveStreamIfNeededAfterDelay(device)
        return
    }
    goProDevice.startLiveStream(
        wifiSsid = device.wifiSsid,
        wifiPassword = device.wifiPassword,
        rtmpUrl = rtmpUrl,
        resolution = device.resolution,
        bitrate = device.bitrate.toUInt(),
        lens = device.lens,
        deviceId = deviceId
    )
    device.autoRestartStreamTimer.startSingleShot(95.0) {
        makeErrorToast(
            title = localized("Failed to start live stream from GoPro ${device.name}")
        )
        restartGoProLiveStreamIfNeeded(device)
    }
}

private fun Model.restartGoProLiveStreamIfNeeded(device: SettingsGoProDevice) {
    if (device.rtmpUrlType != SettingsDjiDeviceUrlType.server ||
        !device.autoRestartStream ||
        !device.isStarted
    ) {
        return
    }
    val goProDevice = goProDevices[device.id] ?: return
    startGoProDeviceLiveStreamInternal(goProDevice, device)
}

private fun Model.getGoProDeviceSettings(goProDevice: GoProDevice): SettingsGoProDevice? {
    return database.goPro.devices.firstOrNull { goProDevices[it.id] === goProDevice }
}

fun Model.goProDeviceStreamingState(goProDevice: GoProDevice, state: GoProDeviceState) {
    val device = getGoProDeviceSettings(goProDevice) ?: return
    device.state = state
    when (state) {
        GoProDeviceState.connecting -> {
            makeToast(title = localized("Connecting to GoPro ${device.name}"))
        }
        GoProDeviceState.streaming -> {
            if (device.rtmpUrlType == SettingsDjiDeviceUrlType.custom) {
                device.autoRestartStreamTimer.stop()
                makeToast(title = localized("GoPro ${device.name} streaming to custom URL"))
            }
        }
        GoProDeviceState.wifiSetupFailed -> {
            makeErrorToast(
                title = localized("WiFi setup failed for GoPro ${device.name}"),
                subTitle = localized("Please check the WiFi settings")
            )
        }
        GoProDeviceState.failed -> {
            makeErrorToast(title = localized("GoPro ${device.name} failed to start streaming"))
            restartGoProLiveStreamIfNeededAfterDelay(device)
        }
        else -> {}
    }
}
