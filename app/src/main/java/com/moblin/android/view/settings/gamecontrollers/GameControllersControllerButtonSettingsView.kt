package com.moblin.android.view.settings.gamecontrollers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsControllerFunction
import com.moblin.android.various.settings.SettingsControllerFunctionData
import com.moblin.android.various.settings.SettingsControllerFunctionSection
import com.moblin.android.various.settings.SettingsGameControllerButton
import com.moblin.android.various.settings.SettingsGimbalMotion
import com.moblin.android.view.settings.scenes.SceneNameView
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetNameView
import java.util.UUID
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
fun ControllerButtonView(
    model: Model = LocalModel.current,
    functions: List<SettingsControllerFunction>,
    function: SettingsControllerFunction,
    onFunctionChange: (SettingsControllerFunction) -> Unit,
    functionData: SettingsControllerFunctionData,
    onFunctionDataChange: (SettingsControllerFunctionData) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FunctionPicker(
            functions = functions,
            selected = function,
            onSelected = onFunctionChange,
        )
        when (function) {
            SettingsControllerFunction.SCENE -> {
                SettingsPicker(
                    label = "Scene",
                    options = listOf<UUID?>(null) + model.database.scenes.map { scene -> scene.id },
                    selected = functionData.sceneId,
                    selectedText = model.getSceneName(functionData.sceneId) ?: "-- None --",
                    optionContent = { id ->
                        val scene = model.database.scenes.firstOrNull { it.id == id }
                        if (scene != null) {
                            SceneNameView(scene = scene)
                        } else {
                            Text("-- None --")
                        }
                    },
                    onSelected = { id -> onFunctionDataChange(functionData.copy(sceneId = id)) },
                )
            }
            SettingsControllerFunction.WIDGET -> {
                SettingsPicker(
                    label = "Widget",
                    options = listOf<UUID?>(null) + model.database.widgets.map { widget -> widget.id },
                    selected = functionData.widgetId,
                    selectedText = model.getWidgetName(functionData.widgetId) ?: "-- None --",
                    optionContent = { id ->
                        val widget = model.database.widgets.firstOrNull { it.id == id }
                        if (widget != null) {
                            WidgetNameView(widget = widget)
                        } else {
                            Text("-- None --")
                        }
                    },
                    onSelected = { id -> onFunctionDataChange(functionData.copy(widgetId = id)) },
                )
            }
            SettingsControllerFunction.GIMBAL_PRESET -> {
                val presets = model.database.gimbal.presets
                SettingsPicker(
                    label = "Preset",
                    options = listOf<UUID?>(null) + presets.map { preset -> preset.id },
                    selected = functionData.gimbalPresetId,
                    selectedText = presets.firstOrNull { it.id == functionData.gimbalPresetId }?.name
                        ?: "-- None --",
                    optionContent = { id ->
                        val preset = presets.firstOrNull { it.id == id }
                        if (preset != null) {
                            Text(preset.name)
                        } else {
                            Text("-- None --")
                        }
                    },
                    onSelected = { id -> onFunctionDataChange(functionData.copy(gimbalPresetId = id)) },
                )
            }
            SettingsControllerFunction.GIMBAL_ANIMATE -> {
                SettingsPicker(
                    label = "Motion",
                    options = SettingsGimbalMotion.entries.toList(),
                    selected = functionData.gimbalMotion,
                    selectedText = functionData.gimbalMotion.toString(),
                    optionContent = { motion -> Text(motion.toString()) },
                    onSelected = { motion -> onFunctionDataChange(functionData.copy(gimbalMotion = motion)) },
                )
            }
            SettingsControllerFunction.MACRO -> {
                val macros = model.database.macros.macros
                SettingsPicker(
                    label = "Macro",
                    options = listOf<UUID?>(null) + macros.map { macro -> macro.id },
                    selected = functionData.macroId,
                    selectedText = macros.firstOrNull { it.id == functionData.macroId }?.name
                        ?: "-- None --",
                    optionContent = { id ->
                        val macro = macros.firstOrNull { it.id == id }
                        if (macro != null) {
                            Text(macro.name)
                        } else {
                            Text("-- None --")
                        }
                    },
                    onSelected = { id -> onFunctionDataChange(functionData.copy(macroId = id)) },
                )
            }
            SettingsControllerFunction.STREAM_DECK_LAYOUT -> {
                val layouts = model.database.streamDecks.layouts.value
                SettingsPicker(
                    label = "Layout",
                    options = listOf<UUID?>(null) + layouts.map { layout -> layout.id },
                    selected = functionData.streamDeckLayoutId,
                    selectedText = layouts.firstOrNull { it.id == functionData.streamDeckLayoutId }?.name
                        ?: "-- None --",
                    optionContent = { id ->
                        val layout = layouts.firstOrNull { it.id == id }
                        if (layout != null) {
                            Text(layout.name)
                        } else {
                            Text("-- None --")
                        }
                    },
                    onSelected = { id -> onFunctionDataChange(functionData.copy(streamDeckLayoutId = id)) },
                )
            }
            else -> Unit
        }
    }
}

@Composable
fun GameControllersControllerButtonSettingsView(
    model: Model = LocalModel.current,
    button: SettingsGameControllerButton,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val function by button.function.collectAsState()
    val functionData by button.functionData.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("Button") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = TODO("map the SF Symbol name ${button.name} to a Material icon"),
            contentDescription = null,
        )
        Text(
            text = button.text,
            modifier = Modifier.padding(start = 8.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = function.toString(
                model.getSceneName(functionData.sceneId),
                model.getWidgetName(functionData.widgetId),
            ),
            color = function.color(),
        )
    }
}

@Composable
fun GameControllersControllerButtonSettingsViewDestination(
    model: Model = LocalModel.current,
    button: SettingsGameControllerButton,
) {
    val function by button.function.collectAsState()
    val functionData by button.functionData.collectAsState()
    Column(modifier = Modifier.fillMaxWidth()) {
        ControllerButtonView(
            model = model,
            functions = functions(),
            function = function,
            onFunctionChange = { newFunction -> button.function.value = newFunction },
            functionData = functionData,
            onFunctionDataChange = { newFunctionData -> button.functionData.value = newFunctionData },
        )
    }
}

private fun functions(): List<SettingsControllerFunction> {
    return SettingsControllerFunction.entries.toList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FunctionPicker(
    functions: List<SettingsControllerFunction>,
    selected: SettingsControllerFunction,
    onSelected: (SettingsControllerFunction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Function") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Text(
                text = "General",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            functions
                .filter { it.section() == SettingsControllerFunctionSection.GENERAL }
                .forEach { function ->
                    DropdownMenuItem(
                        text = { Text(function.toString()) },
                        onClick = {
                            onSelected(function)
                            expanded = false
                        },
                    )
                }
            Text(
                text = "Filters",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            functions
                .filter { it.section() == SettingsControllerFunctionSection.FILTERS }
                .forEach { function ->
                    DropdownMenuItem(
                        text = { Text(function.toString()) },
                        onClick = {
                            onSelected(function)
                            expanded = false
                        },
                    )
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsPicker(
    label: String,
    options: List<T>,
    selected: T,
    selectedText: String,
    optionContent: @Composable (T) -> Unit,
    onSelected: (T) -> Unit,
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
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { optionContent(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun Model.getSceneName(id: UUID?): String? {
    if (id == null) {
        return null
    }
    return database.scenes.firstOrNull { it.id == id }?.name
}

private fun Model.getWidgetName(id: UUID?): String? {
    if (id == null) {
        return null
    }
    return database.widgets.firstOrNull { it.id == id }?.name
}
