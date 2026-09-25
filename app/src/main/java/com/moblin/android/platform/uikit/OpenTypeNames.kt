package com.moblin.android.platform.uikit

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

internal class OpenTypeNames(
    val familyName: String,
    val styleName: String?,
    val fullName: String?,
    val postScriptName: String?,
    val weight: Int,
    val italic: Boolean,
    val variable: Boolean,
)

internal object OpenTypeNameTable {
    private const val collectionTag = 0x74746366
    private const val nameTag = 0x6E616D65
    private const val os2Tag = 0x4F532F32
    private const val headTag = 0x68656164
    private const val fvarTag = 0x66766172
    private const val familyId = 1
    private const val subfamilyId = 2
    private const val fullNameId = 4
    private const val postScriptNameId = 6
    private const val typographicFamilyId = 16
    private const val typographicSubfamilyId = 17
    private val macRoman: Charset = try {
        Charset.forName("x-MacRoman")
    } catch (error: Exception) {
        Charsets.ISO_8859_1
    }

    fun fontCount(buffer: ByteBuffer): Int {
        val data = bigEndian(buffer)
        return try {
            if (data.getInt(0) == collectionTag) {
                data.getInt(8).coerceIn(0, 256)
            } else {
                1
            }
        } catch (error: IndexOutOfBoundsException) {
            0
        }
    }

    fun read(buffer: ByteBuffer, index: Int): OpenTypeNames? {
        val data = bigEndian(buffer)
        return try {
            readFont(data, fontOffset(data, index) ?: return null)
        } catch (error: IndexOutOfBoundsException) {
            null
        }
    }

    private fun bigEndian(buffer: ByteBuffer): ByteBuffer {
        val data = buffer.duplicate().order(ByteOrder.BIG_ENDIAN)
        data.clear()
        return data
    }

    private fun fontOffset(data: ByteBuffer, index: Int): Int? {
        if (data.getInt(0) != collectionTag) {
            return if (index == 0) 0 else null
        }
        val count = data.getInt(8)
        if (index < 0 || index >= count) {
            return null
        }
        return data.getInt(12 + 4 * index)
    }

    private fun readFont(data: ByteBuffer, offset: Int): OpenTypeNames? {
        val tables = tables(data, offset)
        val name = tables[nameTag] ?: return null
        val names = names(data, name)
        val familyName = names[typographicFamilyId] ?: names[familyId] ?: return null
        val os2 = tables[os2Tag]
        val head = tables[headTag]
        var weight = 400
        var italic = false
        if (os2 != null && os2.second >= 64) {
            weight = data.getShort(os2.first + 4).toInt() and 0xFFFF
            val selection = data.getShort(os2.first + 62).toInt() and 0xFFFF
            italic = selection and 0x201 != 0
        } else if (head != null && head.second >= 46) {
            val macStyle = data.getShort(head.first + 44).toInt() and 0xFFFF
            weight = if (macStyle and 1 != 0) 700 else 400
            italic = macStyle and 2 != 0
        }
        return OpenTypeNames(
            familyName = familyName,
            styleName = names[typographicSubfamilyId] ?: names[subfamilyId],
            fullName = names[fullNameId],
            postScriptName = names[postScriptNameId],
            weight = if (weight in 1..1000) weight else 400,
            italic = italic,
            variable = tables.containsKey(fvarTag),
        )
    }

    private fun tables(data: ByteBuffer, offset: Int): Map<Int, Pair<Int, Int>> {
        val count = data.getShort(offset + 4).toInt() and 0xFFFF
        val tables = HashMap<Int, Pair<Int, Int>>()
        for (i in 0 until count) {
            val record = offset + 12 + 16 * i
            val tableOffset = data.getInt(record + 8)
            val length = data.getInt(record + 12)
            if (tableOffset < 0 || length < 0 || tableOffset.toLong() + length > data.limit()) {
                continue
            }
            tables[data.getInt(record)] = tableOffset to length
        }
        return tables
    }

    private fun names(data: ByteBuffer, table: Pair<Int, Int>): Map<Int, String> {
        val start = table.first
        val end = start + table.second
        val count = data.getShort(start + 2).toInt() and 0xFFFF
        val storage = start + (data.getShort(start + 4).toInt() and 0xFFFF)
        val best = HashMap<Int, Pair<Int, String>>()
        for (i in 0 until count) {
            val record = start + 6 + 12 * i
            if (record + 12 > end) {
                break
            }
            val platform = data.getShort(record).toInt() and 0xFFFF
            val encoding = data.getShort(record + 2).toInt() and 0xFFFF
            val language = data.getShort(record + 4).toInt() and 0xFFFF
            val nameId = data.getShort(record + 6).toInt() and 0xFFFF
            val length = data.getShort(record + 8).toInt() and 0xFFFF
            val stringOffset = storage + (data.getShort(record + 10).toInt() and 0xFFFF)
            if (nameId !in wantedIds || stringOffset + length > end) {
                continue
            }
            val rank = rank(platform, encoding, language) ?: continue
            val previous = best[nameId]
            if (previous != null && previous.first <= rank) {
                continue
            }
            val bytes = ByteArray(length)
            for (j in 0 until length) {
                bytes[j] = data.get(stringOffset + j)
            }
            val charset = if (platform == 1) macRoman else Charsets.UTF_16BE
            val text = String(bytes, charset).trim { it.isWhitespace() || it == '\u0000' }
            if (text.isNotEmpty()) {
                best[nameId] = rank to text
            }
        }
        return best.mapValues { it.value.second }
    }

    private val wantedIds = setOf(
        familyId,
        subfamilyId,
        fullNameId,
        postScriptNameId,
        typographicFamilyId,
        typographicSubfamilyId,
    )

    private fun rank(platform: Int, encoding: Int, language: Int): Int? = when (platform) {
        3 -> when {
            encoding != 0 && encoding != 1 && encoding != 10 -> null
            language == 0x409 -> 0
            language and 0xFF == 0x09 -> 3
            else -> 4
        }
        0 -> 1
        1 -> if (encoding == 0 && language == 0) 2 else null
        else -> null
    }
}
