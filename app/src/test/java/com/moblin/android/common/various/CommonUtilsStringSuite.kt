package com.moblin.android.common.various

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import org.junit.Test

class CommonUtilsStringSuite {
    @Test
    fun trimRemovesWhitespacesAndNewlinesLikeSwift() {
        assertEquals("a b", " \t\n a b \r\n\u00A0\u2028".trim())
        assertEquals("", " \n\t ".trim())
        assertEquals("rtmp://example.com/live", "rtmp://example.com/live".trim())
    }

    @Test
    fun substringTakesTheCharactersBetweenTheOffsets() {
        assertEquals("bc", "abcd".substring(1, 3))
        assertEquals("", "abcd".substring(2, 2))
        assertContentEquals(byteArrayOf(0x0A, 0xFF.toByte(), 0x00), hexStringToByteArray("0aff00"))
    }
}
