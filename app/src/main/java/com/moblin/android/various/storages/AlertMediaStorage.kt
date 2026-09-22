package com.moblin.android.various.storages

import com.moblin.android.various.utils.createAndGetDirectory
import java.io.File

const val alertsStorageDirectory = "Alerts"

class AlertVideoMediaStorage {
    private val mediasDir: File = createAndGetDirectory(alertsStorageDirectory, "Videos")

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

class AlertMediaStorage : FileStorage(alertsStorageDirectory) {
    val videos = AlertVideoMediaStorage()
}
