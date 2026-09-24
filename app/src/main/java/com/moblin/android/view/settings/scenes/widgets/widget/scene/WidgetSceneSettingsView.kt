package com.moblin.android.view.settings.scenes.widgets.widget.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.PickerStyle
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import java.util.UUID
import com.moblin.android.localized
import com.moblin.android.various.model.resetSelectedScene

@Composable
fun WidgetSceneSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    selectedSceneId: UUID,
) {
    var sceneId by remember { mutableStateOf(selectedSceneId) }
    val scenes = model.database.scenes
    Section(header = localized("Scene")) {
        Picker(
            title = "",
            selection = sceneId,
            options = scenes.map { it.id },
            text = { id -> scenes.firstOrNull { it.id == id }?.name ?: "" },
            pickerStyle = PickerStyle.inline,
            onChange = { newSceneId ->
                sceneId = newSceneId
                widget.scene.sceneId = newSceneId
                model.resetSelectedScene(changeScene = false)
            },
        )
    }
}
