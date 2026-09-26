package com.moblin.android.platform.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.test.core.app.ApplicationProvider
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetwork

@RunWith(RobolectricTestRunner::class)
class NWConnectionRequiredInterfaceSuite {
    private lateinit var server: ServerSocket
    private val connections = mutableListOf<NWConnection>()

    private val connectivityManager: ConnectivityManager
        get() = ApplicationProvider.getApplicationContext<Context>().getSystemService(ConnectivityManager::class.java)

    @Before
    fun setUp() {
        server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        server.soTimeout = 10_000
    }

    @After
    fun tearDown() {
        for (connection in connections) {
            connection.cancel()
        }
        server.close()
    }

    private fun callbackType(callback: ConnectivityManager.NetworkCallback): NWInterface.InterfaceType? {
        val field = callback.javaClass.declaredFields.firstOrNull { it.type == NWInterface.InterfaceType::class.java }
            ?: return null
        field.isAccessible = true
        return field.get(callback) as? NWInterface.InterfaceType
    }

    private fun awaitCallback(
        known: Set<ConnectivityManager.NetworkCallback>,
    ): ConnectivityManager.NetworkCallback {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            val added = shadowOf(connectivityManager).networkCallbacks.firstOrNull { it !in known }
            if (added != null) {
                return added
            }
            Thread.sleep(5)
        }
        throw AssertionError("No network request was made")
    }

    private fun start(type: NWInterface.InterfaceType): Pair<NWConnection, LinkedBlockingQueue<NWConnection.State>> {
        val parameters = NWParameters.tcp
        parameters.requiredInterfaceType = type
        val connection = NWConnection(
            endpoint = NWEndpoint.hostPort(host = NWEndpoint.Host("127.0.0.1"), port = NWEndpoint.Port(server.localPort)),
            parameters = parameters,
        )
        connections.add(connection)
        val states = LinkedBlockingQueue<NWConnection.State>()
        connection.stateUpdateHandler = { states.put(it) }
        connection.start(queue = Dispatchers.Default)
        return connection to states
    }

    private fun awaitState(states: LinkedBlockingQueue<NWConnection.State>, timeoutMs: Long): NWConnection.State {
        while (true) {
            val state = assertNotNull(states.poll(timeoutMs, TimeUnit.MILLISECONDS), "no state change")
            if (state != NWConnection.State.preparing) {
                return state
            }
        }
    }

    @Test
    fun bindsTheSocketToTheRequestedWifiNetwork() {
        val known = shadowOf(connectivityManager).networkCallbacks.toSet()
        val (_, states) = start(NWInterface.InterfaceType.wifi)
        val callback = awaitCallback(known)
        assertEquals(NWInterface.InterfaceType.wifi, callbackType(callback))
        val wifi = ShadowNetwork.newInstance(101)
        callback.onAvailable(wifi)
        server.accept().close()
        assertEquals(NWConnection.State.ready, awaitState(states, 5_000))
        assertEquals(1, shadowOf(wifi).boundSocketCount())
    }

    @Test
    fun waitsWithoutACellularNetworkAndConnectsWhenOneAppears() {
        val known = shadowOf(connectivityManager).networkCallbacks.toSet()
        val (_, states) = start(NWInterface.InterfaceType.cellular)
        val callback = awaitCallback(known)
        assertEquals(NWInterface.InterfaceType.cellular, callbackType(callback))
        assertIs<NWConnection.State.waiting>(awaitState(states, 6_000))
        val cellular: Network = ShadowNetwork.newInstance(102)
        callback.onAvailable(cellular)
        server.accept().close()
        assertEquals(NWConnection.State.ready, awaitState(states, 5_000))
        assertEquals(1, shadowOf(cellular).boundSocketCount())
    }

    @Test
    fun cancelReleasesTheNetworkRequest() {
        val known = shadowOf(connectivityManager).networkCallbacks.toSet()
        val (connection, _) = start(NWInterface.InterfaceType.cellular)
        val callback = awaitCallback(known)
        connection.cancel()
        val deadline = System.currentTimeMillis() + 5_000
        while (callback in shadowOf(connectivityManager).networkCallbacks && System.currentTimeMillis() < deadline) {
            Thread.sleep(5)
        }
        assertTrue(callback !in shadowOf(connectivityManager).networkCallbacks)
    }

    @Test
    fun noRequiredInterfaceTypeConnectsWithoutRequestingANetwork() {
        val known = shadowOf(connectivityManager).networkCallbacks.toSet()
        val (_, states) = start(NWInterface.InterfaceType.other)
        server.accept().close()
        assertEquals(NWConnection.State.ready, awaitState(states, 5_000))
        assertEquals(known, shadowOf(connectivityManager).networkCallbacks.toSet())
    }
}
