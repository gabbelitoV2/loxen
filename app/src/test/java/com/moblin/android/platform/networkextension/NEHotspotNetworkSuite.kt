package com.moblin.android.platform.networkextension

import android.Manifest
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.robolectric.shadows.ShadowNetworkInfo
import org.robolectric.shadows.ShadowWifiInfo

@RunWith(RobolectricTestRunner::class)
class NEHotspotNetworkSuite {
    private lateinit var application: Application
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiManager: WifiManager
    private val results = mutableListOf<NEHotspotNetwork?>()
    private var completions = 0

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        connectivityManager = application.getSystemService(ConnectivityManager::class.java)
        wifiManager = application.getSystemService(WifiManager::class.java)
        shadowOf(connectivityManager).clearAllNetworks()
        shadowOf(application).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }

    private fun fetch(sdkInt: Int = Build.VERSION_CODES.VANILLA_ICE_CREAM, context: Context? = application) {
        NEHotspotNetwork.fetchCurrent(context, sdkInt) {
            completions += 1
            results.add(it)
        }
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun wifiInfo(ssid: String?, bssid: String = "aa:bb:cc:dd:ee:ff"): WifiInfo {
        val info = ShadowWifiInfo.newInstance()
        if (ssid != null) {
            shadowOf(info).setSSID(ssid)
        }
        shadowOf(info).setBSSID(bssid)
        return info
    }

    private fun addNetwork(netId: Int, transport: Int, type: Int): Network {
        val network = ShadowNetwork.newInstance(netId)
        val networkInfo = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.CONNECTED,
            type,
            0,
            true,
            NetworkInfo.State.CONNECTED,
        )
        shadowOf(connectivityManager).addNetwork(network, networkInfo)
        val capabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(capabilities).addTransportType(transport)
        shadowOf(connectivityManager).setNetworkCapabilities(network, capabilities)
        return network
    }

    private fun addWifiNetwork(): Network =
        addNetwork(100, NetworkCapabilities.TRANSPORT_WIFI, ConnectivityManager.TYPE_WIFI)

    private fun capabilitiesWith(info: WifiInfo?): NetworkCapabilities {
        val capabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(capabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        if (info != null) {
            shadowOf(capabilities).setTransportInfo(info)
        }
        return capabilities
    }

    private fun registeredCallbacks(): Set<ConnectivityManager.NetworkCallback> =
        shadowOf(connectivityManager).networkCallbacks

    private fun grant(vararg permissions: String) {
        shadowOf(application).grantPermissions(*permissions)
    }

    @Test
    fun ssidDropsTheQuotesAndroidAddsAroundUtf8Names() {
        assertEquals("Home", NEHotspotNetwork.unquotedSsid("\"Home\""))
        assertEquals("My \"quoted\" net", NEHotspotNetwork.unquotedSsid("\"My \"quoted\" net\""))
        assertEquals("48656c6c6f", NEHotspotNetwork.unquotedSsid("48656c6c6f"))
    }

    @Test
    fun unknownOrEmptySsidIsNoNetwork() {
        assertNull(NEHotspotNetwork.unquotedSsid(null))
        assertNull(NEHotspotNetwork.unquotedSsid(""))
        assertNull(NEHotspotNetwork.unquotedSsid("\"\""))
        assertNull(NEHotspotNetwork.unquotedSsid("<unknown ssid>"))
    }

    @Test
    fun withoutLocationPermissionTheHandlerGetsNullLikeIos() {
        addWifiNetwork()
        shadowOf(wifiManager).setConnectionInfo(wifiInfo("Home"))
        fetch()
        fetch(sdkInt = Build.VERSION_CODES.P)
        assertEquals(0, completions)
        runMain()
        assertEquals(listOf<NEHotspotNetwork?>(null, null), results)
        assertTrue(registeredCallbacks().isEmpty())
    }

    @Test
    fun withoutAContextTheHandlerGetsNull() {
        fetch(context = null)
        runMain()
        assertEquals(listOf<NEHotspotNetwork?>(null), results)
    }

    @Test
    fun beforeAndroid12TheSsidComesFromWifiManager() {
        grant(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(wifiManager).setConnectionInfo(wifiInfo("Home", "11:22:33:44:55:66"))
        fetch(sdkInt = Build.VERSION_CODES.R)
        assertEquals(0, completions)
        runMain()
        assertEquals("Home", results.single()?.ssid)
        assertEquals("11:22:33:44:55:66", results.single()?.bssid)
        assertTrue(registeredCallbacks().isEmpty())
    }

    @Test
    fun coarseLocationIsEnoughOnlyBeforeAndroid10() {
        grant(Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(wifiManager).setConnectionInfo(wifiInfo("Home"))
        fetch(sdkInt = Build.VERSION_CODES.P)
        fetch(sdkInt = Build.VERSION_CODES.Q)
        runMain()
        assertEquals("Home", results[0]?.ssid)
        assertNull(results[1])
    }

    @Test
    fun beforeAndroid12AnUnknownSsidIsNoNetwork() {
        grant(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(wifiManager).setConnectionInfo(wifiInfo(null))
        fetch(sdkInt = Build.VERSION_CODES.R)
        runMain()
        assertEquals(listOf<NEHotspotNetwork?>(null), results)
    }

    @Test
    fun beforeAndroid12AMissingWifiStatePermissionGivesNullInsteadOfCrashing() {
        grant(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(wifiManager).setConnectionInfo(wifiInfo("Home"))
        shadowOf(wifiManager).setAccessWifiStatePermission(false)
        fetch(sdkInt = Build.VERSION_CODES.R)
        runMain()
        assertEquals(listOf<NEHotspotNetwork?>(null), results)
    }

    @Test
    fun fromAndroid12TheSsidComesFromTheWifiTransportInfoWithLocation() {
        grant(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val network = addWifiNetwork()
        fetch()
        val callback = registeredCallbacks().single()
        assertEquals(0, completions)
        callback.onCapabilitiesChanged(network, capabilitiesWith(wifiInfo("Home", "11:22:33:44:55:66")))
        assertEquals("Home", results.single()?.ssid)
        assertEquals("11:22:33:44:55:66", results.single()?.bssid)
        assertTrue(registeredCallbacks().isEmpty())
        callback.onCapabilitiesChanged(network, capabilitiesWith(wifiInfo("Other")))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertEquals(1, completions)
    }

    @Test
    fun fromAndroid12CapabilitiesWithoutWifiInfoAreIgnoredUntilTheyArrive() {
        grant(Manifest.permission.ACCESS_FINE_LOCATION)
        val network = addWifiNetwork()
        fetch()
        val callback = registeredCallbacks().single()
        callback.onCapabilitiesChanged(network, capabilitiesWith(null))
        assertEquals(0, completions)
        callback.onCapabilitiesChanged(network, capabilitiesWith(wifiInfo("Home")))
        assertEquals("Home", results.single()?.ssid)
    }

    @Test
    fun fromAndroid12ARedactedSsidIsNoNetwork() {
        grant(Manifest.permission.ACCESS_FINE_LOCATION)
        val network = addWifiNetwork()
        fetch()
        registeredCallbacks().single().onCapabilitiesChanged(network, capabilitiesWith(wifiInfo(null)))
        assertEquals(listOf<NEHotspotNetwork?>(null), results)
        assertTrue(registeredCallbacks().isEmpty())
    }

    @Test
    fun fromAndroid12WithoutAWifiNetworkTheHandlerGetsNullWithoutWaiting() {
        grant(Manifest.permission.ACCESS_FINE_LOCATION)
        addNetwork(101, NetworkCapabilities.TRANSPORT_CELLULAR, ConnectivityManager.TYPE_MOBILE)
        fetch()
        assertTrue(registeredCallbacks().isEmpty())
        runMain()
        assertEquals(listOf<NEHotspotNetwork?>(null), results)
    }

    @Test
    fun fromAndroid12FineLocationIsRequired() {
        grant(Manifest.permission.ACCESS_COARSE_LOCATION)
        addWifiNetwork()
        fetch()
        assertTrue(registeredCallbacks().isEmpty())
        runMain()
        assertEquals(listOf<NEHotspotNetwork?>(null), results)
    }

    @Test
    fun fromAndroid12NoAnswerWithinASecondGivesNullAndStopsListening() {
        grant(Manifest.permission.ACCESS_FINE_LOCATION)
        addWifiNetwork()
        fetch()
        assertEquals(1, registeredCallbacks().size)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(900))
        assertEquals(0, completions)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        assertEquals(listOf<NEHotspotNetwork?>(null), results)
        assertTrue(registeredCallbacks().isEmpty())
    }

    @Test
    fun theAppContextEntryPointAnswersOnTheMainThread() {
        grant(Manifest.permission.ACCESS_FINE_LOCATION)
        val network = addWifiNetwork()
        var thread: Thread? = null
        NEHotspotNetwork.fetchCurrent {
            thread = Thread.currentThread()
            results.add(it)
        }
        registeredCallbacks().single().onCapabilitiesChanged(network, capabilitiesWith(wifiInfo("Home")))
        runMain()
        assertEquals("Home", results.single()?.ssid)
        assertEquals(Looper.getMainLooper().thread, thread)
    }
}
