package com.moblin.android.various.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineDispatcher

enum class InterfaceType {
    cellular,
    wifi,
    wiredEthernet,
    other,
}

class NetworkInterfaceTypeSelector(
    context: Context,
    queue: CoroutineDispatcher,
    cellular: Boolean = true,
) {
    private val interfaceTypes: ArrayDeque<InterfaceType> = ArrayDeque(
        listOf(
            InterfaceType.cellular,
            InterfaceType.wifi,
            InterfaceType.wiredEthernet,
            InterfaceType.other,
        ),
    )
    private val cellular: Boolean = cellular
    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            interfaceTypes.clear()
            val wifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val cell = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val ethernet = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            if (wifi) {
                interfaceTypes.add(InterfaceType.wifi)
            }
            if (this@NetworkInterfaceTypeSelector.cellular && cell) {
                interfaceTypes.add(InterfaceType.cellular)
            }
            if (ethernet) {
                interfaceTypes.add(InterfaceType.wiredEthernet)
            }
            if (!wifi && !cell && !ethernet) {
                interfaceTypes.add(InterfaceType.other)
            }
        }
    }

    init {
        if (!cellular) {
            interfaceTypes.removeFirst()
        }
        com.moblin.android.platform.network.SharedDefaultNetworkCallback.register(context, networkCallback)
    }

    fun getNextType(): InterfaceType? {
        val interfaceType = interfaceTypes.removeFirstOrNull() ?: return null
        interfaceTypes.add(interfaceType)
        return interfaceType
    }

    fun getType(): InterfaceType? {
        return interfaceTypes.firstOrNull()
    }

    fun markBad(interfaceType: InterfaceType) {
        if (interfaceType != interfaceTypes.firstOrNull()) {
            return
        }
        interfaceTypes.removeFirst()
        interfaceTypes.add(interfaceType)
    }
}
