package com.moblin.android.view.settings.gimbal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.localized
import com.moblin.android.various.Gimbal
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsControllerFunction
import com.moblin.android.various.settings.SettingsGimbal
import com.moblin.android.various.settings.SettingsGimbalPreset
import com.moblin.android.view.settings.display.quickbuttons.PositionButtonView
import com.moblin.android.view.settings.gamecontrollers.ControllerButtonView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import kotlin.math.PI
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun ZoomValueView(
    preset: SettingsGimbalPreset,
    zoomX: Float,
    onZoomXChange: (Float) -> Unit,
) {
    val presetZoomX = preset.zoomX
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Zoom")
        Spacer(Modifier.weight(1f))
        Text(formatOneDecimal(presetZoomX))
        Button(onClick = { onZoomXChange(maxOf(0.5f, zoomX - 0.1f)) }) {
            Icon(Icons.Default.RemoveCircle, contentDescription = null)
        }
        Button(onClick = { onZoomXChange(minOf(15f, zoomX + 0.1f)) }) {
            Icon(Icons.Default.AddCircle, contentDescription = null)
        }
    }
}

@Composable
private fun GimbalPresetView(
    model: Model = LocalModel.current,
    gimbal: SettingsGimbal,
    preset: SettingsGimbalPreset,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val presetName = preset.name
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("preset") },
    ) {
        Text(presetName)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GimbalPresetDetailView(
    model: Model = LocalModel.current,
    gimbal: SettingsGimbal,
    preset: SettingsGimbalPreset,
) {
    val presetName = preset.name
    val presets = gimbal.presets
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
            Unit
        } else {
            presentingConfirm = true
        }
    }

    LaunchedEffect(Unit) {
        moveAllowed = false
        localFromSettings()
    }
    LaunchedEffect(x) {
        settingChanged()
    }
    LaunchedEffect(y) {
        settingChanged()
    }
    LaunchedEffect(zoomX) {
        settingChanged()
    }
    LaunchedEffect(presentingConfirm) {
        if (!presentingConfirm && !moveAllowed) {
            localFromSettings()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Preset") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            NameEditView(
                name = presetName,
                existingNames = presets,
                onNameChange = { preset.name = it },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Position")
                Spacer(Modifier.weight(1f))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    PositionButtonView(image = "arrow.up.circle", action = {
                        x += 0.1f * (PI.toFloat() / 180f)
                    })
                    Row {
                        PositionButtonView(image = "arrow.left.circle", action = {
                            y += 0.1f * (PI.toFloat() / 180f)
                        })
                        PositionButtonView(image = "arrow.down.circle", action = {
                            x -= 0.1f * (PI.toFloat() / 180f)
                        })
                        PositionButtonView(image = "arrow.right.circle", action = {
                            y -= 0.1f * (PI.toFloat() / 180f)
                        })
                    }
                }
            }
            ZoomValueView(
                preset = preset,
                zoomX = zoomX,
                onZoomXChange = { zoomX = it },
            )
        }
        if (presentingConfirm) {
            AlertDialog(
                onDismissRequest = { presentingConfirm = false },
                title = { Text("Beware, changing settings will move the Gimbal to the new position.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            moveAllowed = true
                            settingChanged()
                        },
                    ) {
                        Text("Ok")
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GimbalSettingsView(
    model: Model = LocalModel.current,
    gimbal: SettingsGimbal,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val zoomSpeed = gimbal.zoomSpeed
    val naturalZoom = gimbal.naturalZoom
    val tracking = gimbal.tracking
    val presets = gimbal.presets
    val functionShutter = gimbal.functionShutter
    val functionDataShutter = gimbal.functionDataShutter
    val functionFlip = gimbal.functionFlip
    val functionDataFlip = gimbal.functionDataFlip

    val functions = SettingsControllerFunction.entries.filter {
        it != SettingsControllerFunction.UNUSED &&
            it != SettingsControllerFunction.ZOOM_IN &&
            it != SettingsControllerFunction.ZOOM_OUT
    }

    fun deletePreset(offsets: Set<Int>) {
        gimbal.presets = gimbal.presets.filterIndexed { index, _ -> index !in offsets }
    }

    LaunchedEffect(tracking) {
        Gimbal.shared?.setTracking(tracking)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gimbal") }) },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Text("Control Moblin with gimbals that supports DockKit.")
            }
            item {
                Text("Zoom")
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Speed")
                    Slider(
                        value = zoomSpeed,
                        onValueChange = { gimbal.zoomSpeed = it },
                        modifier = Modifier.weight(1f),
                        valueRange = 10f..100f,
                        steps = 89,
                    )
                    Text(zoomSpeed.toInt().toString(), modifier = Modifier.width(30.dp))
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Natural")
                    Switch(
                        checked = naturalZoom,
                        onCheckedChange = { gimbal.naturalZoom = it },
                    )
                }
            }
            item {
                Text("Tracking")
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enabled")
                    Switch(
                        checked = tracking,
                        onCheckedChange = { gimbal.tracking = it },
                    )
                }
            }
            item {
                Text("Shutter button")
            }
            item {
                ControllerButtonView(
                    model = model,
                    functions = functions,
                    function = functionShutter,
                    functionData = functionDataShutter,
                    onFunctionChange = { gimbal.functionShutter = it },
                    onFunctionDataChange = { gimbal.functionDataShutter = it },
                )
            }
            item {
                Text("Flip button")
            }
            item {
                ControllerButtonView(
                    model = model,
                    functions = functions,
                    function = functionFlip,
                    functionData = functionDataFlip,
                    onFunctionChange = { gimbal.functionFlip = it },
                    onFunctionDataChange = { gimbal.functionDataFlip = it },
                )
            }
            item {
                Text("Presets")
            }
            itemsIndexed(presets, key = { _, preset -> preset.id }) { index, preset ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            deletePreset(setOf(index))
                            true
                        } else {
                            false
                        }
                    },
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = { },
                ) {
                    Box(
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { TODO("no Android counterpart for contextMenu") },
                            )
                        },
                    ) {
                        GimbalPresetView(
                            model = model,
                            gimbal = gimbal,
                            preset = preset,
                            onNavigate = onNavigate,
                        )
                    }
                }
            }
            item {
                TextButtonView(title = "Save current position") {
                    Unit
                }
            }
            item {
                SwipeLeftToDeleteHelpView(kind = localized("preset"))
            }
        }
    }
}
