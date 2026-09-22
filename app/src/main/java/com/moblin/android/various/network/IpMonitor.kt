package com.moblin.android.various.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface

class IPMonitor(private val context: Context) {

    enum class IPType {
        ipv4,
        ipv6;

        fun formatAddress(address: String): String {
            return when (this) {
                ipv4 -> address
                ipv6 -> "[$address]"
            }
        }
    }

    enum class InterfaceType {
        wifi,
        cellular,
        wiredEthernet,
        loopback,
        other,
    }

    data class Status(
        var id: Int,
        var name: String,
        var interfaceType: InterfaceType,
        var ip: String,
        var ipType: IPType,
    )

    private val connectivityManager: ConnectivityManager? =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val monitor: ConnectivityManager.NetworkCallback

    var pathUpdateHandler: ((List<Status>) -> Unit)? = null

    init {
        monitor = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handlePathUpdate()
            }

            override fun onLost(network: Network) {
                handlePathUpdate()
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                handlePathUpdate()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                handlePathUpdate()
            }
        }
    }

    private fun handlePathUpdate() {
        val statuses = mutableListOf<Status>()
        var id = 0
        for (network in uniqueAvailableInterfaces()) {
            val name = getInterfaceName(network) ?: continue
            for ((ip, type) in getIpAddresses(interfaceName = name)) {
                statuses.add(
                    Status(
                        id = id,
                        name = name,
                        interfaceType = getInterfaceType(network),
                        ip = ip,
                        ipType = type,
                    )
                )
                id += 1
            }
        }
        pathUpdateHandler?.invoke(statuses)
    }

    fun start() {
        val connectivityManager = connectivityManager ?: return
        connectivityManager.registerDefaultNetworkCallback(monitor, Handler(Looper.getMainLooper()))
    }

    private fun getIpAddresses(interfaceName: String): List<Pair<String, IPType>> {
        val addresses = mutableListOf<Pair<String, IPType>>()
        val networkInterface = NetworkInterface.getByName(interfaceName) ?: return addresses
        for (inetAddress in networkInterface.inetAddresses) {
            val type = when (inetAddress) {
                is Inet4Address -> IPType.ipv4
                is Inet6Address -> IPType.ipv6
                else -> continue
            }
            val hostAddress = inetAddress.hostAddress ?: continue
            val address = hostAddress.substringBefore('%') to type
            if (!addresses.contains(address)) {
                addresses.add(address)
            }
        }
        return addresses
    }

    private fun uniqueAvailableInterfaces(): List<Network> {
        val connectivityManager = connectivityManager ?: return emptyList()
        val interfaces = mutableListOf<Network>()
        val names = mutableSetOf<String>()
        for (network in connectivityManager.allNetworks) {
            val name = getInterfaceName(network) ?: continue
            if (names.add(name)) {
                interfaces.add(network)
            }
        }
        return interfaces
    }

    private fun getInterfaceName(network: Network): String? {
        val connectivityManager = connectivityManager ?: return null
        val linkProperties = connectivityManager.getLinkProperties(network) ?: return null
        return linkProperties.interfaceName
    }

    private fun getInterfaceType(network: Network): InterfaceType {
        val connectivityManager = connectivityManager ?: return InterfaceType.other
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return InterfaceType.other
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> InterfaceType.wifi
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> InterfaceType.cellular
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> InterfaceType.wiredEthernet
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> InterfaceType.other
            else -> InterfaceType.other
        }
    }
}
