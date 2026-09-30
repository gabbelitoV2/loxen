package com.moblin.android

import com.moblin.android.integrations.emotes.Emote
import com.moblin.android.integrations.emotes.Emotes
import com.moblin.android.various.ChatPostSegment
import android.os.Looper
import java.io.IOException
import java.time.Duration
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource
import org.robolectric.Shadows.shadowOf

private class BundleToken

fun isEqual(actual: Double, expected: Double, epsilon: Double): Boolean {
    return abs(actual - expected) < epsilon
}

fun isEqual(actual: Float, expected: Float, epsilon: Float): Boolean {
    return abs(actual - expected) < epsilon
}

fun areEqual(actual: DoubleArray, expected: DoubleArray, epsilon: Double): Boolean {
    if (actual.size != expected.size) {
        return false
    }
    for (index in 0 until actual.size) {
        if (!isEqual(actual[index], expected[index], epsilon)) {
            return false
        }
    }
    return true
}

fun areEqual(actual: FloatArray, expected: FloatArray, epsilon: Float): Boolean {
    if (actual.size != expected.size) {
        return false
    }
    for (index in 0 until actual.size) {
        if (!isEqual(actual[index], expected[index], epsilon)) {
            return false
        }
    }
    return true
}

class MessageQueue<Message> {
    private val channel = Channel<Message>(Channel.UNLIMITED)

    fun put(message: Message) {
        channel.trySend(message)
    }

    suspend fun get(): Message {
        return kotlinx.coroutines.withTimeout(10_000) { channel.receive() }
    }
}

fun waitUntil(timeout: Duration, condition: () -> Boolean): Boolean {
    val startTime = System.nanoTime()
    while (!condition()) {
        if (Duration.ofNanos(System.nanoTime() - startTime) > timeout) {
            return false
        }
        Thread.sleep(20)
    }
    return true
}

class TemporaryDirectory : AutoCloseable {
    val url: java.io.File

    init {
        url = java.io.File(AppDelegate.context.cacheDir, java.util.UUID.randomUUID().toString())
        if (!url.mkdirs() && !url.isDirectory) {
            throw IOException("Could not create temporary directory: $url")
        }
    }

    override fun close() {
        url.deleteRecursively()
    }
}

@Throws(IOException::class)
fun readMainFile(name: String, suffix: String): ByteArray {
    val path = "/$name.$suffix"
    val stream = BundleToken::class.java.getResourceAsStream(path)
        ?: throw IOException("Resource not found: $path")
    return stream.use { it.readBytes() }
}

@Throws(IOException::class)
fun readTestFile(name: String, suffix: String): ByteArray {
    val path = "/$name.$suffix"
    val stream = BundleToken::class.java.getResourceAsStream(path)
        ?: throw IOException("Resource not found: $path")
    return stream.use { it.readBytes() }
}

fun makeEmotes(names: List<String>): Emotes {
    val emotes = Emotes()
    val byName = mutableMapOf<String, Emote>()
    for (name in names) {
        byName[name] = Emote(url = "https://emotes.example.com/$name")
    }
    emotes.addEmotes(byName)
    return emotes
}

fun texts(segments: List<ChatPostSegment>): List<String?> {
    return segments.map { it.text }
}

fun emoteNames(segments: List<ChatPostSegment>): List<String?> {
    return segments.map { it.url?.still?.substringAfterLast('/') }
}

fun runMainTest(block: suspend CoroutineScope.() -> Unit) {
    val looper = shadowOf(Looper.getMainLooper())
    val test = CoroutineScope(Dispatchers.Main).async(block = block)
    var last = System.nanoTime()
    while (!test.isCompleted) {
        Thread.sleep(1)
        val now = System.nanoTime()
        looper.idleFor(Duration.ofNanos(now - last))
        last = now
    }
    runBlocking { test.await() }
}

class MainLooperDrainRule : ExternalResource() {
    override fun after() {
        shadowOf(Looper.getMainLooper()).idle()
    }
}
