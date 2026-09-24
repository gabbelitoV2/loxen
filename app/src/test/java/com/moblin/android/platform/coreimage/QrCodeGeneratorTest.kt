package com.moblin.android.platform.coreimage

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test

class QrCodeGeneratorTest {
    private val coreImageHelloWorld = listOf(
        "111111100101101111111",
        "100000100010001000001",
        "101110101111001011101",
        "101110101110101011101",
        "101110101010101011101",
        "100000101001001000001",
        "111111101010101111111",
        "000000001010000000000",
        "101111100101001111100",
        "011011010101111111101",
        "101011110110111001110",
        "101001000101110011100",
        "000101111100111000001",
        "000000001010100011001",
        "111111100001001000110",
        "100000101000010101111",
        "101110101001001100001",
        "101110101100111111000",
        "101110101100100100100",
        "100000100110110011100",
        "111111101101101010010",
    )

    @Test
    fun helloWorldMatchesCoreImageModuleForModule() {
        val modules = assertNotNull(qrCodeModules("hello world".toByteArray(Charsets.UTF_8), "M"))
        assertEquals(21 + 2 * qrCodeQuietZone, modules.size)
        for (y in 0 until 21) {
            for (x in 0 until 21) {
                assertEquals(
                    coreImageHelloWorld[y][x] == '1',
                    modules.isDark(x + qrCodeQuietZone, y + qrCodeQuietZone),
                    "module ($x, $y)",
                )
            }
        }
    }

    @Test
    fun quietZoneIsOneLightModule() {
        val modules = assertNotNull(qrCodeModules("https://moblin.example/a".toByteArray(Charsets.UTF_8), "M"))
        for (index in 0 until modules.size) {
            assertFalse(modules.isDark(index, 0))
            assertFalse(modules.isDark(index, modules.size - 1))
            assertFalse(modules.isDark(0, index))
            assertFalse(modules.isDark(modules.size - 1, index))
        }
        assertEquals(true, modules.isDark(1, 1))
    }

    @Test
    fun utf8BytesAreEncodedWithoutEci() {
        val message = "å".repeat(7).toByteArray(Charsets.UTF_8)
        assertEquals(14, message.size)
        assertEquals(23, assertNotNull(qrCodeModules(message, "M")).size)
    }

    @Test
    fun versionGrowsWithCorrectionLevel() {
        val message = ByteArray(15) { 'a'.code.toByte() }
        assertEquals(23, assertNotNull(qrCodeModules(message, "L")).size)
        assertEquals(27, assertNotNull(qrCodeModules(message, "M")).size)
        assertEquals(27, assertNotNull(qrCodeModules(message, "Q")).size)
        assertEquals(31, assertNotNull(qrCodeModules(message, "H")).size)
    }

    @Test
    fun invalidInputHasNoOutput() {
        assertNull(qrCodeModules("abc".toByteArray(), "X"))
        assertNull(qrCodeModules(ByteArray(3000), "H"))
    }
}
