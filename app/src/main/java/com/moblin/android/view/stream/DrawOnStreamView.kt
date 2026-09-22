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
import java.util.UUID
import com.moblin.android.LocalModel

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
    BoxWithConstraints {
        val layout = model.streamViewLayout(maxWidth, maxHeight)
        Canvas(
            modifier = Modifier
                .size(layout.size.width, layout.size.height)
                .offset(layout.offset.x, layout.offset.y)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { position ->
                            if (!drawing) {
                                drawOnStream.lines.add(
                                    DrawOnStreamLine(
                                        points = mutableListOf(position),
                                        width = drawOnStream.selectedWidth,
                                        color = drawOnStream.selectedColor,
                                    )
                                )
                            }
                            drawing = true
                        },
                        onDrag = { change, _ ->
                            val lastIndex = drawOnStream.lines.indices.lastOrNull()
                            if (lastIndex != null) {
                                drawOnStream.lines[lastIndex].points.add(change.position)
                            }
                        },
                        onDragEnd = {
                            model.drawOnStreamLineComplete()
                            drawing = false
                        },
                    )
                },
        ) {
            for (line in drawOnStream.lines) {
                val width = line.width
                if (line.points.size > 1) {
                    drawPath(
                        path = drawOnStreamCreatePath(line.points),
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
    return if (drawOnStream.lines.isEmpty()) {
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
    Column {
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.padding(end = 15.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .background(backgroundColor)
                    .clip(RoundedCornerShape(5.dp)),
            ) {
                IconButton(
                    onClick = {
                        model.drawOnStreamWipe()
                    },
                    enabled = drawOnStream.lines.isNotEmpty(),
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
                    enabled = drawOnStream.lines.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = buttonColor(drawOnStream),
                    )
                }
                TODO("no Compose counterpart for ColorPicker")
                Slider(
                    value = drawOnStream.selectedWidth,
                    onValueChange = {
                        drawOnStream.selectedWidth = it
                    },
                    valueRange = 1f..20f,
                    modifier = Modifier.width(150.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = drawOnStream.selectedColor,
                        activeTrackColor = drawOnStream.selectedColor,
                    ),
                )
            }
        }
    }
}

@Composable
fun DrawOnStreamView(model: Model = LocalModel.current) {
    Box {
        DrawOnStreamCanvasView(
            model = model,
            stream = model.stream,
            orientation = model.orientation,
            drawOnStream = model.drawOnStream,
        )
        DrawOnStreamControlsView(model = model, drawOnStream = model.drawOnStream)
    }
}
