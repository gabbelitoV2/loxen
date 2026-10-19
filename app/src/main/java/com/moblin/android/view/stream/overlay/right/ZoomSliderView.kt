package com.moblin.android.view.stream.overlay.right

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.common.various.clamped
import com.moblin.android.platform.SystemImage
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.setZoomSpeed
import com.moblin.android.various.model.stopZoomSpeed
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val knobSize = 26.0

@Composable
fun StreamOverlayRightZoomSliderView(model: Model = LocalModel.current) {
    val scope = rememberCoroutineScope()
    var offset by remember { mutableStateOf(0f) }
    var animationJob by remember { mutableStateOf<Job?>(null) }
    val animateBack = {
        animationJob?.cancel()
        val start = offset
        animationJob = scope.launch {
            animate(initialValue = start, targetValue = 0f, animationSpec = spring()) { value, _ ->
                offset = value
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            animationJob?.cancel()
            model.stopZoomSpeed()
        }
    }
    Row(
        modifier = Modifier
            .padding(bottom = 5.dp)
            .border(width = 1.dp, color = pickerBorderColor, shape = RoundedCornerShape(7.dp))
            .clip(RoundedCornerShape(7.dp))
            .background(backgroundColor)
            .size(width = sliderWidth.dp, height = sliderHeight.dp)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SystemImage(name = "minus.magnifyingglass", fontSize = 17.sp, tint = Color.White)
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val knobSizePx = with(density) { knobSize.dp.toPx() }
            val halfWidth = max((widthPx - knobSizePx) / 2f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(halfWidth) {
                        detectDragGestures(
                            onDragStart = {
                                animationJob?.cancel()
                                offset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offset = (offset + dragAmount.x).clamped(to = -halfWidth..halfWidth)
                                model.setZoomSpeed(speed = offset / halfWidth)
                            },
                            onDragEnd = {
                                animateBack()
                                model.stopZoomSpeed()
                            },
                            onDragCancel = {
                                animateBack()
                                model.stopZoomSpeed()
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(percent = 50)),
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(Color.White, RoundedCornerShape(percent = 50)),
                )
                Box(
                    modifier = Modifier
                        .offset { IntOffset(offset.roundToInt(), 0) }
                        .shadow(elevation = 2.dp, shape = CircleShape, clip = false)
                        .size(knobSize.dp)
                        .background(Color.White, CircleShape),
                )
            }
        }
        SystemImage(name = "plus.magnifyingglass", fontSize = 17.sp, tint = Color.White)
    }
}
