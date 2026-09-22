package com.moblin.android.media.srtla

import com.moblin.android.media.haishinkit.util.ByteWriter
import com.moblin.android.media.srtla.common.processSrtNak
import kotlin.test.assertEquals
import org.junit.Test

private fun makeNakPacket(sns: List<UInt>): ByteArray {
    val writer = ByteWriter()
    writer.writeBytes(ByteArray(16))
    for (sn in sns) {
        writer.writeUInt32(sn)
    }
    return writer.data
}

class SrtSuite {
    @Test
    fun processNakSingleAndRange() {
        val sns = mutableListOf<UInt>()
        processSrtNak(makeNakPacket(listOf(7u, 0x8000_0000u or 10u, 12u, 20u))) { sn ->
            sns.add(sn)
        }
        assertEquals(listOf(7u, 10u, 11u, 12u, 20u), sns)
    }

    @Test
    fun processNakTooBigRangeIsIgnored() {
        val sns = mutableListOf<UInt>()
        processSrtNak(makeNakPacket(listOf(0x8000_0000u, 0x7FFF_FFFFu, 5u))) { sn ->
            sns.add(sn)
        }
        assertEquals(listOf(5u), sns)
    }

    @Test
    fun processNakReversedRangeIsIgnored() {
        val sns = mutableListOf<UInt>()
        processSrtNak(makeNakPacket(listOf(0x8000_0000u or 10u, 3u, 5u))) { sn ->
            sns.add(sn)
        }
        assertEquals(listOf(5u), sns)
    }

    @Test
    fun processNakTruncatedRange() {
        val sns = mutableListOf<UInt>()
        processSrtNak(makeNakPacket(listOf(1u, 0x8000_0000u or 10u))) { sn ->
            sns.add(sn)
        }
        assertEquals(listOf(1u), sns)
    }
}
