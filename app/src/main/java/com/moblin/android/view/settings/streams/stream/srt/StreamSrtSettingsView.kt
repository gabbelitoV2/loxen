package com.moblin.android.view.settings.streams.stream.srt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsDnsLookupStrategy
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamSrt
import com.moblin.android.various.settings.SettingsStreamSrtImplementation
import com.moblin.android.view.utils.TextEditNavigationView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamSrtSettingsView(
    model: Model,
    stream: SettingsStream,
    srt: SettingsStreamSrt,
    onNavigate: (String) -> Unit,
) {
    val isLive = model.isLive.collectAsState().value
    val latency = srt.latency.collectAsState().value
    val overheadBandwidth = srt.overheadBandwidth.collectAsState().value
    val adaptiveBitrateEnabled = srt.adaptiveBitrateEnabled.collectAsState().value
    val maximumBandwidthFollowInput = srt.maximumBandwidthFollowInput.collectAsState().value
    val bigPackets = srt.bigPackets.collectAsState().value
    val dnsLookupStrategy = srt.dnsLookupStrategy.collectAsState().value
    val implementation = srt.implementation.collectAsState().value
    val disabled = stream.enabled && isLive

    fun changeLatency(value: String): String? {
        val parsed = value.toIntOrNull() ?: return localized("Not a number")
        if (parsed < 0) {
            return localized("Too small")
        }
        if (parsed > 65535) {
            return localized("Too big")
        }
        return null
    }

    fun submitLatency(value: String) {
        val parsed = value.toIntOrNull() ?: return
        srt.setLatency(parsed)
        model.reloadStreamIfEnabled(stream)
    }

    fun changeOverheadBandwidth(value: String): String? {
        val parsed = value.toIntOrNull() ?: return localized("Not a number")
        if (parsed < 5) {
            return localized("Too small")
        }
        if (parsed > 100) {
            return localized("Too big")
        }
        return null
    }

    fun submitOverheadBandwidth(value: String) {
        val parsed = value.toIntOrNull() ?: return
        srt.setOverheadBandwidth(parsed)
        model.reloadStreamIfEnabled(stream)
    }

    var dnsExpanded by remember { mutableStateOf(false) }
    var implementationExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SRT(LA)") })
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                Column {
                    TextEditNavigationView(
                        title = localized("Latency"),
                        value = latency.toString(),
                        onChange = { changeLatency(it) },
                        onSubmit = { submitLatency(it) },
                        valueFormat = { "$it ms" },
                        enabled = !disabled,
                    )
                    if (implementation == SettingsStreamSrtImplementation.moblin && latency < 1000) {
                        Text(
                            "⚠️ The \"Moblin\" implementation does not perform well with low " +
                                "latency. Select the \"Official\" implementation at the bottom " +
                                "of this page.",
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !disabled) {
                                onNavigate("StreamSrtAdaptiveBitrateSettingsView")
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Adaptive bitrate", modifier = Modifier.weight(1f))
                        Switch(
                            checked = adaptiveBitrateEnabled,
                            onCheckedChange = {
                                srt.setAdaptiveBitrateEnabled(it)
                                model.reloadStreamIfEnabled(stream)
                            },
                            enabled = !disabled,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("StreamSrtConnectionPriorityView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Connection priorities", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    }
                    when (implementation) {
                        SettingsStreamSrtImplementation.official -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Max bandwidth follows input",
                                    modifier = Modifier.weight(1f),
                                )
                                Switch(
                                    checked = maximumBandwidthFollowInput,
                                    onCheckedChange = {
                                        srt.setMaximumBandwidthFollowInput(it)
                                        model.reloadStreamIfEnabled(stream)
                                    },
                                    enabled = !disabled,
                                )
                            }
                            TextEditNavigationView(
                                title = localized("Overhead bandwidth"),
                                value = overheadBandwidth.toString(),
                                onChange = { changeOverheadBandwidth(it) },
                                onSubmit = { submitOverheadBandwidth(it) },
                                valueFormat = { "$it%" },
                                enabled = !disabled,
                            )
                        }
                        SettingsStreamSrtImplementation.moblin -> Unit
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Big packets", modifier = Modifier.weight(1f))
                        Switch(
                            checked = bigPackets,
                            onCheckedChange = {
                                srt.setBigPackets(it)
                                model.reloadStreamIfEnabled(stream)
                            },
                            enabled = !disabled,
                        )
                    }
                    Text(
                        "Big packets means 7 MPEG-TS packets per SRT packet, 6 otherwise. " +
                            "Sometimes Android hotspots does not work with big packets.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            item {
                Column {
                    ExposedDropdownMenuBox(
                        expanded = dnsExpanded && !disabled,
                        onExpandedChange = { if (!disabled) dnsExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = dnsLookupStrategy.rawValue,
                            onValueChange = {},
                            readOnly = true,
                            enabled = !disabled,
                            label = { Text("DNS lookup strategy") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dnsExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                        ExposedDropdownMenu(
                            expanded = dnsExpanded && !disabled,
                            onDismissRequest = { dnsExpanded = false },
                        ) {
                            SettingsDnsLookupStrategy.entries.forEach { strategy ->
                                DropdownMenuItem(
                                    text = { Text(strategy.rawValue) },
                                    onClick = {
                                        srt.setDnsLookupStrategy(strategy)
                                        dnsExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    Text(
                        "System seems to work best for TMobile. IPv4 probably best for " +
                            "IRLToolkit.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            item {
                Column {
                    ExposedDropdownMenuBox(
                        expanded = implementationExpanded && !disabled,
                        onExpandedChange = { if (!disabled) implementationExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = implementation.toString(),
                            onValueChange = {},
                            readOnly = true,
                            enabled = !disabled,
                            label = { Text("Implementation") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = implementationExpanded,
                                )
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                        ExposedDropdownMenu(
                            expanded = implementationExpanded && !disabled,
                            onDismissRequest = { implementationExpanded = false },
                        ) {
                            SettingsStreamSrtImplementation.entries.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.toString()) },
                                    onClick = {
                                        srt.setImplementation(item)
                                        implementationExpanded = false
                                        model.reloadStreamIfEnabled(stream)
                                    },
                                )
                            }
                        }
                    }
                    Text(
                        "\"Official\" uses the widely supported libSRT (version 1.5.3) and " +
                            "\"Moblin\" uses a more energy efficient custom implementation.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
