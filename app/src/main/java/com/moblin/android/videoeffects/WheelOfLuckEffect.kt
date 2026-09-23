package com.moblin.android.videoeffects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coregraphics.toCGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTILayer
import com.moblin.android.platform.metalpetal.MTIMultilayerCompositingFilter
import com.moblin.android.platform.swiftui.Chart
import com.moblin.android.platform.swiftui.ImageRenderer
import com.moblin.android.platform.swiftui.SwiftUIFonts
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

@Composable
private fun WheelView(size: Double, options: List<WheelOfLuckEffectOption>) {
    val offset = size / 3.4
    val font = size / 10
    Box(
        modifier = Modifier.size(size.dp),
        contentAlignment = Alignment.Center,
    ) {
        Chart(options, modifier = Modifier.size(size.dp)) { option ->
            SectorMark(
                angle = option.weight.toDouble(),
                foregroundStyle = optionColors[option.id % optionColors.size],
            )
        }
        Box(
            modifier = Modifier
                .size((size / 5).dp)
                .background(Color.White, CircleShape),
        )
        options.forEach { option ->
            var scale by remember { mutableStateOf(1f) }
            Text(
                text = option.text,
                modifier = Modifier
                    .width((150 * (size / wheelSize)).dp)
                    .rotate(option.textAngle.toFloat())
                    .offset(x = offset.dp),
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = SwiftUIFonts.system((font * scale).toFloat()),
                onTextLayout = { layoutResult ->
                    if (layoutResult.hasVisualOverflow) {
                        scale = (scale * 0.9f).coerceAtLeast(0.1f)
                    }
                },
            )
        }
    }
}

@Composable
private fun ArrowView(size: Double) {
    SystemImage(
        name = "location.north.fill",
        fontSize = (size / 12).sp,
        modifier = Modifier.rotate(270f),
        tint = Color.White,
    )
}

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
        val position = metalPetalLayerPosition(
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
        val renderer = ImageRenderer(content = { WheelView(size = size, options = options) })
        return renderer.cgImage?.toEffectImage()
    }

    private fun renderArrow(size: Double): EffectImageCgImage? {
        val renderer = ImageRenderer(content = { ArrowView(size = size) })
        return renderer.cgImage?.toEffectImage()
    }
}
