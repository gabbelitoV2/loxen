package com.moblin.android.view.settings.scenes.widgets.widget.scene

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.PickerStyle
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetScene
import java.util.UUID
import com.moblin.android.localized
import com.moblin.android.various.model.resetSelectedScene
import com.moblin.android.various.model.selectableScenes

@Composable
fun WidgetSceneSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    scene: SettingsWidgetScene,
) {
    val scenes = model.selectableScenes(widget = widget)
    Section(header = localized("Scene")) {
        Picker(
            title = "",
            selection = scene.sceneId,
            options = listOf<UUID?>(null) + scenes.map { it.id },
            text = { id -> scenes.firstOrNull { it.id == id }?.name ?: localized("-- None --") },
            pickerStyle = PickerStyle.inline,
            onChange = { newSceneId ->
                scene.sceneId = newSceneId
                model.resetSelectedScene(changeScene = false)
            },
        )
    }
}
