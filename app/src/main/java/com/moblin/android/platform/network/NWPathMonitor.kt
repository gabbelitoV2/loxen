package com.moblin.android.platform.network

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.moblin.android.AppDelegate
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NWPath internal constructor(
    val status: Status,
    val availableInterfaces: List<NWInterface>,
) {
    enum class Status {
        satisfied,
        unsatisfied,
        requiresConnection,
    }

    val isExpensive: Boolean
        get() = availableInterfaces.firstOrNull()?.type == NWInterface.InterfaceType.cellular

    val isConstrained: Boolean
        get() = false

    fun usesInterfaceType(type: NWInterface.InterfaceType): Boolean {
        return availableInterfaces.firstOrNull()?.type == type
    }

    fun uniqueAvailableInterfaces(): List<NWInterface> {
        val interfaces = ArrayList<NWInterface>()
        for (availableInterface in availableInterfaces) {
            if (!interfaces.contains(availableInterface)) {
                interfaces.add(availableInterface)
            }
        }
        return interfaces
    }

    override fun equals(other: Any?): Boolean {
        return other is NWPath && other.status == status && other.availableInterfaces == availableInterfaces
    }

    override fun hashCode(): Int {
        return status.hashCode() * 31 + availableInterfaces.hashCode()
    }

    override fun toString(): String {
        if (availableInterfaces.isEmpty()) {
            return status.toString()
        }
        return availableInterfaces.joinToString(" ") { "${it.type}(${it.name})" }
    }
}

class NWPathMonitor(private val requiredInterfaceType: NWInterface.InterfaceType? = null) {
    private class Link(val network: Network, val type: NWInterface.InterfaceType, var name: String?)

    private class Snapshot(val path: NWPath, val key: List<Any>)

    private val lock = Any()
    private val id = nextId.incrementAndGet()
    private val links = LinkedHashMap<Network, Link>()
    private val callbacks = ArrayList<ConnectivityManager.NetworkCallback>()
    private var connectivityManager: ConnectivityManager? = null
    private var scope: CoroutineScope? = null
    private var generation = 0
    private var latestKey: List<Any>? = null

    @Volatile
    var pathUpdateHandler: ((NWPath) -> Unit)? = null

    val currentPath: NWPath
        get() = synchronized(lock) {
            makeSnapshotLocked().path
        }

    fun start(queue: CoroutineDispatcher) {
        startInternal(CoroutineScope(queue + SupervisorJob()))
    }

    fun start(queue: CoroutineScope) {
        val dispatcher = queue.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher ?: Dispatchers.Default
        startInternal(CoroutineScope(dispatcher + SupervisorJob()))
    }

    fun cancel() {
        val manager: ConnectivityManager?
        val registered: List<ConnectivityManager.NetworkCallback>
        synchronized(lock) {
            if (scope == null) {
                return
            }
            generation += 1
            scope = null
            links.clear()
            latestKey = null
            manager = connectivityManager
            connectivityManager = null
            registered = callbacks.toList()
            callbacks.clear()
        }
        for (callback in registered) {
            unregister(manager, callback)
        }
        Log.i(TAG, "path#$id: Cancelled")
    }

    private fun startInternal(queueScope: CoroutineScope) {
        val manager = try {
            AppDelegate.context.getSystemService(ConnectivityManager::class.java)
        } catch (error: Throwable) {
            null
        }
        val startGeneration = synchronized(lock) {
            if (scope != null) {
                return
            }
            generation += 1
            scope = queueScope
            connectivityManager = manager
            generation
        }
        if (manager == null) {
            Log.i(TAG, "path#$id: No connectivity manager")
            postUpdate(startGeneration)
            return
        }
        for ((transport, type) in transports()) {
            if (requiredInterfaceType != null && requiredInterfaceType != type) {
                continue
            }
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(transport)
                .build()
            val callback = makeCallback(startGeneration, type)
            if (!register(manager, request, callback)) {
                continue
            }
            val keep = synchronized(lock) {
                if (generation == startGeneration) {
                    callbacks.add(callback)
                    true
                } else {
                    false
                }
            }
            if (!keep) {
                unregister(manager, callback)
            }
        }
        postUpdate(startGeneration)
    }

    private fun register(
        manager: ConnectivityManager,
        request: NetworkRequest,
        callback: ConnectivityManager.NetworkCallback,
    ): Boolean {
        try {
            manager.requestNetwork(request, callback, callbackHandler)
            return true
        } catch (error: Throwable) {
            Log.i(TAG, "path#$id: Request network failed, only listening: $error")
        }
        return try {
            manager.registerNetworkCallback(request, callback, callbackHandler)
            true
        } catch (error: Throwable) {
            Log.i(TAG, "path#$id: Register network callback failed: $error")
            false
        }
    }

    private fun unregister(manager: ConnectivityManager?, callback: ConnectivityManager.NetworkCallback) {
        try {
            manager?.unregisterNetworkCallback(callback)
        } catch (error: Throwable) {
            Log.i(TAG, "path#$id: Unregister network callback failed: $error")
        }
    }

    private fun makeCallback(
        callbackGeneration: Int,
        type: NWInterface.InterfaceType,
    ): ConnectivityManager.NetworkCallback {
        return object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val name = linkName(network)
                synchronized(lock) {
                    if (generation != callbackGeneration) {
                        return
                    }
                    val link = links[network]
                    if (link == null) {
                        links[network] = Link(network, type, name)
                    } else if (name != null) {
                        link.name = name
                    }
                }
                postUpdate(callbackGeneration)
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                val name = linkProperties.interfaceName
                synchronized(lock) {
                    if (generation != callbackGeneration) {
                        return
                    }
                    val link = links[network] ?: return
                    if (link.name == name) {
                        return
                    }
                    link.name = name
                }
                postUpdate(callbackGeneration)
            }

            override fun onLost(network: Network) {
                synchronized(lock) {
                    if (generation != callbackGeneration) {
                        return
                    }
                    if (links.remove(network) == null) {
                        return
                    }
                }
                postUpdate(callbackGeneration)
            }
        }
    }

    private fun linkName(network: Network): String? {
        val manager = synchronized(lock) {
            connectivityManager
        } ?: return null
        return try {
            manager.getLinkProperties(network)?.interfaceName
        } catch (error: Throwable) {
            null
        }
    }

    private fun postUpdate(expectedGeneration: Int) {
        val queueScope = synchronized(lock) {
            if (generation != expectedGeneration) {
                null
            } else {
                scope
            }
        } ?: return
        queueScope.launch {
            val path = synchronized(lock) {
                if (generation != expectedGeneration) {
                    return@launch
                }
                val snapshot = makeSnapshotLocked()
                if (snapshot.key == latestKey) {
                    return@launch
                }
                latestKey = snapshot.key
                snapshot.path
            }
            Log.i(TAG, "path#$id: $path")
            val handler = pathUpdateHandler ?: return@launch
            try {
                handler(path)
            } catch (error: Throwable) {
                Log.e(TAG, "path#$id: Path update handler failed", error)
            }
        }
    }

    private fun makeSnapshotLocked(): Snapshot {
        val defaultNetwork = try {
            connectivityManager?.activeNetwork
        } catch (error: Throwable) {
            null
        }
        val named = links.values.filter { it.name != null }
        val ordered = named.sortedWith(
            compareBy<Link> { if (it.network == defaultNetwork) 0 else 1 }.thenBy { typeOrder(it.type) },
        )
        val interfaces = ordered.map { NWInterface(name = it.name ?: "", type = it.type, network = it.network) }
        val status = if (interfaces.isEmpty()) NWPath.Status.unsatisfied else NWPath.Status.satisfied
        val key = ordered.map { listOf(it.network, it.type, it.name ?: "") }
        return Snapshot(NWPath(status = status, availableInterfaces = interfaces), key)
    }

    private fun typeOrder(type: NWInterface.InterfaceType): Int {
        return when (type) {
            NWInterface.InterfaceType.wifi -> 0
            NWInterface.InterfaceType.wiredEthernet -> 1
            NWInterface.InterfaceType.cellular -> 2
            else -> 3
        }
    }

    private fun transports(): List<Pair<Int, NWInterface.InterfaceType>> {
        val transports = mutableListOf(
            NetworkCapabilities.TRANSPORT_CELLULAR to NWInterface.InterfaceType.cellular,
            NetworkCapabilities.TRANSPORT_WIFI to NWInterface.InterfaceType.wifi,
            NetworkCapabilities.TRANSPORT_ETHERNET to NWInterface.InterfaceType.wiredEthernet,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            transports.add(NetworkCapabilities.TRANSPORT_USB to NWInterface.InterfaceType.wiredEthernet)
        }
        return transports
    }

    private companion object {
        private const val TAG = "MoblinNet"
        private val nextId = AtomicInteger(0)
        private val callbackHandler: Handler by lazy {
            val thread = HandlerThread("nw-path-monitor")
            thread.start()
            Handler(thread.looper)
        }
    }
}
