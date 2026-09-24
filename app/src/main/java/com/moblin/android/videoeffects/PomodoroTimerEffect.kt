package com.moblin.android.videoeffects

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.combine.AnyCancellable
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coregraphics.toCGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.swiftui.ImageRenderer
import com.moblin.android.platform.swiftui.layout.Alignment
import com.moblin.android.platform.swiftui.layout.EdgeSet
import com.moblin.android.platform.swiftui.layout.Font
import com.moblin.android.platform.swiftui.layout.HorizontalAlignment
import com.moblin.android.platform.swiftui.layout.SwiftUIView
import com.moblin.android.platform.swiftui.layout.View
import com.moblin.android.platform.swiftui.layout.ViewBuilder
import com.moblin.android.platform.swiftui.layout.background
import com.moblin.android.platform.swiftui.layout.clipShape
import com.moblin.android.platform.swiftui.layout.font
import com.moblin.android.platform.swiftui.layout.foregroundStyle
import com.moblin.android.platform.swiftui.layout.frame
import com.moblin.android.platform.swiftui.layout.lineLimit
import com.moblin.android.platform.swiftui.layout.padding
import com.moblin.android.various.settings.PomodoroPhase
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetPomodoroTimer
import com.moblin.android.view.utils.FontDesign
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlinx.coroutines.launch

internal fun ViewBuilder.PomodoroTimerView(
    settings: SettingsWidgetPomodoroTimer,
    sceneWidget: SettingsSceneWidget,
    canvasSize: CGSize,
): View {
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
    val timeString = String.format(Locale.US, "%02d:%02d", minutes, seconds)
    val foregroundColor = settings.foregroundColorColor
    return VStack(alignment = HorizontalAlignment.leading, spacing = spacing) {
        HStack(spacing = spacing) {
            Image(systemName = phaseIcon)
                .font(Font.system(size = phaseSize, weight = FontWeight.SemiBold))
                .foregroundStyle(phaseColor)
            Text(phaseName)
                .lineLimit(1)
                .font(Font.system(size = phaseSize, weight = FontWeight.SemiBold))
                .foregroundStyle(phaseColor)
            Spacer(minLength = 0.0)
            Text(timeString)
                .lineLimit(1)
                .font(Font.system(size = timerSize, weight = FontWeight.Bold, design = FontDesign.Monospaced))
                .foregroundStyle(foregroundColor)
        }
        ZStack(alignment = Alignment.leading) {
            RoundedRectangle(cornerRadius = barCornerRadius)
                .fill(foregroundColor.copy(alpha = foregroundColor.alpha * 0.2f))
                .frame(width = width - padding * 2, height = barHeight)
            RoundedRectangle(cornerRadius = barCornerRadius)
                .fill(phaseColor)
                .frame(width = max(0.0, (width - padding * 2) * progress), height = barHeight)
        }
            .frame(width = width - padding * 2, height = barHeight)
    }
        .padding(EdgeSet.top, padding / 2)
        .padding(EdgeSet.horizontal + EdgeSet.bottom, padding)
        .frame(width = width)
        .background(settings.backgroundColorColor)
        .clipShape(RoundedRectangle(cornerRadius = cornerRadius))
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
            SwiftUIView {
                PomodoroTimerView(settings = settings, sceneWidget = sceneWidget, canvasSize = canvasSize)
            }
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
