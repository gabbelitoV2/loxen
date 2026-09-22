package com.moblin.android.various.model

import android.bluetooth.BluetoothAdapter
import android.graphics.Bitmap
import android.hardware.SensorManager
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.geometry.Size
import com.moblin.android.localized
import com.moblin.android.common.various.*
import com.moblin.android.integrations.*
import com.moblin.android.integrations.blacksharkcooler.*
import com.moblin.android.integrations.catprinter.*
import com.moblin.android.integrations.dji.djidevice.*
import com.moblin.android.integrations.gopro.*
import com.moblin.android.integrations.realtimeirl.*
import com.moblin.android.integrations.workoutdevice.*
import com.moblin.android.intents.*
import com.moblin.android.media.*
import com.moblin.android.media.adaptivebitrate.*
import com.moblin.android.media.haishinkit.media.*
import com.moblin.android.media.haishinkit.media.video.*
import com.moblin.android.media.haishinkit.rtmp.*
import com.moblin.android.media.ristserver.*
import com.moblin.android.media.rtmpserver.*
import com.moblin.android.media.rtspclient.*
import com.moblin.android.media.srtclient.*
import com.moblin.android.media.srtla.server.*
import com.moblin.android.media.webrtc.whepclient.*
import com.moblin.android.media.webrtc.whipserver.*
import com.moblin.android.moblink.*
import com.moblin.android.obs.*
import com.moblin.android.remotecontrol.*
import com.moblin.android.streamingplatforms.*
import com.moblin.android.streamingplatforms.kick.*
import com.moblin.android.streamingplatforms.openstreamingplatform.*
import com.moblin.android.streamingplatforms.soop.*
import com.moblin.android.streamingplatforms.twitch.*
import com.moblin.android.streamingplatforms.youtube.*
import com.moblin.android.various.*
import com.moblin.android.various.managers.*
import com.moblin.android.various.model.chat.*
import com.moblin.android.various.network.*
import com.moblin.android.various.settings.*
import com.moblin.android.various.storages.*
import com.moblin.android.various.subtitles.*
import com.moblin.android.various.utils.*
import com.moblin.android.videoeffects.*
import com.moblin.android.videoeffects.alerts.*
import com.moblin.android.videoeffects.replay.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

private sealed class BackgroundRunLevel {
    data object Full : BackgroundRunLevel()

    data class Service(
        val keepChatRunning: Boolean,
        val keepBatteryLevelRunning: Boolean,
    ) : BackgroundRunLevel()

    data object Off : BackgroundRunLevel()
}

enum class ShowingPanel {
    none,
    settings,
    bitrate,
    mic,
    streamSwitcher,
    luts,
    obs,
    sceneWidgets,
    store,
    chat,
    djiDevices,
    sceneSettings,
    goPro,
    connectionPriorities,
    autoSceneSwitcher,
    quickButtonSettings,
    streamingButtonSettings,
    live,
    macros;

    fun buttonsBackgroundColor(): Color = if (this == chat) Color.Black else Color(0xFFF2F2F7)
}

class Browser(var name: String, var browserEffect: BrowserEffect) {
    var id: UUID = UUID.randomUUID()
}

val fallbackStream = SettingsStream(name = "Fallback")
val flameRedMessage = localized("🔥 Flame is red 🔥")
val flameRedSubMessage = localized("Your device is hot and may overheat.")
val unknownSad = localized("Unknown 😢")
const val maxNotLoggedInToastCount = 10

private fun randomBuyIconsTitle(): String = listOf(
    localized("👍 Buy Moblin icons if you like the app 👍"),
    localized("🍔 Buy Moblin icons to support the devs 🍔"),
    localized("🙈 Buy Moblin icons to hide this message 🙈"),
    localized("🙏 Buy Moblin icons please =) 🙏"),
).random()

fun formatWarning(message: String): String = "⚠️ $message ⚠️"

val noMic = SettingsMicsMic()

data class QuickButtonPair(
    val id: UUID,
    val first: SettingsQuickButton,
    val second: SettingsQuickButton? = null,
) {
    override fun equals(other: Any?): Boolean = other is QuickButtonPair && id == other.id

    override fun hashCode(): Int = id.hashCode()
}

data class LogEntry(var id: Int, var message: String)

enum class AlertToastType { regular }

data class AlertToast(
    val type: AlertToastType,
    val title: String,
    val subTitle: String? = null,
    val titleColor: Color? = null,
    val titleFont: FontFamily? = null,
    val subTitleFont: FontFamily? = null,
)

enum class BatteryState { unknown, unplugged, charging, full }

enum class VideoOrientation { portrait, landscapeLeft, landscapeRight }

enum class WatchProtocolWorkoutType { running, cycling }

data class WatchProtocolWorkoutStats(
    val heartRate: Int? = null,
    val activeEnergyBurned: Int? = null,
    val distance: Int? = null,
    val stepCount: Int? = null,
    val power: Int? = null,
    val cyclingPower: Int? = null,
    val cyclingCadence: Int? = null,
)

private data class IngestStats(
    val anyServerEnabled: Boolean = false,
    val speed: Long = 0L,
    val total: Long = 0L,
    val numberOfClients: Int = 0,
)

class DebugOverlayProvider {
    internal val _debugLines = MutableStateFlow<List<String>>(emptyList())
    val debugLines = _debugLines.asStateFlow()
}

class StreamUptimeProvider {
    internal val _uptime = MutableStateFlow(noValue)
    val uptime = _uptime.asStateFlow()
}

class ProgressBar {
    internal val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()
    internal val _goal = MutableStateFlow(1f)
    val goal = _goal.asStateFlow()
}

class Banners {
    internal val _minimized = MutableStateFlow(false)
    val minimized = _minimized.asStateFlow()
}

class HypeTrain {
    internal val _level = MutableStateFlow<Int?>(null)
    val level = _level.asStateFlow()
    internal val _progress = MutableStateFlow<ProgressBar?>(null)
    val progress = _progress.asStateFlow()
    internal val _message = MutableStateFlow("")
    val message = _message.asStateFlow()
    var expiresAt: Instant? = null
}

enum class RaidState { idle, ongoing, cancelling, completed }

class Raid {
    internal val _state = MutableStateFlow(RaidState.idle)
    val state = _state.asStateFlow()
    internal val _channelImage = MutableStateFlow("")
    val channelImage = _channelImage.asStateFlow()
    internal val _channelLogin = MutableStateFlow("")
    val channelLogin = _channelLogin.asStateFlow()
    internal val _message = MutableStateFlow("")
    val message = _message.asStateFlow()
    internal val _progress = MutableStateFlow(ProgressBar())
    val progress = _progress.asStateFlow()
}

enum class TwitchPollState { idle, ongoing, completed }

data class TwitchPollChoice(val id: String, val title: String, val votes: Int)

class TwitchPoll {
    internal val _state = MutableStateFlow(TwitchPollState.idle)
    val state = _state.asStateFlow()
    internal val _title = MutableStateFlow("")
    val title = _title.asStateFlow()
    internal val _choices = MutableStateFlow<List<TwitchPollChoice>>(emptyList())
    val choices = _choices.asStateFlow()
    internal val _totalVotes = MutableStateFlow(0)
    val totalVotes = _totalVotes.asStateFlow()
    internal val _message = MutableStateFlow("")
    val message = _message.asStateFlow()
    var endsAt: Instant? = null
}

enum class TwitchPredictionState { idle, ongoing, locked, completed }

data class TwitchPredictionOutcome(
    val id: String,
    val title: String,
    val color: String,
    val users: Int,
    val channelPoints: Int,
    val winner: Boolean,
)

class TwitchPrediction {
    internal val _state = MutableStateFlow(TwitchPredictionState.idle)
    val state = _state.asStateFlow()
    internal val _title = MutableStateFlow("")
    val title = _title.asStateFlow()
    internal val _outcomes = MutableStateFlow<List<TwitchPredictionOutcome>>(emptyList())
    val outcomes = _outcomes.asStateFlow()
    internal val _totalChannelPoints = MutableStateFlow(0)
    val totalChannelPoints = _totalChannelPoints.asStateFlow()
    internal val _message = MutableStateFlow("")
    val message = _message.asStateFlow()
    var locksAt: Instant? = null
}

class Ingests {
    var rtmp: RtmpServer? = null
    var srtla: SrtlaServer? = null
    var srt: MutableList<SrtClient> = mutableListOf()
    var rist: RistServer? = null
    var rtsp: MutableList<RtspClient> = mutableListOf()
    var whip: WhipServer? = null
    var whep: MutableList<WhepClient> = mutableListOf()
    internal val _speedAndTotal = MutableStateFlow(noValue)
    val speedAndTotal = _speedAndTotal.asStateFlow()
}

class Bitrate {
    internal val _speedAndTotal = MutableStateFlow(noValue)
    val speedAndTotal = _speedAndTotal.asStateFlow()
    internal val _speedMbpsOneDecimal = MutableStateFlow(noValue)
    val speedMbpsOneDecimal = _speedMbpsOneDecimal.asStateFlow()
    internal val _statusColor = MutableStateFlow(Color.White)
    val statusColor = _statusColor.asStateFlow()
    internal val _statusIconColor = MutableStateFlow<Color?>(null)
    val statusIconColor = _statusIconColor.asStateFlow()
}

class Bonding {
    internal val _statistics = MutableStateFlow(noValue)
    val statistics = _statistics.asStateFlow()
    internal val _rtts = MutableStateFlow(noValue)
    val rtts = _rtts.asStateFlow()
    internal val _pieChartPercentages = MutableStateFlow<List<BondingPercentage>>(emptyList())
    val pieChartPercentages = _pieChartPercentages.asStateFlow()
    var statisticsFormatter = BondingStatisticsFormatter()
}

class Show {
    internal val _cameraPreview = MutableStateFlow(false)
    val cameraPreview = _cameraPreview.asStateFlow()
    internal val _chatPhone = MutableStateFlow(false)
    val chatPhone = _chatPhone.asStateFlow()
}

class Battery {
    internal val _level = MutableStateFlow(0.0)
    val level = _level.asStateFlow()
    internal val _state = MutableStateFlow(BatteryState.full)
    val state = _state.asStateFlow()
}

class StatusOther {
    internal val _ipStatuses = MutableStateFlow<List<IPMonitor.Status>>(emptyList())
    val ipStatuses = _ipStatuses.asStateFlow()
    internal val _thermalState = MutableStateFlow(getThermalState())
    val thermalState = _thermalState.asStateFlow()
    internal val _digitalClock = MutableStateFlow(noValue)
    val digitalClock = _digitalClock.asStateFlow()

    fun isConnectedToIpv4WiFi(): Boolean = _ipStatuses.value.any {
        it.interfaceType == IPMonitor.InterfaceType.wifi && it.ipType == IPMonitor.IpType.ipv4
    }
}

sealed class PlatformStatus {
    data class live(val viewerCount: Int) : PlatformStatus()

    data object unknown : PlatformStatus()

    data object offline : PlatformStatus()
}

data class StreamingPlatformStatus(val platform: Platform, val status: PlatformStatus)

data class ChatPlatformStatus(val platform: Platform, val connected: Boolean)

class StatusTopLeft {
    internal val _numberOfViewersIconColor = MutableStateFlow(Color.White)
    val numberOfViewersIconColor = _numberOfViewersIconColor.asStateFlow()
    internal val _numberOfViewersCompact = MutableStateFlow(noValue)
    val numberOfViewersCompact = _numberOfViewersCompact.asStateFlow()
    internal val _streamingPlatformStatuses = MutableStateFlow<List<StreamingPlatformStatus>>(emptyList())
    val streamingPlatformStatuses = _streamingPlatformStatuses.asStateFlow()
    internal val _chatPlatformStatuses = MutableStateFlow<List<ChatPlatformStatus>>(emptyList())
    val chatPlatformStatuses = _chatPlatformStatuses.asStateFlow()
    internal val _statusEventsText = MutableStateFlow(noValue)
    val statusEventsText = _statusEventsText.asStateFlow()
    internal val _statusChatText = MutableStateFlow(noValue)
    val statusChatText = _statusChatText.asStateFlow()
    internal val _streamText = MutableStateFlow(noValue)
    val streamText = _streamText.asStateFlow()
    internal val _statusCameraText = MutableStateFlow(noValue)
    val statusCameraText = _statusCameraText.asStateFlow()
    internal val _statusObsText = MutableStateFlow(noValue)
    val statusObsText = _statusObsText.asStateFlow()
}

class SystemMonitor {
    internal val _appCpu = MutableStateFlow(0)
    val appCpu = _appCpu.asStateFlow()
    internal val _cpu = MutableStateFlow(0)
    val cpu = _cpu.asStateFlow()
    internal val _ram = MutableStateFlow(0)
    val ram = _ram.asStateFlow()

    fun format(): String = "${_appCpu.value}%/${_cpu.value}% ${_ram.value} MB"

    fun formatShort(): String = _cpu.value.toString()
}

class StatusTopRight {
    internal val _browserWidgetsStatusChanged = MutableStateFlow(false)
    val browserWidgetsStatusChanged = _browserWidgetsStatusChanged.asStateFlow()
    internal val _remoteControlOk = MutableStateFlow(false)
    val remoteControlOk = _remoteControlOk.asStateFlow()
    internal val _remoteControlStatus = MutableStateFlow(noValue)
    val remoteControlStatus = _remoteControlStatus.asStateFlow()
    internal val _djiDevicesStatus = MutableStateFlow(noValue)
    val djiDevicesStatus = _djiDevicesStatus.asStateFlow()
    internal val _browserWidgetsStatus = MutableStateFlow(noValue)
    val browserWidgetsStatus = _browserWidgetsStatus.asStateFlow()
    internal val _catPrinterStatus = MutableStateFlow(noValue)
    val catPrinterStatus = _catPrinterStatus.asStateFlow()
    internal val _workoutDeviceStatus = MutableStateFlow(noValue)
    val workoutDeviceStatus = _workoutDeviceStatus.asStateFlow()
    internal val _fixedHorizonStatus = MutableStateFlow(noValue)
    val fixedHorizonStatus = _fixedHorizonStatus.asStateFlow()
    internal val _adsRemainingTimerStatus = MutableStateFlow(noValue)
    val adsRemainingTimerStatus = _adsRemainingTimerStatus.asStateFlow()
    internal val _blackSharkCoolerPhoneTemp = MutableStateFlow<Int?>(null)
    val blackSharkCoolerPhoneTemp = _blackSharkCoolerPhoneTemp.asStateFlow()
    internal val _blackSharkCoolerExhaustTemp = MutableStateFlow<Int?>(null)
    val blackSharkCoolerExhaustTemp = _blackSharkCoolerExhaustTemp.asStateFlow()
    internal val _blackSharkCoolerDeviceState = MutableStateFlow<BlackSharkCoolerDeviceState?>(null)
    val blackSharkCoolerDeviceState = _blackSharkCoolerDeviceState.asStateFlow()
    internal val _gameControllersTotal = MutableStateFlow(noValue)
    val gameControllersTotal = _gameControllersTotal.asStateFlow()
    internal val _djiDeviceStreamingState = MutableStateFlow<DjiDeviceState?>(null)
    val djiDeviceStreamingState = _djiDeviceStreamingState.asStateFlow()
    internal val _catPrinterState = MutableStateFlow<CatPrinterState?>(null)
    val catPrinterState = _catPrinterState.asStateFlow()
    internal val _workoutDeviceState = MutableStateFlow<WorkoutDeviceState?>(null)
    val workoutDeviceState = _workoutDeviceState.asStateFlow()
    internal val _location = MutableStateFlow(noValue)
    val location = _location.asStateFlow()
    internal val _isLowPowerMode = MutableStateFlow(false)
    val isLowPowerMode = _isLowPowerMode.asStateFlow()
}

class Toast {
    internal val _showingToast = MutableStateFlow(false)
    val showingToast = _showingToast.asStateFlow()
    internal val _toast = MutableStateFlow(AlertToast(type = AlertToastType.regular, title = ""))
    val toast = _toast.asStateFlow()
    var onTapped: (() -> Unit)? = null
}

class SceneSelector {
    internal val _trigger = MutableStateFlow(0)
    val trigger = _trigger.asStateFlow()
    internal val _sceneIndex = MutableStateFlow(0)
    val sceneIndex = _sceneIndex.asStateFlow()
    var selectedSceneId = UUID.randomUUID()
}

class StreamOverlay {
    internal val _showMediaPlayerControls = MutableStateFlow(false)
    val showMediaPlayerControls = _showMediaPlayerControls.asStateFlow()
    internal val _isFrontCameraSelected = MutableStateFlow(false)
    val isFrontCameraSelected = _isFrontCameraSelected.asStateFlow()
    internal val _showingCamera = MutableStateFlow(false)
    val showingCamera = _showingCamera.asStateFlow()
    internal val _showingPinch = MutableStateFlow(false)
    val showingPinch = _showingPinch.asStateFlow()
    internal val _showingReplay = MutableStateFlow(false)
    val showingReplay = _showingReplay.asStateFlow()
    internal val _showingPixellate = MutableStateFlow(false)
    val showingPixellate = _showingPixellate.asStateFlow()
    internal val _showingWhirlpool = MutableStateFlow(false)
    val showingWhirlpool = _showingWhirlpool.asStateFlow()
    internal val _showingBeauty = MutableStateFlow(false)
    val showingBeauty = _showingBeauty.asStateFlow()
    internal val _showingVideoPreview = MutableStateFlow(false)
    val showingVideoPreview = _showingVideoPreview.asStateFlow()
    internal val _isTorchOn = MutableStateFlow(false)
    val isTorchOn = _isTorchOn.asStateFlow()
}

class Store {
    internal val _myIcons = MutableStateFlow<List<Icon>>(emptyList())
    val myIcons = _myIcons.asStateFlow()
    internal val _iconsInStore = MutableStateFlow<List<Icon>>(emptyList())
    val iconsInStore = _iconsInStore.asStateFlow()
    internal val _iconImage = MutableStateFlow(plainIcon.id)
    val iconImage = _iconImage.asStateFlow()
    var hasBoughtSomething: Boolean = true
}

class DrawOnStream {
    internal val _lines = MutableStateFlow<List<DrawOnStreamLine>>(emptyList())
    val lines = _lines.asStateFlow()
    internal val _selectedColor = MutableStateFlow(Color.Pink)
    val selectedColor = _selectedColor.asStateFlow()
    internal val _selectedWidth = MutableStateFlow(4f)
    val selectedWidth = _selectedWidth.asStateFlow()
}

class StealthMode {
    var hideButtonsTimer = MainTimer()
    internal val _showButtons = MutableStateFlow(true)
    val showButtons = _showButtons.asStateFlow()
    internal val _image = MutableStateFlow<Bitmap?>(null)
    val image = _image.asStateFlow()
}

class ControlBar {
    internal val _backgroundImage = MutableStateFlow<Bitmap?>(null)
    val backgroundImage = _backgroundImage.asStateFlow()
    internal val _backgroundImageOpacity = MutableStateFlow(1.0)
    val backgroundImageOpacity = _backgroundImageOpacity.asStateFlow()
}

class QuickButtonChat {
    internal val _showAllChatMessages = MutableStateFlow(true)
    val showAllChatMessages = _showAllChatMessages.asStateFlow()
    internal val _showFirstTimeChatterMessage = MutableStateFlow(true)
    val showFirstTimeChatterMessage = _showFirstTimeChatterMessage.asStateFlow()
    internal val _showNewFollowerMessage = MutableStateFlow(true)
    val showNewFollowerMessage = _showNewFollowerMessage.asStateFlow()
    internal val _chatAlertsPosts = MutableStateFlow<ArrayDeque<ChatPost>>(ArrayDeque())
    val chatAlertsPosts = _chatAlertsPosts.asStateFlow()
    internal val _pausedChatAlertsPostsCount = MutableStateFlow(0)
    val pausedChatAlertsPostsCount = _pausedChatAlertsPostsCount.asStateFlow()
    internal val _chatAlertsPaused = MutableStateFlow(false)
    val chatAlertsPaused = _chatAlertsPaused.asStateFlow()
}

class ExternalDisplay {
    internal val _chatEnabled = MutableStateFlow(false)
    val chatEnabled = _chatEnabled.asStateFlow()
}

class GoProState {
    internal val _launchLiveStreamSelection = MutableStateFlow<UUID?>(null)
    val launchLiveStreamSelection = _launchLiveStreamSelection.asStateFlow()
    internal val _wifiCredentialsSelection = MutableStateFlow<UUID?>(null)
    val wifiCredentialsSelection = _wifiCredentialsSelection.asStateFlow()
    internal val _rtmpUrlSelection = MutableStateFlow<UUID?>(null)
    val rtmpUrlSelection = _rtmpUrlSelection.asStateFlow()
}

class QuickButtons {
    internal val _pairs = MutableStateFlow<List<List<QuickButtonPair>>>(List(controlBarPages) { emptyList() })
    val pairs = _pairs.asStateFlow()
    internal val _selectedButtonType = MutableStateFlow<SettingsQuickButtonType?>(null)
    val selectedButtonType = _selectedButtonType.asStateFlow()
    var page = 1
    internal val _activePage = MutableStateFlow<Int?>(1)
    val activePage = _activePage.asStateFlow()
}

class Snapshot {
    internal val _countdown = MutableStateFlow(0)
    val countdown = _countdown.asStateFlow()
    internal val _currentJob = MutableStateFlow<SnapshotJob?>(null)
    val currentJob = _currentJob.asStateFlow()
}

class Orientation {
    internal val _isPortrait = MutableStateFlow(false)
    val isPortrait = _isPortrait.asStateFlow()
}

class CameraLevel {
    internal val _angle = MutableStateFlow<Double?>(null)
    val angle = _angle.asStateFlow()

    fun start(portrait: Boolean) {
        TODO("no Android counterpart for CMMotionManager.startDeviceMotionUpdates; use SensorManager TYPE_GRAVITY")
    }

    fun stop() {
        TODO("no Android counterpart for CMMotionManager.stopDeviceMotionUpdates")
    }
}

private val enterForegroundCountStorage = SimpleIntStorage(key = "enterForegroundCount")

class Model : FaxReceiverDelegate, AlertsEffectDelegate {
    var enterForegroundCount: Int
        get() = enterForegroundCountStorage.get()
        set(value) = enterForegroundCountStorage.set(value)

    internal val _showingPanel = MutableStateFlow(ShowingPanel.none)
    val showingPanel = _showingPanel.asStateFlow()
    internal val _panelHidden = MutableStateFlow(false)
    val panelHidden = _panelHidden.asStateFlow()
    internal val _showStealthMode = MutableStateFlow(false)
    val showStealthMode = _showStealthMode.asStateFlow()
    internal val _lockScreen = MutableStateFlow(false)
    val lockScreen = _lockScreen.asStateFlow()
    internal val _isLive = MutableStateFlow(false)
    val isLive = _isLive.asStateFlow()
    internal val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()
    internal val _isPreviewStreaming = MutableStateFlow(false)
    val isPreviewStreaming = _isPreviewStreaming.asStateFlow()
    internal val _browsers = MutableStateFlow<List<Browser>>(emptyList())
    val browsers = _browsers.asStateFlow()
    internal val _interactiveBrowsers = MutableStateFlow(false)
    val interactiveBrowsers = _interactiveBrowsers.asStateFlow()
    internal val _showingGrid = MutableStateFlow(false)
    val showingGrid = _showingGrid.asStateFlow()
    internal val _showingCameraLevel = MutableStateFlow(false)
    val showingCameraLevel = _showingCameraLevel.asStateFlow()
    internal val _showingRemoteControl = MutableStateFlow(false)
    val showingRemoteControl = _showingRemoteControl.asStateFlow()
    internal val _portraitVideoOffsetFromTop = MutableStateFlow(0.0)
    val portraitVideoOffsetFromTop = _portraitVideoOffsetFromTop.asStateFlow()
    internal val _currentStreamId = MutableStateFlow(UUID.randomUUID())
    val currentStreamId = _currentStreamId.asStateFlow()
    internal val _showTwitchAuth = MutableStateFlow(false)
    val showTwitchAuth = _showTwitchAuth.asStateFlow()
    internal val _showModerationAuth = MutableStateFlow(false)
    val showModerationAuth = _showModerationAuth.asStateFlow()
    internal val _presentingModeration = MutableStateFlow(false)
    val presentingModeration = _presentingModeration.asStateFlow()
    internal val _presentingPredefinedMessages = MutableStateFlow(false)
    val presentingPredefinedMessages = _presentingPredefinedMessages.asStateFlow()
    internal val _presentingSettingsImportConfirmation = MutableStateFlow(false)
    val presentingSettingsImportConfirmation = _presentingSettingsImportConfirmation.asStateFlow()
    var pendingSettingsImportAction: (() -> Unit)? = null
    internal val _presentingStreamImportCollisionConfirmation = MutableStateFlow(false)
    val presentingStreamImportCollisionConfirmation =
        _presentingStreamImportCollisionConfirmation.asStateFlow()
    var pendingStreamImportCollisionAction: ((replaceExisting: Boolean) -> Unit)? = null
    var pendingStreamImportCollisionTitle: String = ""
    internal val _showDrawOnStream = MutableStateFlow(false)
    val showDrawOnStream = _showDrawOnStream.asStateFlow()
    internal val _showLocalOverlays = MutableStateFlow(true)
    val showLocalOverlays = _showLocalOverlays.asStateFlow()
    internal val _showBrowser = MutableStateFlow(false)
    val showBrowser = _showBrowser.asStateFlow()
    internal val _showNavigation = MutableStateFlow(false)
    val showNavigation = _showNavigation.asStateFlow()
    internal val _webBrowserUrl = MutableStateFlow("")
    val webBrowserUrl = _webBrowserUrl.asStateFlow()
    internal val _quickButtonSettingsButton = MutableStateFlow<SettingsQuickButton?>(null)
    val quickButtonSettingsButton = _quickButtonSettingsButton.asStateFlow()
    internal val _bluetoothAllowed = MutableStateFlow(false)
    val bluetoothAllowed = _bluetoothAllowed.asStateFlow()
    internal val _sceneSettingsPanelSceneId = MutableStateFlow(1)
    val sceneSettingsPanelSceneId = _sceneSettingsPanelSceneId.asStateFlow()
    internal val _cameraControlEnabled = MutableStateFlow(false)
    val cameraControlEnabled = _cameraControlEnabled.asStateFlow()
    internal val _stream = MutableStateFlow<SettingsStream>(fallbackStream)
    val stream = _stream.asStateFlow()
    internal val _layout = MutableStateFlow<SettingsWidgetLayout?>(null)
    val layout = _layout.asStateFlow()
    internal val _workoutType = MutableStateFlow<WatchProtocolWorkoutType?>(null)
    val workoutType = _workoutType.asStateFlow()
    internal val _photoShootEnabled = MutableStateFlow(false)
    val photoShootEnabled = _photoShootEnabled.asStateFlow()

    var streamState: StreamState = StreamState.disconnected
        set(value) {
            Log.i("Model", "stream: State $field -> $value")
            field = value
        }

    var defaultMic: SettingsMicsMic = noMic
        set(value) {
            field = value
            database.mics.defaultMic = value.id
        }

    var activeBufferedVideoIds: MutableSet<UUID> = mutableSetOf()
    var wiFiAwareSenderTask: Job? = null
    var wiFiAwareReceiverTask: Job? = null
    val youTube = YouTube()
    val webBrowserState = WebBrowserState()
    val cameraLevel = CameraLevel()
    val orientation = Orientation()
    val snapshot = Snapshot()
    val quickButtons = QuickButtons()
    val mic = Mic()
    val goPro = GoProState()
    val obsQuickButton = QuickButtonObs()
    val streamingHistory = StreamingHistory()
    val quickButtonChatState = QuickButtonChat()
    val externalDisplay = ExternalDisplay()
    val tesla = Tesla()
    val debugOverlay = DebugOverlayProvider()
    val stealthMode = StealthMode()
    val controlBar = ControlBar()
    var faceBackgroundImage: Bitmap? = null
    val drawOnStream = DrawOnStream()
    val store = Store()
    val show = Show()
    val streamOverlay = StreamOverlay()
    val sceneSelector = SceneSelector()
    val toast = Toast()
    val statusOther = StatusOther()
    val statusTopLeft = StatusTopLeft()
    val systemMonitor = SystemMonitor()
    val statusTopRight = StatusTopRight()
    val battery = Battery()
    val remoteControl = RemoteControl()
    val createStreamWizard = CreateStreamWizard()
    val createWidgetWizard = CreateWidgetWizard()
    val zoom = Zoom()
    val camera = CameraState()
    val mediaPlayerPlayer = MediaPlayerPlayer()
    lateinit var media: Media
    val banners = Banners()
    val hypeTrain = HypeTrain()
    val raid = Raid()
    val twitchPoll = TwitchPoll()
    val twitchPrediction = TwitchPrediction()
    val moblink = Moblink()
    val ingests = Ingests()
    val bitrate = Bitrate()
    val bonding = Bonding()
    var currentFps: Int? = null
    var currentResolution: String? = null
    var lowLightBoost = false
    var showBackgroundStreamingDisabledToast = false
    private var manualFocusMotionAttitude: Any? = null
    var streaming = false
    var inServiceBackground = false
    var chatPhoneBackgroundAudioPlayer: AudioPlayer? = null
    var liveActivity: Any? = null
    var macStatusItem: Any? = null
    var streamStartTime: Instant? = null
    var isRecorderRecording = false
    var currentRecording: Recording? = null
    val recording = RecordingProvider()
    var streamUptime = StreamUptimeProvider()
    val audio = AudioProvider()
    var inputGainObservation: Any? = null
    var settings = Settings()
    var twitchChat: TwitchChat? = null
    var twitchEventSub: TwitchEventSub? = null
    var kickPusher: KickPusher? = null
    var kickPlatformStatus: KickPlatformStatus? = null
    var youTubeLiveChats: MutableMap<String, YouTubeLiveChat> = mutableMapOf()
    var soopChat: SoopChat? = null
    var soopPlatformStatus: SoopPlatformStatus? = null
    private var openStreamingPlatformChat: OpenStreamingPlatformChat? = null
    var youTubeFetchVideoIdStartTime: Instant? = null
    var youTubePlatformStatus: PlatformStatus = PlatformStatus.unknown
    var youTubeStreamUpdateTimePollDelta: Duration = Duration.ofSeconds(15)
    var youTubeStreamUpdateTime = Instant.now()
    var obsWebSocket: ObsWebSocket? = null
    var chatPostId = 0
    val chat = ChatProvider(maximumNumberOfMessages = maximumNumberOfChatMessages)
    val chatActivityFeed = ChatProvider(maximumNumberOfMessages = maximumNumberOfChatMessages)
    val quickButtonChat = ChatProvider(maximumNumberOfMessages = maximumNumberOfInteractiveChatMessages)
    val externalDisplayChat = ChatProvider(maximumNumberOfMessages = 50)
    val chatWidgetChat = ChatProvider(maximumNumberOfMessages = 5)
    private var externalDisplayWindow: Any? = null
    var chatBotMessages: ArrayDeque<ChatBotMessage> = ArrayDeque()
    var newQuickButtonChatAlertsPosts: ArrayDeque<ChatPost> = ArrayDeque()
    var pausedQuickButtonChatAlertsPosts: ArrayDeque<ChatPost> = ArrayDeque()
    var watchChatPosts: ArrayDeque<Any> = ArrayDeque()
    var nextWatchChatPostId = 1
    var previousBitrateStatusColorSrtDroppedPacketsTotal: Int = 0
    var previousBitrateStatusNumberOfFailedEncodings = 0
    val streamPreviewView = PreviewView()
    val externalDisplayStreamPreviewView = PreviewView()
    val cameraPreviewView = CameraPreviewUiView()
    val videoPreview = VideoPreviewProvider()
    var pipController: Any? = null
    var textEffects: MutableMap<UUID, TextEffect> = mutableMapOf()
    var imageEffects: MutableMap<UUID, ImageEffect> = mutableMapOf()
    var browserEffects: MutableMap<UUID, BrowserEffect> = mutableMapOf()
    var lutEffects: MutableMap<UUID, LutEffect> = mutableMapOf()
    var mapEffects: MutableMap<UUID, MapEffect> = mutableMapOf()
    var qrCodeEffects: MutableMap<UUID, QrCodeEffect> = mutableMapOf()
    var alertsEffects: MutableMap<UUID, AlertsEffect> = mutableMapOf()
    var videoSourceEffects: MutableMap<UUID, VideoSourceEffect> = mutableMapOf()
    var enabledAlertsEffects: MutableList<AlertsEffect> = mutableListOf()
    var drawOnStreamEffect = DrawOnStreamEffect()
    var lutEffect = LutEffect()
    var scoreboardEffects: MutableMap<UUID, ScoreboardEffect> = mutableMapOf()
    var vTuberEffects: MutableMap<UUID, VTuberEffect> = mutableMapOf()
    var pngTuberEffects: MutableMap<UUID, PngTuberEffect> = mutableMapOf()
    var snapshotEffects: MutableMap<UUID, SnapshotEffect> = mutableMapOf()
    var chatEffects: MutableMap<UUID, ChatEffect> = mutableMapOf()
    var chatEmoteComboEffects: MutableMap<UUID, ChatEmoteComboEffect> = mutableMapOf()
    var slideshowEffects: MutableMap<UUID, SlideshowEffect> = mutableMapOf()
    var wheelOfLuckEffects: MutableMap<UUID, WheelOfLuckEffect> = mutableMapOf()
    var bingoCardEffects: MutableMap<UUID, BingoCardEffect> = mutableMapOf()
    var pomodoroTimerEffects: MutableMap<UUID, PomodoroTimerEffect> = mutableMapOf()
    var pomodoroAudioPlayer: AudioPlayer? = null
    var enabledSnapshotEffects: MutableList<SnapshotEffect> = mutableListOf()
    var enabledChatEffects: MutableList<ChatEffect> = mutableListOf()
    var enabledChatEmoteComboEffects: MutableList<ChatEmoteComboEffect> = mutableListOf()
    var speechToTextAlertMatchOffset = 0
    var log: ArrayDeque<LogEntry> = ArrayDeque()
    var remoteControlAssistantLog: ArrayDeque<LogEntry> = ArrayDeque()
    val imageStorage = ImageStorage()
    var replayTransitionsStorage = ReplayTransitionsStorage()
    val logsStorage = LogsStorage()
    val mediaStorage = MediaPlayerStorage()
    val alertMediaStorage = AlertMediaStorage()
    val vTuberStorage = VTuberStorage()
    val pngTuberStorage = PngTuberStorage()
    val reconnectTimer = MainTimer()
    var logId = 1
    private var serversSpeed: Long = 0
    var adsEndDate: Instant? = null
    var heartRates: MutableMap<String, Int?> = mutableMapOf()
    var runningMetrics: MutableMap<String, WorkoutDeviceRunningMetrics> = mutableMapOf()
    var workoutActiveEnergyBurned: Int? = null
    var workoutDistance: Int? = null
    var workoutPower: Int? = null
    var workoutStepCount: Int? = null
    private var pollVotes: MutableList<Int> = mutableListOf(0, 0, 0)
    var pollEnabled = false
    var mediaPlayers: MutableMap<UUID, MediaPlayer> = mutableMapOf()
    var previousSrtDroppedPacketsTotal: Int = 0
    var streamBecameBrokenTime: Instant? = null
    var cameraPosition: Int? = null
    private var motionManager: SensorManager? = null
    var gForceManager: GForceManager? = null
    val database: Database
        get() = settings.database

    var speechToText: SpeechToText? = null
    val twitchAuth = TwitchAuth()
    var twitchAuthOnComplete: ((accessToken: String) -> Unit)? = null
    var kickAuthOnComplete: ((accessToken: String) -> Unit)? = null
    var twitchPlatformStatus: PlatformStatus = PlatformStatus.unknown
    val twitchSearchCategoriesTimer = MainTimer()
    val twitchSearchChannelsTimer = MainTimer()
    val kickSearchCategoriesTimer = MainTimer()
    val kickSearchChannelsTimer = MainTimer()
    var drawOnStreamSize: Size = Size.Zero
    var webBrowser: Any? = null
    val webBrowserController = WebBrowserController()
    var lowFpsImageFps: Long = 1
    val chatTextToSpeech = ChatTextToSpeech()
    private var lastAttachCompletedTime: Instant? = null
    private var relaxedBitrateStartTime: Instant? = null
    var relaxedBitrate = false
    var remoteControlAssistantStreamerState = RemoteControlAssistantStreamerState()
    var remoteControlStreamer: RemoteControlStreamer? = null
    var remoteControlAssistant: RemoteControlAssistant? = null
    var remoteControlRelay: RemoteControlRelay? = null
    var remoteControlWeb: RemoteControlWeb? = null
    var isRemoteControlAssistantRequestingPreview = false
    var isRemoteControlAssistantRequestingStatus = false
    var isRemoteControlAssistantRequestingStats = false
    var isRemoteControlWebRequestingPreview = false
    var remoteControlAssistantRequestingStatusFilter: RemoteControlStartStatusFilter? = null
    var remoteControlAssistantRequestingStatsFilter = RemoteControlStartStatsFilter()
    var remoteControlAssistantPreviewUsers: MutableSet<RemoteControlAssistantPreviewUser> = mutableSetOf()
    var remoteControlAssistantStatusRequested: Boolean = false
    var remoteControlStreamerLatestReceivedChatMessageId = -1
    var useRemoteControlForChatAndEvents = false
    var currentWiFiSsid: String? = null
    var currentDjiDeviceSettings: SettingsDjiDevice? = null
    var djiDevices: MutableMap<UUID, DjiDevice> = mutableMapOf()
    var goProDevices: MutableMap<UUID, GoProDevice> = mutableMapOf()
    val autoSceneSwitcher = AutoSceneSwitcherProvider()
    var currentCatPrinterSettings: SettingsCatPrinter? = null
    var catPrinters: MutableMap<UUID, CatPrinter> = mutableMapOf()
    var cyclingPower = 0
    var cyclingCadence = 0
    var latestCyclingPower: CyclingSampleInfo? = null
    var latestCyclingCadence: CyclingSampleInfo? = null
    var cyclingSpeed = 0.0
    var latestSubscriber = ""
    var latestFollower = ""
    private val periodicTimer20ms = MainTimer()
    private val periodicTimer200ms = MainTimer()
    private val periodicTimer1s = MainTimer()
    private val periodicTimer3s = MainTimer()
    private val periodicTimer5s = MainTimer()
    private val periodicTimer10s = MainTimer()
    private val periodicTimerBatteryLevel = MainTimer()
    var currentWorkoutDeviceSettings: SettingsWorkoutDevice? = null
    var workoutDevices: MutableMap<UUID, WorkoutDevice> = mutableMapOf()
    var blackSharkCoolerDevices: MutableMap<UUID, BlackSharkCoolerDevice> = mutableMapOf()
    var cameraDevice: CaptureDevice? = null
    var cameraZoomLevelToXScale: Float = 1.0f
    var cameraZoomXMinimum: Float = 1.0f
    var cameraZoomXMaximum: Float = 1.0f
    var latestDebugLines: List<String> = emptyList()
    var latestDebugActions: List<String> = emptyList()
    var streamingHistoryStream: StreamingHistoryStream? = null
    var backCameras: MutableList<Camera> = mutableListOf()
    var frontCameras: MutableList<Camera> = mutableListOf()
    var externalCameras: MutableList<Camera> = mutableListOf()
    var recordingsStorage = RecordingsStorage()
    var recordingThumbnailsCache: MutableMap<String, ByteArray> = mutableMapOf()
    var latestLowBitrateTime = Instant.now()
    var bluetoothCentralManger: BluetoothAdapter? = null
    var sceneSettingsPanelScene = SettingsScene(name = "")
    var snapshotJobs: ArrayDeque<SnapshotJob> = ArrayDeque()
    var gameControllers: MutableList<Any?> = mutableListOf()
    var moveToGimbalPresetQueue: ArrayDeque<UUID> = ArrayDeque()
    var moveToGimbalPresetQueueRunning = false
    var gimbalPresetLongPressTimers: MutableMap<String, MainTimer> = mutableMapOf()
    var latestKnownLocation: Location? = null
    var slopePercent = 0.0
    var previousSlopeAltitude: Double? = 0.0
    var previousSlopeDistance = 0.0
    var altitudeReference: Double? = null
    var averageSpeed = 0.0
    var averageSpeedStartTime: Instant = Instant.now()
    var averageSpeedStartDistance = 0.0
    val replaysStorage = ReplaysStorage()
    var replaySettings: ReplaySettings? = null
    var replayFrameExtractor: ReplayFrameExtractor? = null
    var replayVideo: ReplayBufferFile? = null
    var replayBuffer = ReplayBuffer()
    val replay = ReplayProvider()
    private var sampleBufferReceiver: Any? = null
    val faxReceiver = FaxReceiver()
    var twitchStreamUpdateTime = Instant.now()
    var externalDisplayPreview = false
    var remoteSceneScenes: MutableList<SettingsScene> = mutableListOf()
    var remoteSceneWidgets: MutableList<SettingsWidget> = mutableListOf()
    var remoteSceneData = RemoteControlRemoteSceneData(textStats = null, location = null)
    var remoteSceneSettingsUpdateRequested = false
    var remoteSceneSettingsUpdating = false
    var builtinCameraIds: MutableMap<String, UUID> = mutableMapOf()
    var isAppActive = true
    var initialVolume: Float? = null
    var latestVolumeChangeSequenceNumber: Int? = null
    val volumeView: Any? = null
    var latestSetVolumeTime = Instant.now()
    private var appStoreUpdateListenerTask: Job? = null
    var products: MutableMap<String, Any> = mutableMapOf()
    var streamTotalBytes: Long = 0
    var fileLog = createFileLog()
    private var ipMonitor = IPMonitor()
    var faceEffect = FaceEffect()
    var movieEffect = MovieEffect()
    var whirlpoolEffect = WhirlpoolEffect(angle = (Math.PI / 2).toFloat())
    var pinchEffect = PinchEffect(scale = 0.5f)
    var fourThreeEffect = FourThreeEffect()
    var crtEffect = CrtEffect()
    var grayScaleEffect = GrayScaleEffect()
    var sepiaEffect = SepiaEffect()
    var tripleEffect = TripleEffect()
    var twinEffect = TwinEffect()
    var pixellateEffect = PixellateEffect(strength = 0.0f)
    var cameraManEffect = CameraManEffect(moveVertically = false, speed = 1f, alwaysMove = false)
    var pollEffect: PollEffect? = null
    var fixedHorizonEffect = FixedHorizonEffect()
    var glassesEffect: AlertsEffect? = null
    var sparkleEffect: AlertsEffect? = null
    var beautyEffect = BeautyEffect(fps = 30)
    var replayEffect: ReplayEffect? = null
    var locationManager = Location()
    var realtimeIrl: RealtimeIrl? = null
    var supportsAppleLog: Boolean = false
    val weatherManager = WeatherManager()
    val geographyManager = GeographyManager()
    var onDocumentPickerUrl: ((String) -> Unit)? = null
    var healthStore: Any? = null
    private val resourceUsage = ResourceUsage()
    var speechToTextLatestPosition: Int? = null
    var speechToTextLatestText: String? = null
    var speechToTextTextAligners: MutableMap<String?, TextAligner> = mutableMapOf()
    var httpProxyServer: HttpProxyServer? = null
    var httpProxyPort: Int? = null
    val streamDeck = StreamDeck()
    val photoShootTimer = MainTimer()

    var processor: Processor? = null
        set(value) {
            field?.setDrawable(null)
            field = value
        }

    init {
        settings.load()
        streamingHistory.load()
        replaysStorage.load()
        setCurrentStream()
        updateIsPortrait()
        updateOrientationLock()
    }

    fun updateIsPortrait() {
        orientation._isPortrait.value = _stream.value.portrait || database.portrait || isChatPhone()
    }

    fun isLandscapeStreamAndPortraitUi(): Boolean = !_stream.value.portrait && database.portrait

    val enabledScenes: List<SettingsScene>
        get() = database.scenes.filter { it.enabled }

    fun setAdaptiveBitrateSrtAlgorithm(stream: SettingsStream) {
        media.srtSetAdaptiveBitrateAlgorithm(
            targetBitrate = stream.bitrate,
            adaptiveBitrateAlgorithm = stream.srt.adaptiveBitrate.algorithm,
        )
    }

    fun updateAdaptiveBitrateSrt(srt: SettingsStreamSrt) {
        when (srt.adaptiveBitrate.algorithm) {
            SettingsStreamSrtAdaptiveBitrateAlgorithm.fastIrl -> {
                var settings = adaptiveBitrateFastSettings
                settings.packetsInFlight = srt.adaptiveBitrate.fastIrlSettings.packetsInFlight.toLong()
                settings.minimumBitrate =
                    (srt.adaptiveBitrate.fastIrlSettings.minimumBitrate * 1000).toLong()
                media.setAdaptiveBitrateSettings(settings = settings)
            }
            SettingsStreamSrtAdaptiveBitrateAlgorithm.slowIrl -> {
                media.setAdaptiveBitrateSettings(settings = adaptiveBitrateSlowSettings)
            }
            SettingsStreamSrtAdaptiveBitrateAlgorithm.customIrl -> {
                val customSettings = srt.adaptiveBitrate.customSettings
                media.setAdaptiveBitrateSettings(
                    settings = AdaptiveBitrateSettings(
                        packetsInFlight = customSettings.packetsInFlight.toLong(),
                        rttDiffHighFactor = customSettings.rttDiffHighDecreaseFactor.toDouble(),
                        rttDiffHighAllowedSpike = customSettings.rttDiffHighAllowedSpike.toDouble(),
                        rttDiffHighMinDecrease =
                            (customSettings.rttDiffHighMinimumDecrease * 1000).toLong(),
                        pifDiffIncreaseFactor = (customSettings.pifDiffIncreaseFactor * 1000).toLong(),
                        minimumBitrate = (customSettings.minimumBitrate * 1000).toLong(),
                    ),
                )
            }
            SettingsStreamSrtAdaptiveBitrateAlgorithm.belabox -> {
                var settings = adaptiveBitrateBelaboxSettings
                settings.minimumBitrate =
                    (srt.adaptiveBitrate.belaboxSettings.minimumBitrate * 1000).toLong()
                media.setAdaptiveBitrateSettings(settings = settings)
            }
        }
    }

    fun updateAdaptiveBitrateRtmpIfEnabled() {
        var settings = adaptiveBitrateFastSettings
        settings.rttDiffHighAllowedSpike = 500
        media.setAdaptiveBitrateSettings(settings = settings)
    }

    fun updateAdaptiveBitrateRistIfEnabled() {
        val settings = adaptiveBitrateRistFastSettings
        media.setAdaptiveBitrateSettings(settings = settings)
    }

    fun toggleVerboseStatuses() {
        database.verboseStatuses = !database.verboseStatuses
    }

    private fun isShowingPanelQuickButton(type: SettingsQuickButtonType): Boolean = listOf(
        SettingsQuickButtonType.widgets,
        SettingsQuickButtonType.luts,
        SettingsQuickButtonType.chat,
        SettingsQuickButtonType.mic,
        SettingsQuickButtonType.bitrate,
        SettingsQuickButtonType.stream,
        SettingsQuickButtonType.obs,
        SettingsQuickButtonType.djiDevices,
        SettingsQuickButtonType.goPro,
        SettingsQuickButtonType.connectionPriorities,
        SettingsQuickButtonType.autoSceneSwitcher,
        SettingsQuickButtonType.live,
        SettingsQuickButtonType.macros,
    ).contains(type)

    fun toggleShowingPanel(type: SettingsQuickButtonType?, panel: ShowingPanel) {
        if (_showingPanel.value == ShowingPanel.quickButtonSettings) {
            quickButtons._selectedButtonType.value = null
        }
        if (_showingPanel.value == panel) {
            _showingPanel.value = ShowingPanel.none
        } else {
            _showingPanel.value = panel
        }
        _panelHidden.value = false
        for (pageButtonPairs in quickButtons._pairs.value) {
            for (pair in pageButtonPairs) {
                if (isShowingPanelQuickButton(pair.first.type)) {
                    setQuickButton(type = pair.first.type, isOn = false)
                }
                val second = pair.second
                if (second != null && isShowingPanelQuickButton(second.type)) {
                    setQuickButton(type = second.type, isOn = false)
                }
            }
        }
        if (type != null) {
            setQuickButton(type = type, isOn = _showingPanel.value == panel)
        }
    }

    fun setInteractiveBrowserWidgets(on: Boolean) {
        _interactiveBrowsers.value = on
        setQuickButton(type = SettingsQuickButtonType.interactiveBrowserWidgets, isOn = on)
    }

    fun setAllowVideoRangePixelFormat() {
        allowVideoRangePixelFormat = database.debug.allowVideoRangePixelFormat
    }

    fun setHighQualityDownsampling() {
        highQualityDownsampling = database.graphicsHighQualityDownsampling
    }

    fun makeToast(
        title: String,
        subTitle: String? = null,
        vibrate: Boolean = false,
        onTapped: (() -> Unit)? = null,
    ) {
        toast._toast.value = AlertToast(
            type = AlertToastType.regular,
            title = title,
            subTitle = subTitle,
            subTitleFont = FontFamily.Default,
        )
        toast.onTapped = onTapped
        showToast()
        Log.d("Model", "toast: Info: $title: ${subTitle ?: "-"}")
        if (vibrate) {
            TODO("no Android counterpart for UIDevice.vibrate; use Vibrator")
        }
    }

    fun makeWarningToast(title: String, subTitle: String? = null, vibrate: Boolean = false) {
        toast._toast.value = AlertToast(
            type = AlertToastType.regular,
            title = formatWarning(title),
            subTitle = subTitle,
            subTitleFont = FontFamily.Default,
        )
        showToast()
        Log.d("Model", "toast: Warning: $title: ${subTitle ?: "-"}")
        if (vibrate) {
            TODO("no Android counterpart for UIDevice.vibrate; use Vibrator")
        }
    }

    fun makeErrorToast(
        title: String,
        font: FontFamily? = null,
        subTitle: String? = null,
        vibrate: Boolean = false,
    ) {
        toast._toast.value = AlertToast(
            type = AlertToastType.regular,
            title = title,
            subTitle = subTitle,
            titleColor = Color.Red,
            titleFont = font,
            subTitleFont = FontFamily.Default,
        )
        showToast()
        Log.d("Model", "toast: Error: $title: ${subTitle ?: "-"}")
        if (vibrate) {
            TODO("no Android counterpart for UIDevice.vibrate; use Vibrator")
        }
    }

    fun makeErrorToastMain(
        title: String,
        font: FontFamily? = null,
        subTitle: String? = null,
        vibrate: Boolean = false,
    ) {
        mainScope.launch {
            makeErrorToast(title = title, font = font, subTitle = subTitle, vibrate = vibrate)
        }
    }

    private fun showToast() {
        toast._showingToast.value = false
        mainScope.launch {
            toast._showingToast.value = true
        }
    }

    private fun makeBuyIconsToastIfNeeded(): Boolean {
        if (store.hasBoughtSomething) {
            return false
        }
        if (enterForegroundCount < 100) {
            return false
        }
        if (enterForegroundCount < 500 && (enterForegroundCount % 10) != 0) {
            return false
        }
        makeToast(
            title = randomBuyIconsTitle(),
            subTitle = localized("Tap here to open the store."),
            onTapped = {
                toggleShowingPanel(type = null, panel = ShowingPanel.none)
                toggleShowingPanel(type = null, panel = ShowingPanel.store)
            },
        )
        return true
    }

    fun makePortErrorToast(port: String) {
        makeErrorToast(title = localized("Invalid port ${port.trim()}"))
    }

    fun updateQuickButtonPairs() {
        for (page in 0 until controlBarPages) {
            val buttons = database.quickButtons.filter { button ->
                button.enabled && button.page == page + 1 && isQuickButtonAllowed(button.type)
            }
            val pairs = mutableListOf<QuickButtonPair>()
            var index = 0
            while (index < buttons.size) {
                if (buttons.size - index > 1) {
                    pairs.add(
                        QuickButtonPair(
                            id = UUID.randomUUID(),
                            first = buttons[index + 1],
                            second = buttons[index],
                        ),
                    )
                } else {
                    pairs.add(QuickButtonPair(id = UUID.randomUUID(), first = buttons[index]))
                }
                index += 2
            }
            val newPairs = quickButtons._pairs.value.toMutableList()
            newPairs[page] = pairs
            quickButtons._pairs.value = newPairs
        }
    }

    fun getQuickButtonPairs(page: Int): List<QuickButtonPair> {
        if (page <= 0 || page > quickButtons._pairs.value.size) {
            return emptyList()
        }
        return quickButtons._pairs.value[page - 1]
    }

    fun setAllowHapticsAndSystemSoundsDuringRecording() {
        TODO("no Android counterpart for AVAudioSession.setAllowHapticsAndSystemSoundsDuringRecording")
    }

    private fun removeUnusedKeychainItems() {
        val streamIds = database.streams.map { it.id }
        removeUnusedTwitchAccessTokensInKeychain(usedStreamIds = streamIds)
        removeUnusedKickAccessTokensInKeychain(usedStreamIds = streamIds)
        removeUnusedYouTubeAuthStatesInKeychain(usedStreamIds = streamIds)
    }

    fun setup() {
        battery._level.value = TODO("no Android counterpart for UIDevice.batteryLevel")
        bluetoothCentralManger = TODO("no Android counterpart for CBCentralManager; use BluetoothAdapter")
        deleteTrash()
        removeUnusedKeychainItems()
        media = Media(delegate = this)
        setupAppIntents()
        faxReceiver.delegate = this
        fixAlertMediasNoUpdate()
        setAllowVideoRangePixelFormat()
        setHighQualityDownsampling()
        setExternalDisplayContent()
        _portraitVideoOffsetFromTop.value = database.portraitVideoOffsetFromTop
        loadTextWidgetStopwatches()
        quickButtonChatState._showFirstTimeChatterMessage.value =
            database.chat.showFirstTimeChatterMessage
        quickButtonChatState._showNewFollowerMessage.value = database.chat.showNewFollowerMessage
        autoSceneSwitcher.currentSwitcherId = database.autoSceneSwitchers.switcherId
        supportsAppleLog = hasAppleLog()
        chat.interactiveChat =
            getQuickButton(type = SettingsQuickButtonType.interactiveChat)?.isOn ?: false
        chatActivityFeed.interactiveChat = chat.interactiveChat
        _interactiveBrowsers.value =
            getQuickButton(type = SettingsQuickButtonType.interactiveBrowserWidgets)?.isOn ?: false
        updateShowCameraPreview()
        show._chatPhone.value = isChatPhone()
        showChatLabelsForAWhile()
        updateScreenAutoOff()
        setDisplayPortrait(portrait = database.portrait)
        setBitrateDropFix()
        TODO("no Android counterpart for SDImageWebPCoder; use a WebP capable image loader")
        TODO("no Android counterpart for UIDevice.isBatteryMonitoringEnabled")
        setupLogging()
        updateCameraLists()
        updateBatteryLevel()
        setPixelFormat()
        setupInputGainObserver()
        setupAudioSession()
        val camera = preferredCamera(position = CameraSelector.LENS_FACING_BACK)
        if (camera != null) {
            val range = camera.getUIZoomRange(hasUltraWideCamera = hasUltraWideBackCamera)
            cameraZoomXMinimum = range.first
            cameraZoomXMaximum = range.second
            val preset = zoom.backZoomPresets.firstOrNull()
            if (preset != null) {
                zoom.backPresetId = preset.id
                zoom.backX = preset.x
            } else {
                zoom.backX = cameraZoomXMinimum
            }
            zoom.x = zoom.backX
        }
        updateFrontZoomPresets()
        updateBackZoomPresets()
        zoom.frontPresetId = database.zoom.front[0].id
        streamPreviewView.videoGravity = VideoGravity.resizeAspect
        externalDisplayStreamPreviewView.videoGravity = VideoGravity.resizeAspect
        cameraPreviewView.backgroundColor = Color.Black
        updateDigitalClock(now = Instant.now())
        twitchChat = TwitchChat(delegate = this)
        setupSampleBufferReceiver()
        reloadStream()
        resetSelectedScene()
        mainScope.launch {
            setupAudio()
        }
        startPeriodicTimers()
        setupThermalState()
        setupMacStatusItem()
        removeUnusedImages()
        removeUnusedAlertMedias()
        removeUnusedVTubers()
        removeUnusedPngTubers()
        addObserver("UIDevice.orientationDidChangeNotification", "handleOrientationDidChange")
        store._iconImage.value = database.iconImage
        mainScope.launch {
            appStoreUpdateListenerTask = listenForAppStoreTransactions()
            getProductsFromAppStore()
            updateProductFromAppStore()
            updateIconImageFromDatabase()
        }
        addObserver("SystemVolumeDidChange", "handleSystemVolumeDidChange")
        addObserver("UIApplication.willResignActiveNotification", "handleApplicationDidChangeActive")
        addObserver("UIApplication.didBecomeActiveNotification", "handleApplicationDidChangeActive")
        addObserver("AVAudioSession.routeChangeNotification", "handleAudioRouteChange")
        addObserver(
            "UIApplication.didEnterBackgroundNotification",
            "handleApplicationDidEnterBackground",
        )
        addObserver(
            "UIApplication.willEnterForegroundNotification",
            "handleApplicationWillEnterForeground",
        )
        addObserver("UIApplication.willTerminateNotification", "handleApplicationWillTerminate")
        updateOrientation()
        reloadHttpProxyServer()
        reloadIngests()
        setupPictureInPicture()
        ipMonitor.pathUpdateHandler = { statuses -> handleIpStatusUpdate(statuses) }
        ipMonitor.start()
        addObserver("UIDevice.batteryStateDidChangeNotification", "handleBatteryStateDidChange")
        updateBatteryState()
        addObserver("GCControllerDidConnect", "handleGameControllerDidConnect")
        addObserver("GCControllerDidDisconnect", "handleGameControllerDidDisconnect")
        TODO("no Android counterpart for GameController wireless controller discovery")
        reloadLocation()
        _currentStreamId.value = _stream.value.id
        lutUpdated()
        addObserver("AVCaptureDevice.wasConnectedNotification", "handleCaptureDeviceWasConnected")
        addObserver("AVCaptureDevice.wasDisconnectedNotification", "handleCaptureDeviceWasDisconnected")
        TODO("no Android counterpart for WatchConnectivity WCSession")
        val chat = database.chat
        chatTextToSpeech.setRate(rate = chat.textToSpeechRate)
        chatTextToSpeech.setVolume(volume = chat.textToSpeechSayVolume)
        chatTextToSpeech.setVoices(voices = chat.textToSpeechLanguageVoices)
        chatTextToSpeech.setSayUsername(value = chat.textToSpeechSayUsername)
        chatTextToSpeech.setDefaultLanguage(value = chat.textToSpeechDefaultLanguage)
        chatTextToSpeech.setDetectLanguagePerMessage(value = chat.textToSpeechDetectLanguagePerMessage)
        chatTextToSpeech.setFilter(value = chat.textToSpeechFilter)
        chatTextToSpeech.setFilterMentions(value = chat.textToSpeechFilterMentions)
        chatTextToSpeech.setPauseBetweenMessages(value = chat.textToSpeechPauseBetweenMessages)
        chatTextToSpeech.setTtsMonsterApiToken(apiToken = chat.ttsMonster.apiToken)
        setTextToSpeechStreamerMentions()
        updateOrientationLock()
        initMediaPlayers()
        autoStartDjiDevices()
        autoStartGoProDevices()
        autoStartCatPrinters()
        autoStartWorkoutDevices()
        autoStartBlackSharkCoolerDevices()
        startWeatherManager()
        startGeographyManager()
        twitchAuth.setOnAccessToken(onAccessToken = { accessToken -> handleTwitchAccessToken(accessToken) })
        MoblinShortcuts.updateAppShortcutParameters()
        bonding.statisticsFormatter.setNetworkInterfaceNames(database.networkInterfaceNames)
        reloadTeslaVehicle()
        updateQuickButtonPairs()
        setQuickButton(type = SettingsQuickButtonType.blurFaces, isOn = database.face.blurFaces)
        setQuickButton(type = SettingsQuickButtonType.blurText, isOn = database.face.blurText)
        setQuickButton(type = SettingsQuickButtonType.privacy, isOn = database.face.blurBackground)
        setQuickButton(type = SettingsQuickButtonType.moblinInMouth, isOn = database.face.showMoblin)
        setQuickButton(type = SettingsQuickButtonType.beauty, isOn = database.beauty.enabled)
        updateLutsButtonState()
        updateAutoSceneSwitcherButtonState()
        reloadNtpClient()
        moblinkRelayLoadRelayId()
        reloadMoblinkRelay()
        reloadMoblinkStreamer()
        setCameraControlsEnabled()
        resetAverageSpeed()
        resetSlope()
        goPro._launchLiveStreamSelection.value = database.goPro.selectedLaunchLiveStream
        goPro._wifiCredentialsSelection.value = database.goPro.selectedWifiCredentials
        goPro._rtmpUrlSelection.value = database.goPro.selectedRtmpUrl
        replay.speed = database.replay.speed
        gForceManager = GForceManager(motionManager = motionManager)
        startGForceManager()
        chatBotCustomCommandsTextChanged()
        macrosTextFormatChanged()
        autoStartMacros()
        loadStealthModeImage()
        loadControlBarBackgroundImage()
        loadFaceBackgroundImage()
        updateKickChannelInfoIfNeeded()
        reloadSpeechToText()
        if (false) {
            wiFiAwareUpdated()
        }
        updateTalkback()
        Gimbal.shared = Gimbal(model = this)
        setGimbalTracking(on = database.gimbal.tracking)
        removeDeadMacrosSettings()
        mainScope.launch {
            writeFileLogToFile()
            flushFileLogToFile()
        }
        setupStreamDeck()
        setSelectedStreamDeck()
        startStreamIfAutoGoLive()
    }

    fun reloadIngests() {
        reloadRtmpServer()
        reloadSrtlaServer()
        reloadSrtClient()
        reloadRistServer()
        reloadRtspClient()
        reloadWhipServer()
        reloadWhepClient()
    }

    fun handleApplicationDidChangeActive(notificationName: String) {
        isAppActive = notificationName == "UIApplication.didBecomeActiveNotification"
        if (isMac()) {
            return
        }
        if (isAppActive) {
            media.setShowCameraPreview(updateShowCameraPreview())
        } else if (pictureInPictureEnabled()) {
            media.setShowCameraPreview(false)
        }
    }

    fun startGForceManager() {
        if (isGForceManagerNeeded()) {
            gForceManager?.start()
        } else {
            gForceManager?.stop()
        }
    }

    private fun isGForceManagerNeeded(): Boolean {
        for (widget in widgetsInCurrentSceneOrRemoteScene(onlyEnabled = true)) {
            when (widget.widget.type) {
                SettingsWidgetType.text -> {
                    if (widget.widget.text.needsGForce) {
                        return true
                    }
                }
                SettingsWidgetType.slideshow -> {
                    for (slide in widget.widget.slideshow.slides) {
                        if (getTextWidget(id = slide.widgetId)?.text?.needsGForce == true) {
                            return true
                        }
                    }
                }
                else -> {}
            }
        }
        if (isChatBotCustomCommandsGForceNeeded()) {
            return true
        }
        if (isMacrosGForceNeeded()) {
            return true
        }
        if (isRemoteControlStreamerGForceStatsFilterEnabled()) {
            return true
        }
        return false
    }

    fun setBitrateDropFix() {
        if (database.debug.bitrateDropFix) {
            videoEncoderDataRateLimitFactor = database.debug.dataRateLimitFactor.toDouble()
        } else {
            videoEncoderDataRateLimitFactor = 1.2
        }
    }

    fun formatDeviceStatus(
        name: String,
        batteryPercentage: Int?,
        thermalState: MoblinkThermalState?,
    ): Pair<String, Boolean> {
        var ok = true
        var status = name
        when (thermalState) {
            MoblinkThermalState.red -> {
                status += "🔥"
                ok = false
            }
            else -> {}
        }
        if (batteryPercentage != null) {
            if (batteryPercentage <= 10) {
                status += "🪫$batteryPercentage%"
                ok = false
            } else {
                status += "🔋$batteryPercentage%"
            }
        }
        return status to ok
    }

    fun reloadNtpClient() {
        stopNtpClient()
        if (isTimecodesEnabled()) {
            Log.d("Model", "Starting NTP client for pool ${_stream.value.ntpPoolAddress}")
            TODO("no Android counterpart for TrueTime NTP client")
        }
    }

    fun stopNtpClient() {
        Log.d("Model", "Stopping NTP client")
        TODO("no Android counterpart for TrueTime NTP client")
    }

    private fun isWeatherNeeded(): Boolean {
        for (widget in widgetsInCurrentSceneOrRemoteScene(onlyEnabled = true)) {
            when (widget.widget.type) {
                SettingsWidgetType.text -> {
                    if (widget.widget.text.needsWeather) {
                        return true
                    }
                }
                SettingsWidgetType.slideshow -> {
                    for (slide in widget.widget.slideshow.slides) {
                        if (getTextWidget(id = slide.widgetId)?.text?.needsWeather == true) {
                            return true
                        }
                    }
                }
                else -> {}
            }
        }
        if (isChatBotCustomCommandsWeatherNeeded()) {
            return true
        }
        if (isMacrosWeatherNeeded()) {
            return true
        }
        if (isRemoteControlStreamerWeatherStatsFilterEnabled()) {
            return true
        }
        return false
    }

    fun startWeatherManager() {
        weatherManager.setEnabled(value = isWeatherNeeded())
        weatherManager.start()
    }

    private fun isGeographyNeeded(): Boolean {
        for (widget in widgetsInCurrentSceneOrRemoteScene(onlyEnabled = true)) {
            when (widget.widget.type) {
                SettingsWidgetType.text -> {
                    if (widget.widget.text.needsGeography) {
                        return true
                    }
                }
                SettingsWidgetType.slideshow -> {
                    for (slide in widget.widget.slideshow.slides) {
                        if (getTextWidget(id = slide.widgetId)?.text?.needsGeography == true) {
                            return true
                        }
                    }
                }
                else -> {}
            }
        }
        if (isChatBotCustomCommandsGeographyNeeded()) {
            return true
        }
        if (isMacrosGeographyNeeded()) {
            return true
        }
        if (isRemoteControlStreamerGeographyStatsFilterEnabled()) {
            return true
        }
        return false
    }

    fun startGeographyManager() {
        geographyManager.setEnabled(value = isGeographyNeeded())
        geographyManager.start()
    }

    fun setExternalDisplayContent() {
        when (database.externalDisplayContent) {
            SettingsExternalDisplayContent.stream -> externalDisplay._chatEnabled.value = false
            SettingsExternalDisplayContent.cleanStream -> externalDisplay._chatEnabled.value = false
            SettingsExternalDisplayContent.chat -> externalDisplay._chatEnabled.value = true
            SettingsExternalDisplayContent.mirror -> externalDisplay._chatEnabled.value = false
        }
        setCleanExternalDisplay()
        updateExternalMonitorWindow()
    }

    private fun setupSampleBufferReceiver() {
        TODO("no Android counterpart for ReplayKit sample buffer receiver")
    }

    fun updateFaceFilterSettings() {
        faceEffect.setSettings(
            settings = database.face.toEffectSettings(
                backgroundImage = faceBackgroundImage,
                iconImage = loadFaceIconImage(),
            ),
        )
    }

    private fun loadFaceIconImage(): Bitmap? =
        TODO("no Android counterpart for UIImage(named:).cgImage; load with BitmapFactory")

    fun updateImageButtonState() {
        var isOn = streamOverlay._showingCamera.value
        if (camera.bias != 0.0) {
            isOn = true
        }
        if (camera.isWhiteBalanceLocked) {
            isOn = true
        }
        if (camera.isExposureAndIsoLocked) {
            isOn = true
        }
        if (camera.isFocusLocked) {
            isOn = true
        }
        if (isOn != getQuickButton(type = SettingsQuickButtonType.image)?.isOn) {
            setQuickButton(type = SettingsQuickButtonType.image, isOn = isOn)
        }
    }

    fun updateBeautyButtonState() {
        var isOn = streamOverlay._showingBeauty.value
        if (database.beauty.enabled) {
            isOn = true
        }
        if (isOn != getQuickButton(type = SettingsQuickButtonType.beauty)?.isOn) {
            setQuickButton(type = SettingsQuickButtonType.beauty, isOn = isOn)
        }
    }

    private fun handleIpStatusUpdate(statuses: List<IPMonitor.Status>) {
        statusOther._ipStatuses.value = statuses
        for (status in statuses) {
            if (status.interfaceType != IPMonitor.InterfaceType.wiredEthernet) {
                continue
            }
            for (stream in database.streams) {
                if (!stream.srt.connectionPriorities.priorities.any { it.name == status.name }) {
                    stream.srt.connectionPriorities.priorities.add(
                        SettingsStreamSrtConnectionPriority(name = status.name),
                    )
                }
            }
            if (!database.networkInterfaceNames.any { it.interfaceName == status.name }) {
                val interface = SettingsNetworkInterfaceName()
                interface.interfaceName = status.name
                interface.name = status.name
                database.networkInterfaceNames.add(interface)
            }
        }
        moblinkIpStatusesUpdated()
    }

    fun handleCaptureDeviceWasConnected() {
        updateCameraLists()
    }

    fun handleCaptureDeviceWasDisconnected() {
        updateCameraLists()
    }

    fun handleApplicationDidEnterBackground() {
        if (isMac()) {
            return
        }
        when (val level = backgroundRunLevel()) {
            BackgroundRunLevel.Full -> {
                startLiveActivity()
                if (!pictureInPictureEnabled()) {
                    disableScreenPreview()
                }
            }
            is BackgroundRunLevel.Service -> {
                startLiveActivity()
                inServiceBackground = true
                disableScreenPreview()
                stopPeriodicTimers(
                    keepChatRunning = level.keepChatRunning,
                    keepBatteryLevelRunning = level.keepBatteryLevelRunning,
                )
                if (level.keepChatRunning) {
                    startChatPhoneBackgroundAudio()
                }
            }
            BackgroundRunLevel.Off -> {
                storeSettings()
                replaysStorage.store()
                stopAll()
            }
        }
    }

    fun handleApplicationWillEnterForeground() {
        stopLiveActivity()
        if (isMac()) {
            return
        }
        inServiceBackground = false
        stopChatPhoneBackgroundAudio()
        when (backgroundRunLevel()) {
            BackgroundRunLevel.Full -> {
                maybeEnableScreenPreview()
            }
            is BackgroundRunLevel.Service -> {
                maybeEnableScreenPreview()
                startPeriodicTimers()
            }
            BackgroundRunLevel.Off -> {
                enterForegroundCount += 1
                showChatLabelsForAWhile()
                if (!makeBuyIconsToastIfNeeded()) {
                    makeReplayShouldBeDisabledToastIfNeeded()
                }
                makeNotLoggedInToTwitchToastIfNeeded()
                makeNotLoggedInToKickToastIfNeeded()
                makeNotLoggedInToYouTubeToastIfNeeded()
                clearRemoteSceneSettingsAndData()
                reloadStream()
                sceneUpdated(attachCamera = true, updateRemoteScene = false)
                reloadAudioSession()
                reloadIngests()
                reloadDjiDevices()
                reloadBrowserWidgets()
                chatTextToSpeech.reset(running = true)
                startWeatherManager()
                startGeographyManager()
                startGForceManager()
                if (_isRecording.value) {
                    resumeRecording()
                }
                reloadSpeechToText()
                reloadTeslaVehicle()
                reloadMoblinkRelay()
                reloadMoblinkStreamer()
                updateOrientation()
                autoStartCatPrinters()
                autoStartWorkoutDevices()
                autoStartBlackSharkCoolerDevices()
                if (showBackgroundStreamingDisabledToast) {
                    makeStreamEndedToast(
                        subTitle = localized("Tap here to enable background streaming."),
                        onTapped = {
                            _stream.value.backgroundStreaming = true
                            updatePictureInPicture()
                            makeToast(title = localized("Background streaming enabled"))
                        },
                    )
                    showBackgroundStreamingDisabledToast = false
                }
                reloadCameraLevel()
                updateIsStreamDeckDeviceDriverInstalled()
                startStreamIfAutoGoLive()
            }
        }
    }

    fun handleApplicationWillTerminate() {
        if (_isRecording.value) {
            suspendRecording()
        }
        updateSettingsFromTextWidgets()
        storeSettings()
        replaysStorage.store()
        if (isMac()) {
            stopAll()
        }
        stopLiveActivity()
        stopMacStatusItem()
        writeFileLogToFile()
        flushFileLogToFile()
    }

    private fun stopAll() {
        if (_isRecording.value) {
            suspendRecording()
        }
        showBackgroundStreamingDisabledToast = stopStream()
        stopPreviewStream()
        stopRtmpServer()
        stopSrtlaServer()
        stopSrtClient()
        stopRtspClient()
        stopWhepClient()
        stopWhipServer()
        teardownAudioSession()
        chatTextToSpeech.reset(running = false)
        locationManager.stop()
        weatherManager.stop()
        geographyManager.stop()
        gForceManager?.stop()
        obsWebSocket?.stop()
        media.stopAllNetStreams()
        stopSpeechToText()
        stopWorkout()
        stopTeslaVehicle()
        stopNtpClient()
        stopMoblinkRelay()
        stopMoblinkStreamer()
        stopCatPrinters()
        stopWorkoutDevices()
        stopRemoteControlAssistant()
        stopHttpProxyServer()
        fixedHorizonEffect.stop()
        cameraLevel.stop()
        writeFileLogToFile()
        flushFileLogToFile()
    }

    fun externalMonitorConnected(windowScene: Any) {
        externalDisplayWindow = TODO("no Android counterpart for UIWindowScene and UIWindow")
        updateExternalMonitorWindow()
        externalDisplayPreview = true
        reattachCamera()
    }

    fun disableScreenPreview() {
        media.setScreenPreview(enabled = false)
    }

    fun maybeEnableScreenPreview() {
        if (_showStealthMode.value) {
            return
        }
        media.setScreenPreview(enabled = true)
    }

    fun externalMonitorDisconnected() {
        externalDisplayWindow = null
        externalDisplayPreview = false
        reattachCamera()
    }

    private fun updateExternalMonitorWindow() {
        if (externalDisplayWindow == null) {
            return
        }
        TODO("no Android counterpart for UIWindow.makeKeyAndVisible; use Presentation API")
    }

    private fun backgroundRunLevel(): BackgroundRunLevel {
        if ((_isLive.value || _isRecording.value) && _stream.value.backgroundStreaming) {
            return BackgroundRunLevel.Full
        }
        if (_isLive.value || _isRecording.value) {
            return BackgroundRunLevel.Off
        }
        val keepChatRunning = database.chat.background || database.catPrinters.backgroundPrinting
        if (keepChatRunning || database.moblink.relay.enabled) {
            return BackgroundRunLevel.Service(
                keepChatRunning = keepChatRunning,
                keepBatteryLevelRunning = database.moblink.relay.enabled,
            )
        }
        return BackgroundRunLevel.Off
    }

    fun handleBatteryStateDidChange() {
        updateBatteryState()
    }

    fun close() {
        appStoreUpdateListenerTask?.cancel()
    }

    fun updateOrientation() {
        updateIsPortrait()
        if (_stream.value.portrait) {
            media.setVideoOrientation(value = VideoOrientation.portrait)
        } else {
            when (deviceRotation()) {
                Surface.ROTATION_90 -> media.setVideoOrientation(value = VideoOrientation.landscapeRight)
                Surface.ROTATION_270 -> media.setVideoOrientation(value = VideoOrientation.landscapeLeft)
                else -> {}
            }
        }
        updateCameraPreviewRotation()
    }

    private fun deviceRotation(): Int =
        TODO("no Android counterpart for UIDevice.orientation; use Display.rotation")

    fun handleOrientationDidChange(animated: Boolean) {
        updateOrientation()
    }

    fun startPeriodicTimers() {
        periodicTimer20ms.startPeriodic(interval = 0.02) { handle20msTimer() }
        periodicTimer200ms.startPeriodic(interval = 0.2) { handle200msTimer() }
        periodicTimer1s.startPeriodic(interval = 1.0) { handle1sTimer() }
        periodicTimer3s.startPeriodic(interval = 3.0) { handle3sTimer() }
        periodicTimer5s.startPeriodic(interval = 5.0) { handle5sTimer() }
        periodicTimer10s.startPeriodic(interval = 10.0) { handle10sTimer() }
        periodicTimerBatteryLevel.startPeriodic(interval = 30.0) { handle30sTimer() }
    }

    private fun handle20msTimer() {
        updateAdaptiveBitrate()
    }

    private fun handle200msTimer() {
        val monotonicNow = Instant.now()
        updateAudioLevel()
        updateChat()
        executeChatBotMessage()
        if (isWatchLocal()) {
            trySendNextChatPostToWatch()
        }
        val lastAttachCompletedTime = this.lastAttachCompletedTime
        if (lastAttachCompletedTime != null &&
            Duration.between(lastAttachCompletedTime, monotonicNow) > Duration.ofMillis(500)
        ) {
            updateTorch()
            this.lastAttachCompletedTime = null
        }
        val relaxedBitrateStartTime = this.relaxedBitrateStartTime
        if (relaxedBitrateStartTime != null &&
            Duration.between(relaxedBitrateStartTime, monotonicNow) > Duration.ofSeconds(3)
        ) {
            relaxedBitrate = false
            this.relaxedBitrateStartTime = null
        }
        speechToText?.tick(now = monotonicNow)
    }

    private fun handle1sTimer() {
        val now = Instant.now()
        val monotonicNow = Instant.now()
        updateDigitalClock(now = now)
        removeOldChatMessages(now = monotonicNow)
        if (inServiceBackground) {
            return
        }
        updateStreamUptime(now = monotonicNow)
        updateRecordingLength(now = now)
        media.updateSrtTransportBitrate()
        updateSpeed(now = monotonicNow)
        updateIngestsSpeed()
        updateBondingStatistics()
        updateLocation()
        updateObsSourceScreenshot()
        updateObsAudioVolume()
        updateBrowserWidgetStatus()
        logStatus()
        updateDebugOverlay()
        updateDistance()
        updateAltitude()
        updateSlope()
        updateAverageSpeed(now = monotonicNow)
        updateTextEffects(now = now, timestamp = monotonicNow)
        updateMapEffects()
        updateScoreboardEffects()
        updatePoll()
        updateObsSceneSwitcher(now = monotonicNow)
        weatherManager.setLocation(location = latestKnownLocation)
        geographyManager.setLocation(location = latestKnownLocation)
        updateBitrateStatus()
        updateAdsRemainingTimer(now = now)
        if (database.show.systemMonitor) {
            resourceUsage.update(now = monotonicNow)
            systemMonitor._appCpu.value = resourceUsage.getAppCpuUsage()
            systemMonitor._cpu.value = resourceUsage.getCpuUsage()
            systemMonitor._ram.value = resourceUsage.getMemoryUsage()
        }
        updateMoblinkStatus()
        updateStatusEventsText()
        updateStatusChatText()
        updateAutoSceneSwitcher(now = monotonicNow)
        sendPeriodicRemoteControlStreamerStatus()
        sendPeriodicRemoteControlStreamerStats(now = now)
        speechToTextProcess()
        updateTwitchRaid()
        updateTwitchPollCountdown()
        updateTwitchPredictionCountdown()
        updateHypeTrainCountdown()
    }

    private fun handle3sTimer() {
        teslaGetDriveState()
    }

    private fun handle5sTimer() {
        updateRemoteControlAssistantStatus()
        if (isWatchLocal()) {
            sendThermalStateToWatch(thermalState = statusOther._thermalState.value)
        }
        teslaGetMediaState()
    }

    private fun handle10sTimer() {
        val monotonicNow = Instant.now()
        media.logStatistics()
        updateObsStatus()
        updateRemoteControlStatus()
        if (_stream.value.enabled && database.debug.videoBitrateChange) {
            media.updateVideoStreamBitrate(bitrate = _stream.value.bitrate)
        }
        updateViewers()
        updateCurrentSsid()
        teslaGetChargeState()
        moblink.streamer?.updateStatus()
        updateTwitchStream(monotonicNow = monotonicNow)
        updateYouTubeStream(monotonicNow = monotonicNow)
        updateAvailableDiskSpace()
        tryToFetchYouTubeVideoId()
        keepSpeakerAlive(now = monotonicNow)
    }

    private fun handle30sTimer() {
        updateDjiDevicesStatus()
        updateBatteryLevel()
        writeFileLogToFile()
    }

    fun stopPeriodicTimers(keepChatRunning: Boolean, keepBatteryLevelRunning: Boolean) {
        periodicTimer20ms.stop()
        if (!keepChatRunning) {
            periodicTimer200ms.stop()
            periodicTimer1s.stop()
        }
        periodicTimer3s.stop()
        periodicTimer5s.stop()
        periodicTimer10s.stop()
        if (!keepBatteryLevelRunning) {
            periodicTimerBatteryLevel.stop()
        }
    }

    private fun updateAvailableDiskSpace() {
        if (!_isRecording.value) {
            return
        }
        val available = getAvailableDiskSpace() ?: return
        if (available < 1_000_000_000) {
            stopRecording(
                toastTitle = localized("‼️ Low on disk. Stopping recording. ‼️"),
                toastSubTitle = localized("Please delete recordings and other big files"),
            )
        } else if (available < 2_000_000_000) {
            makeToast(
                title = localized("⚠️ Low on disk ⚠️"),
                subTitle = localized("Please delete recordings and other big files"),
            )
        }
    }

    private fun updateAdsRemainingTimer(now: Instant) {
        val adsEndDate = this.adsEndDate ?: return
        val secondsLeft = Duration.between(now, adsEndDate).seconds
        if (secondsLeft < 0) {
            this.adsEndDate = null
            statusTopRight._adsRemainingTimerStatus.value = noValue
        } else {
            statusTopRight._adsRemainingTimerStatus.value = secondsLeft.toInt().toString()
        }
    }

    private fun updateCurrentSsid() {
        fetchCurrentWiFiSsid { ssid ->
            currentWiFiSsid = ssid
        }
    }

    private fun removeUnusedImages() {
        for (id in imageStorage.ids()) {
            var used = false
            for (widget in database.widgets) {
                if (widget.type != SettingsWidgetType.image) {
                    continue
                }
                if (widget.id == id) {
                    used = true
                    break
                }
            }
            if (database.color.diskLutsPng.any { it.id == id }) {
                used = true
            }
            if (database.color.diskLutsCube.any { it.id == id }) {
                used = true
            }
            if (!used) {
                Log.i("Model", "Removing unused image $id")
                imageStorage.remove(id = id)
            }
        }
    }

    private fun updateViewers() {
        var newColor: Color = Color.White
        var newNumberOfViewers = 0
        var hasCount = false
        val streamingPlatformsStatus = mutableListOf<StreamingPlatformStatus>()
        for (streamingPlatformStatus in statusTopLeft._streamingPlatformStatuses.value) {
            val newStreamingPlatformStatus = when (streamingPlatformStatus.platform) {
                Platform.twitch -> updateViewersTwitch()
                Platform.kick -> updateViewersKick()
                Platform.youTube -> updateViewersYouTube()
                Platform.soop -> updateViewersSoop()
                else -> streamingPlatformStatus
            }
            streamingPlatformsStatus.add(newStreamingPlatformStatus)
            when (val status = newStreamingPlatformStatus.status) {
                is PlatformStatus.live -> {
                    newNumberOfViewers += status.viewerCount
                    hasCount = true
                }
                PlatformStatus.unknown -> {
                    if (newColor != Color.Red) {
                        newColor = Color(0xFFFF9500)
                    }
                }
                PlatformStatus.offline -> {
                    newColor = Color.Red
                }
            }
        }
        if (newColor != statusTopLeft._numberOfViewersIconColor.value) {
            statusTopLeft._numberOfViewersIconColor.value = newColor
        }
        if (streamingPlatformsStatus != statusTopLeft._streamingPlatformStatuses.value) {
            statusTopLeft._streamingPlatformStatuses.value = streamingPlatformsStatus
        }
        val newNumberOfViewersCompact = updateViewersCompact(newNumberOfViewers, hasCount)
        if (newNumberOfViewersCompact != statusTopLeft._numberOfViewersCompact.value) {
            statusTopLeft._numberOfViewersCompact.value = newNumberOfViewersCompact
            sendViewerCountWatch()
        }
    }

    private fun updateViewersCompact(newNumberOfViewers: Int, hasCount: Boolean): String =
        if (hasCount) countFormatter.format(newNumberOfViewers) else noValue

    private fun fixAlert(alert: SettingsWidgetAlertsAlert) {
        if (getAllAlertImages().firstOrNull { it.id == alert.imageId } == null) {
            alert.imageId = database.alertsMediaGallery.bundledImages[0].id
        }
        if (getAllAlertSounds().firstOrNull { it.id == alert.soundId } == null) {
            alert.soundId = database.alertsMediaGallery.bundledSounds[0].id
        }
    }

    fun fixAlertMedias() {
        fixAlertMediasNoUpdate()
        updateAlertsSettings()
    }

    private fun fixAlertMediasNoUpdate() {
        for (widget in database.widgets) {
            fixAlert(alert = widget.alerts.twitch.follows)
            fixAlert(alert = widget.alerts.twitch.subscriptions)
            for (command in widget.alerts.chatBot.commands) {
                fixAlert(alert = command.alert)
            }
        }
    }

    private fun removeUnusedAlertMedias() {
        for (mediaId in alertMediaStorage.ids()) {
            var found = false
            if (database.alertsMediaGallery.customImages.any { it.id == mediaId }) {
                found = true
            }
            if (database.alertsMediaGallery.customSounds.any { it.id == mediaId }) {
                found = true
            }
            if (!found) {
                alertMediaStorage.remove(id = mediaId)
            }
        }
    }

    private fun removeUnusedVTubers() {
        for (vTuberId in vTuberStorage.ids()) {
            var found = false
            for (widget in database.widgets) {
                if (widget.vTuber.id == vTuberId) {
                    found = true
                    break
                }
            }
            if (!found) {
                vTuberStorage.remove(id = vTuberId)
            }
        }
    }

    private fun removeUnusedPngTubers() {
        for (pngTuberId in pngTuberStorage.ids()) {
            var found = false
            for (widget in database.widgets) {
                if (widget.pngTuber.id == pngTuberId) {
                    found = true
                    break
                }
            }
            if (!found) {
                pngTuberStorage.remove(id = pngTuberId)
            }
        }
    }

    fun getAllAlertImages(): List<SettingsAlertsMediaGalleryItem> =
        database.alertsMediaGallery.bundledImages + database.alertsMediaGallery.customImages

    fun getAllAlertSounds(): List<SettingsAlertsMediaGalleryItem> =
        database.alertsMediaGallery.bundledSounds + database.alertsMediaGallery.customSounds

    fun getAlertSoundUrl(soundId: UUID): String? {
        if (database.alertsMediaGallery.bundledSounds.any { it.id == soundId }) {
            TODO("no Android counterpart for Bundle.main.url; load from res/raw")
        }
        return alertMediaStorage.makePath(id = soundId)
    }

    fun getAlertsEffect(id: UUID): AlertsEffect? {
        for ((alertsEffectId, alertsEffect) in alertsEffects) {
            if (id == alertsEffectId) {
                return alertsEffect
            }
        }
        return null
    }

    fun setPoll(on: Boolean) {
        pollEnabled = on
        pollVotes = mutableListOf(0, 0, 0)
        if (pollEnabled) {
            pollEffect = PollEffect(canvasSize = media.getCanvasSize())
        } else {
            pollEffect = null
        }
    }

    fun togglePoll() {
        setPoll(on = !pollEnabled)
    }

    fun handlePollVote(vote: String?) {
        when (vote) {
            "1" -> pollVotes[0] += 1
            "2" -> pollVotes[1] += 1
            "3" -> pollVotes[2] += 1
            else -> {}
        }
    }

    private fun updatePoll() {
        if (!pollEnabled) {
            return
        }
        val totalVotes = pollVotes.sum().toDouble()
        if (totalVotes <= 0.0) {
            return
        }
        val votes = mutableListOf<String>()
        for (index in pollVotes.indices) {
            val percentage = (100.0 * pollVotes[index] / totalVotes).roundToInt()
            votes.add("${index + 1}: $percentage%")
        }
        pollEffect?.updateText(text = votes.joinToString(", "))
    }

    fun storeSettings() {
        settings.store()
    }

    fun networkInterfaceNamesUpdated() {
        media.setNetworkInterfaceNames(networkInterfaceNames = database.networkInterfaceNames)
        bonding.statisticsFormatter.setNetworkInterfaceNames(database.networkInterfaceNames)
    }

    fun playAlert(alert: AlertsEffectAlert) {
        for (alertsEffect in enabledAlertsEffects) {
            alertsEffect.play(alert = alert)
        }
    }

    fun testAlert(alert: AlertsEffectAlert) {
        playAlert(alert = alert)
    }

    fun updateAlertsSettings() {
        for (widget in database.widgets) {
            if (widget.type != SettingsWidgetType.alerts) {
                continue
            }
            widget.alerts.needsSubtitles =
                widget.alerts.speechToText.strings.any { it.alert.enabled }
            getAlertsEffect(id = widget.id)?.setSettings(settings = widget.alerts.clone())
        }
        if (isSpeechToTextNeeded()) {
            reloadSpeechToText()
        }
        sceneUpdated()
    }

    fun updateOrientationLock() {
        updateCameraPreviewRotation()
        TODO("no Android counterpart for AppDelegate.orientationLock; use Activity.requestedOrientation")
    }

    fun reloadBrowserWidgets() {
        reloadHttpProxyServer()
        for (browser in _browsers.value) {
            browser.browserEffect.reload()
        }
    }

    fun getQuickButton(type: SettingsQuickButtonType): SettingsQuickButton? =
        database.quickButtons.firstOrNull { it.type == type }

    fun showQuickButtonSettings(type: SettingsQuickButtonType) {
        _quickButtonSettingsButton.value = getQuickButton(type = type)
        toggleShowingPanel(type = null, panel = ShowingPanel.none)
        toggleShowingPanel(type = null, panel = ShowingPanel.quickButtonSettings)
        quickButtons._selectedButtonType.value = type
    }

    fun setQuickButton(type: SettingsQuickButtonType, isOn: Boolean) {
        val button = getQuickButton(type = type) ?: return
        button.isOn = isOn
        val filter = RemoteControlFilter(type)
        if (filter != null) {
            remoteControlStateChanged(
                state = RemoteControlAssistantStreamerState(filters = mapOf(filter to button.isOn)),
            )
        }
    }

    fun toggleQuickButton(type: SettingsQuickButtonType) {
        val button = getQuickButton(type = type) ?: return
        setQuickButton(type = type, isOn = !button.isOn)
    }

    fun setFilterQuickButton(type: SettingsQuickButtonType, on: Boolean) {
        setQuickButton(type = type, isOn = on)
        sceneUpdated(updateRemoteScene = false)
    }

    fun toggleFilterQuickButton(type: SettingsQuickButtonType) {
        toggleQuickButton(type = type)
        sceneUpdated(updateRemoteScene = false)
    }

    fun setWhirlpoolQuickButton(on: Boolean) {
        streamOverlay._showingWhirlpool.value = on
        setFilterQuickButton(type = SettingsQuickButtonType.whirlpool, on = on)
    }

    fun toggleWhirlpoolQuickButton() {
        setWhirlpoolQuickButton(on = !streamOverlay._showingWhirlpool.value)
    }

    fun setBeautyQuickButton(on: Boolean) {
        streamOverlay._showingBeauty.value = on
        updateBeautyButtonState()
    }

    fun toggleBeautyQuickButton() {
        setBeautyQuickButton(on = !streamOverlay._showingBeauty.value)
    }

    fun toggleVideoPreview() {
        streamOverlay._showingVideoPreview.value = !streamOverlay._showingVideoPreview.value
        media.setVideoPreviewEnabled(enabled = streamOverlay._showingVideoPreview.value)
        updateVideoPreviews()
    }

    fun setPinchQuickButton(on: Boolean) {
        streamOverlay._showingPinch.value = on
        setFilterQuickButton(type = SettingsQuickButtonType.pinch, on = on)
    }

    fun togglePinchQuickButton() {
        setPinchQuickButton(on = !streamOverlay._showingPinch.value)
    }

    fun setPixellateQuickButton(on: Boolean) {
        streamOverlay._showingPixellate.value = on
        setFilterQuickButton(type = SettingsQuickButtonType.pixellate, on = on)
    }

    fun togglePixellateQuickButton() {
        setPixellateQuickButton(on = !streamOverlay._showingPixellate.value)
    }

    fun setCameraManQuickButton(on: Boolean) {
        cameraManEffect = CameraManEffect(
            moveVertically = database.debug.cameraManMoveVertically,
            speed = database.debug.cameraManSpeed,
            alwaysMove = database.debug.cameraManAlwaysMove,
        )
        setFilterQuickButton(type = SettingsQuickButtonType.cameraMan, on = on)
    }

    fun toggleCameraManQuickButton() {
        cameraManEffect = CameraManEffect(
            moveVertically = database.debug.cameraManMoveVertically,
            speed = database.debug.cameraManSpeed,
            alwaysMove = database.debug.cameraManAlwaysMove,
        )
        toggleFilterQuickButton(type = SettingsQuickButtonType.cameraMan)
    }

    fun setPollQuickButton(on: Boolean) {
        setPoll(on = on)
        setFilterQuickButton(type = SettingsQuickButtonType.poll, on = on)
    }

    fun togglePollQuickButton() {
        togglePoll()
        toggleFilterQuickButton(type = SettingsQuickButtonType.poll)
    }

    fun setDisplayPortrait(portrait: Boolean) {
        database.portrait = portrait
        updateIsPortrait()
        setQuickButton(type = SettingsQuickButtonType.portrait, isOn = portrait)
        updateOrientationLock()
    }

    fun setIsWorkout(type: WatchProtocolWorkoutType?) {
        _workoutType.value = type
        setQuickButton(type = SettingsQuickButtonType.workout, isOn = type != null)
    }

    fun setMuteOn(value: Boolean) {
        audio.muted = value
        updateMute()
        setQuickButton(type = SettingsQuickButtonType.mute, isOn = value)
    }

    fun setIsMuted(value: Boolean) {
        setMuteOn(value = value)
    }

    fun updateScreenAutoOff() {
        TODO("no Android counterpart for UIApplication.isIdleTimerDisabled; use FLAG_KEEP_SCREEN_ON")
    }

    fun reloadConnections() {
        useRemoteControlForChatAndEvents = false
        reloadViewers()
        reloadChats()
        reloadTwitchEventSub()
        reloadObsWebSocket()
        reloadRemoteControlStreamer()
        reloadRemoteControlAssistant()
        reloadRemoteControlRelay()
        reloadRemoteControlWeb()
        reloadKickViewers()
        reloadSoopPlatformStatus()
        reloadNtpClient()
    }

    fun isTimecodesEnabled(): Boolean =
        _stream.value.timecodesEnabled && !_stream.value.ntpPoolAddress.isEmpty()

    fun setPixellateStrength(strength: Float) {
        pixellateEffect.setSettings(strength = strength)
    }

    fun setWhirlpoolAngle(angle: Float) {
        whirlpoolEffect.setSettings(angle = angle)
    }

    fun setPinchScale(scale: Float) {
        pinchEffect.setSettings(scale = scale)
    }

    fun setDebugLogging(on: Boolean) {
        loggerDebugEnabled = on
        remoteControlStateChanged(state = RemoteControlAssistantStreamerState(debugLogging = on))
    }

    fun isEventsConfigured(): Boolean = isTwitchEventSubConfigured()

    fun isEventsConnected(): Boolean = isTwitchEventsConnected()

    fun isViewersConfigured(): Boolean =
        isTwitchViewersConfigured() || isKickViewersConfigured() || isYouTubeViewersConfigured() ||
            isSoopViewersConfigured()

    fun isOpenStreamingPlatformChatConfigured(): Boolean =
        database.chat.enabled && _stream.value.openStreamingPlatformUrl != "" &&
            _stream.value.openStreamingPlatformChannelId != ""

    fun isOpenStreamingPlatformChatConnected(): Boolean =
        openStreamingPlatformChat?.isConnected() ?: false

    fun hasOpenStreamingPlatformChatEmotes(): Boolean =
        openStreamingPlatformChat?.hasEmotes() ?: false

    fun reloadOpenStreamingPlatformChat() {
        openStreamingPlatformChat?.stop()
        openStreamingPlatformChat = null
        if (isOpenStreamingPlatformChatConfigured() &&
            !isRemoteControlChatAndEvents(platform = Platform.openStreamingPlatform)
        ) {
            openStreamingPlatformChat = OpenStreamingPlatformChat(
                model = this,
                url = _stream.value.openStreamingPlatformUrl,
                channelId = _stream.value.openStreamingPlatformChannelId,
            )
            openStreamingPlatformChat?.start()
        }
        updateChatMoreThanOneChatConfigured()
    }

    fun openStreamingPlatformUrlUpdated() {
        reloadOpenStreamingPlatformChat()
        resetChat()
    }

    fun openStreamingPlatformRoomUpdated() {
        reloadOpenStreamingPlatformChat()
        resetChat()
    }

    fun bttvEmotesEnabledUpdated() {
        reloadChats()
    }

    fun ffzEmotesEnabledUpdated() {
        reloadChats()
    }

    fun seventvEmotesEnabledUpdated() {
        reloadChats()
    }

    fun updateBrowserWidgetStatus() {
        if (statusTopRight._browserWidgetsStatusChanged.value) {
            statusTopRight._browserWidgetsStatusChanged.value = false
        }
        val messages = mutableListOf<String>()
        for (browser in _browsers.value) {
            val progress = browser.browserEffect.progress
            if (browser.browserEffect.isLoaded) {
                messages.add("${browser.browserEffect.host}: $progress%")
                if (progress != 100 ||
                    browser.browserEffect.startLoadingTime.plusSeconds(5).isAfter(Instant.now())
                ) {
                    if (!statusTopRight._browserWidgetsStatusChanged.value) {
                        statusTopRight._browserWidgetsStatusChanged.value = true
                    }
                }
            }
        }
        val message: String = if (messages.isEmpty()) noValue else messages.joinToString(", ")
        if (statusTopRight._browserWidgetsStatus.value != message) {
            statusTopRight._browserWidgetsStatus.value = message
        }
    }

    fun reloadViewers() {
        statusTopLeft._numberOfViewersIconColor.value = Color(0xFFFF9500)
        statusTopLeft._numberOfViewersCompact.value = noValue
        statusTopLeft._streamingPlatformStatuses.value = emptyList()
        if (isTwitchViewersConfigured()) {
            statusTopLeft._streamingPlatformStatuses.value = statusTopLeft._streamingPlatformStatuses.value +
                StreamingPlatformStatus(Platform.twitch, PlatformStatus.unknown)
        }
        if (isKickViewersConfigured()) {
            statusTopLeft._streamingPlatformStatuses.value = statusTopLeft._streamingPlatformStatuses.value +
                StreamingPlatformStatus(Platform.kick, PlatformStatus.unknown)
        }
        if (isYouTubeViewersConfigured()) {
            statusTopLeft._streamingPlatformStatuses.value = statusTopLeft._streamingPlatformStatuses.value +
                StreamingPlatformStatus(Platform.youTube, PlatformStatus.unknown)
        }
        if (isSoopViewersConfigured()) {
            statusTopLeft._streamingPlatformStatuses.value = statusTopLeft._streamingPlatformStatuses.value +
                StreamingPlatformStatus(Platform.soop, PlatformStatus.unknown)
        }
        twitchPlatformStatus = PlatformStatus.unknown
        twitchStreamUpdateTime = Instant.now().plusSeconds(15)
        youTubeStreamUpdateTimePollDelta = Duration.ofSeconds(15)
    }

    private fun logStatus() {
        if (loggerDebugEnabled && _isLive.value) {
            Log.d(
                "Model",
                "Status: Bitrate: ${bitrate._speedAndTotal.value}, Uptime: ${streamUptime._uptime.value}",
            )
        }
    }

    fun setLowFpsImage() {
        var fps = 0f
        if (isWatchReachable() && isWatchLocal()) {
            fps = 1.0f
        }
        if (isRemoteControlStreamerPreviewActive()) {
            fps = database.remoteControl.streamer.previewFps
        }
        if (isRemoteControlWebPreviewActive()) {
            fps = max(fps, 1.0f)
        }
        media.setLowFpsImage(fps = fps)
        lowFpsImageFps = max(fps, 1f).toLong()
    }

    fun setSceneSwitchTransition() {
        media.setSceneSwitchTransition(
            sceneSwitchTransition = database.sceneSwitchTransition.toVideoUnit(),
        )
    }

    fun setCleanExternalDisplay() {
        media.setCleanExternalDisplay(
            enabled = database.externalDisplayContent == SettingsExternalDisplayContent.cleanStream,
        )
    }

    fun toggleLocalOverlays() {
        _showLocalOverlays.value = !_showLocalOverlays.value
    }

    fun toggleBrowser() {
        _showBrowser.value = !_showBrowser.value
    }

    fun toggleNavigation() {
        _showNavigation.value = !_showNavigation.value
    }

    fun toggleLockScreen() {
        _lockScreen.value = !_lockScreen.value
        setQuickButton(type = SettingsQuickButtonType.lockScreen, isOn = _lockScreen.value)
        if (_lockScreen.value) {
            makeToast(
                title = localized("Screen locked"),
                subTitle = localized("Double tap to unlock"),
            )
        } else {
            makeToast(title = localized("Screen unlocked"))
        }
    }

    fun findScoreboardPlayer(id: UUID): String =
        database.scoreboardPlayers.firstOrNull { it.id == id }?.name ?: "🇸🇪 Moblin"

    private fun updateDigitalClock(now: Instant) {
        val digitalClock = digitalClockFormatter.format(now)
        if (statusOther._digitalClock.value != digitalClock) {
            statusOther._digitalClock.value = digitalClock
        }
    }

    private fun updateBatteryLevel() {
        val level = getBatteryLevel()
        if (level != battery._level.value) {
            battery._level.value = level
        }
        streamingHistoryStream?.updateLowestBatteryLevel(level = battery._level.value)
        if (battery._level.value <= 0.07 && !isBatteryCharging() && !isMac() &&
            battery._level.value != -1.0
        ) {
            makeWarningToast(title = lowBatteryMessage, vibrate = true)
            if (database.chat.botEnabled && database.chat.botSendLowBatteryWarning) {
                sendChatMessage(message = "Moblin bot: $lowBatteryMessage")
            }
        }
    }

    private fun updateBatteryState() {
        val state = getBatteryState()
        if (state != battery._state.value) {
            battery._state.value = state
            remoteControlStateChanged(
                state = RemoteControlAssistantStreamerState(batteryCharging = isBatteryCharging()),
            )
        }
    }

    fun isBatteryCharging(): Boolean =
        battery._state.value == BatteryState.charging || battery._state.value == BatteryState.full

    private fun getBatteryLevel(): Double = TODO("no Android counterpart for UIDevice.batteryLevel")

    private fun getBatteryState(): BatteryState =
        TODO("no Android counterpart for UIDevice.batteryState")

    private fun updateIngestsSpeed() {
        var stats = IngestStats()
        stats = updateRtmpIngestsSpeed(stats)
        stats = updateSrtlaIngestsSpeed(stats)
        stats = updateSrtClientIngestsSpeed(stats)
        stats = updateRistIngestsSpeed(stats)
        stats = updateRtspIngestsSpeed(stats)
        stats = updateWhipIngestsSpeed(stats)
        stats = updateWhepIngestsSpeed(stats)
        val message: String
        if (stats.anyServerEnabled) {
            if (stats.numberOfClients > 0) {
                val total = stats.total.formatBytes()
                serversSpeed = (serversSpeed * 0.7 + stats.speed * 0.3).toLong()
                val speed = formatBytesPerSecond(speed = 8 * serversSpeed)
                message = localized("$speed ($total) ${stats.numberOfClients}")
            } else {
                message = stats.numberOfClients.toString()
            }
        } else {
            message = noValue
        }
        if (message != ingests._speedAndTotal.value) {
            ingests._speedAndTotal.value = message
        }
    }

    private fun updateRtmpIngestsSpeed(stats: IngestStats): IngestStats {
        val rtmpServer = ingests.rtmp ?: return stats
        val serverStats = rtmpServer.updateStats()
        val numberOfRtmpClients = rtmpServer.getNumberOfClients()
        var result = stats.copy(
            anyServerEnabled = true,
            numberOfClients = stats.numberOfClients + numberOfRtmpClients,
        )
        if (numberOfRtmpClients > 0) {
            result = result.copy(
                total = result.total + serverStats.total,
                speed = result.speed + serverStats.speed,
            )
        }
        return result
    }

    private fun updateSrtlaIngestsSpeed(stats: IngestStats): IngestStats {
        val srtlaServer = ingests.srtla ?: return stats
        val serverStats = srtlaServer.updateStats()
        val numberOfSrtlaClients = srtlaServer.getNumberOfClients()
        var result = stats.copy(
            anyServerEnabled = true,
            numberOfClients = stats.numberOfClients + numberOfSrtlaClients,
        )
        if (numberOfSrtlaClients > 0) {
            result = result.copy(
                total = result.total + serverStats.total,
                speed = result.speed + serverStats.speed,
            )
        }
        return result
    }

    private fun updateSrtClientIngestsSpeed(stats: IngestStats): IngestStats {
        var result = stats
        for (client in ingests.srt) {
            val clientStats = client.updateStats()
            result = result.copy(
                total = result.total + clientStats.total,
                speed = result.speed + clientStats.speed,
                numberOfClients = result.numberOfClients + 1,
                anyServerEnabled = true,
            )
        }
        return result
    }

    private fun updateRistIngestsSpeed(stats: IngestStats): IngestStats {
        val ristServer = ingests.rist ?: return stats
        val serverStats = ristServer.updateStats()
        val numberOfRistClients = ristServer.getNumberOfClients()
        var result = stats.copy(
            anyServerEnabled = true,
            numberOfClients = stats.numberOfClients + numberOfRistClients,
        )
        if (numberOfRistClients > 0) {
            result = result.copy(
                total = result.total + serverStats.total,
                speed = result.speed + serverStats.speed,
            )
        }
        return result
    }

    private fun updateRtspIngestsSpeed(stats: IngestStats): IngestStats {
        var result = stats
        for (client in ingests.rtsp) {
            val clientStats = client.updateStats()
            result = result.copy(
                total = result.total + clientStats.total,
                speed = result.speed + clientStats.speed,
                numberOfClients = result.numberOfClients + 1,
                anyServerEnabled = true,
            )
        }
        return result
    }

    private fun updateWhipIngestsSpeed(stats: IngestStats): IngestStats {
        val whipServer = ingests.whip ?: return stats
        val serverStats = whipServer.updateStats()
        val numberOfWhipClients = whipServer.getNumberOfClients()
        var result = stats.copy(
            anyServerEnabled = true,
            numberOfClients = stats.numberOfClients + numberOfWhipClients,
        )
        if (numberOfWhipClients > 0) {
            result = result.copy(
                total = result.total + serverStats.total,
                speed = result.speed + serverStats.speed,
            )
        }
        return result
    }

    private fun updateWhepIngestsSpeed(stats: IngestStats): IngestStats {
        var result = stats
        for (client in ingests.whep) {
            val clientStats = client.updateStats()
            result = result.copy(
                total = result.total + clientStats.total,
                speed = result.speed + clientStats.speed,
                numberOfClients = result.numberOfClients + 1,
                anyServerEnabled = true,
            )
        }
        return result
    }

    fun checkPhotoLibraryAuthorization() {
        TODO("no Android counterpart for PhotosUI PHPhotoLibrary; use the Photo Picker")
    }

    private fun addObserver(name: String, selector: String) {
        TODO("no Android counterpart for NotificationCenter.addObserver; use BroadcastReceiver")
    }

    private fun setupThermalState() {
        updateThermalState()
        addObserver("ProcessInfo.thermalStateDidChangeNotification", "handleThermalStateDidChange")
    }

    fun handleThermalStateDidChange() {
        mainScope.launch {
            updateThermalState()
        }
    }

    private fun updateThermalState() {
        val state = getThermalState()
        if (state != statusOther._thermalState.value) {
            statusOther._thermalState.value = state
        }
        streamingHistoryStream?.updateHighestThermalState(thermalState = ThermalState.from(state))
        if (isWatchLocal()) {
            sendThermalStateToWatch(thermalState = state)
        }
        Log.i("Model", "Thermal state: $state")
        if (statusOther._thermalState.value == MoblinkThermalState.red) {
            makeFlameRedToast()
        }
    }

    private fun getThermalState(): MoblinkThermalState =
        TODO("no Android counterpart for ProcessInfo.thermalState; use PowerManager.getCurrentThermalStatus")

    fun reattachCamera() {
        detachCamera()
        attachCamera()
    }

    fun detachCamera() {
        val params = VideoUnitAttachParams(
            devices = CaptureDevices(hasSceneDevice = false, devices = emptyList()),
            builtinDelay = 0,
            cameraPreviewLayers = cameraPreviewView.previewLayers,
            attachCameraPreview = false,
            showCameraPreview = false,
            externalDisplayPreview = false,
            bufferedVideo = null,
            preferredVideoStabilizationMode = SettingsVideoStabilizationMode.off,
            ignoreFramesAfterAttachSeconds = 0.0,
            fillFrame = false,
            isLandscapeStreamAndPortraitUi = isLandscapeStreamAndPortraitUi(),
            forceSceneTransition = false,
            macScreenCapture = false,
            attachPhotoShoot = false,
        )
        media.attachCamera(params)
    }

    fun attachCamera() {
        val scene = getSelectedScene() ?: return
        attachSingleLayout(scene = scene)
    }

    private fun updateCameraPreviewRotation() {
        if (useLandscapeStreamAndPortraitUi(cameraDevice, isLandscapeStreamAndPortraitUi())) {
            cameraPreviewView.setVideoOrientation(VideoOrientation.portrait)
        } else if (_stream.value.portrait) {
            cameraPreviewView.setVideoOrientation(VideoOrientation.portrait)
        } else {
            when (deviceRotation()) {
                Surface.ROTATION_90 ->
                    cameraPreviewView.setVideoOrientation(VideoOrientation.landscapeRight)
                Surface.ROTATION_270 ->
                    cameraPreviewView.setVideoOrientation(VideoOrientation.landscapeLeft)
                else -> cameraPreviewView.setVideoOrientation(VideoOrientation.landscapeRight)
            }
        }
    }

    fun getVideoMirroredOnStream(device: CaptureDevice): Boolean {
        if (device.position == CameraSelector.LENS_FACING_FRONT) {
            return database.mirrorFrontCameraOnStream
        }
        return false
    }

    private fun getVideoMirroredOnScreen(): Boolean {
        if (cameraPosition == CameraSelector.LENS_FACING_FRONT) {
            return !database.mirrorFrontCameraOnStream
        }
        return false
    }

    fun attachBackTripleLowEnergyCamera(force: Boolean = true) {
        TODO("no Android counterpart for AVCaptureDevice builtInTripleCamera; use CameraX")
    }

    fun attachBackDualLowEnergyCamera(force: Boolean = true) {
        TODO("no Android counterpart for AVCaptureDevice builtInDualCamera; use CameraX")
    }

    fun attachBackWideDualLowEnergyCamera(force: Boolean = true) {
        TODO("no Android counterpart for AVCaptureDevice builtInDualWideCamera; use CameraX")
    }

    fun attachCamera(scene: SettingsScene, position: Int) {
        val cameraDevice = preferredCamera(position = position)
        this.cameraDevice = cameraDevice
        setFocusAfterCameraAttach()
        cameraZoomLevelToXScale = cameraDevice
            ?.getZoomFactorScale(hasUltraWideCamera = hasUltraWideCamera(position = position)) ?: 1.0f
        val range = cameraDevice
            ?.getUIZoomRange(hasUltraWideCamera = hasUltraWideCamera(position = position))
        cameraZoomXMinimum = range?.first ?: 1.0f
        cameraZoomXMaximum = range?.second ?: 1.0f
        cameraPosition = position
        when (position) {
            CameraSelector.LENS_FACING_BACK -> {
                updateBackZoomSwitchTo()
                zoom.x = zoom.backX
            }
            CameraSelector.LENS_FACING_FRONT -> {
                updateFrontZoomSwitchTo()
                zoom.x = zoom.frontX
            }
            else -> {}
        }
        attachCameraFinalize(scene = scene)
    }

    private fun attachCameraFinalize(scene: SettingsScene) {
        lastAttachCompletedTime = null
        val isMirrored = getVideoMirroredOnScreen()
        val devices = getBuiltinCameraDevices(scene = scene, sceneDevice = cameraDevice)
        val showCameraPreview = updateShowCameraPreview()
        val attachCameraPreview = showCameraPreview || database.alwaysAttachCameraPreview
        cameraPreviewView.setDevices(
            ids = if (attachCameraPreview) {
                getCameraPreviewDeviceIds(scene = scene, sceneDevice = cameraDevice)
            } else {
                emptyList()
            },
        )
        val params = VideoUnitAttachParams(
            devices = devices,
            builtinDelay = database.debug.builtinAudioAndVideoDelay,
            cameraPreviewLayers = cameraPreviewView.previewLayers,
            attachCameraPreview = attachCameraPreview,
            showCameraPreview = showCameraPreview,
            externalDisplayPreview = externalDisplayPreview,
            bufferedVideo = null,
            preferredVideoStabilizationMode = getVideoStabilizationMode(scene = scene),
            ignoreFramesAfterAttachSeconds = getIgnoreFramesAfterAttachSeconds(),
            fillFrame = getFillFrame(scene = scene),
            isLandscapeStreamAndPortraitUi = isLandscapeStreamAndPortraitUi(),
            forceSceneTransition = database.forceSceneSwitchTransition,
            macScreenCapture = sceneNeedsMacScreenCapture(scene = scene),
            attachPhotoShoot = _photoShootEnabled.value || database.alwaysAttachPhotoShoot,
        )
        media.attachCamera(
            params = params,
            onSuccess = {
                streamPreviewView.isMirrored = isMirrored
                externalDisplayStreamPreviewView.isMirrored = isMirrored
                val x = setCameraZoomX(x = zoom.x)
                if (x != null) {
                    setZoomXWhenInRange(x = x)
                }
                val device = cameraDevice
                if (device != null) {
                    setExposureAndIsoAfterCameraAttach(device = device)
                    setWhiteBalanceAfterCameraAttach(device = device)
                    updateImageButtonState()
                }
                lastAttachCompletedTime = Instant.now()
                relaxedBitrateStartTime = lastAttachCompletedTime
                relaxedBitrate = database.debug.relaxedBitrate
                cameraPreviewView.select(id = devices.getSceneDevice()?.id)
                updateCameraPreviewRotation()
                updateVideoPreviews()
            },
        )
        zoom.xPinch = zoom.x
        zoom.hasZoom = true
        updateFrontZoomPresets()
        updateBackZoomPresets()
    }

    private fun getIgnoreFramesAfterAttachSeconds(): Double =
        database.debug.cameraSwitchRemoveBlackish.toDouble() + database.debug.builtinAudioAndVideoDelay

    private fun getIgnoreFramesAfterAttachSecondsReplaceCamera(): Double =
        if (database.forceSceneSwitchTransition) {
            database.debug.cameraSwitchRemoveBlackish.toDouble()
        } else {
            0.0
        }

    fun attachBufferedCamera(cameraId: UUID, scene: SettingsScene) {
        cameraDevice = null
        cameraPosition = null
        streamPreviewView.isMirrored = false
        externalDisplayStreamPreviewView.isMirrored = false
        zoom.hasZoom = false
        cameraPreviewView.setDevices(ids = emptyList())
        media.attachBufferedCamera(
            devices = getBuiltinCameraDevices(scene = scene, sceneDevice = null),
            builtinDelay = database.debug.builtinAudioAndVideoDelay,
            cameraPreviewLayers = cameraPreviewView.previewLayers,
            attachCameraPreview = false,
            showCameraPreview = updateShowCameraPreview(),
            externalDisplayPreview = externalDisplayPreview,
            cameraId = cameraId,
            preferredVideoStabilizationMode = getVideoStabilizationMode(scene = scene),
            ignoreFramesAfterAttachSeconds = getIgnoreFramesAfterAttachSecondsReplaceCamera(),
            fillFrame = getFillFrame(scene = scene),
            isLandscapeStreamAndPortraitUi = isLandscapeStreamAndPortraitUi(),
            forceSceneTransition = database.forceSceneSwitchTransition,
            macScreenCapture = sceneNeedsMacScreenCapture(scene = scene),
            attachPhotoShoot = _photoShootEnabled.value || database.alwaysAttachPhotoShoot,
        )
        media.usePendingAfterAttachEffects()
        updateVideoPreviews()
        zoomPresetsMayHaveChanged()
    }

    fun attachExternalCamera(scene: SettingsScene) {
        attachCamera(scene = scene, position = CameraSelector.LENS_FACING_FRONT)
    }

    private fun getVideoStabilizationMode(scene: SettingsScene): SettingsVideoStabilizationMode =
        if (scene.overrideVideoStabilizationMode) {
            getVideoStabilization(mode = scene.videoStabilizationMode)
        } else {
            getVideoStabilization(mode = database.videoStabilizationMode)
        }

    private fun getVideoStabilization(mode: SettingsVideoStabilizationMode): SettingsVideoStabilizationMode =
        when (mode) {
            SettingsVideoStabilizationMode.off -> SettingsVideoStabilizationMode.off
            SettingsVideoStabilizationMode.standard -> SettingsVideoStabilizationMode.standard
            SettingsVideoStabilizationMode.cinematic ->
                SettingsVideoStabilizationMode.cinematic
            SettingsVideoStabilizationMode.cinematicExtendedEnhanced ->
                SettingsVideoStabilizationMode.cinematicExtendedEnhanced
        }

    fun setTorch(on: Boolean) {
        streamOverlay._isTorchOn.value = on
        updateTorch()
    }

    fun toggleTorch() {
        streamOverlay._isTorchOn.value = !streamOverlay._isTorchOn.value
        updateTorch()
    }

    fun updateTorch() {
        media.setTorchLevel(level = database.torchLevel)
        media.setTorch(on = streamOverlay._isTorchOn.value)
        remoteControlStateChanged(
            state = RemoteControlAssistantStreamerState(torchOn = streamOverlay._isTorchOn.value),
        )
    }

    fun setTorchLevel(level: Float) {
        media.setTorchLevel(level = level)
    }

    fun toggleMute() {
        audio.muted = !audio.muted
        updateMute()
    }

    fun setMuted(value: Boolean) {
        audio.muted = value
        updateMute()
    }

    fun updateMute() {
        media.setMute(on = audio.muted)
        if (isWatchLocal()) {
            sendIsMutedToWatch(isMuteOn = audio.muted)
        }
        updateTextEffects(now = Instant.now(), timestamp = Instant.now())
        forceUpdateTextEffects()
        remoteControlStateChanged(state = RemoteControlAssistantStreamerState(muted = audio.muted))
    }

    private fun makeFlameRedToast() {
        makeToast(title = flameRedMessage, subTitle = flameRedSubMessage, vibrate = true)
    }

    fun startMotionDetection() {
        TODO("no Android counterpart for CMMotionManager.startDeviceMotionUpdates")
    }

    fun stopMotionDetection() {
        TODO("no Android counterpart for CMMotionManager.stopDeviceMotionUpdates")
    }

    fun reloadCameraLevel() {
        if (_showingCameraLevel.value) {
            cameraLevel.start(portrait = _stream.value.portrait)
        } else {
            cameraLevel.stop()
        }
    }

    fun preferredCamera(position: Int): CaptureDevice? {
        val scene = findEnabledScene(id = sceneSelector.selectedSceneId)
        if (scene != null) {
            val id = when (position) {
                CameraSelector.LENS_FACING_BACK -> scene.videoSource.backCameraId
                CameraSelector.LENS_FACING_FRONT -> scene.videoSource.frontCameraId
                else -> scene.videoSource.externalCameraId
            }
            return CaptureDevice(id)
        }
        return null
    }

    fun isShowingStatusCamera(): Boolean = database.show.cameras && !isChatPhone()

    fun isShowingStatusMic(): Boolean = database.show.microphone && !isChatPhone()

    fun isShowingStatusAudioLevel(): Boolean = database.show.audioLevel && !isChatPhone()

    fun isShowingStatusEvents(): Boolean = database.show.events && isEventsConfigured()

    fun isShowingStatusViewers(): Boolean =
        _isLive.value && database.show.viewers && statusTopLeft._streamingPlatformStatuses.value.isNotEmpty()

    private fun statusStreamText(): String {
        val proto = _stream.value.protocolString()
        val resolution = currentResolution ?: _stream.value.resolutionString()
        val codec = _stream.value.codecString()
        val rateControl = _stream.value.rateControlString()
        val bitrate = _stream.value.bitrateString()
        val audioCodec = _stream.value.audioCodecString()
        val audioBitrate = _stream.value.audioBitrateString()
        val fps = if (lowLightBoost) {
            "${currentFps ?: _stream.value.fps} LLB"
        } else {
            (currentFps ?: _stream.value.fps).toString()
        }
        return "${_stream.value.name} ($resolution, $fps, $proto, $codec $rateControl $bitrate, " +
            "$audioCodec $audioBitrate)"
    }

    fun updateStatusStreamText() {
        val status = statusStreamText()
        if (status != statusTopLeft._streamText.value) {
            statusTopLeft._streamText.value = status
        }
    }

    private fun updateStatusEventsText() {
        val status = if (!isEventsConfigured()) {
            localized("Not configured")
        } else if (isRemoteControlChatAndEvents(platform = null)) {
            if (isRemoteControlStreamerConnected()) {
                localized("Connected (remote control)")
            } else {
                localized("Disconnected (remote control)")
            }
        } else {
            if (isEventsConnected()) {
                localized("Connected")
            } else {
                localized("Disconnected")
            }
        }
        if (status != statusTopLeft._statusEventsText.value) {
            statusTopLeft._statusEventsText.value = status
        }
    }

    fun statusViewersText(): String =
        if (isViewersConfigured()) {
            statusTopLeft._numberOfViewersCompact.value
        } else {
            localized("Not configured")
        }

    fun isShowingStatusAdsRemainingTimer(): Boolean =
        statusTopRight._adsRemainingTimerStatus.value != noValue

    fun isShowingStatusIngests(): Boolean = database.show.ingests && isIngestsConfigured()

    fun isIngestsConfigured(): Boolean =
        rtmpServerEnabled() ||
            srtlaServerEnabled() ||
            ristServerEnabled() ||
            ingests.rtsp.isNotEmpty() ||
            whipServerEnabled() ||
            ingests.whep.isNotEmpty() ||
            ingests.srt.isNotEmpty()

    fun isShowingStatusMoblink(): Boolean = database.show.moblink && isAnyMoblinkConfigured()

    fun isAnyMoblinkConfigured(): Boolean =
        isMoblinkRelayConfigured() || isMoblinkStreamerConfigured()

    fun isShowingStatusDjiDevices(): Boolean =
        database.show.djiDevices && statusTopRight._djiDevicesStatus.value != noValue

    fun isShowingStatusBitrate(): Boolean = database.show.speed && _isLive.value

    fun isShowingStatusStreamUptime(): Boolean = database.show.uptime && _isLive.value

    fun isShowingStatusBonding(): Boolean = database.show.bonding && isStatusBondingActive()

    fun isStatusBondingActive(): Boolean = _stream.value.isBonding() && _isLive.value

    fun isShowingStatusBondingRtts(): Boolean =
        database.show.bondingRtts && isStatusBondingRttsActive()

    fun isStatusBondingRttsActive(): Boolean = _stream.value.isBonding() && _isLive.value

    fun isShowingStatusReplay(): Boolean = _stream.value.replay.enabled && !isChatPhone()

    fun isShowingStatusBrowserWidgets(): Boolean =
        database.show.browserWidgets && isStatusBrowserWidgetsActive() && !isChatPhone()

    fun isShowingStatusCatPrinter(): Boolean =
        database.show.catPrinter && isAnyCatPrinterConfigured()

    fun isShowingStatusWorkoutDevice(): Boolean =
        database.show.workoutDevice && isAnyWorkoutDeviceConfigured()

    fun isShowingStatusFixedHorizon(): Boolean {
        if (isChatPhone()) {
            return false
        }
        val scene = getSelectedScene()
        return if (scene != null) isFixedHorizonEnabled(scene = scene) else false
    }

    fun isStatusBrowserWidgetsActive(): Boolean =
        statusTopRight._browserWidgetsStatus.value.isNotEmpty() &&
            statusTopRight._browserWidgetsStatusChanged.value

    fun isShowingStatusCpu(): Boolean = database.show.systemMonitor

    fun setBlurFaces(on: Boolean) {
        database.face.blurFaces = on
        setFilterQuickButton(type = SettingsQuickButtonType.blurFaces, on = on)
        updateFaceFilterSettings()
    }

    fun toggleBlurFaces() {
        database.face.blurFaces = !database.face.blurFaces
        toggleFilterQuickButton(type = SettingsQuickButtonType.blurFaces)
        updateFaceFilterSettings()
    }

    fun toggleBlurText() {
        database.face.blurText = !database.face.blurText
        toggleFilterQuickButton(type = SettingsQuickButtonType.blurText)
        updateFaceFilterSettings()
    }

    fun setPrivacy(on: Boolean) {
        database.face.blurBackground = on
        setFilterQuickButton(type = SettingsQuickButtonType.privacy, on = on)
        updateFaceFilterSettings()
    }

    fun togglePrivacy() {
        database.face.blurBackground = !database.face.blurBackground
        toggleFilterQuickButton(type = SettingsQuickButtonType.privacy)
        updateFaceFilterSettings()
    }

    fun setMoblinInMouth(on: Boolean) {
        database.face.showMoblin = on
        setFilterQuickButton(type = SettingsQuickButtonType.moblinInMouth, on = on)
        updateFaceFilterSettings()
    }

    fun toggleMoblinInMouth() {
        database.face.showMoblin = !database.face.showMoblin
        toggleFilterQuickButton(type = SettingsQuickButtonType.moblinInMouth)
        updateFaceFilterSettings()
    }

    fun triggerGlasses() {
        triggerQuickButtonEffect(
            type = SettingsQuickButtonType.glasses,
            effect = glassesEffect,
            duration = 5.8,
        )
    }

    fun triggerSparkle() {
        triggerQuickButtonEffect(
            type = SettingsQuickButtonType.sparkle,
            effect = sparkleEffect,
            duration = 1.3,
        )
    }

    private fun triggerQuickButtonEffect(
        type: SettingsQuickButtonType,
        effect: AlertsEffect?,
        duration: Double,
    ) {
        if (getQuickButton(type = type)?.isOn != false) {
            return
        }
        setQuickButton(type = type, isOn = true)
        effect?.play(alert = AlertsEffectAlert.quickButton)
        mainScope.launch {
            delay((duration * 1000).toLong())
            setQuickButton(type = type, isOn = false)
        }
    }

    fun handleWorkout(stats: WatchProtocolWorkoutStats) {
        val heartRate = stats.heartRate
        if (heartRate != null) {
            heartRates[""] = heartRate
        }
        val activeEnergyBurned = stats.activeEnergyBurned
        if (activeEnergyBurned != null) {
            workoutActiveEnergyBurned = activeEnergyBurned
        }
        val distance = stats.distance
        if (distance != null) {
            workoutDistance = distance
        }
        val stepCount = stats.stepCount
        if (stepCount != null) {
            workoutStepCount = stepCount
        }
        val power = stats.power
        if (power != null) {
            workoutPower = power
        }
        val cyclingPower = stats.cyclingPower
        if (cyclingPower != null) {
            setCyclingPower(cyclingPower, source = CyclingSource.watch)
        }
        val cyclingCadence = stats.cyclingCadence
        if (cyclingCadence != null) {
            setCyclingCadence(cyclingCadence, source = CyclingSource.watch)
        }
    }

    override fun alertsMakeErrorToast(title: String) {
        makeErrorToastMain(title = title)
    }

    override fun alertsMakeTwitchSegments(
        text: String,
        fragments: List<TwitchEventSubMessageFragment>,
        bits: String?,
    ): List<ChatPostSegment> = makeTwitchAlertSegments(text = text, fragments = fragments, bits = bits)

    override fun alertsMakeKickSegments(text: String): List<ChatPostSegment> =
        kickPusher?.makeChatPostSegments(content = text) ?: makeChatPostTextSegments(text = text)

    override fun faxReceiverPrint(image: Bitmap) {
        printAllCatPrinters(image = image)
    }

    fun documentPicker(urls: List<String>) {
        val url = urls.firstOrNull() ?: return
        mainScope.launch {
            onDocumentPickerUrl?.invoke(url)
        }
    }

    fun startLiveActivity() {
        TODO("no Android counterpart for ActivityKit")
    }

    fun stopLiveActivity() {
        TODO("no Android counterpart for ActivityKit")
    }

    fun setupMacStatusItem() {
        TODO("no Android counterpart for macOS status item")
    }

    fun stopMacStatusItem() {
        TODO("no Android counterpart for macOS status item")
    }
}

fun Model.toggleDrawOnStream() {
    _showDrawOnStream.value = !_showDrawOnStream.value
    drawOnStreamUpdateButtonState()
}

fun Model.drawOnStreamLineComplete() {
    drawOnStreamEffect.updateOverlay(
        videoSize = media.getCanvasSize(),
        size = drawOnStreamSize,
        lines = drawOnStream._lines.value,
        mirror = streamOverlay._isFrontCameraSelected.value && !database.mirrorFrontCameraOnStream,
    )
    media.registerEffect(drawOnStreamEffect)
    drawOnStreamUpdateButtonState()
}

fun Model.drawOnStreamWipe() {
    drawOnStream._lines.value = emptyList()
    drawOnStreamEffect.updateOverlay(
        videoSize = media.getCanvasSize(),
        size = drawOnStreamSize,
        lines = drawOnStream._lines.value,
        mirror = streamOverlay._isFrontCameraSelected.value && !database.mirrorFrontCameraOnStream,
    )
    media.unregisterEffect(drawOnStreamEffect)
    drawOnStreamUpdateButtonState()
}

fun Model.drawOnStreamUndo() {
    if (drawOnStream._lines.value.isEmpty()) {
        return
    }
    drawOnStream._lines.value = drawOnStream._lines.value.dropLast(1)
    drawOnStreamEffect.updateOverlay(
        videoSize = media.getCanvasSize(),
        size = drawOnStreamSize,
        lines = drawOnStream._lines.value,
        mirror = streamOverlay._isFrontCameraSelected.value && !database.mirrorFrontCameraOnStream,
    )
    if (drawOnStream._lines.value.isEmpty()) {
        media.unregisterEffect(drawOnStreamEffect)
    }
    drawOnStreamUpdateButtonState()
}

fun Model.drawOnStreamUpdateButtonState() {
    setQuickButton(
        type = SettingsQuickButtonType.draw,
        isOn = _showDrawOnStream.value || drawOnStream._lines.value.isNotEmpty(),
    )
}
