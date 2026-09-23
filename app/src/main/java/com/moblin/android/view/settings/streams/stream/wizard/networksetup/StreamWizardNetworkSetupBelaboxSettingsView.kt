package com.moblin.android.view.settings.streams.stream.wizard.networksetup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formFootnoteStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardNetworkSetup
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.settings.streams.stream.wizard.StreamWizardObsRemoteControlSettingsView
import com.moblin.android.view.utils.FormFieldError
import com.moblin.android.view.utils.HCenter

@Composable
fun StreamWizardNetworkSetupBelaboxSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var urlError by remember { mutableStateOf("") }
    var belaboxUrl by remember { mutableStateOf(createStreamWizard.belaboxUrl) }
    val palette = formPalette()

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
        createStreamWizard.networkSetup = WizardNetworkSetup.belaboxCloudObs
        updateUrlError()
    }

    Form(
        title = "BELABOX cloud and OBS",
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        },
    ) {
        Section(
            header = "Ingest URL",
            footerContent = {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FormFieldError(error = urlError)
                    Text(
                        "Press \"Add automatically to Moblin\" on https://cloud.belabox.net " +
                            "SRT(LA) relays (requires login). See screenshot below.",
                        style = formFootnoteStyle,
                        color = palette.secondaryLabel,
                    )
                    HCenter {
                        Bundle.image("BelaboxCloudIngest")?.asImageBitmap()?.let { bitmap ->
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                modifier = Modifier
                                    .widthIn(max = 400.dp)
                                    .fillMaxWidth(),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                }
            },
        ) {
            FormRow {
                Box {
                    if (belaboxUrl.isEmpty()) {
                        Text(
                            "srtla://uk.srt.belabox.net:5000?streamid=jO4ijfFgrlpv4m2375msdoG3DDr2",
                            style = formBodyStyle,
                            color = palette.tertiaryLabel,
                            maxLines = 1,
                        )
                    }
                    BasicTextField(
                        value = belaboxUrl,
                        onValueChange = {
                            belaboxUrl = it
                            createStreamWizard.belaboxUrl = it
                            updateUrlError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = formBodyStyle.copy(color = palette.label),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            capitalization = KeyboardCapitalization.None,
                        ),
                    )
                }
            }
        }
        Section {
            NavigationLink(
                destination = {
                    StreamWizardObsRemoteControlSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard,
                    )
                },
                enabled = !nextDisabled(),
            ) {
                WizardNextButtonView()
            }
        }
    }
}
