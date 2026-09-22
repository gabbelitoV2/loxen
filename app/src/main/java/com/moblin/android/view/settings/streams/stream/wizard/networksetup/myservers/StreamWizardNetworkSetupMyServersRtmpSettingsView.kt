package com.moblin.android.view.settings.streams.stream.wizard.networksetup.myservers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardCustomProtocol
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.utils.FormFieldError
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private fun nextDisabled(createStreamWizard: CreateStreamWizard, urlError: String): Boolean {
    return createStreamWizard.customRtmpUrl.isEmpty() ||
        createStreamWizard.customRtmpStreamKey.isEmpty() ||
        urlError.isNotEmpty()
}

private fun updateUrlError(createStreamWizard: CreateStreamWizard): String {
    val url = cleanUrl(createStreamWizard.customRtmpUrl)
    if (url.isEmpty()) {
        return ""
    }
    return isValidUrl(url, listOf("rtmp", "rtmps"), false) ?: ""
}

@Composable
fun StreamWizardNetworkSetupMyServersRtmpSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    val customRtmpUrl = createStreamWizard.customRtmpUrl
    val customRtmpStreamKey = createStreamWizard.customRtmpStreamKey
    var urlError by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        createStreamWizard.customProtocol = WizardCustomProtocol.rtmp
    }
    LaunchedEffect(customRtmpUrl) {
        urlError = updateUrlError(createStreamWizard)
    }

    Scaffold(
        topBar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Column {
                    Text("URL")
                    OutlinedTextField(
                        value = customRtmpUrl,
                        onValueChange = {
                            createStreamWizard.customRtmpUrl = it
                        },
                        placeholder = {
                            Text("rtmp://arn03.contribute.live-video.net/app/")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    FormFieldError(error = urlError)
                }
            }
            item {
                Column {
                    Text("Stream key")
                    OutlinedTextField(
                        value = customRtmpStreamKey,
                        onValueChange = {
                            createStreamWizard.customRtmpStreamKey = it
                        },
                        placeholder = {
                            Text("live_48950233_okF4f455GRWEF443fFr23GRbt5rEv")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                WizardNextButtonView()
            }
        }
    }
}
