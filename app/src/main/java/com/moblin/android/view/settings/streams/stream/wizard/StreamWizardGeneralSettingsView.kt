package com.moblin.android.view.settings.streams.stream.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.utils.isMac
import com.moblin.android.view.settings.streams.stream.AutoGoLiveFooterView
import com.moblin.android.view.settings.streams.stream.BackgroundStreamingFooterView
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.utils.TextButtonView
import kotlinx.coroutines.launch
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardGeneralSettingsView(model: Model = LocalModel.current, createStreamWizard: CreateStreamWizard) {
    val name = createStreamWizard.name
    val platform = createStreamWizard.platform
    val autoGoLive = createStreamWizard.autoGoLive
    val backgroundStreaming = createStreamWizard.backgroundStreaming
    val goLiveNotificationMoblinWebsite = createStreamWizard.goLiveNotificationMoblinWebsite
    val isMacOs = isMac()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("General") },
                actions = {
                    CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stream name", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { createStreamWizard.name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (platform == CreateStreamWizard.WizardPlatform.mobcam) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Auto go live", modifier = Modifier.weight(1f))
                            Switch(
                                checked = autoGoLive,
                                onCheckedChange = { createStreamWizard.autoGoLive = it }
                            )
                        }
                        AutoGoLiveFooterView()
                    }
                }
            } else if (!isMacOs) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Background streaming", modifier = Modifier.weight(1f))
                            Switch(
                                checked = backgroundStreaming,
                                onCheckedChange = { createStreamWizard.backgroundStreaming = it }
                            )
                        }
                        BackgroundStreamingFooterView()
                    }
                }
            }
            if (!isMacOs) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Send Go live notification to Moblin website",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = goLiveNotificationMoblinWebsite,
                            onCheckedChange = {
                                createStreamWizard.goLiveNotificationMoblinWebsite = it
                            }
                        )
                    }
                }
            }
            item {
                TextButtonView("Create") {
                    if (name.isNotEmpty()) {
                        scope.launch {
                            TODO("model.createStreamFromWizard()")
                            createStreamWizard.presenting = false
                            createStreamWizard.presentingSetup = false
                        }
                    }
                }
            }
        }
    }
}
