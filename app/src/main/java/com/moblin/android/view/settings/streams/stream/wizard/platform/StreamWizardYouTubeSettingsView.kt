package com.moblin.android.view.settings.streams.stream.wizard.platform

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.YouTube
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.utils.TextButtonView

private fun fetchLiveStreams(
    model: Model,
    createStreamWizard: CreateStreamWizard,
    youTubeStream: SettingsStream
) {
    model.getYouTubeApi(stream = youTubeStream) { youTubeApi ->
        youTubeApi?.listLiveStreams { result ->
            TODO("YouTubeApi.listLiveStreams result handling is not available in this port: $result")
        }
    }
}

private fun fetchChannelHandle(
    model: Model,
    createStreamWizard: CreateStreamWizard,
    youTubeStream: SettingsStream
) {
    model.getYouTubeApi(stream = youTubeStream) { youTubeApi ->
        youTubeApi?.listChannels { result ->
            TODO("YouTubeApi.listChannels result handling is not available in this port: $result")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardYouTubeSettingsView(
    model: Model,
    createStreamWizard: CreateStreamWizard,
    youTubeStream: SettingsStream,
    onNavigate: (String) -> Unit
) {
    val youTubeAuthState by youTubeStream.youTubeAuthState.collectAsState()
    val youTubeHandle by createStreamWizard.youTubeHandle.collectAsState()

    LaunchedEffect(Unit) {
        createStreamWizard.platform.value = YouTube
        createStreamWizard.name.value = makeUniqueName(
            name = localized("YouTube"),
            existingNames = model.database.streams
        )
        createStreamWizard.directIngest.value = "rtmp://a.rtmp.youtube.com/live2"
        youTubeStream.youTubeAuthState.value = null
    }

    LaunchedEffect(youTubeAuthState) {
        if (youTubeAuthState != null) {
            fetchLiveStreams(model, createStreamWizard, youTubeStream)
            fetchChannelHandle(model, createStreamWizard, youTubeStream)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("YouTube")
                },
                actions = {
                    CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (!youTubeStream.isYouTubeAuthorized()) {
                TextButtonView("Login") {
                    model.youTubeSignIn(stream = youTubeStream)
                }
            } else {
                TextButtonView("Logout") {
                    model.youTubeSignOut(stream = youTubeStream)
                }
            }
            Text(localized("Optional, but simplifies the setup."))

            Text(localized("Channel handle"))
            OutlinedTextField(
                value = youTubeHandle,
                onValueChange = {
                    createStreamWizard.youTubeHandle.value = it
                },
                placeholder = {
                    Text("@erimo144")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(localized("Only needed for chat."))

            WizardNextButtonView {
                onNavigate("StreamWizardNetworkSetupSettingsView")
            }
        }
    }
}
