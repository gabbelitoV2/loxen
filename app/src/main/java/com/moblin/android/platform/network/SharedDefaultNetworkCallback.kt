package com.moblin.android.platform.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import java.lang.ref.WeakReference

object SharedDefaultNetworkCallback {
    private const val TAG = "SharedDefaultNetwork"
    private val callbacks = mutableListOf<WeakReference<ConnectivityManager.NetworkCallback>>()
    private var registered = false
    private var network: Network? = null
    private var capabilities: NetworkCapabilities? = null
    private var linkProperties: LinkProperties? = null

    internal val systemCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            synchronized(this@SharedDefaultNetworkCallback) {
                this@SharedDefaultNetworkCallback.network = network
            }
            forEach { it.onAvailable(network) }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            synchronized(this@SharedDefaultNetworkCallback) {
                this@SharedDefaultNetworkCallback.network = network
                capabilities = networkCapabilities
            }
            forEach { it.onCapabilitiesChanged(network, networkCapabilities) }
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            synchronized(this@SharedDefaultNetworkCallback) {
                this@SharedDefaultNetworkCallback.linkProperties = linkProperties
            }
            forEach { it.onLinkPropertiesChanged(network, linkProperties) }
        }

        override fun onLost(network: Network) {
            synchronized(this@SharedDefaultNetworkCallback) {
                if (this@SharedDefaultNetworkCallback.network == network) {
                    this@SharedDefaultNetworkCallback.network = null
                    capabilities = null
                    linkProperties = null
                }
            }
            forEach { it.onLost(network) }
        }
    }

    fun register(context: Context, callback: ConnectivityManager.NetworkCallback) {
        val replay: Triple<Network, NetworkCapabilities?, LinkProperties?>?
        synchronized(this) {
            callbacks.removeAll { it.get() == null || it.get() === callback }
            callbacks.add(WeakReference(callback))
            if (!registered) {
                val manager = context.getSystemService(ConnectivityManager::class.java) ?: return
                try {
                    manager.registerDefaultNetworkCallback(systemCallback)
                    registered = true
                } catch (error: RuntimeException) {
                    Log.i(TAG, "Register default network callback failed: $error")
                }
            }
            replay = network?.let { Triple(it, capabilities, linkProperties) }
        }
        if (replay != null) {
            callback.onAvailable(replay.first)
            replay.second?.let { callback.onCapabilitiesChanged(replay.first, it) }
            replay.third?.let { callback.onLinkPropertiesChanged(replay.first, it) }
        }
    }

    fun unregister(callback: ConnectivityManager.NetworkCallback) {
        synchronized(this) {
            callbacks.removeAll { it.get() == null || it.get() === callback }
        }
    }

    internal fun registeredCallbacks(): Int = synchronized(this) {
        callbacks.removeAll { it.get() == null }
        callbacks.size
    }

    private fun forEach(action: (ConnectivityManager.NetworkCallback) -> Unit) {
        val alive = synchronized(this) {
            callbacks.removeAll { it.get() == null }
            callbacks.mapNotNull { it.get() }
        }
        alive.forEach(action)
    }
}
