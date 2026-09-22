package com.moblin.android.view.settings.scenes.widgets.widget

import android.util.Size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.outlined.Square
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.PositionEditView
import com.moblin.android.view.utils.SizeEditView

@Composable
fun AlignmentOptionView(
    layout: MutableState<SettingsWidgetLayout>,
    alignment: SettingsAlignment
) {
    IconButton(onClick = { layout.value.alignment = alignment }) {
        Icon(
            imageVector = if (layout.value.alignment == alignment) {
                Icons.Filled.Square
            } else {
                Icons.Outlined.Square
            },
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun SaveLoadLayoutView(
    model: Model,
    layout: MutableState<SettingsWidgetLayout>
) {
    Column(horizontalAlignment = Alignment.Start) {
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = { model.layout = layout.value }) {
            HCenter {
                Text("Save layout")
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = {
                layout.value = model.layout ?: layout.value
                model.sceneUpdated()
            },
            enabled = model.layout != null
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

private fun dimensions(model: Model): Size {
    return model.stream.resolution.dimensions(portrait = model.stream.portrait)
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
    layout.value.x = layout.value.y * horizontalIncrement(model) / verticalIncrement(model)
    layout.value.xString = layout.value.x.toString()
}

private fun setYBasedOnXIfLocked(layout: MutableState<SettingsWidgetLayout>, model: Model) {
    if (!layout.value.positioningLock) {
        return
    }
    layout.value.y = layout.value.x * verticalIncrement(model) / horizontalIncrement(model)
    layout.value.yString = layout.value.y.toString()
}

@Composable
private fun generalAndAlignmentPicker(
    model: Model,
    layout: MutableState<SettingsWidgetLayout>,
    widget: SettingsWidget
) {
    Row {
        Row {
            SaveLoadLayoutView(model = model, layout = layout)
            Spacer(Modifier.weight(1f))
        }
        if (widget.hasAlignment()) {
            VerticalDivider()
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
            LaunchedEffect(layout.value.alignment) {
                model.sceneUpdated()
            }
        }
    }
}

@Composable
private fun horizontalAndVerticalPositioning(
    model: Model,
    layout: MutableState<SettingsWidgetLayout>,
    numericInput: MutableState<Boolean>
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column {
            PositionEditView(
                number = layout.value.x,
                value = layout.value.xString,
                onNumberChange = { layout.value.x = it },
                onValueChange = { layout.value.xString = it },
                onSubmit = {
                    setYBasedOnXIfLocked(layout, model)
                    model.sceneUpdated()
                },
                numericInput = numericInput,
                incrementImageName = "arrow.forward.circle",
                decrementImageName = "arrow.backward.circle",
                mirror = layout.value.alignment.mirrorPositionHorizontally(),
                increment = horizontalIncrement(model)
            )
            Spacer(Modifier.height(10.dp))
            PositionEditView(
                number = layout.value.y,
                value = layout.value.yString,
                onNumberChange = { layout.value.y = it },
                onValueChange = { layout.value.yString = it },
                onSubmit = {
                    setXBasedOnYIfLocked(layout, model)
                    model.sceneUpdated()
                },
                numericInput = numericInput,
                incrementImageName = "arrow.down.circle",
                decrementImageName = "arrow.up.circle",
                mirror = layout.value.alignment.mirrorPositionVertically(),
                increment = verticalIncrement(model)
            )
        }
        IconButton(onClick = {
            layout.value.positioningLock = !layout.value.positioningLock
            setYBasedOnXIfLocked(layout, model)
        }) {
            Icon(
                imageVector = if (layout.value.positioningLock) {
                    Icons.Filled.Lock
                } else {
                    Icons.Filled.LockOpen
                },
                contentDescription = null,
                modifier = Modifier.width(35.dp)
            )
        }
    }
}

@Composable
private fun horizontalPositioning(
    model: Model,
    layout: MutableState<SettingsWidgetLayout>,
    numericInput: MutableState<Boolean>
) {
    PositionEditView(
        number = layout.value.x,
        value = layout.value.xString,
        onNumberChange = { layout.value.x = it },
        onValueChange = { layout.value.xString = it },
        onSubmit = {
            model.sceneUpdated()
        },
        numericInput = numericInput,
        incrementImageName = "arrow.forward.circle",
        decrementImageName = "arrow.backward.circle",
        mirror = layout.value.alignment.mirrorPositionHorizontally(),
        increment = horizontalIncrement(model)
    )
}

@Composable
private fun verticalPositioning(
    model: Model,
    layout: MutableState<SettingsWidgetLayout>,
    numericInput: MutableState<Boolean>
) {
    PositionEditView(
        number = layout.value.y,
        value = layout.value.yString,
        onNumberChange = { layout.value.y = it },
        onValueChange = { layout.value.yString = it },
        onSubmit = {
            model.sceneUpdated()
        },
        numericInput = numericInput,
        incrementImageName = "arrow.down.circle",
        decrementImageName = "arrow.up.circle",
        mirror = layout.value.alignment.mirrorPositionVertically(),
        increment = verticalIncrement(model)
    )
}

@Composable
fun WidgetLayoutView(
    model: Model,
    database: Database,
    layout: MutableState<SettingsWidgetLayout>,
    widget: SettingsWidget,
    numericInput: MutableState<Boolean>
) {
    if (widget.hasAlignment() || widget.hasPosition() || widget.hasSize()) {
        Column {
            Text("Layout", style = MaterialTheme.typography.titleMedium)
            generalAndAlignmentPicker(model = model, layout = layout, widget = widget)
            if (widget.hasPosition()) {
                if (!layout.value.alignment.isHorizontalCenter() &&
                    !layout.value.alignment.isVerticalCenter()
                ) {
                    horizontalAndVerticalPositioning(model = model, layout = layout, numericInput = numericInput)
                } else if (!layout.value.alignment.isHorizontalCenter()) {
                    horizontalPositioning(model = model, layout = layout, numericInput = numericInput)
                } else if (!layout.value.alignment.isVerticalCenter()) {
                    verticalPositioning(model = model, layout = layout, numericInput = numericInput)
                }
            }
            if (widget.hasSize()) {
                SizeEditView(
                    number = layout.value.size,
                    value = layout.value.sizeString,
                    onNumberChange = { layout.value.size = it },
                    onValueChange = { layout.value.sizeString = it },
                    onSubmit = {
                        model.sceneUpdated()
                    },
                    numericInput = numericInput
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Numeric input")
                Switch(
                    checked = database.sceneNumericInput,
                    onCheckedChange = { database.sceneNumericInput = it }
                )
            }
            Text(
                "Use save/load layout to position a widget in the same place in multiple " +
                    "scenes. Alternatively, use a Scene widget to easily show the same widgets " +
                    "in multiple scenes."
            )
        }
    }
}

@Composable
fun WidgetNameView(widget: SettingsWidget) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = widget.image(), contentDescription = null)
        Text(widget.name)
    }
}

@Composable
fun WidgetSettingsView(
    model: Model,
    database: Database,
    widget: SettingsWidget
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        NameEditView(
            name = widget.name,
            existingNames = database.widgets,
            onNameChange = { widget.name = it }
        )
        when (widget.type) {
            SettingsWidgetType.image -> WidgetImageSettingsView(model = model, widget = widget)
            SettingsWidgetType.browser -> WidgetBrowserSettingsView(widget = widget, browser = widget.browser)
            SettingsWidgetType.text -> WidgetTextSettingsView(widget = widget, text = widget.text)
            SettingsWidgetType.crop -> WidgetCropSettingsView(widget = widget)
            SettingsWidgetType.map -> WidgetMapSettingsView(
                widget = widget,
                delay = widget.map.delay,
                size = widget.map.size
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
