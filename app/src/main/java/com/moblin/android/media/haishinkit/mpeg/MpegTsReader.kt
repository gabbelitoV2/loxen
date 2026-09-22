package com.moblin.android.media.haishinkit.mpeg

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.WrappingTimestamp
import com.moblin.android.media.haishinkit.codec.video.VideoDecoder
import com.moblin.android.media.haishinkit.codec.video.VideoDecoderDelegate
import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.various.utils.currentPresentationTimeStamp
import kotlinx.coroutines.CoroutineDispatcher
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

private const val logTag = "MpegTsReader"

private const val invalidPresentationTimeUs = Long.MIN_VALUE

interface MpegTsReaderDelegate {
    fun mpegTsReaderAudioBuffer(sampleBuffer: MediaSample)

    fun mpegTsReaderVideoBuffer(sampleBuffer: MediaSample)
}

class MpegTsReader(
    private val name: String,
    private val decoderQueue: CoroutineDispatcher,
    private val timecodesEnabled: Boolean,
    private val softwareDecoding: Boolean,
    private val targetLatency: Double,
) : VideoDecoderDelegate {
    private var programAssociationTable = MpegTsProgramAssociation()
    private var programMappingTable: MutableMap<UShort, MpegTsProgramMapping> = mutableMapOf()
    private var programs: MutableMap<UShort, UShort> = mutableMapOf()
    private var elementaryStreamSpecificData: MutableMap<UShort, ElementaryStreamSpecificData> = mutableMapOf()
    private var packetizedElementaryStreams: MutableMap<UShort, MpegTsPacketizedElementaryStream> = mutableMapOf()
    private var formatDescriptions: MutableMap<UShort, MediaFormat> = mutableMapOf()
    private var adtsHeaders: MutableMap<UShort, AdtsHeader> = mutableMapOf()
    private var firstReceivedPresentationTimeStamp: Long? = null
    private var previousReceivedPresentationTimeStamps: MutableMap<UShort, Long> = mutableMapOf()
    private var basePresentationTimeStamp: Long = invalidPresentationTimeUs
    private var audioBuffer: ByteArray? = null
    private var pcmAudioBuffer: ShortArray? = null
    private var latestAudioBufferPresentationTimeStamp: Long? = null
    private var audioDecoder: MediaCodec? = null
    private var pcmAudioFormat: AudioFormat? = null
    private var videoDecoder: VideoDecoder? = null
    var delegate: MpegTsReaderDelegate? = null
    private val wrappingTimestamp = WrappingTimestamp(
        "MpegTsReader",
        (0x2_0000_0000L * 1_000_000L) / TSTimestamp.resolution,
    )

    fun handlePacketFromClient(packet: ByteArray) {
        val reader = ByteReader(packet)
        while (reader.bytesAvailable >= MpegTsPacket.size) {
            val packet = MpegTsPacket(reader)
            val programNumber = programs[packet.id]
            val data = elementaryStreamSpecificData[packet.id]
            if (packet.id == MpegTsPacket.programAssociationTableId) {
                handleProgramAssociationTable(packet)
            } else if (programNumber != null) {
                handleProgramMappingTable(programNumber, packet)
            } else if (data != null) {
                handleProgramMedia(packet, data)
            }
        }
    }

    private fun handleProgramAssociationTable(packet: MpegTsPacket) {
        programAssociationTable = MpegTsProgramAssociation(packet.payload)
        for ((programNumber, programId) in programAssociationTable.programs) {
            programs[programId] = programNumber
        }
    }

    private fun handleProgramMappingTable(programNumber: UShort, packet: MpegTsPacket) {
        programMappingTable[programNumber] = MpegTsProgramMapping(packet.payload)
        for (programMapping in programMappingTable.values) {
            for (data in programMapping.elementaryStreamSpecificDatas) {
                elementaryStreamSpecificData[data.elementaryPacketId] = data
            }
        }
    }

    private fun handleProgramMedia(packet: MpegTsPacket, data: ElementaryStreamSpecificData) {
        if (packet.payloadUnitStartIndicator) {
            tryMakeSampleBuffers(packet.id, data)
            packetizedElementaryStreams[packet.id] = MpegTsPacketizedElementaryStream(packet.payload)
        } else {
            packetizedElementaryStreams[packet.id]?.append(packet.payload)
        }
    }

    private fun handleAudioSampleBuffer(sampleBuffer: MediaSample) {
        val audioDecoder = this.audioDecoder ?: return
        val pcmAudioFormat = this.pcmAudioFormat ?: return
        val audioBuffer = this.audioBuffer ?: return
        val pcmAudioBuffer = this.pcmAudioBuffer ?: return
        val data = sampleBuffer.data
        val length = data.size
        if (length > audioBuffer.size) {
            return
        }
        data.copyInto(audioBuffer, 0, 0, length)
        if (!decodeAudio(audioDecoder, audioBuffer, length, pcmAudioBuffer)) {
            Log.i(logTag, "mpeg-ts-reader: Audio error")
            return
        }
        outputSilenceIfGap(sampleBuffer.presentationTimeUs, pcmAudioFormat)
        delegate?.mpegTsReaderAudioBuffer(makePcmAudioSampleBuffer(pcmAudioBuffer, sampleBuffer.presentationTimeUs))
    }

    private fun createAudioDecoder(formatDescription: MediaFormat): MediaCodec =
        TODO("MediaCodec audio/mp4a-latm decoder configuration port")

    private fun decodeAudio(
        audioDecoder: MediaCodec,
        input: ByteArray,
        length: Int,
        output: ShortArray,
    ): Boolean = TODO("MediaCodec audio/mp4a-latm decode port")

    private fun makePcmAudioSampleBuffer(pcmAudioBuffer: ShortArray, presentationTimeStamp: Long): MediaSample {
        val buffer = ByteBuffer.allocate(pcmAudioBuffer.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        buffer.asShortBuffer().put(pcmAudioBuffer)
        return MediaSample(buffer.array(), presentationTimeStamp, false, null)
    }

    private fun outputSilenceIfGap(presentationTimeStamp: Long, pcmAudioFormat: AudioFormat) {
        try {
            val latest = latestAudioBufferPresentationTimeStamp ?: return
            val pcmAudioBuffer = this.pcmAudioBuffer ?: return
            val samplesPerBuffer = pcmAudioBuffer.size / pcmAudioFormat.channelCount
            val sampleFrequency = pcmAudioFormat.sampleRate.toDouble()
            val numberOfGapBuffers = calcNumberOfGapBuffers(
                presentationTimeStamp,
                latest,
                samplesPerBuffer,
                sampleFrequency,
            )
            for (index in 0 until numberOfGapBuffers) {
                val timeOffset = (
                    samplesPerBuffer.toDouble() * (1 + index) * 1_000_000.0 /
                        sampleFrequency
                    ).toLong()
                val newPresentationTimeStamp = latest + timeOffset
                Log.i(
                    logTag,
                    "mpeg-ts-reader: Filling audio gap " +
                        "$latest..$presentationTimeStamp with $newPresentationTimeStamp",
                )
                delegate?.mpegTsReaderAudioBuffer(
                    createSilentSampleBuffer(pcmAudioFormat, newPresentationTimeStamp, samplesPerBuffer),
                )
            }
        } finally {
            latestAudioBufferPresentationTimeStamp = presentationTimeStamp
        }
    }

    private fun createSilentSampleBuffer(
        pcmAudioFormat: AudioFormat,
        presentationTimeStamp: Long,
        frameLength: Int,
    ): MediaSample {
        return MediaSample(
            ByteArray(frameLength * pcmAudioFormat.channelCount * 2),
            presentationTimeStamp,
            false,
            null,
        )
    }

    private fun calcNumberOfGapBuffers(
        presentationTimeStamp: Long,
        latestPresentationTimeStamp: Long,
        samplesPerBuffer: Int,
        sampleFrequency: Double,
    ): Int {
        val ptsDelta = (presentationTimeStamp - latestPresentationTimeStamp) / 1_000_000.0
        val timePerBuffer = samplesPerBuffer / sampleFrequency
        if (!ptsDelta.isFinite() || ptsDelta >= 10.0) {
            return 0
        }
        val numberOfGapBuffers = (ptsDelta / timePerBuffer - 1.0).roundToInt().toDouble()
        if (!numberOfGapBuffers.isFinite()) {
            return 0
        }
        return maxOf(numberOfGapBuffers.roundToInt(), 0)
    }

    private fun handleVideoSampleBuffer(sampleBuffer: MediaSample) {
        val videoDecoder = this.videoDecoder ?: return
        videoDecoder.decodeSampleBuffer(sampleBuffer)
    }

    private fun handleAudioFormatDescription(formatDescription: MediaFormat) {
        if (!formatDescription.containsKey(MediaFormat.KEY_SAMPLE_RATE) ||
            !formatDescription.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
        ) {
            return
        }
        val sampleRate = formatDescription.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = formatDescription.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        if (sampleRate <= 0 || channelCount <= 0) {
            Log.i(logTag, "mpeg-ts-reader: Failed to create audio format")
            audioBuffer = null
            audioDecoder = null
            return
        }
        val channelMask = if (channelCount == 1) {
            AudioFormat.CHANNEL_OUT_MONO
        } else {
            AudioFormat.CHANNEL_OUT_STEREO
        }
        val pcmFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(channelMask)
            .build()
        pcmAudioFormat = pcmFormat
        audioBuffer = ByteArray(1024 * channelCount)
        Log.i(
            logTag,
            "mpeg-ts-reader: in: ${sampleRate}Hz ${channelCount}ch, " +
                "out: ${pcmFormat.sampleRate}Hz ${pcmFormat.channelCount}ch",
        )
        pcmAudioBuffer = ShortArray(1024 * channelCount)
        audioDecoder = createAudioDecoder(formatDescription)
    }

    private fun handleVideoFormatDescription(formatDescription: MediaFormat) {
        val width = formatDescription.getInteger(MediaFormat.KEY_WIDTH)
        val height = formatDescription.getInteger(MediaFormat.KEY_HEIGHT)
        Log.i(logTag, "mpeg-ts-reader: Got new video dimensions ${width}x${height}")
        videoDecoder?.stopRunning()
        videoDecoder = VideoDecoder(name, decoderQueue, softwareDecoding)
        videoDecoder?.delegate = this
        videoDecoder?.startRunning(formatDescription)
    }

    private fun tryMakeSampleBuffers(packetId: UShort, data: ElementaryStreamSpecificData) {
        val packetizedElementaryStream = packetizedElementaryStreams[packetId] ?: return
        try {
            when (data.streamType) {
                ElementaryStreamType.mpeg2PacketizedData ->
                    makeSampleBufferMpeg2PacketizedData(packetId, data, packetizedElementaryStream)
                ElementaryStreamType.adtsAac ->
                    makeSampleBufferAac(packetId, packetizedElementaryStream)
                ElementaryStreamType.h264 ->
                    makeSampleBufferH264(packetId, packetizedElementaryStream)
                ElementaryStreamType.h265 ->
                    makeSampleBufferH265(packetId, packetizedElementaryStream)
                else -> Unit
            }
        } finally {
            packetizedElementaryStreams.remove(packetId)
        }
    }

    private fun makeSampleBufferMpeg2PacketizedData(
        packetId: UShort,
        data: ElementaryStreamSpecificData,
        packetizedElementaryStream: MpegTsPacketizedElementaryStream,
    ) {
        when (data.getDescriptor(ElementaryStreamDescriptiorTag.registration)) {
            ElementaryStreamDescriptiorRegistration.opus ->
                makeSampleBufferMpeg2PacketizedDataOpus(packetId, data, packetizedElementaryStream)
            else -> Unit
        }
    }

    private fun getOpusFormatDescription(
        packetId: UShort,
        data: ElementaryStreamSpecificData,
    ): MediaFormat? {
        val extensionData =
            data.getDescriptor(ElementaryStreamDescriptiorTag.extension) ?: return null
        if (extensionData.size != 2 || (extensionData[0].toInt() and 0xff) != 0x80) {
            return null
        }
        val channels = extensionData[1].toInt() and 0xff
        val formatDescription = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_OPUS,
            48000,
            channels,
        )
        if (formatDescriptions[packetId].toString() != formatDescription.toString()) {
            formatDescriptions[packetId] = formatDescription
            handleAudioFormatDescription(formatDescription)
        }
        return formatDescription
    }

    private fun makeSampleBufferMpeg2PacketizedDataOpus(
        packetId: UShort,
        data: ElementaryStreamSpecificData,
        packetizedElementaryStream: MpegTsPacketizedElementaryStream,
    ) {
        val formatDescription = getOpusFormatDescription(packetId, data) ?: return
        val header = OpusHeader.decode(packetizedElementaryStream.data) ?: return
        val length = header.first
        val payloadOffset = header.second
        val blockBuffer = packetizedElementaryStream.data.copyOfRange(
            payloadOffset,
            packetizedElementaryStream.data.size,
        )
        val sampleSizes = mutableListOf(length)
        val optionalHeader = packetizedElementaryStream.optionalHeader
        val sampleBuffer = makeSampleBuffer(
            packetId,
            optionalHeader.getPresentationTimeStamp(),
            optionalHeader.getDecodeTimeStamp(),
            formatDescription,
            blockBuffer,
            sampleSizes,
        ) ?: return
        handleAudioSampleBuffer(sampleBuffer)
    }

    private fun getAacFormatDescription(
        packetId: UShort,
        packetizedElementaryStream: MpegTsPacketizedElementaryStream,
    ): MediaFormat? {
        val adtsHeader = AdtsHeader(packetizedElementaryStream.data) ?: return null
        if (adtsHeader.isSameFormatDescription(adtsHeaders[packetId])) {
            return formatDescriptions[packetId]
        }
        val formatDescription = adtsHeader.makeFormatDescription() ?: return null
        adtsHeaders[packetId] = adtsHeader
        formatDescriptions[packetId] = formatDescription
        handleAudioFormatDescription(formatDescription)
        return formatDescription
    }

    private fun makeSampleBufferAac(
        packetId: UShort,
        packetizedElementaryStream: MpegTsPacketizedElementaryStream,
    ) {
        val formatDescription = getAacFormatDescription(packetId, packetizedElementaryStream)
            ?: return
        if (!formatDescription.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            return
        }
        val sampleRate = formatDescription.getInteger(MediaFormat.KEY_SAMPLE_RATE).toDouble()
        val reader = ADTSReader(packetizedElementaryStream.data)
        val iterator = reader.iterator()
        var offset = 0.0
        var dataOffset = AdtsHeader.size
        val optionalHeader = packetizedElementaryStream.optionalHeader
        while (iterator.hasNext()) {
            val dataLength = iterator.next()
            val delta = (offset * 1_000_000.0).toLong()
            val blockBuffer = packetizedElementaryStream.data.copyOfRange(
                dataOffset,
                dataOffset + dataLength,
            )
            val sampleSizes = mutableListOf(dataLength)
            val sampleBuffer = makeSampleBuffer(
                packetId,
                optionalHeader.getPresentationTimeStamp() + delta,
                optionalHeader.getDecodeTimeStamp() + delta,
                formatDescription,
                blockBuffer,
                sampleSizes,
            ) ?: return
            offset += 1024.0 / sampleRate
            dataOffset += dataLength + AdtsHeader.size
            handleAudioSampleBuffer(sampleBuffer)
        }
    }

    private fun makeSampleBufferH264(
        packetId: UShort,
        packetizedElementaryStream: MpegTsPacketizedElementaryStream,
    ) {
        val nalUnits = getNalUnits(packetizedElementaryStream.data)
        val units = readH264NalUnits(
            packetizedElementaryStream.data,
            nalUnits,
            listOf(NalUnitType.pps, NalUnitType.sps, NalUnitType.idr),
        )
        val formatDescription = units.makeFormatDescription()
        if (formatDescription != null &&
            formatDescriptions[packetId].toString() != formatDescription.toString()
        ) {
            formatDescriptions[packetId] = formatDescription
            handleVideoFormatDescription(formatDescription)
        }
        val data = removeNalUnitStartCodes(packetizedElementaryStream.data, nalUnits)
        val sampleSizes = mutableListOf(data.size)
        val optionalHeader = packetizedElementaryStream.optionalHeader
        val sampleBuffer = makeSampleBuffer(
            packetId,
            optionalHeader.getPresentationTimeStamp(),
            optionalHeader.getDecodeTimeStamp(),
            formatDescriptions[packetId],
            data,
            sampleSizes,
            units.any { it.header.type == NalUnitType.idr },
        ) ?: return
        handleVideoSampleBuffer(sampleBuffer)
    }

    private fun makeSampleBufferH265(
        packetId: UShort,
        packetizedElementaryStream: MpegTsPacketizedElementaryStream,
    ) {
        val nalUnits = getNalUnits(packetizedElementaryStream.data)
        val units = readH265NalUnits(
            packetizedElementaryStream.data,
            nalUnits,
            listOf(NalUnitType.sps, NalUnitType.pps, NalUnitType.vps, NalUnitType.prefixSeiNut),
        )
        val formatDescription = units.makeFormatDescription()
        if (formatDescription != null &&
            formatDescriptions[packetId].toString() != formatDescription.toString()
        ) {
            formatDescriptions[packetId] = formatDescription
            handleVideoFormatDescription(formatDescription)
        }
        if (timecodesEnabled) {
            for (unit in units) {
                val timecode = readHevcTimecode(unit) ?: continue
                Log.d(
                    logTag,
                    "mpeg-ts-reader: Got H.265 SEI timecode ${timecode.first} " +
                        "(frame: ${timecode.second})",
                )
            }
        }
        val data = removeNalUnitStartCodes(packetizedElementaryStream.data, nalUnits)
        val sampleSizes = mutableListOf(data.size)
        val optionalHeader = packetizedElementaryStream.optionalHeader
        val sampleBuffer = makeSampleBuffer(
            packetId,
            optionalHeader.getPresentationTimeStamp(),
            optionalHeader.getDecodeTimeStamp(),
            formatDescriptions[packetId],
            data,
            sampleSizes,
            units.any { it.header.type == NalUnitType.sps },
        ) ?: return
        handleVideoSampleBuffer(sampleBuffer)
    }

    private fun readHevcTimecode(unit: NalUnit): Pair<String, Int>? =
        TODO("H.265 SEI timecode extraction port")

    private fun makeSampleBuffer(
        packetId: UShort,
        presentationTimeStamp: Long,
        decodeTimeStamp: Long,
        formatDescription: MediaFormat?,
        blockBuffer: ByteArray?,
        sampleSizes: MutableList<Int>,
        isKeyFrame: Boolean = false,
    ): MediaSample? {
        val basePresentationTimeStamp = getBasePresentationTimeStamp()
        val receivedPresentationTimeStamp = wrappingTimestamp.update(presentationTimeStamp)
        val receivedDecodeTimeStamp = wrappingTimestamp.update(decodeTimeStamp)
        val timingPresentationTimeStamp: Long
        val timingDecodeTimeStamp: Long
        val timingDuration: Long
        var firstReceivedPresentationTimeStamp = firstReceivedPresentationTimeStamp
        if (firstReceivedPresentationTimeStamp != null) {
            val base = basePresentationTimeStamp - firstReceivedPresentationTimeStamp
            timingPresentationTimeStamp = base + receivedPresentationTimeStamp
            timingDecodeTimeStamp = base + receivedDecodeTimeStamp
            val previousReceivedPresentationTimeStamp =
                previousReceivedPresentationTimeStamps[packetId]
            timingDuration = if (previousReceivedPresentationTimeStamp != null) {
                timingPresentationTimeStamp - previousReceivedPresentationTimeStamp
            } else {
                invalidPresentationTimeUs
            }
        } else {
            timingPresentationTimeStamp = basePresentationTimeStamp
            timingDecodeTimeStamp = basePresentationTimeStamp
            timingDuration = invalidPresentationTimeUs
            firstReceivedPresentationTimeStamp = receivedPresentationTimeStamp
        }
        val data = blockBuffer ?: return null
        val sampleBuffer = MediaSample(
            data,
            timingPresentationTimeStamp,
            isKeyFrame,
            formatDescription,
        )
        this.firstReceivedPresentationTimeStamp = firstReceivedPresentationTimeStamp
        previousReceivedPresentationTimeStamps[packetId] = timingPresentationTimeStamp
        return sampleBuffer
    }

    private fun getBasePresentationTimeStamp(): Long {
        if (basePresentationTimeStamp == invalidPresentationTimeUs) {
            val latency = (targetLatency * 1_000_000.0).toLong()
            basePresentationTimeStamp = currentPresentationTimeStamp() + latency
        }
        return basePresentationTimeStamp
    }

    override fun videoDecoderOutputSampleBuffer(videoDecoder: VideoDecoder, sampleBuffer: MediaSample) {
        delegate?.mpegTsReaderVideoBuffer(sampleBuffer)
    }
}
