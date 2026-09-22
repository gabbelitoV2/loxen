package com.moblin.android.various.utils

import android.util.Log
import java.io.File
import java.nio.file.Files
import java.util.UUID

private const val TAG = "FileSystemUtils"

private val temporaryDirectory: File
    get() = File(System.getProperty("java.io.tmpdir") ?: "/data/local/tmp")

private val documentsDirectory: File
    get() = temporaryDirectory.parentFile ?: File("/data/local/tmp")

val File.attributes: Map<String, Any>?
    get() = try {
        Files.readAttributes(toPath(), "*")
    } catch (e: Exception) {
        Log.i(TAG, "file-system: Failed to get attributes for file $this")
        null
    }

val File.fileSize: Long
    get() = attributes?.get("size") as? Long ?: 0L

fun File.remove() {
    runCatching { delete() }
}

fun File.exists(): Boolean = Files.exists(toPath())

object FileManager {
    fun ids(directory: String): List<UUID> {
        val ids = mutableListOf<UUID>()
        val files = runCatching { File(directory).list() }.getOrNull() ?: emptyArray<String>()
        for (file in files) {
            val id = runCatching { UUID.fromString(file) }.getOrNull() ?: continue
            ids.add(id)
        }
        return ids
    }

    fun idsBeforeDot(directory: String): List<UUID> {
        val ids = mutableListOf<UUID>()
        val files = runCatching { File(directory).list() }.getOrNull() ?: emptyArray<String>()
        for (file in files) {
            val parts = file.split(".")
            if (parts.size <= 1) {
                continue
            }
            val id = runCatching { UUID.fromString(parts[0]) }.getOrNull() ?: continue
            ids.add(id)
        }
        return ids
    }
}

fun getAvailableDiskSpace(): Long? = runCatching { temporaryDirectory.usableSpace }.getOrNull()

fun deleteTrash() {
    val folders = listOf(
        temporaryDirectory,
        File(documentsDirectory, ".Trash"),
    )
    for (folder in folders) {
        val paths = runCatching { folder.list() }.getOrNull() ?: continue
        for (path in paths) {
            File(folder, path).remove()
        }
    }
}

fun createAndGetDirectory(vararg name: String): File {
    var directory = documentsDirectory
    for (component in name) {
        directory = File(directory, component)
    }
    directory.mkdirs()
    return directory
}

const val moblinSettingsFileType = "com.eerimoq.moblin.settings"
