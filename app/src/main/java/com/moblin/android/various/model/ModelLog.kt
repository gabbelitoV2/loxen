package com.moblin.android.various.model

import com.moblin.android.common.various.appVersion
import com.moblin.android.common.various.formatDate
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val maximumFileLogLines = 100

private val mainScope = CoroutineScope(Dispatchers.Main)

private object logger {
    var handler: ((String) -> Unit)? = null
    var debugEnabled = false
}

fun createFileLog(): List<String> {
    val log = ArrayList<String>()
    log.ensureCapacity(maximumFileLogLines)
    log.add("")
    log.add("Version: ${appVersion()}")
    log.add("Date: ${formatDate(Instant.now())}")
    log.add("")
    return log
}

fun Model.setupLogging() {
    logger.handler = { message -> debugLog(message) }
    logger.debugEnabled = database.debug.debugLogging.value
}

fun Model.clearLog() {
    log.clear()
}

fun Model.formatLog(log: List<LogEntry>): String {
    var data = "Version: ${appVersion()}\n"
    data += "Debug: ${logger.debugEnabled}\n\n"
    data += log.joinToString(separator = "\n") { it.message }
    val file = File(System.getProperty("java.io.tmpdir"), "Moblin-log-${System.currentTimeMillis()}.txt")
    runCatching {
        file.writeText(data)
    }
    return file.absolutePath
}

fun Model.writeFileLogToFile() {
    logsStorage.write(fileLog)
    fileLog = emptyList()
}

fun Model.flushFileLogToFile() {
    logsStorage.flush()
}

private fun Model.debugLog(message: String) {
    mainScope.launch {
        if (log.size > database.debug.maximumLogLines) {
            log.removeFirst()
        }
        log.add(LogEntry(id = logId, message = message))
        logId += 1
        remoteControlLog(message)
        if (fileLog.size >= maximumFileLogLines) {
            writeFileLogToFile()
        }
        fileLog = fileLog + message
    }
}
