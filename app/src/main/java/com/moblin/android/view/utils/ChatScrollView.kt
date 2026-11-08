package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.moblin.android.various.ChatPost
import com.moblin.android.various.model.chat.ChatProvider

private const val latestPostId = "latest"

@Composable
private fun LatestPostView(
    onLatestAppear: () -> Unit,
    onLatestDisappear: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onLatestAppear()
    }
    DisposableEffect(Unit) {
        onDispose {
            onLatestDisappear()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp),
    )
}

@Composable
fun ChatScrollView(
    chat: ChatProvider,
    newMessagesAtTop: Boolean,
    mirrored: Boolean,
    onLatestAppear: () -> Unit = {},
    onLatestDisappear: () -> Unit = {},
    row: @Composable (ChatPost) -> Unit,
) {
    val posts by chat.posts.collectAsState()
    val interactiveChat by chat.interactiveChat.collectAsState()
    val triggerScrollToBottom by chat.triggerScrollToBottom.collectAsState()
    val listState = rememberLazyListState()

    var previousInteractiveChat by remember { mutableStateOf(interactiveChat) }
    var previousTriggerScrollToBottom by remember { mutableStateOf(triggerScrollToBottom) }

    LaunchedEffect(interactiveChat) {
        if (interactiveChat != previousInteractiveChat) {
            previousInteractiveChat = interactiveChat
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(triggerScrollToBottom) {
        if (triggerScrollToBottom != previousTriggerScrollToBottom) {
            previousTriggerScrollToBottom = triggerScrollToBottom
            listState.scrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .chatScrollEdgeEffectHidden()
            .graphicsLayer {
                scaleX = if (mirrored) -1f else 1f
            },
        reverseLayout = !newMessagesAtTop,
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        item(key = latestPostId) {
            LatestPostView(
                onLatestAppear = onLatestAppear,
                onLatestDisappear = onLatestDisappear,
            )
        }
        items(posts.reversed(), key = { it.id }) { post ->
            row(post)
        }
    }
}

private fun Modifier.chatScrollEdgeEffectHidden(): Modifier = this
