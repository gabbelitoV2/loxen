package com.moblin.android.platform.swiftui

import androidx.compose.runtime.snapshots.Snapshot
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class PublishedSuiteSettings(name: String = "") {
    var name: String by Published(name)
    var items: MutableList<Int> by PublishedList(listOf(1, 2))
    var values: MutableMap<String, Int> by PublishedMap()
}

@RunWith(RobolectricTestRunner::class)
class PublishedSuite {
    @Test
    fun readsAreObserved() {
        val settings = PublishedSuiteSettings("a")
        val reads = mutableListOf<Any>()
        Snapshot.observe(readObserver = { reads.add(it) }) {
            settings.name
            settings.items.size
            settings.values.size
        }
        assertTrue(reads.size >= 3)
    }

    @Test
    fun valueSemanticsLikeSwift() {
        val settings = PublishedSuiteSettings()
        val source = mutableListOf(5, 6)
        settings.items = source
        source.add(7)
        assertEquals(listOf(5, 6), settings.items.toList())
        settings.items.add(8)
        assertEquals(listOf(5, 6, 8), settings.items.toList())
        settings.values["x"] = 1
        assertEquals(1, settings.values["x"])
        settings.name = "b"
        assertEquals("b", settings.name)
    }
}
