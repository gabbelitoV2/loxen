package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.moblin.android.LocalModel
import com.moblin.android.common.various.isValidHttpUrl
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidgetBrowser
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.utils.CloseToolbar

@Composable
fun WidgetWizardBrowserSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    createWidgetWizard: CreateWidgetWizard,
    browser: SettingsWidgetBrowser,
    presentingCreateWizard: Boolean,
    onPresentingCreateWizardChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = formPalette()
    Form(
        title = basicWidgetSettingsTitle(createWidgetWizard),
        modifier = modifier,
        toolbar = {
            CloseToolbar(
                presenting = presentingCreateWizard,
                onPresentingChange = onPresentingCreateWizardChange
            )
        }
    ) {
        Section(
            header = "URL",
            footerContent = {
                val message = isValidHttpUrl(browser.url)
                if (message != null) {
                    Text(
                        text = message,
                        color = palette.red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        ) {
            FormRow {
                BasicTextField(
                    value = browser.url,
                    onValueChange = { browser.url = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = formBodyStyle.copy(color = palette.label),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done
                    ),
                    cursorBrush = SolidColor(palette.accent),
                    decorationBox = { innerTextField ->
                        Box {
                            if (browser.url.isEmpty()) {
                                Text(
                                    text = "https://example.com",
                                    style = formBodyStyle,
                                    color = palette.secondaryLabel
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
        WidgetWizardSelectScenesNavigationView(
            model = model,
            database = database,
            createWidgetWizard = createWidgetWizard,
            presentingCreateWizard = presentingCreateWizard,
            onPresentingCreateWizardChange = onPresentingCreateWizardChange
        )
    }
}
