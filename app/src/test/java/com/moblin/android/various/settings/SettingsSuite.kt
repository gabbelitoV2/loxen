package com.moblin.android.various.settings

import com.moblin.android.chat.ChatMessageSegment
import com.moblin.android.various.network.DefaultTcpPorts
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class SettingsSuite {
    @Test
    fun streamUrlSchemeSelectsProtocol() {
        val stream = SettingsStream(name = "Test")
        stream.url = "mobcam://localhost:7777"
        assertEquals(SettingsStreamProtocol.mobcam, stream.getProtocol())
        assertEquals(SettingsStreamDetailedProtocol.mobcam, stream.getDetailedProtocol())
        assertEquals("Mobcam", stream.protocolString())
        assertEquals(7777, stream.mobcamPort())
        assertFalse(stream.isBonding())
        stream.url = "mobcam://localhost"
        assertEquals(DefaultTcpPorts.mobcamStream, stream.mobcamPort())
    }

    @Test
    fun chatFilter() {
        val filter = SettingsChatFilter()
        filter.enabled = true
        filter.user = ""
        filter.messageStartWords = mutableListOf("!")
        assertTrue(
            filter.isMatching(
                user = "erik",
                segments = mutableListOf(ChatMessageSegment(id = 0, text = "!moblin")),
            ),
        )
        assertTrue(
            filter.isMatching(
                user = "erik",
                segments = mutableListOf(ChatMessageSegment(id = 0, text = "!")),
            ),
        )
        assertFalse(
            filter.isMatching(
                user = "erik",
                segments = mutableListOf(ChatMessageSegment(id = 0, text = "@foo")),
            ),
        )
        assertFalse(
            filter.isMatching(
                user = "erik",
                segments = mutableListOf(ChatMessageSegment(id = 0, text = "@")),
            ),
        )
        filter.messageStartWords = mutableListOf("hell", "h")
        assertTrue(
            filter.isMatching(
                user = "erik",
                segments = mutableListOf(
                    ChatMessageSegment(id = 0, text = "hell"),
                    ChatMessageSegment(id = 0, text = "hi"),
                    ChatMessageSegment(id = 0, text = "ho"),
                ),
            ),
        )
        assertFalse(
            filter.isMatching(
                user = "erik",
                segments = mutableListOf(
                    ChatMessageSegment(id = 0, text = "hello"),
                    ChatMessageSegment(id = 0, text = "hi"),
                    ChatMessageSegment(id = 0, text = "ho"),
                ),
            ),
        )
    }
}
