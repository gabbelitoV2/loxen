package com.moblin.android.streamingplatforms.kick

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.moblin.android.various.Keychain
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val kickDomain = "kick.com"
private const val loginUrl = "https://kick.com/login"
private const val sessionTokenCookieName = "session_token"

@Composable
fun KickLoginView(
    presenting: Boolean,
    onPresentingChange: (Boolean) -> Unit,
    onAccessToken: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { onPresentingChange(false) }) {
                Text(text = "Close")
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            KickWebView(
                onAccessToken = {
                    onAccessToken(it)
                    onPresentingChange(false)
                },
                modifier = Modifier.height(1200.dp),
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KickWebView(onAccessToken: (String) -> Unit, modifier: Modifier = Modifier) {
    val coordinator = remember { KickWebViewCoordinator(onAccessToken) }
    var webView: WebView? by remember { mutableStateOf(null) }
    DisposableEffect(Unit) {
        onDispose {
            coordinator.stop()
            webView?.stopLoading()
        }
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                CookieManager.getInstance().removeAllCookies(null)
                webViewClient = coordinator
                loadUrl(loginUrl)
                coordinator.periodicallyCheckForAccessTokenCookie(this)
            }.also { webView = it }
        },
        update = { view ->
            view.webViewClient = coordinator
        },
    )
}

class KickWebViewCoordinator(
    private val onAccessToken: (String) -> Unit,
) : WebViewClient() {
    private var loginButtonClicked = false
    private val scope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null

    fun stop() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onPageFinished(view: WebView, url: String?) {
        if (!loginButtonClicked) {
            detectAndClickLoginButton(view)
        }
    }

    fun periodicallyCheckForAccessTokenCookie(webView: WebView) {
        if (timerJob?.isActive == true) {
            return
        }
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                val accessToken = findSessionTokenCookie()
                if (accessToken != null) {
                    onAccessToken(Uri.decode(accessToken))
                    return@launch
                }
            }
        }
    }

    private fun detectAndClickLoginButton(webView: WebView) {
        val detectAndClickLoginButtonScript = """
            (async function() {
                try {
                    for (var attempt = 0; attempt < 50; attempt++) {
                        await new Promise(resolve => setTimeout(resolve, 200));
                        if (document.querySelector('input[name="emailOrUsername"]')) {
                            return true;
                        }
                        var loginButton = document.querySelector('[data-testid="login"]');
                        if (loginButton) {
                            loginButton.click();
                        }
                    }
                    return false;
                } catch (error) {
                    return false;
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(detectAndClickLoginButtonScript) { result ->
            if (result == "true") {
                loginButtonClicked = true
            }
        }
    }

    private fun findSessionTokenCookie(): String? {
        val cookies = CookieManager.getInstance().getCookie(loginUrl) ?: return null
        for (cookie in cookies.split(";")) {
            val trimmedCookie = cookie.trim()
            if (trimmedCookie.startsWith("$sessionTokenCookieName=")) {
                return trimmedCookie.substringAfter('=')
            }
        }
        return null
    }
}

fun storeKickAccessTokenInKeychain(streamId: UUID, accessToken: String) {
    createKeychain(streamId.toString()).store(value = accessToken)
}

fun loadKickAccessTokenFromKeychain(streamId: UUID): String? {
    return createKeychain(streamId.toString()).load()
}

fun removeKickAccessTokenInKeychain(streamId: UUID) {
    createKeychain(streamId.toString()).remove()
}

fun removeUnusedKickAccessTokensInKeychain(usedStreamIds: List<UUID>) {
    val usedStreamIds = usedStreamIds.map { it.toString() }.toSet()
    for (streamId in Keychain.loadStreamIds(server = kickDomain)) {
        if (!usedStreamIds.contains(streamId)) {
            createKeychain(streamId).remove()
        }
    }
}

private fun createKeychain(streamId: String): Keychain {
    return Keychain(streamId = streamId, server = kickDomain, logPrefix = "kick: auth")
}
