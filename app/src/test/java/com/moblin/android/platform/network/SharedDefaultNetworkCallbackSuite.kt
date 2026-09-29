package com.moblin.android.platform.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import com.moblin.android.various.network.InterfaceType
import com.moblin.android.various.network.NetworkInterfaceTypeSelector
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(RobolectricTestRunner::class)
class SharedDefaultNetworkCallbackSuite {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    private fun capabilities(vararg transports: Int): NetworkCapabilities {
        val capabilities = ShadowNetworkCapabilities.newInstance()
        for (transport in transports) {
            shadowOf(capabilities).addTransportType(transport)
        }
        return capabilities
    }

    @Test
    fun manySelectorsShareOneSystemRegistration() {
        val before = shadowOf(connectivityManager).networkCallbacks.size
        val selectors = List(150) { NetworkInterfaceTypeSelector(context, Dispatchers.Default) }
        assertTrue(shadowOf(connectivityManager).networkCallbacks.size <= before + 1)
        assertTrue(SharedDefaultNetworkCallback.registeredCallbacks() >= selectors.size)
    }

    @Test
    fun theDefaultNetworkReachesEverySelector() {
        val selectors = List(3) { NetworkInterfaceTypeSelector(context, Dispatchers.Default) }
        val network: Network = ShadowNetwork.newInstance(7)
        SharedDefaultNetworkCallback.systemCallback.onCapabilitiesChanged(
            network,
            capabilities(NetworkCapabilities.TRANSPORT_WIFI),
        )
        for (selector in selectors) {
            assertEquals(InterfaceType.wifi, selector.getType())
        }
    }

    @Test
    fun aLateSelectorGetsTheCurrentDefaultNetwork() {
        val network: Network = ShadowNetwork.newInstance(8)
        SharedDefaultNetworkCallback.systemCallback.onAvailable(network)
        SharedDefaultNetworkCallback.systemCallback.onCapabilitiesChanged(
            network,
            capabilities(NetworkCapabilities.TRANSPORT_ETHERNET),
        )
        val selector = NetworkInterfaceTypeSelector(context, Dispatchers.Default)
        assertEquals(InterfaceType.wiredEthernet, selector.getType())
    }
}
