package com.moblin.android.platform.uikit

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.LoadedFontFamily
import com.moblin.android.platform.coretext.CTFontCollectionCreateFromAvailableFonts
import com.moblin.android.platform.coretext.CTFontCollectionCreateMatchingFontDescriptors
import com.moblin.android.platform.coretext.CTFontDescriptorCopyAttribute
import com.moblin.android.platform.coretext.kCTFontFamilyNameAttribute
import com.moblin.android.platform.coretext.kCTFontNameAttribute
import com.moblin.android.platform.swiftui.SwiftUIFonts
import com.moblin.android.view.utils.fontStyleName
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

private class NameRecord(val platform: Int, val encoding: Int, val language: Int, val id: Int, val text: String)

private fun windowsName(id: Int, text: String, language: Int = 0x409) = NameRecord(3, 1, language, id, text)

private fun nameTable(records: List<NameRecord>): ByteArray {
    val strings = ByteArrayOutputStream()
    val header = ByteBuffer.allocate(6 + 12 * records.size)
    header.putShort(0)
    header.putShort(records.size.toShort())
    header.putShort((6 + 12 * records.size).toShort())
    for (record in records) {
        val bytes = if (record.platform == 1) {
            record.text.toByteArray(Charsets.ISO_8859_1)
        } else {
            record.text.toByteArray(Charsets.UTF_16BE)
        }
        header.putShort(record.platform.toShort())
        header.putShort(record.encoding.toShort())
        header.putShort(record.language.toShort())
        header.putShort(record.id.toShort())
        header.putShort(bytes.size.toShort())
        header.putShort(strings.size().toShort())
        strings.write(bytes)
    }
    return header.array() + strings.toByteArray()
}

private fun os2Table(weight: Int, selection: Int): ByteArray {
    val table = ByteBuffer.allocate(96)
    table.putShort(4, weight.toShort())
    table.putShort(62, selection.toShort())
    return table.array()
}

private fun sfnt(tables: Map<String, ByteArray>, base: Int = 0): ByteArray {
    val sorted = tables.toSortedMap()
    val headerSize = 12 + 16 * sorted.size
    val header = ByteBuffer.allocate(headerSize)
    header.putInt(0x00010000)
    header.putShort(sorted.size.toShort())
    header.putShort(0)
    header.putShort(0)
    header.putShort(0)
    val body = ByteArrayOutputStream()
    for ((tag, data) in sorted) {
        header.put(tag.toByteArray(Charsets.US_ASCII))
        header.putInt(0)
        header.putInt(base + headerSize + body.size())
        header.putInt(data.size)
        body.write(data)
    }
    return header.array() + body.toByteArray()
}

private fun collection(fonts: List<Map<String, ByteArray>>): ByteArray {
    val headerSize = 12 + 4 * fonts.size
    val header = ByteBuffer.allocate(headerSize)
    header.putInt(0x74746366)
    header.putInt(0x00010000)
    header.putInt(fonts.size)
    val body = ByteArrayOutputStream()
    for (tables in fonts) {
        val offset = headerSize + body.size()
        header.putInt(offset)
        body.write(sfnt(tables, offset))
    }
    return header.array() + body.toByteArray()
}

private fun names(
    family: String = "Test",
    postScriptName: String? = null,
    variable: Boolean = false,
) = OpenTypeNames(
    familyName = family,
    styleName = null,
    fullName = null,
    postScriptName = postScriptName,
    weight = 400,
    italic = false,
    variable = variable,
)

private fun face(family: String, fontName: String, weight: Int = 400, italic: Boolean = false, width: Float = 100f) =
    InstalledFontFace(
        familyName = family,
        fontName = fontName,
        fullName = fontName.replace("-", " "),
        styleName = "",
        weight = weight,
        italic = italic,
        width = width,
        makeTypeface = { Typeface.create(Typeface.SERIF, Typeface.NORMAL) },
    )

@RunWith(RobolectricTestRunner::class)
class UIFontSuite {
    @After
    fun tearDown() {
        InstalledFonts.override = null
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun listsTheFontsOfAFontDirectory() {
        InstalledFonts.override = InstalledFonts(InstalledFonts.fontDirectory(File("src/main/assets/fonts")))
        assertEquals(listOf("Inter Variable", "Nunito"), UIFont.familyNames)
        assertEquals(listOf("Nunito-ExtraLight"), UIFont.fontNames(forFamilyName = "Nunito"))
        assertEquals(listOf("InterVariable-Regular"), UIFont.fontNames(forFamilyName = "Inter Variable"))
        val font = assertNotNull(UIFont(name = "Nunito", size = 17))
        assertNotSame(Typeface.DEFAULT, font.typeface)
    }

    @Test
    fun readsTheTypographicFamilyAndPostScriptNameOfAVariableFont() {
        val buffer = ByteBuffer.wrap(File("src/main/assets/fonts/Nunito.ttf").readBytes())
        assertEquals(1, OpenTypeNameTable.fontCount(buffer))
        val names = assertNotNull(OpenTypeNameTable.read(buffer, 0))
        assertEquals("Nunito", names.familyName)
        assertEquals("Nunito-ExtraLight", names.postScriptName)
        assertEquals(200, names.weight)
        assertFalse(names.italic)
        assertTrue(names.variable)
        assertNull(OpenTypeNameTable.read(buffer, 1))
    }

    @Test
    fun prefersWindowsEnglishNames() {
        val buffer = ByteBuffer.wrap(File("src/main/assets/fonts/InterVariable.ttf").readBytes())
        val names = assertNotNull(OpenTypeNameTable.read(buffer, 0))
        assertEquals("Inter Variable", names.familyName)
        assertEquals("InterVariable", names.postScriptName)
        assertEquals("Regular", names.styleName)
    }

    @Test
    fun readsStaticFontsAndCollections() {
        val regular = mapOf(
            "name" to nameTable(
                listOf(
                    NameRecord(1, 0, 0, 1, "Mac Family"),
                    windowsName(1, "Famille", language = 0x40C),
                    windowsName(1, "Sample Sans"),
                    windowsName(2, "Regular"),
                    windowsName(4, "Sample Sans Regular"),
                    windowsName(6, "SampleSans-Regular"),
                ),
            ),
            "OS/2" to os2Table(weight = 400, selection = 0x40),
        )
        val boldItalic = mapOf(
            "name" to nameTable(
                listOf(
                    windowsName(1, "Sample Sans Heavy"),
                    windowsName(2, "Italic"),
                    windowsName(6, "SampleSans-HeavyItalic"),
                    windowsName(16, "Sample Sans"),
                    windowsName(17, "Heavy Italic"),
                ),
            ),
            "OS/2" to os2Table(weight = 800, selection = 0x1),
        )
        val single = ByteBuffer.wrap(sfnt(regular))
        val first = assertNotNull(OpenTypeNameTable.read(single, 0))
        assertEquals("Sample Sans", first.familyName)
        assertEquals("Sample Sans Regular", first.fullName)
        assertEquals("SampleSans-Regular", first.postScriptName)
        assertEquals(400, first.weight)
        assertFalse(first.italic)
        assertFalse(first.variable)
        val fonts = ByteBuffer.wrap(collection(listOf(regular, boldItalic)))
        assertEquals(2, OpenTypeNameTable.fontCount(fonts))
        assertEquals("SampleSans-Regular", OpenTypeNameTable.read(fonts, 0)?.postScriptName)
        val second = assertNotNull(OpenTypeNameTable.read(fonts, 1))
        assertEquals("Sample Sans", second.familyName)
        assertEquals("Heavy Italic", second.styleName)
        assertEquals("SampleSans-HeavyItalic", second.postScriptName)
        assertEquals(800, second.weight)
        assertTrue(second.italic)
        assertNull(OpenTypeNameTable.read(fonts, 2))
    }

    @Test
    fun readsMacRomanNamesWhenThereAreNoOthers() {
        val font = sfnt(mapOf("name" to nameTable(listOf(NameRecord(1, 0, 0, 1, "Old Face")))))
        val names = assertNotNull(OpenTypeNameTable.read(ByteBuffer.wrap(font), 0))
        assertEquals("Old Face", names.familyName)
        assertNull(names.postScriptName)
    }

    @Test
    fun rejectsDamagedFonts() {
        val font = sfnt(mapOf("name" to nameTable(listOf(windowsName(1, "Cut")))))
        assertNull(OpenTypeNameTable.read(ByteBuffer.wrap(font.copyOf(20)), 0))
        assertNull(OpenTypeNameTable.read(ByteBuffer.wrap(ByteArray(3)), 0))
        assertEquals(0, OpenTypeNameTable.fontCount(ByteBuffer.wrap(ByteArray(3))))
        assertNull(OpenTypeNameTable.read(ByteBuffer.wrap(sfnt(mapOf("OS/2" to os2Table(400, 0)))), 0))
    }

    @Test
    fun namesVariableInstancesAfterTheirStyle() {
        val roboto = names(family = "Roboto", postScriptName = "Roboto-Regular", variable = true)
        val create = { null }
        val regular = InstalledFonts.face(roboto, weight = 400, italic = false, width = 100f, makeTypeface = create)
        assertEquals("Roboto-Regular", regular.fontName)
        assertEquals("Roboto Regular", regular.fullName)
        val italic = InstalledFonts.face(roboto, weight = 400, italic = true, width = 100f, makeTypeface = create)
        assertEquals("Roboto-Italic", italic.fontName)
        val boldItalic = InstalledFonts.face(roboto, weight = 700, italic = true, width = 100f, makeTypeface = create)
        assertEquals("Roboto-BoldItalic", boldItalic.fontName)
        assertEquals("Roboto Bold Italic", boldItalic.fullName)
        assertEquals("Bold Italic", boldItalic.styleName)
        val condensed = InstalledFonts.face(roboto, weight = 400, italic = false, width = 75f, makeTypeface = create)
        assertEquals("Roboto-Condensed", condensed.fontName)
        val condensedMedium = InstalledFonts.face(roboto, weight = 500, italic = false, width = 75f, makeTypeface = create)
        assertEquals("Roboto-CondensedMedium", condensedMedium.fontName)
        val noto = names(family = "Noto Serif", postScriptName = "NotoSerif-Regular", variable = true)
        val semiBold = InstalledFonts.face(noto, weight = 600, italic = false, width = 100f, makeTypeface = create)
        assertEquals("NotoSerif-SemiBold", semiBold.fontName)
        assertEquals("SemiBold", fontStyleName(family = "Noto Serif", fontName = semiBold.fontName))
        assertEquals("BoldItalic", fontStyleName(family = "Roboto", fontName = boldItalic.fontName))
        assertEquals("Regular", fontStyleName(family = "Roboto", fontName = regular.fontName))
    }

    @Test
    fun staticFontsKeepTheirPostScriptName() {
        val static = names(family = "Cutive Mono", postScriptName = "CutiveMono-Regular")
        val face = InstalledFonts.face(static, weight = 400, italic = false, width = 100f) { null }
        assertEquals("CutiveMono-Regular", face.fontName)
        val unnamed = names(family = "Coming Soon")
        assertEquals("ComingSoon-Bold", InstalledFonts.face(unnamed, 700, false, 100f) { null }.fontName)
    }

    @Test
    fun listsFamiliesAndTheirFacesRegularFirst() {
        InstalledFonts.override = InstalledFonts(
            listOf(
                face("Roboto", "Roboto-Thin", weight = 100),
                face("Roboto", "Roboto-BoldItalic", weight = 700, italic = true),
                face("Roboto", "Roboto-Condensed", width = 75f),
                face("Roboto", "Roboto-Italic", italic = true),
                face("Roboto", "Roboto-Regular"),
                face("Roboto", "Roboto-Bold", weight = 700),
                face("Roboto", "Roboto-Regular", weight = 500),
                face("Noto Serif", "NotoSerif-Regular"),
                face("Cutive Mono", "CutiveMono-Regular"),
                face(".Hidden", "Hidden-Regular"),
            ),
        )
        assertEquals(listOf("Cutive Mono", "Noto Serif", "Roboto"), UIFont.familyNames)
        assertEquals(
            listOf(
                "Roboto-Regular",
                "Roboto-Thin",
                "Roboto-Italic",
                "Roboto-Bold",
                "Roboto-BoldItalic",
                "Roboto-Condensed",
            ),
            UIFont.fontNames(forFamilyName = "Roboto"),
        )
        assertEquals(listOf("CutiveMono-Regular"), UIFont.fontNames(forFamilyName = "cutive mono"))
        assertEquals(emptyList(), UIFont.fontNames(forFamilyName = "Helvetica Neue"))
        assertEquals(emptyList(), UIFont.fontNames(forFamilyName = ".Hidden"))
    }

    @Test
    fun findsFontsByPostScriptFullAndFamilyName() {
        InstalledFonts.override = InstalledFonts(
            listOf(
                face("Roboto", "Roboto-Bold", weight = 700),
                face("Roboto", "Roboto-Regular"),
            ),
        )
        assertEquals("Roboto-Bold", UIFont(name = "Roboto-Bold", size = 17)?.fontName)
        assertEquals("Roboto-Bold", UIFont(name = "Roboto Bold", size = 17)?.fontName)
        assertEquals("Roboto-Regular", UIFont(name = "Roboto", size = 17)?.fontName)
        assertEquals("Roboto-Bold", UIFont(name = "roboto-bold", size = 17)?.fontName)
        val font = assertNotNull(UIFont(name = "Roboto", size = 20.5f))
        assertEquals("Roboto", font.familyName)
        assertEquals(20.5, font.pointSize)
        assertNull(UIFont(name = "HelveticaNeue-Bold", size = 17))
    }

    @Test
    fun skipsFontsThatCannotBeLoaded() {
        InstalledFonts.override = InstalledFonts(
            listOf(InstalledFontFace("Broken", "Broken-Regular", "Broken", "", 400, false, 100f) { error("damaged") }),
        )
        assertEquals(listOf("Broken"), UIFont.familyNames)
        assertNull(UIFont(name = "Broken", size = 17))
    }

    @Test
    fun coreTextListsTheInstalledFamilies() {
        InstalledFonts.override = InstalledFonts(
            listOf(
                face("Roboto", "Roboto-Regular"),
                face("Roboto", "Roboto-Bold", weight = 700),
                face("Noto Serif", "NotoSerif-Regular"),
            ),
        )
        val descriptors = assertNotNull(
            CTFontCollectionCreateMatchingFontDescriptors(CTFontCollectionCreateFromAvailableFonts(null)),
        )
        assertEquals(
            listOf("Noto Serif", "Roboto", "Roboto"),
            descriptors.map { CTFontDescriptorCopyAttribute(it, kCTFontFamilyNameAttribute) as? String },
        )
        assertEquals(
            listOf("NotoSerif-Regular", "Roboto-Regular", "Roboto-Bold"),
            descriptors.map { CTFontDescriptorCopyAttribute(it, kCTFontNameAttribute) as? String },
        )
        InstalledFonts.override = InstalledFonts(emptyList())
        assertNull(CTFontCollectionCreateMatchingFontDescriptors(CTFontCollectionCreateFromAvailableFonts(null)))
    }

    @Test
    fun customFontsUseTheInstalledFace() {
        InstalledFonts.override = InstalledFonts(
            listOf(face("Test Face", "TestFace-Regular"), face("Test Face", "TestFace-Bold", weight = 700)),
        )
        val style = SwiftUIFonts.custom(name = "TestFace-Bold", size = 17f)
        assertTrue(style.fontFamily is LoadedFontFamily)
        assertNull(style.fontWeight)
        assertSame(style.fontFamily, SwiftUIFonts.custom(name = "TestFace-Bold", size = 30f).fontFamily)
        assertTrue(SwiftUIFonts.custom(name = "Test Face", size = 17f).fontFamily is LoadedFontFamily)
        val fallback = SwiftUIFonts.custom(name = "Georgia-Bold", size = 17f)
        assertEquals(FontFamily.Serif, fallback.fontFamily)
        assertEquals(FontWeight.W700, fallback.fontWeight)
    }
}
