package com.moblin.android.media.haishinkit.media

import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.various.utils.screenScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface MacScreenCaptureDelegate {
    fun macScreenCaptureDidStart(latency: Double)

    fun macScreenCaptureDidStop()

    fun macScreenCaptureDidOutputSampleBuffer(sampleBuffer: MediaSample)
}

private const val macScreenCaptureLatency = 0.15

private const val macScreenCaptureTag = "MacScreenCapture"

private val macScreenCaptureScope = CoroutineScope(Dispatchers.Main)

class MacScreenCapture {
    companion object {
        val shared = MacScreenCapture()
    }

    var delegate: MacScreenCaptureDelegate? = null

    private var stream: Any? = null

    private var latestSampleBufferWithImageBuffer: MediaSample? = null

    fun start(fps: Double) {
        macScreenCaptureScope.launch {
            startInternal(fps = fps)
        }
    }

    fun stop() {
        macScreenCaptureScope.launch {
            stopInternal()
        }
    }

    private suspend fun startInternal(fps: Double) {
        try {
            val (filter, display) = makeContentFilter()
            val scale = screenScale().toInt()
            Log.i(
                macScreenCaptureTag,
                "mac-screen-capture: ${fps.toInt()} FPS, filter=$filter, display=$display, scale=$scale",
            )
            TODO("no Android counterpart for ScreenCaptureKit SCStream")
            delegate?.macScreenCaptureDidStart(latency = macScreenCaptureLatency)
        } catch (e: Exception) {
            Log.i(macScreenCaptureTag, "mac-screen-capture: Failed to start: ${e.message}")
        }
    }

    private suspend fun stopInternal() {
        try {
            stream?.let {
                TODO("no Android counterpart for ScreenCaptureKit SCStream.stopCapture")
            }
        } catch (e: Exception) {
            Log.i(macScreenCaptureTag, "mac-screen-capture: Failed to stop: ${e.message}")
        }
        stream = null
        delegate?.macScreenCaptureDidStop()
    }

    private suspend fun makeContentFilter(): Pair<Any, Any> =
        TODO("no Android counterpart for ScreenCaptureKit SCShareableContent")

    fun stream(stream: Any, didOutputSampleBuffer: MediaSample, of: Any) {
        var sampleBuffer = didOutputSampleBuffer
        val latest = latestSampleBufferWithImageBuffer
        if (sampleBuffer.data.isNotEmpty()) {
            latestSampleBufferWithImageBuffer = sampleBuffer
        } else if (latest != null) {
            sampleBuffer = latest
        } else {
            return
        }
        delegate?.macScreenCaptureDidOutputSampleBuffer(sampleBuffer)
    }

    fun stream(stream: Any, didStopWithError: Throwable) {
        Log.i(macScreenCaptureTag, "mac-screen-capture: Stopped with error: ${didStopWithError.message}")
        delegate?.macScreenCaptureDidStop()
    }
}
