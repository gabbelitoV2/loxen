package com.moblin.android.view.settings.streams.stream.wizard.platform

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.moblin.android.localized
import com.moblin.android.streamingplatforms.twitch.TwitchApi
import com.moblin.android.streamingplatforms.twitch.TwitchLoginView
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardPlatform
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.utils.TextButtonView
import kotlinx.coroutines.launch
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardTwitchSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val scope = rememberCoroutineScope()
    val twitchChannelName = createStreamWizard.twitchChannelName
    val twitchChannelId = createStreamWizard.twitchChannelId
    val showTwitchAuth = createStreamWizard.showTwitchAuth

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

    LaunchedEffect(Unit) {
        createStreamWizard.platform = WizardPlatform.twitch
        createStreamWizard.name = makeUniqueName(
            localized("Twitch"),
            model.database.streams,
        )
        createStreamWizard.directIngest = "rtmp://ingest.global-contribute.live-video.net/app"
        createStreamWizard.twitchStream.twitchAccessToken = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Twitch") },
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
            item {
                Column {
                    if (createStreamWizard.twitchStream.twitchAccessToken.isEmpty()) {
                        TextButtonView("Login") {
                            TODO("model.twitchLogin")
                        }
                    } else {
                        TextButtonView("Logout") {
                            TODO("model.twitchLogout")
                        }
                    }
                    Text(
                        "Optional, but simplifies the setup.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item {
                Column {
                    Text("Channel name", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = twitchChannelName,
                        onValueChange = { createStreamWizard.twitchChannelName = it },
                        placeholder = { Text("MyChannel") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Column {
                    Text("Channel id", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = twitchChannelId,
                        onValueChange = { createStreamWizard.twitchChannelId = it },
                        placeholder = { Text("908123903") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Column {
                    WizardNextButtonView()
                }
            }
        }
    }

    if (showTwitchAuth) {
        ModalBottomSheet(
            onDismissRequest = { createStreamWizard.showTwitchAuth = false },
        ) {
            TwitchLoginView(
                model = model,
                presenting = showTwitchAuth,
                onPresentingChange = { createStreamWizard.showTwitchAuth = it },
            )
        }
    }
}
