package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import android.graphics.Bitmap
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.settings.scenes.widgets.widget.image.WidgetImagePickerView
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetWizardImageSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    widget: SettingsWidget,
    createWidgetWizard: CreateWidgetWizard,
    presentingCreateWizard: Boolean,
    onChangePresentingCreateWizard: (Boolean) -> Unit,
) {
    var image by remember { mutableStateOf<Bitmap?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(basicWidgetSettingsTitle(createWidgetWizard)) },
                actions = {
                    CloseToolbar(
                        presenting = presentingCreateWizard,
                        onChangePresenting = onChangePresentingCreateWizard,
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                WidgetImagePickerView(
                    model = model,
                    widget = widget,
                    image = image,
                    onChangeImage = { image = it },
                    sizeScale = 5,
                )
            }
            item {
                WidgetWizardSelectScenesNavigationView(
                    model = model,
                    database = database,
                    createWidgetWizard = createWidgetWizard,
                    presentingCreateWizard = presentingCreateWizard,
                    onChangePresentingCreateWizard = onChangePresentingCreateWizard,
                    enabled = image != null,
                )
            }
        }
    }
}
