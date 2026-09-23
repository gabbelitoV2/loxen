package com.moblin.android.various.storages

import android.util.Log
import com.moblin.android.various.settings.SettingsReplay
import com.moblin.android.various.utils.createAndGetDirectory
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

private const val tag = "ReplaysStorage"

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private object ReplaysStorageUuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.util.UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

private fun getReplaysDirectory(): File {
    return createAndGetDirectory("Replays")
}

@Serializable
class ReplaySettings(
    @Serializable(with = ReplaysStorageUuidSerializer::class) var id: UUID = UUID.randomUUID(),
    var duration: Double = 0.0,
    var start: Double = 20.0,
    var stop: Double = SettingsReplay.stop,
) {
    fun name(): String {
        return "$id.mp4"
    }

    fun url(): File {
        return File(getReplaysDirectory(), name())
    }

    fun thumbnailOffset(): Double {
        return maxOf(startFromVideoStart(), 0.0)
    }

    fun startFromEnd(): Double {
        return SettingsReplay.stop - start
    }

    private fun stopFromEnd(): Double {
        return SettingsReplay.stop - stop
    }

    fun startFromVideoStart(): Double {
        return duration - startFromEnd()
    }

    fun stopFromVideoStart(): Double {
        return duration - stopFromEnd()
    }
}

@Serializable
private class ReplaysDatabaseDto(
    @SerialName("replays") val replays: List<ReplaySettings> = emptyList(),
)

class ReplaysDatabase {
    val replays = MutableStateFlow<List<ReplaySettings>>(emptyList())

    fun setReplays(value: List<ReplaySettings>) {
        replays.value = value
    }

    override fun toString(): String {
        return json.encodeToString(
            ReplaysDatabaseDto.serializer(),
            ReplaysDatabaseDto(replays.value),
        )
    }

    companion object {
        fun fromString(settings: String): ReplaysDatabase {
            val dto = json.decodeFromString(ReplaysDatabaseDto.serializer(), settings)
            val database = ReplaysDatabase()
            database.setReplays(dto.replays)
            return database
        }
    }
}

private val storage = SimpleStringStorage("replays")

class ReplaysStorage {
    private var realDatabase = ReplaysDatabase()

    val database: ReplaysDatabase
        get() = realDatabase

    fun load() {
        try {
            tryLoadAndMigrate(storage.get())
        } catch (e: Exception) {
            Log.i(tag, "replays-storage: Failed to load with error $e. Using default.")
            realDatabase = ReplaysDatabase()
        }
        cleanup()
    }

    fun delete(id: UUID) {
        database.setReplays(database.replays.value.filterNot { it.id == id })
    }

    private fun cleanup() {
        database.setReplays(database.replays.value.filter { it.url().exists() })
        val files = getReplaysDirectory().walkTopDown().filter { it.isFile }.toList()
        for (file in files) {
            val isUsed = database.replays.value.any { it.url().canonicalFile == file.canonicalFile }
            if (!isUsed) {
                Log.d(tag, "replays-storage: Removing unused file $file")
                file.delete()
            }
        }
    }

    private fun tryLoadAndMigrate(settings: String) {
        realDatabase = ReplaysDatabase.fromString(settings)
        migrateFromOlderVersions()
    }

    fun store() {
        try {
            storage.set(realDatabase.toString())
        } catch (e: Exception) {
            Log.i(tag, "replays-storage: Failed to store.")
        }
    }

    private fun migrateFromOlderVersions() {}

    fun createReplay(): ReplaySettings {
        return ReplaySettings()
    }

    fun append(replay: ReplaySettings) {
        while (isFull()) {
            val last = database.replays.value.lastOrNull() ?: break
            database.setReplays(database.replays.value.dropLast(1))
            last.url().delete()
        }
        database.setReplays(listOf(replay) + database.replays.value)
    }

    fun isFull(): Boolean {
        return database.replays.value.size > 499
    }

    fun defaultStorageDirectory(): File {
        return getReplaysDirectory()
    }
}
