package com.moblin.android.view.stream.overlay.right

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.color
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.SceneSelector
import com.moblin.android.various.model.selectScene
import com.moblin.android.various.model.showSceneSettings
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.defaultSegmentedPickerSelectedColor

@Composable
private fun <T> OnChange(value: T, action: (T) -> Unit) {
    val currentAction by rememberUpdatedState(action)
    var previous by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (value != previous) {
            previous = value
            currentAction(value)
        }
    }
}

@Composable
private fun SceneItemView(
    model: Model = LocalModel.current,
    database: Database,
    scene: SettingsScene,
    width: Float,
) {
    fun height(): Double = if (database.bigButtons) segmentHeightBig else segmentHeight

    Box {
        PickerLabelText(
            text = scene.name,
            width = minOf(sceneSegmentWidth, maxOf((width - 20.0) / model.enabledScenes.size, 1.0)).dp,
            height = height().dp,
        )
        val quickSwitchGroup = scene.quickSwitchGroup
        if (quickSwitchGroup != null) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Text(
                    text = quickSwitchGroup.toString(),
                    fontSize = 8.sp,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .padding(bottom = 3.dp),
                )
            }
        }
    }
}

@Composable
fun StreamOverlayRightSceneSelectorView(
    model: Model = LocalModel.current,
    database: Database,
    sceneSelector: SceneSelector,
    width: Float,
) {
    val sceneIndex by sceneSelector.sceneIndex.collectAsState()

    fun selectedScene(): SettingsScene? =
        if (sceneIndex < model.enabledScenes.size) model.enabledScenes[sceneIndex] else null

    fun selectedSceneColor(): Color =
        (selectedScene()?.backgroundColor ?: defaultSegmentedPickerSelectedColor).color()

    OnChange(sceneIndex) { tag ->
        model.selectScene(id = model.enabledScenes[tag].id)
    }

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            modifier = Modifier
                .width(
                    minOf(
                        sceneSegmentWidth * model.enabledScenes.size,
                        maxOf(width - 20.0, 1.0),
                    ).dp,
                )
                .clip(RoundedCornerShape(7.dp))
                .background(pickerBackgroundColor)
                .border(1.dp, pickerBorderColor, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SegmentedHPicker(
                items = model.enabledScenes,
                selectedItem = selectedScene(),
                onSelectedItemChange = { value ->
                    val index = if (value != null) model.enabledScenes.indexOf(value) else -1
                    sceneSelector.sceneIndex.value = if (index >= 0) index else 0
                },
                selectedColor = selectedSceneColor(),
                onLongPress = { index ->
                    if (index < model.enabledScenes.size) {
                        model.showSceneSettings(scene = model.enabledScenes[index])
                    }
                },
            ) { scene ->
                SceneItemView(
                    model = model,
                    database = database,
                    scene = scene,
                    width = width,
                )
            }
        }
    }
}

@Composable
fun StreamOverlayRightSceneVSelectorView(
    model: Model = LocalModel.current,
    database: Database,
    sceneSelector: SceneSelector,
    width: Float,
) {
    val sceneIndex by sceneSelector.sceneIndex.collectAsState()

    fun selectedScene(): SettingsScene? =
        if (sceneIndex < model.enabledScenes.size) model.enabledScenes[sceneIndex] else null

    fun selectedSceneColor(): Color =
        (selectedScene()?.backgroundColor ?: defaultSegmentedPickerSelectedColor).color()

    OnChange(sceneIndex) { tag ->
        model.selectScene(id = model.enabledScenes[tag].id)
    }

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            modifier = Modifier
                .padding(bottom = 5.dp)
                .width(sceneSegmentWidth.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(pickerBackgroundColor)
                .border(1.dp, pickerBorderColor, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SegmentedVPicker(
                items = model.enabledScenes.reversed(),
                selectedItem = selectedScene(),
                onSelectedItemChange = { value ->
                    val index = if (value != null) model.enabledScenes.indexOf(value) else -1
                    sceneSelector.sceneIndex.value = if (index >= 0) index else 0
                },
                selectedColor = selectedSceneColor(),
                onLongPress = { index ->
                    if (index < model.enabledScenes.size) {
                        model.showSceneSettings(scene = model.enabledScenes[index])
                    }
                },
            ) { scene ->
                SceneItemView(
                    model = model,
                    database = database,
                    scene = scene,
                    width = width,
                )
            }
        }
    }
}
