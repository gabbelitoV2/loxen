package com.moblin.android.platform.speech

import android.media.AudioFormat
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SFSpeechRecognizerSuite {
    private lateinit var backend: FakeSpeechRecognitionBackend
    private val results = mutableListOf<Pair<SFSpeechRecognitionResult?, Throwable?>>()

    @Before
    fun setUp() {
        backend = FakeSpeechRecognitionBackend()
        SpeechRecognitionSystem.backend = backend
    }

    @After
    fun tearDown() {
        SpeechRecognitionSystem.reset()
    }

    private fun startTask(
        recognizer: SFSpeechRecognizer = SFSpeechRecognizer(Locale.forLanguageTag("sv-SE")),
    ): Pair<SFSpeechAudioBufferRecognitionRequest, SFSpeechRecognitionTask> {
        val request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true
        request.addsPunctuation = false
        val task = recognizer.recognitionTask(with = request) { result, error ->
            results.add(result to error)
        }
        runMain()
        return request to task
    }

    @Test
    fun streamAudioIsPassedToTheOnDeviceRecognizerThroughAPipe() {
        val (request, task) = startTask()
        assertEquals(1, backend.engines.size)
        val engine = backend.engine
        assertTrue(engine.onDevice)
        assertEquals(SFSpeechRecognitionTaskState.running, task.state)
        val intent = engine.intent
        assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, intent.action)
        assertEquals("sv-SE", intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertEquals(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL))
        assertTrue(intent.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false))
        assertEquals("com.moblin.android", intent.getStringExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE))
        assertEquals(AudioFormat.ENCODING_PCM_16BIT, intent.getIntExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, -1))
        assertEquals(1, intent.getIntExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, -1))
        assertEquals(16000, intent.getIntExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, -1))
        assertFalse(intent.hasExtra(RecognizerIntent.EXTRA_ENABLE_FORMATTING))
        val source = assertNotNull(engine.audioSource)
        request.appendAudioSampleBuffer(pcm16Sample(48000, 2, 4800) { _, _ -> 1234 })
        waitUntil { request.audio.writtenBytes == 3200L }
        val samples = shorts(readAvailable(source))
        assertEquals(1600, samples.size)
        assertTrue(samples.all { it.toInt() == 1234 })
    }

    @Test
    fun theDefaultRecognizerIsUsedWhenOnDeviceRecognitionIsUnavailable() {
        backend.onDeviceRecognitionAvailable = false
        startTask()
        assertFalse(backend.engine.onDevice)
        assertNotNull(backend.engine.audioSource)
    }

    @Test
    fun theDefaultRecognizerIsUsedWhenTheOnDeviceRecognizerCannotBeCreated() {
        backend.onDeviceCreationFails = true
        startTask()
        assertFalse(backend.engine.onDevice)
    }

    @Test
    fun addsPunctuationEnablesFormatting() {
        val request = SFSpeechAudioBufferRecognitionRequest()
        request.addsPunctuation = true
        SFSpeechRecognizer().recognitionTask(with = request) { _, _ -> }
        runMain()
        assertEquals(
            RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY,
            backend.engine.intent.getStringExtra(RecognizerIntent.EXTRA_ENABLE_FORMATTING),
        )
    }

    @Test
    fun partialAndFinalResultsReachTheResultHandler() {
        val (_, task) = startTask()
        val engine = backend.engine
        engine.partial("hej")
        engine.partial("hej hopp", "hej hop")
        assertEquals(2, results.size)
        assertEquals("hej", results[0].first!!.bestTranscription.formattedString)
        assertFalse(results[0].first!!.isFinal)
        assertEquals(listOf("hej hopp", "hej hop"), results[1].first!!.transcriptions.map { it.formattedString })
        engine.final("hej hopp")
        assertEquals(3, results.size)
        val final = results[2].first!!
        assertTrue(final.isFinal)
        assertEquals("hej hopp", final.bestTranscription.formattedString)
        assertNull(results[2].second)
        assertEquals(SFSpeechRecognitionTaskState.completed, task.state)
        engine.partial("late")
        engine.error(SpeechRecognizer.ERROR_CLIENT)
        assertEquals(3, results.size)
    }

    @Test
    fun emptyPartialResultsAreIgnoredAndAnEmptyFinalResultIsNoSpeechDetected() {
        startTask()
        backend.engine.partial("")
        assertTrue(results.isEmpty())
        backend.engine.final()
        assertEquals(1, results.size)
        val error = results[0].second as SFSpeechRecognitionError
        assertEquals("kAFAssistantErrorDomain", error.domain)
        assertEquals(1110, error.code)
    }

    @Test
    fun errorsReachTheResultHandlerAndResetTheRecognizerUnlessNothingWasSaid() {
        startTask()
        val first = backend.engine
        runMainFor(2_000)
        first.error(SpeechRecognizer.ERROR_NO_MATCH)
        val error = results.single().second as SFSpeechRecognitionError
        assertEquals(SpeechRecognizer.ERROR_NO_MATCH, error.code)
        assertFalse(first.destroyed)
        results.clear()
        val recognizer = SFSpeechRecognizer()
        startTask(recognizer)
        val second = backend.engine
        runMainFor(2_000)
        second.error(SpeechRecognizer.ERROR_SERVER_DISCONNECTED)
        assertTrue(second.destroyed)
        startTask(recognizer)
        assertEquals(3, backend.engines.size)
    }

    @Test
    fun theRecognizerIsReusedBetweenTasksAndDestroyedWhenIdle() {
        val recognizer = SFSpeechRecognizer()
        startTask(recognizer)
        val engine = backend.engine
        engine.final("one")
        startTask(recognizer)
        assertEquals(1, backend.engines.size)
        assertEquals(2, engine.intents.size)
        assertTrue(engine.listeners[0] !== engine.listeners[1])
        engine.final("two")
        runMainFor(4_000)
        assertFalse(engine.destroyed)
        runMainFor(2_000)
        assertTrue(engine.destroyed)
    }

    @Test
    fun missingOnDeviceLanguageDownloadsItOnceAndFallsBackToTheDefaultRecognizer() {
        val recognizer = SFSpeechRecognizer(Locale.forLanguageTag("sv-SE"))
        startTask(recognizer)
        val onDevice = backend.engine
        assertTrue(onDevice.onDevice)
        onDevice.error(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
        assertEquals("sv-SE", onDevice.modelDownloads.single().getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        runMain()
        assertEquals(1, onDevice.modelDownloadsSent)
        assertEquals(0, onDevice.modelDownloadsAfterDestroy)
        assertTrue(onDevice.destroyed)
        runMainFor(1_000)
        startTask(recognizer)
        runMainFor(1_000)
        assertFalse(backend.engine.onDevice)
        backend.engine.final("hej")
        startTask(SFSpeechRecognizer(Locale.forLanguageTag("sv-SE")))
        assertFalse(backend.engine.onDevice)
        assertEquals(1, backend.engines.count { it.onDevice })
        assertEquals(1, backend.engines.sumOf { it.modelDownloads.size })
        startTask(SFSpeechRecognizer(Locale.forLanguageTag("en-US")))
        assertTrue(backend.engine.onDevice)
    }

    @Test
    fun anOnDeviceRecognizerThatKeepsFailingIsReplacedByTheDefaultRecognizer() {
        val recognizer = SFSpeechRecognizer(Locale.forLanguageTag("sv-SE"))
        startTask(recognizer)
        repeat(3) {
            assertTrue(backend.engine.onDevice)
            backend.engine.error(SpeechRecognizer.ERROR_SERVER)
            startTask(recognizer)
            runMainFor(1_000)
        }
        assertFalse(backend.engine.onDevice)
        assertTrue(backend.engines.all { it.destroyed || !it.onDevice })
        assertTrue(backend.engines.all { it.modelDownloads.isEmpty() })
        backend.engine.error(SpeechRecognizer.ERROR_NO_MATCH)
        startTask(SFSpeechRecognizer(Locale.forLanguageTag("sv-SE")))
        assertFalse(backend.engine.onDevice)
    }

    @Test
    fun quickNoMatchOrPermissionErrorsKeepTheOnDeviceRecognizer() {
        val recognizer = SFSpeechRecognizer(Locale.forLanguageTag("sv-SE"))
        startTask(recognizer)
        repeat(4) {
            backend.engine.error(SpeechRecognizer.ERROR_NO_MATCH)
            startTask(recognizer)
            runMainFor(2_000)
        }
        assertEquals(1, backend.engines.size)
        assertTrue(backend.engine.onDevice)
        repeat(4) {
            backend.engine.error(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
            startTask(recognizer)
            runMainFor(6_000)
        }
        assertEquals(5, backend.engines.size)
        assertTrue(backend.engines.all { it.onDevice })
    }

    @Test
    fun theLanguageIsSentWithoutRegionalPreferenceExtensions() {
        startTask(SFSpeechRecognizer(Locale.forLanguageTag("sv-SE-u-fw-mon-mu-celsius")))
        assertEquals("sv-SE", backend.engine.intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
    }

    @Test
    fun aRecognizerThatStopsReadingAudioIsStillStoppedAndGivenUp() {
        val stuck = StuckAudioOutput()
        backend.stuckAudioOutput = stuck
        val (request, task) = startTask()
        val engine = backend.engine
        request.appendAudioSampleBuffer(pcm16Sample(16000, 1, 160) { _, _ -> 1 })
        waitUntil { stuck.blockedWrites.get() == 1 }
        request.endAudio()
        assertEquals(SFSpeechRecognitionTaskState.finishing, task.state)
        runMainFor(1_900)
        assertEquals(0, engine.stopListeningCount)
        runMainFor(200)
        assertEquals(1, engine.stopListeningCount)
        assertTrue(stuck.isClosed)
        waitUntil { request.audio.isFinished }
        Thread.sleep(50)
        runMainFor(1_000)
        assertEquals(1, engine.stopListeningCount)
        assertTrue(results.isEmpty())
        runMainFor(4_000)
        assertEquals(1110, (results.single().second as SFSpeechRecognitionError).code)
        assertTrue(engine.destroyed)
    }

    @Test
    fun aRecognizerThatWasToldToStopListeningIsNotReused() {
        val recognizer = SFSpeechRecognizer()
        val (request, _) = startTask(recognizer)
        val engine = backend.engine
        request.endAudio()
        waitUntil { request.audio.isFinished }
        Thread.sleep(50)
        runMainFor(500)
        assertEquals(1, engine.stopListeningCount)
        engine.final("done")
        assertTrue(engine.destroyed)
        startTask(recognizer)
        assertEquals(2, backend.engines.size)
        assertEquals(1, engine.intents.size)
    }

    @Test
    fun audioEndedBeforeTheTaskStartsFinishesTheTask() {
        val request = SFSpeechAudioBufferRecognitionRequest()
        request.appendAudioSampleBuffer(pcm16Sample(16000, 1, 160) { _, _ -> 1 })
        request.endAudio()
        val task = SFSpeechRecognizer().recognitionTask(with = request) { result, error ->
            results.add(result to error)
        }
        runMain()
        assertEquals(SFSpeechRecognitionTaskState.finishing, task.state)
        waitUntil { request.audio.isFinished }
        Thread.sleep(50)
        runMainFor(500)
        assertEquals(1, backend.engine.stopListeningCount)
    }

    @Test
    fun quickFailuresBackOffBeforeTheNextStart() {
        val recognizer = SFSpeechRecognizer()
        startTask(recognizer)
        backend.engine.error(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
        startTask(recognizer)
        assertEquals(1, backend.engines.size)
        runMainFor(100)
        assertEquals(2, backend.engines.size)
        backend.engine.error(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
        startTask(recognizer)
        runMainFor(150)
        assertEquals(2, backend.engines.size)
        runMainFor(60)
        assertEquals(3, backend.engines.size)
        runMainFor(2_000)
        backend.engine.final("hello")
        startTask(recognizer)
        assertEquals(2, backend.engine.intents.size)
    }

    @Test
    fun failingToCreateARecognizerIsReportedAndBacksOff() {
        backend.creationFails = true
        val recognizer = SFSpeechRecognizer()
        startTask(recognizer)
        val error = results.single().second as SFSpeechRecognitionError
        assertEquals(SpeechRecognizer.ERROR_CLIENT, error.code)
        results.clear()
        startTask(recognizer)
        assertTrue(results.isEmpty())
        runMainFor(100)
        assertEquals(1, results.size)
    }

    @Test
    fun endAudioClosesThePipeAndThenStopsListening() {
        val (request, task) = startTask()
        val engine = backend.engine
        val source = engine.audioSource!!
        request.appendAudioSampleBuffer(pcm16Sample(16000, 1, 160) { _, _ -> 1 })
        request.endAudio()
        assertEquals(SFSpeechRecognitionTaskState.finishing, task.state)
        assertTrue(task.isFinishing)
        waitUntil { request.audio.isFinished }
        Thread.sleep(50)
        assertEquals(320, readAvailable(source).size)
        request.appendAudioSampleBuffer(pcm16Sample(16000, 1, 160) { _, _ -> 1 })
        runMain()
        assertEquals(0, engine.stopListeningCount)
        runMainFor(500)
        assertEquals(1, engine.stopListeningCount)
        engine.final("done")
        assertTrue(results.single().first!!.isFinal)
    }

    @Test
    fun aRecognizerThatNeverAnswersAfterStoppingIsGivenUp() {
        val (request, task) = startTask()
        val engine = backend.engine
        request.endAudio()
        waitUntil { request.audio.isFinished }
        Thread.sleep(50)
        runMainFor(500)
        assertEquals(1, engine.stopListeningCount)
        runMainFor(4_900)
        assertTrue(results.isEmpty())
        runMainFor(200)
        val error = results.single().second as SFSpeechRecognitionError
        assertEquals(1110, error.code)
        assertEquals(SFSpeechRecognitionTaskState.completed, task.state)
        assertTrue(engine.destroyed)
        assertEquals(1, engine.cancelCount)
    }

    @Test
    fun finishEndsTheAudio() {
        val (request, task) = startTask()
        task.finish()
        assertEquals(SFSpeechRecognitionTaskState.finishing, task.state)
        waitUntil { request.audio.isFinished }
        Thread.sleep(50)
        runMainFor(600)
        assertEquals(1, backend.engine.stopListeningCount)
    }

    @Test
    fun cancelStopsTheRecognizerAndReportsCancellationLater() {
        val (request, task) = startTask()
        val engine = backend.engine
        task.cancel()
        assertTrue(task.isCancelled)
        assertEquals(1, engine.cancelCount)
        assertTrue(engine.destroyed)
        assertTrue(request.audio.isEnded)
        assertTrue(results.isEmpty())
        runMain()
        val error = results.single().second as SFSpeechRecognitionError
        assertEquals("kLSRErrorDomain", error.domain)
        assertEquals(301, error.code)
        engine.final("ignored")
        task.cancel()
        runMain()
        assertEquals(1, results.size)
    }

    @Test
    fun aNewTaskReplacesTheRunningOneWithoutReportingIt() {
        val recognizer = SFSpeechRecognizer()
        val (_, first) = startTask(recognizer)
        val firstEngine = backend.engine
        val (_, second) = startTask(recognizer)
        assertEquals(SFSpeechRecognitionTaskState.completed, first.state)
        assertTrue(firstEngine.destroyed)
        assertEquals(SFSpeechRecognitionTaskState.running, second.state)
        assertEquals(2, backend.engines.size)
        assertTrue(results.isEmpty())
    }

    @Test
    fun belowAndroid13TheMicrophoneIsUsedAndAppendedAudioIsIgnored() {
        backend.sdkInt = 32
        val (request, task) = startTask()
        val engine = backend.engine
        assertFalse(engine.onDevice)
        assertNull(engine.audioSource)
        assertFalse(engine.intent.hasExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE))
        request.appendAudioSampleBuffer(pcm16Sample(48000, 1, 480) { _, _ -> 1 })
        assertEquals(0, request.audio.writtenBytes)
        request.endAudio()
        assertEquals(1, engine.stopListeningCount)
        assertEquals(SFSpeechRecognitionTaskState.finishing, task.state)
        engine.final("mic")
        assertTrue(results.single().first!!.isFinal)
        runMainFor(6_000)
        assertEquals(1, results.size)
    }

    @Test
    fun authorizationFollowsTheRecognitionServiceAndTheMicrophonePermission() {
        assertEquals(SFSpeechRecognizerAuthorizationStatus.authorized, SFSpeechRecognizer.authorizationStatus())
        backend.recordAudioPermission = false
        assertEquals(SFSpeechRecognizerAuthorizationStatus.denied, SFSpeechRecognizer.authorizationStatus())
        backend.recognitionAvailable = false
        backend.onDeviceRecognitionAvailable = false
        assertEquals(SFSpeechRecognizerAuthorizationStatus.restricted, SFSpeechRecognizer.authorizationStatus())
        backend.recordAudioPermission = true
        backend.onDeviceRecognitionAvailable = true
        assertEquals(SFSpeechRecognizerAuthorizationStatus.authorized, SFSpeechRecognizer.authorizationStatus())
        backend.sdkInt = 32
        assertEquals(SFSpeechRecognizerAuthorizationStatus.restricted, SFSpeechRecognizer.authorizationStatus())
        val statuses = mutableListOf<SFSpeechRecognizerAuthorizationStatus>()
        SFSpeechRecognizer.requestAuthorization { statuses.add(it) }
        assertTrue(statuses.isEmpty())
        runMain()
        assertEquals(listOf(SFSpeechRecognizerAuthorizationStatus.restricted), statuses)
    }
}
