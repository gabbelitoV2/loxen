package com.moblin.android.various.model

import android.content.Context
import android.os.Build
import com.moblin.android.platform.log.Log
import com.moblin.android.common.various.noValue
import com.moblin.android.moblink.MoblinkRelayServer
import com.moblin.android.moblink.MoblinkRelayDelegate
import com.moblin.android.moblink.MoblinkRelayState
import com.moblin.android.moblink.MoblinkScanner
import com.moblin.android.moblink.MoblinkScannerDelegate
import com.moblin.android.moblink.MoblinkScannerStreamer
import com.moblin.android.moblink.MoblinkStreamer
import com.moblin.android.moblink.MoblinkStreamerDelegate
import com.moblin.android.moblink.MoblinkThermalState
import com.moblin.android.various.settings.SettingsStreamSrtConnectionPriority
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val applicationContext: Context get() = com.moblin.android.AppDelegate.context

class Moblink {
    var streamer: MoblinkStreamer? = null
    var relays: MutableList<MoblinkRelayServer> = mutableListOf()
    var scanner: MoblinkScanner? = null
    var relayState: MoblinkRelayState =
        MoblinkRelayState.values().firstOrNull { it.rawValue == "waiting-for-streamers" }
            ?: MoblinkRelayState.values().first()
    internal val _streamerOk = MutableStateFlow(true)
    val streamerOk: StateFlow<Boolean> = _streamerOk
    internal val _status = MutableStateFlow(noValue)
    val status: StateFlow<String> = _status
    internal val _scannerDiscoveredStreamers = MutableStateFlow<List<MoblinkScannerStreamer>>(emptyList())
    val scannerDiscoveredStreamers: StateFlow<List<MoblinkScannerStreamer>> = _scannerDiscoveredStreamers
}

fun Model.stopMoblinkStreamer() {
    moblink.streamer?.stop()
    moblink.streamer = null
}

fun Model.reloadMoblinkStreamer() {
    stopMoblinkStreamer()
    if (isMoblinkStreamerConfigured()) {
        val model = this
        moblink.streamer = MoblinkStreamer(
            context = applicationContext,
            port = database.moblink.streamer.port.value,
            password = database.moblink.password,
            name = Build.MODEL
        )
        moblink.streamer?.start(delegate = object : MoblinkStreamerDelegate {
            override fun moblinkStreamerTunnelAdded(host: String, port: Int, relayId: UUID, relayName: String) {
                model.moblinkStreamerTunnelAdded(host, port, relayId, relayName)
            }

            override fun moblinkStreamerTunnelRemoved(host: String, port: Int) {
                model.moblinkStreamerTunnelRemoved(host, port)
            }
        })
    }
}

fun Model.isMoblinkStreamerConfigured(): Boolean {
    val server = database.moblink.streamer
    return server.enabled.value && server.port.value > 0 && database.moblink.password.isNotEmpty()
}

fun Model.reloadMoblinkRelay() {
    stopMoblinkRelay()
    stopMoblinkScanner()
    if (isMoblinkRelayConfigured()) {
        reloadMoblinkScanner()
        if (database.moblink.relay.manual.value) {
            startMoblinkRelayManual()
        } else {
            startMoblinkRelayAutomatic()
        }
    }
}

private fun Model.startMoblinkRelayManual() {
    val streamerUrl = database.moblink.relay.url.value
    if (streamerUrl.isEmpty()) {
        return
    }
    addMoblinkRelay(streamerUrl)
}

private fun Model.startMoblinkRelayAutomatic() {
    for (streamer in moblink.scannerDiscoveredStreamers.value) {
        val url = streamer.urls.firstOrNull() ?: continue
        addMoblinkRelay(url)
    }
}

private fun Model.addMoblinkRelay(streamerUrl: String) {
    if (moblink.relays.any { it.streamerUrl == streamerUrl }) {
        return
    }
    if (isMoblinkRelayOnThisDevice(streamerUrl)) {
        Log.i("Model", "Not adding Moblink relay to ourselves: $streamerUrl")
        return
    }
    val model = this
    val relay = MoblinkRelayServer(
        name = database.moblink.relay.name.value,
        streamerUrl = streamerUrl,
        password = database.moblink.password,
        delegate = object : MoblinkRelayDelegate {
            override fun moblinkRelayNewState(state: MoblinkRelayState) {
                model.moblinkRelayNewState(state)
            }

            override fun moblinkRelayGetStatus(): Pair<Int?, MoblinkThermalState?> {
                return model.moblinkRelayGetStatus()
            }
        }
    )
    relay.start()
    moblink.relays.add(relay)
}

fun Model.isMoblinkRelayConfigured(): Boolean {
    val client = database.moblink.relay
    if (!client.enabled.value) {
        return false
    }
    return if (client.manual.value) {
        client.url.value.isNotEmpty() && database.moblink.password.isNotEmpty()
    } else {
        true
    }
}

fun Model.areMoblinkRelaysOk(): Boolean {
    return moblink.relayState.rawValue == "connected" ||
        moblink.relayState.rawValue == "waiting-for-streamers"
}

fun Model.moblinkIpStatusesUpdated() {
    val toRemove = mutableListOf<Int>()
    for ((index, relay) in moblink.relays.withIndex()) {
        if (isMoblinkRelayOnThisDevice(relay.streamerUrl)) {
            Log.i("Model", "Removing Moblink relay as it is connected to ourselves.")
            relay.stop()
            toRemove.add(index)
        }
    }
    for (index in toRemove.sortedDescending()) {
        moblink.relays.removeAt(index)
    }
}

private fun Model.isMoblinkRelayOnThisDevice(streamerUrl: String): Boolean {
    val host = runCatching { URI(streamerUrl).host }.getOrNull() ?: return false
    return statusOther.ipStatuses.value.any { it.ipType.formatAddress(it.ip) == host }
}

fun Model.stopMoblinkRelay() {
    for (relay in moblink.relays) {
        relay.stop()
    }
    moblink.relays.clear()
    stopMoblinkScanner()
}

fun Model.reloadMoblinkScanner() {
    stopMoblinkScanner()
    val model = this
    moblink.scanner = MoblinkScanner(
        context = applicationContext,
        delegate = object : MoblinkScannerDelegate {
            override fun moblinkScannerDiscoveredStreamers(streamers: List<MoblinkScannerStreamer>) {
                model.moblinkScannerDiscoveredStreamers(streamers)
            }
        }
    )
    moblink.scanner?.start()
}

fun Model.stopMoblinkScanner() {
    moblink.scanner?.stop()
    moblink.scanner = null
    moblink._scannerDiscoveredStreamers.value = emptyList()
}

fun Model.updateMoblinkStatus() {
    val status: String
    var serverOk = true
    if (isMoblinkRelayConfigured() && isMoblinkStreamerConfigured()) {
        val (serverStatus, ok) = moblinkStreamerStatus()
        status = "$serverStatus, ${moblink.relayState.rawValue}"
        serverOk = ok
    } else if (isMoblinkRelayConfigured()) {
        status = moblink.relayState.rawValue
    } else if (isMoblinkStreamerConfigured()) {
        val (serverStatus, ok) = moblinkStreamerStatus()
        status = serverStatus
        serverOk = ok
    } else {
        status = noValue
    }
    if (status != moblink.status.value) {
        moblink._status.value = status
    }
    if (serverOk != moblink.streamerOk.value) {
        moblink._streamerOk.value = serverOk
    }
}

private fun Model.moblinkStreamerStatus(): Pair<String, Boolean> {
    val streamer = moblink.streamer ?: return Pair("", true)
    val statuses = mutableListOf<String>()
    var ok = true
    for ((name, batteryPercentage, thermalState) in streamer.getStatuses()) {
        val (status, deviceOk) = formatDeviceStatus(
            name = name,
            batteryPercentage = batteryPercentage,
            thermalState = thermalState
        )
        if (!deviceOk) {
            ok = false
        }
        statuses.add(status)
    }
    return Pair(statuses.joinToString(", "), ok)
}

fun Model.moblinkStreamerTunnelAdded(host: String, port: Int, relayId: UUID, relayName: String) {
    val connectionPriorities = stream.value.srt.connectionPriorities
    val existing = connectionPriorities.priorities.firstOrNull { it.relayId == relayId }
    if (existing != null) {
        existing.name = relayName
    } else {
        val priority = SettingsStreamSrtConnectionPriority(name = relayName)
        priority.relayId = relayId
        connectionPriorities.priorities.add(priority)
    }
    media.addMoblink(host = host, port = port, id = relayId, name = relayName)
}

fun Model.moblinkStreamerTunnelRemoved(host: String, port: Int) {
    media.removeMoblink(host = host, port = port)
}

fun Model.moblinkRelayNewState(state: MoblinkRelayState) {
    moblink.relayState = state
}

fun Model.moblinkRelayGetStatus(): Pair<Int?, MoblinkThermalState?> {
    val thermalState: MoblinkThermalState? = when ("${statusOther.thermalState.value}") {
        "nominal", "fair" -> MoblinkThermalState.white
        "serious" -> MoblinkThermalState.yellow
        "critical" -> MoblinkThermalState.red
        else -> null
    }
    return Pair((100 * battery.level.value).toInt(), thermalState)
}

fun Model.moblinkScannerDiscoveredStreamers(streamers: List<MoblinkScannerStreamer>) {
    moblink._scannerDiscoveredStreamers.value = streamers
    if (!database.moblink.relay.manual.value) {
        startMoblinkRelayAutomatic()
    }
}
