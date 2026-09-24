package com.moblin.android.platform.webkit

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.network.NWEndpoint
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

internal const val webKitTag = "MoblinWeb"

internal object WebKitLog {
    private val loggedMessages = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun once(key: String, message: String) {
        if (loggedMessages.size > 1000) {
            loggedMessages.clear()
        }
        if (!loggedMessages.add(key)) {
            return
        }
        info(message)
    }

    fun info(message: String) {
        try {
            Log.i(webKitTag, message)
        } catch (_: Throwable) {
        }
    }

    fun error(message: String) {
        try {
            Log.e(webKitTag, message)
        } catch (_: Throwable) {
        }
    }
}

internal val webKitMainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

internal val webKitMainExecutor = Executor { runnable -> webKitMainHandler.post(runnable) }

internal fun isWebKitMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()

internal fun runOnWebKitMain(block: () -> Unit) {
    if (isWebKitMainThread()) {
        block()
    } else {
        webKitMainHandler.post(block)
    }
}

internal object WebKitFeatures {
    val documentStartScript: Boolean by lazy {
        isSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
    }

    val proxyOverride: Boolean by lazy {
        isSupported(WebViewFeature.PROXY_OVERRIDE)
    }

    private fun isSupported(feature: String): Boolean {
        return try {
            WebViewFeature.isFeatureSupported(feature)
        } catch (error: Throwable) {
            WebKitLog.once("feature:$feature", "WebView feature check $feature failed: $error")
            false
        }
    }
}

internal fun webKitJsonToValue(json: String?): Any? {
    if (json == null) {
        return null
    }
    val value = try {
        JSONTokener(json).nextValue()
    } catch (_: Exception) {
        return json
    }
    return webKitConvert(value)
}

private fun webKitConvert(value: Any?): Any? {
    return when (value) {
        null, JSONObject.NULL -> null
        is JSONObject -> {
            val map = LinkedHashMap<String, Any?>()
            for (key in value.keys()) {
                map[key] = webKitConvert(value.opt(key))
            }
            map
        }
        is JSONArray -> (0 until value.length()).map { webKitConvert(value.opt(it)) }
        else -> value
    }
}

enum class WKAudiovisualMediaTypes {
    audio,
    video,
    all,
}

enum class WKUserScriptInjectionTime {
    atDocumentStart,
    atDocumentEnd,
}

class WKUserScript(
    val source: String,
    val injectionTime: WKUserScriptInjectionTime,
    val forMainFrameOnly: Boolean,
)

class WKScriptMessage internal constructor(val name: String, val body: Any)

interface WKScriptMessageHandler {
    fun userContentController(userContentController: WKUserContentController, didReceive: WKScriptMessage)
}

class WKUserContentController {
    private val scripts = mutableListOf<WKUserScript>()
    private val handlers = LinkedHashMap<String, WeakReference<WKScriptMessageHandler>>()
    private val observers = mutableListOf<WeakReference<WKWebView>>()

    val userScripts: List<WKUserScript>
        get() = synchronized(this) { scripts.toList() }

    fun addUserScript(userScript: WKUserScript) {
        synchronized(this) {
            scripts.add(userScript)
        }
        notifyChanged()
    }

    fun removeAllUserScripts() {
        synchronized(this) {
            scripts.clear()
        }
        notifyChanged()
    }

    fun add(scriptMessageHandler: WKScriptMessageHandler, name: String) {
        synchronized(this) {
            handlers[name] = WeakReference(scriptMessageHandler)
        }
        notifyChanged()
    }

    fun removeScriptMessageHandler(forName: String) {
        synchronized(this) {
            handlers.remove(forName)
        }
        notifyChanged()
    }

    fun removeAllScriptMessageHandlers() {
        synchronized(this) {
            handlers.clear()
        }
        notifyChanged()
    }

    internal fun handlerNames(): List<String> {
        return synchronized(this) {
            handlers.entries.removeAll { it.value.get() == null }
            handlers.keys.toList()
        }
    }

    internal fun addObserver(webView: WKWebView) {
        synchronized(this) {
            observers.removeAll { it.get() == null }
            observers.add(WeakReference(webView))
        }
    }

    private fun notifyChanged() {
        val targets = synchronized(this) {
            observers.removeAll { it.get() == null }
            observers.mapNotNull { it.get() }
        }
        for (target in targets) {
            target.userContentChanged()
        }
    }

    internal fun dispatch(name: String, body: Any) {
        val handler = synchronized(this) { handlers[name]?.get() } ?: return
        handler.userContentController(this, WKScriptMessage(name = name, body = body))
    }
}

class ProxyConfiguration(val httpCONNECTProxy: NWEndpoint)

internal object WebKitProxy {
    @Volatile
    var configurations: List<ProxyConfiguration> = emptyList()
        private set

    private var appliedRules: List<String> = emptyList()
    private var pendingApplies = 0
    private val waiting = mutableListOf<() -> Unit>()

    fun set(configurations: List<ProxyConfiguration>) {
        this.configurations = configurations
        runOnWebKitMain {
            applyOnMain(configurations)
        }
    }

    fun whenApplied(block: () -> Unit) {
        runOnWebKitMain {
            if (pendingApplies == 0) {
                runGuarded(block)
            } else {
                waiting.add(block)
            }
        }
    }

    private fun applyOnMain(configurations: List<ProxyConfiguration>) {
        val rules = configurations.map { it.httpCONNECTProxy.toString() }
        if (rules == appliedRules) {
            return
        }
        appliedRules = rules
        if (!WebKitFeatures.proxyOverride) {
            WebKitLog.once("proxyUnsupported", "This WebView has no proxy override; browser widgets connect directly")
            return
        }
        pendingApplies += 1
        val finished = AtomicBoolean(false)
        val done = Runnable {
            if (finished.compareAndSet(false, true)) {
                finishApply()
            }
        }
        try {
            val controller = ProxyController.getInstance()
            if (rules.isEmpty()) {
                controller.clearProxyOverride(webKitMainExecutor, done)
                WebKitLog.info("proxy cleared")
            } else {
                val builder = ProxyConfig.Builder()
                for (rule in rules) {
                    builder.addProxyRule(rule, ProxyConfig.MATCH_HTTPS)
                }
                controller.setProxyOverride(builder.build(), webKitMainExecutor, done)
                WebKitLog.info("proxy ${rules.joinToString(", ")} for https and wss")
            }
        } catch (error: Throwable) {
            WebKitLog.error("proxy override failed: $error")
            done.run()
            return
        }
        webKitMainHandler.postDelayed(done, 2000)
    }

    private fun finishApply() {
        pendingApplies = (pendingApplies - 1).coerceAtLeast(0)
        if (pendingApplies > 0) {
            return
        }
        val blocks = waiting.toList()
        waiting.clear()
        for (block in blocks) {
            runGuarded(block)
        }
    }

    private fun runGuarded(block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            WebKitLog.once("deferred:$error", "deferred web view call failed: $error")
        }
    }
}

class WKWebsiteDataStore internal constructor(val isPersistent: Boolean) {
    var proxyConfigurations: List<ProxyConfiguration>
        get() = WebKitProxy.configurations
        set(value) {
            WebKitProxy.set(value)
        }

    companion object {
        private val defaultStore = WKWebsiteDataStore(isPersistent = true)

        fun default(): WKWebsiteDataStore {
            return defaultStore
        }

        fun nonPersistent(): WKWebsiteDataStore {
            return WKWebsiteDataStore(isPersistent = false)
        }
    }
}

class WKWebViewConfiguration {
    var allowsInlineMediaPlayback: Boolean = false
    var allowsPictureInPictureMediaPlayback: Boolean = true
    var mediaTypesRequiringUserActionForPlayback: Set<WKAudiovisualMediaTypes> = setOf(WKAudiovisualMediaTypes.all)
    var userContentController: WKUserContentController = WKUserContentController()
    var websiteDataStore: WKWebsiteDataStore = WKWebsiteDataStore.default()
}

class WKSnapshotConfiguration {
    var snapshotWidth: Double? = null
    var rect: CGRect? = null
    var afterScreenUpdates: Boolean = true
}

class URLRequest(val url: String) {
    constructor(url: java.net.URI) : this(url.toString())

    constructor(url: java.net.URL) : this(url.toString())
}

class UIScrollViewFlags internal constructor(private val onChange: () -> Unit) {
    var backgroundColor: Int? = null
        set(value) {
            field = value
            onChange()
        }

    var showsVerticalScrollIndicator: Boolean = true
        set(value) {
            field = value
            onChange()
        }

    var showsHorizontalScrollIndicator: Boolean = true
        set(value) {
            field = value
            onChange()
        }

    var isScrollEnabled: Boolean = true
        set(value) {
            field = value
            onChange()
        }
}
