package com.moblin.android.platform.avfoundation

import java.lang.ref.WeakReference
import java.util.Locale

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
            ReaderLog.notImplemented("AVSpeechSynthesisVoice.speechVoices")
            return emptyList()
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

    var delegate: AVSpeechSynthesizerDelegate?
        get() = delegateReference?.get()
        set(value) {
            delegateReference = value?.let { WeakReference(it) }
        }

    var usesApplicationAudioSession: Boolean = true

    val isSpeaking: Boolean
        get() = false

    val isPaused: Boolean
        get() = false

    fun speak(utterance: AVSpeechUtterance) {
        ReaderLog.notImplemented("AVSpeechSynthesizer.speak")
    }

    fun stopSpeaking(at: AVSpeechBoundary): Boolean {
        return false
    }

    fun pauseSpeaking(at: AVSpeechBoundary): Boolean {
        return false
    }

    fun continueSpeaking(): Boolean {
        return false
    }
}
