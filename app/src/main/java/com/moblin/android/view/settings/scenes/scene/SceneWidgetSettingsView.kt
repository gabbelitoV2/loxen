package com.moblin.android.view.settings.scenes.scene

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetLayoutView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.WidgetShortcutView
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneWidgetSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
) {
    val layout = remember { mutableStateOf(sceneWidget.layout) }
    val numericInput = remember { mutableStateOf(database.sceneNumericInput) }
    LaunchedEffect(layout.value) { sceneWidget.layout = layout.value }
    LaunchedEffect(numericInput.value) { database.sceneNumericInput = numericInput.value }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(widget.name) })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                WidgetLayoutView(
                    model = model,
                    database = database,
                    layout = layout,
                    widget = widget,
                    numericInput = numericInput,
                )
            }
            item {
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
    }
}
