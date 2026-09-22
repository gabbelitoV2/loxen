package com.moblin.android.media.rtmpserver

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.codec.video.VideoDecoder
import com.moblin.android.media.haishinkit.codec.video.VideoDecoderDelegate
import com.moblin.android.media.haishinkit.flv.FlvAacPacketType
import com.moblin.android.media.haishinkit.flv.FlvAudioCodec
import com.moblin.android.media.haishinkit.flv.FlvAvcPacketType
import com.moblin.android.media.haishinkit.flv.FlvFrameType
import com.moblin.android.media.haishinkit.flv.FlvSoundRate
import com.moblin.android.media.haishinkit.flv.FlvSoundSize
import com.moblin.android.media.haishinkit.flv.FlvSoundType
import com.moblin.android.media.haishinkit.flv.FlvTagType
import com.moblin.android.media.haishinkit.flv.FlvVideoCodec
import com.moblin.android.media.haishinkit.flv.FlvVideoFourCC
import com.moblin.android.media.haishinkit.flv.FlvVideoPacketType
import com.moblin.android.media.haishinkit.mpeg.MpegTsAudioConfig
import com.moblin.android.media.haishinkit.mpeg.avc.MpegTsVideoConfigAvc
import com.moblin.android.media.haishinkit.mpeg.hevc.MpegTsVideoConfigHevc
import com.moblin.android.media.haishinkit.rtmp.RtmpChunk
import com.moblin.android.media.haishinkit.rtmp.RtmpChunkType
import com.moblin.android.media.haishinkit.rtmp.amf.Amf0Decoder
import com.moblin.android.media.haishinkit.rtmp.amf.AsObject
import com.moblin.android.media.haishinkit.rtmp.amf.AsValue
import com.moblin.android.media.haishinkit.rtmp.extendedVideoHeader
import com.moblin.android.media.haishinkit.rtmp.message.RtmpCommandMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpCommandName
import com.moblin.android.media.haishinkit.rtmp.message.RtmpMessageType
import com.moblin.android.media.haishinkit.rtmp.message.RtmpSetChunkSizeMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpSetPeerBandwidthMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpWindowAcknowledgementSizeMessage
import java.net.URI

private const val TAG = "RtmpServerChunkStream"

private fun ByteArray.getFourBytesBe(): UInt {
    return ((this[0].toInt() and 0xFF).toUInt() shl 24) or
        ((this[1].toInt() and 0xFF).toUInt() shl 16) or
        ((this[2].toInt() and 0xFF).toUInt() shl 8) or
        ((this[3].toInt() and 0xFF).toUInt() shl 0)
}

private fun ByteArray.hexString(): String {
    return joinToString(separator = "") { "%02x".format(it) }
}

class RtmpServerChunkStream(
    private var client: RtmpServerClient?,
    private val streamId: UShort,
    private val softwareDecoding: Boolean,
) : VideoDecoderDelegate {
    private var messageBody: ByteArray = ByteArray(0)
    var messageLength: Int = 0
    var messageTypeId: UByte = 0u.toUByte()
    var messageTimestamp: UInt = 0u
    var messageStreamId: UInt = 0u
    var isAbsoluteTimeStamp: Boolean = true
    var extendedTimestampPresentInType3: Boolean = false
    private var mediaTimestamp: Double = 0.0
    private var mediaTimestampZero: Double = -1.0
    private var videoTimestamp: Double = -1.0
    private var formatDescription: MediaFormat? = null
    private var videoDecoder: VideoDecoder? = null
    private var audioBuffer: ByteArray? = null
    private var audioDecoder: MediaCodec? = null
    private var pcmAudioFormat: MediaFormat? = null
    private var pcmAudioBuffer: ShortArray? = null

    fun stop() {
        videoDecoder?.stopRunning()
        videoDecoder = null
        client = null
    }

    fun getChunkDataSize(): Int {
        val client = this.client ?: return 0
        return minOf(client.chunkSizeFromClient, messageRemain())
    }

    fun handleBody(data: ByteArray) {
        messageBody += data
        if (messageRemain() == 0) {
            processMessage()
            messageBody = ByteArray(0)
        }
    }

    private fun messageRemain(): Int {
        return messageLength - messageBody.size
    }

    private fun processMessage() {
        val messageType = RtmpMessageType.fromRawValue(messageTypeId)
        if (messageType == null) {
            Log.i(TAG, "rtmp-server: client: Bad message type $messageTypeId")
            return
        }
        if (isAbsoluteTimeStamp) {
            mediaTimestamp = messageTimestamp.toDouble()
        } else {
            mediaTimestamp += messageTimestamp.toDouble()
        }
        if (mediaTimestampZero == -1.0) {
            mediaTimestampZero = mediaTimestamp
        }
        when (messageType) {
            RtmpMessageType.amf0Command -> processMessageAmf0Command()
            RtmpMessageType.amf0Data -> processMessageAmf0Data()
            RtmpMessageType.chunkSize -> processMessageChunkSize()
            RtmpMessageType.windowAck -> processMessageWindowAck()
            RtmpMessageType.video -> processMessageVideo()
            RtmpMessageType.audio -> processMessageAudio()
            else -> Log.i(TAG, "rtmp-server: client: Message type $messageType not supported")
        }
    }

    private fun processMessageAmf0Command() {
        val client = this.client ?: return
        val decoder = Amf0Decoder(messageBody)
        var commandName: RtmpCommandName = RtmpCommandName.unknown
        var transactionId = 0
        var commandObject: AsObject? = null
        val arguments = mutableListOf<AsValue>()
        try {
            commandName = RtmpCommandName.fromRawValue(decoder.decodeString()) ?: RtmpCommandName.unknown
            transactionId = decoder.decodeInt()
            commandObject = decoder.decodeObject()
            if (decoder.bytesAvailable > 0) {
                arguments.add(decoder.decode())
            }
        } catch (e: Exception) {
            client.stopInternal("AMF-0 decode error $e")
            return
        }
        val object0 = commandObject ?: return
        when (commandName) {
            RtmpCommandName.connect -> processMessageAmf0CommandConnect(transactionId, object0)
            RtmpCommandName.fcPublish -> processMessageAmf0CommandFCPublish(transactionId)
            RtmpCommandName.fcUnpublish -> processMessageAmf0CommandFCUnpublish(transactionId)
            RtmpCommandName.createStream -> processMessageAmf0CommandCreateStream(transactionId)
            RtmpCommandName.deleteStream -> processMessageAmf0CommandDeleteStream(transactionId)
            RtmpCommandName.publish -> processMessageAmf0CommandPublish(transactionId, arguments)
            else -> Log.i(TAG, "rtmp-server: client: Unsupported command $commandName")
        }
    }

    private fun processMessageAmf0Data() {
        Log.i(TAG, "rtmp-server: client: Ignoring AMF-0 data")
    }

    private fun processMessageAmf0CommandConnect(transactionId: Int, commandObject: AsObject) {
        val client = this.client ?: return
        val tcUrl = commandObject["tcUrl"]
        if (tcUrl !is AsValue.String) {
            client.stopInternal("Stream URL missing")
            return
        }
        val url = tcUrl.value
        val uri = runCatching { URI(url) }.getOrNull()
        if (uri == null) {
            client.stopInternal("Invalid stream URL")
            return
        }
        if (uri.path != rtmpServerApp) {
            client.stopInternal("Not a camera path")
            return
        }
        client.sendMessage(
            RtmpChunk(
                type = RtmpChunkType.zero,
                chunkStreamId = RtmpChunk.ChunkStreamId.control.rawValue,
                message = RtmpWindowAcknowledgementSizeMessage(2_500_000),
            )
        )
        client.sendMessage(
            RtmpChunk(
                type = RtmpChunkType.zero,
                chunkStreamId = RtmpChunk.ChunkStreamId.control.rawValue,
                message = RtmpSetPeerBandwidthMessage(2_500_000, RtmpSetPeerBandwidthMessage.Limit.dynamic),
            )
        )
        client.sendMessage(
            RtmpChunk(
                type = RtmpChunkType.zero,
                chunkStreamId = RtmpChunk.ChunkStreamId.control.rawValue,
                message = RtmpSetChunkSizeMessage(1024),
            )
        )
        client.chunkSizeToClient = 1024
        client.sendMessage(
            RtmpChunk(
                type = RtmpChunkType.zero,
                chunkStreamId = streamId,
                message = RtmpCommandMessage(
                    streamId = messageStreamId,
                    transactionId = transactionId,
                    commandType = RtmpMessageType.amf0Command,
                    commandName = RtmpCommandName.result,
                    commandObject = null,
                    arguments = listOf(
                        AsValue.Object(
                            mapOf(
                                "level" to AsValue.String("status"),
                                "code" to AsValue.String("NetConnection.Connect.Success"),
                                "description" to AsValue.String("Connection succeeded."),
                            )
                        ),
                    ),
                ),
            )
        )
    }

    private fun processMessageAmf0CommandFCPublish(transactionId: Int) {}

    private fun processMessageAmf0CommandFCUnpublish(transactionId: Int) {}

    private fun processMessageAmf0CommandCreateStream(transactionId: Int) {
        client?.sendMessage(
            RtmpChunk(
                type = RtmpChunkType.zero,
                chunkStreamId = streamId,
                message = RtmpCommandMessage(
                    streamId = messageStreamId,
                    transactionId = transactionId,
                    commandType = RtmpMessageType.amf0Command,
                    commandName = RtmpCommandName.result,
                    commandObject = null,
                    arguments = listOf(
                        AsValue.Number(1.0),
                    ),
                ),
            )
        )
    }

    private fun processMessageAmf0CommandDeleteStream(transactionId: Int) {}

    private fun processMessageAmf0CommandPublish(transactionId: Int, arguments: List<AsValue>) {
        val client = this.client ?: return
        if (arguments.isEmpty()) {
            client.stopInternal("Missing publish argument")
            return
        }
        val firstArgument = arguments[0]
        if (firstArgument !is AsValue.String) {
            client.stopInternal("Stream key not a string")
            return
        }
        val streamKey = firstArgument.value
        val stream = client.server?.settings.streams
            ?.filter { it.streamKey.isNotEmpty() }
            ?.firstOrNull { it.streamKey == streamKey }
        val isStreamKeyConfigured: Boolean
        if (stream != null) {
            client.latency = stream.latency
            client.cameraId = stream.id
            isStreamKeyConfigured = true
        } else {
            isStreamKeyConfigured = false
        }
        if (!isStreamKeyConfigured) {
            client.stopInternal("Stream key $streamKey not configured")
            return
        }
        client.streamKey = streamKey
        client.connectionState = RtmpServerClientConnectionState.connected
        client.server?.handleClientConnected(client)
        client.sendMessage(
            RtmpChunk(
                type = RtmpChunkType.zero,
                chunkStreamId = streamId,
                message = RtmpCommandMessage(
                    streamId = messageStreamId,
                    transactionId = transactionId,
                    commandType = RtmpMessageType.amf0Command,
                    commandName = RtmpCommandName.onStatus,
                    commandObject = null,
                    arguments = listOf(
                        AsValue.Object(
                            mapOf(
                                "level" to AsValue.String("status"),
                                "code" to AsValue.String("NetStream.Publish.Start"),
                                "description" to AsValue.String("Start publishing."),
                            )
                        ),
                    ),
                ),
            )
        )
    }

    private fun processMessageChunkSize() {
        val client = this.client ?: return
        if (messageBody.size != 4) {
            client.stopInternal("Not 4 bytes chunk size")
            return
        }
        client.chunkSizeFromClient = messageBody.getFourBytesBe().toInt()
        Log.i(TAG, "rtmp-server: client: Chunk size from client: ${client.chunkSizeFromClient}")
    }

    private fun processMessageWindowAck() {
        val client = this.client ?: return
        if (messageBody.size != 4) {
            client.stopInternal("Not 4 bytes window acknowledgement size")
            return
        }
        client.windowAcknowledgementSize = messageBody.getFourBytesBe().toInt()
        Log.i(TAG, "rtmp-server: client: Window acknowledgement size from client: ${client.windowAcknowledgementSize}")
    }

    private fun processMessageAudio() {
        val client = this.client ?: return
        if (!checkMessageBodyBigEnough(client, 2)) {
            return
        }
        val control = messageBody[0].toInt() and 0xFF
        val codec = FlvAudioCodec.fromRawValue((control shr 4).toUByte())
        if (codec == null) {
            client.stopInternal("Failed to parse audio settings $control")
            return
        }
        if (codec != FlvAudioCodec.aac) {
            client.stopInternal("Unsupported audio codec $codec. Only AAC is supported.")
            return
        }
        if (FlvSoundRate.fromRawValue(((control and 0x0C) shr 2).toUByte()) == null ||
            FlvSoundSize.fromRawValue(((control and 0x02) shr 1).toUByte()) == null ||
            FlvSoundType.fromRawValue((control and 0x01).toUByte()) == null
        ) {
            client.stopInternal("Failed to parse audio settings $control")
            return
        }
        when (FlvAacPacketType.fromRawValue(messageBody[1].toUByte())) {
            FlvAacPacketType.seq -> processMessageAudioTypeSeq(client, codec)
            FlvAacPacketType.raw -> processMessageAudioTypeRaw(client, codec)
            else -> {
            }
        }
    }

    private fun processMessageAudioTypeSeq(client: RtmpServerClient, codec: FlvAudioCodec) {
        val config = MpegTsAudioConfig(messageBody.copyOfRange(codec.headerSize, messageBody.size)) ?: return
        Log.i(TAG, "rtmp-server: client: $config")
        audioBuffer = TODO("AVAudioCompressedBuffer port: allocate the compressed AAC packet buffer")
        audioDecoder = TODO("AVAudioConverter port: configure a MediaCodec audio/mp4a-latm decoder with csd-0 from the sequence header")
        pcmAudioFormat = TODO("AVAudioFormat port: build an android.media.MediaFormat for the decoded 16 bit PCM output")
        pcmAudioBuffer = TODO("AVAudioPCMBuffer port: allocate a 1024 frame ShortArray for the decoded PCM")
    }

    private fun processMessageAudioTypeRaw(client: RtmpServerClient, codec: FlvAudioCodec) {
        val audioBuffer = this.audioBuffer ?: return
        val length = messageBody.size - codec.headerSize
        if (length <= 0) {
            return
        }
        if (length > audioBuffer.size) {
            Log.i(TAG, "rtmp-server: Audio packet too long ($length > ${audioBuffer.size})")
            return
        }
        messageBody.copyInto(audioBuffer, 0, codec.headerSize, codec.headerSize + length)
        if (audioDecoder == null || pcmAudioBuffer == null) {
            return
        }
        val pcm = TODO<ShortArray>("MediaCodec audio/mp4a-latm decode of the $length byte AAC packet")
        val sampleBuffer = makeAudioSampleBuffer(client, pcm) ?: return
        client.handleAudioBuffer(sampleBuffer)
    }

    private fun processMessageVideo() {
        val client = this.client ?: return
        if (!checkMessageBodyBigEnough(client, 2)) {
            return
        }
        val control = messageBody[0].toInt() and 0xFF
        val isExVideoHeader = (control and extendedVideoHeader.toInt()) == extendedVideoHeader.toInt()
        if (isExVideoHeader) {
            processMessageVideoExtendedHeader(client, control)
        } else {
            processMessageVideoDefaultHeader(client, control)
        }
    }

    private fun processMessageVideoDefaultHeader(client: RtmpServerClient, control: Int) {
        val format = FlvVideoCodec.fromRawValue((control and 0xF).toUByte())
        if (format == null) {
            client.stopInternal("Unsupported video codec ${control and 0xF}")
            return
        }
        when (FlvAvcPacketType.fromRawValue(messageBody[1].toUByte())) {
            FlvAvcPacketType.seq -> processMessageVideoTypeSeq(client, format)
            FlvAvcPacketType.nal -> processMessageVideoTypeNal(client)
            else -> Log.i(TAG, "rtmp-server: Unsupported video $format packet type ${messageBody[1]}")
        }
    }

    private fun processMessageVideoExtendedHeader(client: RtmpServerClient, control: Int) {
        if (!checkMessageBodyBigEnough(client, 5)) {
            return
        }
        val frameType = (control shr 4) and 0b111
        val videoType = FlvFrameType.fromRawValue(frameType.toUByte())
        if (videoType == null) {
            client.stopInternal("Unsupported video frame type $frameType")
            return
        }
        val packetTypeValue = control and 0b1111
        val packetType = FlvVideoPacketType.fromRawValue(packetTypeValue.toUByte())
        if (packetType == null) {
            client.stopInternal("Unsupported video packet type $packetTypeValue")
            return
        }
        val fourCc = ((messageBody[1].toInt() and 0xFF) shl 24) or
            ((messageBody[2].toInt() and 0xFF) shl 16) or
            ((messageBody[3].toInt() and 0xFF) shl 8) or
            ((messageBody[4].toInt() and 0xFF) shl 0)
        val fourCcValue = FlvVideoFourCC.fromRawValue(fourCc.toUInt())
        if (fourCcValue == null) {
            client.stopInternal("Unsupported fourCC $fourCc")
            return
        }
        if (fourCcValue != FlvVideoFourCC.hevc) {
            client.stopInternal("Unsupported fourCC $fourCcValue.")
            return
        }
        when (packetType) {
            FlvVideoPacketType.sequenceStart -> processMessageVideoTypeSequenceStart(client)
            FlvVideoPacketType.codedFrames -> processMessageVideoTypeCodedFrames(client, videoType == FlvFrameType.key)
            FlvVideoPacketType.sequenceEnd -> client.stopInternal("Stream ended")
            FlvVideoPacketType.codedFramesX -> processMessageVideoTypeCodedFramesX(client, videoType == FlvFrameType.key)
            else -> Log.i(TAG, "rtmp-server: Unsupported video packet type $packetType")
        }
    }

    private fun processMessageVideoTypeSeq(client: RtmpServerClient, format: FlvVideoCodec) {
        if (!checkMessageBodyBigEnough(client, FlvTagType.video.headerSize)) {
            return
        }
        val configRecord = messageBody.copyOfRange(FlvTagType.video.headerSize, messageBody.size)
        val newFormatDescription = when (format) {
            FlvVideoCodec.avc -> MpegTsVideoConfigAvc(configRecord).makeFormatDescription()
            FlvVideoCodec.hevc -> MpegTsVideoConfigHevc(configRecord).makeFormatDescription()
            else -> null
        }
        if (newFormatDescription != null) {
            formatDescription = newFormatDescription
            setupVideoEncoderIfNeeded(formatDescription)
        } else {
            client.stopInternal("$format format description error")
        }
    }

    private fun processMessageVideoTypeSequenceStart(client: RtmpServerClient) {
        if (!checkMessageBodyBigEnough(client, FlvTagType.video.headerSize)) {
            return
        }
        val hvcC = messageBody.copyOfRange(FlvTagType.video.headerSize, messageBody.size)
        val videoConfig = MpegTsVideoConfigHevc(hvcC)
        val newFormatDescription = videoConfig.makeFormatDescription()
        if (newFormatDescription != null) {
            formatDescription = newFormatDescription
            setupVideoEncoderIfNeeded(formatDescription)
        } else {
            client.stopInternal("H.265/HEVC format description error")
        }
    }

    private fun setupVideoEncoderIfNeeded(formatDescription: MediaFormat?) {
        if (videoDecoder != null) {
            return
        }
        videoDecoder = VideoDecoder(
            name = "rtmp-server",
            lockQueue = rtmpServerDispatchQueue,
            softwareDecoding = softwareDecoding,
        )
        videoDecoder?.delegate = this
        videoDecoder?.startRunning(formatDescription = formatDescription)
    }

    private fun processMessageVideoTypeNal(client: RtmpServerClient) {
        if (messageBody.size <= 9) {
            Log.i(TAG, "rtmp-server: client: Dropping short packet with data ${messageBody.hexString()}")
            return
        }
        val isKeyFrame = (((messageBody[0].toInt() and 0xFF) shr 4) and 0b0111) == FlvFrameType.key.rawValue.toInt()
        processMessageVideoFrame(
            client = client,
            isKeyFrame = isKeyFrame,
            compositionTime = calcCompositionTime(2),
            dataOffset = FlvTagType.video.headerSize,
        )
    }

    private fun processMessageVideoTypeCodedFrames(client: RtmpServerClient, isKeyFrame: Boolean) {
        if (messageBody.size <= 9) {
            Log.i(TAG, "rtmp-server: client: Dropping short packet with data ${messageBody.hexString()}")
            return
        }
        processMessageVideoFrame(
            client = client,
            isKeyFrame = isKeyFrame,
            compositionTime = calcCompositionTime(5),
            dataOffset = FlvTagType.video.headerSize + 3,
        )
    }

    private fun calcCompositionTime(offset: Int): Int {
        var compositionTime = ((messageBody[offset].toInt() and 0xFF) shl 16) or
            ((messageBody[offset + 1].toInt() and 0xFF) shl 8) or
            ((messageBody[offset + 2].toInt() and 0xFF) shl 0)
        compositionTime = compositionTime shl 8
        compositionTime /= 256
        return compositionTime
    }

    private fun processMessageVideoTypeCodedFramesX(client: RtmpServerClient, isKeyFrame: Boolean) {
        processMessageVideoFrame(
            client = client,
            isKeyFrame = isKeyFrame,
            compositionTime = 0,
            dataOffset = FlvTagType.video.headerSize,
        )
    }

    private fun processMessageVideoFrame(
        client: RtmpServerClient,
        isKeyFrame: Boolean,
        compositionTime: Int,
        dataOffset: Int,
    ) {
        val sampleBuffer = makeVideoSampleBuffer(client, isKeyFrame, compositionTime, dataOffset) ?: return
        videoDecoder?.decodeSampleBuffer(sampleBuffer)
    }

    private fun makeVideoSampleBuffer(
        client: RtmpServerClient,
        isKeyFrame: Boolean,
        compositionTime: Int,
        dataOffset: Int,
    ): MediaSample? {
        val duration: Long = if (videoTimestamp == -1.0) {
            0L
        } else {
            ((mediaTimestamp - mediaTimestampZero) - videoTimestamp).toLong()
        }
        videoTimestamp = mediaTimestamp - mediaTimestampZero
        val basePresentationTimeStamp = videoTimestamp + getBasePresentationTimeStamp(client)
        val presentationTimeStamp = basePresentationTimeStamp.toLong() + (compositionTime + client.latency)
        val decodeTimeStamp = basePresentationTimeStamp.toLong() + client.latency
        return MediaSample(
            data = messageBody.copyOfRange(dataOffset, messageBody.size),
            presentationTimeUs = presentationTimeStamp * 1000L,
            isKeyFrame = isKeyFrame,
            format = formatDescription,
        )
    }

    private fun makeAudioSampleBuffer(client: RtmpServerClient, audioBuffer: ShortArray): MediaSample? {
        val audioTimestamp = mediaTimestamp - mediaTimestampZero
        val presentationTimeUs = ((audioTimestamp + getBasePresentationTimeStamp(client)) * 1000).toLong() +
            client.latency * 1000L
        return TODO("Wrap the ${audioBuffer.size} decoded PCM samples in a MediaSample with presentationTimeUs $presentationTimeUs")
    }

    private fun getBasePresentationTimeStamp(client: RtmpServerClient): Double {
        return client.getBasePresentationTimeStamp()
    }

    private fun checkMessageBodyBigEnough(client: RtmpServerClient, minimumSize: Int): Boolean {
        if (messageBody.size < minimumSize) {
            client.stopInternal("Got ${messageBody.size} bytes message, expected >= $minimumSize")
            return false
        }
        return true
    }

    override fun videoDecoderOutputSampleBuffer(videoDecoder: VideoDecoder, sampleBuffer: MediaSample) {
        client?.handleFrame(sampleBuffer)
    }
}
