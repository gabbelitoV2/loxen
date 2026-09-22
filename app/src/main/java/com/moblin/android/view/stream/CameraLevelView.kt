package com.moblin.android.view.stream

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.moblin.android.various.model.CameraLevel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CameraLevelView(cameraLevel: CameraLevel) {
    val angle by cameraLevel.angle.collectAsState()
    Canvas(modifier = Modifier) {
        val a = angle ?: return@Canvas
        val y = size.height / 2f
        val xLeft = size.width / 3f
        val xRight = size.width * 2f / 3f
        val halfGap = 2.5f
        val shortLine = 25f
        val leftShortLineBegin = xLeft - shortLine - halfGap
        val rightShortLineEnd = xRight + shortLine + halfGap
        val path = Path()
        val color: Color
        if (abs(a) < 0.01) {
            path.moveTo(leftShortLineBegin, y)
            path.lineTo(rightShortLineEnd, y)
            color = Color.Yellow
        } else {
            path.moveTo(leftShortLineBegin, y)
            path.lineTo(xLeft - halfGap, y)
            path.moveTo(xRight + halfGap, y)
            path.lineTo(rightShortLineEnd, y)
            val longLine = xRight - xLeft - 2f * halfGap
            val xLine = (cos(a) * longLine / 2.0).toFloat()
            val yLine = (sin(a) * longLine / 2.0).toFloat()
            path.moveTo(size.width / 2f - xLine, y - yLine)
            path.lineTo(size.width / 2f + xLine, y + yLine)
            color = Color.White
        }
        drawPath(path = path, color = color, style = Stroke(width = 1f))
    }
}
