package com.moblin.android.remotecontrol

import android.location.Location
import android.os.PowerManager
import android.util.Base64
import com.moblin.android.common.various.RgbColor
import com.moblin.android.integrations.workoutdevice.WorkoutDeviceRunningMetrics
import com.moblin.android.localized
import com.moblin.android.platform.codable.UUIDSerializer
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.various.ChatHighlightKind
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.Variables
import com.moblin.android.various.managers.GForce
import com.moblin.android.various.settings.SettingsAlignment
import com.moblin.android.various.settings.SettingsFontDesign
import com.moblin.android.various.settings.SettingsFontWeight
import com.moblin.android.various.settings.SettingsGimbalMotion
import com.moblin.android.various.settings.SettingsHorizontalAlignment
import com.moblin.android.various.settings.SettingsHttpHeader
import com.moblin.android.various.settings.SettingsQuickButtonType
import com.moblin.android.various.settings.SettingsReaction
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetBrowser
import com.moblin.android.various.settings.SettingsWidgetBrowserMode
import com.moblin.android.various.settings.SettingsWidgetMap
import com.moblin.android.various.settings.SettingsWidgetScene
import com.moblin.android.various.settings.SettingsWidgetText
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.various.utils.clockAsMinutesAndSeconds
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.double
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

const val remoteControlApiVersion = "0.1"

private val remoteControlJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    explicitNulls = false
}

val remoteControlStartStatsFilterAllEnabled = RemoteControlStartStatsFilter(
    weather = true,
    geography = true,
    gForce = true,
)

class RemoteControlStartStatusFilter(
    var topRight: Boolean = true,
)

@Serializable
data class RemoteControlStartStatsFilter(
    var weather: Boolean? = null,
    var geography: Boolean? = null,
    var gForce: Boolean? = null,
)

sealed class RemoteControlRequest {
    abstract fun toJsonElement(): JsonObject

    fun toJson(): String = remoteControlJson.encodeToString(JsonElement.serializer(), toJsonElement())

    data object GetStatus : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("getStatus") {}
        }
    }

    data object GetSettings : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("getSettings") {}
        }
    }

    data class SetRecord(val on: Boolean) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setRecord") { put("on", on) }
        }
    }

    data class SetLive(val on: Boolean) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setLive") { put("on", on) }
        }
    }

    data class SetPreviewStream(val on: Boolean) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setPreviewStream") { put("on", on) }
        }
    }

    data class SetZoom(val x: Float) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setZoom") { put("x", x) }
        }
    }

    data class SetZoomPreset(val id: UUID) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setZoomPreset") { put("id", id.toString()) }
        }
    }

    data class SetMute(val on: Boolean) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setMute") { put("on", on) }
        }
    }

    data class SetStealthMode(val on: Boolean) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setStealthMode") { put("on", on) }
        }
    }

    data class SetTorch(val on: Boolean) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setTorch") { put("on", on) }
        }
    }

    data class SetDebugLogging(val on: Boolean) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setDebugLogging") { put("on", on) }
        }
    }

    data class SetStream(val id: UUID) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setStream") { put("id", id.toString()) }
        }
    }

    data class SetScene(val id: UUID) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setScene") { put("id", id.toString()) }
        }
    }

    data class SetAutoSceneSwitcher(val id: UUID?) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setAutoSceneSwitcher") {
                if (id == null) {
                    put("id", JsonNull)
                } else {
                    put("id", id.toString())
                }
            }
        }
    }

    data class SetBitratePreset(val id: UUID) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setBitratePreset") { put("id", id.toString()) }
        }
    }

    data class SetMic(val id: String) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setMic") { put("id", id) }
        }
    }

    data class SetTalkbackMic(val id: String) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setTalkbackMic") { put("id", id) }
        }
    }

    data class SetSrtConnectionPriority(val id: UUID, val priority: Int, val enabled: Boolean) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setSrtConnectionPriority") {
                put("id", id.toString())
                put("priority", priority)
                put("enabled", enabled)
            }
        }
    }

    data class SetSrtConnectionPrioritiesEnabled(val enabled: Boolean) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setSrtConnectionPrioritiesEnabled") { put("enabled", enabled) }
        }
    }

    data object ReloadBrowserWidgets : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("reloadBrowserWidgets") {}
        }
    }

    data class TwitchEventSubNotification(val message: String) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("twitchEventSubNotification") { put("message", message) }
        }
    }

    data object StartPreview : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("startPreview") {}
        }
    }

    data object StopPreview : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("stopPreview") {}
        }
    }

    data class ChatMessages(val history: Boolean, val messages: List<RemoteControlChatMessage>) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("chatMessages") {
                put("history", history)
                put("messages", remoteControlJson.encodeToJsonElement(messages))
            }
        }
    }

    data class SetRemoteSceneSettings(val data: RemoteControlRemoteSceneSettings) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setRemoteSceneSettings") {
                put("data", remoteControlJson.encodeToJsonElement(data))
            }
        }
    }

    data class SetRemoteSceneData(val data: RemoteControlRemoteSceneData) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setRemoteSceneData") {
                put("data", remoteControlJson.encodeToJsonElement(data))
            }
        }
    }

    data object InstantReplay : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("instantReplay") {}
        }
    }

    data object SaveReplay : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("saveReplay") {}
        }
    }

    data class StartStatus(val interval: Int, val filter: RemoteControlStartStatusFilter) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("startStatus") {
                put("interval", interval)
                put("filter", remoteControlJson.encodeToJsonElement(filter))
            }
        }
    }

    data object StopStatus : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("stopStatus") {}
        }
    }

    data object GetScoreboardSports : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("getScoreboardSports") {}
        }
    }

    data class SetScoreboardSport(val sportId: String) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setScoreboardSport") { put("sportId", sportId) }
        }
    }

    data class UpdateScoreboard(val config: RemoteControlScoreboardMatchConfig) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("updateScoreboard") {
                put("config", remoteControlJson.encodeToJsonElement(config))
            }
        }
    }

    data object ToggleScoreboardClock : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("toggleScoreboardClock") {}
        }
    }

    data class SetScoreboardDuration(val minutes: Int) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setScoreboardDuration") { put("minutes", minutes) }
        }
    }

    data class SetScoreboardClock(val time: String) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setScoreboardClock") { put("time", time) }
        }
    }

    data class Whip(
        val url: String,
        val method: String,
        val headers: List<SettingsHttpHeader>,
        val body: ByteArray,
    ) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("whip") {
                put("url", url)
                put("method", method)
                put("headers", remoteControlJson.encodeToJsonElement(headers))
                put("body", Base64.encodeToString(body, Base64.NO_WRAP))
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Whip) return false
            return url == other.url &&
                method == other.method &&
                headers == other.headers &&
                body.contentEquals(other.body)
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + method.hashCode()
            result = 31 * result + headers.hashCode()
            result = 31 * result + body.contentHashCode()
            return result
        }
    }

    data class SetFilter(val filter: RemoteControlFilter, val on: Boolean) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setFilter") {
                putJsonObject("filter") { putJsonObject(filter.wireName) {} }
                put("on", on)
            }
        }
    }

    data class TriggerReaction(val reaction: RemoteControlReaction) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("triggerReaction") {
                putJsonObject("reaction") { putJsonObject(reaction.wireName) {} }
            }
        }
    }

    data class MoveToGimbalPreset(val id: UUID) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("moveToGimbalPreset") { put("id", id.toString()) }
        }
    }

    data class SetGimbalTracking(val on: Boolean) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setGimbalTracking") { put("on", on) }
        }
    }

    data class SetGimbalMovement(val x: Float, val y: Float) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("setGimbalMovement") {
                put("x", x)
                put("y", y)
            }
        }
    }

    data class AnimateGimbal(val motion: SettingsGimbalMotion) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("animateGimbal") {
                putJsonObject("motion") { putJsonObject(motion.rawValue) {} }
            }
        }
    }

    data object SaveGimbalPreset : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("saveGimbalPreset") {}
        }
    }

    data object GetGolfScoreboard : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("getGolfScoreboard") {}
        }
    }

    data class UpdateGolfScoreboard(val data: RemoteControlGolfScoreboard) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("updateGolfScoreboard") {
                put("data", remoteControlJson.encodeToJsonElement(data))
            }
        }
    }

    data class ImportSettings(val data: ByteArray) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("importSettings") {
                put("data", Base64.encodeToString(data, Base64.NO_WRAP))
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ImportSettings) return false
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = data.contentHashCode()
    }

    data class StartStats(val filter: RemoteControlStartStatsFilter?) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("startStats") {
                if (filter == null) {
                    put("filter", JsonNull)
                } else {
                    put("filter", remoteControlJson.encodeToJsonElement(filter))
                }
            }
        }
    }

    data object StopStats : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("stopStats") {}
        }
    }

    data class StartMacro(val id: UUID) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("startMacro") { put("id", id.toString()) }
        }
    }

    data class StopMacro(val id: UUID) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("stopMacro") { put("id", id.toString()) }
        }
    }

    data class SendMessage(val text: String) : RemoteControlRequest() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("sendMessage") { put("text", text) }
        }
    }

    companion object {
        fun fromJson(data: String): RemoteControlRequest =
            fromJsonElement(remoteControlJson.parseToJsonElement(data))

        fun fromJsonElement(element: JsonElement): RemoteControlRequest {
            val entry = element.jsonObject.entries.first()
            val params = entry.value.jsonObject
            return when (entry.key) {
                "getStatus" -> GetStatus
                "getSettings" -> GetSettings
                "setRecord" -> SetRecord(on = params.getValue("on").jsonPrimitive.boolean)
                "setLive" -> SetLive(on = params.getValue("on").jsonPrimitive.boolean)
                "setPreviewStream" -> SetPreviewStream(on = params.getValue("on").jsonPrimitive.boolean)
                "setZoom" -> SetZoom(x = params.getValue("x").jsonPrimitive.float)
                "setZoomPreset" -> SetZoomPreset(id = UUID.fromString(params.getValue("id").jsonPrimitive.content))
                "setMute" -> SetMute(on = params.getValue("on").jsonPrimitive.boolean)
                "setStealthMode" -> SetStealthMode(on = params.getValue("on").jsonPrimitive.boolean)
                "setTorch" -> SetTorch(on = params.getValue("on").jsonPrimitive.boolean)
                "setDebugLogging" -> SetDebugLogging(on = params.getValue("on").jsonPrimitive.boolean)
                "setStream" -> SetStream(id = UUID.fromString(params.getValue("id").jsonPrimitive.content))
                "setScene" -> SetScene(id = UUID.fromString(params.getValue("id").jsonPrimitive.content))
                "setAutoSceneSwitcher" -> SetAutoSceneSwitcher(id = params["id"]?.let {
                    if (it is JsonNull) null else UUID.fromString(it.jsonPrimitive.content)
                })
                "setBitratePreset" -> SetBitratePreset(id = UUID.fromString(params.getValue("id").jsonPrimitive.content))
                "setMic" -> SetMic(id = params.getValue("id").jsonPrimitive.content)
                "setTalkbackMic" -> SetTalkbackMic(id = params.getValue("id").jsonPrimitive.content)
                "setSrtConnectionPriority" -> SetSrtConnectionPriority(
                    id = UUID.fromString(params.getValue("id").jsonPrimitive.content),
                    priority = params.getValue("priority").jsonPrimitive.int,
                    enabled = params.getValue("enabled").jsonPrimitive.boolean,
                )
                "setSrtConnectionPrioritiesEnabled" -> SetSrtConnectionPrioritiesEnabled(
                    enabled = params.getValue("enabled").jsonPrimitive.boolean,
                )
                "reloadBrowserWidgets" -> ReloadBrowserWidgets
                "twitchEventSubNotification" -> TwitchEventSubNotification(
                    message = params.getValue("message").jsonPrimitive.content,
                )
                "startPreview" -> StartPreview
                "stopPreview" -> StopPreview
                "chatMessages" -> ChatMessages(
                    history = params.getValue("history").jsonPrimitive.boolean,
                    messages = remoteControlJson.decodeFromJsonElement<List<RemoteControlChatMessage>>(
                        params.getValue("messages"),
                    ),
                )
                "setRemoteSceneSettings" -> SetRemoteSceneSettings(
                    data = remoteControlJson.decodeFromJsonElement<RemoteControlRemoteSceneSettings>(
                        params.getValue("data"),
                    ),
                )
                "setRemoteSceneData" -> SetRemoteSceneData(
                    data = remoteControlJson.decodeFromJsonElement<RemoteControlRemoteSceneData>(
                        params.getValue("data"),
                    ),
                )
                "instantReplay" -> InstantReplay
                "saveReplay" -> SaveReplay
                "startStatus" -> StartStatus(
                    interval = params.getValue("interval").jsonPrimitive.int,
                    filter = remoteControlJson.decodeFromJsonElement<RemoteControlStartStatusFilter>(
                        params.getValue("filter"),
                    ),
                )
                "stopStatus" -> StopStatus
                "getScoreboardSports" -> GetScoreboardSports
                "setScoreboardSport" -> SetScoreboardSport(
                    sportId = params.getValue("sportId").jsonPrimitive.content,
                )
                "updateScoreboard" -> UpdateScoreboard(
                    config = remoteControlJson.decodeFromJsonElement<RemoteControlScoreboardMatchConfig>(
                        params.getValue("config"),
                    ),
                )
                "toggleScoreboardClock" -> ToggleScoreboardClock
                "setScoreboardDuration" -> SetScoreboardDuration(
                    minutes = params.getValue("minutes").jsonPrimitive.int,
                )
                "setScoreboardClock" -> SetScoreboardClock(
                    time = params.getValue("time").jsonPrimitive.content,
                )
                "whip" -> Whip(
                    url = params.getValue("url").jsonPrimitive.content,
                    method = params.getValue("method").jsonPrimitive.content,
                    headers = remoteControlJson.decodeFromJsonElement<List<SettingsHttpHeader>>(
                        params.getValue("headers"),
                    ),
                    body = Base64.decode(params.getValue("body").jsonPrimitive.content, Base64.NO_WRAP),
                )
                "setFilter" -> SetFilter(
                    filter = decodeRemoteControlCase(params.getValue("filter"), RemoteControlFilter::fromName),
                    on = params.getValue("on").jsonPrimitive.boolean,
                )
                "triggerReaction" -> TriggerReaction(
                    reaction = decodeRemoteControlCase(params.getValue("reaction"), RemoteControlReaction::fromName),
                )
                "moveToGimbalPreset" -> MoveToGimbalPreset(
                    id = UUID.fromString(params.getValue("id").jsonPrimitive.content),
                )
                "setGimbalTracking" -> SetGimbalTracking(on = params.getValue("on").jsonPrimitive.boolean)
                "setGimbalMovement" -> SetGimbalMovement(
                    x = params.getValue("x").jsonPrimitive.float,
                    y = params.getValue("y").jsonPrimitive.float,
                )
                "animateGimbal" -> AnimateGimbal(
                    motion = decodeRemoteControlCase(params.getValue("motion")) { name ->
                        SettingsGimbalMotion.entries.firstOrNull { it.rawValue == name }
                    },
                )
                "saveGimbalPreset" -> SaveGimbalPreset
                "getGolfScoreboard" -> GetGolfScoreboard
                "updateGolfScoreboard" -> UpdateGolfScoreboard(
                    data = remoteControlJson.decodeFromJsonElement<RemoteControlGolfScoreboard>(
                        params.getValue("data"),
                    ),
                )
                "importSettings" -> ImportSettings(
                    data = Base64.decode(params.getValue("data").jsonPrimitive.content, Base64.NO_WRAP),
                )
                "startStats" -> StartStats(
                    filter = params["filter"]?.let {
                        if (it is JsonNull) null
                        else remoteControlJson.decodeFromJsonElement<RemoteControlStartStatsFilter>(it)
                    },
                )
                "stopStats" -> StopStats
                "startMacro" -> StartMacro(id = UUID.fromString(params.getValue("id").jsonPrimitive.content))
                "stopMacro" -> StopMacro(id = UUID.fromString(params.getValue("id").jsonPrimitive.content))
                "sendMessage" -> SendMessage(text = params.getValue("text").jsonPrimitive.content)
                else -> throw SerializationException("Unknown request ${entry.key}")
            }
        }
    }
}

sealed class RemoteControlResponse {
    abstract fun toJsonElement(): JsonObject

    fun toJson(): String = remoteControlJson.encodeToString(JsonElement.serializer(), toJsonElement())

    data class GetStatus(
        val general: RemoteControlStatusGeneral?,
        val topLeft: RemoteControlStatusTopLeft,
        val topRight: RemoteControlStatusTopRight,
    ) : RemoteControlResponse() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("getStatus") {
                if (general == null) {
                    put("general", JsonNull)
                } else {
                    put("general", remoteControlJson.encodeToJsonElement(general))
                }
                put("topLeft", remoteControlJson.encodeToJsonElement(topLeft))
                put("topRight", remoteControlJson.encodeToJsonElement(topRight))
            }
        }
    }

    data class GetSettings(val data: RemoteControlSettings) : RemoteControlResponse() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("getSettings") { put("data", remoteControlJson.encodeToJsonElement(data)) }
        }
    }

    data class GetScoreboardSports(val names: List<String>) : RemoteControlResponse() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("getScoreboardSports") { put("names", remoteControlJson.encodeToJsonElement(names)) }
        }
    }

    data class Whip(val status: Int, val headers: List<SettingsHttpHeader>, val body: ByteArray) : RemoteControlResponse() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("whip") {
                put("status", status)
                put("headers", remoteControlJson.encodeToJsonElement(headers))
                put("body", Base64.encodeToString(body, Base64.NO_WRAP))
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Whip) return false
            return status == other.status && headers == other.headers && body.contentEquals(other.body)
        }

        override fun hashCode(): Int {
            var result = status
            result = 31 * result + headers.hashCode()
            result = 31 * result + body.contentHashCode()
            return result
        }
    }

    data class GetGolfScoreboard(val data: RemoteControlGolfScoreboard) : RemoteControlResponse() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("getGolfScoreboard") { put("data", remoteControlJson.encodeToJsonElement(data)) }
        }
    }

    companion object {
        fun fromJson(data: String): RemoteControlResponse =
            fromJsonElement(remoteControlJson.parseToJsonElement(data))

        fun fromJsonElement(element: JsonElement): RemoteControlResponse {
            val entry = element.jsonObject.entries.first()
            val params = entry.value.jsonObject
            return when (entry.key) {
                "getStatus" -> GetStatus(
                    general = params["general"]?.let {
                        if (it is JsonNull) null
                        else remoteControlJson.decodeFromJsonElement<RemoteControlStatusGeneral>(it)
                    },
                    topLeft = remoteControlJson.decodeFromJsonElement(params.getValue("topLeft")),
                    topRight = remoteControlJson.decodeFromJsonElement(params.getValue("topRight")),
                )
                "getSettings" -> GetSettings(
                    data = remoteControlJson.decodeFromJsonElement(params.getValue("data")),
                )
                "getScoreboardSports" -> GetScoreboardSports(
                    names = remoteControlJson.decodeFromJsonElement(params.getValue("names")),
                )
                "whip" -> Whip(
                    status = params.getValue("status").jsonPrimitive.int,
                    headers = remoteControlJson.decodeFromJsonElement(params.getValue("headers")),
                    body = Base64.decode(params.getValue("body").jsonPrimitive.content, Base64.NO_WRAP),
                )
                "getGolfScoreboard" -> GetGolfScoreboard(
                    data = remoteControlJson.decodeFromJsonElement(params.getValue("data")),
                )
                else -> throw SerializationException("Unknown response ${entry.key}")
            }
        }
    }
}

sealed class RemoteControlEvent {
    abstract fun toJsonElement(): JsonObject

    fun toJson(): String = remoteControlJson.encodeToString(JsonElement.serializer(), toJsonElement())

    data class State(val data: RemoteControlAssistantStreamerState) : RemoteControlEvent() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("state") { put("data", remoteControlJson.encodeToJsonElement(data)) }
        }
    }

    data class Log(val entry: String) : RemoteControlEvent() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("log") { put("entry", entry) }
        }
    }

    data class Status(
        val general: RemoteControlStatusGeneral?,
        val topLeft: RemoteControlStatusTopLeft?,
        val topRight: RemoteControlStatusTopRight?,
    ) : RemoteControlEvent() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("status") {
                if (general == null) {
                    put("general", JsonNull)
                } else {
                    put("general", remoteControlJson.encodeToJsonElement(general))
                }
                if (topLeft == null) {
                    put("topLeft", JsonNull)
                } else {
                    put("topLeft", remoteControlJson.encodeToJsonElement(topLeft))
                }
                if (topRight == null) {
                    put("topRight", JsonNull)
                } else {
                    put("topRight", remoteControlJson.encodeToJsonElement(topRight))
                }
            }
        }
    }

    data class Scoreboard(val config: RemoteControlScoreboardMatchConfig) : RemoteControlEvent() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("scoreboard") { put("config", remoteControlJson.encodeToJsonElement(config)) }
        }
    }

    data class GolfScoreboard(val data: RemoteControlGolfScoreboard) : RemoteControlEvent() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("golfScoreboard") { put("data", remoteControlJson.encodeToJsonElement(data)) }
        }
    }

    data class Stats(val data: RemoteControlStats) : RemoteControlEvent() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("stats") { put("data", remoteControlJson.encodeToJsonElement(data)) }
        }
    }

    companion object {
        fun fromJson(data: String): RemoteControlEvent =
            fromJsonElement(remoteControlJson.parseToJsonElement(data))

        fun fromJsonElement(element: JsonElement): RemoteControlEvent {
            val entry = element.jsonObject.entries.first()
            val params = entry.value.jsonObject
            return when (entry.key) {
                "state" -> State(data = remoteControlJson.decodeFromJsonElement(params.getValue("data")))
                "log" -> Log(entry = params.getValue("entry").jsonPrimitive.content)
                "status" -> Status(
                    general = params["general"]?.let {
                        if (it is JsonNull) null
                        else remoteControlJson.decodeFromJsonElement<RemoteControlStatusGeneral>(it)
                    },
                    topLeft = params["topLeft"]?.let {
                        if (it is JsonNull) null
                        else remoteControlJson.decodeFromJsonElement<RemoteControlStatusTopLeft>(it)
                    },
                    topRight = params["topRight"]?.let {
                        if (it is JsonNull) null
                        else remoteControlJson.decodeFromJsonElement<RemoteControlStatusTopRight>(it)
                    },
                )
                "scoreboard" -> Scoreboard(
                    config = remoteControlJson.decodeFromJsonElement(params.getValue("config")),
                )
                "golfScoreboard" -> GolfScoreboard(
                    data = remoteControlJson.decodeFromJsonElement(params.getValue("data")),
                )
                "stats" -> Stats(data = remoteControlJson.decodeFromJsonElement(params.getValue("data")))
                else -> throw SerializationException("Unknown event ${entry.key}")
            }
        }
    }
}

object RemoteControlDateSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("RemoteControlDate", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeDouble(value.epochSecond + value.nano / 1_000_000_000.0 - 978_307_200.0)
    }

    override fun deserialize(decoder: Decoder): Instant {
        val seconds = decoder.decodeDouble() + 978_307_200.0
        val whole = kotlin.math.floor(seconds).toLong()
        val nanos = ((seconds - whole) * 1_000_000_000.0).toLong()
        return Instant.ofEpochSecond(whole, nanos)
    }
}

@Serializable
private data class RemoteControlMeasurementConverter(
    val coefficient: Double,
    val constant: Double,
)

@Serializable
private data class RemoteControlMeasurementUnit(
    val symbol: String,
    val converter: RemoteControlMeasurementConverter,
)

@Serializable
private data class RemoteControlMeasurement(
    val value: Double,
    val unit: RemoteControlMeasurementUnit,
)

private abstract class RemoteControlMeasurementSerializer(
    private val symbol: String,
    private val constant: Double,
) : KSerializer<Double> {
    override val descriptor: SerialDescriptor = RemoteControlMeasurement.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Double) {
        val unit = RemoteControlMeasurementUnit(
            symbol = symbol,
            converter = RemoteControlMeasurementConverter(coefficient = 1.0, constant = constant),
        )
        encoder.encodeSerializableValue(
            RemoteControlMeasurement.serializer(),
            RemoteControlMeasurement(value = value, unit = unit),
        )
    }

    override fun deserialize(decoder: Decoder): Double {
        val measurement = decoder.decodeSerializableValue(RemoteControlMeasurement.serializer())
        val converter = measurement.unit.converter
        return measurement.value * converter.coefficient + converter.constant - constant
    }
}

private object RemoteControlTemperatureSerializer : RemoteControlMeasurementSerializer("°C", 273.15)

private object RemoteControlSpeedSerializer : RemoteControlMeasurementSerializer("m/s", 0.0)

private fun <T> decodeRemoteControlCase(element: JsonElement, fromName: (String) -> T?): T =
    element.jsonObject.keys.mapNotNull(fromName).singleOrNull()
        ?: throw SerializationException("Expected exactly one known case in $element")

private object RemoteControlFiltersSerializer : KSerializer<Map<RemoteControlFilter, Boolean>> {
    override val descriptor: SerialDescriptor = JsonArray.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Map<RemoteControlFilter, Boolean>) {
        val array = buildJsonArray {
            for ((filter, on) in value) {
                addJsonObject { putJsonObject(filter.wireName) {} }
                add(on)
            }
        }
        encoder.encodeSerializableValue(JsonArray.serializer(), array)
    }

    override fun deserialize(decoder: Decoder): Map<RemoteControlFilter, Boolean> {
        val array = decoder.decodeSerializableValue(JsonArray.serializer())
        if (array.size % 2 != 0) {
            throw SerializationException("Expected collection of key-value pairs")
        }
        return array.chunked(2).associate { (filter, on) ->
            decodeRemoteControlCase(filter, RemoteControlFilter::fromName) to on.jsonPrimitive.boolean
        }
    }
}

@Serializable
data class RemoteControlStats(
    var date: @Serializable(with = RemoteControlDateSerializer::class) Instant,
    var timeZone: String,
    var speed: Double,
    var averageSpeed: Double,
    var altitude: Double,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var distance: Double,
    var splitDistance: Double,
    var slopePercent: Double,
    var altitudeAscent: Double,
    var altitudeDescent: Double,
    var splitAltitudeAscent: Double,
    var splitAltitudeDescent: Double,
    var temperature: Double? = null,
    var feelsLikeTemperature: Double? = null,
    var windSpeed: Double? = null,
    var windGust: Double? = null,
    var country: String? = null,
    var countryFlag: String? = null,
    var state: String? = null,
    var area: String? = null,
    var city: String? = null,
    var neighborhood: String? = null,
    var heartRates: Map<String, Int?>,
    var activeEnergyBurned: Int? = null,
    var workoutDistance: Int? = null,
    var power: Int? = null,
    var stepCount: Int? = null,
    var cyclingPower: Int,
    var cyclingCadence: Int,
    var cyclingSpeed: Double,
    var gForce: GForce? = null,
)

@Serializable
data class RemoteControlChatHighlight(
    val kind: ChatHighlightKind,
    val barColor: RgbColor,
    val image: String,
    val titleSegments: List<ChatPostSegment>? = null,
)

@Serializable
data class RemoteControlChatMessage(
    val id: Int,
    val platform: Platform,
    val messageId: String? = null,
    val displayName: String? = null,
    val user: String? = null,
    val userId: String? = null,
    val userColor: RgbColor? = null,
    val userBadges: List<String>,
    val segments: List<ChatPostSegment>,
    val timestamp: String,
    val isAction: Boolean,
    val isModerator: Boolean,
    val isSubscriber: Boolean,
    val isOwner: Boolean,
    val bits: String? = null,
    var highlight: RemoteControlChatHighlight? = null,
)

@Serializable
enum class RemoteControlReaction(val wireName: String) {
    @SerialName("fireworks")
    Fireworks("fireworks"),

    @SerialName("balloons")
    Balloons("balloons"),

    @SerialName("hearts")
    Hearts("hearts"),

    @SerialName("confetti")
    Confetti("confetti"),

    @SerialName("lasers")
    Lasers("lasers"),

    @SerialName("rain")
    Rain("rain"),

    @SerialName("glasses")
    Glasses("glasses"),

    @SerialName("sparkle")
    Sparkle("sparkle"),
    ;

    fun toSettings(): SettingsReaction =
        SettingsReaction.fromRawValue(wireName) ?: TODO("Unknown reaction")

    companion object {
        val allCases: List<RemoteControlReaction> = entries

        fun fromName(name: String): RemoteControlReaction? = entries.firstOrNull { it.wireName == name }
    }
}

@Serializable
enum class RemoteControlFilter(val wireName: String) {
    @SerialName("pixellate")
    Pixellate("pixellate"),

    @SerialName("movie")
    Movie("movie"),

    @SerialName("grayScale")
    GrayScale("grayScale"),

    @SerialName("sepia")
    Sepia("sepia"),

    @SerialName("triple")
    Triple("triple"),

    @SerialName("twin")
    Twin("twin"),

    @SerialName("fourThree")
    FourThree("fourThree"),

    @SerialName("crt")
    Crt("crt"),

    @SerialName("pinch")
    Pinch("pinch"),

    @SerialName("whirlpool")
    Whirlpool("whirlpool"),

    @SerialName("poll")
    Poll("poll"),

    @SerialName("blurFaces")
    BlurFaces("blurFaces"),

    @SerialName("privacy")
    Privacy("privacy"),

    @SerialName("beauty")
    Beauty("beauty"),

    @SerialName("moblinInMouth")
    MoblinInMouth("moblinInMouth"),

    @SerialName("cameraMan")
    CameraMan("cameraMan"),
    ;

    fun toSettings(): SettingsQuickButtonType = when (this) {
        Pixellate -> SettingsQuickButtonType.pixellate
        Movie -> SettingsQuickButtonType.movie
        GrayScale -> SettingsQuickButtonType.grayScale
        Sepia -> SettingsQuickButtonType.sepia
        Triple -> SettingsQuickButtonType.triple
        Twin -> SettingsQuickButtonType.twin
        FourThree -> SettingsQuickButtonType.fourThree
        Crt -> SettingsQuickButtonType.crt
        Pinch -> SettingsQuickButtonType.pinch
        Whirlpool -> SettingsQuickButtonType.whirlpool
        Poll -> SettingsQuickButtonType.poll
        BlurFaces -> SettingsQuickButtonType.blurFaces
        Privacy -> SettingsQuickButtonType.privacy
        Beauty -> SettingsQuickButtonType.beauty
        MoblinInMouth -> SettingsQuickButtonType.moblinInMouth
        CameraMan -> SettingsQuickButtonType.cameraMan
    }

    override fun toString(): String = when (this) {
        Pixellate -> localized("Pixellate")
        Movie -> localized("Movie")
        GrayScale -> localized("Gray scale")
        Sepia -> localized("Sepia")
        Triple -> localized("Triple")
        Twin -> localized("Twin")
        FourThree -> localized("4:3")
        Crt -> localized("CRT")
        Pinch -> localized("Pinch")
        Whirlpool -> localized("Whirlpool")
        Poll -> localized("Poll")
        BlurFaces -> localized("Blur faces")
        Privacy -> localized("Blur background")
        Beauty -> localized("Beauty")
        MoblinInMouth -> localized("Moblin in mouth")
        CameraMan -> localized("Camera man")
    }

    companion object {
        val allCases: List<RemoteControlFilter> = entries

        fun fromName(name: String): RemoteControlFilter? = entries.firstOrNull { it.wireName == name }

        fun fromType(type: SettingsQuickButtonType): RemoteControlFilter? = when (type) {
            SettingsQuickButtonType.pixellate -> Pixellate
            SettingsQuickButtonType.movie -> Movie
            SettingsQuickButtonType.grayScale -> GrayScale
            SettingsQuickButtonType.sepia -> Sepia
            SettingsQuickButtonType.triple -> Triple
            SettingsQuickButtonType.twin -> Twin
            SettingsQuickButtonType.fourThree -> FourThree
            SettingsQuickButtonType.crt -> Crt
            SettingsQuickButtonType.pinch -> Pinch
            SettingsQuickButtonType.whirlpool -> Whirlpool
            SettingsQuickButtonType.poll -> Poll
            SettingsQuickButtonType.blurFaces -> BlurFaces
            SettingsQuickButtonType.privacy -> Privacy
            SettingsQuickButtonType.beauty -> Beauty
            SettingsQuickButtonType.moblinInMouth -> MoblinInMouth
            SettingsQuickButtonType.cameraMan -> CameraMan
            else -> null
        }
    }
}

@Serializable
data class RemoteControlRemoteSceneSettings(
    var scenes: List<RemoteControlRemoteSceneSettingsScene>,
    var widgets: List<RemoteControlRemoteSceneSettingsWidget>,
    @Serializable(with = UUIDSerializer::class)
    var selectedSceneId: UUID? = null,
) {
    companion object {
        fun fromSettings(scenes: List<SettingsScene>, widgets: List<SettingsWidget>, selectedSceneId: UUID?) =
            RemoteControlRemoteSceneSettings(
                scenes = scenes.map { RemoteControlRemoteSceneSettingsScene(it) },
                widgets = widgets.mapNotNull { RemoteControlRemoteSceneSettingsWidget.fromWidget(it) },
                selectedSceneId = selectedSceneId,
            )
    }

    fun toSettings(): Triple<List<SettingsScene>, List<SettingsWidget>, UUID?> = Triple(
        scenes.map { it.toSettings() },
        widgets.map { it.toSettings() },
        selectedSceneId,
    )
}

@Serializable
data class RemoteControlRemoteSceneSettingsScene(
    @Serializable(with = UUIDSerializer::class)
    var id: UUID,
    var widgets: List<RemoteControlRemoteSceneSettingsSceneWidget>,
) {
    constructor(scene: SettingsScene) : this(
        id = scene.id,
        widgets = scene.widgets.map { RemoteControlRemoteSceneSettingsSceneWidget(it) },
    )

    fun toSettings(): SettingsScene {
        val scene = SettingsScene("")
        scene.id = id
        scene.widgets = widgets.map { it.toSettings() }.toMutableList()
        return scene
    }
}

@Serializable
data class RemoteControlRemoteSceneSettingsSceneWidgetLayout(
    val x: Double,
    val y: Double,
    val size: Double,
    val alignment: SettingsAlignment,
)

@Serializable
data class RemoteControlRemoteSceneSettingsSceneWidget(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val layout: RemoteControlRemoteSceneSettingsSceneWidgetLayout,
) {
    constructor(widget: SettingsSceneWidget) : this(
        id = widget.widgetId,
        layout = RemoteControlRemoteSceneSettingsSceneWidgetLayout(
            x = widget.layout.x,
            y = widget.layout.y,
            size = widget.layout.size,
            alignment = widget.layout.alignment,
        ),
    )

    fun toSettings(): SettingsSceneWidget {
        val widget = SettingsSceneWidget(id)
        widget.layout.x = layout.x
        widget.layout.y = layout.y
        widget.layout.size = layout.size
        widget.layout.alignment = layout.alignment
        return widget
    }
}

@Serializable
data class RemoteControlRemoteSceneSettingsWidget(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val enabled: Boolean,
    val type: RemoteControlRemoteSceneSettingsWidgetType,
) {
    fun toSettings(): SettingsWidget {
        val widget = SettingsWidget("")
        widget.id = id
        widget.enabled = enabled
        when (val data = type) {
            is RemoteControlRemoteSceneSettingsWidgetType.Browser -> {
                widget.type = SettingsWidgetType.browser
                widget.browser = data.data.toSettings()
            }
            is RemoteControlRemoteSceneSettingsWidgetType.Text -> {
                widget.type = SettingsWidgetType.text
                widget.text = data.data.toSettings()
            }
            is RemoteControlRemoteSceneSettingsWidgetType.Map -> {
                widget.type = SettingsWidgetType.map
                widget.map = data.data.toSettings()
            }
            is RemoteControlRemoteSceneSettingsWidgetType.Scene -> {
                widget.type = SettingsWidgetType.scene
                widget.scene = data.data.toSettings()
            }
        }
        return widget
    }

    companion object {
        fun fromWidget(widget: SettingsWidget): RemoteControlRemoteSceneSettingsWidget? {
            val type = when (widget.type) {
                SettingsWidgetType.browser -> RemoteControlRemoteSceneSettingsWidgetType.Browser(
                    RemoteControlRemoteSceneSettingsWidgetTypeBrowser(widget.browser),
                )
                SettingsWidgetType.text -> RemoteControlRemoteSceneSettingsWidgetType.Text(
                    RemoteControlRemoteSceneSettingsWidgetTypeText(widget.text),
                )
                SettingsWidgetType.map -> RemoteControlRemoteSceneSettingsWidgetType.Map(
                    RemoteControlRemoteSceneSettingsWidgetTypeMap(widget.map),
                )
                SettingsWidgetType.scene -> RemoteControlRemoteSceneSettingsWidgetType.Scene(
                    RemoteControlRemoteSceneSettingsWidgetTypeScene(widget.scene),
                )
                else -> return null
            }
            return RemoteControlRemoteSceneSettingsWidget(widget.id, widget.enabled, type)
        }
    }
}

@Serializable(with = RemoteControlRemoteSceneSettingsWidgetTypeSerializer::class)
sealed class RemoteControlRemoteSceneSettingsWidgetType {
    abstract fun toJsonElement(): JsonObject

    data class Browser(val data: RemoteControlRemoteSceneSettingsWidgetTypeBrowser) : RemoteControlRemoteSceneSettingsWidgetType() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("browser") { put("data", remoteControlJson.encodeToJsonElement(data)) }
        }
    }

    data class Text(val data: RemoteControlRemoteSceneSettingsWidgetTypeText) : RemoteControlRemoteSceneSettingsWidgetType() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("text") { put("data", remoteControlJson.encodeToJsonElement(data)) }
        }
    }

    data class Map(val data: RemoteControlRemoteSceneSettingsWidgetTypeMap) : RemoteControlRemoteSceneSettingsWidgetType() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("map") { put("data", remoteControlJson.encodeToJsonElement(data)) }
        }
    }

    data class Scene(val data: RemoteControlRemoteSceneSettingsWidgetTypeScene) : RemoteControlRemoteSceneSettingsWidgetType() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("scene") { put("data", remoteControlJson.encodeToJsonElement(data)) }
        }
    }

    companion object {
        fun fromJsonElement(element: JsonElement): RemoteControlRemoteSceneSettingsWidgetType? {
            val entry = element.jsonObject.entries.first()
            val params = entry.value.jsonObject
            return when (entry.key) {
                "browser" -> Browser(remoteControlJson.decodeFromJsonElement(params.getValue("data")))
                "text" -> Text(remoteControlJson.decodeFromJsonElement(params.getValue("data")))
                "map" -> Map(remoteControlJson.decodeFromJsonElement(params.getValue("data")))
                "scene" -> Scene(remoteControlJson.decodeFromJsonElement(params.getValue("data")))
                else -> null
            }
        }
    }
}

object RemoteControlRemoteSceneSettingsWidgetTypeSerializer : KSerializer<RemoteControlRemoteSceneSettingsWidgetType> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun serialize(encoder: Encoder, value: RemoteControlRemoteSceneSettingsWidgetType) {
        encoder.encodeSerializableValue(JsonElement.serializer(), value.toJsonElement())
    }

    override fun deserialize(decoder: Decoder): RemoteControlRemoteSceneSettingsWidgetType =
        RemoteControlRemoteSceneSettingsWidgetType.fromJsonElement(
            decoder.decodeSerializableValue(JsonElement.serializer()),
        ) ?: throw SerializationException("Unknown widget type")
}

@Serializable
data class RemoteControlRemoteSceneSettingsWidgetTypeBrowser(
    val url: String,
    val width: Int,
    val height: Int,
    val mode: SettingsWidgetBrowserMode,
    val fps: Float,
    val styleSheet: String,
) {
    constructor(browser: SettingsWidgetBrowser) : this(
        url = browser.url,
        width = browser.width,
        height = browser.height,
        mode = browser.mode,
        fps = browser.baseFps,
        styleSheet = browser.styleSheet,
    )

    fun toSettings(): SettingsWidgetBrowser {
        val browser = SettingsWidgetBrowser()
        browser.url = url
        browser.width = width
        browser.height = height
        browser.mode = mode
        browser.baseFps = fps
        browser.styleSheet = styleSheet
        return browser
    }
}

@Serializable
data class RemoteControlRemoteSceneSettingsWidgetTypeText(
    val formatString: String,
    val backgroundColor: RgbColor,
    val clearBackgroundColor: Boolean,
    val foregroundColor: RgbColor,
    val clearForegroundColor: Boolean,
    val fontSize: Int,
    val fontFamily: String? = null,
    val fontStyle: String? = null,
    val fontDesign: SettingsFontDesign,
    val fontWeight: SettingsFontWeight,
    val fontMonospacedDigits: Boolean,
    val horizontalAlignment: RemoteControlRemoteSceneSettingsHorizontalAlignment,
    val delay: Double,
) {
    constructor(text: SettingsWidgetText) : this(
        formatString = text.formatString,
        backgroundColor = text.backgroundColor,
        clearBackgroundColor = text.clearBackgroundColor,
        foregroundColor = text.foregroundColor,
        clearForegroundColor = text.clearForegroundColor,
        fontSize = text.fontSize,
        fontFamily = text.fontFamily,
        fontStyle = text.fontStyle,
        fontDesign = text.fontDesign,
        fontWeight = text.fontWeight,
        fontMonospacedDigits = text.fontMonospacedDigits,
        horizontalAlignment = RemoteControlRemoteSceneSettingsHorizontalAlignment.fromAlignment(text.horizontalAlignment),
        delay = text.delay,
    )

    fun toSettings(): SettingsWidgetText {
        val text = SettingsWidgetText()
        text.formatString = formatString
        text.backgroundColor = backgroundColor
        text.clearBackgroundColor = clearBackgroundColor
        text.foregroundColor = foregroundColor
        text.clearForegroundColor = clearForegroundColor
        text.fontSize = fontSize
        text.fontFamily = fontFamily ?: ""
        text.fontStyle = fontStyle ?: ""
        text.fontDesign = fontDesign
        text.fontWeight = fontWeight
        text.fontMonospacedDigits = fontMonospacedDigits
        text.horizontalAlignment = horizontalAlignment.toSettings()
        text.delay = delay
        return text
    }
}

@Serializable
enum class RemoteControlRemoteSceneSettingsHorizontalAlignment {
    @SerialName("leading")
    Leading,

    @SerialName("trailing")
    Trailing,

    @SerialName("center")
    Center,
    ;

    fun toSettings(): SettingsHorizontalAlignment = when (this) {
        Leading -> SettingsHorizontalAlignment.leading
        Trailing -> SettingsHorizontalAlignment.trailing
        Center -> SettingsHorizontalAlignment.center
    }

    companion object {
        fun fromAlignment(alignment: SettingsHorizontalAlignment): RemoteControlRemoteSceneSettingsHorizontalAlignment =
            when (alignment) {
                SettingsHorizontalAlignment.leading -> Leading
                SettingsHorizontalAlignment.trailing -> Trailing
                SettingsHorizontalAlignment.center -> Center
            }
    }
}

@Serializable
data class RemoteControlRemoteSceneSettingsWidgetTypeMap(
    val northUp: Boolean,
) {
    constructor(map: SettingsWidgetMap) : this(
        northUp = map.northUp,
    )

    fun toSettings(): SettingsWidgetMap {
        val map = SettingsWidgetMap()
        map.northUp = northUp
        return map
    }
}

@Serializable
data class RemoteControlRemoteSceneSettingsWidgetTypeScene(
    @Serializable(with = UUIDSerializer::class)
    val sceneId: UUID,
) {
    constructor(scene: SettingsWidgetScene) : this(
        sceneId = scene.sceneId,
    )

    fun toSettings(): SettingsWidgetScene {
        val scene = SettingsWidgetScene()
        scene.sceneId = sceneId
        return scene
    }
}

@Serializable
data class RemoteControlRemoteSceneData(
    var textStats: RemoteControlRemoteSceneDataVariables? = null,
    var location: RemoteControlRemoteSceneDataLocation? = null,
)

@Serializable
data class RemoteControlRemoteSceneDataVariables(
    val bitrate: String,
    val bitrateAndTotal: String,
    val bonding: String,
    val resolution: String? = null,
    val fps: Int? = null,
    val date: @Serializable(with = RemoteControlDateSerializer::class) Instant,
    val debugOverlayLines: List<String>,
    val speed: Double,
    val averageSpeed: Double,
    val altitude: Double,
    val distance: Double,
    val splitDistance: Double,
    val altitudeAscent: Double,
    val altitudeDescent: Double,
    val splitAltitudeAscent: Double,
    val splitAltitudeDescent: Double,
    val slope: String,
    val conditions: String? = null,
    val condition: String? = null,
    val temperature: @Serializable(with = RemoteControlTemperatureSerializer::class) Double? = null,
    val feelsLikeTemperature: @Serializable(with = RemoteControlTemperatureSerializer::class) Double? = null,
    val windSpeed: @Serializable(with = RemoteControlSpeedSerializer::class) Double? = null,
    val windGust: @Serializable(with = RemoteControlSpeedSerializer::class) Double? = null,
    val country: String? = null,
    val countryFlag: String? = null,
    val state: String? = null,
    val area: String? = null,
    val city: String? = null,
    val neighborhood: String? = null,
    val muted: Boolean,
    val heartRates: Map<String, Int?>,
    val activeEnergyBurned: Int? = null,
    val workoutDistance: Int? = null,
    val power: Int? = null,
    val stepCount: Int? = null,
    val teslaBatteryLevel: String,
    val teslaDrive: String,
    val teslaMedia: String,
    val cyclingPower: String,
    val cyclingCadence: String,
    val cyclingSpeed: Double,
    val runningMetrics: Map<String, WorkoutDeviceRunningMetrics>,
    val browserTitle: String,
    val gForce: GForce? = null,
    val latestSubscriber: String,
    val latestFollower: String,
    val systemMonitor: String,
) {
    constructor(variables: Variables) : this(
        bitrate = variables.bitrate,
        bitrateAndTotal = variables.bitrateAndTotal,
        bonding = variables.bonding,
        resolution = variables.resolution,
        fps = variables.fps,
        date = variables.date,
        debugOverlayLines = variables.debugOverlayLines,
        speed = variables.speed,
        averageSpeed = variables.averageSpeed,
        altitude = variables.altitude,
        distance = variables.distance,
        splitDistance = variables.splitDistance,
        altitudeAscent = variables.altitudeAscent,
        altitudeDescent = variables.altitudeDescent,
        splitAltitudeAscent = variables.splitAltitudeAscent,
        splitAltitudeDescent = variables.splitAltitudeDescent,
        slope = variables.slope,
        conditions = variables.conditions,
        condition = variables.condition,
        temperature = variables.temperature,
        feelsLikeTemperature = variables.feelsLikeTemperature,
        windSpeed = variables.windSpeed,
        windGust = variables.windGust,
        country = variables.country,
        countryFlag = variables.countryFlag,
        state = variables.state,
        area = variables.area,
        city = variables.city,
        neighborhood = variables.neighborhood,
        muted = variables.muted,
        heartRates = variables.heartRates,
        activeEnergyBurned = variables.activeEnergyBurned,
        workoutDistance = variables.workoutDistance,
        power = variables.power,
        stepCount = variables.stepCount,
        teslaBatteryLevel = variables.teslaBatteryLevel,
        teslaDrive = variables.teslaDrive,
        teslaMedia = variables.teslaMedia,
        cyclingPower = variables.cyclingPower,
        cyclingCadence = variables.cyclingCadence,
        cyclingSpeed = variables.cyclingSpeed,
        runningMetrics = variables.runningMetrics,
        browserTitle = variables.browserTitle,
        gForce = variables.gForce,
        latestSubscriber = variables.latestSubscriber,
        latestFollower = variables.latestFollower,
        systemMonitor = variables.systemMonitor,
    )

    fun toVariables(): Variables = Variables(
        timestamp = com.moblin.android.platform.core.ContinuousClock.now.nanoseconds,
        bitrate = bitrate,
        bitrateAndTotal = bitrateAndTotal,
        bonding = bonding,
        resolution = resolution,
        fps = fps,
        date = date,
        debugOverlayLines = debugOverlayLines,
        speed = speed,
        averageSpeed = averageSpeed,
        altitude = altitude,
        distance = distance,
        splitDistance = splitDistance,
        altitudeAscent = altitudeAscent,
        altitudeDescent = altitudeDescent,
        splitAltitudeAscent = splitAltitudeAscent,
        splitAltitudeDescent = splitAltitudeDescent,
        slope = slope,
        conditions = conditions,
        condition = condition,
        temperature = temperature,
        feelsLikeTemperature = feelsLikeTemperature,
        windSpeed = windSpeed,
        windGust = windGust,
        country = country,
        countryFlag = countryFlag,
        state = state,
        area = area,
        city = city,
        neighborhood = neighborhood,
        muted = muted,
        heartRates = heartRates,
        activeEnergyBurned = activeEnergyBurned,
        workoutDistance = workoutDistance,
        power = power,
        stepCount = stepCount,
        teslaBatteryLevel = teslaBatteryLevel,
        teslaDrive = teslaDrive,
        teslaMedia = teslaMedia,
        cyclingPower = cyclingPower,
        cyclingCadence = cyclingCadence,
        cyclingSpeed = cyclingSpeed,
        runningMetrics = runningMetrics,
        browserTitle = browserTitle,
        gForce = gForce,
        latestSubscriber = latestSubscriber,
        latestFollower = latestFollower,
        systemMonitor = systemMonitor,
    )
}

@Serializable
data class RemoteControlRemoteSceneDataLocation(
    val latitude: Double,
    val longitude: Double,
    val course: Double,
    val speed: Double,
) {
    constructor(location: Location) : this(
        latitude = location.latitude,
        longitude = location.longitude,
        course = location.bearing.toDouble(),
        speed = location.speed.toDouble(),
    )

    fun toLocation(): Location {
        val location = Location("remoteControl")
        location.latitude = latitude
        location.longitude = longitude
        location.bearing = course.toFloat()
        location.speed = speed.toFloat()
        return location
    }
}

@Serializable
data class RemoteControlStatusItem(
    var message: String,
    var ok: Boolean = true,
)

@Serializable
enum class RemoteControlStatusGeneralFlame(val rawValue: String) {
    @SerialName("White")
    White("White"),

    @SerialName("Yellow")
    Yellow("Yellow"),

    @SerialName("Red")
    Red("Red"),
    ;

    fun toThermalState(): Int = when (this) {
        White -> PowerManager.THERMAL_STATUS_LIGHT
        Yellow -> PowerManager.THERMAL_STATUS_SEVERE
        Red -> PowerManager.THERMAL_STATUS_CRITICAL
    }

    companion object {
        fun fromRawValue(value: String): RemoteControlStatusGeneralFlame? =
            entries.firstOrNull { it.rawValue == value }
    }
}

@Serializable(with = RemoteControlStatusTopRightAudioLevelSerializer::class)
sealed class RemoteControlStatusTopRightAudioLevel {
    abstract fun toJsonElement(): JsonObject

    data object Muted : RemoteControlStatusTopRightAudioLevel() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("muted") {}
        }
    }

    data object Unknown : RemoteControlStatusTopRightAudioLevel() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("unknown") {}
        }
    }

    data class Value(val value: Float) : RemoteControlStatusTopRightAudioLevel() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("value") { put("_0", value) }
        }
    }

    fun toFloat(): Float = when (this) {
        Muted -> Float.NaN
        Unknown -> Float.POSITIVE_INFINITY
        is Value -> value
    }

    companion object {
        fun fromJsonElement(element: JsonElement): RemoteControlStatusTopRightAudioLevel? {
            val entry = element.jsonObject.entries.first()
            val params = entry.value.jsonObject
            return when (entry.key) {
                "muted" -> Muted
                "unknown" -> Unknown
                "value" -> Value(params.getValue("_0").jsonPrimitive.float)
                else -> null
            }
        }
    }
}

object RemoteControlStatusTopRightAudioLevelSerializer : KSerializer<RemoteControlStatusTopRightAudioLevel> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun serialize(encoder: Encoder, value: RemoteControlStatusTopRightAudioLevel) {
        encoder.encodeSerializableValue(JsonElement.serializer(), value.toJsonElement())
    }

    override fun deserialize(decoder: Decoder): RemoteControlStatusTopRightAudioLevel =
        RemoteControlStatusTopRightAudioLevel.fromJsonElement(
            decoder.decodeSerializableValue(JsonElement.serializer()),
        ) ?: throw SerializationException("Unknown audio level")
}

@Serializable
data class RemoteControlStatusTopRightAudioInfo(
    var audioLevel: RemoteControlStatusTopRightAudioLevel,
    var numberOfAudioChannels: Int,
)

@Serializable
data class RemoteControlStatusGeneral(
    var batteryCharging: Boolean? = null,
    var batteryLevel: Int? = null,
    var flame: RemoteControlStatusGeneralFlame? = null,
    var wiFiSsid: String? = null,
    var isLive: Boolean? = null,
    var isRecording: Boolean? = null,
    var isMuted: Boolean? = null,
)

@Serializable
data class RemoteControlStatusTopLeft(
    var stream: RemoteControlStatusItem? = null,
    var camera: RemoteControlStatusItem? = null,
    var mic: RemoteControlStatusItem? = null,
    var zoom: RemoteControlStatusItem? = null,
    var obs: RemoteControlStatusItem? = null,
    var events: RemoteControlStatusItem? = null,
    var chat: RemoteControlStatusItem? = null,
    var viewers: RemoteControlStatusItem? = null,
)

@Serializable
data class RemoteControlStatusTopRight(
    var audioInfo: RemoteControlStatusTopRightAudioInfo? = null,
    var audioLevel: RemoteControlStatusItem? = null,
    var rtmpServer: RemoteControlStatusItem? = null,
    var remoteControl: RemoteControlStatusItem? = null,
    var gameController: RemoteControlStatusItem? = null,
    var bitrate: RemoteControlStatusItem? = null,
    var uptime: RemoteControlStatusItem? = null,
    var location: RemoteControlStatusItem? = null,
    var srtla: RemoteControlStatusItem? = null,
    var srtlaRtts: RemoteControlStatusItem? = null,
    var recording: RemoteControlStatusItem? = null,
    var replay: RemoteControlStatusItem? = null,
    var browserWidgets: RemoteControlStatusItem? = null,
    var moblink: RemoteControlStatusItem? = null,
    var djiDevices: RemoteControlStatusItem? = null,
    var systemMonitor: RemoteControlStatusItem? = null,
)

@Serializable
data class RemoteControlSettingsStream(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
)

@Serializable
data class RemoteControlSettingsScene(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
)

@Serializable
data class RemoteControlSettingsAutoSceneSwitcher(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
)

@Serializable
data class RemoteControlSettingsBitratePreset(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val bitrate: UInt,
)

@Serializable
data class RemoteControlSettingsMic(
    val id: String,
    val name: String,
)

@Serializable
data class RemoteControlSettingsSrtConnectionPriority(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
    var priority: Int,
    var enabled: Boolean,
)

@Serializable
data class RemoteControlSettingsSrt(
    val connectionPrioritiesEnabled: Boolean,
    val connectionPriorities: List<RemoteControlSettingsSrtConnectionPriority>,
)

@Serializable
data class RemoteControlSettingsGimbalPreset(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
)

@Serializable
data class RemoteControlSettings(
    var streams: List<RemoteControlSettingsStream>,
    var scenes: List<RemoteControlSettingsScene>,
    var autoSceneSwitchers: List<RemoteControlSettingsAutoSceneSwitcher>? = null,
    var bitratePresets: List<RemoteControlSettingsBitratePreset>,
    var mics: List<RemoteControlSettingsMic>,
    var srt: RemoteControlSettingsSrt,
)

@Serializable
data class RemoteControlStateAutoSceneSwitcher(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
)

@Serializable
data class RemoteControlZoomPreset(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
)

@Serializable
data class RemoteControlMacro(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
    val running: Boolean,
)

@Serializable
data class RemoteControlAssistantStreamerState(
    @Serializable(with = UUIDSerializer::class)
    var scene: UUID? = null,
    var autoSceneSwitcher: RemoteControlStateAutoSceneSwitcher? = null,
    var mic: String? = null,
    @Serializable(with = UUIDSerializer::class)
    var bitrate: UUID? = null,
    var zoom: Float? = null,
    var zoomPresets: List<RemoteControlZoomPreset>? = null,
    @Serializable(with = UUIDSerializer::class)
    var zoomPreset: UUID? = null,
    var debugLogging: Boolean? = null,
    var streaming: Boolean? = null,
    var recording: Boolean? = null,
    var previewStream: Boolean? = null,
    var muted: Boolean? = null,
    var stealthMode: Boolean? = null,
    var torchOn: Boolean? = null,
    var batteryCharging: Boolean? = null,
    var filters: @Serializable(with = RemoteControlFiltersSerializer::class) Map<RemoteControlFilter, Boolean>? = null,
    var gimbalTracking: Boolean? = null,
    var gimbalPresets: List<RemoteControlSettingsGimbalPreset>? = null,
    var macros: List<RemoteControlMacro>? = null,
)

@Serializable
data class RemoteControlScoreboardControl(
    var type: String,
    var label: String,
    var options: List<String>? = null,
    var periodReset: Boolean? = null,
)

@Serializable
data class RemoteControlScoreboardTeam(
    var name: String,
    var bgColor: String,
    var textColor: String = "#ffffff",
    var possession: Boolean,
    var primaryScore: String = "0",
    var secondaryScore: String = "",
    var secondaryScoreLabel: String? = "",
    var secondaryScore1: String? = null,
    var secondaryScore2: String? = null,
    var secondaryScore3: String? = null,
    var secondaryScore4: String? = null,
    var secondaryScore5: String? = null,
    var stat1: String = "",
    var stat1Label: String = "",
    var stat2: String = "",
    var stat2Label: String = "",
    var stat3: String = "",
    var stat3Label: String = "",
    var stat4: String = "",
    var stat4Label: String = "",
)

@Serializable
data class RemoteControlScoreboardGlobalStats(
    var title: String,
    var timer: String,
    var timerDirection: String,
    var duration: Int? = null,
    var period: String,
    var periodLabel: String,
    var infoBoxText: String = "",
    var primaryScoreResetOnPeriod: Boolean,
    var changePossessionOnScore: Boolean,
    var scoringMode: String? = null,
    var showTitle: Boolean? = null,
    var showStats: Boolean? = null,
    var showMoreStats: Boolean? = null,
    var showClock: Boolean? = null,
) {
    fun minutesAndSeconds(): Pair<Int, Int> = clockAsMinutesAndSeconds(timer)
}

@Serializable
data class RemoteControlScoreboardMatchConfig(
    var sportId: String,
    var layout: String,
    var team1: RemoteControlScoreboardTeam,
    var team2: RemoteControlScoreboardTeam,
    var global: RemoteControlScoreboardGlobalStats,
    var controls: Map<String, RemoteControlScoreboardControl>,
) {
    fun periodFull(): String {
        when (sportId) {
            "football" -> return ""
        }
        return "${global.periodLabel} ${global.period}".trim()
    }

    fun infoBoxStats(showClock: Boolean): List<String> = if (showClock) {
        listOf(global.timer, periodFull(), global.infoBoxText).filter { it.isNotEmpty() }
    } else {
        listOf(periodFull(), global.infoBoxText).filter { it.isNotEmpty() }
    }
}

@Serializable
data class RemoteControlGolfPlayer(
    val name: String,
    val scores: List<Int>,
    val color: RgbColor,
)

@Serializable
data class RemoteControlGolfScoreboard(
    val title: String,
    val numberOfHoles: Int,
    val pars: List<Int>,
    val currentHole: Int,
    val players: List<RemoteControlGolfPlayer>,
    val playerColors: Boolean,
)

@Serializable
data class RemoteControlAuthentication(
    val challenge: String,
    val salt: String,
)

@Serializable
enum class RemoteControlResult(val wireName: String) {
    @SerialName("ok")
    Ok("ok"),

    @SerialName("wrongPassword")
    WrongPassword("wrongPassword"),

    @SerialName("unknownRequest")
    UnknownRequest("unknownRequest"),

    @SerialName("notIdentified")
    NotIdentified("notIdentified"),

    @SerialName("alreadyIdentified")
    AlreadyIdentified("alreadyIdentified"),

    @SerialName("error")
    Error("error"),
    ;

    companion object {
        fun fromName(name: String): RemoteControlResult? = entries.firstOrNull { it.wireName == name }
    }
}

sealed class RemoteControlMessageToStreamer {
    abstract fun toJsonElement(): JsonObject

    fun toJson(): String? = runCatching {
        remoteControlJson.encodeToString(JsonElement.serializer(), toJsonElement())
    }.getOrNull()

    data class Hello(val apiVersion: String, val authentication: RemoteControlAuthentication) : RemoteControlMessageToStreamer() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("hello") {
                put("apiVersion", apiVersion)
                put("authentication", remoteControlJson.encodeToJsonElement(authentication))
            }
        }
    }

    data class Identified(val result: RemoteControlResult) : RemoteControlMessageToStreamer() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("identified") {
                put("result", remoteControlJson.encodeToJsonElement(result))
            }
        }
    }

    data class Request(val id: Int, val data: RemoteControlRequest) : RemoteControlMessageToStreamer() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("request") {
                put("id", id)
                put("data", data.toJsonElement())
            }
        }
    }

    data object Pong : RemoteControlMessageToStreamer() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("pong") {}
        }
    }

    companion object {
        fun fromJson(data: String): RemoteControlMessageToStreamer {
            val entry = remoteControlJson.parseToJsonElement(data).jsonObject.entries.first()
            val params = entry.value.jsonObject
            return when (entry.key) {
                "hello" -> Hello(
                    apiVersion = params.getValue("apiVersion").jsonPrimitive.content,
                    authentication = remoteControlJson.decodeFromJsonElement(params.getValue("authentication")),
                )
                "identified" -> Identified(
                    result = remoteControlJson.decodeFromJsonElement(params.getValue("result")),
                )
                "request" -> Request(
                    id = params.getValue("id").jsonPrimitive.int,
                    data = RemoteControlRequest.fromJsonElement(params.getValue("data")),
                )
                "pong" -> Pong
                else -> throw SerializationException("Unknown message to streamer ${entry.key}")
            }
        }
    }
}

sealed class RemoteControlMessageToAssistant {
    abstract fun toJsonElement(): JsonObject

    fun toJson(): String = remoteControlJson.encodeToString(JsonElement.serializer(), toJsonElement())

    data class Identify(val streamerId: String?, val authentication: String) : RemoteControlMessageToAssistant() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("identify") {
                if (streamerId == null) {
                    put("streamerId", JsonNull)
                } else {
                    put("streamerId", streamerId)
                }
                put("authentication", authentication)
            }
        }
    }

    data class Response(
        val id: Int,
        val result: RemoteControlResult,
        val data: RemoteControlResponse? = null,
    ) : RemoteControlMessageToAssistant() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("response") {
                put("id", id)
                put("result", remoteControlJson.encodeToJsonElement(result))
                if (data == null) {
                    put("data", JsonNull)
                } else {
                    put("data", data.toJsonElement())
                }
            }
        }
    }

    data class Event(val data: RemoteControlEvent) : RemoteControlMessageToAssistant() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("event") { put("data", data.toJsonElement()) }
        }
    }

    data class Preview(val preview: ByteArray) : RemoteControlMessageToAssistant() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("preview") {
                put("preview", Base64.encodeToString(preview, Base64.NO_WRAP))
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Preview) return false
            return preview.contentEquals(other.preview)
        }

        override fun hashCode(): Int = preview.contentHashCode()
    }

    data class TwitchStart(val channelName: String?, val channelId: String, val accessToken: String) : RemoteControlMessageToAssistant() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("twitchStart") {
                if (channelName == null) {
                    put("channelName", JsonNull)
                } else {
                    put("channelName", channelName)
                }
                put("channelId", channelId)
                put("accessToken", accessToken)
            }
        }
    }

    data object Ping : RemoteControlMessageToAssistant() {
        override fun toJsonElement(): JsonObject = buildJsonObject {
            putJsonObject("ping") {}
        }
    }

    companion object {
        fun fromJson(data: String): RemoteControlMessageToAssistant {
            val entry = remoteControlJson.parseToJsonElement(data).jsonObject.entries.first()
            val params = entry.value.jsonObject
            return when (entry.key) {
                "identify" -> Identify(
                    streamerId = params["streamerId"]?.let {
                        if (it is JsonNull) null else it.jsonPrimitive.content
                    },
                    authentication = params.getValue("authentication").jsonPrimitive.content,
                )
                "response" -> Response(
                    id = params.getValue("id").jsonPrimitive.int,
                    result = remoteControlJson.decodeFromJsonElement(params.getValue("result")),
                    data = params["data"]?.let {
                        if (it is JsonNull) null else RemoteControlResponse.fromJsonElement(it)
                    },
                )
                "event" -> Event(
                    data = RemoteControlEvent.fromJsonElement(params.getValue("data")),
                )
                "preview" -> Preview(
                    preview = Base64.decode(params.getValue("preview").jsonPrimitive.content, Base64.NO_WRAP),
                )
                "twitchStart" -> TwitchStart(
                    channelName = params["channelName"]?.let {
                        if (it is JsonNull) null else it.jsonPrimitive.content
                    },
                    channelId = params.getValue("channelId").jsonPrimitive.content,
                    accessToken = params.getValue("accessToken").jsonPrimitive.content,
                )
                "ping" -> Ping
                else -> throw SerializationException("Unknown message to assistant ${entry.key}")
            }
        }
    }
}

fun remoteControlHashPassword(challenge: String, salt: String, password: String): String {
    var concatenated = "$password$salt"
    var hash = MessageDigest.getInstance("SHA-256").digest(concatenated.toByteArray(Charsets.UTF_8))
    concatenated = "${Base64.encodeToString(hash, Base64.NO_WRAP)}$challenge"
    hash = MessageDigest.getInstance("SHA-256").digest(concatenated.toByteArray(Charsets.UTF_8))
    return Base64.encodeToString(hash, Base64.NO_WRAP)
}

class RemoteControlEncryption(password: String) {
    private val key: ByteArray

    init {
        key = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
    }

    fun encrypt(data: ByteArray): ByteArray? = runCatching {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val nonce = ByteArray(12)
        SecureRandom().nextBytes(nonce)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        val sealed = cipher.doFinal(data)
        nonce + sealed
    }.getOrNull()

    fun decrypt(data: ByteArray): ByteArray? = runCatching {
        if (data.size < 12 + 16) {
            throw IllegalArgumentException("Sealed box too short")
        }
        val nonce = data.copyOfRange(0, 12)
        val sealed = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.doFinal(sealed)
    }.getOrNull()
}
