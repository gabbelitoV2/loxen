package com.moblin.android.integrations.emotes

import com.moblin.android.emoteNames
import com.moblin.android.makeEmotes
import com.moblin.android.spokenEmoteNames
import com.moblin.android.texts
import com.moblin.android.various.ChatPostSegment
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EmotesSuite {
    private fun createSegments(text: String, emotes: List<String> = emptyList()): List<ChatPostSegment> {
        return makeEmotes(emotes).createSegments(text, AtomicInteger(0))
    }

    @Test
    fun plainText() {
        val segments = createSegments("hello world")
        assertEquals(listOf("hello ", "world "), texts(segments))
        assertEquals(listOf<String?>(null, null), emoteNames(segments))
    }

    @Test
    fun emptyText() {
        assertTrue(createSegments("").isEmpty())
    }

    @Test
    fun whitespaceOnlyText() {
        assertTrue(createSegments("   ").isEmpty())
    }

    @Test
    fun surroundingAndRepeatedWhitespaceIsCollapsed() {
        val segments = createSegments("  hello   world  ")
        assertEquals(listOf("hello ", "world "), texts(segments))
    }

    @Test
    fun newlinesSeparateWords() {
        val segments = createSegments("hello\nworld")
        assertEquals(listOf("hello ", "world "), texts(segments))
    }

    @Test
    fun emoteInTheMiddle() {
        val segments = createSegments("hello Kappa world", listOf("Kappa"))
        assertEquals(listOf("hello ", "", "", "world "), texts(segments))
        assertEquals(listOf(null, "Kappa", null, null), emoteNames(segments))
        assertEquals(listOf(null, "Kappa", null, null), spokenEmoteNames(segments))
    }

    @Test
    fun emoteFirstAndLast() {
        val segments = createSegments("Kappa hi LUL", listOf("Kappa", "LUL"))
        assertEquals(listOf("", "", "hi ", "", ""), texts(segments))
        assertEquals(listOf("Kappa", null, null, "LUL", null), emoteNames(segments))
    }

    @Test
    fun consecutiveEmotes() {
        val segments = createSegments("Kappa LUL", listOf("Kappa", "LUL"))
        assertEquals(listOf("Kappa", null, "LUL", null), emoteNames(segments))
        assertEquals(listOf("Kappa", null, "LUL", null), spokenEmoteNames(segments))
    }

    @Test
    fun emoteLookupIsCaseSensitive() {
        val segments = createSegments("kappa", listOf("Kappa"))
        assertEquals(listOf("kappa "), texts(segments))
        assertEquals(listOf<String?>(null), emoteNames(segments))
    }

    @Test
    fun emoteMustBeAWholeWord() {
        val segments = createSegments("xKappa Kappa!", listOf("Kappa"))
        assertEquals(listOf("xKappa ", "Kappa! "), texts(segments))
        assertEquals(listOf<String?>(null, null), emoteNames(segments))
    }

    @Test
    fun idsAreUnique() {
        val segments = createSegments("a Kappa b LUL c", listOf("Kappa", "LUL"))
        assertEquals(segments.size, segments.map { it.id }.toSet().size)
    }

    @Test
    fun idsContinueFromCaller() {
        val id = AtomicInteger(7)
        val segments = makeEmotes(emptyList()).createSegments("a b", id)
        assertEquals(listOf(7, 8), segments.map { it.id })
        assertEquals(9, id.get())
    }
}
