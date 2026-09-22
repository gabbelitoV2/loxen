package com.moblin.android.streamingplatforms.twitch

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test

class CheermotesSuite {
    private fun makeCheermotes(prefixes: Map<String, List<Int>>): Cheermotes {
        val cheermotes = Cheermotes()
        cheermotes.addCheermotes(
            datas = prefixes.map { (prefix, minBits) ->
                TwitchApiGetCheermotesData(
                    prefix = prefix,
                    tiers = minBits.map { minBits ->
                        TwitchApiGetCheermotesDataTier(
                            min_bits = minBits,
                            images = TwitchApiGetCheermotesDataTiersImages(
                                dark = TwitchApiGetCheermotesDataTiersImagesTheme(
                                    static_ = TwitchApiGetCheermotesDataTiersImagesThemeKind(
                                        two = "https://cheer.example.com/$prefix/$minBits"
                                    )
                                )
                            )
                        )
                    }
                )
            }
        )
        return cheermotes
    }

    @Test
    fun matchesPrefixAndBits() {
        val cheermotes = makeCheermotes(linkedMapOf("cheer" to listOf(1)))
        val (url, bits) = assertNotNull(cheermotes.getUrlAndBits("cheer100"))
        assertEquals("https://cheer.example.com/cheer/1", url.toString())
        assertEquals(100, bits)
    }

    @Test
    fun ignoresSurroundingWhitespaceAndCase() {
        val cheermotes = makeCheermotes(linkedMapOf("cheer" to listOf(1)))
        val (_, bits) = assertNotNull(cheermotes.getUrlAndBits(" Cheer250 "))
        assertEquals(250, bits)
    }

    @Test
    fun picksHighestMatchingTier() {
        val cheermotes = makeCheermotes(linkedMapOf("cheer" to listOf(1, 100, 1000, 5000)))
        assertEquals(
            "1",
            assertNotNull(cheermotes.getUrlAndBits("cheer1")).first.toString().substringAfterLast("/")
        )
        assertEquals(
            "100",
            assertNotNull(cheermotes.getUrlAndBits("cheer500")).first.toString().substringAfterLast("/")
        )
        assertEquals(
            "1000",
            assertNotNull(cheermotes.getUrlAndBits("cheer1000")).first.toString().substringAfterLast("/")
        )
        assertEquals(
            "5000",
            assertNotNull(cheermotes.getUrlAndBits("cheer99999")).first.toString().substringAfterLast("/")
        )
    }

    @Test
    fun belowLowestTierDoesNotMatch() {
        val cheermotes = makeCheermotes(linkedMapOf("cheer" to listOf(100)))
        assertNull(cheermotes.getUrlAndBits("cheer1"))
    }

    @Test
    fun prefixEndingInDigitPrefersTheLongestPrefix() {
        val cheermotes = makeCheermotes(linkedMapOf("cheer" to listOf(1), "cheer1" to listOf(1)))
        val (url, bits) = assertNotNull(cheermotes.getUrlAndBits("cheer1100"))
        assertEquals("https://cheer.example.com/cheer1/1", url.toString())
        assertEquals(100, bits)
    }

    @Test
    fun withoutBitsDoesNotMatch() {
        val cheermotes = makeCheermotes(linkedMapOf("cheer" to listOf(1)))
        assertNull(cheermotes.getUrlAndBits("cheer"))
    }

    @Test
    fun unknownPrefixDoesNotMatch() {
        val cheermotes = makeCheermotes(linkedMapOf("cheer" to listOf(1)))
        assertNull(cheermotes.getUrlAndBits("notacheermote100"))
    }

    @Test
    fun partialWordDoesNotMatch() {
        val cheermotes = makeCheermotes(linkedMapOf("cheer" to listOf(1)))
        assertNull(cheermotes.getUrlAndBits("xcheer100"))
        assertNull(cheermotes.getUrlAndBits("cheer100x"))
    }

    @Test
    fun emptyWordDoesNotMatch() {
        val cheermotes = makeCheermotes(linkedMapOf("cheer" to listOf(1)))
        assertNull(cheermotes.getUrlAndBits(""))
        assertNull(cheermotes.getUrlAndBits("100"))
    }

    @Test
    fun noCheermotesFetched() {
        assertNull(Cheermotes().getUrlAndBits("cheer100"))
    }
}
