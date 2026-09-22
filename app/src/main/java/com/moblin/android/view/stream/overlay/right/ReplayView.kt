package com.moblin.android.view.stream.overlay.right

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Orientation
import com.moblin.android.various.model.ReplayProvider
import com.moblin.android.various.settings.SettingsReplay
import com.moblin.android.various.settings.SettingsReplaySpeed
import com.moblin.android.various.storages.ReplaySettings
import com.moblin.android.various.storages.ReplaysDatabase
import com.moblin.android.various.utils.createThumbnail
import com.moblin.android.LocalModel

@Composable
private fun ReplayPreview(
    model: Model = LocalModel.current,
    orientation: Orientation,
    replay: ReplayProvider,
) {
    val isPortrait by orientation.isPortrait.collectAsState()
    val isPlaying by replay.isPlaying.collectAsState()
    val previewImage by replay.previewImage.collectAsState()
    val image = previewImage
    val maxWidth = if (isPortrait) 200.dp else 300.dp
    if (!isPlaying && image != null) {
        Box {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .clip(RoundedCornerShape(7.dp))
                    .clickable {
                        replay.previewImage.value = null
                    },
            )
            Button(
                onClick = {
                    model.deleteSelectedReplay()
                    replay.selectedId.value = null
                    replay.previewImage.value = null
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier
                        .size(30.dp)
                        .border(1.dp, Color.Gray, CircleShape)
                        .padding(7.dp),
                )
            }
        }
    }
}

@Composable
private fun ReplayControlsInterval(
    model: Model = LocalModel.current,
    replay: ReplayProvider,
) {
    val startFromEnd by replay.startFromEnd.collectAsState()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = startFromEnd,
            onValueChange = {
                replay.startFromEnd.value = it
                model.setReplayPosition(SettingsReplay.stop - it)
            },
            valueRange = 0f..SettingsReplay.stop,
            steps = ((SettingsReplay.stop - 0f) / 0.1f).toInt() - 1,
            modifier = Modifier
                .width(250.dp)
                .rotate(180f),
        )
        Text(
            text = "${startFromEnd.toInt()}s",
            modifier = Modifier.width(35.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
        )
    }
}

@Composable
private fun ReplayControlsSpeedPicker(
    model: Model = LocalModel.current,
    replay: ReplayProvider,
) {
    val speed by replay.speed.collectAsState()
    SegmentedHPicker(
        items = SettingsReplaySpeed.entries,
        selectedItem = speed,
        onSelectedItemChange = {
            replay.speed.value = it
            model.replaySpeedChanged()
        },
        modifier = Modifier
            .width(90.dp)
            .clip(RoundedCornerShape(7.dp))
            .border(1.dp, pickerBorderColor, RoundedCornerShape(7.dp)),
    ) { item ->
        Text(
            text = item.rawValue,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.height(35.dp),
        )
    }
}

@Composable
private fun ReplayControlsPlayPauseButton(
    model: Model = LocalModel.current,
    replay: ReplayProvider,
) {
    val isPlaying by replay.isPlaying.collectAsState()
    val selectedId by replay.selectedId.collectAsState()
    Button(
        onClick = {
            val newIsPlaying = !isPlaying
            replay.isPlaying.value = newIsPlaying
            if (newIsPlaying) {
                if (!model.replayPlay()) {
                    replay.isPlaying.value = false
                }
            } else {
                model.replayCancel()
            }
        },
        enabled = selectedId != null,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "stop" else "play",
            tint = if (selectedId != null) Color.White else LocalContentColor.current,
            modifier = Modifier
                .size(30.dp)
                .padding(7.dp),
        )
    }
}

@Composable
private fun ReplayControlsSaveButton(
    model: Model = LocalModel.current,
    replay: ReplayProvider,
) {
    val isSaving by replay.isSaving.collectAsState()
    if (isSaving) {
        CircularProgressIndicator(
            modifier = Modifier.size(30.dp),
            color = Color.White,
        )
    } else {
        Button(
            onClick = {
                if (model.stream.replay.enabled) {
                    model.saveReplay()
                } else {
                    model.makeReplayIsNotEnabledToast()
                }
            },
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun ControlRowView(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleLarge) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .padding(end = 4.dp)
                .height(45.dp)
                .background(backgroundColor)
                .clip(RoundedCornerShape(5.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

@Composable
private fun ReplayControls(
    model: Model = LocalModel.current,
    replay: ReplayProvider,
    orientation: Orientation,
) {
    val isPortrait by orientation.isPortrait.collectAsState()
    if (isPortrait) {
        Column(horizontalAlignment = Alignment.End) {
            ControlRowView {
                ReplayControlsInterval(model = model, replay = replay)
            }
            ControlRowView {
                ReplayControlsSpeedPicker(model = model, replay = replay)
                ReplayControlsPlayPauseButton(model = model, replay = replay)
                VerticalDivider(color = Color.White)
                ReplayControlsSaveButton(model = model, replay = replay)
            }
        }
    } else {
        ControlRowView {
            ReplayControlsInterval(model = model, replay = replay)
            ReplayControlsSpeedPicker(model = model, replay = replay)
            ReplayControlsPlayPauseButton(model = model, replay = replay)
            VerticalDivider(color = Color.White)
            ReplayControlsSaveButton(model = model, replay = replay)
        }
    }
}

@Composable
private fun ReplayHistoryItem(
    model: Model = LocalModel.current,
    orientation: Orientation,
    replay: ReplayProvider,
    video: ReplaySettings,
) {
    val isPortrait by orientation.isPortrait.collectAsState()
    val selectedId by replay.selectedId.collectAsState()
    var image by remember { mutableStateOf<Bitmap?>(null) }
    var presentingMenu by remember { mutableStateOf(false) }
    val heightDp = if (isPortrait) 118.dp else 68.dp
    Column {
        val currentImage = image
        if (currentImage != null) {
            Image(
                bitmap = currentImage.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(heightDp)
                    .clip(RoundedCornerShape(5.dp))
                    .clickable {
                        model.loadReplay(video)
                    }
                    .then(
                        if (video.id == selectedId) {
                            Modifier.border(2.dp, Color.White, RoundedCornerShape(5.dp))
                        } else {
                            Modifier
                        },
                    ),
            )
        } else {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
            )
        }
    }
    LaunchedEffect(Unit) {
        createThumbnail(video.url(), video.thumbnailOffset()) { thumbnail ->
            image = thumbnail
        }
    }
}

@Composable
private fun ReplayHistory(
    model: Model = LocalModel.current,
    orientation: Orientation,
    replayDatabase: ReplaysDatabase,
    replay: ReplayProvider,
) {
    val isPortrait by orientation.isPortrait.collectAsState()
    val replays by replayDatabase.replays.collectAsState()
    val heightDp = if (isPortrait) 120.dp else 70.dp
    LazyRow(
        modifier = Modifier
            .padding(4.dp)
            .background(backgroundColor)
            .clip(RoundedCornerShape(5.dp))
            .height(heightDp),
    ) {
        if (replays.isEmpty()) {
            item {
                Text(
                    text = "No replays saved",
                    modifier = Modifier.padding(start = 30.dp),
                    color = Color.White,
                )
            }
        }
        items(replays, key = { it.id }) { video ->
            ReplayHistoryItem(
                model = model,
                orientation = orientation,
                replay = replay,
                video = video,
            )
        }
    }
}

@Composable
fun StreamOverlayRightReplayView(
    model: Model = LocalModel.current,
    replay: ReplayProvider,
    orientation: Orientation,
) {
    Column(horizontalAlignment = Alignment.End) {
        ReplayPreview(model = model, orientation = orientation, replay = replay)
        ReplayControls(model = model, replay = replay, orientation = orientation)
        ReplayHistory(
            model = model,
            orientation = orientation,
            replayDatabase = model.replaysStorage.database,
            replay = replay,
        )
    }
}
