package com.moblin.android.platform

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import com.moblin.android.platform.swiftui.hasSystemImage
import com.moblin.android.various.settings.SettingsGameController
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SystemImagesSuite {
    private val symbolsDrawnAsEmptyCircle = setOf("circle")

    private val symbolLiteral = """"([a-z][a-z0-9]*(?:\.[a-z0-9]+)*)""""

    private val symbolArgument = Regex(
        """(?:\b(?:systemImage|systemName|image|imageName|[A-Za-z]+ImageName)[ \t]*=[ \t]*""" +
            """|\bSystemImage\(\s*(?:name[ \t]*=[ \t]*)?)""" +
            """(?:if \([^()\n]*\)[ \t]*)?$symbolLiteral(?:[ \t]*else[ \t]*$symbolLiteral)?""",
    )

    private fun gameControllerButtonNames(): List<String> {
        return SettingsGameController().buttons.value.map { it.name }
    }

    private fun sourceRoot(): File {
        return listOf(File("src/main/java/com/moblin/android"), File("app/src/main/java/com/moblin/android"))
            .first { it.isDirectory }
    }

    private fun literalSymbolsInSources(): Map<String, List<String>> {
        val root = sourceRoot()
        val symbols = mutableMapOf<String, MutableList<String>>()
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            for (match in symbolArgument.findAll(file.readText())) {
                for (name in match.groupValues.drop(1).filter { it.isNotEmpty() }) {
                    symbols.getOrPut(name) { mutableListOf() }.add(file.relativeTo(root).invariantSeparatorsPath)
                }
            }
        }
        return symbols
    }

    @Test
    fun everyDefaultGameControllerButtonHasItsOwnGlyph() {
        val names = gameControllerButtonNames()
        assertEquals(20, names.size)
        for (name in names) {
            val image = systemImage(name)
            assertNotSame(Icons.Filled.RadioButtonUnchecked, image, name)
            assertEquals(name, image.name)
        }
        assertEquals(names.size, names.map { systemImage(it) }.toSet().size)
    }

    @Test
    fun gameControllerGlyphsExistLikeTheirSfSymbols() {
        for (name in gameControllerButtonNames()) {
            assertTrue(hasSystemImage(name), name)
        }
        for (name in listOf("a.circle", "triangle.circle", "l1.rectangle.roundedbottom", "zr.rectangle.roundedtop")) {
            assertTrue(hasSystemImage("$name.fill"), name)
            assertSame(systemImage(name), systemImage("$name.fill"), name)
        }
        assertTrue(hasSystemImage("xmark.circle.fill"))
        assertNotSame(systemImage("xmark.circle"), systemImage("xmark.circle.fill"))
        for (direction in listOf("up", "down", "left", "right")) {
            assertFalse(hasSystemImage("dpad.$direction.fill.fill"), direction)
            assertFalse(hasSystemImage("dpad.$direction"), direction)
        }
    }

    @Test
    fun symbolsWithoutAnEarlierMappingResolve() {
        for (name in listOf(
            "play.tv",
            "arrow.forward.circle",
            "arrow.backward.circle",
            "face.smiling",
            "face.smiling.inverse",
            "checkmark.square",
        )) {
            assertNotSame(Icons.Filled.RadioButtonUnchecked, systemImage(name), name)
            assertTrue(hasSystemImage(name), name)
        }
        assertFalse(hasSystemImage("face.smiling.inverse.fill"))
    }

    @Test
    fun everyLiteralSymbolInTheSourcesResolves() {
        val symbols = literalSymbolsInSources()
        assertTrue("play.tv" in symbols)
        assertTrue("face.smiling.inverse" in symbols)
        assertTrue("arrow.backward.circle" in symbols)
        val unresolved = symbols
            .filterKeys { it !in symbolsDrawnAsEmptyCircle }
            .filterKeys { systemImage(it) === Icons.Filled.RadioButtonUnchecked }
        assertEquals(emptyMap(), unresolved)
        for (name in symbolsDrawnAsEmptyCircle) {
            assertSame(Icons.Filled.RadioButtonUnchecked, systemImage(name))
        }
    }
}
