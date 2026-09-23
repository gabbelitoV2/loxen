package com.moblin.android.view.settings.streams.stream.wizard.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.streamingplatforms.twitch.*
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardPlatform
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.settings.streams.stream.wizard.networksetup.StreamWizardNetworkSetupSettingsView
import kotlinx.coroutines.launch
import com.moblin.android.various.model.twitchLogin
import com.moblin.android.various.model.twitchLogout

@Composable
fun StreamWizardTwitchSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val scope = rememberCoroutineScope()
    val palette = formPalette()

    fun nextDisabled(): Boolean {
        return createStreamWizard.twitchChannelName.trim().isEmpty()
    }

    fun onLoginComplete() {
        createStreamWizard.twitchChannelName = createStreamWizard.twitchStream.twitchChannelName
        createStreamWizard.twitchChannelId = createStreamWizard.twitchStream.twitchChannelId
        createStreamWizard.twitchAccessToken = createStreamWizard.twitchStream.twitchAccessToken
        createStreamWizard.twitchLoggedIn = createStreamWizard.twitchStream.twitchLoggedIn
        scope.launch {
            TwitchApi(createStreamWizard.twitchAccessToken)
                .getStreamKey(
                    broadcasterId = createStreamWizard.twitchChannelId,
                ) { streamKey ->
                    if (streamKey != null) {
                        createStreamWizard.directStreamKey = streamKey
                    }
                }
        }
    }

    DisposableEffect(Unit) {
        createStreamWizard.platform = WizardPlatform.twitch
        createStreamWizard.name = makeUniqueName(
            localized("Twitch"),
            model.database.streams,
        )
        createStreamWizard.directIngest = "rtmp://ingest.global-contribute.live-video.net/app"
        createStreamWizard.twitchStream.twitchAccessToken = ""
        onDispose {}
    }

    Form(
        title = localized("Twitch"),
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        },
    ) {
        Section(footer = localized("Optional, but simplifies the setup.")) {
            if (createStreamWizard.twitchStream.twitchAccessToken.isEmpty()) {
                FormButton(title = localized("Login")) {
                    model.twitchLogin(
                        stream = createStreamWizard.twitchStream,
                        onComplete = { onLoginComplete() },
                        showWebBrowser = { createStreamWizard.showTwitchAuth = true },
                    )
                }
            } else {
                FormButton(title = localized("Logout")) {
                    model.twitchLogout(stream = createStreamWizard.twitchStream)
                }
            }
        }
        Section(header = localized("Channel name")) {
            FormRow {
                BasicTextField(
                    value = createStreamWizard.twitchChannelName,
                    onValueChange = { createStreamWizard.twitchChannelName = it },
                    modifier = Modifier.weight(1f),
                    textStyle = formBodyStyle.copy(color = palette.label),
                    singleLine = true,
                    cursorBrush = SolidColor(palette.accent),
                    decorationBox = { innerTextField ->
                        Box {
                            if (createStreamWizard.twitchChannelName.isEmpty()) {
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
        }
        Section(header = localized("Channel id")) {
            FormRow {
                BasicTextField(
                    value = createStreamWizard.twitchChannelId,
                    onValueChange = { createStreamWizard.twitchChannelId = it },
                    modifier = Modifier.weight(1f),
                    textStyle = formBodyStyle.copy(color = palette.label),
                    singleLine = true,
                    cursorBrush = SolidColor(palette.accent),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (createStreamWizard.twitchChannelId.isEmpty()) {
                                Text(
                                    text = localized("908123903"),
                                    style = formBodyStyle,
                                    color = palette.tertiaryLabel,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
        Section {
            NavigationLink(
                destination = {
                    StreamWizardNetworkSetupSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard,
                        platform = localized("Twitch"),
                    )
                },
                enabled = !nextDisabled(),
            ) {
                WizardNextButtonView()
            }
        }
    }

    if (createStreamWizard.showTwitchAuth) {
        Sheet(
            onDismissRequest = { createStreamWizard.showTwitchAuth = false },
        ) {
            TwitchLoginView(
                model = model,
                presenting = createStreamWizard.showTwitchAuth,
                onPresentingChange = { createStreamWizard.showTwitchAuth = it },
            )
        }
    }
}
