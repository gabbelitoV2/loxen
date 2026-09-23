package com.moblin.android.view.settings.streams.stream.wizard.custom

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardCustomProtocol
import com.moblin.android.various.model.WizardPlatform
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardSkipButtonView
import com.moblin.android.view.settings.streams.stream.wizard.StreamWizardGeneralSettingsView

@Composable
fun StreamWizardCustomSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    LaunchedEffect(Unit) {
        createStreamWizard.platform = WizardPlatform.custom
        createStreamWizard.customProtocol = WizardCustomProtocol.none
        createStreamWizard.name = makeUniqueName(
            name = localized("Custom"),
            existingNames = model.database.streams,
        )
    }
    Form(
        title = "Custom",
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        },
    ) {
        Section(header = "Protocol") {
            NavigationLink(
                destination = {
                    StreamWizardCustomSrtSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard,
                    )
                },
            ) {
                Text(localized("SRT(LA)"))
            }
            NavigationLink(
                destination = {
                    StreamWizardCustomRtmpSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard,
                    )
                },
            ) {
                Text(localized("RTMP(S)"))
            }
            NavigationLink(
                destination = {
                    StreamWizardCustomRistSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard,
                    )
                },
            ) {
                Text(localized("RIST"))
            }
            NavigationLink(
                destination = {
                    StreamWizardCustomWhipSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard,
                    )
                },
            ) {
                Text(localized("WHIP"))
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
            ) {
                WizardSkipButtonView()
            }
        }
    }
}
