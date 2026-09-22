package com.moblin.android.common.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.common.various.smallFont

enum class StreamOverlayIconAndTextPlacement {
    BeforeIcon,
    AfterIcon,
    Hide,
}

@Composable
fun StreamOverlayIconAndTextView(
    icon: String,
    text: String,
    textPlacement: StreamOverlayIconAndTextPlacement,
    color: Color = Color.White,
    iconBackgroundColor: Color = backgroundColor,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(20.dp)
            .then(
                TODO(
                    "contentShape(Rectangle()) plus negative padding hit-area expansion has no Compose counterpart",
                ),
            ),
    ) {
        if (textPlacement == StreamOverlayIconAndTextPlacement.BeforeIcon) {
            StreamOverlayTextView(text = text, style = smallFont)
        }
        Icon(
            painter = TODO("no Android counterpart for SF Symbols icon lookup: $icon"),
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(width = 17.dp, height = 17.dp)
                .padding(horizontal = 2.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(iconBackgroundColor),
        )
        if (textPlacement == StreamOverlayIconAndTextPlacement.AfterIcon) {
            StreamOverlayTextView(text = text, style = smallFont)
        }
    }
}
