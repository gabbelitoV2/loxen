package com.moblin.android.view.settings.streams.stream.wizard.networksetup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.moblin.android.R
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.utils.FormFieldError
import com.moblin.android.view.utils.HCenter
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWizardNetworkSetupBelaboxSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var urlError by remember { mutableStateOf("") }
    val belaboxUrl by createStreamWizard.belaboxUrl.collectAsState()

    fun updateUrlError() {
        val url = cleanUrl(belaboxUrl)
        urlError = if (url.isEmpty()) {
            ""
        } else {
            isValidUrl(url, listOf("srt", "srtla")) ?: ""
        }
    }

    fun nextDisabled(): Boolean {
        return belaboxUrl.trim().isEmpty() || urlError.isNotEmpty()
    }

    LaunchedEffect(Unit) {
        createStreamWizard.networkSetup.value = CreateStreamWizard.NetworkSetup.belaboxCloudObs
        updateUrlError()
    }

    LaunchedEffect(belaboxUrl) {
        updateUrlError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("BELABOX cloud and OBS")
                },
                actions = {
                    CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Text("Ingest URL")
            }
            item {
                OutlinedTextField(
                    value = belaboxUrl,
                    onValueChange = {
                        createStreamWizard.belaboxUrl.value = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("srtla://uk.srt.belabox.net:5000?streamid=jO4ijfFgrlpv4m2375msdoG3DDr2")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        capitalization = KeyboardCapitalization.None,
                    ),
                )
            }
            item {
                Column(horizontalAlignment = Alignment.Start) {
                    FormFieldError(error = urlError)
                    Text(
                        "Press \"Add automatically to Moblin\" on https://cloud.belabox.net " +
                            "SRT(LA) relays (requires login). See screenshot below.",
                    )
                    HCenter {
                        Image(
                            painter = painterResource(id = R.drawable.belabox_cloud_ingest),
                            contentDescription = null,
                            modifier = Modifier.widthIn(max = 400.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        onNavigate("StreamWizardObsRemoteControlSettingsView")
                    },
                    enabled = !nextDisabled(),
                ) {
                    WizardNextButtonView()
                }
            }
        }
    }
}
