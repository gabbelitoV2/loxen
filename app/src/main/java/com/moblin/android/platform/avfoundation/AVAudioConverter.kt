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
import java.nio.ByteOrder
import java.nio.ShortBuffer

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

val MediaSample.int16ChannelData: List<ShortBuffer>
    get() = listOf(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer())

var MediaSample.frameLength: Int
    get() = numSamples
    set(value) {
        val format = format
        val channels = maxOf(1, format?.audioChannelCount() ?: 1)
        val frameSize = channels * bytesPerPcmSample(format?.pcmEncoding() ?: AudioFormat.ENCODING_PCM_16BIT)
        val size = value * frameSize
        if (size != data.size) {
            data = data.copyOf(size)
        }
    }

class AVAudioConverter private constructor(
    val inputFormat: MediaFormat,
    val outputFormat: MediaFormat,
    private val codecName: String?,
    private var codec: MediaCodec?,
    private val encodeBitRates: List<Int>?,
    private val decodes: Boolean = false,
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
    private var decodedChannels = inputChannels
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
            if (decodes) {
                return decodeLocked(to, inputBlock)
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
            codec.configure(makeCodecFormat(), null, null, configureFlags())
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
            codec.configure(makeCodecFormat(), null, null, configureFlags())
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

    private fun configureFlags(): Int {
        return if (decodes) 0 else MediaCodec.CONFIGURE_FLAG_ENCODE
    }

    private fun makeCodecFormat(): MediaFormat {
        if (decodes) {
            return makeDecoderFormat(inputFormat)
        }
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

    private fun decodeLocked(to: MediaSample, inputBlock: () -> ByteArray?): String? {
        val codec = startCodecLocked() ?: return "decoder not available"
        val input = inputBlock() ?: return "no input"
        val decoded = java.io.ByteArrayOutputStream()
        try {
            var index = codec.dequeueInputBuffer(10_000)
            if (index < 0) {
                drainDecoderLocked(codec, 0, decoded)
                index = codec.dequeueInputBuffer(10_000)
            }
            if (index < 0) {
                return "no decoder input buffer"
            }
            val buffer: ByteBuffer? = codec.getInputBuffer(index)
            if (buffer == null || input.size > buffer.capacity()) {
                codec.queueInputBuffer(index, 0, 0, presentationTimeUs(inputFramePosition), 0)
                return "packet does not fit the decoder input buffer"
            }
            buffer.clear()
            buffer.put(input)
            codec.queueInputBuffer(index, 0, input.size, presentationTimeUs(inputFramePosition), 0)
            inputFramePosition += 960
            drainDecoderLocked(codec, 0, decoded)
            val deadlineNs = System.nanoTime() + 20_000_000L
            while (decoded.size() == 0) {
                val remainingUs = (deadlineNs - System.nanoTime()) / 1000
                if (remainingUs <= 0) {
                    break
                }
                drainDecoderLocked(codec, remainingUs, decoded)
            }
        } catch (error: Exception) {
            Log.w(TAG, "${inputFormat.audioMime()} decoder failed: $error")
            failCodecLocked()
            return "decoder failed: ${error.message}"
        }
        if (decoded.size() == 0) {
            return "no output yet"
        }
        to.data = decoded.toByteArray()
        return null
    }

    private fun drainDecoderLocked(codec: MediaCodec, timeoutUs: Long, decoded: java.io.ByteArrayOutputStream) {
        var timeout = timeoutUs
        while (true) {
            val index = codec.dequeueOutputBuffer(bufferInfo, timeout)
            timeout = 0
            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                return
            }
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                decodedChannels = codec.outputFormat.audioChannelCount().takeIf { it > 0 } ?: decodedChannels
                continue
            }
            if (index < 0) {
                continue
            }
            val isConfig = bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
            if (isConfig || bufferInfo.size <= 0) {
                codec.releaseOutputBuffer(index, false)
                continue
            }
            val pcm = ByteArray(bufferInfo.size)
            val buffer = codec.getOutputBuffer(index)
            if (buffer != null) {
                buffer.position(bufferInfo.offset)
                buffer.limit(bufferInfo.offset + bufferInfo.size)
                buffer.get(pcm)
            }
            codec.releaseOutputBuffer(index, false)
            if (decodedChannels == outputChannels || decodedChannels <= 0 || outputChannels <= 0) {
                decoded.write(pcm)
            } else {
                val frames = pcm.size / (decodedChannels * 2)
                val remapped = ByteArray(frames * outputChannels * 2)
                remapChannels(
                    pcm,
                    frames,
                    decodedChannels,
                    fullChannelMap(makeDefaultChannelMap(decodedChannels, outputChannels), outputChannels),
                    remapped,
                )
                decoded.write(remapped)
            }
        }
    }

    private fun presentationTimeUs(framePosition: Long): Long {
        if (sampleRate <= 0) {
            return 0
        }
        return framePosition * 1_000_000L / sampleRate
    }

    companion object {
        fun create(from: MediaFormat, to: MediaFormat): AVAudioConverter? {
            if (!from.isRawPcmAudio() && to.isRawPcmAudio()) {
                return createDecoder(from, to)
            }
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

        private fun createDecoder(from: MediaFormat, to: MediaFormat): AVAudioConverter? {
            val mime = from.audioMime() ?: return null
            if (from.audioChannelCount() <= 0 || to.audioChannelCount() <= 0) {
                return null
            }
            if (to.pcmEncoding() != AudioFormat.ENCODING_PCM_16BIT) {
                Log.i(TAG, "Decoder output must be 16 bit PCM: $to")
                return null
            }
            if (from.audioSampleRate() != to.audioSampleRate()) {
                Log.i(TAG, "Decoder sample rate ${to.audioSampleRate()} differs from input ${from.audioSampleRate()}")
                return null
            }
            if (mime != MediaFormat.MIMETYPE_AUDIO_OPUS && !from.containsKey("csd-0")) {
                Log.i(TAG, "Decoder input needs codec specific data: $from")
                return null
            }
            val name = findAudioDecoderName(from) ?: run {
                Log.i(TAG, "No decoder for $from")
                return null
            }
            val codec = try {
                MediaCodec.createByCodecName(name)
            } catch (error: Exception) {
                Log.w(TAG, "Failed to create $name: $error")
                return null
            }
            return AVAudioConverter(from, to, name, codec, null, decodes = true)
        }

        private fun findAudioDecoderName(format: MediaFormat): String? {
            val mime = format.audioMime() ?: return null
            val probe = MediaFormat.createAudioFormat(mime, format.audioSampleRate(), format.audioChannelCount())
            return try {
                val codecList = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
                codecList.findDecoderForFormat(probe) ?: codecList.codecInfos.firstOrNull { info ->
                    !info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
                }?.name
            } catch (error: Exception) {
                Log.w(TAG, "Finding a $mime decoder failed: $error")
                null
            }
        }

        private fun makeDecoderFormat(inputFormat: MediaFormat): MediaFormat {
            val mime = inputFormat.audioMime() ?: ""
            val channels = inputFormat.audioChannelCount()
            val sampleRate = inputFormat.audioSampleRate()
            val format = MediaFormat.createAudioFormat(mime, sampleRate, channels)
            for (key in listOf("csd-0", "csd-1", "csd-2")) {
                if (inputFormat.containsKey(key)) {
                    val buffer = inputFormat.getByteBuffer(key) ?: continue
                    format.setByteBuffer(key, buffer.duplicate())
                }
            }
            if (mime == MediaFormat.MIMETYPE_AUDIO_OPUS && !inputFormat.containsKey("csd-0")) {
                format.setByteBuffer("csd-0", ByteBuffer.wrap(makeOpusHead(channels, sampleRate)))
                format.setByteBuffer("csd-1", makeLittleEndianLong(0))
                format.setByteBuffer("csd-2", makeLittleEndianLong(80_000_000))
            }
            return format
        }

        private fun makeOpusHead(channels: Int, sampleRate: Int): ByteArray {
            val head = ByteBuffer.allocate(19).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            head.put("OpusHead".toByteArray(Charsets.US_ASCII))
            head.put(1)
            head.put(channels.toByte())
            head.putShort(0)
            head.putInt(sampleRate)
            head.putShort(0)
            head.put(0)
            return head.array()
        }

        private fun makeLittleEndianLong(value: Long): ByteBuffer {
            val buffer = ByteBuffer.allocate(8).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            buffer.putLong(value)
            buffer.flip()
            return buffer
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
