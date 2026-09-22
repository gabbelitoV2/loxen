package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.various.settings.SettingsWidgetAlertsSpeechToText
import com.moblin.android.various.settings.SettingsWidgetAlertsSpeechToTextString
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun SpeechToTextStringView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    string: SettingsWidgetAlertsSpeechToTextString,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
    onDelete: () -> Unit,
) {
    var text by remember { mutableStateOf(string.string) }

    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onNavigate("String") },
                    onLongPress = { onDelete() },
                )
            },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(localized("String")) })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(localized("Enabled"))
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = alert.enabled,
                    onCheckedChange = { value ->
                        alert.enabled = value
                        model.updateAlertsSettings()
                    },
                )
            }
            TextEditNavigationView(
                title = localized("String"),
                value = text,
                onSubmit = { value -> onSubmit(value) },
                onNavigate = onNavigate,
            )
            Text(localized("Trigger by saying '$text'."))
            AlertMediaView(model = model, alert = alert)
            AlertPositionView(model = model, alert = alert)
            TextButtonView("Test") {
                model.testAlert(TODO("test alert case for speechToTextString(string.id)"))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetAlertsSpeechToTextSettingsView(
    model: Model = LocalModel.current,
    speechToText: SettingsWidgetAlertsSpeechToText,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    fun deleteString(indexes: Set<Int>) {
        speechToText.strings = speechToText.strings.filterIndexed { index, _ -> index !in indexes }
        model.updateAlertsSettings()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(localized("Speech to text")) })
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items = speechToText.strings, key = { it.id }) { string ->
                SpeechToTextStringView(
                    model = model,
                    alert = string.alert,
                    string = string,
                    onNavigate = onNavigate,
                    onDelete = {
                        val index = speechToText.strings.indexOfFirst { it.id == string.id }
                        if (index >= 0) {
                            deleteString(setOf(index))
                        }
                    },
                )
                HorizontalDivider()
            }
            item {
                CreateButtonView {
                    val string = SettingsWidgetAlertsSpeechToTextString()
                    speechToText.strings = speechToText.strings + string
                    model.fixAlertMedias()
                    model.updateAlertsSettings()
                }
            }
            item {
                Column {
                    Text(localized("Trigger alerts when you say something."))
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = localized("a string"))
                }
            }
        }
    }
}
