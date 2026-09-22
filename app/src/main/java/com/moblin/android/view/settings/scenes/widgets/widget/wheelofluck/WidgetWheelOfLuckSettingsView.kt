package com.moblin.android.view.settings.scenes.widgets.widget.wheelofluck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.iconWidth
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetWheelOfLuck
import com.moblin.android.various.settings.SettingsWidgetWheelOfLuckOption
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.videoeffects.WheelOfLuckEffect
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.MultiLineTextFieldDoneButtonView
import com.moblin.android.view.utils.MultiLineTextFieldView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

val wheelOfLuckOptionWeights = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 40, 60, 80, 100)

@Composable
private fun WheelOfLuckWidgetView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    effect: WheelOfLuckEffect,
    indented: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (indented) {
            Text("")
            Text("", modifier = Modifier.width(iconWidth.dp))
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = {
            widget.wheelOfLuck.shuffle()
            model.getWheelOfLuckEffect(widget.id)?.setSettings(widget.wheelOfLuck)
        }) {
            Icon(
                Icons.Filled.Shuffle,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
        }
        IconButton(onClick = {
            effect.spin()
        }) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    wheelOfLuck: SettingsWidgetWheelOfLuck,
    options: SettingsWidgetWheelOfLuckOption,
    deleteDisabled: Boolean,
    onDelete: () -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val optionsText by options.text.collectAsState()
    val weight by options.weight.collectAsState()
    var showDestination by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    fun updateEffect() {
        model.getWheelOfLuckEffect(widget.id)?.setSettings(wheelOfLuck)
    }

    fun calcPercent(): Int {
        return 100 * weight / wheelOfLuck.totalWeight
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onNavigate("Option")
                showDestination = true
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DraggableItemPrefixView()
        Text(optionsText)
        Spacer(modifier = Modifier.weight(1f))
        Text("${calcPercent()}%")
        IconButton(onClick = onDelete, enabled = !deleteDisabled) {
            Icon(Icons.Filled.Delete, contentDescription = null)
        }
    }

    if (showDestination) {
        ModalBottomSheet(onDismissRequest = { showDestination = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(localized("Option"), style = MaterialTheme.typography.titleMedium)
                TextEditNavigationView(
                    title = localized("Text"),
                    value = optionsText,
                    onChange = {
                        options.text.value = it
                        updateEffect()
                    },
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = weight.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(localized("Weight")) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        wheelOfLuckOptionWeights.forEach { weightOption ->
                            DropdownMenuItem(
                                text = { Text(weightOption.toString()) },
                                onClick = {
                                    options.weight.value = weightOption
                                    wheelOfLuck.updateTotalWeight()
                                    updateEffect()
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetWheelOfLuckQuickButtonControlsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
) {
    model.getWheelOfLuckEffect(widget.id)?.let { effect ->
        WheelOfLuckWidgetView(
            model = model,
            widget = widget,
            effect = effect,
            indented = true,
        )
    }
}

@Composable
fun WheelOfLuckWidgetOptionsView(
    value: String,
    onChange: (String) -> Unit,
) {
    var editingText by remember { mutableStateOf(false) }

    Column {
        Text(localized("Options"), style = MaterialTheme.typography.titleSmall)
        MultiLineTextFieldView(
            value = value,
            onChange = onChange,
            placeholder = localized("My text"),
        )
        MultiLineTextFieldDoneButtonView(
            editingText = editingText,
            onChange = { editingText = it },
        )
    }
}

@Composable
fun WidgetWheelOfLuckSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    wheelOfLuck: SettingsWidgetWheelOfLuck,
    onNavigate: (String) -> Unit = {},
) {
    val advanced by wheelOfLuck.advanced.collectAsState()
    val text by wheelOfLuck.text.collectAsState()
    val options by wheelOfLuck.options.collectAsState()

    fun updateEffect() {
        model.getWheelOfLuckEffect(widget.id)?.setSettings(wheelOfLuck)
    }

    fun deleteOption(offsets: Set<Int>) {
        wheelOfLuck.options.value = wheelOfLuck.options.value
            .filterIndexed { index, _ -> index !in offsets }
        wheelOfLuck.updateText()
        wheelOfLuck.updateTotalWeight()
        updateEffect()
    }

    fun moveOptions(froms: Set<Int>, to: Int) {
        val moved = wheelOfLuck.options.value.toMutableList()
        val sortedFroms = froms.sorted()
        val moving = sortedFroms.map { moved[it] }
        sortedFroms.sortedDescending().forEach { moved.removeAt(it) }
        val insertAt = (to - sortedFroms.count { it < to }).coerceIn(0, moved.size)
        moved.addAll(insertAt, moving)
        wheelOfLuck.options.value = moved
        wheelOfLuck.updateText()
        updateEffect()
    }

    Column {
        if (advanced) {
            Column {
                Text(localized("Options"), style = MaterialTheme.typography.titleSmall)
                options.forEach { option ->
                    OptionView(
                        model = model,
                        widget = widget,
                        wheelOfLuck = wheelOfLuck,
                        options = option,
                        deleteDisabled = options.size < 2,
                        onDelete = {
                            makeOffsets(options, option.id)?.let { offsets ->
                                deleteOption(offsets)
                            }
                        },
                        onNavigate = onNavigate,
                    )
                }
                CreateButtonView(onClick = {
                    wheelOfLuck.options.value =
                        wheelOfLuck.options.value + SettingsWidgetWheelOfLuckOption()
                    wheelOfLuck.updateText()
                    wheelOfLuck.updateTotalWeight()
                    updateEffect()
                })
            }
        } else {
            WheelOfLuckWidgetOptionsView(
                value = text,
                onChange = { wheelOfLuck.text.value = it },
            )
            LaunchedEffect(Unit) {
                wheelOfLuck.optionsFromText(text)
                updateEffect()
            }
            LaunchedEffect(text) {
                wheelOfLuck.optionsFromText(text)
                updateEffect()
            }
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(localized("Advanced"))
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = advanced,
                    onCheckedChange = { wheelOfLuck.advanced.value = it },
                )
            }
        }
        Column {
            model.getWheelOfLuckEffect(widget.id)?.let { effect ->
                WheelOfLuckWidgetView(
                    model = model,
                    widget = widget,
                    effect = effect,
                    indented = false,
                )
            }
        }
    }
}
