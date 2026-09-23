package com.moblin.android.view.settings.importexport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.utils.moblinSettingsFileType
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.TextButtonView

@Composable
private fun SettingsFilePickerView(model: Model = LocalModel.current, onFinished: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { model.onDocumentPickerUrl?.invoke(it.toString()) }
        onFinished()
    }
    LaunchedEffect(Unit) {
        launcher.launch(arrayOf(moblinSettingsFileType))
    }
}

private enum class ImportState {
    idle,
    fromFile,
    fromClipboard,
}

private fun Model.importSettingsWithConfirmation(block: () -> Unit) {
    Unit
}

private fun Model.importSettingsFromFile(url: String, block: () -> Unit) {
    Unit
}

private fun Model.importSettingsFromClipboard(block: () -> Unit) {
    Unit
}

@Composable
fun ImportSettingsView(model: Model = LocalModel.current) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    var importState by remember { mutableStateOf(ImportState.idle) }
    val enabled = !isLive && !isRecording && importState == ImportState.idle
    Section {
        if (importState == ImportState.fromFile) {
            HCenter {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = formPalette().gray,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            TextButtonView(
                title = "Import from file",
            ) {
                if (enabled) {
                    showPicker = true
                    model.onDocumentPickerUrl = { url ->
                        model.importSettingsWithConfirmation {
                            importState = ImportState.fromFile
                            model.importSettingsFromFile(url) {
                                importState = ImportState.idle
                            }
                        }
                    }
                }
            }
        }
    }
    Section {
        if (importState == ImportState.fromClipboard) {
            HCenter {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = formPalette().gray,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            TextButtonView(
                title = "Import from clipboard",
            ) {
                if (enabled) {
                    model.importSettingsWithConfirmation {
                        importState = ImportState.fromClipboard
                        model.importSettingsFromClipboard {
                            importState = ImportState.idle
                        }
                    }
                }
            }
        }
    }
    if (showPicker) {
        Sheet(onDismissRequest = { showPicker = false }) {
            SettingsFilePickerView(model) {
                showPicker = false
            }
        }
    }
}
