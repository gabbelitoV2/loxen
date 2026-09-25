package com.moblin.android.platform.translation

import java.lang.ref.WeakReference
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TranslationSessionSuite {
    private lateinit var backend: FakeTranslationBackend
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("en-US"))
        backend = FakeTranslationBackend()
        TranslationSystem.backend = backend
    }

    @After
    fun tearDown() {
        TranslationSystem.reset()
        Locale.setDefault(originalLocale)
    }

    private fun session(target: String, source: String? = null): TranslationSession {
        return TranslationSession(
            installedSource = source?.let { localeLanguage(identifier = it) } ?: Locale.getDefault(),
            target = localeLanguage(identifier = target),
        )
    }

    private fun failureReason(session: TranslationSession, text: String = "hello"): String? {
        val error = assertFailsWith<TranslationError> {
            runBlocking { session.translate(text) }
        }
        return error.failureReason
    }

    @Test
    fun installedPairIsTranslatedOnDeviceWithOneTranslator() = runBlocking {
        backend.capabilities = listOf(onDevice("en", "sv"), downloadable("en", "de"))
        val session = session(target = "sv")
        val response = session.translate("hello")
        assertEquals("[sv] hello", response.targetText)
        assertEquals("hello", response.sourceText)
        assertEquals("sv", response.targetLanguage.minimalIdentifier)
        assertEquals("en", backend.engine.source)
        assertEquals("sv", backend.engine.target)
        assertEquals("[sv] again", session.translate("again").targetText)
        assertEquals(1, backend.engines.size)
        assertEquals(listOf("hello", "again"), backend.engine.requests)
    }

    @Test
    fun languagesThatAreNotDownloadedFailLikeTranslationErrorNotInstalled() {
        backend.capabilities = listOf(onDevice("en", "sv"), downloadable("en", "de"), downloading("en", "fr"))
        assertEquals("Languages must be downloaded on-device.", failureReason(session(target = "de")))
        assertEquals("Languages must be downloaded on-device.", failureReason(session(target = "fr")))
        val error = assertFailsWith<TranslationError> { runBlocking { session(target = "de").translate("x") } }
        assertEquals(TranslationError.notInstalled, error)
        assertEquals("Unable to Translate", error.localizedDescription)
        assertTrue(backend.engines.isEmpty())
    }

    @Test
    fun unsupportedPairsSourcesAndTargetsUseApplesFailureReasons() {
        backend.capabilities = listOf(onDevice("en", "sv"), onDevice("de", "en"), notAvailable("en", "fi"))
        assertEquals("This language pairing is not supported.", failureReason(session(target = "fi")))
        assertEquals("This language pairing is not supported.", failureReason(session(target = "sv", source = "de")))
        assertEquals("Translation from Japanese is not supported.", failureReason(session(target = "sv", source = "ja")))
        assertEquals("Translation into Korean is not supported.", failureReason(session(target = "ko")))
        assertEquals(
            TranslationError.unsupportedSourceLanguage,
            assertFailsWith<TranslationError> { runBlocking { session(target = "sv", source = "ja").translate("x") } },
        )
    }

    @Test
    fun withoutATranslationServiceNoPairIsSupported() {
        backend.capabilities = null
        assertEquals("This language pairing is not supported.", failureReason(session(target = "sv")))
        backend.capabilities = emptyList()
        assertEquals("This language pairing is not supported.", failureReason(session(target = "sv")))
        backend.capabilities = listOf(onDevice("en", "sv"))
        backend.sdkInt = 30
        assertEquals("This language pairing is not supported.", failureReason(session(target = "sv")))
    }

    @Test
    fun deviceLocaleIsTheSourceAndRegionsAndScriptsAreMatched() = runBlocking {
        Locale.setDefault(Locale.forLanguageTag("sv-SE"))
        backend.capabilities = listOf(
            onDevice("sv", "en"),
            onDevice("sv", "zh-Hans"),
            downloadable("sv", "zh-Hant"),
            onDevice("sv", "pt-BR"),
            downloadable("sv", "pt-PT"),
        )
        assertEquals("[en] hej", session(target = "en-GB").translate("hej").targetText)
        assertEquals("sv", backend.engine.source)
        assertEquals("[zh-Hans] hej", session(target = "zh-Hans").translate("hej").targetText)
        assertEquals("[zh-Hans] hej", session(target = "zh").translate("hej").targetText)
        assertEquals("[pt-BR] hej", session(target = "pt-BR").translate("hej").targetText)
        assertEquals("Languages must be downloaded on-device.", failureReason(session(target = "zh-Hant")))
        assertEquals("Languages must be downloaded on-device.", failureReason(session(target = "pt-PT")))
    }

    @Test
    fun translatorFailuresAreInternalErrorsAndTheTranslatorIsRecreated() = runBlocking {
        backend.capabilities = listOf(onDevice("en", "sv"))
        backend.createFails = true
        assertEquals("Something went wrong. Please try again later.", failureReason(session(target = "sv")))
        backend.createFails = false
        val session = session(target = "sv")
        backend.translateHook = { null }
        assertEquals("Something went wrong. Please try again later.", failureReason(session))
        assertTrue(backend.engine.destroyed)
        backend.translateHook = null
        assertEquals("[sv] hello", session.translate("hello").targetText)
        assertEquals(2, backend.engines.size)
    }

    @Test
    fun emptyTextAndCancelledSessionsFailLikeApple() {
        backend.capabilities = listOf(onDevice("en", "sv"))
        val session = session(target = "sv")
        assertEquals("Please provide text to translate and try again.", failureReason(session, text = ""))
        session.cancel()
        assertEquals("Translation was already cancelled.", failureReason(session))
    }

    @Test
    fun languageAvailabilityReportsLanguagesAndStatus() = runBlocking {
        backend.capabilities = listOf(onDevice("en", "sv"), downloadable("en", "de"), notAvailable("en", "fi"))
        val availability = LanguageAvailability()
        assertEquals(
            listOf("en", "sv", "de", "fi"),
            availability.supportedLanguages().map { it.minimalIdentifier },
        )
        val english = Locale.getDefault()
        assertEquals(LanguageAvailability.Status.installed, availability.status(from = english, to = localeLanguage(identifier = "sv")))
        assertEquals(LanguageAvailability.Status.supported, availability.status(from = english, to = localeLanguage(identifier = "de")))
        assertEquals(LanguageAvailability.Status.unsupported, availability.status(from = english, to = localeLanguage(identifier = "fi")))
        assertEquals(LanguageAvailability.Status.unsupported, availability.status(from = english, to = localeLanguage(identifier = "ja")))
        backend.capabilities = null
        assertEquals(4, availability.supportedLanguages().size)
        assertTrue(LanguageAvailability().supportedLanguages().isEmpty())
    }

    @Test
    fun aSessionThatIsNoLongerUsedDestroysItsTranslator() {
        backend.capabilities = listOf(onDevice("en", "sv"))
        val released = translateOnceAndRelease()
        collectGarbageUntil { released.get() == null }
        val engine = backend.engine
        collectGarbageUntil { engine.destroyed }
        assertTrue(engine.destroyed)
    }

    @Test
    fun onAndroid12TheSharedTranslatorOfAnUnusedSessionIsKept() {
        backend.sdkInt = 32
        backend.capabilities = listOf(onDevice("en", "sv"))
        val released = translateOnceAndRelease()
        collectGarbageUntil { released.get() == null }
        repeat(5) {
            System.gc()
            System.runFinalization()
        }
        assertFalse(backend.engine.destroyed)
    }

    private fun translateOnceAndRelease(): WeakReference<TranslationSession> {
        val session = session(target = "sv")
        assertEquals("[sv] hello", runBlocking { session.translate("hello") }.targetText)
        return WeakReference(session)
    }

    private fun collectGarbageUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition() && System.currentTimeMillis() < deadline) {
            System.gc()
            System.runFinalization()
            Thread.sleep(10)
        }
    }

    @Test
    fun localeLanguageIdentifiers() {
        assertEquals("sv", localeLanguage(identifier = "sv-SE").minimalIdentifier)
        assertEquals("sv", localeLanguage(identifier = "sv_SE").language)
        assertEquals("SE", localeLanguage(identifier = "sv_SE").country)
        assertEquals("zh-TW", localeLanguage(identifier = "zh-Hant").minimalIdentifier)
        assertEquals("zh-Hant-TW", localeLanguage(identifier = "zh-Hant").maximalIdentifier)
        assertEquals("en-GB", localeLanguage(identifier = "en-GB").minimalIdentifier)
    }
}
