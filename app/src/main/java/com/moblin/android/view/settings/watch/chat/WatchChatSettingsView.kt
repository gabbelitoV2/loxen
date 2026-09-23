package com.moblin.android.view.settings.watch.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import kotlinx.coroutines.flow.MutableStateFlow
import com.moblin.android.various.model.sendSettingsToWatch

class WatchSettingsChat {
    val fontSize = MutableStateFlow(20.0f)
    val timestampEnabled = MutableStateFlow(false)
    val badges = MutableStateFlow(true)
    val notificationOnMessage = MutableStateFlow(true)
    val notificationRate = MutableStateFlow(60)
}

@Composable
fun WatchChatSettingsView(model: Model = LocalModel.current, chat: WatchSettingsChat) {
    val fontSize by chat.fontSize.collectAsState()
    val timestampEnabled by chat.timestampEnabled.collectAsState()
    val badges by chat.badges.collectAsState()
    val notificationOnMessage by chat.notificationOnMessage.collectAsState()
    val notificationRate by chat.notificationRate.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            model.sendSettingsToWatch()
        }
    }

    Form(title = localized("Chat")) {
        Section(header = localized("General")) {
            FormRow {
                Text(localized("Font size"))
                FormSlider(
                    value = fontSize,
                    onValueChange = { chat.fontSize.value = it },
                    modifier = Modifier.weight(1f),
                    valueRange = 10f..30f,
                    onValueChangeFinished = { model.sendSettingsToWatch() }
                )
                Box(
                    modifier = Modifier.width(25.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${fontSize.toInt()}")
                }
            }
            Toggle(
                title = localized("Timestamp"),
                isOn = timestampEnabled,
                onChange = {
                    chat.timestampEnabled.value = it
                    model.sendSettingsToWatch()
                }
            )
            Toggle(
                title = localized("Badges"),
                isOn = badges,
                onChange = {
                    chat.badges.value = it
                    model.sendSettingsToWatch()
                }
            )
            Toggle(
                title = localized("Notification on message"),
                isOn = notificationOnMessage,
                onChange = {
                    chat.notificationOnMessage.value = it
                    model.sendSettingsToWatch()
                }
            )
            Picker(
                title = localized("Notification rate"),
                selection = notificationRate,
                options = listOf(60, 30, 15, 5, 1),
                text = { formatShortDuration(it) },
                onChange = {
                    chat.notificationRate.value = it
                    model.sendSettingsToWatch()
                }
            )
        }
    }
}
