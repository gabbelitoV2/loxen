package com.moblin.android.media.haishinkit.media.video

import android.util.Log
import com.moblin.android.common.various.formatThreeDecimals
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.BufferedStats
import com.moblin.android.media.haishinkit.media.DriftTracker
import com.moblin.android.media.haishinkit.media.DriftTrackerMedia
import com.moblin.android.media.haishinkit.media.Processor
import java.util.UUID

private const val TAG = "BufferedVideo"

private val MediaSample.presentationTimeSeconds: Double
    get() = presentationTimeUs / 1_000_000.0

private fun MediaSample.replacePresentationTimeStamp(presentationTimeUs: Long): MediaSample =
    copy(presentationTimeUs = presentationTimeUs)

class BufferedVideo(
    private var cameraId: UUID,
    private val name: String,
    private val update: Boolean,
    val latency: Double,
    private val processor: Processor?,
    private val driftTracker: DriftTracker?,
) {
    private var sampleBuffers: ArrayDeque<MediaSample> = ArrayDeque()
    private var currentSampleBuffer: MediaSample? = null
    private var isInitialBuffering = true
    private var hasBufferBeenAppended = false
    private val stats = BufferedStats()

    init {
        driftTracker?.addMedia(DriftTrackerMedia.Video, targetFillLevel = latency)
    }

    fun close() {
        processor?.delegate?.streamVideoBufferedVideoRemoved(cameraId)
    }

    fun appendSampleBuffer(sampleBuffer: MediaSample) {
        hasBufferBeenAppended = true
        val index = sampleBuffers.indexOfLast {
            it.presentationTimeUs < sampleBuffer.presentationTimeUs
        }
        if (index == -1) {
            sampleBuffers.addFirst(sampleBuffer)
        } else {
            sampleBuffers.add(index + 1, sampleBuffer)
        }
    }

    fun updateSampleBuffer(outputPresentationTimeStamp: Double, forceUpdate: Boolean = false) {
        if (!update && !forceUpdate) {
            return
        }
        var sampleBuffer: MediaSample? = null
        var numberOfBuffersConsumed = 0
        val drift = driftTracker?.getDrift() ?: 0.0
        while (true) {
            val nextSampleBuffer = sampleBuffers.firstOrNull() ?: break
            if (sampleBuffers.size > 200) {
                sampleBuffer = nextSampleBuffer
                numberOfBuffersConsumed = consumeBuffer(numberOfBuffersConsumed)
                continue
            }
            if (hasBestBuffer(nextSampleBuffer, sampleBuffer, outputPresentationTimeStamp, drift)) {
                break
            }
            sampleBuffer = nextSampleBuffer
            numberOfBuffersConsumed = consumeBuffer(numberOfBuffersConsumed)
            markInitialBufferingComplete()
        }
        if (!isInitialBuffering) {
            updateStatsAndLog(
                outputPresentationTimeStamp,
                sampleBuffer,
                drift,
                numberOfBuffersConsumed,
            )
        }
        if (sampleBuffer != null) {
            currentSampleBuffer = sampleBuffer
        }
        if (!isInitialBuffering && hasBufferBeenAppended && update) {
            hasBufferBeenAppended = false
            val tracker = driftTracker
            val newestSampleBuffer = sampleBuffers.lastOrNull() ?: currentSampleBuffer
            if (tracker != null && newestSampleBuffer != null) {
                tracker.update(
                    DriftTrackerMedia.Video,
                    outputPresentationTimeStamp,
                    newestSampleBuffer.presentationTimeSeconds,
                )
            }
        }
    }

    private fun consumeBuffer(numberOfBuffersConsumed: Int): Int {
        sampleBuffers.removeFirst()
        return numberOfBuffersConsumed + 1
    }

    private fun updateStatsAndLog(
        outputPresentationTimeStamp: Double,
        sampleBuffer: MediaSample?,
        drift: Double,
        numberOfBuffersConsumed: Int,
    ) {
        if (numberOfBuffersConsumed == 0) {
            stats.incrementDuplicated()
        } else if (numberOfBuffersConsumed > 1) {
            stats.incrementDropped(numberOfBuffersConsumed - 1)
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            val result = stats.getStats(outputPresentationTimeStamp)
            if (result != null) {
                val (duplicated, dropped) = result
                val lastPresentationTimeStamp = sampleBuffers.lastOrNull()?.presentationTimeSeconds ?: 0.0
                val firstPresentationTimeStamp = sampleBuffers.firstOrNull()?.presentationTimeSeconds ?: 0.0
                val fillLevel = lastPresentationTimeStamp - firstPresentationTimeStamp
                Log.d(
                    TAG,
                    "buffered-video: $name: $duplicated duplicated and $dropped dropped buffers. " +
                        "Output ${formatThreeDecimals(outputPresentationTimeStamp)}, " +
                        "Current ${formatThreeDecimals(currentSampleBuffer?.presentationTimeSeconds ?: 0.0)}, " +
                        "${formatThreeDecimals(firstPresentationTimeStamp + drift)}.." +
                        "${formatThreeDecimals(lastPresentationTimeStamp + drift)} " +
                        "(${formatThreeDecimals(fillLevel)}), " +
                        "Buffers ${sampleBuffers.size}",
                )
            }
        }
    }

    private fun hasBestBuffer(
        nextSampleBuffer: MediaSample,
        candidateSampleBuffer: MediaSample?,
        outputPresentationTimeStamp: Double,
        drift: Double,
    ): Boolean {
        val nextPresentationTimeStamp = nextSampleBuffer.presentationTimeSeconds + drift
        val delta = nextPresentationTimeStamp - outputPresentationTimeStamp
        if (!(delta > 0)) {
            return false
        }
        if (candidateSampleBuffer != null || delta > 0.01) {
            return true
        }
        return false
    }

    private fun markInitialBufferingComplete() {
        if (isInitialBuffering) {
            processor?.delegate?.streamVideoBufferedVideoReady(cameraId)
        }
        isInitialBuffering = false
    }

    fun setLatestSampleBuffer(sampleBuffer: MediaSample?) {
        currentSampleBuffer = sampleBuffer
    }

    fun getLatestSampleBuffer(): MediaSample? {
        return currentSampleBuffer
    }

    fun getSampleBuffer(presentationTimeStamp: Long): MediaSample? {
        return currentSampleBuffer?.replacePresentationTimeStamp(presentationTimeStamp)
    }

    fun numberOfBuffers(): Int {
        return sampleBuffers.size
    }
}
