package com.moblin.android.platform.coreimage

import com.moblin.android.platform.coregraphics.CGColorSpace
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coreimage.internal.EffectsLog
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val defaultCenter = CGPoint(150.0, 150.0)

private val identityCube2: ByteArray = run {
    val buffer = ByteBuffer.allocate(8 * 16).order(ByteOrder.LITTLE_ENDIAN)
    for (blue in 0..1) {
        for (green in 0..1) {
            for (red in 0..1) {
                buffer.putFloat(red.toFloat())
                buffer.putFloat(green.toFloat())
                buffer.putFloat(blue.toFloat())
                buffer.putFloat(1f)
            }
        }
    }
    buffer.array()
}

private fun anyToCIVector(value: Any?, fallback: CIVector): CIVector {
    return when (value) {
        is CIVector -> value
        is CGPoint -> CIVector(cgPoint = value)
        else -> fallback
    }
}

private fun unfinishedOutput(member: String): CIImage? {
    EffectsLog.notImplemented(member)
    return null
}

class CIColorMonochrome : CIFilter() {
    var inputImage: CIImage? = null
    var color: CIColor = CIColor(0.6, 0.45, 0.3, 1.0)
    var intensity: Float = 1f

    override val name: String
        get() = "CIColorMonochrome"

    override val outputImage: CIImage?
        get() = unfinishedOutput("CIColorMonochrome.outputImage")

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputColorKey -> color = value as? CIColor ?: color
            kCIInputIntensityKey -> intensity = anyToFloat(value, intensity)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputColorKey -> color
            kCIInputIntensityKey -> intensity
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        color = CIColor(0.6, 0.45, 0.3, 1.0)
        intensity = 1f
    }
}

class CIColorMatrix : CIFilter() {
    var inputImage: CIImage? = null
    var rVector: CIVector = CIVector(1.0, 0.0, 0.0, 0.0)
    var gVector: CIVector = CIVector(0.0, 1.0, 0.0, 0.0)
    var bVector: CIVector = CIVector(0.0, 0.0, 1.0, 0.0)
    var aVector: CIVector = CIVector(0.0, 0.0, 0.0, 1.0)
    var biasVector: CIVector = CIVector(0.0, 0.0, 0.0, 0.0)

    override val name: String
        get() = "CIColorMatrix"

    override val outputImage: CIImage?
        get() = unfinishedOutput("CIColorMatrix.outputImage")

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            "inputRVector" -> rVector = anyToCIVector(value, rVector)
            "inputGVector" -> gVector = anyToCIVector(value, gVector)
            "inputBVector" -> bVector = anyToCIVector(value, bVector)
            "inputAVector" -> aVector = anyToCIVector(value, aVector)
            "inputBiasVector" -> biasVector = anyToCIVector(value, biasVector)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            "inputRVector" -> rVector
            "inputGVector" -> gVector
            "inputBVector" -> bVector
            "inputAVector" -> aVector
            "inputBiasVector" -> biasVector
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        rVector = CIVector(1.0, 0.0, 0.0, 0.0)
        gVector = CIVector(0.0, 1.0, 0.0, 0.0)
        bVector = CIVector(0.0, 0.0, 1.0, 0.0)
        aVector = CIVector(0.0, 0.0, 0.0, 1.0)
        biasVector = CIVector(0.0, 0.0, 0.0, 0.0)
    }
}

class CISepiaTone : CIFilter() {
    var inputImage: CIImage? = null
    var intensity: Float = 1f

    override val name: String
        get() = "CISepiaTone"

    override val outputImage: CIImage?
        get() = unfinishedOutput("CISepiaTone.outputImage")

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputIntensityKey -> intensity = anyToFloat(value, intensity)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputIntensityKey -> intensity
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        intensity = 1f
    }
}

class CIColorControls : CIFilter() {
    var inputImage: CIImage? = null
    var saturation: Float = 1f
    var brightness: Float = 0f
    var contrast: Float = 1f

    override val name: String
        get() = "CIColorControls"

    override val outputImage: CIImage?
        get() = unfinishedOutput("CIColorControls.outputImage")

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputSaturationKey -> saturation = anyToFloat(value, saturation)
            kCIInputBrightnessKey -> brightness = anyToFloat(value, brightness)
            kCIInputContrastKey -> contrast = anyToFloat(value, contrast)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputSaturationKey -> saturation
            kCIInputBrightnessKey -> brightness
            kCIInputContrastKey -> contrast
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        saturation = 1f
        brightness = 0f
        contrast = 1f
    }
}

class CIVignette : CIFilter() {
    var inputImage: CIImage? = null
    var intensity: Float = 0f
    var radius: Float = 1f

    override val name: String
        get() = "CIVignette"

    override val outputImage: CIImage?
        get() = unfinishedOutput("CIVignette.outputImage")

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputIntensityKey -> intensity = anyToFloat(value, intensity)
            kCIInputRadiusKey -> radius = anyToFloat(value, radius)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputIntensityKey -> intensity
            kCIInputRadiusKey -> radius
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        intensity = 0f
        radius = 1f
    }
}

class CIPixellate : CIFilter() {
    var inputImage: CIImage? = null
    var center: CGPoint = defaultCenter
    var scale: Float = 8f

    override val name: String
        get() = "CIPixellate"

    override val outputImage: CIImage?
        get() = unfinishedOutput("CIPixellate.outputImage")

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputCenterKey -> center = anyToCGPoint(value, center)
            kCIInputScaleKey -> scale = anyToFloat(value, scale)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputCenterKey -> CIVector(cgPoint = center)
            kCIInputScaleKey -> scale
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        center = defaultCenter
        scale = 8f
    }
}

class CIPinchDistortion : CIFilter() {
    var inputImage: CIImage? = null
    var center: CGPoint = defaultCenter
    var radius: Float = 300f
    var scale: Float = 0.5f

    override val name: String
        get() = "CIPinchDistortion"

    override val outputImage: CIImage?
        get() = unfinishedOutput("CIPinchDistortion.outputImage")

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputCenterKey -> center = anyToCGPoint(value, center)
            kCIInputRadiusKey -> radius = anyToFloat(value, radius)
            kCIInputScaleKey -> scale = anyToFloat(value, scale)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputCenterKey -> CIVector(cgPoint = center)
            kCIInputRadiusKey -> radius
            kCIInputScaleKey -> scale
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        center = defaultCenter
        radius = 300f
        scale = 0.5f
    }
}

class CITwirlDistortion : CIFilter() {
    var inputImage: CIImage? = null
    var center: CGPoint = defaultCenter
    var radius: Float = 300f
    var angle: Float = Math.PI.toFloat()

    override val name: String
        get() = "CITwirlDistortion"

    override val outputImage: CIImage?
        get() = unfinishedOutput("CITwirlDistortion.outputImage")

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputCenterKey -> center = anyToCGPoint(value, center)
            kCIInputRadiusKey -> radius = anyToFloat(value, radius)
            kCIInputAngleKey -> angle = anyToFloat(value, angle)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputCenterKey -> CIVector(cgPoint = center)
            kCIInputRadiusKey -> radius
            kCIInputAngleKey -> angle
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        center = defaultCenter
        radius = 300f
        angle = Math.PI.toFloat()
    }
}

class CIStripesGenerator : CIFilter() {
    var center: CGPoint = defaultCenter
    var color0: CIColor = CIColor.white
    var color1: CIColor = CIColor.black
    var width: Float = 80f
    var sharpness: Float = 1f

    override val name: String
        get() = "CIStripesGenerator"

    override val outputImage: CIImage?
        get() = unfinishedOutput("CIStripesGenerator.outputImage")

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputCenterKey -> center = anyToCGPoint(value, center)
            "inputColor0" -> color0 = value as? CIColor ?: color0
            "inputColor1" -> color1 = value as? CIColor ?: color1
            kCIInputWidthKey -> width = anyToFloat(value, width)
            kCIInputSharpnessKey -> sharpness = anyToFloat(value, sharpness)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputCenterKey -> CIVector(cgPoint = center)
            "inputColor0" -> color0
            "inputColor1" -> color1
            kCIInputWidthKey -> width
            kCIInputSharpnessKey -> sharpness
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        center = defaultCenter
        color0 = CIColor.white
        color1 = CIColor.black
        width = 80f
        sharpness = 1f
    }
}

class CICheckerboardGenerator : CIFilter() {
    var center: CGPoint = defaultCenter
    var color0: CIColor = CIColor.white
    var color1: CIColor = CIColor.black
    var width: Float = 80f
    var sharpness: Float = 1f

    override val name: String
        get() = "CICheckerboardGenerator"

    override val outputImage: CIImage?
        get() = unfinishedOutput("CICheckerboardGenerator.outputImage")

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputCenterKey -> center = anyToCGPoint(value, center)
            "inputColor0" -> color0 = value as? CIColor ?: color0
            "inputColor1" -> color1 = value as? CIColor ?: color1
            kCIInputWidthKey -> width = anyToFloat(value, width)
            kCIInputSharpnessKey -> sharpness = anyToFloat(value, sharpness)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputCenterKey -> CIVector(cgPoint = center)
            "inputColor0" -> color0
            "inputColor1" -> color1
            kCIInputWidthKey -> width
            kCIInputSharpnessKey -> sharpness
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        center = defaultCenter
        color0 = CIColor.white
        color1 = CIColor.black
        width = 80f
        sharpness = 1f
    }
}

class CIStretchCrop : CIFilter() {
    var inputImage: CIImage? = null
    var size: CGPoint = CGPoint(1280.0, 720.0)
    var cropAmount: Float = 0.25f
    var centerStretchAmount: Float = 0.25f

    override val name: String
        get() = "CIStretchCrop"

    override val outputImage: CIImage?
        get() = unfinishedOutput("CIStretchCrop.outputImage")

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            "inputSize" -> size = anyToCGPoint(value, size)
            "inputCropAmount" -> cropAmount = anyToFloat(value, cropAmount)
            "inputCenterStretchAmount" -> centerStretchAmount = anyToFloat(value, centerStretchAmount)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            "inputSize" -> CIVector(cgPoint = size)
            "inputCropAmount" -> cropAmount
            "inputCenterStretchAmount" -> centerStretchAmount
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        size = CGPoint(1280.0, 720.0)
        cropAmount = 0.25f
        centerStretchAmount = 0.25f
    }
}

class CIColorCubeWithColorSpace : CIFilter() {
    var inputImage: CIImage? = null
    var cubeDimension: Float = 2f
    var cubeData: ByteArray = identityCube2.copyOf()
    var colorSpace: CGColorSpace? = null
    var extrapolate: Boolean = false

    override val name: String
        get() = "CIColorCubeWithColorSpace"

    override val outputImage: CIImage?
        get() = unfinishedOutput("CIColorCubeWithColorSpace.outputImage")

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            "inputCubeDimension" -> cubeDimension = anyToFloat(value, cubeDimension)
            "inputCubeData" -> cubeData = value as? ByteArray ?: cubeData
            "inputColorSpace" -> colorSpace = value as? CGColorSpace
            "inputExtrapolate" -> extrapolate = value as? Boolean ?: extrapolate
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            "inputCubeDimension" -> cubeDimension
            "inputCubeData" -> cubeData
            "inputColorSpace" -> colorSpace
            "inputExtrapolate" -> extrapolate
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        cubeDimension = 2f
        cubeData = identityCube2.copyOf()
        colorSpace = null
        extrapolate = false
    }
}
