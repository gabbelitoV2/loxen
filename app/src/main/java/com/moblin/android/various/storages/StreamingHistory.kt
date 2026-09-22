package com.moblin.android.various.storages

import android.os.PowerManager
import android.util.Log
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.various.settings.SettingsStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "StreamingHistory"

@Serializable(with = ThermalStateSerializer::class)
enum class ThermalState(val rawValue: Int) : Comparable<ThermalState> {
    NOMINAL(0),
    FAIR(1),
    SERIOUS(2),
    CRITICAL(3);

    override fun compareTo(other: ThermalState): Int {
        return rawValue.compareTo(other.rawValue)
    }

    fun toProcessInfo(): Int {
        return when (this) {
            NOMINAL -> PowerManager.THERMAL_STATUS_NONE
            FAIR -> PowerManager.THERMAL_STATUS_LIGHT
            SERIOUS -> PowerManager.THERMAL_STATUS_MODERATE
            CRITICAL -> PowerManager.THERMAL_STATUS_SEVERE
        }
    }

    companion object {
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
        return ThermalState.fromRawValue(decoder.decodeInt())
    }
}

object UuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.util.UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}

@Serializable
class StreamingHistoryStream(
    @Serializable(with = UuidSerializer::class)
    var id: UUID = UUID.randomUUID(),
    var settings: SettingsStream,
    @Serializable(with = InstantSerializer::class)
    var startTime: Instant = Instant.now(),
    @Serializable(with = InstantSerializer::class)
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
}

private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

@Serializable(with = StreamingHistoryDatabaseSerializer::class)
class StreamingHistoryDatabase {
    var totalTime: MutableStateFlow<Duration> = MutableStateFlow(Duration.ZERO)
    var totalBytes: MutableStateFlow<Long> = MutableStateFlow(0L)
    var totalStreams: MutableStateFlow<Long> = MutableStateFlow(0L)
    var streams: MutableStateFlow<List<StreamingHistoryStream>> = MutableStateFlow(emptyList())

    companion object {
        fun fromString(settings: String): StreamingHistoryDatabase {
            return json.decodeFromString(StreamingHistoryDatabaseSerializer, settings)
        }
    }

    override fun toString(): String {
        return json.encodeToString(StreamingHistoryDatabaseSerializer, this)
    }
}

@Serializable
private class StreamingHistoryDatabaseSnapshot(
    val totalTime: Long = 0,
    val totalBytes: Long = 0,
    val totalStreams: Long = 0,
    val streams: List<StreamingHistoryStream> = emptyList(),
)

object StreamingHistoryDatabaseSerializer : KSerializer<StreamingHistoryDatabase> {
    private val snapshotSerializer = StreamingHistoryDatabaseSnapshot.serializer()

    override val descriptor: SerialDescriptor = snapshotSerializer.descriptor

    override fun serialize(encoder: Encoder, value: StreamingHistoryDatabase) {
        val snapshot = StreamingHistoryDatabaseSnapshot(
            totalTime = value.totalTime.value.inWholeMilliseconds,
            totalBytes = value.totalBytes.value,
            totalStreams = value.totalStreams.value,
            streams = value.streams.value,
        )
        encoder.encodeSerializableValue(snapshotSerializer, snapshot)
    }

    override fun deserialize(decoder: Decoder): StreamingHistoryDatabase {
        val snapshot = decoder.decodeSerializableValue(snapshotSerializer)
        return StreamingHistoryDatabase().apply {
            totalTime.value = snapshot.totalTime.milliseconds
            totalBytes.value = snapshot.totalBytes
            totalStreams.value = snapshot.totalStreams
            streams.value = snapshot.streams
        }
    }
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
