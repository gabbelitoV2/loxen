package com.moblin.android.various.settings

import com.moblin.android.platform.codable.codableJson
import com.moblin.android.various.network.DefaultTcpPorts
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test
import com.moblin.android.various.ChatPostSegment
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
                segments = mutableListOf(ChatPostSegment(id = 0, text = "!moblin")),
            ),
        )
        assertTrue(
            filter.isMatching(
                user = "erik",
                segments = mutableListOf(ChatPostSegment(id = 0, text = "!")),
            ),
        )
        assertFalse(
            filter.isMatching(
                user = "erik",
                segments = mutableListOf(ChatPostSegment(id = 0, text = "@foo")),
            ),
        )
        assertFalse(
            filter.isMatching(
                user = "erik",
                segments = mutableListOf(ChatPostSegment(id = 0, text = "@")),
            ),
        )
        filter.messageStartWords = mutableListOf("hell", "h")
        assertTrue(
            filter.isMatching(
                user = "erik",
                segments = mutableListOf(
                    ChatPostSegment(id = 0, text = "hell"),
                    ChatPostSegment(id = 0, text = "hi"),
                    ChatPostSegment(id = 0, text = "ho"),
                ),
            ),
        )
        assertFalse(
            filter.isMatching(
                user = "erik",
                segments = mutableListOf(
                    ChatPostSegment(id = 0, text = "hello"),
                    ChatPostSegment(id = 0, text = "hi"),
                    ChatPostSegment(id = 0, text = "ho"),
                ),
            ),
        )
    }

    @Test
    fun textToSpeechVoicesWithoutElevenLabsDecode() {
        val json = """{"textToSpeechLanguageVoices":{"en":{"type":"ttsMonster","apple":{"voice":""},"ttsMonster":{"name":"Axel","voiceId":"abc"}}}}"""
        val chat = codableJson.decodeFromString(SettingsChat.serializer(), json)
        val voice = assertNotNull(chat.textToSpeechLanguageVoices["en"])
        assertEquals(SettingsVoiceType.ttsMonster, voice.type)
        assertEquals("abc", voice.ttsMonster.voiceId)
        assertEquals("", voice.elevenLabs.voiceId)
    }

    @Test
    fun textToSpeechElevenLabsVoiceRoundTrips() {
        val chat = SettingsChat()
        chat.elevenLabs.apiKey = "key"
        val voice = SettingsVoice()
        voice.type = SettingsVoiceType.elevenLabs
        voice.elevenLabs.name = "Rachel"
        voice.elevenLabs.voiceId = "21m00Tcm4TlvDq8ikWAM"
        chat.textToSpeechLanguageVoices["en"] = voice
        val decoded = codableJson.decodeFromString(
            SettingsChat.serializer(),
            codableJson.encodeToString(SettingsChat.serializer(), chat),
        )
        assertEquals("key", decoded.elevenLabs.apiKey)
        val decodedVoice = assertNotNull(decoded.textToSpeechLanguageVoices["en"])
        assertEquals(SettingsVoiceType.elevenLabs, decodedVoice.type)
        assertEquals("Rachel", decodedVoice.elevenLabs.name)
        assertEquals("21m00Tcm4TlvDq8ikWAM", decodedVoice.elevenLabs.voiceId)
    }

    @Test
    fun textToSpeechSayEmotesDefaultsToOff() {
        val chat = codableJson.decodeFromString(SettingsChat.serializer(), "{}")
        assertFalse(chat.textToSpeechSayEmotes)
    }

    @Test
    fun textToSpeechSayEmotesIsPersisted() {
        val chat = SettingsChat()
        chat.textToSpeechSayEmotes = true
        val decoded = codableJson.decodeFromString(
            SettingsChat.serializer(),
            codableJson.encodeToString(SettingsChat.serializer(), chat),
        )
        assertTrue(decoded.textToSpeechSayEmotes)
    }
}
