package com.moblin.android.integrations.catprinter

import android.util.Log
import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.media.haishinkit.util.ByteWriter
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

private const val TAG = "CatPrinterCommandMxw01"

private enum class CatPrinterCommandId(val rawValue: UByte) {
    getVersion(0xB1u),
    status(0xA1u),
    print(0xA9u),
    printComplete(0xAAu);

    companion object {
        fun fromRawValue(value: UByte): CatPrinterCommandId? =
            entries.firstOrNull { it.rawValue == value }
    }
}

enum class CatPrinterPrintMode(val rawValue: UByte) {
    blackAndWhite(0u),
    grayscale(2u);

    companion object {
        fun fromRawValue(value: UByte): CatPrinterPrintMode? =
            entries.firstOrNull { it.rawValue == value }
    }
}

sealed class CatPrinterCommandMxw01 {
    data object getVersionRequest : CatPrinterCommandMxw01()

    data class getVersionResponse(val value: String) : CatPrinterCommandMxw01()

    data object statusRequest : CatPrinterCommandMxw01()

    data class statusResponse(val ok: Boolean, val tooHot: Boolean, val hasPaper: Boolean) :
        CatPrinterCommandMxw01()

    data class printRequest(val printMode: CatPrinterPrintMode, val count: UShort) :
        CatPrinterCommandMxw01()

    data class printResponse(val status: UByte) : CatPrinterCommandMxw01()

    data class printCompleteIndication(val value: ByteArray) : CatPrinterCommandMxw01()

    fun pack(): ByteArray {
        val command: CatPrinterCommandId
        val data: ByteArray
        when (this) {
            is getVersionRequest -> {
                command = CatPrinterCommandId.getVersion
                data = byteArrayOf(0x00)
            }
            is statusRequest -> {
                command = CatPrinterCommandId.status
                data = byteArrayOf(0x00)
            }
            is printRequest -> {
                command = CatPrinterCommandId.print
                val writer = ByteWriter()
                writer.writeUInt16Le(count)
                writer.writeUInt8(0x30u)
                writer.writeUInt8(printMode.rawValue)
                data = writer.data
            }
            else -> return ByteArray(0)
        }
        return packCommand(command, data)
    }

    companion object {
        fun fromData(data: ByteArray): CatPrinterCommandMxw01? {
            val unpacked = try {
                unpack(data)
            } catch (e: Exception) {
                Log.i(TAG, "cat-printer: Unpack failed with $e")
                return null
            }
            val (command, payload) = unpacked
            return when (command) {
                CatPrinterCommandId.getVersion -> {
                    val decoder = Charsets.UTF_8.newDecoder()
                    decoder.onMalformedInput(CodingErrorAction.REPORT)
                    val value = runCatching {
                        decoder.decode(ByteBuffer.wrap(payload)).toString()
                    }.getOrNull()
                    getVersionResponse(value ?: "unknown")
                }
                CatPrinterCommandId.status -> {
                    val reader = ByteReader(payload)
                    try {
                        reader.readBytes(6)
                        val ok = reader.readUInt8()
                        val reasons = reader.readUInt8()
                        statusResponse(
                            ok = ok == 0.toUByte(),
                            tooHot = (reasons.toInt() and (1 shl 2)) != 0,
                            hasPaper = (reasons.toInt() and (1 shl 0)) == 0,
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                CatPrinterCommandId.print ->
                    if (payload.isNotEmpty()) printResponse(payload[0].toUByte()) else null
                CatPrinterCommandId.printComplete -> printCompleteIndication(payload)
            }
        }

        private fun packCommand(command: CatPrinterCommandId, data: ByteArray): ByteArray {
            if (data.size > 0xFFFF) {
                Log.i(TAG, "Command data too big (${data.size} > 0xFFFF)")
                return ByteArray(0)
            }
            val writer = ByteWriter()
            writer.writeUInt8(0x22u)
            writer.writeUInt8(0x21u)
            writer.writeUInt8(command.rawValue)
            writer.writeUInt8(0x00u)
            writer.writeUInt16Le(data.size.toUShort())
            writer.writeBytes(data)
            writer.writeUInt8(computeCrc8(data))
            writer.writeUInt8(0xFFu)
            return writer.data
        }

        private fun unpack(data: ByteArray): Pair<CatPrinterCommandId, ByteArray> {
            val reader = ByteReader(data)
            if (reader.readUInt8().toInt() != 0x22) {
                throw IllegalStateException("Wrong first byte")
            }
            if (reader.readUInt8().toInt() != 0x21) {
                throw IllegalStateException("Wrong second byte")
            }
            val command = CatPrinterCommandId.fromRawValue(reader.readUInt8())
                ?: throw IllegalStateException("Unsupported command.")
            reader.readUInt8()
            val length = reader.readUInt16Le()
            val payload = reader.readBytes(length.toInt())
            return command to payload
        }
    }
}

private fun computeCrc8(data: ByteArray): UByte {
    var crc = 0
    for (byte in data) {
        crc = crc xor (byte.toInt() and 0xFF)
        for (i in 0 until 8) {
            crc = if (crc and 0x80 != 0) {
                ((crc shl 1) xor 0x07) and 0xFF
            } else {
                (crc shl 1) and 0xFF
            }
        }
    }
    return crc.toUByte()
}

// One bit per pixel, often 384 pixels wide.
fun catPrinterPackPrintImageCommandsMxw01(
    image: List<List<UByte>>,
    printMode: CatPrinterPrintMode,
): ByteArray {
    var data = ByteArray(0)
    for (imageRow in image) {
        data += catPrinterEncodeImageRow(imageRow, printMode)
    }
    // Doesn't print if smaller. There is probably a better way to do this.
    while (data.size < 90 * catPrinterWidthPixels / 8) {
        data += byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        data += byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        data += byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        data += byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        data += byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    }
    return data
}
