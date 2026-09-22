package com.moblin.android.moblinwatch.shared

import androidx.compose.ui.graphics.Color
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

enum class WatchMessageToWatch(val rawValue: String) {
    chatMessage("chatMessage"),
    speedAndTotal("speedAndTotal"),
    recordingLength("recordingLength"),
    audioLevel("audioLevel"),
    preview("preview"),
    settings("settings"),
    isLive("isLive"),
    isRecording("isRecording"),
    isMuted("isMuted"),
    thermalState("thermalState"),
    zoom("zoom"),
    zoomPresets("zoomPresets"),
    zoomPreset("zoomPreset"),
    scenes("scenes"),
    scene("scene"),
    viewerCount("viewerCount"),
    padelScoreboard("padelScoreboard"),
    genericScoreboard("genericScoreboard"),
    removeScoreboard("removeScoreboard"),
    scoreboardPlayers("scoreboardPlayers"),
    ;

    companion object {
        fun fromRawValue(value: String): WatchMessageToWatch? =
            WatchMessageToWatch.entries.firstOrNull { it.rawValue == value }

        fun pack(type: WatchMessageToWatch, data: Any): Map<String, Any> = mapOf(
            "type" to type.rawValue,
            "data" to data,
        )

        fun unpack(message: Map<String, Any>): Pair<WatchMessageToWatch, Any>? {
            val rawType = message["type"] as? String ?: return null
            val type = fromRawValue(rawType) ?: return null
            val data = message["data"] ?: return null
            return Pair(type, data)
        }
    }
}

enum class WatchMessageFromWatch(val rawValue: String) {
    getImage("getImage"),
    setIsLive("setIsLive"),
    setIsRecording("setIsRecording"),
    setIsMuted("setIsMuted"),
    keepAlive("keepAlive"),
    skipCurrentChatTextToSpeechMessage("skipCurrentChatTextToSpeechMessage"),
    setZoom("setZoom"),
    setZoomPreset("setZoomPreset"),
    setScene("setScene"),
    updateWorkoutStats("updateWorkoutStats"),
    updatePadelScoreboard("updatePadelScoreboard"),
    updateGenericScoreboard("updateGenericScoreboard"),
    createStreamMarker("createStreamMarker"),
    instantReplay("instantReplay"),
    saveReplay("saveReplay"),
    ;

    companion object {
        fun fromRawValue(value: String): WatchMessageFromWatch? =
            WatchMessageFromWatch.entries.firstOrNull { it.rawValue == value }

        fun pack(type: WatchMessageFromWatch, data: Any): Map<String, Any> = mapOf(
            "type" to type.rawValue,
            "data" to data,
        )

        fun unpack(message: Map<String, Any>): Pair<WatchMessageFromWatch, Any>? {
            val rawType = message["type"] as? String ?: return null
            val type = fromRawValue(rawType) ?: return null
            val data = message["data"] ?: return null
            return Pair(type, data)
        }
    }
}

@Serializable
data class WatchProtocolChatSegment(
    var text: String? = null,
    var url: String? = null,
)

@Serializable
enum class WatchProtocolChatHighlightKind {
    reply,
    redemption,
    other,
    moderator,
}

@Serializable
data class WatchProtocolChatHighlight(
    val kind: WatchProtocolChatHighlightKind,
    val barColor: WatchProtocolColor,
    val image: String,
    val title: String? = null,
)

@Serializable
data class WatchProtocolChatMessage(
    var id: Int,
    var timestamp: String,
    var displayName: String,
    var userColor: WatchProtocolColor,
    var userBadges: List<String>,
    var segments: List<WatchProtocolChatSegment>,
    var highlight: WatchProtocolChatHighlight? = null,
)

@Serializable
data class WatchProtocolColor(
    var red: Int,
    var green: Int,
    var blue: Int,
)

@Serializable
data class WatchProtocolScene(
    @Serializable(with = WatchProtocolUuidSerializer::class) var id: UUID,
    var name: String,
)

@Serializable
data class WatchProtocolZoomPreset(
    @Serializable(with = WatchProtocolUuidSerializer::class) var id: UUID,
    var name: String,
)

@Serializable
enum class WatchProtocolWorkoutType {
    walking,
    running,
    cycling,
}

@Serializable
data class WatchProtocolWorkoutStats(
    var heartRate: Int? = null,
    var activeEnergyBurned: Int? = null,
    var distance: Int? = null,
    var stepCount: Int? = null,
    var power: Int? = null,
    var cyclingPower: Int? = null,
    var cyclingCadence: Int? = null,
) {
    fun update(statistics: Any) {
        Unit
    }
}

@Serializable
data class WatchProtocolPadelScoreboardScore(
    var home: Int,
    var away: Int,
)

@Serializable
data class WatchProtocolPadelScoreboard(
    @Serializable(with = WatchProtocolUuidSerializer::class) var id: UUID,
    @Serializable(with = WatchProtocolUuidListSerializer::class) var home: List<UUID>,
    @Serializable(with = WatchProtocolUuidListSerializer::class) var away: List<UUID>,
    var score: List<WatchProtocolPadelScoreboardScore>,
)

@Serializable
data class WatchProtocolGenericScoreboard(
    @Serializable(with = WatchProtocolUuidSerializer::class) var id: UUID,
    var homeTeam: String,
    var awayTeam: String,
    var homeScore: Int,
    var awayScore: Int,
    var clockMinutes: Int,
    var clockSeconds: Int,
    var clockMaximum: Int,
    var isClockStopped: Boolean,
    var title: String,
)

@Serializable
data class WatchProtocolPadelScoreboardAction(
    @Serializable(with = WatchProtocolUuidSerializer::class) val id: UUID,
    val action: WatchProtocolPadelScoreboardActionType,
)

@Serializable
data class WatchProtocolPadelScoreboardActionPlayers(
    @Serializable(with = WatchProtocolUuidListSerializer::class) var home: List<UUID>,
    @Serializable(with = WatchProtocolUuidListSerializer::class) var away: List<UUID>,
)

@Serializable
sealed class WatchProtocolPadelScoreboardActionType {
    @Serializable
    @SerialName("reset")
    object reset : WatchProtocolPadelScoreboardActionType()

    @Serializable
    @SerialName("undo")
    object undo : WatchProtocolPadelScoreboardActionType()

    @Serializable
    @SerialName("incrementHome")
    object incrementHome : WatchProtocolPadelScoreboardActionType()

    @Serializable
    @SerialName("incrementAway")
    object incrementAway : WatchProtocolPadelScoreboardActionType()

    @Serializable
    @SerialName("players")
    data class players(
        val players: WatchProtocolPadelScoreboardActionPlayers,
    ) : WatchProtocolPadelScoreboardActionType()
}

@Serializable
data class WatchProtocolGenericScoreboardAction(
    @Serializable(with = WatchProtocolUuidSerializer::class) val id: UUID,
    val action: WatchProtocolGenericScoreboardActionType,
)

@Serializable
sealed class WatchProtocolGenericScoreboardActionType {
    @Serializable
    @SerialName("reset")
    object reset : WatchProtocolGenericScoreboardActionType()

    @Serializable
    @SerialName("undo")
    object undo : WatchProtocolGenericScoreboardActionType()

    @Serializable
    @SerialName("incrementHome")
    object incrementHome : WatchProtocolGenericScoreboardActionType()

    @Serializable
    @SerialName("incrementAway")
    object incrementAway : WatchProtocolGenericScoreboardActionType()

    @Serializable
    @SerialName("setTitle")
    data class setTitle(
        val title: String,
    ) : WatchProtocolGenericScoreboardActionType()

    @Serializable
    @SerialName("setClock")
    data class setClock(
        val minutes: Int,
        val seconds: Int,
    ) : WatchProtocolGenericScoreboardActionType()

    @Serializable
    @SerialName("setClockState")
    data class setClockState(
        val stopped: Boolean,
    ) : WatchProtocolGenericScoreboardActionType()
}

@Serializable
data class WatchProtocolScoreboardPlayer(
    @Serializable(with = WatchProtocolUuidSerializer::class) var id: UUID,
    var name: String,
)

@Serializable
data class WatchProtocolInstantReplay(
    val duration: Int,
)

private fun WatchProtocolColor.colorScale(color: Int): Double = color.toDouble() / 255

fun WatchProtocolColor.color(): Color = Color(
    red = colorScale(red).toFloat(),
    green = colorScale(green).toFloat(),
    blue = colorScale(blue).toFloat(),
)

object WatchProtocolUuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.moblin.android.moblinwatch.shared.UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

object WatchProtocolUuidListSerializer : KSerializer<List<UUID>> {
    private val delegate = ListSerializer(WatchProtocolUuidSerializer)

    override val descriptor: SerialDescriptor
        get() = delegate.descriptor

    override fun serialize(encoder: Encoder, value: List<UUID>) {
        delegate.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): List<UUID> = delegate.deserialize(decoder)
}
