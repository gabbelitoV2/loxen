package com.moblin.android.streamingplatforms.twitch

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.moblin.android.various.Keychain
import com.moblin.android.various.model.Model
import com.moblin.android.various.utils.randomHumanString
import com.moblin.android.view.CloseButtonTopRightView
import java.net.URI
import java.util.UUID
import com.moblin.android.LocalModel

private const val authorizeUrl = "https://id.twitch.tv/oauth2/authorize"
const val twitchMoblinAppClientId = "qv6bnocuwapqigeqjoamfhif0cv2xn"
private val scopes = listOf(
    "user:read:chat",
    "user:read:follows",
    "user:write:chat",
    "moderator:read:followers",
    "moderator:read:blocked_terms",
    "moderator:read:unban_requests",
    "moderator:read:warnings",
    "moderator:read:moderators",
    "moderator:read:vips",
    "moderator:manage:chat_messages",
    "moderator:manage:banned_users",
    "moderator:manage:chat_settings",
    "moderator:manage:announcements",
    "moderator:manage:shoutouts",
    "channel:moderate",
    "channel:read:subscriptions",
    "channel:read:redemptions",
    "channel:read:stream_key",
    "channel:read:hype_train",
    "channel:read:ads",
    "channel:manage:polls",
    "channel:manage:predictions",
    "channel:manage:broadcast",
    "channel:manage:moderators",
    "channel:manage:vips",
    "channel:manage:raids",
    "channel:edit:commercial",
    "bits:read",
)
private const val browserRedirectHost = "localhost"
private val browserRedirectUri = "https://$browserRedirectHost"
private const val sessionRedirectHost = "mys-lang.org"
private const val sessionRedirectPath = "/auth"
private val sessionRedirectUri = "https://$sessionRedirectHost$sessionRedirectPath"
private const val twitchAuthServer = "www.twitch.tv"

@Composable
private fun TwitchAuthView(twitchAuth: TwitchAuth, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context -> twitchAuth.getWebBrowser(context) },
    )
}

@Composable
fun TwitchLoginView(model: Model = LocalModel.current, presenting: Boolean, onPresentingChange: (Boolean) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            TwitchAuthView(twitchAuth = model.twitchAuth, modifier = Modifier.height(2500.dp))
        }
        CloseButtonTopRightView {
            onPresentingChange(false)
        }
    }
}

class TwitchAuth {
    private var webBrowser: WebView? = null
    private var session: Any? = null
    private var onAccessToken: ((String) -> Unit)? = null
    private var showWebBrowser: (() -> Unit)? = null

    fun login(showWebBrowser: () -> Unit) {
        this.showWebBrowser = showWebBrowser
        if (!startSession()) {
            showWebBrowser()
        }
    }

    private fun startSession(): Boolean {
        return false
    }

    private fun handleSessionCompleted(url: URI?, error: Throwable?) {
        session = null
        if (error != null) {
            Log.i("TwitchAuth", "twitch: auth: Session failed with $error")
            if (!isCanceledByUser(error)) {
                showWebBrowser?.invoke()
            }
            return
        }
        val callbackUrl = url ?: return
        val accessToken = extractAccessToken(callbackUrl) ?: return
        onAccessToken?.invoke(accessToken)
    }

    fun getWebBrowser(context: Context): WebView {
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webBrowser = webView
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (view == null) {
                    return
                }
                this@TwitchAuth.webView(view, null)
            }
        }
        val url = buildAuthUrl(browserRedirectUri) ?: return webView
        webView.loadUrl(url.toString())
        return webView
    }

    fun setOnAccessToken(onAccessToken: (String) -> Unit) {
        this.onAccessToken = onAccessToken
    }

    private fun buildAuthUrl(redirectUri: String): URI? {
        val scope = scopes.joinToString("+") { Uri.encode(it) }
        val url = authorizeUrl +
            "?client_id=${Uri.encode(twitchMoblinAppClientId)}" +
            "&force_verify=true" +
            "&redirect_uri=${Uri.encode(redirectUri)}" +
            "&response_type=token" +
            "&scope=$scope" +
            "&state=${Uri.encode(randomHumanString())}"
        return runCatching { URI(url) }.getOrNull()
    }

    fun presentationAnchor(): Any? {
        return null
    }

    fun webView(webView: WebView, didStartProvisionalNavigation: Any?) {
        val url = webView.url?.let { runCatching { URI(it) }.getOrNull() } ?: return
        if (url.host != browserRedirectHost) {
            return
        }
        val accessToken = extractAccessToken(url) ?: return
        onAccessToken?.invoke(accessToken)
    }
}

private fun isCanceledByUser(error: Throwable): Boolean {
    return false
}

private fun extractAccessToken(url: URI): String? {
    val fragment = url.fragment ?: return null
    return Uri.parse("foo:///?$fragment").getQueryParameter("access_token")
}

fun storeTwitchAccessTokenInKeychain(streamId: UUID, accessToken: String) {
    createKeychain(streamId.toString()).store(accessToken)
}

fun loadTwitchAccessTokenFromKeychain(streamId: UUID): String? {
    return createKeychain(streamId.toString()).load()
}

fun removeTwitchAccessTokenInKeychain(streamId: UUID) {
    createKeychain(streamId.toString()).remove()
}

fun removeUnusedTwitchAccessTokensInKeychain(usedStreamIds: List<UUID>) {
    val used = usedStreamIds.map { it.toString() }.toSet()
    for (streamId in Keychain.loadStreamIds(twitchAuthServer)) {
        if (!used.contains(streamId)) {
            createKeychain(streamId).remove()
        }
    }
}

private fun createKeychain(streamId: String): Keychain {
    return Keychain(streamId = streamId, server = twitchAuthServer, logPrefix = "twitch: auth")
}
