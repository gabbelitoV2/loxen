package com.moblin.android.various

import com.moblin.android.common.various.RgbColor
import com.moblin.android.makeEmotes
import com.moblin.android.streamingplatforms.kick.createKickSegments
import com.moblin.android.streamingplatforms.twitch.createTwitchSegments
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatPostTextSuite {
    private fun makePost(segments: List<ChatPostSegment>): ChatPost {
        return ChatPost(
            id = 0,
            messageId = null,
            displayName = "user",
            user = "user",
            userId = null,
            userColor = RgbColor(red = 0, green = 0, blue = 0),
            userBadges = emptyList(),
            segments = segments,
            timestamp = "",
            timestampTime = Instant.now(),
            isAction = false,
            isSubscriber = false,
            bits = null,
            highlight = null,
            live = true,
            filter = null,
            platform = null,
            sourceChannelIcon = null,
            state = ChatPostState(),
        )
    }

    private fun makeEmote(name: String?): ChatPostEmote {
        val url = "https://emotes.example.com/emote"
        return ChatPostEmote(moving = url, still = url, name = name)
    }

    private fun makeTwitchPost(): ChatPost {
        val kappa = ChatMessageEmote(url = "https://twitch.example.com/Kappa", range = 3..7)
        val id = AtomicInteger(0)
        val segments = createTwitchSegments(
            text = "hi Kappa LUL lol",
            emotes = listOf(kappa),
            emotesManager = makeEmotes(listOf("LUL")),
            id = id,
        )
        return makePost(segments = segments)
    }

    @Test
    fun emotesAreSkippedByDefault() {
        val post = makeTwitchPost()
        assertEquals("hi lol", post.text())
    }

    @Test
    fun emoteNamesAreIncluded() {
        val post = makeTwitchPost()
        assertEquals("hi Kappa LUL lol", post.text(emoteNames = true))
    }

    @Test
    fun onlyEmotes() {
        val id = AtomicInteger(0)
        val segments = createKickSegments(
            message = "[emote:1:KEKW][emote:2:PogU]",
            emotesManager = makeEmotes(emptyList()),
            id = id,
        )
        val post = makePost(segments = segments)
        assertEquals("", post.text())
        assertEquals("KEKW PogU", post.text(emoteNames = true))
    }

    @Test
    fun bigGifNameIsIncluded() {
        val post = makePost(
            segments = listOf(
                ChatPostSegment(id = 0, text = "look "),
                ChatPostSegment(id = 1, bigGifUrl = makeEmote("Hello GIF")),
            ),
        )
        assertEquals("look Hello GIF", post.text(emoteNames = true))
    }

    @Test
    fun emoteWithoutNameIsSkipped() {
        val post = makePost(
            segments = listOf(
                ChatPostSegment(id = 0, text = "hi "),
                ChatPostSegment(id = 1, url = makeEmote(null)),
                ChatPostSegment(id = 2, text = "there "),
            ),
        )
        assertEquals("hi there", post.text(emoteNames = true))
    }
}
