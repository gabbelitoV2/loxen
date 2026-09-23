package com.moblin.android.view.settings.streams.stream.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardPlatform
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView

@Composable
fun StreamWizardMobcamSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    LaunchedEffect(Unit) {
        createStreamWizard.platform = WizardPlatform.mobcam
        createStreamWizard.name = makeUniqueName(
            localized("Custom Mobcam"),
            model.database.streams,
        )
    }
    Form(
        title = localized("Mobcam"),
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        },
    ) {
        Section {
            Text(
                text = localized(
                    "Use Moblin as a low latency camera in OBS Studio over USB.",
                ),
            )
        }
        Section(header = localized("Configure OBS on your computer")) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = localized(
                        "1. Install the OBS Mobcam Plugin as described " +
                            "[here](https://github.com/eerimoq/mobcam/tree/main/crates/obs-plugin#mobcam-obs-plugin).",
                    ),
                )
                Text("")
                Text("2. Connect Moblin to the computer with a USB cable.")
                Text("")
                Text("3. Add a Mobcam source in OBS.")
                Text("")
                Text("4. Press Go live in Moblin to start the stream to OBS.")
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
                WizardNextButtonView()
            }
        }
    }
}
