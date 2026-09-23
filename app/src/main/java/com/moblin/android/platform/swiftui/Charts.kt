package com.moblin.android.platform.swiftui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.min

class ChartContentScope internal constructor() {
    internal class Sector(val angle: Double, val color: Color)

    internal val sectors = mutableListOf<Sector>()

    fun SectorMark(angle: Double, foregroundStyle: Color) {
        sectors.add(Sector(angle, foregroundStyle))
    }
}

@Composable
fun <T> Chart(data: List<T>, modifier: Modifier = Modifier, content: ChartContentScope.(T) -> Unit) {
    val scope = ChartContentScope()
    for (item in data) {
        scope.content(item)
    }
    val sectors = scope.sectors.filter { it.angle > 0 && it.angle.isFinite() }
    val total = sectors.sumOf { it.angle }
    Canvas(modifier.fillMaxSize()) {
        if (total <= 0) {
            return@Canvas
        }
        val diameter = min(size.width, size.height)
        if (diameter <= 0f) {
            return@Canvas
        }
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)
        var start = -90.0
        for (sector in sectors) {
            val sweep = sector.angle / total * 360.0
            drawArc(
                color = sector.color,
                startAngle = start.toFloat(),
                sweepAngle = sweep.toFloat(),
                useCenter = true,
                topLeft = topLeft,
                size = arcSize,
            )
            start += sweep
        }
    }
}
