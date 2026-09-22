package com.moblin.android.media.haishinkit.rtmp

import android.media.MediaFormat
import android.util.Log
import com.moblin.android.common.various.makeRtmpStreamKey
import com.moblin.android.common.various.makeRtmpUri
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderDelegate
import com.moblin.android.media.haishinkit.codec.video.VideoEncoder
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderDelegate
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderSettings
import com.moblin.android.media.haishinkit.flv.FlvAacPacketType
import com.moblin.android.media.haishinkit.flv.FlvAudioCodec
import com.moblin.android.media.haishinkit.flv.FlvAvcPacketType
import com.moblin.android.media.haishinkit.flv.FlvFrameType
import com.moblin.android.media.haishinkit.flv.FlvOpusPacketType
import com.moblin.android.media.haishinkit.flv.FlvSoundRate
import com.moblin.android.media.haishinkit.flv.FlvSoundSize
import com.moblin.android.media.haishinkit.flv.FlvSoundType
import com.moblin.android.media.haishinkit.flv.FlvTagType
import com.moblin.android.media.haishinkit.flv.FlvVideoCodec
import com.moblin.android.media.haishinkit.flv.FlvVideoFourCC
import com.moblin.android.media.haishinkit.flv.FlvVideoPacketType
import com.moblin.android.media.haishinkit.media.AudioVideoEncoderDelegate
import com.moblin.android.media.haishinkit.media.Processor
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.mpeg.MpegTsAudioConfig
import com.moblin.android.media.haishinkit.mpeg.avc.MpegTsVideoConfigAvc
import com.moblin.android.media.haishinkit.mpeg.hevc.MpegTsVideoConfigHevc
import com.moblin.android.media.haishinkit.rtmp.amf.AsObject
import com.moblin.android.media.haishinkit.rtmp.amf.AsValue
import com.moblin.android.media.haishinkit.rtmp.message.RtmpAudioMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpCommandMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpCommandName
import com.moblin.android.media.haishinkit.rtmp.message.RtmpDataMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpMessageType
import com.moblin.android.media.haishinkit.rtmp.message.RtmpVideoMessage
import com.moblin.android.media.haishinkit.util.ByteWriter
import com.moblin.android.various.SimpleTimer
import java.time.Duration
import java.time.Instant
import kotlin.math.floor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "RtmpStream"

val extendedVideoHeader: UByte = 0b1000_0000u.toUByte()

private fun calcVideoCompositionTime(sampleBuffer: MediaSample): Int {
    return 0
}

private fun makeVideoHeader(
    frameType: FlvFrameType,
    fourCc: FlvVideoFourCC,
    avcPacketType: FlvAvcPacketType,
    videoPacketType: FlvVideoPacketType,
): ByteArray {
    val writer = ByteWriter()
    if (fourCc == FlvVideoFourCC.avc1) {
        writer.writeUInt8(
            ((frameType.rawValue.toInt() shl 4) or FlvVideoCodec.avc.rawValue.toInt()).toUByte(),
        )
        writer.writeUInt8(avcPacketType.rawValue.toUByte())
    } else {
        writer.writeUInt8(
            (
                extendedVideoHeader.toInt() or
                    (frameType.rawValue.toInt() shl 4) or
                    videoPacketType.rawValue.toInt()
                ).toUByte(),
        )
        writer.writeUInt32(fourCc.rawValue.toUInt())
    }
    return writer.data
}

private fun makeAvcVideoTagHeader(frameType: FlvFrameType, packetType: FlvAvcPacketType): ByteArray {
    return makeVideoHeader(frameType, FlvVideoFourCC.avc1, packetType, FlvVideoPacketType.sequenceStart)
}

private fun makeHevcExtendedTagHeader(frameType: FlvFrameType, packetType: FlvVideoPacketType): ByteArray {
    return makeVideoHeader(frameType, FlvVideoFourCC.hevc, FlvAvcPacketType.nal, packetType)
}

interface RtmpStreamDelegate {
    fun rtmpStreamStatus(rtmpStream: RtmpStream, code: String)
    fun rtmpStreamConnected(rtmpStream: RtmpStream)
}

enum class RtmpStreamCode(val rawValue: String) {
    publishStart("NetStream.Publish.Start"),
    ;

    companion object {
        fun fromRawValue(rawValue: String): RtmpStreamCode? {
            return entries.firstOrNull { it.rawValue == rawValue }
        }
    }
}

private val aac: UByte = (
    (FlvAudioCodec.aac.rawValue.toInt() shl 4) or
        (FlvSoundRate.kHz44.rawValue.toInt() shl 2) or
        (FlvSoundSize.snd16bit.rawValue.toInt() shl 1) or
        FlvSoundType.stereo.rawValue.toInt()
    ).toUByte()

private val opus: UByte = (
    (FlvAudioCodec.exHeader.rawValue.toInt() shl 4) or
        FlvOpusPacketType.sequenceStart.rawValue.toInt()
    ).toUByte()

private enum class State {
    initialized,
    open,
    publishing,
}

class RtmpStream(
    val name: String,
    private val processor: Processor,
    val delegate: RtmpStreamDelegate?,
    private val queue: CoroutineDispatcher,
) : AudioVideoEncoderDelegate {
    val info = RtmpStreamInfo()
    var streamId: UInt = 0u
    private var state: State = State.initialized
    private var startedAt: Instant = Instant.now()
    private var audioChunkType: RtmpChunkType = RtmpChunkType.zero
    private var videoChunkType: RtmpChunkType = RtmpChunkType.zero
    private val dataTimeStamps = mutableMapOf<String, Instant>()
    private val connection: RtmpConnection
    private var streamKey = ""
    private var url = ""
    private val connectTimer: SimpleTimer
    private val queueScope = CoroutineScope(queue)

    private var baseTimeStamp = -1.0
    private var audioTimeStampDelta = 0.0
    private var videoTimeStampDelta = 0.0
    private var prevRebasedAudioTimeStamp: Double? = null
    private var prevRebasedVideoTimeStamp: Double? = null
    private var audioMime: String? = null

    init {
        connectTimer = SimpleTimer(queue)
        connection = RtmpConnection(name, queue)
        connection.stream = this
    }

    fun setUrl(url: String) {
        streamKey = makeRtmpStreamKey(url)
        this.url = makeRtmpUri(url)
    }

    fun connect() {
        queueScope.launch {
            connectInternal()
        }
    }

    fun disconnect() {
        queueScope.launch {
            disconnectInternal()
        }
    }

    fun reconnectSoon() {
        queueScope.launch {
            delay(5000)
            connectInternal()
        }
    }

    fun onTimeout() {
        info.onTimeout()
    }

    fun closeInternal() {
        setState(State.initialized)
        stopConnectTimer()
        processorPipelineQueue.launch {
            processor.stopEncoding(this@RtmpStream)
        }
    }

    fun onInternal(data: AsObject) {
        val value = data["code"]
        if (value !is AsValue.String) {
            return
        }
        val code = value.value
        delegate?.rtmpStreamStatus(this, code)
        when (code) {
            RtmpConnectionCode.connectSuccess.rawValue -> {
                setState(State.initialized)
                sendReleaseStream()
                sendFCPublish()
                sendCreateStream()
            }
            RtmpStreamCode.publishStart.rawValue -> {
                if (state != State.initialized) {
                    setState(State.publishing)
                }
            }
            else -> Unit
        }
    }

    private fun setState(state: State) {
        if (this.state == state) {
            return
        }
        val oldState = this.state
        this.state = state
        Log.i(TAG, "rtmp: $name: Stream state $oldState -> $state")
        if (oldState == State.publishing) {
            sendFCUnpublish()
            sendDeleteStream()
            sendCloseStream()
            processorPipelineQueue.launch {
                processor.stopEncoding(this@RtmpStream)
            }
        }
        when (state) {
            State.open -> handleStateChangeToOpen()
            State.publishing -> handleStateChangeToPublishing()
            else -> Unit
        }
    }

    private fun connectInternal() {
        startConnectTimer()
        connection.connect(url)
    }

    private fun disconnectInternal() {
        setState(State.initialized)
        processorPipelineQueue.launch {
            processor.stopEncoding(this@RtmpStream)
        }
        stopConnectTimer()
        connection.disconnect()
    }

    private fun startConnectTimer() {
        connectTimer.startSingleShot(20.0) {
            Log.i(TAG, "rtmp: $name: Connect timeout")
            connection.socket.close(isDisconnected = true)
        }
    }

    private fun stopConnectTimer() {
        connectTimer.stop()
    }

    private fun send(handlerName: String, vararg arguments: AsValue) {
        if (state != State.publishing) {
            return
        }
        val dataWasSent = dataTimeStamps[handlerName] != null
        val elapsed = if (dataWasSent) {
            dataTimeStamps[handlerName]?.let { Duration.between(it, Instant.now()).toMillis() } ?: 0L
        } else {
            Duration.between(startedAt, Instant.now()).toMillis()
        }
        val timestamp = elapsed.coerceAtLeast(0L).toUInt()
        val chunk = RtmpChunk(
            type = if (dataWasSent) RtmpChunkType.one else RtmpChunkType.zero,
            chunkStreamId = RtmpChunk.ChunkStreamId.command.rawValue,
            message = RtmpDataMessage(
                streamId = streamId,
                dataType = RtmpMessageType.amf0Data,
                timestamp = timestamp,
                handlerName = handlerName,
                arguments = arguments.toList(),
            ),
        )
        val length = connection.socket.write(chunk)
        dataTimeStamps[handlerName] = Instant.now()
        info.bitrateStats.value.add(bytesTransferred = length)
    }

    private fun createOnMetaData(): AsObject {
        val audioEncoder = processor.getAudioEncoder()
        val videoEncoder = processor.getVideoEncoder()
        val metadata: MutableMap<String, AsValue> = mutableMapOf()
        val settings = videoEncoder.settings.value
        metadata["width"] = AsValue.Number(settings.videoSize.width.toDouble())
        metadata["height"] = AsValue.Number(settings.videoSize.height.toDouble())
        metadata["framerate"] = AsValue.Number(processor.getFps())
        when (settings.format) {
            VideoEncoderSettings.Format.h264 ->
                metadata["videocodecid"] = AsValue.Number(FlvVideoCodec.avc.rawValue.toInt().toDouble())
            VideoEncoderSettings.Format.hevc ->
                metadata["videocodecid"] = AsValue.Number(FlvVideoFourCC.hevc.rawValue.toInt().toDouble())
            else -> Unit
        }
        metadata["videodatarate"] = AsValue.Number(settings.bitrate.toDouble() / 1000)
        metadata["audiocodecid"] = AsValue.Number(FlvAudioCodec.aac.rawValue.toInt().toDouble())
        metadata["audiodatarate"] = AsValue.Number(audioEncoder.getBitrate().toDouble() / 1000)
        audioEncoder.getSampleRate()?.let {
            metadata["audiosamplerate"] = AsValue.Number(it.toDouble())
        }
        return metadata
    }

    private fun handleStateChangeToOpen() {
        info.clear()
        connection.socket.write(
            RtmpChunk(
                message = RtmpCommandMessage(
                    streamId = streamId,
                    transactionId = connection.getNextTransactionId(),
                    commandType = RtmpMessageType.amf0Command,
                    commandName = RtmpCommandName.publish,
                    commandObject = null,
                    arguments = listOf(AsValue.String(streamKey), AsValue.String("live")),
                ),
            ),
        )
        startedAt = Instant.now()
        baseTimeStamp = -1.0
        audioTimeStampDelta = 0.0
        videoTimeStampDelta = 0.0
        prevRebasedAudioTimeStamp = null
        prevRebasedVideoTimeStamp = null
        videoChunkType = RtmpChunkType.zero
        audioChunkType = RtmpChunkType.zero
        dataTimeStamps.clear()
    }

    private fun handleStateChangeToPublishing() {
        send("@setDataFrame", AsValue.String("onMetaData"), AsValue.Object(createOnMetaData()))
        stopConnectTimer()
        delegate?.rtmpStreamConnected(this)
        processorPipelineQueue.launch {
            processor.startEncoding(this@RtmpStream)
        }
    }

    private fun sendCreateStream() {
        connection.call(RtmpCommandName.createStream, listOf()) { arguments ->
            if (arguments.isEmpty()) {
                return@call
            }
            val id = (arguments[0] as? AsValue.Number)?.value ?: return@call
            if (id < 0.0 || id > UInt.MAX_VALUE.toDouble()) {
                return@call
            }
            streamId = id.toUInt()
            setState(State.open)
        }
    }

    private fun sendReleaseStream() {
        connection.call(RtmpCommandName.releaseStream, listOf(AsValue.String(streamKey)))
    }

    private fun sendFCPublish() {
        connection.call(RtmpCommandName.fcPublish, listOf(AsValue.String(streamKey)))
    }

    private fun sendFCUnpublish() {
        connection.call(RtmpCommandName.fcUnpublish, listOf(AsValue.String(streamKey)))
    }

    private fun sendDeleteStream() {
        connection.socket.write(
            RtmpChunk(
                message = RtmpCommandMessage(
                    streamId = streamId,
                    transactionId = 0,
                    commandType = RtmpMessageType.amf0Command,
                    commandName = RtmpCommandName.deleteStream,
                    commandObject = null,
                    arguments = listOf(AsValue.Number(streamId.toDouble())),
                ),
            ),
        )
    }

    private fun sendCloseStream() {
        connection.socket.write(
            RtmpChunk(
                type = RtmpChunkType.zero,
                chunkStreamId = RtmpChunk.ChunkStreamId.command.rawValue,
                message = RtmpCommandMessage(
                    streamId = 0u,
                    transactionId = 0,
                    commandType = RtmpMessageType.amf0Command,
                    commandName = RtmpCommandName.closeStream,
                    commandObject = null,
                    arguments = listOf(AsValue.Number(streamId.toDouble())),
                ),
            ),
        )
    }

    private fun handleEncodedAudioBuffer(buffer: ByteArray, timestamp: UInt) {
        if (state != State.publishing) {
            return
        }
        val length = connection.socket.write(
            RtmpChunk(
                type = audioChunkType,
                chunkStreamId = FlvTagType.audio.streamId,
                message = RtmpAudioMessage(streamId = streamId, timestamp = timestamp, payload = buffer),
            ),
        )
        audioChunkType = RtmpChunkType.one
        info.bitrateStats.value.add(bytesTransferred = length)
    }

    private fun handleEncodedVideoBuffer(buffer: ByteArray, timestamp: UInt) {
        if (state != State.publishing) {
            return
        }
        val length = connection.socket.write(
            RtmpChunk(
                type = videoChunkType,
                chunkStreamId = FlvTagType.video.streamId,
                message = RtmpVideoMessage(streamId = streamId, timestamp = timestamp, payload = buffer),
            ),
        )
        videoChunkType = RtmpChunkType.one
        info.bitrateStats.value.add(bytesTransferred = length)
    }

    private fun audioEncoderOutputFormatInternal(format: MediaFormat) {
        val writer = ByteWriter()
        val mime = format.getString(MediaFormat.KEY_MIME)
        audioMime = mime
        if (mime == "audio/opus") {
            writer.writeUInt8(opus)
            writer.writeUTF8Bytes("Opus")
            writer.writeUTF8Bytes("OpusHead")
            writer.writeUInt8(1u.toUByte())
            writer.writeUInt8(format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).toUByte())
            writer.writeUInt16(0u.toUShort())
            writer.writeUInt32(format.getInteger(MediaFormat.KEY_SAMPLE_RATE).toUInt())
            writer.writeUInt16(0u.toUShort())
            writer.writeUInt8(0u.toUByte())
        } else {
            writer.writeUInt8(aac)
            writer.writeUInt8(FlvAacPacketType.seq.rawValue.toUByte())
            writer.writeBytes(MpegTsAudioConfig(format).encode())
        }
        handleEncodedAudioBuffer(writer.data, 0u)
    }

    private fun audioEncoderOutputBufferInternal(audioBuffer: ByteArray, presentationTimeUs: Long) {
        val rebasedTimestamp = rebaseTimeStamp(presentationTimeUs / 1_000_000.0)
        if (rebasedTimestamp == null) {
            Log.i(TAG, "rtmp: $name: Dropping audio buffer. Failed to rebase timestamp.")
            return
        }
        var delta = 0.0
        prevRebasedAudioTimeStamp?.let {
            delta = (rebasedTimestamp - it) * 1000
        }
        if (delta < 0) {
            Log.i(TAG, "rtmp: $name: Dropping audio buffer (delta: $delta)")
            return
        }
        var buffer: ByteArray = if (audioMime == "audio/opus") {
            val header = byteArrayOf(
                (
                    (FlvAudioCodec.exHeader.rawValue.toInt() shl 4) or
                        FlvOpusPacketType.codedFrames.rawValue.toInt()
                    ).toByte(),
            )
            header + "Opus".toByteArray(Charsets.UTF_8)
        } else {
            byteArrayOf(aac.toByte(), FlvAacPacketType.raw.rawValue.toByte())
        }
        buffer += audioBuffer
        prevRebasedAudioTimeStamp = rebasedTimestamp
        audioTimeStampDelta += delta
        handleEncodedAudioBuffer(buffer, audioTimeStampDelta.toUInt())
        audioTimeStampDelta -= floor(audioTimeStampDelta)
    }

    private fun videoEncoderOutputFormatInternal(
        format: VideoEncoderSettings.Format,
        formatDescription: MediaFormat,
    ) {
        val buffer: ByteArray = when (format) {
            VideoEncoderSettings.Format.h264 -> {
                val avcC = MpegTsVideoConfigAvc.getAvcC(formatDescription) ?: return
                makeAvcVideoTagHeader(FlvFrameType.key, FlvAvcPacketType.seq) +
                    byteArrayOf(0, 0, 0) +
                    avcC
            }
            VideoEncoderSettings.Format.hevc -> {
                val hvcC = MpegTsVideoConfigHevc.getHvcC(formatDescription) ?: return
                makeHevcExtendedTagHeader(FlvFrameType.key, FlvVideoPacketType.sequenceStart) + hvcC
            }
            else -> return
        }
        handleEncodedVideoBuffer(buffer, 0u)
    }

    private fun videoEncoderOutputSampleBufferInternal(
        format: VideoEncoderSettings.Format,
        sampleBuffer: MediaSample,
    ) {
        val decodeTimeStamp = sampleBuffer.presentationTimeUs / 1_000_000.0
        val rebasedTimestamp = rebaseTimeStamp(decodeTimeStamp) ?: return
        var delta = 0.0
        prevRebasedVideoTimeStamp?.let {
            delta = (rebasedTimestamp - it) * 1000
        }
        if (delta < 0) {
            Log.i(TAG, "rtmp: $name: Dropping video buffer (delta: $delta)")
            return
        }
        val data = sampleBuffer.data
        val frameType = if (sampleBuffer.isKeyFrame) FlvFrameType.key else FlvFrameType.inter
        var buffer: ByteArray = when (format) {
            VideoEncoderSettings.Format.h264 -> makeAvcVideoTagHeader(frameType, FlvAvcPacketType.nal)
            VideoEncoderSettings.Format.hevc -> makeHevcExtendedTagHeader(frameType, FlvVideoPacketType.codedFrames)
            else -> return
        }
        val compositionTime = calcVideoCompositionTime(sampleBuffer)
        buffer += byteArrayOf(
            ((compositionTime shr 16) and 0xFF).toByte(),
            ((compositionTime shr 8) and 0xFF).toByte(),
            (compositionTime and 0xFF).toByte(),
        )
        buffer += data
        prevRebasedVideoTimeStamp = rebasedTimestamp
        videoTimeStampDelta += delta
        handleEncodedVideoBuffer(buffer, videoTimeStampDelta.toUInt())
        videoTimeStampDelta -= floor(videoTimeStampDelta)
    }

    private fun rebaseTimeStamp(timestamp: Double): Double? {
        if (baseTimeStamp == -1.0) {
            baseTimeStamp = timestamp
        }
        val rebased = timestamp - baseTimeStamp
        return if (rebased >= 0) {
            rebased
        } else {
            null
        }
    }

    override fun audioEncoderOutputFormat(format: MediaFormat) {
        queueScope.launch {
            audioEncoderOutputFormatInternal(format)
        }
    }

    override fun audioEncoderOutputBuffer(buffer: MediaSample, presentationTimeStamp: Long) {
        queueScope.launch {
            audioEncoderOutputBufferInternal(buffer.data, presentationTimeStamp)
        }
    }

    override fun videoEncoderOutputFormat(encoder: VideoEncoder, formatDescription: MediaFormat) {
        val format = encoder.settings.value.format
        queueScope.launch {
            videoEncoderOutputFormatInternal(format, formatDescription)
        }
    }

    override fun videoEncoderOutputSampleBuffer(
        codec: VideoEncoder,
        sampleBuffer: MediaSample,
        presentationTimeUs: Long,
    ) {
        val format = codec.settings.value.format
        queueScope.launch {
            videoEncoderOutputSampleBufferInternal(format, sampleBuffer)
        }
    }
}
