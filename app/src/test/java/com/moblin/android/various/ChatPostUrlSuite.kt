package com.moblin.android.various

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatPostUrlSuite {
    private val moving = "https://emotes.example.com/moving.gif"
    private val still = "https://emotes.example.com/still.png"

    @Test
    fun prefersMovingWhenAnimated() {
        val url = ChatPostUrl(moving, still)
        assertEquals(moving, url.url(animated = true))
        assertEquals(still, url.url(animated = false))
    }

    @Test
    fun fallsBackToTheOtherOne() {
        assertEquals(still, ChatPostUrl(null, still).url(animated = true))
        assertEquals(moving, ChatPostUrl(moving, null).url(animated = false))
        assertNull(ChatPostUrl(null, null).url(animated = true))
    }
}
