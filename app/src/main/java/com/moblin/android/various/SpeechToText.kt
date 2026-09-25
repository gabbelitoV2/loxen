package com.moblin.android.various

import android.os.SystemClock
import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.speech.SFSpeechAudioBufferRecognitionRequest
import com.moblin.android.platform.speech.SFSpeechRecognitionTask
import com.moblin.android.platform.speech.SFSpeechRecognizer
import com.moblin.android.platform.speech.SFSpeechRecognizerAuthorizationStatus
import com.moblin.android.platform.speech.SFSpeechRecognizerDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "SpeechToText"

interface SpeechToTextDelegate {
    fun speechToTextPartialResult(position: Int, text: String)
    fun speechToTextClear()
}

class SpeechToText : SFSpeechRecognizerDelegate {
    private val speechRecognizer = SFSpeechRecognizer()
    private var recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
    private var recognitionTask: SFSpeechRecognitionTask? = null
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
        speechRecognizer.delegate = this
        SFSpeechRecognizer.requestAuthorization { authStatus ->
            mainScope.launch {
                when (authStatus) {
                    SFSpeechRecognizerAuthorizationStatus.authorized ->
                        startAuthorized()
                    SFSpeechRecognizerAuthorizationStatus.denied ->
                        onError("Speech recognition not allowed")
                    SFSpeechRecognizerAuthorizationStatus.restricted ->
                        onError("Speech recognition restricted on this device")
                    SFSpeechRecognizerAuthorizationStatus.notDetermined ->
                        onError("Speech recognition not yet authorized")
                }
            }
        }
    }

    fun stop() {
        isStarted = false
        stopInternal()
    }

    private fun stopInternal() {
        recognitionTask?.cancel()
        recognitionTask = null
        hasResult = false
        running = false
    }

    fun append(sampleBuffer: MediaSample) {
        if (!running) {
            return
        }
        recognitionRequest.appendAudioSampleBuffer(sampleBuffer)
    }

    fun tick(now: Long) {
        if (!running) {
            return
        }
        if (hasResult && now - latestResultTime > 2_000_000_000L) {
            running = false
            recognitionRequest.endAudio()
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
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        recognitionRequest.shouldReportPartialResults = true
        recognitionRequest.addsPunctuation = false
        recognitionTask = speechRecognizer.recognitionTask(with = recognitionRequest) { result, error ->
            if (error != null) {
                Log.d(TAG, "speech-to-text: Error $error")
                startRecognition()
                return@recognitionTask
            }
            if (result == null) {
                startRecognition()
                return@recognitionTask
            }
            val text = result.bestTranscription.formattedString
            if (result.isFinal) {
                startRecognition()
            } else {
                delegate?.speechToTextPartialResult(
                    position = frozenTextPosition,
                    text = frozenText + text,
                )
                latestResultTime = SystemClock.elapsedRealtimeNanos()
                hasResult = true
            }
            previousBestTranscription = text
        }
    }

    override fun speechRecognizer(speechRecognizer: SFSpeechRecognizer, availabilityDidChange: Boolean) {
        Log.i(TAG, "speech-to-text: Available $availabilityDidChange")
    }
}
