package com.moblin.android.various.model

import android.bluetooth.BluetoothAdapter
import android.graphics.Bitmap
import android.hardware.SensorManager
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraInfo
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
import com.moblin.android.videoeffects.browser.*
import com.moblin.android.videoeffects.crt.*
import com.moblin.android.videoeffects.replay.*
import com.moblin.android.videoeffects.scoreboard.*
import com.moblin.android.videoeffects.text.*
import com.moblin.android.videoeffects.vtuber.*
import com.moblin.android.view.controlbar.*
import com.moblin.android.view.stream.*
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import com.moblin.android.AppDelegate
import android.os.BatteryManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.WindowManager
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.various.utils.getUIZoomRange
import com.moblin.android.various.utils.getZoomFactorScale
import com.moblin.android.various.utils.hasUltraWideBackCamera
import com.moblin.android.platform.coregraphics.toCGSize

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
    val debugLines = MutableStateFlow<List<String>>(emptyList())
}

class StreamUptimeProvider {
    val uptime = MutableStateFlow(noValue)
}

class ProgressBar {
    val progress = MutableStateFlow(0f)
    val goal = MutableStateFlow(1f)
}

class Banners {
    val minimized = MutableStateFlow(false)
}

class HypeTrain {
    val level = MutableStateFlow<Int?>(null)
    val progress = MutableStateFlow<ProgressBar?>(null)
    val message = MutableStateFlow("")
    var expiresAt: Instant? = null
}

enum class RaidState { idle, ongoing, cancelling, completed }

class Raid {
    val state = MutableStateFlow(RaidState.idle)
    val channelImage = MutableStateFlow("")
    val channelLogin = MutableStateFlow("")
    val message = MutableStateFlow("")
    val progress = MutableStateFlow(ProgressBar())
}

enum class TwitchPollState { idle, ongoing, completed }

data class TwitchPollChoice(val id: String, val title: String, val votes: Int)

class TwitchPoll {
    val state = MutableStateFlow(TwitchPollState.idle)
    val title = MutableStateFlow("")
    val choices = MutableStateFlow<List<TwitchPollChoice>>(emptyList())
    val totalVotes = MutableStateFlow(0)
    val message = MutableStateFlow("")
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
    val state = MutableStateFlow(TwitchPredictionState.idle)
    val title = MutableStateFlow("")
    val outcomes = MutableStateFlow<List<TwitchPredictionOutcome>>(emptyList())
    val totalChannelPoints = MutableStateFlow(0)
    val message = MutableStateFlow("")
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
    val speedAndTotal = MutableStateFlow(noValue)
}

class Bitrate {
    val speedAndTotal = MutableStateFlow(noValue)
    val speedMbpsOneDecimal = MutableStateFlow(noValue)
    val statusColor = MutableStateFlow(Color.White)
    val statusIconColor = MutableStateFlow<Color?>(null)
}

class Bonding {
    val statistics = MutableStateFlow(noValue)
    val rtts = MutableStateFlow(noValue)
    val pieChartPercentages = MutableStateFlow<List<BondingPercentage>>(emptyList())
    var statisticsFormatter = BondingStatisticsFormatter()
}

class Show {
    val cameraPreview = MutableStateFlow(false)
    val chatPhone = MutableStateFlow(false)
}

class Battery {
    val level = MutableStateFlow(0.0)
    val state = MutableStateFlow(BatteryState.full)
}

class StatusOther {
    val ipStatuses = MutableStateFlow<List<IPMonitor.Status>>(emptyList())
    val thermalState = MutableStateFlow(MoblinkThermalState.white)
    val digitalClock = MutableStateFlow(noValue)

    fun isConnectedToIpv4WiFi(): Boolean = ipStatuses.value.any {
        it.interfaceType == IPMonitor.InterfaceType.wifi &&
            it.ipType.toString().lowercase() == "ipv4"
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
    val numberOfViewersIconColor = MutableStateFlow(Color.White)
    val numberOfViewersCompact = MutableStateFlow(noValue)
    val streamingPlatformStatuses = MutableStateFlow<List<StreamingPlatformStatus>>(emptyList())
    val chatPlatformStatuses = MutableStateFlow<List<ChatPlatformStatus>>(emptyList())
    val statusEventsText = MutableStateFlow(noValue)
    val statusChatText = MutableStateFlow(noValue)
    val streamText = MutableStateFlow(noValue)
    val statusCameraText = MutableStateFlow(noValue)
    val statusObsText = MutableStateFlow(noValue)
}

class SystemMonitor {
    val appCpu = MutableStateFlow(0)
    val cpu = MutableStateFlow(0)
    val ram = MutableStateFlow(0)

    fun format(): String = "${appCpu.value}%/${cpu.value}% ${ram.value} MB"

    fun formatShort(): String = cpu.value.toString()
}

class StatusTopRight {
    val browserWidgetsStatusChanged = MutableStateFlow(false)
    val remoteControlOk = MutableStateFlow(false)
    val remoteControlStatus = MutableStateFlow(noValue)
    val djiDevicesStatus = MutableStateFlow(noValue)
    val browserWidgetsStatus = MutableStateFlow(noValue)
    val catPrinterStatus = MutableStateFlow(noValue)
    val workoutDeviceStatus = MutableStateFlow(noValue)
    val fixedHorizonStatus = MutableStateFlow(noValue)
    val adsRemainingTimerStatus = MutableStateFlow(noValue)
    val blackSharkCoolerPhoneTemp = MutableStateFlow<Int?>(null)
    val blackSharkCoolerExhaustTemp = MutableStateFlow<Int?>(null)
    val blackSharkCoolerDeviceState = MutableStateFlow<BlackSharkCoolerDeviceState?>(null)
    val gameControllersTotal = MutableStateFlow(noValue)
    val djiDeviceStreamingState = MutableStateFlow<DjiDeviceState?>(null)
    val catPrinterState = MutableStateFlow<CatPrinterState?>(null)
    val workoutDeviceState = MutableStateFlow<WorkoutDeviceState?>(null)
    val location = MutableStateFlow(noValue)
    val isLowPowerMode = MutableStateFlow(false)
}

class Toast {
    val showingToast = MutableStateFlow(false)
    val toast = MutableStateFlow(AlertToast(type = AlertToastType.regular, title = ""))
    var onTapped: (() -> Unit)? = null
}

class SceneSelector {
    val trigger = MutableStateFlow(0)
    val sceneIndex = MutableStateFlow(0)
    var selectedSceneId = UUID.randomUUID()
}

class StreamOverlay {
    val showMediaPlayerControls = MutableStateFlow(false)
    val isFrontCameraSelected = MutableStateFlow(false)
    val showingCamera = MutableStateFlow(false)
    val showingPinch = MutableStateFlow(false)
    val showingReplay = MutableStateFlow(false)
    val showingPixellate = MutableStateFlow(false)
    val showingWhirlpool = MutableStateFlow(false)
    val showingBeauty = MutableStateFlow(false)
    val showingVideoPreview = MutableStateFlow(false)
    val isTorchOn = MutableStateFlow(false)
}

class Store {
    val myIcons = MutableStateFlow<List<Icon>>(emptyList())
    val iconsInStore = MutableStateFlow<List<Icon>>(emptyList())
    val iconImage = MutableStateFlow(plainIcon.id)
    var hasBoughtSomething: Boolean = true
}

class DrawOnStream {
    val lines = MutableStateFlow<List<DrawOnStreamLine>>(emptyList())
    val selectedColor = MutableStateFlow(Color(0xFFFFC0CB))
    val selectedWidth = MutableStateFlow(4f)
}

class StealthMode {
    var hideButtonsTimer = MainTimer()
    val showButtons = MutableStateFlow(true)
    val image = MutableStateFlow<Bitmap?>(null)
}

class ControlBar {
    val backgroundImage = MutableStateFlow<Bitmap?>(null)
    val backgroundImageOpacity = MutableStateFlow(1.0)
}

class QuickButtonChat {
    val showAllChatMessages = MutableStateFlow(true)
    val showFirstTimeChatterMessage = MutableStateFlow(true)
    val showNewFollowerMessage = MutableStateFlow(true)
    val chatAlertsPosts = MutableStateFlow<ArrayDeque<ChatPost>>(ArrayDeque())
    val pausedChatAlertsPostsCount = MutableStateFlow(0)
    val chatAlertsPaused = MutableStateFlow(false)
}

class ExternalDisplay {
    val chatEnabled = MutableStateFlow(false)
}

class GoProState {
    val launchLiveStreamSelection = MutableStateFlow<UUID?>(null)
    val wifiCredentialsSelection = MutableStateFlow<UUID?>(null)
    val rtmpUrlSelection = MutableStateFlow<UUID?>(null)
}

class QuickButtons {
    val pairs = MutableStateFlow<List<List<QuickButtonPair>>>(List(controlBarPages) { emptyList() })
    val selectedButtonType = MutableStateFlow<SettingsQuickButtonType?>(null)
    var page = 1
    val activePage = MutableStateFlow<Int?>(1)
}

class Snapshot {
    val countdown = MutableStateFlow(0)
    val currentJob = MutableStateFlow<SnapshotJob?>(null)
}

class Orientation {
    val isPortrait = MutableStateFlow(false)
}

class CameraLevel {
    val angle = MutableStateFlow<Double?>(null)

    fun start(portrait: Boolean) {
        Unit
    }

    fun stop() {
        Unit
    }
}

private val enterForegroundCountStorage = SimpleIntStorage(key = "enterForegroundCount")

private fun toComposeSize(size: android.util.Size): Size =
    Size(size.width.toFloat(), size.height.toFloat())

class Model : FaxReceiverDelegate, AlertsEffectDelegate {
    var enterForegroundCount: Int
        get() = enterForegroundCountStorage.get()
        set(value) = enterForegroundCountStorage.set(value)

    val showingPanel = MutableStateFlow(ShowingPanel.none)
    val panelHidden = MutableStateFlow(false)
    val showStealthMode = MutableStateFlow(false)
    val lockScreen = MutableStateFlow(false)
    val isLive = MutableStateFlow(false)
    val isRecording = MutableStateFlow(false)
    val isPreviewStreaming = MutableStateFlow(false)
    val browsers = MutableStateFlow<List<Browser>>(emptyList())
    val interactiveBrowsers = MutableStateFlow(false)
    val showingGrid = MutableStateFlow(false)
    val showingCameraLevel = MutableStateFlow(false)
    val showingRemoteControl = MutableStateFlow(false)
    val portraitVideoOffsetFromTop = MutableStateFlow(0.0)
    val currentStreamId = MutableStateFlow(UUID.randomUUID())
    val showTwitchAuth = MutableStateFlow(false)
    val showModerationAuth = MutableStateFlow(false)
    val presentingModeration = MutableStateFlow(false)
    val presentingPredefinedMessages = MutableStateFlow(false)
    val presentingSettingsImportConfirmation = MutableStateFlow(false)
    var pendingSettingsImportAction: (() -> Unit)? = null
    val presentingStreamImportCollisionConfirmation = MutableStateFlow(false)
    var pendingStreamImportCollisionAction: ((replaceExisting: Boolean) -> Unit)? = null
    var pendingStreamImportCollisionTitle: String = ""
    val showDrawOnStream = MutableStateFlow(false)
    val showLocalOverlays = MutableStateFlow(true)
    val showBrowser = MutableStateFlow(false)
    val showNavigation = MutableStateFlow(false)
    val webBrowserUrl = MutableStateFlow("")
    val quickButtonSettingsButton = MutableStateFlow<SettingsQuickButton?>(null)
    val bluetoothAllowed = MutableStateFlow(false)
    val sceneSettingsPanelSceneId = MutableStateFlow(1)
    val cameraControlEnabled = MutableStateFlow(false)
    val stream = MutableStateFlow<SettingsStream>(fallbackStream)
    val layout = MutableStateFlow<SettingsWidgetLayout?>(null)
    val workoutType = MutableStateFlow<WatchProtocolWorkoutType?>(null)
    val photoShootEnabled = MutableStateFlow(false)

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
    var faceBackgroundImage: com.moblin.android.platform.coreimage.CIImage? = null
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
    val streamPreviewView = PreviewView(context = AppDelegate.context)
    val externalDisplayStreamPreviewView = PreviewView(context = AppDelegate.context)
    val cameraPreviewView = CameraPreviewUiView(context = AppDelegate.context)
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
    internal var replayFrameExtractor: ReplayFrameExtractor? = null
    internal var replayVideo: ReplayBufferFile? = null
    internal var replayBuffer = ReplayBuffer()
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
    private var ipMonitor = IPMonitor(context = AppDelegate.context)
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
    var cameraManEffect = CameraManEffect(moveVertically = false, speed = 1.0, alwaysMove = false)
    var pollEffect: PollEffect? = null
    var fixedHorizonEffect = FixedHorizonEffect()
    var glassesEffect: AlertsEffect? = null
    var sparkleEffect: AlertsEffect? = null
    var beautyEffect = BeautyEffect(fps = 30f)
    var replayEffect: ReplayEffect? = null
    var locationManager = Location(context = AppDelegate.context)
    var realtimeIrl: RealtimeIrl? = null
    var supportsAppleLog: Boolean = false
    val weatherManager = WeatherManager()
    val geographyManager = GeographyManager(context = AppDelegate.context)
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
        orientation.isPortrait.value = stream.value.portrait || database.portrait || isChatPhone()
    }

    fun isLandscapeStreamAndPortraitUi(): Boolean = !stream.value.portrait && database.portrait

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
                var settings = adaptiveBitrateFastSettings.copy()
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
                var settings = adaptiveBitrateBelaboxSettings.copy()
                settings.minimumBitrate =
                    (srt.adaptiveBitrate.belaboxSettings.minimumBitrate * 1000).toLong()
                media.setAdaptiveBitrateSettings(settings = settings)
            }
        }
    }

    fun updateAdaptiveBitrateRtmpIfEnabled() {
        var settings = adaptiveBitrateFastSettings.copy()
        settings.rttDiffHighAllowedSpike = 500.0
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
        if (showingPanel.value == ShowingPanel.quickButtonSettings) {
            quickButtons.selectedButtonType.value = null
        }
        if (showingPanel.value == panel) {
            showingPanel.value = ShowingPanel.none
        } else {
            showingPanel.value = panel
        }
        panelHidden.value = false
        for (pageButtonPairs in quickButtons.pairs.value) {
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
            setQuickButton(type = type, isOn = showingPanel.value == panel)
        }
    }

    fun setInteractiveBrowserWidgets(on: Boolean) {
        interactiveBrowsers.value = on
        setQuickButton(type = SettingsQuickButtonType.interactiveBrowserWidgets, isOn = on)
    }

    fun setAllowVideoRangePixelFormat() {
        allowVideoRangePixelFormat = database.debug.allowVideoRangePixelFormat.value
    }

    fun setNativeLowLightBoost() {
        nativeLowLightBoost = database.debug.nativeLowLightBoost.value
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
        toast.toast.value = AlertToast(
            type = AlertToastType.regular,
            title = title,
            subTitle = subTitle,
            subTitleFont = FontFamily.Default,
        )
        toast.onTapped = onTapped
        showToast()
        Log.d("Model", "toast: Info: $title: ${subTitle ?: "-"}")
        if (vibrate) {
            Unit
        }
    }

    fun makeWarningToast(title: String, subTitle: String? = null, vibrate: Boolean = false) {
        toast.toast.value = AlertToast(
            type = AlertToastType.regular,
            title = formatWarning(title),
            subTitle = subTitle,
            subTitleFont = FontFamily.Default,
        )
        showToast()
        Log.d("Model", "toast: Warning: $title: ${subTitle ?: "-"}")
        if (vibrate) {
            Unit
        }
    }

    fun makeErrorToast(
        title: String,
        font: FontFamily? = null,
        subTitle: String? = null,
        vibrate: Boolean = false,
    ) {
        toast.toast.value = AlertToast(
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
            Unit
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
        toast.showingToast.value = false
        mainScope.launch {
            toast.showingToast.value = true
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
                button.enabled.value && button.page.value == page + 1 &&
                    isQuickButtonAllowed(button.type)
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
            val newPairs = quickButtons.pairs.value.toMutableList()
            newPairs[page] = pairs
            quickButtons.pairs.value = newPairs
        }
    }

    fun getQuickButtonPairs(page: Int): List<QuickButtonPair> {
        if (page <= 0 || page > quickButtons.pairs.value.size) {
            return emptyList()
        }
        return quickButtons.pairs.value[page - 1]
    }

    fun setAllowHapticsAndSystemSoundsDuringRecording() {
        Unit
    }

    private fun removeUnusedKeychainItems() {
        val streamIds = database.streams.map { it.id }
        removeUnusedTwitchAccessTokensInKeychain(usedStreamIds = streamIds)
        removeUnusedKickAccessTokensInKeychain(usedStreamIds = streamIds)
        removeUnusedYouTubeAuthStatesInKeychain(usedStreamIds = streamIds)
    }

    fun setup() {
        battery.level.value = getBatteryLevel()
        bluetoothCentralManger = BluetoothAdapter.getDefaultAdapter()
        deleteTrash()
        removeUnusedKeychainItems()
        media = Media(delegate = object : MediaDelegate {
            override fun mediaOnSrtConnected() {
                this@Model.mediaOnSrtConnected()
            }
            override fun mediaOnSrtDisconnected(reason: String) {
                this@Model.mediaOnSrtDisconnected(reason)
            }
            override fun mediaOnRtmpConnected() {
                this@Model.mediaOnRtmpConnected()
            }
            override fun mediaOnRtmpDisconnected(message: String) {
                this@Model.mediaOnRtmpDisconnected(message)
            }
            override fun mediaOnRtmpDestinationConnected(destination: String) {
                this@Model.mediaOnRtmpDestinationConnected(destination)
            }
            override fun mediaOnRtmpDestinationDisconnected(destination: String) {
                this@Model.mediaOnRtmpDestinationDisconnected(destination)
            }
            override fun mediaOnRistConnected() {
                this@Model.mediaOnRistConnected()
            }
            override fun mediaOnRistDisconnected() {
                this@Model.mediaOnRistDisconnected()
            }
            override fun mediaOnWhipConnected() {
                this@Model.mediaOnWhipConnected()
            }
            override fun mediaOnWhipDisconnected(reason: String) {
                this@Model.mediaOnWhipDisconnected(reason)
            }
            override fun mediaOnMobcamConnected() {
                this@Model.mediaOnMobcamConnected()
            }
            override fun mediaOnMobcamDisconnected(reason: String) {
                this@Model.mediaOnMobcamDisconnected(reason)
            }
            override fun mediaOnWhipPerform(request: okhttp3.Request, queue: kotlinx.coroutines.CoroutineDispatcher, completion: ((ByteArray?, okhttp3.Response?, Throwable?) -> Unit)?) {
                this@Model.mediaOnWhipPerform(request, queue, completion)
            }
            override fun mediaOnAudioBuffer(sampleBuffer: MediaSample) {
                this@Model.mediaOnAudioBuffer(sampleBuffer)
            }
            override fun mediaOnLowFpsImage(lowFpsImage: ByteArray?, frameNumber: Long) {
                this@Model.mediaOnLowFpsImage(lowFpsImage, frameNumber.toULong())
            }
            override fun mediaOnAttachCameraError() {
                this@Model.mediaOnAttachCameraError()
            }
            override fun mediaOnCaptureSessionError(message: String) {
                this@Model.mediaOnCaptureSessionError(message)
            }
            override fun mediaOnBufferedVideoReady(cameraId: UUID) {
                this@Model.mediaOnBufferedVideoReady(cameraId)
            }
            override fun mediaOnBufferedVideoRemoved(cameraId: UUID) {
                this@Model.mediaOnBufferedVideoRemoved(cameraId)
            }
            override fun mediaOnEncoderResolutionChanged(resolution: android.util.Size) {
                this@Model.mediaOnEncoderResolutionChanged(resolution)
            }
            override fun mediaOnRecorderInitSegment(data: ByteArray) {
                this@Model.mediaOnRecorderInitSegment(data)
            }
            override fun mediaOnRecorderDataSegment(segment: RecorderDataSegment) {
                this@Model.mediaOnRecorderDataSegment(segment)
            }
            override fun mediaOnRecorderFinished() {
                this@Model.mediaOnRecorderFinished()
            }
            override fun mediaOnNoTorch() {
                this@Model.mediaOnNoTorch()
            }
            override fun mediaOnFps(fps: Int) {
                this@Model.mediaOnFps(fps)
            }
            override fun mediaMoblinkStreamerDestinationAddress(address: String, port: Int) {
                this@Model.mediaMoblinkStreamerDestinationAddress(address, port)
            }
            override fun mediaMoblinkStreamerRestartTunnel(relayId: UUID) {
                this@Model.mediaMoblinkStreamerRestartTunnel(relayId)
            }
            override fun mediaSetZoomX(x: Float) {
                this@Model.mediaSetZoomX(x)
            }
            override fun mediaSetExposureBias(bias: Float) {
                this@Model.mediaSetExposureBias(bias)
            }
            override fun mediaSelectedFps(auto: Boolean) {
                this@Model.mediaSelectedFps(auto)
            }
            override fun mediaError(error: Throwable) {
                this@Model.mediaError(error)
            }
        })
        setupAppIntents()
        faxReceiver.delegate = this
        fixAlertMediasNoUpdate()
        setAllowVideoRangePixelFormat()
        setNativeLowLightBoost()
        setHighQualityDownsampling()
        setExternalDisplayContent()
        portraitVideoOffsetFromTop.value = database.portraitVideoOffsetFromTop
        loadTextWidgetStopwatches()
        quickButtonChatState.showFirstTimeChatterMessage.value =
            database.chat.showFirstTimeChatterMessage
        quickButtonChatState.showNewFollowerMessage.value = database.chat.showNewFollowerMessage
        autoSceneSwitcher.currentSwitcherId.value = database.autoSceneSwitchers.switcherId
        supportsAppleLog = hasAppleLog()
        chat.interactiveChat.value =
            getQuickButton(type = SettingsQuickButtonType.interactiveChat)?.isOn?.value ?: false
        chatActivityFeed.interactiveChat.value = chat.interactiveChat.value
        interactiveBrowsers.value =
            getQuickButton(type = SettingsQuickButtonType.interactiveBrowserWidgets)?.isOn?.value ?: false
        updateShowCameraPreview()
        show.chatPhone.value = isChatPhone()
        showChatLabelsForAWhile()
        updateScreenAutoOff()
        setDisplayPortrait(portrait = database.portrait)
        setBitrateDropFix()
        setupLogging()
        updateCameraLists()
        updateBatteryLevel()
        setPixelFormat()
        setupInputGainObserver()
        setupAudioSession()
        val camera = preferredCamera(position = CameraSelector.LENS_FACING_BACK)
        if (camera != null) {
            val range: Pair<Float, Float> = (camera.device as? AVCaptureDevice)?.getUIZoomRange(hasUltraWideBackCamera) ?: Pair(1f, 1f)
            cameraZoomXMinimum = range.first
            cameraZoomXMaximum = range.second
            val preset = zoom.backZoomPresets.value.firstOrNull()
            if (preset != null) {
                zoom.backPresetId.value = preset.id
                zoom.backX = preset.x
            } else {
                zoom.backX = cameraZoomXMinimum
            }
            zoom.x.value = zoom.backX
        }
        updateFrontZoomPresets()
        updateBackZoomPresets()
        zoom.frontPresetId.value = database.zoom.front[0].id
        streamPreviewView.videoGravity = VideoGravity.resizeAspect
        externalDisplayStreamPreviewView.videoGravity = VideoGravity.resizeAspect
        updateDigitalClock(now = Instant.now())
        twitchChat = TwitchChat(delegate = object : TwitchChatDelegate {
            override fun twitchChatMakeErrorToast(title: String, subTitle: String?) {
                this@Model.twitchChatMakeErrorToast(title, subTitle)
            }
            override fun twitchChatAppendMessage(messageId: String?, displayName: String, user: String, userId: String?, userColor: RgbColor?, userBadges: List<String>, segments: List<ChatPostSegment>, isAction: Boolean, isSubscriber: Boolean, isModerator: Boolean, bits: String?, highlight: ChatHighlight?, sourceChannelIcon: String?) {
                this@Model.twitchChatAppendMessage(messageId, displayName, user, userId, userColor, userBadges, segments, isAction, isSubscriber, isModerator, bits, highlight, sourceChannelIcon)
            }
            override fun twitchChatDeleteMessage(messageId: String) {
                this@Model.twitchChatDeleteMessage(messageId)
            }
            override fun twitchChatDeleteUser(userId: String) {
                this@Model.twitchChatDeleteUser(userId)
            }
        })
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
        store.iconImage.value = database.iconImage
        mainScope.launch {
            appStoreUpdateListenerTask = listenForAppStoreTransactions(scope = mainScope)
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
        reloadLocation()
        currentStreamId.value = stream.value.id
        lutUpdated()
        addObserver("AVCaptureDevice.wasConnectedNotification", "handleCaptureDeviceWasConnected")
        addObserver("AVCaptureDevice.wasDisconnectedNotification", "handleCaptureDeviceWasDisconnected")
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
        goPro.launchLiveStreamSelection.value = database.goPro.selectedLaunchLiveStream
        goPro.wifiCredentialsSelection.value = database.goPro.selectedWifiCredentials
        goPro.rtmpUrlSelection.value = database.goPro.selectedRtmpUrl
        replay.speed.value = database.replay.speed
        gForceManager = motionManager?.let { GForceManager(sensorManager = it) }
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
        if (database.debug.bitrateDropFix.value) {
            Unit
        } else {
            Unit
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
            Log.d("Model", "Starting NTP client for pool ${stream.value.ntpPoolAddress}")
            com.moblin.android.platform.ntp.TrueTimeClient.sharedInstance.start(pool = listOf(stream.value.ntpPoolAddress))
        }
    }

    fun stopNtpClient() {
        Log.d("Model", "Stopping NTP client")
        com.moblin.android.platform.ntp.TrueTimeClient.sharedInstance.pause()
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
            SettingsExternalDisplayContent.stream -> externalDisplay.chatEnabled.value = false
            SettingsExternalDisplayContent.cleanStream -> externalDisplay.chatEnabled.value = false
            SettingsExternalDisplayContent.chat -> externalDisplay.chatEnabled.value = true
            SettingsExternalDisplayContent.mirror -> externalDisplay.chatEnabled.value = false
        }
        setCleanExternalDisplay()
        updateExternalMonitorWindow()
    }

    private fun setupSampleBufferReceiver() {
    }

    fun updateFaceFilterSettings() {
        faceEffect.setSettings(
            settings = database.face.toEffectSettings(
                backgroundImage = null,
                iconImage = null,
            ),
        )
    }

    private fun loadFaceIconImage(): Bitmap? =
        null
    fun updateImageButtonState() {
        var isOn = streamOverlay.showingCamera.value
        if (camera.bias.value != 0.0f) {
            isOn = true
        }
        if (camera.isWhiteBalanceLocked.value) {
            isOn = true
        }
        if (camera.isExposureAndIsoLocked.value) {
            isOn = true
        }
        if (camera.isFocusLocked.value) {
            isOn = true
        }
        if (isOn != getQuickButton(type = SettingsQuickButtonType.image)?.isOn?.value) {
            setQuickButton(type = SettingsQuickButtonType.image, isOn = isOn)
        }
    }

    fun updateBeautyButtonState() {
        var isOn = streamOverlay.showingBeauty.value
        if (database.beauty.enabled) {
            isOn = true
        }
        if (isOn != getQuickButton(type = SettingsQuickButtonType.beauty)?.isOn?.value) {
            setQuickButton(type = SettingsQuickButtonType.beauty, isOn = isOn)
        }
    }

    private fun handleIpStatusUpdate(statuses: List<IPMonitor.Status>) {
        statusOther.ipStatuses.value = statuses
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
                val networkInterface = SettingsNetworkInterfaceName()
                networkInterface.interfaceName = status.name
                networkInterface.name = status.name
                database.networkInterfaceNames.add(networkInterface)
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
                if (isRecording.value) {
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
                            stream.value.backgroundStreaming = true
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
        if (isRecording.value) {
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
        if (isRecording.value) {
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
        externalDisplayWindow = null
        updateExternalMonitorWindow()
        externalDisplayPreview = true
        attachCamera()
    }

    fun disableScreenPreview() {
        media.setScreenPreview(enabled = false)
    }

    fun maybeEnableScreenPreview() {
        if (showStealthMode.value) {
            return
        }
        media.setScreenPreview(enabled = true)
    }

    fun externalMonitorDisconnected() {
        externalDisplayWindow = null
        externalDisplayPreview = false
        attachCamera()
    }

    private fun updateExternalMonitorWindow() {
        if (externalDisplayWindow == null) {
            return
        }
        Unit
    }

    private fun backgroundRunLevel(): BackgroundRunLevel {
        if ((isLive.value || isRecording.value) && stream.value.backgroundStreaming) {
            return BackgroundRunLevel.Full
        }
        if (isLive.value || isRecording.value) {
            return BackgroundRunLevel.Off
        }
        val keepChatRunning = database.chat.background ||
            database.catPrinters.backgroundPrinting.value
        if (keepChatRunning || database.moblink.relay.enabled.value) {
            return BackgroundRunLevel.Service(
                keepChatRunning = keepChatRunning,
                keepBatteryLevelRunning = database.moblink.relay.enabled.value,
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
        if (stream.value.portrait) {
            media.setVideoOrientation(value = com.moblin.android.platform.avfoundation.AVCaptureVideoOrientation.portrait)
        } else {
            when (com.moblin.android.platform.uikit.UIDevice.current.orientation) {
                com.moblin.android.platform.uikit.UIDeviceOrientation.landscapeLeft -> media.setVideoOrientation(value = com.moblin.android.platform.avfoundation.AVCaptureVideoOrientation.landscapeRight)
                com.moblin.android.platform.uikit.UIDeviceOrientation.landscapeRight -> media.setVideoOrientation(value = com.moblin.android.platform.avfoundation.AVCaptureVideoOrientation.landscapeLeft)
                else -> {}
            }
        }
        updateCameraPreviewRotation()
    }

    private fun deviceRotation(): Int {
        val manager = AppDelegate.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        return when (manager.defaultDisplay.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

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
        speechToText?.tick(now = monotonicNow.toEpochMilli())
    }

    private fun handle1sTimer() {
        val now = Instant.now()
        val monotonicNow = Instant.now()
        updateDigitalClock(now = now)
        removeOldChatMessages(now = monotonicNow)
        if (inServiceBackground) {
            return
        }
        updateStreamUptime(now = monotonicNow.toEpochMilli())
        updateRecordingLength(now = now)
        media.updateSrtTransportBitrate()
        updateSpeed(now = monotonicNow.toEpochMilli())
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
        updateAverageSpeed(now = monotonicNow.toEpochMilli())
        updateTextEffects(now = now, timestamp = TimeSource.Monotonic.markNow())
        updateMapEffects()
        updateScoreboardEffects()
        updatePoll()
        updateObsSceneSwitcher(now = monotonicNow)
        weatherManager.setLocation(location = null)
        geographyManager.setLocation(location = null)
        updateBitrateStatus()
        updateAdsRemainingTimer(now = now)
        if (database.show.systemMonitor) {
            resourceUsage.update(now = monotonicNow.toEpochMilli())
            systemMonitor.appCpu.value = resourceUsage.getAppCpuUsage()
            systemMonitor.cpu.value = resourceUsage.getCpuUsage()
            systemMonitor.ram.value = resourceUsage.getMemoryUsage()
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
            sendThermalStateToWatch(
                thermalState = ThermalState.from(statusOther.thermalState.value.ordinal),
            )
        }
        teslaGetMediaState()
    }

    private fun handle10sTimer() {
        val monotonicNow = Instant.now()
        media.logStatistics()
        updateObsStatus()
        updateRemoteControlStatus()
        if (stream.value.enabled && database.debug.videoBitrateChange.value) {
            media.updateVideoStreamBitrate(bitrate = stream.value.bitrate)
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
        if (!isRecording.value) {
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
            statusTopRight.adsRemainingTimerStatus.value = noValue
        } else {
            statusTopRight.adsRemainingTimerStatus.value = secondsLeft.toInt().toString()
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
        for (streamingPlatformStatus in statusTopLeft.streamingPlatformStatuses.value) {
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
        if (newColor != statusTopLeft.numberOfViewersIconColor.value) {
            statusTopLeft.numberOfViewersIconColor.value = newColor
        }
        if (streamingPlatformsStatus != statusTopLeft.streamingPlatformStatuses.value) {
            statusTopLeft.streamingPlatformStatuses.value = streamingPlatformsStatus
        }
        val newNumberOfViewersCompact = updateViewersCompact(newNumberOfViewers, hasCount)
        if (newNumberOfViewersCompact != statusTopLeft.numberOfViewersCompact.value) {
            statusTopLeft.numberOfViewersCompact.value = newNumberOfViewersCompact
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
            Unit
        }
        return alertMediaStorage.makePath(id = soundId).toString()
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
            pollEffect = PollEffect(canvasSize = toComposeSize(media.getCanvasSize()))
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
        AppDelegate.orientationLock = if (database.portrait) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    fun reloadBrowserWidgets() {
        reloadHttpProxyServer()
        for (browser in browsers.value) {
            browser.browserEffect.reload()
        }
    }

    fun getQuickButton(type: SettingsQuickButtonType): SettingsQuickButton? =
        database.quickButtons.firstOrNull { it.type == type }

    fun showQuickButtonSettings(type: SettingsQuickButtonType) {
        quickButtonSettingsButton.value = getQuickButton(type = type)
        toggleShowingPanel(type = null, panel = ShowingPanel.none)
        toggleShowingPanel(type = null, panel = ShowingPanel.quickButtonSettings)
        quickButtons.selectedButtonType.value = type
    }

    fun setQuickButton(type: SettingsQuickButtonType, isOn: Boolean) {
        val button = getQuickButton(type = type) ?: return
        button.isOn.value = isOn
        val filter = RemoteControlFilter.fromType(type)
        if (filter != null) {
            remoteControlStateChanged(
                state = RemoteControlAssistantStreamerState(filters = mapOf(filter to button.isOn.value)),
            )
        }
    }

    fun toggleQuickButton(type: SettingsQuickButtonType) {
        val button = getQuickButton(type = type) ?: return
        setQuickButton(type = type, isOn = !button.isOn.value)
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
        streamOverlay.showingWhirlpool.value = on
        setFilterQuickButton(type = SettingsQuickButtonType.whirlpool, on = on)
    }

    fun toggleWhirlpoolQuickButton() {
        setWhirlpoolQuickButton(on = !streamOverlay.showingWhirlpool.value)
    }

    fun setBeautyQuickButton(on: Boolean) {
        streamOverlay.showingBeauty.value = on
        updateBeautyButtonState()
    }

    fun toggleBeautyQuickButton() {
        setBeautyQuickButton(on = !streamOverlay.showingBeauty.value)
    }

    fun toggleVideoPreview() {
        streamOverlay.showingVideoPreview.value = !streamOverlay.showingVideoPreview.value
        media.setVideoPreviewEnabled(enabled = streamOverlay.showingVideoPreview.value)
        updateVideoPreviews()
    }

    fun setPinchQuickButton(on: Boolean) {
        streamOverlay.showingPinch.value = on
        setFilterQuickButton(type = SettingsQuickButtonType.pinch, on = on)
    }

    fun togglePinchQuickButton() {
        setPinchQuickButton(on = !streamOverlay.showingPinch.value)
    }

    fun setPixellateQuickButton(on: Boolean) {
        streamOverlay.showingPixellate.value = on
        setFilterQuickButton(type = SettingsQuickButtonType.pixellate, on = on)
    }

    fun togglePixellateQuickButton() {
        setPixellateQuickButton(on = !streamOverlay.showingPixellate.value)
    }

    fun setCameraManQuickButton(on: Boolean) {
        cameraManEffect = CameraManEffect(
            moveVertically = database.debug.cameraManMoveVertically.value,
            speed = database.debug.cameraManSpeed.value,
            alwaysMove = database.debug.cameraManAlwaysMove.value,
        )
        setFilterQuickButton(type = SettingsQuickButtonType.cameraMan, on = on)
    }

    fun toggleCameraManQuickButton() {
        cameraManEffect = CameraManEffect(
            moveVertically = database.debug.cameraManMoveVertically.value,
            speed = database.debug.cameraManSpeed.value,
            alwaysMove = database.debug.cameraManAlwaysMove.value,
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
        workoutType.value = type
        setQuickButton(type = SettingsQuickButtonType.workout, isOn = type != null)
    }

    fun setMuteOn(value: Boolean) {
        audio.muted.value = value
        updateMute()
        setQuickButton(type = SettingsQuickButtonType.mute, isOn = value)
    }

    fun setIsMuted(value: Boolean) {
        setMuteOn(value = value)
    }

    fun updateScreenAutoOff() {
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
        stream.value.timecodesEnabled && !stream.value.ntpPoolAddress.isEmpty()

    fun setPixellateStrength(strength: Float) {
        pixellateEffect.setSettings(strength = strength)
    }

    fun setWhirlpoolAngle(angle: Float) {
        whirlpoolEffect.setSettings(angle = angle)
    }

    fun setPinchScale(scale: Float) {
        pinchEffect.setSettings(scale = scale)
    }

    private var loggerDebugEnabled = false

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
        database.chat.enabled && stream.value.openStreamingPlatformUrl != "" &&
            stream.value.openStreamingPlatformChannelId != ""

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
                url = stream.value.openStreamingPlatformUrl,
                channelId = stream.value.openStreamingPlatformChannelId,
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
        if (statusTopRight.browserWidgetsStatusChanged.value) {
            statusTopRight.browserWidgetsStatusChanged.value = false
        }
        val messages = mutableListOf<String>()
        for (browser in browsers.value) {
            val progress = browser.browserEffect.progress
            if (browser.browserEffect.isLoaded) {
                messages.add("${browser.browserEffect.host}: $progress%")
                if (progress != 100 ||
                    com.moblin.android.platform.core.ContinuousClock.now < browser.browserEffect.startLoadingTime.advanced(bySeconds = 5.0)
                ) {
                    if (!statusTopRight.browserWidgetsStatusChanged.value) {
                        statusTopRight.browserWidgetsStatusChanged.value = true
                    }
                }
            }
        }
        val message: String = if (messages.isEmpty()) noValue else messages.joinToString(", ")
        if (statusTopRight.browserWidgetsStatus.value != message) {
            statusTopRight.browserWidgetsStatus.value = message
        }
    }

    fun reloadViewers() {
        statusTopLeft.numberOfViewersIconColor.value = Color(0xFFFF9500)
        statusTopLeft.numberOfViewersCompact.value = noValue
        statusTopLeft.streamingPlatformStatuses.value = emptyList()
        if (isTwitchViewersConfigured()) {
            statusTopLeft.streamingPlatformStatuses.value = statusTopLeft.streamingPlatformStatuses.value +
                StreamingPlatformStatus(Platform.twitch, PlatformStatus.unknown)
        }
        if (isKickViewersConfigured()) {
            statusTopLeft.streamingPlatformStatuses.value = statusTopLeft.streamingPlatformStatuses.value +
                StreamingPlatformStatus(Platform.kick, PlatformStatus.unknown)
        }
        if (isYouTubeViewersConfigured()) {
            statusTopLeft.streamingPlatformStatuses.value = statusTopLeft.streamingPlatformStatuses.value +
                StreamingPlatformStatus(Platform.youTube, PlatformStatus.unknown)
        }
        if (isSoopViewersConfigured()) {
            statusTopLeft.streamingPlatformStatuses.value = statusTopLeft.streamingPlatformStatuses.value +
                StreamingPlatformStatus(Platform.soop, PlatformStatus.unknown)
        }
        twitchPlatformStatus = PlatformStatus.unknown
        twitchStreamUpdateTime = Instant.now().plusSeconds(15)
        youTubeStreamUpdateTimePollDelta = Duration.ofSeconds(15)
    }

    private fun logStatus() {
        if (loggerDebugEnabled && isLive.value) {
            Log.d(
                "Model",
                "Status: Bitrate: ${bitrate.speedAndTotal.value}, Uptime: ${streamUptime.uptime.value}",
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
        showLocalOverlays.value = !showLocalOverlays.value
    }

    fun toggleBrowser() {
        showBrowser.value = !showBrowser.value
    }

    fun toggleNavigation() {
        showNavigation.value = !showNavigation.value
    }

    fun toggleLockScreen() {
        lockScreen.value = !lockScreen.value
        setQuickButton(type = SettingsQuickButtonType.lockScreen, isOn = lockScreen.value)
        if (lockScreen.value) {
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
        val digitalClock = formatDate(date = now)
        if (statusOther.digitalClock.value != digitalClock) {
            statusOther.digitalClock.value = digitalClock
        }
    }

    private fun updateBatteryLevel() {
        val level = getBatteryLevel()
        if (level != battery.level.value) {
            battery.level.value = level
        }
        streamingHistoryStream?.updateLowestBatteryLevel(level = battery.level.value)
        if (battery.level.value <= 0.07 && !isBatteryCharging() && !isMac() &&
            battery.level.value != -1.0
        ) {
            makeWarningToast(title = lowBatteryMessage, vibrate = true)
            if (database.chat.botEnabled && database.chat.botSendLowBatteryWarning) {
                sendChatMessage(message = "Moblin bot: $lowBatteryMessage")
            }
        }
    }

    private fun updateBatteryState() {
        val state = getBatteryState()
        if (state != battery.state.value) {
            battery.state.value = state
            remoteControlStateChanged(
                state = RemoteControlAssistantStreamerState(batteryCharging = isBatteryCharging()),
            )
        }
    }

    fun isBatteryCharging(): Boolean =
        battery.state.value == BatteryState.charging || battery.state.value == BatteryState.full

    private fun getBatteryLevel(): Double {
        val manager = AppDelegate.context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) / 100.0
    }

    private fun getBatteryState(): BatteryState = BatteryState.unknown

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
                val total = sizeFormatter.string(fromByteCount = stats.total)
                serversSpeed = (serversSpeed * 0.7 + stats.speed * 0.3).toLong()
                val speed = formatBytesPerSecond(speed = 8 * serversSpeed)
                message = localized("$speed ($total) ${stats.numberOfClients}")
            } else {
                message = stats.numberOfClients.toString()
            }
        } else {
            message = noValue
        }
        if (message != ingests.speedAndTotal.value) {
            ingests.speedAndTotal.value = message
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
                total = result.total + serverStats.total.toLong(),
                speed = result.speed + serverStats.speed.toLong(),
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
                total = result.total + serverStats.total.toLong(),
                speed = result.speed + serverStats.speed.toLong(),
            )
        }
        return result
    }

    private fun updateSrtClientIngestsSpeed(stats: IngestStats): IngestStats {
        var result = stats
        for (client in ingests.srt) {
            val clientStats = client.updateStats()
            result = result.copy(
                total = result.total + clientStats.total.toLong(),
                speed = result.speed + clientStats.speed.toLong(),
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
                total = result.total + serverStats.total.toLong(),
                speed = result.speed + serverStats.speed.toLong(),
            )
        }
        return result
    }

    private fun updateRtspIngestsSpeed(stats: IngestStats): IngestStats {
        var result = stats
        for (client in ingests.rtsp) {
            val clientStats = client.updateStats()
            result = result.copy(
                total = result.total + clientStats.total.toLong(),
                speed = result.speed + clientStats.speed.toLong(),
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
                total = result.total + serverStats.total.toLong(),
                speed = result.speed + serverStats.speed.toLong(),
            )
        }
        return result
    }

    private fun updateWhepIngestsSpeed(stats: IngestStats): IngestStats {
        var result = stats
        for (client in ingests.whep) {
            val clientStats = client.updateStats()
            result = result.copy(
                total = result.total + clientStats.total.toLong(),
                speed = result.speed + clientStats.speed.toLong(),
                numberOfClients = result.numberOfClients + 1,
                anyServerEnabled = true,
            )
        }
        return result
    }

    fun checkPhotoLibraryAuthorization() {
        Unit
    }

    private fun addObserver(name: String, selector: String) {
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
        if (state != statusOther.thermalState.value) {
            statusOther.thermalState.value = state
        }
        streamingHistoryStream?.updateHighestThermalState(
            thermalState = ThermalState.from(state.ordinal),
        )
        if (isWatchLocal()) {
            sendThermalStateToWatch(thermalState = ThermalState.from(state.ordinal))
        }
        Log.i("Model", "Thermal state: $state")
        if (statusOther.thermalState.value == MoblinkThermalState.red) {
            makeFlameRedToast()
        }
    }

    private fun getThermalState(): MoblinkThermalState = MoblinkThermalState.white

    fun detachCamera() {
        val params = VideoUnitAttachParams(
            devices = CaptureDevices(hasSceneDevice = false, devices = mutableListOf()),
            builtinDelay = 0.0,
            cameraPreviewLayers = cameraPreviewView.previewLayers.toMap(),
            attachCameraPreview = false,
            showCameraPreview = false,
            externalDisplayPreview = false,
            bufferedVideo = null,
            preferredVideoStabilizationMode = SettingsVideoStabilizationMode.off.ordinal,
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
        if (useLandscapeStreamAndPortraitUi(null, isLandscapeStreamAndPortraitUi())) {
            cameraPreviewView.setVideoOrientation(com.moblin.android.platform.avfoundation.AVCaptureVideoOrientation.portrait)
        } else if (stream.value.portrait) {
            cameraPreviewView.setVideoOrientation(videoOrientation = com.moblin.android.platform.avfoundation.AVCaptureVideoOrientation.portrait)
        } else {
            when (com.moblin.android.platform.uikit.UIDevice.current.orientation) {
                com.moblin.android.platform.uikit.UIDeviceOrientation.landscapeLeft ->
                    cameraPreviewView.setVideoOrientation(com.moblin.android.platform.avfoundation.AVCaptureVideoOrientation.landscapeRight)
                com.moblin.android.platform.uikit.UIDeviceOrientation.landscapeRight ->
                    cameraPreviewView.setVideoOrientation(com.moblin.android.platform.avfoundation.AVCaptureVideoOrientation.landscapeLeft)
                else -> cameraPreviewView.setVideoOrientation(com.moblin.android.platform.avfoundation.AVCaptureVideoOrientation.landscapeRight)
            }
        }
    }

    fun getVideoMirroredOnStream(device: CaptureDevice): Boolean {
        if ((device.device as? com.moblin.android.platform.avfoundation.AVCaptureDevice)?.position == com.moblin.android.platform.avfoundation.AVCaptureDevice.Position.front) {
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
    }

    fun attachBackDualLowEnergyCamera(force: Boolean = true) {
    }

    fun attachBackWideDualLowEnergyCamera(force: Boolean = true) {
    }

    fun attachCamera(scene: SettingsScene, position: Int) {
        val cameraDevice = preferredCamera(position = position)
        this.cameraDevice = cameraDevice
        setFocusAfterCameraAttach()
        cameraZoomLevelToXScale = (cameraDevice?.device as? AVCaptureDevice)?.getZoomFactorScale(hasUltraWideBackCamera) ?: 1f
        val range: Pair<Float, Float> = (cameraDevice?.device as? AVCaptureDevice)?.getUIZoomRange(hasUltraWideBackCamera) ?: Pair(1f, 1f)
        cameraZoomXMinimum = range.first
        cameraZoomXMaximum = range.second
        cameraPosition = position
        when (position) {
            CameraSelector.LENS_FACING_BACK -> {
                updateBackZoomSwitchTo()
                zoom.x.value = zoom.backX
            }
            CameraSelector.LENS_FACING_FRONT -> {
                updateFrontZoomSwitchTo()
                zoom.x.value = zoom.frontX
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
            builtinDelay = database.debug.builtinAudioAndVideoDelay.value,
            cameraPreviewLayers = cameraPreviewView.previewLayers.toMap(),
            attachCameraPreview = attachCameraPreview,
            showCameraPreview = showCameraPreview,
            externalDisplayPreview = externalDisplayPreview,
            bufferedVideo = null,
            preferredVideoStabilizationMode = getVideoStabilizationMode(scene = scene).ordinal,
            ignoreFramesAfterAttachSeconds = getIgnoreFramesAfterAttachSeconds(),
            fillFrame = getFillFrame(scene = scene),
            isLandscapeStreamAndPortraitUi = isLandscapeStreamAndPortraitUi(),
            forceSceneTransition = database.forceSceneSwitchTransition,
            macScreenCapture = sceneNeedsMacScreenCapture(scene = scene),
            attachPhotoShoot = photoShootEnabled.value || database.alwaysAttachPhotoShoot,
        )
        media.attachCamera(
            params = params,
            onSuccess = {
                streamPreviewView.isMirrored = isMirrored
                externalDisplayStreamPreviewView.isMirrored = isMirrored
                val x = setCameraZoomX(x = zoom.x.value)
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
                relaxedBitrate = database.debug.relaxedBitrate.value
                cameraPreviewView.select(id = devices.getSceneDevice()?.id)
                updateCameraPreviewRotation()
                updateVideoPreviews()
            },
        )
        zoom.xPinch = zoom.x.value
        zoom.hasZoom.value = true
        updateFrontZoomPresets()
        updateBackZoomPresets()
    }

    private fun getIgnoreFramesAfterAttachSeconds(): Double =
        database.debug.cameraSwitchRemoveBlackish.value + database.debug.builtinAudioAndVideoDelay.value

    private fun getIgnoreFramesAfterAttachSecondsReplaceCamera(): Double =
        if (database.forceSceneSwitchTransition) {
            database.debug.cameraSwitchRemoveBlackish.value.toDouble()
        } else {
            0.0
        }

    fun attachBufferedCamera(cameraId: UUID, scene: SettingsScene) {
        cameraDevice = null
        cameraPosition = null
        streamPreviewView.isMirrored = false
        externalDisplayStreamPreviewView.isMirrored = false
        zoom.hasZoom.value = false
        cameraPreviewView.setDevices(ids = emptyList<UUID>())
        media.attachBufferedCamera(
            devices = getBuiltinCameraDevices(scene = scene, sceneDevice = null),
            builtinDelay = database.debug.builtinAudioAndVideoDelay.value,
            cameraPreviewLayers = cameraPreviewView.previewLayers.toMap(),
            attachCameraPreview = false,
            showCameraPreview = updateShowCameraPreview(),
            externalDisplayPreview = externalDisplayPreview,
            cameraId = cameraId,
            preferredVideoStabilizationMode = getVideoStabilizationMode(scene = scene).ordinal,
            ignoreFramesAfterAttachSeconds = getIgnoreFramesAfterAttachSecondsReplaceCamera(),
            fillFrame = getFillFrame(scene = scene),
            isLandscapeStreamAndPortraitUi = isLandscapeStreamAndPortraitUi(),
            forceSceneTransition = database.forceSceneSwitchTransition,
            macScreenCapture = sceneNeedsMacScreenCapture(scene = scene),
            attachPhotoShoot = photoShootEnabled.value || database.alwaysAttachPhotoShoot,
        )
        media.usePendingAfterAttachEffects()
        updateVideoPreviews()
        zoomPresetsMayHaveChanged()
    }

    fun attachExternalCamera(scene: SettingsScene) {
        attachCamera(scene = scene, position = com.moblin.android.platform.avfoundation.AVCaptureDevice.Position.unspecified.ordinal)
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
        streamOverlay.isTorchOn.value = on
        updateTorch()
    }

    fun toggleTorch() {
        streamOverlay.isTorchOn.value = !streamOverlay.isTorchOn.value
        updateTorch()
    }

    fun updateTorch() {
        media.setTorchLevel(level = database.torchLevel)
        media.setTorch(on = streamOverlay.isTorchOn.value)
        remoteControlStateChanged(
            state = RemoteControlAssistantStreamerState(torchOn = streamOverlay.isTorchOn.value),
        )
    }

    fun setTorchLevel(level: Float) {
        media.setTorchLevel(level = level)
    }

    fun toggleMute() {
        audio.muted.value = !audio.muted.value
        updateMute()
    }

    fun setMuted(value: Boolean) {
        audio.muted.value = value
        updateMute()
    }

    fun updateMute() {
        media.setMute(on = audio.muted.value)
        if (isWatchLocal()) {
            sendIsMutedToWatch(isMuteOn = audio.muted.value)
        }
        updateTextEffects(now = Instant.now(), timestamp = TimeSource.Monotonic.markNow())
        forceUpdateTextEffects()
        remoteControlStateChanged(state = RemoteControlAssistantStreamerState(muted = audio.muted.value))
    }

    private fun makeFlameRedToast() {
        makeToast(title = flameRedMessage, subTitle = flameRedSubMessage, vibrate = true)
    }

    fun startMotionDetection() {
        Unit
    }

    fun stopMotionDetection() {
        Unit
    }

    fun reloadCameraLevel() {
        if (showingCameraLevel.value) {
            cameraLevel.start(portrait = stream.value.portrait)
        } else {
            cameraLevel.stop()
        }
    }

    fun preferredCamera(position: Int): CaptureDevice? {
        val scene = findEnabledScene(id = sceneSelector.selectedSceneId)
        if (scene != null) {
            val deviceId = when (position) {
                CameraSelector.LENS_FACING_BACK -> scene.videoSource.backCameraId
                CameraSelector.LENS_FACING_FRONT -> scene.videoSource.frontCameraId
                else -> scene.videoSource.externalCameraId
            }
            return CaptureDevice(
                device = AVCaptureDevice.withUniqueID(deviceId) ?: return null,
                id = builtinCameraIds[deviceId] ?: UUID.randomUUID(),
                isVideoMirrored = false,
            )
        }
        return null
    }

    fun isShowingStatusCamera(): Boolean = database.show.cameras && !isChatPhone()

    fun isShowingStatusMic(): Boolean = database.show.microphone && !isChatPhone()

    fun isShowingStatusAudioLevel(): Boolean = database.show.audioLevel && !isChatPhone()

    fun isShowingStatusEvents(): Boolean = database.show.events && isEventsConfigured()

    fun isShowingStatusViewers(): Boolean =
        isLive.value && database.show.viewers && statusTopLeft.streamingPlatformStatuses.value.isNotEmpty()

    private fun statusStreamText(): String {
        val proto = stream.value.protocolString()
        val resolution = currentResolution ?: stream.value.resolutionString()
        val codec = stream.value.codecString()
        val rateControl = stream.value.rateControlString()
        val bitrate = stream.value.bitrateString()
        val audioCodec = stream.value.audioCodecString()
        val audioBitrate = stream.value.audioBitrateString()
        val fps = if (lowLightBoost) {
            "${currentFps ?: stream.value.fps} LLB"
        } else {
            (currentFps ?: stream.value.fps).toString()
        }
        return "${stream.value.name} ($resolution, $fps, $proto, $codec $rateControl $bitrate, " +
            "$audioCodec $audioBitrate)"
    }

    fun updateStatusStreamText() {
        val status = statusStreamText()
        if (status != statusTopLeft.streamText.value) {
            statusTopLeft.streamText.value = status
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
        if (status != statusTopLeft.statusEventsText.value) {
            statusTopLeft.statusEventsText.value = status
        }
    }

    fun statusViewersText(): String =
        if (isViewersConfigured()) {
            statusTopLeft.numberOfViewersCompact.value
        } else {
            localized("Not configured")
        }

    fun isShowingStatusAdsRemainingTimer(): Boolean =
        statusTopRight.adsRemainingTimerStatus.value != noValue

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
        database.show.djiDevices && statusTopRight.djiDevicesStatus.value != noValue

    fun isShowingStatusBitrate(): Boolean = database.show.speed && isLive.value

    fun isShowingStatusStreamUptime(): Boolean = database.show.uptime && isLive.value

    fun isShowingStatusBonding(): Boolean = database.show.bonding && isStatusBondingActive()

    fun isStatusBondingActive(): Boolean = stream.value.isBonding() && isLive.value

    fun isShowingStatusBondingRtts(): Boolean =
        database.show.bondingRtts && isStatusBondingRttsActive()

    fun isStatusBondingRttsActive(): Boolean = stream.value.isBonding() && isLive.value

    fun isShowingStatusReplay(): Boolean = stream.value.replay.enabled && !isChatPhone()

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
        statusTopRight.browserWidgetsStatus.value.isNotEmpty() &&
            statusTopRight.browserWidgetsStatusChanged.value

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
        if (getQuickButton(type = type)?.isOn?.value != false) {
            return
        }
        setQuickButton(type = type, isOn = true)
        effect?.play(alert = com.moblin.android.videoeffects.alerts.AlertsEffectAlert.QuickButton)
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
        Unit
    }

    fun stopLiveActivity() {
        Unit
    }

    fun setupMacStatusItem() {
        Unit
    }

    fun stopMacStatusItem() {
        Unit
    }
}

fun Model.toggleDrawOnStream() {
    showDrawOnStream.value = !showDrawOnStream.value
    drawOnStreamUpdateButtonState()
}

fun Model.drawOnStreamLineComplete() {
    drawOnStreamEffect.updateOverlay(
        videoSize = media.getCanvasSize().toCGSize(),
        size = drawOnStreamSize.toCGSize(),
        lines = drawOnStream.lines.value,
        mirror = streamOverlay.isFrontCameraSelected.value && !database.mirrorFrontCameraOnStream,
    )
    media.registerEffect(drawOnStreamEffect)
    drawOnStreamUpdateButtonState()
}

fun Model.drawOnStreamWipe() {
    drawOnStream.lines.value = emptyList()
    drawOnStreamEffect.updateOverlay(
        videoSize = media.getCanvasSize().toCGSize(),
        size = drawOnStreamSize.toCGSize(),
        lines = drawOnStream.lines.value,
        mirror = streamOverlay.isFrontCameraSelected.value && !database.mirrorFrontCameraOnStream,
    )
    media.unregisterEffect(drawOnStreamEffect)
    drawOnStreamUpdateButtonState()
}

fun Model.drawOnStreamUndo() {
    if (drawOnStream.lines.value.isEmpty()) {
        return
    }
    drawOnStream.lines.value = drawOnStream.lines.value.dropLast(1)
    drawOnStreamEffect.updateOverlay(
        videoSize = media.getCanvasSize().toCGSize(),
        size = drawOnStreamSize.toCGSize(),
        lines = drawOnStream.lines.value,
        mirror = streamOverlay.isFrontCameraSelected.value && !database.mirrorFrontCameraOnStream,
    )
    if (drawOnStream.lines.value.isEmpty()) {
        media.unregisterEffect(drawOnStreamEffect)
    }
    drawOnStreamUpdateButtonState()
}

fun Model.drawOnStreamUpdateButtonState() {
    setQuickButton(
        type = SettingsQuickButtonType.draw,
        isOn = showDrawOnStream.value || drawOnStream.lines.value.isNotEmpty(),
    )
}
