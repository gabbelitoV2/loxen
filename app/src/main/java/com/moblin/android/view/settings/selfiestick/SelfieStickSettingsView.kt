package com.moblin.android.view.settings.selfiestick

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsControllerFunction
import com.moblin.android.various.settings.SettingsSelfieStick
import com.moblin.android.view.settings.gamecontrollers.ControllerButtonView

@Composable
fun SelfieStickDoesNotWorkView(database: Database, selfieStick: SettingsSelfieStick) {
    val cameraControlsEnabled = database.cameraControlsEnabled
    val enabled by selfieStick.enabled.collectAsState()
    if (cameraControlsEnabled && enabled) {
        Text("⚠️ Selfie stick button does not work with Camera controls enabled.")
    }
}

private fun functions(): List<SettingsControllerFunction> {
    return SettingsControllerFunction.entries.filter {
        it != SettingsControllerFunction.UNUSED &&
            it != SettingsControllerFunction.ZOOM_IN &&
            it != SettingsControllerFunction.ZOOM_OUT
    }
}

@Composable
fun SelfieStickSettingsView(model: Model = LocalModel.current, selfieStick: SettingsSelfieStick) {
    Form(title = "Selfie stick") {
        Section(
            header = "Button",
            footer = "⚠️ Hijacks volume buttons. You can only change volume in Control Center when enabled.",
        ) {
            val enabled by selfieStick.enabled.collectAsState()
            val function by selfieStick.function.collectAsState()
            val functionData by selfieStick.functionData.collectAsState()
            Toggle("Enabled", isOn = enabled) { selfieStick.enabled.value = it }
            ControllerButtonView(
                model = model,
                functions = functions(),
                function = function,
                onFunctionChange = { selfieStick.function.value = it },
                functionData = functionData,
                onFunctionDataChange = { selfieStick.functionData.value = it },
            )
            SelfieStickDoesNotWorkView(database = model.database, selfieStick = selfieStick)
        }
    }
}
