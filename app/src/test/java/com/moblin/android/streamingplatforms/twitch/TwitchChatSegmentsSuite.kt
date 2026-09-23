package com.moblin.android.streamingplatforms.twitch

import com.moblin.android.emoteNames
import com.moblin.android.makeEmotes
import com.moblin.android.texts
import com.moblin.android.various.ChatMessageEmote
import com.moblin.android.various.ChatPostSegment
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun makeTwitchEmote(name: String, range: IntRange): ChatMessageEmote {
    return ChatMessageEmote(
        url = "https://twitch.example.com/$name",
        stillUrl = "https://twitch.example.com/still/$name",
        range = range,
    )
}

private fun makeTwitchGif(name: String, range: IntRange): ChatMessageEmote {
    return ChatMessageEmote(
        url = "https://giphy.example.com/$name",
        range = range,
        isGif = true,
    )
}

private fun gifNames(segments: List<ChatPostSegment>): List<String?> {
    return segments.map { it.bigGifUrl?.moving?.substringAfterLast('/') }
}

private fun textFragment(text: String): TwitchEventSubMessageFragment {
    return TwitchEventSubMessageFragment(type = "text", text = text, emote = null)
}

private fun emoteFragment(text: String, id: String): TwitchEventSubMessageFragment {
    return TwitchEventSubMessageFragment(type = "emote",
                                         text = text,
                                         emote = TwitchEventSubMessageFragmentEmote(id = id))
}

private fun twitchEmoteIds(segments: List<ChatPostSegment>): List<String?> {
    return segments.map { segment ->
        val path = segment.url?.still?.let { runCatching { URI(it).path }.getOrNull() }
            ?: return@map null
        path.split("/").filter { it.isNotEmpty() }.drop(2).firstOrNull()
    }
}

@RunWith(RobolectricTestRunner::class)
class TwitchChatSegmentsSuite {
    private fun createSegments(text: String,
                              emotes: List<ChatMessageEmote>,
                              thirdParty: List<String> = listOf()): List<ChatPostSegment> {
        val id = AtomicInteger(0)
        return createTwitchSegments(text, emotes, makeEmotes(thirdParty), id)
    }

    private fun createSegments(fragments: List<TwitchEventSubMessageFragment>,
                              thirdParty: List<String> = listOf()): List<ChatPostSegment> {
        val id = AtomicInteger(0)
        return createTwitchSegments(fragments, makeEmotes(thirdParty), id)
    }

    @Test
    fun fragmentsWithoutEmotes() {
        val segments = createSegments(listOf(textFragment("hello world")))
        assertEquals(listOf<String?>("hello ", "world "), texts(segments))
        assertEquals(listOf<String?>(null, null), twitchEmoteIds(segments))
    }

    @Test
    fun fragmentsWithEmoteInTheMiddle() {
        val segments = createSegments(listOf(
            textFragment("hi "),
            emoteFragment("Kappa", id = "25"),
            textFragment(" lol"),
        ))
        assertEquals(listOf<String?>("hi ", null, "", "lol "), texts(segments))
        assertEquals(listOf<String?>(null, "25", null, null), twitchEmoteIds(segments))
    }

    @Test
    fun fragmentsWithAdjacentEmotes() {
        val segments = createSegments(listOf(
            emoteFragment("Kappa", id = "25"),
            textFragment(" "),
            emoteFragment("PogChamp", id = "305954156"),
        ))
        assertEquals(listOf<String?>(null, "", null, ""), texts(segments))
        assertEquals(listOf<String?>("25", null, "305954156", null), twitchEmoteIds(segments))
    }

    @Test
    fun fragmentsWithThirdPartyEmoteInText() {
        val segments = createSegments(listOf(textFragment("LUL "), emoteFragment("Kappa", id = "25")),
                                      thirdParty = listOf("LUL"))
        assertEquals(listOf<String?>("", "", null, ""), texts(segments))
        assertEquals("LUL", emoteNames(segments).first())
        assertEquals("25", twitchEmoteIds(segments)[2])
    }

    @Test
    fun fragmentsWithMentionAndCheermoteAreText() {
        val segments = createSegments(listOf(
            TwitchEventSubMessageFragment(type = "mention", text = "@Viewer", emote = null),
            textFragment(" "),
            TwitchEventSubMessageFragment(type = "cheermote", text = "Cheer100", emote = null),
        ))
        assertEquals(listOf<String?>("@Viewer ", "Cheer100 "), texts(segments))
        assertEquals(listOf<String?>(null, null), twitchEmoteIds(segments))
    }

    @Test
    fun fragmentIdsAreUnique() {
        val segments = createSegments(listOf(
            textFragment("a "),
            emoteFragment("Kappa", id = "25"),
            textFragment(" LUL b"),
        ), thirdParty = listOf("LUL"))
        assertEquals(segments.size, segments.map { it.id }.toSet().size)
    }

    @Test
    fun noEmotes() {
        val segments = createSegments("hello world", listOf<ChatMessageEmote>())
        assertEquals(listOf<String?>("hello ", "world "), texts(segments))
        assertEquals(listOf<String?>(null, null), emoteNames(segments))
    }

    @Test
    fun emoteInTheMiddle() {
        val segments = createSegments("hi Kappa lol", listOf(makeTwitchEmote("Kappa", 3..7)))
        assertEquals(listOf<String?>("hi ", null, "", "lol "), texts(segments))
        assertEquals(listOf<String?>(null, "Kappa", null, null), emoteNames(segments))
    }

    @Test
    fun emoteAtStart() {
        val segments = createSegments("Kappa lol", listOf(makeTwitchEmote("Kappa", 0..4)))
        assertEquals(listOf<String?>(null, "", "lol "), texts(segments))
        assertEquals(listOf<String?>("Kappa", null, null), emoteNames(segments))
    }

    @Test
    fun emoteAtEnd() {
        val segments = createSegments("lol Kappa", listOf(makeTwitchEmote("Kappa", 4..8)))
        assertEquals(listOf<String?>("lol ", null, ""), texts(segments))
        assertEquals(listOf<String?>(null, "Kappa", null), emoteNames(segments))
    }

    @Test
    fun onlyEmote() {
        val segments = createSegments("Kappa", listOf(makeTwitchEmote("Kappa", 0..4)))
        assertEquals(listOf<String?>(null, ""), texts(segments))
        assertEquals(listOf<String?>("Kappa", null), emoteNames(segments))
    }

    @Test
    fun adjacentEmotes() {
        val segments = createSegments("KappaLUL", listOf(
            makeTwitchEmote("Kappa", 0..4),
            makeTwitchEmote("LUL", 5..7),
        ))
        assertEquals(listOf<String?>(null, "", null, ""), texts(segments))
        assertEquals(listOf<String?>("Kappa", null, "LUL", null), emoteNames(segments))
    }

    @Test
    fun unsortedEmotes() {
        val segments = createSegments("Kappa a LUL", listOf(
            makeTwitchEmote("LUL", 8..10),
            makeTwitchEmote("Kappa", 0..4),
        ))
        assertEquals(listOf<String?>(null, "", "a ", null, ""), texts(segments))
        assertEquals(listOf<String?>("Kappa", null, null, "LUL", null), emoteNames(segments))
    }

    @Test
    fun emoteRangeOutsideText() {
        val segments = createSegments("hi", listOf(makeTwitchEmote("Kappa", 3..7)))
        assertEquals(listOf<String?>("hi "), texts(segments))
        assertEquals(listOf<String?>(null), emoteNames(segments))
    }

    @Test
    fun rangesAreCountedInUnicodeScalars() {
        val segments = createSegments("😀 Kappa", listOf(makeTwitchEmote("Kappa", 2..6)))
        assertEquals(listOf<String?>("😀 ", null, ""), texts(segments))
        assertEquals(listOf<String?>(null, "Kappa", null), emoteNames(segments))
    }

    @Test
    fun onlyGif() {
        val segments = createSegments("[Hello GIF by HULU]", listOf(makeTwitchGif("hello", 0..18)))
        assertEquals(listOf<String?>(null, ""), texts(segments))
        assertEquals(listOf<String?>(null, null), emoteNames(segments))
        assertEquals(listOf<String?>("hello", null), gifNames(segments))
        assertTrue(segments.mapNotNull { it.text }.joinToString("").isEmpty())
    }

    @Test
    fun gifAfterEmote() {
        val segments = createSegments("Kappa [gif]", listOf(
            makeTwitchGif("hello", 6..10),
            makeTwitchEmote("Kappa", 0..4),
        ))
        assertEquals(listOf<String?>(null, "", null, ""), texts(segments))
        assertEquals(listOf<String?>("Kappa", null, null, null), emoteNames(segments))
        assertEquals(listOf<String?>(null, null, "hello", null), gifNames(segments))
    }

    @Test
    fun thirdPartyEmotesAroundTwitchEmote() {
        val segments = createSegments("LUL Kappa LUL",
                                      listOf(makeTwitchEmote("Kappa", 4..8)),
                                      thirdParty = listOf("LUL"))
        assertEquals(listOf<String?>("", "", null, "", "", ""), texts(segments))
        assertEquals(listOf<String?>("LUL", null, "Kappa", null, "LUL", null), emoteNames(segments))
    }

    @Test
    fun idsAreUnique() {
        val segments = createSegments("a Kappa b LUL c",
                                      listOf(makeTwitchEmote("Kappa", 2..6)),
                                      thirdParty = listOf("LUL"))
        assertEquals(segments.size, segments.map { it.id }.toSet().size)
    }

    @Test
    fun rangeInsideWordKeepsSurroundingCharacters() {
        val segments = createSegments("hello", listOf(makeTwitchEmote("Kappa", 1..3)))
        assertEquals(listOf<String?>("h ", null, "", "o "), texts(segments))
        assertEquals(listOf<String?>(null, "Kappa", null, null), emoteNames(segments))
    }

    @Test
    fun overlappingRangesKeepTheFirstEmote() {
        val segments = createSegments("aa Kappa bb", listOf(
            makeTwitchEmote("Kappa", 3..7),
            makeTwitchEmote("Overlapping", 5..9),
        ))
        assertEquals(listOf<String?>("aa ", null, "", "bb "), texts(segments))
        assertEquals(listOf<String?>(null, "Kappa", null, null), emoteNames(segments))
    }
}
