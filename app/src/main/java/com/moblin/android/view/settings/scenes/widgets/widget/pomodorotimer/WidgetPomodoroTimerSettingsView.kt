package com.moblin.android.view.settings.scenes.widgets.widget.pomodorotimer

import android.media.MediaPlayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.PomodoroBreakIcon
import com.moblin.android.various.settings.PomodoroFocusIcon
import com.moblin.android.various.settings.PomodoroPhase
import com.moblin.android.various.settings.SettingsWidgetPomodoroTimer
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.view.utils.TextEditNavigationView
import java.util.UUID
import kotlin.math.roundToInt

private fun getPomodoroSoundName(model: Model, soundId: UUID?): String {
    return model.getAllAlertSounds().firstOrNull { it.id == soundId }?.name ?: localized("-- None --")
}

@Composable
fun PomodoroSoundSelectorView(
    model: Model = LocalModel.current,
    soundId: UUID?,
    onChange: (UUID?) -> Unit,
) {
    val palette = formPalette()
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.release()
            previewPlayer = null
        }
    }
    Form(title = localized("Sound")) {
        Section {
            for (sound in model.getAllAlertSounds()) {
                FormRow(onClick = { onChange(sound.id) }) {
                    Text(sound.name)
                    Spacer(modifier = Modifier.weight(1f))
                    val interactionSource = remember { MutableInteractionSource() }
                    val pressed by interactionSource.collectIsPressedAsState()
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .alpha(if (pressed) 0.2f else 1f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {
                                val url = model.getAlertSoundUrl(soundId = sound.id)
                                    ?: return@clickable
                                previewPlayer?.release()
                                previewPlayer = runCatching {
                                    MediaPlayer().apply {
                                        setDataSource(url)
                                        prepare()
                                        start()
                                    }
                                }.getOrNull()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        SystemImage(
                            name = "play.fill",
                            fontSize = 17.sp,
                            tint = palette.accent,
                        )
                    }
                    if (sound.id == soundId) {
                        SystemImage(
                            name = "checkmark",
                            fontSize = 17.sp,
                            modifier = Modifier.padding(start = 8.dp),
                            tint = palette.accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PomodoroIconButton(
    systemImage: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = formPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .alpha(if (pressed) 0.2f else 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        SystemImage(
            name = systemImage,
            fontSize = 28.sp,
            tint = palette.accent,
        )
    }
}

@Composable
fun WidgetPomodoroTimerQuickButtonControlsView(pomodoroTimer: SettingsWidgetPomodoroTimer) {
    val isRunning = pomodoroTimer.isRunning
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        PomodoroIconButton(systemImage = "forward.end") {
            pomodoroTimer.advancePhase()
        }
        PomodoroIconButton(systemImage = "arrow.counterclockwise") {
            pomodoroTimer.reset()
        }
        PomodoroIconButton(
            systemImage = if (isRunning) "stop" else "play",
            modifier = Modifier.width(35.dp),
        ) {
            if (isRunning) {
                pomodoroTimer.pause()
            } else {
                pomodoroTimer.start()
            }
        }
    }
}

@Composable
fun WidgetPomodoroTimerSettingsView(
    model: Model = LocalModel.current,
    pomodoroTimer: SettingsWidgetPomodoroTimer,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val focusDurationValues = listOf(1, 2, 3, 5, 10, 15, 20, 25, 30, 45, 60, 90, 120)
    val breakDurationValues = listOf(1, 2, 3, 5, 7, 10, 15, 20, 25, 30)

    Section(header = localized("Controls")) {
        WidgetPomodoroTimerQuickButtonControlsView(pomodoroTimer = pomodoroTimer)
    }
    Section(header = localized("Durations")) {
        Picker(
            title = localized("Focus"),
            selection = pomodoroTimer.focusDuration,
            options = focusDurationValues,
            text = { value -> "$value min" },
            onChange = { value ->
                pomodoroTimer.focusDuration = value
                if (pomodoroTimer.phase == PomodoroPhase.focus && !pomodoroTimer.isRunning) {
                    pomodoroTimer.secondsRemaining = value * 60
                }
            },
        )
        Picker(
            title = localized("Break"),
            selection = pomodoroTimer.breakDuration,
            options = breakDurationValues,
            text = { value -> "$value min" },
            onChange = { value ->
                pomodoroTimer.breakDuration = value
                if (pomodoroTimer.phase == PomodoroPhase.shortBreak && !pomodoroTimer.isRunning) {
                    pomodoroTimer.secondsRemaining = value * 60
                }
            },
        )
    }
    Section(header = localized("Names")) {
        TextEditNavigationView(
            title = localized("Focus"),
            value = pomodoroTimer.focusName,
            onSubmit = { pomodoroTimer.focusName = it },
        )
        TextEditNavigationView(
            title = localized("Break"),
            value = pomodoroTimer.breakName,
            onSubmit = { pomodoroTimer.breakName = it },
        )
    }
    Section(header = localized("Icons")) {
        Picker(
            title = localized("Focus"),
            selection = pomodoroTimer.focusIcon,
            options = PomodoroFocusIcon.entries,
            onChange = { pomodoroTimer.focusIcon = it },
        )
        Picker(
            title = localized("Break"),
            selection = pomodoroTimer.breakIcon,
            options = PomodoroBreakIcon.entries,
            onChange = { pomodoroTimer.breakIcon = it },
        )
    }
    Section {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(localized("Width"))
            FormSlider(
                value = pomodoroTimer.width.toFloat(),
                onValueChange = {
                    pomodoroTimer.width = ((it / 0.05f).roundToInt() * 0.05f).toDouble()
                },
                valueRange = 1f..5f,
                modifier = Modifier.weight(1f),
            )
        }
    }
    Section(header = localized("Colors")) {
        RgbColorPickerView(
            title = "Background",
            color = pomodoroTimer.backgroundColorColor,
            onColorChanged = { pomodoroTimer.backgroundColorColor = it },
            opacity = true,
        ) {
            pomodoroTimer.backgroundColor = it
        }
        RgbColorPickerView(
            title = "Text",
            color = pomodoroTimer.foregroundColorColor,
            onColorChanged = { pomodoroTimer.foregroundColorColor = it },
        ) {
            pomodoroTimer.foregroundColor = it
        }
        RgbColorPickerView(
            title = "Focus",
            color = pomodoroTimer.focusColorColor,
            onColorChanged = { pomodoroTimer.focusColorColor = it },
        ) {
            pomodoroTimer.focusColor = it
        }
        RgbColorPickerView(
            title = "Break",
            color = pomodoroTimer.breakColorColor,
            onColorChanged = { pomodoroTimer.breakColorColor = it },
        ) {
            pomodoroTimer.breakColor = it
        }
    }
    Section(header = localized("Sounds")) {
        NavigationLink(
            destination = {
                PomodoroSoundSelectorView(
                    model = model,
                    soundId = pomodoroTimer.focusToBreakSoundId,
                    onChange = { pomodoroTimer.focusToBreakSoundId = it },
                )
            },
        ) {
            Text(localized("Focus to break"))
            Spacer(modifier = Modifier.weight(1f))
            GrayTextView(
                text = getPomodoroSoundName(
                    model = model,
                    soundId = pomodoroTimer.focusToBreakSoundId,
                ),
            )
        }
        NavigationLink(
            destination = {
                PomodoroSoundSelectorView(
                    model = model,
                    soundId = pomodoroTimer.breakToFocusSoundId,
                    onChange = { pomodoroTimer.breakToFocusSoundId = it },
                )
            },
        ) {
            Text(localized("Break to focus"))
            Spacer(modifier = Modifier.weight(1f))
            GrayTextView(
                text = getPomodoroSoundName(
                    model = model,
                    soundId = pomodoroTimer.breakToFocusSoundId,
                ),
            )
        }
    }
    Section(header = localized("Chat messages")) {
        TextEditNavigationView(
            title = localized("Focus to break"),
            value = pomodoroTimer.focusToBreakChatMessage,
            onSubmit = { pomodoroTimer.focusToBreakChatMessage = it },
        )
        TextEditNavigationView(
            title = localized("Break to focus"),
            value = pomodoroTimer.breakToFocusChatMessage,
            onSubmit = { pomodoroTimer.breakToFocusChatMessage = it },
        )
    }
}
