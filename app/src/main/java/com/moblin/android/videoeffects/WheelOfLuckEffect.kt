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
import com.moblin.android.platform.swiftui.layout.TextAlignment
import com.moblin.android.platform.swiftui.layout.View
import com.moblin.android.platform.swiftui.layout.ViewBuilder
import com.moblin.android.platform.swiftui.layout.background
import com.moblin.android.platform.swiftui.layout.bold
import com.moblin.android.platform.swiftui.layout.font
import com.moblin.android.platform.swiftui.layout.foregroundStyle
import com.moblin.android.platform.swiftui.layout.frame
import com.moblin.android.platform.swiftui.layout.lineLimit
import com.moblin.android.platform.swiftui.layout.minimumScaleFactor
import com.moblin.android.platform.swiftui.layout.multilineTextAlignment
import com.moblin.android.platform.swiftui.layout.offset
import com.moblin.android.platform.swiftui.layout.padding
import com.moblin.android.platform.swiftui.layout.rotationEffect
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetWheelOfLuck
import java.time.Instant
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

data class WheelOfLuckSpinResult(
    val id: UUID = UUID.randomUUID(),
    val date: Instant = Instant.now(),
    val text: String,
    val name: String,
    val message: String,
)

private data class PendingSpin(
    val caption: EffectImageCgImage?,
    val name: String,
    val message: String,
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

private fun ViewBuilder.CaptionView(size: Double, name: String, message: String): View = VStack(spacing = size / 80) {
    if (name.isNotEmpty()) {
        Text(name)
            .bold()
            .lineLimit(1)
            .font(Font.system(size = size / 12))
    }
    if (message.isNotEmpty()) {
        Text(message)
            .lineLimit(2)
            .multilineTextAlignment(TextAlignment.center)
            .font(Font.system(size = size / 16))
    }
}
    .minimumScaleFactor(0.1)
    .foregroundStyle(Color.White)
    .padding(size / 40)
    .frame(width = size)
    .background(RoundedRectangle(cornerRadius = size / 20).foregroundStyle(Color.Black.copy(alpha = 0.6f)))

class WheelOfLuckEffect(
    canvasSize: androidx.compose.ui.geometry.Size,
    private val onResult: (WheelOfLuckSpinResult) -> Unit,
) : VideoEffect() {
    private var wheel: EffectImageCgImage? = null
    private var arrow: EffectImageCgImage? = null
    private var caption: EffectImageCgImage? = null
    private var pendingSpins: MutableList<PendingSpin> = mutableListOf()
    private var currentSpin: PendingSpin? = null
    private var options: List<WheelOfLuckEffectOption> = emptyList()
    private var startPresentationTimeStamp: Double = Double.POSITIVE_INFINITY
    private var previousPresentationTimeStamp: Double = 0.0
    private var speed: Double = 0.0
    private var angle: Double = 0.0
    private val spinTime: Double = 12.0
    private val pauseBetweenSpins: Double = 3.0
    private val showBeforeSpin: Double = 1.5
    private var onlyShowWhenSpinning = false
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
        val onlyShowWhenSpinning = settings.onlyShowWhenSpinning
        processorPipelineQueue.launch {
            this@WheelOfLuckEffect.onlyShowWhenSpinning = onlyShowWhenSpinning
        }
        render(size = getSize(), options = options)
    }

    fun spin() {
        processorPipelineQueue.launch {
            this@WheelOfLuckEffect.pendingSpins.add(PendingSpin(caption = null, name = "", message = ""))
        }
    }

    fun spin(name: String, message: String, count: Int) {
        val size = getSize()
        CoroutineScope(Dispatchers.Main).launch {
            val caption = this@WheelOfLuckEffect.renderCaption(size = size, name = name, message = message)
            processorPipelineQueue.launch {
                this@WheelOfLuckEffect.pendingSpins += List(count) {
                    PendingSpin(caption = caption, name = name, message = message)
                }
            }
        }
    }

    private fun getSize(): Double = wheelSize * (canvasSize.width / 1920)

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val wheel = this.wheel?.getCiImage() ?: return image
        val arrow = this.arrow?.getCiImage() ?: return image
        updateAngle(info.presentationTimeStamp / 1_000_000.0)
        val size = wheel.extent.width
        var output = arrow
            .translated(x = size - arrow.extent.width * 0.7, y = size / 2 - arrow.extent.height / 2)
            .composited(
                over = wheel
                    .translated(x = -size / 2, y = -size / 2)
                    .transformed(by = CGAffineTransform(rotationAngle = angle))
                    .translated(x = size / 2, y = size / 2)
                    .cropped(to = CGRect(x = 0.0, y = 0.0, width = size, height = size)),
            )
            .move(layout = sceneWidget.layout, streamSize = image.extent.size)
        val caption = this.caption?.getCiImage()
        if (caption != null) {
            output = caption
                .translated(
                    x = output.extent.minX + size / 2 - caption.extent.width / 2,
                    y = output.extent.minY - caption.extent.height - size / 40,
                )
                .composited(over = output)
        }
        return output
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
        val wheelCenterX = position.x - 0.15 * arrowSize.width
        filter.layers = listOf(
            MTILayer(
                content = wheel,
                position = CGPoint(x = wheelCenterX, y = position.y),
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
        val caption = this.caption?.getMetalPetalImage()
        if (caption != null) {
            filter.layers = filter.layers + MTILayer(
                content = caption,
                position = CGPoint(
                    x = wheelCenterX,
                    y = position.y + size / 2 + size / 40 + caption.extent.height / 2,
                ),
            )
        }
        return filter.outputImage ?: image
    }

    override fun isEnabled(): Boolean =
        !onlyShowWhenSpinning || !startPresentationTimeStamp.isInfinite() || pendingSpins.isNotEmpty()

    private fun updateAngle(presentationTimeStamp: Double) {
        if (!startPresentationTimeStamp.isInfinite()) {
            val elapsedSinceStart = presentationTimeStamp - startPresentationTimeStamp
            if (elapsedSinceStart > 0) {
                val ratio = maxOf(1 - elapsedSinceStart / spinTime, 0.0)
                val previous = maxOf(previousPresentationTimeStamp, startPresentationTimeStamp)
                angle += -speed * ratio * (presentationTimeStamp - previous)
            }
            previousPresentationTimeStamp = presentationTimeStamp
            val currentSpin = this.currentSpin
            if (elapsedSinceStart >= spinTime && currentSpin != null) {
                reportResult(spin = currentSpin)
                this.currentSpin = null
            }
            if (elapsedSinceStart >= spinTime + 5) {
                caption = null
                startPresentationTimeStamp = Double.POSITIVE_INFINITY
            }
        }
        if (pendingSpins.isNotEmpty() &&
            (
                startPresentationTimeStamp.isInfinite() ||
                    presentationTimeStamp - startPresentationTimeStamp >= spinTime + pauseBetweenSpins
                )
        ) {
            val delay = if (onlyShowWhenSpinning && startPresentationTimeStamp.isInfinite()) {
                showBeforeSpin
            } else {
                0.0
            }
            val spin = pendingSpins.removeAt(0)
            currentSpin = spin
            caption = spin.caption
            speed = Random.nextDouble(8.0, 15.0)
            startPresentationTimeStamp = presentationTimeStamp + delay
            previousPresentationTimeStamp = presentationTimeStamp
        }
    }

    private fun reportResult(spin: PendingSpin) {
        val totalWeight = options.fold(0) { acc, option -> acc + option.weight }.toDouble()
        if (totalWeight <= 0.0) {
            return
        }
        var arrowAngle = (90 + angle * 180 / Math.PI) % 360
        if (arrowAngle < 0) {
            arrowAngle += 360
        }
        var endAngle = 0.0
        var text = options.lastOrNull()?.text ?: ""
        for (option in options) {
            endAngle += 360 * option.weight.toDouble() / totalWeight
            if (arrowAngle < endAngle) {
                text = option.text
                break
            }
        }
        val result = WheelOfLuckSpinResult(text = text, name = spin.name, message = spin.message)
        CoroutineScope(Dispatchers.Main).launch {
            this@WheelOfLuckEffect.onResult(result)
        }
    }

    private fun render(size: Double, options: List<WheelOfLuckEffectOption>) {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            val wheel = renderWheel(size = size, options = options)
            val arrow = renderArrow(size = size)
            processorPipelineQueue.launch {
                this@WheelOfLuckEffect.wheel = wheel
                this@WheelOfLuckEffect.arrow = arrow
                this@WheelOfLuckEffect.options = options
            }
        }
    }

    private fun renderWheel(size: Double, options: List<WheelOfLuckEffectOption>): EffectImageCgImage? {
        val renderer = ImageRenderer(content = { SwiftUIView { WheelView(size = size, options = options) } })
        return renderer.cgImage?.toEffectImage()
    }

    private fun renderCaption(size: Double, name: String, message: String): EffectImageCgImage? {
        if (name.isEmpty() && message.isEmpty()) {
            return null
        }
        val renderer = ImageRenderer(content = { SwiftUIView { CaptionView(size = size, name = name, message = message) } })
        return renderer.cgImage?.toEffectImage()
    }

    private fun renderArrow(size: Double): EffectImageCgImage? {
        val renderer = ImageRenderer(content = { SwiftUIView { ArrowView(size = size) } })
        return renderer.cgImage?.toEffectImage()
    }
}
