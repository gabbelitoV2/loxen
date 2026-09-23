package com.moblin.android.view.utils

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

private const val shadowRadius = 0.5f

fun Modifier.stroke(color: Color, width: Float = 1f): Modifier {
    if (width <= 0f) {
        return this
    }
    return this.drawWithCache {
        val widthPx = width * density
        val radiusPx = shadowRadius * density
        val bleed = widthPx + radiusPx * 2f
        val paint = Paint().apply {
            isAntiAlias = true
            colorFilter = PorterDuffColorFilter(color.toArgb(), PorterDuff.Mode.SRC_IN)
            maskFilter = BlurMaskFilter(radiusPx, BlurMaskFilter.Blur.NORMAL)
        }
        val offsets = listOf(
            widthPx to 0f,
            -widthPx to 0f,
            0f to widthPx,
            0f to -widthPx,
        )
        onDrawWithContent {
            offsets.forEach { (dx, dy) ->
                withTransform({ translate(dx, dy) }) {
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        nativeCanvas.saveLayer(
                            -bleed,
                            -bleed,
                            size.width + bleed,
                            size.height + bleed,
                            paint
                        )
                        this@onDrawWithContent.drawContent()
                        nativeCanvas.restore()
                    }
                }
            }
            drawContent()
        }
    }
}
