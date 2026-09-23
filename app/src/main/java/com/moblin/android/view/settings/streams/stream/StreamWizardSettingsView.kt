package com.moblin.android.view.settings.streams.stream

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.utils.isMac
import com.moblin.android.view.settings.streams.stream.wizard.StreamWizardMobcamSettingsView
import com.moblin.android.view.settings.streams.stream.wizard.custom.StreamWizardCustomSettingsView
import com.moblin.android.view.settings.streams.stream.wizard.platform.StreamWizardKickSettingsView
import com.moblin.android.view.settings.streams.stream.wizard.platform.StreamWizardObsSettingsView
import com.moblin.android.view.settings.streams.stream.wizard.platform.StreamWizardSoopSettingsView
import com.moblin.android.view.settings.streams.stream.wizard.platform.StreamWizardTwitchSettingsView
import com.moblin.android.view.settings.streams.stream.wizard.platform.StreamWizardYouTubeSettingsView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.localized

@Composable
fun WizardNextButtonView() {
    HCenter {
        Text(
            text = localized("Next"),
            color = formPalette().accent
        )
    }
}

@Composable
fun WizardSkipButtonView() {
    HCenter {
        Text(
            text = localized("Skip"),
            color = formPalette().accent
        )
    }
}

@Composable
fun CreateStreamWizardToolbar(createStreamWizard: CreateStreamWizard) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    SystemImage(
        name = "xmark",
        fontSize = 17.sp,
        modifier = Modifier
            .alpha(if (pressed) 0.2f else 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                createStreamWizard.presenting = false
                createStreamWizard.presentingSetup = false
            }
    )
}

@Composable
fun StreamWizardSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Form(
        title = "Create stream wizard",
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        }
    ) {
        Section(header = "Platform to stream to") {
            NavigationLink(destination = {
                StreamWizardTwitchSettingsView(
                    model = model,
                    createStreamWizard = createStreamWizard
                )
            }) {
                TwitchLogoAndNameView()
            }
            NavigationLink(destination = {
                StreamWizardKickSettingsView(
                    model = model,
                    createStreamWizard = createStreamWizard
                )
            }) {
                KickLogoAndNameView()
            }
            NavigationLink(destination = {
                StreamWizardYouTubeSettingsView(
                    model = model,
                    createStreamWizard = createStreamWizard,
                    youTubeStream = createStreamWizard.youTubeStream
                )
            }) {
                YouTubeLogoAndNameView()
            }
            NavigationLink(destination = {
                StreamWizardSoopSettingsView(
                    model = model,
                    createStreamWizard = createStreamWizard
                )
            }) {
                SoopLogoAndNameView()
            }
            NavigationLink(destination = {
                StreamWizardObsSettingsView(
                    model = model,
                    createStreamWizard = createStreamWizard
                )
            }) {
                ObsLogoAndNameView()
            }
        }
        if (!isMac()) {
            Section {
                NavigationLink(destination = {
                    StreamWizardMobcamSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard
                    )
                }) {
                    MobcamLogoAndNameView()
                }
            }
        }
        Section(footer = "For advanced users or if your platform is not in the list above.") {
            NavigationLink(destination = {
                StreamWizardCustomSettingsView(
                    model = model,
                    createStreamWizard = createStreamWizard
                )
            }) {
                Text(localized("Custom"))
            }
        }
    }
}
