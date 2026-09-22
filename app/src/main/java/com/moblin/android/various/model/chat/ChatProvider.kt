package com.moblin.android.various.model.chat

import com.moblin.android.various.ChatPost
import com.moblin.android.various.MainTimer
import kotlin.math.max
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatProvider(maximumNumberOfMessages: Int) {
    var newPosts: ArrayDeque<ChatPost> = ArrayDeque()
    var pausedPosts: ArrayDeque<ChatPost> = ArrayDeque()
    val posts = MutableStateFlow<List<ChatPost>>(emptyList())
    val pausedPostsCount = MutableStateFlow(0)
    val paused = MutableStateFlow(false)
    private val maximumNumberOfMessages: Int
    val moreThanOneStreamingPlatform = MutableStateFlow(false)
    val interactiveChat = MutableStateFlow(false)
    val triggerScrollToBottom = MutableStateFlow(false)
    val showLabel = MutableStateFlow(false)
    private val hideLabelTimer = MainTimer()

    init {
        this.maximumNumberOfMessages = maximumNumberOfMessages
    }

    fun showLabelForAWhile() {
        showLabel.value = true
        hideLabelTimer.startSingleShot(5.0) {
            showLabel.value = false
        }
    }

    fun appendMessage(post: ChatPost) {
        if (paused.value) {
            if (pausedPosts.size < 2 * maximumNumberOfMessages) {
                pausedPosts.addLast(post)
            }
        } else {
            newPosts.addLast(post)
        }
    }

    fun deleteMessage(messageId: String) {
        for (post in newPosts) {
            if (post.messageId == messageId) {
                post.state.deleted = true
            }
        }
        for (post in pausedPosts) {
            if (post.messageId == messageId) {
                post.state.deleted = true
            }
        }
        for (post in posts.value) {
            if (post.messageId == messageId) {
                post.state.deleted = true
            }
        }
    }

    fun deleteUser(userId: String) {
        for (post in newPosts) {
            if (post.userId == userId) {
                post.state.deleted = true
            }
        }
        for (post in pausedPosts) {
            if (post.userId == userId) {
                post.state.deleted = true
            }
        }
        for (post in posts.value) {
            if (post.userId == userId) {
                post.state.deleted = true
            }
        }
    }

    fun update() {
        if (paused.value) {
            val count = max(pausedPosts.size - 1, 0)
            if (count != pausedPostsCount.value) {
                pausedPostsCount.value = count
            }
        } else {
            val updated = posts.value.toMutableList()
            while (newPosts.isNotEmpty()) {
                val post = newPosts.removeFirst()
                if (updated.size > maximumNumberOfMessages - 1) {
                    updated.removeAt(updated.size - 1)
                }
                updated.add(0, post)
            }
            posts.value = updated
        }
    }

    fun pause(redLine: ChatPost) {
        paused.value = true
        pausedPostsCount.value = 0
        pausedPosts.clear()
        pausedPosts.addLast(redLine)
        while (newPosts.isNotEmpty()) {
            appendMessage(newPosts.removeFirst())
        }
    }

    fun endReachedWhenPaused() {
        val updated = posts.value.toMutableList()
        while (pausedPosts.isNotEmpty()) {
            val post = pausedPosts.removeFirst()
            if (post.isRedLine()) {
                if (updated.firstOrNull()?.isRedLine() == true) {
                    continue
                }
                if (pausedPosts.isEmpty()) {
                    continue
                }
            }
            if (updated.size > maximumNumberOfMessages - 1) {
                updated.removeAt(updated.size - 1)
            }
            updated.add(0, post)
        }
        posts.value = updated
        paused.value = false
    }
}
