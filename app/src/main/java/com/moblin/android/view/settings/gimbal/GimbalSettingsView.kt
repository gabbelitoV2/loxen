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

@Composable
private fun ZoomValueView(
    preset: SettingsGimbalPreset,
    zoomX: Float,
    onZoomXChange: (Float) -> Unit,
) {
    val presetZoomX by preset.zoomX.collectAsState()
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
    model: Model,
    gimbal: SettingsGimbal,
    preset: SettingsGimbalPreset,
    onNavigate: (String) -> Unit,
) {
    val presetName by preset.name.collectAsState()
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
    model: Model,
    gimbal: SettingsGimbal,
    preset: SettingsGimbalPreset,
) {
    val presetName by preset.name.collectAsState()
    val presets by gimbal.presets.collectAsState()
    var presentingConfirm by remember { mutableStateOf(false) }
    var moveAllowed by remember { mutableStateOf(false) }
    var x by remember { mutableStateOf(0f) }
    var y by remember { mutableStateOf(0f) }
    var zoomX by remember { mutableStateOf(1f) }

    fun localFromSettings() {
        x = preset.x.value
        y = preset.y.value
        zoomX = preset.zoomX.value
    }

    fun localToSettings() {
        preset.x.value = x
        preset.y.value = y
        preset.zoomX.value = zoomX
    }

    fun settingChanged() {
        if (x == preset.x.value && y == preset.y.value && zoomX == preset.zoomX.value) {
            return
        }
        if (moveAllowed) {
            localToSettings()
            model.moveToGimbalPreset(preset.id)
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
                onNameChange = { preset.name.value = it },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Position")
                Spacer(Modifier.weight(1f))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    PositionButtonView(image = "arrow.up.circle") {
                        x += 0.1f * (PI.toFloat() / 180f)
                    }
                    Row {
                        PositionButtonView(image = "arrow.left.circle") {
                            y += 0.1f * (PI.toFloat() / 180f)
                        }
                        PositionButtonView(image = "arrow.down.circle") {
                            x -= 0.1f * (PI.toFloat() / 180f)
                        }
                        PositionButtonView(image = "arrow.right.circle") {
                            y -= 0.1f * (PI.toFloat() / 180f)
                        }
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
    model: Model,
    gimbal: SettingsGimbal,
    onNavigate: (String) -> Unit,
) {
    val zoomSpeed by gimbal.zoomSpeed.collectAsState()
    val naturalZoom by gimbal.naturalZoom.collectAsState()
    val tracking by gimbal.tracking.collectAsState()
    val presets by gimbal.presets.collectAsState()
    val functionShutter by gimbal.functionShutter.collectAsState()
    val functionDataShutter by gimbal.functionDataShutter.collectAsState()
    val functionFlip by gimbal.functionFlip.collectAsState()
    val functionDataFlip by gimbal.functionDataFlip.collectAsState()

    val functions = SettingsControllerFunction.entries.filter {
        it != SettingsControllerFunction.unused &&
            it != SettingsControllerFunction.zoomIn &&
            it != SettingsControllerFunction.zoomOut
    }

    fun deletePreset(offsets: Set<Int>) {
        gimbal.presets.value = gimbal.presets.value.filterIndexed { index, _ -> index !in offsets }
    }

    LaunchedEffect(tracking) {
        model.setGimbalTracking(tracking)
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
                        onValueChange = { gimbal.zoomSpeed.value = it },
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
                        onCheckedChange = { gimbal.naturalZoom.value = it },
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
                        onCheckedChange = { gimbal.tracking.value = it },
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
                    onFunctionChange = { gimbal.functionShutter.value = it },
                    onFunctionDataChange = { gimbal.functionDataShutter.value = it },
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
                    onFunctionChange = { gimbal.functionFlip.value = it },
                    onFunctionDataChange = { gimbal.functionDataFlip.value = it },
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
                TextButtonView(
                    text = "Save current position",
                    enabled = Gimbal.shared?.isConnected() == true,
                    onClick = { model.saveGimbalPreset(null) },
                )
            }
            item {
                SwipeLeftToDeleteHelpView(kind = localized("preset"))
            }
        }
    }
}
