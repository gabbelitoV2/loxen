package com.moblin.android.view.utils

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalOnNavigate
import com.moblin.android.integrations.ttsmonster.TtsMonster
import com.moblin.android.integrations.ttsmonster.TtsMonsterVoicesResponse
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.AudioPlayer
import com.moblin.android.various.KeepSpeakerAlivePlayer
import com.moblin.android.various.settings.SettingsVoice
import com.moblin.android.various.settings.SettingsVoiceType
import com.moblin.android.various.utils.createSpeechSynthesizer
import com.moblin.android.various.utils.emojiFlag
import com.moblin.android.view.settings.chat.textToSpeechLanguages
import com.moblin.android.view.settings.chat.textToSpeechLocalize
import kotlinx.coroutines.launch

private fun getVoice(
    appleVoices: List<android.speech.tts.Voice>,
    languageCode: String,
    identifier: String,
): android.speech.tts.Voice? {
    return appleVoices.firstOrNull {
        it.locale.language == languageCode && it.name == identifier
    }
}

private val testMessageByLanguage: Map<String, String> = mapOf(
    "ar" to "هذا ما يتحدث عنه جمهورك! استمع بعناية!",
    "bg" to "Това говорят вашите зрители! Слушайте внимателно!",
    "ca" to "Això són els teus espectadors! Escolta atentament!",
    "cs" to "Tohle mluví vaši diváci! Poslouchejte pozorně!",
    "da" to "Det er dine seere, der taler! Lyt godt efter!",
    "de" to "Hier sprechen Ihre Zuschauer! Hören Sie gut zu!",
    "en" to "This is your chat speaking! Listen carefully!",
    "el" to "Αυτοί είναι οι θεατές σας που μιλάνε! Ακούστε προσεκτικά!",
    "es" to "¡Les habla la audiencia! ¡Escuchen atentamente!",
    "fi" to "Katsojasi puhuvat! Kuuntele tarkkaan!",
    "fr" to "Ce sont vos spectateurs qui parlent! Écoutez attentivement!",
    "he" to "אלו הצופים שלכם שמדברים! תקשיבו היטב!",
    "hi" to "ये आपके दर्शक बोल रहे हैं! ध्यान से सुनिए!",
    "hr" to "Ovo govore vaši gledatelji! Slušajte pažljivo!",
    "hu" to "Itt a nézőid beszélnek! Figyeljetek jól!",
    "id" to "Ini pemirsa Anda yang berbicara! Dengarkan baik-baik!",
    "it" to "Sono i tuoi spettatori a parlare! Ascolta attentamente!",
    "ja" to "視聴者の声です！よく聞いてください！",
    "ko" to "시청자 여러분의 이야기입니다! 잘 들어보세요!",
    "ms" to "Ini adalah penonton anda bercakap! Dengar baik-baik!",
    "nb" to "Dette er seerne dine som snakker! Lytt nøye!",
    "nl" to "Dit zijn je kijkers die praten! Luister goed!",
    "no" to "Dette er seerne dine som snakker! Lytt nøye!",
    "pl" to "To mówią Twoi widzowie! Słuchaj uważnie!",
    "pt" to "Quem fala aqui são os seus espectadores! Ouçam com atenção!",
    "ro" to "Aici vorbesc spectatorii tăi! Ascultați cu atenție!",
    "ru" to "Это говорят ваши зрители! Слушайте внимательно!",
    "sk" to "Toto hovoria vaši diváci! Počúvajte pozorne!",
    "sl" to "To govorijo vaši gledalci! Poslušajte pozorno!",
    "sv" to "Det här är dina tittare som pratar! Lyssna noga!",
    "ta" to "இது உங்கள் பார்வையாளர்கள் பேசுவது! கவனமாகக் கேளுங்கள்!",
    "th" to "นี่คือเสียงผู้ชมของคุณ! ฟังอย่างตั้งใจ!",
    "tr" to "İzleyicileriniz konuşuyor! Dikkatlice dinleyin!",
    "uk" to "Це говорять ваші глядачі! Слухайте уважно!",
    "vi" to "Đây là lời của khán giả! Hãy lắng nghe thật kỹ!",
    "zh" to "這是觀眾的發言！仔細聽！",
)

private fun getTestMessage(languageCode: String): String {
    return testMessageByLanguage[languageCode] ?: ""
}

private sealed class Voice {
    data class Apple(val name: String, val identifier: String) : Voice()

    data class TtsMonster(val name: String, val voiceId: String) : Voice()

    companion object {
        fun fromSettings(voice: SettingsVoice): Voice {
            return when (voice.type) {
                SettingsVoiceType.apple -> Apple(name = "", identifier = voice.apple.voice)
                SettingsVoiceType.ttsMonster -> TtsMonster(
                    name = voice.ttsMonster.name,
                    voiceId = voice.ttsMonster.voiceId,
                )
            }
        }
    }
}

private data class VoicePickerItem(
    val flagEmoji: String,
    val voice: Voice,
) {
    val id: String
        get() = when (val voice = voice) {
            is Voice.Apple -> "apple:${voice.identifier}"
            is Voice.TtsMonster -> "tts-monster:${voice.voiceId}"
        }

    fun toSettings(): SettingsVoice {
        val settings = SettingsVoice()
        when (val voice = voice) {
            is Voice.Apple -> {
                settings.type = SettingsVoiceType.apple
                settings.apple.voice = voice.identifier
            }
            is Voice.TtsMonster -> {
                settings.type = SettingsVoiceType.ttsMonster
                settings.ttsMonster.name = voice.name
                settings.ttsMonster.voiceId = voice.voiceId
            }
        }
        return settings
    }
}

@Composable
private fun TtsMonsterLogo(modifier: Modifier = Modifier) {
    val bitmap = remember { com.moblin.android.platform.Bundle.image("TtsMonster")?.asImageBitmap() }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.width(15.dp))
    }
}

@Composable
private fun PlayButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .alpha(if (pressed) 0.2f else 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun VoiceView(
    voiceItem: VoicePickerItem,
    appleVoices: List<android.speech.tts.Voice>,
    languageCode: String,
    synthesizer: TextToSpeech,
    rate: Float,
    volume: Float,
    ttsMonsterApiToken: String,
    modifier: Modifier = Modifier,
) {
    var audioPlayer by remember { mutableStateOf<AudioPlayer?>(null) }
    var fetching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val palette = formPalette()

    fun playAppleTestMessage(languageCode: String, identifier: String) {
        synthesizer.setSpeechRate(rate)
        synthesizer.setPitch(0.8f)
        val params = Bundle()
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
        val voice = getVoice(appleVoices, languageCode, identifier)
        if (voice != null) {
            synthesizer.setVoice(voice)
        }
        synthesizer.speak(
            getTestMessage(languageCode),
            TextToSpeech.QUEUE_FLUSH,
            params,
            identifier,
        )
        KeepSpeakerAlivePlayer.shared.audioPlayed()
    }

    fun playTtsMonsterTestMessage(voiceId: String) {
        if (ttsMonsterApiToken.isEmpty()) {
            return
        }
        scope.launch {
            fetching = true
            try {
                val ttsMonster = TtsMonster(apiToken = ttsMonsterApiToken)
                val message = getTestMessage(languageCode)
                val data = ttsMonster.generateTts(voiceId = voiceId, message = message) ?: return@launch
                audioPlayer = runCatching { AudioPlayer(data) }.getOrNull()
                audioPlayer?.play()
            } finally {
                fetching = false
            }
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (val voice = voiceItem.voice) {
            is Voice.Apple -> {
                SystemImage(
                    name = "apple.logo",
                    fontSize = 15.sp,
                    modifier = Modifier.width(15.dp),
                    tint = palette.label,
                )
                Text("${voiceItem.flagEmoji} ${voice.name}")
                Spacer(Modifier.weight(1f))
                PlayButton(
                    onClick = {
                        playAppleTestMessage(languageCode = languageCode, identifier = voice.identifier)
                    },
                ) {
                    SystemImage(name = "play.fill", fontSize = 17.sp, tint = palette.accent)
                }
            }
            is Voice.TtsMonster -> {
                TtsMonsterLogo()
                Text("${voiceItem.flagEmoji} ${voice.name}")
                Spacer(Modifier.weight(1f))
                PlayButton(
                    onClick = {
                        playTtsMonsterTestMessage(voiceId = voice.voiceId)
                    },
                ) {
                    if (fetching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = palette.gray,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        SystemImage(name = "play.fill", fontSize = 17.sp, tint = palette.accent)
                    }
                }
            }
        }
    }
}

private fun voices(
    appleVoices: List<android.speech.tts.Voice>,
    ttsMonsterVoices: TtsMonsterVoicesResponse?,
    languageCode: String,
): List<VoicePickerItem> {
    val voices = mutableListOf<VoicePickerItem>()
    for (voice in appleVoices) {
        if (voice.locale.language != languageCode) {
            continue
        }
        val country = voice.locale.country
        val flagEmoji = emojiFlag(countryCode = country.ifEmpty { null })
        voices.add(
            VoicePickerItem(
                flagEmoji = flagEmoji,
                voice = Voice.Apple(name = voice.name, identifier = voice.name),
            ),
        )
    }
    for (voice in ttsMonsterVoices?.allVoices() ?: emptyList()) {
        if (voice.languageCode() != languageCode) {
            continue
        }
        val flagEmoji = emojiFlag(countryCode = voice.countryCode())
        voices.add(
            VoicePickerItem(
                flagEmoji = flagEmoji,
                voice = Voice.TtsMonster(name = voice.name, voiceId = voice.voice_id),
            ),
        )
    }
    return voices
}

@Composable
private fun LanguageView(
    appleVoices: List<android.speech.tts.Voice>,
    ttsMonsterVoices: TtsMonsterVoicesResponse?,
    languageCode: String,
    initialSelectedVoice: VoicePickerItem?,
    onVoiceChange: (String, SettingsVoice) -> Unit,
    onLanguageReset: (String) -> Unit,
    synthesizer: TextToSpeech,
    rate: Float,
    volume: Float,
    ttsMonsterApiToken: String,
) {
    var selectedVoice by remember { mutableStateOf(initialSelectedVoice) }
    val palette = formPalette()

    Form(title = textToSpeechLocalize(languageCode)) {
        Section {
            voices(
                appleVoices = appleVoices,
                ttsMonsterVoices = ttsMonsterVoices,
                languageCode = languageCode,
            ).forEach { voiceItem ->
                key(voiceItem.id) {
                    FormRow(
                        onClick = {
                            selectedVoice = voiceItem
                            onVoiceChange(languageCode, voiceItem.toSettings())
                        },
                    ) {
                        VoiceView(
                            voiceItem = voiceItem,
                            appleVoices = appleVoices,
                            languageCode = languageCode,
                            synthesizer = synthesizer,
                            rate = rate,
                            volume = volume,
                            ttsMonsterApiToken = ttsMonsterApiToken,
                            modifier = Modifier.weight(1f),
                        )
                        if (selectedVoice?.id == voiceItem.id) {
                            SystemImage(name = "checkmark", fontSize = 17.sp, tint = palette.accent)
                        }
                    }
                }
            }
        }
        Section {
            Text(
                localized(
                    "Download enhanced and premium voices in iOS Settings → Accessibility → " +
                        "Live Speech → Preferred Voices.",
                ),
            )
        }
        Section {
            TextButtonView("Reset") {
                selectedVoice = null
                onLanguageReset(languageCode)
            }
        }
    }
}

private data class Language(
    val name: String,
    val code: String,
    val selectedVoice: SettingsVoice?,
)

private fun languages(
    textToSpeechLanguageVoices: Map<String, SettingsVoice>,
    appleVoices: List<android.speech.tts.Voice>,
): List<Language> {
    return textToSpeechLanguages(appleVoices).map {
        Language(
            name = it.name,
            code = it.code,
            selectedVoice = textToSpeechLanguageVoices[it.code],
        )
    }
}

private fun selectedVoice(language: Language): VoicePickerItem? {
    val selectedVoice = language.selectedVoice ?: return null
    return VoicePickerItem(
        flagEmoji = emojiFlag(countryCode = language.code),
        voice = Voice.fromSettings(selectedVoice),
    )
}

@Composable
fun VoicesView(
    textToSpeechLanguageVoices: Map<String, SettingsVoice>,
    onVoiceChange: (String, SettingsVoice) -> Unit,
    onLanguageReset: (String) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
    rate: Float,
    volume: Float,
    ttsMonsterApiToken: String,
) {
    val synthesizer = remember { createSpeechSynthesizer() }
    var appleVoices by remember { mutableStateOf<List<android.speech.tts.Voice>>(emptyList()) }
    var ttsMonsterVoices by remember { mutableStateOf<TtsMonsterVoicesResponse?>(null) }
    val palette = formPalette()

    Form(title = "Voices") {
        languages(
            textToSpeechLanguageVoices = textToSpeechLanguageVoices,
            appleVoices = appleVoices,
        ).forEach { language ->
            key(language.code) {
                NavigationLink(
                    destination = {
                        LanguageView(
                            appleVoices = appleVoices,
                            ttsMonsterVoices = ttsMonsterVoices,
                            languageCode = language.code,
                            initialSelectedVoice = selectedVoice(language),
                            onVoiceChange = onVoiceChange,
                            onLanguageReset = onLanguageReset,
                            synthesizer = synthesizer,
                            rate = rate,
                            volume = volume,
                            ttsMonsterApiToken = ttsMonsterApiToken,
                        )
                    },
                ) {
                    Text(language.name)
                    Spacer(Modifier.weight(1f))
                    val voice = language.selectedVoice
                    if (voice != null) {
                        when (voice.type) {
                            SettingsVoiceType.apple -> {
                                SystemImage(
                                    name = "apple.logo",
                                    fontSize = 15.sp,
                                    modifier = Modifier.width(15.dp),
                                    tint = palette.label,
                                )
                                Text(
                                    getVoice(
                                        appleVoices = appleVoices,
                                        languageCode = language.code,
                                        identifier = voice.apple.voice,
                                    )?.name ?: localized("Unknown"),
                                )
                            }
                            SettingsVoiceType.ttsMonster -> {
                                TtsMonsterLogo()
                                Text(voice.ttsMonster.name)
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        appleVoices = synthesizer.voices?.toList() ?: emptyList()
    }

    LaunchedEffect(Unit) {
        if (ttsMonsterVoices == null && ttsMonsterApiToken.isNotEmpty()) {
            val ttsMonster = TtsMonster(apiToken = ttsMonsterApiToken)
            ttsMonsterVoices = ttsMonster.getVoices()
        }
    }
}
