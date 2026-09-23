package com.moblin.android.platform.avfoundation

import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.audio.AudioCapture
import com.moblin.android.platform.audio.AudioCaptureConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "MoblinAudio"

interface AVCaptureAudioDataOutputSampleBufferDelegate {
    fun captureOutput(output: AVCaptureOutput, didOutput: MediaSample, from: AVCaptureConnection?)
}

class AVCaptureAudioDataOutput : AVCaptureOutput() {
    @Volatile
    private var delegate: AVCaptureAudioDataOutputSampleBufferDelegate? = null

    @Volatile
    private var queue: CoroutineScope? = null

    val sampleBufferDelegate: AVCaptureAudioDataOutputSampleBufferDelegate?
        get() = delegate

    val sampleBufferCallbackQueue: CoroutineScope?
        get() = queue

    fun setSampleBufferDelegate(delegate: AVCaptureAudioDataOutputSampleBufferDelegate?, queue: CoroutineScope?) {
        this.delegate = delegate
        this.queue = queue
    }

    internal fun deliver(sampleBuffer: MediaSample) {
        val delegate = delegate ?: return
        val queue = queue
        if (queue == null) {
            deliverNow(delegate, sampleBuffer)
        } else {
            queue.launch {
                deliverNow(delegate, sampleBuffer)
            }
        }
    }

    private fun deliverNow(delegate: AVCaptureAudioDataOutputSampleBufferDelegate, sampleBuffer: MediaSample) {
        try {
            delegate.captureOutput(this, sampleBuffer, null)
        } catch (error: Throwable) {
            Log.e(TAG, "Audio sample buffer delegate failed", error)
        }
    }
}

object AudioCaptureEngine {
    private const val maximumRestarts = 5
    private const val restartWindowNs = 10_000_000_000L
    private val lock = Any()
    private var device: AVCaptureDevice? = null
    private var output: AVCaptureAudioDataOutput? = null
    private var isRunning = false
    private var capture: AudioCapture? = null
    private val restartTimesNs = ArrayDeque<Long>()

    fun configure(device: AVCaptureDevice?, output: AVCaptureAudioDataOutput?) {
        synchronized(lock) {
            if (device == null && output == null) {
                return
            }
            this.device = device
            this.output = output
            if (isRunning) {
                applyLocked(forceRestart = false)
            }
        }
    }

    fun start() {
        synchronized(lock) {
            isRunning = true
            applyLocked(forceRestart = false)
        }
    }

    fun stop() {
        synchronized(lock) {
            isRunning = false
            stopCaptureLocked()
        }
    }

    val isCapturing: Boolean
        get() = synchronized(lock) { capture != null }

    internal fun restartAfterFailure(failedCapture: AudioCapture) {
        synchronized(lock) {
            if (capture !== failedCapture || !isRunning) {
                return
            }
            val nowNs = System.nanoTime()
            while (restartTimesNs.isNotEmpty() && nowNs - restartTimesNs.first() > restartWindowNs) {
                restartTimesNs.removeFirst()
            }
            if (restartTimesNs.size >= maximumRestarts) {
                Log.e(TAG, "AudioRecord failed $maximumRestarts times in 10 s, giving up until reconfigured")
                stopCaptureLocked()
                return
            }
            restartTimesNs.addLast(nowNs)
            Log.i(TAG, "Restarting audio capture")
            applyLocked(forceRestart = true)
        }
    }

    private fun applyLocked(forceRestart: Boolean) {
        val output = output
        if (output == null) {
            stopCaptureLocked()
            return
        }
        val config = AudioCaptureConfig.resolve(device?.uniqueID)
        if (config == null) {
            stopCaptureLocked()
            return
        }
        val current = capture
        if (current != null && !forceRestart && current.isHealthy() && current.config == config) {
            current.sink = { output.deliver(it) }
            return
        }
        stopCaptureLocked()
        val newCapture = AudioCapture.start(config) { failed ->
            restartLater(failed)
        } ?: return
        newCapture.sink = { output.deliver(it) }
        capture = newCapture
    }

    private fun stopCaptureLocked() {
        val current = capture ?: return
        capture = null
        current.stop()
    }

    private fun restartLater(failedCapture: AudioCapture) {
        com.moblin.android.media.haishinkit.media.processorControlQueue.launch {
            kotlinx.coroutines.delay(300)
            restartAfterFailure(failedCapture)
        }
    }
}
