package com.moblin.android.view.settings.ingests.rtspclient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidIngestLatency
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsRtspClient
import com.moblin.android.various.settings.SettingsRtspClientStream
import com.moblin.android.various.settings.SettingsRtspTransport
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.FormFieldError
import com.moblin.android.view.utils.MultiLineTextFieldView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.view.utils.UrlCopyView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlSettingsView(
    disabled: Boolean,
    url: String,
    onChangeUrl: (String) -> Unit,
    value: String,
    placeholder: String,
    allowedSchemes: List<String>?,
    examples: List<Pair<String, String>>,
    onSubmitted: () -> Unit,
    onDismiss: () -> Unit
) {
    var valueState by remember { mutableStateOf(value) }
    var changed by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var presentingHelp by remember { mutableStateOf(false) }

    fun submitUrl() {
        if (submitted) {
            return
        }
        valueState = cleanUrl(valueState)
        if (isValidUrl(valueState, allowedSchemes) != null) {
            onDismiss()
            return
        }
        submitted = true
        onChangeUrl(valueState)
        onSubmitted()
        onDismiss()
    }

    DisposableEffect(Unit) {
        onDispose {
            if (changed && !submitted) {
                submitUrl()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("URL")) })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Column {
                    MultiLineTextFieldView(
                        value = valueState,
                        onValueChange = { newValue ->
                            valueState = newValue
                            error = isValidUrl(newValue, allowedSchemes)
                            changed = true
                            if (newValue.contains("\n")) {
                                valueState = newValue.replace("\n", "")
                                submitUrl()
                            }
                        },
                        placeholder = placeholder
                    )
                    error?.let { FormFieldError(error = it) }
                }
            }
            item {
                TextButtonView(
                    title = localized("Examples"),
                    action = { presentingHelp = true }
                )
            }
        }
    }

    if (presentingHelp) {
        ModalBottomSheet(onDismissRequest = { presentingHelp = false }) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(localized("Examples")) },
                        actions = {
                            CloseToolbar(
                                presenting = presentingHelp,
                                onPresentingChange = { presentingHelp = it }
                            )
                        }
                    )
                }
            ) { padding ->
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    items(items = examples, key = { it.second }) { example ->
                        Column {
                            Text(localized(example.first))
                            UrlCopyView(url = example.second)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RtspClientStreamSettingsView(
    model: Model = LocalModel.current,
    rtspClient: SettingsRtspClient,
    stream: SettingsRtspClientStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("Stream") }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stream.name)
        Spacer(Modifier.weight(1f))
        Switch(
            checked = stream.enabled,
            onCheckedChange = { enabled ->
                stream.enabled = enabled
                TODO("reloadRtspClient")
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RtspClientStreamSettingsViewDestination(
    model: Model = LocalModel.current,
    rtspClient: SettingsRtspClient,
    stream: SettingsRtspClientStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    var transportExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Stream")) })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                NameEditView(
                    name = stream.name,
                    onNameChange = { stream.name = it },
                    existingNames = rtspClient.streams
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("URL") }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextItemLocalizedView(name = "URL", value = stream.url, sensitive = true)
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(localized("Transport"))
                    Spacer(Modifier.width(16.dp))
                    Box(Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(
                            expanded = transportExpanded,
                            onExpandedChange = { transportExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = stream.transport.toString(),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = transportExpanded
                                    )
                                },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = transportExpanded,
                                onDismissRequest = { transportExpanded = false }
                            ) {
                                SettingsRtspTransport.entries.forEach { transport ->
                                    DropdownMenuItem(
                                        text = { Text(transport.toString()) },
                                        onClick = {
                                            stream.transport = transport
                                            transportExpanded = false
                                            TODO("reloadRtspClient")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Column {
                    TextEditNavigationView(
                        title = localized("Latency"),
                        value = stream.latency.toString(),
                        onChange = { isValidIngestLatency(it) },
                        onSubmit = { text ->
                            val latency = text.toIntOrNull()
                            if (latency != null) {
                                stream.latency = latency
                                TODO("reloadRtspClient")
                            }
                        },
                        footers = listOf(
                            localized("5 or more milliseconds. 2000 ms by default.")
                        ),
                        keyboardType = KeyboardType.Number,
                        valueFormat = { "$it ms" }
                    )
                    Text(localized("The higher, the lower risk of stuttering."))
                }
            }
        }
    }
}
