package com.moblin.android.view.settings.scenes.widgets.widget.effects

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectType
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.utils.CloseToolbarButtonView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.various.model.resetSelectedScene

@Composable
fun WidgetEffectWizardSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    presentingCreateWizard: Boolean,
    onChangePresentingCreateWizard: (Boolean) -> Unit,
) {
    Form(
        title = "Create effect wizard",
        toolbar = {
            CloseToolbarButtonView(
                presenting = presentingCreateWizard,
                onPresentingChange = onChangePresentingCreateWizard,
            )
        },
    ) {
        Section {
            Picker(
                title = "Type",
                selection = effect.type,
                options = SettingsVideoEffectType.entries,
                text = { it.toString() },
            ) { effect.type = it }
        }
        Section {
            TextButtonView("Create") {
                create(
                    model = model,
                    widget = widget,
                    effect = effect,
                    onChangePresentingCreateWizard = onChangePresentingCreateWizard,
                )
            }
        }
    }
}

private fun create(
    model: Model,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    onChangePresentingCreateWizard: (Boolean) -> Unit,
) {
    onChangePresentingCreateWizard(false)
    widget.effects = widget.effects + effect
    model.resetSelectedScene(changeScene = false)
}
