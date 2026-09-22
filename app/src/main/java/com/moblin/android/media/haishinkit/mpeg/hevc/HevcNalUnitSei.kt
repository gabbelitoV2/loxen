package com.moblin.android.media.haishinkit.mpeg.hevc

import android.util.Log
import com.moblin.android.media.haishinkit.mpeg.NalUnitReader
import com.moblin.android.media.haishinkit.mpeg.NalUnitWriter
import java.time.Instant
import java.util.Calendar
import java.util.Date

class HevcSeiPayloadTimeCode {
    var hours: UByte = 0u
        private set
    var minutes: UByte = 0u
        private set
    var seconds: UByte = 0u
        private set
    var frame: UInt = 0u
        private set

    private constructor()

    constructor(clock: Instant, frame: UInt) {
        val cal = calendar.clone() as Calendar
        cal.time = Date.from(clock)
        hours = cal.get(Calendar.HOUR_OF_DAY).toUByte()
        minutes = cal.get(Calendar.MINUTE).toUByte()
        seconds = cal.get(Calendar.SECOND).toUByte()
        this.frame = frame
    }

    fun encode(): ByteArray {
        val numClockTs: UByte = 1u
        val clockTimestampFlag = true
        val unitFieldBasedFlag = true
        val fullTimestampFlag = true
        val writer = NalUnitWriter(emulationPrevention = false)
        writer.writeBits(numClockTs, count = 2)
        writer.writeBit(clockTimestampFlag)
        writer.writeBit(unitFieldBasedFlag)
        writer.writeBits(0u, count = 5)
        writer.writeBit(fullTimestampFlag)
        writer.writeBit(false)
        writer.writeBit(false)
        writer.writeBitsU32(frame, count = 9)
        if (fullTimestampFlag) {
            writer.writeBits(seconds, count = 6)
            writer.writeBits(minutes, count = 6)
            writer.writeBits(hours, count = 5)
        }
        writer.writeBits(0u, count = 5)
        writeMoreDataInPayload(writer)
        return writer.data
    }

    fun makeClock(): Pair<Instant, UInt> {
        val clockTimestamp = seconds.toDouble() + minutes.toDouble() * 60.0 + hours.toDouble() * 3600.0
        val cal = calendar.clone() as Calendar
        cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.time.toInstant()
        return startOfDay.plusMillis((clockTimestamp * 1000.0).toLong()) to frame
    }

    companion object {
        private const val TAG = "HevcSeiPayloadTimeCode"

        fun from(reader: NalUnitReader): HevcSeiPayloadTimeCode? {
            return try {
                if (reader.readBits(2).toInt() != 1) {
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
                    val seconds = reader.readBits(6).toUByte()
                    val minutes = reader.readBits(6).toUByte()
                    val hours = reader.readBits(5).toUByte()
                    val value = HevcSeiPayloadTimeCode()
                    value.hours = hours
                    value.minutes = minutes
                    value.seconds = seconds
                    value.frame = frame
                    value
                } else {
                    Log.i(TAG, "SEI timecode: Not full timestamp")
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

enum class HevcSeiPayloadType(val rawValue: UByte) {
    timeCode(136u);

    companion object {
        fun fromRawValue(value: UByte): HevcSeiPayloadType? =
            entries.firstOrNull { it.rawValue == value }
    }
}

sealed class HevcNalUnitSeiPayload {
    data class TimeCode(val payload: HevcSeiPayloadTimeCode) : HevcNalUnitSeiPayload()
}

class HevcNalUnitSei {
    val payload: HevcNalUnitSeiPayload

    constructor(payload: HevcNalUnitSeiPayload) {
        this.payload = payload
    }

    constructor(reader: NalUnitReader) {
        val type = reader.readBits(8)
        if (type.toInt() == 0xFF) {
            throw IllegalStateException("SEI message type too long")
        }
        val length = reader.readBits(8)
        if (length.toInt() == 0xFF) {
            throw IllegalStateException("SEI message length too long")
        }
        when (HevcSeiPayloadType.fromRawValue(type.toUByte())) {
            HevcSeiPayloadType.timeCode -> {
                val timeCode = HevcSeiPayloadTimeCode.from(reader)
                    ?: throw IllegalStateException("Failed to decode time code payload")
                payload = HevcNalUnitSeiPayload.TimeCode(timeCode)
            }
            else -> throw IllegalStateException("Unsupported SEI payload type $type")
        }
    }

    fun encode(writer: NalUnitWriter) {
        val type: HevcSeiPayloadType
        val data: ByteArray
        when (val p = payload) {
            is HevcNalUnitSeiPayload.TimeCode -> {
                type = HevcSeiPayloadType.timeCode
                data = p.payload.encode()
            }
        }
        writer.writeBits(type.rawValue, count = 8)
        writer.writeBits(data.size.toUByte(), count = 8)
        writer.writeBytes(data)
        writeRbspTrailingBits(writer)
    }
}
