package com.moblin.android.streamingplatforms.youtube

import com.moblin.android.runMainTest
import com.moblin.android.spokenEmoteNames
import com.moblin.android.texts
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.settings.SettingsStreamChat
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private data class AppendedMessage(
    val user: String,
    val userId: String?,
    val segments: List<ChatPostSegment>,
    val isModerator: Boolean,
    val isOwner: Boolean,
    val highlight: ChatHighlight?,
)

private class Delegate : YouTubeLiveChatDelegate {
    val messages = mutableListOf<AppendedMessage>()

    override fun youTubeLiveChatMakeErrorToast(title: String, subTitle: String) {}

    override fun youTubeLiveChatMakeToast(title: String) {}

    override fun youTubeLiveChatAppendMessage(
        user: String,
        userId: String?,
        segments: List<ChatPostSegment>,
        isModerator: Boolean,
        isOwner: Boolean,
        highlight: ChatHighlight?,
    ) {
        messages.add(
            AppendedMessage(
                user = user,
                userId = userId,
                segments = segments,
                isModerator = isModerator,
                isOwner = isOwner,
                highlight = highlight,
            ),
        )
    }
}

private fun makeGetLiveChat(actions: List<String>, continuation: String? = "next"): ByteArray {
    val continuations = if (continuation != null) {
        """[{"invalidationContinuationData": {"continuation": "$continuation"}}]"""
    } else {
        "[]"
    }
    return """
{
    "continuationContents": {
        "liveChatContinuation": {
            "continuations": $continuations,
            "actions": [${actions.joinToString(",")}]
        }
    }
}
""".trimIndent().toByteArray()
}

private fun makeAction(item: String): String {
    return """{"addChatItemAction": {"item": {$item}}}"""
}

private fun makeTextMessage(author: String = "Viewer", runs: String, badges: String = "[]"): String {
    return makeAction(
        """
"liveChatTextMessageRenderer": {
    "authorName": {"simpleText": "$author"},
    "authorExternalChannelId": "UC123",
    "message": {"runs": $runs},
    "authorBadges": $badges
}
""".trimIndent(),
    )
}

private fun makeBadge(iconType: String): String {
    return """{"liveChatAuthorBadgeRenderer": {"icon": {"iconType": "$iconType"}}}"""
}

private val emojiRun = """
{
    "emoji": {
        "emojiId": "UCkszU2WH9gy1mb0dV-11UJg/1",
        "shortcuts": [":yt:", ":youtube:"],
        "image": {"thumbnails": [{"url": "https://yt3.example.com/yt.png"}]}
    }
}
""".trimIndent()

@RunWith(RobolectricTestRunner::class)
class YouTubeLiveChatSuite {
    private fun handle(data: ByteArray): List<AppendedMessage> {
        val delegate = Delegate()
        val chat = YouTubeLiveChat(delegate = delegate, videoId = "video", settings = SettingsStreamChat())
        chat.handleGetLiveChat(data)
        return delegate.messages
    }

    @Test
    fun textMessage() = runMainTest {
        val messages = handle(
            makeGetLiveChat(
                actions = listOf(
                    makeTextMessage(runs = """[{"text": "hello world"}]"""),
                ),
            ),
        )
        assertEquals(1, messages.size)
        val message = messages[0]
        assertEquals("Viewer", message.user)
        assertEquals("UC123", message.userId)
        assertEquals(listOf<String?>("hello ", "world "), texts(message.segments))
        assertFalse(message.isOwner)
        assertFalse(message.isModerator)
        assertNull(message.highlight)
    }

    @Test
    fun emoji() = runMainTest {
        val messages = handle(
            makeGetLiveChat(
                actions = listOf(
                    makeTextMessage(runs = """[{"text": "hi "}, $emojiRun, {"text": " there"}]"""),
                ),
            ),
        )
        val segments = messages[0].segments
        assertEquals(listOf<String?>("hi ", null, "there "), texts(segments))
        assertEquals("https://yt3.example.com/yt.png", segments[1].url?.still)
        assertEquals(segments.size, segments.map { it.id }.toSet().size)
        assertEquals(listOf<String?>(null, "yt", null), spokenEmoteNames(segments))
    }

    @Test
    fun emojiWithoutShortcutsHasNoName() = runMainTest {
        val emoji = """{"emoji": {"image": {"thumbnails": [{"url": "https://yt3.example.com/a.png"}]}}}"""
        val messages = handle(makeGetLiveChat(actions = listOf(makeTextMessage(runs = "[$emoji]"))))
        assertTrue(messages[0].segments[0].url != null)
        assertEquals(listOf<String?>(null), spokenEmoteNames(messages[0].segments))
    }

    @Test
    fun emojiWithoutThumbnailIsSkipped() = runMainTest {
        val messages = handle(
            makeGetLiveChat(
                actions = listOf(
                    makeTextMessage(runs = """[{"text": "hi"}, {"emoji": {"image": {"thumbnails": []}}}]"""),
                ),
            ),
        )
        assertEquals(listOf<String?>("hi "), texts(messages[0].segments))
    }

    @Test
    fun ownerAndModeratorBadges() = runMainTest {
        val messages = handle(
            makeGetLiveChat(
                actions = listOf(
                    makeTextMessage(runs = """[{"text": "a"}]""", badges = "[${makeBadge("OWNER")}]"),
                    makeTextMessage(runs = """[{"text": "b"}]""", badges = "[${makeBadge("MODERATOR")}]"),
                    makeTextMessage(runs = """[{"text": "c"}]""", badges = "[${makeBadge("VERIFIED")}, {}]"),
                ),
            ),
        )
        assertEquals(listOf(true, false, false), messages.map { it.isOwner })
        assertEquals(listOf(false, true, false), messages.map { it.isModerator })
    }

    @Test
    fun emptyTextMessageIsDropped() = runMainTest {
        val messages = handle(
            makeGetLiveChat(
                actions = listOf(
                    makeTextMessage(runs = "[]"),
                    makeTextMessage(runs = """[{"text": "kept"}]"""),
                ),
            ),
        )
        assertEquals(listOf(listOf<String?>("kept ")), messages.map { texts(it.segments) })
    }

    @Test
    fun paidMessage() = runMainTest {
        val messages = handle(
            makeGetLiveChat(
                actions = listOf(
                    makeAction(
                        """
"liveChatPaidMessageRenderer": {
    "authorName": {"simpleText": "Fan"},
    "purchaseAmountText": {"simpleText": "${'$'}5.00"},
    "message": {"runs": [{"text": "great stream"}]}
}
""".trimIndent(),
                    ),
                ),
            ),
        )
        val message = messages[0]
        assertEquals("Fan", message.user)
        assertNull(message.userId)
        assertEquals("message", message.highlight?.image)
        val text = message.segments.mapNotNull { it.text }.joinToString("")
        assertTrue(text.contains("\$5.00"))
        assertTrue(text.endsWith("great stream "))
    }

    @Test
    fun paidStickerWithoutMessage() = runMainTest {
        val messages = handle(
            makeGetLiveChat(
                actions = listOf(
                    makeAction(
                        """
"liveChatPaidStickerRenderer": {
    "authorName": {"simpleText": "Fan"},
    "purchaseAmountText": {"simpleText": "€2.00"}
}
""".trimIndent(),
                    ),
                ),
            ),
        )
        val message = messages[0]
        assertEquals("doc.plaintext", message.highlight?.image)
        assertTrue(message.segments.mapNotNull { it.text }.joinToString("").contains("€2.00"))
    }

    @Test
    fun membershipWithoutTextIsKept() = runMainTest {
        val messages = handle(
            makeGetLiveChat(
                actions = listOf(
                    makeAction(
                        """
"liveChatMembershipItemRenderer": {
    "authorName": {"simpleText": "Member"}
}
""".trimIndent(),
                    ),
                ),
            ),
        )
        assertEquals(1, messages.size)
        assertTrue(messages[0].segments.isEmpty())
        assertEquals("medal", messages[0].highlight?.image)
    }

    @Test
    fun membershipHeaderSubtextComesBeforeMessage() = runMainTest {
        val messages = handle(
            makeGetLiveChat(
                actions = listOf(
                    makeAction(
                        """
"liveChatMembershipItemRenderer": {
    "authorName": {"simpleText": "Member"},
    "headerSubtext": {"runs": [{"text": "Welcome"}]},
    "message": {"runs": [{"text": "hi"}]}
}
""".trimIndent(),
                    ),
                ),
            ),
        )
        assertEquals(listOf<String?>("Welcome ", "hi "), texts(messages[0].segments))
    }

    @Test
    fun giftPurchase() = runMainTest {
        val messages = handle(
            makeGetLiveChat(
                actions = listOf(
                    makeAction(
                        """
"liveChatSponsorshipsGiftPurchaseAnnouncementRenderer": {
    "header": {
        "liveChatSponsorshipsHeaderRenderer": {
            "authorName": {"simpleText": "Gifter"},
            "authorExternalChannelId": "UC456",
            "primaryText": {"runs": [{"text": "Gifted "}, {"text": "5"}, {"text": " memberships"}]},
            "authorBadges": [${makeBadge("MODERATOR")}]
        }
    }
}
""".trimIndent(),
                    ),
                ),
            ),
        )
        val message = messages[0]
        assertEquals("Gifter", message.user)
        assertEquals("UC456", message.userId)
        assertTrue(message.isModerator)
        assertEquals("gift", message.highlight?.image)
        assertEquals(listOf<String?>("Gifted ", "5 ", "memberships "), texts(message.segments))
    }

    @Test
    fun giftPurchaseWithoutTextIsDropped() = runMainTest {
        val messages = handle(
            makeGetLiveChat(
                actions = listOf(
                    makeAction(
                        """
"liveChatSponsorshipsGiftPurchaseAnnouncementRenderer": {
    "header": {
        "liveChatSponsorshipsHeaderRenderer": {
            "authorName": {"simpleText": "Gifter"}
        }
    }
}
""".trimIndent(),
                    ),
                ),
            ),
        )
        assertTrue(messages.isEmpty())
    }

    @Test
    fun giftRedemption() = runMainTest {
        val messages = handle(
            makeGetLiveChat(
                actions = listOf(
                    makeAction(
                        """
"liveChatSponsorshipsGiftRedemptionAnnouncementRenderer": {
    "authorName": {"simpleText": "Lucky"},
    "message": {"runs": [{"text": "received a gift membership"}]}
}
""".trimIndent(),
                    ),
                ),
            ),
        )
        assertEquals("Lucky", messages[0].user)
        assertEquals("gift", messages[0].highlight?.image)
    }

    @Test
    fun giftMessageViewModel() = runMainTest {
        val messages = handle(
            makeGetLiveChat(
                actions = listOf(
                    makeAction(
                        """
"giftMessageViewModel": {
    "authorName": {"content": "Jeweler"},
    "text": {"content": "sent Girl power for 50 Jewels"}
}
""".trimIndent(),
                    ),
                ),
            ),
        )
        val message = messages[0]
        assertEquals("Jeweler", message.user)
        assertNull(message.userId)
        assertEquals("diamond", message.highlight?.image)
        assertEquals("sent Girl power for 50 Jewels ", message.segments.mapNotNull { it.text }.joinToString(""))
    }

    @Test
    fun unknownActionsAreIgnored() = runMainTest {
        val messages = handle(
            makeGetLiveChat(
                actions = listOf(
                    """{"markChatItemAsDeletedAction": {"targetItemId": "abc"}}""",
                    makeAction("\"liveChatViewerEngagementMessageRenderer\": {\"id\": \"x\"}"),
                    makeTextMessage(runs = """[{"text": "hi"}]"""),
                ),
            ),
        )
        assertEquals(1, messages.size)
    }

    @Test
    fun noActions() = runMainTest {
        val messages = handle(
            """
{
    "continuationContents": {
        "liveChatContinuation": {
            "continuations": [{"invalidationContinuationData": {"continuation": "abc"}}]
        }
    }
}
""".trimIndent().toByteArray(),
        )
        assertTrue(messages.isEmpty())
    }

    @Test
    fun missingContinuationThrows() = runMainTest {
        assertFailsWith<Exception> {
            handle(makeGetLiveChat(actions = emptyList(), continuation = null))
        }
    }

    @Test
    fun invalidJsonThrows() = runMainTest {
        assertFailsWith<Exception> {
            handle("{}".toByteArray())
        }
    }
}
