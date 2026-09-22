package com.moblin.android.integrations.catprinter

import android.util.Log
import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.media.haishinkit.util.ByteWriter
import java.io.ByteArrayOutputStream

val catPrinterFeedPaperPixels: UShort = 50.toUShort()

private enum class CatPrinterPrintCommandId(val rawValue: UByte) {
    FeedPaper(0xA1u.toUByte()),
    DrawRow(0xA2u.toUByte()),
    GetDeviceState(0xA3u.toUByte()),
    SetQuality(0xA4u.toUByte()),
    Lattice(0xA6u.toUByte()),
    WritePacing(0xAEu.toUByte()),
    SetEnergy(0xAFu.toUByte()),
    SetDrawMode(0xBEu.toUByte());

    companion object {
        fun fromRawValue(value: Int): CatPrinterPrintCommandId? =
            entries.firstOrNull { it.rawValue.toInt() == value }
    }
}

data class CatPrinterDeviceState(
    val noPaper: Boolean,
    val coverIsOpen: Boolean,
    val isOverheated: Boolean,
    val batteryIsLow: Boolean,
)

enum class CatPrinterDrawMode(val rawValue: UByte) {
    Image(0x00u.toUByte()),
    Text(0x01u.toUByte()),
}

sealed class CatPrinterCommand {
    data class GetDeviceState(val state: CatPrinterDeviceState? = null) : CatPrinterCommand()

    data class WritePacing(val ready: Boolean) : CatPrinterCommand()

    data class SetQuality(val level: UByte) : CatPrinterCommand()

    data class SetEnergy(val energy: UShort) : CatPrinterCommand()

    data class FeedPaper(val pixels: UShort) : CatPrinterCommand()

    data class SetDrawMode(val mode: CatPrinterDrawMode) : CatPrinterCommand()

    class DrawRow(val imageRow: ByteArray, val printMode: CatPrinterPrintMode) : CatPrinterCommand()

    class Lattice(val data: ByteArray) : CatPrinterCommand()

    fun pack(): ByteArray {
        return when (this) {
            is GetDeviceState ->
                packCommand(CatPrinterPrintCommandId.GetDeviceState, byteArrayOf(0x00))
            is WritePacing ->
                packCommand(CatPrinterPrintCommandId.WritePacing, byteArrayOf(0x00))
            is SetQuality ->
                packCommand(CatPrinterPrintCommandId.SetQuality, byteArrayOf(level.toByte()))
            is SetEnergy -> {
                val writer = ByteWriter()
                writer.writeUInt16Le(energy)
                packCommand(CatPrinterPrintCommandId.SetEnergy, writer.data)
            }
            is FeedPaper -> {
                val writer = ByteWriter()
                writer.writeUInt16Le(pixels)
                packCommand(CatPrinterPrintCommandId.FeedPaper, writer.data)
            }
            is SetDrawMode ->
                packCommand(CatPrinterPrintCommandId.SetDrawMode, byteArrayOf(mode.rawValue.toByte()))
            is DrawRow ->
                packCommand(CatPrinterPrintCommandId.DrawRow,
                    catPrinterEncodeImageRow(imageRow.toUByteArray(), printMode))
            is Lattice ->
                packCommand(CatPrinterPrintCommandId.Lattice, data)
        }
    }

    companion object {
        val latticeStartData: ByteArray = byteArrayOf(
            0xAA.toByte(), 0x55.toByte(), 0x17.toByte(), 0x38.toByte(), 0x44.toByte(), 0x5F.toByte(),
            0x5F.toByte(), 0x5F.toByte(), 0x44.toByte(), 0x38.toByte(), 0x2C.toByte(),
        )

        val latticeEndData: ByteArray = byteArrayOf(
            0xAA.toByte(), 0x55.toByte(), 0x17.toByte(), 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x17.toByte(),
        )

        fun fromData(data: ByteArray): CatPrinterCommand? {
            return try {
                val (command, payload) = unpack(data)
                when (command) {
                    CatPrinterPrintCommandId.GetDeviceState -> {
                        if (payload.size < 1) {
                            return null
                        }
                        val value = payload[0].toInt() and 0xFF
                        GetDeviceState(
                            CatPrinterDeviceState(
                                noPaper = (value and 0x01) != 0,
                                coverIsOpen = (value and 0x02) != 0,
                                isOverheated = (value and 0x04) != 0,
                                batteryIsLow = (value and 0x08) != 0,
                            )
                        )
                    }
                    CatPrinterPrintCommandId.WritePacing -> {
                        if (payload.size != 1) {
                            return null
                        }
                        WritePacing(ready = payload[0].toInt() == 0x00)
                    }
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }

        private fun packCommand(command: CatPrinterPrintCommandId, data: ByteArray): ByteArray {
            if (data.size > 0xFFFF) {
                Log.i("CatPrinterCommand", "Command data too big (${data.size} > 0xFFFF)")
                return ByteArray(0)
            }
            val writer = ByteWriter()
            writer.writeUInt8(0x51u.toUByte())
            writer.writeUInt8(0x78u.toUByte())
            writer.writeUInt8(command.rawValue)
            writer.writeUInt8(0x00u.toUByte())
            writer.writeUInt16Le(data.size.toUShort())
            writer.writeBytes(data)
            writer.writeUInt8(computeCrc8(data).toUByte())
            writer.writeUInt8(0xFFu.toUByte())
            return writer.data
        }

        private fun unpack(data: ByteArray): Pair<CatPrinterPrintCommandId, ByteArray> {
            val reader = ByteReader(data)
            if (reader.readUInt8().toInt() != 0x51) {
                throw IllegalArgumentException("Wrong first byte")
            }
            if (reader.readUInt8().toInt() != 0x78) {
                throw IllegalArgumentException("Wrong second byte")
            }
            val command = CatPrinterPrintCommandId.fromRawValue(reader.readUInt8().toInt())
                ?: throw IllegalArgumentException("Unsupported command.")
            reader.readUInt8()
            val length = reader.readUInt16Le()
            val commandData = reader.readBytes(length.toInt())
            val crc = reader.readUInt8()
            if (computeCrc8(commandData) != (crc.toInt() and 0xFF)) {
                throw IllegalArgumentException("Wrong crc")
            }
            if (reader.readUInt8().toInt() != 0xFF) {
                throw IllegalArgumentException("Wrong last byte")
            }
            return command to commandData
        }
    }
}

fun catPrinterPackPrintImageCommands(image: List<ByteArray>, feedPaper: Boolean,
                                     printMode: CatPrinterPrintMode): ByteArray
{
    val commands: MutableList<CatPrinterCommand> = mutableListOf(
        CatPrinterCommand.SetQuality(0x35u.toUByte()),
        CatPrinterCommand.Lattice(CatPrinterCommand.latticeStartData),
        CatPrinterCommand.SetEnergy(0x7000u.toUShort()),
        CatPrinterCommand.SetDrawMode(CatPrinterDrawMode.Image),
    )
    for (imageRow in image) {
        commands.add(CatPrinterCommand.DrawRow(imageRow, printMode))
    }
    if (feedPaper) {
        commands.add(CatPrinterCommand.FeedPaper(catPrinterFeedPaperPixels))
    }
    commands.add(CatPrinterCommand.Lattice(CatPrinterCommand.latticeEndData))
    val output = ByteArrayOutputStream()
    for (command in commands) {
        output.write(command.pack())
    }
    return output.toByteArray()
}

private fun computeCrc8(data: ByteArray): Int {
    var crc = 0x00
    for (byte in data) {
        crc = crc xor (byte.toInt() and 0xFF)
        repeat(8) {
            crc = if ((crc and 0x80) != 0) {
                ((crc shl 1) xor 0x07) and 0xFF
            } else {
                (crc shl 1) and 0xFF
            }
        }
    }
    return crc and 0xFF
}
