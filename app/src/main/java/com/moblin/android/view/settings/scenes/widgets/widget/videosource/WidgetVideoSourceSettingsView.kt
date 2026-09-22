package com.moblin.android.view.settings.scenes.widgets.widget.videosource

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetVideoSource
import com.moblin.android.view.settings.scenes.scene.GrayTextView
import com.moblin.android.view.settings.scenes.scene.startScreenCatptureHelp
import com.moblin.android.view.settings.scenes.widgets.widget.effects.WidgetEffectsView
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.view.utils.InlinePickerView
import com.moblin.android.view.utils.VideoSourceRotationView

enum class AnchorPoint {
    topLeft,
    topRight,
    bottomLeft,
    bottomRight,
    center,
}

data class PositioningRectangle(
    val xTopLeft: Double,
    val yTopLeft: Double,
    val xBottomRight: Double,
    val yBottomRight: Double,
)

fun calculatePositioningRectangle(
    positionAnchorPoint: AnchorPoint?,
    cropX: Double,
    cropY: Double,
    cropWidth: Double,
    cropHeight: Double,
    position: Offset,
    size: Size,
    positionOffset: Size,
): PositioningRectangle {
    var xTopLeft = cropX
    var yTopLeft = cropY
    var xBottomRight = xTopLeft + cropWidth
    var yBottomRight = yTopLeft + cropHeight
    val positionX = (position.x / size.width + positionOffset.width).toDouble().coerceIn(0.0, 1.0)
    val positionY = (position.y / size.height + positionOffset.height).toDouble().coerceIn(0.0, 1.0)
    val minimumWidth = 0.05
    val minimumHeight = 0.04
    when (positionAnchorPoint) {
        AnchorPoint.topLeft -> {
            if (positionX + minimumWidth < xBottomRight) {
                xTopLeft = positionX
            }
            if (positionY + minimumHeight < yBottomRight) {
                yTopLeft = positionY
            }
        }
        AnchorPoint.topRight -> {
            if (positionX > xTopLeft + minimumWidth) {
                xBottomRight = positionX
            }
            if (positionY + minimumHeight < yBottomRight) {
                yTopLeft = positionY
            }
        }
        AnchorPoint.bottomLeft -> {
            if (positionX + minimumWidth < xBottomRight) {
                xTopLeft = positionX
            }
            if (positionY > yTopLeft + minimumHeight) {
                yBottomRight = positionY
            }
        }
        AnchorPoint.bottomRight -> {
            if (positionX > xTopLeft + minimumWidth) {
                xBottomRight = positionX
            }
            if (positionY > yTopLeft + minimumHeight) {
                yBottomRight = positionY
            }
        }
        AnchorPoint.center -> {
            val halfWidth = cropWidth / 2
            val halfHeight = cropHeight / 2
            var x = cropX
            var y = cropY
            if (positionX - halfWidth >= 0 && positionX + halfWidth <= 1) {
                x = positionX - halfWidth
            }
            if (positionY - halfHeight >= 0 && positionY + halfHeight <= 1) {
                y = positionY - halfHeight
            }
            xTopLeft = x
            yTopLeft = y
            xBottomRight = x + cropWidth
            yBottomRight = y + cropHeight
        }
        null -> {
        }
    }
    return PositioningRectangle(xTopLeft, yTopLeft, xBottomRight, yBottomRight)
}

fun DrawScope.drawPositioningRectangle(rectangle: Rect) {
    drawRect(
        color = Color.White.copy(alpha = 0.25f),
        topLeft = rectangle.topLeft,
        size = rectangle.size,
    )
    drawRect(
        color = Color.White,
        topLeft = rectangle.topLeft,
        size = rectangle.size,
        style = Stroke(width = 1.5f),
    )
    val corners = listOf(
        Offset(rectangle.left, rectangle.top),
        Offset(rectangle.right, rectangle.top),
        Offset(rectangle.right, rectangle.bottom),
        Offset(rectangle.left, rectangle.bottom),
    )
    for (corner in corners) {
        drawCircle(color = Color.White, radius = 6f, center = corner)
        drawCircle(color = Color.Black, radius = 6f, center = corner, style = Stroke(width = 1f))
    }
}

fun calculatePositioningAnchorPoint(
    location: Offset,
    size: Size,
    cropX: Double,
    cropY: Double,
    cropWidth: Double,
    cropHeight: Double,
): Pair<AnchorPoint?, Size> {
    val x = (location.x / size.width).toDouble()
    val y = (location.y / size.height).toDouble()
    val xTopLeft = cropX
    val yTopLeft = cropY
    val xBottomRight = cropX + cropWidth
    val yBottomRight = cropY + cropHeight
    val xCenter = xTopLeft + cropWidth / 2
    val yCenter = yTopLeft + cropHeight / 2
    val xCenterTopLeft = xTopLeft + cropWidth / 4
    val yCenterTopLeft = yTopLeft + cropHeight / 4
    val xCenterBottomRight = xBottomRight - cropWidth / 4
    val yCenterBottomRight = yBottomRight - cropHeight / 4
    return if (x > xCenterTopLeft && x < xCenterBottomRight && y > yCenterTopLeft &&
        y < yCenterBottomRight
    ) {
        Pair(AnchorPoint.center, Size((xCenter - x).toFloat(), (yCenter - y).toFloat()))
    } else if (x + 0.1 < xTopLeft || x > xBottomRight + 0.1 || y + 0.1 < yTopLeft ||
        y > yBottomRight + 0.1
    ) {
        Pair(AnchorPoint.center, Size((xCenter - x).toFloat(), (yCenter - y).toFloat()))
    } else if (x < xCenterTopLeft && y < yCenterTopLeft) {
        Pair(AnchorPoint.topLeft, Size((xTopLeft - x).toFloat(), (yTopLeft - y).toFloat()))
    } else if (x > xCenterBottomRight && y < yCenterTopLeft) {
        Pair(AnchorPoint.topRight, Size((xBottomRight - x).toFloat(), (yTopLeft - y).toFloat()))
    } else if (x < xCenterTopLeft && y > yCenterBottomRight) {
        Pair(AnchorPoint.bottomLeft, Size((xTopLeft - x).toFloat(), (yBottomRight - y).toFloat()))
    } else if (x > xCenterBottomRight && y > yCenterBottomRight) {
        Pair(
            AnchorPoint.bottomRight,
            Size((xBottomRight - x).toFloat(), (yBottomRight - y).toFloat()),
        )
    } else {
        Pair(null, Size.Zero)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetVideoSourceSettingsView(
    model: Model,
    widget: SettingsWidget,
    videoSource: SettingsWidgetVideoSource,
) {
    var presentingScreenCaptureAlert by remember { mutableStateOf(false) }
    var showVideoSourcePicker by remember { mutableStateOf(false) }

    fun onCameraChange(cameraId: String) {
        videoSource.updateCameraId(
            settingsCameraId = model.cameraIdToSettingsCameraId(cameraId = cameraId),
        )
        model.sceneUpdated(attachCamera = true, updateRemoteScene = false)
        if (model.isScreenCaptureCamera(cameraId = cameraId)) {
            presentingScreenCaptureAlert = true
        }
    }

    fun setEffectSettings() {
        model.getVideoSourceEffect(id = widget.id)?.setSettings(
            settings = videoSource.toEffectSettings(),
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showVideoSourcePicker = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Video source")
            Spacer(modifier = Modifier.weight(1f))
            GrayTextView(text = model.getCameraPositionName(videoSourceWidget = videoSource))
        }
        if (presentingScreenCaptureAlert) {
            AlertDialog(
                onDismissRequest = { presentingScreenCaptureAlert = false },
                title = { Text(startScreenCatptureHelp) },
                confirmButton = {
                    TextButton(onClick = { presentingScreenCaptureAlert = false }) {
                        Text("Got it")
                    }
                },
            )
        }
        if (showVideoSourcePicker) {
            ModalBottomSheet(onDismissRequest = { showVideoSourcePicker = false }) {
                InlinePickerView(
                    title = "Video source",
                    onChange = { cameraId ->
                        onCameraChange(cameraId)
                        showVideoSourcePicker = false
                    },
                    items = model.listCameras(excludeBuiltin = false).map {
                        InlinePickerItem(id = it.id, text = it.name)
                    },
                    selectedId = model.getCameraId(videoSourceWidget = videoSource),
                )
            }
        }
        VideoSourceRotationView(
            selectedRotation = videoSource.rotation,
            onSelectedRotationChange = { videoSource.rotation = it },
        )
        LaunchedEffect(videoSource.rotation) {
            setEffectSettings()
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mirror")
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = videoSource.mirror,
                onCheckedChange = { videoSource.mirror = it },
            )
        }
        LaunchedEffect(videoSource.mirror) {
            setEffectSettings()
        }
        Text("Face tracking", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Enabled")
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = videoSource.trackFaceEnabled,
                onCheckedChange = { videoSource.trackFaceEnabled = it },
            )
        }
        LaunchedEffect(videoSource.trackFaceEnabled) {
            setEffectSettings()
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Zoom")
            Slider(
                value = videoSource.trackFaceZoom.toFloat(),
                onValueChange = { videoSource.trackFaceZoom = it.toDouble() },
                valueRange = 0f..1f,
                steps = 99,
                modifier = Modifier.weight(1f),
            )
        }
        LaunchedEffect(videoSource.trackFaceZoom) {
            setEffectSettings()
        }
        WidgetEffectsView(model = model, widget = widget)
    }
}
