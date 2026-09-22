package com.moblin.android.moblink

import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

const val moblinkBonjourType = "_moblink._tcp"
const val moblinkBonjourDomain = "local"

private val moblinkJson = Json {
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
}

private fun moblinkCaseObject(name: String, payload: JsonElement): JsonObject =
    JsonObject(mapOf(name to payload))

private fun JsonElement.moblinkCase(): Pair<String, JsonElement> {
    val entry = jsonObject.entries.firstOrNull() ?: throw SerializationException("Missing case")
    return entry.key to entry.value
}

private fun Decoder.moblinkJsonDecoder(): JsonDecoder =
    this as? JsonDecoder ?: throw SerializationException("Only JSON is supported")

private fun Encoder.moblinkJsonEncoder(): JsonEncoder =
    this as? JsonEncoder ?: throw SerializationException("Only JSON is supported")

@Serializable
enum class MoblinkThermalState(val rawValue: String) {
    @SerialName("white")
    white("white"),

    @SerialName("yellow")
    yellow("yellow"),

    @SerialName("red")
    red("red");

    companion object {
        fun fromRawValue(rawValue: String): MoblinkThermalState? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable(with = MoblinkRequestSerializer::class)
sealed class MoblinkRequest {
    @Serializable
    data class StartTunnel(val address: String, val port: UShort) : MoblinkRequest()

    object Status : MoblinkRequest()
}

object MoblinkRequestSerializer : KSerializer<MoblinkRequest> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MoblinkRequest")

    override fun serialize(encoder: Encoder, value: MoblinkRequest) {
        val output = encoder.moblinkJsonEncoder()
        val element = when (value) {
            is MoblinkRequest.StartTunnel -> moblinkCaseObject(
                "startTunnel",
                output.json.encodeToJsonElement(MoblinkRequest.StartTunnel.serializer(), value)
            )
            is MoblinkRequest.Status -> moblinkCaseObject("status", JsonObject(emptyMap()))
        }
        output.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): MoblinkRequest {
        val input = decoder.moblinkJsonDecoder()
        val (name, payload) = input.decodeJsonElement().moblinkCase()
        return when (name) {
            "startTunnel" -> input.json.decodeFromJsonElement(
                MoblinkRequest.StartTunnel.serializer(),
                payload
            )
            "status" -> MoblinkRequest.Status
            else -> throw SerializationException("Unknown MoblinkRequest case $name")
        }
    }
}

@Serializable(with = MoblinkResponseSerializer::class)
sealed class MoblinkResponse {
    @Serializable
    data class StartTunnel(val port: UShort) : MoblinkResponse()

    @Serializable
    data class Status(
        val batteryPercentage: Int? = null,
        val thermalState: MoblinkThermalState? = null,
    ) : MoblinkResponse()
}

object MoblinkResponseSerializer : KSerializer<MoblinkResponse> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MoblinkResponse")

    override fun serialize(encoder: Encoder, value: MoblinkResponse) {
        val output = encoder.moblinkJsonEncoder()
        val element = when (value) {
            is MoblinkResponse.StartTunnel -> moblinkCaseObject(
                "startTunnel",
                output.json.encodeToJsonElement(MoblinkResponse.StartTunnel.serializer(), value)
            )
            is MoblinkResponse.Status -> moblinkCaseObject(
                "status",
                output.json.encodeToJsonElement(MoblinkResponse.Status.serializer(), value)
            )
        }
        output.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): MoblinkResponse {
        val input = decoder.moblinkJsonDecoder()
        val (name, payload) = input.decodeJsonElement().moblinkCase()
        return when (name) {
            "startTunnel" -> input.json.decodeFromJsonElement(
                MoblinkResponse.StartTunnel.serializer(),
                payload
            )
            "status" -> input.json.decodeFromJsonElement(
                MoblinkResponse.Status.serializer(),
                payload
            )
            else -> throw SerializationException("Unknown MoblinkResponse case $name")
        }
    }
}

@Serializable
data class MoblinkAuthentication(
    var challenge: String,
    var salt: String,
)

@Serializable(with = MoblinkResultSerializer::class)
enum class MoblinkResult(val rawValue: String) {
    ok("ok"),
    wrongPassword("wrongPassword"),
    unknownRequest("unknownRequest"),
    notIdentified("notIdentified"),
    alreadyIdentified("alreadyIdentified");

    companion object {
        fun fromRawValue(rawValue: String): MoblinkResult? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

object MoblinkResultSerializer : KSerializer<MoblinkResult> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MoblinkResult")

    override fun serialize(encoder: Encoder, value: MoblinkResult) {
        val output = encoder.moblinkJsonEncoder()
        output.encodeJsonElement(moblinkCaseObject(value.rawValue, JsonObject(emptyMap())))
    }

    override fun deserialize(decoder: Decoder): MoblinkResult {
        val input = decoder.moblinkJsonDecoder()
        val (name, _) = input.decodeJsonElement().moblinkCase()
        return MoblinkResult.fromRawValue(name)
            ?: throw SerializationException("Unknown MoblinkResult case $name")
    }
}

@Serializable(with = MoblinkMessageToRelaySerializer::class)
sealed class MoblinkMessageToRelay {
    @Serializable
    data class Hello(
        val apiVersion: String,
        val authentication: MoblinkAuthentication,
    ) : MoblinkMessageToRelay()

    @Serializable
    data class Identified(val result: MoblinkResult) : MoblinkMessageToRelay()

    @Serializable
    data class Request(val id: Int, val data: MoblinkRequest) : MoblinkMessageToRelay()

    fun toJson(): String? = runCatching {
        moblinkJson.encodeToString(MoblinkMessageToRelaySerializer, this)
    }.getOrNull()

    companion object {
        fun fromJson(data: String): MoblinkMessageToRelay =
            moblinkJson.decodeFromString(MoblinkMessageToRelaySerializer, data)
    }
}

object MoblinkMessageToRelaySerializer : KSerializer<MoblinkMessageToRelay> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MoblinkMessageToRelay")

    override fun serialize(encoder: Encoder, value: MoblinkMessageToRelay) {
        val output = encoder.moblinkJsonEncoder()
        val element = when (value) {
            is MoblinkMessageToRelay.Hello -> moblinkCaseObject(
                "hello",
                output.json.encodeToJsonElement(MoblinkMessageToRelay.Hello.serializer(), value)
            )
            is MoblinkMessageToRelay.Identified -> moblinkCaseObject(
                "identified",
                output.json.encodeToJsonElement(
                    MoblinkMessageToRelay.Identified.serializer(),
                    value
                )
            )
            is MoblinkMessageToRelay.Request -> moblinkCaseObject(
                "request",
                output.json.encodeToJsonElement(MoblinkMessageToRelay.Request.serializer(), value)
            )
        }
        output.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): MoblinkMessageToRelay {
        val input = decoder.moblinkJsonDecoder()
        val (name, payload) = input.decodeJsonElement().moblinkCase()
        return when (name) {
            "hello" -> input.json.decodeFromJsonElement(
                MoblinkMessageToRelay.Hello.serializer(),
                payload
            )
            "identified" -> input.json.decodeFromJsonElement(
                MoblinkMessageToRelay.Identified.serializer(),
                payload
            )
            "request" -> input.json.decodeFromJsonElement(
                MoblinkMessageToRelay.Request.serializer(),
                payload
            )
            else -> throw SerializationException("Unknown MoblinkMessageToRelay case $name")
        }
    }
}

object MoblinkUuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MoblinkUuid", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString().uppercase())
    }

    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

@Serializable(with = MoblinkMessageToStreamerSerializer::class)
sealed class MoblinkMessageToStreamer {
    @Serializable
    data class Identify(
        @Serializable(with = MoblinkUuidSerializer::class) val id: UUID,
        val name: String,
        val authentication: String,
    ) : MoblinkMessageToStreamer()

    @Serializable
    data class Response(
        val id: Int,
        val result: MoblinkResult,
        val data: MoblinkResponse? = null,
    ) : MoblinkMessageToStreamer()

    fun toJson(): String = moblinkJson.encodeToString(MoblinkMessageToStreamerSerializer, this)

    companion object {
        fun fromJson(data: String): MoblinkMessageToStreamer =
            moblinkJson.decodeFromString(MoblinkMessageToStreamerSerializer, data)
    }
}

object MoblinkMessageToStreamerSerializer : KSerializer<MoblinkMessageToStreamer> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("MoblinkMessageToStreamer")

    override fun serialize(encoder: Encoder, value: MoblinkMessageToStreamer) {
        val output = encoder.moblinkJsonEncoder()
        val element = when (value) {
            is MoblinkMessageToStreamer.Identify -> moblinkCaseObject(
                "identify",
                output.json.encodeToJsonElement(
                    MoblinkMessageToStreamer.Identify.serializer(),
                    value
                )
            )
            is MoblinkMessageToStreamer.Response -> moblinkCaseObject(
                "response",
                output.json.encodeToJsonElement(
                    MoblinkMessageToStreamer.Response.serializer(),
                    value
                )
            )
        }
        output.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): MoblinkMessageToStreamer {
        val input = decoder.moblinkJsonDecoder()
        val (name, payload) = input.decodeJsonElement().moblinkCase()
        return when (name) {
            "identify" -> input.json.decodeFromJsonElement(
                MoblinkMessageToStreamer.Identify.serializer(),
                payload
            )
            "response" -> input.json.decodeFromJsonElement(
                MoblinkMessageToStreamer.Response.serializer(),
                payload
            )
            else -> throw SerializationException("Unknown MoblinkMessageToStreamer case $name")
        }
    }
}
