package com.moblin.android.view.settings.streams.stream

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.utils.isMac
import com.moblin.android.view.utils.HCenter
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
fun WizardNextButtonView() {
    HCenter {
        Text(
            text = "Next",
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun WizardSkipButtonView() {
    HCenter {
        Text(
            text = "Skip",
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun CreateStreamWizardToolbar(createStreamWizard: CreateStreamWizard) {
    IconButton(onClick = {
        createStreamWizard.presenting = false
        createStreamWizard.presentingSetup = false
    }) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create stream wizard") },
                actions = {
                    CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
        ) {
            item {
                Text(
                    text = "Platform to stream to",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("StreamWizardTwitchSettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    TwitchLogoAndNameView()
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("StreamWizardKickSettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    KickLogoAndNameView()
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("StreamWizardYouTubeSettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    YouTubeLogoAndNameView()
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("StreamWizardSoopSettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    SoopLogoAndNameView()
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("StreamWizardObsSettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    ObsLogoAndNameView()
                }
            }
            if (!isMac()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("StreamWizardMobcamSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        MobcamLogoAndNameView()
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("StreamWizardCustomSettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("Custom")
                }
            }
            item {
                Text(
                    text = "For advanced users or if your platform is not in the list above.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
