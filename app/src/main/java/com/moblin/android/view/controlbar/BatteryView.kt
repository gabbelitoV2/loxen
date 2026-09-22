package com.moblin.android.view.controlbar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.various.model.Battery
import com.moblin.android.various.model.Model

private fun percentage(level: Double): String {
    return (level * 100).toInt().toString()
}

private fun boltColor(model: Model): Color {
    return if (model.isBatteryCharging()) {
        Color.White
    } else {
        Color.Transparent
    }
}

@Composable
fun BatteryView(model: Model, battery: Battery) {
    val level by battery.level.collectAsState()
    Row(
        modifier = Modifier.padding(top = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = null,
            tint = boltColor(model),
            modifier = Modifier.size(10.dp)
        )
        Row(
            modifier = Modifier.size(width = 28.dp, height = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(width = 24.dp, height = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White, RoundedCornerShape(2.dp))
                )
                Text(
                    text = percentage(level),
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Canvas(modifier = Modifier.size(width = 4.dp, height = 8.dp)) {
                val strokeWidth = size.width / 2f
                drawArc(
                    color = Color.Gray,
                    startAngle = -90f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}
