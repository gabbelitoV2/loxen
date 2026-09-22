package com.moblin.android.view.settings.streams.stream.previewstream

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.various.settings.SettingsStreamPreviewStream
import com.moblin.android.various.settings.SettingsStreamResolution
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.LocalOnNavigate

private fun resolutions(): List<SettingsStreamResolution> =
    listOf(
        SettingsStreamResolution.r854x480,
        SettingsStreamResolution.r640x360,
        SettingsStreamResolution.r426x240,
    )

private fun videoBitrates(): List<Int> =
    listOf(2_000_000, 1_500_000, 1_000_000, 500_000, 250_000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamPreviewStreamSettingsView(
    previewStream: SettingsStreamPreviewStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val url = previewStream.url
    val resolution = previewStream.resolution
    val bitrate = previewStream.bitrate
    var resolutionExpanded by remember { mutableStateOf(false) }
    var bitrateExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "A low-quality low-latency stream sent to a WHIP server. Can be used to " +
                "preview the stream from another device.",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("urlSettings") },
        ) {
            TextItemLocalizedView(name = "URL", value = url, sensitive = true)
        }
        ExposedDropdownMenuBox(
            expanded = resolutionExpanded,
            onExpandedChange = { resolutionExpanded = it },
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("Resolution")
                OutlinedTextField(
                    value = resolution.shortString(),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = resolutionExpanded)
                    },
                    modifier = Modifier.menuAnchor(),
                )
            }
            ExposedDropdownMenu(
                expanded = resolutionExpanded,
                onDismissRequest = { resolutionExpanded = false },
            ) {
                resolutions().forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.shortString()) },
                        onClick = {
                            previewStream.resolution = item
                            resolutionExpanded = false
                        },
                    )
                }
            }
        }
        ExposedDropdownMenuBox(
            expanded = bitrateExpanded,
            onExpandedChange = { bitrateExpanded = it },
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("Video bitrate")
                OutlinedTextField(
                    value = formatBytesPerSecond(speed = bitrate.toLong()),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = bitrateExpanded)
                    },
                    modifier = Modifier.menuAnchor(),
                )
            }
            ExposedDropdownMenu(
                expanded = bitrateExpanded,
                onDismissRequest = { bitrateExpanded = false },
            ) {
                videoBitrates().forEach { item ->
                    DropdownMenuItem(
                        text = { Text(formatBytesPerSecond(speed = item.toLong())) },
                        onClick = {
                            previewStream.bitrate = item
                            bitrateExpanded = false
                        },
                    )
                }
            }
        }
    }
}
