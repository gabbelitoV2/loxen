package com.moblin.android.view.settings.streams.stream.wizard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardMobcamSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    LaunchedEffect(Unit) {
        TODO("createStreamWizard.platform = the Mobcam platform enum value")
        createStreamWizard.name = makeUniqueName(
            localized("Custom Mobcam"),
            model.database.streams,
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Mobcam")
                },
                actions = {
                    CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Text(
                    text = "Use Moblin as a low latency camera in OBS Studio over USB.",
                    modifier = Modifier.padding(16.dp),
                )
            }
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Configure OBS on your computer",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = "1. Install the OBS Mobcam Plugin as described " +
                                "[here](https://github.com/eerimoq/mobcam/tree/main/crates/obs-plugin#mobcam-obs-plugin).",
                        )
                        Text("")
                        Text("2. Connect Moblin to the computer with a USB cable.")
                        Text("")
                        Text("3. Add a Mobcam source in OBS.")
                        Text("")
                        Text("4. Press Go live in Moblin to start the stream to OBS.")
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onNavigate("StreamWizardGeneralSettingsView")
                        },
                ) {
                    WizardNextButtonView()
                }
            }
        }
    }
}
