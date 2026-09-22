package com.moblin.android.view.settings.streams.stream.wizard.custom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
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
import com.moblin.android.localized
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardSkipButtonView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardCustomSettingsView(
    model: Model,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit,
) {
    LaunchedEffect(Unit) {
        createStreamWizard.platform = TODO("assign the custom platform case of CreateStreamWizard.platform")
        createStreamWizard.customProtocol = TODO("assign the none case of CreateStreamWizard.customProtocol")
        createStreamWizard.name = makeUniqueName(
            name = localized("Custom"),
            existingNames = model.database.streams,
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom") },
                actions = {
                    CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Text(
                    text = "Protocol",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onNavigate("StreamWizardCustomSrtSettingsView")
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text("SRT(LA)")
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onNavigate("StreamWizardCustomRtmpSettingsView")
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text("RTMP(S)")
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onNavigate("StreamWizardCustomRistSettingsView")
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text("RIST")
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onNavigate("StreamWizardCustomWhipSettingsView")
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text("WHIP")
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onNavigate("StreamWizardGeneralSettingsView")
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    WizardSkipButtonView()
                }
            }
        }
    }
}
