package com.moblin.android.view.externaldisplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.ChatPost
import com.moblin.android.various.ChatPostState
import com.moblin.android.various.model.ExternalDisplay
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.chat.ChatProvider
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.view.stream.SharedUiViewContainerView
import com.moblin.android.view.utils.ChatLineContent
import com.moblin.android.view.utils.ChatLineItem
import com.moblin.android.view.utils.ChatLineStyle
import com.moblin.android.view.utils.ChatLineTextStyle
import com.moblin.android.view.utils.ChatLineView
import com.moblin.android.LocalModel

@Composable
private fun makeChatLineStyle(chat: SettingsChat): ChatLineStyle {
    val fontSize = chat.fontSize.toDouble()
    val timestampColorEnabled = chat.timestampColorEnabled
    val badges = chat.badges
    val animatedEmotes = chat.animatedEmotes
    val nicknames = chat.nicknames
    val displayStyle = chat.displayStyle
    val font = chat.font
    return ChatLineStyle(
        fontSize = (3.0 * fontSize).toFloat(),
        timestampColor = if (timestampColorEnabled) Color.Gray else null,
        boldUsername = true,
        badges = badges,
        animatedEmotes = animatedEmotes,
        nicknames = nicknames,
        displayStyle = displayStyle,
        font = font,
    )
}

@Composable
private fun HighlightMessageView(style: ChatLineStyle, highlight: ChatHighlight) {
    val title = highlight.titleNoEmotes() ?: return
    ChatLineView(content = content(style = style, highlight = highlight, title = title))
}

private fun content(
    style: ChatLineStyle,
    highlight: ChatHighlight,
    title: String,
): ChatLineContent {
    val color = highlight.messageColor()
    return style.content(
        items = mutableListOf(
            style.symbolItem(name = highlight.image, color = color),
            ChatLineItem.Text(" $title", ChatLineTextStyle(color = color)),
        ),
    )
}

@Composable
private fun HighlightImageView(style: ChatLineStyle, highlight: ChatHighlight) {
    ChatLineView(content = style.makeHighlightImageContent(highlight = highlight))
}

@Composable
private fun LineView(
    deleted: Boolean,
    post: ChatPost,
    style: ChatLineStyle,
    platform: Boolean,
) {
    ChatLineView(content = style.makeContent(post = post, platform = platform, deleted = deleted))
}

@Composable
private fun PostView(
    chatSettings: SettingsChat,
    style: ChatLineStyle,
    moreThanOneStreamingPlatform: Boolean,
    post: ChatPost,
    state: ChatPostState,
    rotation: Double,
    scaleX: Double,
) {
    val compactEvents = chatSettings.compactEvents
    val showDeletedMessages = chatSettings.showDeletedMessages
    val deleted by state.deleted.collectAsState()
    if (!deleted || showDeletedMessages) {
        val highlight = post.highlight
        if (highlight != null) {
            Row(
                modifier = Modifier.graphicsLayer(
                    rotationZ = rotation.toFloat(),
                    scaleX = scaleX.toFloat(),
                    scaleY = 1f,
                ),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(highlight.barColor),
                )
                if (compactEvents && highlight.titleSegments != null) {
                    HighlightImageView(style = style, highlight = highlight)
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    if (!compactEvents) {
                        HighlightMessageView(style = style, highlight = highlight)
                    }
                    LineView(
                        deleted = deleted,
                        post = post,
                        style = style,
                        platform = moreThanOneStreamingPlatform,
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .padding(start = 3.dp)
                    .graphicsLayer(
                        rotationZ = rotation.toFloat(),
                        scaleX = scaleX.toFloat(),
                        scaleY = 1f,
                    ),
            ) {
                LineView(
                    deleted = deleted,
                    post = post,
                    style = style,
                    platform = moreThanOneStreamingPlatform,
                )
            }
        }
    }
}

@Composable
private fun MessagesView(chatSettings: SettingsChat, chat: ChatProvider) {
    val rotation = chatSettings.getRotation()
    val scaleX = chatSettings.getScaleX()
    val style = makeChatLineStyle(chat = chatSettings)
    val posts by chat.posts.collectAsState()
    val moreThanOneStreamingPlatform by chat.moreThanOneStreamingPlatform.collectAsState()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    rotationZ = rotation.toFloat(),
                    scaleX = (scaleX * chatSettings.isMirrored()).toFloat(),
                    scaleY = 1f,
                ),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            items(posts) { post ->
                PostView(
                    chatSettings = chatSettings,
                    style = style,
                    moreThanOneStreamingPlatform = moreThanOneStreamingPlatform,
                    post = post,
                    state = post.state,
                    rotation = rotation,
                    scaleX = scaleX,
                )
            }
        }
    }
}

@Composable
private fun ChatView(model: Model = LocalModel.current, chat: ChatProvider) {
    Box(modifier = Modifier.padding(16.dp)) {
        MessagesView(chatSettings = model.database.chat, chat = chat)
    }
}

@Composable
private fun ExternalDisplayStreamPreviewView(model: Model = LocalModel.current) {
    AndroidView(
        factory = { context ->
            SharedUiViewContainerView(context, model.externalDisplayStreamPreviewView)
        },
        update = { uiView ->
            uiView.attachSharedView()
        },
    )
}

@Composable
fun ExternalDisplayView(model: Model = LocalModel.current, externalDisplay: ExternalDisplay) {
    val chatEnabled by externalDisplay.chatEnabled.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (chatEnabled) {
            ChatView(model = model, chat = model.externalDisplayChat)
        } else {
            ExternalDisplayStreamPreviewView(model = model)
        }
    }
}
