package com.moblin.android.view.settings.blacksharkcoolers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsBlackSharkCoolerDevice
import com.moblin.android.various.settings.SettingsBlackSharkCoolerDevices
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.catprinters.IntegrationImageView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView

@Composable
fun BlackSharkCoolerDevicesSettingsView(
    model: Model = LocalModel.current,
    blackSharkCoolerDevices: SettingsBlackSharkCoolerDevices,
) {
    val statusTopRight = model.statusTopRight
    Form(title = localized("Black Shark coolers")) {
        Section {
            HCenter {
                IntegrationImageView(imageName = "BlackSharkMagCooler4Pro")
            }
        }
        Section(footerContent = {
            SwipeLeftToDeleteHelpView(kind = localized("a cooler"))
        }) {
            blackSharkCoolerDevices.devices.forEach { device ->
                key(device.id) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val pressed by interactionSource.collectIsPressedAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            BlackSharkCoolerDeviceSettingsView(
                                blackSharkCoolerDevices = blackSharkCoolerDevices,
                                device = device,
                                status = statusTopRight,
                            )
                        }
                        SystemImage(
                            name = "trash",
                            fontSize = 20.sp,
                            modifier = Modifier
                                .alpha(if (pressed) 0.2f else 1f)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {
                                    blackSharkCoolerDevices.devices =
                                        blackSharkCoolerDevices.devices.filterNot { it.id == device.id }
                                },
                            tint = formPalette().red,
                        )
                    }
                }
            }
            CreateButtonView {
                val device = SettingsBlackSharkCoolerDevice()
                device.name = makeUniqueName(
                    name = SettingsBlackSharkCoolerDevice.baseName,
                    existingNames = blackSharkCoolerDevices.devices,
                )
                blackSharkCoolerDevices.devices = blackSharkCoolerDevices.devices + device
            }
        }
    }
}
