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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.localized
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.utils.FormFieldError
import com.moblin.android.LocalModel

private fun nextDisabled(url: String, streamKey: String, urlError: String): Boolean {
    return url.isEmpty() || streamKey.isEmpty() || urlError.isNotEmpty()
}

private fun updateUrlError(url: String): String {
    val cleanedUrl = cleanUrl(url)
    return if (cleanedUrl.isEmpty()) {
        ""
    } else {
        isValidUrl(cleanedUrl, listOf("rtmp", "rtmps"), false) ?: ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardCustomRtmpSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onCustomRtmpUrlChange: (String) -> Unit = {},
    onCustomRtmpStreamKeyChange: (String) -> Unit = {},
    onCustomProtocolRtmp: () -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onNavigate: (String) -> Unit = {},
) {
    val customRtmpUrl = createStreamWizard.customRtmpUrl
    val customRtmpStreamKey = createStreamWizard.customRtmpStreamKey
    var urlError by remember { mutableStateOf("") }

    LaunchedEffect(customRtmpUrl) {
        urlError = updateUrlError(customRtmpUrl)
    }

    LaunchedEffect(Unit) {
        onCustomProtocolRtmp()
        onNameChange(
            makeUniqueName(
                localized("Custom RTMP"),
                model.database.streams
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RTMP(S)") },
                actions = {
                    CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                Text(
                    text = "URL",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = customRtmpUrl,
                    onValueChange = onCustomRtmpUrlChange,
                    placeholder = { Text("rtmp://arn03.contribute.live-video.net/app/") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Uri
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
            item {
                FormFieldError(error = urlError)
            }
            item {
                Text(
                    text = "Stream key",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = customRtmpStreamKey,
                    onValueChange = onCustomRtmpStreamKeyChange,
                    placeholder = { Text("live_48950233_okF4f455GRWEF443fFr23GRbt5rEv") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Uri
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = !nextDisabled(customRtmpUrl, customRtmpStreamKey, urlError),
                            onClick = { onNavigate("StreamWizardGeneralSettingsView") }
                        )
                ) {
                    WizardNextButtonView()
                }
            }
        }
    }
}
