package com.moblin.android.view.settings.ingests.whepclient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.isValidIngestLatency
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWhepClient
import com.moblin.android.various.settings.SettingsWhepClientStream
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
fun WhepClientStreamSettingsView(
    model: Model = LocalModel.current,
    whepClient: SettingsWhepClient,
    stream: SettingsWhepClientStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("whepClientStream") }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stream.name,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = stream.enabled,
            onCheckedChange = {
                stream.enabled = it
                TODO("reloadWhepClient")
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhepClientStreamSettingsViewInner(
    model: Model = LocalModel.current,
    whepClient: SettingsWhepClient,
    stream: SettingsWhepClientStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Stream")) })
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                NameEditView(
                    name = stream.name,
                    onNameChange = { stream.name = it },
                    existingNames = whepClient.streams,
                )
            }
            item { HorizontalDivider() }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("whepClientUrl") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextItemLocalizedView(
                        name = "URL",
                        value = stream.url,
                        sensitive = true,
                    )
                }
            }
            item { HorizontalDivider() }
            item {
                TextEditNavigationView(
                    title = localized("Latency"),
                    value = stream.latency.toString(),
                    onChange = { isValidIngestLatency(it) },
                    onSubmit = { value ->
                        val latency = value.toIntOrNull()
                        if (latency != null) {
                            stream.latency = latency
                            TODO("reloadWhepClient")
                        }
                    },
                    footers = listOf(
                        localized("5 or more milliseconds. 100 ms by default."),
                    ),
                    keyboardType = KeyboardType.Number,
                    valueFormat = { "$it ms" },
                )
                Text(
                    text = localized("The higher, the lower risk of stuttering."),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item { HorizontalDivider() }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = localized("Sync timestamps"),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = stream.syncTimestamps,
                        onCheckedChange = {
                            stream.syncTimestamps = it
                            TODO("reloadWhepClient")
                        },
                        enabled = !stream.enabled,
                    )
                }
            }
        }
    }
}
