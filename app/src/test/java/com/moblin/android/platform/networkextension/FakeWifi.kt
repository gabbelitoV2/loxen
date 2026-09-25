package com.moblin.android.platform.networkextension

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.robolectric.shadows.ShadowNetworkInfo
import org.robolectric.shadows.ShadowWifiInfo

class FakeWifi(context: Context) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val network: Network = ShadowNetwork.newInstance(100)

    init {
        shadowOf(connectivityManager).clearAllNetworks()
    }

    fun connect() {
        val networkInfo = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.CONNECTED,
            ConnectivityManager.TYPE_WIFI,
            0,
            true,
            NetworkInfo.State.CONNECTED,
        )
        shadowOf(connectivityManager).addNetwork(network, networkInfo)
        val capabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(capabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        shadowOf(connectivityManager).setNetworkCapabilities(network, capabilities)
    }

    fun pendingRequests(): List<ConnectivityManager.NetworkCallback> =
        shadowOf(connectivityManager).networkCallbacks.filter { it.javaClass.simpleName == "WifiTransportInfoRequest" }

    fun answer(ssid: String): Int {
        val requests = pendingRequests()
        val info = ShadowWifiInfo.newInstance()
        shadowOf(info).setSSID(ssid)
        val capabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(capabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        shadowOf(capabilities).setTransportInfo(info)
        for (request in requests) {
            request.onCapabilitiesChanged(network, capabilities)
        }
        return requests.size
    }
}
