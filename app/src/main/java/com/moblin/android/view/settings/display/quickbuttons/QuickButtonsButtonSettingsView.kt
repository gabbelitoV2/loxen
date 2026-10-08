package com.moblin.android.view.settings.display.quickbuttons

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.color
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formPalette
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
import com.moblin.android.various.model.deleteStealthModeImage
import com.moblin.android.various.model.saveStealthModeImage

@Composable
private fun QuickButtonStealthModeView(
    model: Model = LocalModel.current,
    stealthMode: StealthMode,
) {
    val image = stealthMode.image.collectAsState().value
    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val data = com.moblin.android.platform.DocumentPicker.readInput(context, uri)?.use { stream ->
                stream.readBytes()
            }
            if (data != null) {
                model.saveStealthModeImage(data)
                stealthMode.image.value = BitmapFactory.decodeByteArray(data, 0, data.size)
            }
        }
    }
    Section(footer = localized("Show selected image instead of a black screen.")) {
        FormRow(onClick = {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }) {
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
        if (image != null) {
            CompositionLocalProvider(LocalTint provides formPalette().red) {
                TextButtonView(localized("Delete image")) {
                    stealthMode.image.value = null
                    model.deleteStealthModeImage()
                }
            }
        }
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .alpha(if (pressed) 0.2f else 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = action,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = positionButtonIcon(image),
            contentDescription = null,
            tint = formPalette().accent,
            modifier = Modifier.size(28.dp),
        )
    }
}

private fun positionButtonIcon(image: String): ImageVector = when (image) {
    "arrow.up.circle" -> Icons.Default.ArrowUpward
    "arrow.down.circle" -> Icons.Default.ArrowDownward
    "arrow.left.circle" -> Icons.Default.ArrowBack
    "arrow.right.circle" -> Icons.Default.ArrowForward
    else -> Icons.Default.ArrowBack
}

@Composable
fun QuickButtonsButtonSettingsView(
    model: Model = LocalModel.current,
    orientation: Orientation,
    quickButtonsSettings: SettingsQuickButtons,
    button: SettingsQuickButton,
    showAll: Boolean,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isPortrait by orientation.isPortrait.collectAsState()
    val enabled by button.enabled.collectAsState()
    val isOn by button.isOn.collectAsState()
    val page by button.page.collectAsState()
    Form(title = "${button.name} quick button") {
        Section(header = localized("Layout")) {
            Picker(
                title = localized("Page"),
                selection = page,
                options = (1..controlBarPages).toList(),
            ) { newPage ->
                button.page.value = newPage
                model.quickButtons.page = newPage
                model.quickButtons.activePage.value = newPage
                model.updateQuickButtonPairs()
                Unit
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
        Section(header = localized("Color")) {
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
            QuickButtonStealthModeView(
                model = model,
                stealthMode = model.stealthMode,
            )
        }
        if (showAll) {
            Section {
                Toggle(
                    title = localized("Enabled"),
                    isOn = enabled,
                    enabled = !(isOn && enabled),
                ) { newValue ->
                    button.enabled.value = newValue
                    model.updateQuickButtonPairs()
                }
            }
            ShortcutSectionView {
                NavigationLink(
                    destination = {
                        QuickButtonsSettingsView(model = model, showAll = false)
                    },
                ) {
                    Label(localized("Quick buttons"), systemImage = "rectangle.inset.topright.fill")
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
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
