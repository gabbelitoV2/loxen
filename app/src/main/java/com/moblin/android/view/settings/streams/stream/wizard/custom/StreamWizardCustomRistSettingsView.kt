package com.moblin.android.view.settings.streams.stream.wizard.custom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.localized
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.CustomProtocol
import com.moblin.android.various.model.Model
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.utils.FormFieldError
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardCustomRistSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val urlError = remember { mutableStateOf("") }
    val customRistUrl by createStreamWizard.customRistUrl.collectAsState()

    fun nextDisabled(): Boolean = customRistUrl.isEmpty() || urlError.value.isNotEmpty()

    fun updateUrlError() {
        val url = cleanUrl(customRistUrl)
        urlError.value = if (url.isEmpty()) {
            ""
        } else {
            isValidUrl(url, listOf("rist")) ?: ""
        }
    }

    LaunchedEffect(customRistUrl) {
        updateUrlError()
    }

    LaunchedEffect(Unit) {
        createStreamWizard.setCustomProtocol(CustomProtocol.rist)
        createStreamWizard.setName(
            makeUniqueName(localized("Custom RIST"), model.database.streams),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RIST") },
                actions = {
                    CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                Text("URL", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = customRistUrl,
                    onValueChange = { createStreamWizard.setCustomRistUrl(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("rist://120.35.234.2:2030") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                    ),
                )
                FormFieldError(error = urlError.value)
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !nextDisabled()) {
                            onNavigate("StreamWizardGeneralSettingsView")
                        },
                ) {
                    WizardNextButtonView()
                }
            }
        }
    }
}
