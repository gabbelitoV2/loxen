package com.moblin.android.view.settings.streams.stream.wizard.networksetup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formFootnoteStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardNetworkSetup
import com.moblin.android.various.model.WizardPlatform
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.settings.streams.stream.wizard.StreamWizardGeneralSettingsView
import com.moblin.android.view.utils.FormFieldError

@Composable
fun StreamWizardNetworkSetupDirectSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var ingestError by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    val palette = formPalette()

    fun nextDisabled(): Boolean {
        return createStreamWizard.directIngest.isEmpty() ||
            createStreamWizard.directStreamKey.isEmpty() ||
            ingestError.isNotEmpty()
    }

    fun twitchStreamKeyUrl(): String {
        return "https://dashboard.twitch.tv/u/${createStreamWizard.twitchChannelName.trim()}/settings/stream"
    }

    fun updateIngestError() {
        val url = cleanUrl(value = createStreamWizard.directIngest)
        ingestError = if (url.isEmpty()) {
            ""
        } else {
            isValidUrl(value = url, rtmpStreamKeyRequired = false) ?: ""
        }
    }

    DisposableEffect(Unit) {
        createStreamWizard.networkSetup = WizardNetworkSetup.direct
        updateIngestError()
        onDispose {}
    }

    Form(
        title = localized("Direct"),
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        },
    ) {
        when (createStreamWizard.platform) {
            WizardPlatform.twitch -> {
                Section(
                    header = localized("Nearby ingest endpoint"),
                    footerContent = {
                        Column(horizontalAlignment = Alignment.Start) {
                            FormFieldError(error = ingestError)
                            Text(
                                localized(
                                    "Copy from https://help.twitch.tv/s/twitch-ingest-recommendation. Remove {stream_key}.",
                                ),
                            )
                        }
                    },
                ) {
                    BasicTextField(
                        value = createStreamWizard.directIngest,
                        onValueChange = {
                            createStreamWizard.directIngest = it
                            updateIngestError()
                        },
                        singleLine = true,
                        textStyle = formBodyStyle.copy(color = palette.label),
                        cursorBrush = SolidColor(palette.accent),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (createStreamWizard.directIngest.isEmpty()) {
                                Text(
                                    text = localized("rtmp://arn03.contribute.live-video.net/app"),
                                    style = formBodyStyle,
                                    color = palette.tertiaryLabel,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
                Section(
                    header = localized("Stream key"),
                    footerContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(localized("Copy from "))
                            val url = twitchStreamKeyUrl()
                            Text(
                                text = url,
                                style = formFootnoteStyle,
                                color = palette.accent,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    uriHandler.openUri(url)
                                },
                            )
                            Text(localized(" (requires login)."))
                        }
                    },
                ) {
                    BasicTextField(
                        value = createStreamWizard.directStreamKey,
                        onValueChange = {
                            createStreamWizard.directStreamKey = it
                        },
                        singleLine = true,
                        textStyle = formBodyStyle.copy(color = palette.label),
                        cursorBrush = SolidColor(palette.accent),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (createStreamWizard.directStreamKey.isEmpty()) {
                                Text(
                                    text = localized("live_48950233_okF4f455GRWEF443fFr23GRbt5rEv"),
                                    style = formBodyStyle,
                                    color = palette.tertiaryLabel,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
            }

            WizardPlatform.kick -> {
                Section(
                    header = localized("Stream URL"),
                    footerContent = {
                        Column(horizontalAlignment = Alignment.Start) {
                            FormFieldError(error = ingestError)
                            Text(
                                localized(
                                    "Copy from https://kick.com/dashboard/settings/stream (requires login).",
                                ),
                            )
                        }
                    },
                ) {
                    BasicTextField(
                        value = createStreamWizard.directIngest,
                        onValueChange = {
                            createStreamWizard.directIngest = it
                            updateIngestError()
                        },
                        singleLine = true,
                        textStyle = formBodyStyle.copy(color = palette.label),
                        cursorBrush = SolidColor(palette.accent),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (createStreamWizard.directIngest.isEmpty()) {
                                Text(
                                    text = localized("rtmps://fa723fc1b171.global-contribute.live-video.net"),
                                    style = formBodyStyle,
                                    color = palette.tertiaryLabel,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
                Section(
                    header = localized("Stream key"),
                    footer = localized(
                        "Copy from https://kick.com/dashboard/settings/stream (requires login).",
                    ),
                ) {
                    BasicTextField(
                        value = createStreamWizard.directStreamKey,
                        onValueChange = {
                            createStreamWizard.directStreamKey = it
                        },
                        singleLine = true,
                        textStyle = formBodyStyle.copy(color = palette.label),
                        cursorBrush = SolidColor(palette.accent),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (createStreamWizard.directStreamKey.isEmpty()) {
                                Text(
                                    text = localized("sk_us-west-2_okfef49k34k_34g59gGDDHGHSREj754gYJYTJERH"),
                                    style = formBodyStyle,
                                    color = palette.tertiaryLabel,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
            }

            WizardPlatform.youTube -> {
                Section(
                    header = localized("Stream URL"),
                    footerContent = {
                        Column(horizontalAlignment = Alignment.Start) {
                            FormFieldError(error = ingestError)
                            Text(
                                localized("Copy from https://youtube.com (requires login)."),
                            )
                        }
                    },
                ) {
                    BasicTextField(
                        value = createStreamWizard.directIngest,
                        onValueChange = {
                            createStreamWizard.directIngest = it
                            updateIngestError()
                        },
                        singleLine = true,
                        textStyle = formBodyStyle.copy(color = palette.label),
                        cursorBrush = SolidColor(palette.accent),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (createStreamWizard.directIngest.isEmpty()) {
                                Text(
                                    text = localized("rtmp://a.rtmp.youtube.com/live2"),
                                    style = formBodyStyle,
                                    color = palette.tertiaryLabel,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
                Section(
                    header = localized("Stream key"),
                    footer = localized("Copy from https://youtube.com (requires login)."),
                ) {
                    BasicTextField(
                        value = createStreamWizard.directStreamKey,
                        onValueChange = {
                            createStreamWizard.directStreamKey = it
                        },
                        singleLine = true,
                        textStyle = formBodyStyle.copy(color = palette.label),
                        cursorBrush = SolidColor(palette.accent),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (createStreamWizard.directStreamKey.isEmpty()) {
                                Text(
                                    text = localized("4bkf-8d03-g6w3-ekjh-emdc"),
                                    style = formBodyStyle,
                                    color = palette.tertiaryLabel,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
            }

            WizardPlatform.soop -> {
                Section(
                    header = localized("Stream URL"),
                    footerContent = {
                        Column(horizontalAlignment = Alignment.Start) {
                            FormFieldError(error = ingestError)
                            Text(
                                localized("Copy from ??? (requires login)."),
                            )
                        }
                    },
                ) {
                    BasicTextField(
                        value = createStreamWizard.directIngest,
                        onValueChange = {
                            createStreamWizard.directIngest = it
                            updateIngestError()
                        },
                        singleLine = true,
                        textStyle = formBodyStyle.copy(color = palette.label),
                        cursorBrush = SolidColor(palette.accent),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (createStreamWizard.directIngest.isEmpty()) {
                                Text(
                                    text = localized("???"),
                                    style = formBodyStyle,
                                    color = palette.tertiaryLabel,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
                Section(
                    header = localized("Stream key"),
                    footer = localized("Copy from ??? (requires login)."),
                ) {
                    BasicTextField(
                        value = createStreamWizard.directStreamKey,
                        onValueChange = {
                            createStreamWizard.directStreamKey = it
                        },
                        singleLine = true,
                        textStyle = formBodyStyle.copy(color = palette.label),
                        cursorBrush = SolidColor(palette.accent),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (createStreamWizard.directStreamKey.isEmpty()) {
                                Text(
                                    text = localized("???"),
                                    style = formBodyStyle,
                                    color = palette.tertiaryLabel,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
            }

            WizardPlatform.custom -> {
            }

            WizardPlatform.obs -> {
            }

            WizardPlatform.mobcam -> {
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
                enabled = !nextDisabled(),
            ) {
                WizardNextButtonView()
            }
        }
    }
}
