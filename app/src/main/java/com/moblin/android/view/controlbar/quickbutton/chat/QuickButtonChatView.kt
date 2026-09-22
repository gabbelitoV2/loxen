package com.moblin.android.view.controlbar.quickbutton.chat

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.view.stream.ChatInfo
import com.moblin.android.view.utils.BorderlessButtonView
import com.moblin.android.view.utils.ChatActionButtonsView
import com.moblin.android.view.utils.ChatLineContent
import com.moblin.android.view.utils.ChatLineStyle
import com.moblin.android.view.utils.ChatLineView
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.localized
import com.moblin.android.various.model.BannersView
import com.moblin.android.various.model.ChatPost
import com.moblin.android.various.model.ChatPostState
import com.moblin.android.various.model.ChatProvider
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Orientation
import com.moblin.android.various.model.QuickButtonChat
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.various.settings.SettingsChatPredefinedMessage
import com.moblin.android.various.settings.SettingsChatPredefinedMessagesFilter
import com.moblin.android.various.settings.SettingsStream
import kotlinx.coroutines.launch
import java.net.URL
import java.util.UUID
import com.moblin.android.localized
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private fun makeChatLineStyle(chat: SettingsChat): ChatLineStyle =
    ChatLineStyle(
        fontSize = chat.fontSize.sp,
        timestampColor = if (chat.timestampColorEnabled) AndroidColor.GRAY else null,
        boldUsername = true,
        badges = chat.badges,
        sharedChatIcons = chat.sharedChatIcons,
        animatedEmotes = chat.animatedEmotes,
        bigGifScale = chat.bigGifScale,
        linkify = true,
        nicknames = chat.nicknames,
        displayStyle = chat.displayStyle,
    )

@Composable
private fun HighlightMessageView(
    deleted: Boolean,
    style: ChatLineStyle,
    highlight: com.moblin.android.various.model.ChatHighlight,
    onLinkUrl: (URL?) -> Unit,
) {
    val titleSegments = highlight.titleSegments
    if (titleSegments != null) {
        val content = style.makeHighlightContent(
            highlight = highlight,
            titleSegments = titleSegments,
            deleted = deleted,
        )
        ChatLineView(content = content, onLink = onLinkUrl)
    }
}

@Composable
private fun HighlightImageView(
    style: ChatLineStyle,
    highlight: com.moblin.android.various.model.ChatHighlight,
) {
    ChatLineView(content = style.makeHighlightImageContent(highlight = highlight))
}

@Composable
private fun LineView(
    deleted: Boolean,
    post: ChatPost,
    style: ChatLineStyle,
    platform: Boolean,
    onSelectedPost: (ChatPost?) -> Unit,
    onLinkUrl: (URL?) -> Unit,
) {
    ChatLineView(
        content = style.makeContent(post = post, platform = platform, deleted = deleted),
    ) { url ->
        if (url != null) {
            onLinkUrl(url)
        } else {
            onSelectedPost(post)
        }
    }
}

@Composable
private fun PostView(
    chatSettings: SettingsChat,
    style: ChatLineStyle,
    moreThanOneStreamingPlatform: Boolean,
    onSelectedPost: (ChatPost?) -> Unit,
    onLinkUrl: (URL?) -> Unit,
    post: ChatPost,
    state: ChatPostState,
    rotation: Double,
    scaleX: Double,
    size: androidx.compose.ui.unit.IntSize,
) {
    if (post.user != null) {
        if (!state.deleted || chatSettings.showDeletedMessages) {
            val highlight = post.highlight
            if (highlight != null) {
                Row(modifier = Modifier.graphicsLayer {
                    rotationZ = rotation.toFloat()
                    scaleX = scaleX.toFloat()
                }) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(androidx.compose.foundation.layout.Dp.Unspecified)
                            .background(highlight.barColor),
                    )
                    if (chatSettings.compactEvents && highlight.titleSegments != null) {
                        HighlightImageView(style = style, highlight = highlight)
                    }
                    Column(
                        modifier = Modifier,
                        horizontalAlignment = Alignment.Start,
                    ) {
                        if (!chatSettings.compactEvents) {
                            HighlightMessageView(
                                deleted = state.deleted,
                                style = style,
                                highlight = highlight,
                                onLinkUrl = onLinkUrl,
                            )
                        }
                        LineView(
                            deleted = state.deleted,
                            post = post,
                            style = style,
                            platform = moreThanOneStreamingPlatform,
                            onSelectedPost = onSelectedPost,
                            onLinkUrl = onLinkUrl,
                        )
                    }
                }
            } else {
                LineView(
                    deleted = state.deleted,
                    post = post,
                    style = style,
                    platform = moreThanOneStreamingPlatform,
                    onSelectedPost = onSelectedPost,
                    onLinkUrl = onLinkUrl,
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .graphicsLayer {
                    rotationZ = rotation.toFloat()
                    scaleX = scaleX.toFloat()
                }
                .width(size.width.dp)
                .height(1.5.dp)
                .background(Color.Red),
        )
    }
}

@Composable
private fun MessagesView(
    model: Model = LocalModel.current,
    chatSettings: SettingsChat,
    chat: ChatProvider,
    onSelectedPost: (ChatPost?) -> Unit,
    onLinkUrl: (URL?) -> Unit,
) {
    val rotation = chatSettings.getRotation()
    val scaleX = chatSettings.getScaleX()
    val style = makeChatLineStyle(chatSettings)
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationZ = rotation.toFloat()
                scaleX = (scaleX * chatSettings.isMirrored()).toFloat()
            },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start,
        ) {
            item {
                Box(modifier = Modifier.height(1.dp))
            }
            items(chat.posts) { post ->
                PostView(
                    chatSettings = chatSettings,
                    style = style,
                    moreThanOneStreamingPlatform = chat.moreThanOneStreamingPlatform,
                    onSelectedPost = onSelectedPost,
                    onLinkUrl = onLinkUrl,
                    post = post,
                    state = post.state,
                    rotation = rotation,
                    scaleX = scaleX,
                    size = androidx.compose.ui.unit.IntSize(0, 0),
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        model.endOfQuickButtonChatReachedWhenPaused()
    }
}

@Composable
private fun ChatView(
    model: Model = LocalModel.current,
    chat: ChatProvider,
    onSelectedPost: (ChatPost?) -> Unit,
    onLinkUrl: (URL?) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        MessagesView(
            model = model,
            chatSettings = model.database.chat,
            chat = chat,
            onSelectedPost = onSelectedPost,
            onLinkUrl = onLinkUrl,
        )
        if (chat.paused) {
            ChatInfo(
                message = localized("Chat paused: ${chat.pausedPostsCount} new messages")
            )
        }
        BannersView(model = model, banners = model.banners)
    }
}

@Composable
private fun AlertsPostView(
    chatSettings: SettingsChat,
    style: ChatLineStyle,
    moreThanOneStreamingPlatform: Boolean,
    showFirstTimeChatterMessage: Boolean,
    showNewFollowerMessage: Boolean,
    onSelectedPost: (ChatPost?) -> Unit,
    onLinkUrl: (URL?) -> Unit,
    post: ChatPost,
    state: ChatPostState,
    rotation: Double,
    scaleX: Double,
    size: androidx.compose.ui.unit.IntSize,
) {
    fun shouldShowMessage(highlight: com.moblin.android.various.model.ChatHighlight): Boolean {
        if (highlight.kind == com.moblin.android.various.model.ChatHighlightKind.firstMessage &&
            !showFirstTimeChatterMessage
        ) {
            return false
        }
        if (highlight.kind == com.moblin.android.various.model.ChatHighlightKind.newFollower &&
            !showNewFollowerMessage
        ) {
            return false
        }
        return true
    }

    if (post.user != null) {
        if (!state.deleted || chatSettings.showDeletedMessages) {
            val highlight = post.highlight
            if (highlight != null) {
                if (shouldShowMessage(highlight)) {
                    Row(modifier = Modifier.graphicsLayer {
                        rotationZ = rotation.toFloat()
                        scaleX = scaleX.toFloat()
                    }) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxSize()
                                .background(highlight.barColor),
                        )
                        if (chatSettings.compactEvents && highlight.titleSegments != null) {
                            HighlightImageView(style = style, highlight = highlight)
                        }
                        Column {
                            if (!chatSettings.compactEvents) {
                                HighlightMessageView(
                                    deleted = state.deleted,
                                    style = style,
                                    highlight = highlight,
                                    onLinkUrl = onLinkUrl,
                                )
                            }
                            LineView(
                                deleted = state.deleted,
                                post = post,
                                style = style,
                                platform = moreThanOneStreamingPlatform,
                                onSelectedPost = onSelectedPost,
                                onLinkUrl = onLinkUrl,
                            )
                        }
                    }
                }
            } else {
                LineView(
                    deleted = state.deleted,
                    post = post,
                    style = style,
                    platform = moreThanOneStreamingPlatform,
                    onSelectedPost = onSelectedPost,
                    onLinkUrl = onLinkUrl,
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .graphicsLayer {
                    rotationZ = rotation.toFloat()
                    scaleX = scaleX.toFloat()
                }
                .width(size.width.dp)
                .height(1.5.dp)
                .background(Color.Red),
        )
    }
}

@Composable
private fun AlertsMessagesView(
    model: Model = LocalModel.current,
    chatSettings: SettingsChat,
    chat: ChatProvider,
    quickButtonChat: QuickButtonChat,
    onSelectedPost: (ChatPost?) -> Unit,
    onLinkUrl: (URL?) -> Unit,
) {
    val rotation = chatSettings.getRotation()
    val scaleX = chatSettings.getScaleX()
    val style = makeChatLineStyle(chatSettings)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationZ = rotation.toFloat()
                scaleX = (scaleX * chatSettings.isMirrored()).toFloat()
            },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start,
        ) {
            item {
                Box(modifier = Modifier.height(1.dp))
            }
            items(quickButtonChat.chatAlertsPosts) { post ->
                AlertsPostView(
                    chatSettings = chatSettings,
                    style = style,
                    moreThanOneStreamingPlatform = chat.moreThanOneStreamingPlatform,
                    showFirstTimeChatterMessage = quickButtonChat.showFirstTimeChatterMessage,
                    showNewFollowerMessage = quickButtonChat.showNewFollowerMessage,
                    onSelectedPost = onSelectedPost,
                    onLinkUrl = onLinkUrl,
                    post = post,
                    state = post.state,
                    rotation = rotation,
                    scaleX = scaleX,
                    size = androidx.compose.ui.unit.IntSize(0, 0),
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        model.endOfQuickButtonChatAlertsReachedWhenPaused()
    }
}

@Composable
private fun ChatAlertsView(
    model: Model = LocalModel.current,
    quickButtonChat: QuickButtonChat,
    onSelectedPost: (ChatPost?) -> Unit,
    onLinkUrl: (URL?) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AlertsMessagesView(
            model = model,
            chatSettings = model.database.chat,
            chat = model.quickButtonChat,
            quickButtonChat = quickButtonChat,
            onSelectedPost = onSelectedPost,
            onLinkUrl = onLinkUrl,
        )
        if (quickButtonChat.chatAlertsPaused) {
            ChatInfo(
                message = localized(
                    "Chat paused: ${quickButtonChat.pausedChatAlertsPostsCount} new alerts"
                )
            )
        }
        BannersView(model = model, banners = model.banners)
    }
}

@Composable
private fun TagButtonView(
    tag: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    if (enabled) {
        Button(
            onClick = { onEnabledChange(!enabled) },
            colors = ButtonDefaults.buttonColors(),
        ) {
            Text(tag)
        }
    } else {
        TextButton(onClick = { onEnabledChange(!enabled) }) {
            Text(tag)
        }
    }
}

@Composable
private fun PredefinedMessageView(
    model: Model = LocalModel.current,
    filter: SettingsChatPredefinedMessagesFilter,
    predefinedMessage: SettingsChatPredefinedMessage,
    showingPredefinedMessages: Boolean,
    onShowingPredefinedMessagesChange: (Boolean) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onNavigate("predefinedMessage") }) {
        DraggableItemPrefixView(
            modifier = Modifier.alpha(if (filter.isEnabled()) 0.5f else 1f)
        )
        Text(predefinedMessage.tagsString())
        Text(predefinedMessage.text)
        Spacer(modifier = Modifier.weight(1f))
        BorderlessButtonView(
            text = "Send",
            enabled = predefinedMessage.text.isNotEmpty(),
            onClick = {
                model.sendChatMessageShowLogin(message = predefinedMessage.text)
                onShowingPredefinedMessagesChange(false)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredefinedMessagesView(
    model: Model = LocalModel.current,
    chat: SettingsChat,
    filter: SettingsChatPredefinedMessagesFilter,
    presentingPredefinedMessages: Boolean,
    onPresentingPredefinedMessagesChange: (Boolean) -> Unit,
    messageToSend: UUID?,
    onMessageToSendChange: (UUID?) -> Unit,
) {
    fun filteredMessages(): List<SettingsChatPredefinedMessage> {
        if (!(filter.blueTag || filter.greenTag || filter.yellowTag || filter.orangeTag || filter.redTag)) {
            return chat.predefinedMessages
        }
        val messages = mutableListOf<SettingsChatPredefinedMessage>()
        for (message in chat.predefinedMessages) {
            var shouldAdd = true
            if (filter.blueTag && !message.blueTag) shouldAdd = false
            if (filter.greenTag && !message.greenTag) shouldAdd = false
            if (filter.yellowTag && !message.yellowTag) shouldAdd = false
            if (filter.orangeTag && !message.orangeTag) shouldAdd = false
            if (filter.redTag && !message.redTag) shouldAdd = false
            if (shouldAdd) messages.add(message)
        }
        return messages
    }

    Box {
        TopAppBar(title = { Text(localized("Predefined messages")) })
        Column(modifier = Modifier.padding(top = 56.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(11.dp)) {
                Text("Filter")
                Spacer(modifier = Modifier.weight(1f))
                TagButtonView(
                    tag = SettingsChatPredefinedMessage.tagBlue,
                    enabled = filter.blueTag,
                    onEnabledChange = { filter.blueTag = it })
                TagButtonView(
                    tag = SettingsChatPredefinedMessage.tagGreen,
                    enabled = filter.greenTag,
                    onEnabledChange = { filter.greenTag = it })
                TagButtonView(
                    tag = SettingsChatPredefinedMessage.tagYellow,
                    enabled = filter.yellowTag,
                    onEnabledChange = { filter.yellowTag = it })
                TagButtonView(
                    tag = SettingsChatPredefinedMessage.tagOrange,
                    enabled = filter.orangeTag,
                    onEnabledChange = { filter.orangeTag = it })
                TagButtonView(
                    tag = SettingsChatPredefinedMessage.tagRed,
                    enabled = filter.redTag,
                    onEnabledChange = { filter.redTag = it })
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredMessages()) { predefinedMessage ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PredefinedMessageView(
                            model = model,
                            filter = filter,
                            predefinedMessage = predefinedMessage,
                            showingPredefinedMessages = presentingPredefinedMessages,
                            onShowingPredefinedMessagesChange = onPresentingPredefinedMessagesChange,
                            onNavigate = {},
                        )
                        if (!filter.isEnabled()) {
                            IconButton(onClick = {
                                chat.predefinedMessages.removeAll { it.id == predefinedMessage.id }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
            TextButtonView("Create") {
                chat.predefinedMessages.add(SettingsChatPredefinedMessage())
            }
            if (filter.isEnabled()) {
                Text(localized("Cannot move or delete predefined messages when filtering."))
            } else {
                SwipeLeftToDeleteHelpView(kind = localized("a predefined message"))
            }
        }
    }
}

@Composable
private fun SendMessagesToView(
    platform: com.moblin.android.streamingplatforms.Platform,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = platform.imageNameResource()),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = platform.name(),
            modifier = Modifier.padding(start = 6.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(checked = enabled, onCheckedChange = { onEnabledChange(it) })
    }
}

@Composable
private fun PlatformIconView(image: String) {
    androidx.compose.foundation.Image(
        painter = painterResource(id = platformIconResource(image)),
        contentDescription = null,
        modifier = Modifier.size(25.dp),
    )
}

private fun platformIconResource(image: String): Int = TODO("Android drawable resource id for $image")

@Composable
private fun SendMessagesToSelectorView(
    stream: SettingsStream,
    presentingSelector: Boolean,
    onPresentingSelectorChange: (Boolean) -> Unit,
) {
    fun isTwitchOnly(): Boolean = stream.twitchSendMessagesTo && !stream.kickSendMessagesTo

    fun isKickOnly(): Boolean = stream.kickSendMessagesTo && !stream.twitchSendMessagesTo

    Box(modifier = Modifier.size(30.dp).clickable { onPresentingSelectorChange(true) }) {
        if (isTwitchOnly()) {
            PlatformIconView(image = "TwitchLogo")
        } else if (isKickOnly()) {
            PlatformIconView(image = "KickLogo")
        } else {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
            )
        }
    }
    if (presentingSelector) {
        ModalBottomSheet(onDismissRequest = { onPresentingSelectorChange(false) }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Send messages to", modifier = Modifier.padding(11.dp))
                SendMessagesToView(
                    platform = com.moblin.android.streamingplatforms.Platform.Twitch,
                    enabled = stream.twitchSendMessagesTo,
                    onEnabledChange = { stream.twitchSendMessagesTo = it },
                )
                SendMessagesToView(
                    platform = com.moblin.android.streamingplatforms.Platform.Kick,
                    enabled = stream.kickSendMessagesTo,
                    onEnabledChange = { stream.kickSendMessagesTo = it },
                )
            }
        }
    }
}

@Composable
private fun MenuItemView(
    image: String,
    text: String,
    action: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { action() }.padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = platformIconResource(image)),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Text(localized(text), modifier = Modifier.padding(start = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlMenuButtonView(
    model: Model = LocalModel.current,
    presentingMenu: Boolean,
    onPresentingMenuChange: (Boolean) -> Unit,
) {
    IconButton(onClick = { onPresentingMenuChange(true) }) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = null,
            modifier = Modifier.graphicsLayer { rotationZ = 90f },
        )
    }
    if (presentingMenu) {
        ModalBottomSheet(onDismissRequest = { onPresentingMenuChange(false) }) {
            Column(modifier = Modifier.padding(5.dp)) {
                MenuItemView(image = "shield", text = "Moderation") {
                    onPresentingMenuChange(false)
                    model.presentingModeration = true
                }
                MenuItemView(image = "list.bullet", text = "Predefined messages") {
                    onPresentingMenuChange(false)
                    model.presentingPredefinedMessages = true
                }
            }
        }
    }
}

@Composable
private fun ControlAlertsButtonView(quickButtonChat: QuickButtonChat) {
    IconButton(onClick = { quickButtonChat.showAllChatMessages = !quickButtonChat.showAllChatMessages }) {
        Icon(
            imageVector = if (quickButtonChat.showAllChatMessages) {
                Icons.Default.Notifications
            } else {
                Icons.Default.NotificationsActive
            },
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun ControlView(
    model: Model = LocalModel.current,
    message: String,
    onMessageChange: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            placeholder = { Text("Send message", color = Color.Gray) },
            modifier = Modifier.weight(1f).padding(5.dp),
        )
        TextButton(onClick = {
            if (message.isNotEmpty()) {
                model.sendChatMessageShowLogin(message = message)
            }
            onMessageChange("")
        }) {
            Icon(Icons.Default.Send, contentDescription = null)
        }
        SendMessagesToSelectorView(
            stream = model.stream,
            presentingSelector = false,
            onPresentingSelectorChange = {},
        )
        ControlAlertsButtonView(quickButtonChat = model.quickButtonChatState)
        ControlMenuButtonView(
            model = model,
            presentingMenu = false,
            onPresentingMenuChange = {},
        )
    }
}

@Composable
private fun AlertsControlView(
    model: Model = LocalModel.current,
    quickButtonChat: QuickButtonChat,
    message: String,
    onMessageChange: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            quickButtonChat.showFirstTimeChatterMessage = !quickButtonChat.showFirstTimeChatterMessage
            model.database.chat.showFirstTimeChatterMessage = quickButtonChat.showFirstTimeChatterMessage
        }) {
            Icon(
                imageVector = if (quickButtonChat.showFirstTimeChatterMessage) {
                    Icons.Default.ChatBubble
                } else {
                    Icons.Default.ChatBubbleOutline
                },
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
        }
        IconButton(onClick = {
            quickButtonChat.showNewFollowerMessage = !quickButtonChat.showNewFollowerMessage
            model.database.chat.showNewFollowerMessage = quickButtonChat.showNewFollowerMessage
        }) {
            Icon(
                imageVector = if (quickButtonChat.showNewFollowerMessage) {
                    Icons.Default.MilitaryTech
                } else {
                    Icons.Default.MilitaryTech
                },
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        ControlAlertsButtonView(quickButtonChat = quickButtonChat)
        ControlMenuButtonView(
            model = model,
            presentingMenu = false,
            onPresentingMenuChange = {},
        )
    }
}

@Composable
fun QuickButtonChatView(
    model: Model = LocalModel.current,
    orientation: Orientation,
    quickButtonChat: QuickButtonChat,
) {
    var message by remember { mutableStateOf("") }
    var selectedPost by remember { mutableStateOf<ChatPost?>(null) }
    var linkUrl by remember { mutableStateOf<URL?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (orientation.isPortrait) 5.dp else 0.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (quickButtonChat.showAllChatMessages) {
                    ChatView(
                        model = model,
                        chat = model.quickButtonChat,
                        onSelectedPost = { selectedPost = it },
                        onLinkUrl = { linkUrl = it },
                    )
                } else {
                    ChatAlertsView(
                        model = model,
                        quickButtonChat = quickButtonChat,
                        onSelectedPost = { selectedPost = it },
                        onLinkUrl = { linkUrl = it },
                    )
                }
            }
            Row(
                modifier = Modifier
                    .height(50.dp)
                    .border(1.dp, Color.Gray)
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (quickButtonChat.showAllChatMessages) {
                    ControlView(
                        model = model,
                        message = message,
                        onMessageChange = { message = it },
                    )
                } else {
                    AlertsControlView(
                        model = model,
                        quickButtonChat = quickButtonChat,
                        message = message,
                        onMessageChange = { message = it },
                    )
                }
            }
        }
        ChatActionButtonsView(
            model = model,
            style = makeChatLineStyle(chat = model.database.chat),
            selectedPost = selectedPost,
            onSelectedPost = { selectedPost = it },
            linkUrl = linkUrl,
            onLinkUrl = { linkUrl = it },
        )
    }
    TopAppBar(title = { Text(localized("Chat")) })
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(linkUrl) {
        val url = linkUrl ?: return@LaunchedEffect
        uriHandler.openUri(url.toString())
        linkUrl = null
    }
}
