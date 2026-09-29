package com.moblin.android.integrations.catprinter

import com.moblin.android.platform.log.Log
import com.moblin.android.common.various.isBitSet
import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.media.haishinkit.util.ByteWriter
import com.moblin.android.platform.crcswift.CrcSwift

val catPrinterFeedPaperPixels: UShort = 50u

private enum class CatPrinterCommandsCommandId(val rawValue: UByte) {
    feedPaper(0xA1u),
    drawRow(0xA2u),
    getDeviceState(0xA3u),
    setQuality(0xA4u),
    lattice(0xA6u),
    writePacing(0xAEu),
    setEnergy(0xAFu),
    setDrawMode(0xBEu);

    companion object {
        fun fromRawValue(rawValue: UByte): CatPrinterCommandsCommandId? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

data class CatPrinterDeviceState(
    val noPaper: Boolean,
    val coverIsOpen: Boolean,
    val isOverheated: Boolean,
    val batteryIsLow: Boolean,
)

enum class CatPrinterDrawMode(val rawValue: UByte) {
    image(0u),
    text(1u),
}

sealed class CatPrinterCommand {
    data class GetDeviceState(val state: CatPrinterDeviceState? = null) : CatPrinterCommand()
    data class WritePacing(val ready: Boolean) : CatPrinterCommand()
    data class SetQuality(val level: UByte) : CatPrinterCommand()
    data class SetEnergy(val energy: UShort) : CatPrinterCommand()
    data class FeedPaper(val pixels: UShort) : CatPrinterCommand()
    data class SetDrawMode(val mode: CatPrinterDrawMode) : CatPrinterCommand()
    data class DrawRow(val imageRow: UByteArray, val printMode: CatPrinterPrintMode) : CatPrinterCommand()
    data class Lattice(val data: ByteArray) : CatPrinterCommand()

    fun pack(): ByteArray {
        val self = this
        val command: CatPrinterCommandsCommandId
        val data: ByteArray
        when (self) {
            is GetDeviceState -> {
                command = CatPrinterCommandsCommandId.getDeviceState
                data = byteArrayOf(0x00)
            }
            is WritePacing -> {
                command = CatPrinterCommandsCommandId.writePacing
                data = byteArrayOf(0x00)
            }
            is SetQuality -> {
                command = CatPrinterCommandsCommandId.setQuality
                data = byteArrayOf(self.level.toByte())
            }
            is SetEnergy -> {
                command = CatPrinterCommandsCommandId.setEnergy
                val writer = ByteWriter()
                writer.writeUInt16Le(self.energy)
                data = writer.data
            }
            is FeedPaper -> {
                command = CatPrinterCommandsCommandId.feedPaper
                val writer = ByteWriter()
                writer.writeUInt16Le(self.pixels)
                data = writer.data
            }
            is SetDrawMode -> {
                command = CatPrinterCommandsCommandId.setDrawMode
                data = byteArrayOf(self.mode.rawValue.toByte())
            }
            is DrawRow -> {
                command = CatPrinterCommandsCommandId.drawRow
                data = catPrinterEncodeImageRow(self.imageRow, self.printMode)
            }
            is Lattice -> {
                command = CatPrinterCommandsCommandId.lattice
                data = self.data
            }
        }
        return packCommand(command, data)
    }

    companion object {
        val latticeStartData = byteArrayOf(
            0xAA.toByte(),
            0x55.toByte(),
            0x17.toByte(),
            0x38.toByte(),
            0x44.toByte(),
            0x5F.toByte(),
            0x5F.toByte(),
            0x5F.toByte(),
            0x44.toByte(),
            0x38.toByte(),
            0x2C.toByte(),
        )

        val latticeEndData = byteArrayOf(
            0xAA.toByte(),
            0x55.toByte(),
            0x17.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x17.toByte(),
        )

        operator fun invoke(data: ByteArray): CatPrinterCommand? {
            val unpacked = runCatching { unpack(data) }.getOrNull() ?: return null
            val command = unpacked.first
            val payload = unpacked.second
            return when (command) {
                CatPrinterCommandsCommandId.getDeviceState -> {
                    if (payload.size < 1) {
                        return null
                    }
                    val value = payload[0].toUByte()
                    GetDeviceState(
                        state = CatPrinterDeviceState(
                            noPaper = value.isBitSet(index = 0),
                            coverIsOpen = value.isBitSet(index = 1),
                            isOverheated = value.isBitSet(index = 2),
                            batteryIsLow = value.isBitSet(index = 3),
                        ),
                    )
                }
                CatPrinterCommandsCommandId.writePacing -> {
                    if (payload.size != 1) {
                        return null
                    }
                    WritePacing(ready = payload[0] == 0x00.toByte())
                }
                else -> null
            }
        }

        private fun packCommand(command: CatPrinterCommandsCommandId, data: ByteArray): ByteArray {
            if (data.size > 0xFFFF) {
                Log.i("CatPrinterCommand", "Command data too big (${data.size} > 0xFFFF)")
                return ByteArray(0)
            }
            val writer = ByteWriter()
            writer.writeUInt8(0x51.toUByte())
            writer.writeUInt8(0x78.toUByte())
            writer.writeUInt8(command.rawValue)
            writer.writeUInt8(0x00.toUByte())
            writer.writeUInt16Le(data.size.toUShort())
            writer.writeBytes(data)
            writer.writeUInt8(CrcSwift.computeCrc8(data))
            writer.writeUInt8(0xFF.toUByte())
            return writer.data
        }

        private fun unpack(data: ByteArray): Pair<CatPrinterCommandsCommandId, ByteArray> {
            val reader = ByteReader(data)
            if (reader.readUInt8() != 0x51.toUByte()) {
                throw IllegalStateException("Wrong first byte")
            }
            if (reader.readUInt8() != 0x78.toUByte()) {
                throw IllegalStateException("Wrong second byte")
            }
            val command = CatPrinterCommandsCommandId.fromRawValue(reader.readUInt8())
                ?: throw IllegalStateException("Unsupported command.")
            reader.readUInt8()
            val length = reader.readUInt16Le()
            val payload = reader.readBytes(length.toInt())
            val crc = reader.readUInt8()
            if (CrcSwift.computeCrc8(payload) != crc) {
                throw IllegalStateException("Wrong crc")
            }
            if (reader.readUInt8() != 0xFF.toUByte()) {
                throw IllegalStateException("Wrong last byte")
            }
            return command to payload
        }
    }
}

fun catPrinterPackPrintImageCommands(
    image: List<UByteArray>,
    feedPaper: Boolean,
    printMode: CatPrinterPrintMode,
): ByteArray {
    val commands = mutableListOf<CatPrinterCommand>(
        CatPrinterCommand.SetQuality(level = 0x35u),
        CatPrinterCommand.Lattice(data = CatPrinterCommand.latticeStartData),
        CatPrinterCommand.SetEnergy(energy = 0x7000u),
        CatPrinterCommand.SetDrawMode(mode = CatPrinterDrawMode.image),
    )
    for (imageRow in image) {
        commands.add(CatPrinterCommand.DrawRow(imageRow = imageRow, printMode = printMode))
    }
    if (feedPaper) {
        commands.add(CatPrinterCommand.FeedPaper(pixels = catPrinterFeedPaperPixels))
    }
    commands.add(CatPrinterCommand.Lattice(data = CatPrinterCommand.latticeEndData))
    var data = ByteArray(0)
    for (command in commands) {
        data += command.pack()
    }
    return data
}
