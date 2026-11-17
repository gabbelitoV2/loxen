package com.moblin.android.view.settings

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsSearchSuite {
    data class MatchCase(
        val testDescription: String,
        val text: String,
    )

    @Test
    fun matches() {
        val cases = listOf(
            MatchCase("title", "Video"),
            MatchCase("title prefix", "vid"),
            MatchCase("keyword", "fps"),
            MatchCase("breadcrumb", "stre"),
            MatchCase("case insensitive", "BITRATE"),
            MatchCase("diacritic insensitive", "résolution"),
            MatchCase("all words match", "video  hevc"),
            MatchCase("words across title and path", "streams bitrate"),
            MatchCase("any word order", "hevc video"),
        )
        for (testCase in cases) {
            assertTrue(
                settingsSearchMatches(
                    text = testCase.text,
                    title = "Video",
                    path = listOf("Streams"),
                    keywords = listOf("Bitrate", "Resolution", "FPS", "hevc"),
                ),
                testCase.testDescription,
            )
        }
    }

    @Test
    fun doesNotMatch() {
        val cases = listOf(
            MatchCase("empty", ""),
            MatchCase("whitespace", "   "),
            MatchCase("unknown word", "vibrate"),
            MatchCase("one of two words missing", "video vibrate"),
            MatchCase("middle of word", "ideo"),
            MatchCase("end of word", "rate"),
        )
        for (testCase in cases) {
            assertFalse(
                settingsSearchMatches(
                    text = testCase.text,
                    title = "Video",
                    path = listOf("Streams"),
                    keywords = listOf("Bitrate", "Resolution", "FPS", "hevc"),
                ),
                testCase.testDescription,
            )
        }
    }
}
