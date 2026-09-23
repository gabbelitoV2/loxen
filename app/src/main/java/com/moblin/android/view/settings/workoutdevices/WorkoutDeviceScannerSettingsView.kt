package com.moblin.android.view.settings.workoutdevices

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.bluetoothNotAllowedMessage
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.InlinePickerItem

@Composable
fun WorkoutDeviceScannerSettingsView(
    model: Model = LocalModel.current,
    onChange: (String) -> Unit,
    selectedId: String,
    onSelectedIdChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val bluetoothAllowed = model.bluetoothAllowed.collectAsState().value
    val discoveredPeripherals = emptyList<InlinePickerItem>()

    Form(title = "Device") {
        Section {
            if (!bluetoothAllowed) {
                Text(text = bluetoothNotAllowedMessage)
            } else if (discoveredPeripherals.isEmpty()) {
                HCenter {
                    CircularProgressIndicator()
                }
            } else {
                discoveredPeripherals.forEach { item ->
                    FormRow(
                        onClick = {
                            onChange(item.id)
                            onDismiss()
                        },
                    ) {
                        Text(text = item.text)
                    }
                }
            }
        }
    }
}
