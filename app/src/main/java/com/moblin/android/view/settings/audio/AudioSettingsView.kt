package com.moblin.android.view.settings.audio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.localized
import com.moblin.android.various.model.Mic
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.fallbackStream
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsAudio
import com.moblin.android.various.settings.SettingsDebug
import com.moblin.android.various.settings.SettingsMics
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.TextEditNavigationView

@Composable
private fun MicView(
    model: Model,
    mics: SettingsMics,
    mic: Mic,
    onNavigate: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("QuickButtonMicView") }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Mic, contentDescription = null)
        Spacer(Modifier.width(16.dp))
        Text("Mic")
        Spacer(Modifier.weight(1f))
        GrayTextView(text = mic.current.name)
    }
}

@Composable
fun AudioSettingsView(
    model: Model,
    database: Database,
    stream: SettingsStream,
    mic: Mic,
    debug: SettingsDebug,
    audio: SettingsAudio,
    onNavigate: (String) -> Unit,
) {
    val showAllSettings by database.showAllSettings.collectAsState()
    val audioBitrate by stream.audioBitrate.collectAsState()
    val inputGain by mic.inputGain.collectAsState()
    val gainDb by audio.gainDb.collectAsState()
    val bluetoothOutputOnly by debug.bluetoothOutputOnly.collectAsState()
    val preferStereoMic by audio.preferStereoMic.collectAsState()
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()

    fun changeOutputChannel(value: String): String? {
        return if (value.toIntOrNull() != null) {
            null
        } else {
            localized("Not a number")
        }
    }

    fun submitOutputChannel1(value: String) {
        val channel = value.toIntOrNull() ?: return
        audio.outputToInputChannelsMap.channel1 = maxOf(channel - 1, -1)
        model.reloadStreamIfEnabled(stream)
    }

    fun submitOutputChannel2(value: String) {
        val channel = value.toIntOrNull() ?: return
        audio.outputToInputChannelsMap.channel2 = maxOf(channel - 1, -1)
        model.reloadStreamIfEnabled(stream)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (showAllSettings && stream !== fallbackStream) {
            ShortcutSectionView {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("StreamAudioSettingsView") }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Text("Audio")
                }
            }
        }
        MicView(model = model, mics = database.mics, mic = mic, onNavigate = onNavigate)
        if (showAllSettings) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("MicsDelaySettingsView") }
                    .padding(vertical = 12.dp),
            ) {
                Text("Delays")
            }
        }
        if (showAllSettings) {
            Text(
                "Input gain",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeDown, contentDescription = null)
                Slider(
                    value = inputGain,
                    onValueChange = {
                        mic.inputGain.value = it
                        model.setInputGainIfSupported(it)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    enabled = !(mic.current.isAudioSession() && !mic.inputGainSettable),
                    valueRange = 0.0f..1.0f,
                    steps = 9,
                )
                Icon(Icons.Default.VolumeUp, contentDescription = null)
            }
            Text(
                "Typically only supported by external mics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Output gain",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeDown, contentDescription = null)
                Slider(
                    value = gainDb,
                    onValueChange = {
                        audio.gainDb.value = it
                        model.setAudioGain(it)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    valueRange = 0.0f..24.0f,
                    steps = 23,
                )
                Icon(Icons.Default.VolumeUp, contentDescription = null)
                Text(
                    "${formatOneDecimal(gainDb)} dB",
                    modifier = Modifier.width(65.dp),
                )
            }
            Text(
                "0.0 dB by default, leaving the input level unchanged.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Bluetooth output only", modifier = Modifier.weight(1f))
            Switch(
                checked = bluetoothOutputOnly,
                onCheckedChange = {
                    debug.bluetoothOutputOnly.value = it
                    model.reloadAudioSession()
                },
            )
        }
        Text(
            "Makes most Bluetooth speakers work better.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showAllSettings) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Prefer stereo mic", modifier = Modifier.weight(1f))
                Switch(
                    checked = preferStereoMic,
                    onCheckedChange = {
                        audio.preferStereoMic.value = it
                        if (mic.current.isAudioSession()) {
                            model.reloadAudioSession()
                            model.selectMicDefault(mic.current)
                        }
                    },
                )
            }
            Column {
                Text(
                    "Only works when front or back mic is selected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("")
                Text(
                    "Switching between mono and stereo mics may not work.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "Input to output channel mapping",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            TextEditNavigationView(
                title = localized("Output channel 1"),
                value = (audio.outputToInputChannelsMap.channel1 + 1).toString(),
                onChange = { changeOutputChannel(it) },
                onSubmit = { submitOutputChannel1(it) },
                enabled = !(isLive || isRecording),
            )
            TextEditNavigationView(
                title = localized("Output channel 2"),
                value = (audio.outputToInputChannelsMap.channel2 + 1).toString(),
                onChange = { changeOutputChannel(it) },
                onSubmit = { submitOutputChannel2(it) },
                enabled = !(isLive || isRecording),
            )
            Text(
                "Mono audio only uses output channel 1. Stereo audio uses both output channels.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
