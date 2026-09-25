package com.moblin.android.platform.speech

import android.content.Intent
import android.media.AudioFormat
import android.media.MediaFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.moblin.android.media.MediaSample
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import org.robolectric.Shadows.shadowOf

internal class FakeSpeechEngine(val onDevice: Boolean) : SpeechRecognitionEngine {
    val intents = mutableListOf<Intent>()
    val listeners = mutableListOf<RecognitionListener>()
    val modelDownloads = mutableListOf<Intent>()
    var modelDownloadsSent = 0
    var modelDownloadsAfterDestroy = 0
    var stopListeningCount = 0
    var cancelCount = 0
    var destroyed = false

    val intent: Intent
        get() = intents.last()

    val listener: RecognitionListener
        get() = listeners.last()

    val audioSource: ParcelFileDescriptor?
        get() = intent.getParcelableExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, ParcelFileDescriptor::class.java)

    override fun startListening(intent: Intent, listener: RecognitionListener) {
        intents.add(intent)
        listeners.add(listener)
    }

    override fun stopListening() {
        stopListeningCount += 1
    }

    override fun cancel() {
        cancelCount += 1
    }

    override fun destroy() {
        destroyed = true
    }

    override fun triggerModelDownload(intent: Intent) {
        modelDownloads.add(intent)
        Handler(Looper.getMainLooper()).post {
            if (destroyed) {
                modelDownloadsAfterDestroy += 1
            } else {
                modelDownloadsSent += 1
            }
        }
    }

    fun partial(vararg texts: String) {
        listener.onPartialResults(results(*texts))
    }

    fun final(vararg texts: String) {
        listener.onResults(results(*texts))
    }

    fun error(code: Int) {
        listener.onError(code)
    }

    private fun results(vararg texts: String): Bundle {
        val bundle = Bundle()
        bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, ArrayList(texts.toList()))
        return bundle
    }
}

internal class FakeSpeechRecognitionBackend(
    override var sdkInt: Int = 35,
    var recordAudioPermission: Boolean = true,
    var recognitionAvailable: Boolean = true,
    var onDeviceRecognitionAvailable: Boolean = true,
    var onDeviceCreationFails: Boolean = false,
    var creationFails: Boolean = false,
) : SpeechRecognitionBackend {
    val engines = mutableListOf<FakeSpeechEngine>()
    var stuckAudioOutput: StuckAudioOutput? = null

    val engine: FakeSpeechEngine
        get() = engines.last()

    override val packageName: String = "com.moblin.android"

    override fun hasRecordAudioPermission(): Boolean = recordAudioPermission

    override fun isRecognitionAvailable(): Boolean = recognitionAvailable

    override fun isOnDeviceRecognitionAvailable(): Boolean = onDeviceRecognitionAvailable

    override fun createEngine(onDevice: Boolean): SpeechRecognitionEngine? {
        if (creationFails || (onDevice && onDeviceCreationFails)) {
            return null
        }
        val engine = FakeSpeechEngine(onDevice)
        engines.add(engine)
        return engine
    }

    override fun createAudioPipe(): Array<ParcelFileDescriptor> = ParcelFileDescriptor.createPipe()

    override fun openAudioOutput(writeSide: ParcelFileDescriptor): OutputStream {
        val output = stuckAudioOutput ?: return super.openAudioOutput(writeSide)
        output.writeSide = writeSide
        return output
    }
}

internal class StuckAudioOutput : OutputStream() {
    private val closedLatch = CountDownLatch(1)
    val blockedWrites = AtomicInteger(0)
    var writeSide: ParcelFileDescriptor? = null

    val isClosed: Boolean
        get() = closedLatch.count == 0L

    override fun write(b: Int) {
        write(byteArrayOf(b.toByte()), 0, 1)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        blockedWrites.incrementAndGet()
        closedLatch.await()
        throw IOException("closed")
    }

    override fun close() {
        closedLatch.countDown()
        writeSide?.close()
    }
}

internal fun runMain() {
    shadowOf(Looper.getMainLooper()).idle()
}

internal fun runMainFor(milliseconds: Long) {
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(milliseconds))
}

internal fun waitUntil(condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + 5_000
    while (!condition()) {
        if (System.currentTimeMillis() > deadline) {
            throw AssertionError("Timed out waiting for condition")
        }
        Thread.sleep(5)
    }
}

internal fun pcm16Sample(sampleRate: Int, channels: Int, frames: Int, value: (Int, Int) -> Int): MediaSample {
    val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_RAW, sampleRate, channels)
    format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
    val buffer = ByteBuffer.allocate(frames * channels * 2).order(ByteOrder.LITTLE_ENDIAN)
    for (frame in 0 until frames) {
        for (channel in 0 until channels) {
            buffer.putShort(value(frame, channel).toShort())
        }
    }
    return MediaSample(data = buffer.array(), presentationTimeUs = 0, isKeyFrame = true, format = format)
}

internal fun pcmFloatSample(sampleRate: Int, channels: Int, frames: Int, value: (Int, Int) -> Float): MediaSample {
    val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_RAW, sampleRate, channels)
    format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_FLOAT)
    val buffer = ByteBuffer.allocate(frames * channels * 4).order(ByteOrder.LITTLE_ENDIAN)
    for (frame in 0 until frames) {
        for (channel in 0 until channels) {
            buffer.putFloat(value(frame, channel))
        }
    }
    return MediaSample(data = buffer.array(), presentationTimeUs = 0, isKeyFrame = true, format = format)
}

internal fun shorts(bytes: ByteArray): ShortArray {
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
    val shorts = ShortArray(buffer.remaining())
    buffer.get(shorts)
    return shorts
}

internal fun readAvailable(source: ParcelFileDescriptor): ByteArray {
    return FileInputStream(source.fileDescriptor).readBytes()
}
