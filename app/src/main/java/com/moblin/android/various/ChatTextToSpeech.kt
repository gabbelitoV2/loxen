package com.moblin.android.various

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.moblin.android.integrations.ttsmonster.TtsMonster
import com.moblin.android.various.settings.SettingsVoice
import com.moblin.android.various.settings.SettingsVoiceType
import com.moblin.android.various.utils.createSpeechSynthesizer
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val textToSpeechDispatchQueue: CoroutineDispatcher = Executors
    .newSingleThreadExecutor { runnable -> Thread(runnable, "com.eerimoq.textToSpeech") }
    .asCoroutineDispatcher()

private data class TextToSpeechMessage(
    val messageId: String?,
    val userId: String?,
    val user: String,
    val message: String,
    val isRedemption: Boolean,
    val isPreview: Boolean,
)

private val saysByLanguage = mapOf(
    "ar" to "يقول",
    "bg" to "казва",
    "ca" to "diu",
    "cs" to "říká",
    "da" to "siger",
    "de" to "sagt",
    "en" to "says",
    "el" to "λέει",
    "es" to "dice",
    "fi" to "sanoo",
    "fr" to "dit",
    "he" to "אומר",
    "hi" to "कहते हैं",
    "hr" to "kaže",
    "hu" to "mondja",
    "id" to "mengatakan",
    "it" to "dice",
    "ja" to "言う",
    "ko" to "라고",
    "ms" to "berkata",
    "nb" to "sier",
    "nl" to "zegt",
    "no" to "sier",
    "pl" to "mówi",
    "pt" to "diz",
    "ro" to "spune",
    "ru" to "говорит",
    "sk" to "hovorí",
    "sl" to "pravi",
    "sv" to "säger",
    "ta" to "என்கிறார்",
    "th" to "พูดว่า",
    "tr" to "diyor",
    "uk" to "каже",
    "vi" to "nói",
    "zh" to "说",
)

private sealed class Voice {
    data class Apple(val voice: android.speech.tts.Voice?) : Voice()

    data class TtsMonster(val voiceId: String) : Voice()
}

class ChatTextToSpeech {
    private var rate: Float = 0.4f
    private var volume: Float = 0.6f
    private var sayUsername: Boolean = false
    private var defaultLanguage: String? = null
    private var detectLanguagePerMessage: Boolean = false
    private var pauseBetweenMessages: Double = 0.0
    private var voices: Map<String, SettingsVoice> = emptyMap()
    private var messageQueue: ArrayDeque<TextToSpeechMessage> = ArrayDeque()
    private var synthesizer: TextToSpeech = createSpeechSynthesizer()
    private var latestUserThatSaidSomething: String? = null
    private var sayLatestUserThatSaidSomethingAgain: Long = System.nanoTime()
    private var filterEnabled: Boolean = true
    private var filterMentionsEnabled: Boolean = true
    private var streamerMentions: List<String> = emptyList()
    private var running: Boolean = true
    private var paused: Boolean = false
    private var currentlyPlayingMessage: TextToSpeechMessage? = null
    private var ttsMonster: TtsMonster? = null
    private var audioPlayer: AudioPlayer? = null
    private var isSpeaking: Boolean = false
    private val scope = CoroutineScope(textToSpeechDispatchQueue)
    private val synthesizerDelegate = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}

        override fun onDone(utteranceId: String?) {
            sayFinished()
        }

        override fun onError(utteranceId: String?) {
            sayFinished()
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {}
    }

    init {
        synthesizer.setOnUtteranceProgressListener(synthesizerDelegate)
    }

    fun say(messageId: String?, user: String, userId: String?, message: String, isRedemption: Boolean) {
        scope.launch {
            if (!running) {
                return@launch
            }
            messageQueue.addLast(
                TextToSpeechMessage(
                    messageId = messageId,
                    userId = userId,
                    user = user,
                    message = message,
                    isRedemption = isRedemption,
                    isPreview = false,
                )
            )
            trySayNextMessage()
        }
    }

    fun sayPreview(user: String, message: String) {
        scope.launch {
            if (!running) {
                return@launch
            }
            messageQueue.addLast(
                TextToSpeechMessage(
                    messageId = null,
                    userId = null,
                    user = user,
                    message = message,
                    isRedemption = false,
                    isPreview = true,
                )
            )
            trySayNextMessage()
        }
    }

    fun delete(messageId: String) {
        scope.launch {
            messageQueue = ArrayDeque(messageQueue.filter { it.messageId != messageId })
            if (currentlyPlayingMessage?.messageId == messageId) {
                skipCurrentMessageInternal()
            }
        }
    }

    fun deleteByUserId(userId: String) {
        scope.launch {
            messageQueue = ArrayDeque(messageQueue.filter { it.userId != userId })
            if (currentlyPlayingMessage?.userId == userId) {
                skipCurrentMessageInternal()
            }
        }
    }

    fun setRate(rate: Float) {
        scope.launch {
            this@ChatTextToSpeech.rate = rate
        }
    }

    fun setVolume(volume: Float) {
        scope.launch {
            this@ChatTextToSpeech.volume = volume
        }
    }

    fun setVoices(voices: Map<String, SettingsVoice>) {
        scope.launch {
            this@ChatTextToSpeech.voices = voices
        }
    }

    fun setSayUsername(value: Boolean) {
        scope.launch {
            sayUsername = value
        }
    }

    fun setFilter(value: Boolean) {
        scope.launch {
            filterEnabled = value
        }
    }

    fun setFilterMentions(value: Boolean) {
        scope.launch {
            filterMentionsEnabled = value
        }
    }

    fun setStreamerMentions(streamerMentions: List<String>) {
        scope.launch {
            this@ChatTextToSpeech.streamerMentions = streamerMentions
        }
    }

    fun setDefaultLanguage(value: String?) {
        scope.launch {
            defaultLanguage = value
        }
    }

    fun setDetectLanguagePerMessage(value: Boolean) {
        scope.launch {
            detectLanguagePerMessage = value
        }
    }

    fun setPauseBetweenMessages(value: Double) {
        scope.launch {
            pauseBetweenMessages = value
        }
    }

    fun setTtsMonsterApiToken(apiToken: String) {
        scope.launch {
            if (apiToken.isEmpty()) {
                ttsMonster = null
            } else {
                ttsMonster = TtsMonster(apiToken = apiToken)
            }
        }
    }

    fun reset(running: Boolean) {
        scope.launch {
            this@ChatTextToSpeech.running = running
            synthesizer.stop()
            audioPlayer?.stop()
            isSpeaking = false
            currentlyPlayingMessage = null
            latestUserThatSaidSomething = null
            messageQueue.clear()
            synthesizer = createSpeechSynthesizer()
            synthesizer.setOnUtteranceProgressListener(synthesizerDelegate)
        }
    }

    fun skipCurrentMessage() {
        scope.launch {
            skipCurrentMessageInternal()
        }
    }

    fun play() {
        scope.launch {
            paused = false
            trySayNextMessage()
        }
    }

    fun pause() {
        scope.launch {
            paused = true
        }
    }

    fun audioPlayerDidFinishPlaying(successfully: Boolean) {
        sayFinished()
    }

    private fun skipCurrentMessageInternal() {
        synthesizer.stop()
        synthesizer = createSpeechSynthesizer()
        synthesizer.setOnUtteranceProgressListener(synthesizerDelegate)
        audioPlayer?.stop()
        isSpeaking = false
        currentlyPlayingMessage = null
        trySayNextMessage()
    }

    private fun isFilteredOut(message: String): Boolean {
        if (isFilteredOutFilter(message = message)) {
            return true
        }
        if (isFilteredOutFilterMentions(message = message)) {
            return true
        }
        return false
    }

    private fun isFilteredOutFilter(message: String): Boolean {
        if (!filterEnabled) {
            return false
        }
        val probability = languageProbability(message)
        if (probability < 0.7 && message.length > 30) {
            return true
        }
        if (message.startsWith("!")) {
            return true
        }
        if (message.contains("https")) {
            return true
        }
        return false
    }

    private fun isFilteredOutFilterMentions(message: String): Boolean {
        if (!filterMentionsEnabled) {
            return false
        }
        if (message.startsWith("@") || message.contains(" @")) {
            for (streamerMention in streamerMentions) {
                val index = message.indexOf(streamerMention)
                if (index >= 0) {
                    if (isStreamerMention(
                            message = message,
                            mentionLowerBound = index,
                            mentionUpperBound = index + streamerMention.length
                        )
                    ) {
                        return false
                    }
                }
            }
            return true
        }
        return false
    }

    private fun isStreamerMention(message: String, mentionLowerBound: Int, mentionUpperBound: Int): Boolean {
        if (mentionUpperBound >= message.length) {
            return false
        }
        if (mentionLowerBound > 0) {
            if (message[mentionLowerBound - 1] != ' ') {
                return false
            }
        }
        if (message[mentionUpperBound] != ' ') {
            return false
        }
        return true
    }

    private fun getSays(language: String): String {
        return saysByLanguage[language] ?: ""
    }

    private fun dominantLanguage(message: String): String? {
        return TODO("no Android counterpart for NaturalLanguage NLLanguageRecognizer")
    }

    private fun languageProbability(message: String): Double {
        return TODO("no Android counterpart for NaturalLanguage NLLanguageRecognizer")
    }

    private fun getVoice(message: String): Pair<Voice?, String>? {
        if (isFilteredOut(message = message)) {
            return null
        }
        var language = dominantLanguage(message)
        if (!detectLanguagePerMessage || language == null) {
            language = defaultLanguage ?: Locale.getDefault().language
        }
        val resolvedLanguage = language ?: return null
        val voice = voices[resolvedLanguage]
        val appleVoice = voice?.apple?.voice
        if (voice?.type == SettingsVoiceType.apple && appleVoice != null) {
            val found = synthesizer.voices?.firstOrNull { it.name == appleVoice }
            return Voice.Apple(voice = found) to getSays(resolvedLanguage)
        }
        val ttsMonsterVoiceId = voice?.ttsMonster?.voiceId
        if (voice?.type == SettingsVoiceType.ttsMonster && ttsMonsterVoiceId != null) {
            return Voice.TtsMonster(voiceId = ttsMonsterVoiceId) to getSays(resolvedLanguage)
        }
        val fallbackVoice = synthesizer.voices?.firstOrNull { it.locale.language.startsWith(resolvedLanguage) }
        if (fallbackVoice != null) {
            return Voice.Apple(voice = fallbackVoice) to getSays(resolvedLanguage)
        }
        return null
    }

    private fun trySayNextMessage() {
        if (paused || isSpeaking) {
            return
        }
        val next = findNextMessageToSay() ?: return
        val message = next.first
        val voice = next.second
        val says = next.third
        currentlyPlayingMessage = message
        val now = System.nanoTime()
        val text: String
        if (message.isRedemption) {
            text = "${message.user} ${message.message}"
        } else if ((sayUsername && shouldSayUser(user = message.user, now = now)) || message.isPreview) {
            text = "${message.user} $says: ${message.message}"
        } else {
            text = message.message
        }
        isSpeaking = true
        when (voice) {
            is Voice.Apple -> utterApple(voice = voice.voice, text = text)
            is Voice.TtsMonster -> utterTtsMonster(voiceId = voice.voiceId, text = text)
        }
        latestUserThatSaidSomething = message.user
        sayLatestUserThatSaidSomethingAgain = now + 30_000_000_000L
    }

    private fun findNextMessageToSay(): Triple<TextToSpeechMessage, Voice, String>? {
        while (true) {
            val message = messageQueue.removeFirstOrNull() ?: return null
            val result = getVoice(message = message.message) ?: continue
            val voice = result.first ?: continue
            return Triple(message, voice, result.second)
        }
    }

    private fun utterApple(voice: android.speech.tts.Voice?, text: String) {
        scope.launch {
            if (pauseBetweenMessages > 0.0) {
                delay((pauseBetweenMessages * 1000.0).toLong())
            }
            if (voice != null) {
                synthesizer.voice = voice
            }
            val params = Bundle()
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
            synthesizer.setSpeechRate(rate)
            synthesizer.setPitch(0.8f)
            synthesizer.speak(text, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString())
            KeepSpeakerAlivePlayer.shared.audioPlayed()
        }
    }

    private fun utterTtsMonster(voiceId: String, text: String) {
        scope.launch {
            val data = ttsMonster?.generateTts(voiceId = voiceId, message = text)
            if (data != null) {
                audioPlayer = runCatching { AudioPlayer(data = data) }.getOrNull()
                audioPlayer?.setDelegate(delegate = this@ChatTextToSpeech)
                audioPlayer?.play()
            } else {
                sayFinished()
            }
        }
    }

    private fun shouldSayUser(user: String, now: Long): Boolean {
        if (user != latestUserThatSaidSomething) {
            return true
        }
        if (now > sayLatestUserThatSaidSomethingAgain) {
            return true
        }
        return false
    }

    private fun sayFinished() {
        scope.launch {
            isSpeaking = false
            currentlyPlayingMessage = null
            trySayNextMessage()
        }
    }
}
