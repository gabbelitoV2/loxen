package com.moblin.android.view.settings.streams.stream.wizard.networksetup.myservers

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.settings.streams.stream.wizard.StreamWizardObsRemoteControlSettingsView
import com.moblin.android.view.settings.streams.stream.wizard.custom.StreamWizardSrtUrlSettingsView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun nextDisabled(createStreamWizard: CreateStreamWizard, urlError: String): Boolean {
    val customSrtUrl by createStreamWizard.customSrtUrl.collectAsState()
    val customSrtStreamId by createStreamWizard.customSrtStreamId.collectAsState()
    return customSrtUrl.isEmpty()
        || customSrtStreamId.isEmpty()
        || urlError.isNotEmpty()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardNetworkSetupMyServersSrtSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    var urlError by remember { mutableStateOf("") }
    val nextDisabled = nextDisabled(createStreamWizard, urlError)

    LaunchedEffect(Unit) {
        TODO("createStreamWizard.customProtocol = .srt: protocol enum setter is not available in the glossary")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("SRT(LA)")
                },
                actions = {
                    CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                StreamWizardSrtUrlSettingsView(
                    createStreamWizard = createStreamWizard,
                    urlError = urlError,
                    onUrlErrorChange = { urlError = it }
                )
            }
            item {
                WizardNextButtonView(
                    enabled = !nextDisabled,
                    onClick = {
                        onNavigate(StreamWizardObsRemoteControlSettingsView.NAVIGATION_NAME)
                    }
                )
            }
        }
    }
}
