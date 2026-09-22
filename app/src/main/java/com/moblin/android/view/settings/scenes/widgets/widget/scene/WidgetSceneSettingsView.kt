package com.moblin.android.view.settings.scenes.widgets.widget.scene

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.settings.scenes.SceneNameView
import java.util.UUID
import com.moblin.android.LocalModel

@Composable
fun WidgetSceneSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    selectedSceneId: UUID,
) {
    var sceneId by remember { mutableStateOf(selectedSceneId) }
    var firstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(sceneId) {
        if (firstComposition) {
            firstComposition = false
            return@LaunchedEffect
        }
        widget.scene.sceneId = sceneId
        TODO("Model.resetSelectedScene(changeScene = false)")
    }
    Column {
        Text(
            text = "Scene",
            style = MaterialTheme.typography.titleSmall,
        )
        model.database.scenes.forEach { scene ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        sceneId = scene.id
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = sceneId == scene.id,
                    onClick = {
                        sceneId = scene.id
                    },
                )
                SceneNameView(scene = scene)
            }
        }
    }
}
