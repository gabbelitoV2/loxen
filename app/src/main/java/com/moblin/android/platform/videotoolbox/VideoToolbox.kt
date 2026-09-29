package com.moblin.android.platform.videotoolbox

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.opengl.EGL14
import android.opengl.EGLSurface
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Range
import android.view.Surface
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.mpeg.NalUnitInfo
import com.moblin.android.media.haishinkit.mpeg.getNalUnits
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.EglCore
import com.moblin.android.platform.video.GlRenderer
import java.util.TreeMap
import java.util.concurrent.atomic.AtomicInteger

const val noErr = 0
const val kVTPropertyNotSupportedErr = -12900
const val kVTParameterErr = -12902
const val kVTInvalidSessionErr = -12903
const val kVTCouldNotFindVideoEncoderErr = -12908
const val kVTEncodeInfo_FrameDropped = 1 shl 1
const val kVTProfileLevel_H264_Baseline_AutoLevel = "H264_Baseline_AutoLevel"
const val kVTProfileLevel_H264_Main_AutoLevel = "H264_Main_AutoLevel"
const val kVTProfileLevel_H264_High_AutoLevel = "H264_High_AutoLevel"
const val kVTProfileLevel_HEVC_Main_AutoLevel = "HEVC_Main_AutoLevel"
const val kVTProfileLevel_HEVC_Main10_AutoLevel = "HEVC_Main10_AutoLevel"
const val kVTDecompressionPropertyKey_PixelTransferProperties = "PixelTransferProperties"
const val kVTPixelTransferPropertyKey_DestinationYCbCrMatrix = "DestinationYCbCrMatrix"
const val kVTEncodeFrameOptionKey_ForceKeyFrame = "ForceKeyFrame"

typealias VTCompressionOutputHandler = (status: Int, infoFlags: Int, sampleBuffer: MediaSample?) -> Unit

private const val TAG = "MoblinEncoder"

private val nextSessionIndex = AtomicInteger(0)

private val liveBitrateKeys = setOf("AverageBitRate", "ConstantBitRate", "VariableBitRate")

class VTCompressionSession internal constructor(
    val width: Int,
    val height: Int,
    val mimeType: String,
    internal val codecInfo: MediaCodecInfo,
    codec: MediaCodec,
) {
    private val lock = Any()
    private val index = nextSessionIndex.incrementAndGet()
    private val isHevc = mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC
    private var codec: MediaCodec? = codec
    private val pendingProperties = mutableMapOf<String, Any>()
    private var thread: HandlerThread? = null
    private var callbackHandler: Handler? = null
    private var inputSurface: Surface? = null
    private var eglSurface: EGLSurface? = null

    @Volatile
    private var prepared = false

    @Volatile
    private var invalidated = false

    @Volatile
    private var failed = false

    private var started = false

    private var allowFrameReordering = false

    @Volatile
    private var reorderingDetected = false

    @Volatile
    private var latestOutputTimeNs = 0L
    private var numberOfUnmatchedOutputs = 0
    private var scalingMode = GlRenderer.ScalingMode.stretch
    private var bitrateRange: Range<Int>? = null
    private val outputHandlers = TreeMap<Long, VTCompressionOutputHandler>()
    private val decodeTimeStamps = ArrayDeque<Long>()
    private var latestInputPresentationTimeStamp = Long.MIN_VALUE
    private var latestOutputPresentationTimeStamp = Long.MIN_VALUE
    private var videoParameterSet: ByteArray? = null
    private var sequenceParameterSet: ByteArray? = null
    private var pictureParameterSet: ByteArray? = null
    private var formatDescription: MediaFormat? = null

    private val callback = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {}

        override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
            try {
                handleOutputBuffer(codec, index, info)
            } catch (e: Exception) {
                Log.i(TAG, "video-encoder-${this@VTCompressionSession.index}: Failed to handle output buffer: $e")
            }
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            handleError(e)
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            try {
                handleOutputFormatChanged(format)
            } catch (e: Exception) {
                Log.i(TAG, "video-encoder-$index: Failed to handle output format: $e")
            }
        }
    }

    internal val isReadyForMoreFrames: Boolean
        get() {
            if (!prepared || invalidated || failed) {
                return true
            }
            val numberOfPendingFrames = synchronized(lock) { outputHandlers.size }
            if (numberOfPendingFrames < maximumNumberOfPendingFrames()) {
                return true
            }
            return System.nanoTime() - latestOutputTimeNs > 2_000_000_000L
        }

    internal fun applyProperties(properties: Map<String, Any>): Int {
        if (invalidated) {
            return kVTInvalidSessionErr
        }
        if (!prepared) {
            synchronized(lock) {
                pendingProperties.putAll(properties)
            }
            return noErr
        }
        var status = noErr
        for ((key, value) in properties) {
            val result = applyLiveProperty(key, value)
            if (result != noErr && status == noErr) {
                status = result
            }
        }
        return status
    }

    internal fun prepareCodec(): Int {
        if (invalidated || failed) {
            return kVTInvalidSessionErr
        }
        if (prepared) {
            return noErr
        }
        val codec = codec ?: return kVTInvalidSessionErr
        val properties = synchronized(lock) {
            pendingProperties.toMap()
        }
        val configuration = try {
            makeEncoderConfiguration(codecInfo, mimeType, width, height, properties)
        } catch (e: Exception) {
            Log.i(TAG, "video-encoder-$index: Failed to create configuration: $e")
            null
        }
        if (configuration == null) {
            invalidated = true
            releaseResources()
            return kVTParameterErr
        }
        allowFrameReordering = configuration.allowFrameReordering
        scalingMode = configuration.scalingMode
        bitrateRange = configuration.bitrateRange
        val thread = HandlerThread("video-encoder-$index")
        thread.start()
        this.thread = thread
        val handler = Handler(thread.looper)
        callbackHandler = handler
        try {
            configure(codec, configuration, handler)
            val surface = codec.createInputSurface()
            inputSurface = surface
            codec.start()
            started = true
            val eglSurface = runOnPipeline { EglCore.createWindowSurface(surface) }
            if (eglSurface == EGL14.EGL_NO_SURFACE) {
                throw IllegalStateException("No EGL window surface for the encoder input surface")
            }
            this.eglSurface = eglSurface
        } catch (e: Throwable) {
            Log.i(TAG, "video-encoder-$index: Failed to prepare ${codecInfo.name} ${width}x$height: $e")
            invalidated = true
            releaseResources()
            return kVTParameterErr
        }
        latestOutputTimeNs = System.nanoTime()
        prepared = true
        Log.i(TAG, configuration.summary)
        return noErr
    }

    internal fun encodeImageBuffer(
        imageBuffer: CVPixelBuffer,
        presentationTimeStamp: Long,
        frameProperties: Map<String, Any>?,
        outputHandler: VTCompressionOutputHandler,
    ): Int {
        if (!PipelineThread.isCurrent()) {
            return try {
                PipelineThread.runSync {
                    encodeImageBuffer(imageBuffer, presentationTimeStamp, frameProperties, outputHandler)
                }
            } catch (e: Throwable) {
                Log.i(TAG, "video-encoder-$index: Encode on the pipeline thread failed: $e")
                kVTParameterErr
            }
        }
        if (invalidated || failed) {
            return kVTInvalidSessionErr
        }
        if (!prepared) {
            val status = prepareCodec()
            if (status != noErr) {
                return status
            }
        }
        val surface = eglSurface ?: return kVTInvalidSessionErr
        if (!imageBuffer.checkReadable("video encoder")) {
            PipelineStats.increment("encDrop")
            postDroppedFrame(outputHandler)
            return noErr
        }
        if (presentationTimeStamp <= latestInputPresentationTimeStamp) {
            logOnce("video-encoder: Dropping frames with non-increasing presentation time stamps")
            return kVTParameterErr
        }
        val (numberOfPendingFrames, oldestPendingPresentationTimeStamp) = synchronized(lock) {
            Pair(outputHandlers.size, outputHandlers.keys.firstOrNull() ?: presentationTimeStamp)
        }
        if (numberOfPendingFrames >= maximumNumberOfPendingFrames()) {
            if (presentationTimeStamp - oldestPendingPresentationTimeStamp > 2_000_000) {
                Log.i(
                    TAG,
                    "video-encoder-$index: No output for $numberOfPendingFrames pending frames in over 2 s, " +
                        "failing session",
                )
                failed = true
                return kVTInvalidSessionErr
            }
            PipelineStats.increment("encDrop")
            postDroppedFrame(outputHandler)
            return noErr
        }
        try {
            EglCore.makeCurrent(surface)
        } catch (e: Throwable) {
            Log.i(TAG, "video-encoder-$index: Failed to make surface current: $e")
            failed = true
            return kVTInvalidSessionErr
        }
        if (EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW) != surface) {
            val error = Integer.toHexString(EGL14.eglGetError())
            Log.i(TAG, "video-encoder-$index: Encoder surface is not current (0x$error)")
            failed = true
            return kVTInvalidSessionErr
        }
        try {
            GlRenderer.bind(null)
            GlRenderer.draw(imageBuffer, width, height, scalingMode)
        } catch (e: Throwable) {
            logOnce("video-encoder: Failed to draw frame: $e")
            restorePbuffer()
            return kVTParameterErr
        }
        EglCore.setPresentationTime(surface, presentationTimeStamp * 1000)
        synchronized(lock) {
            outputHandlers[presentationTimeStamp] = outputHandler
            decodeTimeStamps.addLast(presentationTimeStamp)
        }
        latestInputPresentationTimeStamp = presentationTimeStamp
        if (frameProperties?.get(kVTEncodeFrameOptionKey_ForceKeyFrame) == true) {
            requestKeyFrame()
        }
        val result = EglCore.swapBuffers(surface)
        restorePbuffer()
        if (result != EGL14.EGL_SUCCESS) {
            synchronized(lock) {
                outputHandlers.remove(presentationTimeStamp)
                decodeTimeStamps.remove(presentationTimeStamp)
            }
            Log.i(TAG, "video-encoder-$index: Swap failed with EGL error 0x${Integer.toHexString(result)}")
            if (result == EGL14.EGL_BAD_SURFACE || result == EGL14.EGL_BAD_NATIVE_WINDOW) {
                failed = true
                return kVTInvalidSessionErr
            }
            return kVTParameterErr
        }
        PipelineStats.increment("encIn")
        return noErr
    }

    internal fun invalidateCodec() {
        synchronized(lock) {
            if (invalidated) {
                return
            }
            invalidated = true
            outputHandlers.clear()
            decodeTimeStamps.clear()
        }
        val looper = thread?.looper
        if (looper != null && Looper.myLooper() == looper) {
            PipelineThread.post {
                releaseResources()
            }
        } else {
            releaseResources()
        }
    }

    private fun maximumNumberOfPendingFrames(): Int {
        return if (allowFrameReordering || reorderingDetected) 8 else 5
    }

    private fun configure(codec: MediaCodec, configuration: EncoderConfiguration, handler: Handler) {
        codec.setCallback(callback, handler)
        try {
            codec.configure(configuration.format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (e: Exception) {
            Log.i(
                TAG,
                "video-encoder-$index: Configure ${codecInfo.name} with ${configuration.format} failed ($e), " +
                    "retrying with ${configuration.fallbackFormat}",
            )
            codec.reset()
            codec.setCallback(callback, handler)
            codec.configure(configuration.fallbackFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            allowFrameReordering = false
        }
    }

    private fun applyLiveProperty(key: String, value: Any): Int {
        return when (key) {
            in liveBitrateKeys -> setBitrate(value)
            "DataRateLimits" -> {
                logOnce("DataRateLimits has no MediaCodec equivalent, ignored")
                noErr
            }
            else -> kVTPropertyNotSupportedErr
        }
    }

    private fun setBitrate(value: Any): Int {
        val requested = (value as? Number)?.toInt() ?: return kVTParameterErr
        val bitrate = bitrateRange?.clamp(requested) ?: requested
        val codec = codec ?: return kVTInvalidSessionErr
        return try {
            codec.setParameters(Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bitrate) })
            Log.i(TAG, "setParameters bitrate=$bitrate")
            noErr
        } catch (e: IllegalStateException) {
            Log.i(TAG, "video-encoder-$index: Failed to set bitrate $bitrate: $e")
            kVTInvalidSessionErr
        }
    }

    private fun requestKeyFrame() {
        try {
            codec?.setParameters(Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0) })
        } catch (e: IllegalStateException) {
            Log.i(TAG, "video-encoder-$index: Failed to request key frame: $e")
        }
    }

    private fun postDroppedFrame(outputHandler: VTCompressionOutputHandler) {
        val handler = callbackHandler
        if (handler == null) {
            invokeOutputHandler(outputHandler, noErr, kVTEncodeInfo_FrameDropped, null)
            return
        }
        handler.post {
            invokeOutputHandler(outputHandler, noErr, kVTEncodeInfo_FrameDropped, null)
        }
    }

    private fun restorePbuffer() {
        try {
            EglCore.makePbufferCurrent()
        } catch (e: Throwable) {
            logOnce("video-encoder: Failed to make the pbuffer current: $e")
        }
    }

    private fun <T> runOnPipeline(block: () -> T): T {
        return if (PipelineThread.isCurrent()) {
            block()
        } else {
            PipelineThread.runSync(block = block)
        }
    }

    private fun releaseResources() {
        val eglSurface = this.eglSurface
        this.eglSurface = null
        if (eglSurface != null) {
            val destroy: () -> Unit = {
                restorePbuffer()
                EglCore.destroySurface(eglSurface)
            }
            try {
                runOnPipeline(destroy)
            } catch (e: Throwable) {
                Log.i(TAG, "video-encoder-$index: Destroying the EGL surface later: $e")
                PipelineThread.post(destroy)
            }
        }
        val codec = this.codec
        this.codec = null
        if (codec != null) {
            if (started) {
                try {
                    codec.stop()
                } catch (e: Exception) {
                    Log.i(TAG, "video-encoder-$index: Stop failed: $e")
                }
            }
            started = false
            try {
                codec.release()
            } catch (e: Exception) {
                Log.i(TAG, "video-encoder-$index: Release failed: $e")
            }
        }
        inputSurface?.release()
        inputSurface = null
        thread?.quitSafely()
        thread = null
        callbackHandler = null
    }

    private fun handleOutputBuffer(codec: MediaCodec, bufferIndex: Int, info: MediaCodec.BufferInfo) {
        if (invalidated) {
            return
        }
        val data = try {
            val buffer = codec.getOutputBuffer(bufferIndex)
            val bytes = if (buffer != null && info.size > 0) {
                buffer.position(info.offset)
                buffer.limit(info.offset + info.size)
                ByteArray(info.size).also { buffer.get(it) }
            } else {
                ByteArray(0)
            }
            codec.releaseOutputBuffer(bufferIndex, false)
            bytes
        } catch (e: IllegalStateException) {
            return
        }
        if (data.isEmpty()) {
            return
        }
        handleOutputData(data, info.flags, info.presentationTimeUs)
    }

    private fun handleOutputData(data: ByteArray, flags: Int, presentationTimeStamp: Long) {
        latestOutputTimeNs = System.nanoTime()
        val nalUnits = getNalUnits(data).asReversed()
        val keptNalUnits = mutableListOf<NalUnitInfo>()
        var vps: ByteArray? = null
        var sps: ByteArray? = null
        var pps: ByteArray? = null
        var hasPicture = false
        var hasRandomAccessPicture = false
        for (nalUnit in nalUnits) {
            val offset = nalUnit.dataOffset()
            val header = data[offset].toInt() and 0xFF
            if (isHevc) {
                when (val type = (header shr 1) and 0x3F) {
                    32 -> vps = copyNalUnit(data, nalUnit)
                    33 -> sps = copyNalUnit(data, nalUnit)
                    34 -> pps = copyNalUnit(data, nalUnit)
                    35 -> Unit
                    else -> {
                        if (type <= 31) {
                            hasPicture = true
                            if (type in 16..23) {
                                hasRandomAccessPicture = true
                            }
                        }
                        keptNalUnits.add(nalUnit)
                    }
                }
            } else {
                when (val type = header and 0x1F) {
                    7 -> sps = copyNalUnit(data, nalUnit)
                    8 -> pps = copyNalUnit(data, nalUnit)
                    9 -> Unit
                    else -> {
                        if (type in 1..5) {
                            hasPicture = true
                            if (type == 5) {
                                hasRandomAccessPicture = true
                            }
                        }
                        keptNalUnits.add(nalUnit)
                    }
                }
            }
        }
        updateParameterSets(vps, sps, pps)
        if ((flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 || !hasPicture) {
            return
        }
        val isKeyFrame = (flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0 || hasRandomAccessPicture
        val avcc = makeAvcc(data, keptNalUnits)
        val droppedHandlers = mutableListOf<VTCompressionOutputHandler>()
        val outputHandler: VTCompressionOutputHandler?
        val decodeTimeStamp: Long
        val format: MediaFormat?
        synchronized(lock) {
            if (presentationTimeStamp < latestOutputPresentationTimeStamp &&
                !allowFrameReordering &&
                !reorderingDetected
            ) {
                reorderingDetected = true
                Log.i(TAG, "video-encoder-$index: Encoder reorders frames although frame reordering is off")
            }
            latestOutputPresentationTimeStamp = maxOf(latestOutputPresentationTimeStamp, presentationTimeStamp)
            if (isKeyFrame || !(allowFrameReordering || reorderingDetected)) {
                val olderHandlers = outputHandlers.headMap(presentationTimeStamp)
                droppedHandlers.addAll(olderHandlers.values)
                olderHandlers.clear()
                while (true) {
                    val first = decodeTimeStamps.firstOrNull() ?: break
                    if (first >= presentationTimeStamp) {
                        break
                    }
                    decodeTimeStamps.removeFirst()
                }
            } else {
                val staleHandlers = outputHandlers.headMap(presentationTimeStamp - 1_000_000)
                droppedHandlers.addAll(staleHandlers.values)
                staleHandlers.clear()
            }
            outputHandler = outputHandlers.remove(presentationTimeStamp)
            decodeTimeStamp = if (outputHandler != null) {
                decodeTimeStamps.removeFirstOrNull() ?: presentationTimeStamp
            } else {
                presentationTimeStamp
            }
            format = formatDescription
        }
        for (droppedHandler in droppedHandlers) {
            PipelineStats.increment("encDrop")
            invokeOutputHandler(droppedHandler, noErr, kVTEncodeInfo_FrameDropped, null)
        }
        if (outputHandler == null) {
            numberOfUnmatchedOutputs += 1
            if (numberOfUnmatchedOutputs <= 5 || numberOfUnmatchedOutputs % 100 == 0) {
                Log.i(
                    TAG,
                    "video-encoder-$index: No pending frame for output presentation time stamp " +
                        "$presentationTimeStamp ($numberOfUnmatchedOutputs so far)",
                )
            }
            return
        }
        if (format == null) {
            logOnce("video-encoder: Dropping frame output before any parameter sets")
            PipelineStats.increment("encDrop")
            invokeOutputHandler(outputHandler, noErr, kVTEncodeInfo_FrameDropped, null)
            return
        }
        PipelineStats.increment("encOut")
        invokeOutputHandler(
            outputHandler,
            noErr,
            0,
            MediaSample(
                data = avcc,
                presentationTimeUs = presentationTimeStamp,
                isKeyFrame = isKeyFrame,
                format = format,
                decodeTimeStampUs = decodeTimeStamp,
            ),
        )
    }

    private fun handleOutputFormatChanged(format: MediaFormat) {
        Log.i(TAG, "video-encoder-$index: Output format $format")
        val hasFormatDescription = synchronized(lock) {
            formatDescription != null
        }
        if (hasFormatDescription) {
            return
        }
        val parameterSets = listOf("csd-0", "csd-1", "csd-2").mapNotNull { readByteBufferBytes(format, it) }
        if (parameterSets.isEmpty()) {
            return
        }
        val data = parameterSets.fold(ByteArray(0)) { result, bytes -> result + bytes }
        var vps: ByteArray? = null
        var sps: ByteArray? = null
        var pps: ByteArray? = null
        for (nalUnit in getNalUnits(data)) {
            val header = data[nalUnit.dataOffset()].toInt() and 0xFF
            val type = if (isHevc) (header shr 1) and 0x3F else header and 0x1F
            when {
                isHevc && type == 32 -> vps = copyNalUnit(data, nalUnit)
                isHevc && type == 33 -> sps = copyNalUnit(data, nalUnit)
                isHevc && type == 34 -> pps = copyNalUnit(data, nalUnit)
                !isHevc && type == 7 -> sps = copyNalUnit(data, nalUnit)
                !isHevc && type == 8 -> pps = copyNalUnit(data, nalUnit)
            }
        }
        updateParameterSets(vps, sps, pps)
    }

    private fun handleError(e: MediaCodec.CodecException) {
        Log.i(
            TAG,
            "video-encoder-$index: Codec error ${e.diagnosticInfo} code=${e.errorCode} " +
                "transient=${e.isTransient} recoverable=${e.isRecoverable}",
        )
        failed = true
        val pendingHandlers = synchronized(lock) {
            val handlers = outputHandlers.values.toList()
            outputHandlers.clear()
            decodeTimeStamps.clear()
            handlers
        }
        val status = if (e.errorCode != 0) e.errorCode else kVTInvalidSessionErr
        for (handler in pendingHandlers) {
            invokeOutputHandler(handler, status, 0, null)
        }
    }

    private fun updateParameterSets(vps: ByteArray?, sps: ByteArray?, pps: ByteArray?) {
        if (vps == null && sps == null && pps == null) {
            return
        }
        val format: MediaFormat
        synchronized(lock) {
            val newVps = vps ?: videoParameterSet
            val newSps = sps ?: sequenceParameterSet
            val newPps = pps ?: pictureParameterSet
            if (newVps contentEquals videoParameterSet &&
                newSps contentEquals sequenceParameterSet &&
                newPps contentEquals pictureParameterSet &&
                formatDescription != null
            ) {
                return
            }
            videoParameterSet = newVps
            sequenceParameterSet = newSps
            pictureParameterSet = newPps
            if (newSps == null || newPps == null || (isHevc && newVps == null)) {
                return
            }
            format = makeVideoFormatDescription(mimeType, width, height, newVps, newSps, newPps)
            formatDescription = format
        }
        val recordKey = if (isHevc) "hvcC" else "avcC"
        val record = readByteBufferBytes(format, recordKey)
        if (record == null) {
            Log.i(TAG, "video-encoder-$index: Failed to create $recordKey")
        } else {
            Log.i(TAG, "$recordKey ${hexPrefix(record, 16)}")
        }
    }

    private fun invokeOutputHandler(
        outputHandler: VTCompressionOutputHandler,
        status: Int,
        infoFlags: Int,
        sampleBuffer: MediaSample?,
    ) {
        try {
            outputHandler(status, infoFlags, sampleBuffer)
        } catch (e: Throwable) {
            Log.i(TAG, "video-encoder-$index: Output handler failed: $e")
        }
    }

    private fun copyNalUnit(data: ByteArray, nalUnit: NalUnitInfo): ByteArray {
        val offset = nalUnit.dataOffset()
        return data.copyOfRange(offset, offset + nalUnit.dataLength)
    }

    private fun makeAvcc(data: ByteArray, nalUnits: List<NalUnitInfo>): ByteArray {
        var size = 0
        for (nalUnit in nalUnits) {
            size += 4 + nalUnit.dataLength
        }
        val avcc = ByteArray(size)
        var position = 0
        for (nalUnit in nalUnits) {
            val length = nalUnit.dataLength
            avcc[position] = ((length ushr 24) and 0xFF).toByte()
            avcc[position + 1] = ((length ushr 16) and 0xFF).toByte()
            avcc[position + 2] = ((length ushr 8) and 0xFF).toByte()
            avcc[position + 3] = (length and 0xFF).toByte()
            System.arraycopy(data, nalUnit.dataOffset(), avcc, position + 4, length)
            position += 4 + length
        }
        return avcc
    }
}

fun VTCompressionSessionCreate(
    width: Int,
    height: Int,
    codecType: String,
    imageBufferAttributes: Map<String, Any>?,
): Pair<Int, VTCompressionSession?> {
    if (width <= 0 || height <= 0) {
        Log.i(TAG, "Invalid session size ${width}x$height")
        return Pair(kVTParameterErr, null)
    }
    val mimeType = normalizeVideoMimeType(codecType)
    val candidates = selectVideoEncoders(mimeType, width, height)
    if (candidates.isEmpty()) {
        Log.i(TAG, "No $mimeType encoder supports ${width}x$height")
        return Pair(kVTCouldNotFindVideoEncoderErr, null)
    }
    for (codecInfo in candidates) {
        val codec = try {
            MediaCodec.createByCodecName(codecInfo.name)
        } catch (e: Exception) {
            Log.i(TAG, "Failed to create ${codecInfo.name}: $e")
            continue
        }
        if (imageBufferAttributes != null) {
            logOnce("Image buffer attributes are not used, the encoder draws RGBA textures to its input surface")
        }
        return Pair(noErr, VTCompressionSession(width, height, mimeType, codecInfo, codec))
    }
    return Pair(kVTCouldNotFindVideoEncoderErr, null)
}

fun VTSessionSetProperties(session: VTCompressionSession, properties: Map<String, Any>): Int {
    return session.applyProperties(properties)
}

fun VTCompressionSessionPrepareToEncodeFrames(session: VTCompressionSession): Int {
    return session.prepareCodec()
}

fun VTCompressionSessionEncodeFrame(
    session: VTCompressionSession,
    imageBuffer: CVPixelBuffer,
    presentationTimeStamp: Long,
    duration: Long,
    frameProperties: Map<String, Any>?,
    outputHandler: VTCompressionOutputHandler,
): Int {
    return session.encodeImageBuffer(imageBuffer, presentationTimeStamp, frameProperties, outputHandler)
}

fun VTCompressionSessionInvalidate(session: VTCompressionSession) {
    session.invalidateCodec()
}
