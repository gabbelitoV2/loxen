package com.moblin.android.platform.avfoundation

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.moblin.android.AppDelegate
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

const val AVSpeechUtteranceMinimumSpeechRate: Float = 0f
const val AVSpeechUtteranceMaximumSpeechRate: Float = 1f
const val AVSpeechUtteranceDefaultSpeechRate: Float = 0.5f

enum class AVSpeechBoundary {
    immediate,
    word,
}

class AVSpeechSynthesisVoice private constructor(
    val identifier: String,
    val language: String,
    val name: String,
) {
    override fun equals(other: Any?): Boolean {
        return other is AVSpeechSynthesisVoice && other.identifier == identifier
    }

    override fun hashCode(): Int {
        return identifier.hashCode()
    }

    override fun toString(): String {
        return "AVSpeechSynthesisVoice($identifier, $language)"
    }

    companion object {
        operator fun invoke(identifier: String): AVSpeechSynthesisVoice? {
            return speechVoices().firstOrNull { it.identifier == identifier }
        }

        @JvmName("invokeWithLanguage")
        operator fun invoke(language: String?): AVSpeechSynthesisVoice? {
            val tag = language ?: Locale.getDefault().toLanguageTag()
            val voices = speechVoices()
            return voices.firstOrNull { it.language.equals(tag, ignoreCase = true) }
                ?: voices.firstOrNull { it.language.startsWith(tag.substringBefore('-'), ignoreCase = true) }
        }

        fun speechVoices(): List<AVSpeechSynthesisVoice> {
            SpeechEngine.prepare()
            val voices = SpeechEngine.currentVoices()
            if (voices.isEmpty()) {
                ReaderLog.once("speechVoicesEmpty", "AVSpeechSynthesisVoice.speechVoices: no voices yet")
            }
            return voices
        }

        internal fun make(voice: Voice): AVSpeechSynthesisVoice {
            return AVSpeechSynthesisVoice(identifier = voice.name, language = voice.locale.toLanguageTag(), name = voice.name)
        }
    }
}

class AVSpeechUtterance(val string: String) {
    var rate: Float = AVSpeechUtteranceDefaultSpeechRate
    var pitchMultiplier: Float = 1f
    var volume: Float = 1f
    var voice: AVSpeechSynthesisVoice? = null
    var preUtteranceDelay: Double = 0.0
    var postUtteranceDelay: Double = 0.0
}

interface AVSpeechSynthesizerDelegate {
    fun speechSynthesizerDidStart(synthesizer: AVSpeechSynthesizer, utterance: AVSpeechUtterance) {}

    fun speechSynthesizerDidFinish(synthesizer: AVSpeechSynthesizer, utterance: AVSpeechUtterance) {}

    fun speechSynthesizerDidCancel(synthesizer: AVSpeechSynthesizer, utterance: AVSpeechUtterance) {}
}

class AVSpeechSynthesizer {
    private var delegateReference: WeakReference<AVSpeechSynthesizerDelegate>? = null
    internal val outstanding = AtomicInteger(0)

    var delegate: AVSpeechSynthesizerDelegate?
        get() = delegateReference?.get()
        set(value) {
            delegateReference = value?.let { WeakReference(it) }
        }

    var usesApplicationAudioSession: Boolean = true

    val isSpeaking: Boolean
        get() = outstanding.get() > 0

    val isPaused: Boolean
        get() = false

    init {
        SpeechEngine.prepare()
    }

    fun speak(utterance: AVSpeechUtterance) {
        outstanding.incrementAndGet()
        SpeechEngine.speak(this, utterance)
    }

    fun stopSpeaking(at: AVSpeechBoundary): Boolean {
        val wasSpeaking = isSpeaking
        SpeechEngine.stop(this)
        return wasSpeaking
    }

    fun pauseSpeaking(at: AVSpeechBoundary): Boolean {
        ReaderLog.once("pauseSpeaking", "AVSpeechSynthesizer.pauseSpeaking is not supported by Android text to speech")
        return false
    }

    fun continueSpeaking(): Boolean {
        return false
    }
}

internal object SpeechEngine {
    private enum class State {
        uninitialized,
        initializing,
        ready,
    }

    private class Spoken(
        val synthesizer: WeakReference<AVSpeechSynthesizer>,
        val utterance: AVSpeechUtterance,
    )

    private class Pending(val synthesizer: AVSpeechSynthesizer, val utterance: AVSpeechUtterance)

    private var state = State.uninitialized
    private var textToSpeech: TextToSpeech? = null
    private val pending = mutableListOf<Pending>()
    private val spoken = HashMap<String, Spoken>()

    @Volatile
    var voices: List<AVSpeechSynthesisVoice> = emptyList()
        private set

    private val mainHandler: Handler? by lazy {
        runCatching { Looper.getMainLooper()?.let { Handler(it) } }.getOrNull()
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            val id = utteranceId ?: return
            onMain { started(id) }
        }

        override fun onDone(utteranceId: String?) {
            val id = utteranceId ?: return
            onMain { ended(id, cancelled = false) }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            val id = utteranceId ?: return
            onMain { ended(id, cancelled = true) }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            val id = utteranceId ?: return
            onMain { ended(id, cancelled = true) }
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            val id = utteranceId ?: return
            onMain { ended(id, cancelled = true) }
        }
    }

    fun prepare() {
        onMain { ensureCreated() }
    }

    fun currentVoices(): List<AVSpeechSynthesisVoice> {
        val cached = voices
        if (cached.isNotEmpty() || Looper.myLooper() == null || Looper.myLooper() != Looper.getMainLooper()) {
            return cached
        }
        val textToSpeech = textToSpeech
        if (state != State.ready || textToSpeech == null) {
            return cached
        }
        voices = readVoices(textToSpeech)
        return voices
    }

    fun speak(synthesizer: AVSpeechSynthesizer, utterance: AVSpeechUtterance) {
        if (!onMain { speakOnMain(synthesizer, utterance) }) {
            synthesizer.outstanding.decrementAndGet()
        }
    }

    fun stop(synthesizer: AVSpeechSynthesizer) {
        onMain { stopOnMain(synthesizer) }
    }

    private fun onMain(block: () -> Unit): Boolean {
        return try {
            if (Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper()) {
                block()
                true
            } else {
                mainHandler?.post(block) ?: false
            }
        } catch (error: Throwable) {
            ReaderLog.once("speechMain:${error.javaClass.name}", "AVSpeechSynthesizer: $error")
            false
        }
    }

    private fun ensureCreated() {
        if (state != State.uninitialized) {
            return
        }
        val context = try {
            AppDelegate.context
        } catch (_: Throwable) {
            return
        }
        state = State.initializing
        textToSpeech = try {
            TextToSpeech(context) { status -> onMain { initialized(status) } }
        } catch (error: Throwable) {
            ReaderLog.info("AVSpeechSynthesizer: cannot create text to speech: $error")
            state = State.uninitialized
            null
        }
    }

    private fun initialized(status: Int) {
        val textToSpeech = textToSpeech ?: return
        if (status != TextToSpeech.SUCCESS) {
            ReaderLog.info("AVSpeechSynthesizer: text to speech failed to initialize ($status)")
            runCatching { textToSpeech.shutdown() }
            this.textToSpeech = null
            state = State.uninitialized
            val failed = pending.toList()
            pending.clear()
            for (item in failed) {
                item.synthesizer.outstanding.decrementAndGet()
                item.synthesizer.delegate?.speechSynthesizerDidCancel(item.synthesizer, item.utterance)
            }
            return
        }
        state = State.ready
        textToSpeech.setOnUtteranceProgressListener(listener)
        voices = readVoices(textToSpeech)
        val queued = pending.toList()
        pending.clear()
        for (item in queued) {
            speakNow(textToSpeech, item.synthesizer, item.utterance)
        }
    }

    private fun readVoices(textToSpeech: TextToSpeech): List<AVSpeechSynthesisVoice> {
        val all = try {
            textToSpeech.voices?.toList() ?: emptyList()
        } catch (error: Throwable) {
            ReaderLog.info("AVSpeechSynthesizer: cannot list voices: $error")
            emptyList()
        }
        val defaultName = runCatching { textToSpeech.defaultVoice?.name }.getOrNull()
        return all
            .filter { !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
            .sortedWith(
                compareBy<Voice>(
                    { if (it.name == defaultName) 0 else 1 },
                    { if (it.isNetworkConnectionRequired) 1 else 0 },
                    { -it.quality },
                    { it.name },
                ),
            )
            .map { AVSpeechSynthesisVoice.make(it) }
    }

    private fun speakOnMain(synthesizer: AVSpeechSynthesizer, utterance: AVSpeechUtterance) {
        ensureCreated()
        val textToSpeech = textToSpeech
        when {
            state == State.ready && textToSpeech != null -> speakNow(textToSpeech, synthesizer, utterance)
            state == State.initializing -> pending.add(Pending(synthesizer, utterance))
            else -> {
                synthesizer.outstanding.decrementAndGet()
                synthesizer.delegate?.speechSynthesizerDidCancel(synthesizer, utterance)
            }
        }
    }

    private fun speakNow(textToSpeech: TextToSpeech, synthesizer: AVSpeechSynthesizer, utterance: AVSpeechUtterance) {
        val id = UUID.randomUUID().toString()
        val result = try {
            val requested = utterance.voice?.identifier
            val voice = textToSpeech.voices?.firstOrNull { it.name == requested } ?: textToSpeech.defaultVoice
            if (voice != null) {
                textToSpeech.setVoice(voice)
            }
            textToSpeech.setSpeechRate(max(0.1f, utterance.rate / AVSpeechUtteranceDefaultSpeechRate))
            textToSpeech.setPitch(max(0.1f, utterance.pitchMultiplier))
            val parameters = android.os.Bundle()
            parameters.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, utterance.volume.coerceIn(0f, 1f))
            spoken[id] = Spoken(WeakReference(synthesizer), utterance)
            textToSpeech.speak(utterance.string, TextToSpeech.QUEUE_ADD, parameters, id)
        } catch (error: Throwable) {
            ReaderLog.once("speak:${error.javaClass.name}", "AVSpeechSynthesizer.speak failed: $error")
            TextToSpeech.ERROR
        }
        if (result != TextToSpeech.SUCCESS) {
            spoken.remove(id)
            synthesizer.outstanding.decrementAndGet()
            synthesizer.delegate?.speechSynthesizerDidCancel(synthesizer, utterance)
        }
    }

    private fun stopOnMain(synthesizer: AVSpeechSynthesizer) {
        val cancelled = mutableListOf<AVSpeechUtterance>()
        val iterator = pending.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.synthesizer === synthesizer) {
                cancelled.add(item.utterance)
                iterator.remove()
            }
        }
        val ids = spoken.filterValues { it.synthesizer.get() === synthesizer }.keys.toList()
        if (ids.isNotEmpty()) {
            runCatching { textToSpeech?.stop() }
        }
        for (id in ids) {
            spoken.remove(id)?.let { cancelled.add(it.utterance) }
        }
        for (utterance in cancelled) {
            synthesizer.outstanding.decrementAndGet()
            synthesizer.delegate?.speechSynthesizerDidCancel(synthesizer, utterance)
        }
    }

    private fun started(id: String) {
        val item = spoken[id] ?: return
        val synthesizer = item.synthesizer.get() ?: return
        synthesizer.delegate?.speechSynthesizerDidStart(synthesizer, item.utterance)
    }

    private fun ended(id: String, cancelled: Boolean) {
        val item = spoken.remove(id) ?: return
        val synthesizer = item.synthesizer.get() ?: return
        synthesizer.outstanding.decrementAndGet()
        val delegate = synthesizer.delegate ?: return
        if (cancelled) {
            delegate.speechSynthesizerDidCancel(synthesizer, item.utterance)
        } else {
            delegate.speechSynthesizerDidFinish(synthesizer, item.utterance)
        }
    }
}
