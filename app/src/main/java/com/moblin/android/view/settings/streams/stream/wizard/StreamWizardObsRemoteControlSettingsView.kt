package com.moblin.android.view.settings.streams.stream.wizard

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.moblin.android.R
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.utils.FormFieldError

private fun nextDisabled(createStreamWizard: CreateStreamWizard, urlError: String): Boolean {
    if (createStreamWizard.obsRemoteControlEnabled.value) {
        if (createStreamWizard.obsRemoteControlUrl.value.isEmpty() ||
            createStreamWizard.obsRemoteControlPassword.value.isEmpty() ||
            urlError.isNotEmpty()
        ) {
            return true
        }
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardObsRemoteControlSettingsView(
    model: Model,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit,
) {
    val obsRemoteControlEnabled by createStreamWizard.obsRemoteControlEnabled.collectAsState()
    val obsRemoteControlUrl by createStreamWizard.obsRemoteControlUrl.collectAsState()
    val obsRemoteControlPassword by createStreamWizard.obsRemoteControlPassword.collectAsState()
    val obsRemoteControlMainScene by createStreamWizard.obsRemoteControlMainScene.collectAsState()
    val obsRemoteControlBrbScene by createStreamWizard.obsRemoteControlBrbScene.collectAsState()
    val obsRemoteControlSourceName by createStreamWizard.obsRemoteControlSourceName.collectAsState()
    var urlError by remember { mutableStateOf("") }

    val updateUrlError: () -> Unit = {
        val url = cleanUrl(url = createStreamWizard.obsRemoteControlUrl.value)
        val message = isValidWebSocketUrl(url = url)
        urlError = message ?: ""
    }

    LaunchedEffect(Unit) {
        updateUrlError()
    }
    LaunchedEffect(obsRemoteControlUrl) {
        updateUrlError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OBS remote control") },
                actions = {
                    CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Enabled", modifier = Modifier.weight(1f))
                    Switch(
                        checked = obsRemoteControlEnabled,
                        onCheckedChange = { createStreamWizard.obsRemoteControlEnabled.value = it },
                    )
                }
            }
            if (obsRemoteControlEnabled) {
                item {
                    Text(
                        "URL",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                item {
                    OutlinedTextField(
                        value = obsRemoteControlUrl,
                        onValueChange = { createStreamWizard.obsRemoteControlUrl.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        placeholder = { Text("ws://213.33.45.132:4567") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                        ),
                    )
                }
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        FormFieldError(error = urlError)
                        Text("Use your public IP address if streaming over the internet.")
                        Text("")
                        Text("Configure port forwarding in your router to forward incoming traffic to OBS.")
                    }
                }
                item {
                    Text(
                        "Password",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                item {
                    OutlinedTextField(
                        value = obsRemoteControlPassword,
                        onValueChange = { createStreamWizard.obsRemoteControlPassword.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        placeholder = { Text("po3Gg4pflp3s") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                        ),
                    )
                }
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            "Copy from OBS Show Connect Info as seen in the screenshot below. " +
                                "Tools → WebSocket Server Settings → Show Connect Info → Server Password.",
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.weight(1f))
                            Image(
                                painter = painterResource(id = R.drawable.obs_remote_control),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Text(
                        "Main scene",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                item {
                    OutlinedTextField(
                        value = obsRemoteControlMainScene,
                        onValueChange = { createStreamWizard.obsRemoteControlMainScene.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        placeholder = { Text("Main scene") },
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                    )
                }
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            "The name of your main scene in OBS. Moblin will periodically try to switch " +
                                "to this scene from your BRB scene if the stream is likely working.",
                        )
                    }
                }
                item {
                    Text(
                        "BRB scene",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                item {
                    OutlinedTextField(
                        value = obsRemoteControlBrbScene,
                        onValueChange = { createStreamWizard.obsRemoteControlBrbScene.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        placeholder = { Text("My BRB scene") },
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                    )
                }
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            "The name of your BRB scene in OBS. Moblin will periodically try to switch " +
                                "from your main scene to this scene if the stream is likely broken.",
                        )
                    }
                }
                item {
                    Text(
                        "Source name",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                item {
                    OutlinedTextField(
                        value = obsRemoteControlSourceName,
                        onValueChange = { createStreamWizard.obsRemoteControlSourceName.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        placeholder = { Text("My source") },
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                    )
                }
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("The name of the Source in OBS that receives the stream from Moblin.")
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable(
                            enabled = !nextDisabled(createStreamWizard = createStreamWizard, urlError = urlError),
                        ) {
                            onNavigate("StreamWizardGeneralSettingsView")
                        },
                ) {
                    WizardNextButtonView()
                }
            }
        }
    }
}
