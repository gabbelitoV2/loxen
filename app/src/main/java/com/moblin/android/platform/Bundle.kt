package com.moblin.android.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.moblin.android.AppDelegate
import com.moblin.android.platform.loxen.Loxen
import java.io.File
import java.io.InputStream

object Bundle {
    private const val TAG = "Bundle"

    private fun assetPath(name: String, ext: String?): String =
        if (ext.isNullOrEmpty()) name else "$name.$ext"

    fun open(name: String, ext: String? = null): InputStream? =
        runCatching { AppDelegate.context.assets.open(assetPath(name, ext)) }
            .onFailure { Log.i(TAG, "Missing asset ${assetPath(name, ext)}") }
            .getOrNull()

    fun readBytes(name: String, ext: String? = null): ByteArray? = open(name, ext)?.use { it.readBytes() }

    fun url(name: String, ext: String? = null): String? {
        val path = assetPath(name, ext)
        val file = File(AppDelegate.context.cacheDir, "bundle/$path")
        if (!file.exists()) {
            val input = open(name, ext) ?: return null
            file.parentFile?.mkdirs()
            input.use { source -> file.outputStream().use { source.copyTo(it) } }
        }
        return file.absolutePath
    }

    fun image(name: String): Bitmap? {
        for (ext in listOf("png", "jpg", "jpeg")) {
            val bytes = readBytes(Loxen.imagePath(name), ext) ?: continue
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        return null
    }
}
