package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsColor
import com.moblin.android.various.settings.SettingsColorLut
import com.moblin.android.view.settings.camera.CameraSettingsLutsView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.various.model.sceneUpdated

@Composable
private fun LutView(model: Model = LocalModel.current, lut: SettingsColorLut) {
    Toggle(
        title = lut.name,
        isOn = lut.enabled,
        onChange = { value ->
            lut.enabled = value
            model.sceneUpdated(updateRemoteScene = false)
        },
    )
}

@Composable
fun QuickButtonLutsView(
    model: Model = LocalModel.current,
    color: SettingsColor,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = "LUTs") {
        Section {
            color.allLuts().forEach { lut ->
                LutView(model = model, lut = lut)
            }
        }
        ShortcutSectionView {
            NavigationLink(
                destination = {
                    CameraSettingsLutsView(color = color)
                },
            ) {
                Label("LUTs", systemImage = "camera")
            }
        }
    }
}
