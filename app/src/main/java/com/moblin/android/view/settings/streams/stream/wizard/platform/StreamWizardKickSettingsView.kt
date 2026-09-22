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
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardKickSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = {},
) {
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
                    title = "Login",
                    action = {
                        createStreamWizard.showKickAuth = true
                        TODO("no Android counterpart for Model.kickLogin(stream:onComplete:)")
                    },
                )
            } else {
                TextButtonView(
                    title = "Logout",
                    action = {
                        TODO("no Android counterpart for Model.kickLogout(stream:)")
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
                onValueChange = { createStreamWizard.kickChannelName = it },
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
            onDismissRequest = { createStreamWizard.showKickAuth = false },
        ) {
            KickLoginView(
                presenting = showKickAuth,
                onPresentingChange = { createStreamWizard.showKickAuth = it },
                onAccessToken = { accessToken -> model.kickAuthOnComplete?.invoke(accessToken) },
            )
        }
    }

    LaunchedEffect(Unit) {
        createStreamWizard.platform = WizardPlatform.kick
        createStreamWizard.name = makeUniqueName(
            name = localized("Kick"),
            existingNames = model.database.streams,
        )
        createStreamWizard.directIngest = ""
        createStreamWizard.kickStream.kickAccessToken = ""
    }
}
