package com.moblin.android.various.storages

import android.util.Log
import com.moblin.android.various.utils.createAndGetDirectory
import java.io.File
import java.io.IOException

const val replayTransitionsStorageDirectory = "ReplayTransitions"

class ReplayTransitionsStorage {
    private var fileManager: File
    private var mediasUrl: File

    init {
        mediasUrl = createAndGetDirectory(replayTransitionsStorageDirectory)
        fileManager = mediasUrl
    }

    fun makePath(filename: String): File {
        return File(mediasUrl, filename)
    }

    fun add(filename: String, url: File) {
        runCatching { url.renameTo(makePath(filename)) }
    }

    fun remove(filename: String) {
        try {
            val target = makePath(filename)
            if (!target.delete()) {
                throw IOException("Remove failed for ${target.path}")
            }
        } catch (e: Exception) {
            Log.d("ReplayTransitionsStorage", "media-player-storage: Remove failed with error $e")
        }
    }
}
