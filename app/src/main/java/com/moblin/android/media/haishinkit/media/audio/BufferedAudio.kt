package com.moblin.android.media.haishinkit.media.audio

import android.media.MediaFormat
import android.util.Log
import com.moblin.android.common.various.formatThreeDecimals
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.BufferedStats
import com.moblin.android.media.haishinkit.media.DriftTracker
import com.moblin.android.media.haishinkit.media.DriftTrackerMedia
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.utils.currentPresentationTimeStamp
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

private const val deltaLimit = 0.03
private const val tag = "BufferedAudio"

interface BufferedAudioSampleBufferDelegate {
    fun didOutputBufferedSampleBuffer(cameraId: UUID, sampleBuffer: MediaSample)
}

class BufferedAudio(
    private var cameraId: UUID,
    private val name: String,
    val latency: Double,
    private val manualOutput: Boolean,
    private val driftTracker: DriftTracker?
) {
    private var sampleRate: Double = 0.0
    private var frameLength: Double = 0.0
    private val sampleBuffers = ArrayDeque<MediaSample>()
    private val outputTimer = SimpleTimer(processorPipelineQueue.coroutineContext[CoroutineDispatcher] ?: Dispatchers.Default)
    private var isInitialized: Boolean = false
    private var isOutputting: Boolean = false
    private var latestSampleBuffer: MediaSample? = null
    private var outputCounter: Long = -1L
    private var startPresentationTimeStamp: Long = 0L
    private var isInitialBuffering = true
    private var isSyncingWithOutput = true
    var delegate: BufferedAudioSampleBufferDelegate? = null
    private var hasBufferBeenAppended = false
    private val stats = BufferedStats()

    init {
        if (manualOutput) {
            isOutputting = true
        }
        driftTracker?.addMedia(DriftTrackerMedia.audio, latency)
    }

    fun numberOfBuffers(): Int {
        return sampleBuffers.size
    }

    fun appendSampleBuffer(sampleBuffer: MediaSample) {
        sampleBuffers.addLast(sampleBuffer)
        hasBufferBeenAppended = true
        if (!isInitialized) {
            isInitialized = true
            initialize(sampleBuffer)
        }
        if (!isOutputting) {
            isOutputting = true
            startOutput()
        }
    }

    fun getSampleBuffer(outputPresentationTimeStamp: Double): MediaSample? {
        var sampleBuffer: MediaSample? = null
        var numberOfBuffersConsumed = 0
        val drift = driftTracker?.getDrift() ?: 0.0
        while (true) {
            val nextSampleBuffer = sampleBuffers.firstOrNull() ?: break
            if (latestSampleBuffer == null) {
                latestSampleBuffer = nextSampleBuffer
            }
            if (sampleBuffers.size > 300) {
                Log.i(
                    tag,
                    "buffered-audio: $name: Over 300 buffers (${sampleBuffers.size}) buffered. " +
                        "Dropping oldest buffer."
                )
                sampleBuffer = nextSampleBuffer
                numberOfBuffersConsumed = consumeBuffer(numberOfBuffersConsumed)
                continue
            }
            if (hasBestBuffer(
                    nextSampleBuffer,
                    sampleBuffer,
                    outputPresentationTimeStamp,
                    drift
                )
            ) {
                break
            }
            sampleBuffer = nextSampleBuffer
            numberOfBuffersConsumed = consumeBuffer(numberOfBuffersConsumed)
            isInitialBuffering = false
        }
        if (!isInitialBuffering) {
            updateStatsAndLog(
                outputPresentationTimeStamp,
                sampleBuffer,
                drift,
                numberOfBuffersConsumed
            )
        }
        if (sampleBuffer != null) {
            latestSampleBuffer = sampleBuffer
        } else {
            val latest = latestSampleBuffer
            if (latest != null) {
                latest.data.fill(0)
                sampleBuffer = latest
            }
        }
        if (!isInitialBuffering && hasBufferBeenAppended && !manualOutput) {
            hasBufferBeenAppended = false
            val tracker = driftTracker
            val newestSampleBuffer = sampleBuffers.lastOrNull() ?: latestSampleBuffer
            if (tracker != null && newestSampleBuffer != null) {
                tracker.update(
                    DriftTrackerMedia.audio,
                    outputPresentationTimeStamp,
                    newestSampleBuffer.presentationTimeUs / 1_000_000.0
                )
            }
        }
        return sampleBuffer
    }

    private fun consumeBuffer(numberOfBuffersConsumed: Int): Int {
        sampleBuffers.removeFirst()
        return numberOfBuffersConsumed + 1
    }

    private fun updateStatsAndLog(
        outputPresentationTimeStamp: Double,
        sampleBuffer: MediaSample?,
        drift: Double,
        numberOfBuffersConsumed: Int
    ) {
        if (numberOfBuffersConsumed == 0) {
            stats.incrementDuplicated()
        } else if (numberOfBuffersConsumed > 1) {
            stats.incrementDropped(numberOfBuffersConsumed - 1)
        }
        if (!Log.isLoggable(tag, Log.DEBUG)) {
            return
        }
        val (duplicated, dropped) = stats.getStats(outputPresentationTimeStamp) ?: return
        val lastPresentationTimeStamp =
            (sampleBuffers.lastOrNull()?.presentationTimeUs ?: 0L) / 1_000_000.0
        val firstPresentationTimeStamp =
            (sampleBuffers.firstOrNull()?.presentationTimeUs ?: 0L) / 1_000_000.0
        val fillLevel = lastPresentationTimeStamp - firstPresentationTimeStamp
        Log.d(
            tag,
            "buffered-audio: $name: $duplicated duplicated and $dropped dropped buffers. " +
                "Output ${formatThreeDecimals(outputPresentationTimeStamp)}, " +
                "Current ${formatThreeDecimals(sampleBuffer?.presentationTimeUs?.let { it / 1_000_000.0 } ?: 0.0)}, " +
                "${formatThreeDecimals(firstPresentationTimeStamp + drift)}.." +
                "${formatThreeDecimals(lastPresentationTimeStamp + drift)} " +
                "(${formatThreeDecimals(fillLevel)}), " +
                "Buffers ${sampleBuffers.size}"
        )
    }

    private fun hasBestBuffer(
        nextSampleBuffer: MediaSample,
        candidateSampleBuffer: MediaSample?,
        outputPresentationTimeStamp: Double,
        drift: Double
    ): Boolean {
        return if (isSyncingWithOutput) {
            hasBestBufferSynching(
                nextSampleBuffer,
                candidateSampleBuffer,
                outputPresentationTimeStamp,
                drift
            )
        } else if (candidateSampleBuffer != null) {
            hasBestBufferNormal(candidateSampleBuffer, outputPresentationTimeStamp, drift)
        } else {
            false
        }
    }

    private fun hasBestBufferSynching(
        nextSampleBuffer: MediaSample,
        candidateSampleBuffer: MediaSample?,
        outputPresentationTimeStamp: Double,
        drift: Double
    ): Boolean {
        val nextPresentationTimeStamp = nextSampleBuffer.presentationTimeUs / 1_000_000.0 + drift
        val delta = nextPresentationTimeStamp - outputPresentationTimeStamp
        if (delta <= 0) {
            return false
        }
        if (candidateSampleBuffer != null) {
            isSyncingWithOutput = false
        }
        return true
    }

    private fun hasBestBufferNormal(
        candidateSampleBuffer: MediaSample,
        outputPresentationTimeStamp: Double,
        drift: Double
    ): Boolean {
        val candidatePresentationTimeStamp =
            candidateSampleBuffer.presentationTimeUs / 1_000_000.0 + drift
        val delta = candidatePresentationTimeStamp - outputPresentationTimeStamp
        if (abs(delta) > 0.05) {
            isSyncingWithOutput = true
        }
        return true
    }

    private fun initialize(sampleBuffer: MediaSample) {
        frameLength = TODO("MediaSample does not expose the PCM frame count (CMSampleBuffer.numSamples)")
        sampleBuffer.format?.let { format ->
            sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE).toDouble()
            } else {
                1.0
            }
        }
    }

    private fun startOutput() {
        Log.i(
            tag,
            "buffered-audio: $name: Start output with sample rate $sampleRate and " +
                "frame length $frameLength"
        )
        outputTimer.startPeriodic(1 / (sampleRate / frameLength), 0.0) {
            output()
        }
    }

    fun stopOutput() {
        Log.i(tag, "buffered-audio: $name: Stopping output.")
        outputTimer.stop()
    }

    private fun makePresentationTimeStamp(): Long {
        val value = (frameLength * outputCounter.toDouble()).toLong()
        return (value * 1_000_000.0 / sampleRate).toLong() + startPresentationTimeStamp
    }

    private fun output() {
        outputCounter += 1
        val clockPresentationTimeStamp = currentPresentationTimeStamp()
        if (startPresentationTimeStamp == 0L) {
            startPresentationTimeStamp = clockPresentationTimeStamp
        }
        var presentationTimeStamp = makePresentationTimeStamp()
        val deltaFromCalculatedToClock = presentationTimeStamp - clockPresentationTimeStamp
        if (abs(deltaFromCalculatedToClock / 1_000_000.0) > deltaLimit) {
            if (deltaFromCalculatedToClock > 0L) {
                Log.i(
                    tag,
                    "buffered-audio: Adjust PTS back in time. Calculated is " +
                        "${presentationTimeStamp / 1_000_000.0} " +
                        "and clock is ${clockPresentationTimeStamp / 1_000_000.0}"
                )
                outputCounter -= 1
            } else {
                Log.i(
                    tag,
                    "buffered-audio: Adjust PTS forward in time. Calculated is " +
                        "${presentationTimeStamp / 1_000_000.0} " +
                        "and clock is ${clockPresentationTimeStamp / 1_000_000.0}"
                )
                outputCounter += 1
            }
            presentationTimeStamp = makePresentationTimeStamp()
        }
        val sampleBuffer = getSampleBuffer(presentationTimeStamp / 1_000_000.0)?.let {
            MediaSample(
                data = it.data,
                presentationTimeUs = presentationTimeStamp,
                isKeyFrame = it.isKeyFrame,
                format = it.format
            )
        } ?: return
        delegate?.didOutputBufferedSampleBuffer(cameraId, sampleBuffer)
    }
}
