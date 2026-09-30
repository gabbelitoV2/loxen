package com.moblin.android.various.storages

import com.moblin.android.platform.log.Log
import com.moblin.android.various.utils.createAndGetDirectory
import java.io.File
import java.nio.file.Files
import java.util.UUID

const val mediaPlayerStorageDirectory = "Medias"

class MediaPlayerStorage {
    private val mediasUrl: File = createAndGetDirectory(mediaPlayerStorageDirectory)

    fun makePath(id: UUID): File {
        return com.moblin.android.platform.core.uuidFile(mediasUrl, id, ".mp4")
    }

    fun ids(): List<UUID> {
        return mediasUrl.listFiles()?.mapNotNull { file ->
            runCatching { UUID.fromString(file.name.substringBefore('.')) }.getOrNull()
        } ?: emptyList()
    }

    fun add(id: UUID, url: File) {
        runCatching { Files.move(url.toPath(), makePath(id).toPath()) }
    }

    fun remove(id: UUID) {
        try {
            Files.delete(makePath(id).toPath())
        } catch (e: Exception) {
            Log.i("MediaPlayerStorage", "media-player-storage: Remove failed with error $e")
        }
    }
}
