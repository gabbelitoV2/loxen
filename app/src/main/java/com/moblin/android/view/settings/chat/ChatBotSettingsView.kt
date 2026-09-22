package com.moblin.android.view.settings.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.model.Model
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
import com.moblin.android.view.utils.CreateButtonView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionsSettingsInnerView(permissions: SettingsChatBotPermissionsCommand) {
    Text(text = "Permissions", style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Moderators", modifier = Modifier.weight(1f))
        Switch(
            checked = permissions.moderatorsEnabled.collectAsState().value,
            onCheckedChange = { permissions.moderatorsEnabled.value = it }
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Subscribers", modifier = Modifier.weight(1f))
        Switch(
            checked = permissions.subscribersEnabled.collectAsState().value,
            onCheckedChange = { permissions.subscribersEnabled.value = it }
        )
    }
    var tierExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = tierExpanded, onExpandedChange = { tierExpanded = it }) {
        OutlinedTextField(
            value = permissions.minimumSubscriberTier.collectAsState().value.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Minimum subscriber tier") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tierExpanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = tierExpanded, onDismissRequest = { tierExpanded = false }) {
            listOf(3, 2, 1).forEach { tier ->
                DropdownMenuItem(
                    text = { Text(text = tier.toString()) },
                    onClick = {
                        permissions.minimumSubscriberTier.value = tier
                        tierExpanded = false
                    }
                )
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Others", modifier = Modifier.weight(1f))
        Switch(
            checked = permissions.othersEnabled.collectAsState().value,
            onCheckedChange = { permissions.othersEnabled.value = it }
        )
    }
    var cooldownExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = cooldownExpanded, onExpandedChange = { cooldownExpanded = it }) {
        OutlinedTextField(
            value = permissions.cooldown.collectAsState().value?.let { "${it}s" } ?: "-- None --",
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "Cooldown") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cooldownExpanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = cooldownExpanded, onDismissRequest = { cooldownExpanded = false }) {
            DropdownMenuItem(
                text = { Text(text = "-- None --") },
                onClick = {
                    permissions.cooldown.value = null
                    cooldownExpanded = false
                }
            )
            listOf(1, 2, 3, 5, 10, 15, 30, 60).forEach { cooldown ->
                DropdownMenuItem(
                    text = { Text(text = "${cooldown}s") },
                    onClick = {
                        permissions.cooldown.value = cooldown
                        cooldownExpanded = false
                    }
                )
            }
        }
    }
    Text(
        text = "Does not apply to you and your moderators.",
        style = MaterialTheme.typography.bodySmall
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Send chat responses", modifier = Modifier.weight(1f))
        Switch(
            checked = permissions.sendChatMessages.collectAsState().value,
            onCheckedChange = { permissions.sendChatMessages.value = it }
        )
    }
    Text(
        text = "Typically sends a chat message if the user is not allowed to execute the command. " +
            "Some commands responds on success as well.",
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun PermissionsSettingsView(
    title: String,
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(title) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title)
    }
}

@Composable
private fun FixPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = "!moblin obs fix",
            permissions = permissions,
            onNavigate = onNavigate
        )
        Text(text = "Fix OBS input.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun GimbalPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = localized("!moblin gimbal preset <name>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
        Text(text = "Move to given gimbal preset.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AlertPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = localized("!moblin alert <name>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
        Text(
            text = "Trigger alerts. Configure alert names in alert widgets.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun FaxPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = localized("!moblin fax <url>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
        Text(text = "Fax the streamer images.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SnapshotPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = localized("!moblin snapshot <optional message>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
        Text(text = "Take snapshot.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReactionPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = localized("!moblin reaction <reaction>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
        Column {
            Text(text = "Perform reaction.")
            Text(text = "")
            Text(text = "<reaction> is hearts, fireworks, balloons, confetti, lasers, glasses or sparkle.")
        }
    }
}

@Composable
private fun FilterPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = localized("!moblin filter <filter> <on/off>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
        Column {
            Text(text = "Turn a filter on or off.")
            Text(text = "")
            Text(text = "<filter> is movie, grayscale, sepia, triple, pixellate or 4:3.")
            Text(text = "")
            Text(text = "<on/off> is on or off.")
        }
    }
}

@Composable
private fun ZoomPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = "!moblin zoom <x>",
            permissions = permissions,
            onNavigate = onNavigate
        )
        Text(text = "Set zoom for the current camera.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ScenePermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = localized("!moblin scene <name>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
        Text(text = "Switch to given scene.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StreamPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = "!moblin stream ...",
            permissions = permissions,
            onNavigate = onNavigate
        )
        Column {
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
    }
}

@Composable
private fun WidgetPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = localized("!moblin widget ..."),
            permissions = permissions,
            onNavigate = onNavigate
        )
        Column {
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
    }
}

@Composable
private fun LocationPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = "!moblin location ...",
            permissions = permissions,
            onNavigate = onNavigate
        )
        Column {
            Text(text = "!moblin location data reset")
            Text(text = "Resets distances, average speed and slope.")
            Text(text = "")
            Text(text = "!moblin location data split")
            Text(text = "Reset split distance.")
        }
    }
}

@Composable
private fun MapPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = "!moblin map zoom out",
            permissions = permissions,
            onNavigate = onNavigate
        )
        Text(text = "Zoom out map widget temporarily.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TtsSayPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = "!moblin tts/say ...",
            permissions = permissions,
            onNavigate = onNavigate
        )
        Column {
            Text(text = "!moblin tts on")
            Text(text = "Turn on chat text to speech.")
            Text(text = "")
            Text(text = "!moblin tts off")
            Text(text = "Turn off chat text to speech.")
            Text(text = "")
            Text(text = "!moblin say <message>")
            Text(text = "Say given message.")
        }
    }
}

@Composable
private fun AiPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    ai: SettingsOpenAi,
    onNavigate: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("!moblin ai ask <question>") },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "!moblin ai ask <question>")
        }
        Text(text = "Ask an AI a question.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TwitchPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = "!moblin twitch ...",
            permissions = permissions,
            onNavigate = onNavigate
        )
        Column {
            Text(text = "!moblin twitch raid <channel>")
            Text(text = "Raid given channel.")
        }
    }
}

@Composable
private fun MuteUnmutePermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = "!moblin mute/unmute",
            permissions = permissions,
            onNavigate = onNavigate
        )
        Column {
            Text(text = "!moblin mute")
            Text(text = "Mute audio.")
            Text(text = "")
            Text(text = "!moblin unmute")
            Text(text = "Unmute audio.")
        }
    }
}

@Composable
private fun TeslaPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = "!moblin tesla ...",
            permissions = permissions,
            onNavigate = onNavigate
        )
        Column {
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
    }
}

@Composable
private fun MacroPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = localized("!moblin macro <run/cancel> <name>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
        Column {
            Text(text = "!moblin macro run <name>")
            Text(text = "Run given macro.")
            Text(text = "")
            Text(text = "!moblin macro cancel <name>")
            Text(text = "Cancel given macro.")
        }
    }
}

@Composable
private fun SendPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = localized("!moblin send <message>"),
            permissions = permissions,
            onNavigate = onNavigate
        )
        Column {
            Text(text = "!moblin send <message>")
            Text(text = "Send given message.")
        }
    }
}

@Composable
private fun AppleMusicPermissionsSettingsView(
    permissions: SettingsChatBotPermissionsCommand,
    onNavigate: (String) -> Unit
) {
    Column {
        PermissionsSettingsView(
            title = "!moblin music ...",
            permissions = permissions,
            onNavigate = onNavigate
        )
        Column {
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
    }
}

@Composable
private fun ChatBotCommandsSettingsView(model: Model, onNavigate: (String) -> Unit) {
    val database = model.database.collectAsState().value
    val permissions = database.chat.botCommandPermissions
    LazyColumn {
        item {
            AiPermissionsSettingsView(
                permissions = permissions.ai,
                ai = database.chat.botCommandAi,
                onNavigate = onNavigate
            )
        }
        item {
            AlertPermissionsSettingsView(
                permissions = permissions.alert,
                onNavigate = onNavigate
            )
        }
        item {
            FaxPermissionsSettingsView(
                permissions = permissions.fax,
                onNavigate = onNavigate
            )
        }
        item {
            FilterPermissionsSettingsView(
                permissions = permissions.filter,
                onNavigate = onNavigate
            )
        }
        item {
            FixPermissionsSettingsView(
                permissions = permissions.fix,
                onNavigate = onNavigate
            )
        }
        item {
            GimbalPermissionsSettingsView(
                permissions = permissions.gimbal,
                onNavigate = onNavigate
            )
        }
        item {
            LocationPermissionsSettingsView(
                permissions = permissions.location,
                onNavigate = onNavigate
            )
        }
        item {
            MacroPermissionsSettingsView(
                permissions = permissions.macro,
                onNavigate = onNavigate
            )
        }
        item {
            MapPermissionsSettingsView(
                permissions = permissions.map,
                onNavigate = onNavigate
            )
        }
        item {
            MuteUnmutePermissionsSettingsView(
                permissions = permissions.audio,
                onNavigate = onNavigate
            )
        }
        item {
            ReactionPermissionsSettingsView(
                permissions = permissions.reaction,
                onNavigate = onNavigate
            )
        }
        item {
            ScenePermissionsSettingsView(
                permissions = permissions.scene,
                onNavigate = onNavigate
            )
        }
        item {
            SnapshotPermissionsSettingsView(
                permissions = permissions.snapshot,
                onNavigate = onNavigate
            )
        }
        item {
            StreamPermissionsSettingsView(
                permissions = permissions.stream,
                onNavigate = onNavigate
            )
        }
        item {
            TeslaPermissionsSettingsView(
                permissions = permissions.tesla,
                onNavigate = onNavigate
            )
        }
        item {
            SendPermissionsSettingsView(
                permissions = permissions.send,
                onNavigate = onNavigate
            )
        }
        item {
            AppleMusicPermissionsSettingsView(
                permissions = permissions.music,
                onNavigate = onNavigate
            )
        }
        item {
            TtsSayPermissionsSettingsView(
                permissions = permissions.tts,
                onNavigate = onNavigate
            )
        }
        item {
            WidgetPermissionsSettingsView(
                permissions = permissions.widget,
                onNavigate = onNavigate
            )
        }
        item {
            TwitchPermissionsSettingsView(
                permissions = permissions.twitch,
                onNavigate = onNavigate
            )
        }
        item {
            ZoomPermissionsSettingsView(
                permissions = permissions.zoom,
                onNavigate = onNavigate
            )
        }
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
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("Alias") },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = alias.alias.collectAsState().value)
        Spacer(modifier = Modifier.weight(1f))
        GrayTextView(text = alias.replacement.collectAsState().value)
    }
}

@Composable
private fun ChatBotCustomCommandTextSettingsView(
    model: Model,
    customCommand: SettingsChatBotCustomCommand,
    value: String
) {
    var text by remember { mutableStateOf(value) }
    val database = model.database.collectAsState().value
    LazyColumn {
        item {
            TextWidgetTextView(
                value = text,
                onChange = { text = it }
            )
        }
        item {
            TextFormatWarningsView(
                model = model,
                location = database.location,
                value = text,
                onChange = { text = it }
            )
        }
        item {
            TextWidgetSuggestionsView(
                widget = false,
                text = text,
                onChange = { text = it }
            )
        }
        item {
            TextFormatVariablesView(
                widget = false,
                value = text,
                onChange = { text = it }
            )
        }
    }
    LaunchedEffect(text) {
        customCommand.formatString.value = text
        model.chatBotCustomCommandsTextChanged()
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
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("Command") },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = customCommand.name.collectAsState().value)
        Spacer(modifier = Modifier.weight(1f))
        GrayTextView(text = customCommand.formatString.collectAsState().value)
    }
}

@Composable
private fun ChatBotCustomCommandsSettingsView(
    model: Model,
    chat: SettingsChat,
    onNavigate: (String) -> Unit
) {
    val customCommands = chat.customCommands.collectAsState().value
    LazyColumn {
        items(customCommands, key = { it.id }) { customCommand ->
            ChatBotCustomCommandSettingsView(
                customCommand = customCommand,
                onNavigate = onNavigate
            )
            TODO("contextMenuDeleteButton, List onMove and onDelete have no Compose counterpart")
        }
        item {
            CreateButtonView {
                chat.customCommands.value = chat.customCommands.value + SettingsChatBotCustomCommand()
            }
        }
        item {
            Column {
                Text(text = "!moblin custom <name>")
                Text(text = "Send the text of the command with given name to chat.")
            }
        }
    }
}

@Composable
private fun ChatBotAliasesSettingsView(
    chat: SettingsChat,
    onNavigate: (String) -> Unit
) {
    val aliases = chat.aliases.collectAsState().value
    LazyColumn {
        items(aliases, key = { it.id }) { alias ->
            ChatBotAliasSettingsView(
                alias = alias,
                onNavigate = onNavigate
            )
            TODO("contextMenuDeleteButton, List onMove and onDelete have no Compose counterpart")
        }
        item {
            CreateButtonView {
                chat.aliases.value = chat.aliases.value + SettingsChatBotAlias()
            }
        }
    }
}

@Composable
fun ChatBotSettingsView(model: Model, onNavigate: (String) -> Unit) {
    val database = model.database.collectAsState().value
    LazyColumn {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("Commands") },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Commands")
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("Custom commands") },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Custom commands")
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("Aliases") },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Aliases")
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Send low battery message", modifier = Modifier.weight(1f))
                Switch(
                    checked = database.chat.botSendLowBatteryWarning.collectAsState().value,
                    onCheckedChange = { database.chat.botSendLowBatteryWarning.value = it }
                )
            }
        }
    }
}
