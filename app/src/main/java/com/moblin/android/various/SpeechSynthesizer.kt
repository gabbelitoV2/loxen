package com.moblin.android.various

import com.moblin.android.platform.avfoundation.AVSpeechBoundary
import com.moblin.android.platform.avfoundation.AVSpeechSynthesizer
import com.moblin.android.platform.avfoundation.AVSpeechSynthesizerDelegate
import com.moblin.android.platform.avfoundation.AVSpeechUtterance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SpeechSynthesizer(private val queue: CoroutineScope) : AVSpeechSynthesizerDelegate {
    private val synthesizer = AVSpeechSynthesizer()
    private var current: AVSpeechUtterance? = null
    private var onFinished: (() -> Unit)? = null

    init {
        synthesizer.usesApplicationAudioSession = false
        synthesizer.delegate = this
    }

    fun speak(utterance: AVSpeechUtterance, volume: Float, onFinished: (() -> Unit)? = null) {
        stop()
        current = utterance
        this.onFinished = onFinished
        utterance.volume = volume
        synthesizer.speak(utterance)
    }

    fun stop() {
        current = null
        onFinished = null
        synthesizer.stopSpeaking(at = AVSpeechBoundary.immediate)
    }

    private fun finished(utterance: AVSpeechUtterance) {
        if (utterance !== current) {
            return
        }
        current = null
        val onFinished = onFinished
        this.onFinished = null
        onFinished?.invoke()
    }

    override fun speechSynthesizerDidStart(synthesizer: AVSpeechSynthesizer, utterance: AVSpeechUtterance) {}

    override fun speechSynthesizerDidFinish(synthesizer: AVSpeechSynthesizer, utterance: AVSpeechUtterance) {
        queue.launch {
            finished(utterance)
        }
    }

    override fun speechSynthesizerDidCancel(synthesizer: AVSpeechSynthesizer, utterance: AVSpeechUtterance) {
        queue.launch {
            finished(utterance)
        }
    }
}
