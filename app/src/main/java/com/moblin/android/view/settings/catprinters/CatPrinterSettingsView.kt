package com.moblin.android.view.settings.catprinters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.integrations.catprinter.CatPrinterState
import com.moblin.android.integrations.catprinter.catPrinterScanner
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusTopRight
import com.moblin.android.various.settings.SettingsCatPrinter
import com.moblin.android.various.settings.SettingsCatPrinters
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextButtonView
import java.util.UUID
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private fun formatCatPrinterState(state: CatPrinterState?): String {
    return when (state) {
        null, CatPrinterState.disconnected -> localized("Disconnected")
        CatPrinterState.discovering -> localized("Discovering")
        CatPrinterState.connecting -> localized("Connecting")
        CatPrinterState.connected -> localized("Connected")
        else -> localized("Unknown")
    }
}

fun onDeviceChange(device: SettingsCatPrinter, value: String) {
    val deviceId = runCatching { UUID.fromString(value) }.getOrNull() ?: return
    TODO(
        "No setter for SettingsCatPrinter.bluetoothPeripheralName and bluetoothPeripheralId: " +
            "$deviceId"
    )
}

@Composable
fun CatPrinterSettingsView(
    model: Model = LocalModel.current,
    catPrinters: SettingsCatPrinters,
    device: SettingsCatPrinter,
    status: StatusTopRight,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val name = device.name
    val bluetoothPeripheralId by device.bluetoothPeripheralId.collectAsState()
    val bluetoothPeripheralName by device.bluetoothPeripheralName.collectAsState()
    val enabled by device.enabled.collectAsState()
    val printChat by device.printChat.collectAsState()
    val printSnapshots by device.printSnapshots.collectAsState()
    val faxMeowSound by device.faxMeowSound.collectAsState()
    val catPrinterState by status.catPrinterState.collectAsState()
    val devices by catPrinters.devices.collectAsState()

    fun state(): String {
        return formatCatPrinterState(catPrinterState)
    }

    fun canEnable(): Boolean {
        return bluetoothPeripheralId != null
    }

    LaunchedEffect(Unit) {
        TODO("Model.setCurrentCatPrinter is not available in this port")
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            NameEditView(
                name = name,
                onNameChange = { newName ->
                    TODO("No setter for SettingsCatPrinter.name in this port: $newName")
                },
                existingNames = devices,
            )
        }
        item {
            Text(
                text = localized("Device"),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !enabled) {
                        onNavigate("CatPrinterScannerSettingsView")
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                GrayTextView(text = bluetoothPeripheralName ?: localized("Select device"))
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = localized("Enabled"),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = { value ->
                        if (value) {
                            TODO("Model.enableCatPrinter is not available in this port")
                        } else {
                            TODO("Model.disableCatPrinter is not available in this port")
                        }
                    },
                    enabled = canEnable(),
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = localized("Print chat"),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = printChat,
                    onCheckedChange = { value ->
                        TODO("No setter for SettingsCatPrinter.printChat in this port: $value")
                    },
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = localized("Print snapshots"),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = printSnapshots,
                    onCheckedChange = { value ->
                        TODO("No setter for SettingsCatPrinter.printSnapshots in this port: $value")
                    },
                )
            }
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNavigate("PrintAlerts")
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(text = localized("Print alerts"))
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = localized("Fax meow sound"),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = faxMeowSound,
                    onCheckedChange = { value ->
                        TODO("No setter for SettingsCatPrinter.faxMeowSound in this port: $value")
                    },
                )
            }
        }
        if (enabled) {
            item {
                HCenter {
                    Text(text = state())
                }
            }
            item {
                TextButtonView("Test") {
                    TODO("Model.catPrinterPrintTestImage is not available in this port")
                }
            }
        }
    }
}
