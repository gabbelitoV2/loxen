package com.moblin.android.media.haishinkit.mpeg

import android.media.AudioFormat
import android.media.MediaFormat
import android.util.Log
import com.moblin.android.common.various.createSilent
import com.moblin.android.common.various.makeSampleBuffer
import com.moblin.android.media.MediaSample
import com.moblin.android.media.WrappingTimestamp
import com.moblin.android.media.haishinkit.codec.video.VideoDecoder
import com.moblin.android.media.haishinkit.codec.video.VideoDecoderDelegate
import com.moblin.android.media.haishinkit.mpeg.avc.AvcNalUnitType
import com.moblin.android.media.haishinkit.mpeg.avc.makeFormatDescription
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcNalUnitType
import com.moblin.android.media.haishinkit.mpeg.hevc.makeFormatDescription
import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.platform.audio.makePcmFormat
import com.moblin.android.platform.avfoundation.AVAudioConverter
import com.moblin.android.platform.avfoundation.AVAudioPCMBuffer
import com.moblin.android.various.utils.currentPresentationTimeStamp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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
    private var audioDecoder: AVAudioConverter? = null
    private var pcmAudioFormat: AudioFormat? = null
    private var videoDecoder: VideoDecoder? = null
    var delegate: MpegTsReaderDelegate? = null
    private val wrappingTimestamp = WrappingTimestamp(
        "MpegTsReader",
        ((0x2_0000_0000L * 1_000_000L) / TSTimestamp.resolution).toLong(),
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
        val pcmSampleBuffer = pcmAudioBuffer.makeSampleBuffer(
            sampleBuffer.presentationTimeUs,
            pcmAudioFormat.sampleRate,
            pcmAudioFormat.channelCount,
        ) ?: return
        delegate?.mpegTsReaderAudioBuffer(pcmSampleBuffer)
    }

    private fun createAudioDecoder(formatDescription: MediaFormat): AVAudioConverter? {
        audioDecoder?.release()
        val mime = formatDescription.getString(MediaFormat.KEY_MIME) ?: return null
        val sampleRate = formatDescription.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = formatDescription.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val audioFormat = MediaFormat.createAudioFormat(mime, sampleRate, channelCount)
        if (mime == MediaFormat.MIMETYPE_AUDIO_AAC) {
            audioFormat.setByteBuffer("csd-0", ByteBuffer.wrap(MpegTsAudioConfig(formatDescription).encode()))
        }
        val audioDecoder = AVAudioConverter.create(from = audioFormat, to = makePcmFormat(sampleRate, channelCount))
        if (audioDecoder == null) {
            Log.i(logTag, "mpeg-ts-reader: Failed to create audio decoder")
        }
        return audioDecoder
    }

    private fun decodeAudio(
        audioDecoder: AVAudioConverter,
        input: ByteArray,
        length: Int,
        output: ShortArray,
    ): Boolean {
        val pcmBuffer = AVAudioPCMBuffer(pcmFormat = audioDecoder.outputFormat, frameCapacity = 0) ?: return false
        val error = audioDecoder.convert(to = pcmBuffer) { input.copyOf(length) }
        if (error != null) {
            return false
        }
        val samples = ByteBuffer.wrap(pcmBuffer.data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val count = minOf(samples.remaining(), output.size)
        samples.get(output, 0, count)
        output.fill(0, count)
        return true
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
                createSilent(
                    pcmAudioFormat.sampleRate,
                    pcmAudioFormat.channelCount,
                    newPresentationTimeStamp,
                    samplesPerBuffer,
                )?.let {
                    delegate?.mpegTsReaderAudioBuffer(it)
                }
            }
        } finally {
            latestAudioBufferPresentationTimeStamp = presentationTimeStamp
        }
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
        videoDecoder = VideoDecoder(name, CoroutineScope(decoderQueue), softwareDecoding)
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
                addTime(optionalHeader.getPresentationTimeStamp(), delta),
                addTime(optionalHeader.getDecodeTimeStamp(), delta),
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
            listOf(AvcNalUnitType.pps, AvcNalUnitType.sps, AvcNalUnitType.idr),
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
            units.any { it.header.type == AvcNalUnitType.idr },
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
            listOf(HevcNalUnitType.sps, HevcNalUnitType.pps, HevcNalUnitType.vps, HevcNalUnitType.prefixSeiNut),
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
            units.any { it.header.type == HevcNalUnitType.sps },
        ) ?: return
        handleVideoSampleBuffer(sampleBuffer)
    }

    private fun readHevcTimecode(unit: NalUnit): Pair<String, Int>? =
        null
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
        val receivedPresentationTimeStamp = updateWrappingTimestamp(presentationTimeStamp)
        val receivedDecodeTimeStamp = updateWrappingTimestamp(decodeTimeStamp)
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

    private fun addTime(time: Long, delta: Long): Long {
        if (time == invalidPresentationTimeUs) {
            return time
        }
        return time + delta
    }

    private fun updateWrappingTimestamp(timestamp: Long): Long {
        if (timestamp == invalidPresentationTimeUs) {
            return timestamp
        }
        return wrappingTimestamp.update(timestamp)
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
