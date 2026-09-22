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
    private val _posts = MutableStateFlow<List<ChatPost>>(emptyList())
    val posts: StateFlow<List<ChatPost>> = _posts.asStateFlow()
    private val _pausedPostsCount = MutableStateFlow(0)
    val pausedPostsCount: StateFlow<Int> = _pausedPostsCount.asStateFlow()
    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()
    private val maximumNumberOfMessages: Int
    private val _moreThanOneStreamingPlatform = MutableStateFlow(false)
    val moreThanOneStreamingPlatform: StateFlow<Boolean> = _moreThanOneStreamingPlatform.asStateFlow()
    private val _interactiveChat = MutableStateFlow(false)
    val interactiveChat: StateFlow<Boolean> = _interactiveChat.asStateFlow()
    private val _triggerScrollToBottom = MutableStateFlow(false)
    val triggerScrollToBottom: StateFlow<Boolean> = _triggerScrollToBottom.asStateFlow()
    private val _showLabel = MutableStateFlow(false)
    val showLabel: StateFlow<Boolean> = _showLabel.asStateFlow()
    private val hideLabelTimer = MainTimer()

    init {
        this.maximumNumberOfMessages = maximumNumberOfMessages
    }

    fun showLabelForAWhile() {
        _showLabel.value = true
        hideLabelTimer.startSingleShot(5.0) {
            _showLabel.value = false
        }
    }

    fun appendMessage(post: ChatPost) {
        if (_paused.value) {
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
        if (_paused.value) {
            val count = max(pausedPosts.size - 1, 0)
            if (count != _pausedPostsCount.value) {
                _pausedPostsCount.value = count
            }
        } else {
            val updated = _posts.value.toMutableList()
            while (newPosts.isNotEmpty()) {
                val post = newPosts.removeFirst()
                if (updated.size > maximumNumberOfMessages - 1) {
                    updated.removeAt(updated.size - 1)
                }
                updated.add(0, post)
            }
            _posts.value = updated
        }
    }

    fun pause(redLine: ChatPost) {
        _paused.value = true
        _pausedPostsCount.value = 0
        pausedPosts.clear()
        pausedPosts.addLast(redLine)
        while (newPosts.isNotEmpty()) {
            appendMessage(newPosts.removeFirst())
        }
    }

    fun endReachedWhenPaused() {
        val updated = _posts.value.toMutableList()
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
        _posts.value = updated
        _paused.value = false
    }
}
