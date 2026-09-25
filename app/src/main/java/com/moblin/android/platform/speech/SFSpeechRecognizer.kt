package com.moblin.android.platform.speech

import android.content.Intent
import android.media.AudioFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.moblin.android.media.MediaSample
import java.io.IOException
import java.util.Locale

private const val TAG = "SFSpeechRecognizer"
private const val maximumBufferedAudioSeconds = 5
private const val stopListeningAfterAudioEndedMs = 500L
private const val audioDrainTimeoutMs = 2_000L
private const val resultAfterStopListeningTimeoutMs = 5_000L
private const val idleEngineLifetimeMs = 5_000L
private const val quickFailureMs = 1_000L
private const val maximumRetryDelayMs = 5_000L
private const val onDeviceQuickFailuresBeforeFallback = 3

enum class SFSpeechRecognizerAuthorizationStatus {
    notDetermined,
    denied,
    restricted,
    authorized,
}

interface SFSpeechRecognizerDelegate {
    fun speechRecognizer(speechRecognizer: SFSpeechRecognizer, availabilityDidChange: Boolean) {}
}

class SFTranscription internal constructor(val formattedString: String) {
    override fun toString(): String = "SFTranscription($formattedString)"
}

class SFSpeechRecognitionResult internal constructor(
    val bestTranscription: SFTranscription,
    val transcriptions: List<SFTranscription>,
    val isFinal: Boolean,
)

class SFSpeechRecognitionError internal constructor(
    val domain: String,
    val code: Int,
    val localizedDescription: String,
) : Exception(localizedDescription) {
    override fun toString(): String = "Error Domain=$domain Code=$code \"$localizedDescription\""

    companion object {
        internal const val androidDomain = "android.speech.SpeechRecognizer"

        internal fun canceled() = SFSpeechRecognitionError("kLSRErrorDomain", 301, "Recognition request was canceled")

        internal fun noSpeechDetected() = SFSpeechRecognitionError("kAFAssistantErrorDomain", 1110, "No speech detected")

        internal fun android(code: Int) = SFSpeechRecognitionError(androidDomain, code, androidErrorDescription(code))

        private fun androidErrorDescription(code: Int): String {
            return when (code) {
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network operation timed out"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result matched"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests"
                SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Server disconnected"
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported"
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language unavailable"
                SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT -> "Cannot check support"
                SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS -> "Cannot listen to download events"
                else -> "Unknown error"
            }
        }
    }
}

open class SFSpeechRecognitionRequest {
    var shouldReportPartialResults = true
    var requiresOnDeviceRecognition = false
    var addsPunctuation = false
    var contextualStrings: List<String> = emptyList()
}

class SFSpeechAudioBufferRecognitionRequest : SFSpeechRecognitionRequest() {
    private val converterLock = Any()
    private val converter = SpeechAudioConverter(speechAudioSampleRate)
    internal val audio = SpeechAudioPipe(maximumBufferedBytes = speechAudioSampleRate * 2 * maximumBufferedAudioSeconds)

    @Volatile
    internal var acceptsAudio = true

    @Volatile
    internal var onEndAudio: (() -> Unit)? = null

    fun appendAudioSampleBuffer(sampleBuffer: MediaSample) {
        if (!acceptsAudio || audio.isEnded) {
            return
        }
        val data = synchronized(converterLock) {
            converter.convert(sampleBuffer)
        } ?: return
        audio.write(data)
    }

    fun append(audioPCMBuffer: MediaSample) {
        appendAudioSampleBuffer(audioPCMBuffer)
    }

    fun endAudio() {
        audio.end()
        onEndAudio?.invoke()
    }
}

enum class SFSpeechRecognitionTaskState {
    starting,
    running,
    finishing,
    canceling,
    completed,
}

class SFSpeechRecognitionTask internal constructor(
    private val recognizer: SFSpeechRecognizer,
    internal val request: SFSpeechRecognitionRequest,
    internal val resultHandler: (SFSpeechRecognitionResult?, Throwable?) -> Unit,
) {
    var state = SFSpeechRecognitionTaskState.starting
        internal set
    var isCancelled = false
        internal set
    var error: Throwable? = null
        internal set
    val isFinishing: Boolean
        get() = state == SFSpeechRecognitionTaskState.finishing

    internal var endRequested = false
    internal var startedAtMs = 0L
    internal var readSide: ParcelFileDescriptor? = null
    internal var stopListening: Runnable? = null
    internal var audioDrainTimeout: Runnable? = null
    internal var stoppedListening = false
    internal var resultTimeout: Runnable? = null

    fun finish() {
        (request as? SFSpeechAudioBufferRecognitionRequest)?.endAudio()
    }

    fun cancel() {
        recognizer.cancelTask(this)
    }
}

class SFSpeechRecognizer(val locale: Locale = Locale.getDefault()) {
    var delegate: SFSpeechRecognizerDelegate? = null
    private val backend = SpeechRecognitionSystem.backend
    private val handler = Handler(Looper.getMainLooper())
    private var engine: SpeechRecognitionEngine? = null
    private var engineOnDevice = false
    private var currentTask: SFSpeechRecognitionTask? = null
    private var quickFailures = 0
    private val destroyIdleEngine = Runnable {
        if (currentTask == null) {
            destroyEngine()
        }
    }

    val isAvailable: Boolean
        get() = isRecognitionAvailable(backend)

    val supportsOnDeviceRecognition: Boolean
        get() = usesAudioSource && backend.isOnDeviceRecognitionAvailable()

    private val usesAudioSource: Boolean
        get() = backend.sdkInt >= Build.VERSION_CODES.TIRAMISU

    private val languageTag: String
        get() = locale.stripExtensions().toLanguageTag()

    fun recognitionTask(
        with: SFSpeechRecognitionRequest,
        resultHandler: (SFSpeechRecognitionResult?, Throwable?) -> Unit,
    ): SFSpeechRecognitionTask {
        val task = SFSpeechRecognitionTask(this, with, resultHandler)
        val request = with as? SFSpeechAudioBufferRecognitionRequest
        if (request != null) {
            request.acceptsAudio = usesAudioSource
            task.endRequested = request.audio.isEnded
            request.onEndAudio = {
                runOnMain {
                    endAudio(task)
                }
            }
        }
        runOnMain {
            schedule(task)
        }
        return task
    }

    internal fun cancelTask(task: SFSpeechRecognitionTask) {
        runOnMain {
            if (stopTask(task)) {
                task.isCancelled = true
                val error = SFSpeechRecognitionError.canceled()
                task.error = error
                handler.post {
                    task.resultHandler(null, error)
                }
            }
        }
    }

    private fun schedule(task: SFSpeechRecognitionTask) {
        handler.removeCallbacks(destroyIdleEngine)
        val previousTask = currentTask
        if (previousTask != null && previousTask !== task) {
            stopTask(previousTask)
        }
        if (task.state != SFSpeechRecognitionTaskState.starting) {
            return
        }
        currentTask = task
        if (task.request !is SFSpeechAudioBufferRecognitionRequest) {
            complete(task, null, SFSpeechRecognitionError.android(SpeechRecognizer.ERROR_CLIENT))
            return
        }
        val delay = if (quickFailures == 0) 0L else minOf(100L shl minOf(quickFailures - 1, 6), maximumRetryDelayMs)
        handler.postDelayed({ start(task) }, delay)
    }

    private fun start(task: SFSpeechRecognitionTask) {
        if (currentTask !== task || task.state != SFSpeechRecognitionTaskState.starting) {
            return
        }
        val request = task.request as SFSpeechAudioBufferRecognitionRequest
        val engine = engine ?: createEngine()
        if (engine == null) {
            failStart(task, SpeechRecognizer.ERROR_CLIENT)
            return
        }
        val intent = makeIntent(request)
        if (usesAudioSource) {
            val pipe = try {
                backend.createAudioPipe()
            } catch (error: IOException) {
                Log.i(TAG, "Failed to create audio pipe: $error")
                failStart(task, SpeechRecognizer.ERROR_AUDIO)
                return
            }
            task.readSide = pipe[0]
            intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, pipe[0])
            intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
            intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, speechAudioSampleRate)
            request.audio.attach(backend.openAudioOutput(pipe[1])) {
                handler.post {
                    scheduleStopListeningIfAudioFinished(task)
                }
            }
        }
        task.startedAtMs = SystemClock.uptimeMillis()
        task.state = if (task.endRequested) {
            SFSpeechRecognitionTaskState.finishing
        } else {
            SFSpeechRecognitionTaskState.running
        }
        try {
            engine.startListening(intent, TaskListener(task))
        } catch (error: RuntimeException) {
            Log.i(TAG, "Failed to start listening: $error")
            destroyEngine()
            failStart(task, SpeechRecognizer.ERROR_CLIENT)
            return
        }
        if (task.endRequested) {
            if (usesAudioSource) {
                finishAudio(task)
            } else {
                stopListening(task)
            }
        }
    }

    private fun failStart(task: SFSpeechRecognitionTask, code: Int) {
        quickFailures += 1
        complete(task, null, SFSpeechRecognitionError.android(code))
    }

    private fun endAudio(task: SFSpeechRecognitionTask) {
        when (task.state) {
            SFSpeechRecognitionTaskState.starting -> task.endRequested = true
            SFSpeechRecognitionTaskState.running -> {
                task.endRequested = true
                task.state = SFSpeechRecognitionTaskState.finishing
                if (usesAudioSource) {
                    finishAudio(task)
                } else if (currentTask === task) {
                    stopListening(task)
                }
            }
            else -> Unit
        }
    }

    private fun finishAudio(task: SFSpeechRecognitionTask) {
        scheduleStopListeningIfAudioFinished(task)
        if (task.stopListening != null || task.audioDrainTimeout != null) {
            return
        }
        val request = task.request as? SFSpeechAudioBufferRecognitionRequest ?: return
        val audioDrainTimeout = Runnable {
            task.audioDrainTimeout = null
            if (currentTask === task &&
                task.state == SFSpeechRecognitionTaskState.finishing &&
                task.stopListening == null
            ) {
                Log.i(TAG, "Recognizer stopped reading audio, stopping to listen")
                request.audio.close()
                stopListening(task)
            }
        }
        task.audioDrainTimeout = audioDrainTimeout
        handler.postDelayed(audioDrainTimeout, audioDrainTimeoutMs)
    }

    private fun scheduleStopListeningIfAudioFinished(task: SFSpeechRecognitionTask) {
        if (currentTask !== task ||
            task.state != SFSpeechRecognitionTaskState.finishing ||
            task.stopListening != null ||
            task.stoppedListening
        ) {
            return
        }
        val request = task.request as? SFSpeechAudioBufferRecognitionRequest ?: return
        if (!request.audio.isFinished) {
            return
        }
        val stopListeningLater = Runnable {
            if (currentTask === task && task.state == SFSpeechRecognitionTaskState.finishing) {
                stopListening(task)
            }
        }
        task.audioDrainTimeout?.let { handler.removeCallbacks(it) }
        task.audioDrainTimeout = null
        task.stopListening = stopListeningLater
        handler.postDelayed(stopListeningLater, stopListeningAfterAudioEndedMs)
    }

    private fun stopListening(task: SFSpeechRecognitionTask) {
        if (task.stoppedListening) {
            return
        }
        task.stoppedListening = true
        engine?.stopListening()
        val resultTimeout = Runnable {
            if (currentTask === task && task.state == SFSpeechRecognitionTaskState.finishing) {
                Log.i(TAG, "No result after stopping to listen")
                engine?.cancel()
                destroyEngine()
                complete(task, null, SFSpeechRecognitionError.noSpeechDetected())
            }
        }
        task.resultTimeout = resultTimeout
        handler.postDelayed(resultTimeout, resultAfterStopListeningTimeoutMs)
    }

    private fun stopTask(task: SFSpeechRecognitionTask): Boolean {
        if (task.state == SFSpeechRecognitionTaskState.completed ||
            task.state == SFSpeechRecognitionTaskState.canceling
        ) {
            return false
        }
        val started = task.state != SFSpeechRecognitionTaskState.starting
        task.state = SFSpeechRecognitionTaskState.canceling
        if (currentTask === task && started) {
            engine?.cancel()
            destroyEngine()
        }
        complete(task, null, null, notify = false)
        return true
    }

    private fun complete(
        task: SFSpeechRecognitionTask,
        result: SFSpeechRecognitionResult?,
        error: Throwable?,
        notify: Boolean = true,
    ) {
        if (task.state == SFSpeechRecognitionTaskState.completed) {
            return
        }
        task.state = SFSpeechRecognitionTaskState.completed
        task.error = error
        task.stopListening?.let { handler.removeCallbacks(it) }
        task.stopListening = null
        task.audioDrainTimeout?.let { handler.removeCallbacks(it) }
        task.audioDrainTimeout = null
        task.resultTimeout?.let { handler.removeCallbacks(it) }
        task.resultTimeout = null
        (task.request as? SFSpeechAudioBufferRecognitionRequest)?.let {
            it.onEndAudio = null
            it.audio.close()
        }
        try {
            task.readSide?.close()
        } catch (closeError: IOException) {
            Log.i(TAG, "Failed to close audio pipe: $closeError")
        }
        task.readSide = null
        if (currentTask === task) {
            if (task.stoppedListening) {
                destroyEngine()
            }
            currentTask = null
            handler.postDelayed(destroyIdleEngine, idleEngineLifetimeMs)
        }
        if (notify) {
            task.resultHandler(result, error)
        }
    }

    private fun handleResults(task: SFSpeechRecognitionTask, bundle: Bundle?, isFinal: Boolean) {
        val transcriptions = bundle
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.filterNotNull()
            ?.map { SFTranscription(it) }
            ?: emptyList()
        val bestTranscription = transcriptions.firstOrNull()
        if (bestTranscription == null || bestTranscription.formattedString.isBlank()) {
            if (isFinal) {
                handleError(task, SFSpeechRecognitionError.noSpeechDetected(), null)
            }
            return
        }
        quickFailures = 0
        val result = SFSpeechRecognitionResult(bestTranscription, transcriptions, isFinal)
        if (isFinal) {
            complete(task, result, null)
        } else if (task.request.shouldReportPartialResults) {
            task.resultHandler(result, null)
        }
    }

    private fun handleError(task: SFSpeechRecognitionTask, error: SFSpeechRecognitionError, code: Int?) {
        if (SystemClock.uptimeMillis() - task.startedAtMs < quickFailureMs) {
            quickFailures += 1
        } else {
            quickFailures = 0
        }
        if (code == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
            code == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
        ) {
            val onDeviceEngine = engine
            if (engineOnDevice && onDeviceEngine != null) {
                Log.i(TAG, "On-device recognition of $languageTag unavailable, using default recognizer")
                SpeechRecognitionSystem.onDeviceUnavailableLanguageTags.add(languageTag)
                engine = null
                engineOnDevice = false
                onDeviceEngine.triggerModelDownload(makeIntent(task.request))
                handler.post {
                    onDeviceEngine.destroy()
                }
            }
        }
        val hardError = code != null &&
            code != SpeechRecognizer.ERROR_NO_MATCH &&
            code != SpeechRecognizer.ERROR_SPEECH_TIMEOUT
        val onDeviceFailure = hardError &&
            code != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS &&
            code != SpeechRecognizer.ERROR_TOO_MANY_REQUESTS
        if (engineOnDevice && onDeviceFailure && quickFailures >= onDeviceQuickFailuresBeforeFallback) {
            Log.i(TAG, "On-device recognizer keeps failing with $code, using default recognizer")
            SpeechRecognitionSystem.onDeviceUnavailableLanguageTags.add(languageTag)
        }
        if (hardError) {
            destroyEngine()
        }
        complete(task, null, error)
    }

    private fun createEngine(): SpeechRecognitionEngine? {
        val onDevice = usesAudioSource &&
            !SpeechRecognitionSystem.onDeviceUnavailableLanguageTags.contains(languageTag) &&
            backend.isOnDeviceRecognitionAvailable()
        var engine = backend.createEngine(onDevice)
        engineOnDevice = onDevice && engine != null
        if (engine == null && onDevice) {
            engine = backend.createEngine(false)
        }
        this.engine = engine
        return engine
    }

    private fun destroyEngine() {
        engine?.destroy()
        engine = null
        engineOnDevice = false
    }

    private fun makeIntent(request: SFSpeechRecognitionRequest): Intent {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, request.shouldReportPartialResults)
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, backend.packageName)
        if (request.requiresOnDeviceRecognition) {
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        if (backend.sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            if (request.addsPunctuation) {
                intent.putExtra(RecognizerIntent.EXTRA_ENABLE_FORMATTING, RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY)
            }
            if (request.contextualStrings.isNotEmpty()) {
                intent.putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, ArrayList(request.contextualStrings))
            }
        }
        return intent
    }

    private fun isActive(task: SFSpeechRecognitionTask): Boolean {
        return currentTask === task &&
            (task.state == SFSpeechRecognitionTaskState.running || task.state == SFSpeechRecognitionTaskState.finishing)
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            handler.post(block)
        }
    }

    private inner class TaskListener(private val task: SFSpeechRecognitionTask) : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            if (!isActive(task)) {
                return
            }
            handleError(task, SFSpeechRecognitionError.android(error), error)
        }

        override fun onResults(results: Bundle?) {
            if (!isActive(task)) {
                return
            }
            handleResults(task, results, isFinal = true)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!isActive(task)) {
                return
            }
            handleResults(task, partialResults, isFinal = false)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    companion object {
        fun authorizationStatus(): SFSpeechRecognizerAuthorizationStatus {
            val backend = SpeechRecognitionSystem.backend
            if (!isRecognitionAvailable(backend)) {
                return SFSpeechRecognizerAuthorizationStatus.restricted
            }
            if (!backend.hasRecordAudioPermission()) {
                return SFSpeechRecognizerAuthorizationStatus.denied
            }
            return SFSpeechRecognizerAuthorizationStatus.authorized
        }

        fun requestAuthorization(handler: (SFSpeechRecognizerAuthorizationStatus) -> Unit) {
            val status = authorizationStatus()
            Handler(Looper.getMainLooper()).post {
                handler(status)
            }
        }

        private fun isRecognitionAvailable(backend: SpeechRecognitionBackend): Boolean {
            if (backend.isRecognitionAvailable()) {
                return true
            }
            return backend.sdkInt >= Build.VERSION_CODES.TIRAMISU && backend.isOnDeviceRecognitionAvailable()
        }
    }
}
