package com.moblin.android.platform.swiftcube

import android.icu.text.BreakIterator
import com.moblin.android.platform.coregraphics.CGColorSpaceCreateDeviceRGB
import com.moblin.android.platform.coreimage.CIColorCubeWithColorSpace
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CharacterCodingException

data class LutEntry(val red: Float, val green: Float, val blue: Float)

enum class LutType {
    oneDimensional,
    threeDimensional,
}

sealed class SwiftCubeError(message: String) : Exception(message) {
    object couldNotDecodeData : SwiftCubeError("Not a text file")

    object sizeMissing : SwiftCubeError("Size missing")

    class sizeTooBig(val size: Long) : SwiftCubeError("Size $size too big")

    object oneDimensionalLutNotSupported : SwiftCubeError("One dimensional LUT not supported")

    class unsupportedKey(val key: String) : SwiftCubeError("Unsupported key $key")

    object invalidType : SwiftCubeError("Invalid type")

    object typeMissing : SwiftCubeError("Type missing")

    class invalidDataPoint(val point: String) : SwiftCubeError("Invalid data point $point")

    class wrongNumberOfDataPoints(val count: Int) : SwiftCubeError("Wrong number of data points $count")

    class invalidSyntax(val text: String) : SwiftCubeError("Invalid syntax $text")
}

private val space = ' '.code.toByte()
private val tab = '\t'.code.toByte()
private val newline = '\n'.code.toByte()
private val carriageReturn = '\r'.code.toByte()
private val hash = '#'.code.toByte()
private val decimalFloatRegex = Regex("[+-]?([0-9]+\\.?[0-9]*|\\.[0-9]+)([eE][+-]?[0-9]+)?")
private val hexFloatRegex =
    Regex("([+-]?0[xX]([0-9a-fA-F]+\\.?[0-9a-fA-F]*|\\.[0-9a-fA-F]+))([pP][+-]?[0-9]+)?")
private val infinityRegex = Regex("([+-]?)(inf|infinity)", RegexOption.IGNORE_CASE)
private val nanRegex = Regex("[+-]?(nan(\\((0[xX])?[0-9a-fA-F]+\\))?|snan)", RegexOption.IGNORE_CASE)
private val intRegex = Regex("[+-]?[0-9]+")

private fun isSpace(byte: Byte): Boolean {
    return byte == space || byte == tab
}

private fun parseFloat(text: String): Float? {
    if (decimalFloatRegex.matches(text)) {
        return text.toFloat()
    }
    hexFloatRegex.matchEntire(text)?.let { match ->
        return (match.groupValues[1] + match.groupValues[3].ifEmpty { "p0" }).toFloat()
    }
    infinityRegex.matchEntire(text)?.let { match ->
        return if (match.groupValues[1] == "-") Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY
    }
    if (nanRegex.matches(text)) {
        return Float.NaN
    }
    return null
}

private fun decodeUtf8(bytes: ByteArray): String? {
    var start = 0
    if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() &&
        bytes[2] == 0xBF.toByte()
    ) {
        start = 3
    }
    return try {
        Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes, start, bytes.size - start)).toString()
    } catch (error: CharacterCodingException) {
        null
    }
}

private class CubeLineParser(val line: ByteArray) {
    var index = 0

    fun skipSpaces() {
        while (index < line.size && isSpace(line[index])) {
            index += 1
        }
    }

    private fun parseNumber(): Float? {
        var end = index
        while (end < line.size && !isSpace(line[end])) {
            end += 1
        }
        if (end <= index) {
            return null
        }
        val value = parseFloat(String(line, index, end - index, Charsets.UTF_8)) ?: return null
        index = end
        return value
    }

    fun parseEntry(): LutEntry? {
        val red = parseNumber() ?: return null
        skipSpaces()
        val green = parseNumber() ?: return null
        skipSpaces()
        val blue = parseNumber() ?: return null
        skipSpaces()
        if (index != line.size) {
            return null
        }
        return LutEntry(red = red, green = green, blue = blue)
    }
}

private fun makeInvalidSyntaxError(line: ByteArray): SwiftCubeError {
    return SwiftCubeError.invalidSyntax(decodeUtf8(line.copyOfRange(0, minOf(line.size, 50))) ?: "")
}

private fun characters(text: String): List<String> {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(text)
    val characters = mutableListOf<String>()
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        characters.add(text.substring(start, end))
        start = end
        end = iterator.next()
    }
    return characters
}

private fun splitOnSpaces(text: String): List<String> {
    val parts = mutableListOf<String>()
    var part = ""
    for (character in characters(text)) {
        if (character == " " || character == "\t") {
            if (part.isNotEmpty()) {
                parts.add(part)
            }
            part = ""
        } else {
            part += character
        }
    }
    if (part.isNotEmpty()) {
        parts.add(part)
    }
    return parts
}

private fun readContents(contentsOf: String): ByteArray {
    val file = if (contentsOf.startsWith("file:")) File(URI(contentsOf)) else File(contentsOf)
    return file.readBytes()
}

class SC3DLut private constructor(
    var title: String?,
    var type: LutType?,
    var size: Int,
    var entries: List<LutEntry>,
) {
    constructor(fileData: ByteArray) : this(null, null, 0, emptyList()) {
        parse(fileData)
    }

    constructor(contentsOf: String) : this(readContents(contentsOf))

    private fun parse(fileData: ByteArray) {
        val parsedEntries = mutableListOf<LutEntry>()
        var parsedSize: Long? = null
        var start = 0
        while (start < fileData.size) {
            var end = start
            while (end < fileData.size && fileData[end] != newline && fileData[end] != carriageReturn) {
                end += 1
            }
            val line = fileData.copyOfRange(start, end)
            start = end + 1
            parseLine(line, parsedEntries)?.let { parsedSize = it }
        }
        entries = parsedEntries
        val lutSize = parsedSize ?: throw SwiftCubeError.sizeMissing
        size = lutSize.toInt()
        when (type ?: throw SwiftCubeError.typeMissing) {
            LutType.oneDimensional -> if (parsedEntries.size.toLong() != lutSize) {
                throw SwiftCubeError.wrongNumberOfDataPoints(parsedEntries.size)
            }
            LutType.threeDimensional -> if (parsedEntries.size.toLong() != lutSize * lutSize * lutSize) {
                throw SwiftCubeError.wrongNumberOfDataPoints(parsedEntries.size)
            }
        }
    }

    private fun parseLine(line: ByteArray, parsedEntries: MutableList<LutEntry>): Long? {
        val parser = CubeLineParser(line)
        parser.skipSpaces()
        if (parser.index >= line.size || line[parser.index] == hash) {
            return null
        }
        if (type == LutType.threeDimensional) {
            val entry = parser.parseEntry()
            if (entry != null) {
                parsedEntries.add(entry)
                return null
            }
        }
        val text = decodeUtf8(line) ?: throw SwiftCubeError.couldNotDecodeData
        val parts = splitOnSpaces(text)
        when (parts.firstOrNull()) {
            "TITLE" -> {
                title = characters(parts.drop(1).joinToString(" ")).drop(1).dropLast(1).joinToString("")
                return null
            }
            "LUT_1D_SIZE" -> throw SwiftCubeError.oneDimensionalLutNotSupported
            "LUT_3D_SIZE" -> {
                type = LutType.threeDimensional
                if (parts.size != 2 || !intRegex.matches(parts[1])) {
                    throw makeInvalidSyntaxError(line)
                }
                val size = parts[1].toLongOrNull() ?: throw makeInvalidSyntaxError(line)
                if (size >= 100) {
                    throw SwiftCubeError.sizeTooBig(size)
                }
                return size
            }
            "DOMAIN_MIN" -> throw SwiftCubeError.unsupportedKey("DOMAIN_MIN")
            "DOMAIN_MAX" -> throw SwiftCubeError.unsupportedKey("DOMAIN_MAX")
            else -> throw makeInvalidSyntaxError(line)
        }
    }

    fun ciFilter(): CIColorCubeWithColorSpace {
        val buffer = ByteBuffer.allocate(entries.size * 16).order(ByteOrder.LITTLE_ENDIAN)
        for (entry in entries) {
            buffer.putFloat(entry.red)
            buffer.putFloat(entry.green)
            buffer.putFloat(entry.blue)
            buffer.putFloat(1f)
        }
        val filter = CIColorCubeWithColorSpace()
        filter.cubeDimension = size.toFloat()
        filter.cubeData = buffer.array()
        filter.colorSpace = CGColorSpaceCreateDeviceRGB()
        return filter
    }

    fun copy(): SC3DLut {
        return SC3DLut(title, type, size, entries.toList())
    }
}
