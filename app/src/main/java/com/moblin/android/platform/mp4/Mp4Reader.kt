package com.moblin.android.platform.mp4

import android.media.MediaFormat
import com.moblin.android.platform.video.ColorDescription
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

internal class Mp4ReaderSample(
    val offset: Long,
    val size: Int,
    val decodeTime: Long,
    val duration: Long,
    val compositionTimeOffset: Long,
    val isSync: Boolean,
)

internal class Mp4ReaderTrack(
    val trackId: Int,
    val handlerType: String,
    val timescale: Int,
    val format: MediaFormat,
    val samples: List<Mp4ReaderSample>,
    private val mediaDuration: Long,
) {
    val isVideo: Boolean
        get() = handlerType == "vide"

    val isAudio: Boolean
        get() = handlerType == "soun"

    val durationUs: Long
        get() {
            val duration = if (samples.isEmpty()) {
                mediaDuration
            } else {
                samples.last().decodeTime + samples.last().duration - samples.first().decodeTime
            }
            return timescaleToUs(duration, timescale)
        }
}

private class Box(val type: String, val start: Long, val end: Long, val headerSize: Int) {
    val payloadStart: Long
        get() = start + headerSize
}

private class TrackBuilder(val trackId: Int) {
    var handlerType = ""
    var timescale = 1
    var mediaDuration = 0L
    var format: MediaFormat? = null
    val samples = mutableListOf<Mp4ReaderSample>()
    var sampleSizes = IntArray(0)
    var chunkOffsets = LongArray(0)
    var samplesPerChunk = listOf<IntArray>()
    var timeToSample = listOf<IntArray>()
    var compositionOffsets = listOf<IntArray>()
    var syncSamples: Set<Int>? = null
    var defaultDuration = 0L
    var defaultSize = 0
    var defaultFlags = 0
    var nextFragmentDecodeTime = 0L
}

internal object Mp4Reader {
    fun read(path: String): List<Mp4ReaderTrack>? {
        val file = File(path)
        if (!file.isFile) {
            return null
        }
        return try {
            RandomAccessFile(file, "r").use { input -> Parser(input).parse() }
        } catch (error: Exception) {
            null
        }
    }

    fun readSample(path: String, sample: Mp4ReaderSample): ByteArray? {
        return try {
            RandomAccessFile(path, "r").use { input ->
                val data = ByteArray(sample.size)
                input.seek(sample.offset)
                input.readFully(data)
                data
            }
        } catch (error: Exception) {
            null
        }
    }

    private class Parser(private val input: RandomAccessFile) {
        private val tracks = LinkedHashMap<Int, TrackBuilder>()

        fun parse(): List<Mp4ReaderTrack> {
            var moofStart = 0L
            for (box in children(0, input.length())) {
                when (box.type) {
                    "moov" -> parseMovie(box)
                    "moof" -> {
                        moofStart = box.start
                        parseMovieFragment(box, moofStart)
                    }
                }
            }
            return tracks.values.mapNotNull { track ->
                val format = track.format ?: return@mapNotNull null
                if (track.samples.isEmpty()) {
                    buildSampleTable(track)
                }
                val result = Mp4ReaderTrack(
                    track.trackId,
                    track.handlerType,
                    track.timescale,
                    format,
                    track.samples.toList(),
                    track.mediaDuration,
                )
                format.setLong(MediaFormat.KEY_DURATION, result.durationUs)
                result
            }
        }

        private fun children(start: Long, end: Long): List<Box> {
            val boxes = mutableListOf<Box>()
            var position = start
            while (position + 8 <= end) {
                input.seek(position)
                var size = input.readInt().toLong() and 0xFFFF_FFFFL
                val type = readType()
                var headerSize = 8
                if (size == 1L) {
                    size = input.readLong()
                    headerSize = 16
                } else if (size == 0L) {
                    size = end - position
                }
                if (size < headerSize || position + size > end) {
                    break
                }
                boxes.add(Box(type, position, position + size, headerSize))
                position += size
            }
            return boxes
        }

        private fun readType(): String {
            val bytes = ByteArray(4)
            input.readFully(bytes)
            return String(bytes, Charsets.ISO_8859_1)
        }

        private fun payload(box: Box): ByteBuffer {
            val bytes = ByteArray((box.end - box.payloadStart).toInt())
            input.seek(box.payloadStart)
            input.readFully(bytes)
            return ByteBuffer.wrap(bytes)
        }

        private fun parseMovie(moov: Box) {
            for (box in children(moov.payloadStart, moov.end)) {
                when (box.type) {
                    "trak" -> parseTrack(box)
                    "mvex" -> for (trex in children(box.payloadStart, box.end).filter { it.type == "trex" }) {
                        val data = payload(trex)
                        data.int
                        val trackId = data.int
                        val track = tracks.getOrPut(trackId) { TrackBuilder(trackId) }
                        data.int
                        track.defaultDuration = data.int.toLong() and 0xFFFF_FFFFL
                        track.defaultSize = data.int
                        track.defaultFlags = data.int
                    }
                }
            }
        }

        private fun parseTrack(trak: Box) {
            var trackId = 0
            val boxes = children(trak.payloadStart, trak.end)
            boxes.firstOrNull { it.type == "tkhd" }?.let { tkhd ->
                val data = payload(tkhd)
                val version = data.get().toInt()
                data.position(if (version == 1) 20 else 12)
                trackId = data.int
            }
            val track = tracks.getOrPut(trackId) { TrackBuilder(trackId) }
            val mdia = boxes.firstOrNull { it.type == "mdia" } ?: return
            for (box in children(mdia.payloadStart, mdia.end)) {
                when (box.type) {
                    "mdhd" -> {
                        val data = payload(box)
                        val version = data.get().toInt()
                        if (version == 1) {
                            data.position(20)
                            track.timescale = data.int
                            track.mediaDuration = data.long
                        } else {
                            data.position(12)
                            track.timescale = data.int
                            track.mediaDuration = data.int.toLong() and 0xFFFF_FFFFL
                        }
                    }
                    "hdlr" -> {
                        val data = payload(box)
                        data.position(8)
                        val bytes = ByteArray(4)
                        data.get(bytes)
                        track.handlerType = String(bytes, Charsets.ISO_8859_1)
                    }
                    "minf" -> parseMediaInformation(box, track)
                }
            }
        }

        private fun parseMediaInformation(minf: Box, track: TrackBuilder) {
            val stbl = children(minf.payloadStart, minf.end).firstOrNull { it.type == "stbl" } ?: return
            for (box in children(stbl.payloadStart, stbl.end)) {
                val data = payload(box)
                when (box.type) {
                    "stsd" -> {
                        data.int
                        data.int
                        val entry = children(box.payloadStart + 8, box.end).firstOrNull() ?: continue
                        track.format = sampleEntryFormat(entry)
                    }
                    "stsz" -> {
                        data.int
                        val size = data.int
                        val count = data.int
                        track.sampleSizes = IntArray(count) { if (size != 0) size else data.int }
                    }
                    "stco" -> {
                        data.int
                        track.chunkOffsets = LongArray(data.int) { data.int.toLong() and 0xFFFF_FFFFL }
                    }
                    "co64" -> {
                        data.int
                        track.chunkOffsets = LongArray(data.int) { data.long }
                    }
                    "stsc" -> {
                        data.int
                        track.samplesPerChunk = List(data.int) { intArrayOf(data.int, data.int, data.int) }
                    }
                    "stts" -> {
                        data.int
                        track.timeToSample = List(data.int) { intArrayOf(data.int, data.int) }
                    }
                    "ctts" -> {
                        data.int
                        track.compositionOffsets = List(data.int) { intArrayOf(data.int, data.int) }
                    }
                    "stss" -> {
                        data.int
                        track.syncSamples = (0 until data.int).map { data.int }.toSet()
                    }
                }
            }
        }

        private fun sampleEntryFormat(entry: Box): MediaFormat? {
            val data = payload(entry)
            return when (entry.type) {
                "avc1", "avc3", "hvc1", "hev1" -> {
                    data.position(24)
                    val width = data.short.toInt() and 0xFFFF
                    val height = data.short.toInt() and 0xFFFF
                    val isHevc = entry.type.startsWith("h")
                    val format = MediaFormat.createVideoFormat(
                        if (isHevc) MediaFormat.MIMETYPE_VIDEO_HEVC else MediaFormat.MIMETYPE_VIDEO_AVC,
                        width,
                        height,
                    )
                    for (child in children(entry.payloadStart + 78, entry.end)) {
                        val bytes = ByteArray((child.end - child.payloadStart).toInt())
                        input.seek(child.payloadStart)
                        input.readFully(bytes)
                        when (child.type) {
                            "avcC" -> {
                                format.setByteBuffer("avcC", ByteBuffer.wrap(bytes))
                                avcParameterSets(bytes)?.let { (sps, pps) ->
                                    format.setByteBuffer("csd-0", ByteBuffer.wrap(sps))
                                    format.setByteBuffer("csd-1", ByteBuffer.wrap(pps))
                                }
                            }
                            "hvcC" -> {
                                format.setByteBuffer("hvcC", ByteBuffer.wrap(bytes))
                                hevcParameterSets(bytes)?.let { format.setByteBuffer("csd-0", ByteBuffer.wrap(it)) }
                            }
                            "colr" -> colorDescription(bytes)?.apply(format)
                        }
                    }
                    format
                }
                "sowt", "lpcm" -> {
                    data.position(16)
                    val channelCount = data.short.toInt() and 0xFFFF
                    val bits = data.short.toInt() and 0xFFFF
                    data.position(24)
                    val sampleRate = ((data.int.toLong() and 0xFFFF_FFFFL) ushr 16).toInt()
                    if (bits != 16) {
                        return null
                    }
                    MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_RAW, sampleRate, channelCount).also {
                        it.setInteger(MediaFormat.KEY_PCM_ENCODING, android.media.AudioFormat.ENCODING_PCM_16BIT)
                    }
                }
                "mp4a" -> {
                    data.position(16)
                    val channelCount = data.short.toInt() and 0xFFFF
                    data.position(24)
                    val sampleRate = ((data.int.toLong() and 0xFFFF_FFFFL) ushr 16).toInt()
                    val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount)
                    for (child in children(entry.payloadStart + 28, entry.end)) {
                        if (child.type == "esds") {
                            val bytes = ByteArray((child.end - child.payloadStart).toInt())
                            input.seek(child.payloadStart)
                            input.readFully(bytes)
                            audioSpecificConfig(bytes)?.let { format.setByteBuffer("csd-0", ByteBuffer.wrap(it)) }
                        }
                    }
                    format
                }
                else -> null
            }
        }

        private fun colorDescription(bytes: ByteArray): ColorDescription? {
            if (bytes.size < 10) {
                return null
            }
            val type = String(bytes, 0, 4, Charsets.ISO_8859_1)
            if (type != "nclx" && type != "nclc") {
                return null
            }
            val data = ByteBuffer.wrap(bytes, 4, bytes.size - 4)
            val primaries = data.short.toInt() and 0xFFFF
            val transfer = data.short.toInt() and 0xFFFF
            val matrix = data.short.toInt() and 0xFFFF
            val fullRange = if (type == "nclx" && data.hasRemaining()) (data.get().toInt() and 0x80) != 0 else null
            return ColorDescription.fromCodes(primaries, transfer, matrix, fullRange)
        }

        private fun avcParameterSets(record: ByteArray): Pair<ByteArray, ByteArray>? {
            if (record.size < 7) {
                return null
            }
            val data = ByteBuffer.wrap(record)
            data.position(5)
            val sps = ByteArrayOutputStream()
            repeat(data.get().toInt() and 0x1F) {
                val length = data.short.toInt() and 0xFFFF
                sps.write(byteArrayOf(0, 0, 0, 1))
                val nalUnit = ByteArray(length)
                data.get(nalUnit)
                sps.write(nalUnit)
            }
            val pps = ByteArrayOutputStream()
            repeat(data.get().toInt() and 0xFF) {
                val length = data.short.toInt() and 0xFFFF
                pps.write(byteArrayOf(0, 0, 0, 1))
                val nalUnit = ByteArray(length)
                data.get(nalUnit)
                pps.write(nalUnit)
            }
            return Pair(sps.toByteArray(), pps.toByteArray())
        }

        private fun hevcParameterSets(record: ByteArray): ByteArray? {
            if (record.size < 23) {
                return null
            }
            val data = ByteBuffer.wrap(record)
            data.position(22)
            val output = ByteArrayOutputStream()
            repeat(data.get().toInt() and 0xFF) {
                data.get()
                repeat(data.short.toInt() and 0xFFFF) {
                    val length = data.short.toInt() and 0xFFFF
                    output.write(byteArrayOf(0, 0, 0, 1))
                    val nalUnit = ByteArray(length)
                    data.get(nalUnit)
                    output.write(nalUnit)
                }
            }
            return output.toByteArray()
        }

        private fun audioSpecificConfig(esds: ByteArray): ByteArray? {
            var index = 4
            while (index < esds.size) {
                val tag = esds[index].toInt() and 0xFF
                index += 1
                var length = 0
                for (count in 0 until 4) {
                    val byte = esds[index].toInt() and 0xFF
                    index += 1
                    length = (length shl 7) or (byte and 0x7F)
                    if (byte and 0x80 == 0) {
                        break
                    }
                }
                when (tag) {
                    0x03 -> index += 3
                    0x04 -> index += 13
                    0x05 -> return esds.copyOfRange(index, minOf(esds.size, index + length))
                    else -> index += length
                }
            }
            return null
        }

        private fun parseMovieFragment(moof: Box, moofStart: Long) {
            for (traf in children(moof.payloadStart, moof.end).filter { it.type == "traf" }) {
                var track: TrackBuilder? = null
                var baseDataOffset = moofStart
                var defaultDuration = 0L
                var defaultSize = 0
                var defaultFlags = 0
                var decodeTime: Long? = null
                for (box in children(traf.payloadStart, traf.end)) {
                    val data = payload(box)
                    val flags = data.int and 0xFF_FFFF
                    when (box.type) {
                        "tfhd" -> {
                            val trackId = data.int
                            val builder = tracks.getOrPut(trackId) { TrackBuilder(trackId) }
                            track = builder
                            defaultDuration = builder.defaultDuration
                            defaultSize = builder.defaultSize
                            defaultFlags = builder.defaultFlags
                            if (flags and 0x01 != 0) {
                                baseDataOffset = data.long
                            }
                            if (flags and 0x02 != 0) {
                                data.int
                            }
                            if (flags and 0x08 != 0) {
                                defaultDuration = data.int.toLong() and 0xFFFF_FFFFL
                            }
                            if (flags and 0x10 != 0) {
                                defaultSize = data.int
                            }
                            if (flags and 0x20 != 0) {
                                defaultFlags = data.int
                            }
                        }
                        "tfdt" -> {
                            data.position(0)
                            val version = data.get().toInt()
                            data.position(4)
                            decodeTime = if (version == 1) data.long else data.int.toLong() and 0xFFFF_FFFFL
                        }
                        "trun" -> {
                            val builder = track ?: continue
                            data.position(0)
                            val version = data.get().toInt()
                            data.position(4)
                            val count = data.int
                            var offset = baseDataOffset
                            if (flags and 0x01 != 0) {
                                offset += data.int
                            }
                            var firstFlags: Int? = null
                            if (flags and 0x04 != 0) {
                                firstFlags = data.int
                            }
                            var time = decodeTime ?: builder.nextFragmentDecodeTime
                            for (index in 0 until count) {
                                val duration = if (flags and 0x100 != 0) {
                                    data.int.toLong() and 0xFFFF_FFFFL
                                } else {
                                    defaultDuration
                                }
                                val size = if (flags and 0x200 != 0) data.int else defaultSize
                                var sampleFlags = if (flags and 0x400 != 0) data.int else defaultFlags
                                if (index == 0 && firstFlags != null) {
                                    sampleFlags = firstFlags
                                }
                                val compositionTimeOffset = if (flags and 0x800 != 0) {
                                    if (version == 0) data.int.toLong() and 0xFFFF_FFFFL else data.int.toLong()
                                } else {
                                    0L
                                }
                                builder.samples.add(
                                    Mp4ReaderSample(
                                        offset,
                                        size,
                                        time,
                                        duration,
                                        compositionTimeOffset,
                                        (sampleFlags and 0x0001_0000) == 0,
                                    ),
                                )
                                offset += size
                                time += duration
                            }
                            builder.nextFragmentDecodeTime = time
                            decodeTime = time
                        }
                    }
                }
            }
        }

        private fun buildSampleTable(track: TrackBuilder) {
            val count = track.sampleSizes.size
            if (count == 0 || track.chunkOffsets.isEmpty()) {
                return
            }
            val durations = LongArray(count)
            var index = 0
            for (entry in track.timeToSample) {
                repeat(entry[0]) {
                    if (index < count) {
                        durations[index] = entry[1].toLong() and 0xFFFF_FFFFL
                        index += 1
                    }
                }
            }
            val offsets = LongArray(count)
            index = 0
            for (entry in track.compositionOffsets) {
                repeat(entry[0]) {
                    if (index < count) {
                        offsets[index] = entry[1].toLong()
                        index += 1
                    }
                }
            }
            var sample = 0
            var time = 0L
            for ((chunkIndex, chunkOffset) in track.chunkOffsets.withIndex()) {
                val chunkNumber = chunkIndex + 1
                val samplesInChunk = track.samplesPerChunk.lastOrNull { it[0] <= chunkNumber }?.get(1) ?: 1
                var offset = chunkOffset
                repeat(samplesInChunk) {
                    if (sample < count) {
                        track.samples.add(
                            Mp4ReaderSample(
                                offset,
                                track.sampleSizes[sample],
                                time,
                                durations[sample],
                                offsets[sample],
                                track.syncSamples?.contains(sample + 1) ?: true,
                            ),
                        )
                        offset += track.sampleSizes[sample]
                        time += durations[sample]
                        sample += 1
                    }
                }
            }
        }
    }
}
