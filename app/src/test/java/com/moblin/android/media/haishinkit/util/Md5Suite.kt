package com.moblin.android.media.haishinkit.util

import org.junit.Test
import kotlin.test.assertEquals

private fun hexToByteArray(hex: String): ByteArray =
    hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

class Md5Suite {
    @Test
    fun appleLogToRec709() {
        var expected = hexToByteArray("d41d8cd98f00b204e9800998ecf8427e")
        assertEquals(expected, calculateMd5(""))
        expected = hexToByteArray("0cc175b9c0f1b6a831c399e269772661")
        assertEquals(expected, calculateMd5("a"))
        expected = hexToByteArray("900150983cd24fb0d6963f7d28e17f72")
        assertEquals(expected, calculateMd5("abc"))
        expected = hexToByteArray("f96b697d7cb7938d525a2f31aaf161d0")
        assertEquals(expected, calculateMd5("message digest"))
        expected = hexToByteArray("c3fcd3d76192e4007dfb496cca67e13b")
        assertEquals(expected, calculateMd5("abcdefghijklmnopqrstuvwxyz"))
        expected = hexToByteArray("b76972fe0dff4baac395b531646f738e")
        assertEquals(expected, calculateMd5("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz012"))
        expected = hexToByteArray("27eca74a76daae63f472b250b5bcff9d")
        assertEquals(expected, calculateMd5("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123"))
        expected = hexToByteArray("d174ab98d277d9f5a5611c2c9f419d9f")
        assertEquals(expected, calculateMd5("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"))
        expected = hexToByteArray("844581cc08fda9c8eb0b449acb7c322b")
        assertEquals(expected, calculateMd5("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789a"))
        expected = hexToByteArray("a27155ae242d64584221b66416d22a61")
        assertEquals(expected, calculateMd5("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789ab"))
        expected = hexToByteArray("57edf4a22be3c955ac49da2e2107b67a")
        assertEquals(
            expected,
            calculateMd5(
                "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
            )
        )
    }
}
