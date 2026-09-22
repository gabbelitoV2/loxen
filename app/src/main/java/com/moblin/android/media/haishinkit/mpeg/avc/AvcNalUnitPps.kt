package com.moblin.android.media.haishinkit.mpeg.avc

import com.moblin.android.media.haishinkit.mpeg.NalUnitReader
import com.moblin.android.media.haishinkit.mpeg.NalUnitWriter
import java.io.IOException

data class AvcNalUnitPps(val data: ByteArray) {
    @Throws(IOException::class)
    constructor(reader: NalUnitReader) : this(reader.readRawBytes())

    fun encode(writer: NalUnitWriter) {
        writer.writeRawBytes(data)
    }
}
