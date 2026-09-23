package com.moblin.android.common.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.common.various.smallFont
import com.moblin.android.platform.systemImage

enum class StreamOverlayIconAndTextPlacement {
    BeforeIcon,
    AfterIcon,
    Hide,
}

fun Modifier.streamOverlayContentShape(): Modifier = this
    .layout { measurable, constraints ->
        val padding = 20.dp.roundToPx()
        val placeable = measurable.measure(constraints.offset(2 * padding, 2 * padding))
        layout(placeable.width - 2 * padding, placeable.height - 2 * padding) {
            placeable.place(-padding, -padding)
        }
    }
    .pointerInput(Unit) {}
    .padding(20.dp)

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
        modifier = Modifier.streamOverlayContentShape(),
    ) {
        if (textPlacement == StreamOverlayIconAndTextPlacement.BeforeIcon) {
            CompositionLocalProvider(LocalTextStyle provides smallFont) {
                StreamOverlayTextView(text = text)
            }
        }
        Icon(
            imageVector = systemImage(icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(iconBackgroundColor)
                .padding(horizontal = 2.dp)
                .size(17.dp),
        )
        if (textPlacement == StreamOverlayIconAndTextPlacement.AfterIcon) {
            CompositionLocalProvider(LocalTextStyle provides smallFont) {
                StreamOverlayTextView(text = text)
            }
        }
    }
}
