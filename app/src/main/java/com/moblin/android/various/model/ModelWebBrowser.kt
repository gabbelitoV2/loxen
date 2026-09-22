package com.moblin.android.various.model

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.URI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
    val browser = WebView(TODO("android.webkit.WebView requires a Context to be constructed"))
    browser.settings.javaScriptCanOpenWindowsAutomatically = true
    browser.settings.mediaPlaybackRequiresUserGesture = false
    TODO("no Android counterpart for WKWebViewConfiguration.setHttpProxy")
    browser.webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            model.webViewDidStartProvisionalNavigation(view, url)
        }
    }
    TODO("no Android counterpart for WKUIDelegate")
    webBrowser = browser
    mainScope.launch {
        model.loadWebBrowserHome()
    }
    return browser
}

fun Model.setWebBrowserProxy() {
    TODO("no Android counterpart for WKWebViewConfiguration.setHttpProxy")
}

fun Model.webViewDidStartProvisionalNavigation(webView: WebView?, url: String?) {
    webBrowserUrl.value = webView?.url ?: ""
    database.webBrowser.home = webBrowserUrl.value
}
