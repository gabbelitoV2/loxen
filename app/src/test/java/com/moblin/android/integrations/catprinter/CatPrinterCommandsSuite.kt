package com.moblin.android.integrations.catprinter

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun hex(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

private fun hex(value: ByteArray): String = value.joinToString("") { "%02x".format(it) }

private fun pixels(vararg values: Int): UByteArray = UByteArray(values.size) { values[it].toUByte() }

@RunWith(RobolectricTestRunner::class)
class CatPrinterCommandsSuite {
    @Test
    fun defaultCommandsArePackedWithHeaderLengthCrcAndTrailer() {
        assertEquals("5178a30001000000ff", hex(CatPrinterCommand.GetDeviceState().pack()))
        assertEquals("5178ae0001000000ff", hex(CatPrinterCommand.WritePacing(ready = true).pack()))
        assertEquals("5178a4000100358bff", hex(CatPrinterCommand.SetQuality(level = 0x35u).pack()))
        assertEquals("5178af000200007057ff", hex(CatPrinterCommand.SetEnergy(energy = 0x7000u).pack()))
        assertEquals("5178a10002003200d3ff", hex(CatPrinterCommand.FeedPaper(pixels = catPrinterFeedPaperPixels).pack()))
        assertEquals("5178be0001000000ff", hex(CatPrinterCommand.SetDrawMode(mode = CatPrinterDrawMode.image).pack()))
        assertEquals(
            "5178a6000b00aa551738445f5f5f44382ca1ff",
            hex(CatPrinterCommand.Lattice(data = CatPrinterCommand.latticeStartData).pack()),
        )
        assertEquals(
            "5178a6000b00aa5517000000000000001711ff",
            hex(CatPrinterCommand.Lattice(data = CatPrinterCommand.latticeEndData).pack()),
        )
        val row = pixels(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1)
        assertEquals(
            "5178a200020001809cff",
            hex(CatPrinterCommand.DrawRow(imageRow = row, printMode = CatPrinterPrintMode.blackAndWhite).pack()),
        )
    }

    @Test
    fun defaultNotificationsAreParsed() {
        val state = assertIs<CatPrinterCommand.GetDeviceState>(CatPrinterCommand(hex("5178a30001000b31ff")))
        assertEquals(
            CatPrinterDeviceState(noPaper = true, coverIsOpen = true, isOverheated = false, batteryIsLow = true),
            state.state,
        )
        assertEquals(CatPrinterCommand.WritePacing(ready = true), CatPrinterCommand(hex("5178ae0001000000ff")))
        assertEquals(CatPrinterCommand.WritePacing(ready = false), CatPrinterCommand(hex("5178ae0001000107ff")))
    }

    @Test
    fun malformedDefaultNotificationsAreIgnored() {
        assertNull(CatPrinterCommand(hex("5178ae0001000100ff")))
        assertNull(CatPrinterCommand(hex("5178ae0001000107fe")))
        assertNull(CatPrinterCommand(hex("5279ae0001000107ff")))
        assertNull(CatPrinterCommand(hex("5178a10002003200d3ff")))
        assertNull(CatPrinterCommand(hex("5178a300000000ff")))
        assertNull(CatPrinterCommand(hex("5178ae")))
    }

    @Test
    fun imageRowsAreEncodedLeastSignificantBitFirst() {
        assertEquals(
            "0180",
            hex(catPrinterEncodeImageRow(pixels(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1), CatPrinterPrintMode.blackAndWhite)),
        )
        assertEquals("ff00", hex(catPrinterEncodeImageRow(pixels(1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0), CatPrinterPrintMode.blackAndWhite)))
        assertEquals("f123", hex(catPrinterEncodeImageRow(pixels(15, 1, 2, 3), CatPrinterPrintMode.grayscale)))
        assertEquals(catPrinterWidthPixels / 8, catPrinterEncodeImageRow(UByteArray(catPrinterWidthPixels), CatPrinterPrintMode.blackAndWhite).size)
    }

    @Test
    fun defaultPrintJobIsFramedByQualityLatticeEnergyAndDrawMode() {
        val row = UByteArray(catPrinterWidthPixels) { if (it % 3 == 0) 1u else 0u }
        val withFeed = catPrinterPackPrintImageCommands(
            image = listOf(row, row),
            feedPaper = true,
            printMode = CatPrinterPrintMode.blackAndWhite,
        )
        val drawRow = CatPrinterCommand.DrawRow(imageRow = row, printMode = CatPrinterPrintMode.blackAndWhite).pack()
        val header = hex("5178a4000100358bff") +
            hex("5178a6000b00aa551738445f5f5f44382ca1ff") +
            hex("5178af000200007057ff") +
            hex("5178be0001000000ff")
        val footer = hex("5178a6000b00aa5517000000000000001711ff")
        assertContentEquals(header + drawRow + drawRow + hex("5178a10002003200d3ff") + footer, withFeed)
        val withoutFeed = catPrinterPackPrintImageCommands(
            image = listOf(row),
            feedPaper = false,
            printMode = CatPrinterPrintMode.blackAndWhite,
        )
        assertContentEquals(header + drawRow + footer, withoutFeed)
        assertEquals(8 + catPrinterWidthPixels / 8, drawRow.size)
    }

    @Test
    fun mxw01CommandsArePacked() {
        assertEquals("2221a10001000000ff", hex(CatPrinterCommandMxw01.StatusRequest.pack()))
        assertEquals("2221b10001000000ff", hex(CatPrinterCommandMxw01.GetVersionRequest.pack()))
        assertEquals(
            "2221a90004005a00300099ff",
            hex(CatPrinterCommandMxw01.PrintRequest(printMode = CatPrinterPrintMode.blackAndWhite, count = 90u).pack()),
        )
        assertEquals(
            "2221a90004005a00300297ff",
            hex(CatPrinterCommandMxw01.PrintRequest(printMode = CatPrinterPrintMode.grayscale, count = 90u).pack()),
        )
        assertEquals(0, CatPrinterCommandMxw01.PrintResponse(status = 0u).pack().size)
    }

    @Test
    fun mxw01NotificationsAreParsed() {
        assertEquals(
            CatPrinterCommandMxw01.StatusResponse(ok = true, tooHot = true, hasPaper = true),
            CatPrinterCommandMxw01(hex("2221a1000800010203040506000471ff")),
        )
        assertEquals(
            CatPrinterCommandMxw01.StatusResponse(ok = false, tooHot = false, hasPaper = false),
            CatPrinterCommandMxw01(hex("2221a100080001020304050601017fff")),
        )
        assertNull(CatPrinterCommandMxw01(hex("2221a10001000000ff")))
        assertEquals(CatPrinterCommandMxw01.PrintResponse(status = 0u), CatPrinterCommandMxw01(hex("2221a90001000000ff")))
        assertEquals(CatPrinterCommandMxw01.PrintResponse(status = 1u), CatPrinterCommandMxw01(hex("2221a90001000107ff")))
        assertNull(CatPrinterCommandMxw01(hex("2221a90000000000ff")))
        val complete = assertIs<CatPrinterCommandMxw01.PrintCompleteIndication>(
            CatPrinterCommandMxw01(hex("2221aa000200070853ff")),
        )
        assertEquals("0708", hex(complete.value))
        assertEquals(CatPrinterCommandMxw01.GetVersionResponse(value = "1.2.3"), CatPrinterCommandMxw01(hex("2221b1000500312e322e33e8ff")))
        assertEquals(CatPrinterCommandMxw01.GetVersionResponse(value = "unknown"), CatPrinterCommandMxw01(hex("2221b1000200fffe23ff")))
        assertNull(CatPrinterCommandMxw01(hex("2222a90001000000ff")))
        assertNull(CatPrinterCommandMxw01(hex("2221a50001000000ff")))
        assertNull(CatPrinterCommandMxw01(hex("2221a9000400")))
    }

    @Test
    fun mxw01PrintDataIsPaddedToNinetyLines() {
        val row = UByteArray(catPrinterWidthPixels) { if (it < 8) 1u else 0u }
        val data = catPrinterPackPrintImageCommandsMxw01(image = listOf(row), printMode = CatPrinterPrintMode.blackAndWhite)
        assertEquals(90 * catPrinterWidthPixels / 8, data.size)
        assertEquals(0xFF.toByte(), data[0])
        assertEquals(0, data.drop(1).count { it != 0.toByte() })
        val tall = List(100) { row }
        assertEquals(100 * catPrinterWidthPixels / 8, catPrinterPackPrintImageCommandsMxw01(tall, CatPrinterPrintMode.blackAndWhite).size)
        val gray = catPrinterPackPrintImageCommandsMxw01(listOf(UByteArray(catPrinterWidthPixels) { 15u }), CatPrinterPrintMode.grayscale)
        assertEquals(90 * catPrinterWidthPixels / 8, gray.size)
        assertEquals(catPrinterWidthPixels / 2, gray.count { it == 0xFF.toByte() })
    }
}
