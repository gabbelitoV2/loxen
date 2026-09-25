package com.moblin.android.various.storages

import android.os.PowerManager
import android.util.Log
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.moblink.MoblinkThermalState
import com.moblin.android.platform.codable.AppleDateSerializer
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.UUIDSerializer
import com.moblin.android.platform.codable.codableJson
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.various.settings.SettingsStream
import java.math.BigInteger
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

private const val TAG = "StreamingHistory"

@Serializable(with = ThermalStateSerializer::class)
enum class ThermalState(val rawValue: Int) : Comparable<ThermalState> {
    NOMINAL(0),
    FAIR(1),
    SERIOUS(2),
    CRITICAL(3);

    fun toProcessInfo(): com.moblin.android.platform.core.ProcessInfo.ThermalState {
        return when (this) {
            NOMINAL -> com.moblin.android.platform.core.ProcessInfo.ThermalState.nominal
            FAIR -> com.moblin.android.platform.core.ProcessInfo.ThermalState.fair
            SERIOUS -> com.moblin.android.platform.core.ProcessInfo.ThermalState.serious
            CRITICAL -> com.moblin.android.platform.core.ProcessInfo.ThermalState.critical
        }
    }

    companion object {
        fun from(from: com.moblin.android.platform.core.ProcessInfo.ThermalState): ThermalState = entries.first { it.rawValue == from.rawValue }
        fun from(from: Int): ThermalState {
            return when (from) {
                PowerManager.THERMAL_STATUS_NONE -> NOMINAL
                PowerManager.THERMAL_STATUS_LIGHT -> FAIR
                PowerManager.THERMAL_STATUS_MODERATE -> SERIOUS
                PowerManager.THERMAL_STATUS_SEVERE -> CRITICAL
                PowerManager.THERMAL_STATUS_CRITICAL -> CRITICAL
                PowerManager.THERMAL_STATUS_EMERGENCY -> CRITICAL
                PowerManager.THERMAL_STATUS_SHUTDOWN -> CRITICAL
                else -> NOMINAL
            }
        }

        fun fromRawValue(rawValue: Int): ThermalState {
            return ThermalState.entries.firstOrNull { it.rawValue == rawValue } ?: NOMINAL
        }
    }
}

object ThermalStateSerializer : KSerializer<ThermalState> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ThermalState", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: ThermalState) {
        encoder.encodeInt(value.rawValue)
    }

    override fun deserialize(decoder: Decoder): ThermalState {
        val rawValue = decoder.decodeInt()
        return ThermalState.entries.firstOrNull { it.rawValue == rawValue }
            ?: throw SerializationException("Invalid ThermalState raw value $rawValue")
    }
}

private object SwiftDurationSerializer : KSerializer<Duration> {
    private val attosecondsPerNanosecond = BigInteger.valueOf(1_000_000_000L)
    private val lowMask = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE)

    override val descriptor: SerialDescriptor = ListSerializer(Long.serializer()).descriptor

    override fun serialize(encoder: Encoder, value: Duration) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("Duration supports only JSON")
        val attoseconds = BigInteger.valueOf(value.inWholeNanoseconds).multiply(attosecondsPerNanosecond)
        val high = attoseconds.shiftRight(64).toLong()
        val low = attoseconds.and(lowMask).toLong().toULong()
        jsonEncoder.encodeJsonElement(
            JsonArray(
                listOf(
                    jsonEncoder.json.encodeToJsonElement(Long.serializer(), high),
                    jsonEncoder.json.encodeToJsonElement(ULong.serializer(), low),
                ),
            ),
        )
    }

    override fun deserialize(decoder: Decoder): Duration {
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("Duration supports only JSON")
        val array = jsonDecoder.decodeJsonElement() as? JsonArray
            ?: throw SerializationException("Duration expects an array")
        if (array.size < 2) {
            throw SerializationException("Duration expects two numbers")
        }
        val high = jsonDecoder.json.decodeFromJsonElement(Long.serializer(), array[0])
        val low = jsonDecoder.json.decodeFromJsonElement(ULong.serializer(), array[1])
        val attoseconds = BigInteger.valueOf(high).shiftLeft(64).add(BigInteger(low.toString()))
        val nanoseconds = attoseconds.divide(attosecondsPerNanosecond)
        if (nanoseconds.bitLength() > 63) {
            throw SerializationException("Duration out of range")
        }
        return nanoseconds.toLong().nanoseconds
    }
}

private fun <T> JsonObject.decodeRequired(key: String, serializer: KSerializer<T>): T {
    val element = this[key] ?: throw SerializationException("Missing key '$key'")
    return codableJson.decodeFromJsonElement(serializer, element)
}

private fun <T : Any> JsonObject.decodeOptional(key: String, serializer: KSerializer<T>): T? {
    val element = this[key] ?: return null
    if (element is JsonNull) {
        return null
    }
    return codableJson.decodeFromJsonElement(serializer, element)
}

@Serializable(with = StreamingHistoryStream.Serializer::class)
class StreamingHistoryStream(
    var id: UUID = UUID.randomUUID(),
    var settings: SettingsStream,
    var startTime: Instant = Instant.now(),
    var stopTime: Instant = Instant.now(),
    var totalBytes: Long = 0,
    var highestThermalState: ThermalState? = ThermalState.NOMINAL,
    var lowestBatteryLevel: Double? = 1.0,
    var highestBitrate: Long? = Long.MIN_VALUE,
) {
    fun updateBitrate(bitrate: Long) {
        if (bitrate > highestBitrate!!) {
            highestBitrate = bitrate
        }
    }

    fun averageBitrateString(): String {
        val bitrate = 8L * totalBytes / maxOf(duration().inWholeSeconds, 1L)
        return formatBytesPerSecond(bitrate)
    }

    fun highestBitrateString(): String {
        return formatBytesPerSecond(highestBitrate!!)
    }

    fun updateHighestThermalState(thermalState: ThermalState) {
        if (thermalState > highestThermalState!!) {
            highestThermalState = thermalState
        }
    }

    fun updateLowestBatteryLevel(level: Double) {
        if (level < lowestBatteryLevel!!) {
            lowestBatteryLevel = level
        }
    }

    fun lowestBatteryPercentageString(): String {
        return "${(100 * lowestBatteryLevel!!).toInt()}%"
    }

    fun duration(): Duration {
        return (stopTime.toEpochMilli() - startTime.toEpochMilli()).milliseconds
    }

    fun encode(): JsonObject = encodeContainer {
        encode("id", id, UUIDSerializer)
        encode("settings", settings, SettingsStream.serializer())
        encode("startTime", startTime, AppleDateSerializer)
        encode("stopTime", stopTime, AppleDateSerializer)
        encode("totalBytes", totalBytes.toULong())
        encodeIfPresent("highestThermalState", highestThermalState, ThermalState.serializer())
        encodeIfPresent("lowestBatteryLevel", lowestBatteryLevel)
        encodeIfPresent("highestBitrate", highestBitrate)
    }

    companion object {
        fun decode(container: JsonObject): StreamingHistoryStream {
            return StreamingHistoryStream(
                id = container.decodeRequired("id", UUIDSerializer),
                settings = container.decodeRequired("settings", SettingsStream.serializer()),
                startTime = container.decodeRequired("startTime", AppleDateSerializer),
                stopTime = container.decodeRequired("stopTime", AppleDateSerializer),
                totalBytes = container.decodeRequired("totalBytes", ULong.serializer()).toLong(),
                highestThermalState = container.decodeOptional("highestThermalState", ThermalState.serializer()),
                lowestBatteryLevel = container.decodeOptional("lowestBatteryLevel", Double.serializer()),
                highestBitrate = container.decodeOptional("highestBitrate", Long.serializer()),
            )
        }
    }

    object Serializer : KSerializer<StreamingHistoryStream> by JsonObjectSerializer(
        "StreamingHistoryStream",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = StreamingHistoryDatabase.Serializer::class)
class StreamingHistoryDatabase {
    var totalTime: MutableStateFlow<Duration> = MutableStateFlow(Duration.ZERO)
    var totalBytes: MutableStateFlow<Long> = MutableStateFlow(0L)
    var totalStreams: MutableStateFlow<Long> = MutableStateFlow(0L)
    var streams: MutableStateFlow<List<StreamingHistoryStream>> = MutableStateFlow(emptyList())

    fun encode(): JsonObject = encodeContainer {
        encode("totalTime", totalTime, SwiftDurationSerializer)
        encode("totalBytes", totalBytes.value.toULong())
        encode("totalStreams", totalStreams.value.toULong())
        encode("streams", streams, ListSerializer(StreamingHistoryStream.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): StreamingHistoryDatabase {
            val database = StreamingHistoryDatabase()
            database.totalTime.value = container.decode("totalTime", SwiftDurationSerializer, Duration.ZERO)
            database.totalBytes.value = container.decode("totalBytes", 0uL).toLong()
            database.totalStreams.value = container.decode("totalStreams", 0uL).toLong()
            database.streams.value = container.decode(
                "streams",
                ListSerializer(StreamingHistoryStream.serializer()),
                emptyList(),
            )
            return database
        }

        fun fromString(settings: String): StreamingHistoryDatabase {
            return codableJson.decodeFromString(Serializer, settings)
        }
    }

    override fun toString(): String {
        return codableJson.encodeToString(Serializer, this)
    }

    object Serializer : KSerializer<StreamingHistoryDatabase> by JsonObjectSerializer(
        "StreamingHistoryDatabase",
        { it.encode() },
        { decode(it) },
    )
}

private val storage = SimpleStringStorage("streamingHistory")

class StreamingHistory {
    private var realDatabase = StreamingHistoryDatabase()

    val database: StreamingHistoryDatabase
        get() = realDatabase

    fun load() {
        try {
            tryLoadAndMigrate(storage.get())
        } catch (e: Exception) {
            Log.i(TAG, "streaming-history: Failed to load with error $e. Using default.")
            realDatabase = StreamingHistoryDatabase()
        }
    }

    private fun tryLoadAndMigrate(settings: String) {
        realDatabase = StreamingHistoryDatabase.fromString(settings)
        migrateFromOlderVersions()
    }

    fun store() {
        try {
            storage.set(realDatabase.toString())
        } catch (e: Exception) {
            Log.i(TAG, "streaming-history: Failed to store.")
        }
    }

    private fun migrateFromOlderVersions() {
        for (stream in database.streams.value.filter { it.highestThermalState == null }) {
            stream.highestThermalState = ThermalState.NOMINAL
            store()
        }
        for (stream in database.streams.value.filter { it.lowestBatteryLevel == null }) {
            stream.lowestBatteryLevel = 1.0
            store()
        }
        for (stream in database.streams.value.filter { it.highestBitrate == null }) {
            stream.highestBitrate = Long.MIN_VALUE
            store()
        }
    }

    fun append(stream: StreamingHistoryStream) {
        while (database.streams.value.size > 100) {
            database.streams.value = database.streams.value.dropLast(1)
        }
        database.totalTime.value += stream.duration()
        database.totalBytes.value += stream.totalBytes
        database.totalStreams.value += 1
        database.streams.value = listOf(stream) + database.streams.value
    }
}

fun MoblinkThermalState.toThermalState(): ThermalState = when (this) {
    MoblinkThermalState.white -> ThermalState.NOMINAL
    MoblinkThermalState.yellow -> ThermalState.FAIR
    MoblinkThermalState.red -> ThermalState.SERIOUS
}
