package com.moblin.android.view.settings.scenes.widgets.widget

import android.util.Size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Button
import com.moblin.android.platform.swiftui.Divider
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.sceneUpdated
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
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.PositionEditView
import com.moblin.android.view.utils.SizeEditView

@Composable
fun AlignmentOptionView(layout: MutableState<SettingsWidgetLayout>, alignment: SettingsAlignment) {
    Button(
        action = {
            layout.value = layout.value.copy(alignment = alignment)
        },
    ) {
        SystemImage(
            name = if (layout.value.alignment == alignment) "square.fill" else "square",
            fontSize = 28.sp,
        )
    }
}

@Composable
fun SaveLoadLayoutView(
    model: Model = LocalModel.current,
    layout: MutableState<SettingsWidgetLayout>,
) {
    Column(horizontalAlignment = Alignment.Start) {
        Spacer(Modifier.weight(1f))
        Button(
            action = {
                model.layout.value = layout.value
            },
        ) {
            HCenter {
                Text("Save layout")
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            action = {
                layout.value = model.layout.value ?: layout.value
                model.sceneUpdated()
            },
            enabled = model.layout.value != null,
        ) {
            Row {
                Text("")
                Spacer(Modifier.weight(1f))
                Text("Load layout")
                Spacer(Modifier.weight(1f))
                Text("")
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

private fun dimensions(model: Model): Size =
    model.stream.value.resolution.dimensions(portrait = model.stream.value.portrait)

private fun horizontalIncrement(model: Model): Double = 100 / dimensions(model).width.toDouble()

private fun verticalIncrement(model: Model): Double = 100 / dimensions(model).height.toDouble()

private fun setXBasedOnYIfLocked(model: Model, layout: MutableState<SettingsWidgetLayout>) {
    if (!layout.value.positioningLock) {
        return
    }
    layout.value = layout.value.copy(
        x = layout.value.y * horizontalIncrement(model) / verticalIncrement(model),
    ).updatingXString()
}

private fun setYBasedOnXIfLocked(model: Model, layout: MutableState<SettingsWidgetLayout>) {
    if (!layout.value.positioningLock) {
        return
    }
    layout.value = layout.value.copy(
        y = layout.value.x * verticalIncrement(model) / horizontalIncrement(model),
    ).updatingYString()
}

@Composable
private fun generalAndAlignmentPicker(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    layout: MutableState<SettingsWidgetLayout>,
) {
    Row {
        Row {
            SaveLoadLayoutView(model = model, layout = layout)
            Spacer(Modifier.weight(1f))
        }
        if (widget.hasAlignment()) {
            Divider()
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    AlignmentOptionView(layout = layout, alignment = SettingsAlignment.topLeft)
                    AlignmentOptionView(layout = layout, alignment = SettingsAlignment.topCenter)
                    AlignmentOptionView(layout = layout, alignment = SettingsAlignment.topRight)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    AlignmentOptionView(layout = layout, alignment = SettingsAlignment.leftCenter)
                    AlignmentOptionView(layout = layout, alignment = SettingsAlignment.center)
                    AlignmentOptionView(layout = layout, alignment = SettingsAlignment.rightCenter)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    AlignmentOptionView(layout = layout, alignment = SettingsAlignment.bottomLeft)
                    AlignmentOptionView(layout = layout, alignment = SettingsAlignment.bottomCenter)
                    AlignmentOptionView(layout = layout, alignment = SettingsAlignment.bottomRight)
                }
            }
            LaunchedEffect(layout.value.alignment) {
                model.sceneUpdated()
            }
        }
    }
}

@Composable
private fun horizontalAndVerticalPositioning(
    model: Model = LocalModel.current,
    layout: MutableState<SettingsWidgetLayout>,
    numericInput: MutableState<Boolean>,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column {
            Box(modifier = Modifier.padding(bottom = 10.dp)) {
                PositionEditView(
                    number = layout.value.x,
                    onNumberChange = { layout.value = layout.value.copy(x = it) },
                    value = layout.value.xString,
                    onValueChange = { layout.value = layout.value.copy(xString = it) },
                    onSubmit = {
                        setYBasedOnXIfLocked(model, layout)
                        model.sceneUpdated()
                    },
                    numericInput = numericInput.value,
                    onNumericInputChange = { numericInput.value = it },
                    incrementImageName = "arrow.forward.circle",
                    decrementImageName = "arrow.backward.circle",
                    mirror = layout.value.alignment.mirrorPositionHorizontally(),
                    increment = horizontalIncrement(model),
                )
            }
            PositionEditView(
                number = layout.value.y,
                onNumberChange = { layout.value = layout.value.copy(y = it) },
                value = layout.value.yString,
                onValueChange = { layout.value = layout.value.copy(yString = it) },
                onSubmit = {
                    setXBasedOnYIfLocked(model, layout)
                    model.sceneUpdated()
                },
                numericInput = numericInput.value,
                onNumericInputChange = { numericInput.value = it },
                incrementImageName = "arrow.down.circle",
                decrementImageName = "arrow.up.circle",
                mirror = layout.value.alignment.mirrorPositionVertically(),
                increment = verticalIncrement(model),
            )
        }
        Button(
            action = {
                layout.value = layout.value.copy(positioningLock = !layout.value.positioningLock)
                setYBasedOnXIfLocked(model, layout)
            },
        ) {
            SystemImage(
                name = if (layout.value.positioningLock) "lock" else "lock.open",
                fontSize = 28.sp,
                modifier = Modifier.width(35.dp),
            )
        }
    }
}

@Composable
private fun horizontalPositioning(
    model: Model = LocalModel.current,
    layout: MutableState<SettingsWidgetLayout>,
    numericInput: MutableState<Boolean>,
) {
    PositionEditView(
        number = layout.value.x,
        onNumberChange = { layout.value = layout.value.copy(x = it) },
        value = layout.value.xString,
        onValueChange = { layout.value = layout.value.copy(xString = it) },
        onSubmit = {
            model.sceneUpdated()
        },
        numericInput = numericInput.value,
        onNumericInputChange = { numericInput.value = it },
        incrementImageName = "arrow.forward.circle",
        decrementImageName = "arrow.backward.circle",
        mirror = layout.value.alignment.mirrorPositionHorizontally(),
        increment = horizontalIncrement(model),
    )
}

@Composable
private fun verticalPositioning(
    model: Model = LocalModel.current,
    layout: MutableState<SettingsWidgetLayout>,
    numericInput: MutableState<Boolean>,
) {
    PositionEditView(
        number = layout.value.y,
        onNumberChange = { layout.value = layout.value.copy(y = it) },
        value = layout.value.yString,
        onValueChange = { layout.value = layout.value.copy(yString = it) },
        onSubmit = {
            model.sceneUpdated()
        },
        numericInput = numericInput.value,
        onNumericInputChange = { numericInput.value = it },
        incrementImageName = "arrow.down.circle",
        decrementImageName = "arrow.up.circle",
        mirror = layout.value.alignment.mirrorPositionVertically(),
        increment = verticalIncrement(model),
    )
}

@Composable
fun WidgetLayoutView(
    model: Model = LocalModel.current,
    database: Database,
    layout: MutableState<SettingsWidgetLayout>,
    widget: SettingsWidget,
    numericInput: MutableState<Boolean>,
) {
    if (widget.hasAlignment() || widget.hasPosition() || widget.hasSize()) {
        Section(
            headerContent = {
                Text("Layout")
            },
            footerContent = {
                Text(
                    "Use save/load layout to position a widget in the same place in multiple " +
                        "scenes. Alternatively, use a Scene widget to easily show the same widgets " +
                        "in multiple scenes.",
                )
            },
        ) {
            generalAndAlignmentPicker(model, widget, layout)
            if (widget.hasPosition()) {
                if (!layout.value.alignment.isHorizontalCenter() && !layout.value.alignment.isVerticalCenter()) {
                    horizontalAndVerticalPositioning(model, layout, numericInput)
                } else if (!layout.value.alignment.isHorizontalCenter()) {
                    horizontalPositioning(model, layout, numericInput)
                } else if (!layout.value.alignment.isVerticalCenter()) {
                    verticalPositioning(model, layout, numericInput)
                }
            }
            if (widget.hasSize()) {
                SizeEditView(
                    number = layout.value.size,
                    onNumberChange = { layout.value = layout.value.copy(size = it) },
                    value = layout.value.sizeString,
                    onValueChange = { layout.value = layout.value.copy(sizeString = it) },
                    onSubmit = {
                        model.sceneUpdated()
                    },
                    numericInput = numericInput.value,
                    onNumericInputChange = { numericInput.value = it },
                )
            }
            Toggle(
                title = "Numeric input",
                isOn = database.sceneNumericInput,
                onChange = { database.sceneNumericInput = it },
            )
        }
    }
}

@Composable
fun WidgetNameView(widget: SettingsWidget) {
    Row {
        SystemImage(name = widget.image(), fontSize = 17.sp)
        Text(widget.name)
    }
}

@Composable
fun WidgetSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    widget: SettingsWidget,
) {
    Form(title = "${widget.type} widget") {
        Section {
            NameEditView(
                name = widget.name,
                onNameChange = { widget.name = it },
                existingNames = database.widgets,
            )
        }
        when (widget.type) {
            SettingsWidgetType.image ->
                WidgetImageSettingsView(model = model, widget = widget)
            SettingsWidgetType.browser ->
                WidgetBrowserSettingsView(model = model, widget = widget, browser = widget.browser)
            SettingsWidgetType.text ->
                WidgetTextSettingsView(widget = widget, text = widget.text)
            SettingsWidgetType.crop ->
                WidgetCropSettingsView(widget = widget)
            SettingsWidgetType.map ->
                WidgetMapSettingsView(
                    widget = widget,
                    initialDelay = widget.map.delay,
                    initialSize = widget.map.size,
                )
            SettingsWidgetType.scene ->
                WidgetSceneSettingsView(widget = widget, selectedSceneId = widget.scene.sceneId)
            SettingsWidgetType.slideshow ->
                WidgetSlideshowSettingsView(widget = widget)
            SettingsWidgetType.qrCode ->
                WidgetQrCodeSettingsView(model = model, widget = widget)
            SettingsWidgetType.alerts ->
                WidgetAlertsSettingsView(model = model, widget = widget)
            SettingsWidgetType.videoSource ->
                WidgetVideoSourceSettingsView(
                    widget = widget,
                    videoSource = widget.videoSource,
                    videoSources = model.videoSources,
                )
            SettingsWidgetType.scoreboard ->
                WidgetScoreboardSettingsView(
                    model = model,
                    widget = widget,
                    scoreboard = widget.scoreboard,
                    web = database.remoteControl.web,
                )
            SettingsWidgetType.vTuber ->
                WidgetVTuberSettingsView(
                    model = model,
                    widget = widget,
                    vTuber = widget.vTuber,
                    videoSources = model.videoSources,
                )
            SettingsWidgetType.pngTuber ->
                WidgetPngTuberSettingsView(
                    model = model,
                    widget = widget,
                    pngTuber = widget.pngTuber,
                    videoSources = model.videoSources,
                )
            SettingsWidgetType.snapshot ->
                WidgetSnapshotSettingsView(model = model, widget = widget, snapshot = widget.snapshot)
            SettingsWidgetType.chat ->
                WidgetChatSettingsView(model = model, database = database, widget = widget, chat = widget.chat)
            SettingsWidgetType.chatEmoteCombo ->
                WidgetChatEmoteComboSettingsView(
                    model = model,
                    widget = widget,
                    chatEmoteCombo = widget.chatEmoteCombo,
                )
            SettingsWidgetType.wheelOfLuck ->
                WidgetWheelOfLuckSettingsView(model = model, widget = widget, wheelOfLuck = widget.wheelOfLuck)
            SettingsWidgetType.bingoCard ->
                WidgetBingoCardSettingsView(model = model, widget = widget, bingoCard = widget.bingoCard)
            SettingsWidgetType.pomodoroTimer ->
                WidgetPomodoroTimerSettingsView(model = model, pomodoroTimer = widget.pomodoroTimer)
        }
    }
}
