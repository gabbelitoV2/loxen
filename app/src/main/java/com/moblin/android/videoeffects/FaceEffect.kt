package com.moblin.android.videoeffects

import android.graphics.Bitmap
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.Detections
import com.moblin.android.media.haishinkit.media.video.TextDetection
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectDetectionsMode
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIColor
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIColorComponent
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTILayer
import com.moblin.android.platform.metalpetal.MTIMPSGaussianBlurFilter
import com.moblin.android.platform.metalpetal.MTIMask
import com.moblin.android.platform.metalpetal.MTIMaskMode
import com.moblin.android.platform.metalpetal.MTIMultilayerCompositingFilter
import com.moblin.android.platform.metalpetal.MTIPixellateFilter
import com.moblin.android.platform.simd.SIMD2
import com.moblin.android.platform.uikit.cgImage
import com.moblin.android.platform.vision.VNFaceObservation
import com.moblin.android.various.calcFaceAngle
import com.moblin.android.various.rotatePoint
import com.moblin.android.various.stableBoundingBox
import kotlinx.coroutines.launch
import com.moblin.android.platform.coregraphics.CGAffineTransform

private fun makeFaceMask(ratio: Float): MTIMask? {
    val side = 256.0
    val filter = CIFilter.radialGradient()
    filter.center = CGPoint(x = side / 2, y = side / 2)
    filter.radius0 = (side / 2).toFloat() / ratio
    filter.radius1 = (side / 2).toFloat()
    filter.color0 = CIColor.white
    filter.color1 = CIColor.black
    val image = filter.outputImage
        ?.cropped(to = CGRect(x = 0.0, y = 0.0, width = side, height = side))
        ?: return null
    return MTIMask(
        content = image.toEffectImage(isOpaque = true).getMetalPetalImage(),
        component = MTIColorComponent.red,
        mode = MTIMaskMode.normal
    )
}

data class FaceEffectSettings(
    val blurFaces: Boolean = true,
    val blurText: Boolean = true,
    val blurBackground: Boolean = true,
    val showMouth: Boolean = true,
    val privacyMode: FaceEffectPrivacyMode = FaceEffectPrivacyMode.Blur(strength = 1.0f)
)

sealed class FaceEffectPrivacyMode {
    data class Blur(val strength: Float) : FaceEffectPrivacyMode()
    data class Pixellate(val strength: Float) : FaceEffectPrivacyMode()
    data class BackgroundImage(val image: CIImage?) : FaceEffectPrivacyMode()
    data class Icon(val image: Bitmap?) : FaceEffectPrivacyMode()
}

private data class FaceIconPlacement(val center: CGPoint, val height: Double, val rotation: Double)

class FaceEffect : VideoEffect() {
    private var settings = FaceEffectSettings()
    private val moblinImage: EffectImageCgImage? = Bundle.image("AppIconNoBackground")
        ?.cgImage
        ?.toEffectImage()
    private var backgroundImage: EffectImageCiImage? = null
    private var iconImage: EffectImageCgImage? = null
    private val faceMasks = mutableMapOf<Float, MTIMask?>()

    fun setSettings(settings: FaceEffectSettings) {
        val backgroundImage = (settings.privacyMode as? FaceEffectPrivacyMode.BackgroundImage)
            ?.image
            ?.toEffectImage(isOpaque = true)
        val iconImage = (settings.privacyMode as? FaceEffectPrivacyMode.Icon)
            ?.image
            ?.toEffectImage()
        processorPipelineQueue.launch {
            this@FaceEffect.settings = settings
            this@FaceEffect.backgroundImage = backgroundImage
            this@FaceEffect.iconImage = iconImage
        }
    }

    override fun needsFaceDetections(interval: Double): VideoEffectDetectionsMode {
        return if (settings.blurFaces || settings.blurBackground || settings.showMouth) {
            VideoEffectDetectionsMode.Now(null)
        } else {
            VideoEffectDetectionsMode.Off
        }
    }

    override fun needsTextDetections(interval: Double): VideoEffectDetectionsMode {
        return if (settings.blurText) {
            VideoEffectDetectionsMode.Now(null)
        } else {
            VideoEffectDetectionsMode.Off
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val detections = info.sceneDetections() ?: return image
        var outputImage: CIImage? = image
        if ((settings.blurFaces && detections.face.isNotEmpty()) ||
            (settings.blurText && detections.text.isNotEmpty()) ||
            settings.blurBackground
        ) {
            outputImage = applyBlur(
                image = image,
                detections = detections,
                blurFaces = settings.blurFaces,
                blurText = settings.blurText,
                blurBackground = settings.blurBackground
            )
        }
        if (settings.showMouth) {
            outputImage = addMouth(image = outputImage, detections = detections.face)
        }
        return outputImage ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val detections = info.sceneDetections() ?: return image
        val blurFaces = settings.blurFaces && detections.face.isNotEmpty()
        val blurText = settings.blurText && detections.text.isNotEmpty()
        if (!blurFaces && !blurText && !settings.blurBackground && !settings.showMouth) {
            return image
        }
        val icon = if (blurFaces) iconImage?.getMetalPetalImage() else null
        var privacyImage = image
        if (blurText || settings.blurBackground || (blurFaces && icon == null)) {
            val madePrivacyImage = makePrivacyImageMetalPetal(image = image) ?: return image
            privacyImage = madePrivacyImage
        }
        val backgroundImage = if (settings.blurBackground) privacyImage else image
        val layers = mutableListOf<MTILayer>()
        if (icon == null && (blurFaces || settings.blurBackground)) {
            val facesImage = if (settings.blurBackground) image else privacyImage
            layers += makeFaceLayers(image = image, facesImage = facesImage, detections = detections.face)
        }
        if (blurText) {
            layers += makeTextLayers(image = image, privacyImage = privacyImage, detections = detections.text)
        }
        if (icon != null) {
            layers += makeIconLayers(image = image, icon = icon, detections = detections.face)
        }
        if (settings.showMouth) {
            layers += makeMouthLayers(image = image, detections = detections.face)
        }
        if (layers.isEmpty() && !settings.blurBackground) {
            return image
        }
        val filter = MTIMultilayerCompositingFilter()
        filter.inputBackgroundImage = backgroundImage
        filter.layers = layers
        return filter.outputImage ?: image
    }

    private fun makePrivacyImageMetalPetal(image: MTIImage): MTIImage? {
        return when (val privacyMode = settings.privacyMode) {
            is FaceEffectPrivacyMode.Blur -> {
                val filter = MTIMPSGaussianBlurFilter()
                filter.inputImage = image
                filter.radius = (image.extent.width / 50.0).toFloat() * privacyMode.strength
                filter.outputImage
            }
            is FaceEffectPrivacyMode.Pixellate -> {
                val filter = MTIPixellateFilter()
                filter.inputImage = image
                val scale = pixellateCalcScale(size = image.extent.size, strength = privacyMode.strength)
                filter.scale = SIMD2(scale, scale)
                filter.outputImage
            }
            is FaceEffectPrivacyMode.BackgroundImage -> {
                val backgroundImage = this.backgroundImage?.getMetalPetalImage() ?: return null
                val size = image.extent.size
                val scale = maxOf(
                    size.width / backgroundImage.extent.width,
                    size.height / backgroundImage.extent.height
                )
                backgroundImage.positionComposited(
                    position = CGPoint(x = size.width / 2, y = size.height / 2),
                    backgroundImage = image,
                    size = CGSize(
                        width = backgroundImage.extent.width * scale,
                        height = backgroundImage.extent.height * scale
                    )
                )
            }
            is FaceEffectPrivacyMode.Icon -> {
                val filter = MTIMPSGaussianBlurFilter()
                filter.inputImage = image
                filter.radius = (image.extent.width / 50.0).toFloat()
                filter.outputImage
            }
        }
    }

    private fun calcIconPlacement(detection: VNFaceObservation, imageSize: CGSize): FaceIconPlacement? {
        val rotation = detection.calcFaceAngle(imageSize = imageSize) ?: 0.0
        val boundingBox = detection.stableBoundingBox(imageSize = imageSize, rotationAngle = rotation)
            ?: return null
        return FaceIconPlacement(
            center = CGPoint(x = boundingBox.midX, y = boundingBox.midY + 0.25 * boundingBox.height),
            height = 1.5 * boundingBox.height,
            rotation = rotation
        )
    }

    private fun makeIconLayers(
        image: MTIImage,
        icon: MTIImage,
        detections: List<VNFaceObservation>
    ): List<MTILayer> {
        val imageSize = image.extent.size
        return detections.mapNotNull { detection ->
            val placement = calcIconPlacement(detection = detection, imageSize = imageSize)
                ?: return@mapNotNull null
            val scale = placement.height / icon.extent.height
            val size = CGSize(width = icon.extent.width * scale, height = placement.height)
            val centerPoint = rotatePoint(
                point = CGPoint(
                    x = placement.center.x - size.width / 2,
                    y = placement.center.y - size.height / 2
                ),
                alpha = placement.rotation
            )
            val rotatedCenter = rotatePoint(
                point = CGPoint(x = size.width / 2, y = size.height / 2),
                alpha = placement.rotation
            )
            val center = CGPoint(
                x = rotatedCenter.x + centerPoint.x,
                y = rotatedCenter.y + centerPoint.y
            )
            MTILayer(
                content = icon,
                position = CGPoint(x = center.x, y = imageSize.height - center.y),
                size = size,
                rotation = (-placement.rotation).toFloat()
            )
        }
    }

    private fun makeFaceLayers(
        image: MTIImage,
        facesImage: MTIImage,
        detections: List<VNFaceObservation>
    ): List<MTILayer> {
        val ratio: Float = when (settings.privacyMode) {
            is FaceEffectPrivacyMode.Blur, is FaceEffectPrivacyMode.Pixellate -> 1.5f
            is FaceEffectPrivacyMode.BackgroundImage, is FaceEffectPrivacyMode.Icon -> 1.2f
        }
        if (!faceMasks.containsKey(ratio)) {
            faceMasks[ratio] = makeFaceMask(ratio)
        }
        val mask = faceMasks[ratio] ?: return emptyList()
        val size = image.extent.size
        return detections.mapNotNull { detection ->
            val boundingBox = detection.stableBoundingBox(imageSize = size) ?: return@mapNotNull null
            val side = 2 * ratio.toDouble() * boundingBox.height / 1.7
            val position = CGPoint(x = boundingBox.midX, y = size.height - boundingBox.midY)
            MTILayer(
                content = facesImage,
                contentRegion = CGRect(
                    x = position.x - side / 2,
                    y = position.y - side / 2,
                    width = side,
                    height = side
                ),
                mask = mask,
                position = position,
                size = CGSize(width = side, height = side)
            )
        }
    }

    private fun makeTextLayers(
        image: MTIImage,
        privacyImage: MTIImage,
        detections: List<TextDetection>
    ): List<MTILayer> {
        val size = image.extent.size
        return detections.map { detection ->
            val boundingBox = CGRect(
                x = detection.boundingBox.origin.x * 1920,
                y = detection.boundingBox.origin.y * 1080,
                width = detection.boundingBox.width * 1920,
                height = detection.boundingBox.height * 1080
            )
            val contentRegion = CGRect(
                x = boundingBox.minX,
                y = size.height - boundingBox.maxY,
                width = boundingBox.width,
                height = boundingBox.height
            )
            MTILayer(
                content = privacyImage,
                contentRegion = contentRegion,
                position = CGPoint(x = contentRegion.midX, y = contentRegion.midY),
                size = contentRegion.size
            )
        }
    }

    private fun makeMouthLayers(
        image: MTIImage,
        detections: List<VNFaceObservation>
    ): List<MTILayer> {
        val moblinImage = this.moblinImage?.getMetalPetalImage() ?: return emptyList()
        val size = image.extent.size
        return detections.mapNotNull { detection ->
            val mouth = calcMouth(
                detection = detection,
                imageSize = size,
                moblinImageSize = moblinImage.extent.size
            ) ?: return@mapNotNull null
            MTILayer(
                content = moblinImage,
                position = CGPoint(x = mouth.midX, y = size.height - mouth.midY),
                size = mouth.size
            )
        }
    }

    private fun makePrivacyImage(image: CIImage): CIImage? {
        return when (val privacyMode = settings.privacyMode) {
            is FaceEffectPrivacyMode.Blur -> image
                .applyingGaussianBlur(sigma = (image.extent.width / 50.0) * privacyMode.strength.toDouble())
                .cropped(to = image.extent)
            is FaceEffectPrivacyMode.Pixellate -> {
                val filter = CIFilter.pixellate()
                filter.inputImage = image
                filter.center = CGPoint.zero
                filter.scale = pixellateCalcScale(size = image.extent.size, strength = privacyMode.strength)
                filter.outputImage?.cropped(to = image.extent) ?: image
            }
            is FaceEffectPrivacyMode.BackgroundImage -> privacyMode.image
                ?.scaledToFill(size = image.extent.size)
                ?.cropped(to = image.extent)
            is FaceEffectPrivacyMode.Icon -> image
                .applyingGaussianBlur(sigma = image.extent.width / 50.0)
                .cropped(to = image.extent)
        }
    }

    private fun createFacesMaskImage(
        imageExtent: CGRect,
        detections: List<VNFaceObservation>
    ): CIImage? {
        var mask = CIImage.empty().cropped(to = imageExtent)
        for (detection in detections) {
            val boundingBox = detection.stableBoundingBox(imageSize = imageExtent.size) ?: continue
            val faceCenter = CGPoint(
                x = boundingBox.maxX - (boundingBox.width / 2),
                y = boundingBox.maxY - (boundingBox.height / 2)
            )
            val faceMask = CIFilter.radialGradient()
            faceMask.center = faceCenter
            faceMask.radius0 = (boundingBox.height / 1.7).toFloat()
            when (settings.privacyMode) {
                is FaceEffectPrivacyMode.Blur -> faceMask.radius1 = faceMask.radius0 * 1.5f
                is FaceEffectPrivacyMode.Pixellate -> faceMask.radius1 = faceMask.radius0 * 1.5f
                is FaceEffectPrivacyMode.BackgroundImage,
                is FaceEffectPrivacyMode.Icon -> faceMask.radius1 = faceMask.radius0 * 1.2f
            }
            faceMask.color0 = CIColor.white
            faceMask.color1 = CIColor(red = 1.0, green = 1.0, blue = 1.0, alpha = 0.0)
            val croppedMask = faceMask.outputImage?.cropped(to = boundingBox.insetBy(
                dx = -boundingBox.width / 2,
                dy = -boundingBox.height / 2
            )) ?: continue
            mask = croppedMask.composited(over = mask)
        }
        return mask
    }

    private fun createTextsMaskImage(
        imageExtent: CGRect,
        detections: List<TextDetection>
    ): CIImage? {
        var mask = CIImage.empty().cropped(to = imageExtent)
        for (detection in detections) {
            val x = detection.boundingBox.origin.x * 1920
            val y = detection.boundingBox.origin.y * 1080
            val width = detection.boundingBox.width * 1920
            val height = detection.boundingBox.height * 1080
            val boundingBox = CGRect(x = x, y = y, width = width, height = height)
            mask = CIImage(color = CIColor.white)
                .cropped(to = boundingBox)
                .composited(over = mask)
        }
        return mask
    }

    private fun applyBlur(
        image: CIImage,
        detections: Detections,
        blurFaces: Boolean,
        blurText: Boolean,
        blurBackground: Boolean
    ): CIImage? {
        val icon = if (blurFaces) iconImage?.getCiImage() else null
        val privacyImage = makePrivacyImage(image = image)
        var outputImage: CIImage? = image
        if ((blurFaces && detections.face.isNotEmpty()) || blurBackground) {
            val mask = createFacesMaskImage(imageExtent = image.extent, detections = detections.face)
            if (blurFaces && icon == null) {
                val blender = CIFilter.blendWithMask()
                blender.inputImage = privacyImage
                blender.backgroundImage = image
                blender.maskImage = mask
                outputImage = blender.outputImage
            }
            if (blurBackground) {
                val blender = CIFilter.blendWithMask()
                blender.inputImage = outputImage
                blender.backgroundImage = privacyImage
                blender.maskImage = mask
                outputImage = blender.outputImage
            }
        }
        if (blurText && detections.text.isNotEmpty()) {
            val mask = createTextsMaskImage(imageExtent = image.extent, detections = detections.text)
            val blender = CIFilter.blendWithMask()
            blender.inputImage = privacyImage
            blender.backgroundImage = outputImage
            blender.maskImage = mask
            outputImage = blender.outputImage
        }
        if (icon != null) {
            outputImage = addIcons(image = outputImage, icon = icon, detections = detections.face)
        }
        return outputImage
    }

    private fun addIcons(
        image: CIImage?,
        icon: CIImage,
        detections: List<VNFaceObservation>
    ): CIImage? {
        val sourceImage = image ?: return null
        var outputImage = sourceImage
        for (detection in detections) {
            val placement = calcIconPlacement(detection = detection, imageSize = sourceImage.extent.size)
                ?: continue
            val scale = placement.height / icon.extent.height
            val iconImage = icon.scaled(x = scale, y = scale)
            val centerPoint = rotatePoint(
                point = CGPoint(
                    x = placement.center.x - iconImage.extent.midX,
                    y = placement.center.y - iconImage.extent.midY
                ),
                alpha = placement.rotation
            )
            outputImage = iconImage
                .transformed(by = CGAffineTransform(rotationAngle = placement.rotation))
                .translated(x = centerPoint.x, y = centerPoint.y)
                .composited(over = outputImage)
        }
        return outputImage.cropped(to = sourceImage.extent)
    }

    private fun calcMouth(
        detection: VNFaceObservation,
        imageSize: CGSize,
        moblinImageSize: CGSize
    ): CGRect? {
        val innerLips = detection.landmarks?.innerLips ?: return null
        val points = innerLips.pointsInImage(imageSize = imageSize)
        val firstPoint = points.firstOrNull() ?: return null
        var minX = firstPoint.x
        var maxX = firstPoint.x
        var minY = firstPoint.y
        var maxY = firstPoint.y
        for (point in points) {
            minX = minOf(point.x, minX)
            maxX = maxOf(point.x, maxX)
            minY = minOf(point.y, minY)
            maxY = maxOf(point.y, maxY)
        }
        val diffX = maxX - minX
        val diffY = maxY - minY
        if (diffY <= diffX) {
            return null
        }
        val height = moblinImageSize.height * (diffX / moblinImageSize.width)
        return CGRect(x = minX, y = minY + (diffY - height) / 2, width = diffX, height = height)
    }

    private fun addMouth(image: CIImage?, detections: List<VNFaceObservation>?): CIImage? {
        val sourceImage = image ?: return null
        if (detections == null) {
            return sourceImage
        }
        val moblinImage = this.moblinImage?.getCiImage() ?: return sourceImage
        var outputImage = sourceImage
        for (detection in detections) {
            val mouth = calcMouth(
                detection = detection,
                imageSize = sourceImage.extent.size,
                moblinImageSize = moblinImage.extent.size
            ) ?: continue
            val scale = mouth.width / moblinImage.extent.width
            outputImage = moblinImage
                .scaled(x = scale, y = scale)
                .translated(x = mouth.minX, y = mouth.minY)
                .composited(over = outputImage)
        }
        return outputImage.cropped(to = sourceImage.extent)
    }
}
