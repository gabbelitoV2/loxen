package com.moblin.android.various.storages

import com.moblin.android.platform.log.Log
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.UUIDSerializer
import com.moblin.android.platform.codable.codableJson
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.various.settings.SettingsReplay
import com.moblin.android.various.utils.createAndGetDirectory
import com.moblin.android.various.utils.remove
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject

private const val tag = "ReplaysStorage"

private fun <T> JsonObject.decodeRequired(key: String, serializer: KSerializer<T>): T {
    val element = this[key] ?: throw SerializationException("Missing key '$key'")
    return codableJson.decodeFromJsonElement(serializer, element)
}

@Serializable(with = ReplaySettings.Serializer::class)
class ReplaySettings(
    var id: UUID = UUID.randomUUID(),
    var duration: Double = 0.0,
    var start: Double = 20.0,
    var stop: Double = SettingsReplay.stop,
) {
    fun name(): String {
        return "$id.mp4"
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

    fun encode(): JsonObject = encodeContainer {
        encode("id", id, UUIDSerializer)
        encode("duration", duration)
        encode("start", start)
        encode("stop", stop)
    }

    companion object {
        fun decode(container: JsonObject): ReplaySettings {
            return ReplaySettings(
                id = container.decodeRequired("id", UUIDSerializer),
                duration = container.decodeRequired("duration", Double.serializer()),
                start = container.decodeRequired("start", Double.serializer()),
                stop = container.decodeRequired("stop", Double.serializer()),
            )
        }
    }

    object Serializer : KSerializer<ReplaySettings> by JsonObjectSerializer(
        "ReplaySettings",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = ReplaysDatabase.Serializer::class)
class ReplaysDatabase {
    val replays = MutableStateFlow<List<ReplaySettings>>(emptyList())

    fun setReplays(value: List<ReplaySettings>) {
        replays.value = value
    }

    fun encode(): JsonObject = encodeContainer {
        encode("replays", replays, ListSerializer(ReplaySettings.serializer()))
    }

    override fun toString(): String {
        return codableJson.encodeToString(Serializer, this)
    }

    companion object {
        fun decode(container: JsonObject): ReplaysDatabase {
            val database = ReplaysDatabase()
            database.replays.value = container.decode("replays", ListSerializer(ReplaySettings.serializer()), emptyList())
            return database
        }

        fun fromString(settings: String): ReplaysDatabase {
            return codableJson.decodeFromString(Serializer, settings)
        }
    }

    object Serializer : KSerializer<ReplaysDatabase> by JsonObjectSerializer(
        "ReplaysDatabase",
        { it.encode() },
        { decode(it) },
    )
}

private val defaultStorage = SimpleStringStorage("replays")

class ReplaysStorage(directory: File? = null) {
    private val storage: SimpleStringStorage
    private val directory: File
    private var realDatabase = ReplaysDatabase()

    val database: ReplaysDatabase
        get() = realDatabase

    init {
        if (directory != null) {
            storage = SimpleStringStorage(
                "replays",
                createAndGetDirectory("Database", root = directory),
            )
            this.directory = createAndGetDirectory("Replays", root = directory)
        } else {
            storage = defaultStorage
            this.directory = createAndGetDirectory("Replays")
        }
    }

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

    fun store() {
        try {
            storage.set(realDatabase.toString())
        } catch (e: Exception) {
            Log.i(tag, "replays-storage: Failed to store.")
        }
    }

    fun createReplay(): ReplaySettings {
        return ReplaySettings()
    }

    fun append(replay: ReplaySettings) {
        while (isFull()) {
            val last = database.replays.value.lastOrNull() ?: break
            database.setReplays(database.replays.value.dropLast(1))
            url(last).remove()
        }
        database.setReplays(listOf(replay) + database.replays.value)
    }

    fun isFull(): Boolean {
        return database.replays.value.size > 499
    }

    fun defaultStorageDirectory(): File {
        return directory
    }

    fun url(replay: ReplaySettings): File {
        return File(directory, replay.name())
    }

    private fun cleanup() {
        database.setReplays(database.replays.value.filter { url(it).exists() })
        val knownNames = database.replays.value.map { it.name() }.toSet()
        val directory = directory
        CoroutineScope(Dispatchers.IO).launch {
            val files = directory.walkTopDown().filter { it.isFile }.toList()
            for (file in files) {
                if (!knownNames.contains(file.name)) {
                    Log.d(tag, "replays-storage: Removing unused file $file")
                    file.remove()
                }
            }
        }
    }

    private fun tryLoadAndMigrate(settings: String) {
        realDatabase = ReplaysDatabase.fromString(settings)
        migrateFromOlderVersions()
    }

    private fun migrateFromOlderVersions() {}
}
