package com.moblin.android.platform.webkit

import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.moblin.android.AppDelegate
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.offscreen.OffscreenSweep
import java.lang.ref.WeakReference
import org.json.JSONArray
import org.json.JSONObject

private const val bridgeName = "moblinBridge"

private class WebViewHolder {
    var view: WebView? = null
}

private fun destroyWebView(view: WebView) {
    runOnWebKitMain {
        try {
            (view.parent as? ViewGroup)?.removeView(view)
            view.stopLoading()
            view.removeJavascriptInterface(bridgeName)
            view.destroy()
        } catch (error: Throwable) {
            WebKitLog.once("destroy:$error", "WebView destroy failed: $error")
        }
    }
}

private fun makeHandlerInstaller(names: List<String>): String {
    val quotedBridgeName = JSONObject.quote(bridgeName)
    return """
        (function() {
          var names = ${JSONArray(names)};
          window.webkit = window.webkit || {};
          window.webkit.messageHandlers = window.webkit.messageHandlers || {};
          names.forEach(function(name) {
            window.webkit.messageHandlers[name] = {
              postMessage: function(message) {
                window[$quotedBridgeName].post(name, JSON.stringify(message === undefined ? null : message));
              }
            };
          });
        })();
    """.trimIndent()
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
}

class WKWebView(frame: CGRect, configuration: WKWebViewConfiguration) {
    val configuration: WKWebViewConfiguration = configuration
    private val holder = WebViewHolder()
    private var lastRequestedUrl: String? = null
    private var borrowed = false

    var frame: CGRect = frame
        set(value) {
            field = value
            runOnWebKitMain {
                holder.view?.let { layoutView(it) }
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

    var estimatedProgress: Double = 0.0
        private set

    var url: String? = null
        private set

    var isLoading: Boolean = false
        private set

    var title: String? = null
        private set

    init {
        if (!isWebKitMainThread()) {
            WebKitLog.once("offMain", "WKWebView must be created on the main thread")
        }
        holder.view = createView()
        val hostedViews = holder
        OffscreenSweep.track(this) {
            hostedViews.view?.let { destroyWebView(it) }
            hostedViews.view = null
        }
    }

    fun load(request: URLRequest) {
        lastRequestedUrl = request.url
        url = request.url
        isLoading = true
        estimatedProgress = 0.0
        runOnWebKitMain {
            holder.view?.loadUrl(request.url)
        }
    }

    fun loadHTMLString(string: String, baseURL: String?) {
        lastRequestedUrl = null
        isLoading = true
        estimatedProgress = 0.0
        runOnWebKitMain {
            holder.view?.loadDataWithBaseURL(baseURL, string, "text/html", "utf-8", null)
        }
    }

    fun reload() {
        runOnWebKitMain {
            holder.view?.reload()
        }
    }

    fun stopLoading() {
        runOnWebKitMain {
            holder.view?.stopLoading()
        }
    }

    fun evaluateJavaScript(javaScriptString: String, completionHandler: ((Any?, Throwable?) -> Unit)? = null) {
        runOnWebKitMain {
            val view = holder.view
            if (view == null) {
                completionHandler?.invoke(null, IllegalStateException("The web view is gone"))
                return@runOnWebKitMain
            }
            if (completionHandler == null) {
                view.evaluateJavascript(javaScriptString, null)
            } else {
                view.evaluateJavascript(javaScriptString) { result ->
                    completionHandler(webKitJsonToValue(result), null)
                }
            }
        }
    }

    fun takeSnapshot(with: WKSnapshotConfiguration?, completionHandler: (Bitmap?, Throwable?) -> Unit) {
        WebKitLog.notImplemented("WKWebView.takeSnapshot")
        webKitMainHandler.post {
            completionHandler(null, null)
        }
    }

    fun borrowView(): WebView {
        borrowed = true
        return holder.view ?: createView().also { holder.view = it }
    }

    fun returnView() {
        borrowed = false
    }

    internal fun receiveScriptMessage(name: String, body: Any) {
        configuration.userContentController.dispatch(name, body)
    }

    internal fun pageStarted(view: WebView, url: String?) {
        if (view !== holder.view) {
            return
        }
        this.url = url ?: this.url
        isLoading = true
        val controller = configuration.userContentController
        view.evaluateJavascript(makeHandlerInstaller(controller.handlerNames()), null)
        val scripts = controller.userScripts.filter { it.injectionTime == WKUserScriptInjectionTime.atDocumentStart }
        if (scripts.isNotEmpty()) {
            WebKitLog.notImplemented("WKUserScriptInjectionTime.atDocumentStart (runs when the page starts)")
        }
        for (script in scripts) {
            view.evaluateJavascript(script.source, null)
        }
    }

    internal fun pageFinished(view: WebView, url: String?) {
        if (view !== holder.view) {
            return
        }
        this.url = url ?: this.url
        title = view.title
        isLoading = false
        estimatedProgress = 1.0
        val scripts = configuration.userContentController.userScripts
            .filter { it.injectionTime == WKUserScriptInjectionTime.atDocumentEnd }
        for (script in scripts) {
            view.evaluateJavascript(script.source, null)
        }
    }

    internal fun progressChanged(view: WebView, newProgress: Int) {
        if (view !== holder.view) {
            return
        }
        estimatedProgress = newProgress.coerceIn(0, 100) / 100.0
    }

    internal fun renderProcessGone(view: WebView) {
        WebKitLog.once("renderProcessGone", "WebView render process gone; creating a new WebView")
        if (view !== holder.view) {
            destroyWebView(view)
            return
        }
        holder.view = null
        destroyWebView(view)
        webKitMainHandler.post {
            if (holder.view != null) {
                return@post
            }
            val newView = createView()
            holder.view = newView
            lastRequestedUrl?.let { newView.loadUrl(it) }
        }
    }

    private fun createView(): WebView {
        val weakSelf = WeakReference(this)
        val view = WebView(AppDelegate.context)
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.settings.mediaPlaybackRequiresUserGesture =
            configuration.mediaTypesRequiringUserActionForPlayback.isNotEmpty()
        view.webViewClient = WKWebViewClient(weakSelf)
        view.webChromeClient = WKWebChromeClient(weakSelf)
        view.addJavascriptInterface(WKScriptBridge(weakSelf), bridgeName)
        applyAppearance(view)
        layoutView(view)
        return view
    }

    private fun applyAppearance() {
        holder.view?.let { applyAppearance(it) }
    }

    private fun applyAppearance(view: WebView) {
        view.setBackgroundColor(backgroundColor ?: Color.TRANSPARENT)
        view.isVerticalScrollBarEnabled = scrollView.showsVerticalScrollIndicator
        view.isHorizontalScrollBarEnabled = scrollView.showsHorizontalScrollIndicator
    }

    private fun layoutView(view: WebView) {
        if (borrowed || view.parent != null) {
            return
        }
        val width = frame.width.toInt().coerceAtLeast(1)
        val height = frame.height.toInt().coerceAtLeast(1)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, width, height)
    }
}
