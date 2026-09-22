package com.moblin.android.various

import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.color
import com.moblin.android.common.various.toRgb
import com.moblin.android.localized
import com.moblin.android.remotecontrol.RemoteControlChatHighlight
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.various.settings.SettingsChatDisplayStyle
import com.moblin.android.various.settings.SettingsChatFilter
import com.moblin.android.various.settings.SettingsChatNicknames
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class ChatMessageEmote(
    val url: String,
    var stillUrl: String? = null,
    val range: IntRange,
    var isGif: Boolean = false,
) {
    val id: UUID = UUID.randomUUID()
}

@Serializable
data class ChatPostUrl(
    val moving: String?,
    val still: String?,
) {
    fun url(animated: Boolean): String? {
        return if (animated) {
            moving ?: still
        } else {
            still ?: moving
        }
    }
}

@Serializable
data class ChatPostSegment(
    val id: Int,
    var text: String? = null,
    var url: ChatPostUrl? = null,
    var bigGifUrl: ChatPostUrl? = null,
)

fun makeChatPostTextSegments(text: String): List<ChatPostSegment> {
    return makeChatPostTextSegments(text, 0).first
}

fun makeChatPostTextSegments(text: String, id: Int): Pair<List<ChatPostSegment>, Int> {
    val segments = mutableListOf<ChatPostSegment>()
    var currentId = id
    for (word in text.split(Regex("[\\s\\u00A0]+"))) {
        if (word.isEmpty()) {
            continue
        }
        segments.add(ChatPostSegment(id = currentId, text = "$word "))
        currentId += 1
    }
    return Pair(segments, currentId)
}

@Serializable
enum class ChatHighlightKind {
    @SerialName("other")
    Other,

    @SerialName("redemption")
    Redemption,

    @SerialName("firstMessage")
    FirstMessage,

    @SerialName("newFollower")
    NewFollower,

    @SerialName("reply")
    Reply,

    @SerialName("moderator")
    Moderator,

    @SerialName("remoteControlAssistant")
    RemoteControlAssistant,

    @SerialName("gigantifiedEmote")
    GigantifiedEmote,
}

data class ChatHighlight(
    val kind: ChatHighlightKind,
    val barColor: Color,
    val image: String,
    val titleSegments: List<ChatPostSegment>?,
) {
    fun toWatchProtocol(): Nothing = TODO("no Android counterpart for WatchConnectivity")

    fun titleNoEmotes(): String? {
        return titleSegments?.mapNotNull { it.text }?.joinToString(separator = "")
    }

    fun messageColor(defaultColor: Color = Color.White): Color {
        return if (kind == ChatHighlightKind.Reply) {
            Color.Gray
        } else {
            defaultColor
        }
    }

    fun isAlert(): Boolean {
        return kind != ChatHighlightKind.Reply
    }

    fun toRemoteControl(): RemoteControlChatHighlight {
        return RemoteControlChatHighlight(
            kind = kind,
            barColor = barColor.toRgb() ?: RgbColor(red = 0, green = 255, blue = 0),
            image = image,
            titleSegments = titleSegments,
        )
    }

    companion object {
        fun makeReply(user: String, segments: List<ChatPostSegment>): ChatHighlight {
            val prefixText = localized("Replying to $user:")
            val segmentsFromPrefix = makeChatPostTextSegments(prefixText, 0)
            val replySegments = segmentsFromPrefix.first.toMutableList()
            var id = segmentsFromPrefix.second
            var totalLength = prefixText.length
            for (segment in segments) {
                val textLength = segment.text?.length ?: 0
                val emoteLength = if (segment.url != null) 3 else 0
                val segmentLength = textLength + emoteLength
                if (totalLength + segmentLength > 65) {
                    val remainingLength = 65 - totalLength
                    val segmentText = segment.text
                    val truncatedText = if (remainingLength > 3 && segmentText != null) {
                        segmentText.take(remainingLength - 3) + "..."
                    } else {
                        "..."
                    }
                    replySegments.add(ChatPostSegment(id = id, text = truncatedText))
                    break
                }
                totalLength += segmentLength
                replySegments.add(ChatPostSegment(id = id, text = segment.text, url = segment.url))
                id += 1
            }
            return ChatHighlight(
                kind = ChatHighlightKind.Reply,
                barColor = Color.Transparent,
                image = "arrowshape.turn.up.left",
                titleSegments = replySegments,
            )
        }

        fun makeAnnouncement(): ChatHighlight {
            return ChatHighlight(
                kind = ChatHighlightKind.Other,
                barColor = Color.Green,
                image = "horn.blast",
                titleSegments = makeChatPostTextSegments(localized("Announcement")),
            )
        }

        fun makeFirstMessage(): ChatHighlight {
            return ChatHighlight(
                kind = ChatHighlightKind.FirstMessage,
                barColor = Color.Yellow,
                image = "bubble.left",
                titleSegments = makeChatPostTextSegments(localized("First time chatter")),
            )
        }

        fun makePaidMessage(): ChatHighlight {
            return ChatHighlight(
                kind = ChatHighlightKind.Other,
                barColor = Color(red = 1.0f, green = 0.5f, blue = 0.0f),
                image = "message",
                titleSegments = makeChatPostTextSegments(localized("Super Chat")),
            )
        }

        fun makePaidSticker(): ChatHighlight {
            return ChatHighlight(
                kind = ChatHighlightKind.Other,
                barColor = Color.Green,
                image = "doc.plaintext",
                titleSegments = makeChatPostTextSegments(localized("Super Sticker")),
            )
        }

        fun makeMember(): ChatHighlight {
            return ChatHighlight(
                kind = ChatHighlightKind.Other,
                barColor = Color.Blue,
                image = "medal",
                titleSegments = makeChatPostTextSegments(localized("Member")),
            )
        }

        fun makeGiftedMemberships(): ChatHighlight {
            return ChatHighlight(
                kind = ChatHighlightKind.Other,
                barColor = Color.Blue,
                image = "gift",
                titleSegments = makeChatPostTextSegments(localized("Gifted Memberships")),
            )
        }

        fun makeJewels(): ChatHighlight {
            return ChatHighlight(
                kind = ChatHighlightKind.Other,
                barColor = Color.Blue,
                image = "diamond",
                titleSegments = makeChatPostTextSegments(localized("Jewels")),
            )
        }

        fun makeGigantifiedEmote(): ChatHighlight {
            return ChatHighlight(
                kind = ChatHighlightKind.GigantifiedEmote,
                barColor = Color(red = 0.5f, green = 0.0f, blue = 0.5f),
                image = "arrow.up.backward.and.arrow.down.forward.square",
                titleSegments = makeChatPostTextSegments(localized("Gigantified emote")),
            )
        }

        fun makeModerator(): ChatHighlight {
            return ChatHighlight(
                kind = ChatHighlightKind.Moderator,
                barColor = Color.Green,
                image = "",
                titleSegments = null,
            )
        }

        fun makeRemoteControlAssistant(): ChatHighlight {
            return ChatHighlight(
                kind = ChatHighlightKind.RemoteControlAssistant,
                barColor = Color.Green,
                image = "person.wave.2",
                titleSegments = makeChatPostTextSegments(localized("Remote control assistant")),
            )
        }
    }

    constructor(remoteControl: RemoteControlChatHighlight) : this(
        kind = remoteControl.kind,
        barColor = remoteControl.barColor.color(),
        image = remoteControl.image,
        titleSegments = remoteControl.titleSegments,
    )
}

class ChatPostState {
    val deleted = MutableStateFlow(false)
}

class ChatPost(
    var id: Int,
    val messageId: String?,
    val displayName: String?,
    val user: String?,
    var userId: String?,
    val userColor: RgbColor,
    val userBadges: List<String>,
    val segments: List<ChatPostSegment>,
    val timestamp: String,
    val timestampTime: Instant,
    val isAction: Boolean,
    val isSubscriber: Boolean,
    val bits: String?,
    val highlight: ChatHighlight?,
    val live: Boolean,
    val filter: SettingsChatFilter?,
    val platform: Platform?,
    val sourceChannelIcon: String?,
    val state: ChatPostState,
) {
    override fun equals(other: Any?): Boolean {
        return other is ChatPost && other.id == id
    }

    override fun hashCode(): Int {
        return id
    }

    fun isRedemption(): Boolean {
        return when (highlight?.kind) {
            ChatHighlightKind.Other, ChatHighlightKind.Redemption, ChatHighlightKind.NewFollower -> true
            else -> false
        }
    }

    fun isBigGif(): Boolean {
        return segments.firstOrNull()?.bigGifUrl != null
    }

    fun text(): String {
        return segments.mapNotNull { it.text }.joinToString(separator = "").trim()
    }

    fun isRedLine(): Boolean {
        return user == null
    }

    fun displayName(nicknames: SettingsChatNicknames, displayStyle: SettingsChatDisplayStyle): String {
        val name = displayName
        val userName = user
        if (name == null || userName == null) {
            return localized("Unknown")
        }
        val nickname = nicknames.getNickname(userName)
        if (nickname != null) {
            return "$nickname @$userName"
        }
        return when (displayStyle) {
            SettingsChatDisplayStyle.internationalNameAndUsername -> {
                if (name.equals(userName, ignoreCase = true)) {
                    name
                } else {
                    "$name ($userName)"
                }
            }
            SettingsChatDisplayStyle.internationalName -> name
            SettingsChatDisplayStyle.username -> userName
        }
    }

    fun shortDisplayName(nicknames: SettingsChatNicknames): String {
        val name = displayName
        val userName = user
        if (name == null || userName == null) {
            return localized("Unknown")
        }
        val nickname = nicknames.getNickname(userName)
        if (nickname != null) {
            return nickname
        }
        return name
    }
}
