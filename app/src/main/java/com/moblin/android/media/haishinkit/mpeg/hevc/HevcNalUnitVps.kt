package com.moblin.android.media.haishinkit.mpeg.hevc

import com.moblin.android.media.haishinkit.mpeg.NalUnitReader
import com.moblin.android.media.haishinkit.mpeg.NalUnitWriter

data class HevcNalUnitVps(val data: ByteArray) {
    constructor(reader: NalUnitReader) : this(reader.readRawBytes())

    fun encode(writer: NalUnitWriter) {
        writer.writeRawBytes(data)
    }
}
