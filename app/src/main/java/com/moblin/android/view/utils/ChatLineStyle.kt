package com.moblin.android.view.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.moblin.android.common.various.color
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.ChatPost
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.network.getHttpsUrl
import com.moblin.android.various.settings.SettingsChatDisplayStyle
import com.moblin.android.various.settings.SettingsChatNicknames
import com.moblin.android.various.settings.SettingsFont

val chatEmoteScale: Float = 1.5f

private val deletedColor = Color.Gray

data class ChatLineStyle(
    var fontSize: Float,
    var borderColor: Color? = null,
    var borderWidth: Float = 0f,
    var backgroundColor: Color? = null,
    var leadingPadding: Float = 5f,
    var timestampColor: Color? = null,
    var messageColor: Color = Color.White,
    var meInUsernameColor: Boolean = false,
    var boldUsername: Boolean = false,
    var boldMessage: Boolean = false,
    var badges: Boolean = false,
    var sharedChatIcons: Boolean = false,
    var animatedEmotes: Boolean = false,
    var bigGifScale: Float? = null,
    var linkify: Boolean = false,
    var highlightSymbolColor: Color? = null,
    var highlightDefaultColor: Color = Color.White,
    var nicknames: SettingsChatNicknames = SettingsChatNicknames(),
    var displayStyle: SettingsChatDisplayStyle = SettingsChatDisplayStyle.username,
    var fontWeight: FontWeight = FontWeight.Normal,
    var fontDesign: FontDesign = FontDesign.Default,
    var font: SettingsFont = SettingsFont(),
) {
    fun content(items: MutableList<ChatLineItem>, topAligned: Boolean = false): ChatLineContent {
        return ChatLineContent(
            items = items,
            fontSize = fontSize,
            borderColor = borderColor,
            borderWidth = borderWidth,
            backgroundColor = backgroundColor,
            leadingPadding = leadingPadding,
            topAligned = topAligned,
            fontWeight = fontWeight,
            fontDesign = fontDesign,
            font = font,
        )
    }

    fun emoteItem(url: String, scale: Float = 1f, deleted: Boolean): ChatLineItem {
        return ChatLineItem.Image(
            ChatLineImage(
                source = ChatImageSource.Url(url),
                animated = animatedEmotes,
                height = fontSize * (chatEmoteScale * scale),
                verticalPadding = if (borderColor != null) borderWidth else 0f,
                opacity = if (deleted) 0.25f else 1f,
            ),
        )
    }

    fun badgeItem(source: ChatImageSource, deleted: Boolean): ChatLineItem {
        return ChatLineItem.Image(
            ChatLineImage(
                source = source,
                height = fontSize * 1.4f,
                horizontalPadding = 2f,
                verticalPadding = 2f,
                opacity = if (deleted) 0.25f else 1f,
            ),
        )
    }

    fun symbolItem(name: String, color: Color): ChatLineItem {
        return ChatLineItem.Image(
            ChatLineImage(
                source = ChatImageSource.Symbol(name, fontSize, color),
            ),
        )
    }

    fun textItem(
        text: String,
        color: Color,
        bold: Boolean = false,
        italic: Boolean = false,
        singleLine: Boolean = false,
        deleted: Boolean,
    ): ChatLineItem {
        return ChatLineItem.Text(
            text,
            ChatLineTextStyle(
                color = if (deleted) deletedColor else color,
                bold = bold,
                italic = italic,
                strikethrough = deleted,
                singleLine = singleLine,
            ),
        )
    }

    fun messageItem(
        text: String,
        color: Color,
        bold: Boolean = false,
        italic: Boolean = false,
        deleted: Boolean,
    ): ChatLineItem {
        val url = if (linkify) getHttpsUrl(text) else null
        if (url == null) {
            return textItem(text = text, color = color, bold = bold, italic = italic, deleted = deleted)
        }
        if (deleted) {
            return textItem(text = text, color = color, bold = bold, italic = italic, deleted = true)
        }
        val style = ChatLineTextStyle(color = Color(0xFF007AFF), bold = bold, italic = italic)
        style.link = url.toString()
        return ChatLineItem.Text(text, style)
    }

    private fun spaceItem(): ChatLineItem {
        return ChatLineItem.Text(" ", ChatLineTextStyle(color = Color.White))
    }

    fun makeContent(post: ChatPost, platform: Boolean, deleted: Boolean): ChatLineContent {
        val items = mutableListOf<ChatLineItem>()
        timestampColor?.let {
            items.add(
                ChatLineItem.Text(
                    "${post.timestamp} ",
                    ChatLineTextStyle(color = it, singleLine = true),
                ),
            )
        }
        if (platform) {
            post.platform?.imageName()?.let {
                items.add(badgeItem(source = ChatImageSource.Asset(it), deleted = deleted))
            }
        }
        if (sharedChatIcons) {
            post.sourceChannelIcon?.let {
                items.add(badgeItem(source = ChatImageSource.Url(it), deleted = deleted))
            }
        }
        if (badges) {
            for (url in post.userBadges) {
                items.add(badgeItem(source = ChatImageSource.Url(url), deleted = deleted))
            }
        }
        val usernameColor = post.userColor.color()
        items.add(
            textItem(
                text = post.displayName(nicknames = nicknames, displayStyle = displayStyle),
                color = usernameColor,
                bold = boldUsername,
                singleLine = true,
                deleted = deleted,
            ),
        )
        items.add(
            ChatLineItem.Text(
                if (post.isRedemption()) " " else ": ",
                ChatLineTextStyle(color = Color.White),
            ),
        )
        val textColor = if (post.isAction && meInUsernameColor) usernameColor else messageColor
        for (segment in post.segments) {
            segment.text?.let {
                items.add(
                    messageItem(
                        text = it,
                        color = textColor,
                        bold = boldMessage,
                        italic = post.isAction,
                        deleted = deleted,
                    ),
                )
            }
            segment.url?.url(animated = animatedEmotes)?.let {
                items.add(emoteItem(url = it, deleted = deleted))
                items.add(spaceItem())
            }
            val bigGifScale = bigGifScale
            if (bigGifScale != null) {
                segment.bigGifUrl?.url(animated = animatedEmotes)?.let {
                    items.add(emoteItem(url = it, scale = bigGifScale, deleted = deleted))
                    items.add(spaceItem())
                }
            }
        }
        return content(items = items, topAligned = post.isBigGif())
    }

    fun makeHighlightContent(
        highlight: ChatHighlight,
        titleSegments: List<ChatPostSegment>,
        deleted: Boolean,
    ): ChatLineContent {
        val color = highlight.messageColor(defaultColor = highlightDefaultColor)
        val symbolColor = highlightSymbolColor ?: color
        val items = mutableListOf<ChatLineItem>(
            symbolItem(name = highlight.image, color = symbolColor),
            ChatLineItem.Text(" ", ChatLineTextStyle(color = symbolColor)),
        )
        for (segment in titleSegments) {
            segment.text?.let {
                items.add(messageItem(text = it, color = color, deleted = deleted))
            }
            segment.url?.url(animated = animatedEmotes)?.let {
                items.add(emoteItem(url = it, deleted = deleted))
            }
        }
        return content(items = items)
    }

    fun makeHighlightImageContent(highlight: ChatHighlight): ChatLineContent {
        val color = highlight.messageColor(defaultColor = highlightDefaultColor)
        return content(items = mutableListOf(symbolItem(name = highlight.image, color = color)))
    }
}
