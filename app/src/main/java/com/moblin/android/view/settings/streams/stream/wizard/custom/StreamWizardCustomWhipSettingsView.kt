package com.moblin.android.view.settings.streams.stream.wizard.custom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.localized
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StreamState
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.utils.FormFieldError

private fun nextDisabled(customWhipUrl: String, urlError: String): Boolean {
    return customWhipUrl.isEmpty() || urlError.isNotEmpty()
}

private fun updateUrlError(customWhipUrl: String): String {
    val url = cleanUrl(url = customWhipUrl)
    return if (url.isEmpty()) {
        ""
    } else {
        isValidUrl(url = url, allowedSchemes = listOf("whip", "whips")) ?: ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardCustomWhipSettingsView(
    model: Model,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit,
) {
    val customWhipUrl by createStreamWizard.customWhipUrl.collectAsState()
    var urlError by remember { mutableStateOf("") }
    val disabled = nextDisabled(customWhipUrl, urlError)

    LaunchedEffect(Unit) {
        createStreamWizard.customProtocol.value = StreamState.whip
        createStreamWizard.name.value = makeUniqueName(
            name = localized("Custom WHIP"),
            existingNames = model.database.streams,
        )
    }

    LaunchedEffect(customWhipUrl) {
        urlError = updateUrlError(customWhipUrl)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(text = localized("WHIP"))
            },
            actions = {
                CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
            },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = localized("URL"),
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedTextField(
                value = customWhipUrl,
                onValueChange = { value ->
                    createStreamWizard.customWhipUrl.value = value
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(text = "whip://120.12.32.12:8889/mystream/whip")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
            )
            FormFieldError(error = urlError)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !disabled) {
                        onNavigate("StreamWizardGeneralSettingsView")
                    },
            ) {
                WizardNextButtonView()
            }
        }
    }
}
