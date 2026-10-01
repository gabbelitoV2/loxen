package com.moblin.android.videoeffects.browser

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.core.ContinuousClock
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.network.NWEndpoint
import com.moblin.android.platform.webkit.URLRequest
import com.moblin.android.platform.webkit.WKSnapshotConfiguration
import com.moblin.android.platform.webkit.WKUserContentController
import com.moblin.android.platform.webkit.WKUserScript
import com.moblin.android.platform.webkit.WKUserScriptInjectionTime
import com.moblin.android.platform.webkit.WKWebView
import com.moblin.android.platform.webkit.WKWebViewConfiguration
import com.moblin.android.various.ChatPost
import com.moblin.android.various.MainTimer
import com.moblin.android.various.network.setHttpProxy
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetBrowser
import com.moblin.android.various.settings.SettingsWidgetBrowserMode
import com.moblin.android.various.settings.SettingsWidgetCrop
import com.moblin.android.various.settings.SettingsWidgetLayout
import com.moblin.android.various.utils.loadStringResource
import com.moblin.android.various.utils.screenScale
import com.moblin.android.videoeffects.EffectImageCgImage
import com.moblin.android.videoeffects.MetalPetalWidgetShape
import com.moblin.android.videoeffects.move
import com.moblin.android.videoeffects.resizeMirror
import com.moblin.android.videoeffects.resizeMirrorMoveComposited
import com.moblin.android.videoeffects.toEffectImage
import java.util.Base64
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import com.moblin.android.platform.uikit.cgImage
import com.moblin.android.videoeffects.translated
import com.moblin.android.common.various.seconds

data class WidgetCrop(val crop: SettingsWidgetCrop, val sceneWidget: SettingsSceneWidget)

private fun createStyleSheetSource(styleSheet: String): String? {
    if (styleSheet.isEmpty()) {
        return null
    }
    val styleSheetData = styleSheet.toByteArray(Charsets.UTF_8)
    return """
        (() => {
            const style = document.createElement('style');
            style.type = 'text/css';
            style.innerHTML = window.atob('${Base64.getEncoder().encodeToString(styleSheetData)}');
            document.head.appendChild(style);
        })();
        """.trimIndent()
}

private fun viewportScript(width: Int): String {
    return """
        (() => {
            const meta = document.createElement('meta');
            meta.name = 'viewport';
            meta.content = 'width=$width, initial-scale=1';
            (document.head ?? document.documentElement).appendChild(meta);
        })();
        """.trimIndent()
}

private fun videoScript(): String = loadStringResource(name = "video", ext = "js")

private fun addScript(
    configuration: WKWebViewConfiguration,
    script: String,
    injectionTime: WKUserScriptInjectionTime,
) {
    configuration.userContentController.addUserScript(
        WKUserScript(source = script, injectionTime = injectionTime, forMainFrameOnly = false)
    )
}

class BrowserEffect(
    url: java.net.URI,
    styleSheet: String,
    widget: SettingsWidgetBrowser,
    moblinAccess: Boolean,
    proxyServer: java.net.InetSocketAddress?,
) : VideoEffect(), BrowserEffectServerDelegate {
    val webView: WKWebView
    private var snapshot: EffectImageCgImage? = null
    private val snapshotQueue = ArrayDeque<EffectImageCgImage>()
    val width: Double
    val height: Double
    private val url: java.net.URI
    var isLoaded: Boolean = false
        private set
    val layout = MutableStateFlow<SettingsWidgetLayout?>(null)
    private val mode: SettingsWidgetBrowserMode
    private var baseFps: Double
    private var fps: Double
    private val snapshotTimer = MainTimer()
    private var snapshotInProgress = false
    private var nextSnapshotTime: ContinuousClock.Instant = ContinuousClock.now
    var startLoadingTime: ContinuousClock.Instant = ContinuousClock.now
    private val scale: Double
    private var sceneWidget: SettingsSceneWidget? = null
    private var crops: List<WidgetCrop> = emptyList()
    private val server: BrowserEffectServer
    private val speechToText: Boolean
    private var stopped = false
    private var suspended = false
    private val snapshotConfiguration: WKSnapshotConfiguration
    private val userContentController: WKUserContentController

    init {
        scale = screenScale().toDouble()
        this.url = url
        baseFps = widget.baseFps.toDouble()
        fps = baseFps
        isLoaded = false
        mode = if (widget.localOnly) {
            SettingsWidgetBrowserMode.audioOnly
        } else {
            widget.mode
        }
        suspended = mode != SettingsWidgetBrowserMode.periodicAudioAndVideo
        speechToText = widget.speechToText
        width = widget.width.toDouble()
        height = widget.height.toDouble()
        snapshotConfiguration = WKSnapshotConfiguration()
        snapshotConfiguration.snapshotWidth = width / scale
        val configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.allowsPictureInPictureMediaPlayback = false
        configuration.mediaTypesRequiringUserActionForPlayback = emptySet()
        val source = createStyleSheetSource(styleSheet)
        if (source != null) {
            addScript(configuration, source, WKUserScriptInjectionTime.atDocumentEnd)
        }
        configuration.userContentController.addUserScript(
            WKUserScript(
                source = viewportScript(widget.width),
                injectionTime = WKUserScriptInjectionTime.atDocumentStart,
                forMainFrameOnly = true,
            )
        )
        addScript(configuration, videoScript(), WKUserScriptInjectionTime.atDocumentStart)
        configuration.setHttpProxy(endpoint = proxyServer?.let {
            NWEndpoint.hostPort(NWEndpoint.Host(it.hostString), NWEndpoint.Port(it.port))
        })
        userContentController = configuration.userContentController
        server = BrowserEffectServer(configuration = configuration, moblinAccess = moblinAccess)
        webView = WKWebView(
            frame = CGRect(x = 0.0, y = 0.0, width = width, height = height),
            configuration = configuration,
        )
        webView.isOpaque = false
        webView.backgroundColor = android.graphics.Color.TRANSPARENT
        webView.scrollView.backgroundColor = android.graphics.Color.TRANSPARENT
        Unit
        webView.scrollView.showsVerticalScrollIndicator = false
        webView.scrollView.showsHorizontalScrollIndicator = false
        server.webView = webView
        server.delegate = this
    }

    override fun isEnabled(): Boolean {
        return mode != SettingsWidgetBrowserMode.audioOnly && (snapshot != null || snapshotQueue.isNotEmpty())
    }

    fun sendChatMessage(post: ChatPost) {
        server.sendChatMessage(post = post)
    }

    fun sendSpeechToText(position: Int, text: String) {
        if (!speechToText) {
            return
        }
        server.sendSpeechToText(position = position, text = text)
    }

    fun sendSpeechToTextClear() {
        if (!speechToText) {
            return
        }
        server.sendSpeechToTextClear()
    }

    val host: String
        get() = url.host ?: "?"

    val progress: Int
        get() = (100 * webView.estimatedProgress).toInt()

    fun stop() {
        stopTakeSnapshots()
    }

    fun reload() {
        webView.reload()
    }

    fun setSceneWidget(sceneWidget: SettingsSceneWidget?, crops: List<WidgetCrop>) {
        layout.value = sceneWidget?.layout
        stopTakeSnapshots()
        if (sceneWidget != null || crops.isNotEmpty()) {
            setSceneWidgetEnabled(sceneWidget = sceneWidget, crops = crops)
        } else if (isLoaded) {
            setSceneWidgetLoaded()
        }
    }

    fun setProxyServer(endpoint: java.net.InetSocketAddress?) {
        webView.configuration.setHttpProxy(endpoint = endpoint?.let {
            NWEndpoint.hostPort(NWEndpoint.Host(it.hostString), NWEndpoint.Port(it.port))
        })
        reload()
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val snapshot = nextSnapshot()?.getCiImage() ?: return image
        var image = image
        val sceneWidget = this.sceneWidget
        if (sceneWidget != null) {
            image = applyEffectsResizeMirrorMove(snapshot, sceneWidget, false, image.extent, info)
                .composited(over = image)
        }
        for (crop in crops) {
            val y = snapshot.extent.height.toInt() - crop.crop.y - crop.crop.height
            image = snapshot
                .cropped(
                    to = CGRect(
                        x = crop.crop.x,
                        y = y,
                        width = crop.crop.width,
                        height = crop.crop.height,
                    )
                )
                .translated(x = -crop.crop.x.toDouble(), y = y.toDouble())
                .resizeMirror(crop.sceneWidget.layout, image.extent.size, false)
                .move(crop.sceneWidget.layout, image.extent.size)
                .cropped(to = image.extent)
                .composited(over = image)
        }
        return image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val snapshot = nextSnapshot()?.getMetalPetalImage() ?: return image
        var image = image
        val sceneWidget = this.sceneWidget
        if (sceneWidget != null) {
            image = applyEffectsResizeMirrorMoveMetalPetal(
                snapshot,
                sceneWidget,
                false,
                image,
                info,
            )
        }
        for (crop in crops) {
            val contentRegion = CGRect(
                x = crop.crop.x,
                y = crop.crop.y,
                width = crop.crop.width,
                height = crop.crop.height,
            )
            image = snapshot.resizeMirrorMoveComposited(
                crop.sceneWidget.layout,
                false,
                image,
                MetalPetalWidgetShape(contentRegion = contentRegion),
            )
        }
        return image
    }

    private fun nextSnapshot(): EffectImageCgImage? {
        val next = snapshotQueue.removeFirstOrNull()
        if (next != null) {
            snapshot = next
        }
        return snapshot
    }

    private fun clearSnapshots() {
        snapshot = null
        snapshotQueue.clear()
    }

    private fun setSceneWidgetEnabled(sceneWidget: SettingsSceneWidget?, crops: List<WidgetCrop>) {
        processorPipelineQueue.launch {
            this@BrowserEffect.sceneWidget = sceneWidget
            this@BrowserEffect.crops = crops
        }
        if (!isLoaded) {
            startLoadingTime = ContinuousClock.now
            webView.load(URLRequest(url = url))
            server.enable()
            isLoaded = true
        }
        stopped = false
        startTakeSnapshots()
    }

    private fun setSceneWidgetLoaded() {
        processorPipelineQueue.launch {
            this@BrowserEffect.clearSnapshots()
        }
        webView.loadHTMLString("<html></html>", baseURL = null)
        server.disable()
        isLoaded = false
    }

    private fun startTakeSnapshots() {
        if (suspended) {
            return
        }
        resumeTakeSnapshots()
    }

    private fun stopTakeSnapshots() {
        stopped = true
        snapshotTimer.stop()
    }

    private fun suspendTakeSnapshots() {
        suspended = true
        snapshotTimer.stop()
        processorPipelineQueue.launch {
            this@BrowserEffect.clearSnapshots()
        }
    }

    private fun resumeTakeSnapshots() {
        suspended = false
        if (stopped) {
            return
        }
        nextSnapshotTime = ContinuousClock.now
        scheduleSnapshot()
    }

    private fun scheduleSnapshot() {
        if (snapshotInProgress) {
            return
        }
        val now = ContinuousClock.now
        val interval = 1 / fps
        nextSnapshotTime = maxOf(nextSnapshotTime.advanced(bySeconds = interval), now.advanced(bySeconds = -interval))
        val timeout = maxOf(now.duration(to = nextSnapshotTime).toDouble(DurationUnit.SECONDS), 0.0)
        snapshotTimer.startSingleShot(timeout = timeout) {
            takeSnapshot()
        }
    }

    private fun takeSnapshot() {
        snapshotInProgress = true
        webView.takeSnapshot(with = snapshotConfiguration) { image, _ ->
            snapshotInProgress = false
            if (stopped || suspended) {
                return@takeSnapshot
            }
            scheduleSnapshot()
            val snapshot = image?.cgImage?.toEffectImage() ?: return@takeSnapshot
            processorPipelineQueue.launch {
                this@BrowserEffect.snapshotQueue.addLast(snapshot)
                if (this@BrowserEffect.snapshotQueue.size > 2) {
                    this@BrowserEffect.snapshotQueue.removeFirst()
                }
            }
        }
    }

    override fun browserEffectServerVideoPlaying() {
        fps = 30.0
        if (mode != SettingsWidgetBrowserMode.audioOnly) {
            resumeTakeSnapshots()
        }
    }

    override fun browserEffectServerVideoEnded() {
        fps = baseFps
        if (mode == SettingsWidgetBrowserMode.periodicAudioAndVideo) {
            resumeTakeSnapshots()
        } else {
            suspendTakeSnapshots()
        }
    }
}
