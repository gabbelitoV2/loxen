package com.moblin.android.view.utils

import android.graphics.BlurMaskFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
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
    return this.drawWithContent {
        val density = this.density
        val widthPx = width * density
        val radiusPx = shadowRadius * density
        val paint = android.graphics.Paint().apply {
            colorFilter = PorterDuffColorFilter(color.toArgb(), PorterDuff.Mode.SRC_IN)
            maskFilter = BlurMaskFilter(radiusPx, BlurMaskFilter.Blur.NORMAL)
        }
        val bleed = widthPx + radiusPx * 2
        fun drawShadow(dx: Float, dy: Float) {
            withTransform({ translate(dx, dy) }) {
                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas
                    nativeCanvas.saveLayer(-bleed, -bleed, size.width + bleed, size.height + bleed, paint)
                    drawContent()
                    nativeCanvas.restore()
                }
            }
        }
        drawShadow(widthPx, 0f)
        drawShadow(-widthPx, 0f)
        drawShadow(0f, widthPx)
        drawShadow(0f, -widthPx)
        drawContent()
    }
}
