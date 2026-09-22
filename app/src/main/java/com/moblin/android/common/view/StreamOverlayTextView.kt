package com.moblin.android.common.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.backgroundColor

@Composable
fun StreamOverlayTextView(text: String) {
    Text(
        text = text,
        color = Color.White,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .background(backgroundColor)
            .clip(RoundedCornerShape(5.dp))
    )
}
