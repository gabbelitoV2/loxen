package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.VideoSources
import com.moblin.android.various.model.cameraIdToSettingsCameraId
import com.moblin.android.various.model.getCameraId
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidgetPngTuber
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.settings.scenes.widgets.widget.pngtuber.WidgetPngTuberPickerView
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.InlinePickerItem

@Composable
fun WidgetWizardPngTuberSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    pngTuber: SettingsWidgetPngTuber,
    videoSources: VideoSources,
    createWidgetWizard: CreateWidgetWizard,
    presentingCreateWizard: Boolean,
    onPresentingCreateWizardChange: (Boolean) -> Unit,
) {
    fun onCameraChange(cameraId: String) {
        pngTuber.updateCameraId(settingsCameraId = model.cameraIdToSettingsCameraId(cameraId = cameraId))
    }
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
            WizardVideoSourcePickerView(
                onChange = { onCameraChange(it) },
                items = videoSources.all().map {
                    InlinePickerItem(id = it.id, text = it.name)
                },
                selectedId = model.getCameraId(pngTuberWidget = pngTuber),
            )
        }
        WidgetPngTuberPickerView(
            model = model,
            pngTuber = pngTuber,
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
