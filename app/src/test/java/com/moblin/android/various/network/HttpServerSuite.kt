package com.moblin.android.various.network

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun parse(request: String): Pair<Boolean, HttpRequestParseResult?> {
    val parser = HttpRequestParser()
    parser.append(request.toByteArray())
    return parser.parse()
}

@RunWith(RobolectricTestRunner::class)
class HttpServerSuite {
    @Test
    fun pathWithoutQuery() {
        val (done, result) = parse("GET /snapshot HTTP/1.1\r\n\r\n")
        assertTrue(done)
        val request = assertNotNull(result)
        assertEquals("GET", request.method)
        assertEquals("/snapshot", request.path)
        assertTrue(request.query.isEmpty())
        assertEquals("HTTP/1.1", request.version)
        assertTrue(request.data.isEmpty())
    }

    @Test
    fun singleQueryParameter() {
        val (done, result) = parse("GET /snapshot?age=5.5 HTTP/1.1\r\n\r\n")
        assertTrue(done)
        val request = assertNotNull(result)
        assertEquals("/snapshot", request.path)
        assertEquals(mapOf("age" to "5.5"), request.query)
    }

    @Test
    fun multipleQueryParameters() {
        val (_, result) = parse("GET /a/b?foo=1&bar=two&baz=3 HTTP/1.0\r\n\r\n")
        val request = assertNotNull(result)
        assertEquals("/a/b", request.path)
        assertEquals(mapOf("foo" to "1", "bar" to "two", "baz" to "3"), request.query)
    }

    @Test
    fun percentEncodedQueryParameters() {
        val (_, result) = parse("GET /x?na%20me=hello%20world&eq=a%3Db%26c HTTP/1.1\r\n\r\n")
        val request = assertNotNull(result)
        assertEquals(mapOf("na me" to "hello world", "eq" to "a=b&c"), request.query)
    }

    @Test
    fun queryParameterWithoutValue() {
        val (_, result) = parse("GET /x?flag&empty=&other=1 HTTP/1.1\r\n\r\n")
        val request = assertNotNull(result)
        assertEquals(mapOf("flag" to "", "empty" to "", "other" to "1"), request.query)
    }

    @Test
    fun emptyQuery() {
        val (_, result) = parse("GET /x? HTTP/1.1\r\n\r\n")
        val request = assertNotNull(result)
        assertEquals("/x", request.path)
        assertTrue(request.query.isEmpty())
    }

    @Test
    fun questionMarkInQueryValue() {
        val (_, result) = parse("GET /x?a=b?c HTTP/1.1\r\n\r\n")
        val request = assertNotNull(result)
        assertEquals("/x", request.path)
        assertEquals(mapOf("a" to "b?c"), request.query)
    }

    @Test
    fun lastDuplicateQueryParameterWins() {
        val (_, result) = parse("GET /x?a=1&a=2 HTTP/1.1\r\n\r\n")
        val request = assertNotNull(result)
        assertEquals(mapOf("a" to "2"), request.query)
    }

    @Test
    fun invalidPercentEncodingSkipsParameter() {
        val (_, result) = parse("GET /x?a%ZZ=1&b=2 HTTP/1.1\r\n\r\n")
        val request = assertNotNull(result)
        assertEquals(mapOf("b" to "2"), request.query)
    }

    @Test
    fun headersAndBody() {
        val (done, result) = parse(
            "POST /x?a=1 HTTP/1.1\r\nContent-Type: text/plain\r\nContent-Length: 5\r\n\r\nhello",
        )
        assertTrue(done)
        val request = assertNotNull(result)
        assertEquals("POST", request.method)
        assertEquals("/x", request.path)
        assertEquals(mapOf("a" to "1"), request.query)
        assertTrue(request.headers.any { it.name == "content-type:" && it.value == "text/plain" })
        assertContentEquals("hello".toByteArray(), request.data)
    }

    @Test
    fun incompleteBodyIsNotDone() {
        val (done, result) = parse("POST /x HTTP/1.1\r\nContent-Length: 10\r\n\r\nhello")
        assertFalse(done)
        assertNull(result)
    }

    @Test
    fun incompleteHeadersIsNotDone() {
        val (done, result) = parse("GET /x?a=1 HTTP/1.1\r\nHost: foo\r\n")
        assertFalse(done)
        assertNull(result)
    }

    @Test
    fun malformedStartLineFails() {
        val (done, result) = parse("GET /x\r\n\r\n")
        assertTrue(done)
        assertNull(result)
    }

    @Test
    fun unsupportedVersionFails() {
        val (done, result) = parse("GET /x HTTP/2\r\n\r\n")
        assertTrue(done)
        assertNull(result)
    }

    @Test
    fun parentDirectoryInPathFails() {
        val (done, result) = parse("GET /../secret HTTP/1.1\r\n\r\n")
        assertTrue(done)
        assertNull(result)
    }

    @Test
    fun parentDirectoryInQueryFails() {
        val (done, result) = parse("GET /x?file=.. HTTP/1.1\r\n\r\n")
        assertTrue(done)
        assertNull(result)
    }

    @Test
    fun negativeContentLengthFails() {
        val (done, result) = parse("GET /x HTTP/1.1\r\nContent-Length: -1\r\n\r\n")
        assertTrue(done)
        assertNull(result)
    }

    @Test
    fun routeMatchesPathWithoutQuery() {
        val route = HttpServerRoute(path = "/snapshot") { _, _ -> }
        val (_, result) = parse("GET /snapshot?age=5 HTTP/1.1\r\n\r\n")
        val request = assertNotNull(result)
        assertTrue(route.matches(path = request.path))
        assertFalse(route.matches(path = "/snapshot/x"))
    }

    @Test
    fun prefixRouteMatchesPath() {
        val route = HttpServerRoute(path = "/recordings/", prefixMatch = true) { _, _ -> }
        val (_, result) = parse("GET /recordings/a.mp4?x=1 HTTP/1.1\r\n\r\n")
        val request = assertNotNull(result)
        assertTrue(route.matches(path = request.path))
        assertFalse(route.matches(path = "/recordings"))
    }
}
