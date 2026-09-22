package com.moblin.android.media.haishinkit.mpeg.hevc

import android.media.MediaFormat
import com.moblin.android.media.haishinkit.mpeg.NalUnit
import com.moblin.android.media.haishinkit.mpeg.NalUnitReader
import com.moblin.android.media.haishinkit.mpeg.NalUnitWriter
import java.util.Calendar
import java.util.TimeZone

enum class HevcNalUnitType(val rawValue: UByte) {
    codedSliceTrailN(0u),
    codedSliceTrailR(1u),
    codedSliceTsaN(2u),
    codedSliceTsaR(3u),
    codedSliceStsaN(4u),
    codedSliceStsaR(5u),
    codedSliceRadlN(6u),
    codedSliceRadlR(7u),
    codedSliceRaslN(8u),
    codedSliceRsslR(9u),
    vps(32u),
    sps(33u),
    pps(34u),
    accessUnitDelimiter(35u),
    prefixSeiNut(39u),
    unspec(0xFFu);

    companion object {
        fun isPicture(type: UByte): Boolean {
            return type <= 31u
        }

        fun fromRawValue(rawValue: UByte): HevcNalUnitType? {
            return entries.firstOrNull { it.rawValue == rawValue }
        }
    }
}

data class HevcNalUnit(
    val header: HevcNalUnitHeader,
    val payload: HevcNalUnitPayload,
) : NalUnit {
    constructor(
        type: HevcNalUnitType,
        temporalIdPlusOne: UByte,
        payload: HevcNalUnitPayload,
    ) : this(HevcNalUnitHeader(type, 0u, temporalIdPlusOne), payload)

    fun encode(): ByteArray {
        val writer = NalUnitWriter()
        header.encode(writer)
        payload.encode(writer)
        return writer.data
    }

    companion object {
        operator fun invoke(data: ByteArray, offset: Int): HevcNalUnit? {
            val reader = NalUnitReader(data, offset)
            return try {
                val header = HevcNalUnitHeader(reader)
                val payload: HevcNalUnitPayload = when (header.type) {
                    HevcNalUnitType.vps -> HevcNalUnitPayload.vps(HevcNalUnitVps(reader))
                    HevcNalUnitType.sps -> HevcNalUnitPayload.sps(HevcNalUnitSps(reader))
                    HevcNalUnitType.pps -> HevcNalUnitPayload.pps(HevcNalUnitPps(reader))
                    HevcNalUnitType.prefixSeiNut -> HevcNalUnitPayload.prefixSeiNut(HevcNalUnitSei(reader))
                    else -> HevcNalUnitPayload.unspec
                }
                HevcNalUnit(header, payload)
            } catch (error: Exception) {
                null
            }
        }
    }
}

data class HevcNalUnitHeader(
    val type: HevcNalUnitType,
    val nuhLayerId: UByte,
    val temporalIdPlusOne: UByte,
) {
    companion object {
        operator fun invoke(reader: NalUnitReader): HevcNalUnitHeader {
            reader.skipBits(1)
            val type = HevcNalUnitType.fromRawValue(reader.readBits(6)) ?: HevcNalUnitType.unspec
            val nuhLayerId = reader.readBits(6)
            val temporalIdPlusOne = reader.readBits(3)
            return HevcNalUnitHeader(type, nuhLayerId, temporalIdPlusOne)
        }
    }

    fun encode(writer: NalUnitWriter) {
        writer.writeBit(false)
        writer.writeBits(type.rawValue, 6)
        writer.writeBits(nuhLayerId, 6)
        writer.writeBits(temporalIdPlusOne, 3)
    }
}

sealed class HevcNalUnitPayload {
    class vps(val payload: HevcNalUnitVps) : HevcNalUnitPayload()
    class sps(val payload: HevcNalUnitSps) : HevcNalUnitPayload()
    class pps(val payload: HevcNalUnitPps) : HevcNalUnitPayload()
    class prefixSeiNut(val payload: HevcNalUnitSei) : HevcNalUnitPayload()
    object unspec : HevcNalUnitPayload()

    fun encode(writer: NalUnitWriter) {
        when (this) {
            is vps -> payload.encode(writer)
            is sps -> payload.encode(writer)
            is pps -> payload.encode(writer)
            is prefixSeiNut -> payload.encode(writer)
            is unspec -> Unit
        }
    }
}

fun List<HevcNalUnit>.makeFormatDescription(): MediaFormat? {
    val vps = firstOrNull { it.header.type == HevcNalUnitType.vps } ?: return null
    val sps = firstOrNull { it.header.type == HevcNalUnitType.sps } ?: return null
    val pps = firstOrNull { it.header.type == HevcNalUnitType.pps } ?: return null
    val vpsData = vps.encode()
    val spsData = sps.encode()
    val ppsData = pps.encode()
    TODO("no Android counterpart for CMVideoFormatDescriptionCreateFromHEVCParameterSets: build a MediaFormat for MIMETYPE_VIDEO_HEVC with csd-0/csd-1 from vpsData, spsData and ppsData")
}

val calendar: Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

fun writeRbspTrailingBits(writer: NalUnitWriter) {
    writer.writeBit(true)
    while (writer.bitOffset != 0) {
        writer.writeBit(false)
    }
}

fun writeMoreDataInPayload(writer: NalUnitWriter) {
    var padding = true
    while (writer.bitOffset != 0) {
        writer.writeBit(padding)
        padding = false
    }
}
