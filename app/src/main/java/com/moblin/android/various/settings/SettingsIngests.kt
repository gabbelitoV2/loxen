package com.moblin.android.various.settings

import com.moblin.android.common.various.ristCamera
import com.moblin.android.common.various.rtmpCamera
import com.moblin.android.common.various.rtspCamera
import com.moblin.android.common.various.srtClientCamera
import com.moblin.android.common.various.srtlaCamera
import com.moblin.android.common.various.whepCamera
import com.moblin.android.common.various.whipCamera
import com.moblin.android.localized
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.platform.swiftui.Published
import com.moblin.android.platform.swiftui.PublishedList
import com.moblin.android.various.network.DefaultTcpPorts
import com.moblin.android.various.network.DefaultUdpPorts
import com.moblin.android.various.utils.Named
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject

private const val defaultRtmpLatency: Int = 2000

@Serializable(with = SettingsRtmpServerStream.Serializer::class)
class SettingsRtmpServerStream : Named {
    companion object {
        val baseName: String = localized("My stream")

        fun decode(container: JsonObject): SettingsRtmpServerStream {
            val stream = SettingsRtmpServerStream()
            stream.id = container.decode("id", UUID.randomUUID())
            stream.name = container.decode("name", baseName)
            stream.streamKey = container.decode("streamKey", "")
            stream.latency = container.decode("latency", defaultRtmpLatency)
            return stream
        }
    }

    var id: UUID = UUID.randomUUID()
    override var name: String by Published(baseName)
    var streamKey: String by Published("")
    var latency: Int by Published(defaultRtmpLatency)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("streamKey", streamKey)
        encode("latency", latency)
    }

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

    object Serializer : KSerializer<SettingsRtmpServerStream> by JsonObjectSerializer(
        "SettingsRtmpServerStream",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsRtmpServer.Serializer::class)
class SettingsRtmpServer {
    var enabled: Boolean by Published(false)
    var port: Int by Published(DefaultTcpPorts.rtmpServer.toInt())
    var streams: MutableList<SettingsRtmpServerStream> by PublishedList()

    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("port", port)
        encode("streams", streams)
    }

    fun clone(): SettingsRtmpServer {
        val new = SettingsRtmpServer()
        new.enabled = enabled
        new.port = port
        for (stream in streams) {
            new.streams.add(stream.clone())
        }
        return new
    }

    companion object {
        fun decode(container: JsonObject): SettingsRtmpServer {
            val server = SettingsRtmpServer()
            server.enabled = container.decode("enabled", false)
            server.port = container.decode<UShort>("port", DefaultTcpPorts.rtmpServer.toUShort()).toInt()
            server.streams = container.decode(
                "streams",
                ListSerializer(SettingsRtmpServerStream.serializer()),
                emptyList(),
            ).toMutableList()
            return server
        }
    }

    object Serializer : KSerializer<SettingsRtmpServer> by JsonObjectSerializer(
        "SettingsRtmpServer",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsSrtlaServerStream.Serializer::class)
class SettingsSrtlaServerStream : Named {
    companion object {
        val baseName: String = localized("My stream")

        fun decode(container: JsonObject): SettingsSrtlaServerStream {
            val stream = SettingsSrtlaServerStream()
            stream.id = container.decode("id", UUID.randomUUID())
            stream.name = container.decode("name", baseName)
            stream.streamId = container.decode("streamId", "")
            return stream
        }
    }

    var id: UUID = UUID.randomUUID()
    override var name: String by Published(baseName)
    var streamId: String by Published("")

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("streamId", streamId)
    }

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

    object Serializer : KSerializer<SettingsSrtlaServerStream> by JsonObjectSerializer(
        "SettingsSrtlaServerStream",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsSrtlaServer.Serializer::class)
class SettingsSrtlaServer {
    var enabled: Boolean by Published(false)
    var srtPort: Int by Published(DefaultUdpPorts.srtServer.toInt())
    var srtlaPort: Int by Published(DefaultUdpPorts.srtlaServer.toInt())
    var streams: MutableList<SettingsSrtlaServerStream> by PublishedList()

    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("srtPort", srtPort)
        encode("srtlaPort", srtlaPort)
        encode("streams", streams)
    }

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

    companion object {
        fun decode(container: JsonObject): SettingsSrtlaServer {
            val server = SettingsSrtlaServer()
            server.enabled = container.decode("enabled", false)
            server.srtPort = container.decode<UShort>("srtPort", DefaultUdpPorts.srtServer.toUShort()).toInt()
            server.srtlaPort = container.decode<UShort>("srtlaPort", DefaultUdpPorts.srtlaServer.toUShort()).toInt()
            server.streams = container.decode(
                "streams",
                ListSerializer(SettingsSrtlaServerStream.serializer()),
                emptyList(),
            ).toMutableList()
            return server
        }
    }

    object Serializer : KSerializer<SettingsSrtlaServer> by JsonObjectSerializer(
        "SettingsSrtlaServer",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsSrtClientStream.Serializer::class)
class SettingsSrtClientStream : Named {
    companion object {
        val baseName: String = localized("My stream")

        fun decode(container: JsonObject): SettingsSrtClientStream {
            val stream = SettingsSrtClientStream()
            stream.id = container.decode("id", UUID.randomUUID())
            stream.name = container.decode("name", baseName)
            stream.url = container.decode("url", "")
            stream.enabled = container.decode("enabled", false)
            return stream
        }
    }

    var id: UUID = UUID.randomUUID()
    override var name: String by Published(baseName)
    var url: String by Published("")
    var enabled: Boolean by Published(false)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("url", url)
        encode("enabled", enabled)
    }

    fun camera(): String {
        return srtClientCamera(name)
    }

    object Serializer : KSerializer<SettingsSrtClientStream> by JsonObjectSerializer(
        "SettingsSrtClientStream",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsSrtClient.Serializer::class)
class SettingsSrtClient {
    var streams: MutableList<SettingsSrtClientStream> by PublishedList()

    fun encode(): JsonObject = encodeContainer {
        encode("streams", streams)
    }

    companion object {
        fun decode(container: JsonObject): SettingsSrtClient {
            val client = SettingsSrtClient()
            client.streams = container.decode(
                "streams",
                ListSerializer(SettingsSrtClientStream.serializer()),
                emptyList(),
            ).toMutableList()
            return client
        }
    }

    object Serializer : KSerializer<SettingsSrtClient> by JsonObjectSerializer(
        "SettingsSrtClient",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsRistServerStream.Serializer::class)
class SettingsRistServerStream : Named {
    companion object {
        val baseName: String = localized("My stream")

        fun decode(container: JsonObject): SettingsRistServerStream {
            val stream = SettingsRistServerStream()
            stream.id = container.decode("id", UUID.randomUUID())
            stream.name = container.decode("name", baseName)
            stream.virtualDestinationPort = container.decode<UShort>("virtualDestinationPort", 2u).toInt()
            stream.latency = container.decode("latency", 2000)
            return stream
        }
    }

    var id: UUID = UUID.randomUUID()
    override var name: String by Published(baseName)
    var virtualDestinationPort: Int by Published(2)
    var latency: Int by Published(2000)
    var connected: Boolean = false

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("virtualDestinationPort", virtualDestinationPort)
        encode("latency", latency)
    }

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

    object Serializer : KSerializer<SettingsRistServerStream> by JsonObjectSerializer(
        "SettingsRistServerStream",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsRistServer.Serializer::class)
class SettingsRistServer {
    var enabled: Boolean by Published(false)
    var port: Int by Published(DefaultUdpPorts.ristServer.toInt())
    var streams: MutableList<SettingsRistServerStream> by PublishedList()

    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("port", port)
        encode("streams", streams)
    }

    fun makeUniqueVirtualDestinationPort(): Int {
        var port: Int = 2
        while (streams.any { it.virtualDestinationPort == port }) {
            port += 2
        }
        return port
    }

    companion object {
        fun decode(container: JsonObject): SettingsRistServer {
            val server = SettingsRistServer()
            server.enabled = container.decode("enabled", false)
            server.port = container.decode<UShort>("port", DefaultUdpPorts.ristServer.toUShort()).toInt()
            server.streams = container.decode(
                "streams",
                ListSerializer(SettingsRistServerStream.serializer()),
                emptyList(),
            ).toMutableList()
            return server
        }
    }

    object Serializer : KSerializer<SettingsRistServer> by JsonObjectSerializer(
        "SettingsRistServer",
        { it.encode() },
        { decode(it) },
    )
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

@Serializable(with = SettingsRtspClientStream.Serializer::class)
class SettingsRtspClientStream : Named {
    companion object {
        val baseName: String = localized("My stream")

        fun decode(container: JsonObject): SettingsRtspClientStream {
            val stream = SettingsRtspClientStream()
            stream.id = container.decode("id", UUID.randomUUID())
            stream.name = container.decode("name", baseName)
            stream.url = container.decode("url", "")
            stream.enabled = container.decode("enabled", false)
            stream.latency = container.decode("latency", 2000)
            stream.transport = container.decode("transport", SettingsRtspTransport.rtpRtspTcp)
            return stream
        }
    }

    var id: UUID = UUID.randomUUID()
    override var name: String by Published(baseName)
    var url: String by Published("")
    var enabled: Boolean by Published(false)
    var latency: Int by Published(2000)
    var transport: SettingsRtspTransport by Published(SettingsRtspTransport.rtpRtspTcp)

    fun latencySeconds(): Double {
        return latency.toDouble() / 1000
    }

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("url", url)
        encode("enabled", enabled)
        encode("latency", latency)
        encode("transport", transport)
    }

    fun camera(): String {
        return rtspCamera(name)
    }

    object Serializer : KSerializer<SettingsRtspClientStream> by JsonObjectSerializer(
        "SettingsRtspClientStream",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsRtspClient.Serializer::class)
class SettingsRtspClient {
    var streams: MutableList<SettingsRtspClientStream> by PublishedList()

    fun encode(): JsonObject = encodeContainer {
        encode("streams", streams)
    }

    companion object {
        fun decode(container: JsonObject): SettingsRtspClient {
            val client = SettingsRtspClient()
            client.streams = container.decode(
                "streams",
                ListSerializer(SettingsRtspClientStream.serializer()),
                emptyList(),
            ).toMutableList()
            return client
        }
    }

    object Serializer : KSerializer<SettingsRtspClient> by JsonObjectSerializer(
        "SettingsRtspClient",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWhipServerStream.Serializer::class)
class SettingsWhipServerStream : Named {
    companion object {
        val baseName: String = localized("My stream")

        fun decode(container: JsonObject): SettingsWhipServerStream {
            val stream = SettingsWhipServerStream()
            stream.id = container.decode("id", UUID.randomUUID())
            stream.name = container.decode("name", baseName)
            stream.streamKey = container.decode("streamKey", "")
            stream.latency = container.decode("latency", 100)
            stream.syncTimestamps = container.decode("syncTimestamps", true)
            return stream
        }
    }

    var id: UUID = UUID.randomUUID()
    override var name: String by Published(baseName)
    var streamKey: String by Published("")
    var latency: Int by Published(100)
    var syncTimestamps: Boolean by Published(true)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("streamKey", streamKey)
        encode("latency", latency)
        encode("syncTimestamps", syncTimestamps)
    }

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

    object Serializer : KSerializer<SettingsWhipServerStream> by JsonObjectSerializer(
        "SettingsWhipServerStream",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWhipServer.Serializer::class)
class SettingsWhipServer {
    var enabled: Boolean by Published(false)
    var port: Int by Published(DefaultTcpPorts.whipServer.toInt())
    var streams: MutableList<SettingsWhipServerStream> by PublishedList()

    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("port", port)
        encode("streams", streams)
    }

    fun clone(): SettingsWhipServer {
        val new = SettingsWhipServer()
        new.enabled = enabled
        new.port = port
        for (stream in streams) {
            new.streams.add(stream.clone())
        }
        return new
    }

    companion object {
        fun decode(container: JsonObject): SettingsWhipServer {
            val server = SettingsWhipServer()
            server.enabled = container.decode("enabled", false)
            server.port = container.decode<UShort>("port", DefaultTcpPorts.whipServer.toUShort()).toInt()
            server.streams = container.decode(
                "streams",
                ListSerializer(SettingsWhipServerStream.serializer()),
                emptyList(),
            ).toMutableList()
            return server
        }
    }

    object Serializer : KSerializer<SettingsWhipServer> by JsonObjectSerializer(
        "SettingsWhipServer",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWhepClientStream.Serializer::class)
class SettingsWhepClientStream : Named {
    companion object {
        val baseName: String = localized("My stream")

        fun decode(container: JsonObject): SettingsWhepClientStream {
            val stream = SettingsWhepClientStream()
            stream.id = container.decode("id", UUID.randomUUID())
            stream.name = container.decode("name", baseName)
            stream.url = container.decode("url", "")
            stream.enabled = container.decode("enabled", false)
            stream.latency = container.decode("latency", 100)
            stream.syncTimestamps = container.decode("syncTimestamps", true)
            return stream
        }
    }

    var id: UUID = UUID.randomUUID()
    override var name: String by Published(baseName)
    var url: String by Published("")
    var enabled: Boolean by Published(false)
    var latency: Int by Published(100)
    var syncTimestamps: Boolean by Published(true)

    fun latencySeconds(): Double {
        return latency.toDouble() / 1000
    }

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("url", url)
        encode("enabled", enabled)
        encode("latency", latency)
        encode("syncTimestamps", syncTimestamps)
    }

    fun camera(): String {
        return whepCamera(name)
    }

    object Serializer : KSerializer<SettingsWhepClientStream> by JsonObjectSerializer(
        "SettingsWhepClientStream",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWhepClient.Serializer::class)
class SettingsWhepClient {
    var streams: MutableList<SettingsWhepClientStream> by PublishedList()

    fun encode(): JsonObject = encodeContainer {
        encode("streams", streams)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWhepClient {
            val client = SettingsWhepClient()
            client.streams = container.decode(
                "streams",
                ListSerializer(SettingsWhepClientStream.serializer()),
                emptyList(),
            ).toMutableList()
            return client
        }
    }

    object Serializer : KSerializer<SettingsWhepClient> by JsonObjectSerializer(
        "SettingsWhepClient",
        { it.encode() },
        { decode(it) },
    )
}
