package com.moblin.android.view.stream.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.moblin.android.LocalModel
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.common.various.color
import com.moblin.android.localized
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.ChatPost
import com.moblin.android.various.ChatPostState
import com.moblin.android.various.MainTimer
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.chat.ChatProvider
import com.moblin.android.various.model.endOfChatReachedWhenPaused
import com.moblin.android.various.model.pauseChat
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.view.controlbar.quickbutton.chat.quickButtonChatLinkConfirmation
import com.moblin.android.view.stream.ChatInfo
import com.moblin.android.view.utils.ChatActionButtonsView
import com.moblin.android.view.utils.ChatLineStyle
import com.moblin.android.view.utils.ChatLineView
import java.util.UUID
import kotlinx.coroutines.launch

private fun makeChatLineStyle(chat: SettingsChat, interactive: Boolean): ChatLineStyle {
    return ChatLineStyle(
        fontSize = chat.fontSize.toFloat(),
        borderColor = if (chat.shadowColorEnabled) chat.shadowColor.color() else null,
        borderWidth = 1.5f,
        backgroundColor = if (chat.backgroundColorEnabled) {
            chat.backgroundColor.color().copy(alpha = 0.6f)
        } else {
            null
        },
        timestampColor = if (chat.timestampColorEnabled) chat.timestampColor.color() else null,
        messageColor = chat.messageColor.color(),
        meInUsernameColor = chat.meInUsernameColor,
        boldUsername = chat.boldUsername,
        boldMessage = chat.boldMessage,
        badges = chat.badges,
        sharedChatIcons = chat.sharedChatIcons,
        animatedEmotes = chat.animatedEmotes,
        bigGifScale = chat.bigGifScale,
        linkify = interactive,
        highlightSymbolColor = Color.White,
        highlightDefaultColor = chat.messageColorColor,
        nicknames = chat.nicknames,
        displayStyle = chat.displayStyle,
    )
}

private class PreviousValue<T>(var value: T)

@Composable
private fun <T> OnChange(value: T, action: suspend () -> Unit) {
    val previous = remember { PreviousValue(value) }
    LaunchedEffect(value) {
        if (previous.value != value) {
            previous.value = value
            action()
        }
    }
}

@Composable
private fun HighlightMessageView(
    deleted: Boolean,
    style: ChatLineStyle,
    highlight: ChatHighlight,
    interactive: Boolean,
    linkUrl: String?,
    onLinkUrlChange: (String?) -> Unit,
) {
    val onTap: ((String?) -> Unit)? = onTap(interactive, onLinkUrlChange)
    val titleSegments = highlight.titleSegments
    if (titleSegments != null) {
        ChatLineView(
            content = style.makeHighlightContent(
                highlight = highlight,
                titleSegments = titleSegments,
                deleted = deleted,
            ),
            onTap = onTap,
        )
    }
}

private fun onTap(interactive: Boolean, onLinkUrlChange: (String?) -> Unit): ((String?) -> Unit)? {
    return if (interactive) {
        onLinkUrlChange
    } else {
        null
    }
}

@Composable
private fun HighlightImageView(style: ChatLineStyle, highlight: ChatHighlight) {
    val content = style.copy(backgroundColor = null).makeHighlightImageContent(highlight = highlight)
    ChatLineView(content = content)
}

@Composable
private fun LineView(
    deleted: Boolean,
    post: ChatPost,
    style: ChatLineStyle,
    platform: Boolean,
    interactive: Boolean,
    selectedPost: ChatPost?,
    onSelectedPostChange: (ChatPost?) -> Unit,
    linkUrl: String?,
    onLinkUrlChange: (String?) -> Unit,
) {
    val onTap: ((String?) -> Unit)? = if (interactive) {
        { url: String? ->
            if (url != null) {
                onLinkUrlChange(url)
            } else {
                onSelectedPostChange(post)
            }
        }
    } else {
        null
    }
    ChatLineView(
        content = style.makeContent(post = post, platform = platform, deleted = deleted),
        onTap = onTap,
    )
}

private val startId: UUID = UUID.randomUUID()

@Composable
private fun PostView(
    chatSettings: SettingsChat,
    style: ChatLineStyle,
    moreThanOneStreamingPlatform: Boolean,
    post: ChatPost,
    state: ChatPostState,
    width: Float,
    interactive: Boolean,
    selectedPost: ChatPost?,
    onSelectedPostChange: (ChatPost?) -> Unit,
    linkUrl: String?,
    onLinkUrlChange: (String?) -> Unit,
) {
    val deleted by state.deleted.collectAsState()
    if (post.user != null) {
        if (!deleted || chatSettings.showDeletedMessages) {
            val highlight = post.highlight
            if (highlight != null) {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(highlight.barColor),
                    )
                    if (chatSettings.compactEvents && highlight.titleSegments != null) {
                        HighlightImageView(style = style, highlight = highlight)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        if (!chatSettings.compactEvents) {
                            HighlightMessageView(
                                deleted = deleted,
                                style = style,
                                highlight = highlight,
                                interactive = interactive,
                                linkUrl = linkUrl,
                                onLinkUrlChange = onLinkUrlChange,
                            )
                        }
                        LineView(
                            deleted = deleted,
                            post = post,
                            style = style,
                            platform = moreThanOneStreamingPlatform,
                            interactive = interactive,
                            selectedPost = selectedPost,
                            onSelectedPostChange = onSelectedPostChange,
                            linkUrl = linkUrl,
                            onLinkUrlChange = onLinkUrlChange,
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.padding(start = 3.dp)) {
                    LineView(
                        deleted = deleted,
                        post = post,
                        style = style,
                        platform = moreThanOneStreamingPlatform,
                        interactive = interactive,
                        selectedPost = selectedPost,
                        onSelectedPostChange = onSelectedPostChange,
                        linkUrl = linkUrl,
                        onLinkUrlChange = onLinkUrlChange,
                    )
                }
            }
        }
    } else {
        Box(modifier = Modifier.padding(2.dp)) {
            Box(
                modifier = Modifier
                    .width(width.dp)
                    .height(1.5.dp)
                    .background(Color.Red),
            )
        }
    }
}

@Composable
private fun MessagesView(
    model: Model = LocalModel.current,
    chatSettings: SettingsChat,
    chat: ChatProvider,
    width: Float,
    interactive: Boolean,
    selectedPost: ChatPost?,
    onSelectedPostChange: (ChatPost?) -> Unit,
    linkUrl: String?,
    onLinkUrlChange: (String?) -> Unit,
) {
    val rotation = chatSettings.getRotation()
    val scaleX = chatSettings.getScaleX()
    val style = makeChatLineStyle(chat = chatSettings, interactive = interactive)
    val posts by chat.posts.collectAsState()
    val interactiveChat by chat.interactiveChat.collectAsState()
    val triggerScrollToBottom by chat.triggerScrollToBottom.collectAsState()
    val moreThanOneStreamingPlatform by chat.moreThanOneStreamingPlatform.collectAsState()

    val listState = rememberLazyListState()
    val mainScope = rememberCoroutineScope()

    fun tryPause() {
        if (!chat.interactiveChat.value) {
            return
        }
        if (!chat.paused.value) {
            if (chat.posts.value.isNotEmpty()) {
                model.pauseChat(chat = chat)
            }
        }
    }

    fun tryUnpause() {
        if (!chat.interactiveChat.value) {
            return
        }
        if (chat.paused.value) {
            model.endOfChatReachedWhenPaused(chat = chat)
        }
    }

    OnChange(interactiveChat) {
        listState.scrollToItem(0)
    }
    OnChange(triggerScrollToBottom) {
        listState.scrollToItem(0)
    }
    LaunchedEffect(Unit) {
        mainScope.launch {
            tryUnpause()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .width(width.dp)
            .fillMaxHeight()
            .graphicsLayer(
                rotationZ = rotation.toFloat(),
                scaleX = (scaleX * chatSettings.isMirrored()).toFloat(),
                scaleY = 1f,
                transformOrigin = TransformOrigin.Center,
            ),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        userScrollEnabled = interactiveChat,
    ) {
        item(key = startId.toString()) {
            Box(
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth(),
            )
            LaunchedEffect(Unit) {
                mainScope.launch {
                    tryUnpause()
                }
            }
            DisposableEffect(Unit) {
                onDispose {
                    tryPause()
                }
            }
        }
        items(posts, key = { it.id.toString() }) { post ->
            Box(
                modifier = Modifier.graphicsLayer(
                    rotationZ = rotation.toFloat(),
                    scaleX = scaleX.toFloat(),
                    scaleY = 1f,
                    transformOrigin = TransformOrigin.Center,
                ),
            ) {
                PostView(
                    chatSettings = chatSettings,
                    style = style,
                    moreThanOneStreamingPlatform = moreThanOneStreamingPlatform,
                    post = post,
                    state = post.state,
                    width = width,
                    interactive = interactive && interactiveChat,
                    selectedPost = selectedPost,
                    onSelectedPostChange = onSelectedPostChange,
                    linkUrl = linkUrl,
                    onLinkUrlChange = onLinkUrlChange,
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(0.dp))
        }
    }
}

@Composable
private fun ChatPausedView(chat: ChatProvider, alerts: Boolean) {
    val paused by chat.paused.collectAsState()
    val pausedPostsCount by chat.pausedPostsCount.collectAsState()

    fun message(): String {
        return if (alerts) {
            localized("Chat paused: $pausedPostsCount new alerts")
        } else {
            localized("Chat paused: $pausedPostsCount new messages")
        }
    }

    if (paused) {
        Box(modifier = Modifier.padding(2.dp)) {
            ChatInfo(message = message())
        }
    }
}

private val separatorHeight = 2.0

@Composable
private fun ChatLabelView(chat: ChatProvider, message: String, alignment: Alignment) {
    val showLabel by chat.showLabel.collectAsState()
    if (showLabel) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = alignment,
        ) {
            Text(
                text = message,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(backgroundColor)
                    .padding(vertical = 5.dp, horizontal = 10.dp),
            )
        }
    }
}

private class Triangle : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        path.moveTo(0f, 0f)
        path.lineTo(size.width, size.height / 2f)
        path.lineTo(0f, size.height)
        path.close()
        return Outline.Generic(path)
    }
}

@Composable
private fun SeparatorView(
    chatSettings: SettingsChat,
    activityFeed: ChatProvider,
    width: Float,
    height: Float,
    activityFeedHeight: Float,
    draggedActivityFeedHeight: Double?,
    onDraggedActivityFeedHeightChange: (Double?) -> Unit,
) {
    val showLabel by activityFeed.showLabel.collectAsState()
    val posts by activityFeed.posts.collectAsState()
    val pausedPostsCount by activityFeed.pausedPostsCount.collectAsState()

    var dragStartActivityFeedHeight by remember { mutableStateOf<Double?>(null) }
    var hasNewPosts by remember { mutableStateOf(false) }
    val hideNewPostsTimer = remember { MainTimer() }
    val currentChatSettings by rememberUpdatedState(chatSettings)
    val currentHeight by rememberUpdatedState(height)
    val currentOnDraggedActivityFeedHeightChange by rememberUpdatedState(onDraggedActivityFeedHeightChange)

    fun handleNewPost() {
        if (activityFeedHeight != 0f) {
            return
        }
        hasNewPosts = true
        hideNewPostsTimer.startSingleShot(timeout = 60.0) {
            hasNewPosts = false
        }
    }

    fun clearNewPosts() {
        hideNewPostsTimer.stop()
        hasNewPosts = false
    }

    OnChange(posts.firstOrNull()?.id) {
        handleNewPost()
    }
    OnChange(pausedPostsCount) {
        if (pausedPostsCount > 0) {
            handleNewPost()
        }
    }
    OnChange(activityFeedHeight) {
        clearNewPosts()
    }
    DisposableEffect(Unit) {
        onDispose {
            clearNewPosts()
        }
    }

    Box(
        modifier = Modifier
            .zIndex(1f)
            .width(width.dp)
            .height(separatorHeight.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (showLabel || activityFeedHeight > 0f) {
            Box(
                modifier = Modifier
                    .width(width.dp)
                    .height(separatorHeight.dp)
                    .background(Color.White),
            )
        }
        Row(
            modifier = Modifier.wrapContentHeight(align = Alignment.CenterVertically, unbounded = true),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 10.dp)
                    .background(Color.White, Triangle()),
            )
            if (hasNewPosts) {
                Text(
                    text = localized("New"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(vertical = 2.dp, horizontal = 6.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(44.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var translation = 0f
                        var dragged: Double? = null

                        fun onChanged() {
                            val start = dragStartActivityFeedHeight ?: currentChatSettings.activityFeedHeight
                            dragStartActivityFeedHeight = start
                            val value = (start + translation / density / currentHeight).coerceIn(0.0, 1.0)
                            dragged = value
                            currentOnDraggedActivityFeedHeightChange(value)
                        }

                        down.consume()
                        onChanged()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            translation += change.positionChangeIgnoreConsumed().y
                            change.consume()
                            onChanged()
                        }
                        dragStartActivityFeedHeight = null
                        val value = dragged
                        if (value != null) {
                            currentChatSettings.activityFeedHeight = value
                        }
                        currentOnDraggedActivityFeedHeightChange(null)
                    }
                },
        )
    }
}

@Composable
fun StreamOverlayChatView(
    model: Model = LocalModel.current,
    database: Database,
    chatSettings: SettingsChat,
    chat: ChatProvider,
    chatActivityFeed: ChatProvider,
    fullSize: Boolean,
) {
    val chatPhone by model.show.chatPhone.collectAsState()
    var draggedAlertsHeight by remember { mutableStateOf<Double?>(null) }
    var selectedPost by remember { mutableStateOf<ChatPost?>(null) }
    var linkUrl by remember { mutableStateOf<String?>(null) }

    fun isInteractive(): Boolean {
        return chatPhone
    }

    fun heightFactor(): Float {
        return if (fullSize) {
            1f
        } else if (chatPhone) {
            0.96f
        } else {
            chatSettings.height.toFloat()
        }
    }

    fun widthFactor(): Float {
        return if (fullSize || chatPhone) {
            1f
        } else {
            chatSettings.width.toFloat()
        }
    }

    BoxWithConstraints {
        val width = maxWidth.value * widthFactor()
        val height = maxHeight.value * heightFactor()
        Box(
            modifier = Modifier
                .width(width.dp)
                .fillMaxHeight(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.weight(1f))
                if (chatSettings.activityFeed) {
                    val splitHeight = height - separatorHeight.toFloat()
                    val alertsHeight = splitHeight *
                        (draggedAlertsHeight ?: chatSettings.activityFeedHeight).toFloat()
                    Box(modifier = Modifier.height(alertsHeight.coerceAtLeast(0f).dp)) {
                        MessagesView(
                            model = model,
                            chatSettings = chatSettings,
                            chat = chatActivityFeed,
                            width = width,
                            interactive = isInteractive(),
                            selectedPost = selectedPost,
                            onSelectedPostChange = { selectedPost = it },
                            linkUrl = linkUrl,
                            onLinkUrlChange = { linkUrl = it },
                        )
                        if (alertsHeight > 10f) {
                            Box(modifier = Modifier.matchParentSize()) {
                                ChatPausedView(chat = chatActivityFeed, alerts = true)
                            }
                        }
                        if (alertsHeight > 40f) {
                            Box(modifier = Modifier.matchParentSize()) {
                                ChatLabelView(
                                    chat = chatActivityFeed,
                                    message = localized("Activity feed"),
                                    alignment = Alignment.BottomCenter,
                                )
                            }
                        }
                    }
                    SeparatorView(
                        chatSettings = chatSettings,
                        activityFeed = chatActivityFeed,
                        width = width,
                        height = splitHeight,
                        activityFeedHeight = alertsHeight,
                        draggedActivityFeedHeight = draggedAlertsHeight,
                        onDraggedActivityFeedHeightChange = { draggedAlertsHeight = it },
                    )
                    Box(modifier = Modifier.height((splitHeight - alertsHeight).coerceAtLeast(0f).dp)) {
                        MessagesView(
                            model = model,
                            chatSettings = chatSettings,
                            chat = chat,
                            width = width,
                            interactive = isInteractive(),
                            selectedPost = selectedPost,
                            onSelectedPostChange = { selectedPost = it },
                            linkUrl = linkUrl,
                            onLinkUrlChange = { linkUrl = it },
                        )
                        Box(modifier = Modifier.matchParentSize()) {
                            ChatPausedView(chat = chat, alerts = false)
                        }
                        if (splitHeight - alertsHeight > 40f) {
                            Box(modifier = Modifier.matchParentSize()) {
                                ChatLabelView(
                                    chat = chat,
                                    message = localized("Chat"),
                                    alignment = Alignment.TopCenter,
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.height(height.coerceAtLeast(0f).dp)) {
                        MessagesView(
                            model = model,
                            chatSettings = chatSettings,
                            chat = chat,
                            width = width,
                            interactive = isInteractive(),
                            selectedPost = selectedPost,
                            onSelectedPostChange = { selectedPost = it },
                            linkUrl = linkUrl,
                            onLinkUrlChange = { linkUrl = it },
                        )
                        Box(modifier = Modifier.matchParentSize()) {
                            ChatPausedView(chat = chat, alerts = false)
                        }
                    }
                }
            }
            if (isInteractive()) {
                Box(modifier = Modifier.matchParentSize()) {
                    ChatActionButtonsView(
                        model = model,
                        style = makeChatLineStyle(chat = chatSettings, interactive = true),
                        selectedPost = selectedPost,
                        onSelectedPostChange = { selectedPost = it },
                        linkUrl = linkUrl,
                        onLinkUrlChange = { linkUrl = it },
                    )
                }
            }
        }
    }

    quickButtonChatLinkConfirmation(
        url = linkUrl,
        onUrlChange = { linkUrl = it },
    )
}
