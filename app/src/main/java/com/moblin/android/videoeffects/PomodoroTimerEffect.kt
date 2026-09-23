package com.moblin.android.videoeffects

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.combine.AnyCancellable
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coregraphics.toCGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.swiftui.ImageRenderer
import com.moblin.android.platform.swiftui.SwiftUIFonts
import com.moblin.android.various.settings.PomodoroPhase
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetPomodoroTimer
import com.moblin.android.view.utils.FontDesign
import java.util.UUID
import kotlin.math.max
import kotlinx.coroutines.launch

@Composable
private fun PomodoroTimerView(
    settings: SettingsWidgetPomodoroTimer,
    sceneWidget: SettingsSceneWidget,
    canvasSize: CGSize,
) {
    val baseWidth = toPixels(sceneWidget.layout.size, canvasSize.minimum())
    val padding = baseWidth * 0.06
    val cornerRadius = baseWidth * 0.08
    val barHeight = baseWidth * 0.07
    val barCornerRadius = barHeight / 2
    val phaseSize = baseWidth * 0.15
    val timerSize = baseWidth * 0.18
    val spacing = baseWidth * 0.04
    val width = settings.width * baseWidth
    val phaseColor = when (settings.phase) {
        PomodoroPhase.focus -> settings.focusColorColor
        PomodoroPhase.shortBreak -> settings.breakColorColor
    }
    val phaseIcon = when (settings.phase) {
        PomodoroPhase.focus -> settings.focusIcon.rawValue
        PomodoroPhase.shortBreak -> settings.breakIcon.rawValue
    }
    val phaseName = when (settings.phase) {
        PomodoroPhase.focus -> settings.focusName
        PomodoroPhase.shortBreak -> settings.breakName
    }
    val total = settings.totalSecondsForCurrentPhase()
    val progress = if (total > 0) {
        settings.secondsRemaining.toDouble() / total.toDouble()
    } else {
        1.0
    }
    val minutes = settings.secondsRemaining / 60
    val seconds = settings.secondsRemaining % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(settings.backgroundColorColor)
            .width(width.dp)
            .padding(start = padding.dp, end = padding.dp)
            .padding(bottom = padding.dp)
            .padding(top = (padding / 2).dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        ) {
            SystemImage(name = phaseIcon, fontSize = phaseSize.sp, tint = phaseColor)
            Text(
                text = phaseName,
                style = SwiftUIFonts.system(phaseSize, FontWeight.SemiBold),
                color = phaseColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = timeString,
                style = SwiftUIFonts.system(timerSize, FontWeight.Bold, FontDesign.Monospaced),
                color = settings.foregroundColorColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .width((width - padding * 2).dp)
                .height(barHeight.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(settings.foregroundColorColor.copy(alpha = 0.2f), RoundedCornerShape(barCornerRadius.dp)),
            )
            Box(
                modifier = Modifier
                    .width(max(0.0, (width - padding * 2) * progress).dp)
                    .height(barHeight.dp)
                    .background(phaseColor, RoundedCornerShape(barCornerRadius.dp)),
            )
        }
    }
}

class PomodoroTimerEffect(canvasSize: Size) : VideoEffect() {
    private val canvasSize: CGSize = canvasSize.toCGSize()
    private var settings = SettingsWidgetPomodoroTimer()
    private var sceneWidget = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private var sceneWidgetPipeline = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private var renderer: ImageRenderer? = null
    private var cancellable: AnyCancellable? = null
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

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        return timerImage?.getCiImage()
            ?.move(sceneWidgetPipeline.layout, image.extent.size)
            ?.cropped(to = image.extent)
            ?.composited(over = image)
            ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        return timerImage?.getMetalPetalImage()?.moveComposited(sceneWidgetPipeline.layout, image) ?: image
    }

    private fun setup() {
        cancellable?.cancel()
        val settings = this.settings
        val sceneWidget = this.sceneWidget
        val canvasSize = this.canvasSize
        renderer = ImageRenderer(content = {
            PomodoroTimerView(settings = settings, sceneWidget = sceneWidget, canvasSize = canvasSize)
        })
        val weakSelf = java.lang.ref.WeakReference(this)
        cancellable = renderer?.objectWillChange?.sink {
            weakSelf.get()?.let { self -> self.setTimerImage(image = self.renderer?.cgImage) }
        }
        setTimerImage(image = renderer?.cgImage)
    }

    private fun setTimerImage(image: Bitmap?) {
        val timerImage = image?.toEffectImage()
        processorPipelineQueue.launch {
            this@PomodoroTimerEffect.timerImage = timerImage
        }
    }
}
