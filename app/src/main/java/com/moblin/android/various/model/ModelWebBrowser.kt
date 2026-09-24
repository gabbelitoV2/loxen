package com.moblin.android.various.model

import android.graphics.Bitmap
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.moblin.android.platform.network.NWEndpoint
import com.moblin.android.platform.webkit.WKWebViewConfiguration
import com.moblin.android.platform.webkit.WebKitProxy
import com.moblin.android.various.WebBrowserController
import com.moblin.android.various.network.setHttpProxy
import java.net.URI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.moblin.android.AppDelegate

private val mainScope = CoroutineScope(Dispatchers.Main)

class WebBrowserState {
    private val _isSmall = MutableStateFlow(false)
    val isSmall: StateFlow<Boolean> = _isSmall

    fun setIsSmall(value: Boolean) {
        _isSmall.value = value
    }
}

private fun Model.getWebBrowserUrl(): String? {
    val parsed = runCatching { URI(webBrowserUrl.value) }.getOrNull()
    val scheme = parsed?.scheme
    if (parsed != null && !scheme.isNullOrEmpty()) {
        return webBrowserUrl.value
    }
    if (webBrowserUrl.value.contains(".")) {
        return "https://${webBrowserUrl.value}"
    }
    return "https://www.google.com/search?q=${webBrowserUrl.value}"
}

fun Model.loadWebBrowserUrl() {
    val url = getWebBrowserUrl() ?: return
    (webBrowser as? WebView)?.loadUrl(url)
}

fun Model.loadWebBrowserHome() {
    webBrowserUrl.value = database.webBrowser.home
    loadWebBrowserUrl()
}

fun Model.loadWebBrowserPage(url: String) {
    webBrowserUrl.value = url
    loadWebBrowserUrl()
}

fun Model.getWebBrowser(): WebView {
    (webBrowser as? WebView)?.let { return it }
    val model = this
    val browser = WebView(AppDelegate.context)
    browser.settings.javaScriptEnabled = true
    browser.settings.domStorageEnabled = true
    browser.settings.javaScriptCanOpenWindowsAutomatically = true
    WKWebViewConfiguration().setHttpProxy(endpoint = getHttpProxyServerNWEndpoint())
    browser.webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            model.webViewDidStartProvisionalNavigation(view, url)
        }
    }
    browser.webChromeClient = WebBrowserChromeClient(webBrowserController)
    webBrowser = browser
    mainScope.launch {
        WebKitProxy.whenApplied {
            model.loadWebBrowserHome()
        }
    }
    return browser
}

fun Model.setWebBrowserProxy() {
    if (webBrowser == null) {
        return
    }
    WKWebViewConfiguration().setHttpProxy(endpoint = getHttpProxyServerNWEndpoint())
}

private fun Model.getHttpProxyServerNWEndpoint(): NWEndpoint? {
    val endpoint = getHttpProxyServerEndpoint() ?: return null
    return NWEndpoint.hostPort(NWEndpoint.Host(endpoint.hostString), NWEndpoint.Port(endpoint.port))
}

private class WebBrowserChromeClient(private val controller: WebBrowserController) : WebChromeClient() {
    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        controller.webViewRunJavaScriptAlertPanelWithMessage(
            message = message ?: "",
            initiatedByFrame = null,
        ) {
            result?.confirm()
        }
        return true
    }

    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        controller.webViewRunJavaScriptConfirmPanelWithMessage(
            message = message ?: "",
            initiatedByFrame = null,
        ) { confirmed ->
            if (confirmed) {
                result?.confirm()
            } else {
                result?.cancel()
            }
        }
        return true
    }

    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?,
    ): Boolean {
        controller.webViewRunJavaScriptTextInputPanelWithPrompt(
            prompt = message ?: "",
            defaultText = defaultValue,
            initiatedByFrame = null,
        ) { text ->
            if (text != null) {
                result?.confirm(text)
            } else {
                result?.cancel()
            }
        }
        return true
    }
}

fun Model.webViewDidStartProvisionalNavigation(webView: WebView?, url: String?) {
    webBrowserUrl.value = webView?.url ?: ""
    database.webBrowser.home = webBrowserUrl.value
}
