package com.moblin.android.view.settings.streams.stream.wizard.networksetup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.wizard.networksetup.myservers.StreamWizardNetworkSetupMyServersSettingsView
import com.moblin.android.localized

@Composable
fun StreamWizardNetworkSetupSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    platform: String,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(
        title = "Network setup",
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        },
    ) {
        Section(
            footer = "Good stability in most network conditions.",
        ) {
            NavigationLink(
                destination = {
                    StreamWizardNetworkSetupObsSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard,
                    )
                },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(com.moblin.android.localized("Moblin"))
                    SystemImage("arrow.right", fontSize = 17.sp)
                    Text("OBS")
                    SystemImage("arrow.right", fontSize = 17.sp)
                    Text(platform)
                }
            }
        }
        Section(
            footer = "Best possible stability. Uses bonding. Paid third-party service.",
        ) {
            NavigationLink(
                destination = {
                    StreamWizardNetworkSetupBelaboxSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard,
                    )
                },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = com.moblin.android.localized("Moblin"))
                    SystemImage("arrow.right", fontSize = 17.sp)
                    Text("BELABOX cloud")
                    SystemImage("arrow.right", fontSize = 17.sp)
                    Text("OBS")
                    SystemImage("arrow.right", fontSize = 17.sp)
                    Text(platform)
                }
            }
        }
        Section(
            footer = "Often bad stability if network connection is unstable. No server side disconnection protection possible.",
        ) {
            NavigationLink(
                destination = {
                    StreamWizardNetworkSetupDirectSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard,
                    )
                },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(com.moblin.android.localized(text = "Moblin"))
                    SystemImage("arrow.right", fontSize = 17.sp)
                    Text(platform)
                }
            }
        }
        Section(
            footer = "Best possible stability. May use bonding. Most flexible setup.",
        ) {
            NavigationLink(
                destination = {
                    StreamWizardNetworkSetupMyServersSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard,
                    )
                },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = com.moblin.android.localized(text = "Moblin"))
                    SystemImage("arrow.right", fontSize = 17.sp)
                    Text("My server(s)")
                    SystemImage("arrow.right", fontSize = 17.sp)
                    Text(platform)
                }
            }
        }
    }
}
