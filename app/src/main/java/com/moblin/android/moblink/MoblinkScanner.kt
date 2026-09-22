package com.moblin.android.moblink

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class MoblinkScannerStreamer(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val urls: List<String>,
)

private class DiscoveredSerivce(
    var service: NsdServiceInfo,
) {
    var urls: MutableList<String> = mutableListOf()
}

interface MoblinkScannerDelegate {
    fun moblinkScannerDiscoveredStreamers(streamers: List<MoblinkScannerStreamer>)
}

class MoblinkScanner(
    private val context: Context,
    delegate: MoblinkScannerDelegate,
) : NsdManager.DiscoveryListener, NsdManager.ResolveListener {
    private var nsdManager: NsdManager? = null
    private var services: MutableList<DiscoveredSerivce> = mutableListOf()
    private var delegate: MoblinkScannerDelegate? = delegate
    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun start() {
        val manager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
        nsdManager = manager
        manager.discoverServices(moblinkBonjourType, NsdManager.PROTOCOL_DNS_SD, this)
    }

    fun stop() {
        nsdManager?.let { manager ->
            runCatching { manager.stopServiceDiscovery(this) }
        }
        nsdManager = null
        services.clear()
    }

    private fun discoveredStreamersUpdated() {
        val streamers = mutableListOf<MoblinkScannerStreamer>()
        for (service in services) {
            val metadata = service.service.attributes ?: continue
            val nameData = metadata["name"] ?: continue
            val name = String(nameData, Charsets.UTF_8)
            streamers.add(MoblinkScannerStreamer(name = name, urls = service.urls.toList()))
        }
        val listener = delegate ?: return
        mainScope.launch {
            listener.moblinkScannerDiscoveredStreamers(streamers)
        }
    }

    override fun onDiscoveryStarted(serviceType: String) {
    }

    override fun onServiceFound(service: NsdServiceInfo) {
        if (services.any { it.service.serviceName == service.serviceName }) {
            return
        }
        services.add(DiscoveredSerivce(service))
        nsdManager?.resolveService(service, this)
    }

    override fun onServiceLost(service: NsdServiceInfo) {
    }

    override fun onDiscoveryStopped(serviceType: String) {
    }

    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        stop()
    }

    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
    }

    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
    }

    override fun onServiceResolved(service: NsdServiceInfo) {
        val discoveredService = services.firstOrNull { it.service.serviceName == service.serviceName } ?: return
        val host = service.host ?: return
        val (address, ipv6) = getAddressInfo(host.address)
        val url = formatWebsocketUrl(address = address, ipv6 = ipv6, port = service.port)
        if (url != null) {
            discoveredService.urls.add(url)
        }
        discoveredStreamersUpdated()
    }

    private fun getAddressInfo(address: ByteArray): Pair<String, Boolean> {
        val inetAddress = runCatching { InetAddress.getByAddress(address) }.getOrNull()
            ?: return "" to false
        val ipv6 = inetAddress is Inet6Address
        val hostname = inetAddress.hostAddress ?: ""
        return hostname to ipv6
    }

    private fun formatWebsocketUrl(address: String, ipv6: Boolean, port: Int): String? {
        val host: String
        if (ipv6) {
            val address6 = runCatching { InetAddress.getByName(address) }.getOrNull() as? Inet6Address
                ?: return null
            if (address6.isLinkLocalAddress || address6.isLoopbackAddress) {
                return null
            }
            host = "[$address]"
        } else {
            val address4 = runCatching { InetAddress.getByName(address) }.getOrNull() as? Inet4Address
                ?: return null
            if (address4.isLinkLocalAddress || address4.isLoopbackAddress) {
                return null
            }
            host = address
        }
        return "ws://$host:$port"
    }
}
