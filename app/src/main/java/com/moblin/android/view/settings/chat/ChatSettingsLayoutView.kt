package com.moblin.android.view.settings.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsAppMode
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.localized
import com.moblin.android.various.model.reloadChatMessages

val sliderValuePercentageWidth = 60.0

@Composable
fun ChatSettingsLayoutView(
    model: Model = LocalModel.current,
    database: Database,
    chat: SettingsChat,
) {
    NavigationLink(title = "Layout") {
        Form(title = "Layout") {
            Section {
                if (database.appMode != SettingsAppMode.chatPhone) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(localized("Height"))
                        FormSlider(
                            value = chat.height.toFloat(),
                            onValueChange = { chat.height = it.toDouble() },
                            modifier = Modifier.weight(1f),
                            valueRange = 0.2f..1.0f,
                            onValueChangeFinished = { model.reloadChatMessages() },
                        )
                        Text(
                            text = "${(100 * chat.height).toInt()}%",
                            modifier = Modifier.width(sliderValuePercentageWidth.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(localized("Width"))
                        FormSlider(
                            value = chat.width.toFloat(),
                            onValueChange = { chat.width = it.toDouble() },
                            modifier = Modifier.weight(1f),
                            valueRange = 0.2f..1.0f,
                            onValueChangeFinished = { model.reloadChatMessages() },
                        )
                        Text(
                            text = "${(100 * chat.width).toInt()}%",
                            modifier = Modifier.width(sliderValuePercentageWidth.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(localized("Bottom"))
                        FormSlider(
                            value = chat.bottomPoints.toFloat(),
                            onValueChange = { chat.bottomPoints = it.toDouble() },
                            modifier = Modifier.weight(1f),
                            valueRange = 0f..200f,
                            onValueChangeFinished = { model.reloadChatMessages() },
                        )
                        Text(
                            text = "${chat.bottomPoints.toInt()} pts",
                            modifier = Modifier.width(sliderValuePercentageWidth.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (database.showAllSettings) {
                    Toggle(
                        title = "New messages at top",
                        isOn = binding({ chat.newMessagesAtTop }, { chat.newMessagesAtTop = it }),
                    )
                    Toggle(
                        title = "Mirrored",
                        isOn = binding({ chat.mirrored }, { chat.mirrored = it }),
                    )
                }
            }
        }
    }
}
