package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.color
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.various.settings.SettingsWidgetAlertsChatBot
import com.moblin.android.various.settings.SettingsWidgetAlertsChatBotCommand
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import kotlinx.coroutines.withTimeout

@Composable
private fun ChatBotCommandView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    command: SettingsWidgetAlertsChatBotCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        NavigationLink(
            destination = {
                ChatBotCommandDetailView(model = model, alert = alert, command = command)
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(command.id) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var longPressed = false
                            try {
                                withTimeout(viewConfiguration.longPressTimeoutMillis) {
                                    waitForUpOrCancellation()
                                }
                            } catch (_: PointerEventTimeoutCancellationException) {
                                longPressed = true
                            }
                            if (longPressed) {
                                showMenu = true
                                var event = awaitPointerEvent()
                                while (event.changes.any { it.pressed }) {
                                    event.changes.forEach { it.consume() }
                                    event = awaitPointerEvent()
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    },
            ) {
                Text(command.name.replaceFirstChar { it.titlecase() })
            }
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(localized("Delete")) },
                onClick = {
                    showMenu = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
fun ChatBotCommandDetailView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    command: SettingsWidgetAlertsChatBotCommand,
) {
    var name by remember { mutableStateOf(command.name) }

    Form(title = localized("Command")) {
        Section {
            Toggle(title = localized("Enabled"), isOn = alert.enabled) { value ->
                alert.enabled = value
                model.updateAlertsSettings()
            }
        }
        Section(footer = localized("Trigger with chat message '!moblin alert $name'")) {
            TextEditNavigationView(
                title = localized("Name"),
                value = name,
                onSubmit = { value ->
                    command.name = value.lowercase().filterNot { it.isWhitespace() }
                    name = command.name
                    model.updateAlertsSettings()
                },
            )
        }
        AlertMediaView(alert = alert)
        AlertPositionView(model = model, alert = alert)
        AlertColorsView(
            alert = alert,
            textColor = alert.textColor.color(),
            accentColor = alert.accentColor.color(),
        )
        AlertFontView(
            alert = alert,
            fontSize = alert.fontSize.toFloat(),
            fontDesign = alert.fontDesign,
            fontWeight = alert.fontWeight,
        )
        AlertTextToSpeechView(alert = alert)
        Section {
            TextButtonView("Test") {
                model.testAlert(TODO("AlertTest has no Android counterpart"))
            }
        }
    }
}

@Composable
fun WidgetAlertsChatBotSettingsView(
    model: Model = LocalModel.current,
    chatBot: SettingsWidgetAlertsChatBot,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    fun deleteCommand(index: Int) {
        if (index in chatBot.commands.indices) {
            chatBot.commands = chatBot.commands.filterIndexed { i, _ -> i != index }
        }
        model.updateAlertsSettings()
    }

    Form(title = localized("Chat bot")) {
        Section(
            footerContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(localized("Trigger alerts with chat bot commands."))
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = localized("a command"))
                }
            },
        ) {
            chatBot.commands.forEach { command ->
                key(command.id) {
                    ChatBotCommandView(
                        model = model,
                        alert = command.alert,
                        command = command,
                        onNavigate = onNavigate,
                        onDelete = {
                            chatBot.commands.indexOfFirst { it.id == command.id }
                                .takeIf { it >= 0 }?.let { index ->
                                    deleteCommand(index)
                                }
                        },
                    )
                }
            }
            CreateButtonView {
                val command = SettingsWidgetAlertsChatBotCommand()
                chatBot.commands = chatBot.commands + command
                model.fixAlertMedias()
                model.updateAlertsSettings()
            }
        }
    }
}
