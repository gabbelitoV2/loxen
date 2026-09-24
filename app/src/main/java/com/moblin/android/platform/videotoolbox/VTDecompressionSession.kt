package com.moblin.android.platform.videotoolbox

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.CVPixelBufferPool
import com.moblin.android.platform.video.GlRenderer
import com.moblin.android.platform.video.PixelBufferTurn
import com.moblin.android.platform.video.kCVPixelBufferPixelFormatTypeKey
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import java.util.Collections
import java.util.TreeMap
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger

const val kVTVideoDecoderSpecification_EnableHardwareAcceleratedVideoDecoder = "EnableHardwareAcceleratedVideoDecoder"
const val kVTVideoDecoderSpecification_RequireHardwareAcceleratedVideoDecoder = "RequireHardwareAcceleratedVideoDecoder"
const val kVTCouldNotFindVideoDecoderErr = -12906
const val kVTVideoDecoderBadDataErr = -12909
const val kVTVideoDecoderMalfunctionErr = -12911

object VTDecodeFrameFlags {
    const val _EnableAsynchronousDecompression = 1 shl 0
    const val _DoNotOutputFrame = 1 shl 1
    const val _1xRealTimePlayback = 1 shl 2
    const val _EnableTemporalProcessing = 1 shl 3
}

object VTDecodeInfoFlags {
    const val _Asynchronous = 1 shl 0
    const val _FrameDropped = 1 shl 1
    const val _ImageBufferModifiable = 1 shl 2
}

typealias VTDecompressionOutputHandler = (
    status: Int,
    infoFlags: Int,
    imageBuffer: CVPixelBuffer?,
    presentationTimeStamp: Long,
    presentationDuration: Long,
) -> Unit

private const val TAG = "MoblinDecoder"

private const val defaultWidth = 1920

private const val defaultHeight = 1080

private const val maximumInputSize = 2 * 1024 * 1024

private const val maximumNumberOfPendingFrames = 120

private const val maximumNumberOfDecodedFrames = 8

internal const val maximumNumberOfOutputBuffers = 256

private const val renderTimeoutNs = 500_000_000L

private val nextSessionIndex = AtomicInteger(0)

internal object DecompressionSessions {
    private val sessions = Collections.newSetFromMap(WeakHashMap<VTDecompressionSession, Boolean>())
    private val numberOfRunningSessions = AtomicInteger(0)

    init {
        PipelineStats.addReporter { report() }
    }

    fun started(session: VTDecompressionSession) {
        synchronized(sessions) {
            sessions.add(session)
        }
        numberOfRunningSessions.incrementAndGet()
    }

    fun invalidated(session: VTDecompressionSession) {
        synchronized(sessions) {
            sessions.remove(session)
        }
        numberOfRunningSessions.decrementAndGet()
    }

    fun running(): List<VTDecompressionSession> {
        return synchronized(sessions) { sessions.toList() }
    }

    private fun report(): String? {
        val count = numberOfRunningSessions.get()
        return if (count > 0) "decoders $count" else null
    }
}

internal class DecodeOutputHandlers {
    private val handlers = TreeMap<Long, Pair<Long, VTDecompressionOutputHandler>>()
    private var latestOutputHandler: VTDecompressionOutputHandler? = null

    fun add(presentationTimeStamp: Long, duration: Long, outputHandler: VTDecompressionOutputHandler) {
        handlers[presentationTimeStamp] = Pair(duration, outputHandler)
        latestOutputHandler = outputHandler
    }

    fun take(
        presentationTimeStamp: Long,
        skipped: (Long, Long, VTDecompressionOutputHandler) -> Unit,
    ): Pair<Long, VTDecompressionOutputHandler?> {
        val entry = handlers.remove(presentationTimeStamp)
        val older = handlers.headMap(presentationTimeStamp)
        for ((olderPresentationTimeStamp, value) in older) {
            skipped(olderPresentationTimeStamp, value.first, value.second)
        }
        older.clear()
        return entry ?: Pair(-1L, latestOutputHandler)
    }

    fun drain(dropped: (Long, Long, VTDecompressionOutputHandler) -> Unit) {
        for ((presentationTimeStamp, value) in handlers) {
            dropped(presentationTimeStamp, value.first, value.second)
        }
        handlers.clear()
    }

    fun clear() {
        handlers.clear()
        latestOutputHandler = null
    }
}

class VTDecompressionSession internal constructor(
    private val mimeType: String,
    private val codecName: String,
    private val codecFormat: MediaFormat,
    private val pixelFormatType: Int,
) {
    companion object {}

    private class PendingFrame(
        val data: ByteArray,
        val isKeyFrame: Boolean,
        val presentationTimeStamp: Long,
        val duration: Long,
        val outputHandler: VTDecompressionOutputHandler,
    )

    private class DecodedFrame(
        val index: Int,
        val presentationTimeStamp: Long,
        val duration: Long,
        val outputHandler: VTDecompressionOutputHandler?,
    )

    private class DroppedFrame(
        val status: Int,
        val infoFlags: Int,
        val presentationTimeStamp: Long,
        val duration: Long,
        val outputHandler: VTDecompressionOutputHandler,
    )

    private val lock = Any()
    private val index = nextSessionIndex.incrementAndGet()
    private val isHevc = mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC
    private var codec: MediaCodec? = null
    private var thread: HandlerThread? = null
    private var callbackHandler: Handler? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    private var oesTexture = 0
    private val stMatrix = FloatArray(16)
    private var pool: CVPixelBufferPool? = null
    private val pendingFrames = ArrayDeque<PendingFrame>()
    private val availableInputIndexes = ArrayDeque<Int>()
    private val outputHandlers = DecodeOutputHandlers()
    private var droppedFrames = ArrayList<DroppedFrame>()
    private val decodedFrames = ArrayDeque<DecodedFrame>()
    private var renderingFrame: DecodedFrame? = null
    private var renderStartedNs = 0L
    private var waitingForKeyFrame = true
    private var numberOfDroppedFrames = 0
    private var lastDropLogMs = 0L
    private var running = false

    @Volatile
    private var invalidated = false

    @Volatile
    private var failed = false

    @Volatile
    private var outputWidth = 0

    @Volatile
    private var outputHeight = 0

    internal val isInvalidated: Boolean
        get() = invalidated

    internal val handlerThread: HandlerThread?
        get() = thread

    internal val outputPool: CVPixelBufferPool?
        get() = pool

    private val frameListener = SurfaceTexture.OnFrameAvailableListener { onFrameAvailable() }

    private val callback = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            synchronized(lock) {
                availableInputIndexes.addLast(index)
                feedInputsLocked(codec)
            }
            reportDroppedFrames()
        }

        override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
            handleOutputBuffer(codec, index, info)
            reportDroppedFrames()
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            Log.i(TAG, "video-decoder-$index: Codec error: ${e.diagnosticInfo}")
            synchronized(lock) {
                failed = true
                failFramesLocked()
            }
            reportDroppedFrames()
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            updateOutputSize(format)
        }
    }

    init {
        for (name in listOf("decIn", "decOut", "decDrop")) {
            PipelineStats.increment(name, 0)
        }
    }

    internal fun start(): Int {
        val created = try {
            PipelineThread.runSync {
                val texture = GlRenderer.createOesTexture()
                if (texture == 0) {
                    false
                } else {
                    val newSurfaceTexture = SurfaceTexture(texture)
                    newSurfaceTexture.setOnFrameAvailableListener(frameListener, PipelineThread.handler)
                    oesTexture = texture
                    surfaceTexture = newSurfaceTexture
                    surface = Surface(newSurfaceTexture)
                    true
                }
            }
        } catch (error: Throwable) {
            Log.i(TAG, "video-decoder-$index: Failed to create surface texture: $error")
            false
        }
        if (!created) {
            invalidateSession()
            return kVTVideoDecoderMalfunctionErr
        }
        val thread = HandlerThread("video-decoder-$index")
        thread.start()
        this.thread = thread
        val handler = Handler(thread.looper)
        callbackHandler = handler
        return try {
            val codec = MediaCodec.createByCodecName(codecName)
            this.codec = codec
            codec.setCallback(callback, handler)
            codec.configure(codecFormat, surface, null, 0)
            codec.start()
            Log.i(TAG, "video-decoder-$index: Started $codecName for $mimeType")
            synchronized(lock) {
                running = true
            }
            DecompressionSessions.started(this)
            noErr
        } catch (error: Exception) {
            Log.i(TAG, "video-decoder-$index: Failed to start $codecName: $error")
            invalidateSession()
            kVTVideoDecoderMalfunctionErr
        }
    }

    internal fun decodeFrame(sampleBuffer: MediaSample, outputHandler: VTDecompressionOutputHandler): Int {
        if (invalidated || failed) {
            return kVTInvalidSessionErr
        }
        val data = sampleDataToAnnexB(sampleBuffer.data) ?: return kVTVideoDecoderBadDataErr
        val keyFrame = annexBContainsKeyFrame(data, isHevc)
        var dropped = false
        synchronized(lock) {
            if (waitingForKeyFrame && !keyFrame) {
                dropped = true
            } else {
                waitingForKeyFrame = false
                pendingFrames.addLast(
                    PendingFrame(
                        data,
                        keyFrame,
                        sampleBuffer.presentationTimeUs,
                        sampleBuffer.durationUs,
                        outputHandler,
                    ),
                )
                if (pendingFrames.size > maximumNumberOfPendingFrames) {
                    dropPendingFrameLocked(pendingFrames.removeFirst(), "decoder input is full")
                    skipToKeyFrameLocked("decoder input is full")
                }
                val codec = codec
                if (codec != null) {
                    feedInputsLocked(codec)
                }
            }
        }
        reportDroppedFrames()
        if (dropped) {
            PipelineStats.increment("decDrop")
            invokeOutputHandler(
                outputHandler,
                kVTVideoDecoderBadDataErr,
                VTDecodeInfoFlags._FrameDropped,
                null,
                sampleBuffer.presentationTimeUs,
                sampleBuffer.durationUs,
            )
        }
        return if (failed) kVTInvalidSessionErr else noErr
    }

    internal fun invalidateSession() {
        val codec: MediaCodec?
        val thread: HandlerThread?
        val wasRunning: Boolean
        synchronized(lock) {
            if (invalidated) {
                return
            }
            invalidated = true
            wasRunning = running
            running = false
            codec = this.codec
            this.codec = null
            thread = this.thread
            this.thread = null
            callbackHandler = null
            pendingFrames.clear()
            availableInputIndexes.clear()
            outputHandlers.clear()
            droppedFrames.clear()
            decodedFrames.clear()
            renderingFrame = null
        }
        if (codec != null) {
            try {
                codec.stop()
            } catch (error: Exception) {
                Log.d(TAG, "video-decoder-$index: Stop failed: $error")
            }
            try {
                codec.release()
            } catch (error: Exception) {
                Log.d(TAG, "video-decoder-$index: Release failed: $error")
            }
        }
        thread?.quitSafely()
        if (wasRunning) {
            DecompressionSessions.invalidated(this)
        }
        PipelineThread.post {
            surface?.release()
            surface = null
            surfaceTexture?.setOnFrameAvailableListener(null)
            surfaceTexture?.release()
            surfaceTexture = null
            GlRenderer.deleteTexture(oesTexture)
            oesTexture = 0
            pool?.invalidate()
            pool = null
        }
    }

    private fun feedInputsLocked(codec: MediaCodec) {
        while (!invalidated && !failed && availableInputIndexes.isNotEmpty() && pendingFrames.isNotEmpty()) {
            val frame = pendingFrames.removeFirst()
            val inputIndex = availableInputIndexes.removeFirst()
            var queued = false
            try {
                val buffer = codec.getInputBuffer(inputIndex)
                if (buffer == null || frame.data.size > buffer.capacity()) {
                    codec.queueInputBuffer(inputIndex, 0, 0, frame.presentationTimeStamp, 0)
                    val reason = "frame of ${frame.data.size} bytes does not fit the input buffer"
                    dropPendingFrameLocked(frame, reason)
                    skipToKeyFrameLocked(reason)
                    continue
                }
                buffer.clear()
                buffer.put(frame.data)
                outputHandlers.add(frame.presentationTimeStamp, frame.duration, frame.outputHandler)
                queued = true
                codec.queueInputBuffer(inputIndex, 0, frame.data.size, frame.presentationTimeStamp, 0)
                PipelineStats.increment("decIn")
            } catch (error: Exception) {
                Log.i(TAG, "video-decoder-$index: Failed to queue input: $error")
                if (!queued) {
                    pendingFrames.addFirst(frame)
                }
                failed = true
                failFramesLocked()
                return
            }
        }
    }

    private fun skipToKeyFrameLocked(reason: String) {
        while (true) {
            val frame = pendingFrames.firstOrNull()
            if (frame == null) {
                waitingForKeyFrame = true
                return
            }
            if (frame.isKeyFrame) {
                return
            }
            dropPendingFrameLocked(pendingFrames.removeFirst(), reason)
        }
    }

    private fun handleOutputBuffer(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
        synchronized(lock) {
            if (invalidated) {
                return
            }
            if (failed || info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                releaseOutputBufferLocked(codec, index, false)
                return
            }
            val presentationTimeStamp = info.presentationTimeUs
            val (duration, outputHandler) = outputHandlers.take(presentationTimeStamp) { skipped, length, handler ->
                dropFrameLocked(skipped, length, handler, "the codec did not output it")
            }
            decodedFrames.addLast(DecodedFrame(index, presentationTimeStamp, duration, outputHandler))
            while (decodedFrames.size > maximumNumberOfDecodedFrames) {
                val frame = decodedFrames.removeFirst()
                releaseOutputBufferLocked(codec, frame.index, false)
                dropDecodedFrameLocked(frame, "rendering is too slow")
            }
            renderNextLocked()
        }
    }

    private fun renderNextLocked() {
        if (invalidated) {
            return
        }
        val timedOutFrame = renderingFrame
        if (timedOutFrame != null) {
            if (SystemClock.elapsedRealtimeNanos() - renderStartedNs < renderTimeoutNs) {
                return
            }
            Log.i(TAG, "video-decoder-$index: Frame render timed out")
            renderingFrame = null
            dropDecodedFrameLocked(timedOutFrame, "its render timed out")
        }
        val codec = codec ?: return
        val frame = decodedFrames.removeFirstOrNull() ?: return
        if (releaseOutputBufferLocked(codec, frame.index, true)) {
            renderingFrame = frame
            renderStartedNs = SystemClock.elapsedRealtimeNanos()
        } else {
            failFrameLocked(frame.presentationTimeStamp, frame.duration, frame.outputHandler)
        }
    }

    private fun releaseOutputBufferLocked(codec: MediaCodec, index: Int, render: Boolean): Boolean {
        return try {
            codec.releaseOutputBuffer(index, render)
            true
        } catch (error: Exception) {
            Log.i(TAG, "video-decoder-${this.index}: Failed to release output buffer: $error")
            failed = true
            failFramesLocked()
            false
        }
    }

    private fun countDroppedFrameLocked(reason: String) {
        numberOfDroppedFrames += 1
        val nowMs = SystemClock.elapsedRealtime()
        if (nowMs - lastDropLogMs > 5000) {
            lastDropLogMs = nowMs
            Log.i(TAG, "video-decoder-$index: Dropped $numberOfDroppedFrames frame(s), latest because $reason")
            numberOfDroppedFrames = 0
        }
    }

    private fun dropPendingFrameLocked(frame: PendingFrame, reason: String) {
        dropFrameLocked(frame.presentationTimeStamp, frame.duration, frame.outputHandler, reason)
    }

    private fun dropDecodedFrameLocked(frame: DecodedFrame, reason: String) {
        dropFrameLocked(frame.presentationTimeStamp, frame.duration, frame.outputHandler, reason)
    }

    private fun dropFrameLocked(
        presentationTimeStamp: Long,
        duration: Long,
        outputHandler: VTDecompressionOutputHandler?,
        reason: String,
    ) {
        countDroppedFrameLocked(reason)
        addDroppedFrameLocked(noErr, VTDecodeInfoFlags._FrameDropped, presentationTimeStamp, duration, outputHandler)
    }

    private fun failFramesLocked() {
        for (frame in pendingFrames) {
            failFrameLocked(frame.presentationTimeStamp, frame.duration, frame.outputHandler)
        }
        pendingFrames.clear()
        outputHandlers.drain { presentationTimeStamp, duration, outputHandler ->
            failFrameLocked(presentationTimeStamp, duration, outputHandler)
        }
        for (frame in decodedFrames) {
            failFrameLocked(frame.presentationTimeStamp, frame.duration, frame.outputHandler)
        }
        decodedFrames.clear()
        val frame = renderingFrame ?: return
        renderingFrame = null
        failFrameLocked(frame.presentationTimeStamp, frame.duration, frame.outputHandler)
    }

    private fun failFrameLocked(
        presentationTimeStamp: Long,
        duration: Long,
        outputHandler: VTDecompressionOutputHandler?,
    ) {
        addDroppedFrameLocked(kVTVideoDecoderMalfunctionErr, 0, presentationTimeStamp, duration, outputHandler)
    }

    private fun addDroppedFrameLocked(
        status: Int,
        infoFlags: Int,
        presentationTimeStamp: Long,
        duration: Long,
        outputHandler: VTDecompressionOutputHandler?,
    ) {
        if (invalidated) {
            return
        }
        PipelineStats.increment("decDrop")
        if (outputHandler != null) {
            droppedFrames.add(DroppedFrame(status, infoFlags, presentationTimeStamp, duration, outputHandler))
        }
    }

    private fun reportDroppedFrames() {
        val frames = synchronized(lock) {
            if (droppedFrames.isEmpty()) {
                return
            }
            val frames = droppedFrames
            droppedFrames = ArrayList()
            frames
        }
        for (frame in frames) {
            invokeOutputHandler(
                frame.outputHandler,
                frame.status,
                frame.infoFlags,
                null,
                frame.presentationTimeStamp,
                frame.duration,
            )
        }
    }

    private fun invokeOutputHandler(
        outputHandler: VTDecompressionOutputHandler,
        status: Int,
        infoFlags: Int,
        imageBuffer: CVPixelBuffer?,
        presentationTimeStamp: Long,
        duration: Long,
    ) {
        try {
            outputHandler(status, infoFlags, imageBuffer, presentationTimeStamp, duration)
        } catch (error: Throwable) {
            Log.i(TAG, "video-decoder-$index: Output handler failed: $error")
        }
    }

    internal fun updateOutputSize(format: MediaFormat) {
        val width = readInteger(format, MediaFormat.KEY_WIDTH) ?: return
        val height = readInteger(format, MediaFormat.KEY_HEIGHT) ?: return
        val cropLeft = readInteger(format, "crop-left")
        val cropRight = readInteger(format, "crop-right")
        val cropTop = readInteger(format, "crop-top")
        val cropBottom = readInteger(format, "crop-bottom")
        if (cropLeft != null && cropRight != null && cropTop != null && cropBottom != null) {
            outputWidth = cropRight - cropLeft + 1
            outputHeight = cropBottom - cropTop + 1
        } else {
            outputWidth = width
            outputHeight = height
        }
        Log.i(TAG, "video-decoder-$index: Output size ${outputWidth}x$outputHeight")
    }

    internal fun onFrameAvailable() {
        try {
            handleFrame()
        } catch (error: Throwable) {
            Log.i(TAG, "video-decoder-$index: Frame handling failed: $error")
        } finally {
            PixelBufferTurn.end()
        }
    }

    private fun handleFrame() {
        val surfaceTexture = surfaceTexture ?: return
        if (invalidated) {
            return
        }
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(stMatrix)
        val frame = synchronized(lock) {
            val frame = renderingFrame
            renderingFrame = null
            frame
        }
        callbackHandler?.post {
            synchronized(lock) {
                renderNextLocked()
            }
            reportDroppedFrames()
        }
        if (frame == null) {
            return
        }
        val outputHandler = frame.outputHandler ?: return
        outputFrame(frame.presentationTimeStamp, frame.duration, outputHandler)
    }

    internal fun outputFrame(
        presentationTimeStamp: Long,
        duration: Long,
        outputHandler: VTDecompressionOutputHandler,
    ) {
        if (outputWidth <= 0 || outputHeight <= 0) {
            val format = try {
                codec?.outputFormat
            } catch (error: Exception) {
                null
            }
            if (format != null) {
                updateOutputSize(format)
            }
        }
        val width = outputWidth
        val height = outputHeight
        val buffer = if (width > 0 && height > 0) outputPixelBufferPool(width, height).createPixelBuffer() else null
        if (buffer == null) {
            synchronized(lock) {
                dropFrameLocked(presentationTimeStamp, duration, outputHandler, "no output pixel buffer is free")
            }
            reportDroppedFrames()
            return
        }
        GlRenderer.drawOes(oesTexture, stMatrix, buffer, 0, false)
        PipelineStats.increment("decOut")
        invokeOutputHandler(outputHandler, noErr, 0, buffer, presentationTimeStamp, duration)
    }

    internal fun outputPixelBufferPool(width: Int, height: Int): CVPixelBufferPool {
        val existing = pool
        if (existing != null && existing.width == width && existing.height == height) {
            return existing
        }
        existing?.invalidate()
        val newPool = CVPixelBufferPool(
            width = width,
            height = height,
            pixelFormatType = pixelFormatType,
            maximumBufferCount = maximumNumberOfOutputBuffers,
        )
        newPool.name = "decoder"
        pool = newPool
        return newPool
    }
}

internal fun sampleDataToAnnexB(data: ByteArray): ByteArray? {
    if (data.isEmpty()) {
        return null
    }
    val annexB = data.copyOf()
    var offset = 0
    while (offset + 4 <= annexB.size) {
        val length = ((annexB[offset].toInt() and 0xFF) shl 24) or
            ((annexB[offset + 1].toInt() and 0xFF) shl 16) or
            ((annexB[offset + 2].toInt() and 0xFF) shl 8) or
            (annexB[offset + 3].toInt() and 0xFF)
        if (length < 0 || length > annexB.size - offset - 4) {
            break
        }
        annexB[offset] = 0
        annexB[offset + 1] = 0
        annexB[offset + 2] = 0
        annexB[offset + 3] = 1
        offset += 4 + length
    }
    if (offset == annexB.size) {
        return annexB
    }
    if (data.size >= 4 && data[0].toInt() == 0 && data[1].toInt() == 0 &&
        (data[2].toInt() == 1 || (data[2].toInt() == 0 && data[3].toInt() == 1))
    ) {
        return data
    }
    return null
}

internal fun annexBContainsKeyFrame(annexB: ByteArray, isHevc: Boolean): Boolean {
    var offset = 0
    while (offset + 3 < annexB.size) {
        if (annexB[offset].toInt() == 0 && annexB[offset + 1].toInt() == 0 && annexB[offset + 2].toInt() == 1) {
            val header = annexB[offset + 3].toInt() and 0xFF
            if (isHevc) {
                if (((header shr 1) and 0x3F) in 16..23) {
                    return true
                }
            } else if ((header and 0x1F) == 5) {
                return true
            }
            offset += 3
        } else {
            offset += 1
        }
    }
    return false
}

fun VTDecompressionSessionCreate(
    allocator: Any? = null,
    formatDescription: MediaFormat,
    decoderSpecification: Map<String, Any>? = null,
    imageBufferAttributes: Map<String, Any>? = null,
    outputCallback: Any? = null,
): Pair<Int, VTDecompressionSession?> {
    val mimeType = readString(formatDescription, MediaFormat.KEY_MIME) ?: return Pair(kVTParameterErr, null)
    val hardwareAccelerated =
        decoderSpecification?.get(kVTVideoDecoderSpecification_EnableHardwareAcceleratedVideoDecoder) as? Boolean
            ?: true
    val codecFormat = makeDecoderFormat(formatDescription, mimeType)
    val codecInfo = selectVideoDecoder(mimeType, codecFormat, hardwareAccelerated)
        ?: return Pair(kVTCouldNotFindVideoDecoderErr, null)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val capabilities = try {
            codecInfo.getCapabilitiesForType(mimeType)
        } catch (error: Exception) {
            null
        }
        if (capabilities?.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_LowLatency) == true) {
            codecFormat.setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
        }
    }
    val pixelFormatType = (imageBufferAttributes?.get(kCVPixelBufferPixelFormatTypeKey) as? Number)?.toInt()
        ?: kCVPixelFormatType_32BGRA
    val session = VTDecompressionSession(mimeType, codecInfo.name, codecFormat, pixelFormatType)
    val status = session.start()
    if (status != noErr) {
        return Pair(status, null)
    }
    return Pair(noErr, session)
}

fun VTDecompressionSessionDecodeFrame(
    session: VTDecompressionSession,
    sampleBuffer: MediaSample,
    flags: Int,
    outputHandler: VTDecompressionOutputHandler,
): Int {
    return session.decodeFrame(sampleBuffer, outputHandler)
}

fun VTDecompressionSessionInvalidate(session: VTDecompressionSession) {
    session.invalidateSession()
}

private fun makeDecoderFormat(formatDescription: MediaFormat, mimeType: String): MediaFormat {
    val width = readInteger(formatDescription, MediaFormat.KEY_WIDTH)?.takeIf { it > 0 } ?: defaultWidth
    val height = readInteger(formatDescription, MediaFormat.KEY_HEIGHT)?.takeIf { it > 0 } ?: defaultHeight
    val format = MediaFormat.createVideoFormat(mimeType, width, height)
    for (key in listOf("csd-0", "csd-1", "csd-2")) {
        val buffer = try {
            if (formatDescription.containsKey(key)) formatDescription.getByteBuffer(key) else null
        } catch (error: Exception) {
            null
        } ?: continue
        format.setByteBuffer(key, buffer.duplicate().also { it.rewind() })
    }
    format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maximumInputSize)
    format.setInteger(MediaFormat.KEY_PRIORITY, 0)
    return format
}

private fun selectVideoDecoder(mimeType: String, format: MediaFormat, hardwareAccelerated: Boolean): MediaCodecInfo? {
    val candidates = try {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { info ->
            !info.isEncoder && info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }
        }
    } catch (error: Exception) {
        Log.i(TAG, "Failed to list $mimeType decoders: $error")
        return null
    }
    val probe = MediaFormat.createVideoFormat(
        mimeType,
        readInteger(format, MediaFormat.KEY_WIDTH) ?: defaultWidth,
        readInteger(format, MediaFormat.KEY_HEIGHT) ?: defaultHeight,
    )
    val supported = candidates.filter { info ->
        try {
            info.getCapabilitiesForType(mimeType).isFormatSupported(probe)
        } catch (error: Exception) {
            false
        }
    }.ifEmpty { candidates }
    return supported.sortedBy { info -> if (isSoftwareDecoder(info) == hardwareAccelerated) 1 else 0 }.firstOrNull()
}

private fun isSoftwareDecoder(info: MediaCodecInfo): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return info.isSoftwareOnly
    }
    val name = info.name.lowercase()
    return name.startsWith("omx.google.") || name.startsWith("c2.android.") || name.contains(".sw.")
}

private fun readInteger(format: MediaFormat, key: String): Int? {
    return try {
        if (format.containsKey(key)) format.getInteger(key) else null
    } catch (error: Exception) {
        null
    }
}

private fun readString(format: MediaFormat, key: String): String? {
    return try {
        format.getString(key)
    } catch (error: Exception) {
        null
    }
}
