package com.moblin.android.platform.mp4

import android.media.MediaFormat
import com.moblin.android.platform.video.ColorDescription

internal const val mp4MovieTimescale = 1000
internal const val mp4VideoTimescale = 90000
internal const val mp4SampleFlagsSync = 0x02000000
internal const val mp4SampleFlagsNonSync = 0x01010000
private const val secondsFrom1904To1970 = 2_082_844_800L

private val aacSampleRates = intArrayOf(
    96000,
    88200,
    64000,
    48000,
    44100,
    32000,
    24000,
    22050,
    16000,
    12000,
    11025,
    8000,
    7350,
)

internal class Mp4Writer(initialCapacity: Int = 1024) {
    private var buffer = ByteArray(maxOf(initialCapacity, 16))

    var size = 0
        private set

    fun u8(value: Int) {
        ensure(1)
        buffer[size] = value.toByte()
        size += 1
    }

    fun u16(value: Int) {
        ensure(2)
        buffer[size] = (value ushr 8).toByte()
        buffer[size + 1] = value.toByte()
        size += 2
    }

    fun u24(value: Int) {
        ensure(3)
        buffer[size] = (value ushr 16).toByte()
        buffer[size + 1] = (value ushr 8).toByte()
        buffer[size + 2] = value.toByte()
        size += 3
    }

    fun u32(value: Long) {
        ensure(4)
        writeU32At(size, value)
        size += 4
    }

    fun u32(value: Int) {
        u32(value.toLong() and 0xFFFF_FFFFL)
    }

    fun u64(value: Long) {
        u32(value ushr 32)
        u32(value and 0xFFFF_FFFFL)
    }

    fun bytes(data: ByteArray) {
        bytes(data, 0, data.size)
    }

    fun bytes(data: ByteArray, offset: Int, length: Int) {
        ensure(length)
        System.arraycopy(data, offset, buffer, size, length)
        size += length
    }

    fun zeros(count: Int) {
        ensure(count)
        buffer.fill(0, size, size + count)
        size += count
    }

    fun fourcc(type: String) {
        for (index in 0 until 4) {
            u8(if (index < type.length) type[index].code else 0x20)
        }
    }

    fun beginBox(type: String): Int {
        val start = size
        u32(0)
        fourcc(type)
        return start
    }

    fun beginFullBox(type: String, version: Int, flags: Int): Int {
        val start = beginBox(type)
        u8(version)
        u24(flags)
        return start
    }

    fun endBox(start: Int) {
        writeU32At(start, (size - start).toLong())
    }

    fun beginDescriptor(tag: Int): Int {
        u8(tag)
        val start = size
        zeros(4)
        return start
    }

    fun endDescriptor(start: Int) {
        val length = size - start - 4
        buffer[start] = (0x80 or ((length ushr 21) and 0x7F)).toByte()
        buffer[start + 1] = (0x80 or ((length ushr 14) and 0x7F)).toByte()
        buffer[start + 2] = (0x80 or ((length ushr 7) and 0x7F)).toByte()
        buffer[start + 3] = (length and 0x7F).toByte()
    }

    fun patchU32(position: Int, value: Long) {
        writeU32At(position, value)
    }

    fun toByteArray(): ByteArray {
        return buffer.copyOf(size)
    }

    private fun writeU32At(position: Int, value: Long) {
        buffer[position] = (value ushr 24).toByte()
        buffer[position + 1] = (value ushr 16).toByte()
        buffer[position + 2] = (value ushr 8).toByte()
        buffer[position + 3] = value.toByte()
    }

    private fun ensure(count: Int) {
        val required = size + count
        if (required <= buffer.size) {
            return
        }
        var capacity = buffer.size * 2
        while (capacity < required) {
            capacity *= 2
        }
        buffer = buffer.copyOf(capacity)
    }
}

internal inline fun Mp4Writer.box(type: String, body: Mp4Writer.() -> Unit) {
    val start = beginBox(type)
    body()
    endBox(start)
}

internal inline fun Mp4Writer.fullBox(type: String, version: Int, flags: Int, body: Mp4Writer.() -> Unit) {
    val start = beginFullBox(type, version, flags)
    body()
    endBox(start)
}

internal inline fun Mp4Writer.descriptor(tag: Int, body: Mp4Writer.() -> Unit) {
    val start = beginDescriptor(tag)
    body()
    endDescriptor(start)
}

class Mp4VideoTrackConfig(
    val trackId: Int,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val decoderConfigurationRecord: ByteArray,
    val color: ColorDescription? = null,
) {
    val isHevc: Boolean
        get() = mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC
}

class Mp4AudioTrackConfig(
    val trackId: Int,
    val sampleRate: Int,
    val channelCount: Int,
    val bitrate: Int,
    val audioSpecificConfig: ByteArray,
    val isPcm: Boolean = false,
)

internal class Mp4FragmentSample(
    val data: ByteArray,
    val duration: Long,
    val compositionTimeOffset: Long,
    val isSync: Boolean,
)

internal class Mp4TrackFragment(
    val trackId: Int,
    val baseMediaDecodeTime: Long,
    val samples: List<Mp4FragmentSample>,
    val writeSampleFlags: Boolean,
)

fun makeAacAudioSpecificConfig(sampleRate: Int, channelCount: Int): ByteArray {
    val frequencyIndex = aacSampleRates.indexOf(sampleRate)
    val channelConfiguration = channelCount.coerceIn(1, 7)
    if (frequencyIndex >= 0) {
        val value = (2 shl 11) or (frequencyIndex shl 7) or (channelConfiguration shl 3)
        return byteArrayOf((value ushr 8).toByte(), value.toByte())
    }
    val bits = (2L shl 35) or (0xFL shl 31) or ((sampleRate.toLong() and 0xFF_FFFFL) shl 7) or
        (channelConfiguration.toLong() shl 3)
    return byteArrayOf(
        (bits ushr 32).toByte(),
        (bits ushr 24).toByte(),
        (bits ushr 16).toByte(),
        (bits ushr 8).toByte(),
        bits.toByte(),
    )
}

internal fun makeMp4InitializationSegment(
    video: Mp4VideoTrackConfig?,
    audio: Mp4AudioTrackConfig?,
    creationTimeSeconds: Long,
): ByteArray {
    val writer = Mp4Writer(2048)
    val time = creationTimeSeconds + secondsFrom1904To1970
    writer.box("ftyp") {
        fourcc("iso6")
        u32(0)
        fourcc("iso6")
        fourcc("isom")
        fourcc("mp41")
        fourcc("dash")
    }
    writer.box("moov") {
        fullBox("mvhd", 0, 0) {
            u32(time)
            u32(time)
            u32(mp4MovieTimescale)
            u32(0)
            u32(0x00010000)
            u16(0x0100)
            zeros(10)
            writeUnityMatrix()
            zeros(24)
            u32(maxOf(video?.trackId ?: 0, audio?.trackId ?: 0) + 1)
        }
        if (video != null) {
            writeVideoTrack(video, time)
        }
        if (audio != null) {
            writeAudioTrack(audio, time)
        }
        box("mvex") {
            for (trackId in listOfNotNull(video?.trackId, audio?.trackId)) {
                fullBox("trex", 0, 0) {
                    u32(trackId)
                    u32(1)
                    u32(0)
                    u32(0)
                    u32(0)
                }
            }
        }
    }
    return writer.toByteArray()
}

internal fun makeMp4MediaSegment(sequenceNumber: Int, fragments: List<Mp4TrackFragment>): ByteArray {
    val tracks = fragments.filter { it.samples.isNotEmpty() }
    var dataSize = 0L
    for (track in tracks) {
        for (sample in track.samples) {
            dataSize += sample.data.size
        }
    }
    val writer = Mp4Writer((dataSize + 512 + tracks.sumOf { it.samples.size } * 16L).toInt())
    val dataOffsetPositions = IntArray(tracks.size)
    val moofStart = writer.beginBox("moof")
    writer.fullBox("mfhd", 0, 0) {
        u32(sequenceNumber)
    }
    for ((index, track) in tracks.withIndex()) {
        val hasCompositionTimeOffsets = track.samples.any { it.compositionTimeOffset != 0L }
        var trunFlags = 0x000001 or 0x000100 or 0x000200
        if (track.writeSampleFlags) {
            trunFlags = trunFlags or 0x000400
        }
        if (hasCompositionTimeOffsets) {
            trunFlags = trunFlags or 0x000800
        }
        writer.box("traf") {
            fullBox("tfhd", 0, 0x020000) {
                u32(track.trackId)
            }
            fullBox("tfdt", 1, 0) {
                u64(track.baseMediaDecodeTime)
            }
            fullBox("trun", if (hasCompositionTimeOffsets) 1 else 0, trunFlags) {
                u32(track.samples.size)
                dataOffsetPositions[index] = size
                u32(0)
                for (sample in track.samples) {
                    u32(sample.duration.coerceIn(0L, 0xFFFF_FFFFL))
                    u32(sample.data.size)
                    if (track.writeSampleFlags) {
                        u32(if (sample.isSync) mp4SampleFlagsSync else mp4SampleFlagsNonSync)
                    }
                    if (hasCompositionTimeOffsets) {
                        u32(sample.compositionTimeOffset.toInt())
                    }
                }
            }
        }
    }
    writer.endBox(moofStart)
    val moofSize = writer.size - moofStart
    writer.u32(8 + dataSize)
    writer.fourcc("mdat")
    var dataOffset = moofSize.toLong() + 8
    for ((index, track) in tracks.withIndex()) {
        writer.patchU32(dataOffsetPositions[index], dataOffset)
        for (sample in track.samples) {
            dataOffset += sample.data.size
        }
    }
    for (track in tracks) {
        for (sample in track.samples) {
            writer.bytes(sample.data)
        }
    }
    return writer.toByteArray()
}

private fun Mp4Writer.writeUnityMatrix() {
    u32(0x00010000)
    u32(0)
    u32(0)
    u32(0)
    u32(0x00010000)
    u32(0)
    u32(0)
    u32(0)
    u32(0x40000000)
}

private fun Mp4Writer.writeTrackHeader(trackId: Int, time: Long, volume: Int, width: Int, height: Int) {
    fullBox("tkhd", 0, 0x000003) {
        u32(time)
        u32(time)
        u32(trackId)
        u32(0)
        u32(0)
        zeros(8)
        u16(0)
        u16(0)
        u16(volume)
        u16(0)
        writeUnityMatrix()
        u32((width.toLong() and 0xFFFF) shl 16)
        u32((height.toLong() and 0xFFFF) shl 16)
    }
}

private fun Mp4Writer.writeMediaHeader(time: Long, timescale: Int) {
    fullBox("mdhd", 0, 0) {
        u32(time)
        u32(time)
        u32(timescale)
        u32(0)
        u16(0x55C4)
        u16(0)
    }
}

private fun Mp4Writer.writeHandler(handlerType: String, name: String) {
    fullBox("hdlr", 0, 0) {
        u32(0)
        fourcc(handlerType)
        zeros(12)
        bytes(name.toByteArray(Charsets.US_ASCII))
        u8(0)
    }
}

private fun Mp4Writer.writeDataInformation() {
    box("dinf") {
        fullBox("dref", 0, 0) {
            u32(1)
            fullBox("url ", 0, 1) {}
        }
    }
}

private fun Mp4Writer.writeEmptySampleTables() {
    fullBox("stts", 0, 0) {
        u32(0)
    }
    fullBox("stsc", 0, 0) {
        u32(0)
    }
    fullBox("stsz", 0, 0) {
        u32(0)
        u32(0)
    }
    fullBox("stco", 0, 0) {
        u32(0)
    }
}

private fun Mp4Writer.writeVideoTrack(video: Mp4VideoTrackConfig, time: Long) {
    box("trak") {
        writeTrackHeader(video.trackId, time, 0, video.width, video.height)
        box("mdia") {
            writeMediaHeader(time, mp4VideoTimescale)
            writeHandler("vide", "VideoHandler")
            box("minf") {
                fullBox("vmhd", 0, 1) {
                    zeros(8)
                }
                writeDataInformation()
                box("stbl") {
                    fullBox("stsd", 0, 0) {
                        u32(1)
                        writeVisualSampleEntry(video)
                    }
                    writeEmptySampleTables()
                }
            }
        }
    }
}

private fun Mp4Writer.writeVisualSampleEntry(video: Mp4VideoTrackConfig) {
    box(if (video.isHevc) "hvc1" else "avc1") {
        zeros(6)
        u16(1)
        u16(0)
        u16(0)
        zeros(12)
        u16(video.width)
        u16(video.height)
        u32(0x00480000)
        u32(0x00480000)
        u32(0)
        u16(1)
        val name = (if (video.isHevc) "HEVC Coding" else "AVC Coding").toByteArray(Charsets.US_ASCII)
        u8(name.size)
        bytes(name)
        zeros(31 - name.size)
        u16(0x0018)
        u16(0xFFFF)
        box(if (video.isHevc) "hvcC" else "avcC") {
            bytes(video.decoderConfigurationRecord)
        }
        val color = video.color
        if (color != null && !color.isEmpty) {
            box("colr") {
                fourcc("nclx")
                u16(color.primariesCode())
                u16(color.transferCode())
                u16(color.matrixCode())
                u8(if (color.fullRange == true) 0x80 else 0)
            }
        }
        box("pasp") {
            u32(1)
            u32(1)
        }
    }
}

private fun Mp4Writer.writeAudioTrack(audio: Mp4AudioTrackConfig, time: Long) {
    box("trak") {
        writeTrackHeader(audio.trackId, time, 0x0100, 0, 0)
        box("mdia") {
            writeMediaHeader(time, audio.sampleRate)
            writeHandler("soun", "SoundHandler")
            box("minf") {
                fullBox("smhd", 0, 0) {
                    u16(0)
                    u16(0)
                }
                writeDataInformation()
                box("stbl") {
                    fullBox("stsd", 0, 0) {
                        u32(1)
                        writeAudioSampleEntry(audio)
                    }
                    writeEmptySampleTables()
                }
            }
        }
    }
}

private fun Mp4Writer.writeAudioSampleEntry(audio: Mp4AudioTrackConfig) {
    if (audio.isPcm) {
        box("sowt") {
            zeros(6)
            u16(1)
            zeros(8)
            u16(audio.channelCount)
            u16(16)
            u16(0)
            u16(0)
            u32(if (audio.sampleRate in 1..0xFFFF) audio.sampleRate.toLong() shl 16 else 0L)
        }
        return
    }
    box("mp4a") {
        zeros(6)
        u16(1)
        zeros(8)
        u16(audio.channelCount)
        u16(16)
        u16(0)
        u16(0)
        u32(if (audio.sampleRate in 1..0xFFFF) audio.sampleRate.toLong() shl 16 else 0L)
        fullBox("esds", 0, 0) {
            descriptor(0x03) {
                u16(audio.trackId)
                u8(0)
                descriptor(0x04) {
                    u8(0x40)
                    u8(0x15)
                    u24(0)
                    u32(audio.bitrate)
                    u32(audio.bitrate)
                    descriptor(0x05) {
                        bytes(audio.audioSpecificConfig)
                    }
                }
                descriptor(0x06) {
                    u8(0x02)
                }
            }
        }
    }
}
