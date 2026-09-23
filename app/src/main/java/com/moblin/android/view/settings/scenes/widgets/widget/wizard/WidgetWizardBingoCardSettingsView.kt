package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidgetBingoCard
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.settings.scenes.widgets.widget.bingocard.BingCardWidgetSquaresView
import com.moblin.android.view.utils.CloseToolbar

@Composable
fun WidgetWizardBingoCardSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    bingoCard: SettingsWidgetBingoCard,
    createWidgetWizard: CreateWidgetWizard,
    presentingCreateWizard: Boolean,
    onPresentingCreateWizardChange: (Boolean) -> Unit,
) {
    val squaresText = binding(
        get = { bingoCard.squaresText },
        set = { newValue ->
            bingoCard.squaresText = newValue
            bingoCard.squaresTextChanged()
        },
    )

    Form(
        title = basicWidgetSettingsTitle(createWidgetWizard),
        toolbar = {
            CloseToolbar(presentingCreateWizard, onPresentingCreateWizardChange)
        },
    ) {
        BingCardWidgetSquaresView(
            value = squaresText.value,
            onValueChange = { squaresText.value = it },
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
