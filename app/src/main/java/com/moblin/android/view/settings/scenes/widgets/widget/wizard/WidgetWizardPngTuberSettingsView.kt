package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidgetPngTuber
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.settings.scenes.widgets.widget.pngtuber.WidgetPngTuberPickerView
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetWizardPngTuberSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    pngTuber: SettingsWidgetPngTuber,
    createWidgetWizard: CreateWidgetWizard,
    presentingCreateWizard: Boolean,
    onPresentingCreateWizardChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(basicWidgetSettingsTitle(createWidgetWizard))
            },
            actions = {
                CloseToolbar(
                    presenting = presentingCreateWizard,
                    onPresentingChange = onPresentingCreateWizardChange,
                )
            },
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                WidgetPngTuberPickerView(
                    model = model,
                    pngTuber = pngTuber,
                )
            }
            item {
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
}
