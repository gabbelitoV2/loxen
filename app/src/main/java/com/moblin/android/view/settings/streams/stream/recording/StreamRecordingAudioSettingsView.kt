package com.moblin.android.view.settings.streams.stream.recording

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import kotlin.math.ceil
import kotlin.math.roundToInt

private fun calcBitrate(bitrate: Float): Int {
    return ceil(bitrate * 1000.0).toInt()
}

@Composable
fun StreamRecordingAudioSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    initialBitrate: Float,
) {
    var bitrate by remember { mutableStateOf(initialBitrate) }
    val isRecording by model.isRecording.collectAsState()
    val disabled = stream.enabled && isRecording

    Form(title = "Audio") {
        Section(
            header = "Bitrate",
            footerContent = {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("128 Kbps or higher is recommended. Set to 0 for automatic.")
                    Text("")
                    Text("320 Kbps typically requires stereo mic.")
                }
            },
        ) {
            FormRow(enabled = !disabled) {
                FormSlider(
                    value = bitrate,
                    onValueChange = { bitrate = (it / 32f).roundToInt() * 32f },
                    modifier = Modifier.weight(1f),
                    valueRange = 0f..320f,
                    enabled = !disabled,
                    onValueChangeFinished = {
                        stream.recording.audioBitrate = calcBitrate(bitrate)
                    },
                )
                Box(
                    modifier = Modifier.width(90.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(formatBytesPerSecond(speed = calcBitrate(bitrate).toLong()))
                }
            }
        }
    }
}
