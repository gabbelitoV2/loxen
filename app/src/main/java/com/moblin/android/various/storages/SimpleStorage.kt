package com.moblin.android.various.storages

import android.content.Context
import java.io.File
import android.util.Log

internal object SimpleStorageContext {
    val applicationContext: Context get() = com.moblin.android.AppDelegate.context
}

private fun setup(): File {
    val url = File(SimpleStorageContext.applicationContext.filesDir, "SimpleStorage")
    return try {
        if (!url.isDirectory && !url.mkdirs()) {
            Log.e("SimpleStorage", "Failed to create storage directory")
        }
        val testFile = File(url, ".init")
        testFile.writeBytes(ByteArray(0))
        testFile.delete()
        url
    } catch (e: Exception) {
        Log.e("SimpleStorage", "Failed to set up storage: $e")
        url
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
            0
        }
    }

    fun set(value: Int) {
        file.writeString(value.toString())
    }
}

private fun File.writeString(value: String) {
    try {
        val temporary = File(parentFile, ".$name.tmp")
        temporary.writeText(value)
        if (!temporary.renameTo(this)) {
            temporary.delete()
            writeText(value)
        }
    } catch (e: Exception) {
        Log.e("SimpleStorage", "Failed to write $name: $e")
    }
}

private fun File.readString(): String {
    return try {
        readText()
    } catch (e: Exception) {
        Log.e("SimpleStorage", "Failed to read $name: $e")
        ""
    }
}
