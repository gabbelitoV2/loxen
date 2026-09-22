package com.moblin.android.view.settings.streams.stream.wizard.custom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardCustomProtocol
import com.moblin.android.various.utils.extractSrtStreamId
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.wizard.StreamWizardGeneralSettingsView
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.utils.FormFieldError
import com.moblin.android.localized
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
fun StreamWizardSrtUrlSettingsView(
    createStreamWizard: CreateStreamWizard,
    urlError: String,
    onUrlErrorChange: (String) -> Unit,
) {
    val url = createStreamWizard.customSrtUrl
    LaunchedEffect(url) {
        val cleaned = cleanUrl(value = url)
        if (cleaned.isEmpty()) {
            onUrlErrorChange("")
        } else {
            onUrlErrorChange(isValidUrl(value = cleaned, allowedSchemes = listOf("srt", "srtla")) ?: "")
        }
        createStreamWizard.customSrtStreamId = extractSrtStreamId(url = url) ?: ""
    }
    LazyColumn {
        item {
            Text("URL")
        }
        item {
            OutlinedTextField(
                value = createStreamWizard.customSrtUrl,
                onValueChange = { createStreamWizard.customSrtUrl = it },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                singleLine = true,
            )
        }
        item {
            FormFieldError(error = urlError)
        }
        item {
            Text("Stream id")
        }
        item {
            OutlinedTextField(
                value = createStreamWizard.customSrtStreamId,
                onValueChange = { createStreamWizard.customSrtStreamId = it },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                singleLine = true,
            )
        }
        item {
            Text("Replaces or adds the stream id to the URL.")
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
    Column(modifier = Modifier.fillMaxWidth()) {
        CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        Text("SRT(LA)")
        StreamWizardSrtUrlSettingsView(
            createStreamWizard = createStreamWizard,
            urlError = urlError,
            onUrlErrorChange = { urlError = it },
        )
        TextButton(
            onClick = { onNavigate("StreamWizardGeneralSettingsView") },
            enabled = !nextDisabled,
        ) {
            WizardNextButtonView()
        }
    }
    LaunchedEffect(Unit) {
        createStreamWizard.customProtocol = WizardCustomProtocol.srt
        createStreamWizard.name = makeUniqueName(
            name = localized("Custom SRT"),
            existingNames = model.database.streams,
        )
    }
}

private fun localized(key: String): String = key
