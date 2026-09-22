package com.moblin.android.view.settings.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsAppMode
import com.moblin.android.various.settings.SettingsChat

val sliderValuePercentageWidth = 60.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsLayoutView(
    model: Model,
    database: Database,
    chat: SettingsChat,
) {
    val appMode by database.appMode.collectAsState()
    val showAllSettings by database.showAllSettings.collectAsState()
    val height by chat.height.collectAsState()
    val width by chat.width.collectAsState()
    val bottomPoints by chat.bottomPoints.collectAsState()
    val newMessagesAtTop by chat.newMessagesAtTop.collectAsState()
    val mirrored by chat.mirrored.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Layout") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (appMode != SettingsAppMode.chatPhone) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Height")
                    Slider(
                        value = height.toFloat(),
                        onValueChange = { chat.height.value = it.toDouble() },
                        valueRange = 0.2f..1.0f,
                        steps = 79,
                        onValueChangeFinished = { model.reloadChatMessages() },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${(100 * height).toInt()}%",
                        modifier = Modifier.width(sliderValuePercentageWidth.dp),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Width")
                    Slider(
                        value = width.toFloat(),
                        onValueChange = { chat.width.value = it.toDouble() },
                        valueRange = 0.2f..1.0f,
                        steps = 79,
                        onValueChangeFinished = { model.reloadChatMessages() },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${(100 * width).toInt()}%",
                        modifier = Modifier.width(sliderValuePercentageWidth.dp),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Bottom")
                    Slider(
                        value = bottomPoints.toFloat(),
                        onValueChange = { chat.bottomPoints.value = it.toDouble() },
                        valueRange = 0.0f..200.0f,
                        steps = 39,
                        onValueChangeFinished = { model.reloadChatMessages() },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${bottomPoints.toInt()} pts",
                        modifier = Modifier.width(sliderValuePercentageWidth.dp),
                    )
                }
            }
            if (showAllSettings) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("New messages at top")
                    Switch(
                        checked = newMessagesAtTop,
                        onCheckedChange = { chat.newMessagesAtTop.value = it },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Mirrored")
                    Switch(
                        checked = mirrored,
                        onCheckedChange = { chat.mirrored.value = it },
                    )
                }
            }
        }
    }
}
