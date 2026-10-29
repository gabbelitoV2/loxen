package com.moblin.android.view.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.various.model.StreamOverlay
import com.moblin.android.various.model.WidgetOutline
import com.moblin.android.various.model.WidgetOutlines
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidget

@Composable
private fun WidgetOutlineLabelView(widget: SettingsWidget) {
    Row(
        modifier = Modifier
            .wrapContentSize(unbounded = true)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SystemImage(name = widget.image(), fontSize = 11.sp)
        Text(
            text = widget.name,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun labelOutside(widget: SettingsWidget, rect: CGRect, leading: Boolean) {
    val above = rect.minY > 20
    var labelHeight by remember { mutableStateOf(0) }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(
                    when {
                        above && leading -> Alignment.TopStart
                        above -> Alignment.TopEnd
                        leading -> Alignment.BottomStart
                        else -> Alignment.BottomEnd
                    }
                )
                .offset {
                    IntOffset(
                        0,
                        if (above) {
                            -labelHeight - 4.dp.roundToPx()
                        } else {
                            4.dp.roundToPx()
                        },
                    )
                }
                .onSizeChanged { labelHeight = it.height },
        ) {
            WidgetOutlineLabelView(widget = widget)
        }
    }
}

@Composable
private fun sizedView(outline: WidgetOutline, rect: CGRect) {
    val inScene = outline.inScene
    Box(
        modifier = Modifier
            .offset { IntOffset(rect.minX.dp.roundToPx(), rect.minY.dp.roundToPx()) }
            .size(rect.width.dp.coerceAtLeast(0.dp), rect.height.dp.coerceAtLeast(0.dp))
            .drawBehind {
                drawRect(
                    color = Color.White,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = if (inScene) {
                            null
                        } else {
                            PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                        },
                    ),
                )
            },
    ) {
        if (rect.width >= 120 && rect.height >= 40) {
            Box(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                WidgetOutlineLabelView(widget = outline.widget)
            }
        } else {
            labelOutside(widget = outline.widget, rect = rect, leading = true)
        }
    }
}

@Composable
private fun anchorView(outline: WidgetOutline, rect: CGRect, mirror: Boolean) {
    val leading = outline.sceneWidget.layout.alignment.isLeft() != mirror
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset { IntOffset((rect.minX - 4).dp.roundToPx(), (rect.minY - 4).dp.roundToPx()) }
                .size(8.dp)
                .background(Color.White, CircleShape),
        )
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (rect.minX - rect.width / 2).dp.roundToPx(),
                        (rect.minY - rect.height / 2).dp.roundToPx(),
                    )
                }
                .size(rect.width.dp.coerceAtLeast(0.dp), rect.height.dp.coerceAtLeast(0.dp)),
        ) {
            labelOutside(widget = outline.widget, rect = rect, leading = leading)
        }
    }
}

@Composable
private fun WidgetOutlineView(outline: WidgetOutline, canvasSize: CGSize, mirror: Boolean, size: CGSize) {
    val scale = canvasSize.width / size.width
    val x = if (mirror) canvasSize.width - outline.rect.maxX else outline.rect.minX
    val rect = CGRect(
        x = x / scale,
        y = outline.rect.minY / scale,
        width = outline.rect.width / scale,
        height = outline.rect.height / scale,
    )
    Box(modifier = Modifier.fillMaxSize()) {
        if (outline.sized) {
            sizedView(outline = outline, rect = rect)
        } else {
            anchorView(outline = outline, rect = rect, mirror = mirror)
        }
    }
}

@Composable
fun WidgetOutlinesView(widgetOutlines: WidgetOutlines, database: Database, streamOverlay: StreamOverlay, size: CGSize) {
    val enabled by widgetOutlines.enabled.collectAsState()
    if (enabled) {
        val outlines by widgetOutlines.outlines.collectAsState()
        val canvasSize by widgetOutlines.canvasSize.collectAsState()
        val isFrontCameraSelected by streamOverlay.isFrontCameraSelected.collectAsState()
        val mirror = isFrontCameraSelected && !database.mirrorFrontCameraOnStream
        Box(modifier = Modifier.size(size.width.dp, size.height.dp)) {
            outlines.forEachIndexed { index, outline ->
                key(index) {
                    WidgetOutlineView(
                        outline = outline,
                        canvasSize = canvasSize,
                        mirror = mirror,
                        size = size,
                    )
                }
            }
        }
    }
}
