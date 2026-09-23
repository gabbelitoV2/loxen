package com.moblin.android.view.settings.scenes.widgets.widget.effects

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.swiftui.*
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectShape
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.AnchorPoint
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.calculatePositioningAnchorPoint
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.calculatePositioningRectangle
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.drawPositioningRectangle
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.various.model.getWidgetShapeEffect
import com.moblin.android.various.model.takeVideoSourcePreviewImage

@Composable
private fun CornerRadiusView(
    shape: SettingsVideoEffectShape,
    updateWidget: () -> Unit
) {
    Section(header = "Corner radius") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FormSlider(
                value = shape.cornerRadius.toFloat(),
                onValueChange = {
                    shape.cornerRadius = it.toFloat()
                    updateWidget()
                },
                modifier = Modifier.weight(1f),
                valueRange = 0f..1f
            )
            Box(
                modifier = Modifier.width(35.dp),
                contentAlignment = Alignment.Center
            ) {
                Text((shape.cornerRadius * 100).toInt().toString())
            }
        }
    }
}

@Composable
private fun BorderView(
    shape: SettingsVideoEffectShape,
    updateWidget: () -> Unit
) {
    Section(header = "Border") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Width")
            FormSlider(
                value = shape.borderWidth.toFloat(),
                onValueChange = {
                    shape.borderWidth = it.toDouble()
                    updateWidget()
                },
                modifier = Modifier.weight(1f),
                valueRange = 0f..1f
            )
        }
        RgbColorPickerView(
            title = "Color",
            color = shape.borderColorColor,
            onColorChanged = { color: Color ->
                shape.borderColorColor = color
                updateWidget()
            },
            onChange = { color ->
                shape.borderColor = color
                updateWidget()
            }
        )
    }
}

@Composable
private fun CropView(
    shape: SettingsVideoEffectShape,
    updateWidget: () -> Unit,
    previewImage: Bitmap?,
    isPortrait: Boolean
) {
    var position by remember { mutableStateOf(Offset(100f, 100f)) }
    var positionOffset by remember { mutableStateOf(Size(0f, 0f)) }
    var positionAnchorPoint by remember { mutableStateOf<AnchorPoint?>(null) }

    fun updatePositionAnchorPoint(location: Offset, size: Size) {
        if (positionAnchorPoint == null) {
            val (anchorPoint, offset) = calculatePositioningAnchorPoint(
                location,
                size,
                shape.cropX,
                shape.cropY,
                shape.cropWidth,
                shape.cropHeight
            )
            positionAnchorPoint = anchorPoint
            positionOffset = offset
        }
    }

    fun createPositionRectangle(size: Size): Rect {
        val (xTopLeft, yTopLeft, xBottomRight, yBottomRight) = calculatePositioningRectangle(
            positionAnchorPoint,
            shape.cropX,
            shape.cropY,
            shape.cropWidth,
            shape.cropHeight,
            position,
            size,
            positionOffset
        )
        shape.cropX = xTopLeft
        shape.cropY = yTopLeft
        shape.cropWidth = xBottomRight - xTopLeft
        shape.cropHeight = yBottomRight - yTopLeft
        updateWidget()
        return Rect(
            left = (shape.cropX * size.width).toFloat(),
            top = (shape.cropY * size.height).toFloat(),
            right = ((shape.cropX + shape.cropWidth) * size.width).toFloat(),
            bottom = ((shape.cropY + shape.cropHeight) * size.height).toFloat()
        )
    }

    val aspectRatio = if (isPortrait) 9f / 16f else 16f / 9f

    Section(header = "Crop") {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val preview = previewImage?.asImageBitmap()
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = null,
                    modifier = Modifier.aspectRatio(aspectRatio),
                    contentScale = ContentScale.Fit
                )
            } else {
                val placeholder = Bundle.image("GamlaLinkoping")?.asImageBitmap()
                if (placeholder != null) {
                    Image(
                        bitmap = placeholder,
                        contentDescription = null,
                        modifier = Modifier.aspectRatio(aspectRatio),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                position = offset
                                updatePositionAnchorPoint(
                                    position,
                                    Size(size.width.toFloat(), size.height.toFloat())
                                )
                            },
                            onDrag = { change, _ ->
                                position = change.position
                                updatePositionAnchorPoint(
                                    position,
                                    Size(size.width.toFloat(), size.height.toFloat())
                                )
                            },
                            onDragEnd = {
                                positionAnchorPoint = null
                            }
                        )
                    }
            ) {
                drawPositioningRectangle(createPositionRectangle(size))
            }
        }
        Toggle(
            title = "Enabled",
            isOn = shape.cropEnabled,
            onChange = { newValue ->
                shape.cropEnabled = newValue
                updateWidget()
            }
        )
    }
}

@Composable
fun ShapeEffectView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    shape: SettingsVideoEffectShape
) {
    var previewImage by remember { mutableStateOf<Bitmap?>(null) }
    val stream by model.stream.collectAsState()
    val isPortrait = stream.portrait

    fun updateWidget() {
        model.getWidgetShapeEffect(widget, effect)?.setSettings(shape.toSettings())
    }

    LaunchedEffect(Unit) {
        model.takeVideoSourcePreviewImage(widget) { image ->
            previewImage = image
        }
    }

    CornerRadiusView(shape = shape, updateWidget = { updateWidget() })
    BorderView(shape = shape, updateWidget = { updateWidget() })
    CropView(
        shape = shape,
        updateWidget = { updateWidget() },
        previewImage = previewImage,
        isPortrait = isPortrait
    )
}
