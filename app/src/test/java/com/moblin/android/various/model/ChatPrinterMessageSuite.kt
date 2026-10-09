package com.moblin.android.various.model

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import com.moblin.android.common.various.RgbColor
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.various.ChatPost
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.ChatPostState
import com.moblin.android.various.ChatPostEmote
import com.moblin.android.various.makeChatPostTextSegments
import com.moblin.android.various.settings.SettingsChat
import java.time.Instant
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w600dp-h800dp-mdpi")
class ChatPrinterMessageSuite {
    @get:Rule
    val rule = createComposeRule()

    private fun post(segments: List<ChatPostSegment>): ChatPost {
        return ChatPost(
            id = 1,
            messageId = "m",
            displayName = "Gabbe",
            user = "gabbe",
            userId = "1",
            userColor = RgbColor(red = 1, green = 2, blue = 3),
            userBadges = emptyList(),
            segments = segments,
            timestamp = "",
            timestampTime = Instant.now(),
            isAction = false,
            isSubscriber = false,
            bits = null,
            highlight = null,
            live = true,
            filter = null,
            platform = Platform.twitch,
            sourceChannelIcon = null,
            state = ChatPostState(),
        )
    }

    private fun images(node: LayoutInfo): Int {
        val getChildren = node.javaClass.methods.first { it.name.startsWith("getChildren") && it.parameterCount == 0 }
        val children = getChildren.invoke(node) as List<*>
        val painters = node.getModifierInfo().count { it.modifier.javaClass.name.contains("Painter") }
        return painters + children.sumOf { images(it as LayoutInfo) }
    }

    private fun show(post: ChatPost, chat: SettingsChat) {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                Box(modifier = Modifier.testTag("message")) {
                    ChatPrinterMessage(post = post, chat = chat)
                }
            }
        }
    }

    @Test
    fun messageIsAsWideAsThePrinter() {
        val chat = SettingsChat()
        val post = post(makeChatPostTextSegments(text = "hello world"))
        show(post, chat)
        val message = rule.onNodeWithTag("message").fetchSemanticsNode()
        assertEquals(384, message.size.width)
        val name = post.displayName(nicknames = chat.nicknames, displayStyle = chat.displayStyle)
        rule.onNodeWithText(name, useUnmergedTree = true).assertExists()
        rule.onNodeWithText(": ", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("hello", substring = true, useUnmergedTree = true).assertExists()
        assertEquals(0, images(message.layoutInfo))
    }

    @Test
    fun emoteThatIsNotLoadedYetShowsTheAppIcon() {
        val segments = listOf(
            ChatPostSegment(id = 0, text = "hi "),
            ChatPostSegment(id = 1, url = ChatPostEmote(moving = null, still = "http://127.0.0.1:1/emote.png")),
        )
        show(post(segments), SettingsChat())
        val message = rule.onNodeWithTag("message").fetchSemanticsNode()
        assertEquals(384, message.size.width)
        assertEquals(1, images(message.layoutInfo))
    }
}
