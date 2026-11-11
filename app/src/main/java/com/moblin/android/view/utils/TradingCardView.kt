package com.moblin.android.view.utils

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.moblin.android.platform.swiftui.AssetImage

private const val TradingCardReferenceDateEpochSeconds = 978_307_200.0

private const val TradingCardFrameInterval = 1.0 / 30

fun strokeTradingCardDash(
    context: DrawScope,
    path: Path,
    color: Color,
    lineWidth: Float,
    dash: Float,
    gap: Float,
    time: Double,
) {
    val length = dash + gap
    val period = length * 0.055
    val phase = -length * (time % period) / period
    context.drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = lineWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), phase.toFloat()),
        ),
    )
}

fun tradingCardPulseOpacity(time: Double): Double {
    val progress = time % 1.6 / 1.6
    val x = if (progress < 0.5) 2 * progress else 2 - 2 * progress
    return 1 - 0.75 * x * x * (3 - 2 * x)
}

private fun tradingCardReferenceTime(): Double {
    return System.currentTimeMillis() / 1000.0 - TradingCardReferenceDateEpochSeconds
}

@Composable
private fun rememberTradingCardTime(paused: Boolean): Double {
    var time by remember { mutableStateOf(tradingCardReferenceTime()) }
    LaunchedEffect(paused) {
        if (paused) {
            return@LaunchedEffect
        }
        var lastFrameNs = 0L
        while (true) {
            withFrameNanos { frameNs ->
                if (lastFrameNs == 0L || (frameNs - lastFrameNs) / 1_000_000_000.0 >= TradingCardFrameInterval) {
                    lastFrameNs = frameNs
                    time = tradingCardReferenceTime()
                }
            }
        }
    }
    return time
}

@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

@Composable
fun TradingCardView(
    name: String,
    background: Color,
    hasFront: Boolean = true,
    draw: ((DrawScope, Double) -> Unit)? = null,
) {
    val reduceMotion = rememberReduceMotion()
    val time = rememberTradingCardTime(paused = reduceMotion || draw == null)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .aspectRatio(260f / 150f),
        ) {
            if (draw != null) {
                AssetImage(
                    name = "${name}CardBack",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.FillBounds,
                )
                Canvas(modifier = Modifier.matchParentSize()) {
                    val factor = size.width / 260f
                    scale(factor, factor) {
                        draw(this, time)
                    }
                }
                if (hasFront) {
                    AssetImage(
                        name = "${name}CardFront",
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                }
            } else {
                AssetImage(
                    name = "${name}Card",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.FillBounds,
                )
            }
        }
    }
}
