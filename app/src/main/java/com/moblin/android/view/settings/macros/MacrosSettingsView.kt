package com.moblin.android.view.settings.macros

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRowInfo
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.platform.swiftui.moving
import com.moblin.android.platform.swiftui.removing
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.remoteControlMacrosStateChanged
import com.moblin.android.various.model.startMacro
import com.moblin.android.various.model.stopMacro
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsMacros
import com.moblin.android.various.settings.SettingsMacrosAction
import com.moblin.android.various.settings.SettingsMacrosActionFunction
import com.moblin.android.various.settings.SettingsMacrosActionIfComparison
import com.moblin.android.various.settings.SettingsMacrosEvent
import com.moblin.android.various.settings.SettingsMacrosMacro
import com.moblin.android.various.settings.SettingsMacrosMacroRepeatMode
import com.moblin.android.various.settings.SettingsQuickButtonType
import com.moblin.android.various.settings.SettingsReaction
import com.moblin.android.various.settings.minZoomX
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.scenes.widgets.widget.text.TextFormatVariablesView
import com.moblin.android.view.settings.scenes.widgets.widget.text.TextFormatWarningsView
import com.moblin.android.view.settings.scenes.widgets.widget.text.TextWidgetSuggestionsView
import com.moblin.android.view.settings.scenes.widgets.widget.text.TextWidgetTextView
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.TextItemLocalizedView
import java.util.UUID

private fun <T> isSelected(values: Set<T>, type: T): Boolean {
    return values.contains(type)
}

private fun <T> setSelected(values: Set<T>, type: T, selected: Boolean): Set<T> {
    return if (selected) {
        values + type
    } else {
        values - type
    }
}

private fun getSceneName(database: Database, id: UUID?): String? {
    if (id == null) {
        return null
    }
    return database.scenes.firstOrNull { it.id == id }?.name
}

data class MacroActionIfBar(
    val level: Int,
    val isFirst: Boolean,
    val isLast: Boolean,
)

fun macroActionIfBars(actions: List<SettingsMacrosAction>): List<List<MacroActionIfBar>> {
    val blocks = mutableListOf<Pair<Int, Int>>()
    val bars = mutableListOf<List<MacroActionIfBar>>()
    for ((index, action) in actions.withIndex()) {
        blocks.removeAll { it.first <= index }
        var firstLevel: Int? = null
        if (action.function == SettingsMacrosActionFunction.IF_CONDITION && action.ifRunCount > 0) {
            var level = 0
            while (blocks.any { it.second == level }) {
                level += 1
            }
            blocks.add(Pair(minOf(index + 1 + action.ifRunCount, actions.size), level))
            firstLevel = level
        }
        bars.add(
            blocks.map {
                MacroActionIfBar(
                    level = it.second,
                    isFirst = it.second == firstLevel,
                    isLast = it.first == index + 1,
                )
            },
        )
    }
    return bars
}

private val macroActionIfColors: List<Color> = listOf(
    Color(0xFF007AFFL),
    Color(0xFFAF52DEL),
    Color(0xFFFF9500L),
    Color(0xFF30B0C7L),
)

@Composable
private fun bar(bars: List<MacroActionIfBar>, level: Int) {
    val bar = bars.firstOrNull { it.level == level }
    Column(
        modifier = Modifier
            .width(3.dp)
            .fillMaxHeight(),
    ) {
        if (bar?.isFirst == true) {
            Spacer(modifier = Modifier.weight(1f))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = if (bar?.isLast == true) 4.dp else 0.dp)
                .background(
                    if (bar != null) {
                        macroActionIfColors[level % macroActionIfColors.size]
                    } else {
                        Color.Transparent
                    },
                ),
        )
    }
}

@Composable
private fun ActionIfBarsView(bars: List<MacroActionIfBar>) {
    Row(
        modifier = Modifier
            .background(formPalette().cell)
            .fillMaxSize()
            .padding(start = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val levels = (bars.maxOfOrNull { it.level } ?: -1) + 1
        for (level in 0 until levels) {
            bar(bars = bars, level = level)
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TextFormatView(
    model: Model = LocalModel.current,
    title: String,
    suggestions: Boolean,
    text: String,
    onTextChange: (String) -> Unit,
    value: String,
) {
    var currentValue by remember { mutableStateOf(value) }
    LaunchedEffect(currentValue) {
        onTextChange(currentValue)
    }
    Form(title = title) {
        TextWidgetTextView(
            value = currentValue,
            onChange = { currentValue = it },
        )
        TextFormatWarningsView(
            model = model,
            location = model.database.location,
            value = currentValue,
            onChange = { currentValue = it },
        )
        if (suggestions) {
            Section {
                TextWidgetSuggestionsView(
                    widget = false,
                    text = currentValue,
                    onChange = { currentValue = it },
                )
            }
        }
        TextFormatVariablesView(
            widget = false,
            value = currentValue,
            onChange = { currentValue = it },
        )
    }
}

private fun submitZoomX(action: SettingsMacrosAction, zoomX: String) {
    val value = zoomX.toFloatOrNull() ?: return
    if (!value.isFinite()) {
        return
    }
    action.zoomX = maxOf(value, minZoomX)
}

@Composable
private fun ActionView(
    model: Model = LocalModel.current,
    database: Database,
    macros: SettingsMacros,
    macro: SettingsMacrosMacro,
    action: SettingsMacrosAction,
    ifBars: List<MacroActionIfBar>,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val rowInfo = remember { FormRowInfo() }
    ContextMenuDeleteButton(action = {
        macro.actions = macro.actions.filterNot { it === action }
    }) {
        Box(
            modifier = Modifier
                .layoutId(rowInfo)
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            ActionIfBarsView(bars = ifBars)
            NavigationLink(
                destination = {
                    ActionDestinationView(
                        model = model,
                        database = database,
                        macros = macros,
                        macro = macro,
                        action = action,
                        onNavigate = onNavigate,
                    )
                },
            ) {
                DraggableItemPrefixView()
                Text(action.function?.toString() ?: localized("-- None --"))
                when (action.function) {
                    SettingsMacrosActionFunction.SCENE -> {
                        val sceneName = getSceneName(database = database, id = action.sceneId)
                        if (sceneName != null) {
                            Spacer(modifier = Modifier.weight(1f))
                            GrayTextView(text = sceneName)
                        }
                    }
                    SettingsMacrosActionFunction.ENABLE_DISABLE_SCENES -> {
                        Spacer(modifier = Modifier.weight(1f))
                        GrayTextView(text = action.sceneIds.size.toString())
                    }
                    SettingsMacrosActionFunction.AUTO_SCENE_SWITCHER -> {
                        val switcherName = database.autoSceneSwitchers.switchers
                            .firstOrNull { it.id == action.autoSceneSwitcherId }
                            ?.name
                        if (switcherName != null) {
                            Spacer(modifier = Modifier.weight(1f))
                            GrayTextView(text = switcherName)
                        }
                    }
                    SettingsMacrosActionFunction.ZOOM -> {
                        Spacer(modifier = Modifier.weight(1f))
                        GrayTextView(text = formatOneDecimal(action.zoomX))
                    }
                    SettingsMacrosActionFunction.GIMBAL_PRESET -> {
                        val presetName = database.gimbal.presets
                            .firstOrNull { it.id == action.gimbalPresetId }
                            ?.name
                        if (presetName != null) {
                            Spacer(modifier = Modifier.weight(1f))
                            GrayTextView(text = presetName)
                        }
                    }
                    SettingsMacrosActionFunction.SEND_CHAT_MESSAGE -> {
                        Spacer(modifier = Modifier.weight(1f))
                        GrayTextView(text = action.chatMessage)
                    }
                    SettingsMacrosActionFunction.DELAY -> {
                        Spacer(modifier = Modifier.weight(1f))
                        GrayTextView(text = "${action.delay.toInt()}s")
                    }
                    SettingsMacrosActionFunction.DJI_DEVICES -> {
                        Spacer(modifier = Modifier.weight(1f))
                        GrayTextView(text = action.djiDevices.size.toString())
                    }
                    SettingsMacrosActionFunction.FILTERS -> {
                        Spacer(modifier = Modifier.weight(1f))
                        GrayTextView(text = action.filters.size.toString())
                    }
                    SettingsMacrosActionFunction.RECORD -> {
                        Spacer(modifier = Modifier.weight(1f))
                        GrayTextView(
                            text = if (action.record) localized("Start") else localized("Stop"),
                        )
                    }
                    SettingsMacrosActionFunction.MUTE -> {
                        Spacer(modifier = Modifier.weight(1f))
                        GrayTextView(
                            text = if (action.mute) localized("On") else localized("Off"),
                        )
                    }
                    SettingsMacrosActionFunction.TORCH -> {
                        Spacer(modifier = Modifier.weight(1f))
                        GrayTextView(
                            text = if (action.torch) localized("On") else localized("Off"),
                        )
                    }
                    SettingsMacrosActionFunction.SNAPSHOT,
                    SettingsMacrosActionFunction.SEND_TWITCH_SHOUTOUT,
                    -> {}
                    SettingsMacrosActionFunction.REACTION -> {
                        Spacer(modifier = Modifier.weight(1f))
                        GrayTextView(text = action.reaction.toString())
                    }
                    SettingsMacrosActionFunction.WAIT_FOR_EVENT -> {
                        Spacer(modifier = Modifier.weight(1f))
                        val sceneName = if (action.event == SettingsMacrosEvent.SWITCH_SCENE) {
                            getSceneName(database = database, id = action.eventSceneId)
                        } else {
                            null
                        }
                        when {
                            sceneName != null -> {
                                GrayTextView(text = "${action.event.toString()} ($sceneName)")
                            }
                            action.eventText.isNotEmpty() -> {
                                GrayTextView(text = "${action.event.toString()} (${action.eventText})")
                            }
                            else -> {
                                GrayTextView(text = action.event.toString())
                            }
                        }
                    }
                    SettingsMacrosActionFunction.IF_CONDITION -> {
                        Spacer(modifier = Modifier.weight(1f))
                        GrayTextView(
                            text = "${action.ifValue} ${action.ifComparison.toString()} ${action.ifOtherValue}",
                        )
                    }
                    SettingsMacrosActionFunction.MACRO -> {
                        val macroName = macros.macros.firstOrNull { it.id == action.macroId }?.name
                        if (macroName != null) {
                            Spacer(modifier = Modifier.weight(1f))
                            GrayTextView(text = macroName)
                        }
                    }
                    null -> {}
                }
            }
        }
    }
}

@Composable
fun ActionDestinationView(
    model: Model = LocalModel.current,
    database: Database,
    macros: SettingsMacros,
    macro: SettingsMacrosMacro,
    action: SettingsMacrosAction,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = localized("Action")) {
        Section(
            footerContent = {
                when (action.function) {
                    SettingsMacrosActionFunction.WAIT_FOR_EVENT -> {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            when (action.event) {
                                SettingsMacrosEvent.TWITCH_REWARD, SettingsMacrosEvent.KICK_REWARD -> {
                                    Text(
                                        localized(
                                            "Wait until a viewer redeems the reward, then continue " +
                                                "with the following actions. Leave reward empty to " +
                                                "wait for any reward.",
                                        ),
                                    )
                                }
                                else -> {
                                    Text(
                                        localized(
                                            "Wait until the event happens, then continue with the " +
                                                "following actions.",
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    SettingsMacrosActionFunction.SEND_TWITCH_SHOUTOUT -> {
                        Text(
                            localized(
                                "Send a Twitch shoutout to the channel in {twitchRaidChannelId}, " +
                                    "typically set by an earlier Twitch raid event.",
                            ),
                        )
                    }
                    SettingsMacrosActionFunction.IF_CONDITION -> {
                        Text(
                            localized(
                                "Run given number of following actions if the condition is met.",
                            ),
                        )
                    }
                    else -> {}
                }
            },
        ) {
            Picker(
                title = localized("Function"),
                selection = action.function,
                options = listOf<SettingsMacrosActionFunction?>(null) +
                    SettingsMacrosActionFunction.entries,
                text = { it?.toString() ?: localized("-- None --") },
                onChange = { action.function = it },
            )
            when (action.function) {
                SettingsMacrosActionFunction.SCENE -> {
                    Picker(
                        title = localized("Scene"),
                        selection = action.sceneId,
                        options = listOf<UUID?>(null) + database.scenes.map { it.id },
                        text = { id ->
                            if (id == null) {
                                localized("-- None --")
                            } else {
                                getSceneName(database = database, id = id)
                                    ?: localized("-- None --")
                            }
                        },
                        onChange = { action.sceneId = it },
                    )
                }
                SettingsMacrosActionFunction.ENABLE_DISABLE_SCENES -> {
                    database.scenes.forEach { scene ->
                        key(scene.id) {
                            Toggle(
                                title = scene.name,
                                isOn = isSelected(action.sceneIds, scene.id),
                                onChange = {
                                    action.sceneIds = setSelected(action.sceneIds, scene.id, it)
                                },
                            )
                        }
                    }
                }
                SettingsMacrosActionFunction.AUTO_SCENE_SWITCHER -> {
                    val switchers = database.autoSceneSwitchers.switchers
                    Picker(
                        title = localized("Auto scene switcher"),
                        selection = action.autoSceneSwitcherId,
                        options = listOf<UUID?>(null) + switchers.map { it.id },
                        text = { id ->
                            if (id == null) {
                                localized("-- None --")
                            } else {
                                switchers.firstOrNull { it.id == id }?.name
                                    ?: localized("-- None --")
                            }
                        },
                        onChange = { action.autoSceneSwitcherId = it },
                    )
                }
                SettingsMacrosActionFunction.ZOOM -> {
                    TextEditNavigationView(
                        title = localized("X"),
                        value = action.zoomX.toString(),
                        onSubmit = { submitZoomX(action = action, zoomX = it) },
                        keyboardType = KeyboardType.Decimal,
                    )
                }
                SettingsMacrosActionFunction.GIMBAL_PRESET -> {
                    val presets = database.gimbal.presets
                    Picker(
                        title = localized("Preset"),
                        selection = action.gimbalPresetId,
                        options = listOf<UUID?>(null) + presets.map { it.id },
                        text = { id ->
                            if (id == null) {
                                localized("-- None --")
                            } else {
                                presets.firstOrNull { it.id == id }?.name
                                    ?: localized("-- None --")
                            }
                        },
                        onChange = { action.gimbalPresetId = it },
                    )
                }
                SettingsMacrosActionFunction.SEND_CHAT_MESSAGE -> {
                    NavigationLink(
                        destination = {
                            TextFormatView(
                                model = model,
                                title = localized("Message"),
                                suggestions = true,
                                text = action.chatMessage,
                                onTextChange = { action.chatMessage = it },
                                value = action.chatMessage,
                            )
                        },
                    ) {
                        TextItemLocalizedView(
                            name = "Message",
                            value = action.chatMessage,
                        )
                    }
                }
                SettingsMacrosActionFunction.SEND_TWITCH_SHOUTOUT -> {}
                SettingsMacrosActionFunction.DELAY -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(localized("Delay"))
                        FormSlider(
                            value = action.delay.toFloat(),
                            onValueChange = { action.delay = it.toDouble() },
                            modifier = Modifier.weight(1f),
                            valueRange = 1f..60f,
                        )
                        Text("${action.delay.toInt()}s")
                    }
                }
                SettingsMacrosActionFunction.DJI_DEVICES -> {
                    database.djiDevices.devices.forEach { device ->
                        key(device.id) {
                            Toggle(
                                title = device.name,
                                isOn = isSelected(action.djiDevices, device.id),
                                onChange = {
                                    action.djiDevices = setSelected(action.djiDevices, device.id, it)
                                },
                            )
                        }
                    }
                }
                SettingsMacrosActionFunction.RECORD -> {
                    Picker(
                        title = localized("Record"),
                        selection = action.record,
                        options = listOf(true, false),
                        text = { if (it) localized("Start") else localized("Stop") },
                        onChange = { action.record = it },
                    )
                }
                SettingsMacrosActionFunction.MUTE -> {
                    Picker(
                        title = localized("Mute"),
                        selection = action.mute,
                        options = listOf(true, false),
                        text = { if (it) localized("On") else localized("Off") },
                        onChange = { action.mute = it },
                    )
                }
                SettingsMacrosActionFunction.TORCH -> {
                    Picker(
                        title = localized("Torch"),
                        selection = action.torch,
                        options = listOf(true, false),
                        text = { if (it) localized("On") else localized("Off") },
                        onChange = { action.torch = it },
                    )
                }
                SettingsMacrosActionFunction.SNAPSHOT -> {}
                SettingsMacrosActionFunction.FILTERS -> {
                    SettingsQuickButtonType.filters().forEach { filter ->
                        Toggle(
                            title = filter.toString(),
                            isOn = isSelected(action.filters, filter),
                            onChange = {
                                action.filters = setSelected(action.filters, filter, it)
                            },
                        )
                    }
                }
                SettingsMacrosActionFunction.REACTION -> {
                    Picker(
                        title = localized("Reaction"),
                        selection = action.reaction,
                        options = SettingsReaction.entries.toList(),
                        text = { it.toString() },
                        onChange = { action.reaction = it },
                    )
                }
                SettingsMacrosActionFunction.WAIT_FOR_EVENT -> {
                    Picker(
                        title = localized("Event"),
                        selection = action.event,
                        options = SettingsMacrosEvent.entries.toList(),
                        text = { it.toString() },
                        onChange = { action.event = it },
                    )
                    action.event.minimumAmountTitle()?.let { title ->
                        TextEditNavigationView(
                            title = title,
                            value = action.eventMinimumAmount.toString(),
                            onSubmit = { action.eventMinimumAmount = it.toIntOrNull() ?: 0 },
                            keyboardType = KeyboardType.Number,
                        )
                    }
                    action.event.textTitle()?.let { title ->
                        TextEditNavigationView(
                            title = title,
                            value = action.eventText,
                            onSubmit = { action.eventText = it },
                            keyboardType = KeyboardType.Text,
                        )
                    }
                    if (action.event == SettingsMacrosEvent.SWITCH_SCENE) {
                        Picker(
                            title = localized("Scene"),
                            selection = action.eventSceneId,
                            options = listOf<UUID?>(null) + database.scenes.map { it.id },
                            text = { id ->
                                if (id == null) {
                                    localized("-- Any --")
                                } else {
                                    getSceneName(database = database, id = id)
                                        ?: localized("-- Any --")
                                }
                            },
                            onChange = { action.eventSceneId = it },
                        )
                    }
                }
                SettingsMacrosActionFunction.IF_CONDITION -> {
                    NavigationLink(
                        destination = {
                            TextFormatView(
                                model = model,
                                title = localized("Value"),
                                suggestions = false,
                                text = action.ifValue,
                                onTextChange = { action.ifValue = it },
                                value = action.ifValue,
                            )
                        },
                    ) {
                        TextItemLocalizedView(
                            name = "Value",
                            value = action.ifValue,
                        )
                    }
                    Picker(
                        title = localized("Comparison"),
                        selection = action.ifComparison,
                        options = SettingsMacrosActionIfComparison.entries.toList(),
                        text = { it.toString() },
                        onChange = { action.ifComparison = it },
                    )
                    NavigationLink(
                        destination = {
                            TextFormatView(
                                model = model,
                                title = localized("Other value"),
                                suggestions = false,
                                text = action.ifOtherValue,
                                onTextChange = { action.ifOtherValue = it },
                                value = action.ifOtherValue,
                            )
                        },
                    ) {
                        TextItemLocalizedView(
                            name = "Other value",
                            value = action.ifOtherValue,
                        )
                    }
                    Picker(
                        title = localized("Actions to run"),
                        selection = action.ifRunCount,
                        options = (1..10).toList(),
                        text = { it.toString() },
                        onChange = { action.ifRunCount = it },
                    )
                }
                SettingsMacrosActionFunction.MACRO -> {
                    val availableMacros = macros.macros
                    Picker(
                        title = localized("Macro"),
                        selection = action.macroId,
                        options = listOf<UUID?>(null) + availableMacros.map { it.id },
                        text = { id ->
                            if (id == null) {
                                localized("-- None --")
                            } else {
                                availableMacros.firstOrNull { it.id == id }?.name
                                    ?: localized("-- None --")
                            }
                        },
                        onChange = { action.macroId = it },
                    )
                }
                null -> {}
            }
        }
    }
}

@Composable
private fun MacroView(
    model: Model = LocalModel.current,
    database: Database,
    macros: SettingsMacros,
    macro: SettingsMacrosMacro,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    ContextMenuDeleteButton(action = {
        model.stopMacro(macro = macro)
        macros.macros = macros.macros.filterNot { it === macro }
        model.remoteControlMacrosStateChanged()
    }) {
        NavigationLink(
            destination = {
                MacroDestinationView(
                    model = model,
                    database = database,
                    macros = macros,
                    macro = macro,
                    onNavigate = onNavigate,
                )
            },
        ) {
            Text(macro.name)
        }
    }
}

@Composable
fun MacroDestinationView(
    model: Model = LocalModel.current,
    database: Database,
    macros: SettingsMacros,
    macro: SettingsMacrosMacro,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = localized("Macro")) {
        Section {
            NameEditView(
                name = macro.name,
                onNameChange = {
                    macro.name = it
                    model.remoteControlMacrosStateChanged()
                },
                existingNames = macros.macros,
            )
        }
        Section(
            header = localized("Actions"),
            footerContent = {
                SwipeLeftToDeleteHelpView(kind = localized("an action"))
            },
        ) {
            val ifBars = macroActionIfBars(actions = macro.actions)
            ForEach(
                macro.actions.withIndex().toList(),
                id = { it.value.id },
                onDelete = { offsets ->
                    macro.actions = macro.actions.removing(atOffsets = offsets)
                },
                onMove = { froms, to ->
                    macro.actions = macro.actions.moving(fromOffsets = froms, toOffset = to)
                },
            ) { (index, action) ->
                ActionView(
                    model = model,
                    database = database,
                    macros = macros,
                    macro = macro,
                    action = action,
                    ifBars = ifBars[index],
                    onNavigate = onNavigate,
                )
            }
            CreateButtonView {
                macro.actions = macro.actions + SettingsMacrosAction()
            }
        }
        Section {
            Picker(
                title = localized("Repeat"),
                selection = macro.repeatMode,
                options = SettingsMacrosMacroRepeatMode.entries.toList(),
                text = { it.toString() },
                onChange = { macro.repeatMode = it },
            )
            if (macro.repeatMode == SettingsMacrosMacroRepeatMode.COUNT) {
                TextEditNavigationView(
                    title = localized("Count"),
                    value = macro.repeatCount.toString(),
                    onSubmit = { value ->
                        val count = value.toIntOrNull() ?: return@TextEditNavigationView
                        macro.repeatCount = count.coerceIn(1, 1_000_000)
                    },
                    keyboardType = KeyboardType.Number,
                )
            }
        }
        Section(
            footerContent = {
                Text(
                    localized(
                        "Run at app start is useful for macros that wait for events, for example " +
                            "to run actions when someone follows. Set repeat to forever to wait " +
                            "again after each event.",
                    ),
                )
            },
        ) {
            Toggle(
                title = localized("Close macros panel on run"),
                isOn = macro.closePanelOnRun,
                onChange = { macro.closePanelOnRun = it },
            )
            Toggle(
                title = localized("Run at app start"),
                isOn = macro.runAtAppStart,
                onChange = { macro.runAtAppStart = it },
            )
        }
        Section {
            if (macro.running) {
                CompositionLocalProvider(LocalTint provides formPalette().red) {
                    TextButtonView(localized("Cancel")) {
                        model.stopMacro(macro = macro)
                    }
                }
            } else if (macro.finished) {
                Text(
                    text = localized("Finished"),
                    color = formPalette().green,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            } else {
                TextButtonView(localized("Run")) {
                    model.startMacro(macro = macro)
                }
            }
        }
    }
}

@Composable
fun MacrosSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    macros: SettingsMacros,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = localized("Macros")) {
        Section {
            Text(
                localized(
                    "A macro is a sequence of actions that can change settings, filters, etc. " +
                        "with a single button tap. It can also wait for events, for example new " +
                        "followers, before continuing.",
                ),
            )
        }
        Section(
            footerContent = {
                SwipeLeftToDeleteHelpView(kind = localized("a macro"))
            },
        ) {
            ForEach(
                macros.macros,
                id = { it.id },
                onDelete = { offsets ->
                    for (offset in offsets) {
                        model.stopMacro(macro = macros.macros[offset])
                    }
                    macros.macros = macros.macros.removing(atOffsets = offsets)
                    model.remoteControlMacrosStateChanged()
                },
                onMove = { froms, to ->
                    macros.macros = macros.macros.moving(fromOffsets = froms, toOffset = to)
                    model.remoteControlMacrosStateChanged()
                },
            ) { macro ->
                MacroView(
                    model = model,
                    database = database,
                    macros = macros,
                    macro = macro,
                    onNavigate = onNavigate,
                )
            }
            CreateButtonView {
                val macro = SettingsMacrosMacro()
                macro.name = makeUniqueName(
                    name = SettingsMacrosMacro.baseName,
                    existingNames = macros.macros,
                )
                macros.macros = macros.macros + macro
                model.remoteControlMacrosStateChanged()
            }
        }
    }
}
