package com.moblin.android.various.model

import com.moblin.android.media.MediaSample
import com.moblin.android.platform.speech.FakeSpeechRecognitionBackend
import com.moblin.android.platform.speech.SpeechRecognitionSystem
import com.moblin.android.platform.speech.pcm16Sample
import com.moblin.android.platform.speech.readAvailable
import com.moblin.android.platform.speech.runMain
import com.moblin.android.platform.speech.shorts
import com.moblin.android.platform.speech.waitUntil
import com.moblin.android.platform.translation.FakeTranslationBackend
import com.moblin.android.platform.translation.TranslationSystem
import com.moblin.android.platform.translation.downloadable
import com.moblin.android.platform.translation.onDevice
import com.moblin.android.various.Media
import com.moblin.android.various.MediaDelegate
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetTextSubtitles
import com.moblin.android.various.subtitles.Translator
import java.lang.reflect.Proxy
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModelSpeechToTextSuite {
    private lateinit var speech: FakeSpeechRecognitionBackend
    private lateinit var translation: FakeTranslationBackend
    private lateinit var model: Model
    private lateinit var originalLocale: Locale
    private val scene = SettingsScene(name = "Subtitles")

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("en-US"))
        speech = FakeSpeechRecognitionBackend()
        SpeechRecognitionSystem.backend = speech
        translation = FakeTranslationBackend(capabilities = listOf(onDevice("en", "sv"), downloadable("en", "de")))
        TranslationSystem.backend = translation
        model = Model()
        model.media = Media(delegate = mediaDelegate())
        runMain()
    }

    @After
    fun tearDown() {
        model.stopSpeechToText()
        runMain()
        SpeechRecognitionSystem.reset()
        TranslationSystem.reset()
        Locale.setDefault(originalLocale)
    }

    private fun mediaDelegate(): MediaDelegate {
        return Proxy.newProxyInstance(MediaDelegate::class.java.classLoader, arrayOf(MediaDelegate::class.java)) { _, method, args ->
            if (method.name == "mediaOnAudioBuffer") {
                model.mediaOnAudioBuffer(args[0] as MediaSample)
            }
            when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Double.TYPE -> 0.0
                java.lang.Float.TYPE -> 0f
                else -> null
            }
        } as MediaDelegate
    }

    private fun addSubtitlesWidget(vararg languageIdentifiers: String?) {
        val database = model.database
        if (database.scenes.none { it === scene }) {
            database.scenes.add(scene)
        }
        model.sceneSelector.selectedSceneId = scene.id
        val widget = SettingsWidget(name = "Subtitles")
        widget.text.formatString = "{subtitles}"
        widget.text.needsSubtitles = true
        widget.text.subtitles = languageIdentifiers.map { SettingsWidgetTextSubtitles(identifier = it) }
        database.widgets.add(widget)
        scene.widgets.add(SettingsSceneWidget(widgetId = widget.id))
    }

    @Test
    fun streamAudioReachesTheRecognizerThroughMediaOnAudioBuffer() {
        model.startSpeechToText()
        runMain()
        val engine = speech.engine
        val source = assertNotNull(engine.audioSource)
        model.media.streamAudio(pcm16Sample(48000, 2, 960) { _, channel -> if (channel == 0) 200 else 400 })
        runMain()
        var received = ByteArray(0)
        waitUntil {
            received += readAvailable(source)
            received.size == 640
        }
        assertTrue(shorts(received).all { it.toInt() == 300 })
        engine.partial("hello")
        assertEquals(0, model.speechToTextLatestPosition)
        assertEquals("hello", model.speechToTextLatestText)
        model.speechToTextProcess()
        assertNull(model.speechToTextLatestText)
    }

    @Test
    fun subtitlesAreTranslatedIntoTheLanguagesOfTheTextWidgets() {
        addSubtitlesWidget(null, "sv", "sv", "de")
        model.startSpeechToText()
        runMain()
        assertEquals(2, Translator.translators.size)
        speech.engine.partial("hello " + "x".repeat(200))
        model.speechToTextProcess()
        runMain()
        assertEquals(listOf("hello " + "x".repeat(200)).map { it.takeLast(150) }, translation.engine.requests)
        assertEquals("sv", translation.engine.target)
        assertNotNull(model.speechToTextTextAligners["sv"])
        assertNotNull(model.speechToTextTextAligners["de"])
        model.stopSpeechToText()
        assertTrue(Translator.translators.isEmpty())
        assertTrue(model.speechToTextTextAligners.isEmpty())
    }

    @Test
    fun speechToTextIsOnlyStartedWhenAWidgetNeedsIt() {
        assertTrue(!model.isSpeechToTextNeeded())
        model.updateSpeechToText()
        assertNull(model.speechToText)
        addSubtitlesWidget()
        model.updateSpeechToText()
        runMain()
        assertNotNull(model.speechToText)
        assertEquals(1, speech.engines.size)
    }
}
