package com.moblin.android.view.settings.display.quickbuttons

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.color
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Orientation
import com.moblin.android.various.model.StealthMode
import com.moblin.android.various.settings.SettingsQuickButton
import com.moblin.android.various.settings.SettingsQuickButtonType
import com.moblin.android.various.settings.SettingsQuickButtons
import com.moblin.android.various.settings.defaultQuickButtonColor
import com.moblin.android.view.controlbar.controlBarPages
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun QuickButtonStealthModeView(
    model: Model = LocalModel.current,
    stealthMode: StealthMode,
) {
    val image = stealthMode.image.collectAsState().value
    var presentingPicker by remember { mutableStateOf(false) }
    var selectedImageItem by remember { mutableStateOf<Any?>(null) }

    Column {
        Button(onClick = { presentingPicker = true }) {
            if (image != null) {
                HCenter {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                HCenter {
                    Text(localized("Select image"))
                }
            }
        }
        LaunchedEffect(presentingPicker) {
            if (presentingPicker) {
                Unit
            }
        }
        LaunchedEffect(selectedImageItem) {
            val imageItem = selectedImageItem
            selectedImageItem = null
            if (imageItem != null) {
                val data: ByteArray =
                    TODO()
                Unit
                stealthMode.image.value = BitmapFactory.decodeByteArray(data, 0, data.size)
            }
        }
        if (image != null) {
            TextButtonView(localized("Delete image")) {
                stealthMode.image.value = null
                Unit
            }
        }
        Text(localized("Show selected image instead of a black screen."))
    }
    LaunchedEffect(Unit) {
        model.checkPhotoLibraryAuthorization()
    }
}

@Composable
fun PositionButtonView(
    image: String,
    action: () -> Unit,
    enabled: Boolean = true,
) {
    Button(onClick = action, enabled = enabled) {
        Icon(imageVector = positionButtonIcon(image), contentDescription = null)
    }
}

private fun positionButtonIcon(image: String): ImageVector = when (image) {
    "arrow.up.circle" -> Icons.Default.ArrowUpward
    "arrow.down.circle" -> Icons.Default.ArrowDownward
    "arrow.left.circle" -> Icons.Default.ArrowBack
    "arrow.right.circle" -> Icons.Default.ArrowForward
    else -> Icons.Default.ArrowBack
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickButtonsButtonSettingsView(
    model: Model = LocalModel.current,
    orientation: Orientation,
    quickButtonsSettings: SettingsQuickButtons,
    button: SettingsQuickButton,
    showAll: Boolean,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isPortrait = orientation.isPortrait.collectAsState().value
    val enabled by button.enabled.collectAsState()
    val isOn by button.isOn.collectAsState()
    Column {
        TopAppBar(title = { Text("${button.name} quick button") })
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Text(
                    text = localized("Layout"),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    TextField(
                        value = button.page.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(localized("Page")) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        for (page in 1..controlBarPages) {
                            DropdownMenuItem(
                                text = { Text(page.toString()) },
                                onClick = {
                                    button.page.value = page
                                    model.quickButtons.page = page
                                    model.quickButtons.activePage.value = page
                                    model.updateQuickButtonPairs()
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(localized("Position"))
                    Spacer(Modifier.weight(1f))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        if (isPortrait) {
                            positionPortrait(model, quickButtonsSettings, button)
                        } else {
                            positionLandscape(model, quickButtonsSettings, button)
                        }
                    }
                }
            }
            item {
                Text(
                    text = localized("Color"),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                RgbColorPickerView(
                    title = localized("Background"),
                    color = button.color.collectAsState().value,
                    onColorChanged = { button.color.value = it },
                ) { newColor ->
                    button.backgroundColor = newColor
                    model.updateQuickButtonPairs()
                }
                TextButtonView(localized("Reset")) {
                    button.backgroundColor = defaultQuickButtonColor
                    button.color.value = (button.backgroundColor as RgbColor).color()
                    model.updateQuickButtonPairs()
                }
            }
            if (button.type == SettingsQuickButtonType.blackScreen) {
                item {
                    QuickButtonStealthModeView(
                        model = model,
                        stealthMode = model.stealthMode,
                    )
                }
            }
            if (showAll) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(localized("Enabled"))
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled ->
                                button.enabled.value = enabled
                                model.updateQuickButtonPairs()
                            },
                            enabled = !(isOn && enabled),
                        )
                    }
                }
                item {
                    ShortcutSectionView {
                        TextButton(onClick = { onNavigate("QuickButtonsSettingsView") }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = null,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(localized("Quick buttons"))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun positionPortrait(
    model: Model = LocalModel.current,
    quickButtonsSettings: SettingsQuickButtons,
    button: SettingsQuickButton,
) {
    val twoColumns by quickButtonsSettings.twoColumns.collectAsState()
    PositionButtonView(
        image = "arrow.up.circle",
        action = { moveLeftRight(model, button) },
        enabled = twoColumns,
    )
    Row {
        PositionButtonView(
            image = "arrow.left.circle",
            action = { moveUp(model, quickButtonsSettings, button) },
        )
        PositionButtonView(
            image = "arrow.down.circle",
            action = { moveLeftRight(model, button) },
            enabled = twoColumns,
        )
        PositionButtonView(
            image = "arrow.right.circle",
            action = { moveDown(model, quickButtonsSettings, button) },
        )
    }
}

@Composable
private fun positionLandscape(
    model: Model = LocalModel.current,
    quickButtonsSettings: SettingsQuickButtons,
    button: SettingsQuickButton,
) {
    val twoColumns by quickButtonsSettings.twoColumns.collectAsState()
    PositionButtonView(
        image = "arrow.up.circle",
        action = { moveUp(model, quickButtonsSettings, button) },
    )
    Row {
        PositionButtonView(
            image = "arrow.left.circle",
            action = { moveLeftRight(model, button) },
            enabled = twoColumns,
        )
        PositionButtonView(
            image = "arrow.down.circle",
            action = { moveDown(model, quickButtonsSettings, button) },
        )
        PositionButtonView(
            image = "arrow.right.circle",
            action = { moveLeftRight(model, button) },
            enabled = twoColumns,
        )
    }
}

private fun moveUp(
    model: Model,
    quickButtonsSettings: SettingsQuickButtons,
    button: SettingsQuickButton,
) {
    var otherButton: SettingsQuickButton? = null
    val pairs = model.getQuickButtonPairs(button.page.value)
    for ((pairIndex, pair) in pairs.withIndex()) {
        val otherPair = pairs[(pairIndex + 1) % pairs.size]
        if (pair.first.id == button.id) {
            if (quickButtonsSettings.twoColumns.value) {
                otherButton = otherPair.first
            } else {
                otherButton = otherPair.second
                if (otherButton == null) {
                    otherButton = otherPair.first
                }
            }
            break
        } else if (pair.second?.id == button.id) {
            if (quickButtonsSettings.twoColumns.value) {
                otherButton = otherPair.second
                if (otherButton == null) {
                    otherButton = pairs[0].second
                }
            } else {
                otherButton = pair.first
            }
            break
        }
    }
    swapButtons(model, button, otherButton)
}

private fun moveDown(
    model: Model,
    quickButtonsSettings: SettingsQuickButtons,
    button: SettingsQuickButton,
) {
    var otherButton: SettingsQuickButton? = null
    val pairs = model.getQuickButtonPairs(button.page.value)
    for ((pairIndex, pair) in pairs.withIndex()) {
        val otherPair = pairs[(pairs.size + pairIndex - 1) % pairs.size]
        if (pair.first.id == button.id) {
            if (quickButtonsSettings.twoColumns.value) {
                otherButton = otherPair.first
            } else {
                otherButton = pair.second
                if (otherButton == null) {
                    otherButton = otherPair.first
                }
            }
            break
        } else if (pair.second?.id == button.id) {
            if (quickButtonsSettings.twoColumns.value) {
                otherButton = otherPair.second
                if (otherButton == null) {
                    otherButton = pairs[pairs.size - 2].second
                }
            } else {
                otherButton = otherPair.first
            }
            break
        }
    }
    swapButtons(model, button, otherButton)
}

private fun moveLeftRight(
    model: Model,
    button: SettingsQuickButton,
) {
    val pair = model.getQuickButtonPairs(button.page.value).firstOrNull {
        it.first.id == button.id || it.second?.id == button.id
    } ?: return
    swapButtons(model, pair.first, pair.second)
}

private fun swapButtons(
    model: Model,
    firstButton: SettingsQuickButton?,
    secondButton: SettingsQuickButton?,
) {
    val database = model.database
    val firstIndex = database.quickButtons.indexOfFirst { it.id == firstButton?.id }
    val secondIndex = database.quickButtons.indexOfFirst { it.id == secondButton?.id }
    if (firstIndex == -1 || secondIndex == -1) {
        return
    }
    val temporary = database.quickButtons[firstIndex]
    database.quickButtons[firstIndex] = database.quickButtons[secondIndex]
    database.quickButtons[secondIndex] = temporary
    model.updateQuickButtonPairs()
}
