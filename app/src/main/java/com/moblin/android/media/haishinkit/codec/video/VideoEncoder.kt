package com.moblin.android.media.haishinkit.codec.video

import android.media.Image
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.util.Atomic
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

class VideoEncoder(private val lockQueue: CoroutineScope) {
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

    private var session: MediaCodec? = null
        set(value) {
            runCatching { field?.stop() }
            runCatching { field?.release() }
            field = value
            invalidateSession = false
        }

    private var invalidateSession = true
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

    fun encodeImageBuffer(imageBuffer: Image, presentationTimeStamp: Long, duration: Long) {
        if (!isRunning) {
            return
        }
        val settings = this.settings.value
        val newBitrateVideoSize = updateAdaptiveResolution(settings)
        if (newBitrateVideoSize != oldBitrateVideoSize) {
            session = makeSession(settings, newBitrateVideoSize)
            oldBitrateVideoSize = newBitrateVideoSize
            val resolution = Size(newBitrateVideoSize.width, newBitrateVideoSize.height)
            controlDelegate?.videoEncoderControlResolutionChanged(this, resolution)
        }
        if (invalidateSession) {
            session = makeSession(settings)
        }
        updateBitrate(settings)
        val codec = session
        if (codec != null) {
            try {
                codec.encodeFrame(imageBuffer, presentationTimeStamp, duration) { status, sampleBuffer ->
                    lockQueue.launch {
                        if (sampleBuffer == null || status != 0) {
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
            } catch (e: MediaCodec.CodecException) {
                Log.i(TAG, "video-encoder: Encode failed. Resetting session.")
                invalidateSession = true
                currentBitrate = 0
            }
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
        if (formatDescription == this.formatDescription) {
            return
        }
        this.formatDescription = formatDescription
        val description = formatDescription ?: return
        delegate?.videoEncoderOutputFormat(this, description)
    }

    private fun updateBitrate(settings: VideoEncoderSettings) {
        if (currentBitrate == settings.bitrate) {
            return
        }
        currentBitrate = settings.bitrate
        val bitrate = currentBitrate
        val properties = settings.bitrateProperties(bitrate)
        val codec = session
        if (codec != null) {
            TODO(
                "apply $properties to the running MediaCodec; VTCompressionSession.setProperties has no " +
                    "MediaCodec equivalent that reports an OSStatus",
            )
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

    private fun makeSession(settings: VideoEncoderSettings, videoSize: Size? = null): MediaCodec? {
        TODO(
            "create and start a MediaCodec encoder for VideoEncoderSettings.format at " +
                "${videoSize?.width ?: settings.videoSize.width}x${videoSize?.height ?: settings.videoSize.height} " +
                "using the async callback API and settings.properties()",
        )
    }

    companion object {
        private const val TAG = "VideoEncoder"
    }
}

private fun MediaCodec.encodeFrame(
    imageBuffer: Image,
    presentationTimeStamp: Long,
    duration: Long,
    onEncoded: (status: Int, sampleBuffer: MediaSample?) -> Unit,
) {
    TODO(
        "copy $imageBuffer into a MediaCodec input buffer, queue it with presentationTimeUs=$presentationTimeStamp " +
            "and durationUs=$duration, then invoke onEncoded from the async output callback",
    )
}
