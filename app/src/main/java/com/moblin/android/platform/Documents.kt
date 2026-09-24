package com.moblin.android.platform

import android.content.Context
import android.util.Log
import com.moblin.android.AppDelegate
import java.io.File

object Documents {
    private const val TAG = "Documents"
    private val legacyDataDirectoryFolders = listOf(
        "Alerts",
        "Images",
        "Logs",
        "Medias",
        "PNGTuber",
        "Recordings",
        "ReplayTransitions",
        "Replays",
        "VTuber",
    )
    private val legacyFilesDirectoryFiles = listOf(
        "controlBarBackgroundImage.img",
        "faceBackgroundImage.img",
        "stealthModeImage.img",
    )
    private var preparedDirectory: File? = null

    val directory: File
        get() = directory(AppDelegate.context)

    @Synchronized
    internal fun directory(context: Context): File {
        val documents = documentsDirectory(context)
        if (documents != preparedDirectory) {
            setup(context)
            preparedDirectory = documents
        }
        return documents
    }

    internal fun documentsDirectory(context: Context): File = File(context.filesDir, "Documents")

    internal fun migratedMarker(context: Context): File = File(context.filesDir, ".documentsMigrated")

    internal fun legacyDataDirectory(context: Context): File? = context.cacheDir.parentFile

    internal fun setup(context: Context): File {
        val documents = documentsDirectory(context)
        documents.mkdirs()
        val marker = migratedMarker(context)
        if (!marker.exists() && migrate(context, documents)) {
            runCatching { marker.createNewFile() }
                .onFailure { Log.e(TAG, "Failed to create $marker: $it") }
        }
        return documents
    }

    private fun migrate(context: Context, documents: File): Boolean {
        var done = true
        val dataDirectory = legacyDataDirectory(context)
        if (dataDirectory != null) {
            for (name in legacyDataDirectoryFolders) {
                val source = File(dataDirectory, name)
                if (source.isDirectory) {
                    done = move(source, File(documents, name)) && done
                }
            }
        }
        for (name in legacyFilesDirectoryFiles) {
            val source = File(context.filesDir, name)
            if (source.isFile) {
                done = move(source, File(documents, name)) && done
            }
        }
        return done
    }

    private fun move(source: File, target: File): Boolean {
        if (!target.exists()) {
            if (source.renameTo(target)) {
                Log.i(TAG, "Moved $source to $target")
                return true
            }
            Log.e(TAG, "Failed to move $source to $target")
            return false
        }
        if (!source.isDirectory || !target.isDirectory) {
            Log.i(TAG, "Kept $target and left $source")
            return true
        }
        var done = true
        for (name in source.list().orEmpty()) {
            done = move(File(source, name), File(target, name)) && done
        }
        source.delete()
        return done
    }
}
