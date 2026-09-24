package com.moblin.android.platform.webkit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import com.moblin.android.platform.coregraphics.CGRect
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

private val transparentVideoPoster: Bitmap by lazy {
    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
}

private class WKScriptBridge(private val owner: WeakReference<WKWebView>) {
    @JavascriptInterface
    fun post(name: String?, json: String?) {
        if (name == null) {
            return
        }
        val body = webKitJsonToValue(json) ?: Unit
        webKitMainHandler.post {
            owner.get()?.receiveScriptMessage(name, body)
        }
    }
}

private class WKWebViewClient(private val owner: WeakReference<WKWebView>) : WebViewClient() {
    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        owner.get()?.pageStarted(view, url)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        owner.get()?.pageFinished(view, url)
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail?): Boolean {
        val webView = owner.get()
        if (webView != null) {
            webView.renderProcessGone(view)
        } else {
            destroyWebView(view)
        }
        return true
    }
}

private class WKWebChromeClient(private val owner: WeakReference<WKWebView>) : WebChromeClient() {
    override fun onProgressChanged(view: WebView, newProgress: Int) {
        owner.get()?.progressChanged(view, newProgress)
    }

    override fun getDefaultVideoPoster(): Bitmap {
        return transparentVideoPoster
    }

    override fun onJsAlert(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
        result.confirm()
        return true
    }

    override fun onJsConfirm(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
        result.cancel()
        return true
    }

    override fun onJsPrompt(
        view: WebView,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult,
    ): Boolean {
        result.cancel()
        return true
    }

    override fun onJsBeforeUnload(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
        result.confirm()
        return true
    }

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message?,
    ): Boolean {
        return false
    }
}

class WKWebView(frame: CGRect, configuration: WKWebViewConfiguration) {
    val configuration: WKWebViewConfiguration = configuration
    private val host = WebViewHost()
    private val weakSelf = WeakReference(this)
    private var lastRequestedUrl: String? = null
    private var scriptsUpdatePosted = false

    var frame: CGRect = frame
        set(value) {
            field = value
            val width = pixels(value.width)
            val height = pixels(value.height)
            val hostRef = host
            runOnWebKitMain {
                hostRef.resize(width, height)
            }
        }

    var isOpaque: Boolean = true
        set(value) {
            field = value
            runOnWebKitMain { applyAppearance() }
        }

    var backgroundColor: Int? = null
        set(value) {
            field = value
            runOnWebKitMain { applyAppearance() }
        }

    val scrollView: UIScrollViewFlags = UIScrollViewFlags {
        runOnWebKitMain { applyAppearance() }
    }

    @Volatile
    var estimatedProgress: Double = 0.0
        private set

    @Volatile
    var url: String? = null
        private set

    @Volatile
    var isLoading: Boolean = false
        private set

    @Volatile
    var title: String? = null
        private set

    init {
        configuration.userContentController.addObserver(this)
        if (!isWebKitMainThread()) {
            WebKitLog.once("offMain", "WKWebView must be created on the main thread; tracking it there")
        }
        val hostRef = host
        runOnWebKitMain {
            WebKitHosts.track(this, hostRef)
        }
    }

    fun load(request: URLRequest) {
        lastRequestedUrl = request.url
        url = request.url
        isLoading = true
        estimatedProgress = 0.0
        val target = request.url
        navigate(startsHost = true) { it.loadUrl(target) }
    }

    fun loadHTMLString(string: String, baseURL: String?) {
        lastRequestedUrl = null
        isLoading = true
        estimatedProgress = 0.0
        navigate(startsHost = true) { it.loadDataWithBaseURL(baseURL, string, "text/html", "utf-8", null) }
    }

    fun reload() {
        navigate(startsHost = false) { it.reload() }
    }

    fun stopLoading() {
        navigate(startsHost = false) { it.stopLoading() }
    }

    fun evaluateJavaScript(javaScriptString: String, completionHandler: ((Any?, Throwable?) -> Unit)? = null) {
        val hostRef = host
        runOnWebKitMain {
            val view = hostRef.webView
            if (view == null) {
                completionHandler?.invoke(null, IllegalStateException("The web view is gone"))
                return@runOnWebKitMain
            }
            try {
                if (completionHandler == null) {
                    view.evaluateJavascript(javaScriptString, null)
                } else {
                    view.evaluateJavascript(javaScriptString) { result ->
                        completionHandler(webKitJsonToValue(result), null)
                    }
                }
            } catch (error: Throwable) {
                completionHandler?.invoke(null, error)
            }
        }
    }

    fun takeSnapshot(with: WKSnapshotConfiguration?, completionHandler: (Bitmap?, Throwable?) -> Unit) {
        val hostRef = host
        runOnWebKitMain {
            hostRef.snapshot(with, completionHandler)
        }
    }

    fun borrowView(): WebView {
        if (!isWebKitMainThread()) {
            WebKitLog.once("borrowOffMain", "WKWebView.borrowView must be called on the main thread")
        }
        setUp()
        val borrowed = host.borrow()
        if (borrowed != null) {
            return borrowed
        }
        val view = createView(host.viewContext)
        host.attach(view)
        installScripts(view)
        lastRequestedUrl?.let { target -> navigate(startsHost = true) { it.loadUrl(target) } }
        return host.borrow() ?: view
    }

    fun returnView() {
        val hostRef = host
        runOnWebKitMain {
            hostRef.giveBack()
        }
    }

    fun setBorrowedViewVisible(visible: Boolean) {
        val hostRef = host
        runOnWebKitMain {
            hostRef.setBorrowVisible(visible)
        }
    }

    internal fun userContentChanged() {
        runOnWebKitMain {
            if (scriptsUpdatePosted) {
                return@runOnWebKitMain
            }
            scriptsUpdatePosted = true
            webKitMainHandler.post {
                scriptsUpdatePosted = false
                host.webView?.let { installScripts(it) }
            }
        }
    }

    internal fun receiveScriptMessage(name: String, body: Any) {
        configuration.userContentController.dispatch(name, body)
    }

    internal fun pageStarted(view: WebView, url: String?) {
        if (view !== host.webView) {
            return
        }
        this.url = url ?: this.url
        isLoading = true
        if (WebKitFeatures.documentStartScript) {
            return
        }
        WebKitLog.once(
            "noDocumentStart",
            "This WebView cannot inject document start scripts; running them when the page starts",
        )
        val controller = configuration.userContentController
        view.evaluateJavascript(WebKitScripts.bridgeInstaller(controller.handlerNames()), null)
        for (script in controller.userScripts) {
            if (script.injectionTime == WKUserScriptInjectionTime.atDocumentStart) {
                view.evaluateJavascript(WebKitScripts.documentStart(script), null)
            }
        }
    }

    internal fun pageFinished(view: WebView, url: String?) {
        if (view !== host.webView) {
            return
        }
        this.url = url ?: this.url
        title = view.title
        isLoading = false
        estimatedProgress = 1.0
        if (WebKitFeatures.documentStartScript) {
            return
        }
        for (script in configuration.userContentController.userScripts) {
            if (script.injectionTime == WKUserScriptInjectionTime.atDocumentEnd) {
                view.evaluateJavascript(WebKitScripts.documentStart(script), null)
            }
        }
    }

    internal fun progressChanged(view: WebView, newProgress: Int) {
        if (view !== host.webView) {
            return
        }
        estimatedProgress = newProgress.coerceIn(0, 100) / 100.0
    }

    internal fun renderProcessGone(view: WebView) {
        WebKitLog.once("renderProcessGone", "WebView render process gone; creating a new WebView")
        if (view !== host.webView) {
            destroyWebView(view)
            return
        }
        host.detachWebView(view)
        destroyWebView(view)
        webKitMainHandler.post {
            if (host.webView != null) {
                return@post
            }
            try {
                val newView = createView(host.viewContext)
                host.attach(newView)
                installScripts(newView)
                lastRequestedUrl?.let { target -> navigate(startsHost = true) { it.loadUrl(target) } }
            } catch (error: Throwable) {
                WebKitLog.error("recreating the WebView failed: $error")
            }
        }
    }

    private fun setUp() {
        if (host.isStarted) {
            return
        }
        host.start(pixels(frame.width), pixels(frame.height))
        try {
            val view = createView(host.viewContext)
            host.attach(view)
            installScripts(view)
        } catch (error: Throwable) {
            WebKitLog.error("creating the WebView failed: $error")
        }
    }

    private fun navigate(startsHost: Boolean, action: (WebView) -> Unit) {
        val hostRef = host
        runOnWebKitMain {
            if (startsHost) {
                setUp()
            } else if (!hostRef.isStarted) {
                return@runOnWebKitMain
            }
            WebKitProxy.whenApplied {
                hostRef.webView?.let(action)
            }
        }
    }

    private fun createView(context: Context): HostedWebView {
        val view = HostedWebView(context)
        val settings = view.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = configuration.mediaTypesRequiringUserActionForPlayback.isNotEmpty()
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.textZoom = 100
        settings.standardFontFamily = "serif"
        settings.minimumFontSize = 1
        settings.minimumLogicalFontSize = 9
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.allowFileAccess = false
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = false
        view.overScrollMode = View.OVER_SCROLL_NEVER
        view.webViewClient = WKWebViewClient(weakSelf)
        view.webChromeClient = WKWebChromeClient(weakSelf)
        view.addJavascriptInterface(WKScriptBridge(weakSelf), webKitBridgeName)
        applyAppearance(view)
        return view
    }

    private fun installScripts(view: HostedWebView) {
        if (!WebKitFeatures.documentStartScript) {
            return
        }
        for (handler in view.scriptHandlers) {
            runCatching { handler.remove() }
        }
        view.scriptHandlers.clear()
        val controller = configuration.userContentController
        val sources = mutableListOf(WebKitScripts.bridgeInstaller(controller.handlerNames()))
        for (script in controller.userScripts) {
            sources.add(
                when (script.injectionTime) {
                    WKUserScriptInjectionTime.atDocumentStart -> WebKitScripts.documentStart(script)
                    WKUserScriptInjectionTime.atDocumentEnd -> WebKitScripts.documentEndAtDocumentStart(script)
                }
            )
        }
        try {
            for (source in sources) {
                view.scriptHandlers.add(WebViewCompat.addDocumentStartJavaScript(view, source, setOf("*")))
            }
        } catch (error: Throwable) {
            WebKitLog.once("documentStart:$error", "adding document start scripts failed: $error")
        }
    }

    private fun applyAppearance() {
        host.webView?.let { applyAppearance(it) }
    }

    private fun applyAppearance(view: WebView) {
        val fallback = if (isOpaque) Color.WHITE else Color.TRANSPARENT
        view.setBackgroundColor(backgroundColor ?: scrollView.backgroundColor ?: fallback)
        view.isVerticalScrollBarEnabled = scrollView.showsVerticalScrollIndicator
        view.isHorizontalScrollBarEnabled = scrollView.showsHorizontalScrollIndicator
    }

    private fun pixels(points: Double): Int {
        if (points.isNaN() || points.isInfinite()) {
            return 1
        }
        return points.roundToInt().coerceAtLeast(1)
    }
}
