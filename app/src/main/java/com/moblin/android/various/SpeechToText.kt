package com.moblin.android.various

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.moblin.android.media.MediaSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface SpeechToTextDelegate {
    fun speechToTextPartialResult(position: Int, text: String)
    fun speechToTextClear()
}

class SpeechToText(private val context: Context) : RecognitionListener {
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionRequest: Intent? = null
    private var recognitionTask = false
    var delegate: SpeechToTextDelegate? = null
    private var latestResultTime: Long = SystemClock.elapsedRealtimeNanos()
    private var hasResult = false
    private var running = false
    private var isStarted = false
    private var frozenText = ""
    private var frozenTextPosition = 0
    private var previousBestTranscription = ""
    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun start(onError: (String) -> Unit) {
        isStarted = true
        clearFrozenText()
        previousBestTranscription = ""
        mainScope.launch {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
                    it.setRecognitionListener(this@SpeechToText)
                }
            }
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onError("Speech recognition not available on this device")
                return@launch
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                onError("Speech recognition not allowed")
                return@launch
            }
            startAuthorized()
        }
    }

    fun stop() {
        isStarted = false
        stopInternal()
    }

    private fun stopInternal() {
        speechRecognizer?.cancel()
        recognitionTask = false
        hasResult = false
        running = false
    }

    fun append(sampleBuffer: MediaSample) {
        if (!running) {
            return
        }
        TODO("Android SpeechRecognizer cannot accept raw audio buffers: no equivalent of SFSpeechAudioBufferRecognitionRequest.appendAudioSampleBuffer (RecognizerIntent.EXTRA_AUDIO_SOURCE only exists on API 33+ and requires 16 kHz mono PCM through a ParcelFileDescriptor).")
    }

    fun tick(now: Long) {
        if (!running) {
            return
        }
        if (hasResult && now - latestResultTime > 2_000_000_000L) {
            running = false
            speechRecognizer?.stopListening()
        }
        if (now - latestResultTime > 5_000_000_000L) {
            if (frozenText.isNotEmpty()) {
                clearFrozenText()
                delegate?.speechToTextClear()
            }
        }
    }

    private fun clearFrozenText() {
        frozenText = ""
        frozenTextPosition = 0
    }

    private fun startAuthorized() {
        stopInternal()
        startRecognition()
    }

    private fun startRecognition() {
        if (!isStarted) {
            return
        }
        val newFrozenText = frozenText + previousBestTranscription
        frozenText = newFrozenText.takeLast(150).trim()
        frozenTextPosition += newFrozenText.length - frozenText.length
        if (frozenText.isNotEmpty()) {
            frozenText += " "
        }
        hasResult = false
        running = true
        val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        recognitionRequest = request
        recognitionTask = true
        speechRecognizer?.startListening(request)
    }

    private fun bestTranscription(bundle: Bundle?): String? {
        val results = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        return results?.firstOrNull()
    }

    private fun handleRecognitionText(text: String, isFinal: Boolean) {
        if (isFinal) {
            startRecognition()
        } else {
            delegate?.speechToTextPartialResult(frozenTextPosition, frozenText + text)
            latestResultTime = SystemClock.elapsedRealtimeNanos()
            hasResult = true
        }
        previousBestTranscription = text
    }

    override fun onReadyForSpeech(params: Bundle?) {}

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        Log.d("SpeechToText", "speech-to-text: Error $error")
        startRecognition()
    }

    override fun onResults(results: Bundle?) {
        val text = bestTranscription(results)
        if (text == null) {
            startRecognition()
            return
        }
        handleRecognitionText(text, true)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val text = bestTranscription(partialResults) ?: return
        handleRecognitionText(text, false)
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun speechRecognizer(speechRecognizer: SpeechRecognizer, availabilityDidChange: Boolean) {
        Log.i("SpeechToText", "speech-to-text: Available $availabilityDidChange")
    }
}
