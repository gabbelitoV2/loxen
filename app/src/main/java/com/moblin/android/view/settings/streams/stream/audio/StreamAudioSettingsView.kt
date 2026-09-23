package com.moblin.android.view.settings.streams.stream.audio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamAudioCodec
import kotlin.math.ceil
import com.moblin.android.various.model.reloadStreamIfEnabled
import com.moblin.android.various.model.setAudioStreamBitrate

private fun calcBitrate(bitrate: Float): Int {
    return ceil(bitrate * 1000.0f).toInt()
}

@Composable
fun StreamAudioSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
) {
    var bitrate by remember { mutableStateOf(stream.audioBitrate / 1000.0f) }
    val streamEnabled = stream.enabled
    val isLive by model.isLive.collectAsState()
    val locked = streamEnabled && isLive

    Form(title = "Audio") {
        Section(footer = "WHIP generally only supports Opus.") {
            Picker(
                title = "Codec",
                selection = stream.audioCodec,
                options = SettingsStreamAudioCodec.entries,
                enabled = !locked,
                text = { it.toString() },
                onChange = { codec ->
                    stream.audioCodec = codec
                    model.reloadStreamIfEnabled(stream)
                },
            )
        }
        Section(
            header = "Bitrate",
            footerContent = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("128 Kbps or higher is recommended.")
                    Text("")
                    Text("The actual bitrate may be lower if the device does not support it.")
                }
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FormSlider(
                    value = bitrate,
                    onValueChange = { bitrate = it },
                    modifier = Modifier.weight(1f),
                    valueRange = 32f..320f,
                    enabled = !locked,
                    onValueChangeFinished = {
                        stream.audioBitrate = calcBitrate(bitrate)
                        if (streamEnabled) {
                            model.setAudioStreamBitrate(stream)
                        }
                    },
                )
                Box(
                    modifier = Modifier.width(90.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(formatBytesPerSecond(calcBitrate(bitrate).toLong()))
                }
            }
        }
    }
}
