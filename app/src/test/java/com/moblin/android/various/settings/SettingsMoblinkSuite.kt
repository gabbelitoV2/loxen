package com.moblin.android.various.settings

import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsMoblinkSuite {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun relayUrlWithoutSchemeIsReplacedWithDefault() {
        val payload = """{"enabled": true, "name": "Relay", "url": "//1.2.3.4:5678", "manual": true}"""
        val relay = json.decodeFromString<SettingsMoblinkRelay>(payload)
        assertTrue(relay.enabled)
        assertEquals("Relay", relay.name)
        assertEquals("", relay.url)
        assertTrue(relay.manual)
    }

    @Test
    fun validRelayUrlIsKept() {
        val payload = """{"url": "ws://1.2.3.4:5678"}"""
        val relay = json.decodeFromString<SettingsMoblinkRelay>(payload)
        assertEquals("ws://1.2.3.4:5678", relay.url)
    }
}
