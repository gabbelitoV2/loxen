package com.moblin.android.media.haishinkit.mpeg.hevc

import com.moblin.android.media.haishinkit.mpeg.NalUnitReader
import com.moblin.android.media.haishinkit.mpeg.NalUnitWriter

data class HevcNalUnitSps(val data: ByteArray) {
    constructor(reader: NalUnitReader) : this(reader.readRawBytes())

    fun encode(writer: NalUnitWriter) {
        writer.writeRawBytes(data)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is HevcNalUnitSps) {
            return false
        }
        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}
