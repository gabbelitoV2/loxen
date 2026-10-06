package com.moblin.android.view.settings.savereset

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.loadControlBarBackgroundImage
import com.moblin.android.various.model.loadFaceBackgroundImage
import com.moblin.android.various.model.loadStealthModeImage
import com.moblin.android.various.model.reloadStream
import com.moblin.android.various.model.resetSelectedScene
import com.moblin.android.various.model.setCurrentStream
import com.moblin.android.platform.swiftui.*
import com.moblin.android.LocalModel

@Composable
fun SettingsResetView(model: Model = LocalModel.current) {
    var presentingResetConfirm by remember { mutableStateOf(false) }
    FormButton(title = "Reset settings", destructive = true, centered = true) {
        presentingResetConfirm = true
    }
    ConfirmationDialog(
        title = "Are you sure?",
        isPresented = presentingResetConfirm,
        onDismissRequest = { presentingResetConfirm = false },
        titleVisibility = Visibility.visible,
    ) {
        Button("Reset settings", role = ButtonRole.destructive) {
            model.settings.reset()
            model.setCurrentStream()
            model.reloadStream()
            model.resetSelectedScene()
            model.updateQuickButtonPairs()
            model.loadStealthModeImage()
            model.loadControlBarBackgroundImage()
            model.loadFaceBackgroundImage()
        }
    }
}
