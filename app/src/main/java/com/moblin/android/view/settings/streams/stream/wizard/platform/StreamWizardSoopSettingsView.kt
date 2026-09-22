package com.moblin.android.view.settings.streams.stream.wizard.platform

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.moblin.android.localized
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardSoopSettingsView(
    model: Model,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit,
) {
    val soopChannelName by createStreamWizard.soopChannelName.collectAsState()
    val soopStreamId by createStreamWizard.soopStreamId.collectAsState()

    LaunchedEffect(Unit) {
        createStreamWizard.platform = CreateStreamWizard.Platform.soop
        createStreamWizard.name = makeUniqueName(
            name = localized("SOOP"),
            existingNames = model.database.streams,
        )
        createStreamWizard.directIngest = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("SOOP")
                },
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
                    Text("Channel name")
                    OutlinedTextField(
                        value = soopChannelName,
                        onValueChange = { createStreamWizard.soopChannelName.value = it },
                        placeholder = { Text("MyChannel") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Column {
                    Text("Video id")
                    OutlinedTextField(
                        value = soopStreamId,
                        onValueChange = { createStreamWizard.soopStreamId.value = it },
                        placeholder = { Text("908123903") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                WizardNextButtonView(
                    onClick = { onNavigate("StreamWizardNetworkSetupSettingsView") },
                )
            }
        }
    }
}
