package com.moblin.android.various.network

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class HttpProxyServerSuite {
    @Test
    fun connectParserNoData() {
        val parser = HttpConnectRequestParser()
        val (done, result) = parser.parse()
        assertFalse(done)
        assertNull(result)
    }

    @Test
    fun connectParserIncompleteHeader() {
        val parser = HttpConnectRequestParser()
        parser.append(data = "CONNECT example.com:443 HTTP/1.1\r\n".encodeToByteArray())
        val (done, result) = parser.parse()
        assertFalse(done)
        assertNull(result)
    }

    @Test
    fun connectParserValid() {
        val parser = HttpConnectRequestParser()
        parser.append(data = "CONNECT example.com:443 HTTP/1.1\r\nHost: example.com:443\r\n\r\n".encodeToByteArray())
        val (done, result) = parser.parse()
        assertTrue(done)
        assertEquals("example.com", result?.destinationHost)
        assertEquals(443, result?.destinationPort)
        assertEquals("HTTP/1.1", result?.version)
    }

    @Test
    fun connectParserWrongMethod() {
        val parser = HttpConnectRequestParser()
        parser.append(data = "GET / HTTP/1.1\r\n\r\n".encodeToByteArray())
        val (done, result) = parser.parse()
        assertTrue(done)
        assertNull(result)
    }

    @Test
    fun connectParserInvalidPort() {
        val parser = HttpConnectRequestParser()
        parser.append(data = "CONNECT example.com:notaport HTTP/1.1\r\n\r\n".encodeToByteArray())
        val (done, result) = parser.parse()
        assertTrue(done)
        assertNull(result)
    }

    @Test
    fun connectParserInvalidHttpVersion() {
        val parser = HttpConnectRequestParser()
        parser.append(data = "CONNECT example.com:443 FTTP/1.1\r\n\r\n".encodeToByteArray())
        val (done, result) = parser.parse()
        assertTrue(done)
        assertNull(result)
    }

    @Test
    fun connectParserBodyOffset() {
        val parser = HttpConnectRequestParser()
        val header = "CONNECT example.com:80 HTTP/1.0\r\n\r\n"
        val body = "some body data".encodeToByteArray()
        parser.append(data = header.encodeToByteArray() + body)
        val (done, result) = parser.parse()
        assertTrue(done)
        assertNotNull(result)
        assertEquals(header.encodeToByteArray().size, result?.bodyOffset)
        assertEquals("example.com", result?.destinationHost)
        assertEquals(80, result?.destinationPort)
        assertEquals("HTTP/1.0", result?.version)
    }

    @Test
    fun connectParserSplitAcrossChunks() {
        val parser = HttpConnectRequestParser()
        parser.append(data = "CONNECT example.com:8080 HTTP/1.1\r\n".encodeToByteArray())
        var parsed = parser.parse()
        assertFalse(parsed.first)
        assertNull(parsed.second)
        parser.append(data = "\r\n".encodeToByteArray())
        parsed = parser.parse()
        assertTrue(parsed.first)
        assertEquals("example.com", parsed.second?.destinationHost)
        assertEquals(8080, parsed.second?.destinationPort)
    }
}
