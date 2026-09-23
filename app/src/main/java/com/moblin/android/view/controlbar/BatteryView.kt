package com.moblin.android.view.controlbar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.platform.SystemImage
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
fun BatteryView(model: Model = LocalModel.current, battery: Battery) {
    val level by battery.level.collectAsState()
    val state by battery.state.collectAsState()
    val bolt = remember(state) { boltColor(model) }
    Row(
        modifier = Modifier.padding(top = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SystemImage(name = "bolt.fill", fontSize = 10.sp, tint = bolt)
        Row(
            modifier = Modifier.size(width = 28.dp, height = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = 12.dp)
                    .background(Color.White, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = percentage(level),
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.wrapContentSize(unbounded = true),
                )
            }
            Canvas(modifier = Modifier.size(width = 4.dp, height = 13.dp)) {
                val diameter = size.width
                drawArc(
                    color = Color(0xFF8E8E93),
                    startAngle = -90f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(0f, (size.height - diameter) / 2f),
                    size = Size(diameter, diameter),
                )
            }
        }
    }
}
