package com.moblin.android.media.haishinkit.codec.video

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.moblin.android.media.MediaSample
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface VideoDecoderDelegate {
    fun videoDecoderOutputSampleBuffer(codec: VideoDecoder, sampleBuffer: MediaSample)
}

class VideoDecoder(
    private val name: String,
    private val lockQueue: CoroutineScope,
    private val softwareDecoding: Boolean,
) {
    private var isRunning = false
    private var formatDescription: MediaFormat? = null

    private var delegateRef: WeakReference<VideoDecoderDelegate>? = null
    var delegate: VideoDecoderDelegate?
        get() = delegateRef?.get()
        set(value) {
            delegateRef = value?.let { WeakReference(it) }
        }

    private var invalidateSession = true
    private var numberOfFailedFrames = 0
    private var latestFailedFrameStatus = 0

    private var session: MediaCodec? = null
        set(value) {
            val oldValue = field
            field = value
            if (oldValue !== value) {
                runCatching { oldValue?.stop() }
                runCatching { oldValue?.release() }
            }
            invalidateSession = false
        }

    private val inputLock = Any()
    private val pendingSampleBuffers = ArrayDeque<MediaSample>()
    private var availableInputBufferIndex = -1
    private var callbackThread: HandlerThread? = null

    fun startRunning(formatDescription: MediaFormat? = null) {
        isRunning = true
        invalidateSession = true
        numberOfFailedFrames = 0
        this.formatDescription = formatDescription
    }

    fun stopRunning() {
        session = null
        invalidateSession = true
        formatDescription = null
        isRunning = false
        synchronized(inputLock) {
            pendingSampleBuffers.clear()
            availableInputBufferIndex = -1
        }
        callbackThread?.let { thread -> runCatching { thread.quitSafely() } }
        callbackThread = null
    }

    fun decodeSampleBuffer(sampleBuffer: MediaSample) {
        if (!isRunning) {
            return
        }
        if (invalidateSession) {
            session = makeSession()
        }
        val codec = session ?: return
        try {
            synchronized(inputLock) {
                pendingSampleBuffers.addLast(sampleBuffer)
                feedInput(codec)
            }
        } catch (e: IllegalStateException) {
            Log.i(TAG, "video-decoder: $name: Decode failed. Resetting session.")
            invalidateSession = true
        }
    }

    private fun logFailedFrames() {
        if (numberOfFailedFrames <= 0) {
            return
        }
        Log.i(
            TAG,
            "video-decoder: $name: Failed to decode $numberOfFailedFrames frame(s). " +
                "Latest status $latestFailedFrameStatus.",
        )
        numberOfFailedFrames = 0
    }

    private fun makeSession(): MediaCodec? {
        val format = formatDescription
        if (format == null) {
            Log.i(TAG, "video-decoder: $name: Format description missing")
            return null
        }
        val mimeType = format.getString(MediaFormat.KEY_MIME)
        if (mimeType == null) {
            Log.i(TAG, "video-decoder: $name: Format description missing mime type")
            return null
        }
        return runCatching {
            val thread = callbackThread?.takeIf { it.isAlive } ?: HandlerThread("video-decoder-$name").also {
                it.start()
                callbackThread = it
            }
            val codec = createCodec(format, mimeType)
            codec.setCallback(makeCallback(), Handler(thread.looper))
            codec.configure(format, null, null, 0)
            codec.start()
            codec
        }.getOrElse { e ->
            Log.i(TAG, "video-decoder: $name: Failed to create session: ${e.message}")
            null
        }
    }

    private fun makeCallback(): MediaCodec.Callback {
        return object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                synchronized(inputLock) {
                    availableInputBufferIndex = index
                    feedInput(codec)
                }
            }

            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                handleOutputBuffer(codec, index, info)
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                Log.i(TAG, "video-decoder: $name: Codec error (${e.message}).")
                countFailedFrame(e.errorCode)
                invalidateSession = true
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            }
        }
    }

    private fun feedInput(codec: MediaCodec) {
        while (availableInputBufferIndex >= 0 && pendingSampleBuffers.isNotEmpty()) {
            val sampleBuffer = pendingSampleBuffers.removeFirst()
            val index = availableInputBufferIndex
            availableInputBufferIndex = -1
            try {
                val inputBuffer = codec.getInputBuffer(index) ?: break
                inputBuffer.clear()
                if (sampleBuffer.data.size > inputBuffer.capacity()) {
                    Log.i(TAG, "video-decoder: $name: Input buffer too small for sample")
                    break
                }
                inputBuffer.put(sampleBuffer.data)
                codec.queueInputBuffer(index, 0, sampleBuffer.data.size, sampleBuffer.presentationTimeUs, 0)
            } catch (e: IllegalStateException) {
                Log.i(TAG, "video-decoder: $name: Failed to queue input buffer (${e.message}).")
                break
            }
        }
    }

    private fun handleOutputBuffer(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
        try {
            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                return
            }
            if (info.size <= 0) {
                countFailedFrame(STATUS_UNKNOWN)
                return
            }
            val outputBuffer = codec.getOutputBuffer(index)
            if (outputBuffer == null) {
                countFailedFrame(STATUS_UNKNOWN)
                return
            }
            outputBuffer.position(info.offset)
            outputBuffer.limit(info.offset + info.size)
            val data = ByteArray(info.size)
            outputBuffer.get(data)
            val format = runCatching { codec.outputFormat }.getOrNull()
            val sampleBuffer = MediaSample(
                data = data,
                presentationTimeUs = info.presentationTimeUs,
                isKeyFrame = true,
                format = format,
            )
            lockQueue.launch {
                logFailedFrames()
                delegate?.videoDecoderOutputSampleBuffer(this@VideoDecoder, sampleBuffer)
            }
        } catch (e: IllegalStateException) {
            Log.i(TAG, "video-decoder: $name: Failed to read output buffer (${e.message}).")
            countFailedFrame(STATUS_UNKNOWN)
        } finally {
            runCatching { codec.releaseOutputBuffer(index, false) }
        }
    }

    private fun countFailedFrame(status: Int) {
        synchronized(inputLock) {
            numberOfFailedFrames += 1
            latestFailedFrameStatus = status
        }
    }

    private fun createCodec(format: MediaFormat, mimeType: String): MediaCodec {
        val codecName = selectCodecName(format, mimeType)
        return if (codecName != null) {
            MediaCodec.createByCodecName(codecName)
        } else {
            MediaCodec.createDecoderByType(mimeType)
        }
    }

    private fun selectCodecName(format: MediaFormat, mimeType: String): String? {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        if (softwareDecoding && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val softwareCodec = codecList.codecInfos.firstOrNull { info ->
                !info.isEncoder &&
                    info.isSoftwareOnly &&
                    info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }
            }
            if (softwareCodec != null) {
                return softwareCodec.name
            }
        }
        return codecList.findDecoderForFormat(format)
    }

    companion object {
        private const val TAG = "VideoDecoder"
        private const val STATUS_UNKNOWN = -1
    }
}
