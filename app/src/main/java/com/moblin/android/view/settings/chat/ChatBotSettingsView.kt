package com.moblin.android.view.settings.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.move
import com.moblin.android.platform.swiftui.remove
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.chatBotCustomCommandsTextChanged
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.various.settings.SettingsChatBotAlias
import com.moblin.android.various.settings.SettingsChatBotCustomCommand
import com.moblin.android.various.settings.SettingsChatBotPermissionsCommand
import com.moblin.android.various.settings.SettingsOpenAi
import com.moblin.android.view.settings.scenes.widgets.widget.text.TextFormatVariablesView
import com.moblin.android.view.settings.scenes.widgets.widget.text.TextFormatWarningsView
import com.moblin.android.view.settings.scenes.widgets.widget.text.TextWidgetSuggestionsView
import com.moblin.android.view.settings.scenes.widgets.widget.text.TextWidgetTextView
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.OpenAiSettingsView
import com.moblin.android.view.utils.TextEditView
import com.moblin.android.view.utils.TextItemLocalizedView

@Composable
private fun PermissionsSettingsInnerView(permissions: SettingsChatBotPermissionsCommand) {
    Section(header = "Permissions") {
        Toggle(title = "Moderators", isOn = permissions.moderatorsEnabled) {
            permissions.moderatorsEnabled = it
        }
        Toggle(title = "Subscribers", isOn = permissions.subscribersEnabled) {
            permissions.subscribersEnabled = it
        }
        Picker(
            title = "Minimum subscriber tier",
            selection = permissions.minimumSubscriberTier,
            options = listOf(3, 2, 1),
        ) {
            permissions.minimumSubscriberTier = it
        }
        Toggle(title = "Others", isOn = permissions.othersEnabled) {
            permissions.othersEnabled = it
        }
    }
    Section(footer = "Does not apply to you and your moderators.") {
        Picker(
            title = "Cooldown",
            selection = permissions.cooldown,
            options = listOf<Int?>(null, 1, 2, 3, 5, 10, 15, 30, 60),
            text = { if (it == null) "-- None --" else "${it}s" },
        ) {
            permissions.cooldown = it
        }
    }
    Section(
        footer = "Typically sends a chat message if the user is not allowed to execute the command. " +
            "Some commands responds on success as well."
    ) {
        Toggle(title = "Send chat responses", isOn = permissions.sendChatMessages) {
            permissions.sendChatMessages = it
        }
    }
}

@Composable
private fun PermissionsSettingsView(
    title: String,
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    NavigationLink(
        destination = {
            Form(title = title) {
                PermissionsSettingsInnerView(permissions = permissions)
            }
        },
    ) {
        Text(text = title)
    }
}

@Composable
private fun FixPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(footer = "Fix OBS input.") {
        PermissionsSettingsView(
            title = "!moblin obs fix",
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun GimbalPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(footer = "Move to given gimbal preset.") {
        PermissionsSettingsView(
            title = localized("!moblin gimbal preset <name>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun AlertPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(footer = "Trigger alerts. Configure alert names in alert widgets.") {
        PermissionsSettingsView(
            title = localized("!moblin alert <name>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun FaxPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(footer = "Fax the streamer images.") {
        PermissionsSettingsView(
            title = localized("!moblin fax <url>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun SnapshotPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(footer = "Take snapshot.") {
        PermissionsSettingsView(
            title = localized("!moblin snapshot <optional message>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun ReactionPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(
        footerContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Perform reaction.")
                Text(text = "")
                Text(text = "<reaction> is hearts, fireworks, balloons, confetti, lasers, glasses or sparkle.")
            }
        },
    ) {
        PermissionsSettingsView(
            title = localized("!moblin reaction <reaction>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun FilterPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(
        footerContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Turn a filter on or off.")
                Text(text = "")
                Text(text = "<filter> is movie, grayscale, sepia, triple, pixellate or 4:3.")
                Text(text = "")
                Text(text = "<on/off> is on or off.")
            }
        },
    ) {
        PermissionsSettingsView(
            title = localized("!moblin filter <filter> <on/off>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun ZoomPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(footer = "Set zoom for the current camera.") {
        PermissionsSettingsView(
            title = "!moblin zoom <x>",
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun ScenePermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(footer = "Switch to given scene.") {
        PermissionsSettingsView(
            title = localized("!moblin scene <name>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun StreamPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(
        footerContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "!moblin stream start")
                Text(text = "Start the stream.")
                Text(text = "")
                Text(text = "!moblin stream stop")
                Text(text = "Stop the stream.")
                Text(text = "")
                Text(text = "!moblin stream title <title>")
                Text(text = "Set stream title.")
                Text(text = "")
                Text(text = "!moblin stream category <category name>")
                Text(text = "Set stream category.")
            }
        },
    ) {
        PermissionsSettingsView(
            title = "!moblin stream ...",
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun WidgetPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(
        footerContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "!moblin widget <name> enable")
                Text(text = "Enable given widget.")
                Text(text = "")
                Text(text = "!moblin widget <name> disable")
                Text(text = "Disable given widget.")
                Text(text = "")
                Text(text = "!moblin widget <name> wheelofluck options <option 1> <option 2> ...")
                Text(text = "Set options for given wheel of luck.")
                Text(text = "")
                Text(text = "!moblin widget <name> wheelofluck spin")
                Text(text = "Spin given wheel of luck.")
                Text(text = "")
                Text(text = "!moblin widget <name> timer <number> add <seconds>")
                Text(text = "Change timer value.")
            }
        },
    ) {
        PermissionsSettingsView(
            title = localized("!moblin widget ..."),
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun LocationPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(
        footerContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "!moblin location data reset")
                Text(text = "Resets distances, average speed and slope.")
                Text(text = "")
                Text(text = "!moblin location data split")
                Text(text = "Reset split distance.")
            }
        },
    ) {
        PermissionsSettingsView(
            title = "!moblin location ...",
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun MapPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(footer = "Zoom out map widget temporarily.") {
        PermissionsSettingsView(
            title = "!moblin map zoom out",
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun TtsSayPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(
        footerContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "!moblin tts on")
                Text(text = "Turn on chat text to speech.")
                Text(text = "")
                Text(text = "!moblin tts off")
                Text(text = "Turn off chat text to speech.")
                Text(text = "")
                Text(text = "!moblin say <message>")
                Text(text = "Say given message.")
            }
        },
    ) {
        PermissionsSettingsView(
            title = "!moblin tts/say ...",
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun AiPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    ai: SettingsOpenAi,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(footer = "Ask an AI a question.") {
        NavigationLink(
            destination = {
                Form(title = "!moblin ai ask <question>") {
                    OpenAiSettingsView(ai = ai)
                    PermissionsSettingsInnerView(permissions = permissions)
                }
            },
        ) {
            Text(text = "!moblin ai ask <question>")
        }
    }
}

@Composable
private fun TwitchPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(
        footerContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "!moblin twitch raid <channel>")
                Text(text = "Raid given channel.")
            }
        },
    ) {
        PermissionsSettingsView(
            title = "!moblin twitch ...",
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun MuteUnmutePermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(
        footerContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "!moblin mute")
                Text(text = "Mute audio.")
                Text(text = "")
                Text(text = "!moblin unmute")
                Text(text = "Unmute audio.")
            }
        },
    ) {
        PermissionsSettingsView(
            title = "!moblin mute/unmute",
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun TorchPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(
        footerContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "!moblin torch on")
                Text(text = "Turn the torch on.")
                Text(text = "")
                Text(text = "!moblin torch off")
                Text(text = "Turn the torch off.")
                Text(text = "")
                Text(text = "!moblin torch level <level>")
                Text(text = "Set torch level, from 1 to 100.")
            }
        },
    ) {
        PermissionsSettingsView(
            title = "!moblin torch ...",
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun TeslaPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(
        footerContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "!moblin tesla trunk open")
                Text(text = "Open the trunk.")
                Text(text = "")
                Text(text = "!moblin tesla trunk close")
                Text(text = "Close the trunk.")
                Text(text = "")
                Text(text = "!moblin tesla media next")
                Text(text = "Next track.")
                Text(text = "")
                Text(text = "!moblin tesla media previous")
                Text(text = "Previous track.")
                Text(text = "")
                Text(text = "!moblin tesla media toggle-playback")
                Text(text = "Toggle playback.")
            }
        },
    ) {
        PermissionsSettingsView(
            title = "!moblin tesla ...",
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun MacroPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(
        footerContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "!moblin macro run <name>")
                Text(text = "Run given macro.")
                Text(text = "")
                Text(text = "!moblin macro cancel <name>")
                Text(text = "Cancel given macro.")
            }
        },
    ) {
        PermissionsSettingsView(
            title = localized("!moblin macro <run/cancel> <name>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun SendPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(
        footerContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "!moblin send <message>")
                Text(text = "Send given message.")
            }
        },
    ) {
        PermissionsSettingsView(
            title = localized("!moblin send <message>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun AppleMusicPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(
        footerContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "!moblin music add <song>")
                Text(text = "Add given song to the queue.")
                Text(text = "<song> is either a share link or text search.")
                Text(text = "")
                Text(text = "!moblin music play")
                Text(text = "Play.")
                Text(text = "")
                Text(text = "!moblin music pause")
                Text(text = "Pause.")
                Text(text = "")
                Text(text = "!moblin music next")
                Text(text = "Next song.")
                Text(text = "")
                Text(text = "!moblin music previous")
                Text(text = "Previous song.")
                Text(text = "")
                Text(text = "!moblin music status")
                Text(text = "Show status.")
            }
        },
    ) {
        PermissionsSettingsView(
            title = "!moblin music ...",
            permissions = permissions,
            onNavigate = onNavigate
        )
    }
}

@Composable
private fun ChatBotCommandsSettingsView(
    model: Model = LocalModel.current,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    val database = model.database
    val permissions = database.chat.botCommandPermissions
    Form(title = "Commands") {
        AiPermissionsSettingsView(
            permissions = permissions.ai,
            ai = database.chat.botCommandAi,
            onNavigate = onNavigate
        )
        AlertPermissionsSettingsView(
            permissions = permissions.alert,
            onNavigate = onNavigate
        )
        FaxPermissionsSettingsView(
            permissions = permissions.fax,
            onNavigate = onNavigate
        )
        FilterPermissionsSettingsView(
            permissions = permissions.filter,
            onNavigate = onNavigate
        )
        FixPermissionsSettingsView(
            permissions = permissions.fix,
            onNavigate = onNavigate
        )
        GimbalPermissionsSettingsView(
            permissions = permissions.gimbal,
            onNavigate = onNavigate
        )
        LocationPermissionsSettingsView(
            permissions = permissions.location,
            onNavigate = onNavigate
        )
        MacroPermissionsSettingsView(
            permissions = permissions.macro,
            onNavigate = onNavigate
        )
        MapPermissionsSettingsView(
            permissions = permissions.map,
            onNavigate = onNavigate
        )
        MuteUnmutePermissionsSettingsView(
            permissions = permissions.audio,
            onNavigate = onNavigate
        )
        ReactionPermissionsSettingsView(
            permissions = permissions.reaction,
            onNavigate = onNavigate
        )
        ScenePermissionsSettingsView(
            permissions = permissions.scene,
            onNavigate = onNavigate
        )
        SnapshotPermissionsSettingsView(
            permissions = permissions.snapshot,
            onNavigate = onNavigate
        )
        StreamPermissionsSettingsView(
            permissions = permissions.stream,
            onNavigate = onNavigate
        )
        TeslaPermissionsSettingsView(
            permissions = permissions.tesla,
            onNavigate = onNavigate
        )
        TorchPermissionsSettingsView(
            permissions = permissions.torch,
            onNavigate = onNavigate
        )
        SendPermissionsSettingsView(
            permissions = permissions.send,
            onNavigate = onNavigate
        )
        AppleMusicPermissionsSettingsView(
            permissions = permissions.music,
            onNavigate = onNavigate
        )
        TtsSayPermissionsSettingsView(
            permissions = permissions.tts,
            onNavigate = onNavigate
        )
        WidgetPermissionsSettingsView(
            permissions = permissions.widget,
            onNavigate = onNavigate
        )
        TwitchPermissionsSettingsView(
            permissions = permissions.twitch,
            onNavigate = onNavigate
        )
        ZoomPermissionsSettingsView(
            permissions = permissions.zoom,
            onNavigate = onNavigate
        )
    }
}

private fun onAliasChange(value: String): String? {
    if (value.isEmpty()) {
        return localized("The alias must not be empty.")
    }
    if (!value.startsWith("!")) {
        return localized("The alias must start with !.")
    }
    if (value.length <= 1) {
        return localized("The alias is too short.")
    }
    if (value.startsWith("!moblin")) {
        return localized("The alias must not start with !moblin.")
    }
    if (value.split(" ").count { it.isNotEmpty() } != 1) {
        return localized("The alias must be exactly one word.")
    }
    return null
}

private fun onReplacementChange(value: String): String? {
    if (value.isEmpty()) {
        return localized("The replacement must not be empty.")
    }
    if (!value.startsWith("!moblin")) {
        return localized("The replacement must start with !moblin.")
    }
    if (value.split(" ").count { it.isNotEmpty() } <= 1) {
        return localized("The replacement must be more than one word.")
    }
    return null
}

@Composable
private fun ChatBotAliasSettingsView(
    alias: SettingsChatBotAlias,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    NavigationLink(
        destination = {
            Form(title = "Alias") {
                Section {
                    NavigationLink(
                        destination = {
                            TextEditView(
                                title = localized("Alias"),
                                value = alias.alias,
                                onChange = ::onAliasChange,
                                onSubmit = { alias.alias = it }
                            )
                        },
                    ) {
                        TextItemLocalizedView(name = "Alias", value = alias.alias)
                    }
                    NavigationLink(
                        destination = {
                            TextEditView(
                                title = localized("Replacement"),
                                value = alias.replacement,
                                onChange = ::onReplacementChange,
                                onSubmit = { alias.replacement = it }
                            )
                        },
                    ) {
                        TextItemLocalizedView(name = "Replacement", value = alias.replacement)
                    }
                }
            }
        },
    ) {
        Text(text = alias.alias)
        Spacer(modifier = Modifier.weight(1f))
        GrayTextView(text = alias.replacement)
    }
}

@Composable
private fun ChatBotCustomCommandTextSettingsView(
    model: Model = LocalModel.current,
    customCommand: SettingsChatBotCustomCommand,
    value: String
) {
    var text by remember { mutableStateOf(value) }
    val database = model.database
    Form(title = "Text") {
        TextWidgetTextView(
            value = text,
            onChange = { text = it }
        )
        TextFormatWarningsView(
            model = model,
            location = database.location,
            value = text,
            onChange = { text = it }
        )
        Section {
            TextWidgetSuggestionsView(
                widget = false,
                text = text,
                onChange = { text = it }
            )
        }
        TextFormatVariablesView(
            model = model,
            widget = false,
            value = text,
            onChange = { text = it }
        )
    }
    LaunchedEffect(text) {
        customCommand.formatString = text
    }
}

private fun onNameChange(value: String): String? {
    if (value.isEmpty()) {
        return localized("The name must not be empty.")
    }
    return null
}

@Composable
private fun ChatBotCustomCommandSettingsView(
    customCommand: SettingsChatBotCustomCommand,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    NavigationLink(
        destination = {
            Form(title = "Command") {
                Section(footer = "Send the text to chat when a user sends !moblin custom <name>.") {
                    NavigationLink(
                        destination = {
                            TextEditView(
                                title = localized("Name"),
                                value = customCommand.name,
                                onChange = ::onNameChange,
                                onSubmit = { customCommand.name = it }
                            )
                        },
                    ) {
                        TextItemLocalizedView(name = "Name", value = customCommand.name)
                    }
                    NavigationLink(
                        destination = {
                            ChatBotCustomCommandTextSettingsView(
                                customCommand = customCommand,
                                value = customCommand.formatString
                            )
                        },
                    ) {
                        TextItemLocalizedView(name = "Text", value = customCommand.formatString)
                    }
                }
                PermissionsSettingsInnerView(permissions = customCommand.permissions)
            }
        },
    ) {
        Text(text = customCommand.name)
        Spacer(modifier = Modifier.weight(1f))
        GrayTextView(text = customCommand.formatString)
    }
}

@Composable
private fun ChatBotCustomCommandsSettingsView(
    model: Model = LocalModel.current,
    chat: SettingsChat,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Form(title = "Custom commands") {
        Section(
            footerContent = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "!moblin custom <name>")
                    Text(text = "Send the text of the command with given name to chat.")
                }
            },
        ) {
            ForEach(
                chat.customCommands,
                id = { it.id },
                onDelete = { offsets ->
                    chat.customCommands.remove(atOffsets = offsets)
                    model.chatBotCustomCommandsTextChanged()
                },
                onMove = { froms, to ->
                    chat.customCommands.move(fromOffsets = froms, toOffset = to)
                },
            ) { customCommand ->
                ContextMenuDeleteButton(
                    action = {
                        chat.customCommands.removeAll { it.id == customCommand.id }
                        model.chatBotCustomCommandsTextChanged()
                    },
                ) {
                    ChatBotCustomCommandSettingsView(
                        customCommand = customCommand,
                        onNavigate = onNavigate
                    )
                }
            }
            CreateButtonView {
                chat.customCommands.add(SettingsChatBotCustomCommand())
            }
        }
    }
}

@Composable
private fun ChatBotAliasesSettingsView(
    chat: SettingsChat,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Form(title = "Aliases") {
        Section {
            ForEach(
                chat.aliases,
                id = { it.id },
                onDelete = { offsets ->
                    chat.aliases.remove(atOffsets = offsets)
                },
                onMove = { froms, to ->
                    chat.aliases.move(fromOffsets = froms, toOffset = to)
                },
            ) { alias ->
                ContextMenuDeleteButton(
                    action = {
                        chat.aliases.removeAll { it.id == alias.id }
                    },
                ) {
                    ChatBotAliasSettingsView(
                        alias = alias,
                        onNavigate = onNavigate
                    )
                }
            }
            CreateButtonView {
                chat.aliases.add(SettingsChatBotAlias())
            }
        }
    }
}

@Composable
fun ChatBotSettingsView(
    model: Model = LocalModel.current,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Form(title = "Bot") {
        Section {
            NavigationLink(
                destination = {
                    ChatBotCommandsSettingsView(
                        model = model,
                        onNavigate = onNavigate
                    )
                },
            ) {
                Text(text = "Commands")
            }
            NavigationLink(
                destination = {
                    ChatBotCustomCommandsSettingsView(
                        model = model,
                        chat = model.database.chat,
                        onNavigate = onNavigate
                    )
                },
            ) {
                Text(text = "Custom commands")
            }
            NavigationLink(
                destination = {
                    ChatBotAliasesSettingsView(
                        chat = model.database.chat,
                        onNavigate = onNavigate
                    )
                },
            ) {
                Text(text = "Aliases")
            }
        }
        Section {
            Toggle(
                isOn = model.database.chat.botSendLowBatteryWarning,
                onChange = { model.database.chat.botSendLowBatteryWarning = it },
            ) {
                Text(text = "Send low battery message")
            }
        }
    }
}
