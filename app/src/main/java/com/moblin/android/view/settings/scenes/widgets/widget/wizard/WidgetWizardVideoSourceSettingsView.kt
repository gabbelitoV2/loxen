package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidgetVideoSource
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.InlinePickerItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerView(
    onChange: (String) -> Unit,
    items: List<InlinePickerItem>,
    selectedId: String,
) {
    var selected by remember { mutableStateOf(selectedId) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(selected) {
        onChange(selected)
    }

    Row {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = items.firstOrNull { it.id == selected }?.text ?: "Unknown 😢",
                onValueChange = {},
                readOnly = true,
                label = { Text("Video source") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier.menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.text) },
                        onClick = {
                            selected = item.id
                            expanded = false
                        },
                    )
                }
                if (items.none { it.id == selected }) {
                    DropdownMenuItem(
                        text = { Text("Unknown 😢") },
                        onClick = { expanded = false },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetWizardVideoSourceSettingsView(
    model: Model,
    database: Database,
    createWidgetWizard: CreateWidgetWizard,
    videoSource: SettingsWidgetVideoSource,
    presentingCreateWizard: Boolean,
    onPresentingCreateWizardChange: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(basicWidgetSettingsTitle(createWidgetWizard)) },
                actions = {
                    CloseToolbar(
                        presenting = presentingCreateWizard,
                        onPresentingChange = onPresentingCreateWizardChange,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            PickerView(
                onChange = { cameraId ->
                    onCameraChange(
                        model = model,
                        videoSource = videoSource,
                        cameraId = cameraId,
                    )
                },
                items = model.listCameras(excludeBuiltin = false).map {
                    InlinePickerItem(id = it.id, text = it.name)
                },
                selectedId = model.getCameraId(videoSourceWidget = videoSource),
            )
            WidgetWizardSelectScenesNavigationView(
                model = model,
                database = database,
                createWidgetWizard = createWidgetWizard,
                presentingCreateWizard = presentingCreateWizard,
                onPresentingCreateWizardChange = onPresentingCreateWizardChange,
            )
        }
    }
}

private fun onCameraChange(
    model: Model,
    videoSource: SettingsWidgetVideoSource,
    cameraId: String,
) {
    videoSource.updateCameraId(
        settingsCameraId = model.cameraIdToSettingsCameraId(cameraId = cameraId),
    )
}
