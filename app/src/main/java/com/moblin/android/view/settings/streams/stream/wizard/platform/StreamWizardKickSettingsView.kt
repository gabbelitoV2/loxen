package com.moblin.android.view.settings.streams.stream.wizard.platform

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.streamingplatforms.kick.KickLoginView
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Platform
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.utils.TextButtonView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardKickSettingsView(
    model: Model,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = {},
) {
    val showKickAuth by createStreamWizard.showKickAuth.collectAsState()
    val kickChannelName by createStreamWizard.kickChannelName.collectAsState()
    val kickAccessToken by createStreamWizard.kickStream.kickAccessToken.collectAsState()

    fun nextDisabled(): Boolean = kickChannelName.trim().isEmpty()

    fun onLoginComplete() {
        createStreamWizard.kickChannelName.value = createStreamWizard.kickStream.kickChannelName.value
        createStreamWizard.kickAccessToken.value = createStreamWizard.kickStream.kickAccessToken.value
        createStreamWizard.kickLoggedIn.value = createStreamWizard.kickStream.kickLoggedIn.value
        createStreamWizard.kickChannelId.value = createStreamWizard.kickStream.kickChannelId.value
        createStreamWizard.kickSlug.value = createStreamWizard.kickStream.kickSlug.value
        createStreamWizard.kickChatroomChannelId.value = createStreamWizard.kickStream.kickChatroomChannelId.value
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kick") },
                actions = { CreateStreamWizardToolbar(createStreamWizard = createStreamWizard) },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (kickAccessToken.isEmpty()) {
                TextButtonView(
                    text = "Login",
                    action = {
                        createStreamWizard.showKickAuth.value = true
                        model.kickLogin(stream = createStreamWizard.kickStream) {
                            onLoginComplete()
                        }
                    },
                )
            } else {
                TextButtonView(
                    text = "Logout",
                    action = {
                        model.kickLogout(stream = createStreamWizard.kickStream)
                    },
                )
            }
            Text(
                text = localized("Optional, but simplifies the setup."),
                style = MaterialTheme.typography.bodySmall,
            )

            Text(
                text = localized("Channel name"),
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedTextField(
                value = kickChannelName,
                onValueChange = { createStreamWizard.kickChannelName.value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                label = { Text("MyChannel") },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !nextDisabled()) {
                        onNavigate("StreamWizardNetworkSetupSettingsView")
                    },
            ) {
                WizardNextButtonView()
            }
        }
    }

    if (showKickAuth) {
        ModalBottomSheet(
            onDismissRequest = { createStreamWizard.showKickAuth.value = false },
        ) {
            KickLoginView(
                onComplete = { accessToken -> model.kickAuthOnComplete?.invoke(accessToken) },
            )
        }
    }

    LaunchedEffect(Unit) {
        createStreamWizard.platform.value = Platform.kick
        createStreamWizard.name.value = makeUniqueName(
            name = localized("Kick"),
            existingNames = model.database.streams,
        )
        createStreamWizard.directIngest.value = ""
        createStreamWizard.kickStream.kickAccessToken.value = ""
    }
}
