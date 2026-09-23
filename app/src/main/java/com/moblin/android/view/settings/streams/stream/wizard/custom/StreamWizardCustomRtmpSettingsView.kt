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
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.LocalModel
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
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.settings.streams.stream.wizard.StreamWizardGeneralSettingsView
import com.moblin.android.view.utils.FormFieldError

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
    val palette = formPalette()
    var urlError by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        onCustomProtocolRtmp()
        onNameChange(
            makeUniqueName(
                localized("Custom RTMP"),
                model.database.streams,
            ),
        )
    }

    Form(
        title = localized("RTMP(S)"),
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        },
    ) {
        Section(
            header = localized("URL"),
            footerContent = {
                FormFieldError(error = urlError)
            },
        ) {
            BasicTextField(
                value = createStreamWizard.customRtmpUrl,
                onValueChange = { value ->
                    createStreamWizard.customRtmpUrl = value
                    onCustomRtmpUrlChange(value)
                    urlError = updateUrlError(value)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = formBodyStyle.copy(color = palette.label),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Uri,
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (createStreamWizard.customRtmpUrl.isEmpty()) {
                            Text(
                                text = "rtmp://arn03.contribute.live-video.net/app/",
                                style = formBodyStyle.copy(color = palette.secondaryLabel),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
        Section(header = localized("Stream key")) {
            BasicTextField(
                value = createStreamWizard.customRtmpStreamKey,
                onValueChange = { value ->
                    createStreamWizard.customRtmpStreamKey = value
                    onCustomRtmpStreamKeyChange(value)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = formBodyStyle.copy(color = palette.label),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Uri,
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (createStreamWizard.customRtmpStreamKey.isEmpty()) {
                            Text(
                                text = "live_48950233_okF4f455GRWEF443fFr23GRbt5rEv",
                                style = formBodyStyle.copy(color = palette.secondaryLabel),
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
                enabled = !nextDisabled(
                    createStreamWizard.customRtmpUrl,
                    createStreamWizard.customRtmpStreamKey,
                    urlError,
                ),
            ) {
                WizardNextButtonView()
            }
        }
    }
}
