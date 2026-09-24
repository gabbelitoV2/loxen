package com.moblin.android.various.storages

import com.moblin.android.various.utils.createAndGetDirectory
import java.io.File
import java.util.UUID

const val alertsStorageDirectory = "Alerts"

fun createAlertVideosDirectory(): File {
    return createAndGetDirectory(alertsStorageDirectory, "Videos")
}

class AlertVideoMediaStorage {
    private val mediasDir: File = createAlertVideosDirectory()

    fun makePath(filename: String): File {
        return File(mediasDir, filename)
    }

    fun add(filename: String, url: File) {
        runCatching { url.renameTo(makePath(filename)) }
    }

    fun remove(filename: String) {
        runCatching { makePath(filename).delete() }
    }
}

class AlertMediaStorage {
    private val storage = FileStorage(alertsStorageDirectory)
    val videos = AlertVideoMediaStorage()

    fun makePath(id: UUID): File = storage.makePath(id)

    fun ids(): List<UUID> = storage.ids()

    fun add(id: UUID, url: File) = storage.add(id, url)

    fun remove(id: UUID) = storage.remove(id)

    fun write(id: UUID, data: ByteArray) = storage.write(id, data)

    fun write(id: UUID, url: File) = storage.write(id, url)

    fun read(id: UUID): ByteArray? = storage.read(id)

    fun tryRead(id: UUID): ByteArray? = storage.tryRead(id)
}
