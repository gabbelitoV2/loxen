package com.moblin.android.various

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Test
import com.moblin.android.streamingplatforms.Platform
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatBotCommandSuite {
    @Test
    fun simplePopFirst() {
        val command = createCommand("!moblin widget Foo enable")
        assertEquals("widget Foo enable", command.rest())
        assertEquals("widget", command.popFirst())
        assertEquals("Foo", command.popFirst())
        assertEquals("enable", command.popFirst())
        assertEquals<String?>(null, command.popFirst())
        assertEquals("", command.rest())
    }

    @Test
    fun simplePopAll() {
        val command = createCommand("!moblin widget Foo enable")
        assertEquals("widget Foo enable", command.rest())
        assertEquals(listOf("widget", "Foo", "enable"), command.popAll())
        assertEquals("", command.rest())
    }

    @Test
    fun quotesPopFirst() {
        val command = createCommand("!moblin widget \"My Foo 1\" enable \"\" a")
        assertEquals("widget \"My Foo 1\" enable \"\" a", command.rest())
        assertEquals("widget", command.popFirst())
        assertEquals("My Foo 1", command.popFirst())
        assertEquals("enable", command.popFirst())
        assertEquals("\"\" a", command.rest())
        assertEquals("", command.popFirst())
        assertEquals("a", command.popFirst())
        assertEquals<String?>(null, command.popFirst())
        assertEquals("", command.rest())
    }

    @Test
    fun quotesPopAll() {
        val command = createCommand("!moblin widget \"My Foo 1\" enable \"\" a")
        assertEquals(listOf("widget", "My Foo 1", "enable", "", "a"), command.popAll())
        assertEquals("", command.rest())
    }

    @Test
    fun whitespaces() {
        val command = createCommand("!moblin  widget  \"My   Foo 1\"  enable \"    \"  a")
        assertEquals(listOf("widget", "My Foo 1", "enable", " ", "a"), command.popAll())
        assertEquals("", command.rest())
    }

    @Test
    fun fuzzyPopFirst() {
        val command = createCommand("!moblin Snapshto hello")
        assertEquals(ChatBotMainArgument.snapshot, command.popFirstArgument<ChatBotMainArgument>())
        assertEquals<ChatBotMainArgument?>(null, command.popFirstArgument<ChatBotMainArgument>())
        assertEquals<ChatBotMainArgument?>(null, command.popFirstArgument<ChatBotMainArgument>())
    }

    @Test
    fun fuzzyMatch() {
        val cases: List<Pair<String, ChatBotMainArgument?>> = listOf(
            "snapshot" to ChatBotMainArgument.snapshot,
            "SNAPSHOT" to ChatBotMainArgument.snapshot,
            "snapsht" to ChatBotMainArgument.snapshot,
            "snapshhot" to ChatBotMainArgument.snapshot,
            "snpashot" to ChatBotMainArgument.snapshot,
            "snap" to null,
            "hot" to null,
            "s" to null,
            "zom" to ChatBotMainArgument.zoom,
            "zoomm" to ChatBotMainArgument.zoom,
            "mure" to ChatBotMainArgument.mute,
            "mtue" to null,
            "umute" to null,
            "unmtue" to ChatBotMainArgument.unmute,
            "sey" to ChatBotMainArgument.say,
            "scen" to ChatBotMainArgument.scene,
            "fliter" to ChatBotMainArgument.filter,
            "hepl" to ChatBotMainArgument.help,
            "hello" to null,
            "a" to null,
            "ai" to ChatBotMainArgument.ai,
            "raid" to null,
        )
        for ((word, expected) in cases) {
            val command = createCommand("!moblin $word")
            assertEquals(expected, command.popFirstArgument<ChatBotMainArgument>())
        }
    }

    @Test
    fun fuzzyMatchShortWords() {
        val cases: List<Pair<String, ChatBotOnOffArgument?>> = listOf(
            "on" to ChatBotOnOffArgument.on,
            "onn" to ChatBotOnOffArgument.on,
            "of" to ChatBotOnOffArgument.off,
            "no" to null,
            "o" to null,
        )
        for ((word, expected) in cases) {
            val command = createCommand("!moblin $word")
            assertEquals(expected, command.popFirstArgument<ChatBotOnOffArgument>())
        }
    }

    @Test
    fun fuzzyMatchFilter() {
        val cases: List<Pair<String, ChatBotFilterArgument?>> = listOf(
            "pixellate" to ChatBotFilterArgument.pixellate,
            "pixele" to ChatBotFilterArgument.pixellate,
            "pixlate" to ChatBotFilterArgument.pixellate,
            "pixeelaate" to ChatBotFilterArgument.pixellate,
            "4:3" to ChatBotFilterArgument.fourThree,
            "greyscle" to ChatBotFilterArgument.grayscale,
            "movi" to ChatBotFilterArgument.movie,
        )
        for ((word, expected) in cases) {
            val command = createCommand("!moblin $word")
            assertEquals(expected, command.popFirstArgument<ChatBotFilterArgument>())
        }
    }

    @Test
    fun fuzzyMatchReaction() {
        val cases: List<Pair<String, ChatBotReactionArgument?>> = listOf(
            "rain" to ChatBotReactionArgument.rain,
            "rai" to ChatBotReactionArgument.rain,
            "balloons" to ChatBotReactionArgument.balloons,
            "balons" to ChatBotReactionArgument.balloons,
        )
        for ((word, expected) in cases) {
            val command = createCommand("!moblin $word")
            assertEquals(expected, command.popFirstArgument<ChatBotReactionArgument>())
        }
    }

    private fun createMessage(text: String): ChatBotMessage {
        return ChatBotMessage(
            platform = Platform.twitch,
            user = "erik",
            isOwner = true,
            isModerator = true,
            isSubscriber = false,
            userId = "1234",
            segments = makeChatPostTextSegments(text = text),
        )
    }

    private fun createCommand(text: String): ChatBotCommand {
        return assertNotNull(ChatBotCommand(createMessage(text = text), emptyList()))
    }
}
