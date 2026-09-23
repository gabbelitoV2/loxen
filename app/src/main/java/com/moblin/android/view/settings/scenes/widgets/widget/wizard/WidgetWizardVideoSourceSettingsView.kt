package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidgetVideoSource
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.various.model.cameraIdToSettingsCameraId
import com.moblin.android.various.model.getCameraId
import com.moblin.android.various.model.listCameras

@Composable
private fun PickerView(
    onChange: (String) -> Unit,
    items: List<InlinePickerItem>,
    selectedId: String,
) {
    var selected by remember { mutableStateOf(selectedId) }
    var options = items.map { it.id }
    if (options.none { it == selected }) {
        options = options + selected
    }
    Picker(
        title = "Video source",
        selection = selected,
        options = options,
        text = { id -> items.firstOrNull { it.id == id }?.text ?: "Unknown 😢" },
        onChange = { id ->
            selected = id
            onChange(id)
        },
    )
}

@Composable
fun WidgetWizardVideoSourceSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    createWidgetWizard: CreateWidgetWizard,
    videoSource: SettingsWidgetVideoSource,
    presentingCreateWizard: Boolean,
    onPresentingCreateWizardChange: (Boolean) -> Unit,
) {
    Form(
        title = basicWidgetSettingsTitle(createWidgetWizard),
        toolbar = {
            CloseToolbar(
                presenting = presentingCreateWizard,
                onPresentingChange = onPresentingCreateWizardChange,
            )
        },
    ) {
        Section {
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
        }
        WidgetWizardSelectScenesNavigationView(
            model = model,
            database = database,
            createWidgetWizard = createWidgetWizard,
            presentingCreateWizard = presentingCreateWizard,
            onPresentingCreateWizardChange = onPresentingCreateWizardChange,
        )
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
