package com.moblin.android.various.storages

import android.content.Context
import java.io.File
import kotlin.system.exitProcess

internal object SimpleStorageContext {
    val applicationContext: Context get() = com.moblin.android.AppDelegate.context
}

private fun setup(): File {
    val url = File(SimpleStorageContext.applicationContext.filesDir, "SimpleStorage")
    return try {
        if (!url.isDirectory && !url.mkdirs()) {
            exitProcess(0)
        }
        val testFile = File(url, ".init")
        testFile.writeBytes(ByteArray(0))
        testFile.delete()
        url
    } catch (e: Exception) {
        exitProcess(0)
    }
}

private val directory: File by lazy { setup() }

class SimpleStringStorage(key: String) {
    private val file: File = File(directory, key)

    init {
        if (!file.exists()) {
            val context = SimpleStorageContext.applicationContext
            val preferences = context.getSharedPreferences(
                "${context.packageName}_preferences",
                Context.MODE_PRIVATE
            )
            set(preferences.getString(key, "") ?: "")
        }
    }

    fun get(): String {
        return file.readString()
    }

    fun set(value: String) {
        file.writeString(value)
    }
}

class SimpleIntStorage(key: String) {
    private val file: File = File(directory, key)

    init {
        if (!file.exists()) {
            val context = SimpleStorageContext.applicationContext
            val preferences = context.getSharedPreferences(
                "${context.packageName}_preferences",
                Context.MODE_PRIVATE
            )
            set(preferences.getInt(key, 0))
        }
    }

    fun get(): Int {
        val value = file.readString().toIntOrNull()
        return if (value != null) {
            value
        } else {
            exitProcess(0)
        }
    }

    fun set(value: Int) {
        file.writeString(value.toString())
    }
}

private fun File.writeString(value: String) {
    try {
        writeText(value)
    } catch (e: Exception) {
        exitProcess(0)
    }
}

private fun File.readString(): String {
    return try {
        readText()
    } catch (e: Exception) {
        exitProcess(0)
    }
}
