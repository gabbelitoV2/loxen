package com.moblin.android.various.network

import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkUtilsSuite {
    @Test
    fun makeUrls() {
        assertEquals("channels?foo=bar", makeUrl("channels", listOf("foo" to "bar")))
        assertEquals("/a/b/c?", makeUrl("/a/b/c", emptyList()))
        assertEquals("kalle?1=2&3=4", makeUrl("kalle", listOf("1" to "2", "3" to "4")))
        assertEquals("foo/bar?%5E%26*%25=%23\$%25%5E", makeUrl("foo/bar", listOf("^&*%" to "#\$%^")))
    }

    @Test
    fun makeMdnsHostnames() {
        assertEquals("iphone.local", makeMdnsHostname(deviceName = "iPhone"))
        assertEquals("erik-17-pro.local", makeMdnsHostname(deviceName = "Erik 17 Pro"))
        assertEquals("asb-6.local", makeMdnsHostname(deviceName = "a's\$b 6"))
        assertEquals("a-b-c.local", makeMdnsHostname(deviceName = "a    b----c--"))
    }

    @Test
    fun isLoopback() {
        assertTrue(URI("ws://localhost:2345").isLoopback())
        assertTrue(URI("ws://127.0.0.1:2345/foo").isLoopback())
        assertTrue(URI("wss://[::1]/foo").isLoopback())
        assertFalse(URI("ws://127.0.0.2:2345").isLoopback())
        assertFalse(URI("ws://192.168.1.5:2345").isLoopback())
        assertFalse(URI("wss://example.com/foo").isLoopback())
        assertFalse(URI("wss://[fe80::1]/foo").isLoopback())
        assertFalse(URI("ws:///foo").isLoopback())
    }
}
