package com.moblin.android.view.stream

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun StreamGridView(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val height = size.height / 3f
        val width = size.width / 3f
        val path = Path()
        path.moveTo(0f, height)
        path.lineTo(size.width, height)
        path.moveTo(0f, 2f * height)
        path.lineTo(size.width, 2f * height)
        path.moveTo(width, 0f)
        path.lineTo(width, size.height)
        path.moveTo(2f * width, 0f)
        path.lineTo(2f * width, size.height)
        drawPath(path, color = Color.Gray, style = Stroke(width = 1.dp.toPx()))
    }
}
