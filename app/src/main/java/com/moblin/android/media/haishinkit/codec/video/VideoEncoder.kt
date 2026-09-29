package com.moblin.android.media.haishinkit.codec.video

import android.media.MediaFormat
import android.util.Log
import android.util.Size
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.extension.convertTo
import com.moblin.android.media.haishinkit.extension.encodeFrame
import com.moblin.android.media.haishinkit.extension.invalidate
import com.moblin.android.media.haishinkit.extension.colorAttachments
import com.moblin.android.media.haishinkit.extension.prepareToEncodeFrames
import com.moblin.android.media.haishinkit.extension.setProperties
import com.moblin.android.media.haishinkit.util.Atomic
import com.moblin.android.platform.video.CMVideoFormatDescriptionCreateForImageBuffer
import com.moblin.android.platform.video.CMVideoFormatDescriptionMatchesImageBuffer
import com.moblin.android.platform.video.CVImageBuffer
import com.moblin.android.platform.video.kCVPixelBufferHeightKey
import com.moblin.android.platform.video.kCVPixelBufferIOSurfacePropertiesKey
import com.moblin.android.platform.video.kCVPixelBufferMetalCompatibilityKey
import com.moblin.android.platform.video.kCVPixelBufferPixelFormatTypeKey
import com.moblin.android.platform.video.kCVPixelBufferWidthKey
import com.moblin.android.platform.videotoolbox.CMFormatDescriptionEqual
import com.moblin.android.platform.videotoolbox.VTCompressionSession
import com.moblin.android.platform.videotoolbox.VTCompressionSessionCreate
import com.moblin.android.platform.videotoolbox.kVTInvalidSessionErr
import com.moblin.android.platform.videotoolbox.noErr
import com.moblin.android.various.settings.SettingsStreamColorRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

var numberOfFailedEncodings = 0

interface VideoEncoderDelegate {
    fun videoEncoderOutputFormat(encoder: VideoEncoder, formatDescription: MediaFormat)

    fun videoEncoderOutputSampleBuffer(
        encoder: VideoEncoder,
        sampleBuffer: MediaSample,
        decodeTimeStampOffset: Long,
    )
}

interface VideoEncoderControlDelegate {
    fun videoEncoderControlResolutionChanged(encoder: VideoEncoder, resolution: Size)
}

class VideoEncoder(
    private val lockQueue: CoroutineScope,
    private val colorRange: SettingsStreamColorRange,
) {
    var settings: Atomic<VideoEncoderSettings> = Atomic(VideoEncoderSettings())
        set(value) {
            val oldValue = field.value
            field = value
            lockQueue.launch {
                if (settings.value.shouldInvalidateSession(oldValue)) {
                    invalidateSession = true
                    currentBitrate = 0
                }
            }
        }

    private var isRunning = false
    private var formatDescription: MediaFormat? = null
    var delegate: VideoEncoderDelegate? = null
    var controlDelegate: VideoEncoderControlDelegate? = null
    private var session: VTCompressionSession? = null
        set(value) {
            field?.invalidate()
            field = value
            invalidateSession = false
        }

    private var invalidateSession = true
    private var colorAttachments: Map<String, String> = emptyMap()
    private var colorAttachmentsFormatDescription: MediaFormat? = null
    var outputColorAttachments: Map<String, String>? = null
    private var currentBitrate = 0
    private var oldBitrateVideoSize: Size = Size(0, 0)

    fun startRunning(formatDescription: MediaFormat? = null) {
        lockQueue.launch {
            isRunning = true
            invalidateSession = true
            currentBitrate = 0
            this@VideoEncoder.formatDescription = formatDescription
            numberOfFailedEncodings = 0
            Log.i(TAG, "video-encoder: Starting with codec ${settings.value.format}")
        }
    }

    fun stopRunning() {
        lockQueue.launch {
            session = null
            invalidateSession = true
            currentBitrate = 0
            this@VideoEncoder.formatDescription = null
            isRunning = false
        }
    }

    fun encodeImageBuffer(imageBuffer: CVImageBuffer, presentationTimeStamp: Long, duration: Long) {
        if (!isRunning) {
            return
        }
        val settings = this.settings.value
        updateColorAttachments(imageBuffer)
        val newBitrateVideoSize = updateAdaptiveResolution(settings)
        if (newBitrateVideoSize != oldBitrateVideoSize) {
            session = makeSession(settings, newBitrateVideoSize)
            oldBitrateVideoSize = newBitrateVideoSize
            val resolution = Size(newBitrateVideoSize.width, newBitrateVideoSize.height)
            controlDelegate?.videoEncoderControlResolutionChanged(this, resolution)
        }
        if (invalidateSession) {
            session = makeSession(settings, oldBitrateVideoSize)
        }
        updateBitrate(settings)
        val err = session?.encodeFrame(
            imageBuffer,
            presentationTimeStamp = presentationTimeStamp,
            duration = duration,
        ) { status, _, sampleBuffer ->
            lockQueue.launch {
                if (sampleBuffer == null || status != noErr) {
                    Log.i(
                        TAG,
                        "video-encoder: Failed to encode frame status $status an got buffer ${sampleBuffer != null}",
                    )
                    numberOfFailedEncodings += 1
                    return@launch
                }
                setFormatDescription(sampleBuffer.format)
                delegate?.videoEncoderOutputSampleBuffer(
                    this@VideoEncoder,
                    sampleBuffer,
                    makeDecodeTimeStampOffset(settings),
                )
            }
        }
        if (err == kVTInvalidSessionErr) {
            Log.i(TAG, "video-encoder: Encode failed. Resetting session.")
            invalidateSession = true
            currentBitrate = 0
        }
    }

    private fun makeDecodeTimeStampOffset(settings: VideoEncoderSettings): Long {
        return if (settings.allowFrameReordering) {
            150_000L
        } else {
            0L
        }
    }

    private fun setFormatDescription(formatDescription: MediaFormat?) {
        if (CMFormatDescriptionEqual(formatDescription, this.formatDescription)) {
            return
        }
        this.formatDescription = formatDescription
        val description = formatDescription ?: return
        delegate?.videoEncoderOutputFormat(this, description)
    }

    private fun updateColorAttachments(imageBuffer: CVImageBuffer) {
        val colorAttachments: Map<String, String>
        val output = outputColorAttachments
        if (output != null) {
            colorAttachmentsFormatDescription = null
            colorAttachments = output
        } else {
            val description = colorAttachmentsFormatDescription
            if (description != null && CMVideoFormatDescriptionMatchesImageBuffer(description, imageBuffer)) {
                return
            }
            colorAttachmentsFormatDescription = CMVideoFormatDescriptionCreateForImageBuffer(imageBuffer)
            colorAttachments = imageBuffer.colorAttachments
        }
        if (colorAttachments != this.colorAttachments) {
            this.colorAttachments = colorAttachments
            invalidateSession = true
        }
    }

    private fun updateBitrate(settings: VideoEncoderSettings) {
        if (currentBitrate == settings.bitrate) {
            return
        }
        currentBitrate = settings.bitrate
        val bitrate = currentBitrate
        val properties = settings.bitrateProperties(bitrate)
        val status = session?.setProperties(properties)
        if (status != null && status != noErr) {
            Log.i(TAG, "video-encoder: Failed to set bitrate options $status $properties")
        }
    }

    private fun getVideoSize(settings: VideoEncoderSettings): Size? {
        return if (settings.bitrate < settings.adaptiveResolution160Threshold) {
            settings.videoSize.convertTo(dimension = 160)
        } else if (settings.bitrate < settings.adaptiveResolution360Threshold) {
            settings.videoSize.convertTo(dimension = 360)
        } else if (settings.bitrate < settings.adaptiveResolution480Threshold) {
            settings.videoSize.convertTo(dimension = 480)
        } else if (settings.bitrate < settings.adaptiveResolution720Threshold) {
            settings.videoSize.convertTo(dimension = 720)
        } else if (settings.bitrate < settings.adaptiveResolution1080Threshold) {
            settings.videoSize.convertTo(dimension = 1080)
        } else {
            settings.videoSize
        }
    }

    private fun updateAdaptiveResolution(settings: VideoEncoderSettings): Size {
        val videoSize = if (settings.adaptiveResolution) {
            getVideoSize(settings)
        } else {
            settings.videoSize
        }
        if (videoSize == null || videoSize.height > settings.videoSize.height) {
            return settings.videoSize
        }
        return videoSize
    }

    private fun makeSession(settings: VideoEncoderSettings, videoSize: Size): VTCompressionSession? {
        val attributes = mapOf<String, Any>(
            kCVPixelBufferPixelFormatTypeKey to colorRange.pixelFormatType(),
            kCVPixelBufferIOSurfacePropertiesKey to emptyMap<String, Any>(),
            kCVPixelBufferMetalCompatibilityKey to true,
            kCVPixelBufferWidthKey to settings.videoSize.width,
            kCVPixelBufferHeightKey to settings.videoSize.height,
        )
        var (status, session) = VTCompressionSessionCreate(
            width = videoSize.width,
            height = videoSize.height,
            codecType = settings.format.codecType,
            imageBufferAttributes = attributes,
        )
        if (status != noErr || session == null) {
            Log.i(TAG, "video-encoder: Failed to create session with status $status")
            return null
        }
        status = session.setProperties(createColorProperties(colorAttachments))
        if (status != noErr) {
            Log.i(TAG, "video-encoder: Failed to set color properties with status $status")
        }
        status = session.setProperties(settings.properties())
        if (status != noErr) {
            Log.i(TAG, "video-encoder: Failed to set options with status $status")
            return null
        }
        status = session.prepareToEncodeFrames()
        if (status != noErr) {
            Log.i(TAG, "video-encoder: Failed to prepare with status $status")
            return null
        }
        return session
    }

    companion object {
        private const val TAG = "VideoEncoder"
    }
}
