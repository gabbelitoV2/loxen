package com.moblin.android.media.haishinkit.mpeg.avc

import com.moblin.android.platform.log.Log
import com.moblin.android.media.haishinkit.mpeg.NalUnitReader
import com.moblin.android.media.haishinkit.mpeg.NalUnitWriter
import com.moblin.android.media.haishinkit.mpeg.hevc.writeMoreDataInPayload
import com.moblin.android.media.haishinkit.mpeg.hevc.writeRbspTrailingBits
import java.time.Instant
import java.time.ZoneOffset

class AvcSeiPayloadPictureTiming(
    hours: UByte,
    minutes: UByte,
    seconds: UByte,
    frame: UInt,
) {
    var hours: UByte = hours
        private set
    var minutes: UByte = minutes
        private set
    var seconds: UByte = seconds
        private set
    var frame: UInt = frame
        private set

    constructor(clock: Instant, frame: UInt) : this(
        clock.atZone(ZoneOffset.UTC).hour.toUByte(),
        clock.atZone(ZoneOffset.UTC).minute.toUByte(),
        clock.atZone(ZoneOffset.UTC).second.toUByte(),
        frame,
    )

    fun encode(): ByteArray {
        val numClockTs: UByte = 1.toUByte()
        val clockTimestampFlag = true
        val unitFieldBasedFlag = true
        val fullTimestampFlag = true
        val writer = NalUnitWriter(false)
        writer.writeBits(numClockTs, 2)
        writer.writeBit(clockTimestampFlag)
        writer.writeBit(unitFieldBasedFlag)
        writer.writeBits(0.toUByte(), 5)
        writer.writeBit(fullTimestampFlag)
        writer.writeBit(false)
        writer.writeBit(false)
        writer.writeBitsU32(frame, 9)
        if (fullTimestampFlag) {
            writer.writeBits(seconds, 6)
            writer.writeBits(minutes, 6)
            writer.writeBits(hours, 5)
        }
        writer.writeBits(0.toUByte(), 5)
        writeMoreDataInPayload(writer)
        return writer.data
    }

    companion object {
        private const val TAG = "AvcSeiPayloadPictureTiming"

        fun fromReader(reader: NalUnitReader): AvcSeiPayloadPictureTiming? {
            try {
                if (reader.readBits(2) != 1.toUByte()) {
                    Log.i(TAG, "SEI timecode: Not exactly one entry")
                    return null
                }
                if (!reader.readBit()) {
                    Log.i(TAG, "SEI timecode: clockTimestampFlag not set")
                    return null
                }
                reader.skipBits(1 + 5)
                val fullTimestampFlag = reader.readBit()
                reader.skipBits(1 + 1)
                val frame = reader.readBitsU32(9)
                if (fullTimestampFlag) {
                    val seconds = reader.readBits(6)
                    val minutes = reader.readBits(6)
                    val hours = reader.readBits(5)
                    return AvcSeiPayloadPictureTiming(hours, minutes, seconds, frame)
                }
                Log.i(TAG, "SEI timecode: Not full timestamp")
                return null
            } catch (e: Exception) {
                return null
            }
        }
    }
}

enum class AvcSeiPayloadType(val rawValue: UByte) {
    PICTURE_TIMING(1.toUByte());

    companion object {
        fun fromRawValue(value: UByte): AvcSeiPayloadType? =
            AvcSeiPayloadType.entries.firstOrNull { it.rawValue == value }
    }
}

sealed class AvcNalUnitSeiPayload {
    data class PictureTiming(val value: AvcSeiPayloadPictureTiming) : AvcNalUnitSeiPayload()
}

class AvcNalUnitSei(payload: AvcNalUnitSeiPayload) {
    var payload: AvcNalUnitSeiPayload = payload
        private set

    constructor(reader: NalUnitReader) : this(readPayload(reader))

    fun encode(writer: NalUnitWriter) {
        when (val current = payload) {
            is AvcNalUnitSeiPayload.PictureTiming -> {
                val type = AvcSeiPayloadType.PICTURE_TIMING
                val data = current.value.encode()
                writer.writeBits(type.rawValue, 8)
                writer.writeBits(data.size.toUByte(), 8)
                writer.writeBytes(data)
            }
        }
        writeRbspTrailingBits(writer)
    }

    companion object {
        private fun readPayload(reader: NalUnitReader): AvcNalUnitSeiPayload {
            val type = reader.readBits(8)
            if (type == 0xFF.toUByte()) {
                throw IllegalStateException("SEI message type too long")
            }
            val length = reader.readBits(8)
            if (length == 0xFF.toUByte()) {
                throw IllegalStateException("SEI message length too long")
            }
            val payloadType = AvcSeiPayloadType.fromRawValue(type)
                ?: throw IllegalStateException("Unsupported SEI payload type $type")
            when (payloadType) {
                AvcSeiPayloadType.PICTURE_TIMING -> {
                    val pictureTiming = AvcSeiPayloadPictureTiming.fromReader(reader)
                        ?: throw IllegalStateException("Failed to decode picture timing payload")
                    return AvcNalUnitSeiPayload.PictureTiming(pictureTiming)
                }
            }
        }
    }
}
