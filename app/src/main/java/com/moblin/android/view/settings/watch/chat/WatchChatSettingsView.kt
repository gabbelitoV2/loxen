package com.moblin.android.view.settings.watch.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import kotlinx.coroutines.flow.MutableStateFlow
import com.moblin.android.LocalModel

class WatchSettingsChat {
    val fontSize = MutableStateFlow(20.0f)
    val timestampEnabled = MutableStateFlow(false)
    val badges = MutableStateFlow(true)
    val notificationOnMessage = MutableStateFlow(true)
    val notificationRate = MutableStateFlow(60)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchChatSettingsView(model: Model = LocalModel.current, chat: WatchSettingsChat) {
    var pickerExpanded by remember { mutableStateOf(false) }
    val fontSize by chat.fontSize.collectAsState()
    val timestampEnabled by chat.timestampEnabled.collectAsState()
    val badges by chat.badges.collectAsState()
    val notificationOnMessage by chat.notificationOnMessage.collectAsState()
    val notificationRate by chat.notificationRate.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Chat")) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                localized("General"),
                style = MaterialTheme.typography.titleSmall
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(localized("Font size"))
                Slider(
                    value = fontSize,
                    onValueChange = { chat.fontSize.value = it },
                    valueRange = 10f..30f,
                    steps = 19,
                    onValueChangeFinished = {
                        Unit
                    },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${fontSize.toInt()}",
                    modifier = Modifier.width(25.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(localized("Timestamp"))
                Switch(
                    checked = timestampEnabled,
                    onCheckedChange = {
                        chat.timestampEnabled.value = it
                        Unit
                    }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(localized("Badges"))
                Switch(
                    checked = badges,
                    onCheckedChange = {
                        chat.badges.value = it
                        Unit
                    }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(localized("Notification on message"))
                Switch(
                    checked = notificationOnMessage,
                    onCheckedChange = {
                        chat.notificationOnMessage.value = it
                        Unit
                    }
                )
            }
            ExposedDropdownMenuBox(
                expanded = pickerExpanded,
                onExpandedChange = { pickerExpanded = it }
            ) {
                OutlinedTextField(
                    value = formatShortDuration(notificationRate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(localized("Notification rate")) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = pickerExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = pickerExpanded,
                    onDismissRequest = { pickerExpanded = false }
                ) {
                    listOf(60, 30, 15, 5, 1).forEach { rate ->
                        DropdownMenuItem(
                            text = { Text(formatShortDuration(rate)) },
                            onClick = {
                                chat.notificationRate.value = rate
                                pickerExpanded = false
                                Unit
                            }
                        )
                    }
                }
            }
        }
    }
}
