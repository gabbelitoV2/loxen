package com.moblin.android.view.settings.streams.stream.wizard.platform

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardPlatform
import com.moblin.android.various.model.getYouTubeApi
import com.moblin.android.various.model.youTubeSignIn
import com.moblin.android.various.model.youTubeSignOut
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.settings.streams.stream.wizard.networksetup.StreamWizardNetworkSetupSettingsView
import com.moblin.android.view.utils.TextButtonView

private fun fetchLiveStreams(
    model: Model,
    createStreamWizard: CreateStreamWizard,
    youTubeStream: SettingsStream,
) {
    model.getYouTubeApi(stream = youTubeStream) { youTubeApi ->
        youTubeApi?.listLiveStreams { response ->
            when (response) {
                is NetworkResponse.Success -> {
                    val liveStream = response.value.items.firstOrNull()
                    if (liveStream != null) {
                        val ingestionInfo = liveStream.cdn.ingestionInfo
                        createStreamWizard.directIngest = ingestionInfo.ingestionAddress
                        createStreamWizard.directStreamKey = ingestionInfo.streamName
                    }
                }

                NetworkResponse.AuthError, NetworkResponse.Error -> Unit
            }
        }
    }
}

private fun fetchChannelHandle(
    model: Model,
    createStreamWizard: CreateStreamWizard,
    youTubeStream: SettingsStream,
) {
    model.getYouTubeApi(stream = youTubeStream) { youTubeApi ->
        youTubeApi?.listChannels { response ->
            when (response) {
                is NetworkResponse.Success -> {
                    val handle = response.value.items.firstOrNull()?.snippet?.customUrl
                    if (handle != null) {
                        createStreamWizard.youTubeHandle = handle
                    }
                }

                NetworkResponse.AuthError, NetworkResponse.Error -> Unit
            }
        }
    }
}

@Composable
fun StreamWizardYouTubeSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    youTubeStream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val youTubeAuthState = youTubeStream.youTubeAuthState

    LaunchedEffect(Unit) {
        createStreamWizard.platform = WizardPlatform.youTube
        createStreamWizard.name = makeUniqueName(
            name = localized("YouTube"),
            existingNames = model.database.streams
        )
        createStreamWizard.directIngest = "rtmp://a.rtmp.youtube.com/live2"
        youTubeStream.youTubeAuthState = null
    }

    LaunchedEffect(youTubeAuthState) {
        if (youTubeAuthState != null) {
            fetchLiveStreams(model, createStreamWizard, youTubeStream)
            fetchChannelHandle(model, createStreamWizard, youTubeStream)
        }
    }

    Form(
        title = localized("YouTube"),
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        },
    ) {
        Section(footer = localized("Optional, but simplifies the setup.")) {
            if (!youTubeStream.isYouTubeAuthorized()) {
                TextButtonView(localized("Login")) {
                    model.youTubeSignIn(stream = youTubeStream)
                }
            } else {
                TextButtonView(localized("Logout")) {
                    model.youTubeSignOut(stream = youTubeStream)
                }
            }
        }
        Section(
            header = localized("Channel handle"),
            footer = localized("Only needed for chat."),
        ) {
            FormRow {
                BasicTextField(
                    value = createStreamWizard.youTubeHandle,
                    onValueChange = {
                        createStreamWizard.youTubeHandle = it
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = formBodyStyle.copy(color = formPalette().label),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                    ),
                    cursorBrush = SolidColor(formPalette().accent),
                    decorationBox = { innerTextField ->
                        if (createStreamWizard.youTubeHandle.isEmpty()) {
                            Text(
                                localized("@erimo144"),
                                style = formBodyStyle,
                                color = formPalette().secondaryLabel,
                            )
                        }
                        innerTextField()
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
                        platform = localized("YouTube"),
                    )
                },
            ) {
                WizardNextButtonView()
            }
        }
    }
}
