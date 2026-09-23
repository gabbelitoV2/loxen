package com.moblin.android.media

import android.media.AudioFormat
import android.media.MediaFormat
import com.moblin.android.platform.video.CVPixelBuffer

const val kCMTimeInvalidUs: Long = -1L

class MediaSample(
    var data: ByteArray,
    val presentationTimeUs: Long,
    val isKeyFrame: Boolean,
    val format: MediaFormat?,
    val imageBuffer: CVPixelBuffer? = null,
    val durationUs: Long = kCMTimeInvalidUs,
    val decodeTimeStampUs: Long = kCMTimeInvalidUs,
) {
    val numSamples: Int
        get() {
            val format = format ?: return 1
            if (!isRawPcm(format)) {
                return 1
            }
            val channels = readInteger(format, MediaFormat.KEY_CHANNEL_COUNT) ?: 1
            val encoding = readInteger(format, MediaFormat.KEY_PCM_ENCODING) ?: AudioFormat.ENCODING_PCM_16BIT
            val frameSize = channels * bytesPerSample(encoding)
            if (frameSize <= 0) {
                return 0
            }
            return data.size / frameSize
        }

    fun replacePresentationTimeStamp(presentationTimeStamp: Long): MediaSample {
        return MediaSample(
            data = data,
            presentationTimeUs = presentationTimeStamp,
            isKeyFrame = isKeyFrame,
            format = format,
            imageBuffer = imageBuffer,
            durationUs = durationUs,
            decodeTimeStampUs = decodeTimeStampUs,
        )
    }

    fun setAttachmentDisplayImmediately() {}

    private fun isRawPcm(format: MediaFormat): Boolean {
        val mime = runCatching { format.getString(MediaFormat.KEY_MIME) }.getOrNull()
        if (mime != null) {
            return mime == MediaFormat.MIMETYPE_AUDIO_RAW
        }
        return runCatching { format.containsKey(MediaFormat.KEY_PCM_ENCODING) }.getOrDefault(false)
    }

    private fun readInteger(format: MediaFormat, key: String): Int? {
        return runCatching {
            if (format.containsKey(key)) {
                format.getInteger(key)
            } else {
                null
            }
        }.getOrNull()
    }

    private fun bytesPerSample(encoding: Int): Int {
        return when (encoding) {
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_FLOAT -> 4
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
            AudioFormat.ENCODING_PCM_32BIT -> 4
            else -> 2
        }
    }
}
