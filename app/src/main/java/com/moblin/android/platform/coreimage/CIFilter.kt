package com.moblin.android.platform.coreimage

import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coreimage.internal.BlendWithMaskCombine
import com.moblin.android.platform.coreimage.internal.BlurNode
import com.moblin.android.platform.coreimage.internal.CombineNode
import com.moblin.android.platform.coreimage.internal.CompositeNode
import com.moblin.android.platform.coreimage.internal.ConstantNode
import com.moblin.android.platform.coreimage.internal.GeneratorNode
import com.moblin.android.platform.coreimage.internal.RadialGradientGenerator
import com.moblin.android.platform.coreimage.internal.RoundedRectangleGenerator
import com.moblin.android.platform.coreimage.internal.SourceEncoding

const val kCIInputImageKey = "inputImage"
const val kCIInputBackgroundImageKey = "inputBackgroundImage"
const val kCIInputMaskImageKey = "inputMaskImage"
const val kCIInputTargetImageKey = "inputTargetImage"
const val kCIInputRadiusKey = "inputRadius"
const val kCIInputExtentKey = "inputExtent"
const val kCIInputColorKey = "inputColor"
const val kCIInputCenterKey = "inputCenter"
const val kCIInputScaleKey = "inputScale"
const val kCIInputAngleKey = "inputAngle"
const val kCIInputIntensityKey = "inputIntensity"
const val kCIInputTimeKey = "inputTime"
const val kCIInputWidthKey = "inputWidth"
const val kCIInputSharpnessKey = "inputSharpness"
const val kCIInputSaturationKey = "inputSaturation"
const val kCIInputBrightnessKey = "inputBrightness"
const val kCIInputContrastKey = "inputContrast"
const val kCIOutputImageKey = "outputImage"

internal fun anyToFloat(value: Any?, fallback: Float): Float {
    return (value as? Number)?.toFloat() ?: fallback
}

internal fun anyToCGPoint(value: Any?, fallback: CGPoint): CGPoint {
    return when (value) {
        is CGPoint -> value
        is CIVector -> CGPoint(value.x, value.y)
        else -> fallback
    }
}

internal fun anyToCGRect(value: Any?, fallback: CGRect): CGRect {
    return when (value) {
        is CGRect -> value
        is CIVector -> CGRect(value.x, value.y, value.z, value.w)
        else -> fallback
    }
}

open class CIFilter {
    private val values = HashMap<String, Any?>()

    open val name: String
        get() = javaClass.simpleName

    open val outputImage: CIImage?
        get() = null

    open val inputKeys: List<String>
        get() = values.keys.toList()

    open fun setValue(value: Any?, forKey: String) {
        values[forKey] = value
    }

    open fun value(forKey: String): Any? {
        if (forKey == kCIOutputImageKey) {
            return outputImage
        }
        return values[forKey]
    }

    open fun setDefaults() {}

    companion object {
        operator fun invoke(name: String): CIFilter? {
            return when (name) {
                "CIGaussianBlur" -> gaussianBlur()
                "CISourceOverCompositing" -> sourceOverCompositing()
                "CIBlendWithMask" -> blendWithMask()
                "CIRoundedRectangleGenerator" -> roundedRectangleGenerator()
                "CIRadialGradient" -> radialGradient()
                "CIConstantColorGenerator" -> CIConstantColorGenerator()
                "CIQRCodeGenerator" -> qrCodeGenerator()
                "CIColorMonochrome" -> colorMonochrome()
                "CIColorMatrix" -> colorMatrix()
                "CISepiaTone" -> sepiaTone()
                "CIColorControls" -> colorControls()
                "CIVignette" -> vignette()
                "CIPixellate" -> pixellate()
                "CIPinchDistortion" -> pinchDistortion()
                "CITwirlDistortion" -> twirlDistortion()
                "CIStripesGenerator" -> stripesGenerator()
                "CICheckerboardGenerator" -> checkerboardGenerator()
                "CIStretchCrop" -> stretchCrop()
                "CIDissolveTransition" -> dissolveTransition()
                "CIColorCubeWithColorSpace" -> colorCubeWithColorSpace()
                else -> filterByClassName(name)
            }
        }

        private val classAliases = mapOf("CIDissolveTransition" to "CITransitionFilter")

        private fun filterByClassName(name: String): CIFilter? {
            val className = classAliases[name] ?: name
            return try {
                val filterClass = Class.forName("com.moblin.android.platform.coreimage.$className")
                filterClass.getDeclaredConstructor().newInstance() as? CIFilter
            } catch (error: Throwable) {
                null
            }
        }

        operator fun invoke(name: String, parameters: Map<String, Any?>): CIFilter? {
            val filter = invoke(name) ?: return null
            for ((key, value) in parameters) {
                filter.setValue(value, forKey = key)
            }
            return filter
        }

        fun gaussianBlur(): CIGaussianBlur {
            return CIGaussianBlur()
        }

        fun sourceOverCompositing(): CICompositeOperation {
            return CICompositeOperation()
        }

        fun blendWithMask(): CIBlendWithMask {
            return CIBlendWithMask()
        }

        fun roundedRectangleGenerator(): CIRoundedRectangleGenerator {
            return CIRoundedRectangleGenerator()
        }

        fun radialGradient(): CIRadialGradient {
            return CIRadialGradient()
        }

        fun qrCodeGenerator(): CIQRCodeGenerator {
            return CIQRCodeGenerator()
        }

        fun colorMonochrome(): CIColorMonochrome {
            return CIColorMonochrome()
        }

        fun colorMatrix(): CIColorMatrix {
            return CIColorMatrix()
        }

        fun sepiaTone(): CISepiaTone {
            return CISepiaTone()
        }

        fun colorControls(): CIColorControls {
            return CIColorControls()
        }

        fun vignette(): CIVignette {
            return CIVignette()
        }

        fun pixellate(): CIPixellate {
            return CIPixellate()
        }

        fun pinchDistortion(): CIPinchDistortion {
            return CIPinchDistortion()
        }

        fun twirlDistortion(): CITwirlDistortion {
            return CITwirlDistortion()
        }

        fun stripesGenerator(): CIStripesGenerator {
            return CIStripesGenerator()
        }

        fun checkerboardGenerator(): CICheckerboardGenerator {
            return CICheckerboardGenerator()
        }

        fun stretchCrop(): CIStretchCrop {
            return CIStretchCrop()
        }

        fun dissolveTransition(): CITransitionFilter {
            return CITransitionFilter()
        }

        fun colorCubeWithColorSpace(): CIColorCubeWithColorSpace {
            return CIColorCubeWithColorSpace()
        }
    }
}

class CIGaussianBlur : CIFilter() {
    var inputImage: CIImage? = null
    var radius: Float = 10f

    override val name: String
        get() = "CIGaussianBlur"

    override val outputImage: CIImage?
        get() {
            val image = inputImage ?: return null
            val sigma = radius.toDouble()
            if (sigma <= 0.0) {
                return CIImage(image.node)
            }
            return CIImage(BlurNode(image.node, sigma, false, blurExtent(image.extent, sigma)))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputRadiusKey -> radius = anyToFloat(value, radius)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputRadiusKey -> radius
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        radius = 10f
    }
}

class CICompositeOperation : CIFilter() {
    var inputImage: CIImage? = null
    var backgroundImage: CIImage? = null

    override val name: String
        get() = "CISourceOverCompositing"

    override val outputImage: CIImage?
        get() {
            val image = inputImage ?: return null
            val background = backgroundImage ?: return null
            return CIImage(CompositeNode(image.node, background.node))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputBackgroundImageKey -> backgroundImage = value as? CIImage
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputBackgroundImageKey -> backgroundImage
            else -> super.value(forKey)
        }
    }
}

class CIBlendWithMask : CIFilter() {
    var inputImage: CIImage? = null
    var backgroundImage: CIImage? = null
    var maskImage: CIImage? = null

    override val name: String
        get() = "CIBlendWithMask"

    override val outputImage: CIImage?
        get() {
            val image = inputImage ?: return null
            val mask = maskImage ?: return null
            val background = backgroundImage ?: CIImage.empty()
            val extent = image.extent.union(background.extent)
            return CIImage(CombineNode(listOf(image.node, background.node, mask.node), BlendWithMaskCombine(), extent))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputBackgroundImageKey -> backgroundImage = value as? CIImage
            kCIInputMaskImageKey -> maskImage = value as? CIImage
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputBackgroundImageKey -> backgroundImage
            kCIInputMaskImageKey -> maskImage
            else -> super.value(forKey)
        }
    }
}

class CIRoundedRectangleGenerator : CIFilter() {
    var extent: CGRect = CGRect(0.0, 0.0, 100.0, 100.0)
    var radius: Float = 10f
    var color: CIColor = CIColor.white

    override val name: String
        get() = "CIRoundedRectangleGenerator"

    override val outputImage: CIImage?
        get() {
            val rect = extent.standardized
            val generator = RoundedRectangleGenerator(
                rect,
                radius.toDouble(),
                color.red,
                color.green,
                color.blue,
                color.alpha
            )
            return CIImage(GeneratorNode(generator, rect))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputExtentKey -> extent = anyToCGRect(value, extent)
            kCIInputRadiusKey -> radius = anyToFloat(value, radius)
            kCIInputColorKey -> color = value as? CIColor ?: color
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputExtentKey -> CIVector(cgRect = extent)
            kCIInputRadiusKey -> radius
            kCIInputColorKey -> color
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        extent = CGRect(0.0, 0.0, 100.0, 100.0)
        radius = 10f
        color = CIColor.white
    }
}

class CIRadialGradient : CIFilter() {
    var center: CGPoint = CGPoint(150.0, 150.0)
    var radius0: Float = 5f
    var radius1: Float = 100f
    var color0: CIColor = CIColor.white
    var color1: CIColor = CIColor.black

    override val name: String
        get() = "CIRadialGradient"

    override val outputImage: CIImage?
        get() {
            val generator = RadialGradientGenerator(
                center.x,
                center.y,
                radius0.toDouble(),
                radius1.toDouble(),
                doubleArrayOf(color0.red, color0.green, color0.blue, color0.alpha),
                doubleArrayOf(color1.red, color1.green, color1.blue, color1.alpha)
            )
            return CIImage(GeneratorNode(generator, CGRect.infinite))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputCenterKey -> center = anyToCGPoint(value, center)
            "inputRadius0" -> radius0 = anyToFloat(value, radius0)
            "inputRadius1" -> radius1 = anyToFloat(value, radius1)
            "inputColor0" -> color0 = value as? CIColor ?: color0
            "inputColor1" -> color1 = value as? CIColor ?: color1
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputCenterKey -> CIVector(cgPoint = center)
            "inputRadius0" -> radius0
            "inputRadius1" -> radius1
            "inputColor0" -> color0
            "inputColor1" -> color1
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        center = CGPoint(150.0, 150.0)
        radius0 = 5f
        radius1 = 100f
        color0 = CIColor.white
        color1 = CIColor.black
    }
}

class CIConstantColorGenerator : CIFilter() {
    var color: CIColor = CIColor.black

    override val name: String
        get() = "CIConstantColorGenerator"

    override val outputImage: CIImage?
        get() = CIImage(
            ConstantNode(color.red, color.green, color.blue, color.alpha, SourceEncoding.srgb, CGRect.infinite)
        )

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputColorKey -> color = value as? CIColor ?: color
            else -> super.setValue(value, forKey)
        }
    }
}
