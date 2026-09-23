package com.moblin.android.view.settings.savereset

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.loadControlBarBackgroundImage
import com.moblin.android.various.model.loadFaceBackgroundImage
import com.moblin.android.various.model.loadStealthModeImage
import com.moblin.android.various.model.reloadStream
import com.moblin.android.various.model.resetSelectedScene
import com.moblin.android.various.model.setCurrentStream
import com.moblin.android.various.utils.isPhone
import com.moblin.android.platform.swiftui.*

@Composable
fun SettingsResetView(model: Model = LocalModel.current) {
    var presentingResetConfirm by remember { mutableStateOf(false) }
    val palette = formPalette()
    FormButton(title = "Reset settings", destructive = true, centered = true) {
        presentingResetConfirm = true
    }
    if (presentingResetConfirm) {
        AlertDialog(
            onDismissRequest = { presentingResetConfirm = false },
            title = {
                Text(
                    text = localized("Are you sure?"),
                    style = formFootnoteStyle.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.secondaryLabel,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    presentingResetConfirm = false
                    model.settings.reset()
                    model.setCurrentStream()
                    model.reloadStream()
                    model.resetSelectedScene()
                    model.updateQuickButtonPairs()
                    model.loadStealthModeImage()
                    model.loadControlBarBackgroundImage()
                    model.loadFaceBackgroundImage()
                }) {
                    Text(text = localized("Reset settings"), style = formBodyStyle, color = palette.red)
                }
            },
            dismissButton = {
                if (isPhone()) {
                    TextButton(onClick = { presentingResetConfirm = false }) {
                        Text(
                            text = localized("Cancel"),
                            style = formBodyStyle.copy(fontWeight = FontWeight.SemiBold),
                            color = palette.accent,
                        )
                    }
                }
            },
            shape = RoundedCornerShape(13.dp),
            containerColor = palette.menu,
        )
    }
}
