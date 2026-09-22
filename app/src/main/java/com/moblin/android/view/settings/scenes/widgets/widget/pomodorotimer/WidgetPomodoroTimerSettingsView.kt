package com.moblin.android.view.settings.scenes.widgets.widget.pomodorotimer

import android.media.MediaPlayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.PomodoroBreakIcon
import com.moblin.android.various.settings.PomodoroFocusIcon
import com.moblin.android.various.settings.PomodoroPhase
import com.moblin.android.various.settings.SettingsWidgetPomodoroTimer
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.view.utils.TextEditNavigationView
import java.util.UUID
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private fun getPomodoroSoundName(model: Model, soundId: UUID?): String {
    return model.getAllAlertSounds().firstOrNull { it.id == soundId }?.name ?: localized("-- None --")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSoundSelectorView(
    model: Model = LocalModel.current,
    soundId: UUID?,
    onChange: (UUID?) -> Unit,
) {
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.release()
            previewPlayer = null
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Sound") })
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(model.getAllAlertSounds()) { sound ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChange(sound.id) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = sound.id == soundId,
                        onClick = { onChange(sound.id) },
                    )
                    Text(sound.name)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        val url = model.getAlertSoundUrl(soundId = sound.id) ?: return@IconButton
                        previewPlayer?.release()
                        previewPlayer = runCatching {
                            MediaPlayer().apply {
                                setDataSource(url)
                                prepare()
                                start()
                            }
                        }.getOrNull()
                    }) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetPomodoroTimerQuickButtonControlsView(pomodoroTimer: SettingsWidgetPomodoroTimer) {
    val isRunning by pomodoroTimer.isRunning.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { pomodoroTimer.advancePhase() }) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
            )
        }
        IconButton(onClick = { pomodoroTimer.reset() }) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
            )
        }
        IconButton(onClick = {
            if (isRunning) {
                pomodoroTimer.pause()
            } else {
                pomodoroTimer.start()
            }
        }) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier
                    .size(30.dp)
                    .width(35.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPomodoroTimerSettingsView(
    model: Model = LocalModel.current,
    pomodoroTimer: SettingsWidgetPomodoroTimer,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val focusDuration by pomodoroTimer.focusDuration.collectAsState()
    val breakDuration by pomodoroTimer.breakDuration.collectAsState()
    val focusName by pomodoroTimer.focusName.collectAsState()
    val breakName by pomodoroTimer.breakName.collectAsState()
    val focusIcon by pomodoroTimer.focusIcon.collectAsState()
    val breakIcon by pomodoroTimer.breakIcon.collectAsState()
    val width by pomodoroTimer.width.collectAsState()
    val focusToBreakSoundId by pomodoroTimer.focusToBreakSoundId.collectAsState()
    val breakToFocusSoundId by pomodoroTimer.breakToFocusSoundId.collectAsState()
    val focusToBreakChatMessage by pomodoroTimer.focusToBreakChatMessage.collectAsState()
    val breakToFocusChatMessage by pomodoroTimer.breakToFocusChatMessage.collectAsState()

    var focusDurationExpanded by remember { mutableStateOf(false) }
    var breakDurationExpanded by remember { mutableStateOf(false) }
    var focusIconExpanded by remember { mutableStateOf(false) }
    var breakIconExpanded by remember { mutableStateOf(false) }

    val focusDurationValues = listOf(1, 2, 3, 5, 10, 15, 20, 25, 30, 45, 60, 90, 120)
    val breakDurationValues = listOf(1, 2, 3, 5, 7, 10, 15, 20, 25, 30)

    LaunchedEffect(focusDuration) {
        if (pomodoroTimer.phase.value == PomodoroPhase.Focus && !pomodoroTimer.isRunning.value) {
            pomodoroTimer.setSecondsRemaining(focusDuration * 60)
        }
    }
    LaunchedEffect(breakDuration) {
        if (pomodoroTimer.phase.value == PomodoroPhase.ShortBreak && !pomodoroTimer.isRunning.value) {
            pomodoroTimer.setSecondsRemaining(breakDuration * 60)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Controls",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        WidgetPomodoroTimerQuickButtonControlsView(pomodoroTimer = pomodoroTimer)

        Text(
            text = "Durations",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        ExposedDropdownMenuBox(
            expanded = focusDurationExpanded,
            onExpandedChange = { focusDurationExpanded = it },
        ) {
            OutlinedTextField(
                value = "$focusDuration min",
                onValueChange = {},
                readOnly = true,
                label = { Text("Focus") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = focusDurationExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .menuAnchor(),
            )
            DropdownMenu(
                expanded = focusDurationExpanded,
                onDismissRequest = { focusDurationExpanded = false },
            ) {
                for (value in focusDurationValues) {
                    DropdownMenuItem(
                        text = { Text("$value min") },
                        onClick = {
                            pomodoroTimer.setFocusDuration(value)
                            focusDurationExpanded = false
                        },
                    )
                }
            }
        }
        ExposedDropdownMenuBox(
            expanded = breakDurationExpanded,
            onExpandedChange = { breakDurationExpanded = it },
        ) {
            OutlinedTextField(
                value = "$breakDuration min",
                onValueChange = {},
                readOnly = true,
                label = { Text("Break") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = breakDurationExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .menuAnchor(),
            )
            DropdownMenu(
                expanded = breakDurationExpanded,
                onDismissRequest = { breakDurationExpanded = false },
            ) {
                for (value in breakDurationValues) {
                    DropdownMenuItem(
                        text = { Text("$value min") },
                        onClick = {
                            pomodoroTimer.setBreakDuration(value)
                            breakDurationExpanded = false
                        },
                    )
                }
            }
        }

        Text(
            text = "Names",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        TextEditNavigationView(
            title = localized("Focus"),
            value = focusName,
            onSubmit = { pomodoroTimer.setFocusName(it) },
        )
        TextEditNavigationView(
            title = localized("Break"),
            value = breakName,
            onSubmit = { pomodoroTimer.setBreakName(it) },
        )

        Text(
            text = "Icons",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        ExposedDropdownMenuBox(
            expanded = focusIconExpanded,
            onExpandedChange = { focusIconExpanded = it },
        ) {
            OutlinedTextField(
                value = focusIcon.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Focus") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = focusIconExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .menuAnchor(),
            )
            DropdownMenu(
                expanded = focusIconExpanded,
                onDismissRequest = { focusIconExpanded = false },
            ) {
                for (icon in PomodoroFocusIcon.entries) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = TODO("no Android counterpart for SF Symbol ${icon.rawValue}"),
                                    contentDescription = null,
                                )
                                Text(icon.toString())
                            }
                        },
                        onClick = {
                            pomodoroTimer.setFocusIcon(icon)
                            focusIconExpanded = false
                        },
                    )
                }
            }
        }
        ExposedDropdownMenuBox(
            expanded = breakIconExpanded,
            onExpandedChange = { breakIconExpanded = it },
        ) {
            OutlinedTextField(
                value = breakIcon.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Break") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = breakIconExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .menuAnchor(),
            )
            DropdownMenu(
                expanded = breakIconExpanded,
                onDismissRequest = { breakIconExpanded = false },
            ) {
                for (icon in PomodoroBreakIcon.entries) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = TODO("no Android counterpart for SF Symbol ${icon.rawValue}"),
                                    contentDescription = null,
                                )
                                Text(icon.toString())
                            }
                        },
                        onClick = {
                            pomodoroTimer.setBreakIcon(icon)
                            breakIconExpanded = false
                        },
                    )
                }
            }
        }

        Text(
            text = "Width",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Width")
            Slider(
                value = width,
                onValueChange = { pomodoroTimer.setWidth(it) },
                valueRange = 1f..5f,
                steps = 79,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = "Colors",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        RgbColorPickerView(
            title = "Background",
            color = pomodoroTimer.backgroundColorColor.collectAsState().value,
            opacity = true,
        ) {
            pomodoroTimer.setBackgroundColor(it)
        }
        RgbColorPickerView(
            title = "Text",
            color = pomodoroTimer.foregroundColorColor.collectAsState().value,
        ) {
            pomodoroTimer.setForegroundColor(it)
        }
        RgbColorPickerView(
            title = "Focus",
            color = pomodoroTimer.focusColorColor.collectAsState().value,
        ) {
            pomodoroTimer.setFocusColor(it)
        }
        RgbColorPickerView(
            title = "Break",
            color = pomodoroTimer.breakColorColor.collectAsState().value,
        ) {
            pomodoroTimer.setBreakColor(it)
        }

        Text(
            text = "Sounds",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("pomodoroSoundSelectorFocusToBreak") }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Focus to break")
            Spacer(modifier = Modifier.weight(1f))
            GrayTextView(
                text = getPomodoroSoundName(model = model, soundId = focusToBreakSoundId),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("pomodoroSoundSelectorBreakToFocus") }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Break to focus")
            Spacer(modifier = Modifier.weight(1f))
            GrayTextView(
                text = getPomodoroSoundName(model = model, soundId = breakToFocusSoundId),
            )
        }

        Text(
            text = "Chat messages",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        TextEditNavigationView(
            title = localized("Focus to break"),
            value = focusToBreakChatMessage,
            onSubmit = { pomodoroTimer.setFocusToBreakChatMessage(it) },
        )
        TextEditNavigationView(
            title = localized("Break to focus"),
            value = breakToFocusChatMessage,
            onSubmit = { pomodoroTimer.setBreakToFocusChatMessage(it) },
        )
    }
}
