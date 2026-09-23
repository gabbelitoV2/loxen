package com.moblin.android.view.settings.gimbal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.common.various.toRadians
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.ButtonRole
import com.moblin.android.platform.swiftui.ConfirmationDialog
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.Visibility
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsControllerFunction
import com.moblin.android.various.settings.SettingsGimbal
import com.moblin.android.various.settings.SettingsGimbalPreset
import com.moblin.android.view.settings.display.quickbuttons.PositionButtonView
import com.moblin.android.view.settings.gamecontrollers.ControllerButtonView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import kotlin.math.roundToInt
import com.moblin.android.various.model.moveToGimbalPreset
import com.moblin.android.various.model.saveGimbalPreset
import com.moblin.android.various.model.setGimbalTracking

@Composable
private fun ZoomValueView(
    preset: SettingsGimbalPreset,
    zoomX: Float,
    onZoomXChange: (Float) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(localized("Zoom"))
        Spacer(Modifier.weight(1f))
        Text(formatOneDecimal(preset.zoomX))
        val minusInteractionSource = remember { MutableInteractionSource() }
        val minusPressed by minusInteractionSource.collectIsPressedAsState()
        SystemImage(
            name = "minus.circle",
            fontSize = 28.sp,
            modifier = Modifier
                .alpha(if (minusPressed) 0.2f else 1f)
                .clickable(
                    interactionSource = minusInteractionSource,
                    indication = null,
                ) {
                    onZoomXChange(maxOf(0.5f, zoomX - 0.1f))
                },
        )
        val plusInteractionSource = remember { MutableInteractionSource() }
        val plusPressed by plusInteractionSource.collectIsPressedAsState()
        SystemImage(
            name = "plus.circle",
            fontSize = 28.sp,
            modifier = Modifier
                .alpha(if (plusPressed) 0.2f else 1f)
                .clickable(
                    interactionSource = plusInteractionSource,
                    indication = null,
                ) {
                    onZoomXChange(minOf(15f, zoomX + 0.1f))
                },
        )
    }
}

@Composable
private fun GimbalPresetView(
    model: Model = LocalModel.current,
    gimbal: SettingsGimbal,
    preset: SettingsGimbalPreset,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            GimbalPresetDetailView(model = model, gimbal = gimbal, preset = preset)
        },
    ) {
        Text(preset.name)
    }
}

@Composable
fun GimbalPresetDetailView(
    model: Model = LocalModel.current,
    gimbal: SettingsGimbal,
    preset: SettingsGimbalPreset,
) {
    var presentingConfirm by remember { mutableStateOf(false) }
    var moveAllowed by remember { mutableStateOf(false) }
    var x by remember { mutableStateOf(0f) }
    var y by remember { mutableStateOf(0f) }
    var zoomX by remember { mutableStateOf(1f) }

    fun localFromSettings() {
        x = preset.x
        y = preset.y
        zoomX = preset.zoomX
    }

    fun localToSettings() {
        preset.x = x
        preset.y = y
        preset.zoomX = zoomX
    }

    fun settingChanged() {
        if (x == preset.x && y == preset.y && zoomX == preset.zoomX) {
            return
        }
        if (moveAllowed) {
            localToSettings()
            model.moveToGimbalPreset(id = preset.id)
        } else {
            presentingConfirm = true
        }
    }

    fun updateX(value: Float) {
        if (value != x) {
            x = value
            settingChanged()
        }
    }

    fun updateY(value: Float) {
        if (value != y) {
            y = value
            settingChanged()
        }
    }

    fun updateZoomX(value: Float) {
        if (value != zoomX) {
            zoomX = value
            settingChanged()
        }
    }

    fun dismissConfirm() {
        presentingConfirm = false
        if (!moveAllowed) {
            localFromSettings()
        }
    }

    DisposableEffect(Unit) {
        moveAllowed = false
        localFromSettings()
        onDispose {}
    }

    Form(title = "Preset") {
        Section {
            NameEditView(
                name = preset.name,
                existingNames = gimbal.presets,
                onNameChange = { preset.name = it },
            )
        }
        Section {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(localized("Position"))
                Spacer(Modifier.weight(1f))
                CompositionLocalProvider(
                    LocalTextStyle provides TextStyle(fontSize = 28.sp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        PositionButtonView(
                            image = "arrow.up.circle",
                            action = { updateX(x + 0.1f.toRadians()) },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PositionButtonView(
                                image = "arrow.left.circle",
                                action = { updateY(y + 0.1f.toRadians()) },
                            )
                            PositionButtonView(
                                image = "arrow.down.circle",
                                action = { updateX(x - 0.1f.toRadians()) },
                            )
                            PositionButtonView(
                                image = "arrow.right.circle",
                                action = { updateY(y - 0.1f.toRadians()) },
                            )
                        }
                    }
                }
            }
        }
        Section {
            ZoomValueView(
                preset = preset,
                zoomX = zoomX,
                onZoomXChange = { updateZoomX(it) },
            )
        }
    }
    ConfirmationDialog(
        title = "Beware, changing settings will move the Gimbal to the new position.",
        isPresented = presentingConfirm,
        onDismissRequest = { dismissConfirm() },
        titleVisibility = Visibility.visible,
    ) {
        Button("Ok", role = ButtonRole.destructive) {
            moveAllowed = true
            settingChanged()
        }
    }
}

@Composable
fun GimbalSettingsView(
    model: Model = LocalModel.current,
    gimbal: SettingsGimbal,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val palette = formPalette()
    val functions = SettingsControllerFunction.entries.filter {
        it != SettingsControllerFunction.UNUSED &&
            it != SettingsControllerFunction.ZOOM_IN &&
            it != SettingsControllerFunction.ZOOM_OUT
    }

    fun deletePreset(offsets: Set<Int>) {
        gimbal.presets = gimbal.presets.filterIndexed { index, _ -> index !in offsets }
    }

    Form(title = "Gimbal") {
        Section {
            Text(localized("Control Moblin with gimbals that supports DockKit."))
        }
        Section(header = "Zoom") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(localized("Speed"))
                FormSlider(
                    value = gimbal.zoomSpeed,
                    onValueChange = { gimbal.zoomSpeed = it },
                    modifier = Modifier.weight(1f),
                    valueRange = 10f..100f,
                )
                Box(
                    modifier = Modifier.width(30.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(gimbal.zoomSpeed.toInt().toString())
                }
            }
            Toggle(
                title = "Natural",
                isOn = gimbal.naturalZoom,
                onChange = { gimbal.naturalZoom = it },
            )
        }
        Section(header = "Tracking") {
            Toggle(
                title = "Enabled",
                isOn = gimbal.tracking,
                onChange = { model.setGimbalTracking(on = it) },
            )
        }
        Section(header = "Shutter button") {
            ControllerButtonView(
                model = model,
                functions = functions,
                function = gimbal.functionShutter,
                functionData = gimbal.functionDataShutter,
                onFunctionChange = { gimbal.functionShutter = it },
                onFunctionDataChange = { gimbal.functionDataShutter = it },
            )
        }
        Section(header = "Flip button") {
            ControllerButtonView(
                model = model,
                functions = functions,
                function = gimbal.functionFlip,
                functionData = gimbal.functionDataFlip,
                onFunctionChange = { gimbal.functionFlip = it },
                onFunctionDataChange = { gimbal.functionDataFlip = it },
            )
        }
        Section(
            header = "Presets",
            footerContent = { SwipeLeftToDeleteHelpView(kind = localized("preset")) },
        ) {
            gimbal.presets.forEachIndexed { index, preset ->
                var dragOffset by remember(preset.id) { mutableStateOf(0f) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .matchParentSize()
                            .background(palette.red)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                deletePreset(setOf(index))
                            },
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = localized("Delete"),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(dragOffset.roundToInt(), 0) }
                            .background(palette.cell)
                            .pointerInput(preset.id) {
                                val maximumSwipe = 100.dp.toPx()
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        dragOffset = if (dragOffset < -maximumSwipe / 2f) {
                                            -maximumSwipe
                                        } else {
                                            0f
                                        }
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        dragOffset = (dragOffset + dragAmount)
                                            .coerceIn(-maximumSwipe, 0f)
                                    },
                                )
                            },
                    ) {
                        GimbalPresetView(
                            model = model,
                            gimbal = gimbal,
                            preset = preset,
                        )
                    }
                }
            }
            TextButtonView(title = "Save current position") {
                model.saveGimbalPreset(id = null)
            }
        }
    }
}
