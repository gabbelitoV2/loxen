package com.moblin.android.various.model.chat

import com.moblin.android.various.ChatPost
import com.moblin.android.various.MainTimer
import com.moblin.android.various.model.maximumNumberOfChatMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatProvider {
    var newPosts: ArrayDeque<ChatPost> = ArrayDeque()
    var pausedPosts: ArrayDeque<ChatPost> = ArrayDeque()
    val posts = MutableStateFlow<List<ChatPost>>(emptyList())
    val pausedPostsCount = MutableStateFlow(0)
    val paused = MutableStateFlow(false)
    val moreThanOneStreamingPlatform = MutableStateFlow(false)
    val interactiveChat = MutableStateFlow(false)
    val triggerScrollToBottom = MutableStateFlow(false)
    val showLabel = MutableStateFlow(false)
    private val hideLabelTimer = MainTimer()

    fun showLabelForAWhile() {
        showLabel.value = true
        hideLabelTimer.startSingleShot(5.0) {
            showLabel.value = false
        }
    }

    fun appendMessage(post: ChatPost) {
        if (paused.value) {
            if (pausedPosts.size > 2 * maximumNumberOfChatMessages - 1) {
                pausedPosts.removeFirst()
            }
            pausedPosts.addLast(post)
        } else {
            newPosts.addLast(post)
        }
    }

    fun deleteMessage(messageId: String) {
        for (post in newPosts) {
            if (post.messageId == messageId) {
                post.state.deleted.value = true
            }
        }
        for (post in pausedPosts) {
            if (post.messageId == messageId) {
                post.state.deleted.value = true
            }
        }
        for (post in posts.value) {
            if (post.messageId == messageId) {
                post.state.deleted.value = true
            }
        }
    }

    fun deleteUser(userId: String) {
        for (post in newPosts) {
            if (post.userId == userId) {
                post.state.deleted.value = true
            }
        }
        for (post in pausedPosts) {
            if (post.userId == userId) {
                post.state.deleted.value = true
            }
        }
        for (post in posts.value) {
            if (post.userId == userId) {
                post.state.deleted.value = true
            }
        }
    }

    fun update() {
        if (paused.value) {
            if (pausedPosts.size != pausedPostsCount.value) {
                pausedPostsCount.value = pausedPosts.size
            }
        } else {
            while (newPosts.isNotEmpty()) {
                appendPost(newPosts.removeFirst())
            }
        }
    }

    fun pause() {
        paused.value = true
        pausedPostsCount.value = 0
        pausedPosts.clear()
        while (newPosts.isNotEmpty()) {
            appendMessage(newPosts.removeFirst())
        }
    }

    fun endReachedWhenPaused() {
        while (pausedPosts.isNotEmpty()) {
            appendPost(pausedPosts.removeFirst())
        }
        paused.value = false
    }

    private fun appendPost(post: ChatPost) {
        val updated = posts.value.toMutableList()
        if (updated.size > maximumNumberOfChatMessages - 1) {
            updated.removeAt(0)
        }
        updated.add(post)
        posts.value = updated
    }
}
