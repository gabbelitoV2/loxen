package com.moblin.android.various.utils

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Debug
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.ui.geometry.Size
import com.moblin.android.various.network.httpCall
import java.io.ByteArrayOutputStream
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import okhttp3.Request
import com.moblin.android.AppDelegate

fun randomBytes(length: Int): ByteArray = ByteArray(length) { Random.nextInt(0, 256).toByte() }

fun randomString(): String = Base64.getEncoder().encodeToString(randomBytes(64))

fun startBlockingThread(name: String, block: () -> Unit) {
    val thread = Thread(block, name)
    thread.priority = Thread.MAX_PRIORITY
    thread.start()
}

fun randomHumanString(): String = Base64.getEncoder()
    .encodeToString(randomBytes(15))
    .replace(Regex("[+/=]"), "")

fun String.removeAllWhitespaces(): String = replace(Regex("\\s"), "")

fun String.truncate(length: Int): String {
    if (this.length <= length) {
        return this
    }
    return take(max(length - 3, 0)) + "...".take(length)
}

fun randomName(): String {
    val colors = listOf("Black", "Red", "Green", "Yellow", "Blue", "Purple", "Cyan", "White")
    return colors.random()
}

fun openUrl(url: String) {
    Unit
}

private val thumbnails = mutableMapOf<String, Bitmap>()
private val thumbnailDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
private val thumbnailQueue = CoroutineScope(thumbnailDispatcher)
private val mainScope = CoroutineScope(Dispatchers.Main)

private fun createThumbnailInternal(path: String, offset: Double): Bitmap? {
    thumbnails[path]?.let { return it }
    return try {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            val timeUs = (offset * 1_000_000.0).toLong()
            val thumbnail = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (thumbnail != null) {
                thumbnails[path] = thumbnail
            }
            thumbnail
        } finally {
            retriever.release()
        }
    } catch (error: Exception) {
        Log.i("Utils", "Failed to create thumbnail with error $error")
        null
    }
}

fun createThumbnail(path: String, offset: Double = 0.0, onComplete: (Bitmap?) -> Unit) {
    thumbnailQueue.launch {
        val image = createThumbnailInternal(path, offset)
        mainScope.launch {
            onComplete(image)
        }
    }
}

fun currentPresentationTimeStamp(): Long = System.nanoTime() / 1000

fun utcTimeDeltaFromNow(to: Double): Double = to - System.currentTimeMillis() / 1000.0

fun emojiFlag(countryCode: String?): String {
    if (countryCode == null) {
        return ""
    }
    val base = 127_397
    val builder = StringBuilder()
    for (ch in countryCode) {
        builder.appendCodePoint(base + ch.code)
    }
    return builder.toString()
}

fun uploadImage(
    url: String,
    paramName: String,
    fileName: String,
    image: ByteArray,
    message: String?,
    onCompleted: ((Boolean) -> Unit)? = null
) {
    val boundary = UUID.randomUUID().toString()
    val request = Request.Builder()
        .url(url)
        .header("Content-Type", "multipart/form-data; boundary=$boundary")
        .build()
    val data = ByteArrayOutputStream()
    if (message != null) {
        data.write("\r\n--$boundary\r\n".toByteArray())
        data.write("content-disposition: form-data; name=\"content\"\r\n\r\n".toByteArray())
        data.write(message.toByteArray())
    }
    data.write("\r\n--$boundary\r\n".toByteArray())
    data.write(
        "content-disposition: form-data; name=\"$paramName\"; filename=\"$fileName\"\r\n\r\n".toByteArray()
    )
    data.write("content-type: image/jpeg\r\n\r\n".toByteArray())
    data.write(image)
    data.write("\r\n--$boundary--\r\n".toByteArray())
    httpCall(request = request, body = data.toByteArray()) { response ->
        onCompleted?.invoke(response != null)
    }
}

fun Size.minimum(): Float = min(height, width)

fun Size.maximum(): Float = max(height, width)

class ResourceUsage {
    private var previousTime: Long? = null
    private var previousUsage: Any? = null
    private var previousCpuTicks: List<List<UInt>>? = null
    private var appCpuUsage: Float = 0f
    private var cpuUsage: Float = 0f
    private var memoryUsage: ULong = 0u

    fun update(now: Long) {
        updateAppCpuUsage(now)
        updateCpuUsage()
        updateMemoryUsage()
    }

    fun getAppCpuUsage(): Int = appCpuUsage.toInt()

    fun getCpuUsage(): Int = cpuUsage.toInt()

    fun getMemoryUsage(): Int = memoryUsage.toInt()

    private fun updateAppCpuUsage(now: Long) {
        Unit
    }

    private fun updateCpuUsage() {
        Unit
    }

    private fun updateMemoryUsage() {
        val info = Debug.MemoryInfo()
        Debug.getMemoryInfo(info)
        memoryUsage = (info.totalPss.toLong() / 1024L / 1024L).toULong()
    }
}

fun generateQrCode(from: String): Bitmap? = null

fun tryGetToastSubTitle(error: Throwable): String? = null

fun secondsToCMTime(seconds: Double): Long = (seconds * 1000.0).toLong()

fun ByteArray.writeUInt16(value: UShort, offset: Int) {
    this[offset + 0] = ((value.toInt() shr 8) and 0xFF).toByte()
    this[offset + 1] = (value.toInt() and 0xFF).toByte()
}

fun ByteArray.writeUInt32(value: UInt, offset: Int) {
    this[offset + 0] = ((value shr 24) and 0xFFu).toInt().toByte()
    this[offset + 1] = ((value shr 16) and 0xFFu).toInt().toByte()
    this[offset + 2] = ((value shr 8) and 0xFFu).toInt().toByte()
    this[offset + 3] = (value and 0xFFu).toInt().toByte()
}

fun ByteArray.readUInt16(offset: Int): UShort {
    val value = ((this[offset + 0].toInt() and 0xFF) shl 8) or
        (this[offset + 1].toInt() and 0xFF)
    return value.toUShort()
}

fun ByteArray.readUInt32(offset: Int): UInt {
    val value = ((this[offset + 0].toLong() and 0xFFL) shl 24) or
        ((this[offset + 1].toLong() and 0xFFL) shl 16) or
        ((this[offset + 2].toLong() and 0xFFL) shl 8) or
        (this[offset + 3].toLong() and 0xFFL)
    return value.toUInt()
}

interface Named {
    val name: String
}

fun makeUniqueName(name: String, existingNames: List<Named>): String {
    val names = existingNames.map { it.name }
    if (!names.contains(name)) {
        return name
    }
    var number = 1
    while (true) {
        val nameCandidate = "$name $number"
        if (!names.contains(nameCandidate)) {
            return nameCandidate
        }
        number += 1
    }
}

fun createSpeechSynthesizer(): TextToSpeech = TextToSpeech(AppDelegate.context) { }

fun makeRecordingPath(recordingPath: ByteArray): String? =
    null
fun zoomToFieldOfView(zoom: Float, zoomOne: Float = (PI / 2).toFloat()): Float =
    2 * atan(tan(zoomOne / 2) / zoom)

fun fieldOfViewToZoom(fieldOfView: Float, zoomOne: Float = (PI / 2).toFloat()): Float =
    tan(zoomOne / 2) / tan(fieldOfView / 2)

fun Locale.name(): String {
    val name = getDisplayName()
    return if (name.isNullOrEmpty()) "Unknown" else name
}

fun MediaMetadataRetriever.duration(): Double {
    val value = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    return value?.toDoubleOrNull()?.div(1000.0) ?: 0.0
}

fun loadStringResource(name: String, ext: String): String =
    ""
fun loadResource(name: String, ext: String): ByteArray =
    ByteArray(0)
fun <T> MutableList<T>.truncate(length: Int, create: () -> T) {
    while (size < length) {
        add(create())
    }
    while (size > length) {
        removeAt(size - 1)
    }
}

fun UUID.add(data: ByteArray): UUID {
    val msb = this.mostSignificantBits
    val lsb = this.leastSignificantBits
    val tuple = ByteArray(16)
    for (i in 0 until 8) {
        tuple[i] = ((msb ushr (8 * (7 - i))) and 0xFFL).toByte()
    }
    for (i in 0 until 8) {
        tuple[8 + i] = ((lsb ushr (8 * (7 - i))) and 0xFFL).toByte()
    }
    val bytes = ByteArray(16) { tuple[15 - it] }
    for ((index, value) in data.reversed().withIndex()) {
        bytes[index % 16] = (bytes[index % 16] + value).toByte()
    }
    val outTuple = ByteArray(16) { bytes[15 - it] }
    var newMsb = 0L
    var newLsb = 0L
    for (i in 0 until 8) {
        newMsb = (newMsb shl 8) or outTuple[i].toUByte().toLong()
    }
    for (i in 0 until 8) {
        newLsb = (newLsb shl 8) or outTuple[8 + i].toUByte().toLong()
    }
    return UUID(newMsb, newLsb)
}

fun clockAsMinutesAndSeconds(clock: String): Pair<Int, Int> {
    val parts = clock.split(":")
    if (parts.size == 2) {
        val minutes = parts[0].toIntOrNull()
        val seconds = parts[1].toIntOrNull()
        if (minutes != null && seconds != null) {
            return Pair(minutes, seconds)
        }
    }
    return Pair(0, 0)
}

fun <T> List<String>.withCPointers(body: (Array<ByteArray?>) -> T): T {
    val pointers = arrayOfNulls<ByteArray>(size)
    for (index in indices) {
        pointers[index] = this[index].toByteArray(Charsets.UTF_8) + byteArrayOf(0)
    }
    return body(pointers)
}

class TimeStampRebaser {
    private var firstPresentationTimeStamp: Double = Double.NaN

    fun rebase(presentationTimeStamp: Double): Double? {
        if (firstPresentationTimeStamp.isNaN()) {
            firstPresentationTimeStamp = presentationTimeStamp
        }
        val value = presentationTimeStamp - firstPresentationTimeStamp
        if (!(value > 0)) {
            return null
        }
        return value
    }
}

private val filenameDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss").withZone(ZoneId.systemDefault())

fun formatFilenameDateAndTime(date: Instant? = null): String =
    filenameDateFormatter.format(date ?: Instant.now()).replace(Regex("\\s+"), "_")

private val filenameDateFormatterIsoish: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss-SSS", Locale.US).withZone(ZoneOffset.UTC)

fun formatFilenameDateAndTimeIsoish(date: Instant? = null): String =
    filenameDateFormatterIsoish.format(date ?: Instant.now()).replace(Regex("\\s+"), "_")

fun extractSrtStreamId(url: String): String? {
    val query = runCatching { URI(url).rawQuery }.getOrNull() ?: return null
    for (part in query.split("&")) {
        val pair = part.split("=", limit = 2)
        if (pair.size == 2 && pair[0] == "streamid") {
            return pair[1]
        }
    }
    return null
}

fun stringFromCArray(cArray: ByteArray): String {
    val end = cArray.indexOf(0).let { if (it == -1) cArray.size else it }
    return String(cArray, 0, end, Charsets.UTF_8)
}
