package com.moblin.android.videoeffects.browser

import android.content.Context
import android.graphics.Color
import android.media.Image
import android.webkit.WebView
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.ChatPost
import com.moblin.android.various.MainTimer
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetBrowser
import com.moblin.android.various.settings.SettingsWidgetBrowserMode
import com.moblin.android.various.settings.SettingsWidgetCrop
import com.moblin.android.various.settings.SettingsWidgetLayout
import com.moblin.android.various.utils.loadStringResource
import com.moblin.android.various.utils.screenScale
import com.moblin.android.videoeffects.EffectImageCgImage
import java.net.InetSocketAddress
import java.net.URI
import java.util.Base64
import kotlin.math.max
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WidgetCrop(
    val crop: SettingsWidgetCrop,
    val sceneWidget: SettingsSceneWidget,
)

private enum class UserScriptInjectionTime {
    DOCUMENT_START,
    DOCUMENT_END,
}

private fun createStyleSheetSource(styleSheet: String): String? {
    if (styleSheet.isEmpty()) {
        return null
    }
    val styleSheetData = styleSheet.toByteArray(Charsets.UTF_8)
    val styleSheetBase64 = Base64.getEncoder().encodeToString(styleSheetData)
    return """
    var style = document.createElement('style');
    style.type = 'text/css';
    style.innerHTML = window.atob('$styleSheetBase64');
    document.head.appendChild(style);
    """.trimIndent()
}

private fun videoScript(): String {
    return loadStringResource("video", "js")
}

private fun addScript(webView: WebView, script: String, injectionTime: UserScriptInjectionTime) {
    TODO("no Android counterpart for WKUserScript")
}

class BrowserEffect(
    url: URI,
    styleSheet: String,
    widget: SettingsWidgetBrowser,
    moblinAccess: Boolean,
    proxyServer: InetSocketAddress?,
    context: Context,
) : VideoEffect(), BrowserEffectServerDelegate {
    val webView: WebView
    @Volatile private var snapshot: EffectImageCgImage? = null
    val width: Double
    val height: Double
    private val url: URI
    var isLoaded: Boolean = false
        private set
    val layout = MutableStateFlow<SettingsWidgetLayout?>(null)
    private val mode: SettingsWidgetBrowserMode
    private var baseFps: Double
    private var fps: Double
    private val snapshotTimer = MainTimer()
    var startLoadingTime: Long = System.nanoTime()
    private val scale: Double
    private var sceneWidget: SettingsSceneWidget? = null
    private var crops: List<WidgetCrop> = emptyList()
    private val server: BrowserEffectServer
    private val speechToText: Boolean
    private var stopped = false
    private var suspended = false
    private val snapshotConfiguration: Double

    init {
        scale = screenScale()
        this.url = url
        baseFps = widget.baseFps.toDouble()
        fps = baseFps
        isLoaded = false
        mode = if (widget.localOnly) {
            SettingsWidgetBrowserMode.AUDIO_ONLY
        } else {
            widget.mode
        }
        speechToText = widget.speechToText
        width = widget.width.toDouble()
        height = widget.height.toDouble()
        snapshotConfiguration = width / scale
        webView = WebView(context)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.settings.mediaPlaybackRequiresUserGesture = false
        val styleSheetSource = createStyleSheetSource(styleSheet)
        if (styleSheetSource != null) {
            addScript(webView, styleSheetSource, UserScriptInjectionTime.DOCUMENT_END)
        }
        addScript(webView, videoScript(), UserScriptInjectionTime.DOCUMENT_START)
        server = BrowserEffectServer(webView, moblinAccess)
        server.delegate = this
    }

    fun close() {
        TODO("no Android counterpart for WKUserContentController.removeAllScriptMessageHandlers")
    }

    override fun isEnabled(): Boolean {
        return mode != SettingsWidgetBrowserMode.AUDIO_ONLY && snapshot != null
    }

    fun sendChatMessage(post: ChatPost) {
        server.sendChatMessage(post)
    }

    fun sendSpeechToText(position: Int, text: String) {
        if (!speechToText) {
            return
        }
        server.sendSpeechToText(position, text)
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
        get() = TODO("no Android counterpart for WKWebView.estimatedProgress")

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
            setSceneWidgetEnabled(sceneWidget, crops)
        } else if (isLoaded) {
            setSceneWidgetLoaded()
        }
    }

    fun setProxyServer(endpoint: InetSocketAddress?) {
        TODO("no Android counterpart for WKWebViewConfiguration.setHttpProxy")
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        TODO("OpenGL ES port")
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        TODO("OpenGL ES port")
    }

    private fun setSceneWidgetEnabled(sceneWidget: SettingsSceneWidget?, crops: List<WidgetCrop>) {
        processorPipelineQueue.launch {
            this@BrowserEffect.sceneWidget = sceneWidget
            this@BrowserEffect.crops = crops
        }
        if (!isLoaded) {
            startLoadingTime = System.nanoTime()
            webView.loadUrl(url.toString())
            server.enable()
            isLoaded = true
        }
        stopped = false
        startTakeSnapshots()
    }

    private fun setSceneWidgetLoaded() {
        processorPipelineQueue.launch {
            this@BrowserEffect.snapshot = null
        }
        webView.loadDataWithBaseURL(null, "<html></html>", "text/html", "utf-8", null)
        server.disable()
        isLoaded = false
    }

    private fun startTakeSnapshots() {
        if (stopped || mode != SettingsWidgetBrowserMode.PERIODIC_AUDIO_AND_VIDEO) {
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
            this@BrowserEffect.snapshot = null
        }
    }

    private fun resumeTakeSnapshots() {
        suspended = false
        takeSnapshots(0.0)
    }

    private fun takeSnapshots(takeSnapshotTime: Double) {
        snapshotTimer.startSingleShot(max(1.0 / fps - takeSnapshotTime, 0.001)) {
            val takeSnapshotBeginTime = System.nanoTime()
            val image: EffectImageCgImage? = TODO("no Android counterpart for WKWebView.takeSnapshot")
            if (!stopped && !suspended) {
                takeSnapshots((System.nanoTime() - takeSnapshotBeginTime) / 1_000_000_000.0)
                if (image != null) {
                    processorPipelineQueue.launch {
                        this@BrowserEffect.snapshot = image
                    }
                }
            }
        }
    }

    override fun browserEffectServerVideoPlaying() {
        fps = 30.0
        if (mode != SettingsWidgetBrowserMode.AUDIO_ONLY) {
            resumeTakeSnapshots()
        }
    }

    override fun browserEffectServerVideoEnded() {
        fps = baseFps
        if (mode == SettingsWidgetBrowserMode.PERIODIC_AUDIO_AND_VIDEO) {
            return
        }
        suspendTakeSnapshots()
    }
}
