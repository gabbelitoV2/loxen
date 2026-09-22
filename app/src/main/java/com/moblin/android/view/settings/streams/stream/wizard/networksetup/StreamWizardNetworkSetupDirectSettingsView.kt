package com.moblin.android.view.settings.streams.stream.wizard.networksetup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.localized
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StreamingPlatformStatus
import com.moblin.android.various.model.WizardNetworkSetup
import com.moblin.android.various.model.WizardPlatform
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.utils.FormFieldError
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardNetworkSetupDirectSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var ingestError by remember { mutableStateOf("") }
    val directIngest = createStreamWizard.directIngest
    val directStreamKey = createStreamWizard.directStreamKey
    val platform = createStreamWizard.platform
    val uriHandler = LocalUriHandler.current

    fun nextDisabled(): Boolean {
        return directIngest.isEmpty() || directStreamKey.isEmpty() || ingestError.isNotEmpty()
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

    LaunchedEffect(Unit) {
        createStreamWizard.networkSetup = WizardNetworkSetup.direct
        updateIngestError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localized("Direct")) },
                actions = {
                    CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (platform) {
                WizardPlatform.twitch -> {
                    item {
                        Text(
                            text = localized("Nearby ingest endpoint"),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = directIngest,
                            onValueChange = {
                                createStreamWizard.directIngest = it
                                updateIngestError()
                            },
                            placeholder = {
                                Text(localized("rtmp://arn03.contribute.live-video.net/app"))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Column(horizontalAlignment = Alignment.Start) {
                            FormFieldError(error = ingestError)
                            Text(
                                localized(
                                    "Copy from https://help.twitch.tv/s/twitch-ingest-recommendation. Remove {stream_key}.",
                                ),
                            )
                        }
                    }
                    item {
                        Text(
                            text = localized("Stream key"),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = directStreamKey,
                            onValueChange = {
                                createStreamWizard.directStreamKey = it
                            },
                            placeholder = {
                                Text(localized("live_48950233_okF4f455GRWEF443fFr23GRbt5rEv"))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Row {
                            Text(localized("Copy from "))
                            val url = twitchStreamKeyUrl()
                            Text(
                                text = url,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.clickable {
                                    uriHandler.openUri(url)
                                },
                            )
                            Text(localized(" (requires login)."))
                        }
                    }
                }

                WizardPlatform.kick -> {
                    item {
                        Text(
                            text = localized("Stream URL"),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = directIngest,
                            onValueChange = {
                                createStreamWizard.directIngest = it
                                updateIngestError()
                            },
                            placeholder = {
                                Text(localized("rtmps://fa723fc1b171.global-contribute.live-video.net"))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Column(horizontalAlignment = Alignment.Start) {
                            FormFieldError(error = ingestError)
                            Text(
                                localized(
                                    "Copy from https://kick.com/dashboard/settings/stream (requires login).",
                                ),
                            )
                        }
                    }
                    item {
                        Text(
                            text = localized("Stream key"),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = directStreamKey,
                            onValueChange = {
                                createStreamWizard.directStreamKey = it
                            },
                            placeholder = {
                                Text(localized("sk_us-west-2_okfef49k34k_34g59gGDDHGHSREj754gYJYTJERH"))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Text(
                            localized(
                                "Copy from https://kick.com/dashboard/settings/stream (requires login).",
                            ),
                        )
                    }
                }

                WizardPlatform.youTube -> {
                    item {
                        Text(
                            text = localized("Stream URL"),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = directIngest,
                            onValueChange = {
                                createStreamWizard.directIngest = it
                                updateIngestError()
                            },
                            placeholder = {
                                Text(localized("rtmp://a.rtmp.youtube.com/live2"))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Column(horizontalAlignment = Alignment.Start) {
                            FormFieldError(error = ingestError)
                            Text(
                                localized("Copy from https://youtube.com (requires login)."),
                            )
                        }
                    }
                    item {
                        Text(
                            text = localized("Stream key"),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = directStreamKey,
                            onValueChange = {
                                createStreamWizard.directStreamKey = it
                            },
                            placeholder = {
                                Text(localized("4bkf-8d03-g6w3-ekjh-emdc"))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Text(
                            localized("Copy from https://youtube.com (requires login)."),
                        )
                    }
                }

                WizardPlatform.soop -> {
                    item {
                        Text(
                            text = localized("Stream URL"),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = directIngest,
                            onValueChange = {
                                createStreamWizard.directIngest = it
                                updateIngestError()
                            },
                            placeholder = {
                                Text(localized("???"))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Column(horizontalAlignment = Alignment.Start) {
                            FormFieldError(error = ingestError)
                            Text(
                                localized("Copy from ??? (requires login)."),
                            )
                        }
                    }
                    item {
                        Text(
                            text = localized("Stream key"),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = directStreamKey,
                            onValueChange = {
                                createStreamWizard.directStreamKey = it
                            },
                            placeholder = {
                                Text(localized("???"))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Text(
                            localized("Copy from ??? (requires login)."),
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

            item {
                Box(
                    modifier = Modifier.clickable(enabled = !nextDisabled()) {
                        onNavigate("StreamWizardGeneralSettingsView")
                    },
                ) {
                    WizardNextButtonView()
                }
            }
        }
    }
}
