package com.moblin.android.view.settings.streams.stream.wizard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.utils.FormFieldError

private fun nextDisabled(createStreamWizard: CreateStreamWizard, urlError: String): Boolean {
    if (createStreamWizard.obsRemoteControlEnabled) {
        if (createStreamWizard.obsRemoteControlUrl.isEmpty() ||
            createStreamWizard.obsRemoteControlPassword.isEmpty() ||
            urlError.isNotEmpty()
        ) {
            return true
        }
    }
    return false
}

@Composable
private fun ObsTextFieldRow(
    value: String,
    placeholder: String,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Unspecified,
    onValueChange: (String) -> Unit,
) {
    val palette = formPalette()
    FormRow {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(text = placeholder, color = palette.secondaryLabel, style = formBodyStyle)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = formBodyStyle.copy(color = palette.label),
                singleLine = true,
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = capitalization,
                    autoCorrectEnabled = false,
                ),
            )
        }
    }
}

@Composable
fun StreamWizardObsRemoteControlSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var urlError by remember { mutableStateOf("") }

    val obsRemoteControlEnabled = binding(
        get = { createStreamWizard.obsRemoteControlEnabled },
        set = { createStreamWizard.obsRemoteControlEnabled = it },
    )
    val obsRemoteControlUrl = binding(
        get = { createStreamWizard.obsRemoteControlUrl },
        set = {
            createStreamWizard.obsRemoteControlUrl = it
            urlError = isValidWebSocketUrl(value = cleanUrl(value = it)) ?: ""
        },
    )
    val obsRemoteControlPassword = binding(
        get = { createStreamWizard.obsRemoteControlPassword },
        set = { createStreamWizard.obsRemoteControlPassword = it },
    )
    val obsRemoteControlMainScene = binding(
        get = { createStreamWizard.obsRemoteControlMainScene },
        set = { createStreamWizard.obsRemoteControlMainScene = it },
    )
    val obsRemoteControlBrbScene = binding(
        get = { createStreamWizard.obsRemoteControlBrbScene },
        set = { createStreamWizard.obsRemoteControlBrbScene = it },
    )
    val obsRemoteControlSourceName = binding(
        get = { createStreamWizard.obsRemoteControlSourceName },
        set = { createStreamWizard.obsRemoteControlSourceName = it },
    )

    LaunchedEffect(Unit) {
        urlError = isValidWebSocketUrl(
            value = cleanUrl(value = createStreamWizard.obsRemoteControlUrl),
        ) ?: ""
    }

    Form(
        title = "OBS remote control",
        toolbar = { CreateStreamWizardToolbar(createStreamWizard = createStreamWizard) },
    ) {
        Section {
            Toggle("Enabled", isOn = obsRemoteControlEnabled)
        }
        if (obsRemoteControlEnabled.value) {
            Section(
                header = "URL",
                footerContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FormFieldError(error = urlError)
                        Text("Use your public IP address if streaming over the internet.")
                        Text("")
                        Text("Configure port forwarding in your router to forward incoming traffic to OBS.")
                    }
                },
            ) {
                ObsTextFieldRow(
                    value = obsRemoteControlUrl.value,
                    placeholder = "ws://213.33.45.132:4567",
                    capitalization = KeyboardCapitalization.None,
                    onValueChange = { obsRemoteControlUrl.value = it },
                )
            }
            Section(
                header = "Password",
                footerContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Copy from OBS Show Connect Info as seen in the screenshot below. " +
                                "Tools → WebSocket Server Settings → Show Connect Info → Server Password.",
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            com.moblin.android.platform.Bundle.image("ObsRemoteControl")?.asImageBitmap()?.let {
                                Image(
                                    bitmap = it,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                },
            ) {
                ObsTextFieldRow(
                    value = obsRemoteControlPassword.value,
                    placeholder = "po3Gg4pflp3s",
                    capitalization = KeyboardCapitalization.None,
                    onValueChange = { obsRemoteControlPassword.value = it },
                )
            }
            Section(
                header = "Main scene",
                footer = "The name of your main scene in OBS. Moblin will periodically try to " +
                    "switch to this scene from your BRB scene if the stream is likely working.",
            ) {
                ObsTextFieldRow(
                    value = obsRemoteControlMainScene.value,
                    placeholder = "Main scene",
                    onValueChange = { obsRemoteControlMainScene.value = it },
                )
            }
            Section(
                header = "BRB scene",
                footer = "The name of your BRB scene in OBS. Moblin will periodically try to " +
                    "switch from your main scene to this scene if the stream is likely broken.",
            ) {
                ObsTextFieldRow(
                    value = obsRemoteControlBrbScene.value,
                    placeholder = "My BRB scene",
                    onValueChange = { obsRemoteControlBrbScene.value = it },
                )
            }
            Section(
                header = "Source name",
                footer = "The name of the Source in OBS that receives the stream from Moblin.",
            ) {
                ObsTextFieldRow(
                    value = obsRemoteControlSourceName.value,
                    placeholder = "My source",
                    onValueChange = { obsRemoteControlSourceName.value = it },
                )
            }
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
                    createStreamWizard = createStreamWizard,
                    urlError = urlError,
                ),
            ) {
                WizardNextButtonView()
            }
        }
    }
}
