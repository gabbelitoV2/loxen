package com.moblin.android.view.stream.overlay.right

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.CompactAudioLevelIconView
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.common.various.clippingThresholdDb
import com.moblin.android.common.various.compactAudioLevelColors
import com.moblin.android.common.various.formatAudioLevelChannels
import com.moblin.android.common.various.formatAudioLevelSampleRate
import com.moblin.android.common.various.redThresholdDb
import com.moblin.android.common.various.smallFont
import com.moblin.android.common.various.yellowThresholdDb
import com.moblin.android.common.various.zeroThresholdDb
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.various.model.AudioLevel
import com.moblin.android.various.model.AudioProvider
import com.moblin.android.various.model.Model

private val barWidthPerDb: Float = 1.0f
private val barHeight: Dp = 5.dp
private val bigBarScale: Float = 2.5f

private fun clippingBar(level: Float, scale: Float): Float? {
    if (level <= clippingThresholdDb) {
        return null
    }
    val db = -zeroThresholdDb
    return (db * barWidthPerDb) * scale
}

private fun redBar(level: Float, scale: Float): Float? {
    if (level <= redThresholdDb) {
        return null
    }
    val db = level - redThresholdDb
    return (db * barWidthPerDb) * scale
}

private fun yellowBar(level: Float, scale: Float): Float? {
    if (level <= yellowThresholdDb) {
        return null
    }
    val db = minOf(level - yellowThresholdDb, redThresholdDb - yellowThresholdDb)
    return (db * barWidthPerDb) * scale
}

private fun greenBar(level: Float, scale: Float): Float? {
    if (level <= zeroThresholdDb) {
        return null
    }
    val db = minOf(level - zeroThresholdDb, yellowThresholdDb - zeroThresholdDb)
    return (db * barWidthPerDb) * scale
}

@Composable
private fun AudioBarView(audio: AudioProvider, level: AudioLevel, big: Boolean = false) {
    val muted by audio.muted.collectAsState()
    val levelValue by level.level.collectAsState()
    val scale = if (big) bigBarScale else 1.0f
    if (muted) {
        Text(
            text = localized("Muted"),
            color = Color.White,
            style = smallFont,
        )
    } else {
        Row(
            modifier = Modifier.padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val clippingWidth = clippingBar(levelValue, scale)
            if (clippingWidth != null) {
                Box(
                    modifier = Modifier
                        .width(clippingWidth.dp)
                        .height(barHeight * scale)
                        .background(Color.Red),
                )
            } else {
                val redWidth = redBar(levelValue, scale)
                if (redWidth != null) {
                    Box(
                        modifier = Modifier
                            .width(redWidth.dp)
                            .height(barHeight * scale)
                            .background(Color.Red),
                    )
                }
                val yellowWidth = yellowBar(levelValue, scale)
                if (yellowWidth != null) {
                    Box(
                        modifier = Modifier
                            .width(yellowWidth.dp)
                            .height(barHeight * scale)
                            .background(Color.Yellow),
                    )
                }
                val greenWidth = greenBar(levelValue, scale)
                if (greenWidth != null) {
                    Box(
                        modifier = Modifier
                            .width(greenWidth.dp)
                            .height(barHeight * scale)
                            .background(Color.Green),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelsView(audio: AudioProvider) {
    val numberOfChannels by audio.numberOfChannels.collectAsState()
    if (numberOfChannels != 1) {
        Text(
            text = formatAudioLevelChannels(numberOfChannels),
            color = Color.White,
            style = smallFont,
        )
    }
}

@Composable
private fun SampleRateView(audio: AudioProvider) {
    val sampleRate by audio.sampleRate.collectAsState()
    if (sampleRate != 48000.0) {
        Text(
            text = formatAudioLevelSampleRate(sampleRate),
            color = Color.White,
            style = smallFont,
        )
    }
}

@Composable
fun AudioLevelView(model: Model = LocalModel.current, big: Boolean = false) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(backgroundColor)
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AudioBarView(audio = model.audio, level = model.audio.level, big = big)
            ChannelsView(audio = model.audio)
            SampleRateView(audio = model.audio)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(backgroundColor)
                .padding(horizontal = 2.dp)
                .size(17.dp),
            contentAlignment = Alignment.Center,
        ) {
            SystemImage(name = "waveform", fontSize = smallFont.fontSize, tint = Color.White)
        }
    }
}

@Composable
fun CompactAudioBarView(audio: AudioProvider, level: AudioLevel) {
    val muted by audio.muted.collectAsState()
    val levelValue by level.level.collectAsState()
    if (muted) {
        CompactAudioLevelIconView(
            name = "microphone.slash",
            foregroundColor = Color.White,
            backgroundColor = backgroundColor,
        )
    } else {
        val (foregroundColor, backgroundColor) = compactAudioLevelColors(levelValue)
        CompactAudioLevelIconView(
            name = "waveform",
            foregroundColor = foregroundColor,
            backgroundColor = backgroundColor,
        )
    }
}
