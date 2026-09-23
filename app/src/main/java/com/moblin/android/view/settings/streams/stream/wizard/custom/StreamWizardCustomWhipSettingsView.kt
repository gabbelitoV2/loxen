package com.moblin.android.view.settings.streams.stream.wizard.custom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
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

private fun nextDisabled(customWhipUrl: String, urlError: String): Boolean {
    return customWhipUrl.isEmpty() || urlError.isNotEmpty()
}

private fun updateUrlError(customWhipUrl: String): String {
    val url = cleanUrl(value = customWhipUrl)
    return if (url.isEmpty()) {
        ""
    } else {
        isValidUrl(value = url, allowedSchemes = listOf("whip", "whips")) ?: ""
    }
}

@Composable
fun StreamWizardCustomWhipSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val palette = formPalette()
    val customWhipUrl = createStreamWizard.customWhipUrl
    var urlError by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        createStreamWizard.customProtocol = WizardCustomProtocol.whip
        createStreamWizard.name = makeUniqueName(
            name = localized("Custom WHIP"),
            existingNames = model.database.streams,
        )
    }

    Form(
        title = "WHIP",
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        },
    ) {
        Section(
            header = "URL",
            footerContent = {
                FormFieldError(error = urlError)
            },
        ) {
            BasicTextField(
                value = customWhipUrl,
                onValueChange = { value ->
                    createStreamWizard.customWhipUrl = value
                    urlError = updateUrlError(value)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = formBodyStyle.copy(color = palette.label),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
                singleLine = true,
                cursorBrush = SolidColor(palette.accent),
                decorationBox = { innerTextField ->
                    Box {
                        if (customWhipUrl.isEmpty()) {
                            Text(
                                text = "whip://120.12.32.12:8889/mystream/whip",
                                style = formBodyStyle,
                                color = palette.tertiaryLabel,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
        Section {
            NavigationLink(
                destination = {
                    StreamWizardGeneralSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard,
                    )
                },
                enabled = !nextDisabled(customWhipUrl, urlError),
            ) {
                WizardNextButtonView()
            }
        }
    }
}
