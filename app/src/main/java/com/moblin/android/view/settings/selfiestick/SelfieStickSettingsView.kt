package com.moblin.android.view.settings.selfiestick

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsControllerFunction
import com.moblin.android.various.settings.SettingsSelfieStick
import com.moblin.android.view.settings.gamecontrollers.ControllerButtonView

@Composable
fun SelfieStickDoesNotWorkView(database: Database, selfieStick: SettingsSelfieStick) {
    val cameraControlsEnabled = database.cameraControlsEnabled.collectAsState().value
    val enabled = selfieStick.enabled.collectAsState().value
    if (cameraControlsEnabled && enabled) {
        Text("⚠️ Selfie stick button does not work with Camera controls enabled.")
    }
}

private fun functions(): List<SettingsControllerFunction> {
    return SettingsControllerFunction.entries.filter {
        it != SettingsControllerFunction.unused &&
            it != SettingsControllerFunction.zoomIn &&
            it != SettingsControllerFunction.zoomOut
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfieStickSettingsView(model: Model, selfieStick: SettingsSelfieStick) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Selfie stick") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Button", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enabled")
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = selfieStick.enabled.collectAsState().value,
                    onCheckedChange = { selfieStick.enabled.value = it },
                )
            }
            ControllerButtonView(
                model = model,
                functions = functions(),
                function = selfieStick.function.collectAsState().value,
                onFunctionChange = { selfieStick.function.value = it },
                functionData = selfieStick.functionData.collectAsState().value,
                onFunctionDataChange = { selfieStick.functionData.value = it },
            )
            SelfieStickDoesNotWorkView(database = model.database, selfieStick = selfieStick)
            Text(
                "⚠️ Hijacks volume buttons. You can only change volume in Control Center when enabled.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
