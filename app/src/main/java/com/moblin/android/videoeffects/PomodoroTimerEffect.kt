package com.moblin.android.videoeffects

import android.graphics.Bitmap
import android.media.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.settings.PomodoroBreakIcon
import com.moblin.android.various.settings.PomodoroFocusIcon
import com.moblin.android.various.settings.PomodoroPhase
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetPomodoroTimer
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private fun width(size: Double, canvasSize: Size): Double {
    return toPixels(size, minOf(canvasSize.width, canvasSize.height).toDouble())
}

private fun progress(secondsRemaining: Int, total: Int): Double {
    if (total <= 0) {
        return 1.0
    }
    return secondsRemaining.toDouble() / total.toDouble()
}

private fun timeString(secondsRemaining: Int): String {
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun phaseColor(phase: PomodoroPhase, focusColor: Color, breakColor: Color): Color {
    return if (phase == PomodoroPhase.focus) focusColor else breakColor
}

private fun phaseIcon(phase: PomodoroPhase,
                      focusIcon: PomodoroFocusIcon,
                      breakIcon: PomodoroBreakIcon): ImageVector {
    return pomodoroIconVector(if (phase == PomodoroPhase.focus) focusIcon.rawValue else breakIcon.rawValue)
}

private fun pomodoroIconVector(rawValue: String): ImageVector {
    return when (rawValue) {
        "cup.and.saucer.fill", "cup.and.saucer", "mug.fill", "moon.fill", "moon.stars.fill" -> Icons.Default.Refresh
        "timer", "timer.circle.fill", "hourglass", "brain.head.profile" -> Icons.Default.PlayArrow
        else -> Icons.Default.Star
    }
}

private fun phaseName(phase: PomodoroPhase, focusName: String, breakName: String): String {
    return if (phase == PomodoroPhase.focus) focusName else breakName
}

@Composable
private fun PomodoroTimerView(settings: SettingsWidgetPomodoroTimer,
                              sceneWidget: SettingsSceneWidget,
                              canvasSize: Size) {
    val phase = settings.phase
    val secondsRemaining = settings.secondsRemaining
    val foregroundColor = settings.foregroundColorColor
    val backgroundColor = settings.backgroundColorColor
    val focusName = settings.focusName
    val breakName = settings.breakName
    val focusIcon = settings.focusIcon
    val breakIcon = settings.breakIcon
    val focusColor = settings.focusColorColor
    val breakColor = settings.breakColorColor
    val settingsWidth = settings.width
    val layout = sceneWidget.layout

    val effectWidth = width(layout.size, canvasSize)
    val padding = effectWidth * 0.06
    val cornerRadius = effectWidth * 0.08
    val barHeight = effectWidth * 0.07
    val barCornerRadius = barHeight / 2
    val phaseSize = effectWidth * 0.15
    val timerSize = effectWidth * 0.18
    val spacing = effectWidth * 0.04
    val widgetWidth = settingsWidth * effectWidth
    val barWidth = widgetWidth - padding * 2
    val progressValue = progress(secondsRemaining, settings.totalSecondsForCurrentPhase())
    val phaseColorValue = phaseColor(phase, focusColor, breakColor)

    Column(
        modifier = Modifier
            .padding(
                top = (padding / 2).toFloat().dp,
                start = padding.toFloat().dp,
                end = padding.toFloat().dp,
                bottom = padding.toFloat().dp,
            )
            .width(widgetWidth.toFloat().dp)
            .clip(RoundedCornerShape(cornerRadius.toFloat().dp))
            .background(backgroundColor),
        verticalArrangement = Arrangement.spacedBy(spacing.toFloat().dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.toFloat().dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = phaseIcon(phase, focusIcon, breakIcon),
                contentDescription = null,
                modifier = Modifier.size(phaseSize.toFloat().dp),
                tint = phaseColorValue,
            )
            Text(
                text = phaseName(phase, focusName, breakName),
                maxLines = 1,
                fontSize = phaseSize.toFloat().sp,
                fontWeight = FontWeight.SemiBold,
                color = phaseColorValue,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = timeString(secondsRemaining),
                maxLines = 1,
                fontSize = timerSize.toFloat().sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = foregroundColor,
            )
        }
        Box(
            modifier = Modifier
                .width(barWidth.toFloat().dp)
                .height(barHeight.toFloat().dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .width(barWidth.toFloat().dp)
                    .height(barHeight.toFloat().dp)
                    .clip(RoundedCornerShape(barCornerRadius.toFloat().dp))
                    .background(foregroundColor.copy(alpha = 0.2f)),
            )
            Box(
                modifier = Modifier
                    .width(maxOf(0.0, barWidth * progressValue).toFloat().dp)
                    .height(barHeight.toFloat().dp)
                    .clip(RoundedCornerShape(barCornerRadius.toFloat().dp))
                    .background(phaseColorValue),
            )
        }
    }
}

class PomodoroTimerEffect(private val canvasSize: Size) : VideoEffect() {
    private var settings = SettingsWidgetPomodoroTimer()
    private var sceneWidget = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private var sceneWidgetPipeline = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private var renderer: Any? = null
    private var cancellable: Job? = null
    private var timerImage: EffectImageCgImage? = null

    fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        processorPipelineQueue.launch {
            sceneWidgetPipeline.layout = sceneWidget.layout
        }
        this.sceneWidget.layout = sceneWidget.layout
    }

    fun setSettings(settings: SettingsWidgetPomodoroTimer) {
        if (settings === this.settings) {
            return
        }
        this.settings = settings
        setup()
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        if (timerImage == null) {
            return image
        }
        return TODO("no Android counterpart for CIImage move/cropped/composited")
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        if (timerImage == null) {
            return image
        }
        return TODO("no Android counterpart for MTIImage moveComposited")
    }

    private fun setup() {
        cancellable?.cancel()
        renderer = TODO("no Android counterpart for SwiftUI ImageRenderer")
        setTimerImage(null)
    }

    private fun setTimerImage(image: Bitmap?) {
        val timerImage = if (image == null) {
            null
        } else {
            TODO("no Android counterpart for CGImage.toEffectImage()")
        }
        processorPipelineQueue.launch {
            this@PomodoroTimerEffect.timerImage = timerImage
        }
    }
}
