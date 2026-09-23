package com.moblin.android.view.settings.gamecontrollers

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsControllerFunction
import com.moblin.android.various.settings.SettingsControllerFunctionData
import com.moblin.android.various.settings.SettingsControllerFunctionSection
import com.moblin.android.various.settings.SettingsGameControllerButton
import com.moblin.android.various.settings.SettingsGimbalMotion
import java.util.UUID

@Composable
fun ControllerButtonView(
    model: Model = LocalModel.current,
    functions: List<SettingsControllerFunction>,
    function: SettingsControllerFunction,
    onFunctionChange: (SettingsControllerFunction) -> Unit,
    functionData: SettingsControllerFunctionData,
    onFunctionDataChange: (SettingsControllerFunctionData) -> Unit,
) {
    Picker(
        title = "Function",
        selection = function,
        options = functions.filter { it.section() == SettingsControllerFunctionSection.GENERAL } +
            functions.filter { it.section() == SettingsControllerFunctionSection.FILTERS },
        onChange = onFunctionChange,
    )
    when (function) {
        SettingsControllerFunction.SCENE -> {
            val scenes = model.database.scenes
            Picker(
                title = "Scene",
                selection = functionData.sceneId,
                options = listOf<UUID?>(null) + scenes.map { scene -> scene.id },
                text = { id -> scenes.firstOrNull { scene -> scene.id == id }?.name ?: "-- None --" },
                onChange = { id -> onFunctionDataChange(functionData.copy(sceneId = id)) },
            )
        }
        SettingsControllerFunction.WIDGET -> {
            val widgets = model.database.widgets
            Picker(
                title = "Widget",
                selection = functionData.widgetId,
                options = listOf<UUID?>(null) + widgets.map { widget -> widget.id },
                text = { id -> widgets.firstOrNull { widget -> widget.id == id }?.name ?: "-- None --" },
                onChange = { id -> onFunctionDataChange(functionData.copy(widgetId = id)) },
            )
        }
        SettingsControllerFunction.GIMBAL_PRESET -> {
            val presets = model.database.gimbal.presets
            Picker(
                title = "Preset",
                selection = functionData.gimbalPresetId,
                options = listOf<UUID?>(null) + presets.map { preset -> preset.id },
                text = { id -> presets.firstOrNull { preset -> preset.id == id }?.name ?: "-- None --" },
                onChange = { id -> onFunctionDataChange(functionData.copy(gimbalPresetId = id)) },
            )
        }
        SettingsControllerFunction.GIMBAL_ANIMATE -> {
            Picker(
                title = "Motion",
                selection = functionData.gimbalMotion,
                options = SettingsGimbalMotion.entries.toList(),
                onChange = { motion -> onFunctionDataChange(functionData.copy(gimbalMotion = motion)) },
            )
        }
        SettingsControllerFunction.MACRO -> {
            val macros = model.database.macros.macros
            Picker(
                title = "Macro",
                selection = functionData.macroId,
                options = listOf<UUID?>(null) + macros.map { macro -> macro.id },
                text = { id -> macros.firstOrNull { macro -> macro.id == id }?.name ?: "-- None --" },
                onChange = { id -> onFunctionDataChange(functionData.copy(macroId = id)) },
            )
        }
        SettingsControllerFunction.STREAM_DECK_LAYOUT -> {
            val layouts by model.database.streamDecks.layouts.collectAsState()
            Picker(
                title = "Layout",
                selection = functionData.streamDeckLayoutId,
                options = listOf<UUID?>(null) + layouts.map { layout -> layout.id },
                text = { id -> layouts.firstOrNull { layout -> layout.id == id }?.name ?: "-- None --" },
                onChange = { id -> onFunctionDataChange(functionData.copy(streamDeckLayoutId = id)) },
            )
        }
        else -> Unit
    }
}

@Composable
fun GameControllersControllerButtonSettingsView(
    model: Model = LocalModel.current,
    button: SettingsGameControllerButton,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            val function by button.function.collectAsState()
            val functionData by button.functionData.collectAsState()
            Form(title = "Button") {
                Section {
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
        },
    ) {
        val function by button.function.collectAsState()
        val functionData by button.functionData.collectAsState()
        Label(title = button.text, systemImage = button.name)
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
    Form(title = "Button") {
        Section {
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
}

private fun functions(): List<SettingsControllerFunction> {
    return SettingsControllerFunction.entries.toList()
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
