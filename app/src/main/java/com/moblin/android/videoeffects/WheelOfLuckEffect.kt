package com.moblin.android.videoeffects

import androidx.compose.ui.graphics.Color
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coregraphics.toCGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTILayer
import com.moblin.android.platform.metalpetal.MTIMultilayerCompositingFilter
import com.moblin.android.platform.swiftui.ImageRenderer
import com.moblin.android.platform.swiftui.layout.Angle
import com.moblin.android.platform.swiftui.layout.Font
import com.moblin.android.platform.swiftui.layout.SwiftUIView
import com.moblin.android.platform.swiftui.layout.View
import com.moblin.android.platform.swiftui.layout.ViewBuilder
import com.moblin.android.platform.swiftui.layout.font
import com.moblin.android.platform.swiftui.layout.foregroundStyle
import com.moblin.android.platform.swiftui.layout.frame
import com.moblin.android.platform.swiftui.layout.lineLimit
import com.moblin.android.platform.swiftui.layout.minimumScaleFactor
import com.moblin.android.platform.swiftui.layout.offset
import com.moblin.android.platform.swiftui.layout.rotationEffect
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetWheelOfLuck
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

private val wheelSize = 400.0

private val optionColors: List<Color> = listOf(
    Color(0xFF007AFF),
    Color(0xFFFF3B30),
    Color(0xFFFFCC00),
    Color(0xFF34C759),
    Color(0xFFFF2D55),
    Color(0xFF32ADE6),
)

data class WheelOfLuckEffectOption(
    val id: Int,
    val weight: Int,
    val text: String,
    val textAngle: Double,
)

internal fun ViewBuilder.WheelView(size: Double, options: List<WheelOfLuckEffectOption>): View {
    val offset = size / 3.4
    val font = size / 10
    return ZStack {
        Chart(options) { option ->
            SectorMark(
                angle = option.weight.toDouble(),
                foregroundStyle = optionColors[option.id % optionColors.size],
            )
        }
        Circle()
            .foregroundStyle(Color.White)
            .frame(width = size / 5, height = size / 5)
        for (option in options) {
            Text(option.text)
                .lineLimit(1)
                .minimumScaleFactor(0.1)
                .font(Font.system(size = font))
                .offset(x = offset)
                .rotationEffect(Angle.degrees(option.textAngle))
                .frame(width = 150 * (size / wheelSize))
        }
    }
        .frame(width = size, height = size)
}

internal fun ViewBuilder.ArrowView(size: Double): View = Image(systemName = "location.north.fill")
    .font(Font.system(size = size / 12))
    .foregroundStyle(Color.White)
    .rotationEffect(Angle.degrees(270.0))

class WheelOfLuckEffect(canvasSize: androidx.compose.ui.geometry.Size) : VideoEffect() {
    private var wheel: EffectImageCgImage? = null
    private var arrow: EffectImageCgImage? = null
    private var startPresentationTimeStamp: Double = Double.POSITIVE_INFINITY
    private var previousPresentationTimeStamp: Double = 0.0
    private var speed: Double = 0.0
    private var angle: Double = 0.0
    private val spinTime: Double = 12.0
    private var sceneWidget: SettingsSceneWidget = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private val canvasSize: CGSize = canvasSize.toCGSize()

    fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        processorPipelineQueue.launch {
            this@WheelOfLuckEffect.sceneWidget = sceneWidget
        }
    }

    fun setSettings(settings: SettingsWidgetWheelOfLuck) {
        val options = mutableListOf<WheelOfLuckEffectOption>()
        val totalWeight = settings.totalWeight.toDouble()
        var angle = 0.0
        for ((index, inputOption) in settings.options.withIndex()) {
            val ratio = inputOption.weight.toDouble() / totalWeight
            val textAngle = angle + ratio * 360 / 2 - 90
            angle += ratio * 360
            options.add(
                WheelOfLuckEffectOption(
                    id = index,
                    weight = inputOption.weight,
                    text = inputOption.text,
                    textAngle = textAngle,
                ),
            )
        }
        val size = wheelSize * (canvasSize.width / 1920)
        render(size = size, options = options)
    }

    fun spin() {
        processorPipelineQueue.launch {
            this@WheelOfLuckEffect.speed = Random.nextDouble(8.0, 15.0)
            this@WheelOfLuckEffect.startPresentationTimeStamp = Double.NaN
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val wheel = this.wheel?.getCiImage() ?: return image
        val arrow = this.arrow?.getCiImage() ?: return image
        updateAngle(info.presentationTimeStamp / 1_000_000.0)
        val size = wheel.extent.width
        return arrow
            .translated(x = size - arrow.extent.width * 0.7, y = size / 2 - arrow.extent.height / 2)
            .composited(
                over = wheel
                    .translated(x = -size / 2, y = -size / 2)
                    .transformed(by = CGAffineTransform(rotationAngle = angle))
                    .translated(x = size / 2, y = size / 2)
                    .cropped(to = CGRect(x = 0.0, y = 0.0, width = size, height = size)),
            )
            .move(layout = sceneWidget.layout, streamSize = image.extent.size)
            .cropped(to = image.extent)
            .composited(over = image)
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val wheel = this.wheel?.getMetalPetalImage() ?: return image
        val arrow = this.arrow?.getMetalPetalImage() ?: return image
        updateAngle(info.presentationTimeStamp / 1_000_000.0)
        val size = wheel.extent.width
        val arrowSize = arrow.extent.size
        val contentSize = CGSize(width = size + 0.3 * arrowSize.width, height = size)
        val position = layoutCenter(
            layout = sceneWidget.layout,
            size = contentSize,
            streamSize = image.extent.size,
        )
        val rotation = (-angle).toFloat()
        val filter = MTIMultilayerCompositingFilter()
        filter.inputBackgroundImage = image
        filter.layers = listOf(
            MTILayer(
                content = wheel,
                position = CGPoint(x = position.x - 0.15 * arrowSize.width, y = position.y),
                rotation = rotation,
            ),
            MTILayer(
                content = arrow,
                position = CGPoint(
                    x = position.x + size / 2 - 0.35 * arrowSize.width,
                    y = position.y,
                ),
            ),
        )
        return filter.outputImage ?: image
    }

    private fun updateAngle(presentationTimeStamp: Double) {
        if (startPresentationTimeStamp.isInfinite()) {
            return
        } else if (startPresentationTimeStamp.isNaN()) {
            startPresentationTimeStamp = presentationTimeStamp
            previousPresentationTimeStamp = presentationTimeStamp
        }
        val elapsedSinceStart = presentationTimeStamp - startPresentationTimeStamp
        val ratio = maxOf(1 - elapsedSinceStart / spinTime, 0.0)
        angle += -speed * ratio * (presentationTimeStamp - previousPresentationTimeStamp)
        previousPresentationTimeStamp = presentationTimeStamp
    }

    private fun render(size: Double, options: List<WheelOfLuckEffectOption>) {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            val wheel = renderWheel(size = size, options = options)
            val arrow = renderArrow(size = size)
            processorPipelineQueue.launch {
                this@WheelOfLuckEffect.wheel = wheel
                this@WheelOfLuckEffect.arrow = arrow
            }
        }
    }

    private fun renderWheel(size: Double, options: List<WheelOfLuckEffectOption>): EffectImageCgImage? {
        val renderer = ImageRenderer(content = { SwiftUIView { WheelView(size = size, options = options) } })
        return renderer.cgImage?.toEffectImage()
    }

    private fun renderArrow(size: Double): EffectImageCgImage? {
        val renderer = ImageRenderer(content = { SwiftUIView { ArrowView(size = size) } })
        return renderer.cgImage?.toEffectImage()
    }
}
