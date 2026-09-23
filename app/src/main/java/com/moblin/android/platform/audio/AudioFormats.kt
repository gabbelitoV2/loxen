package com.moblin.android.platform.audio

import android.media.AudioFormat
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "MoblinAudio"

const val audioCaptureSampleRate = 48000

const val audioCaptureFramesPerBuffer = 1024

private val encoderAvailability = ConcurrentHashMap<String, Boolean>()

fun makePcmFormat(sampleRate: Int, channels: Int): MediaFormat {
    val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_RAW, sampleRate, channels)
    format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
    return format
}

fun MediaFormat?.isSameAudioFormat(other: MediaFormat?): Boolean {
    if (this === other) {
        return true
    }
    if (this == null || other == null) {
        return false
    }
    return audioMime() == other.audioMime() &&
        audioInteger(MediaFormat.KEY_SAMPLE_RATE) == other.audioInteger(MediaFormat.KEY_SAMPLE_RATE) &&
        audioInteger(MediaFormat.KEY_CHANNEL_COUNT) == other.audioInteger(MediaFormat.KEY_CHANNEL_COUNT) &&
        pcmEncoding() == other.pcmEncoding()
}

fun remapChannels(
    input: ByteArray,
    frames: Int,
    inChannels: Int,
    channelMap: List<Int>,
    output: ByteArray,
): Int {
    val outChannels = channelMap.size
    if (frames <= 0 || inChannels <= 0 || outChannels <= 0) {
        return 0
    }
    val usableFrames = minOf(frames, input.size / (inChannels * 2), output.size / (outChannels * 2))
    val outBytes = usableFrames * outChannels * 2
    if (isIdentityMap(inChannels, channelMap)) {
        System.arraycopy(input, 0, output, 0, outBytes)
        return outBytes
    }
    var inOffset = 0
    var outOffset = 0
    for (frame in 0 until usableFrames) {
        for (outChannel in 0 until outChannels) {
            val inChannel = channelMap[outChannel]
            if (inChannel in 0 until inChannels) {
                val sourceOffset = inOffset + inChannel * 2
                output[outOffset] = input[sourceOffset]
                output[outOffset + 1] = input[sourceOffset + 1]
            } else {
                output[outOffset] = 0
                output[outOffset + 1] = 0
            }
            outOffset += 2
        }
        inOffset += inChannels * 2
    }
    return outBytes
}

fun MediaFormat.isRawPcmAudio(): Boolean {
    val mime = audioMime()
    if (mime != null) {
        return mime == MediaFormat.MIMETYPE_AUDIO_RAW
    }
    return runCatching { containsKey(MediaFormat.KEY_PCM_ENCODING) }.getOrDefault(false)
}

fun MediaFormat.audioSampleRate(): Int {
    return audioInteger(MediaFormat.KEY_SAMPLE_RATE) ?: 0
}

fun MediaFormat.audioChannelCount(): Int {
    return audioInteger(MediaFormat.KEY_CHANNEL_COUNT) ?: 0
}

fun MediaFormat.pcmEncoding(): Int {
    return audioInteger(MediaFormat.KEY_PCM_ENCODING) ?: AudioFormat.ENCODING_PCM_16BIT
}

fun bytesPerPcmSample(encoding: Int): Int {
    return when (encoding) {
        AudioFormat.ENCODING_PCM_8BIT -> 1
        AudioFormat.ENCODING_PCM_FLOAT -> 4
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
        AudioFormat.ENCODING_PCM_32BIT -> 4
        else -> 2
    }
}

fun isOpusSampleRateSupported(sampleRate: Int): Boolean {
    return sampleRate == 8000 || sampleRate == 12000 || sampleRate == 16000 ||
        sampleRate == 24000 || sampleRate == 48000
}

fun findAudioEncoderName(format: MediaFormat): String? {
    val mime = format.audioMime() ?: return null
    if (mime == MediaFormat.MIMETYPE_AUDIO_OPUS) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !isOpusSampleRateSupported(format.audioSampleRate())) {
            return null
        }
    }
    val probe = MediaFormat.createAudioFormat(mime, format.audioSampleRate(), format.audioChannelCount())
    val name = try {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(probe)
    } catch (error: Exception) {
        Log.w(TAG, "findEncoderForFormat($mime) failed: $error")
        null
    }
    if (name != null) {
        return name
    }
    return try {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.firstOrNull { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
        }?.name
    } catch (error: Exception) {
        Log.w(TAG, "Listing $mime encoders failed: $error")
        null
    }
}

fun hasAudioEncoder(format: MediaFormat): Boolean {
    val key = "${format.audioMime()}/${format.audioSampleRate()}/${format.audioChannelCount()}"
    return encoderAvailability.getOrPut(key) { findAudioEncoderName(format) != null }
}

fun audioEncodeBitRateRange(codecName: String, mime: String): List<Int>? {
    return try {
        val info = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.firstOrNull { it.name == codecName }
            ?: return null
        val range = info.getCapabilitiesForType(mime).audioCapabilities?.bitrateRange ?: return null
        listOf(range.lower, range.upper)
    } catch (error: Exception) {
        Log.w(TAG, "No bitrate range for $codecName: $error")
        null
    }
}

internal fun MediaFormat.audioMime(): String? {
    return runCatching { getString(MediaFormat.KEY_MIME) }.getOrNull()
}

internal fun MediaFormat.audioInteger(key: String): Int? {
    return runCatching {
        if (containsKey(key)) {
            getInteger(key)
        } else {
            null
        }
    }.getOrNull()
}

private fun isIdentityMap(inChannels: Int, channelMap: List<Int>): Boolean {
    if (channelMap.size != inChannels) {
        return false
    }
    for (index in channelMap.indices) {
        if (channelMap[index] != index) {
            return false
        }
    }
    return true
}
