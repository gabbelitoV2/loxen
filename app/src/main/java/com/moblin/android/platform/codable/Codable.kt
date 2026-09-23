package com.moblin.android.platform.codable

import java.time.Instant
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.serializer

private val uuidPattern = Regex("^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$")

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.moblin.android.platform.codable.UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString().uppercase())
    }

    override fun deserialize(decoder: Decoder): UUID {
        val text = decoder.decodeString()
        if (!uuidPattern.matches(text)) {
            throw SerializationException("Invalid UUID '$text'")
        }
        return try {
            UUID.fromString(text)
        } catch (error: IllegalArgumentException) {
            throw SerializationException("Invalid UUID '$text'")
        }
    }
}

private const val secondsFrom1970To2001 = 978_307_200.0

object AppleDateSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.moblin.android.platform.codable.Date", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Instant) {
        val seconds = value.epochSecond + value.nano / 1_000_000_000.0 - secondsFrom1970To2001
        encoder.encodeDouble(seconds)
    }

    override fun deserialize(decoder: Decoder): Instant {
        val seconds = decoder.decodeDouble() + secondsFrom1970To2001
        val whole = Math.floor(seconds)
        return Instant.ofEpochSecond(whole.toLong(), ((seconds - whole) * 1_000_000_000.0).toLong())
    }
}

object DataSerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.moblin.android.platform.codable.Data", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(Base64.getEncoder().encodeToString(value))
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        return try {
            Base64.getDecoder().decode(decoder.decodeString())
        } catch (error: IllegalArgumentException) {
            throw SerializationException("Invalid base64 data")
        }
    }
}

class MutableStateFlowSerializer<T>(private val valueSerializer: KSerializer<T>) :
    KSerializer<MutableStateFlow<T>> {
    override val descriptor: SerialDescriptor = valueSerializer.descriptor

    override fun serialize(encoder: Encoder, value: MutableStateFlow<T>) {
        encoder.encodeSerializableValue(valueSerializer, value.value)
    }

    override fun deserialize(decoder: Decoder): MutableStateFlow<T> {
        return MutableStateFlow(decoder.decodeSerializableValue(valueSerializer))
    }
}

val codableJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
    explicitNulls = true
    allowSpecialFloatingPointValues = true
    serializersModule = SerializersModule {
        contextual(UUIDSerializer)
        contextual(AppleDateSerializer)
    }
}

class JsonObjectSerializer<T>(
    serialName: String,
    private val encodeObject: (T) -> JsonObject,
    private val decodeObject: (JsonObject) -> T,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(serialName)

    override fun serialize(encoder: Encoder, value: T) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("$descriptor supports only JSON")
        jsonEncoder.encodeJsonElement(encodeObject(value))
    }

    override fun deserialize(decoder: Decoder): T {
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("$descriptor supports only JSON")
        val element = jsonDecoder.decodeJsonElement()
        val jsonObject = element as? JsonObject ?: throw SerializationException("$descriptor expects a JSON object")
        return decodeObject(jsonObject)
    }
}

class KeyedEncodingContainer(val json: Json = codableJson) {
    val content = LinkedHashMap<String, JsonElement>()

    fun <T> encode(key: String, value: T, serializer: KSerializer<T>) {
        content[key] = json.encodeToJsonElement(serializer, value)
    }

    inline fun <reified T> encode(key: String, value: T) {
        encode(key, value, json.serializersModule.serializer<T>())
    }

    fun <T> encode(key: String, value: MutableStateFlow<T>, serializer: KSerializer<T>) {
        encode(key, value.value, serializer)
    }

    inline fun <reified T> encode(key: String, value: MutableStateFlow<T>) {
        encode(key, value.value, json.serializersModule.serializer<T>())
    }

    fun <T : Any> encodeIfPresent(key: String, value: T?, serializer: KSerializer<T>) {
        if (value != null) {
            encode(key, value, serializer)
        }
    }

    inline fun <reified T : Any> encodeIfPresent(key: String, value: T?) {
        if (value != null) {
            encode(key, value, json.serializersModule.serializer<T>())
        }
    }
}

fun encodeContainer(json: Json = codableJson, block: KeyedEncodingContainer.() -> Unit): JsonObject {
    val container = KeyedEncodingContainer(json)
    container.block()
    return JsonObject(container.content)
}

private val numericKinds = setOf<SerialKind>(
    PrimitiveKind.BYTE,
    PrimitiveKind.SHORT,
    PrimitiveKind.INT,
    PrimitiveKind.LONG,
    PrimitiveKind.FLOAT,
    PrimitiveKind.DOUBLE,
)

private fun hasSwiftType(element: JsonElement, kind: SerialKind): Boolean {
    if (element !is JsonPrimitive) {
        return true
    }
    return when (kind) {
        in numericKinds -> !element.isString && element.content != "true" && element.content != "false"
        PrimitiveKind.BOOLEAN -> !element.isString && (element.content == "true" || element.content == "false")
        PrimitiveKind.STRING, PrimitiveKind.CHAR, SerialKind.ENUM -> element.isString
        else -> true
    }
}

fun <T> JsonObject.decode(key: String, serializer: KSerializer<T>, default: T, json: Json = codableJson): T {
    val element = this[key] ?: return default
    if (element is JsonNull) {
        return default
    }
    if (!hasSwiftType(element, serializer.descriptor.kind)) {
        return default
    }
    return try {
        json.decodeFromJsonElement(serializer, element)
    } catch (error: Exception) {
        default
    }
}

fun <T> JsonObject.decode(
    key: String,
    serializer: KSerializer<T>,
    default: T,
    isValid: (T) -> Boolean,
    json: Json = codableJson,
): T {
    val value = decode(key, serializer, default, json)
    return if (isValid(value)) value else default
}

inline fun <reified T> JsonObject.decode(key: String, default: T): T {
    return decode(key, codableJson.serializersModule.serializer<T>(), default)
}

inline fun <reified T> JsonObject.decode(key: String, default: T, noinline isValid: (T) -> Boolean): T {
    return decode(key, codableJson.serializersModule.serializer<T>(), default, isValid)
}

fun <T : Any> JsonObject.decodeIfPresent(key: String, serializer: KSerializer<T>, json: Json = codableJson): T? {
    val element = this[key] ?: return null
    if (element is JsonNull) {
        return null
    }
    if (!hasSwiftType(element, serializer.descriptor.kind)) {
        return null
    }
    return try {
        json.decodeFromJsonElement(serializer, element)
    } catch (error: Exception) {
        null
    }
}

inline fun <reified T : Any> JsonObject.decodeIfPresent(key: String): T? {
    return decodeIfPresent(key, codableJson.serializersModule.serializer<T>())
}
