package com.moblin.android.view.settings.streams.stream.wizard.networksetup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardNetworkSetupSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    platform: String,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network setup") },
                actions = {
                    CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("StreamWizardNetworkSetupObsSettingsView") }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("Moblin")
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                            Text("OBS")
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                            Text(platform)
                        }
                        Text(
                            "Good stability in most network conditions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("StreamWizardNetworkSetupBelaboxSettingsView") }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("Moblin")
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                            Text("BELABOX cloud")
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                            Text("OBS")
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                            Text(platform)
                        }
                        Text(
                            "Best possible stability. Uses bonding. Paid third-party service.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("StreamWizardNetworkSetupDirectSettingsView") }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("Moblin")
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                            Text(platform)
                        }
                        Text(
                            "Often bad stability if network connection is unstable. No server side disconnection protection possible.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("StreamWizardNetworkSetupMyServersSettingsView") }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("Moblin")
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                            Text("My server(s)")
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                            Text(platform)
                        }
                        Text(
                            "Best possible stability. May use bonding. Most flexible setup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
