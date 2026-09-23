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
import com.moblin.android.platform.swiftui.DeleteDisabled
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.IndexSet
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.platform.swiftui.moving
import com.moblin.android.platform.swiftui.removing
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.getWheelOfLuckEffect
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetWheelOfLuck
import com.moblin.android.various.settings.SettingsWidgetWheelOfLuckOption
import com.moblin.android.videoeffects.WheelOfLuckEffect
import com.moblin.android.view.utils.ContextMenuDeleteButton
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
                    onSubmit = {
                        options.text = it
                        updateEffect()
                    },
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DraggableItemPrefixView()
            Text(options.text)
            Spacer(modifier = Modifier.weight(1f))
            Text("${calcPercent()}%")
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
    val advanced = binding(get = { wheelOfLuck.advanced }, set = { wheelOfLuck.advanced = it })
    val text = wheelOfLuck.text

    fun updateEffect() {
        model.getWheelOfLuckEffect(widget.id)?.setSettings(wheelOfLuck)
    }

    fun deleteOption(offsets: IndexSet) {
        wheelOfLuck.options = wheelOfLuck.options.removing(atOffsets = offsets)
        wheelOfLuck.updateText()
        wheelOfLuck.updateTotalWeight()
        updateEffect()
    }

    if (advanced.value) {
        Section(header = "Options") {
            val deleteDisabled = wheelOfLuck.options.size < 2
            ForEach(
                wheelOfLuck.options,
                id = { it.id },
                onDelete = { deleteOption(it) },
                onMove = { froms, to ->
                    wheelOfLuck.options = wheelOfLuck.options.moving(fromOffsets = froms, toOffset = to)
                    wheelOfLuck.updateText()
                    updateEffect()
                },
            ) { option ->
                DeleteDisabled(deleteDisabled) {
                    ContextMenuDeleteButton(
                        disabled = deleteDisabled,
                        action = {
                            val offset = wheelOfLuck.options.indexOfFirst { it.id == option.id }
                            if (offset != -1) {
                                deleteOption(setOf(offset))
                            }
                        },
                    ) {
                        OptionView(
                            model = model,
                            widget = widget,
                            wheelOfLuck = wheelOfLuck,
                            options = option,
                            onNavigate = onNavigate,
                        )
                    }
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
        Toggle("Advanced", isOn = advanced)
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
