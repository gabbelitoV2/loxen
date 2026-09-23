package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.*
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidgetText
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.settings.scenes.widgets.widget.text.TextWidgetSuggestionsView
import com.moblin.android.view.settings.scenes.widgets.widget.text.TextWidgetTextView
import com.moblin.android.view.utils.CloseToolbarButtonView

@Composable
fun WidgetWizardTextSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    createWidgetWizard: CreateWidgetWizard,
    text: SettingsWidgetText,
    presentingCreateWizard: Boolean,
    onPresentingCreateWizardChange: (Boolean) -> Unit,
) {
    Form(
        title = basicWidgetSettingsTitle(createWidgetWizard),
        toolbar = {
            CloseToolbarButtonView(
                presenting = presentingCreateWizard,
                onPresentingChange = { onPresentingCreateWizardChange(it) },
            )
        },
    ) {
        Section {
            TextWidgetTextView(
                value = text.formatString,
                onChange = { text.formatString = it },
            )
        }
        Section {
            TextWidgetSuggestionsView(
                widget = true,
                text = text.formatString,
                onChange = { text.formatString = it },
            )
        }
        WidgetWizardSelectScenesNavigationView(
            model = model,
            database = database,
            createWidgetWizard = createWidgetWizard,
            presentingCreateWizard = presentingCreateWizard,
            onPresentingCreateWizardChange = { onPresentingCreateWizardChange(it) },
        )
    }
}
