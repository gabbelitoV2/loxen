package com.moblin.android.view.settings.streaminghistory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.various.storages.StreamingHistoryStream
import com.moblin.android.view.utils.TextValueView
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
private fun StreamingHistoryStreamSettingsGeneralView(stream: StreamingHistoryStream) {
    Text("General", style = MaterialTheme.typography.titleSmall)
    TextValueView(
        name = "Start time",
        value = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())
            .format(stream.startTime)
    )
    TextValueView(name = "Duration", value = stream.duration().formatWithSeconds())
    TextValueView(name = "Total sent", value = stream.totalBytes.formatBytes())
    TextValueView(name = "Average bitrate", value = stream.averageBitrateString())
    TextValueView(name = "Highest bitrate", value = stream.highestBitrateString())
}

@Composable
private fun StreamingHistoryStreamSettingsDeviceHealthView(stream: StreamingHistoryStream) {
    Text("Device health", style = MaterialTheme.typography.titleSmall)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Highest thermal state")
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Black)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp))
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = stream.highestThermalState!!.toProcessInfo().color()
            )
        }
    }
    TextValueView(
        name = "Lowest battery percentage",
        value = stream.lowestBatteryPercentageString()
    )
}

@Composable
private fun StreamingHistoryStreamSettingsSettingsView(stream: StreamingHistoryStream) {
    Text("Settings", style = MaterialTheme.typography.titleSmall)
    TextValueView(name = "Name", value = stream.settings.name)
    TextValueView(name = "Resolution", value = stream.settings.resolutionString())
    TextValueView(name = "FPS", value = "${stream.settings.fps}")
    TextValueView(name = "Protocol", value = stream.settings.protocolString())
    TextValueView(name = "Codec", value = stream.settings.codecString())
    TextValueView(name = "Bitrate", value = stream.settings.bitrateString())
    TextValueView(name = "Audio codec", value = stream.settings.audioCodecString())
    TextValueView(name = "Audio bitrate", value = stream.settings.audioBitrateString())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamingHistoryStreamSettingsView(stream: StreamingHistoryStream) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Stream summary") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Column {
                    StreamingHistoryStreamSettingsGeneralView(stream)
                }
            }
            item {
                Column {
                    StreamingHistoryStreamSettingsDeviceHealthView(stream)
                }
            }
            item {
                Column {
                    StreamingHistoryStreamSettingsSettingsView(stream)
                }
            }
        }
    }
}
