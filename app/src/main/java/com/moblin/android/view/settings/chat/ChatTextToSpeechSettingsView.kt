package com.moblin.android.view.settings.chat

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.view.settings.streams.stream.TtsMonsterLogoAndNameView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.VoicesView
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.various.settings.SettingsTtsMonster
import com.moblin.android.various.settings.SettingsVoice
import java.util.Locale
import com.moblin.android.localized

@Composable
fun TtsMonsterSettingsView(
    ttsMonster: SettingsTtsMonster,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            Form(title = "TTS.Monster") {
                Section {
                    TextEditNavigationView(
                        title = localized("API token"),
                        value = ttsMonster.apiToken,
                        onSubmit = { value ->
                            ttsMonster.apiToken = value
                        },
                        sensitive = true,
                    )
                }
            }
        },
    ) {
        TtsMonsterLogoAndNameView()
    }
}

fun textToSpeechLocalize(languageCode: String): String {
    return Locale.forLanguageTag(languageCode).getDisplayLanguage(Locale.getDefault())
        .ifEmpty { languageCode }
}

data class TextToSpeechLanguage(
    val name: String,
    val code: String,
)

fun textToSpeechLanguages(appleVoices: List<Voice>): List<TextToSpeechLanguage> {
    val languages = mutableListOf<TextToSpeechLanguage>()
    val seen = mutableSetOf<String>()
    for (voice in appleVoices) {
        val languageTag = voice.locale.toLanguageTag()
        val code = languageTag.take(2)
        if (seen.contains(code)) {
            continue
        }
        languages.add(TextToSpeechLanguage(name = textToSpeechLocalize(languageTag), code = code))
        seen.add(code)
    }
    return languages
}

@Composable
fun ChatTextToSpeechSettingsView(
    model: Model = LocalModel.current,
    chat: SettingsChat,
    ttsMonster: SettingsTtsMonster,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val context = LocalContext.current
    var appleVoices by remember { mutableStateOf(emptyList<Voice>()) }

    val languages = textToSpeechLanguages(appleVoices)
    val appLanguageLabel = localized("App language")

    val onVoiceChange: (String, SettingsVoice) -> Unit = { languageCode, voice ->
        chat.textToSpeechLanguageVoices =
            (chat.textToSpeechLanguageVoices + (languageCode to voice)).toMutableMap()
        model.chatTextToSpeech.setVoices(chat.textToSpeechLanguageVoices)
    }
    val onLanguageReset: (String) -> Unit = { languageCode ->
        chat.textToSpeechLanguageVoices =
            (chat.textToSpeechLanguageVoices - languageCode).toMutableMap()
        model.chatTextToSpeech.setVoices(chat.textToSpeechLanguageVoices)
    }

    LaunchedEffect(Unit) {
        val textToSpeech = TextToSpeech(context) { }
        appleVoices = textToSpeech.voices?.toList() ?: emptyList()
        textToSpeech.shutdown()
        onVoiceChange
        onLanguageReset
    }

    LaunchedEffect(ttsMonster.apiToken) {
        model.chatTextToSpeech.setTtsMonsterApiToken(ttsMonster.apiToken)
    }

    Form(title = localized("Text to speech")) {
        Section(header = localized("Voice")) {
            NavigationLink(
                destination = {
                    VoicesView(
                        textToSpeechLanguageVoices = chat.textToSpeechLanguageVoices,
                        onVoiceChange = onVoiceChange,
                        onLanguageReset = onLanguageReset,
                        rate = chat.textToSpeechRate,
                        volume = chat.textToSpeechSayVolume,
                        ttsMonsterApiToken = ttsMonster.apiToken,
                    )
                },
            ) {
                Text(localized("Voices"))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SystemImage("volume.1.fill", fontSize = 17.sp)
                FormSlider(
                    value = chat.textToSpeechSayVolume,
                    onValueChange = { chat.textToSpeechSayVolume = it },
                    modifier = Modifier.weight(1f),
                    valueRange = 0.3f..1.0f,
                    onValueChangeFinished = {
                        model.chatTextToSpeech.setVolume(chat.textToSpeechSayVolume)
                    },
                )
                SystemImage("volume.3.fill", fontSize = 17.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SystemImage("tortoise.fill", fontSize = 17.sp)
                FormSlider(
                    value = chat.textToSpeechRate,
                    onValueChange = { chat.textToSpeechRate = it },
                    modifier = Modifier.weight(1f),
                    valueRange = 0.3f..0.6f,
                    onValueChangeFinished = {
                        model.chatTextToSpeech.setRate(chat.textToSpeechRate)
                    },
                )
                SystemImage("hare.fill", fontSize = 17.sp)
            }
            TtsMonsterSettingsView(ttsMonster = chat.ttsMonster, onNavigate = onNavigate)
        }
        Section(header = localized("Pause between messages")) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FormSlider(
                    value = chat.textToSpeechPauseBetweenMessages.toFloat(),
                    onValueChange = { chat.textToSpeechPauseBetweenMessages = it.toDouble() },
                    modifier = Modifier.weight(1f),
                    valueRange = 0.5f..15.0f,
                    onValueChangeFinished = {
                        model.chatTextToSpeech.setPauseBetweenMessages(
                            chat.textToSpeechPauseBetweenMessages,
                        )
                    },
                )
                Box(modifier = Modifier.width(45.dp), contentAlignment = Alignment.Center) {
                    Text("${formatOneDecimal(chat.textToSpeechPauseBetweenMessages.toFloat())} s")
                }
            }
        }
        Section(footer = localized("Subscribers only is not available for all platforms.")) {
            Picker(
                title = localized("Default language"),
                selection = chat.textToSpeechDefaultLanguage,
                options = listOf<String?>(null) + languages.map { it.code },
                text = { code ->
                    if (code == null) {
                        appLanguageLabel
                    } else {
                        languages.firstOrNull { it.code == code }?.name ?: code
                    }
                },
                onChange = { value ->
                    chat.textToSpeechDefaultLanguage = value
                    model.chatTextToSpeech.setDefaultLanguage(value)
                },
            )
            Toggle(
                localized("Detect language per message"),
                isOn = chat.textToSpeechDetectLanguagePerMessage,
                onChange = { value ->
                    chat.textToSpeechDetectLanguagePerMessage = value
                    model.chatTextToSpeech.setDetectLanguagePerMessage(value)
                },
            )
            Toggle(
                localized("Say username"),
                isOn = chat.textToSpeechSayUsername,
                onChange = { value ->
                    chat.textToSpeechSayUsername = value
                    model.chatTextToSpeech.setSayUsername(value)
                },
            )
            Toggle(
                localized("Subscribers only"),
                isOn = chat.textToSpeechSubscribersOnly,
                onChange = { value ->
                    chat.textToSpeechSubscribersOnly = value
                },
            )
        }
        Section(footer = localized("Do not say messages that are likely spam or bot commands.")) {
            Toggle(
                localized("Filter"),
                isOn = chat.textToSpeechFilter,
                onChange = { value ->
                    chat.textToSpeechFilter = value
                    model.chatTextToSpeech.setFilter(value)
                },
            )
        }
        Section(
            footer = localized(
                "Do not say messages that contains mentions, except when you are mentioned.",
            ),
        ) {
            Toggle(
                localized("Filter mentions"),
                isOn = chat.textToSpeechFilterMentions,
                onChange = { value ->
                    chat.textToSpeechFilterMentions = value
                    model.chatTextToSpeech.setFilterMentions(value)
                },
            )
        }
    }
}
