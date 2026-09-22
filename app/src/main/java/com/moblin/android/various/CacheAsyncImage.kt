package com.moblin.android.various

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI

private class Cache {
    private val cache: MutableMap<URI, ImageBitmap> = mutableMapOf()

    @Synchronized
    fun get(url: URI): ImageBitmap? = cache[url]

    @Synchronized
    fun set(url: URI, image: ImageBitmap) {
        cache[url] = image
    }
}

private val cache = Cache()

private val httpClient = OkHttpClient()

private suspend fun loadImageBitmap(url: URI): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val request = Request.Builder().url(url.toString()).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@use null
            }
            val bytes = response.body?.bytes() ?: return@use null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }.getOrNull()
}

@Composable
fun CacheAsyncImage(
    url: URI,
    scale: Float = 1f,
    content: @Composable (ImageBitmap) -> Unit,
    placeholder: @Composable () -> Unit,
) {
    var image by remember(url) { mutableStateOf(cache.get(url)) }
    LaunchedEffect(url) {
        if (image == null) {
            val loaded = loadImageBitmap(url)
            if (loaded != null) {
                cache.set(url, loaded)
                image = loaded
            }
        }
    }
    val current = image
    if (current != null) {
        content(current)
    } else {
        placeholder()
    }
}
