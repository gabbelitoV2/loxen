package com.moblin.android.view.settings.chat

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.view.settings.streams.stream.TtsMonsterLogoAndNameView
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.various.settings.SettingsTtsMonster
import com.moblin.android.various.settings.SettingsVoice
import java.util.Locale
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
fun TtsMonsterSettingsView(ttsMonster: SettingsTtsMonster, onNavigate: (String) -> Unit = LocalOnNavigate.current) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("tts_monster") },
        verticalAlignment = Alignment.CenterVertically,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTextToSpeechSettingsView(
    model: Model = LocalModel.current,
    chat: SettingsChat,
    ttsMonster: SettingsTtsMonster,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val context = LocalContext.current
    var appleVoices by remember { mutableStateOf(emptyList<Voice>()) }
    var languageMenuExpanded by remember { mutableStateOf(false) }

    val sayVolume by chat.textToSpeechSayVolume.collectAsState()
    val rate by chat.textToSpeechRate.collectAsState()
    val pauseBetweenMessages by chat.textToSpeechPauseBetweenMessages.collectAsState()
    val defaultLanguage by chat.textToSpeechDefaultLanguage.collectAsState()
    val detectLanguagePerMessage by chat.textToSpeechDetectLanguagePerMessage.collectAsState()
    val sayUsername by chat.textToSpeechSayUsername.collectAsState()
    val subscribersOnly by chat.textToSpeechSubscribersOnly.collectAsState()
    val filter by chat.textToSpeechFilter.collectAsState()
    val filterMentions by chat.textToSpeechFilterMentions.collectAsState()
    val ttsMonsterApiToken by ttsMonster.apiToken.collectAsState()

    val languages = textToSpeechLanguages(appleVoices)
    val defaultLanguageName = defaultLanguage?.let { code ->
        languages.firstOrNull { it.code == code }?.name
    } ?: "App language"

    val onVoiceChange: (String, SettingsVoice) -> Unit = { languageCode, voice ->
        chat.textToSpeechLanguageVoices.value =
            chat.textToSpeechLanguageVoices.value + (languageCode to voice)
        model.chatTextToSpeech.setVoices(chat.textToSpeechLanguageVoices.value)
    }
    val onLanguageReset: (String) -> Unit = { languageCode ->
        chat.textToSpeechLanguageVoices.value =
            chat.textToSpeechLanguageVoices.value - languageCode
        model.chatTextToSpeech.setVoices(chat.textToSpeechLanguageVoices.value)
    }

    LaunchedEffect(Unit) {
        val textToSpeech = TextToSpeech(context) { }
        appleVoices = textToSpeech.voices?.toList() ?: emptyList()
        textToSpeech.shutdown()
        TODO("no Android counterpart for AVSpeechSynthesizer.requestPersonalVoiceAuthorization")
    }

    LaunchedEffect(ttsMonsterApiToken) {
        model.chatTextToSpeech.setTtsMonsterApiToken(ttsMonsterApiToken)
    }
    LaunchedEffect(defaultLanguage) {
        model.chatTextToSpeech.setDefaultLanguage(defaultLanguage)
    }
    LaunchedEffect(detectLanguagePerMessage) {
        model.chatTextToSpeech.setDetectLanguagePerMessage(detectLanguagePerMessage)
    }
    LaunchedEffect(sayUsername) {
        model.chatTextToSpeech.setSayUsername(sayUsername)
    }
    LaunchedEffect(filter) {
        model.chatTextToSpeech.setFilter(filter)
    }
    LaunchedEffect(filterMentions) {
        model.chatTextToSpeech.setFilterMentions(filterMentions)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Voice", style = MaterialTheme.typography.titleSmall)
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("voices") },
            ) {
                Text("Voices")
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeDown, contentDescription = null)
                Slider(
                    value = sayVolume,
                    onValueChange = { chat.textToSpeechSayVolume.value = it },
                    modifier = Modifier.weight(1f),
                    valueRange = 0.3f..1.0f,
                    steps = 69,
                    onValueChangeFinished = {
                        model.chatTextToSpeech.setVolume(chat.textToSpeechSayVolume.value)
                    },
                )
                Icon(Icons.Default.VolumeUp, contentDescription = null)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsWalk, contentDescription = null)
                Slider(
                    value = rate,
                    onValueChange = { chat.textToSpeechRate.value = it },
                    modifier = Modifier.weight(1f),
                    valueRange = 0.3f..0.6f,
                    steps = 29,
                    onValueChangeFinished = {
                        model.chatTextToSpeech.setRate(chat.textToSpeechRate.value)
                    },
                )
                Icon(Icons.Default.DirectionsRun, contentDescription = null)
            }
        }
        item {
            TtsMonsterSettingsView(ttsMonster = chat.ttsMonster, onNavigate = onNavigate)
        }
        item {
            Text("Pause between messages", style = MaterialTheme.typography.titleSmall)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = pauseBetweenMessages.toFloat(),
                    onValueChange = { chat.textToSpeechPauseBetweenMessages.value = it.toDouble() },
                    modifier = Modifier.weight(1f),
                    valueRange = 0.5f..15.0f,
                    steps = 28,
                    onValueChangeFinished = {
                        model.chatTextToSpeech.setPauseBetweenMessages(
                            chat.textToSpeechPauseBetweenMessages.value,
                        )
                    },
                )
                Text(
                    text = "${formatOneDecimal(pauseBetweenMessages.toFloat())} s",
                    modifier = Modifier.width(45.dp),
                )
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Default language", modifier = Modifier.weight(1f))
                ExposedDropdownMenuBox(
                    expanded = languageMenuExpanded,
                    onExpandedChange = { languageMenuExpanded = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = defaultLanguageName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = languageMenuExpanded,
                        onDismissRequest = { languageMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("App language") },
                            onClick = {
                                chat.textToSpeechDefaultLanguage.value = null
                                languageMenuExpanded = false
                            },
                        )
                        languages.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(language.name) },
                                onClick = {
                                    chat.textToSpeechDefaultLanguage.value = language.code
                                    languageMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Detect language per message", modifier = Modifier.weight(1f))
                Switch(
                    checked = detectLanguagePerMessage,
                    onCheckedChange = { chat.textToSpeechDetectLanguagePerMessage.value = it },
                )
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Say username", modifier = Modifier.weight(1f))
                Switch(
                    checked = sayUsername,
                    onCheckedChange = { chat.textToSpeechSayUsername.value = it },
                )
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Subscribers only", modifier = Modifier.weight(1f))
                Switch(
                    checked = subscribersOnly,
                    onCheckedChange = { chat.textToSpeechSubscribersOnly.value = it },
                )
            }
        }
        item {
            Text(
                "Subscribers only is not available for all platforms.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Filter", modifier = Modifier.weight(1f))
                Switch(
                    checked = filter,
                    onCheckedChange = { chat.textToSpeechFilter.value = it },
                )
            }
        }
        item {
            Text(
                "Do not say messages that are likely spam or bot commands.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Filter mentions", modifier = Modifier.weight(1f))
                Switch(
                    checked = filterMentions,
                    onCheckedChange = { chat.textToSpeechFilterMentions.value = it },
                )
            }
        }
        item {
            Text(
                "Do not say messages that contains mentions, except when you are mentioned.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
