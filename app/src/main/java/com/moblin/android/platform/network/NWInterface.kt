package com.moblin.android.platform.network

import android.net.ConnectivityManager
import android.net.Network
import com.moblin.android.AppDelegate

class NWInterface internal constructor(
    val name: String,
    val type: InterfaceType,
    internal val network: Network?,
) {
    enum class InterfaceType {
        wifi,
        cellular,
        wiredEthernet,
        loopback,
        other,
    }

    @Suppress("DEPRECATION")
    internal fun resolveNetwork(): Network? {
        val current = network
        val connectivityManager = try {
            AppDelegate.context.getSystemService(ConnectivityManager::class.java)
        } catch (error: Throwable) {
            null
        } ?: return current
        if (current != null && linkName(connectivityManager, current) == name) {
            return current
        }
        val candidates: Array<Network> = try {
            connectivityManager.allNetworks
        } catch (error: Throwable) {
            emptyArray()
        }
        for (candidate in candidates) {
            if (linkName(connectivityManager, candidate) == name) {
                return candidate
            }
        }
        return current
    }

    private fun linkName(connectivityManager: ConnectivityManager, network: Network): String? {
        return try {
            connectivityManager.getLinkProperties(network)?.interfaceName
        } catch (error: Throwable) {
            null
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is NWInterface && other.name == name && other.type == type
    }

    override fun hashCode(): Int {
        return name.hashCode() * 31 + type.hashCode()
    }

    override fun toString(): String {
        return name
    }
}
