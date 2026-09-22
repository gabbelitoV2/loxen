package com.moblin.android.common.various

import android.media.AudioFormat
import android.media.Image
import android.media.MediaFormat
import com.moblin.android.media.MediaSample
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val kCMSampleAttachmentKey_NotSync = "NotSync"
private const val kCMSampleAttachmentKey_DisplayImmediately = "DisplayImmediately"

fun create(
    imageBuffer: Image,
    formatDescription: MediaFormat,
    duration: Long,
    presentationTimeStamp: Long,
    decodeTimeStamp: Long
): MediaSample? {
    TODO()
}

fun createSilent(
    sampleRate: Int,
    channels: Int,
    presentationTimeUs: Long,
    samplesPerBuffer: Int
): MediaSample? {
    val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_RAW, sampleRate, channels)
    format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
    val data = ByteArray(samplesPerBuffer * channels * 2)
    return MediaSample(
        data = data,
        presentationTimeUs = presentationTimeUs,
        isKeyFrame = true,
        format = format
    )
}

fun MediaSample.setIsSync(value: Boolean): MediaSample {
    return MediaSample(
        data = data,
        presentationTimeUs = presentationTimeUs,
        isKeyFrame = value,
        format = format
    )
}

fun MediaSample.getIsSync(): Boolean {
    return !(getAttachmentValue(kCMSampleAttachmentKey_NotSync) ?: false)
}

fun MediaSample.muted(muted: Boolean): MediaSample? {
    if (!muted) {
        return this
    }
    return MediaSample(
        data = ByteArray(data.size),
        presentationTimeUs = presentationTimeUs,
        isKeyFrame = isKeyFrame,
        format = format
    )
}

fun MediaSample.withGain(gain: Float): MediaSample? {
    if (gain == 1.0f) {
        return this
    }
    return foreachAudioSample(
        float32 = { samples, count ->
            for (index in 0 until count) {
                samples[index] = samples[index] * gain
            }
        },
        int16 = { samples, count ->
            val fixedGain = (gain * 256).toInt()
            for (index in 0 until count) {
                samples[index] = ((samples[index].toInt() * fixedGain) shr 8)
                    .coerceIn(-32768, 32767)
                    .toShort()
            }
        }
    )
}

fun MediaSample.foreachAudioSample(
    float32: (FloatArray, Int) -> Unit,
    int16: (ShortArray, Int) -> Unit
): MediaSample? {
    val mediaFormat = format ?: return null
    val pcmEncoding = runCatching {
        if (mediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
        } else {
            AudioFormat.ENCODING_PCM_16BIT
        }
    }.getOrDefault(AudioFormat.ENCODING_PCM_16BIT)
    when (pcmEncoding) {
        AudioFormat.ENCODING_PCM_FLOAT -> {
            val count = data.size / 4
            val byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.nativeOrder())
            val samples = FloatArray(count)
            byteBuffer.asFloatBuffer().get(samples)
            float32(samples, count)
            byteBuffer.rewind()
            byteBuffer.asFloatBuffer().put(samples)
            return MediaSample(
                data = data,
                presentationTimeUs = presentationTimeUs,
                isKeyFrame = isKeyFrame,
                format = mediaFormat
            )
        }
        AudioFormat.ENCODING_PCM_16BIT -> {
            val count = data.size / 2
            val byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.nativeOrder())
            val samples = ShortArray(count)
            byteBuffer.asShortBuffer().get(samples)
            int16(samples, count)
            byteBuffer.rewind()
            byteBuffer.asShortBuffer().put(samples)
            return MediaSample(
                data = data,
                presentationTimeUs = presentationTimeUs,
                isKeyFrame = isKeyFrame,
                format = mediaFormat
            )
        }
        else -> return null
    }
}

private fun MediaSample.getAttachmentValue(key: String): Boolean? {
    return when (key) {
        kCMSampleAttachmentKey_NotSync -> !isKeyFrame
        else -> null
    }
}

fun MediaSample.setAttachmentDisplayImmediately() {
    Unit
}

private fun MediaSample.setAttachmentValue(key: String, value: Boolean) {
    Unit
}

fun MediaSample.replacePresentationTimeStamp(presentationTimeStamp: Long): MediaSample? {
    return MediaSample(
        data = data,
        presentationTimeUs = presentationTimeStamp,
        isKeyFrame = isKeyFrame,
        format = format
    )
}

fun MediaSample.deepCopyAudioSampleBuffer(): MediaSample? {
    val mediaFormat = format ?: return null
    return MediaSample(
        data = data.copyOf(),
        presentationTimeUs = presentationTimeUs,
        isKeyFrame = isKeyFrame,
        format = mediaFormat
    )
}
