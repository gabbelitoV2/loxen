package com.moblin.android.view.controlbar.quickbutton.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.NavigationTitle
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.platform.swiftui.move
import com.moblin.android.platform.swiftui.remove
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.ChatHighlightKind
import com.moblin.android.various.ChatPost
import com.moblin.android.various.ChatPostState
import com.moblin.android.various.model.chat.ChatProvider
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Orientation
import com.moblin.android.various.model.QuickButtonChat
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.various.settings.SettingsChatPredefinedMessage
import com.moblin.android.various.settings.SettingsChatPredefinedMessagesFilter
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.stream.ChatInfo
import com.moblin.android.view.utils.BannersView
import com.moblin.android.view.utils.BorderlessButtonView
import com.moblin.android.view.utils.ChatActionButtonsView
import com.moblin.android.view.utils.ChatLineStyle
import com.moblin.android.view.utils.ChatLineView
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.IconAndTextLocalizedView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import java.util.UUID
import com.moblin.android.various.model.endOfQuickButtonChatAlertsReachedWhenPaused
import com.moblin.android.various.model.endOfQuickButtonChatReachedWhenPaused
import com.moblin.android.various.model.pauseQuickButtonChat
import com.moblin.android.various.model.pauseQuickButtonChatAlerts
import com.moblin.android.various.model.sendChatMessageShowLogin

private fun makeChatLineStyle(chat: SettingsChat): ChatLineStyle =
    ChatLineStyle(
        fontSize = chat.fontSize.toFloat(),
        timestampColor = if (chat.timestampColorEnabled) Color.Gray else null,
        boldUsername = true,
        badges = chat.badges,
        sharedChatIcons = chat.sharedChatIcons,
        animatedEmotes = chat.animatedEmotes,
        bigGifScale = chat.bigGifScale,
        linkify = true,
        nicknames = chat.nicknames,
        displayStyle = chat.displayStyle,
        font = chat.font,
    )

@Composable
private fun HighlightMessageView(
    deleted: Boolean,
    style: ChatLineStyle,
    highlight: ChatHighlight,
    onLinkUrl: (String?) -> Unit,
) {
    val titleSegments = highlight.titleSegments ?: return
    ChatLineView(
        content = style.makeHighlightContent(
            highlight = highlight,
            titleSegments = titleSegments,
            deleted = deleted,
        ),
        onTap = { url -> onLinkUrl(url) },
    )
}

@Composable
private fun HighlightImageView(
    style: ChatLineStyle,
    highlight: ChatHighlight,
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
    onLinkUrl: (String?) -> Unit,
) {
    ChatLineView(
        content = style.makeContent(post = post, platform = platform, deleted = deleted),
        onTap = { url ->
            if (url != null) {
                onLinkUrl(url)
            } else {
                onSelectedPost(post)
            }
        },
    )
}

@Composable
private fun PostView(
    chatSettings: SettingsChat,
    style: ChatLineStyle,
    moreThanOneStreamingPlatform: Boolean,
    onSelectedPost: (ChatPost?) -> Unit,
    onLinkUrl: (String?) -> Unit,
    post: ChatPost,
    state: ChatPostState,
    rotation: Double,
    scaleX: Double,
    size: IntSize,
) {
    val deleted by state.deleted.collectAsState()
    if (post.user == null) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .graphicsLayer {
                    rotationZ = rotation.toFloat()
                    this.scaleX = scaleX.toFloat()
                }
                .width(size.width.dp)
                .height(1.5.dp)
                .background(Color.Red),
        )
        return
    }
    if (deleted && !chatSettings.showDeletedMessages) {
        return
    }
    val highlight = post.highlight
    if (highlight != null) {
        Row(
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = rotation.toFloat()
                    this.scaleX = scaleX.toFloat()
                }
                .drawBehind {
                    drawRect(color = highlight.barColor, size = Size(3.dp.toPx(), this.size.height))
                }
                .padding(start = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (chatSettings.compactEvents && highlight.titleSegments != null) {
                HighlightImageView(style = style, highlight = highlight)
            }
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                if (!chatSettings.compactEvents) {
                    HighlightMessageView(
                        deleted = deleted,
                        style = style,
                        highlight = highlight,
                        onLinkUrl = onLinkUrl,
                    )
                }
                LineView(
                    deleted = deleted,
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
                .graphicsLayer {
                    rotationZ = rotation.toFloat()
                    this.scaleX = scaleX.toFloat()
                }
                .padding(start = 3.dp),
        ) {
            LineView(
                deleted = deleted,
                post = post,
                style = style,
                platform = moreThanOneStreamingPlatform,
                onSelectedPost = onSelectedPost,
                onLinkUrl = onLinkUrl,
            )
        }
    }
}

@Composable
private fun MessagesView(
    model: Model = LocalModel.current,
    chatSettings: SettingsChat,
    chat: ChatProvider,
    onSelectedPost: (ChatPost?) -> Unit,
    onLinkUrl: (String?) -> Unit,
) {
    val rotation = chatSettings.getRotation()
    val scaleX = chatSettings.getScaleX()
    val style = makeChatLineStyle(chatSettings)
    val posts by chat.posts.collectAsState()
    val moreThanOneStreamingPlatform by chat.moreThanOneStreamingPlatform.collectAsState()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationZ = rotation.toFloat()
                this.scaleX = (scaleX * chatSettings.isMirrored()).toFloat()
            },
    ) {
        val size = IntSize(constraints.maxWidth, constraints.maxHeight)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Box(modifier = Modifier.height(1.dp)) {
                    LaunchedEffect(Unit) {
                        model.endOfQuickButtonChatReachedWhenPaused()
                    }
                    DisposableEffect(Unit) {
                        onDispose {
                            model.pauseQuickButtonChat()
                        }
                    }
                }
            }
            items(posts) { post ->
                PostView(
                    chatSettings = chatSettings,
                    style = style,
                    moreThanOneStreamingPlatform = moreThanOneStreamingPlatform,
                    onSelectedPost = onSelectedPost,
                    onLinkUrl = onLinkUrl,
                    post = post,
                    state = post.state,
                    rotation = rotation,
                    scaleX = scaleX,
                    size = size,
                )
            }
        }
    }
}

@Composable
private fun ChatView(
    model: Model = LocalModel.current,
    chat: ChatProvider,
    onSelectedPost: (ChatPost?) -> Unit,
    onLinkUrl: (String?) -> Unit,
) {
    val paused by chat.paused.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        MessagesView(
            model = model,
            chatSettings = model.database.chat,
            chat = chat,
            onSelectedPost = onSelectedPost,
            onLinkUrl = onLinkUrl,
        )
        if (paused) {
            Box(modifier = Modifier.padding(2.dp)) {
                ChatInfo(
                    message = localized("Chat paused: ${chat.pausedPostsCount} new messages"),
                )
            }
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
    onLinkUrl: (String?) -> Unit,
    post: ChatPost,
    state: ChatPostState,
    rotation: Double,
    scaleX: Double,
    size: IntSize,
) {
    val deleted by state.deleted.collectAsState()

    fun shouldShowMessage(highlight: ChatHighlight): Boolean {
        val kindName = highlight.kind.name.lowercase()
        if (kindName.contains("first") && !showFirstTimeChatterMessage) {
            return false
        }
        if (kindName.contains("follower") && !showNewFollowerMessage) {
            return false
        }
        return true
    }

    if (post.user == null) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .graphicsLayer {
                    rotationZ = rotation.toFloat()
                    this.scaleX = scaleX.toFloat()
                }
                .width(size.width.dp)
                .height(1.5.dp)
                .background(Color.Red),
        )
        return
    }
    if (deleted && !chatSettings.showDeletedMessages) {
        return
    }
    val highlight = post.highlight
    if (highlight != null) {
        if (!shouldShowMessage(highlight)) {
            return
        }
        Row(
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = rotation.toFloat()
                    this.scaleX = scaleX.toFloat()
                }
                .drawBehind {
                    drawRect(color = highlight.barColor, size = Size(3.dp.toPx(), this.size.height))
                }
                .padding(start = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (chatSettings.compactEvents && highlight.titleSegments != null) {
                HighlightImageView(style = style, highlight = highlight)
            }
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                if (!chatSettings.compactEvents) {
                    HighlightMessageView(
                        deleted = deleted,
                        style = style,
                        highlight = highlight,
                        onLinkUrl = onLinkUrl,
                    )
                }
                LineView(
                    deleted = deleted,
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
                .graphicsLayer {
                    rotationZ = rotation.toFloat()
                    this.scaleX = scaleX.toFloat()
                }
                .padding(start = 3.dp),
        ) {
            LineView(
                deleted = deleted,
                post = post,
                style = style,
                platform = moreThanOneStreamingPlatform,
                onSelectedPost = onSelectedPost,
                onLinkUrl = onLinkUrl,
            )
        }
    }
}

@Composable
private fun AlertsMessagesView(
    model: Model = LocalModel.current,
    chatSettings: SettingsChat,
    chat: ChatProvider,
    quickButtonChat: QuickButtonChat,
    onSelectedPost: (ChatPost?) -> Unit,
    onLinkUrl: (String?) -> Unit,
) {
    val rotation = chatSettings.getRotation()
    val scaleX = chatSettings.getScaleX()
    val style = makeChatLineStyle(chatSettings)
    val posts by quickButtonChat.chatAlertsPosts.collectAsState()
    val showFirstTimeChatterMessage by quickButtonChat.showFirstTimeChatterMessage.collectAsState()
    val showNewFollowerMessage by quickButtonChat.showNewFollowerMessage.collectAsState()
    val moreThanOneStreamingPlatform by chat.moreThanOneStreamingPlatform.collectAsState()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationZ = rotation.toFloat()
                this.scaleX = (scaleX * chatSettings.isMirrored()).toFloat()
            },
    ) {
        val size = IntSize(constraints.maxWidth, constraints.maxHeight)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Box(modifier = Modifier.height(1.dp)) {
                    LaunchedEffect(Unit) {
                        model.endOfQuickButtonChatAlertsReachedWhenPaused()
                    }
                    DisposableEffect(Unit) {
                        onDispose {
                            model.pauseQuickButtonChatAlerts()
                        }
                    }
                }
            }
            items(posts) { post ->
                AlertsPostView(
                    chatSettings = chatSettings,
                    style = style,
                    moreThanOneStreamingPlatform = moreThanOneStreamingPlatform,
                    showFirstTimeChatterMessage = showFirstTimeChatterMessage,
                    showNewFollowerMessage = showNewFollowerMessage,
                    onSelectedPost = onSelectedPost,
                    onLinkUrl = onLinkUrl,
                    post = post,
                    state = post.state,
                    rotation = rotation,
                    scaleX = scaleX,
                    size = size,
                )
            }
        }
    }
}

@Composable
private fun ChatAlertsView(
    model: Model = LocalModel.current,
    quickButtonChat: QuickButtonChat,
    onSelectedPost: (ChatPost?) -> Unit,
    onLinkUrl: (String?) -> Unit,
) {
    val chatAlertsPaused by quickButtonChat.chatAlertsPaused.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        AlertsMessagesView(
            model = model,
            chatSettings = model.database.chat,
            chat = model.quickButtonChat,
            quickButtonChat = quickButtonChat,
            onSelectedPost = onSelectedPost,
            onLinkUrl = onLinkUrl,
        )
        if (chatAlertsPaused) {
            Box(modifier = Modifier.padding(2.dp)) {
                ChatInfo(
                    message = localized(
                        "Chat paused: ${quickButtonChat.pausedChatAlertsPostsCount} new alerts"
                    ),
                )
            }
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
    val palette = formPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .graphicsLayer { alpha = if (pressed) 0.2f else 1f }
            .clip(shape)
            .background(if (enabled) palette.accent else Color.Transparent)
            .border(1.dp, palette.accent, shape)
            .clickable(interactionSource = interactionSource, indication = null) {
                onEnabledChange(!enabled)
            }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(tag, color = if (enabled) Color.White else palette.accent)
    }
}

@Composable
private fun PredefinedMessageView(
    model: Model = LocalModel.current,
    filter: SettingsChatPredefinedMessagesFilter,
    filterEnabled: Boolean,
    predefinedMessage: SettingsChatPredefinedMessage,
    showingPredefinedMessages: Boolean,
    onShowingPredefinedMessagesChange: (Boolean) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            Form(title = localized("Predefined message")) {
                Section {
                    TextEditNavigationView(
                        title = localized("Text"),
                        value = predefinedMessage.text,
                        onSubmit = { predefinedMessage.text = it },
                        placeholder = localized("Hello chat!"),
                    )
                }
                Section(header = localized("Tags")) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        TagButtonView(
                            tag = SettingsChatPredefinedMessage.tagBlue,
                            enabled = predefinedMessage.blueTag,
                            onEnabledChange = { predefinedMessage.blueTag = it },
                        )
                        TagButtonView(
                            tag = SettingsChatPredefinedMessage.tagGreen,
                            enabled = predefinedMessage.greenTag,
                            onEnabledChange = { predefinedMessage.greenTag = it },
                        )
                        TagButtonView(
                            tag = SettingsChatPredefinedMessage.tagYellow,
                            enabled = predefinedMessage.yellowTag,
                            onEnabledChange = { predefinedMessage.yellowTag = it },
                        )
                        TagButtonView(
                            tag = SettingsChatPredefinedMessage.tagOrange,
                            enabled = predefinedMessage.orangeTag,
                            onEnabledChange = { predefinedMessage.orangeTag = it },
                        )
                        TagButtonView(
                            tag = SettingsChatPredefinedMessage.tagRed,
                            enabled = predefinedMessage.redTag,
                            onEnabledChange = { predefinedMessage.redTag = it },
                        )
                    }
                }
            }
        },
    ) {
        if (filterEnabled) {
            CompositionLocalProvider(LocalContentColor provides formPalette().gray) {
                DraggableItemPrefixView()
            }
        } else {
            DraggableItemPrefixView()
        }
        Text(predefinedMessage.tagsString())
        Text(predefinedMessage.text)
        Spacer(modifier = Modifier.weight(1f))
        BorderlessButtonView(text = "Send") {
            if (predefinedMessage.text.isNotEmpty()) {
                model.sendChatMessageShowLogin(predefinedMessage.text)
                onShowingPredefinedMessagesChange(false)
            }
        }
    }
}

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
    var blueTag by binding({ filter.blueTag }) { filter.blueTag = it }
    var greenTag by binding({ filter.greenTag }) { filter.greenTag = it }
    var yellowTag by binding({ filter.yellowTag }) { filter.yellowTag = it }
    var orangeTag by binding({ filter.orangeTag }) { filter.orangeTag = it }
    var redTag by binding({ filter.redTag }) { filter.redTag = it }
    var predefinedMessages by binding({ chat.predefinedMessages }) { chat.predefinedMessages = it }
    val filterEnabled = filter.isEnabled()

    fun filteredMessages(): List<SettingsChatPredefinedMessage> {
        if (!(filter.blueTag || filter.greenTag || filter.yellowTag || filter.orangeTag || filter.redTag)) {
            return predefinedMessages
        }
        val messages = mutableListOf<SettingsChatPredefinedMessage>()
        for (message in predefinedMessages) {
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

    Form(
        title = localized("Predefined messages"),
        toolbar = {
            CloseToolbar(
                presenting = presentingPredefinedMessages,
                onPresentingChange = onPresentingPredefinedMessagesChange,
            )
        },
    ) {
        Section {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(localized("Filter"))
                Spacer(modifier = Modifier.weight(1f))
                TagButtonView(
                    tag = SettingsChatPredefinedMessage.tagBlue,
                    enabled = blueTag,
                    onEnabledChange = { blueTag = it },
                )
                TagButtonView(
                    tag = SettingsChatPredefinedMessage.tagGreen,
                    enabled = greenTag,
                    onEnabledChange = { greenTag = it },
                )
                TagButtonView(
                    tag = SettingsChatPredefinedMessage.tagYellow,
                    enabled = yellowTag,
                    onEnabledChange = { yellowTag = it },
                )
                TagButtonView(
                    tag = SettingsChatPredefinedMessage.tagOrange,
                    enabled = orangeTag,
                    onEnabledChange = { orangeTag = it },
                )
                TagButtonView(
                    tag = SettingsChatPredefinedMessage.tagRed,
                    enabled = redTag,
                    onEnabledChange = { redTag = it },
                )
            }
        }
        Section(
            footerContent = {
                if (filterEnabled) {
                    Text(localized("Cannot move or delete predefined messages when filtering."))
                } else {
                    SwipeLeftToDeleteHelpView(kind = localized("a predefined message"))
                }
            },
        ) {
            ForEach(
                filteredMessages(),
                id = { it.id },
                onDelete = if (!filterEnabled) {
                    { offsets -> predefinedMessages.remove(atOffsets = offsets) }
                } else {
                    null
                },
                onMove = if (!filterEnabled) {
                    { froms, to -> predefinedMessages.move(fromOffsets = froms, toOffset = to) }
                } else {
                    null
                },
            ) { predefinedMessage ->
                ContextMenuDeleteButton(
                    disabled = filterEnabled,
                    action = {
                        predefinedMessages.removeAll { it.id == predefinedMessage.id }
                    },
                ) {
                    PredefinedMessageView(
                        model = model,
                        filter = filter,
                        filterEnabled = filterEnabled,
                        predefinedMessage = predefinedMessage,
                        showingPredefinedMessages = presentingPredefinedMessages,
                        onShowingPredefinedMessagesChange = onPresentingPredefinedMessagesChange,
                        onNavigate = {},
                    )
                }
            }
            Section {
                TextButtonView("Create") {
                    predefinedMessages = (predefinedMessages + SettingsChatPredefinedMessage()).toMutableList()
                }
            }
        }
    }
}

@Composable
private fun SendMessagesToView(
    platform: Platform,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val bitmap = Bundle.image(platform.imageName())?.asImageBitmap()
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(24.dp))
        }
        Text(
            text = platform.displayName(),
            modifier = Modifier.padding(start = 6.dp),
            color = formPalette().label,
        )
        Toggle(isOn = enabled, onChange = { onEnabledChange(it) }) {}
    }
}

@Composable
private fun PlatformIconView(image: String) {
    val bitmap = Bundle.image(image)?.asImageBitmap()
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(25.dp))
    }
}

private fun platformIconResource(image: String): Int = 0

@Composable
private fun SendMessagesToSelectorView(
    stream: SettingsStream,
    presentingSelector: Boolean,
    onPresentingSelectorChange: (Boolean) -> Unit,
) {
    var twitchSendMessagesTo by binding({ stream.twitchSendMessagesTo }) { stream.twitchSendMessagesTo = it }
    var kickSendMessagesTo by binding({ stream.kickSendMessagesTo }) { stream.kickSendMessagesTo = it }

    fun isTwitchOnly(): Boolean = twitchSendMessagesTo && !kickSendMessagesTo

    fun isKickOnly(): Boolean = kickSendMessagesTo && !twitchSendMessagesTo

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(30.dp)
            .graphicsLayer { alpha = if (pressed) 0.2f else 1f }
            .clickable(interactionSource = interactionSource, indication = null) {
                onPresentingSelectorChange(true)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isTwitchOnly()) {
            PlatformIconView(image = "TwitchLogo")
        } else if (isKickOnly()) {
            PlatformIconView(image = "KickLogo")
        } else {
            SystemImage("globe", 28.sp, Modifier, formPalette().accent)
        }
    }
    Sheet(isPresented = presentingSelector, onDismissRequest = { onPresentingSelectorChange(false) }) {
        Column(
            modifier = Modifier.padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(localized("Send messages to"), modifier = Modifier.padding(11.dp))
            Box(modifier = Modifier.padding(11.dp)) {
                SendMessagesToView(
                    platform = Platform.twitch,
                    enabled = twitchSendMessagesTo,
                    onEnabledChange = { twitchSendMessagesTo = it },
                )
            }
            Box(modifier = Modifier.padding(11.dp)) {
                SendMessagesToView(
                    platform = Platform.kick,
                    enabled = kickSendMessagesTo,
                    onEnabledChange = { kickSendMessagesTo = it },
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    CompositionLocalProvider(LocalContentColor provides formPalette().accent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = if (pressed) 0.2f else 1f }
                .clickable(interactionSource = interactionSource, indication = null) {
                    action()
                }
                .padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAndTextLocalizedView(image = image, text = text)
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ControlMenuButtonView(
    model: Model = LocalModel.current,
    presentingMenu: Boolean,
    onPresentingMenuChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .padding(end = 13.dp)
            .graphicsLayer { alpha = if (pressed) 0.2f else 1f }
            .clickable(interactionSource = interactionSource, indication = null) {
                onPresentingMenuChange(true)
            },
    ) {
        SystemImage(
            "ellipsis",
            28.sp,
            Modifier.graphicsLayer { rotationZ = 90f },
            formPalette().accent,
        )
    }
    Sheet(isPresented = presentingMenu, onDismissRequest = { onPresentingMenuChange(false) }) {
        Column(
            modifier = Modifier.padding(5.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            MenuItemView(image = "shield", text = "Moderation") {
                onPresentingMenuChange(false)
                model.presentingModeration.value = true
            }
            MenuItemView(image = "list.bullet", text = "Predefined messages") {
                onPresentingMenuChange(false)
                model.presentingPredefinedMessages.value = true
            }
        }
    }
}

@Composable
private fun ControlAlertsButtonView(quickButtonChat: QuickButtonChat) {
    val showAllChatMessages by quickButtonChat.showAllChatMessages.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .graphicsLayer { alpha = if (pressed) 0.2f else 1f }
            .clickable(interactionSource = interactionSource, indication = null) {
                quickButtonChat.showAllChatMessages.value = !showAllChatMessages
            }
            .padding(5.dp),
    ) {
        SystemImage("megaphone", 28.sp, Modifier, formPalette().accent)
    }
}

@Composable
private fun ControlView(
    model: Model = LocalModel.current,
    message: String,
    onMessageChange: (String) -> Unit,
) {
    var presentingSelector by remember { mutableStateOf(false) }
    var presentingMenu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = message,
            onValueChange = onMessageChange,
            modifier = Modifier
                .weight(1f)
                .padding(5.dp),
            textStyle = formBodyStyle.copy(color = Color.White),
            cursorBrush = SolidColor(formPalette().accent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (message.isNotEmpty()) {
                        model.sendChatMessageShowLogin(message)
                    }
                    onMessageChange("")
                },
            ),
            decorationBox = { innerTextField ->
                if (message.isEmpty()) {
                    Text(localized("Send message"), color = Color.Gray)
                }
                innerTextField()
            },
        )
        SendMessagesToSelectorView(
            stream = model.stream.value,
            presentingSelector = presentingSelector,
            onPresentingSelectorChange = { presentingSelector = it },
        )
        ControlAlertsButtonView(quickButtonChat = model.quickButtonChatState)
        ControlMenuButtonView(
            model = model,
            presentingMenu = presentingMenu,
            onPresentingMenuChange = { presentingMenu = it },
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
    var presentingMenu by remember { mutableStateOf(false) }
    val showFirstTimeChatterMessage by quickButtonChat.showFirstTimeChatterMessage.collectAsState()
    val showNewFollowerMessage by quickButtonChat.showNewFollowerMessage.collectAsState()
    val palette = formPalette()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    val value = !showFirstTimeChatterMessage
                    quickButtonChat.showFirstTimeChatterMessage.value = value
                    model.database.chat.showFirstTimeChatterMessage = value
                }
                .padding(5.dp),
        ) {
            SystemImage(
                if (showFirstTimeChatterMessage) "text.bubble" else "bubble.left",
                28.sp,
                Modifier,
                palette.accent,
            )
        }
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    val value = !showNewFollowerMessage
                    quickButtonChat.showNewFollowerMessage.value = value
                    model.database.chat.showNewFollowerMessage = value
                }
                .padding(5.dp),
        ) {
            SystemImage(
                if (showNewFollowerMessage) "person.2.fill" else "person.2",
                28.sp,
                Modifier,
                palette.accent,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        ControlAlertsButtonView(quickButtonChat = quickButtonChat)
        ControlMenuButtonView(
            model = model,
            presentingMenu = presentingMenu,
            onPresentingMenuChange = { presentingMenu = it },
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
    var linkUrl by remember { mutableStateOf<String?>(null) }
    val showAllChatMessages by quickButtonChat.showAllChatMessages.collectAsState()
    val palette = formPalette()

    NavigationTitle(localized("Chat"))
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(bottom = if (orientation.isPortrait.value) 5.dp else 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                if (showAllChatMessages) {
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
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, palette.gray)
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showAllChatMessages) {
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
            onSelectedPostChange = { selectedPost = it },
            linkUrl = linkUrl,
            onLinkUrlChange = { linkUrl = it },
        )
    }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(linkUrl) {
        val url = linkUrl ?: return@LaunchedEffect
        uriHandler.openUri(url)
        linkUrl = null
    }
}
