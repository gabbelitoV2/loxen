package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.various.settings.SettingsWidgetAlertsChatBot
import com.moblin.android.various.settings.SettingsWidgetAlertsChatBotCommand
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalFoundationApi::class)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onNavigate("command") },
                    onLongClick = { showMenu = true },
                ),
        ) {
            Text(command.name.replaceFirstChar { it.titlecase() })
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBotCommandDetailView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    command: SettingsWidgetAlertsChatBotCommand,
) {
    var name by remember { mutableStateOf(command.name) }

    fun onSubmit(value: String) {
        command.name = value.lowercase().filterNot { it.isWhitespace() }
        name = command.name
        model.updateAlertsSettings()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Command")) })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(localized("Enabled"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = alert.enabled,
                        onCheckedChange = { value ->
                            alert.enabled = value
                            model.updateAlertsSettings()
                        },
                    )
                }
            }
            item {
                TextEditNavigationView(
                    title = localized("Name"),
                    value = name,
                    onSubmit = { onSubmit(it) },
                )
            }
            item {
                Text(localized("Trigger with chat message '!moblin alert $name'"))
            }
            item {
                AlertMediaView(alert = alert)
            }
            item {
                AlertPositionView(alert = alert)
            }
            item {
                AlertColorsView(
                    alert = alert,
                    textColor = TODO("SettingsColor.color() has no translation declared in the port glossary"),
                    accentColor = TODO("SettingsColor.color() has no translation declared in the port glossary"),
                )
            }
            item {
                AlertFontView(
                    alert = alert,
                    fontSize = alert.fontSize.toFloat(),
                    fontDesign = alert.fontDesign,
                    fontWeight = alert.fontWeight,
                )
            }
            item {
                AlertTextToSpeechView(alert = alert, ttsDelay = alert.textToSpeechDelay)
            }
            item {
                TextButtonView("Test") {
                    model.testAlert(
                        TODO("AlertTest.chatBotCommand(name, alertTestNames.random()) is not declared in the port glossary"),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetAlertsChatBotSettingsView(
    model: Model = LocalModel.current,
    chatBot: SettingsWidgetAlertsChatBot,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    fun deleteCommand(indexes: IntRange) {
        indexes.sortedDescending().forEach { index ->
            if (index in chatBot.commands.indices) {
                chatBot.commands.removeAt(index)
            }
        }
        model.updateAlertsSettings()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Chat bot")) })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            items(chatBot.commands, key = { it.id }) { command ->
                ChatBotCommandView(
                    model = model,
                    alert = command.alert,
                    command = command,
                    onNavigate = onNavigate,
                    onDelete = {
                        makeOffsets(chatBot.commands, command.id)?.let { deleteCommand(it) }
                    },
                )
            }
            item {
                CreateButtonView {
                    val command = SettingsWidgetAlertsChatBotCommand()
                    chatBot.commands.add(command)
                    model.fixAlertMedias()
                    model.updateAlertsSettings()
                }
            }
            item {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(localized("Trigger alerts with chat bot commands."))
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = localized("a command"))
                }
            }
        }
    }
}
