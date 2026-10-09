package com.moblin.android.various

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatPostEmoteSuite {
    private val moving = "https://emotes.example.com/moving.gif"
    private val still = "https://emotes.example.com/still.png"

    @Test
    fun prefersMovingWhenAnimated() {
        val emote = ChatPostEmote(moving = moving, still = still)
        assertEquals(moving, emote.url(animated = true))
        assertEquals(still, emote.url(animated = false))
    }

    @Test
    fun fallsBackToTheOtherOne() {
        assertEquals(still, ChatPostEmote(moving = null, still = still).url(animated = true))
        assertEquals(moving, ChatPostEmote(moving = moving, still = null).url(animated = false))
        assertNull(ChatPostEmote(moving = null, still = null).url(animated = true))
    }
}
