package com.moblin.android.various.subtitles

import android.os.Looper
import com.moblin.android.platform.translation.FakeTranslationBackend
import com.moblin.android.platform.translation.TranslationSystem
import com.moblin.android.platform.translation.downloadable
import com.moblin.android.platform.translation.onDevice
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TranslatorSuite {
    private lateinit var backend: FakeTranslationBackend
    private lateinit var originalLocale: Locale
    private val translated = mutableListOf<Pair<String, String>>()

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("sv-SE"))
        backend = FakeTranslationBackend(capabilities = listOf(onDevice("sv", "en"), downloadable("sv", "de")))
        TranslationSystem.backend = backend
    }

    @After
    fun tearDown() {
        TranslationSystem.reset()
        Locale.setDefault(originalLocale)
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun translator(targetIdentifier: String): Translator {
        val translator = Translator(targetIdentifier = targetIdentifier)
        translator.delegate = object : TranslatorDelegate {
            override fun translatorTranslated(languageIdentifier: String, text: String) {
                translated.add(languageIdentifier to text)
            }
        }
        return translator
    }

    @Test
    fun onlyTheLatestTextIsTranslatedWhileATranslationIsOngoing() {
        val gate = CompletableDeferred<Unit>()
        backend.translateHook = { text ->
            if (text == "hej") {
                gate.await()
            }
            "T($text)"
        }
        val translator = translator("en")
        translator.translate(text = "hej")
        runMain()
        assertEquals(listOf("hej"), backend.engine.requests)
        translator.translate(text = "hej på")
        translator.translate(text = "hej på dig")
        runMain()
        assertTrue(translated.isEmpty())
        gate.complete(Unit)
        runMain()
        assertEquals(listOf("en" to "T(hej)", "en" to "T(hej på dig)"), translated)
        assertEquals(listOf("hej", "hej på dig"), backend.engine.requests)
        translator.translate(text = "hej då")
        runMain()
        assertEquals("en" to "T(hej då)", translated.last())
        assertEquals(1, backend.engines.size)
    }

    @Test
    fun translationErrorsAreShownAsTheirFailureReason() {
        val notDownloaded = translator("de")
        notDownloaded.translate(text = "hej")
        runMain()
        assertEquals(listOf("de" to "Languages must be downloaded on-device."), translated)
        translated.clear()
        val unsupported = translator("ja")
        unsupported.translate(text = "hej")
        runMain()
        assertEquals(listOf("ja" to "Translation into Japanese is not supported."), translated)
        translated.clear()
        backend.sdkInt = 30
        val oldAndroid = translator("en")
        oldAndroid.translate(text = "hej")
        runMain()
        assertEquals(listOf("en" to "This language pairing is not supported."), translated)
    }

    @Test
    fun otherErrorsAreOnlyLogged() {
        backend.translateHook = { text ->
            if (text == "boom") {
                throw IllegalStateException("boom")
            }
            "T($text)"
        }
        val translator = translator("en")
        translator.translate(text = "boom")
        runMain()
        assertTrue(translated.isEmpty())
        translator.translate(text = "hej")
        runMain()
        assertEquals(listOf("en" to "T(hej)"), translated)
    }
}
