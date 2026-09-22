package com.moblin.android.various.storages

import com.moblin.android.various.utils.createAndGetDirectory
import com.moblin.android.various.utils.formatFilenameDateAndTimeIsoish
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

private const val maximumNumberOfLogFiles = 10
private const val maximumLogFileSizeBytes: Long = 10L * 1024 * 1024
private val queue = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

class LogsStorage {
    private var logsUrl: File = createAndGetDirectory("Logs")
    private var currentFileHandle: FileOutputStream? = null
    private var currentFileSize: Long = 0
    private val scope = CoroutineScope(queue)

    fun storageDirectory(): File {
        return logsUrl
    }

    fun write(lines: List<String>) {
        if (lines.isEmpty()) {
            return
        }
        scope.launch {
            writeInternal(lines)
        }
    }

    fun flush() {
        scope.launch {
            flushInternal()
        }
    }

    private fun writeInternal(lines: List<String>) {
        val blob = (lines.joinToString("\n") + "\n").toByteArray()
        val blobSize = blob.size.toLong()
        if (currentFileHandle == null) {
            val files = logFiles()
            val latestFile = files.lastOrNull()
            if (latestFile != null && latestFile.length() + blobSize < maximumLogFileSizeBytes) {
                setCurrentFile(latestFile)
            }
            if (currentFileHandle == null) {
                openNewFile()
            }
        } else if (currentFileSize + blobSize >= maximumLogFileSizeBytes) {
            openNewFile()
        }
        val handle = currentFileHandle ?: return
        handle.write(blob)
        currentFileSize += blobSize
    }

    private fun flushInternal() {
        runCatching { currentFileHandle?.fd?.sync() }
    }

    private fun logFiles(): List<File> {
        val files = logsUrl.listFiles()?.filter { !it.isHidden } ?: emptyList()
        val logUrls = mutableListOf<File>()
        for (url in files) {
            if (url.name.startsWith("Log_")) {
                logUrls.add(url)
            } else {
                runCatching { url.deleteRecursively() }
            }
        }
        return logUrls.sortedBy { it.name }
    }

    private fun openNewFile() {
        closeCurrentFile()
        val files = logFiles().toMutableList()
        val fileUrl = File(logsUrl, "Log_${formatFilenameDateAndTimeIsoish()}.txt")
        while (files.size >= maximumNumberOfLogFiles) {
            runCatching { files.removeAt(0).deleteRecursively() }
        }
        runCatching { fileUrl.createNewFile() }
        setCurrentFile(fileUrl)
    }

    private fun closeCurrentFile() {
        runCatching { currentFileHandle?.close() }
        currentFileHandle = null
        currentFileSize = 0
    }

    private fun setCurrentFile(url: File) {
        currentFileHandle = runCatching { FileOutputStream(url, true) }.getOrNull()
        currentFileSize = url.length()
    }
}
