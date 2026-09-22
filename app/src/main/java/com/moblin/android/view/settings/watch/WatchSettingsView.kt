package com.moblin.android.view.settings.watch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WatchSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchSettingsView(
    model: Model,
    watch: WatchSettings,
    onViaRemoteControlChange: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
) {
    val viaRemoteControl by watch.viaRemoteControl.collectAsState()
    LaunchedEffect(viaRemoteControl) {
        model.sendInitToWatch()
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Apple Watch") })
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("WatchChatSettingsView") }
                        .padding(16.dp),
                ) {
                    Text("Chat")
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("WatchDisplaySettingsView") }
                        .padding(16.dp),
                ) {
                    Text("Display")
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text("Remote control assistant")
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = viaRemoteControl,
                        onCheckedChange = { onViaRemoteControlChange(it) },
                    )
                }
            }
            item {
                Text(
                    "The watch acts as remote control assistant when enabled. Please note that in " +
                        "this case, chat, skip current TTS and a few other features are not supported.",
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
