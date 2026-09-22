package com.moblin.android.various

import android.util.Log
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class EasyLogger {
    var handler: ((String) -> Unit)? = null
    var debugEnabled: Boolean = false

    private fun makeTimestamp(): String {
        return timestampFormatter.format(Instant.now().atZone(ZoneId.systemDefault()))
    }

    fun debug(message: () -> String) {
        if (debugEnabled) {
            log(message())
        }
    }

    fun info(message: String) {
        log(message)
    }

    private fun log(message: String) {
        val line = "${makeTimestamp()} $message"
        Log.d(TAG, line)
        handler?.invoke(line)
    }

    companion object {
        private const val TAG = "EasyLogger"

        private val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    }
}

val logger = EasyLogger()
