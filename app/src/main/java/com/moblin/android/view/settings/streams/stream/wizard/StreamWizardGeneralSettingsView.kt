package com.moblin.android.view.settings.streams.stream.wizard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardPlatform
import com.moblin.android.various.utils.isMac
import com.moblin.android.view.settings.streams.stream.AutoGoLiveFooterView
import com.moblin.android.view.settings.streams.stream.BackgroundStreamingFooterView
import com.moblin.android.view.settings.streams.stream.CreateStreamWizardToolbar
import kotlinx.coroutines.launch
import com.moblin.android.localized
import com.moblin.android.various.model.createStreamFromWizard

@Composable
fun StreamWizardGeneralSettingsView(model: Model = LocalModel.current, createStreamWizard: CreateStreamWizard) {
    val scope = rememberCoroutineScope()
    val palette = formPalette()
    val name = createStreamWizard.name

    Form(
        title = localized("General"),
        toolbar = {
            CreateStreamWizardToolbar(createStreamWizard = createStreamWizard)
        }
    ) {
        Section(header = localized("Stream name")) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (name.isEmpty()) {
                    Text(
                        localized("Name"),
                        style = formBodyStyle,
                        color = palette.secondaryLabel
                    )
                }
                BasicTextField(
                    value = name,
                    onValueChange = { createStreamWizard.name = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = formBodyStyle.copy(color = palette.label),
                    singleLine = true,
                    cursorBrush = SolidColor(palette.accent)
                )
            }
        }
        if (createStreamWizard.platform == WizardPlatform.mobcam) {
            Section(footerContent = { AutoGoLiveFooterView() }) {
                Toggle(
                    localized("Auto go live"),
                    isOn = createStreamWizard.autoGoLive,
                    onChange = { createStreamWizard.autoGoLive = it }
                )
            }
        } else if (!isMac()) {
            Section(footerContent = { BackgroundStreamingFooterView() }) {
                Toggle(
                    localized("Background streaming"),
                    isOn = createStreamWizard.backgroundStreaming,
                    onChange = { createStreamWizard.backgroundStreaming = it }
                )
            }
        }
        if (!isMac()) {
            Section {
                Toggle(
                    localized("Send Go live notification to Moblin website"),
                    isOn = createStreamWizard.goLiveNotificationMoblinWebsite,
                    onChange = { createStreamWizard.goLiveNotificationMoblinWebsite = it }
                )
            }
        }
        Section {
            FormButton(
                title = localized("Create"),
                centered = true,
                enabled = name.isNotEmpty()
            ) {
                scope.launch {
                    model.createStreamFromWizard()
                    createStreamWizard.presenting = false
                    createStreamWizard.presentingSetup = false
                }
            }
        }
    }
}
