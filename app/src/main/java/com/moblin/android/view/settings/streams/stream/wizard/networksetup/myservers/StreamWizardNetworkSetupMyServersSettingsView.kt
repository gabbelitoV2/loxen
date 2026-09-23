package com.moblin.android.view.settings.streams.stream.wizard.networksetup.myservers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardNetworkSetup
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar

@Composable
fun StreamWizardNetworkSetupMyServersSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    LaunchedEffect(Unit) {
        createStreamWizard.networkSetup = WizardNetworkSetup.myServers
    }
    Form(
        title = "My server(s)",
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        },
    ) {
        Section(header = "Protocol") {
            NavigationLink("SRT(LA)") {
                StreamWizardNetworkSetupMyServersSrtSettingsView(
                    model = model,
                    createStreamWizard = createStreamWizard,
                )
            }
            NavigationLink("RTMP(S)") {
                StreamWizardNetworkSetupMyServersRtmpSettingsView(
                    model = model,
                    createStreamWizard = createStreamWizard,
                )
            }
        }
    }
}
