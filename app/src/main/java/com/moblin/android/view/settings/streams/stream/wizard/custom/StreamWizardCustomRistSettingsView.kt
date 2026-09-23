package com.moblin.android.view.settings.streams.stream.wizard.custom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardCustomProtocol
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.settings.streams.stream.wizard.StreamWizardGeneralSettingsView
import com.moblin.android.view.utils.FormFieldError

@Composable
fun StreamWizardCustomRistSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val palette = formPalette()
    val urlError = remember { mutableStateOf("") }
    val customRistUrl = createStreamWizard.customRistUrl

    fun updateUrlError(url: String) {
        val cleaned = cleanUrl(url)
        urlError.value = if (cleaned.isEmpty()) {
            ""
        } else {
            isValidUrl(cleaned, listOf("rist")) ?: ""
        }
    }

    fun nextDisabled(): Boolean = customRistUrl.isEmpty() || urlError.value.isNotEmpty()

    LaunchedEffect(Unit) {
        createStreamWizard.customProtocol = WizardCustomProtocol.rist
        createStreamWizard.name = makeUniqueName(
            localized("Custom RIST"), model.database.streams,
        )
    }

    Form(
        title = "RIST",
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        },
    ) {
        Section(
            header = "URL",
            footerContent = {
                FormFieldError(error = urlError.value)
            },
        ) {
            FormRow {
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = customRistUrl,
                        onValueChange = {
                            createStreamWizard.customRistUrl = it
                            updateUrlError(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = formBodyStyle.copy(color = palette.label),
                        cursorBrush = SolidColor(palette.accent),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                        ),
                    )
                    if (customRistUrl.isEmpty()) {
                        Text(
                            text = localized("rist://120.35.234.2:2030"),
                            style = formBodyStyle,
                            color = palette.tertiaryLabel,
                        )
                    }
                }
            }
        }
        Section {
            NavigationLink(
                destination = {
                    StreamWizardGeneralSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard,
                    )
                },
                enabled = !nextDisabled(),
            ) {
                WizardNextButtonView()
            }
        }
    }
}
