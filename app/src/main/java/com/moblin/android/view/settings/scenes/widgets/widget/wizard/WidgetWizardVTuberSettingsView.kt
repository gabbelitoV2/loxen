package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidgetVTuber
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.settings.scenes.widgets.widget.vtuber.WidgetVTuberPickerView
import com.moblin.android.view.utils.CloseToolbarButtonView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetWizardVTuberSettingsView(
    model: Model,
    database: Database,
    vTuber: SettingsWidgetVTuber,
    createWidgetWizard: CreateWidgetWizard,
    presentingCreateWizard: Boolean,
    onChangePresentingCreateWizard: (Boolean) -> Unit,
) {
    val modelName = vTuber.modelName.collectAsState().value
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(basicWidgetSettingsTitle(createWidgetWizard))
                },
                actions = {
                    CloseToolbarButtonView(
                        onClick = {
                            onChangePresentingCreateWizard(false)
                        },
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                WidgetVTuberPickerView(
                    model = model,
                    vTuber = vTuber,
                )
            }
            item {
                Box(
                    modifier = Modifier.alpha(if (modelName.isEmpty()) 0.5f else 1f),
                ) {
                    WidgetWizardSelectScenesNavigationView(
                        model = model,
                        database = database,
                        createWidgetWizard = createWidgetWizard,
                        presentingCreateWizard = presentingCreateWizard,
                        onChangePresentingCreateWizard = onChangePresentingCreateWizard,
                    )
                }
            }
        }
    }
}
