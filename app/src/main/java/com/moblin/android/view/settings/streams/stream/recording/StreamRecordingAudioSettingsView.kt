package com.moblin.android.view.settings.streams.stream.recording

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import kotlin.math.ceil
import com.moblin.android.LocalModel

private fun calcBitrate(bitrate: Float): Int {
    return ceil(bitrate * 1000.0).toInt()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamRecordingAudioSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    initialBitrate: Float,
) {
    var bitrate by remember { mutableStateOf(initialBitrate) }
    val isRecording by model.isRecording.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Audio") })
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
        ) {
            item {
                Text(
                    text = "Bitrate",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = bitrate,
                        onValueChange = { bitrate = it },
                        valueRange = 0f..320f,
                        steps = 9,
                        enabled = !(stream.enabled && isRecording),
                        onValueChangeFinished = {
                            stream.recording.audioBitrate = calcBitrate(bitrate)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatBytesPerSecond(speed = calcBitrate(bitrate).toLong()),
                        modifier = Modifier.width(90.dp),
                    )
                }
            }
            item {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("128 Kbps or higher is recommended. Set to 0 for automatic.")
                    Spacer(Modifier.height(16.dp))
                    Text("320 Kbps typically requires stereo mic.")
                }
            }
        }
    }
}
