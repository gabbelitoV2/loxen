package com.moblin.android.common.various

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

val clippingThresholdDb: Float = -0.2f
val redThresholdDb: Float = -8.5f
val yellowThresholdDb: Float = -20.0f
val zeroThresholdDb: Float = -60.0f
val defaultAudioLevel: Float = -160.0f

@Composable
fun CompactAudioLevelIconView(
    name: String,
    foregroundColor: Color,
    backgroundColor: Color,
) {
    Icon(
        imageVector = compactAudioLevelIcon(name),
        contentDescription = null,
        tint = foregroundColor,
        modifier = Modifier
            .size(17.dp)
            .padding(horizontal = 2.dp)
            .padding(bottom = 2.dp)
            .background(backgroundColor)
            .clip(RoundedCornerShape(5.dp)),
    )
}

fun compactAudioLevelColors(level: Float): Pair<Color, Color> = when {
    level > clippingThresholdDb -> Color.White to Color.Red
    level > redThresholdDb -> Color.Red to backgroundColor
    level > yellowThresholdDb -> Color.Yellow to backgroundColor
    level > zeroThresholdDb -> Color.Green to backgroundColor
    else -> Color.White to backgroundColor
}

private fun compactAudioLevelIcon(name: String): ImageVector = when (name) {
    "speaker.wave.1", "speaker.wave.1.fill" -> Icons.Filled.VolumeDown
    "speaker.wave.2", "speaker.wave.2.fill" -> Icons.Filled.VolumeUp
    "speaker.wave.3", "speaker.wave.3.fill" -> Icons.Filled.VolumeUp
    "speaker.slash", "speaker.slash.fill" -> Icons.Filled.VolumeOff
    "speaker.zzz", "speaker.zzz.fill" -> Icons.Filled.VolumeMute
    "mic.slash", "mic.slash.fill" -> Icons.Filled.VolumeOff
    else -> Icons.Filled.VolumeUp
}
