package com.moblin.android.platform.speech

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSpeechRecognizer

@RunWith(RobolectricTestRunner::class)
class AndroidSpeechRecognitionBackendSuite {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        ShadowSpeechRecognizer.reset()
    }

    @Test
    fun theOnDeviceRecognizerGetsTheIntentAndReportsToTheListener() {
        ShadowSpeechRecognizer.setIsOnDeviceRecognitionAvailable(true)
        val backend = AndroidSpeechRecognitionBackend(context)
        assertTrue(backend.isOnDeviceRecognitionAvailable())
        val engine = assertNotNull(backend.createEngine(onDevice = true))
        val partials = mutableListOf<String>()
        val errors = mutableListOf<Int>()
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                errors.add(error)
            }

            override fun onResults(results: Bundle?) {}

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { partials.addAll(it) }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        engine.startListening(intent, listener)
        runMain()
        val shadow = Shadow.extract<ShadowSpeechRecognizer>(ShadowSpeechRecognizer.getLatestSpeechRecognizer())
        assertSame(intent, shadow.lastRecognizerIntent)
        val bundle = Bundle()
        bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("hej"))
        shadow.triggerOnPartialResults(bundle)
        shadow.triggerOnError(SpeechRecognizer.ERROR_NO_MATCH)
        runMain()
        assertEquals(listOf("hej"), partials)
        assertEquals(listOf(SpeechRecognizer.ERROR_NO_MATCH), errors)
        engine.destroy()
        assertTrue(shadow.isDestroyed)
    }

    @Test
    fun recognitionIsAvailableOnlyWithARecognitionService() {
        val backend = AndroidSpeechRecognitionBackend(context)
        assertFalse(backend.isRecognitionAvailable())
        val component = ComponentName("com.google.android.tts", "com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService")
        val packageManager = shadowOf(context.packageManager)
        packageManager.addServiceIfNotPresent(component)
        packageManager.addIntentFilterForService(component, IntentFilter(RecognitionService.SERVICE_INTERFACE))
        assertTrue(backend.isRecognitionAvailable())
        assertEquals(context.packageName, backend.packageName)
    }
}
