package com.moblin.android.view.stream.overlay.right

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.SceneSelector
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.defaultSegmentedPickerSelectedColor
import kotlin.math.max
import kotlin.math.min
import com.moblin.android.LocalModel
import com.moblin.android.common.various.RgbColor

private fun RgbColor.color(): Color = Color(
    red = red.toFloat() / 255f,
    green = green.toFloat() / 255f,
    blue = blue.toFloat() / 255f,
)

private fun Color.color(): Color = this

@Composable
private fun SceneItemView(
    model: Model = LocalModel.current,
    database: Database,
    scene: SettingsScene,
    width: Float,
) {
    val enabledScenes = model.enabledScenes

    fun height(): Double = if (database.bigButtons) segmentHeightBig else segmentHeight

    Box {
        Text(
            text = scene.name,
            modifier = Modifier.size(
                width = min(
                    sceneSegmentWidth.toDouble(),
                    max((width - 20f) / enabledScenes.size.toFloat(), 1f).toDouble(),
                ).dp,
                height = height().dp,
            ),
        )
        scene.quickSwitchGroup?.let { quickSwitchGroup ->
            Row(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.weight(1f))
                Column(modifier = Modifier.fillMaxHeight()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = quickSwitchGroup.toString(),
                        fontSize = 8.sp,
                        modifier = Modifier.padding(end = 4.dp, bottom = 3.dp),
                    )
                }
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
    val enabledScenes = model.enabledScenes
    val sceneIndex by sceneSelector.sceneIndex.collectAsState()

    fun selectedScene(): SettingsScene? =
        if (sceneIndex < enabledScenes.size) enabledScenes[sceneIndex] else null

    fun selectedSceneColor(): Color =
        selectedScene()?.backgroundColor?.color() ?: defaultSegmentedPickerSelectedColor.color()

    LaunchedEffect(sceneIndex) {
        if (sceneIndex < enabledScenes.size) {
            Unit
        }
    }

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(pickerBackgroundColor)
                .border(1.dp, pickerBorderColor, RoundedCornerShape(7.dp))
                .width(
                    min(
                        sceneSegmentWidth.toDouble() * enabledScenes.size,
                        max((width - 20f).toDouble(), 1.0),
                    ).dp,
                ),
        ) {
            SegmentedHPicker(
                items = enabledScenes,
                selectedItem = selectedScene(),
                onSelectedItemChange = { value ->
                    val index = value?.let { enabledScenes.indexOf(it) } ?: -1
                    sceneSelector.sceneIndex.value = if (index >= 0) index else 0
                },
                selectedColor = selectedSceneColor(),
                onLongPress = { index ->
                    if (index < enabledScenes.size) {
                        Unit
                    }
                },
                content = { scene ->
                    SceneItemView(
                        model = model,
                        database = database,
                        scene = scene,
                        width = width,
                    )
                },
            )
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
    val enabledScenes = model.enabledScenes
    val sceneIndex by sceneSelector.sceneIndex.collectAsState()

    fun selectedScene(): SettingsScene? =
        if (sceneIndex < enabledScenes.size) enabledScenes[sceneIndex] else null

    fun selectedSceneColor(): Color =
        selectedScene()?.backgroundColor?.color() ?: defaultSegmentedPickerSelectedColor.color()

    LaunchedEffect(sceneIndex) {
        if (sceneIndex < enabledScenes.size) {
            Unit
        }
    }

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            modifier = Modifier
                .padding(bottom = 5.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(pickerBackgroundColor)
                .border(1.dp, pickerBorderColor, RoundedCornerShape(7.dp))
                .width(sceneSegmentWidth.dp),
        ) {
            SegmentedVPicker(
                items = enabledScenes.reversed(),
                selectedItem = selectedScene(),
                onSelectedItemChange = { value ->
                    val index = value?.let { enabledScenes.indexOf(it) } ?: -1
                    sceneSelector.sceneIndex.value = if (index >= 0) index else 0
                },
                selectedColor = selectedSceneColor(),
                onLongPress = { index ->
                    if (index < enabledScenes.size) {
                        Unit
                    }
                },
                content = { scene ->
                    SceneItemView(
                        model = model,
                        database = database,
                        scene = scene,
                        width = width,
                    )
                },
            )
        }
    }
}
