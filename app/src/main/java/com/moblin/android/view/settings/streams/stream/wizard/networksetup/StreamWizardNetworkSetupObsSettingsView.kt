package com.moblin.android.view.settings.streams.stream.wizard.networksetup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.utils.FormFieldError
import com.moblin.android.view.utils.HCenter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardNetworkSetupObsSettingsView(
    model: Model,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit,
) {
    val obsAddress by createStreamWizard.obsAddress.collectAsState()
    val obsPort by createStreamWizard.obsPort.collectAsState()
    var portError by remember { mutableStateOf("") }

    fun updatePortError() {
        val port = obsPort.trim()
        portError = when {
            port.isEmpty() -> ""
            else -> {
                val parsed = port.toUShortOrNull()
                if (parsed != null && parsed.toInt() > 0) {
                    ""
                } else {
                    localized("Must be a number between 1 and 65535.")
                }
            }
        }
    }

    fun nextDisabled(): Boolean {
        return obsAddress.trim().isEmpty() ||
            obsPort.trim().isEmpty() ||
            portError.isNotEmpty()
    }

    LaunchedEffect(Unit) {
        createStreamWizard.networkSetup = CreateStreamWizard.NetworkSetup.obs
        updatePortError()
    }

    LaunchedEffect(obsPort) {
        updatePortError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OBS") },
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
                Text(
                    text = "IP address or domain name",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = obsAddress,
                    onValueChange = { createStreamWizard.obsAddress.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("213.33.45.132") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Uri,
                    ),
                )
                Text(
                    text = "Your public IP address if streaming over the internet.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            item {
                Text(
                    text = "Port",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
                OutlinedTextField(
                    value = obsPort,
                    onValueChange = { createStreamWizard.obsPort.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("7654") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        keyboardType = KeyboardType.Number,
                    ),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    FormFieldError(error = portError)
                    Text(
                        text = "Configure port forwarding in your router to forward incoming traffic to OBS.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item {
                Text(
                    text = "Configure OBS on your computer",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "1. Create a Media Source in OBS and configure it as shown in the image below.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "2. Replace 7654 with your port.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    HCenter {
                        Image(
                            painter = painterResource(id = com.moblin.android.R.drawable.obs_media_source_srt),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
            item {
                WizardNextButtonView(
                    enabled = !nextDisabled(),
                    onClick = {
                        onNavigate("StreamWizardObsRemoteControlSettingsView")
                    },
                )
            }
        }
    }
}
