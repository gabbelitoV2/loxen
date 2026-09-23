package com.moblin.android.platform.avfoundation

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.kCMTimeInvalidUs
import com.moblin.android.platform.audio.audioChannelCount
import com.moblin.android.platform.audio.audioEncodeBitRateRange
import com.moblin.android.platform.audio.audioMime
import com.moblin.android.platform.audio.audioSampleRate
import com.moblin.android.platform.audio.bytesPerPcmSample
import com.moblin.android.platform.audio.findAudioEncoderName
import com.moblin.android.platform.audio.isOpusSampleRateSupported
import com.moblin.android.platform.audio.isRawPcmAudio
import com.moblin.android.platform.audio.pcmEncoding
import com.moblin.android.platform.audio.remapChannels
import com.moblin.android.platform.core.PipelineStats
import java.nio.ByteBuffer

private const val TAG = "MoblinAudio"

typealias AVAudioCompressedBuffer = MediaSample

typealias AVAudioPCMBuffer = MediaSample

fun AVAudioCompressedBuffer(format: MediaFormat, packetCapacity: Int, maximumPacketSize: Int): MediaSample {
    return MediaSample(
        data = ByteArray(0),
        presentationTimeUs = kCMTimeInvalidUs,
        isKeyFrame = true,
        format = format,
    )
}

fun AVAudioPCMBuffer(pcmFormat: MediaFormat, frameCapacity: Int): MediaSample? {
    if (frameCapacity < 0) {
        return null
    }
    val channels = pcmFormat.audioChannelCount()
    if (channels <= 0) {
        return null
    }
    return MediaSample(
        data = ByteArray(frameCapacity * channels * bytesPerPcmSample(pcmFormat.pcmEncoding())),
        presentationTimeUs = kCMTimeInvalidUs,
        isKeyFrame = true,
        format = pcmFormat,
    )
}

class AVAudioConverter private constructor(
    val inputFormat: MediaFormat,
    val outputFormat: MediaFormat,
    private val codecName: String?,
    private var codec: MediaCodec?,
    private val encodeBitRates: List<Int>?,
) {
    private val lock = Any()
    private val inputChannels = inputFormat.audioChannelCount()
    private val outputChannels = outputFormat.audioChannelCount()
    private val sampleRate = inputFormat.audioSampleRate()
    private val outputMime = outputFormat.audioMime() ?: ""
    private var channelMapValue: List<Int> = makeDefaultChannelMap(inputChannels, outputChannels)
    private var bitRateValue = 0
    private var isStarted = false
    private var isReleased = false
    private var isBroken = false
    private var brokenAtNs = 0L
    private val packets = ArrayDeque<ByteArray>()
    private val bufferInfo = MediaCodec.BufferInfo()
    private var scratch = ByteArray(0)
    private var inputFramePosition = 0L
    private var outputFramePosition = 0L
    private var numberOfOutputPackets = 0L
    private val framesPerOutputPacket = if (outputMime == MediaFormat.MIMETYPE_AUDIO_OPUS) 960 else 1024

    var channelMap: List<Int>
        get() = synchronized(lock) { channelMapValue }
        set(value) {
            synchronized(lock) {
                channelMapValue = value.toList()
            }
        }

    var bitRate: Int
        get() = synchronized(lock) { bitRateValue }
        set(value) {
            synchronized(lock) {
                if (value == bitRateValue) {
                    return
                }
                bitRateValue = value
                if (isStarted && !isReleased) {
                    Log.i(TAG, "Restarting $outputMime encoder for bitrate $value")
                    restartCodecLocked()
                }
            }
        }

    val applicableEncodeBitRates: List<Int>?
        get() = encodeBitRates

    fun convert(to: AVAudioCompressedBuffer, inputBlock: () -> ByteArray?): String? {
        synchronized(lock) {
            if (isReleased) {
                return "converter released"
            }
            if (codecName == null) {
                return "not an encoder"
            }
            val codec = startCodecLocked() ?: return "encoder not available"
            val input = inputBlock() ?: return "no input"
            try {
                queueInputLocked(codec, input)
                drainLocked(codec, 0)
                if (packets.isEmpty()) {
                    val deadlineNs = System.nanoTime() + 20_000_000L
                    while (packets.isEmpty()) {
                        val remainingUs = (deadlineNs - System.nanoTime()) / 1000
                        if (remainingUs <= 0) {
                            break
                        }
                        drainLocked(codec, remainingUs)
                    }
                }
            } catch (error: Exception) {
                Log.w(TAG, "$outputMime encoder failed: $error")
                failCodecLocked()
                return "encoder failed: ${error.message}"
            }
            val packet = packets.removeFirstOrNull() ?: return "no output yet"
            to.data = packet
            return null
        }
    }

    fun convert(to: AVAudioPCMBuffer, from: AVAudioPCMBuffer): String? {
        synchronized(lock) {
            if (isReleased) {
                return "converter released"
            }
            if (codecName != null) {
                return "not a PCM converter"
            }
            val fromFormat = from.format ?: inputFormat
            if (fromFormat.pcmEncoding() != AudioFormat.ENCODING_PCM_16BIT ||
                outputFormat.pcmEncoding() != AudioFormat.ENCODING_PCM_16BIT
            ) {
                return "only 16 bit PCM is supported"
            }
            val fromSampleRate = fromFormat.audioSampleRate().takeIf { it > 0 } ?: sampleRate
            if (fromSampleRate != outputFormat.audioSampleRate()) {
                return "sample rate conversion from $fromSampleRate to ${outputFormat.audioSampleRate()} is not supported"
            }
            val fromChannels = fromFormat.audioChannelCount().takeIf { it > 0 } ?: inputChannels
            if (fromChannels <= 0 || outputChannels <= 0) {
                return "invalid channel count"
            }
            val frames = from.data.size / (fromChannels * 2)
            val outputSize = frames * outputChannels * 2
            val output = if (to.data.size == outputSize) to.data else ByteArray(outputSize)
            remapChannels(from.data, frames, fromChannels, fullChannelMap(channelMapValue, outputChannels), output)
            to.data = output
            return null
        }
    }

    fun reset() {
        synchronized(lock) {
            if (isStarted && !isReleased) {
                restartCodecLocked()
            }
            packets.clear()
        }
    }

    fun release() {
        synchronized(lock) {
            if (isReleased) {
                return
            }
            isReleased = true
            packets.clear()
            val codec = codec
            this.codec = null
            if (codec != null) {
                try {
                    if (isStarted) {
                        codec.stop()
                    }
                } catch (error: Exception) {
                    Log.d(TAG, "Encoder stop failed: $error")
                }
                try {
                    codec.release()
                } catch (error: Exception) {
                    Log.d(TAG, "Encoder release failed: $error")
                }
            }
            isStarted = false
        }
    }

    override fun toString(): String {
        return "AVAudioConverter($inputFormat -> $outputFormat, codec=${codecName ?: "pcm"})"
    }

    private fun startCodecLocked(): MediaCodec? {
        if (isStarted) {
            return codec
        }
        if (isBroken) {
            val name = codecName ?: return null
            if (System.nanoTime() - brokenAtNs < 1_000_000_000L) {
                return null
            }
            brokenAtNs = System.nanoTime()
            codec = try {
                MediaCodec.createByCodecName(name)
            } catch (error: Exception) {
                Log.w(TAG, "Failed to recreate $name: $error")
                return null
            }
            isBroken = false
        }
        val codec = codec ?: return null
        return try {
            codec.configure(makeCodecFormat(), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            isStarted = true
            outputFramePosition = inputFramePosition
            Log.i(
                TAG,
                "Encoder $codecName started: $outputMime $sampleRate Hz $outputChannels ch " +
                    "${effectiveBitRate()} bps",
            )
            codec
        } catch (error: Exception) {
            Log.w(TAG, "Failed to start $codecName: $error")
            failCodecLocked()
            null
        }
    }

    private fun restartCodecLocked() {
        val codec = codec ?: return
        try {
            codec.stop()
        } catch (error: Exception) {
            Log.d(TAG, "Encoder stop failed: $error")
        }
        isStarted = false
        try {
            codec.configure(makeCodecFormat(), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            isStarted = true
            outputFramePosition = inputFramePosition
        } catch (error: Exception) {
            Log.w(TAG, "Failed to restart $codecName: $error")
            failCodecLocked()
        }
    }

    private fun failCodecLocked() {
        val codec = codec
        this.codec = null
        isStarted = false
        isBroken = true
        brokenAtNs = System.nanoTime()
        packets.clear()
        if (codec != null) {
            try {
                codec.release()
            } catch (error: Exception) {
                Log.d(TAG, "Encoder release failed: $error")
            }
        }
    }

    private fun makeCodecFormat(): MediaFormat {
        val format = MediaFormat.createAudioFormat(outputMime, sampleRate, outputChannels)
        if (outputMime == MediaFormat.MIMETYPE_AUDIO_AAC) {
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        }
        format.setInteger(MediaFormat.KEY_BIT_RATE, effectiveBitRate())
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxOf(framesPerOutputPacket, 2048) * outputChannels * 2)
        format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        return format
    }

    private fun effectiveBitRate(): Int {
        val requested = if (bitRateValue > 0) bitRateValue else 64_000
        val range = encodeBitRates ?: return requested
        val minimum = range.minOrNull() ?: return requested
        val maximum = range.maxOrNull() ?: return requested
        return requested.coerceIn(minimum, maximum)
    }

    private fun queueInputLocked(codec: MediaCodec, input: ByteArray) {
        val frames = if (inputChannels > 0) input.size / (inputChannels * 2) else 0
        val size = frames * outputChannels * 2
        if (scratch.size < size) {
            scratch = ByteArray(size)
        }
        remapChannels(input, frames, inputChannels, fullChannelMap(channelMapValue, outputChannels), scratch)
        var index = codec.dequeueInputBuffer(10_000)
        if (index < 0) {
            drainLocked(codec, 0)
            index = codec.dequeueInputBuffer(10_000)
        }
        if (index < 0) {
            Log.w(TAG, "No $outputMime encoder input buffer, dropping $frames frames")
            inputFramePosition += frames
            return
        }
        val buffer: ByteBuffer = codec.getInputBuffer(index) ?: run {
            codec.queueInputBuffer(index, 0, 0, presentationTimeUs(inputFramePosition), 0)
            inputFramePosition += frames
            return
        }
        buffer.clear()
        val length = minOf(size, buffer.remaining())
        buffer.put(scratch, 0, length)
        codec.queueInputBuffer(index, 0, length, presentationTimeUs(inputFramePosition), 0)
        inputFramePosition += frames
    }

    private fun drainLocked(codec: MediaCodec, timeoutUs: Long) {
        var timeout = timeoutUs
        while (true) {
            val index = codec.dequeueOutputBuffer(bufferInfo, timeout)
            timeout = 0
            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                return
            }
            if (index < 0) {
                continue
            }
            val isConfig = bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
            if (isConfig || bufferInfo.size <= 0) {
                codec.releaseOutputBuffer(index, false)
                continue
            }
            val packet = ByteArray(bufferInfo.size)
            val buffer = codec.getOutputBuffer(index)
            if (buffer != null) {
                buffer.position(bufferInfo.offset)
                buffer.limit(bufferInfo.offset + bufferInfo.size)
                buffer.get(packet)
            }
            val packetPresentationTimeUs = bufferInfo.presentationTimeUs
            codec.releaseOutputBuffer(index, false)
            onPacketLocked(packet, packetPresentationTimeUs)
        }
    }

    private fun onPacketLocked(packet: ByteArray, packetPresentationTimeUs: Long) {
        val expectedUs = presentationTimeUs(outputFramePosition)
        outputFramePosition += framesPerOutputPacket
        numberOfOutputPackets += 1
        if (numberOfOutputPackets == 50L || numberOfOutputPackets % 1500L == 0L) {
            val driftMs = (packetPresentationTimeUs - expectedUs) / 1000.0
            Log.d(TAG, "pts drift vs input ${"%.2f".format(driftMs)} ms ($outputMime, $numberOfOutputPackets packets)")
        }
        if (packets.size >= 16) {
            packets.removeFirst()
            Log.w(TAG, "Dropping old $outputMime packet")
        }
        packets.addLast(packet)
        PipelineStats.increment(if (outputMime == MediaFormat.MIMETYPE_AUDIO_OPUS) "opusOut" else "aacOut")
    }

    private fun presentationTimeUs(framePosition: Long): Long {
        if (sampleRate <= 0) {
            return 0
        }
        return framePosition * 1_000_000L / sampleRate
    }

    companion object {
        fun create(from: MediaFormat, to: MediaFormat): AVAudioConverter? {
            if (!from.isRawPcmAudio()) {
                Log.i(TAG, "Converter input must be PCM: $from")
                return null
            }
            if (from.audioChannelCount() <= 0 || to.audioChannelCount() <= 0) {
                return null
            }
            if (to.isRawPcmAudio()) {
                return AVAudioConverter(from, to, null, null, null)
            }
            val mime = to.audioMime() ?: return null
            if (from.audioSampleRate() != to.audioSampleRate()) {
                Log.i(TAG, "Encoder sample rate ${to.audioSampleRate()} differs from input ${from.audioSampleRate()}")
                return null
            }
            if (mime == MediaFormat.MIMETYPE_AUDIO_OPUS && !isOpusSampleRateSupported(to.audioSampleRate())) {
                return null
            }
            if (from.pcmEncoding() != AudioFormat.ENCODING_PCM_16BIT) {
                Log.i(TAG, "Encoder input must be 16 bit PCM: $from")
                return null
            }
            val name = findAudioEncoderName(to) ?: run {
                Log.i(TAG, "No encoder for $to")
                return null
            }
            val codec = try {
                MediaCodec.createByCodecName(name)
            } catch (error: Exception) {
                Log.w(TAG, "Failed to create $name: $error")
                return null
            }
            return AVAudioConverter(from, to, name, codec, audioEncodeBitRateRange(name, mime))
        }

        private fun makeDefaultChannelMap(inputChannels: Int, outputChannels: Int): List<Int> {
            return List(maxOf(outputChannels, 0)) { index ->
                when {
                    index < inputChannels -> index
                    inputChannels == 1 -> 0
                    else -> -1
                }
            }
        }

        private fun fullChannelMap(channelMap: List<Int>, outputChannels: Int): List<Int> {
            if (channelMap.size == outputChannels) {
                return channelMap
            }
            return List(outputChannels) { index -> channelMap.getOrElse(index) { -1 } }
        }
    }
}
