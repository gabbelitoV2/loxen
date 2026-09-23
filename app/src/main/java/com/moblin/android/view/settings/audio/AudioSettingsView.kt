package com.moblin.android.view.settings.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.common.various.iconWidth
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Mic
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.fallbackStream
import com.moblin.android.various.model.reloadAudioSession
import com.moblin.android.various.model.reloadStreamIfEnabled
import com.moblin.android.various.model.selectMicDefault
import com.moblin.android.various.model.setAudioGain
import com.moblin.android.various.model.setInputGainIfSupported
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsAudio
import com.moblin.android.various.settings.SettingsDebug
import com.moblin.android.various.settings.SettingsMics
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.controlbar.quickbutton.QuickButtonMicView
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.settings.streams.stream.audio.StreamAudioSettingsView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.TextEditNavigationView

@Composable
private fun MicView(
    model: Model = LocalModel.current,
    mics: SettingsMics,
    mic: Mic,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val currentMic by mic.current.collectAsState()
    NavigationLink(
        destination = {
            QuickButtonMicView(model = model, mics = mics, modelMic = mic)
        },
    ) {
        SystemImage(name = "music.mic", fontSize = 17.sp, modifier = Modifier.width(iconWidth.dp))
        Text(localized("Mic"))
        Spacer(modifier = Modifier.weight(1f))
        GrayTextView(text = currentMic.name)
    }
}

@Composable
fun AudioSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    stream: SettingsStream,
    mic: Mic,
    debug: SettingsDebug,
    audio: SettingsAudio,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val showAllSettings = database.showAllSettings
    val inputGain by mic.inputGain.collectAsState()
    val gainDb by audio.gainDb.collectAsState()
    val bluetoothOutputOnly by debug.bluetoothOutputOnly.collectAsState()
    val preferStereoMic by audio.preferStereoMic.collectAsState()
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    val currentMic by mic.current.collectAsState()
    val inputGainSettable by mic.inputGainSettable.collectAsState()

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

    Form(title = "Audio") {
        if (showAllSettings && stream !== fallbackStream) {
            ShortcutSectionView {
                NavigationLink(
                    destination = {
                        StreamAudioSettingsView(
                            stream = stream,
                        )
                    },
                ) {
                    Label("Audio", systemImage = "dot.radiowaves.left.and.right")
                }
            }
        }
        Section {
            MicView(
                model = model,
                mics = database.mics,
                mic = mic,
                onNavigate = onNavigate,
            )
            if (showAllSettings) {
                NavigationLink("Delays") {
                    MicsDelaySettingsView(model = model, mics = database.mics)
                }
            }
        }
        if (showAllSettings) {
            Section(
                header = "Input gain",
                footer = "Typically only supported by external mics.",
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SystemImage(name = "speaker.fill", fontSize = 17.sp)
                    FormSlider(
                        value = inputGain,
                        onValueChange = {
                            mic.inputGain.value = it
                            model.setInputGainIfSupported(inputGain = it)
                        },
                        modifier = Modifier.weight(1f),
                        valueRange = 0.0f..1.0f,
                        enabled = !(currentMic.isAudioSession() && !inputGainSettable),
                    )
                    SystemImage(name = "speaker.wave.3.fill", fontSize = 17.sp)
                }
            }
            Section(
                header = "Output gain",
                footer = "0.0 dB by default, leaving the input level unchanged.",
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SystemImage(name = "speaker.wave.1.fill", fontSize = 17.sp)
                    FormSlider(
                        value = gainDb,
                        onValueChange = {
                            audio._gainDb.value = it
                            model.setAudioGain(it)
                        },
                        modifier = Modifier.weight(1f),
                        valueRange = 0.0f..24.0f,
                    )
                    SystemImage(name = "speaker.wave.3.fill", fontSize = 17.sp)
                    Box(
                        modifier = Modifier.width(65.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${formatOneDecimal(gainDb)} dB")
                    }
                }
            }
        }
        Section(footer = "Makes most Bluetooth speakers work better.") {
            Toggle("Bluetooth output only", isOn = bluetoothOutputOnly) {
                debug.bluetoothOutputOnly.value = it
                model.reloadAudioSession()
            }
        }
        if (showAllSettings) {
            Section(footerContent = {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(localized("Only works when front or back mic is selected."))
                    Text("")
                    Text(localized("Switching between mono and stereo mics may not work."))
                }
            }) {
                Toggle("Prefer stereo mic", isOn = preferStereoMic) {
                    audio._preferStereoMic.value = it
                    if (currentMic.isAudioSession()) {
                        model.reloadAudioSession()
                        model.selectMicDefault(mic = currentMic)
                    }
                }
            }
            Section(
                header = "Input to output channel mapping",
                footer = "Mono audio only uses output channel 1. Stereo audio uses both output channels.",
            ) {
                TextEditNavigationView(
                    title = localized("Output channel 1"),
                    value = (audio.outputToInputChannelsMap.channel1 + 1).toString(),
                    onChange = { changeOutputChannel(it) },
                    onSubmit = { submitOutputChannel1(it) },
                )
                TextEditNavigationView(
                    title = localized("Output channel 2"),
                    value = (audio.outputToInputChannelsMap.channel2 + 1).toString(),
                    onChange = { changeOutputChannel(it) },
                    onSubmit = { submitOutputChannel2(it) },
                )
            }
        }
    }
}
