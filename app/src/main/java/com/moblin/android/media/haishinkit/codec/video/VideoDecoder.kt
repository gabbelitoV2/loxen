package com.moblin.android.media.haishinkit.codec.video

import android.media.MediaFormat
import android.util.Log
import com.moblin.android.common.various.create
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.extension.CMVideoFormatDescription
import com.moblin.android.media.haishinkit.extension.decodeFrame
import com.moblin.android.media.haishinkit.extension.invalidate
import com.moblin.android.media.haishinkit.extension.sdrYCbCrMatrix
import com.moblin.android.platform.video.kCVPixelBufferIOSurfacePropertiesKey
import com.moblin.android.platform.video.kCVPixelBufferMetalCompatibilityKey
import com.moblin.android.platform.video.kCVPixelBufferPixelFormatTypeKey
import com.moblin.android.platform.videotoolbox.VTDecompressionSession
import com.moblin.android.platform.videotoolbox.VTDecompressionSessionCreate
import com.moblin.android.platform.videotoolbox.VTSessionSetProperty
import com.moblin.android.platform.videotoolbox.kVTDecompressionPropertyKey_PixelTransferProperties
import com.moblin.android.platform.videotoolbox.kVTInvalidSessionErr
import com.moblin.android.platform.videotoolbox.kVTPixelTransferPropertyKey_DestinationYCbCrMatrix
import com.moblin.android.platform.videotoolbox.kVTVideoDecoderSpecification_EnableHardwareAcceleratedVideoDecoder
import com.moblin.android.platform.videotoolbox.noErr
import com.moblin.android.various.settings.SettingsStreamColorRange
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "VideoDecoder"

interface VideoDecoderDelegate {
    fun videoDecoderOutputSampleBuffer(codec: VideoDecoder, sampleBuffer: MediaSample)
}

class VideoDecoder(
    private val name: String,
    private val lockQueue: CoroutineScope,
    private val softwareDecoding: Boolean,
    private val colorRange: SettingsStreamColorRange,
) {
    private var isRunning = false
    private var formatDescription: MediaFormat? = null
    private var delegateReference: WeakReference<VideoDecoderDelegate>? = null
    var delegate: VideoDecoderDelegate?
        get() = delegateReference?.get()
        set(value) {
            delegateReference = value?.let { WeakReference(it) }
        }
    private var invalidateSession = true
    private var numberOfFailedFrames = 0
    private var latestFailedFrameStatus: Int = noErr
    private var session: VTDecompressionSession? = null
        set(value) {
            val oldValue = field
            field = value
            oldValue?.invalidate()
            invalidateSession = false
        }

    @Synchronized fun startRunning(formatDescription: MediaFormat? = null) {
        isRunning = true
        invalidateSession = true
        numberOfFailedFrames = 0
        this.formatDescription = formatDescription
    }

    @Synchronized fun stopRunning() {
        session = null
        invalidateSession = true
        formatDescription = null
        isRunning = false
    }

    @Synchronized fun decodeSampleBuffer(sampleBuffer: MediaSample) {
        if (!isRunning) {
            return
        }
        if (invalidateSession) {
            session = makeSession()
        }
        val err = session
            ?.decodeFrame(sampleBuffer) { status, _, imageBuffer, presentationTimeStamp, duration ->
                if (imageBuffer == null || status != noErr) {
                    lockQueue.launch {
                        numberOfFailedFrames += 1
                        latestFailedFrameStatus = status
                    }
                    return@decodeFrame
                }
                val formatDescription = CMVideoFormatDescription.create(imageBuffer = imageBuffer)
                    ?: return@decodeFrame
                val sampleBuffer = create(
                    imageBuffer,
                    formatDescription,
                    duration,
                    presentationTimeStamp,
                    sampleBuffer.decodeTimeStampUs,
                ) ?: return@decodeFrame
                com.moblin.android.platform.video.retainLease(sampleBuffer)
                lockQueue.launch {
                    try { logFailedFrames(); delegate?.videoDecoderOutputSampleBuffer(this@VideoDecoder, sampleBuffer) } finally { com.moblin.android.platform.video.releaseLease(sampleBuffer) }
                }
            }
        if (err == kVTInvalidSessionErr) {
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

    private fun makeSession(): VTDecompressionSession? {
        val formatDescription = formatDescription
        if (formatDescription == null) {
            Log.i(TAG, "video-decoder: $name: Format description missing")
            return null
        }
        val attributes: Map<String, Any> = mapOf(
            kCVPixelBufferPixelFormatTypeKey to colorRange.pixelFormatType(),
            kCVPixelBufferIOSurfacePropertiesKey to emptyMap<String, Any>(),
            kCVPixelBufferMetalCompatibilityKey to true,
        )
        var decoderSpecification: Map<String, Any>? = null
        if (softwareDecoding) {
            decoderSpecification = mapOf(
                kVTVideoDecoderSpecification_EnableHardwareAcceleratedVideoDecoder to false,
            )
        }
        val (status, session) = VTDecompressionSessionCreate(
            allocator = null,
            formatDescription = formatDescription,
            decoderSpecification = decoderSpecification,
            imageBufferAttributes = attributes,
            outputCallback = null,
        )
        if (status != noErr || session == null) {
            Log.i(TAG, "video-decoder: $name: Failed to create session with status $status")
            return null
        }
        val pixelTransferProperties = mapOf(
            kVTPixelTransferPropertyKey_DestinationYCbCrMatrix to sdrYCbCrMatrix(colorRange),
        )
        val propertyStatus = VTSessionSetProperty(
            session,
            kVTDecompressionPropertyKey_PixelTransferProperties,
            pixelTransferProperties,
        )
        if (propertyStatus != noErr) {
            Log.i(TAG, "video-decoder: $name: Failed to set pixel transfer properties $propertyStatus")
        }
        return session
    }
}
