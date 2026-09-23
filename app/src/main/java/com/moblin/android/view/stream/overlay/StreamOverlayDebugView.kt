package com.moblin.android.view.stream.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.smallFont
import com.moblin.android.various.model.DebugOverlayProvider

@Composable
fun StreamOverlayDebugView(debugOverlay: DebugOverlayProvider) {
    val debugLines by debugOverlay.debugLines.collectAsState()
    if (debugLines.isNotEmpty()) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0f, 0f, 0f, 0.75f))
                .padding(horizontal = 2.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            debugLines.forEach { line ->
                Text(text = line, style = smallFont, color = Color.White)
            }
        }
    }
}
