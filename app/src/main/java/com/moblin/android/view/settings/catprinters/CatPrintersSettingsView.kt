package com.moblin.android.view.settings.catprinters

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusTopRight
import com.moblin.android.various.settings.SettingsCatPrinter
import com.moblin.android.various.settings.SettingsCatPrinters
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

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
                .background(Color.White)
                .height((height ?: 130.0).dp),
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
    val name = device.name
    Text(
        text = name,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigate("CatPrinterSettingsView") },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatPrintersSettingsView(
    model: Model = LocalModel.current,
    catPrinters: SettingsCatPrinters,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val statusTopRight = model.statusTopRight
    val devices = catPrinters.devices.collectAsState().value
    val backgroundPrinting = catPrinters.backgroundPrinting.collectAsState().value
    val onDelete: (Set<Int>) -> Unit = { offsets ->
        catPrinters.devices.value = catPrinters.devices.value
            .filterIndexed { index, _ -> index !in offsets }
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Cat printers") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
        ) {
            item {
                HCenter {
                    IntegrationImageView(imageName = "CatPrinter")
                }
            }
            item {
                Text("A small affordable black and white printer.")
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Background printing",
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = backgroundPrinting,
                        onCheckedChange = { catPrinters.backgroundPrinting.value = it },
                    )
                }
            }
            item {
                Text(
                    text = "Print when the app is in background mode.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            items(items = devices, key = { it.id }) { device ->
                var menuExpanded by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CatPrinterSettingsWrapperView(
                        catPrinters = catPrinters,
                        device = device,
                        status = statusTopRight,
                        onNavigate = onNavigate,
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    menuExpanded = false
                                    val index = devices.indexOfFirst { it.id == device.id }
                                    if (index >= 0) {
                                        onDelete(setOf(index))
                                    }
                                },
                            )
                        }
                    }
                }
            }
            item {
                CreateButtonView {
                    val device = SettingsCatPrinter()
                    device.name = makeUniqueName(
                        name = SettingsCatPrinter.baseName,
                        existingNames = catPrinters.devices.value,
                    )
                    catPrinters.devices.value = catPrinters.devices.value + device
                }
            }
            item {
                SwipeLeftToDeleteHelpView(kind = localized("a printer"))
            }
        }
    }
}
