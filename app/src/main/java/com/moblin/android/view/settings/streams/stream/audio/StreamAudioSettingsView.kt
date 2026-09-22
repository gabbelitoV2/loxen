package com.moblin.android.view.settings.streams.stream.audio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.moblin.android.various.settings.SettingsStreamAudioCodec
import kotlin.math.ceil
import com.moblin.android.LocalModel

private fun calcBitrate(bitrate: Float): Int {
    return ceil(bitrate * 1000.0f).toInt()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamAudioSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
) {
    var bitrate by remember { mutableStateOf(stream.audioBitrate / 1000.0f) }
    var codecExpanded by remember { mutableStateOf(false) }
    val audioCodec = stream.audioCodec
    val streamEnabled = stream.enabled
    val isLive by model.isLive.collectAsState()
    val locked = streamEnabled && isLive

    LaunchedEffect(audioCodec) {
        Unit
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Audio") })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                ExposedDropdownMenuBox(
                    expanded = codecExpanded,
                    onExpandedChange = { codecExpanded = it },
                ) {
                    OutlinedTextField(
                        value = audioCodec.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Codec") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = codecExpanded)
                        },
                        enabled = !locked,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    ExposedDropdownMenu(
                        expanded = codecExpanded,
                        onDismissRequest = { codecExpanded = false },
                    ) {
                        SettingsStreamAudioCodec.entries.forEach { codec ->
                            DropdownMenuItem(
                                text = { Text(codec.toString()) },
                                onClick = {
                                    stream.audioCodec = codec
                                    codecExpanded = false
                                },
                            )
                        }
                    }
                }
                Text(
                    text = "WHIP generally only supports Opus.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                Text(
                    text = "Bitrate",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Slider(
                        value = bitrate,
                        onValueChange = { bitrate = it },
                        valueRange = 32.0f..320.0f,
                        steps = 8,
                        enabled = !locked,
                        onValueChangeFinished = {
                            stream.audioBitrate = calcBitrate(bitrate)
                            if (streamEnabled) {
                                Unit
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatBytesPerSecond(calcBitrate(bitrate).toLong()),
                        modifier = Modifier.width(90.dp),
                    )
                }
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text("128 Kbps or higher is recommended.")
                    Text("")
                    Text("The actual bitrate may be lower if the device does not support it.")
                }
            }
        }
    }
}
