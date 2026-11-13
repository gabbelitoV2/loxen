package com.moblin.android.various.model

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModelControlBarSuite {
    data class FilterCase(val name: String, val filter: String) {
        val testDescription: String = "\"$name\" with filter \"$filter\""
    }

    @Test
    fun matches() {
        val filterCases = listOf(
            FilterCase("Mute", ""),
            FilterCase("", ""),
            FilterCase("Mute", "m"),
            FilterCase("Mute", "mute"),
            FilterCase("Mute", "MU"),
            FilterCase("MUTE", "mu"),
            FilterCase("Instant replay", "rep"),
            FilterCase("Instant replay", "inst"),
            FilterCase("Scene  widgets", "wid"),
            FilterCase("Scene\twidgets", "wid"),
            FilterCase("4:3", "4:"),
            FilterCase("Ölbrunn", "öl"),
            FilterCase("Torch", " "),
            FilterCase("Scene widgets", "scene w"),
            FilterCase("Scene widgets", "s w"),
            FilterCase("Scene widgets", "Scene Widgets"),
            FilterCase("Scene widgets", "s  w"),
            FilterCase("Scene widgets", "scene "),
            FilterCase("Scene widgets", " w"),
            FilterCase("Start instant replay", "i r"),
        )
        for (filterCase in filterCases) {
            assertTrue(
                quickButtonNameMatchesFilter(name = filterCase.name, filter = filterCase.filter),
                filterCase.testDescription,
            )
        }
    }

    @Test
    fun doesNotMatch() {
        val filterCases = listOf(
            FilterCase("", "m"),
            FilterCase("Mute", "ute"),
            FilterCase("Mute", "muted"),
            FilterCase("Instant replay", "play"),
            FilterCase("Instant replay", "inst x"),
            FilterCase("Scene widgets", "w s"),
            FilterCase("Scene widgets", "s w x"),
            FilterCase("Scene widgets", "s idgets"),
            FilterCase("Start instant replay", "s r"),
        )
        for (filterCase in filterCases) {
            assertFalse(
                quickButtonNameMatchesFilter(name = filterCase.name, filter = filterCase.filter),
                filterCase.testDescription,
            )
        }
    }
}
