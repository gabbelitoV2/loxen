package com.moblin.android.view.stream

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.various.model.DrawOnStream
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Orientation
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.videoeffects.drawOnStreamCreatePath
import com.moblin.android.view.utils.RgbColorPickerView
import java.util.UUID
import com.moblin.android.LocalModel
import com.moblin.android.various.model.drawOnStreamLineComplete
import com.moblin.android.various.model.drawOnStreamWipe
import com.moblin.android.various.model.drawOnStreamUndo

private var drawing = false

data class DrawOnStreamLine(
    val id: UUID = UUID.randomUUID(),
    var points: MutableList<Offset>,
    val width: Float,
    val color: Color,
)

@Composable
private fun DrawOnStreamCanvasView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    orientation: Orientation,
    drawOnStream: DrawOnStream,
) {
    val lines by drawOnStream.lines.collectAsState()
    BoxWithConstraints {
        Canvas(
            modifier = Modifier
                .size(maxWidth, maxHeight)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { position ->
                            if (!drawing) {
                                val newLine = DrawOnStreamLine(
                                    points = mutableListOf(position),
                                    width = drawOnStream.selectedWidth.value * density,
                                    color = drawOnStream.selectedColor.value,
                                )
                                drawOnStream.lines.value =
                                    (drawOnStream.lines.value + newLine).toMutableList()
                            }
                            drawing = true
                        },
                        onDrag = { change, _ ->
                            val currentLines = drawOnStream.lines.value.filterIsInstance<DrawOnStreamLine>()
                            val lastIndex = currentLines.indices.lastOrNull()
                            if (lastIndex != null) {
                                val last = currentLines[lastIndex]
                                val updated = currentLines.toMutableList()
                                updated[lastIndex] = last.copy(
                                    points = (last.points + change.position).toMutableList()
                                )
                                drawOnStream.lines.value = updated
                            }
                        },
                        onDragEnd = {
                            model.drawOnStreamLineComplete()
                            drawing = false
                        },
                    )
                },
        ) {
            for (line in lines.filterIsInstance<DrawOnStreamLine>()) {
                val width = line.width
                if (line.points.size > 1) {
                    drawPath(
                        path = drawOnStreamCreatePath(line.points.map { com.moblin.android.platform.coregraphics.CGPoint(it.x, it.y) }),
                        color = line.color,
                        style = Stroke(width = width),
                    )
                } else {
                    val point = line.points[0]
                    drawCircle(
                        color = line.color,
                        radius = 0.5f,
                        center = point,
                        style = Stroke(width = width),
                    )
                }
            }
            model.drawOnStreamSize = size
        }
    }
}

private fun buttonColor(drawOnStream: DrawOnStream): Color {
    return if (drawOnStream.lines.value.isEmpty()) {
        Color.Gray
    } else {
        Color.White
    }
}

@Composable
private fun DrawOnStreamControlsView(
    model: Model = LocalModel.current,
    drawOnStream: DrawOnStream,
) {
    val lines by drawOnStream.lines.collectAsState()
    val selectedWidth by drawOnStream.selectedWidth.collectAsState()
    val selectedColor by drawOnStream.selectedColor.collectAsState()
    Column {
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.padding(end = 15.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(backgroundColor)
                    .padding(8.dp),
            ) {
                IconButton(
                    onClick = {
                        model.drawOnStreamWipe()
                    },
                    enabled = lines.size > 0,
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = buttonColor(drawOnStream),
                    )
                }
                IconButton(
                    onClick = {
                        model.drawOnStreamUndo()
                    },
                    enabled = lines.size > 0,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = buttonColor(drawOnStream),
                    )
                }
                Box(modifier = Modifier.width(60.dp)) {
                    RgbColorPickerView(
                        title = "",
                        color = selectedColor,
                        onColorChanged = { drawOnStream.selectedColor.value = it },
                    ) {}
                }
                Slider(
                    value = selectedWidth,
                    onValueChange = {
                        drawOnStream.selectedWidth.value = it
                    },
                    valueRange = 1f..20f,
                    modifier = Modifier.width(150.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = selectedColor,
                        activeTrackColor = selectedColor,
                    ),
                )
            }
        }
    }
}

@Composable
fun DrawOnStreamView(model: Model = LocalModel.current) {
    val stream by model.stream.collectAsState()
    Box {
        DrawOnStreamCanvasView(
            model = model,
            stream = stream,
            orientation = model.orientation,
            drawOnStream = model.drawOnStream,
        )
        DrawOnStreamControlsView(model = model, drawOnStream = model.drawOnStream)
    }
}
