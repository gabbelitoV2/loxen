package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.IndexSet
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.removing
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.various.settings.SettingsWidgetAlertsSpeechToText
import com.moblin.android.various.settings.SettingsWidgetAlertsSpeechToTextString
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView

@Composable
private fun SpeechToTextStringView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    string: SettingsWidgetAlertsSpeechToTextString,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var text by remember { mutableStateOf(string.string) }

    NavigationLink(
        destination = {
            SpeechToTextStringDetailView(
                model = model,
                alert = alert,
                string = string,
                onNavigate = onNavigate,
            )
        },
    ) {
        Text(text = text)
    }
}

@Composable
fun SpeechToTextStringDetailView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    string: SettingsWidgetAlertsSpeechToTextString,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var text by remember { mutableStateOf(string.string) }

    fun onSubmit(value: String) {
        string.string = value
        text = value
        model.updateAlertsSettings()
    }

    Form(title = localized("String")) {
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = alert.enabled,
                onChange = { value ->
                    alert.enabled = value
                    model.updateAlertsSettings()
                },
            )
        }
        Section(
            footerContent = {
                Text(localized("Trigger by saying '$text'."))
            },
        ) {
            TextEditNavigationView(
                title = localized("String"),
                value = text,
                onSubmit = { value -> onSubmit(value) },
                onNavigate = onNavigate,
            )
        }
        AlertMediaView(model = model, alert = alert)
        AlertPositionView(model = model, alert = alert)
        Section {
            TextButtonView(title = "Test") {
                model.testAlert(TODO("speechToTextString(string.id)"))
            }
        }
    }
}

@Composable
fun WidgetAlertsSpeechToTextSettingsView(
    model: Model = LocalModel.current,
    speechToText: SettingsWidgetAlertsSpeechToText,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    fun deleteString(indexes: IndexSet) {
        speechToText.strings = speechToText.strings.removing(atOffsets = indexes)
        model.updateAlertsSettings()
    }

    Form(title = localized("Speech to text")) {
        Section(
            footerContent = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(localized("Trigger alerts when you say something."))
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = localized("a string"))
                }
            },
        ) {
            ForEach(
                speechToText.strings,
                id = { it.id },
                onDelete = { deleteString(it) },
            ) { string ->
                ContextMenuDeleteButton(
                    action = {
                        val index = speechToText.strings.indexOfFirst { it.id == string.id }
                        if (index >= 0) {
                            deleteString(setOf(index))
                        }
                    },
                ) {
                    SpeechToTextStringView(
                        model = model,
                        alert = string.alert,
                        string = string,
                        onNavigate = onNavigate,
                    )
                }
            }
            CreateButtonView {
                val string = SettingsWidgetAlertsSpeechToTextString()
                speechToText.strings = speechToText.strings + string
                model.fixAlertMedias()
                model.updateAlertsSettings()
            }
        }
    }
}
