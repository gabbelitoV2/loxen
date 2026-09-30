package com.moblin.android.various.model.chat

import com.moblin.android.common.various.RgbColor
import com.moblin.android.runMainTest
import com.moblin.android.various.ChatPost
import com.moblin.android.various.ChatPostState
import com.moblin.android.various.model.maximumNumberOfChatMessages
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatProviderSuite {
    private fun makePost(id: Int): ChatPost {
        return ChatPost(
            id = id,
            messageId = null,
            displayName = "user",
            user = "user",
            userId = null,
            userColor = RgbColor(red = 0, green = 0, blue = 0),
            userBadges = emptyList(),
            segments = emptyList(),
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

    @Test
    fun newestPostFirst() = runMainTest {
        val chat = ChatProvider()
        for (id in 0 until 3) {
            chat.appendMessage(post = makePost(id = id))
        }
        chat.update()
        assertEquals(listOf(2, 1, 0), chat.posts.value.map { it.id })
    }

    @Test
    fun dropsOldestWhenFull() = runMainTest {
        val chat = ChatProvider()
        for (id in 0 until maximumNumberOfChatMessages + 10) {
            chat.appendMessage(post = makePost(id = id))
        }
        chat.update()
        assertEquals(maximumNumberOfChatMessages, chat.posts.value.size)
        assertEquals(maximumNumberOfChatMessages + 9, chat.posts.value.firstOrNull()?.id)
        assertEquals(10, chat.posts.value.lastOrNull()?.id)
    }

    @Test
    fun pausedKeepsPostsUntilEndReached() = runMainTest {
        val chat = ChatProvider()
        chat.appendMessage(post = makePost(id = 0))
        chat.update()
        chat.pause()
        chat.appendMessage(post = makePost(id = 1))
        chat.appendMessage(post = makePost(id = 2))
        chat.update()
        assertTrue(chat.paused.value)
        assertEquals(2, chat.pausedPostsCount.value)
        assertEquals(listOf(0), chat.posts.value.map { it.id })
        chat.endReachedWhenPaused()
        assertFalse(chat.paused.value)
        assertEquals(listOf(2, 1, 0), chat.posts.value.map { it.id })
    }

    @Test
    fun pausedDropsOldestWhenFull() = runMainTest {
        val chat = ChatProvider()
        chat.pause()
        val count = 2 * maximumNumberOfChatMessages + 10
        for (id in 0 until count) {
            chat.appendMessage(post = makePost(id = id))
        }
        chat.update()
        assertEquals(2 * maximumNumberOfChatMessages, chat.pausedPostsCount.value)
        chat.endReachedWhenPaused()
        assertEquals(maximumNumberOfChatMessages, chat.posts.value.size)
        assertEquals(count - 1, chat.posts.value.firstOrNull()?.id)
        assertEquals(count - maximumNumberOfChatMessages, chat.posts.value.lastOrNull()?.id)
    }
}
