package com.moblin.android.media.haishinkit.mpeg

import android.media.MediaFormat
import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderDelegate
import com.moblin.android.media.haishinkit.codec.video.VideoCodec
import com.moblin.android.media.haishinkit.codec.video.VideoEncoder
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderDelegate
import com.moblin.android.media.haishinkit.mpeg.avc.AvcNalUnit
import com.moblin.android.media.haishinkit.mpeg.avc.AvcNalUnitPayload
import com.moblin.android.media.haishinkit.mpeg.avc.AvcNalUnitSei
import com.moblin.android.media.haishinkit.mpeg.avc.AvcNalUnitSeiPayload
import com.moblin.android.media.haishinkit.mpeg.avc.AvcNalUnitType
import com.moblin.android.media.haishinkit.mpeg.avc.AvcSeiPayloadPictureTiming
import com.moblin.android.media.haishinkit.mpeg.avc.MpegTsVideoConfigAvc
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcNalUnit
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcNalUnitSei
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcNalUnitSeiPayload
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcNalUnitType
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcSeiPayloadTimeCode
import com.moblin.android.media.haishinkit.mpeg.hevc.MpegTsVideoConfigHevc
import com.moblin.android.various.utils.currentPresentationTimeStamp
import java.time.Instant

var payloadSize = 1316

interface MpegTsWriterDelegate {
    fun writer(writer: MpegTsWriter, doOutput: ByteArray, containsAudio: Boolean)
    fun writer(writer: MpegTsWriter, doOutputPointer: ByteArray, count: Int)
}

class MpegTsWriter(timecodesEnabled: Boolean, private val newSrt: Boolean) :
    AudioEncoderDelegate,
    VideoEncoderDelegate
{
    companion object {
        val programAssociationTablePacketId: UShort = 0u
        val programMappingTablePacketId: UShort = 4095u
        val audioPacketId: UShort = 257u
        val videoPacketId: UShort = 256u
        private val audioStreamId: UByte = 192u
        private val videoStreamId: UByte = 224u
        private val segmentDuration: Long = 2_000_000L
        private const val TAG = "MpegTsWriter"
    }

    var delegate: MpegTsWriterDelegate? = null
    private var isRunning = false
    private var audioContinuityCounter: UByte = 0u
    private var videoContinuityCounter: UByte = 0u
    private var patContinuityCounter: UByte = 0u
    private var pmtContinuityCounter: UByte = 0u
    private var latestPeriodicallySendProgramTime: Long = 0L
    private var videoData: MutableList<ByteArray?> = mutableListOf(null, null)
    private var videoDataOffset = 0

    private val programAssociationTable: MpegTsProgramAssociation = MpegTsProgramAssociation().apply {
        programs.clear()
        programs[1] = programMappingTablePacketId
    }

    private var programMappingTable = MpegTsProgramMapping()
    private var audioConfig: MpegTsAudioConfig? = null
    private var videoConfig: MpegTsVideoConfig? = null
    private var programClockReferenceTimestamp: Long? = null
    private val timecodeGenerator: MpegTsTimecodeGenerator? =
        if (timecodesEnabled) MpegTsTimecodeGenerator() else null

    fun startRunning() {
        isRunning = true
    }

    fun stopRunning() {
        if (!isRunning) {
            return
        }
        audioContinuityCounter = 0u
        videoContinuityCounter = 0u
        patContinuityCounter = 0u
        pmtContinuityCounter = 0u
        programAssociationTable.programs.clear()
        programAssociationTable.programs[1] = programMappingTablePacketId
        programMappingTable = MpegTsProgramMapping()
        audioConfig = null
        videoConfig = null
        videoDataOffset = 0
        videoData = mutableListOf(null, null)
        programClockReferenceTimestamp = null
        timecodeGenerator?.reset()
        isRunning = false
    }

    private fun setAudioConfig(config: MpegTsAudioConfig) {
        audioConfig = config
        writeProgramIfNeeded()
    }

    private fun setVideoConfig(config: MpegTsVideoConfig) {
        videoConfig = config
        writeProgramIfNeeded()
    }

    private fun canWriteFor(): Boolean {
        return (audioConfig != null) && (videoConfig != null)
    }

    private fun encode(packetId: UShort, packets: List<MpegTsPacket>): ByteArray {
        val packetsBuffer = createPacketsBuffer(packets.size)
        var offset = 0
        for (packet in packets) {
            packet.continuityCounter = nextContinuityCounter(packetId)
            packet.encodeFixedHeaderInto(packetsBuffer, offset)
            offset += MpegTsPacket.fixedHeaderSize
            val adaptationField = packet.adaptationField
            if (adaptationField != null) {
                val encodedAdaptationField = adaptationField.encode()
                encodedAdaptationField.copyInto(packetsBuffer, offset)
                offset += encodedAdaptationField.size
            }
            packet.payload.copyInto(packetsBuffer, offset)
            offset += packet.payload.size
        }
        return packetsBuffer
    }

    private fun createPacketsBuffer(packetsCount: Int): ByteArray {
        val packetsBufferSize = packetsCount * MpegTsPacket.size
        return ByteArray(packetsBufferSize)
    }

    private fun nextContinuityCounter(packetId: UShort): UByte {
        return when (packetId) {
            audioPacketId -> {
                val value = audioContinuityCounter
                audioContinuityCounter = ((audioContinuityCounter.toInt() + 1) and 0x0F).toUByte()
                value
            }
            videoPacketId -> {
                val value = videoContinuityCounter
                videoContinuityCounter = ((videoContinuityCounter.toInt() + 1) and 0x0F).toUByte()
                value
            }
            else -> 0u
        }
    }

    private fun nextProgramAssociationTableContinuityCounter(): UByte {
        val value = patContinuityCounter
        patContinuityCounter = ((patContinuityCounter.toInt() + 1) and 0x0F).toUByte()
        return value
    }

    private fun nextProgramMappingTableContinuityCounter(): UByte {
        val value = pmtContinuityCounter
        pmtContinuityCounter = ((pmtContinuityCounter.toInt() + 1) and 0x0F).toUByte()
        return value
    }

    private fun periodicallySendProgram(now: Long) {
        if (now - latestPeriodicallySendProgramTime <= segmentDuration) {
            return
        }
        writeProgram()
        latestPeriodicallySendProgramTime = now
    }

    private fun writeNew(data: ByteArray, containsAudio: Boolean) {
        delegate?.writer(this, data, containsAudio)
    }

    private fun writeOld(data: ByteArray) {
        writeBytesOld(data)
    }

    private fun writePacketPointerOld(pointer: ByteArray, count: Int) {
        delegate?.writer(this, pointer, count)
    }

    private fun writeBytesOld(data: ByteArray) {
        writeBytesPointerOld(data, data.size)
    }

    private fun writeBytesPointerOld(pointer: ByteArray, count: Int) {
        var offset = 0
        while (offset < count) {
            val length = minOf(payloadSize, count - offset)
            writePacketPointerOld(pointer.copyOfRange(offset, offset + length), length)
            offset += payloadSize
        }
    }

    private fun appendVideoData(data: ByteArray?) {
        videoData[0] = videoData[1]
        videoData[1] = data
        videoDataOffset = 0
    }

    private fun writeVideo(data: ByteArray) {
        if (newSrt) {
            writeVideoNew(data)
        } else {
            writeVideoOld(data)
        }
    }

    private fun writeAudio(data: ByteArray) {
        if (newSrt) {
            writeAudioNew(data)
        } else {
            writeAudioOld(data)
        }
    }

    private fun writeVideoNew(data: ByteArray) {
        val frame = videoData[0]
        if (frame != null) {
            writeNew(frame.copyOfRange(videoDataOffset, frame.size), false)
        }
        appendVideoData(data)
    }

    private fun writeAudioNew(data: ByteArray) {
        val frame = videoData[0]
        if (frame != null) {
            for (chunk in data.chunks(payloadSize)) {
                var packet = chunk
                val videoSize = payloadSize - packet.size
                if (videoSize > 0) {
                    val endOffset = minOf(videoDataOffset + videoSize, frame.size)
                    if (videoDataOffset != endOffset) {
                        packet = frame.copyOfRange(videoDataOffset, endOffset) + packet
                        videoDataOffset = endOffset
                    }
                }
                writeNew(packet, true)
            }
            if (videoDataOffset == frame.size) {
                appendVideoData(null)
            }
        } else {
            writeNew(data, true)
        }
    }

    private fun writeVideoOld(data: ByteArray) {
        val frame = videoData[0]
        if (frame != null) {
            writeBytesPointerOld(
                frame.copyOfRange(videoDataOffset, frame.size),
                frame.size - videoDataOffset
            )
        }
        appendVideoData(data)
    }

    private fun writeAudioOld(data: ByteArray) {
        val frame = videoData[0]
        if (frame != null) {
            for (chunk in data.chunks(payloadSize)) {
                var packet = chunk
                val videoSize = payloadSize - packet.size
                if (videoSize > 0) {
                    val endOffset = minOf(videoDataOffset + videoSize, frame.size)
                    if (videoDataOffset != endOffset) {
                        packet = frame.copyOfRange(videoDataOffset, endOffset) + packet
                        videoDataOffset = endOffset
                    }
                }
                writeOld(packet)
            }
            if (videoDataOffset == frame.size) {
                appendVideoData(null)
            }
        } else {
            writeOld(data)
        }
    }

    private fun writeProgram() {
        programMappingTable.programClockReferencePacketId = audioPacketId
        val patPacket = programAssociationTable.packet(programAssociationTablePacketId)
        val pmtPacket = programMappingTable.packet(programMappingTablePacketId)
        patPacket.continuityCounter = nextProgramAssociationTableContinuityCounter()
        pmtPacket.continuityCounter = nextProgramMappingTableContinuityCounter()
        if (newSrt) {
            writeNew(patPacket.encode() + pmtPacket.encode(), false)
        } else {
            writeOld(patPacket.encode() + pmtPacket.encode())
        }
    }

    private fun writeProgramIfNeeded() {
        if (!canWriteFor()) {
            return
        }
        writeProgram()
    }

    private fun updateProgramClockReference(timestamp: Long): ULong? {
        var programClockReference: ULong? = null
        if (timestamp - (programClockReferenceTimestamp ?: 0L) >= 100_000L) {
            programClockReference =
                (maxOf(timestamp, 0L) / 1_000_000.0 * TSTimestamp.resolution).toLong().toULong()
            programClockReferenceTimestamp = timestamp
        }
        return programClockReference
    }

    private fun addStreamSpecificDatasToProgramMappingTable(
        packetId: UShort,
        data: ElementaryStreamSpecificData
    ) {
        val index = programMappingTable.elementaryStreamSpecificDatas.indexOfFirst {
            it.elementaryPacketId == packetId
        }
        if (index != -1) {
            programMappingTable.elementaryStreamSpecificDatas[index] = data
        } else {
            programMappingTable.elementaryStreamSpecificDatas.add(data)
        }
    }

    private fun addAudioSpecificDatas(data: ElementaryStreamSpecificData) {
        addStreamSpecificDatasToProgramMappingTable(audioPacketId, data)
    }

    private fun addVideoSpecificDatas(data: ElementaryStreamSpecificData) {
        addStreamSpecificDatasToProgramMappingTable(videoPacketId, data)
    }

    private fun makeAudioHeader(config: MpegTsAudioConfig, length: Int): ByteArray {
        return when (config.type) {
            MpegTsAudioConfig.Format.OPUS -> makeAudioOpusHeader(length)
            else -> makeAudioAacHeader(config, length)
        }
    }

    private fun makeAudioAacHeader(config: MpegTsAudioConfig, length: Int): ByteArray {
        return AdtsHeader.encode(
            type = config.type.rawValue,
            frequency = config.frequency.rawValue,
            channels = config.channel.rawValue,
            length = length
        )
    }

    private fun makeAudioOpusHeader(length: Int): ByteArray {
        return OpusHeader.encode(length = length)
    }

    override fun audioEncoderOutputFormat(format: MediaFormat) {
        Log.i(TAG, "ts-writer: Audio setup $format")
        val data = ElementaryStreamSpecificData()
        when (format.getString(MediaFormat.KEY_MIME)) {
            MediaFormat.MIMETYPE_AUDIO_AAC -> {
                data.streamType = ElementaryStreamType.ADTS_AAC
            }
            MediaFormat.MIMETYPE_AUDIO_OPUS -> {
                data.streamType = ElementaryStreamType.MPEG2_PACKETIZED_DATA
                data.appendDescriptor(
                    tag = ElementaryStreamDescriptiorTag.REGISTRATION,
                    data = ElementaryStreamDescriptiorRegistration.opus
                )
                data.appendDescriptor(
                    tag = ElementaryStreamDescriptiorTag.EXTENSION,
                    data = byteArrayOf(
                        0x80.toByte(),
                        format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).toByte()
                    )
                )
            }
            else -> {
                Log.i(TAG, "ts-writer: Unsupported audio format.")
                return
            }
        }
        data.elementaryPacketId = audioPacketId
        addAudioSpecificDatas(data)
        audioContinuityCounter = 0u
        setAudioConfig(MpegTsAudioConfig(format))
    }

    override fun audioEncoderOutputBuffer(buffer: ByteArray, presentationTimeStamp: Long) {
        if (!canWriteFor()) {
            return
        }
        val config = audioConfig ?: return
        val length = buffer.size
        var data = makeAudioHeader(config, length)
        data += buffer.copyOf(length)
        val packetizedElementaryStream = MpegTsPacketizedElementaryStream(
            streamId = audioStreamId,
            presentationTimeStamp = presentationTimeStamp,
            decodeTimeStamp = null,
            data = data
        )
        val programClockReference = updateProgramClockReference(presentationTimeStamp)
        val packets = packetizedElementaryStream.arrayOfPackets(
            audioPacketId,
            true,
            programClockReference
        )
        periodicallySendProgram(presentationTimeStamp)
        writeAudio(encode(audioPacketId, packets))
    }

    override fun videoEncoderOutputFormat(encoder: VideoEncoder, formatDescription: MediaFormat) {
        val data = ElementaryStreamSpecificData()
        data.elementaryPacketId = videoPacketId
        videoContinuityCounter = 0u
        val videoConfig: MpegTsVideoConfig = when (encoder.settings.value.format) {
            VideoCodec.H264 -> {
                data.streamType = ElementaryStreamType.H264
                val config = MpegTsVideoConfigAvc.fromFormatDescription(formatDescription)
                if (config == null) {
                    Log.i(TAG, "mpeg-ts: Failed to create avcC")
                    return
                }
                MpegTsVideoConfig.Avc(config)
            }
            VideoCodec.HEVC -> {
                data.streamType = ElementaryStreamType.H265
                val config = MpegTsVideoConfigHevc.fromFormatDescription(formatDescription)
                if (config == null) {
                    Log.i(TAG, "mpeg-ts: Failed to create hvcC")
                    return
                }
                MpegTsVideoConfig.Hevc(config)
            }
        }
        addVideoSpecificDatas(data)
        setVideoConfig(videoConfig)
    }

    override fun videoEncoderOutputSampleBuffer(
        encoder: VideoEncoder,
        sampleBuffer: MediaSample,
        decodeTimeStampOffset: Long
    ) {
        if (!canWriteFor()) {
            return
        }
        val config = videoConfig ?: return
        val bytes = sampleBuffer.data
        val length = bytes.size
        val decodeTimeStamp = sampleBuffer.presentationTimeUs - decodeTimeStampOffset
        val randomAccessIndicator = sampleBuffer.isKeyFrame
        val timecode = makeTimecode(sampleBuffer.presentationTimeUs, decodeTimeStamp)
        val data: ByteArray = when (config) {
            is MpegTsVideoConfig.Avc ->
                packH264(randomAccessIndicator, config.value, timecode, bytes, length)
            is MpegTsVideoConfig.Hevc ->
                packH265(randomAccessIndicator, config.value, timecode, bytes, length)
        }
        val packetizedElementaryStream = MpegTsPacketizedElementaryStream(
            streamId = videoStreamId,
            presentationTimeStamp = sampleBuffer.presentationTimeUs,
            decodeTimeStamp = decodeTimeStamp,
            data = data
        )
        val packets = packetizedElementaryStream.arrayOfPackets(
            videoPacketId,
            randomAccessIndicator,
            null
        )
        writeVideo(encode(videoPacketId, packets))
    }

    private fun packH264(
        randomAccessIndicator: Boolean,
        config: MpegTsVideoConfigAvc,
        timecode: MpegTsTimecode?,
        bytes: ByteArray,
        length: Int
    ): ByteArray {
        var data = ByteArray(0)
        if (randomAccessIndicator) {
            data += AvcNalUnit.aud10WithStartCode
            val sequenceParameterSet = config.sequenceParameterSet
            if (sequenceParameterSet != null) {
                data += nalUnitStartCode
                data += sequenceParameterSet
            }
            val pictureParameterSet = config.pictureParameterSet
            if (pictureParameterSet != null) {
                data += nalUnitStartCode
                data += pictureParameterSet
            }
        } else {
            data += AvcNalUnit.aud30WithStartCode
        }
        if (timecode != null && false) {
            data += nalUnitStartCode
            val pictureTiming = AvcSeiPayloadPictureTiming(timecode.clock, timecode.frame)
            val sei = AvcNalUnitSei(AvcNalUnitSeiPayload.PictureTiming(pictureTiming))
            data += AvcNalUnit(AvcNalUnitType.SEI, AvcNalUnitPayload.Sei(sei)).encode()
        }
        val payload = addNalUnitStartCodes(bytes.copyOf(length))
        data += payload
        return data
    }

    private fun packH265(
        randomAccessIndicator: Boolean,
        config: MpegTsVideoConfigHevc,
        timecode: MpegTsTimecode?,
        bytes: ByteArray,
        length: Int
    ): ByteArray {
        var data = ByteArray(0)
        if (randomAccessIndicator) {
            val videoParameterSet = config.videoParameterSet
            if (videoParameterSet != null) {
                data += nalUnitStartCode
                data += videoParameterSet
            }
            val sequenceParameterSet = config.sequenceParameterSet
            if (sequenceParameterSet != null) {
                data += nalUnitStartCode
                data += sequenceParameterSet
            }
            val pictureParameterSet = config.pictureParameterSet
            if (pictureParameterSet != null) {
                data += nalUnitStartCode
                data += pictureParameterSet
            }
        }
        if (timecode != null) {
            data += nalUnitStartCode
            val timeCode = HevcSeiPayloadTimeCode(timecode.clock, timecode.frame)
            val sei = HevcNalUnitSei(HevcNalUnitSeiPayload.TimeCode(timeCode))
            data += HevcNalUnit(
                HevcNalUnitType.PREFIX_SEI_NUT,
                temporalIdPlusOne = 1,
                payload = HevcNalUnitSeiPayload.PrefixSeiNut(sei)
            ).encode()
        }
        val payload = addNalUnitStartCodes(bytes.copyOf(length))
        data += payload
        return data
    }

    private fun makeTimecode(presentationTimeStamp: Long, decodeTimeStamp: Long): MpegTsTimecode? {
        val generator = timecodeGenerator ?: return null
        if (!generator.hasReference()) {
            val now = Instant.now().toEpochMilli() / 1000.0
            generator.setReference(now, currentPresentationTimeStamp() / 1_000_000.0)
        }
        return generator.makeTimecode(presentationTimeStamp, decodeTimeStamp)
    }
}

private fun ByteArray.chunks(size: Int): List<ByteArray> {
    val result = mutableListOf<ByteArray>()
    var offset = 0
    while (offset < this.size) {
        val end = minOf(offset + size, this.size)
        result.add(this.copyOfRange(offset, end))
        offset = end
    }
    return result
}
