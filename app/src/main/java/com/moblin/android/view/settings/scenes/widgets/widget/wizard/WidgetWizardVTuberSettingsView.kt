package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.VideoSources
import com.moblin.android.various.model.cameraIdToSettingsCameraId
import com.moblin.android.various.model.getCameraId
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidgetVTuber
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.settings.scenes.widgets.widget.vtuber.WidgetVTuberPickerView
import com.moblin.android.view.utils.CloseToolbarButtonView
import com.moblin.android.view.utils.InlinePickerItem

@Composable
fun WidgetWizardVTuberSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    vTuber: SettingsWidgetVTuber,
    videoSources: VideoSources,
    createWidgetWizard: CreateWidgetWizard,
    presentingCreateWizard: Boolean,
    onChangePresentingCreateWizard: (Boolean) -> Unit,
) {
    val modelName = vTuber.modelName
    Form(
        title = basicWidgetSettingsTitle(createWidgetWizard),
        toolbar = {
            CloseToolbarButtonView(
                presenting = presentingCreateWizard,
                onPresentingChange = onChangePresentingCreateWizard,
            )
        },
    ) {
        Section {
            WizardVideoSourcePickerView(
                onChange = { cameraId ->
                    vTuber.updateCameraId(
                        settingsCameraId = model.cameraIdToSettingsCameraId(cameraId = cameraId),
                    )
                },
                items = videoSources.all().map {
                    InlinePickerItem(id = it.id, text = it.name)
                },
                selectedId = model.getCameraId(vTuberWidget = vTuber),
            )
        }
        WidgetVTuberPickerView(
            model = model,
            vTuber = vTuber,
        )
        Box(
            modifier = Modifier.alpha(if (modelName.isEmpty()) 0.5f else 1f),
        ) {
            WidgetWizardSelectScenesNavigationView(
                model = model,
                database = database,
                createWidgetWizard = createWidgetWizard,
                presentingCreateWizard = presentingCreateWizard,
                onPresentingCreateWizardChange = onChangePresentingCreateWizard,
            )
        }
    }
}
