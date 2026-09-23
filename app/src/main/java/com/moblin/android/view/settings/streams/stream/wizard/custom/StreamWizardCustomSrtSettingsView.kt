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
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardCustomProtocol
import com.moblin.android.various.utils.extractSrtStreamId
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.settings.streams.stream.wizard.StreamWizardGeneralSettingsView
import com.moblin.android.view.utils.FormFieldError

@Composable
private fun FormTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = formPalette()
    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = formBodyStyle,
                color = palette.tertiaryLabel,
                maxLines = 1,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = formBodyStyle.copy(color = palette.label),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            singleLine = true,
            cursorBrush = SolidColor(palette.accent),
        )
    }
}

private fun updateUrlError(url: String): String {
    val cleaned = cleanUrl(value = url)
    return if (cleaned.isEmpty()) {
        ""
    } else {
        isValidUrl(value = cleaned, allowedSchemes = listOf("srt", "srtla")) ?: ""
    }
}

@Composable
fun StreamWizardSrtUrlSettingsView(
    createStreamWizard: CreateStreamWizard,
    urlError: String,
    onUrlErrorChange: (String) -> Unit,
) {
    Section(
        header = localized("URL"),
        footerContent = { FormFieldError(error = urlError) },
    ) {
        FormRow {
            FormTextField(
                value = createStreamWizard.customSrtUrl,
                placeholder = "srt://107.32.12.132:5000?streamid=1234",
                onValueChange = { newValue ->
                    createStreamWizard.customSrtUrl = newValue
                    onUrlErrorChange(updateUrlError(url = newValue))
                    createStreamWizard.customSrtStreamId =
                        extractSrtStreamId(url = newValue) ?: ""
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Section(
        header = localized("Stream id"),
        footer = localized("Replaces or adds the stream id to the URL."),
    ) {
        FormRow {
            FormTextField(
                value = createStreamWizard.customSrtStreamId,
                placeholder = "#!::r=stream/-NDZ1WPA4zjMBTJTyNwU,m=publish,...",
                onValueChange = { newValue -> createStreamWizard.customSrtStreamId = newValue },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun StreamWizardCustomSrtSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var urlError by remember { mutableStateOf("") }
    val nextDisabled = createStreamWizard.customSrtUrl.isEmpty() ||
        createStreamWizard.customSrtStreamId.isEmpty() ||
        urlError.isNotEmpty()
    LaunchedEffect(Unit) {
        createStreamWizard.customProtocol = WizardCustomProtocol.srt
        createStreamWizard.name = makeUniqueName(
            name = localized("Custom SRT"),
            existingNames = model.database.streams,
        )
    }
    Form(
        title = localized("SRT(LA)"),
        toolbar = { CreateStreamWizardToolbar(createStreamWizard = createStreamWizard) },
    ) {
        StreamWizardSrtUrlSettingsView(
            createStreamWizard = createStreamWizard,
            urlError = urlError,
            onUrlErrorChange = { urlError = it },
        )
        Section {
            NavigationLink(
                enabled = !nextDisabled,
                destination = {
                    StreamWizardGeneralSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard,
                    )
                },
            ) {
                WizardNextButtonView()
            }
        }
    }
}
