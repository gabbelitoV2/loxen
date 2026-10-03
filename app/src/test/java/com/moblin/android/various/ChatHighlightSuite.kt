package com.moblin.android.various

import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.toRgb
import com.moblin.android.moblinwatch.shared.WatchProtocolChatHighlightKind
import com.moblin.android.streamingplatforms.Platform
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import org.junit.Test

class ChatHighlightSuite {
    private fun highlight(kind: ChatHighlightKind, barColor: Color = Color(0xFF32ADE6)): ChatHighlight {
        return ChatHighlight(
            kind = kind,
            barColor = barColor,
            image = "party.popper",
            titleSegments = listOf(
                ChatPostSegment(id = 0, text = "New "),
                ChatPostSegment(id = 1, url = ChatPostUrl("https://x/y.gif", null)),
                ChatPostSegment(id = 2, text = "subscriber"),
            ),
        )
    }

    @Test
    fun watchProtocolKindsMatchSwift() {
        val expected = mapOf(
            ChatHighlightKind.Redemption to WatchProtocolChatHighlightKind.redemption,
            ChatHighlightKind.Other to WatchProtocolChatHighlightKind.other,
            ChatHighlightKind.NewFollower to WatchProtocolChatHighlightKind.redemption,
            ChatHighlightKind.FirstMessage to WatchProtocolChatHighlightKind.other,
            ChatHighlightKind.Reply to WatchProtocolChatHighlightKind.reply,
            ChatHighlightKind.Moderator to WatchProtocolChatHighlightKind.moderator,
            ChatHighlightKind.RemoteControlAssistant to WatchProtocolChatHighlightKind.other,
            ChatHighlightKind.GigantifiedEmote to WatchProtocolChatHighlightKind.other,
            ChatHighlightKind.MacroNotification to WatchProtocolChatHighlightKind.other,
        )
        assertEquals(ChatHighlightKind.entries.toSet(), expected.keys)
        for ((kind, watchKind) in expected) {
            assertEquals(watchKind, highlight(kind).toWatchProtocol().kind, kind.name)
        }
    }

    @Test
    fun watchProtocolCarriesColorImageAndTitleWithoutEmotes() {
        val watch = highlight(ChatHighlightKind.Other).toWatchProtocol()
        val rgb = Color(0xFF32ADE6).toRgb()!!
        assertEquals(rgb.red, watch.barColor.red)
        assertEquals(rgb.green, watch.barColor.green)
        assertEquals(rgb.blue, watch.barColor.blue)
        assertEquals("party.popper", watch.image)
        assertEquals("New subscriber", watch.title)
    }

    @Test
    fun factoriesUseSwiftUISystemColors() {
        assertEquals(Color(0xFF34C759), ChatHighlight.makeAnnouncement().barColor)
        assertEquals(Color(0xFFFFCC00), ChatHighlight.makeFirstMessage().barColor)
        assertEquals(Color(0xFFFF9500), ChatHighlight.makePaidMessage().barColor)
        assertEquals(Color(0xFF34C759), ChatHighlight.makePaidSticker().barColor)
        assertEquals(Color(0xFF007AFF), ChatHighlight.makeMember().barColor)
        assertEquals(Color(0xFF007AFF), ChatHighlight.makeGiftedMemberships().barColor)
        assertEquals(Color(0xFF007AFF), ChatHighlight.makeJewels().barColor)
        assertEquals(Color(0xFFAF52DE), ChatHighlight.makeGigantifiedEmote().barColor)
        assertEquals(Color(0xFF34C759), ChatHighlight.makeModerator().barColor)
        assertEquals(Color(0xFF34C759), ChatHighlight.makeRemoteControlAssistant().barColor)
    }

    @Test
    fun replyMessagesUseSwiftUIGrayAndOthersTheDefault() {
        val reply = ChatHighlight.makeReply(user = "user", segments = makeChatPostTextSegments(text = "hi"))
        assertEquals(Color.Transparent, reply.barColor)
        assertEquals(Color(0xFF8E8E93), reply.messageColor())
        assertEquals(Color.White, highlight(ChatHighlightKind.Other).messageColor())
        assertEquals(Color.Red, highlight(ChatHighlightKind.Other).messageColor(defaultColor = Color.Red))
    }

    @Test
    fun copyGivesANewPostWithTheNewIdAndTheSameState() {
        val post = ChatPost(
            id = 3,
            messageId = "m",
            displayName = "Display",
            user = "user",
            userId = "42",
            userColor = RgbColor(red = 1, green = 2, blue = 3),
            userBadges = listOf("https://badge"),
            segments = makeChatPostTextSegments(text = "hello world"),
            timestamp = "12:00",
            timestampTime = Instant.ofEpochSecond(1000),
            isAction = true,
            isSubscriber = true,
            bits = "100",
            highlight = highlight(ChatHighlightKind.Redemption),
            live = true,
            filter = null,
            platform = Platform.kick,
            sourceChannelIcon = "https://icon",
            state = ChatPostState(),
        )
        val copy = post.copy(id = 7)
        assertNotSame(post, copy)
        assertEquals(3, post.id)
        assertEquals(7, copy.id)
        assertEquals(post.text(), copy.text())
        assertEquals(post.userId, copy.userId)
        assertEquals(post.platform, copy.platform)
        assertEquals(post.bits, copy.bits)
        assertSame(post.highlight, copy.highlight)
        assertSame(post.state, copy.state)
        assertEquals(post.isRedemption(), copy.isRedemption())
    }
}
