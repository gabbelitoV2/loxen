package com.moblin.android.integrations.catprinter

import android.util.Log
import com.moblin.android.common.various.isBitSet
import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.media.haishinkit.util.ByteWriter
import com.moblin.android.platform.crcswift.CrcSwift
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

private const val TAG = "CatPrinterCommandMxw01"

private class CatPrinterCommandException(message: String) : Exception(message) {
    override fun toString(): String = message ?: ""
}

private enum class CatPrinterCommandId(val rawValue: UByte) {
    getVersion(0xB1u),
    status(0xA1u),
    print(0xA9u),
    printComplete(0xAAu);

    companion object {
        fun fromRawValue(rawValue: UByte): CatPrinterCommandId? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

enum class CatPrinterPrintMode(val rawValue: UByte) {
    blackAndWhite(0u),
    grayscale(2u);

    companion object {
        fun fromRawValue(rawValue: UByte): CatPrinterPrintMode? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

sealed class CatPrinterCommandMxw01 {
    object GetVersionRequest : CatPrinterCommandMxw01()

    data class GetVersionResponse(val value: String) : CatPrinterCommandMxw01()

    object StatusRequest : CatPrinterCommandMxw01()

    data class StatusResponse(val ok: Boolean, val tooHot: Boolean, val hasPaper: Boolean) : CatPrinterCommandMxw01()

    data class PrintRequest(val printMode: CatPrinterPrintMode, val count: UShort) : CatPrinterCommandMxw01()

    data class PrintResponse(val status: UByte) : CatPrinterCommandMxw01()

    data class PrintCompleteIndication(val value: ByteArray) : CatPrinterCommandMxw01()

    fun pack(): ByteArray {
        val command: CatPrinterCommandId
        val data: ByteArray
        when (this) {
            GetVersionRequest -> {
                command = CatPrinterCommandId.getVersion
                data = byteArrayOf(0)
            }
            StatusRequest -> {
                command = CatPrinterCommandId.status
                data = byteArrayOf(0)
            }
            is PrintRequest -> {
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
        operator fun invoke(data: ByteArray): CatPrinterCommandMxw01? {
            val unpacked = try {
                unpack(data)
            } catch (error: Exception) {
                Log.i(TAG, "cat-printer: Unpack failed with $error")
                return null
            }
            val command = unpacked.first
            val payload = unpacked.second
            return when (command) {
                CatPrinterCommandId.getVersion ->
                    GetVersionResponse(stringFromUtf8(payload) ?: "unknown")
                CatPrinterCommandId.status -> {
                    val reader = ByteReader(payload)
                    runCatching {
                        reader.readBytes(6)
                        val ok = reader.readUInt8()
                        val reasons = reader.readUInt8()
                        StatusResponse(
                            ok = ok == 0.toUByte(),
                            tooHot = reasons.isBitSet(index = 2),
                            hasPaper = !reasons.isBitSet(index = 0),
                        )
                    }.getOrNull()
                }
                CatPrinterCommandId.print ->
                    if (payload.isEmpty()) {
                        null
                    } else {
                        PrintResponse(payload[0].toUByte())
                    }
                CatPrinterCommandId.printComplete -> PrintCompleteIndication(payload)
            }
        }

        private fun stringFromUtf8(data: ByteArray): String? {
            return runCatching {
                Charsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data))
                    .toString()
                    .removePrefix("\uFEFF")
            }.getOrNull()
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
            writer.writeUInt8(CrcSwift.computeCrc8(data))
            writer.writeUInt8(0xFFu)
            return writer.data
        }

        private fun unpack(data: ByteArray): Pair<CatPrinterCommandId, ByteArray> {
            val reader = ByteReader(data)
            if (reader.readUInt8() != 0x22.toUByte()) {
                throw CatPrinterCommandException("Wrong first byte")
            }
            if (reader.readUInt8() != 0x21.toUByte()) {
                throw CatPrinterCommandException("Wrong second byte")
            }
            val command = CatPrinterCommandId.fromRawValue(reader.readUInt8())
                ?: throw CatPrinterCommandException("Unsupported command.")
            reader.readUInt8()
            val length = reader.readUInt16Le()
            val payload = reader.readBytes(length.toInt())
            return command to payload
        }
    }
}

fun catPrinterPackPrintImageCommandsMxw01(image: List<UByteArray>, printMode: CatPrinterPrintMode): ByteArray {
    var data = ByteArray(0)
    for (imageRow in image) {
        data += catPrinterEncodeImageRow(imageRow, printMode)
    }
    while (data.size < 90 * catPrinterWidthPixels / 8) {
        data += byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        data += byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        data += byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        data += byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        data += byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    }
    return data
}
