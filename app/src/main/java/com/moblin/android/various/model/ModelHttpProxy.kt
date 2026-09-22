package com.moblin.android.various.model

import com.moblin.android.various.network.HttpProxyServer
import com.moblin.android.various.network.HttpProxyServerDelegate
import java.net.InetSocketAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

fun Model.httpProxyServerChanged() {
    reloadHttpProxyServer()
}

fun Model.reloadHttpProxyServer() {
    stopHttpProxyServer()
    val httpProxy = database.httpProxy
    if (httpProxy.enabled.value || httpProxy.localNetwork.value) {
        startHttpProxyServer()
    } else {
        proxyServerPortUpdated()
    }
}

fun Model.getHttpProxyServerEndpoint(): InetSocketAddress? {
    val port = httpProxyPort
    return if (database.httpProxy.enabled.value && port != null) {
        InetSocketAddress("127.0.0.1", port)
    } else {
        null
    }
}

private fun Model.proxyServerPortUpdated() {
    setWebBrowserProxy()
    setBrowserEffectsProxyServer()
}

private fun Model.startHttpProxyServer() {
    val httpProxy = database.httpProxy
    httpProxyServer = HttpProxyServer(TODO("Context"))
    httpProxyServer?.delegate = object : HttpProxyServerDelegate {
        override fun httpProxyServerPortReady(port: Int) {
            this@startHttpProxyServer.httpProxyServerPortReady(port)
        }
    }
    httpProxyServer?.start(httpProxy.port.value.toInt(), httpProxy.localNetwork.value)
}

fun Model.stopHttpProxyServer() {
    httpProxyServer?.stop()
    httpProxyServer = null
    httpProxyPort = null
}

fun Model.httpProxyServerPortReady(port: Int) {
    mainScope.launch {
        this@httpProxyServerPortReady.httpProxyPort = port
        this@httpProxyServerPortReady.proxyServerPortUpdated()
    }
}
