package com.moblin.android.view.stream.overlay.right

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.various.model.MediaPlayerPlayer
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.mediaPlayerNext
import com.moblin.android.various.model.mediaPlayerPrevious
import com.moblin.android.various.model.mediaPlayerSeek
import com.moblin.android.various.model.mediaPlayerSetSeeking
import com.moblin.android.various.model.mediaPlayerTogglePlaying
import com.moblin.android.LocalModel

private fun playPauseImage(playing: Boolean): String {
    return if (playing) {
        "pause"
    } else {
        "play"
    }
}

@Composable
fun StreamOverlayRightMediaPlayerControlsView(model: Model = LocalModel.current, mediaPlayer: MediaPlayerPlayer) {
    val playing by mediaPlayer.playing.collectAsState()
    val fileName by mediaPlayer.fileName.collectAsState()
    val time by mediaPlayer.time.collectAsState()
    val position by mediaPlayer.position.collectAsState()
    val seeking by mediaPlayer.seeking.collectAsState()
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = fileName,
            color = Color.White,
            modifier = Modifier.padding(end = 8.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
        ) {
            Text(
                text = time,
                color = Color.White
            )
            if (false) {
                LaunchedEffect(position) {
                    if (seeking) {
                        model.mediaPlayerSeek(position = model.mediaPlayerPlayer.position.value.toDouble())
                    }
                }
                Slider(
                    value = position.toFloat(),
                    onValueChange = { value ->
                        model.mediaPlayerSetSeeking(on = true)
                        model.mediaPlayerSeek(position = value.toDouble())
                    },
                    onValueChangeFinished = {
                        model.mediaPlayerSetSeeking(on = false)
                        model.mediaPlayerSeek(position = position.toDouble())
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = Color.White),
                    modifier = Modifier.width(250.dp)
                )
            }
            IconButton(
                onClick = {
                    model.mediaPlayerPrevious()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.width(30.dp)
                )
            }
            IconButton(
                onClick = {
                    model.mediaPlayerTogglePlaying()
                }
            ) {
                Icon(
                    imageVector = if (playPauseImage(playing) == "pause") {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.width(30.dp)
                )
            }
            IconButton(
                onClick = {
                    model.mediaPlayerNext()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.width(30.dp)
                )
            }
        }
    }
}
