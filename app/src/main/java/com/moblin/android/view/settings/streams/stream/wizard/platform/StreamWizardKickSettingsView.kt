package com.moblin.android.view.settings.streams.stream.wizard.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.streamingplatforms.kick.KickLoginView
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardPlatform
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.settings.streams.stream.wizard.networksetup.StreamWizardNetworkSetupSettingsView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.various.model.kickLogin
import com.moblin.android.various.model.kickLogout

@Composable
fun StreamWizardKickSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = {},
) {
    val palette = formPalette()
    val showKickAuth = createStreamWizard.showKickAuth
    val kickChannelName = createStreamWizard.kickChannelName
    val kickAccessToken = createStreamWizard.kickStream.kickAccessToken

    fun nextDisabled(): Boolean = kickChannelName.trim().isEmpty()

    fun onLoginComplete() {
        createStreamWizard.kickChannelName = createStreamWizard.kickStream.kickChannelName
        createStreamWizard.kickAccessToken = createStreamWizard.kickStream.kickAccessToken
        createStreamWizard.kickLoggedIn = createStreamWizard.kickStream.kickLoggedIn
        createStreamWizard.kickChannelId = createStreamWizard.kickStream.kickChannelId
        createStreamWizard.kickSlug = createStreamWizard.kickStream.kickSlug
        createStreamWizard.kickChatroomChannelId = createStreamWizard.kickStream.kickChatroomChannelId
    }

    DisposableEffect(Unit) {
        createStreamWizard.platform = WizardPlatform.kick
        createStreamWizard.name = makeUniqueName(
            name = localized("Kick"),
            existingNames = model.database.streams,
        )
        createStreamWizard.directIngest = ""
        createStreamWizard.kickStream.kickAccessToken = ""
        onDispose {}
    }

    Form(
        title = localized("Kick"),
        toolbar = { CreateStreamWizardToolbar(createStreamWizard = createStreamWizard) },
    ) {
        Section(footer = localized("Optional, but simplifies the setup.")) {
            if (kickAccessToken.isEmpty()) {
                TextButtonView(title = localized("Login")) {
                    createStreamWizard.showKickAuth = true
                    model.kickLogin(stream = createStreamWizard.kickStream) {
                        onLoginComplete()
                    }
                }
            } else {
                TextButtonView(title = localized("Logout")) {
                    model.kickLogout(stream = createStreamWizard.kickStream)
                }
            }
        }
        Section(header = localized("Channel name")) {
            BasicTextField(
                value = kickChannelName,
                onValueChange = { createStreamWizard.kickChannelName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = formBodyStyle,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                decorationBox = { innerTextField ->
                    Box {
                        if (kickChannelName.isEmpty()) {
                            Text(
                                text = localized("MyChannel"),
                                style = formBodyStyle,
                                color = palette.tertiaryLabel,
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
                    StreamWizardNetworkSetupSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard,
                        platform = localized("Kick"),
                    )
                },
                enabled = !nextDisabled(),
            ) {
                WizardNextButtonView()
            }
        }
    }

    if (showKickAuth) {
        Sheet(onDismissRequest = { createStreamWizard.showKickAuth = false }) {
            KickLoginView(
                presenting = showKickAuth,
                onPresentingChange = { createStreamWizard.showKickAuth = it },
                onAccessToken = { accessToken -> model.kickAuthOnComplete?.invoke(accessToken) },
            )
        }
    }
}
