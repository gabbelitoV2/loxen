package com.moblin.android.view.settings.streams.stream.wizard.networksetup.myservers

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
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.platform.swiftui.*
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardCustomProtocol
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.settings.streams.stream.wizard.StreamWizardObsRemoteControlSettingsView
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

    Form(
        title = "RTMP(S)",
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        }
    ) {
        Section(
            header = "URL",
            footerContent = {
                FormFieldError(error = urlError)
            }
        ) {
            BasicTextField(
                value = customRtmpUrl,
                onValueChange = {
                    createStreamWizard.customRtmpUrl = it
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = formBodyStyle.copy(color = formPalette().label),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false
                ),
                singleLine = true,
                cursorBrush = SolidColor(formPalette().accent),
                decorationBox = { innerTextField ->
                    Box {
                        if (customRtmpUrl.isEmpty()) {
                            Text(
                                "rtmp://arn03.contribute.live-video.net/app/",
                                style = formBodyStyle.copy(color = formPalette().tertiaryLabel)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        Section(header = "Stream key") {
            BasicTextField(
                value = customRtmpStreamKey,
                onValueChange = {
                    createStreamWizard.customRtmpStreamKey = it
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = formBodyStyle.copy(color = formPalette().label),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false
                ),
                singleLine = true,
                cursorBrush = SolidColor(formPalette().accent),
                decorationBox = { innerTextField ->
                    Box {
                        if (customRtmpStreamKey.isEmpty()) {
                            Text(
                                "live_48950233_okF4f455GRWEF443fFr23GRbt5rEv",
                                style = formBodyStyle.copy(color = formPalette().tertiaryLabel)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        Section {
            NavigationLink(
                destination = {
                    StreamWizardObsRemoteControlSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard
                    )
                },
                enabled = !nextDisabled(createStreamWizard, urlError)
            ) {
                WizardNextButtonView()
            }
        }
    }
}
