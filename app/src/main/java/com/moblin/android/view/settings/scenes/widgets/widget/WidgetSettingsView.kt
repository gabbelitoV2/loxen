package com.moblin.android.view.settings.scenes.widgets.widget

import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsAlignment
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetLayout
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.view.settings.scenes.widgets.widget.alerts.WidgetAlertsSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.bingocard.WidgetBingoCardSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.browser.WidgetBrowserSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.chat.WidgetChatSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.chatemotecombo.WidgetChatEmoteComboSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.crop.WidgetCropSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.image.WidgetImageSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.map.WidgetMapSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.pngtuber.WidgetPngTuberSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.pomodorotimer.WidgetPomodoroTimerSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.qrcode.WidgetQrCodeSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.scene.WidgetSceneSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.scoreboard.WidgetScoreboardSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.slideshow.WidgetSlideshowSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.snapshot.WidgetSnapshotSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.text.WidgetTextSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.WidgetVideoSourceSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.vtuber.WidgetVTuberSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.wheelofluck.WidgetWheelOfLuckSettingsView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.PositionEditView
import com.moblin.android.view.utils.SizeEditView
import com.moblin.android.various.model.sceneUpdated

@Composable
fun AlignmentOptionView(
    layout: MutableState<SettingsWidgetLayout>,
    alignment: SettingsAlignment
) {
    val interactionSource = remember { MutableInteractionSource() }
    SystemImage(
        name = if (layout.value.alignment == alignment) "square.fill" else "square",
        fontSize = 28.sp,
        modifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = null
        ) {
            layout.value = layout.value.copy(alignment = alignment)
        }
    )
}

@Composable
fun SaveLoadLayoutView(
    model: Model = LocalModel.current,
    layout: MutableState<SettingsWidgetLayout>
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FormButton(title = "Save layout", centered = true) {
            model.layout.value = layout.value
        }
        FormButton(
            title = "Load layout",
            centered = true,
            enabled = model.layout.value != null
        ) {
            layout.value = model.layout.value ?: layout.value
            model.sceneUpdated()
        }
    }
}

private fun dimensions(model: Model): Size {
    return model.stream.value.resolution.dimensions(portrait = model.stream.value.portrait)
}

private fun horizontalIncrement(model: Model): Double {
    return 100.0 / dimensions(model).width.toDouble()
}

private fun verticalIncrement(model: Model): Double {
    return 100.0 / dimensions(model).height.toDouble()
}

private fun setXBasedOnYIfLocked(layout: MutableState<SettingsWidgetLayout>, model: Model) {
    if (!layout.value.positioningLock) {
        return
    }
    val x = layout.value.y * horizontalIncrement(model) / verticalIncrement(model)
    layout.value = layout.value.copy(x = x, xString = x.toString())
}

private fun setYBasedOnXIfLocked(layout: MutableState<SettingsWidgetLayout>, model: Model) {
    if (!layout.value.positioningLock) {
        return
    }
    val y = layout.value.x * verticalIncrement(model) / horizontalIncrement(model)
    layout.value = layout.value.copy(y = y, yString = y.toString())
}

@Composable
private fun generalAndAlignmentPicker(
    model: Model = LocalModel.current,
    layout: MutableState<SettingsWidgetLayout>,
    widget: SettingsWidget
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SaveLoadLayoutView(model = model, layout = layout)
            Spacer(Modifier.weight(1f))
        }
        if (widget.hasAlignment()) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(112.dp)
                    .background(formPalette().separator)
            )
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    AlignmentOptionView(layout, SettingsAlignment.topLeft)
                    AlignmentOptionView(layout, SettingsAlignment.topCenter)
                    AlignmentOptionView(layout, SettingsAlignment.topRight)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    AlignmentOptionView(layout, SettingsAlignment.leftCenter)
                    AlignmentOptionView(layout, SettingsAlignment.center)
                    AlignmentOptionView(layout, SettingsAlignment.rightCenter)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    AlignmentOptionView(layout, SettingsAlignment.bottomLeft)
                    AlignmentOptionView(layout, SettingsAlignment.bottomCenter)
                    AlignmentOptionView(layout, SettingsAlignment.bottomRight)
                }
            }
            val previousAlignment = remember { mutableStateOf(layout.value.alignment) }
            LaunchedEffect(layout.value.alignment) {
                if (previousAlignment.value != layout.value.alignment) {
                    previousAlignment.value = layout.value.alignment
                    model.sceneUpdated()
                }
            }
        }
    }
}

@Composable
private fun horizontalAndVerticalPositioning(
    model: Model = LocalModel.current,
    layout: MutableState<SettingsWidgetLayout>,
    numericInput: MutableState<Boolean>
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PositionEditView(
                number = layout.value.x,
                value = layout.value.xString,
                onNumberChange = { layout.value = layout.value.copy(x = it) },
                onValueChange = { layout.value = layout.value.copy(xString = it) },
                onSubmit = {
                    setYBasedOnXIfLocked(layout, model)
                    model.sceneUpdated()
                },
                numericInput = numericInput.value,
                onNumericInputChange = { numericInput.value = it },
                incrementImageName = "arrow.forward.circle",
                decrementImageName = "arrow.backward.circle",
                mirror = layout.value.alignment.mirrorPositionHorizontally(),
                increment = horizontalIncrement(model)
            )
            Spacer(Modifier.height(10.dp))
            PositionEditView(
                number = layout.value.y,
                value = layout.value.yString,
                onNumberChange = { layout.value = layout.value.copy(y = it) },
                onValueChange = { layout.value = layout.value.copy(yString = it) },
                onSubmit = {
                    setXBasedOnYIfLocked(layout, model)
                    model.sceneUpdated()
                },
                numericInput = numericInput.value,
                onNumericInputChange = { numericInput.value = it },
                incrementImageName = "arrow.down.circle",
                decrementImageName = "arrow.up.circle",
                mirror = layout.value.alignment.mirrorPositionVertically(),
                increment = verticalIncrement(model)
            )
        }
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .width(35.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    layout.value = layout.value.copy(positioningLock = !layout.value.positioningLock)
                    setYBasedOnXIfLocked(layout, model)
                },
            contentAlignment = Alignment.Center
        ) {
            SystemImage(
                name = if (layout.value.positioningLock) "lock" else "lock.open",
                fontSize = 28.sp
            )
        }
    }
}

@Composable
private fun horizontalPositioning(
    model: Model = LocalModel.current,
    layout: MutableState<SettingsWidgetLayout>,
    numericInput: MutableState<Boolean>
) {
    PositionEditView(
        number = layout.value.x,
        value = layout.value.xString,
        onNumberChange = { layout.value = layout.value.copy(x = it) },
        onValueChange = { layout.value = layout.value.copy(xString = it) },
        onSubmit = {
            model.sceneUpdated()
        },
        numericInput = numericInput.value,
        onNumericInputChange = { numericInput.value = it },
        incrementImageName = "arrow.forward.circle",
        decrementImageName = "arrow.backward.circle",
        mirror = layout.value.alignment.mirrorPositionHorizontally(),
        increment = horizontalIncrement(model)
    )
}

@Composable
private fun verticalPositioning(
    model: Model = LocalModel.current,
    layout: MutableState<SettingsWidgetLayout>,
    numericInput: MutableState<Boolean>
) {
    PositionEditView(
        number = layout.value.y,
        value = layout.value.yString,
        onNumberChange = { layout.value = layout.value.copy(y = it) },
        onValueChange = { layout.value = layout.value.copy(yString = it) },
        onSubmit = {
            model.sceneUpdated()
        },
        numericInput = numericInput.value,
        onNumericInputChange = { numericInput.value = it },
        incrementImageName = "arrow.down.circle",
        decrementImageName = "arrow.up.circle",
        mirror = layout.value.alignment.mirrorPositionVertically(),
        increment = verticalIncrement(model)
    )
}

@Composable
fun WidgetLayoutView(
    model: Model = LocalModel.current,
    database: Database,
    layout: MutableState<SettingsWidgetLayout>,
    widget: SettingsWidget,
    numericInput: MutableState<Boolean>
) {
    if (widget.hasAlignment() || widget.hasPosition() || widget.hasSize()) {
        Section(
            header = "Layout",
            footer = "Use save/load layout to position a widget in the same place in " +
                "multiple scenes. Alternatively, use a Scene widget to easily show the " +
                "same widgets in multiple scenes."
        ) {
            generalAndAlignmentPicker(model = model, layout = layout, widget = widget)
            if (widget.hasPosition()) {
                if (!layout.value.alignment.isHorizontalCenter() &&
                    !layout.value.alignment.isVerticalCenter()
                ) {
                    horizontalAndVerticalPositioning(
                        model = model,
                        layout = layout,
                        numericInput = numericInput
                    )
                } else if (!layout.value.alignment.isHorizontalCenter()) {
                    horizontalPositioning(
                        model = model,
                        layout = layout,
                        numericInput = numericInput
                    )
                } else if (!layout.value.alignment.isVerticalCenter()) {
                    verticalPositioning(
                        model = model,
                        layout = layout,
                        numericInput = numericInput
                    )
                }
            }
            if (widget.hasSize()) {
                SizeEditView(
                    number = layout.value.size,
                    value = layout.value.sizeString,
                    onNumberChange = { layout.value = layout.value.copy(size = it) },
                    onValueChange = { layout.value = layout.value.copy(sizeString = it) },
                    onSubmit = {
                        model.sceneUpdated()
                    },
                    numericInput = numericInput.value,
                    onNumericInputChange = { numericInput.value = it }
                )
            }
            Toggle(
                "Numeric input",
                isOn = binding({ database.sceneNumericInput }) {
                    database.sceneNumericInput = it
                }
            )
        }
    }
}

@Composable
fun WidgetNameView(widget: SettingsWidget) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SystemImage(name = widget.image(), fontSize = 28.sp)
        Text(widget.name)
    }
}

@Composable
fun WidgetSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    widget: SettingsWidget
) {
    Form(title = "${widget.type} widget") {
        Section {
            NameEditView(
                name = widget.name,
                existingNames = database.widgets,
                onNameChange = { widget.name = it }
            )
        }
        when (widget.type) {
            SettingsWidgetType.image -> WidgetImageSettingsView(model = model, widget = widget)
            SettingsWidgetType.browser -> WidgetBrowserSettingsView(widget = widget, browser = widget.browser)
            SettingsWidgetType.text -> WidgetTextSettingsView(widget = widget, text = widget.text)
            SettingsWidgetType.crop -> WidgetCropSettingsView(widget = widget)
            SettingsWidgetType.map -> WidgetMapSettingsView(
                widget = widget,
                initialDelay = widget.map.delay,
                initialSize = widget.map.size
            )
            SettingsWidgetType.scene -> WidgetSceneSettingsView(
                widget = widget,
                selectedSceneId = widget.scene.sceneId
            )
            SettingsWidgetType.slideshow -> WidgetSlideshowSettingsView(widget = widget)
            SettingsWidgetType.qrCode -> WidgetQrCodeSettingsView(model = model, widget = widget)
            SettingsWidgetType.alerts -> WidgetAlertsSettingsView(model = model, widget = widget)
            SettingsWidgetType.videoSource -> WidgetVideoSourceSettingsView(
                widget = widget,
                videoSource = widget.videoSource
            )
            SettingsWidgetType.scoreboard -> WidgetScoreboardSettingsView(
                model = model,
                widget = widget,
                scoreboard = widget.scoreboard,
                web = database.remoteControl.web
            )
            SettingsWidgetType.vTuber -> WidgetVTuberSettingsView(
                model = model,
                widget = widget,
                vTuber = widget.vTuber
            )
            SettingsWidgetType.pngTuber -> WidgetPngTuberSettingsView(
                model = model,
                widget = widget,
                pngTuber = widget.pngTuber
            )
            SettingsWidgetType.snapshot -> WidgetSnapshotSettingsView(
                model = model,
                widget = widget,
                snapshot = widget.snapshot
            )
            SettingsWidgetType.chat -> WidgetChatSettingsView(
                model = model,
                database = database,
                widget = widget,
                chat = widget.chat
            )
            SettingsWidgetType.chatEmoteCombo -> WidgetChatEmoteComboSettingsView(
                model = model,
                widget = widget,
                chatEmoteCombo = widget.chatEmoteCombo
            )
            SettingsWidgetType.wheelOfLuck -> WidgetWheelOfLuckSettingsView(
                model = model,
                widget = widget,
                wheelOfLuck = widget.wheelOfLuck
            )
            SettingsWidgetType.bingoCard -> WidgetBingoCardSettingsView(
                model = model,
                widget = widget,
                bingoCard = widget.bingoCard
            )
            SettingsWidgetType.pomodoroTimer -> WidgetPomodoroTimerSettingsView(
                model = model,
                pomodoroTimer = widget.pomodoroTimer
            )
        }
    }
}
