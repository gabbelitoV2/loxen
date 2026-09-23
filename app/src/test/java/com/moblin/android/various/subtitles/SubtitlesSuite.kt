package com.moblin.android.various.subtitles

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubtitlesSuite {
    @Test
    fun speechToTextOutput() {
        val subtitles = Subtitles(null)
        var position = 0
        var frozen = ""
        var partial = "Hello"
        subtitles.updateSubtitles(position, frozen + partial)
        assertEquals(listOf("Hello"), subtitles.lines)
        frozen = "Hello What is up "
        partial = "Not much"
        subtitles.updateSubtitles(position, frozen + partial)
        assertEquals(listOf("Hello What is up Not much"), subtitles.lines)
        frozen = "Hello What is up Not much at all "
        partial = "He said will it continue to be a long string when will it go to be the frozen one maybe it will be the frozen one now no it just continues so the partial one can be really really long"
        subtitles.updateSubtitles(position, frozen + partial)
        assertEquals(
            listOf(
                "no it just continues so the partial one can be re",
                "ally really long",
            ),
            subtitles.lines,
        )
        position = 66
        frozen = "long string when will it go to be the frozen one maybe it will be the frozen one now no it just continues so the partial one can be really really long "
        partial = "Have to wait"
        subtitles.updateSubtitles(position, frozen + partial)
        assertEquals(
            listOf(
                "no it just continues so the partial one can be re",
                "ally really long Have to wait",
            ),
            subtitles.lines,
        )
        position = 109
        frozen = "n one maybe it will be the frozen one now no it just continues so the partial one can be really really long Have to wait a while and maybe it switches "
        partial = "Yes"
        subtitles.updateSubtitles(position, frozen + partial)
        assertEquals(
            listOf(
                "ally really long Have to wait a while and maybe it",
                "switches Yes",
            ),
            subtitles.lines,
        )
        partial = "Yes no maybe something is coming up"
        subtitles.updateSubtitles(position, frozen + partial)
        assertEquals(
            listOf(
                "ally really long Have to wait a while and maybe it",
                "switches Yes no maybe something is coming up",
            ),
            subtitles.lines,
        )
        partial = "Yes no maybe something is coming up or is it"
        subtitles.updateSubtitles(position, frozen + partial)
        assertEquals(
            listOf(
                "switches Yes no maybe something is coming up or i",
                "s it",
            ),
            subtitles.lines,
        )
        partial = "Yes no maybe something did come up or is it"
        subtitles.updateSubtitles(position, frozen + partial)
        assertEquals(
            listOf(
                "switches Yes no maybe something did come up or is",
                "it",
            ),
            subtitles.lines,
        )
        partial = "Yes no maybe something did come up or is"
        subtitles.updateSubtitles(position, frozen + partial)
        assertEquals(
            listOf(
                "switches Yes no maybe something did come up or is",
            ),
            subtitles.lines,
        )
        partial = "Yes no maybe something"
        subtitles.updateSubtitles(position, frozen + partial)
        assertEquals(
            listOf(
                "switches Yes no maybe something",
            ),
            subtitles.lines,
        )
        partial = "Yes no maybe something did come up or is this just another"
        subtitles.updateSubtitles(position, frozen + partial)
        assertEquals(
            listOf(
                "switches Yes no maybe something did come up or is",
                "this just another",
            ),
            subtitles.lines,
        )
    }

    @Test
    fun speechToTextEmptyInput() {
        val subtitles = Subtitles(null)
        subtitles.updateSubtitles(0, "")
        assertTrue(subtitles.lines.isEmpty())
    }

    @Test
    fun speechToTextJumping() {
        val subtitles = Subtitles(null)
        subtitles.updateSubtitles(0, "")
        assertTrue(subtitles.lines.isEmpty())
        subtitles.updateSubtitles(-5, "123")
        assertTrue(subtitles.lines.isEmpty())
        subtitles.updateSubtitles(0, "123")
        assertEquals(listOf("123"), subtitles.lines)
        subtitles.updateSubtitles(100, "123")
        assertEquals(listOf("123"), subtitles.lines)
        subtitles.updateSubtitles(10, "123")
        assertEquals(listOf("123"), subtitles.lines)
    }
}
