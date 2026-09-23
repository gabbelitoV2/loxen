package com.moblin.android.view.settings.catprinters

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.bluetoothNotAllowedMessage
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.InlinePickerItem
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun CatPrinterScannerSettingsView(
    model: Model = LocalModel.current,
    scanner: CatPrinterScanner,
    selectedId: String,
    onChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val bluetoothAllowed by model.bluetoothAllowed.collectAsState()
    val discoveredPeripherals by scanner.discoveredPeripherals.collectAsState()
    val palette = formPalette()

    DisposableEffect(scanner) {
        scanner.startScanningForDevices()
        onDispose {
            scanner.stopScanningForDevices()
        }
    }

    Form(title = "Device") {
        Section {
            if (!bluetoothAllowed) {
                FormRow {
                    Text(bluetoothNotAllowedMessage)
                }
            } else if (discoveredPeripherals.isEmpty()) {
                FormRow {
                    HCenter {
                        val transition = rememberInfiniteTransition()
                        val angle by transition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
                        )
                        Canvas(modifier = Modifier.size(20.dp)) {
                            drawArc(
                                color = palette.accent,
                                startAngle = angle,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                            )
                        }
                    }
                }
            } else {
                discoveredPeripherals.forEach { peripheral ->
                    val item = InlinePickerItem(
                        id = peripheral.identifier,
                        text = peripheral.name ?: localized("Unknown"),
                    )
                    key(item.id) {
                        FormButton(title = item.text) {
                            onChange(item.id)
                            onDismiss()
                        }
                    }
                }
            }
        }
    }
}

data class CatPrinterPeripheral(
    val identifier: String,
    val name: String?,
)

class CatPrinterScanner {
    val discoveredPeripherals = MutableStateFlow<List<CatPrinterPeripheral>>(emptyList())

    fun startScanningForDevices() {
    }

    fun stopScanningForDevices() {
    }
}
