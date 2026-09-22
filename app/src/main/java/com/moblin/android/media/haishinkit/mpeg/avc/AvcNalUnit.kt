package com.moblin.android.media.haishinkit.mpeg.avc

import android.media.MediaFormat
import com.moblin.android.media.haishinkit.mpeg.NalUnit
import com.moblin.android.media.haishinkit.mpeg.NalUnitReader
import com.moblin.android.media.haishinkit.mpeg.NalUnitWriter
import com.moblin.android.media.haishinkit.mpeg.nalUnitStartCode
import java.nio.ByteBuffer

enum class AvcNalUnitType(val rawValue: UByte) {
    unspec(0u),
    slice(1u),
    dpa(2u),
    dpb(3u),
    dpc(4u),
    idr(5u),
    sei(6u),
    sps(7u),
    pps(8u),
    aud(9u),
    eoseq(10u),
    eostream(11u),
    fill(12u);

    companion object {
        fun fromRawValue(value: UByte): AvcNalUnitType? {
            return AvcNalUnitType.entries.firstOrNull { it.rawValue == value }
        }

        fun isPicture(type: UByte): Boolean {
            return when (fromRawValue(type)) {
                AvcNalUnitType.slice -> true
                AvcNalUnitType.idr -> true
                else -> false
            }
        }
    }
}

class AvcNalUnit(val header: AvcNalUnitHeader, val payload: AvcNalUnitPayload) : NalUnit {
    constructor(type: AvcNalUnitType, payload: AvcNalUnitPayload) : this(
        AvcNalUnitHeader(0u, type),
        payload
    )

    companion object {
        val audHeader: ByteArray = AvcNalUnitHeader(0u, AvcNalUnitType.aud).encode()
        val aud10WithStartCode: ByteArray = nalUnitStartCode + audHeader + byteArrayOf(0x10)
        val aud30WithStartCode: ByteArray = nalUnitStartCode + audHeader + byteArrayOf(0x30)

        fun create(data: ByteArray, offset: Int): AvcNalUnit? {
            return runCatching {
                val reader = NalUnitReader(data, offset)
                val header = AvcNalUnitHeader.from(reader)
                val payload = when (header.type) {
                    AvcNalUnitType.pps -> AvcNalUnitPayload.Pps(AvcNalUnitPps(reader))
                    AvcNalUnitType.sps -> AvcNalUnitPayload.Sps(AvcNalUnitSps(reader))
                    AvcNalUnitType.sei -> AvcNalUnitPayload.Sei(AvcNalUnitSei(reader))
                    else -> AvcNalUnitPayload.Unspec
                }
                AvcNalUnit(header, payload)
            }.getOrNull()
        }
    }

    override fun encode(): ByteArray {
        val writer = NalUnitWriter()
        header.encode(writer)
        payload.encode(writer)
        return writer.data
    }
}

class AvcNalUnitHeader(val refIdc: UByte, val type: AvcNalUnitType) {
    companion object {
        fun from(reader: NalUnitReader): AvcNalUnitHeader {
            reader.skipBits(1)
            val refIdc = reader.readBits(2)
            val type = AvcNalUnitType.fromRawValue(reader.readBits(5)) ?: AvcNalUnitType.unspec
            return AvcNalUnitHeader(refIdc, type)
        }
    }

    fun encode(writer: NalUnitWriter) {
        writer.writeBit(false)
        writer.writeBits(refIdc, 2)
        writer.writeBits(type.rawValue, 5)
    }

    fun encode(): ByteArray {
        val writer = NalUnitWriter()
        encode(writer)
        return writer.data
    }
}

sealed class AvcNalUnitPayload {
    class Pps(val pps: AvcNalUnitPps) : AvcNalUnitPayload()
    class Sps(val sps: AvcNalUnitSps) : AvcNalUnitPayload()
    class Sei(val sei: AvcNalUnitSei) : AvcNalUnitPayload()
    object Unspec : AvcNalUnitPayload()

    fun encode(writer: NalUnitWriter) {
        when (this) {
            is Pps -> pps.encode(writer)
            is Sps -> sps.encode(writer)
            is Sei -> sei.encode(writer)
            is Unspec -> Unit
        }
    }
}

fun List<AvcNalUnit>.makeFormatDescription(): MediaFormat? {
    val pps = firstOrNull { it.header.type == AvcNalUnitType.pps } ?: return null
    val sps = firstOrNull { it.header.type == AvcNalUnitType.sps } ?: return null
    val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 0, 0)
    format.setByteBuffer("csd-0", ByteBuffer.wrap(nalUnitStartCode + sps.encode()))
    format.setByteBuffer("csd-1", ByteBuffer.wrap(nalUnitStartCode + pps.encode()))
    return format
}
