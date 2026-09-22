package com.moblin.android.various.settings

import com.moblin.android.common.various.ristCamera
import com.moblin.android.common.various.rtmpCamera
import com.moblin.android.common.various.rtspCamera
import com.moblin.android.common.various.srtClientCamera
import com.moblin.android.common.various.srtlaCamera
import com.moblin.android.common.various.whepCamera
import com.moblin.android.common.various.whipCamera
import com.moblin.android.localized
import com.moblin.android.various.network.DefaultTcpPorts
import com.moblin.android.various.network.DefaultUdpPorts
import com.moblin.android.various.utils.Named
import java.util.UUID
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

private const val defaultRtmpLatency: Int = 2000

@Serializable
class SettingsRtmpServerStream : Named {
    companion object {
        val baseName: String = localized("My stream")
    }

    @Contextual
    @SerialName("id")
    var id: UUID = UUID.randomUUID()

    @SerialName("name")
    override var name: String = baseName

    @SerialName("streamKey")
    var streamKey: String = ""

    @SerialName("latency")
    var latency: Int = defaultRtmpLatency

    fun camera(): String {
        return rtmpCamera(name)
    }

    fun latencySeconds(): Double {
        return latency.toDouble() / 1000
    }

    fun clone(): SettingsRtmpServerStream {
        val new = SettingsRtmpServerStream()
        new.id = id
        new.name = name
        new.streamKey = streamKey
        new.latency = latency
        return new
    }
}

@Serializable
class SettingsRtmpServer {
    @SerialName("enabled")
    var enabled: Boolean = false

    @SerialName("port")
    var port: Int = DefaultTcpPorts.rtmpServer.toInt()

    @SerialName("streams")
    var streams: MutableList<SettingsRtmpServerStream> = mutableListOf()

    fun clone(): SettingsRtmpServer {
        val new = SettingsRtmpServer()
        new.enabled = enabled
        new.port = port
        for (stream in streams) {
            new.streams.add(stream.clone())
        }
        return new
    }
}

@Serializable
class SettingsSrtlaServerStream : Named {
    companion object {
        val baseName: String = localized("My stream")
    }

    @Contextual
    @SerialName("id")
    var id: UUID = UUID.randomUUID()

    @SerialName("name")
    override var name: String = baseName

    @SerialName("streamId")
    var streamId: String = ""

    fun camera(): String {
        return srtlaCamera(name)
    }

    fun clone(): SettingsSrtlaServerStream {
        val new = SettingsSrtlaServerStream()
        new.id = id
        new.name = name
        new.streamId = streamId
        return new
    }
}

@Serializable
class SettingsSrtlaServer {
    @SerialName("enabled")
    var enabled: Boolean = false

    @SerialName("srtPort")
    var srtPort: Int = DefaultUdpPorts.srtServer.toInt()

    @SerialName("srtlaPort")
    var srtlaPort: Int = DefaultUdpPorts.srtlaServer.toInt()

    @SerialName("streams")
    var streams: MutableList<SettingsSrtlaServerStream> = mutableListOf()

    fun clone(): SettingsSrtlaServer {
        val new = SettingsSrtlaServer()
        new.enabled = enabled
        new.srtPort = srtPort
        new.srtlaPort = srtlaPort
        for (stream in streams) {
            new.streams.add(stream.clone())
        }
        return new
    }

    fun srtlaSrtPort(): Int {
        return srtlaPort + 1
    }
}

@Serializable
class SettingsSrtClientStream : Named {
    companion object {
        val baseName: String = localized("My stream")
    }

    @Contextual
    @SerialName("id")
    var id: UUID = UUID.randomUUID()

    @SerialName("name")
    override var name: String = baseName

    @SerialName("url")
    var url: String = ""

    @SerialName("enabled")
    var enabled: Boolean = false

    fun camera(): String {
        return srtClientCamera(name)
    }
}

@Serializable
class SettingsSrtClient {
    @SerialName("streams")
    var streams: MutableList<SettingsSrtClientStream> = mutableListOf()
}

@Serializable
class SettingsRistServerStream : Named {
    companion object {
        val baseName: String = localized("My stream")
    }

    @Contextual
    @SerialName("id")
    var id: UUID = UUID.randomUUID()

    @SerialName("name")
    override var name: String = baseName

    @SerialName("virtualDestinationPort")
    var virtualDestinationPort: Int = 1

    @SerialName("latency")
    var latency: Int = 2000

    @Transient
    var connected: Boolean = false

    fun latencySeconds(): Double {
        return latency.toDouble() / 1000
    }

    fun clone(): SettingsRistServerStream {
        val new = SettingsRistServerStream()
        new.id = id
        new.name = name
        new.virtualDestinationPort = virtualDestinationPort
        new.latency = latency
        return new
    }

    fun camera(): String {
        return ristCamera(name)
    }
}

@Serializable
class SettingsRistServer {
    @SerialName("enabled")
    var enabled: Boolean = false

    @SerialName("port")
    var port: Int = DefaultUdpPorts.ristServer.toInt()

    @SerialName("streams")
    var streams: MutableList<SettingsRistServerStream> = mutableListOf()

    fun makeUniqueVirtualDestinationPort(): Int {
        var port: Int = 1
        while (streams.any { it.virtualDestinationPort == port }) {
            port += 1
        }
        return port
    }
}

@Serializable
enum class SettingsRtspTransport(val rawValue: String) {
    rtpRtspTcp("rtpRtspTcp"),
    rtpUdp("rtpUdp");

    override fun toString(): String {
        return when (this) {
            rtpRtspTcp -> "RTP/RTSP/TCP"
            rtpUdp -> "RTP/UDP"
        }
    }

    companion object {
        fun fromRawValue(value: String): SettingsRtspTransport? {
            return SettingsRtspTransport.entries.firstOrNull { it.rawValue == value }
        }
    }
}

@Serializable
class SettingsRtspClientStream : Named {
    companion object {
        val baseName: String = localized("My stream")
    }

    @Contextual
    @SerialName("id")
    var id: UUID = UUID.randomUUID()

    @SerialName("name")
    override var name: String = baseName

    @SerialName("url")
    var url: String = ""

    @SerialName("enabled")
    var enabled: Boolean = false

    @SerialName("latency")
    var latency: Int = 2000

    @SerialName("transport")
    var transport: SettingsRtspTransport = SettingsRtspTransport.rtpRtspTcp

    fun latencySeconds(): Double {
        return latency.toDouble() / 1000
    }

    fun camera(): String {
        return rtspCamera(name)
    }
}

@Serializable
class SettingsRtspClient {
    @SerialName("streams")
    var streams: MutableList<SettingsRtspClientStream> = mutableListOf()
}

@Serializable
class SettingsWhipServerStream : Named {
    companion object {
        val baseName: String = localized("My stream")
    }

    @Contextual
    @SerialName("id")
    var id: UUID = UUID.randomUUID()

    @SerialName("name")
    override var name: String = baseName

    @SerialName("streamKey")
    var streamKey: String = ""

    @SerialName("latency")
    var latency: Int = 100

    @SerialName("syncTimestamps")
    var syncTimestamps: Boolean = true

    fun camera(): String {
        return whipCamera(name)
    }

    fun latencySeconds(): Double {
        return latency.toDouble() / 1000
    }

    fun clone(): SettingsWhipServerStream {
        val new = SettingsWhipServerStream()
        new.id = id
        new.name = name
        new.streamKey = streamKey
        new.latency = latency
        new.syncTimestamps = syncTimestamps
        return new
    }
}

@Serializable
class SettingsWhipServer {
    @SerialName("enabled")
    var enabled: Boolean = false

    @SerialName("port")
    var port: Int = DefaultTcpPorts.whipServer.toInt()

    @SerialName("streams")
    var streams: MutableList<SettingsWhipServerStream> = mutableListOf()

    fun clone(): SettingsWhipServer {
        val new = SettingsWhipServer()
        new.enabled = enabled
        new.port = port
        for (stream in streams) {
            new.streams.add(stream.clone())
        }
        return new
    }
}

@Serializable
class SettingsWhepClientStream : Named {
    companion object {
        val baseName: String = localized("My stream")
    }

    @Contextual
    @SerialName("id")
    var id: UUID = UUID.randomUUID()

    @SerialName("name")
    override var name: String = baseName

    @SerialName("url")
    var url: String = ""

    @SerialName("enabled")
    var enabled: Boolean = false

    @SerialName("latency")
    var latency: Int = 100

    @SerialName("syncTimestamps")
    var syncTimestamps: Boolean = true

    fun latencySeconds(): Double {
        return latency.toDouble() / 1000
    }

    fun camera(): String {
        return whepCamera(name)
    }
}

@Serializable
class SettingsWhepClient {
    @SerialName("streams")
    var streams: MutableList<SettingsWhepClientStream> = mutableListOf()
}
