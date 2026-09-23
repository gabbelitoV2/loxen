package com.moblin.android.platform.webkit

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.network.NWEndpoint
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

private const val TAG = "MoblinWeb"

internal object WebKitLog {
    private val loggedMessages = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun once(key: String, message: String) {
        if (loggedMessages.size > 1000) {
            loggedMessages.clear()
        }
        if (!loggedMessages.add(key)) {
            return
        }
        try {
            Log.i(TAG, message)
        } catch (_: Throwable) {
        }
    }

    fun notImplemented(member: String) {
        once("notImplemented:$member", "$member not implemented yet")
    }
}

internal val webKitMainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

internal fun isWebKitMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()

internal fun runOnWebKitMain(block: () -> Unit) {
    if (isWebKitMainThread()) {
        block()
    } else {
        webKitMainHandler.post(block)
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

    val userScripts: List<WKUserScript>
        get() = synchronized(this) { scripts.toList() }

    fun addUserScript(userScript: WKUserScript) {
        synchronized(this) {
            scripts.add(userScript)
        }
    }

    fun removeAllUserScripts() {
        synchronized(this) {
            scripts.clear()
        }
    }

    fun add(scriptMessageHandler: WKScriptMessageHandler, name: String) {
        synchronized(this) {
            handlers[name] = WeakReference(scriptMessageHandler)
        }
    }

    fun removeScriptMessageHandler(forName: String) {
        synchronized(this) {
            handlers.remove(forName)
        }
    }

    fun removeAllScriptMessageHandlers() {
        synchronized(this) {
            handlers.clear()
        }
    }

    internal fun handlerNames(): List<String> {
        return synchronized(this) {
            handlers.entries.removeAll { it.value.get() == null }
            handlers.keys.toList()
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

    fun apply(configurations: List<ProxyConfiguration>) {
        this.configurations = configurations
        WebKitLog.notImplemented("WKWebsiteDataStore.proxyConfigurations")
    }
}

class WKWebsiteDataStore internal constructor(val isPersistent: Boolean) {
    var proxyConfigurations: List<ProxyConfiguration>
        get() = WebKitProxy.configurations
        set(value) {
            WebKitProxy.apply(value)
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
