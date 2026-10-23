package com.moblin.android.various

import com.moblin.android.platform.avfoundation.AVSpeechSynthesisVoice
import com.moblin.android.various.settings.SettingsVoice
import com.moblin.android.various.settings.SettingsVoiceType

sealed class TextToSpeechVoice {
    data class Apple(val voice: AVSpeechSynthesisVoice?) : TextToSpeechVoice()

    data class TtsMonster(val voiceId: String) : TextToSpeechVoice()

    data class ElevenLabs(val voiceId: String) : TextToSpeechVoice()

    companion object {
        fun find(voices: Map<String, SettingsVoice>, language: String): TextToSpeechVoice? {
            val voice = voices[language]
            if (voice != null) {
                when (voice.type) {
                    SettingsVoiceType.apple -> {
                        val voiceIdentifier = voice.apple.voice
                        if (voiceIdentifier != null) {
                            return Apple(AVSpeechSynthesisVoice(voiceIdentifier))
                        }
                    }
                    SettingsVoiceType.ttsMonster -> {
                        val voiceId = voice.ttsMonster.voiceId
                        if (voiceId != null) {
                            return TtsMonster(voiceId)
                        }
                    }
                    SettingsVoiceType.elevenLabs -> {
                        val voiceId = voice.elevenLabs.voiceId
                        if (voiceId != null) {
                            return ElevenLabs(voiceId)
                        }
                    }
                    else -> {}
                }
            }
            val matching = AVSpeechSynthesisVoice.speechVoices().firstOrNull {
                it.language.startsWith(language)
            }
            if (matching != null) {
                return Apple(AVSpeechSynthesisVoice(matching.identifier))
            }
            return null
        }
    }
}
