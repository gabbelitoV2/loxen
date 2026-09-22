package com.moblin.android.streamingplatforms.twitch

import com.moblin.android.common.various.RgbColor
import com.moblin.android.texts
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.network.WebSocketClient
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

private fun twitchEmoteIds(segments: List<ChatPostSegment>): List<String?> {
    return segments.map { segment ->
        val still = segment.url?.still
        if (still == null) {
            null
        } else {
            URI(still).path?.split("/")?.filter { it.isNotEmpty() }?.drop(2)?.firstOrNull()
        }
    }
}

private data class DelegateMessage(
    val displayName: String,
    val segments: List<ChatPostSegment>,
    val isAction: Boolean,
)

private class Delegate : TwitchChatDelegate {
    val messages: MutableList<DelegateMessage> = mutableListOf()

    override fun twitchChatMakeErrorToast(title: String, subTitle: String?) {}

    override fun twitchChatAppendMessage(
        messageId: String?,
        displayName: String,
        user: String,
        userId: String?,
        userColor: RgbColor?,
        userBadges: List<String>,
        segments: List<ChatPostSegment>,
        isAction: Boolean,
        isSubscriber: Boolean,
        isModerator: Boolean,
        bits: String?,
        highlight: ChatHighlight?,
        sourceChannelIcon: String?,
    ) {
        messages.add(DelegateMessage(displayName, segments, isAction))
    }

    override fun twitchChatDeleteMessage(messageId: String) {}

    override fun twitchChatDeleteUser(userId: String) {}
}

class TwitchChatSuite {
    @Test
    fun emptyMessage() {
        val message = runCatching { TwitchChatMessage("") }.getOrNull()
        assertNull(message)
    }

    @Test
    fun basicMessage() {
        val message = TwitchChatMessage(
            "@badge-info=subscriber/13;" +
                "badges=broadcaster/1,subscriber/0,turbo/1;" +
                "client-nonce=11b2e915221ab4bcfb44714bda0fb575;" +
                "color=;" +
                "display-name=eerimoq;" +
                "emotes=;" +
                "first-msg=0;" +
                "flags=;" +
                "id=52db2f3d-cc5a-46ea-ba0b-bd910579c248;" +
                "mod=0;" +
                "returning-chatter=0;" +
                "room-id=63482386;" +
                "subscriber=1;" +
                "tmi-sent-ts=1760946171865;" +
                "turbo=1;" +
                "user-id=63482386;" +
                "user-type= " +
                ":eerimoq!eerimoq@eerimoq.tmi.twitch.tv " +
                "PRIVMSG " +
                "#eerimoq " +
                ":hi all",
        )
        assertEquals(TwitchChatCommand.PrivateMessage, message.command)
        assertEquals(listOf("#eerimoq", "hi all"), message.parameters)
        assertEquals("eerimoq", message.displayName)
        assertEquals("eerimoq", message.user)
        assertEquals("63482386", message.userId)
        assertNull(message.color)
        assertTrue(message.emotes.isEmpty())
        assertEquals(listOf("broadcaster/1", "subscriber/0", "turbo/1"), message.badges)
        assertNull(message.messageId)
        assertEquals("52db2f3d-cc5a-46ea-ba0b-bd910579c248", message.id)
        assertFalse(message.firstMessage)
        assertTrue(message.subscriber)
        assertFalse(message.moderator)
        assertNull(message.bits)
        assertNull(message.replySender)
        assertNull(message.replyText)
        assertNull(message.targetMessageId)
        assertNull(message.targetUserId)
    }

    @Test
    fun botRixMessage() {
        val message = TwitchChatMessage(
            "@badge-info=;" +
                "badges=moderator/1,bot-badge/1;" +
                "color=#179451;" +
                "display-name=BotRixOficial;" +
                "emotes=;" +
                "first-msg=0;" +
                "flags=;" +
                "id=b8dc3c37-cb4b-4f7a-a011-52eeae902cb2;" +
                "mod=1;" +
                "returning-chatter=0;" +
                "room-id=63482386;" +
                "subscriber=0;" +
                "tmi-sent-ts=1786194630483;" +
                "turbo=0;" +
                "user-id=646848961;" +
                "user-type=mod " +
                ":botrixoficial!botrixoficial@botrixoficial.tmi.twitch.tv " +
                "PRIVMSG " +
                "#eerimoq " +
                ":the test message",
        )
        assertEquals(TwitchChatCommand.PrivateMessage, message.command)
        assertEquals(listOf("#eerimoq", "the test message"), message.parameters)
        assertEquals("BotRixOficial", message.displayName)
        assertEquals("botrixoficial", message.user)
        assertEquals("646848961", message.userId)
        assertEquals("#179451", message.color)
        assertTrue(message.emotes.isEmpty())
        assertEquals(listOf("moderator/1", "bot-badge/1"), message.badges)
        assertNull(message.messageId)
        assertEquals("b8dc3c37-cb4b-4f7a-a011-52eeae902cb2", message.id)
        assertFalse(message.firstMessage)
        assertFalse(message.subscriber)
        assertTrue(message.moderator)
        assertNull(message.bits)
        assertNull(message.replySender)
        assertNull(message.replyText)
        assertNull(message.targetMessageId)
        assertNull(message.targetUserId)
    }

    @Test
    fun unescapedTagValues() {
        val message = TwitchChatMessage(
            "@display-name=eerimoq;" +
                "reply-parent-display-name=someone;" +
                "reply-parent-msg-body=a\\sb\\:c\\rd\\ne\\\\f\\qg " +
                ":eerimoq!eerimoq@eerimoq.tmi.twitch.tv " +
                "PRIVMSG " +
                "#eerimoq " +
                ":hi",
        )
        assertEquals("someone", message.replySender)
        assertEquals("a b;c\rd\ne\\fqg", message.replyText)
    }

    @Test
    fun unicodeWhitespaces() {
        val message = TwitchChatMessage(
            "@display-name=eerimoq;" +
                "reply-parent-display-name=someone;" +
                "reply-parent-msg-body=こんにちは\u3000みなさん " +
                ":eerimoq!eerimoq@eerimoq.tmi.twitch.tv " +
                "PRIVMSG " +
                "#eerimoq " +
                ":hi\u00A0all",
        )
        assertEquals("こんにちは\u3000みなさん", message.replyText)
        assertEquals(listOf("#eerimoq", "hi\u00A0all"), message.parameters)
    }

    @Test
    fun unescapedTagValueEndingInBackslash() {
        val message = TwitchChatMessage(
            "@reply-parent-msg-body=a\\sb\\ " +
                ":eerimoq!eerimoq@eerimoq.tmi.twitch.tv " +
                "PRIVMSG " +
                "#eerimoq " +
                ":hi",
        )
        assertEquals("a b", message.replyText)
    }

    @Test
    fun gifMessage() {
        val message = TwitchChatMessage(
            "@badge-info=subscriber/24;" +
                "badges=broadcaster/1,subscriber/0,sub-gifter/1;" +
                "color=;" +
                "display-name=eerimoq;" +
                "emotes=;" +
                "first-msg=0;" +
                "flags=;" +
                "gifs=0-34|l0MYDEPLWRWbJoRuU|https://media3.giphy.com/media/l0MYDEPLWRWbJoRuU/giphy.gif?" +
                "cid=095d7a5dpq5y4f8xwwlqk053r89k5ezmzgauu6wyjbrun0k0&ep=v1_gifs_trending&rid=giphy.gif&ct=g;" +
                "id=f44b10f3-dd19-4b1b-b8be-e99c4e5502e5;" +
                "mod=0;" +
                "returning-chatter=0;" +
                "room-id=63482386;" +
                "subscriber=1;" +
                "tmi-sent-ts=1788449914571;" +
                "turbo=0;" +
                "user-id=63482386;" +
                "user-type= " +
                ":eerimoq!eerimoq@eerimoq.tmi.twitch.tv " +
                "PRIVMSG " +
                "#eerimoq " +
                ":[George Costanza Hello GIF by HULU]",
        )
        assertEquals(TwitchChatCommand.PrivateMessage, message.command)
        assertEquals(listOf("#eerimoq", "[George Costanza Hello GIF by HULU]"), message.parameters)
        assertEquals(1, message.emotes.size)
        val gif = message.emotes.first()
        assertTrue(gif.isGif)
        assertEquals(0..34, gif.range)
        assertEquals(
            "https://media3.giphy.com/media/l0MYDEPLWRWbJoRuU/100.gif?" +
                "cid=095d7a5dpq5y4f8xwwlqk053r89k5ezmzgauu6wyjbrun0k0&ep=v1_gifs_trending&rid=giphy.gif&ct=g",
            gif.url,
        )
    }

    @Test
    fun gifAndEmotesMessage() {
        val message = TwitchChatMessage(
            "@gifs=6-10|abc|https://media.giphy.com/media/abc/giphy.gif;" +
                "emotes=25:0-4 " +
                ":eerimoq!eerimoq@eerimoq.tmi.twitch.tv " +
                "PRIVMSG " +
                "#eerimoq " +
                ":Kappa [gif]",
        )
        assertEquals(2, message.emotes.size)
        assertEquals(listOf(true, false), message.emotes.map { it.isGif })
        assertEquals(listOf(6..10, 0..4), message.emotes.map { it.range })
    }

    @Test
    fun gigantifiedEmoteMessage() {
        val message = TwitchChatMessage(
            "@badge-info=;" +
                "badges=;" +
                "color=;" +
                "display-name=eerimoq;" +
                "emotes=25:0-4,12-16/1902:6-10;" +
                "id=f44b10f3-dd19-4b1b-b8be-e99c4e5502e5;" +
                "msg-id=gigantified-emote-message;" +
                "user-id=63482386 " +
                ":eerimoq!eerimoq@eerimoq.tmi.twitch.tv " +
                "PRIVMSG " +
                "#eerimoq " +
                ":Kappa Keepo Kappa",
        )
        assertEquals(TwitchChatCommand.PrivateMessage, message.command)
        assertTrue(message.isGigantifiedEmote)
        assertEquals(3, message.emotes.size)
        assertEquals(listOf(0..4, 12..16, 6..10), message.emotes.map { it.range })
        assertEquals(listOf(false, false, false), message.emotes.map { it.isGif })
        assertEquals(
            "https://static-cdn.jtvnw.net/emoticons/v2/25/default/dark/3.0",
            message.emotes[1].url,
        )
    }

    @Test
    fun malformedGifTag() {
        val message = TwitchChatMessage(
            "@gifs=0-4|abc " +
                ":eerimoq!eerimoq@eerimoq.tmi.twitch.tv " +
                "PRIVMSG " +
                "#eerimoq " +
                ":[gif]",
        )
        assertTrue(message.emotes.isEmpty())
    }

    @Test
    fun unescapedTagValueWithoutEscapes() {
        val message = TwitchChatMessage(
            "@reply-parent-msg-body=hello;" +
                "display-name=eerimoq " +
                ":eerimoq!eerimoq@eerimoq.tmi.twitch.tv " +
                "PRIVMSG " +
                "#eerimoq " +
                ":hi",
        )
        assertEquals("hello", message.replyText)
        assertEquals("eerimoq", message.displayName)
    }

    @Test
    fun emotesTag() {
        val message = TwitchChatMessage(
            "@display-name=eerimoq;" +
                "emotes=25:0-4,12-16/1902:6-10 " +
                ":eerimoq!eerimoq@eerimoq.tmi.twitch.tv " +
                "PRIVMSG " +
                "#eerimoq " +
                ":Kappa Keepo Kappa",
        )
        assertEquals(listOf(0..4, 12..16, 6..10), message.emotes.map { it.range })
        assertEquals(
            listOf(
                "https://static-cdn.jtvnw.net/emoticons/v2/25/default/dark/3.0",
                "https://static-cdn.jtvnw.net/emoticons/v2/25/default/dark/3.0",
                "https://static-cdn.jtvnw.net/emoticons/v2/1902/default/dark/3.0",
            ),
            message.emotes.map { it.url },
        )
    }

    @Test
    fun announcement() {
        val message = TwitchChatMessage(
            "@badge-info=subscriber/24;" +
                "badges=broadcaster/1,subscriber/0,sub-gifter/1;" +
                "color=;" +
                "display-name=eerimoq;" +
                "emotes=;" +
                "flags=;" +
                "id=e60e8de0-eb07-49b1-aa77-96cb5f5fabb1;" +
                "login=eerimoq;" +
                "mod=0;" +
                "msg-id=announcement;" +
                "msg-param-color=PRIMARY;" +
                "room-id=63482386;" +
                "subscriber=1;" +
                "system-msg=;" +
                "tmi-sent-ts=1788339928132;" +
                "user-id=63482386;" +
                "user-type=;" +
                "vip=0 " +
                ":tmi.twitch.tv " +
                "USERNOTICE " +
                "#eerimoq " +
                ":foobar",
        )
        assertEquals(TwitchChatCommand.UserNotice, message.command)
        assertEquals(listOf("#eerimoq", "foobar"), message.parameters)
        assertEquals("eerimoq", message.displayName)
        assertEquals("eerimoq", message.user)
        assertEquals("63482386", message.userId)
        assertEquals("announcement", message.messageId)
        assertEquals("e60e8de0-eb07-49b1-aa77-96cb5f5fabb1", message.id)
        assertEquals(listOf("broadcaster/1", "subscriber/0", "sub-gifter/1"), message.badges)
    }

    @Test
    fun meMessageIsAction() {
        val delegate = Delegate()
        val chat = TwitchChat(delegate = delegate)
        chat.webSocketClientReceiveMessage(
            WebSocketClient(url = "wss://irc-ws.chat.twitch.tv"),
            string = "@badge-info=subscriber/24;" +
                "badges=broadcaster/1,subscriber/0,sub-gifter/1;" +
                "client-nonce=cf55e555054a4114a1f3b40af7cc1183;" +
                "color=;" +
                "display-name=eerimoq;" +
                "emotes=emotesv2_1927bf80a13d46049223d39d14ea1e40:8-23/425618:2-4;" +
                "first-msg=0;" +
                "flags=;" +
                "id=817ca58d-0717-47dd-9151-23b195eaf3b7;" +
                "mod=0;" +
                "returning-chatter=0;" +
                "room-id=63482386;" +
                "subscriber=1;" +
                "tmi-sent-ts=1789920112420;" +
                "turbo=0;" +
                "user-id=63482386;" +
                "user-type= " +
                ":eerimoq!eerimoq@eerimoq.tmi.twitch.tv " +
                "PRIVMSG " +
                "#eerimoq " +
                ":\u0001ACTION 1 LUL 2 eerimoPartyDance\u0001",
        )
        assertEquals(1, delegate.messages.size)
        assertTrue(delegate.messages.first().isAction)
        assertEquals("eerimoq", delegate.messages.first().displayName)
        val segments = delegate.messages.first().segments
        assertEquals(listOf("1 ", null, "", "2 ", null, ""), texts(segments))
        assertEquals(
            listOf(
                null,
                "425618",
                null,
                null,
                "emotesv2_1927bf80a13d46049223d39d14ea1e40",
                null,
            ),
            twitchEmoteIds(segments),
        )
    }

    @Test
    fun clearMessage() {
        val message = TwitchChatMessage(
            "@login=eerimoq;" +
                "target-msg-id=8b4f-4dda-a4e6 " +
                ":tmi.twitch.tv " +
                "CLEARMSG " +
                "#eerimoq " +
                ":bye",
        )
        assertEquals(TwitchChatCommand.ClearMsg, message.command)
        assertEquals("8b4f-4dda-a4e6", message.targetMessageId)
    }

    @Test
    fun clearChat() {
        val message = TwitchChatMessage(
            "@target-user-id=63482386 " +
                ":tmi.twitch.tv " +
                "CLEARCHAT " +
                "#eerimoq " +
                ":baduser",
        )
        assertEquals(TwitchChatCommand.ClearChat, message.command)
        assertEquals("63482386", message.targetUserId)
    }

    @Test
    fun ping() {
        val message = TwitchChatMessage("PING :tmi.twitch.tv")
        assertEquals(TwitchChatCommand.Ping, message.command)
        assertEquals(listOf("tmi.twitch.tv"), message.parameters)
    }

    @Test
    fun unknownCommand() {
        assertNull(runCatching { TwitchChatMessage(":tmi.twitch.tv 001 eerimoq :Welcome") }.getOrNull())
    }
}
