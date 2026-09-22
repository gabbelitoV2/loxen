package com.moblin.android.view.settings.streams.stream.wizard.networksetup.myservers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardNetworkSetupMyServersSettingsView(
    model: Model,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit,
) {
    LaunchedEffect(Unit) {
        createStreamWizard.networkSetup.value = TODO("CreateStreamWizard network setup value myServers")
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My server(s)") },
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
                    text = "Protocol",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                Text(
                    text = "SRT(LA)",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onNavigate("StreamWizardNetworkSetupMyServersSrtSettingsView")
                        }
                        .padding(16.dp),
                )
            }
            item {
                Text(
                    text = "RTMP(S)",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onNavigate("StreamWizardNetworkSetupMyServersRtmpSettingsView")
                        }
                        .padding(16.dp),
                )
            }
        }
    }
}
