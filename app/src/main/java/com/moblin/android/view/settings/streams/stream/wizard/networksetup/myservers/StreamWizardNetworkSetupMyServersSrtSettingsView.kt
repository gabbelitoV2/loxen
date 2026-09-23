package com.moblin.android.view.settings.streams.stream.wizard.networksetup.myservers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardCustomProtocol
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.settings.streams.stream.wizard.StreamWizardObsRemoteControlSettingsView
import com.moblin.android.view.settings.streams.stream.wizard.custom.StreamWizardSrtUrlSettingsView
import com.moblin.android.various.settings.SettingsStreamProtocol

private fun nextDisabled(createStreamWizard: CreateStreamWizard, urlError: String): Boolean {
    return createStreamWizard.customSrtUrl.isEmpty()
        || createStreamWizard.customSrtStreamId.isEmpty()
        || urlError.isNotEmpty()
}

@Composable
fun StreamWizardNetworkSetupMyServersSrtSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    var urlError by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        createStreamWizard.customProtocol = WizardCustomProtocol.srt
    }

    Form(
        title = "SRT(LA)",
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        }
    ) {
        StreamWizardSrtUrlSettingsView(
            createStreamWizard = createStreamWizard,
            urlError = urlError,
            onUrlErrorChange = { urlError = it }
        )
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
