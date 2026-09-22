package com.moblin.android.various.network

import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class HttpClientSuite {
    @Test
    fun responseParserNoData() {
        val parser = HttpResponseParser()
        val (done, data) = parser.parse()
        assertFalse(done)
        assertNull(data)
    }

    @Test
    fun responseParserEmptyBody() {
        val parser = HttpResponseParser()
        parser.append("HTTP/1.1 200 OK\r\n\r\n".encodeToByteArray())
        val (done, data) = parser.parse()
        assertTrue(done)
        assertContentEquals(ByteArray(0), data)
    }

    @Test
    fun responseParserBody() {
        val parser = HttpResponseParser()
        val body = "1234567890".encodeToByteArray()
        parser.append("HTTP/1.1 200 OK\r\nContent-Length: ${body.size}\r\n\r\n".encodeToByteArray() + body)
        val (done, data) = parser.parse()
        assertTrue(done)
        assertContentEquals(body, data)
    }

    @Test
    fun responseParserHalfHeader() {
        val parser = HttpResponseParser()
        val body = "1234567890".encodeToByteArray()
        parser.append("HTTP/1.1 200 OK\r\nContent-Le".encodeToByteArray())
        val (done, data) = parser.parse()
        assertFalse(done)
        assertNull(data)
        parser.append("ngth: ${body.size}\r\n\r\n".encodeToByteArray() + body)
        val (done2, data2) = parser.parse()
        assertTrue(done2)
        assertContentEquals(body, data2)
    }

    @Test
    fun responseParserHalfBody() {
        val parser = HttpResponseParser()
        val body = "1234567890".encodeToByteArray()
        parser.append("HTTP/1.1 200 OK\r\nContent-Length: ${body.size}\r\n\r\n".encodeToByteArray() + body.copyOfRange(0, 5))
        val (done, data) = parser.parse()
        assertFalse(done)
        assertNull(data)
        parser.append(body.copyOfRange(5, body.size))
        val (done2, data2) = parser.parse()
        assertTrue(done2)
        assertContentEquals(body, data2)
    }

    @Test
    fun responseParserStatus400() {
        val parser = HttpResponseParser()
        parser.append("HTTP/1.1 400 OK\r\n\r\n".encodeToByteArray())
        val (done, data) = parser.parse()
        assertTrue(done)
        assertNull(data)
    }

    @Test
    fun responseParserGetLineCrash() {
        val parser = HttpResponseParser()
        parser.append("HTTP/1.1 400 OK\r".encodeToByteArray())
        val (done, data) = parser.parse()
        assertFalse(done)
        assertNull(data)
        parser.append("\n\r\n".encodeToByteArray())
        val (done2, data2) = parser.parse()
        assertTrue(done2)
        assertNull(data2)
    }

    @Test
    fun responseParserRejectsNegativeContentLength() {
        val parser = HttpResponseParser()
        parser.append("HTTP/1.1 200 OK\r\nContent-Length: -1\r\n\r\n".encodeToByteArray())
        val (done, data) = parser.parse()
        assertTrue(done)
        assertNull(data)
    }

    @Test
    fun responseParserRejectsNonDecimalContentLength() {
        val parser = HttpResponseParser()
        parser.append("HTTP/1.1 200 OK\r\nContent-Length: nope\r\n\r\n".encodeToByteArray())
        val (done, data) = parser.parse()
        assertTrue(done)
        assertNull(data)
    }
}
