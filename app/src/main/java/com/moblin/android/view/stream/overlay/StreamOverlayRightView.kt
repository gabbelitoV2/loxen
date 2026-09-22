package com.moblin.android.view.stream.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.common.various.smallFont
import com.moblin.android.common.view.StreamOverlayIconAndTextPlacement
import com.moblin.android.common.view.StreamOverlayIconAndTextView
import com.moblin.android.localized
import com.moblin.android.various.model.Bitrate
import com.moblin.android.various.model.Bonding
import com.moblin.android.various.model.Ingests
import com.moblin.android.various.model.Moblink
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.RecordingProvider
import com.moblin.android.various.model.StatusTopRight
import com.moblin.android.various.model.StreamOverlay
import com.moblin.android.various.model.StreamState
import com.moblin.android.various.model.StreamUptimeProvider
import com.moblin.android.various.model.SystemMonitor
import com.moblin.android.various.model.Zoom
import com.moblin.android.various.model.areMoblinkRelaysOk
import com.moblin.android.various.model.isMoblinkRelayConfigured
import com.moblin.android.various.model.isShowingStatusRecording
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsAppMode
import com.moblin.android.various.settings.SettingsAutoSceneSwitcher
import com.moblin.android.various.settings.SettingsAutoSceneSwitchers
import com.moblin.android.various.settings.SettingsMoblinkRelay
import com.moblin.android.various.settings.SettingsMoblinkStreamer
import com.moblin.android.various.settings.SettingsRemoteControlAssistant
import com.moblin.android.various.settings.SettingsRemoteControlStreamer
import com.moblin.android.various.settings.SettingsRtmpServer
import com.moblin.android.various.settings.SettingsShow
import com.moblin.android.various.settings.SettingsSrtlaServer
import com.moblin.android.various.settings.SettingsStreamReplay
import com.moblin.android.view.stream.overlay.right.AudioLevelView
import com.moblin.android.view.stream.overlay.right.CompactAudioBarView
import com.moblin.android.view.stream.overlay.right.StreamOverlayRightBeautyView
import com.moblin.android.view.stream.overlay.right.StreamOverlayRightCameraSettingsControlView
import com.moblin.android.view.stream.overlay.right.StreamOverlayRightFaceView
import com.moblin.android.view.stream.overlay.right.StreamOverlayRightMediaPlayerControlsView
import com.moblin.android.view.stream.overlay.right.StreamOverlayRightPinchView
import com.moblin.android.view.stream.overlay.right.StreamOverlayRightPixellateView
import com.moblin.android.view.stream.overlay.right.StreamOverlayRightReplayView
import com.moblin.android.view.stream.overlay.right.StreamOverlayRightSceneSelectorView
import com.moblin.android.view.stream.overlay.right.StreamOverlayRightSceneVSelectorView
import com.moblin.android.view.stream.overlay.right.StreamOverlayRightTorchView
import com.moblin.android.view.stream.overlay.right.StreamOverlayRightVideoPreviewView
import com.moblin.android.view.stream.overlay.right.StreamOverlayRightWhirlpoolView
import com.moblin.android.view.stream.overlay.right.StreamOverlayRightZoomPresetSelctorView
import com.moblin.android.view.stream.overlay.right.StreamOverlayRightZoomPresetVSelctorView
import com.moblin.android.LocalModel

private val hidePlacement: StreamOverlayIconAndTextPlacement =
    TODO("StreamOverlayIconAndTextPlacement.hide")

private val beforeIconPlacement: StreamOverlayIconAndTextPlacement =
    TODO("StreamOverlayIconAndTextPlacement.beforeIcon")

@Composable
private fun CollapsedBondingView(bonding: Bonding, color: Color) {
    val pieChartPercentages by bonding.pieChartPercentages.collectAsState()
    Row(
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(backgroundColor)
            .clip(RoundedCornerShape(5.dp)),
    ) {
        Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(17.dp)
                .padding(horizontal = 2.dp),
        )
        if (pieChartPercentages.isNotEmpty()) {
            TODO("no Android counterpart for Swift Charts SectorMark")
        }
    }
}

@Composable
private fun BondingStatusView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    bonding: Bonding,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val statistics by bonding.statistics.collectAsState()
    val rtts by bonding.rtts.collectAsState()
    if (model.isShowingStatusBonding()) {
        val color = netStreamColor(model)
        if (textPlacement == hidePlacement) {
            CollapsedBondingView(bonding = bonding, color = color)
        } else {
            StreamOverlayIconAndTextView(
                icon = "phone.connection",
                text = statistics,
                textPlacement = textPlacement,
                color = color,
            )
        }
    }
    if (model.isShowingStatusBondingRtts()) {
        StreamOverlayIconAndTextView(
            icon = "phone.connection",
            text = rtts,
            textPlacement = textPlacement,
            color = netStreamColor(model),
        )
    }
}

@Composable
private fun ReplayStatusView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    replay: SettingsStreamReplay,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    if (model.isShowingStatusReplay()) {
        StreamOverlayIconAndTextView(
            icon = "play",
            text = localized("Enabled"),
            textPlacement = textPlacement,
        )
    }
}

@Composable
private fun CollapsedAdsRemainingTimerView(status: StatusTopRight) {
    val adsRemainingTimerStatus by status.adsRemainingTimerStatus.collectAsState()
    Row(
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(backgroundColor)
            .clip(RoundedCornerShape(5.dp)),
    ) {
        Icon(
            imageVector = Icons.Default.LocalCafe,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(17.dp)
                .padding(horizontal = 2.dp),
        )
        Text(
            text = adsRemainingTimerStatus,
            style = smallFont,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun AdsRemainingTimerView(
    model: Model = LocalModel.current,
    status: StatusTopRight,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val adsRemainingTimerStatus by status.adsRemainingTimerStatus.collectAsState()
    if (model.isShowingStatusAdsRemainingTimer()) {
        if (textPlacement == hidePlacement) {
            CollapsedAdsRemainingTimerView(status = status)
        } else {
            StreamOverlayIconAndTextView(
                icon = "cup.and.saucer",
                text = "$adsRemainingTimerStatus seconds",
                textPlacement = textPlacement,
            )
        }
    }
}

@Composable
private fun CollapsedBitrateView(bitrate: Bitrate) {
    val statusColor by bitrate.statusColor.collectAsState()
    val statusIconColor by bitrate.statusIconColor.collectAsState()
    val speedMbpsOneDecimal by bitrate.speedMbpsOneDecimal.collectAsState()
    Row(
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(backgroundColor)
            .clip(RoundedCornerShape(5.dp)),
    ) {
        Icon(
            imageVector = Icons.Default.Speed,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier
                .size(17.dp)
                .padding(start = 2.dp)
                .background(statusIconColor ?: Color.Transparent),
        )
        if (speedMbpsOneDecimal.isNotEmpty()) {
            Text(
                text = speedMbpsOneDecimal,
                style = smallFont,
                color = Color.White,
                modifier = Modifier.padding(end = 2.dp),
            )
        }
    }
}

@Composable
private fun BitrateStatusView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    bitrate: Bitrate,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val speedAndTotal by bitrate.speedAndTotal.collectAsState()
    val statusColor by bitrate.statusColor.collectAsState()
    val statusIconColor by bitrate.statusIconColor.collectAsState()
    if (model.isShowingStatusBitrate()) {
        if (textPlacement == hidePlacement) {
            CollapsedBitrateView(bitrate = model.bitrate)
        } else {
            StreamOverlayIconAndTextView(
                icon = "speedometer",
                text = speedAndTotal,
                textPlacement = textPlacement,
                color = statusColor,
                iconBackgroundColor = statusIconColor ?: backgroundColor,
            )
        }
    }
}

@Composable
private fun netStreamColor(model: Model = LocalModel.current): Color {
    val streamState = model.streamState
    return if (TODO("model.isStreaming()")) {
        when (streamState) {
            StreamState.connecting -> Color.White
            StreamState.connected -> Color.White
            StreamState.disconnected -> Color.Red
            else -> Color.White
        }
    } else {
        Color.White
    }
}

@Composable
private fun StreamUptimeStatusView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    streamUptime: StreamUptimeProvider,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val uptime by streamUptime.uptime.collectAsState()
    if (model.isShowingStatusStreamUptime()) {
        StreamOverlayIconAndTextView(
            icon = "deskclock",
            text = uptime,
            textPlacement = textPlacement,
            color = netStreamColor(model),
        )
    }
}

@Composable
private fun CpuStatusView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    systemMonitor: SystemMonitor,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    if (model.isShowingStatusCpu()) {
        if (textPlacement == hidePlacement) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(backgroundColor)
                    .clip(RoundedCornerShape(5.dp)),
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(17.dp)
                        .padding(start = 2.dp),
                )
                Text(
                    text = systemMonitor.formatShort(),
                    style = smallFont,
                    color = Color.White,
                    modifier = Modifier.padding(end = 2.dp),
                )
            }
        } else {
            StreamOverlayIconAndTextView(
                icon = "cpu",
                text = systemMonitor.format(),
                textPlacement = textPlacement,
            )
        }
    }
}

@Composable
private fun MoblinkStatusView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    moblink: Moblink,
    streamer: SettingsMoblinkStreamer,
    relay: SettingsMoblinkRelay,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val streamerOk by moblink.streamerOk.collectAsState()
    val status by moblink.status.collectAsState()

    fun color(): Color {
        if (model.isMoblinkRelayConfigured() && !model.areMoblinkRelaysOk()) {
            return Color.Red
        }
        if (!streamerOk) {
            return Color.Red
        }
        return Color.White
    }

    if (model.isShowingStatusMoblink()) {
        StreamOverlayIconAndTextView(
            icon = "app.connected.to.app.below.fill",
            text = status,
            textPlacement = textPlacement,
            color = color(),
        )
    }
}

@Composable
private fun RemoteControlStatusView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    status: StatusTopRight,
    streamer: SettingsRemoteControlStreamer,
    assistant: SettingsRemoteControlAssistant,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val remoteControlOk by status.remoteControlOk.collectAsState()
    val remoteControlStatus by status.remoteControlStatus.collectAsState()

    fun remoteControlColor(): Color {
        return if (remoteControlOk) {
            Color.White
        } else {
            Color.Red
        }
    }

    if (TODO("model.isShowingStatusRemoteControl()")) {
        StreamOverlayIconAndTextView(
            icon = "appletvremote.gen1",
            text = remoteControlStatus,
            textPlacement = textPlacement,
            color = remoteControlColor(),
        )
    }
}

@Composable
private fun DjiDevicesStatusView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    status: StatusTopRight,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val djiDevicesStatus by status.djiDevicesStatus.collectAsState()
    if (model.isShowingStatusDjiDevices()) {
        StreamOverlayIconAndTextView(
            icon = "appletvremote.gen1",
            text = djiDevicesStatus,
            textPlacement = textPlacement,
            color = Color.White,
        )
    }
}

@Composable
private fun GameControllersStatusView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    status: StatusTopRight,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val gameControllersTotal by status.gameControllersTotal.collectAsState()
    if (TODO("model.isShowingStatusGameController()")) {
        StreamOverlayIconAndTextView(
            icon = "gamecontroller",
            text = gameControllersTotal,
            textPlacement = textPlacement,
        )
    }
}

@Composable
private fun IngestsStatusView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    ingests: Ingests,
    rtmpServer: SettingsRtmpServer,
    srtlaServer: SettingsSrtlaServer,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val speedAndTotal by ingests.speedAndTotal.collectAsState()
    if (model.isShowingStatusIngests()) {
        StreamOverlayIconAndTextView(
            icon = "server.rack",
            text = speedAndTotal,
            textPlacement = textPlacement,
        )
    }
}

@Composable
private fun LocationStatusView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    location: com.moblin.android.various.settings.SettingsLocation,
    status: StatusTopRight,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val statusLocation by status.location.collectAsState()
    if (TODO("model.isShowingStatusLocation()")) {
        StreamOverlayIconAndTextView(
            icon = "location",
            text = statusLocation,
            textPlacement = textPlacement,
        )
    }
}

@Composable
private fun RecordingStatusView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    recording: RecordingProvider,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val length = recording.length
    if (model.isShowingStatusRecording()) {
        StreamOverlayIconAndTextView(
            icon = "record.circle",
            text = length,
            textPlacement = textPlacement,
        )
    }
}

@Composable
private fun BrowserWidgetsStatusView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    status: StatusTopRight,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val browserWidgetsStatus by status.browserWidgetsStatus.collectAsState()
    if (model.isShowingStatusBrowserWidgets()) {
        StreamOverlayIconAndTextView(
            icon = "globe",
            text = browserWidgetsStatus,
            textPlacement = textPlacement,
        )
    }
}

@Composable
private fun CatPrinterStatusView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    status: StatusTopRight,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val catPrinterStatus by status.catPrinterStatus.collectAsState()

    fun catPrinterColor(): Color {
        if (TODO("model.isAnyCatPrinterConfigured() && !model.areAllCatPrintersConnected()")) {
            return Color.Red
        }
        return Color.White
    }

    if (model.isShowingStatusCatPrinter()) {
        StreamOverlayIconAndTextView(
            icon = "pawprint",
            text = catPrinterStatus,
            textPlacement = textPlacement,
            color = catPrinterColor(),
        )
    }
}

@Composable
private fun WorkoutDeviceStatusView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    status: StatusTopRight,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val workoutDeviceStatus by status.workoutDeviceStatus.collectAsState()

    fun workoutDeviceColor(): Color {
        if (TODO("model.isAnyWorkoutDeviceConfigured() && !model.areAllWorkoutDevicesConnected()")) {
            return Color.Red
        }
        return Color.White
    }

    if (model.isShowingStatusWorkoutDevice()) {
        StreamOverlayIconAndTextView(
            icon = "figure.walk.motion",
            text = workoutDeviceStatus,
            textPlacement = textPlacement,
            color = workoutDeviceColor(),
        )
    }
}

@Composable
private fun FixedHorizonStatusView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    status: StatusTopRight,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val fixedHorizonStatus by status.fixedHorizonStatus.collectAsState()
    if (model.isShowingStatusFixedHorizon()) {
        StreamOverlayIconAndTextView(
            icon = "circle.and.line.horizontal",
            text = fixedHorizonStatus,
            textPlacement = textPlacement,
        )
    }
}

@Composable
private fun BlackSharkCoolerDeviceStatusView(
    show: SettingsShow,
    status: StatusTopRight,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val blackSharkCoolerDeviceState by status.blackSharkCoolerDeviceState.collectAsState()
    val blackSharkCoolerPhoneTemp by status.blackSharkCoolerPhoneTemp.collectAsState()
    val blackSharkCoolerExhaustTemp by status.blackSharkCoolerExhaustTemp.collectAsState()
    if (TODO("blackSharkCoolerDeviceState == BlackSharkCoolerDeviceState.connected")) {
        StreamOverlayIconAndTextView(
            icon = "fan",
            text = "${blackSharkCoolerPhoneTemp ?: 0} °C / ${blackSharkCoolerExhaustTemp ?: 0} °C",
            textPlacement = textPlacement,
        )
    }
}

@Composable
private fun AutoSceneSwitcherStatusInnerView(
    autoSceneSwitcher: SettingsAutoSceneSwitcher,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val name = autoSceneSwitcher.name
    StreamOverlayIconAndTextView(
        icon = "autostartstop",
        text = name,
        textPlacement = textPlacement,
    )
}

@Composable
private fun AutoSceneSwitcherStatusView(
    autoSceneSwitchers: SettingsAutoSceneSwitchers,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val switchers = autoSceneSwitchers.switchers
    val switcherId = autoSceneSwitchers.switcherId
    val autoSceneSwitcher = switchers.firstOrNull { it.id == switcherId }
    if (autoSceneSwitcher != null) {
        AutoSceneSwitcherStatusInnerView(
            autoSceneSwitcher = autoSceneSwitcher,
            textPlacement = textPlacement,
        )
    }
}

@Composable
private fun StatusesView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    status: StatusTopRight,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    AdsRemainingTimerView(
        model = model,
        status = model.statusTopRight,
        textPlacement = textPlacement,
    )
    IngestsStatusView(
        model = model,
        show = model.database.show,
        ingests = model.ingests,
        rtmpServer = model.database.rtmpServer,
        srtlaServer = model.database.srtlaServer,
        textPlacement = textPlacement,
    )
    MoblinkStatusView(
        model = model,
        show = model.database.show,
        moblink = model.moblink,
        streamer = model.database.moblink.streamer,
        relay = model.database.moblink.relay,
        textPlacement = textPlacement,
    )
    RemoteControlStatusView(
        model = model,
        show = model.database.show,
        status = model.statusTopRight,
        streamer = model.database.remoteControl.streamer,
        assistant = model.database.remoteControl.assistant,
        textPlacement = textPlacement,
    )
    DjiDevicesStatusView(
        model = model,
        show = model.database.show,
        status = model.statusTopRight,
        textPlacement = textPlacement,
    )
    GameControllersStatusView(
        model = model,
        show = model.database.show,
        status = model.statusTopRight,
        textPlacement = textPlacement,
    )
    BitrateStatusView(
        model = model,
        show = model.database.show,
        bitrate = model.bitrate,
        textPlacement = textPlacement,
    )
    BondingStatusView(
        show = model.database.show,
        bonding = model.bonding,
        textPlacement = textPlacement,
    )
    ReplayStatusView(
        show = model.database.show,
        replay = model.stream.value.replay,
        textPlacement = textPlacement,
    )
    StreamUptimeStatusView(
        show = model.database.show,
        streamUptime = model.streamUptime,
        textPlacement = textPlacement,
    )
    LocationStatusView(
        show = model.database.show,
        location = model.database.location,
        status = model.statusTopRight,
        textPlacement = textPlacement,
    )
    RecordingStatusView(
        show = model.database.show,
        recording = model.recording,
        textPlacement = textPlacement,
    )
    BrowserWidgetsStatusView(
        show = model.database.show,
        status = model.statusTopRight,
        textPlacement = textPlacement,
    )
    CatPrinterStatusView(
        show = model.database.show,
        status = model.statusTopRight,
        textPlacement = textPlacement,
    )
    WorkoutDeviceStatusView(
        show = model.database.show,
        status = model.statusTopRight,
        textPlacement = textPlacement,
    )
    FixedHorizonStatusView(
        model = model,
        show = model.database.show,
        status = model.statusTopRight,
        textPlacement = textPlacement,
    )
    BlackSharkCoolerDeviceStatusView(
        show = model.database.show,
        status = model.statusTopRight,
        textPlacement = textPlacement,
    )
    AutoSceneSwitcherStatusView(
        autoSceneSwitchers = model.database.autoSceneSwitchers,
        textPlacement = textPlacement,
    )
    CpuStatusView(
        model = model,
        show = model.database.show,
        systemMonitor = model.systemMonitor,
        textPlacement = textPlacement,
    )
    if (model.isShowingStatusAudioLevel() && textPlacement == hidePlacement) {
        CompactAudioBarView(audio = model.audio, level = model.audio.level)
    }
}

@Composable
private fun AudioView(
    model: Model = LocalModel.current,
    database: Database,
    show: SettingsShow,
) {
    val bigAudioLevelMeter = database.bigAudioLevelMeter
    if (model.isShowingStatusAudioLevel()) {
        AudioLevelView(model = model, big = bigAudioLevelMeter)
    }
}

@Composable
fun RightOverlayTopView(model: Model = LocalModel.current, database: Database) {
    val verboseStatuses = database.verboseStatuses
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier.clickable {
                model.toggleVerboseStatuses()
            },
        ) {
            if (verboseStatuses) {
                AudioView(model = model, database = database, show = database.show)
                StatusesView(
                    show = database.show,
                    status = model.statusTopRight,
                    textPlacement = beforeIconPlacement,
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusesView(
                        show = database.show,
                        status = model.statusTopRight,
                        textPlacement = hidePlacement,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RightOverlayBottomVerticalView(
    model: Model = LocalModel.current,
    database: Database,
    show: SettingsShow,
    streamOverlay: StreamOverlay,
    zoom: Zoom,
    width: Float,
) {
    val showMediaPlayerControls by streamOverlay.showMediaPlayerControls.collectAsState()
    val showingPixellate by streamOverlay.showingPixellate.collectAsState()
    val showingWhirlpool by streamOverlay.showingWhirlpool.collectAsState()
    val showingPinch by streamOverlay.showingPinch.collectAsState()
    val showingCamera by streamOverlay.showingCamera.collectAsState()
    val isTorchOn by streamOverlay.isTorchOn.collectAsState()
    val isFrontCameraSelected by streamOverlay.isFrontCameraSelected.collectAsState()
    val zoomPresets = show.zoomPresets
    val hasZoom by zoom.hasZoom.collectAsState()
    Row(verticalAlignment = Alignment.Bottom) {
        Spacer(modifier = Modifier.weight(1f))
        if (showMediaPlayerControls) {
            StreamOverlayRightMediaPlayerControlsView(mediaPlayer = model.mediaPlayerPlayer)
        } else {
            Column(horizontalAlignment = Alignment.End) {
                StreamOverlayRightFaceView(model = model, face = database.face)
                if (showingPixellate) {
                    StreamOverlayRightPixellateView(model = model, database = database)
                }
                if (showingWhirlpool) {
                    StreamOverlayRightWhirlpoolView(model = model, database = database)
                }
                if (showingPinch) {
                    StreamOverlayRightPinchView(model = model, database = database)
                }
                if (showingCamera) {
                    StreamOverlayRightCameraSettingsControlView(
                        model = model,
                        camera = model.camera,
                        show = model.camera.show,
                    )
                }
                if (isTorchOn && !isFrontCameraSelected) {
                    StreamOverlayRightTorchView(model = model, database = database)
                }
            }
            if (zoomPresets && hasZoom) {
                StreamOverlayRightZoomPresetVSelctorView(
                    model = model,
                    zoom = zoom,
                    width = width,
                )
            }
        }
        StreamOverlayRightSceneVSelectorView(
            database = database,
            sceneSelector = model.sceneSelector,
            width = width,
        )
    }
}

@Composable
private fun RightOverlayBottomHorizontalView(
    model: Model = LocalModel.current,
    database: Database,
    show: SettingsShow,
    streamOverlay: StreamOverlay,
    zoom: Zoom,
    width: Float,
) {
    val showMediaPlayerControls by streamOverlay.showMediaPlayerControls.collectAsState()
    val showingPixellate by streamOverlay.showingPixellate.collectAsState()
    val showingWhirlpool by streamOverlay.showingWhirlpool.collectAsState()
    val showingPinch by streamOverlay.showingPinch.collectAsState()
    val showingCamera by streamOverlay.showingCamera.collectAsState()
    val isTorchOn by streamOverlay.isTorchOn.collectAsState()
    val isFrontCameraSelected by streamOverlay.isFrontCameraSelected.collectAsState()
    val zoomPresets = show.zoomPresets
    val hasZoom by zoom.hasZoom.collectAsState()
    if (showMediaPlayerControls) {
        StreamOverlayRightMediaPlayerControlsView(mediaPlayer = model.mediaPlayerPlayer)
    } else {
        StreamOverlayRightFaceView(model = model, face = database.face)
        if (showingPixellate) {
            StreamOverlayRightPixellateView(model = model, database = database)
        }
        if (showingWhirlpool) {
            StreamOverlayRightWhirlpoolView(model = model, database = database)
        }
        if (showingPinch) {
            StreamOverlayRightPinchView(model = model, database = database)
        }
        if (showingCamera) {
            StreamOverlayRightCameraSettingsControlView(
                model = model,
                camera = model.camera,
                show = model.camera.show,
            )
        }
        if (isTorchOn && !isFrontCameraSelected) {
            StreamOverlayRightTorchView(model = model, database = database)
        }
        if (zoomPresets && hasZoom) {
            StreamOverlayRightZoomPresetSelctorView(
                model = model,
                zoom = zoom,
                width = width,
            )
        }
    }
    StreamOverlayRightSceneSelectorView(
        database = database,
        sceneSelector = model.sceneSelector,
        width = width,
    )
}

@Composable
fun RightOverlayBottomView(
    model: Model = LocalModel.current,
    database: Database,
    show: SettingsShow,
    streamOverlay: StreamOverlay,
    zoom: Zoom,
    width: Float,
) {
    val showDrawOnStream by model.showDrawOnStream.collectAsState()
    val appMode = database.appMode
    val showingReplay by streamOverlay.showingReplay.collectAsState()
    val showingBeauty by streamOverlay.showingBeauty.collectAsState()
    val showingVideoPreview by streamOverlay.showingVideoPreview.collectAsState()
    val zoomPresets = show.zoomPresets
    val hasZoom by zoom.hasZoom.collectAsState()
    val verticalButtons = database.verticalButtons
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        if (!showDrawOnStream && appMode == SettingsAppMode.streaming) {
            if (showingReplay) {
                StreamOverlayRightReplayView(
                    model = model,
                    replay = model.replay,
                    orientation = model.orientation,
                )
            } else if (showingBeauty) {
                StreamOverlayRightBeautyView(model = model, beauty = database.beauty)
            } else if (showingVideoPreview) {
                if (zoomPresets && hasZoom) {
                    StreamOverlayRightZoomPresetSelctorView(
                        model = model,
                        zoom = zoom,
                        width = width,
                    )
                }
                Column(modifier = Modifier.padding(bottom = 5.dp)) {
                    StreamOverlayRightSceneSelectorView(
                        database = database,
                        sceneSelector = model.sceneSelector,
                        width = width,
                    )
                }
                StreamOverlayRightVideoPreviewView(
                    model = model,
                    orientation = model.orientation,
                    videoPreview = model.videoPreview,
                )
            } else {
                if (verticalButtons) {
                    RightOverlayBottomVerticalView(
                        model = model,
                        database = database,
                        show = show,
                        streamOverlay = streamOverlay,
                        zoom = zoom,
                        width = width,
                    )
                } else {
                    RightOverlayBottomHorizontalView(
                        model = model,
                        database = database,
                        show = show,
                        streamOverlay = streamOverlay,
                        zoom = zoom,
                        width = width,
                    )
                }
            }
        }
    }
}
