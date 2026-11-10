package com.moblin.android.various.model

import org.junit.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModelWebBrowserSuite {
    class SearchCase(text: String, query: String) {
        val testDescription: String = text
        val text: String = text
        val url: String = "https://www.google.com/search?q=$query"
    }

    @Test
    fun search() {
        val searchCases = listOf(
            SearchCase("moblin", "moblin"),
            SearchCase("moblin app", "moblin%20app"),
            SearchCase("c++", "c%2B%2B"),
            SearchCase("a & b", "a%20%26%20b"),
            SearchCase("what is #1", "what%20is%20%231"),
            SearchCase("x=1", "x%3D1"),
        )
        for (searchCase in searchCases) {
            assertEquals(searchCase.url, makeWebBrowserUrl(searchCase.text)?.toString())
        }
    }

    @Test
    fun urlWithScheme() {
        assertEquals(
            "http://example.com/a?b=c",
            makeWebBrowserUrl("http://example.com/a?b=c")?.toString(),
        )
    }

    @Test
    fun urlWithoutScheme() {
        assertEquals(
            "https://example.com/a",
            makeWebBrowserUrl("example.com/a")?.toString(),
        )
    }
}
