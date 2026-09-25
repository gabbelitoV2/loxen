package com.moblin.android.platform.uikit

import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontStyle
import android.graphics.fonts.SystemFonts
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs

internal class InstalledFontFace(
    val familyName: String,
    val fontName: String,
    val fullName: String,
    val styleName: String,
    val weight: Int,
    val italic: Boolean,
    val width: Float,
    private val makeTypeface: () -> Typeface?,
) {
    val typeface: Typeface? by lazy {
        try {
            makeTypeface()
        } catch (error: Throwable) {
            Log.w("Moblin", "Font $fontName could not be loaded: $error")
            null
        }
    }

    val isRegular: Boolean
        get() = weight == 400 && !italic && width == 100f
}

internal class InstalledFonts(faces: List<InstalledFontFace>) {
    private val families: Map<String, List<InstalledFontFace>>
    private val familiesLowercased: Map<String, List<InstalledFontFace>>
    private val byFontName = HashMap<String, InstalledFontFace>()
    private val byFullName = HashMap<String, InstalledFontFace>()
    private val byFontNameLowercased = HashMap<String, InstalledFontFace>()
    private val byFullNameLowercased = HashMap<String, InstalledFontFace>()

    val familyNames: List<String>

    init {
        val unique = LinkedHashMap<String, InstalledFontFace>()
        for (face in faces) {
            if (face.familyName.startsWith(".")) {
                continue
            }
            unique.putIfAbsent(face.fontName.lowercase(), face)
        }
        families = unique.values.groupBy { it.familyName }.mapValues { (_, faces) -> faces.sortedWith(faceOrder) }
        familiesLowercased = families.entries.associate { it.key.lowercase() to it.value }
        familyNames = families.keys.sorted()
        for (face in unique.values) {
            byFontName.putIfAbsent(face.fontName, face)
            byFullName.putIfAbsent(face.fullName, face)
            byFontNameLowercased.putIfAbsent(face.fontName.lowercase(), face)
            byFullNameLowercased.putIfAbsent(face.fullName.lowercase(), face)
        }
    }

    fun fontNames(familyName: String): List<String> =
        (families[familyName] ?: familiesLowercased[familyName.lowercase()])?.map { it.fontName } ?: emptyList()

    fun face(name: String): InstalledFontFace? {
        byFontName[name]?.let { return it }
        byFullName[name]?.let { return it }
        families[name]?.let { return it.first() }
        val lowercased = name.lowercase()
        return byFontNameLowercased[lowercased]
            ?: byFullNameLowercased[lowercased]
            ?: familiesLowercased[lowercased]?.first()
    }

    companion object {
        private val faceOrder = compareBy<InstalledFontFace>(
            { !it.isRegular },
            { it.width != 100f },
            { it.width },
            { it.weight },
            { it.italic },
            { it.fontName },
        )

        private val weightNames = listOf(
            100 to "Thin",
            200 to "ExtraLight",
            300 to "Light",
            400 to "Regular",
            500 to "Medium",
            600 to "SemiBold",
            700 to "Bold",
            800 to "ExtraBold",
            900 to "Black",
        )

        private val widthNames = listOf(
            50f to "UltraCondensed",
            62.5f to "ExtraCondensed",
            75f to "Condensed",
            87.5f to "SemiCondensed",
            100f to "",
            112.5f to "SemiExpanded",
            125f to "Expanded",
            150f to "ExtraExpanded",
            200f to "UltraExpanded",
        )

        @Volatile
        internal var override: InstalledFonts? = null

        private val system: InstalledFonts by lazy { InstalledFonts(load()) }

        val shared: InstalledFonts
            get() = override ?: system

        fun styleWords(weight: Int, italic: Boolean, width: Float): List<String> {
            val words = mutableListOf<String>()
            val widthName = widthNames.minBy { abs(it.first - width) }.second
            if (widthName.isNotEmpty()) {
                words.add(widthName)
            }
            val weightName = weightNames.minBy { abs(it.first - weight) }.second
            if (weightName != "Regular" || words.isEmpty() && !italic) {
                words.add(weightName)
            }
            if (italic) {
                words.add("Italic")
            }
            return words
        }

        fun face(
            names: OpenTypeNames,
            weight: Int,
            italic: Boolean,
            width: Float,
            makeTypeface: () -> Typeface?,
        ): InstalledFontFace {
            val family = names.familyName
            val postScriptName = names.postScriptName
            if (!names.variable && postScriptName != null) {
                return InstalledFontFace(
                    familyName = family,
                    fontName = postScriptName,
                    fullName = names.fullName ?: postScriptName,
                    styleName = names.styleName ?: styleWords(weight, italic, width).joinToString(" "),
                    weight = weight,
                    italic = italic,
                    width = width,
                    makeTypeface = makeTypeface,
                )
            }
            val words = styleWords(weight, italic, width)
            return InstalledFontFace(
                familyName = family,
                fontName = family.replace(" ", "") + "-" + words.joinToString(""),
                fullName = family + " " + words.joinToString(" "),
                styleName = words.joinToString(" "),
                weight = weight,
                italic = italic,
                width = width,
                makeTypeface = makeTypeface,
            )
        }

        private fun load(): List<InstalledFontFace> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                systemFonts()
            } else {
                fontDirectory(File("/system/fonts"))
            }
        } catch (error: Throwable) {
            Log.w("Moblin", "Installed fonts could not be listed: $error")
            emptyList()
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun systemFonts(): List<InstalledFontFace> {
            val names = HashMap<Pair<String, Int>, OpenTypeNames?>()
            val faces = ArrayList<InstalledFontFace>()
            val fonts = SystemFonts.getAvailableFonts().sortedWith(
                compareBy<Font>({ it.file?.path ?: "" }, { it.ttcIndex }, { it.style.weight }, { it.style.slant }),
            )
            for (font in fonts) {
                val path = font.file?.path
                val fontNames = if (path != null) {
                    names.getOrPut(path to font.ttcIndex) { OpenTypeNameTable.read(font.buffer, font.ttcIndex) }
                } else {
                    OpenTypeNameTable.read(font.buffer, font.ttcIndex)
                } ?: continue
                val width = font.axes?.firstOrNull { it.tag == "wdth" }?.styleValue ?: 100f
                faces.add(
                    face(
                        names = fontNames,
                        weight = font.style.weight,
                        italic = font.style.slant == FontStyle.FONT_SLANT_ITALIC,
                        width = width,
                    ) {
                        Typeface.CustomFallbackBuilder(android.graphics.fonts.FontFamily.Builder(font).build())
                            .setStyle(font.style)
                            .setSystemFallback("sans-serif")
                            .build()
                    },
                )
            }
            return faces
        }

        internal fun fontDirectory(directory: File): List<InstalledFontFace> {
            val extensions = setOf("ttf", "otf", "ttc", "otc")
            val files = directory.listFiles { file -> file.extension.lowercase() in extensions } ?: return emptyList()
            val faces = ArrayList<InstalledFontFace>()
            for (file in files.sortedBy { it.name }) {
                val buffer = map(file) ?: continue
                for (index in 0 until OpenTypeNameTable.fontCount(buffer)) {
                    val names = OpenTypeNameTable.read(buffer, index) ?: continue
                    faces.add(
                        face(names = names, weight = names.weight, italic = names.italic, width = 100f) {
                            Typeface.Builder(file).setTtcIndex(index).build()
                        },
                    )
                }
            }
            return faces
        }

        private fun map(file: File): ByteBuffer? = try {
            RandomAccessFile(file, "r").use { it.channel.map(FileChannel.MapMode.READ_ONLY, 0, it.length()) }
        } catch (error: Exception) {
            null
        }
    }
}
