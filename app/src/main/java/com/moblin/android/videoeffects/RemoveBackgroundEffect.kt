package com.moblin.android.videoeffects

import com.moblin.android.common.various.RgbColor
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGColorSpaceCreateDeviceRGB
import com.moblin.android.platform.coreimage.CIColorCubeWithColorSpace
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIChromaKeyBlendFilter
import com.moblin.android.platform.metalpetal.MTIColor
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.simd.SIMD2
import com.moblin.android.platform.simd.distance
import com.moblin.android.platform.simd.length
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.moblin.android.common.various.hue

private data class HsvColor(
    val hue: Double,
    val saturation: Double,
    val brightness: Double,
)

private data class FilterSettings(
    val fromHue: Double,
    val toHue: Double,
    val minimumSaturation: Double,
    val minimumBrightness: Double,
)

private val minimumSaturationFloor: Double = 0.15
private val minimumBrightnessFloor: Double = 0.10
private val adaptiveThresholdMultiplier: Double = 0.5
private val hueSectorCount: Double = 6.0
private val greenHueSectorOffset: Double = 2.0
private val blueHueSectorOffset: Double = 4.0

private fun rgbToHsv(red: Float, green: Float, blue: Float): HsvColor {
    val redCG = red.toDouble()
    val greenCG = green.toDouble()
    val blueCG = blue.toDouble()
    val maxColor = max(redCG, max(greenCG, blueCG))
    val minColor = min(redCG, min(greenCG, blueCG))
    val delta = maxColor - minColor
    if (delta <= 0.0) {
        return HsvColor(hue = 0.0, saturation = 0.0, brightness = maxColor)
    }
    val hue: Double
    if (maxColor == redCG) {
        val rawHueSector = (greenCG - blueCG) / delta
        hue = if (rawHueSector < 0) rawHueSector + hueSectorCount else rawHueSector
    } else if (maxColor == greenCG) {
        hue = ((blueCG - redCG) / delta) + greenHueSectorOffset
    } else {
        hue = ((redCG - greenCG) / delta) + blueHueSectorOffset
    }
    return HsvColor(
        hue = hue / hueSectorCount,
        saturation = if (maxColor == 0.0) 0.0 else delta / maxColor,
        brightness = maxColor,
    )
}

private fun isHueInRange(hue: Double, fromHue: Double, toHue: Double): Boolean {
    return if (fromHue <= toHue) {
        hue >= fromHue && hue <= toHue
    } else {
        hue >= fromHue || hue <= toHue
    }
}

private fun makeFilter(settings: FilterSettings): CIColorCubeWithColorSpace {
    val size = 64
    val cube = ArrayList<Float>(size * size * size * 4)
    for (z in 0 until size) {
        val blue = z.toFloat() / (size - 1).toFloat()
        for (y in 0 until size) {
            val green = y.toFloat() / (size - 1).toFloat()
            for (x in 0 until size) {
                val red = x.toFloat() / (size - 1).toFloat()
                cube.add(red)
                cube.add(green)
                cube.add(blue)
                val hsv = rgbToHsv(red = red, green = green, blue = blue)
                val matchesGreenScreen = isHueInRange(
                    hue = hsv.hue,
                    fromHue = settings.fromHue,
                    toHue = settings.toHue,
                ) &&
                    hsv.saturation >= settings.minimumSaturation &&
                    hsv.brightness >= settings.minimumBrightness
                cube.add(if (matchesGreenScreen) 0f else 1f)
            }
        }
    }
    val filter = CIFilter.colorCubeWithColorSpace()
    val bytes = ByteBuffer.allocate(cube.size * 4).order(ByteOrder.nativeOrder())
    for (value in cube) {
        bytes.putFloat(value)
    }
    filter.cubeData = bytes.array()
    filter.cubeDimension = size.toFloat()
    filter.colorSpace = CGColorSpaceCreateDeviceRGB()
    return filter
}

private val chromaKeySmoothing: Float = 0.1f
private val chromaKeyNeutralAxisMargin: Float = 0.8f

private data class ChromaKeySettings(
    val color: MTIColor,
    val thresholdSensitivity: Float,
)

private fun toChroma(color: MTIColor): SIMD2 {
    val luma = 0.2989f * color.red + 0.5866f * color.green + 0.1145f * color.blue
    return SIMD2(0.7132f * (color.red - luma), 0.5647f * (color.blue - luma))
}

private fun makeSaturatedColor(hue: Double): MTIColor {
    val sector = hue.toFloat() * hueSectorCount.toFloat()
    val secondary = 1f - abs(sector % 2f - 1f)
    val (red, green, blue) = when (sector.toInt()) {
        0 -> Triple(1f, secondary, 0f)
        1 -> Triple(secondary, 1f, 0f)
        2 -> Triple(0f, 1f, secondary)
        3 -> Triple(0f, secondary, 1f)
        4 -> Triple(secondary, 0f, 1f)
        else -> Triple(1f, 0f, secondary)
    }
    return MTIColor(red = red, green = green, blue = blue, alpha = 1f)
}

private fun makeChromaKeySettings(from: RgbColor, to: RgbColor): ChromaKeySettings {
    val fromHue = from.hue()
    val toHue = to.hue()
    val hueSpan = if (toHue < fromHue) toHue - fromHue + 1 else toHue - fromHue
    val color = makeSaturatedColor((fromHue + hueSpan / 2) % 1.0)
    val chroma = toChroma(color)
    val thresholdSensitivity = min(
        distance(chroma, toChroma(makeSaturatedColor(hue = fromHue))),
        length(chroma) * chromaKeyNeutralAxisMargin,
    )
    return ChromaKeySettings(color = color, thresholdSensitivity = thresholdSensitivity)
}

class RemoveBackgroundEffect : VideoEffect() {
    private var filter: CIColorCubeWithColorSpace? = null
    private val filterMetalPetal = MTIChromaKeyBlendFilter()
    private var chromaKeySettings: ChromaKeySettings? = null
    private var pendingSettings: FilterSettings? = null
    private var updating = false

    fun setColorRange(from: RgbColor, to: RgbColor) {
        val fromHsv = rgbToHsv(
            red = from.red.toFloat() / 255f,
            green = from.green.toFloat() / 255f,
            blue = from.blue.toFloat() / 255f,
        )
        val toHsv = rgbToHsv(
            red = to.red.toFloat() / 255f,
            green = to.green.toFloat() / 255f,
            blue = to.blue.toFloat() / 255f,
        )
        val minimumSaturation = max(
            minimumSaturationFloor,
            min(fromHsv.saturation, toHsv.saturation) * adaptiveThresholdMultiplier,
        )
        val minimumBrightness = max(
            minimumBrightnessFloor,
            min(fromHsv.brightness, toHsv.brightness) * adaptiveThresholdMultiplier,
        )
        val chromaKeySettings = makeChromaKeySettings(from = from, to = to)
        processorPipelineQueue.launch {
            this@RemoveBackgroundEffect.chromaKeySettings = chromaKeySettings
        }
        pendingSettings = FilterSettings(
            fromHue = from.hue(),
            toHue = to.hue(),
            minimumSaturation = minimumSaturation,
            minimumBrightness = minimumBrightness,
        )
        tryUpdateFilter()
    }

    private fun tryUpdateFilter() {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            if (updating) {
                return@launch
            }
            val settings = pendingSettings ?: return@launch
            pendingSettings = null
            updating = true
            CoroutineScope(Dispatchers.Default).launch {
                val filter = makeFilter(settings)
                processorPipelineQueue.launch {
                    this@RemoveBackgroundEffect.filter = filter
                    CoroutineScope(Dispatchers.Main.immediate).launch {
                        delay(250)
                        updating = false
                        tryUpdateFilter()
                    }
                }
            }
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val filter = this.filter ?: return image
        filter.inputImage = image
        return filter.outputImage ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val chromaKeySettings = this.chromaKeySettings ?: return image
        filterMetalPetal.inputImage = image
        filterMetalPetal.inputBackgroundImage = MTIImage.transparent
        filterMetalPetal.color = chromaKeySettings.color
        filterMetalPetal.thresholdSensitivity = chromaKeySettings.thresholdSensitivity
        filterMetalPetal.smoothing = chromaKeySmoothing
        return filterMetalPetal.outputImage ?: image
    }
}
