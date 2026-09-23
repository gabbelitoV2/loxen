package com.moblin.android.view.settings.scenes.scene

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetLayoutView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.WidgetShortcutView

@Composable
fun SceneWidgetSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
) {
    val layout = binding(get = { sceneWidget.layout }, set = { sceneWidget.layout = it })
    val numericInput = binding(
        get = { database.sceneNumericInput },
        set = { database.sceneNumericInput = it },
    )
    Form(title = widget.name) {
        WidgetLayoutView(
            model = model,
            database = database,
            layout = layout,
            widget = widget,
            numericInput = numericInput,
        )
        ShortcutSectionView {
            WidgetShortcutView(model = model, database = model.database, widget = widget)
            if (widget.type == SettingsWidgetType.scene) {
                val scene = model.database.scenes.firstOrNull { it.id == widget.scene.sceneId }
                if (scene != null) {
                    SceneShortcutView(database = model.database, scene = scene)
                }
            }
        }
    }
}
