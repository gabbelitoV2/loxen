package com.moblin.android.videoeffects

import android.media.Image
import com.moblin.android.common.various.RgbColor
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

private val backgroundScope = CoroutineScope(Dispatchers.Default)

private data class HsvColor(
    val hue: Float,
    val saturation: Float,
    val brightness: Float,
)

private data class FilterSettings(
    val fromHue: Double,
    val toHue: Double,
    val minimumSaturation: Double,
    val minimumBrightness: Double,
)

private const val minimumSaturationFloor: Double = 0.15
private const val minimumBrightnessFloor: Double = 0.10
private const val adaptiveThresholdMultiplier: Double = 0.5
private const val hueSectorCount: Float = 6f
private const val greenHueSectorOffset: Float = 2f
private const val blueHueSectorOffset: Float = 4f

private fun rgbToHsv(red: Float, green: Float, blue: Float): HsvColor {
    val redCG = red
    val greenCG = green
    val blueCG = blue
    val maxColor = max(redCG, max(greenCG, blueCG))
    val minColor = min(redCG, min(greenCG, blueCG))
    val delta = maxColor - minColor
    if (delta <= 0f) {
        return HsvColor(hue = 0f, saturation = 0f, brightness = maxColor)
    }
    val hue: Float
    if (maxColor == redCG) {
        val rawHueSector = (greenCG - blueCG) / delta
        hue = if (rawHueSector < 0f) rawHueSector + hueSectorCount else rawHueSector
    } else if (maxColor == greenCG) {
        hue = ((blueCG - redCG) / delta) + greenHueSectorOffset
    } else {
        hue = ((redCG - greenCG) / delta) + blueHueSectorOffset
    }
    return HsvColor(
        hue = hue / hueSectorCount,
        saturation = if (maxColor == 0f) 0f else delta / maxColor,
        brightness = maxColor,
    )
}

private fun isHueInRange(hue: Float, fromHue: Float, toHue: Float): Boolean {
    return if (fromHue <= toHue) {
        hue >= fromHue && hue <= toHue
    } else {
        hue >= fromHue || hue <= toHue
    }
}

private fun makeFilter(settings: FilterSettings): FloatArray {
    val size = 64
    val denominator = (size - 1).toFloat()
    val cube = ArrayList<Float>(size * size * size * 4)
    val fromHue = settings.fromHue.toFloat()
    val toHue = settings.toHue.toFloat()
    val minimumSaturation = settings.minimumSaturation.toFloat()
    val minimumBrightness = settings.minimumBrightness.toFloat()
    for (z in 0 until size) {
        val blue = z.toFloat() / denominator
        for (y in 0 until size) {
            val green = y.toFloat() / denominator
            for (x in 0 until size) {
                val red = x.toFloat() / denominator
                cube.add(red)
                cube.add(green)
                cube.add(blue)
                val hsv = rgbToHsv(red = red, green = green, blue = blue)
                val matchesGreenScreen = isHueInRange(
                    hue = hsv.hue,
                    fromHue = fromHue,
                    toHue = toHue,
                ) &&
                    hsv.saturation >= minimumSaturation &&
                    hsv.brightness >= minimumBrightness
                cube.add(if (matchesGreenScreen) 0f else 1f)
            }
        }
    }
    return cube.toFloatArray()
}

private const val chromaKeySmoothing: Float = 0.1f
private const val chromaKeyNeutralAxisMargin: Float = 0.8f

private data class MtiColor(
    val red: Float,
    val green: Float,
    val blue: Float,
    val alpha: Float,
)

private data class ChromaKeySettings(
    val color: MtiColor,
    val thresholdSensitivity: Float,
)

private fun toChroma(color: MtiColor): FloatArray {
    val luma = 0.2989f * color.red + 0.5866f * color.green + 0.1145f * color.blue
    return floatArrayOf(
        0.7132f * (color.red - luma),
        0.5647f * (color.blue - luma),
    )
}

private fun makeSaturatedColor(hue: Double): MtiColor {
    val sector = hue.toFloat() * hueSectorCount
    val secondary = 1f - abs(sector % 2f - 1f)
    val rgb: Triple<Float, Float, Float> = when (sector.toInt()) {
        0 -> Triple(1f, secondary, 0f)
        1 -> Triple(secondary, 1f, 0f)
        2 -> Triple(0f, 1f, secondary)
        3 -> Triple(0f, secondary, 1f)
        4 -> Triple(secondary, 0f, 1f)
        else -> Triple(1f, 0f, secondary)
    }
    val (red, green, blue) = rgb
    return MtiColor(red = red, green = green, blue = blue, alpha = 1f)
}

private fun RgbColor.hue(): Double = rgbToHsv(
    red = red.toFloat() / 255f,
    green = green.toFloat() / 255f,
    blue = blue.toFloat() / 255f,
).hue.toDouble()

private fun makeChromaKeySettings(from: RgbColor, to: RgbColor): ChromaKeySettings {
    val fromHue = from.hue()
    val toHue = to.hue()
    val hueSpan = if (toHue < fromHue) toHue - fromHue + 1 else toHue - fromHue
    val color = makeSaturatedColor((fromHue + hueSpan / 2) % 1.0)
    val chroma = toChroma(color)
    val fromChroma = toChroma(makeSaturatedColor(fromHue))
    val thresholdSensitivity = min(
        hypot(chroma[0] - fromChroma[0], chroma[1] - fromChroma[1]),
        hypot(chroma[0], chroma[1]) * chromaKeyNeutralAxisMargin,
    )
    return ChromaKeySettings(color = color, thresholdSensitivity = thresholdSensitivity)
}

class RemoveBackgroundEffect : VideoEffect() {
    private var filter: FloatArray? = null
    private val filterMetalPetal: Any? = null
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
            min(fromHsv.saturation, toHsv.saturation).toDouble() * adaptiveThresholdMultiplier,
        )
        val minimumBrightness = max(
            minimumBrightnessFloor,
            min(fromHsv.brightness, toHsv.brightness).toDouble() * adaptiveThresholdMultiplier,
        )
        val newChromaKeySettings = makeChromaKeySettings(from = from, to = to)
        processorPipelineQueue.launch {
            this@RemoveBackgroundEffect.chromaKeySettings = newChromaKeySettings
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
        mainScope.launch {
            if (updating || pendingSettings == null) {
                return@launch
            }
            val settings = pendingSettings ?: return@launch
            pendingSettings = null
            updating = true
            backgroundScope.launch {
                val newFilter = makeFilter(settings)
                processorPipelineQueue.launch {
                    this@RemoveBackgroundEffect.filter = newFilter
                    mainScope.launch {
                        delay(250)
                        this@RemoveBackgroundEffect.updating = false
                        tryUpdateFilter()
                    }
                }
            }
        }
    }

    override fun execute(image: Image, videoEffectInfo: VideoEffectInfo): Image =
        TODO("no Android counterpart for CoreImage CIColorCubeWithColorSpace")

    override fun executeMetalPetal(image: Image, videoEffectInfo: VideoEffectInfo): Image =
        TODO("no Android counterpart for MetalPetal MTIChromaKeyBlendFilter")
}
