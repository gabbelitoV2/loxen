package com.moblin.android.various

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.speech.SpeechRecognizer
import com.moblin.android.platform.speech.FakeSpeechRecognitionBackend
import com.moblin.android.platform.speech.SpeechRecognitionSystem
import com.moblin.android.platform.speech.pcm16Sample
import com.moblin.android.platform.speech.readAvailable
import com.moblin.android.platform.speech.runMain
import com.moblin.android.platform.speech.runMainFor
import com.moblin.android.platform.speech.waitUntil
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpeechToTextSuite {
    private lateinit var backend: FakeSpeechRecognitionBackend
    private lateinit var speechToText: SpeechToText
    private val partialResults = mutableListOf<Pair<Int, String>>()
    private val errors = mutableListOf<String>()
    private var clears = 0

    @Before
    fun setUp() {
        backend = FakeSpeechRecognitionBackend()
        SpeechRecognitionSystem.backend = backend
        speechToText = SpeechToText()
        speechToText.delegate = object : SpeechToTextDelegate {
            override fun speechToTextPartialResult(position: Int, text: String) {
                partialResults.add(position to text)
            }

            override fun speechToTextClear() {
                clears += 1
            }
        }
    }

    @After
    fun tearDown() {
        speechToText.stop()
        runMain()
        SpeechRecognitionSystem.reset()
    }

    private fun start() {
        speechToText.start { errors.add(it) }
        runMain()
    }

    private fun countingReader(source: ParcelFileDescriptor): () -> Int {
        var total = 0
        return {
            total += readAvailable(source).size
            total
        }
    }

    @Test
    fun partialResultsAreReportedAfterTheFrozenTextOfEarlierRecognitions() {
        start()
        val engine = backend.engine
        assertEquals(1, engine.intents.size)
        engine.partial("hello")
        engine.partial("hello world")
        assertEquals(listOf(0 to "hello", 0 to "hello world"), partialResults)
        engine.final("hello world!")
        runMain()
        assertEquals(2, engine.intents.size)
        engine.partial("how are you")
        assertEquals(0 to "hello world how are you", partialResults.last())
    }

    @Test
    fun anErrorRestartsRecognitionAndKeepsTheText() {
        start()
        val engine = backend.engine
        engine.partial("one")
        engine.error(SpeechRecognizer.ERROR_NO_MATCH)
        runMainFor(200)
        assertEquals(2, engine.intents.size)
        engine.partial("two")
        assertEquals(0 to "one two", partialResults.last())
        assertTrue(errors.isEmpty())
    }

    @Test
    fun frozenTextKeepsTheLast150CharactersAndMovesThePosition() {
        start()
        val engine = backend.engine
        val a = "a".repeat(100)
        val b = "b".repeat(100)
        engine.partial(a)
        engine.final(a)
        runMain()
        engine.partial(b)
        assertEquals(0 to "$a $b", partialResults.last())
        engine.final(b)
        runMain()
        engine.partial("c")
        assertEquals(51 to "a".repeat(49) + " " + b + " c", partialResults.last())
    }

    @Test
    fun tickEndsTheAudioTwoSecondsAfterTheLatestResult() {
        start()
        val engine = backend.engine
        val reader = countingReader(engine.audioSource!!)
        speechToText.append(sampleBuffer = pcm16Sample(48000, 1, 4800) { _, _ -> 100 })
        waitUntil { reader() == 3200 }
        engine.partial("hello")
        val resultTime = SystemClock.elapsedRealtimeNanos()
        speechToText.tick(now = resultTime + 1_900_000_000L)
        runMainFor(600)
        assertEquals(0, engine.stopListeningCount)
        speechToText.tick(now = resultTime + 2_100_000_000L)
        speechToText.append(sampleBuffer = pcm16Sample(48000, 1, 4800) { _, _ -> 100 })
        Thread.sleep(100)
        runMainFor(600)
        assertEquals(1, engine.stopListeningCount)
        assertEquals(3200, reader())
        engine.final("hello there")
        runMain()
        assertTrue(engine.destroyed)
        assertEquals(1, engine.intents.size)
        assertEquals(2, backend.engines.size)
        speechToText.append(sampleBuffer = pcm16Sample(48000, 1, 4800) { _, _ -> 100 })
        val nextReader = countingReader(backend.engine.audioSource!!)
        waitUntil { nextReader() == 3200 }
    }

    @Test
    fun tickClearsTheFrozenTextFiveSecondsAfterTheLatestResult() {
        start()
        val engine = backend.engine
        engine.partial("hello")
        val resultTime = SystemClock.elapsedRealtimeNanos()
        engine.final("hello")
        runMain()
        speechToText.tick(now = resultTime + 4_900_000_000L)
        assertEquals(0, clears)
        speechToText.tick(now = resultTime + 5_100_000_000L)
        assertEquals(1, clears)
        speechToText.tick(now = resultTime + 6_000_000_000L)
        assertEquals(1, clears)
        engine.partial("again")
        assertEquals(0 to "again", partialResults.last())
    }

    @Test
    fun audioIsOnlyAppendedWhileRunning() {
        speechToText.append(sampleBuffer = pcm16Sample(48000, 1, 4800) { _, _ -> 100 })
        start()
        val reader = countingReader(backend.engine.audioSource!!)
        Thread.sleep(50)
        assertEquals(0, reader())
        speechToText.append(sampleBuffer = pcm16Sample(48000, 2, 960) { _, _ -> 100 })
        waitUntil { reader() == 640 }
        speechToText.stop()
        speechToText.append(sampleBuffer = pcm16Sample(48000, 2, 960) { _, _ -> 100 })
        runMainFor(1_000)
        assertEquals(1, backend.engines.size)
        assertEquals(1, backend.engine.intents.size)
    }

    @Test
    fun stopCancelsRecognitionWithoutRestarting() {
        start()
        val engine = backend.engine
        speechToText.stop()
        assertEquals(1, engine.cancelCount)
        assertTrue(engine.destroyed)
        runMainFor(6_000)
        assertEquals(1, backend.engines.size)
        assertEquals(1, engine.intents.size)
    }

    @Test
    fun authorizationProblemsAreReported() {
        backend.recordAudioPermission = false
        start()
        assertEquals(listOf("Speech recognition not allowed"), errors)
        assertTrue(backend.engines.isEmpty())
        errors.clear()
        backend.recognitionAvailable = false
        backend.onDeviceRecognitionAvailable = false
        start()
        assertEquals(listOf("Speech recognition restricted on this device"), errors)
        assertTrue(backend.engines.isEmpty())
    }
}
