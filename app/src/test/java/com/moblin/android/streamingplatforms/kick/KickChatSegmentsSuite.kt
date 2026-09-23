package com.moblin.android.streamingplatforms.kick

import com.moblin.android.emoteNames
import com.moblin.android.makeEmotes
import com.moblin.android.texts
import com.moblin.android.various.ChatPostSegment
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KickChatSegmentsSuite {
    private fun createSegments(message: String, emotes: List<String> = emptyList()): List<ChatPostSegment> {
        val id = AtomicInteger(0)
        return createKickSegments(
            message = message,
            emotesManager = makeEmotes(emotes),
            id = id,
        )
    }

    @Test
    fun noEmotes() {
        val segments = createSegments("hello world")
        assertEquals(listOf<String?>("hello ", "world "), texts(segments))
        assertEquals(listOf<String?>(null, null), emoteNames(segments))
    }

    @Test
    fun emoteInTheMiddle() {
        val segments = createSegments("hey [emote:37226:KEKW] there")
        assertEquals(listOf<String?>("hey ", null, "there "), texts(segments))
        assertEquals(listOf<String?>(null, "fullsize", null), emoteNames(segments))
        assertEquals(
            "https://files.kick.com/emotes/37226/fullsize",
            segments[1].url?.still?.toString(),
        )
    }

    @Test
    fun emoteAtStartAndEnd() {
        val segments = createSegments("[emote:1:A] hi [emote:2:B]")
        assertEquals(listOf<String?>(null, "hi ", null), texts(segments))
        assertEquals(
            listOf(
                "https://files.kick.com/emotes/1/fullsize",
                "https://files.kick.com/emotes/2/fullsize",
            ),
            segments.mapNotNull { it.url?.still?.toString() },
        )
    }

    @Test
    fun consecutiveEmotes() {
        val segments = createSegments("[emote:1:A][emote:2:B]")
        assertEquals(listOf<String?>(null, null), texts(segments))
        assertEquals(2, segments.size)
    }

    @Test
    fun onlyEmote() {
        val segments = createSegments("[emote:1:A]")
        assertEquals(listOf<String?>(null), texts(segments))
    }

    @Test
    fun malformedEmoteIsPlainText() {
        val segments = createSegments("[emote:abc:A] hi")
        assertEquals(listOf<String?>("[emote:abc:A] ", "hi "), texts(segments))
        assertEquals(listOf<String?>(null, null), emoteNames(segments))
    }

    @Test
    fun thirdPartyEmotesAroundKickEmote() {
        val segments = createSegments("LUL [emote:1:A] LUL", emotes = listOf("LUL"))
        assertEquals(listOf<String?>("", "", null, "", ""), texts(segments))
        assertEquals(listOf<String?>("LUL", null, "fullsize", "LUL", null), emoteNames(segments))
    }

    @Test
    fun idsAreUnique() {
        val segments = createSegments("a [emote:1:A] b LUL c", emotes = listOf("LUL"))
        assertEquals(segments.size, segments.map { it.id }.toSet().size)
    }
}
