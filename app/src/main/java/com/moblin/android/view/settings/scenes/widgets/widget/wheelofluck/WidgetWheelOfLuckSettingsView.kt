package com.moblin.android.view.settings.scenes.widgets.widget.wheelofluck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.iconWidth
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.getWheelOfLuckEffect
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

val wheelOfLuckOptionWeights = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 40, 60, 80, 100)

@Composable
private fun SymbolButton(
    systemImage: String,
    fontSize: TextUnit,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    SystemImage(
        systemImage,
        fontSize = fontSize,
        modifier = Modifier
            .alpha(if (pressed && enabled) 0.2f else 1f)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
    )
}

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
        SymbolButton(systemImage = "shuffle", fontSize = 28.sp) {
            widget.wheelOfLuck.shuffle()
            model.getWheelOfLuckEffect(widget.id)?.setSettings(widget.wheelOfLuck)
        }
        SymbolButton(systemImage = "play", fontSize = 28.sp) {
            effect.spin()
        }
    }
}

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
    fun updateEffect() {
        model.getWheelOfLuckEffect(widget.id)?.setSettings(wheelOfLuck)
    }

    fun calcPercent(): Int {
        return 100 * options.weight / wheelOfLuck.totalWeight
    }

    NavigationLink(
        destination = {
            Form(title = "Option") {
                TextEditNavigationView(
                    title = localized("Text"),
                    value = options.text,
                    onChange = {
                        options.text = it
                        updateEffect()
                        null
                    },
                    onSubmit = {},
                )
                Picker(
                    title = "Weight",
                    selection = options.weight,
                    options = wheelOfLuckOptionWeights,
                    text = { it.toString() },
                    onChange = { weight ->
                        options.weight = weight
                        wheelOfLuck.updateTotalWeight()
                        updateEffect()
                    },
                )
            }
        },
    ) {
        DraggableItemPrefixView()
        Text(options.text)
        Spacer(modifier = Modifier.weight(1f))
        Text("${calcPercent()}%")
        SymbolButton(
            systemImage = "trash",
            fontSize = 17.sp,
            enabled = !deleteDisabled,
            onClick = onDelete,
        )
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

    Section(
        header = "Options",
        footerContent = {
            MultiLineTextFieldDoneButtonView(
                editingText = editingText,
                onEditingTextChange = { editingText = it },
            )
        },
    ) {
        MultiLineTextFieldView(
            value = value,
            onValueChange = onChange,
            placeholder = localized("My text"),
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
    val advanced = wheelOfLuck.advanced
    val text = wheelOfLuck.text
    val options = wheelOfLuck.options

    fun updateEffect() {
        model.getWheelOfLuckEffect(widget.id)?.setSettings(wheelOfLuck)
    }

    fun deleteOption(offset: Int) {
        wheelOfLuck.options = wheelOfLuck.options
            .filterIndexed { index, _ -> index != offset }
        wheelOfLuck.updateText()
        wheelOfLuck.updateTotalWeight()
        updateEffect()
    }

    fun moveOptions(froms: Set<Int>, to: Int) {
        val moved = wheelOfLuck.options.toMutableList()
        val sortedFroms = froms.sorted()
        val moving = sortedFroms.map { moved[it] }
        sortedFroms.sortedDescending().forEach { moved.removeAt(it) }
        val insertAt = (to - sortedFroms.count { it < to }).coerceIn(0, moved.size)
        moved.addAll(insertAt, moving)
        wheelOfLuck.options = moved
        wheelOfLuck.updateText()
        updateEffect()
    }

    if (advanced) {
        Section(header = "Options") {
            options.forEach { option ->
                key(option.id) {
                    OptionView(
                        model = model,
                        widget = widget,
                        wheelOfLuck = wheelOfLuck,
                        options = option,
                        deleteDisabled = options.size < 2,
                        onDelete = {
                            val offset = options.indexOfFirst { it.id == option.id }
                            if (offset != -1) {
                                deleteOption(offset)
                            }
                        },
                        onNavigate = onNavigate,
                    )
                }
            }
            CreateButtonView(action = {
                wheelOfLuck.options =
                    wheelOfLuck.options + SettingsWidgetWheelOfLuckOption()
                wheelOfLuck.updateText()
                wheelOfLuck.updateTotalWeight()
                updateEffect()
            })
        }
    } else {
        WheelOfLuckWidgetOptionsView(
            value = text,
            onChange = { newText ->
                wheelOfLuck.text = newText
                wheelOfLuck.optionsFromText(newText)
                updateEffect()
            },
        )
        LaunchedEffect(Unit) {
            wheelOfLuck.optionsFromText(wheelOfLuck.text)
            updateEffect()
        }
    }
    Section {
        Toggle("Advanced", isOn = advanced) { value ->
            wheelOfLuck.advanced = value
        }
    }
    Section {
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
