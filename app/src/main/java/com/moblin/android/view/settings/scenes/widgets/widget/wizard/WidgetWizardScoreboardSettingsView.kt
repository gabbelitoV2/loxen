package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidgetScoreboard
import com.moblin.android.various.settings.SettingsWidgetScoreboardSport
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.utils.CloseToolbarButtonView

@Composable
fun WidgetWizardScoreboardSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    scoreboard: SettingsWidgetScoreboard,
    createWidgetWizard: CreateWidgetWizard,
    presentingCreateWizard: Boolean,
    onChangePresentingCreateWizard: (Boolean) -> Unit,
    onChangeSport: (SettingsWidgetScoreboardSport) -> Unit,
) {
    val sport = scoreboard.sport
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
            Picker(
                title = "Sport",
                selection = sport,
                options = SettingsWidgetScoreboardSport.entries,
                text = { it.toString() },
                onChange = onChangeSport,
            )
        }
        WidgetWizardSelectScenesNavigationView(
            model = model,
            database = database,
            createWidgetWizard = createWidgetWizard,
            presentingCreateWizard = presentingCreateWizard,
            onPresentingCreateWizardChange = onChangePresentingCreateWizard,
        )
    }
}
