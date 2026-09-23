package com.moblin.android.view.settings.catprinters

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formPalette
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
    val context = LocalContext.current
    val resourceId = remember(imageName) {
        context.resources.getIdentifier(imageName, "drawable", context.packageName)
    }
    if (resourceId != 0) {
        Image(
            painter = painterResource(id = resourceId),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height((height ?: 130.0).dp)
                .background(Color.White),
        )
    }
}

@Composable
private fun CatPrinterSettingsWrapperView(
    catPrinters: SettingsCatPrinters,
    device: SettingsCatPrinter,
    status: StatusTopRight,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
    modifier: Modifier = Modifier,
) {
    val palette = formPalette()
    val interactionSource = remember { MutableInteractionSource() }
    NavigationLink(
        destination = {
            CatPrinterSettingsView(
                catPrinters = catPrinters,
                device = device,
                status = status,
            )
        },
        label = {
            Text(
                text = device.name,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = localized("Delete"),
                color = palette.red,
                modifier = Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    catPrinters.devices.value =
                        catPrinters.devices.value.filter { it.id != device.id }
                },
            )
        },
    )
}

@Composable
fun CatPrintersSettingsView(
    model: Model = LocalModel.current,
    catPrinters: SettingsCatPrinters,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val statusTopRight = model.statusTopRight
    val devices by catPrinters.devices.collectAsState()
    val backgroundPrinting by catPrinters.backgroundPrinting.collectAsState()
    Form(title = "Cat printers") {
        Section {
            HCenter {
                IntegrationImageView(imageName = "CatPrinter")
            }
            Text(localized("A small affordable black and white printer."))
        }
        Section(
            footer = localized("Print when the app is in background mode."),
        ) {
            Toggle(
                title = localized("Background printing"),
                isOn = backgroundPrinting,
            ) { value ->
                catPrinters.backgroundPrinting.value = value
            }
        }
        Section(
            footerContent = {
                SwipeLeftToDeleteHelpView(kind = localized("a printer"))
            },
        ) {
            devices.forEach { device ->
                key(device.id) {
                    CatPrinterSettingsWrapperView(
                        catPrinters = catPrinters,
                        device = device,
                        status = statusTopRight,
                        onNavigate = onNavigate,
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
