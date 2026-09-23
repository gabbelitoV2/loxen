package com.moblin.android.view.settings.streams.stream.wizard.networksetup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formFootnoteStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardNetworkSetup
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.settings.streams.stream.wizard.StreamWizardObsRemoteControlSettingsView
import com.moblin.android.view.utils.FormFieldError
import com.moblin.android.view.utils.HCenter

@Composable
fun StreamWizardNetworkSetupObsSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val palette = formPalette()
    var obsAddress by remember { mutableStateOf(createStreamWizard.obsAddress) }
    var obsPort by remember { mutableStateOf(createStreamWizard.obsPort) }
    var portError by remember { mutableStateOf("") }

    fun updatePortError(value: String) {
        val port = value.trim()
        portError = when {
            port.isEmpty() -> ""
            else -> {
                val parsed = port.toUShortOrNull()
                if (parsed != null && parsed.toInt() > 0) {
                    ""
                } else {
                    localized("Must be a number between 1 and 65535.")
                }
            }
        }
    }

    fun nextDisabled(): Boolean {
        return obsAddress.trim().isEmpty() ||
            obsPort.trim().isEmpty() ||
            portError.isNotEmpty()
    }

    DisposableEffect(Unit) {
        createStreamWizard.networkSetup = WizardNetworkSetup.obs
        updatePortError(obsPort)
        onDispose {}
    }

    Form(
        title = "OBS",
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        },
    ) {
        Section(
            header = localized("IP address or domain name"),
            footer = localized("Your public IP address if streaming over the internet."),
        ) {
            BasicTextField(
                value = obsAddress,
                onValueChange = {
                    obsAddress = it
                    createStreamWizard.obsAddress = it
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = formBodyStyle.copy(color = palette.label),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Uri,
                ),
                cursorBrush = SolidColor(palette.accent),
                decorationBox = { innerTextField ->
                    Box {
                        if (obsAddress.isEmpty()) {
                            Text(
                                text = localized("213.33.45.132"),
                                style = formBodyStyle,
                                color = palette.tertiaryLabel,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
        Section(
            header = localized("Port"),
            footerContent = {
                Column(horizontalAlignment = Alignment.Start) {
                    FormFieldError(error = portError)
                    Text(
                        text = localized(
                            "Configure port forwarding in your router to forward incoming traffic to OBS.",
                        ),
                        style = formFootnoteStyle,
                        color = palette.secondaryLabel,
                    )
                }
            },
        ) {
            BasicTextField(
                value = obsPort,
                onValueChange = {
                    obsPort = it
                    createStreamWizard.obsPort = it
                    updatePortError(it)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = formBodyStyle.copy(color = palette.label),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Number,
                ),
                cursorBrush = SolidColor(palette.accent),
                decorationBox = { innerTextField ->
                    Box {
                        if (obsPort.isEmpty()) {
                            Text(
                                text = localized("7654"),
                                style = formBodyStyle,
                                color = palette.tertiaryLabel,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
        Section(header = localized("Configure OBS on your computer")) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = localized(
                        "1. Create a Media Source in OBS and configure it as shown in the image below.",
                    ),
                    style = formBodyStyle,
                    color = palette.label,
                )
                Text(
                    text = "",
                    style = formBodyStyle,
                    color = palette.label,
                )
                Text(
                    text = localized("2. Replace 7654 with your port."),
                    style = formBodyStyle,
                    color = palette.label,
                )
                HCenter {
                    Bundle.image("ObsMediaSourceSrt")?.let { image ->
                        Image(
                            bitmap = image.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
        }
        Section {
            NavigationLink(
                destination = {
                    StreamWizardObsRemoteControlSettingsView(
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
