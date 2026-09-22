package com.moblin.android.view.settings.macros

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.localized
import com.moblin.android.various.model.Model
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
import com.moblin.android.view.settings.scenes.SceneNameView
import com.moblin.android.view.settings.scenes.widgets.widget.text.TextFormatVariablesView
import com.moblin.android.view.settings.scenes.widgets.widget.text.TextFormatWarningsView
import com.moblin.android.view.settings.scenes.widgets.widget.text.TextWidgetSuggestionsView
import com.moblin.android.view.settings.scenes.widgets.widget.text.TextWidgetTextView
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private fun <T> isSelected(values: Set<T>, type: T): Boolean {
    return values.contains(type)
}

private fun <T> setSelected(values: MutableSet<T>, type: T, selected: Boolean) {
    if (selected) {
        values.add(type)
    } else {
        values.remove(type)
    }
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
    Color.Blue,
    Color(0xFF800080L),
    Color(0xFFFFA500L),
    Color(0xFF008080L),
)

@Composable
private fun Toggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> MacroPicker(
    title: String,
    selectedText: String,
    values: List<T>,
    optionContent: @Composable (T) -> Unit,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            values.forEach { value ->
                DropdownMenuItem(
                    text = { optionContent(value) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

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
            .padding(start = 3.dp)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
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
        model.macrosTextFormatChanged()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TextWidgetTextView(
            value = currentValue,
            onValueChange = { currentValue = it },
        )
        TextFormatWarningsView(
            model = model,
            location = model.database.location,
            value = currentValue,
            onValueChange = { currentValue = it },
        )
        if (suggestions) {
            TextWidgetSuggestionsView(
                widget = false,
                text = currentValue,
                onTextChange = { currentValue = it },
            )
        }
        TextFormatVariablesView(
            widget = false,
            value = currentValue,
            onValueChange = { currentValue = it },
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
    val contextMenuDeleteButton: () -> Unit = {
        TODO("contextMenuDeleteButton has no Compose counterpart")
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        ActionIfBarsView(bars = ifBars)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("Action") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DraggableItemPrefixView()
            Text(action.function?.toString() ?: localized("-- None --"))
            when (action.function) {
                SettingsMacrosActionFunction.SCENE -> {
                    val sceneName = model.getSceneName(action.sceneId)
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
                SettingsMacrosActionFunction.SNAPSHOT -> {}
                SettingsMacrosActionFunction.REACTION -> {
                    Spacer(modifier = Modifier.weight(1f))
                    GrayTextView(text = action.reaction.toString())
                }
                SettingsMacrosActionFunction.WAIT_FOR_EVENT -> {
                    Spacer(modifier = Modifier.weight(1f))
                    val sceneName = if (action.event == SettingsMacrosEvent.SWITCH_SCENE) {
                        model.getSceneName(action.eventSceneId)
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

@Composable
fun ActionDestinationView(
    model: Model = LocalModel.current,
    database: Database,
    macros: SettingsMacros,
    macro: SettingsMacrosMacro,
    action: SettingsMacrosAction,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    LaunchedEffect(action.function) {
        TODO("objectWillChange has no Android counterpart")
    }
    LaunchedEffect(action.ifRunCount) {
        TODO("objectWillChange has no Android counterpart")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        MacroPicker(
            title = localized("Function"),
            selectedText = action.function?.toString() ?: localized("-- None --"),
            values = listOf(null) + SettingsMacrosActionFunction.entries,
            optionContent = { Text(it?.toString() ?: localized("-- None --")) },
            onSelect = { action.function = it },
        )
        when (action.function) {
            SettingsMacrosActionFunction.SCENE -> {
                MacroPicker(
                    title = localized("Scene"),
                    selectedText = model.getSceneName(action.sceneId) ?: localized("-- None --"),
                    values = listOf(null) + database.scenes,
                    optionContent = { scene ->
                        if (scene == null) {
                            Text(localized("-- None --"))
                        } else {
                            SceneNameView(scene = scene)
                        }
                    },
                    onSelect = { action.sceneId = it?.id },
                )
            }
            SettingsMacrosActionFunction.ENABLE_DISABLE_SCENES -> {
                database.scenes.forEach { scene ->
                    Toggle(
                        label = scene.name,
                        checked = isSelected(action.sceneIds, scene.id),
                        onCheckedChange = { setSelected(action.sceneIds, scene.id, it) },
                    )
                }
            }
            SettingsMacrosActionFunction.AUTO_SCENE_SWITCHER -> {
                MacroPicker(
                    title = localized("Auto scene switcher"),
                    selectedText = database.autoSceneSwitchers.switchers
                        .firstOrNull { it.id == action.autoSceneSwitcherId }
                        ?.name
                        ?: localized("-- None --"),
                    values = listOf(null) + database.autoSceneSwitchers.switchers,
                    optionContent = { Text(it?.name ?: localized("-- None --")) },
                    onSelect = { action.autoSceneSwitcherId = it?.id },
                )
            }
            SettingsMacrosActionFunction.ZOOM -> {
                TextEditNavigationView(
                    title = localized("X"),
                    value = action.zoomX.toString(),
                    onSubmit = { submitZoomX(action = action, zoomX = it) },
                    keyboardType = KeyboardType.Number,
                )
            }
            SettingsMacrosActionFunction.GIMBAL_PRESET -> {
                MacroPicker(
                    title = localized("Preset"),
                    selectedText = database.gimbal.presets
                        .firstOrNull { it.id == action.gimbalPresetId }
                        ?.name
                        ?: localized("-- None --"),
                    values = listOf(null) + database.gimbal.presets,
                    optionContent = { Text(it?.name ?: localized("-- None --")) },
                    onSelect = { action.gimbalPresetId = it?.id },
                )
            }
            SettingsMacrosActionFunction.SEND_CHAT_MESSAGE -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("Message") },
                ) {
                    TextItemLocalizedView(
                        name = "Message",
                        value = action.chatMessage,
                    )
                }
            }
            SettingsMacrosActionFunction.DELAY -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(localized("Delay"))
                    Slider(
                        value = action.delay.toFloat(),
                        onValueChange = { action.delay = it.toDouble() },
                        valueRange = 1f..60f,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${action.delay.toInt()}s")
                }
            }
            SettingsMacrosActionFunction.DJI_DEVICES -> {
                database.djiDevices.devices.forEach { device ->
                    Toggle(
                        label = device.name,
                        checked = isSelected(action.djiDevices, device.id),
                        onCheckedChange = { setSelected(action.djiDevices, device.id, it) },
                    )
                }
            }
            SettingsMacrosActionFunction.RECORD -> {
                MacroPicker(
                    title = localized("Record"),
                    selectedText = if (action.record) localized("Start") else localized("Stop"),
                    values = listOf(true, false),
                    optionContent = { Text(if (it) localized("Start") else localized("Stop")) },
                    onSelect = { action.record = it },
                )
            }
            SettingsMacrosActionFunction.MUTE -> {
                MacroPicker(
                    title = localized("Mute"),
                    selectedText = if (action.mute) localized("On") else localized("Off"),
                    values = listOf(true, false),
                    optionContent = { Text(if (it) localized("On") else localized("Off")) },
                    onSelect = { action.mute = it },
                )
            }
            SettingsMacrosActionFunction.TORCH -> {
                MacroPicker(
                    title = localized("Torch"),
                    selectedText = if (action.torch) localized("On") else localized("Off"),
                    values = listOf(true, false),
                    optionContent = { Text(if (it) localized("On") else localized("Off")) },
                    onSelect = { action.torch = it },
                )
            }
            SettingsMacrosActionFunction.SNAPSHOT -> {}
            SettingsMacrosActionFunction.FILTERS -> {
                SettingsQuickButtonType.filters().forEach { filter ->
                    Toggle(
                        label = filter.toString(),
                        checked = isSelected(action.filters, filter),
                        onCheckedChange = { setSelected(action.filters, filter, it) },
                    )
                }
            }
            SettingsMacrosActionFunction.REACTION -> {
                MacroPicker(
                    title = localized("Reaction"),
                    selectedText = action.reaction.toString(),
                    values = SettingsReaction.entries.toList(),
                    optionContent = { Text(it.toString()) },
                    onSelect = { action.reaction = it },
                )
            }
            SettingsMacrosActionFunction.WAIT_FOR_EVENT -> {
                MacroPicker(
                    title = localized("Event"),
                    selectedText = action.event.toString(),
                    values = SettingsMacrosEvent.entries.toList(),
                    optionContent = { Text(it.toString()) },
                    onSelect = { action.event = it },
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
                    MacroPicker(
                        title = localized("Scene"),
                        selectedText = model.getSceneName(action.eventSceneId)
                            ?: localized("-- Any --"),
                        values = listOf(null) + database.scenes,
                        optionContent = { scene ->
                            if (scene == null) {
                                Text(localized("-- Any --"))
                            } else {
                                SceneNameView(scene = scene)
                            }
                        },
                        onSelect = { action.eventSceneId = it?.id },
                    )
                }
            }
            SettingsMacrosActionFunction.IF_CONDITION -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("Value") },
                ) {
                    TextItemLocalizedView(
                        name = "Value",
                        value = action.ifValue,
                    )
                }
                MacroPicker(
                    title = localized("Comparison"),
                    selectedText = action.ifComparison.toString(),
                    values = SettingsMacrosActionIfComparison.entries.toList(),
                    optionContent = { Text(it.toString()) },
                    onSelect = { action.ifComparison = it },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("Other value") },
                ) {
                    TextItemLocalizedView(
                        name = "Other value",
                        value = action.ifOtherValue,
                    )
                }
                MacroPicker(
                    title = localized("Actions to run"),
                    selectedText = action.ifRunCount.toString(),
                    values = (1..10).toList(),
                    optionContent = { Text(it.toString()) },
                    onSelect = { action.ifRunCount = it },
                )
            }
            SettingsMacrosActionFunction.MACRO -> {
                MacroPicker(
                    title = localized("Macro"),
                    selectedText = macros.macros
                        .firstOrNull { it.id == action.macroId }
                        ?.name
                        ?: localized("-- None --"),
                    values = listOf(null) + macros.macros,
                    optionContent = { Text(it?.name ?: localized("-- None --")) },
                    onSelect = { action.macroId = it?.id },
                )
            }
            null -> {}
        }
        when (action.function) {
            SettingsMacrosActionFunction.WAIT_FOR_EVENT -> {
                when (action.event) {
                    SettingsMacrosEvent.TWITCH_REWARD, SettingsMacrosEvent.KICK_REWARD -> {
                        Text(
                            localized(
                                "Wait until a viewer redeems the reward, then continue with the following actions. Leave reward empty to wait for any reward.",
                            ),
                        )
                    }
                    else -> {
                        Text(
                            localized(
                                "Wait until the event happens, then continue with the following actions.",
                            ),
                        )
                    }
                }
            }
            SettingsMacrosActionFunction.IF_CONDITION -> {
                Text(localized("Run given number of following actions if the condition is met."))
            }
            else -> {}
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
    val contextMenuDeleteButton: () -> Unit = {
        TODO("contextMenuDeleteButton has no Compose counterpart")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("Macro") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(macro.name)
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
    val onMove: (List<Int>, Int) -> Unit = { _, _ ->
        TODO("List onMove has no Compose counterpart")
    }
    val onDelete: (List<Int>) -> Unit = { offsets ->
        offsets.sortedDescending().forEach { macro.actions.removeAt(it) }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        NameEditView(
            name = macro.name,
            onNameChange = {
                macro.name = it
                model.remoteControlMacrosStateChanged()
            },
            existingNames = macros.macros,
        )
        Text(
            text = localized("Actions"),
            style = MaterialTheme.typography.titleSmall,
        )
        val ifBars = macroActionIfBars(actions = macro.actions)
        macro.actions.forEachIndexed { index, action ->
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
            macro.actions.add(SettingsMacrosAction())
        }
        SwipeLeftToDeleteHelpView(kind = localized("an action"))
        MacroPicker(
            title = localized("Repeat"),
            selectedText = macro.repeatMode.toString(),
            values = SettingsMacrosMacroRepeatMode.entries.toList(),
            optionContent = { Text(it.toString()) },
            onSelect = { macro.repeatMode = it },
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
        Toggle(
            label = localized("Close macros panel on run"),
            checked = macro.closePanelOnRun,
            onCheckedChange = { macro.closePanelOnRun = it },
        )
        Toggle(
            label = localized("Run at app start"),
            checked = macro.runAtAppStart,
            onCheckedChange = { macro.runAtAppStart = it },
        )
        Text(
            localized(
                "Run at app start is useful for macros that wait for events, for example to run actions when someone follows. Set repeat to forever to wait again after each event.",
            ),
        )
        if (macro.running) {
            TextButtonView(localized("Cancel")) {
                model.stopMacro(macro = macro)
            }
        } else if (macro.finished) {
            Text(
                text = localized("Finished"),
                color = Color.Green,
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

@Composable
fun MacrosSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    macros: SettingsMacros,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val onMove: (List<Int>, Int) -> Unit = { _, _ ->
        TODO("List onMove has no Compose counterpart")
    }
    val onDelete: (List<Int>) -> Unit = { offsets ->
        for (offset in offsets) {
            model.stopMacro(macro = macros.macros[offset])
        }
        offsets.sortedDescending().forEach { macros.macros.removeAt(it) }
        model.remoteControlMacrosStateChanged()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            localized(
                "A macro is a sequence of actions that can change settings, filters, etc. with a single button tap. It can also wait for events, for example new followers, before continuing.",
            ),
        )
        macros.macros.forEach { macro ->
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
            macros.macros.add(macro)
            model.remoteControlMacrosStateChanged()
        }
        SwipeLeftToDeleteHelpView(kind = localized("a macro"))
    }
}
