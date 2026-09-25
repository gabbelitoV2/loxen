package com.moblin.android.view.settings.catprinters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.AssetImage
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.removing
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusTopRight
import com.moblin.android.various.settings.SettingsCatPrinter
import com.moblin.android.various.settings.SettingsCatPrinters
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView

@Composable
fun IntegrationImageView(imageName: String, height: Double? = null) {
    AssetImage(
        name = imageName,
        modifier = Modifier
            .height((height ?: 130.0).dp)
            .background(Color.White),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun CatPrinterSettingsWrapperView(
    catPrinters: SettingsCatPrinters,
    device: SettingsCatPrinter,
    status: StatusTopRight,
) {
    NavigationLink(destination = {
        CatPrinterSettingsView(catPrinters = catPrinters, device = device, status = status)
    }) {
        Text(device.name)
    }
}

@Composable
fun CatPrintersSettingsView(model: Model = LocalModel.current, catPrinters: SettingsCatPrinters) {
    val devices by catPrinters.devices.collectAsState()
    val backgroundPrinting by catPrinters.backgroundPrinting.collectAsState()
    Form(title = "Cat printers") {
        Section {
            HCenter {
                IntegrationImageView(imageName = "CatPrinter")
            }
            Text(localized("A small affordable black and white printer."))
        }
        Section(footer = "Print when the app is in background mode.") {
            Toggle(
                title = "Background printing",
                isOn = backgroundPrinting,
                onChange = { catPrinters.backgroundPrinting.value = it },
            )
        }
        Section(footerContent = {
            SwipeLeftToDeleteHelpView(kind = localized("a printer"))
        }) {
            ForEach(
                devices,
                id = { it.id },
                onDelete = { offsets ->
                    catPrinters.devices.value = catPrinters.devices.value.removing(atOffsets = offsets)
                },
            ) { device ->
                ContextMenuDeleteButton(action = {
                    catPrinters.devices.value = catPrinters.devices.value.filterNot { it.id == device.id }
                }) {
                    CatPrinterSettingsWrapperView(
                        catPrinters = catPrinters,
                        device = device,
                        status = model.statusTopRight,
                    )
                }
            }
            CreateButtonView {
                val device = SettingsCatPrinter()
                device.name = makeUniqueName(
                    name = SettingsCatPrinter.baseName,
                    existingNames = catPrinters.devices.value,
                )
                catPrinters.devices.value = catPrinters.devices.value + device
            }
        }
    }
}
