package com.moblin.android.media.haishinkit.mpeg.avc

import com.moblin.android.media.haishinkit.mpeg.NalUnitReader
import com.moblin.android.media.haishinkit.mpeg.NalUnitWriter

data class AvcNalUnitSps(val data: ByteArray) {
    constructor(reader: NalUnitReader) : this(reader.readRawBytes())

    fun encode(writer: NalUnitWriter) {
        writer.writeRawBytes(data)
    }
}
