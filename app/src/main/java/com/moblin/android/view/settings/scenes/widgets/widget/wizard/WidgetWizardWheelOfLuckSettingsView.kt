package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidgetWheelOfLuck
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.settings.scenes.widgets.widget.wheelofluck.WheelOfLuckWidgetOptionsView
import com.moblin.android.view.utils.CloseToolbarButtonView
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetWizardWheelOfLuckSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    wheelOfLuck: SettingsWidgetWheelOfLuck,
    createWidgetWizard: CreateWidgetWizard,
    presentingCreateWizard: Boolean,
    onPresentingCreateWizardChange: (Boolean) -> Unit,
) {
    val text by wheelOfLuck.text.collectAsState()
    val options by wheelOfLuck.options.collectAsState()

    LaunchedEffect(text) {
        wheelOfLuck.optionsFromText(text)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(basicWidgetSettingsTitle(createWidgetWizard)) },
                actions = {
                    CloseToolbarButtonView(
                        presenting = presentingCreateWizard,
                        onPresentingChange = onPresentingCreateWizardChange,
                    )
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                WheelOfLuckWidgetOptionsView(
                    value = text,
                    onValueChange = { newText ->
                        wheelOfLuck.text.value = newText
                    },
                )
            }
            item {
                WidgetWizardSelectScenesNavigationView(
                    model = model,
                    database = database,
                    createWidgetWizard = createWidgetWizard,
                    presentingCreateWizard = presentingCreateWizard,
                    onPresentingCreateWizardChange = onPresentingCreateWizardChange,
                    enabled = options.isNotEmpty(),
                )
            }
        }
    }
}
