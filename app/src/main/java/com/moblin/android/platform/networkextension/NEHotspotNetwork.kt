package com.moblin.android.platform.networkextension

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.moblin.android.AppDelegate

private const val TAG = "NEHotspotNetwork"

class NEHotspotNetwork internal constructor(val ssid: String, val bssid: String) {
    companion object {
        fun fetchCurrent(completionHandler: (NEHotspotNetwork?) -> Unit) {
            val context = try {
                AppDelegate.context
            } catch (error: UninitializedPropertyAccessException) {
                null
            }
            fetchCurrent(context, Build.VERSION.SDK_INT, completionHandler)
        }

        internal fun fetchCurrent(
            context: Context?,
            sdkInt: Int,
            completionHandler: (NEHotspotNetwork?) -> Unit,
        ) {
            val handler = Handler(Looper.getMainLooper())
            if (context == null || !hasLocationPermission(context, sdkInt)) {
                handler.post { completionHandler(null) }
                return
            }
            if (sdkInt >= Build.VERSION_CODES.S) {
                fetchFromTransportInfo(context, handler, completionHandler)
            } else {
                val network = fetchFromWifiManager(context)
                handler.post { completionHandler(network) }
            }
        }

        internal fun unquotedSsid(rawSsid: String?): String? {
            if (rawSsid.isNullOrEmpty() || rawSsid == "<unknown ssid>") {
                return null
            }
            if (rawSsid.length >= 2 && rawSsid.startsWith("\"") && rawSsid.endsWith("\"")) {
                return rawSsid.substring(1, rawSsid.length - 1).ifEmpty { null }
            }
            return rawSsid
        }

        internal fun network(info: WifiInfo?): NEHotspotNetwork? {
            info ?: return null
            val ssid = unquotedSsid(info.ssid) ?: return null
            return NEHotspotNetwork(ssid = ssid, bssid = info.bssid ?: "")
        }

        private fun hasLocationPermission(context: Context, sdkInt: Int): Boolean {
            if (isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                return true
            }
            return sdkInt < Build.VERSION_CODES.Q && isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        private fun isGranted(context: Context, permission: String): Boolean =
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

        @Suppress("DEPRECATION")
        private fun fetchFromWifiManager(context: Context): NEHotspotNetwork? {
            return try {
                val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
                network(wifiManager?.connectionInfo)
            } catch (error: SecurityException) {
                Log.i(TAG, "Not allowed to read the WiFi connection: ${error.message}")
                null
            }
        }

        @RequiresApi(Build.VERSION_CODES.S)
        private fun fetchFromTransportInfo(
            context: Context,
            handler: Handler,
            completionHandler: (NEHotspotNetwork?) -> Unit,
        ) {
            val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
            val hasWifi = try {
                connectivityManager != null && hasWifiNetwork(connectivityManager)
            } catch (error: SecurityException) {
                Log.i(TAG, "Not allowed to list networks: ${error.message}")
                false
            }
            if (connectivityManager == null || !hasWifi) {
                handler.post { completionHandler(null) }
                return
            }
            WifiTransportInfoRequest(connectivityManager, handler, completionHandler).start()
        }

        @Suppress("DEPRECATION")
        private fun hasWifiNetwork(connectivityManager: ConnectivityManager): Boolean =
            connectivityManager.allNetworks.any {
                connectivityManager.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private class WifiTransportInfoRequest(
    private val connectivityManager: ConnectivityManager,
    private val handler: Handler,
    private val completionHandler: (NEHotspotNetwork?) -> Unit,
) : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
    private var completed = false
    private val timeout = Runnable { complete(null) }

    fun start() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(request, this, handler)
        } catch (error: RuntimeException) {
            Log.i(TAG, "Failed to observe WiFi networks: ${error.message}")
            completed = true
            handler.post { completionHandler(null) }
            return
        }
        handler.postDelayed(timeout, 1000)
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        val info = networkCapabilities.transportInfo as? WifiInfo ?: return
        complete(NEHotspotNetwork.network(info))
    }

    private fun complete(network: NEHotspotNetwork?) {
        if (completed) {
            return
        }
        completed = true
        handler.removeCallbacks(timeout)
        try {
            connectivityManager.unregisterNetworkCallback(this)
        } catch (error: IllegalArgumentException) {
            Log.i(TAG, "WiFi network callback already unregistered")
        }
        completionHandler(network)
    }
}
