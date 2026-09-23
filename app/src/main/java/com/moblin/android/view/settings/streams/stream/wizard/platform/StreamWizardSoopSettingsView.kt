package com.moblin.android.view.settings.streams.stream.wizard.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardPlatform
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.settings.streams.stream.wizard.networksetup.StreamWizardNetworkSetupSettingsView

@Composable
fun StreamWizardSoopSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val palette = formPalette()
    val soopChannelName = binding({ createStreamWizard.soopChannelName }) {
        createStreamWizard.soopChannelName = it
    }
    val soopStreamId = binding({ createStreamWizard.soopStreamId }) {
        createStreamWizard.soopStreamId = it
    }

    LaunchedEffect(Unit) {
        createStreamWizard.platform = WizardPlatform.soop
        createStreamWizard.name = makeUniqueName(
            name = localized("SOOP"),
            existingNames = model.database.streams,
        )
        createStreamWizard.directIngest = ""
    }

    Form(
        title = localized("SOOP"),
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        },
    ) {
        Section(header = localized("Channel name")) {
            BasicTextField(
                value = soopChannelName.value,
                onValueChange = { soopChannelName.value = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = formBodyStyle.copy(color = palette.label),
                singleLine = true,
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                decorationBox = { innerTextField ->
                    Box {
                        if (soopChannelName.value.isEmpty()) {
                            Text(
                                localized("MyChannel"),
                                style = formBodyStyle.copy(color = palette.tertiaryLabel),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
        Section(header = localized("Video id")) {
            BasicTextField(
                value = soopStreamId.value,
                onValueChange = { soopStreamId.value = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = formBodyStyle.copy(color = palette.label),
                singleLine = true,
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (soopStreamId.value.isEmpty()) {
                            Text(
                                localized("908123903"),
                                style = formBodyStyle.copy(color = palette.tertiaryLabel),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
        Section {
            NavigationLink(
                destination = {
                    StreamWizardNetworkSetupSettingsView(
                        model = model,
                        createStreamWizard = createStreamWizard,
                        platform = localized("SOOP"),
                    )
                },
            ) {
                WizardNextButtonView()
            }
        }
    }
}
