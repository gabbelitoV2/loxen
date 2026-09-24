package com.moblin.android.media.haishinkit.media.video

import android.media.MediaFormat
import android.util.Log
import com.moblin.android.common.various.create
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.avfoundation.AVCaptureVideoOrientation
import com.moblin.android.platform.core.ContinuousClock
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGColorSpace
import com.moblin.android.platform.coregraphics.CGImagePropertyOrientation
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coregraphics.cgSize
import com.moblin.android.platform.coreimage.CIContext
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIAlphaType
import com.moblin.android.platform.metalpetal.MTIColor
import com.moblin.android.platform.metalpetal.MTIContext
import com.moblin.android.platform.metalpetal.MTICropFilter
import com.moblin.android.platform.metalpetal.MTICropRegion
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTILayer
import com.moblin.android.platform.metalpetal.MTIMPSGaussianBlurFilter
import com.moblin.android.platform.metalpetal.MTIMultilayerCompositingFilter
import com.moblin.android.platform.metalpetal.MTLCreateSystemDefaultDevice
import com.moblin.android.platform.video.CMVideoFormatDescriptionCreateForImageBuffer
import com.moblin.android.platform.video.CVImageBuffer
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.CVPixelBufferPool
import com.moblin.android.platform.video.CVPixelBufferPoolCreate
import com.moblin.android.platform.video.CVPixelBufferPoolCreatePixelBuffer
import com.moblin.android.platform.video.kCVBufferPropagatedAttachmentsKey
import com.moblin.android.platform.video.kCVImageBufferCGColorSpaceKey
import com.moblin.android.platform.video.kCVImageBufferColorPrimariesKey
import com.moblin.android.platform.video.kCVImageBufferColorPrimaries_P3_D65
import com.moblin.android.platform.video.kCVImageBufferLogTransferFunctionKey
import com.moblin.android.platform.video.kCVImageBufferLogTransferFunction_AppleLog
import com.moblin.android.platform.video.kCVImageBufferTransferFunctionKey
import com.moblin.android.platform.video.kCVImageBufferYCbCrMatrixKey
import com.moblin.android.platform.video.kCVPixelBufferHeightKey
import com.moblin.android.platform.video.kCVPixelBufferIOSurfacePropertiesKey
import com.moblin.android.platform.video.kCVPixelBufferMetalCompatibilityKey
import com.moblin.android.platform.video.kCVPixelBufferPixelFormatTypeKey
import com.moblin.android.platform.video.kCVPixelBufferWidthKey
import com.moblin.android.platform.videotoolbox.CMFormatDescriptionGetExtensions
import com.moblin.android.various.settings.SettingsGraphicsImplementation
import com.moblin.android.videoeffects.MetalPetalWidgetShape
import com.moblin.android.videoeffects.VideoSourceEffect
import java.util.UUID
import kotlin.time.DurationUnit

class VideoEffectsProcessor {
    val context = CIContext()
    private val metalPetalContext: MTIContext?
    var canvasSize = CGSize(1920.0, 1080.0)
    var fillFrame = true
    var sceneSwitchTransition: SceneSwitchTransition = sceneSwitchTransitionEntry("blur")
    var latestSampleBufferTime: ContinuousClock.Instant? = null
    private var rotation: Double = 0.0
    private var mirror: Boolean = false
    private var effects: MutableList<VideoEffect> = mutableListOf()
    private var pendingAfterAttachEffects: MutableList<VideoEffect>? = null
    private var pendingAfterAttachRotation: Double? = null
    private var pendingAfterAttachMirror: Boolean? = null
    private var isMetalPetalGraphicsForcedByEffects: Boolean = false
    private var isMetalPetalGraphics: Boolean = false
    private var blackImage: CIImage? = null
    private var blackImageMetalPetal: MTIImage? = null
    private var pool: CVPixelBufferPool? = null
    private var poolColorSpace: CGColorSpace? = null
    private var poolFormatDescriptionExtension: Map<String, Any>? = null
    private var previousFaceDetectionTimes: MutableMap<UUID, Double> = mutableMapOf()
    private var previousTextDetectionTimes: MutableMap<UUID, Double> = mutableMapOf()

    init {
        val metalDevice = MTLCreateSystemDefaultDevice()
        metalPetalContext = if (metalDevice != null) {
            runCatching { MTIContext(device = metalDevice) }.getOrNull()
        } else {
            null
        }
    }

    fun reset() {
        blackImage = null
        blackImageMetalPetal = null
        pool = com.moblin.android.platform.video.swapPool(pool, null)
    }

    fun setGraphicsImplementation(value: SettingsGraphicsImplementation) {
        when (value) {
            SettingsGraphicsImplementation.coreImage -> isMetalPetalGraphics = false
            SettingsGraphicsImplementation.metalPetal -> isMetalPetalGraphics = true
        }
    }

    fun prepareForAttach() {
        if (pendingAfterAttachEffects == null) {
            pendingAfterAttachEffects = effects.toMutableList()
        }
        for (effect in effects.toList()) {
            if (effect is VideoSourceEffect) {
                unregisterEffect(effect)
            }
        }
    }

    fun registerEffect(effect: VideoEffect) {
        if (!effects.contains(effect)) {
            effects.add(effect)
        }
    }

    fun registerEffectBack(effect: VideoEffect) {
        if (!effects.contains(effect)) {
            effects.add(0, effect)
        }
    }

    fun unregisterEffect(effect: VideoEffect) {
        effect.removed()
        val index = effects.indexOf(effect)
        if (index >= 0) {
            effects.removeAt(index)
        }
    }

    fun unregisterAllEffects() {
        for (effect in effects) {
            effect.removed()
        }
        effects.clear()
    }

    fun setPendingAfterAttachEffects(effects: List<VideoEffect>, rotation: Double, mirror: Boolean) {
        pendingAfterAttachEffects = effects.toMutableList()
        pendingAfterAttachRotation = rotation
        pendingAfterAttachMirror = mirror
    }

    fun usePendingAfterAttachEffects() {
        val pendingEffects = pendingAfterAttachEffects
        if (pendingEffects != null) {
            effects = pendingEffects
            isMetalPetalGraphicsForcedByEffects = pendingEffects.any { it.isMetalPetal() }
            pendingAfterAttachEffects = null
        }
        val pendingRotation = pendingAfterAttachRotation
        if (pendingRotation != null) {
            rotation = pendingRotation
            pendingAfterAttachRotation = null
        }
        val pendingMirror = pendingAfterAttachMirror
        if (pendingMirror != null) {
            mirror = pendingMirror
            pendingAfterAttachMirror = null
        }
    }

    private fun getEnabledEffects(): List<VideoEffect> {
        return effects.filter { it.isEnabled() }
    }

    private fun removeEffects() {
        effects.removeAll { effect ->
            if (!effect.shouldRemove()) {
                return@removeAll false
            }
            effect.removed()
            true
        }
    }

    fun needsFaceDetections(presentationTimeStamp: Double, sceneVideoSourceId: UUID): Set<UUID> {
        val detectionsIntervals: MutableMap<UUID, Double> = mutableMapOf()
        val ids: MutableSet<UUID> = mutableSetOf()
        for (effect in getEnabledEffects()) {
            when (val mode = effect.needsFaceDetections(presentationTimeStamp)) {
                VideoEffectDetectionsMode.Off -> {}
                is VideoEffectDetectionsMode.Now -> {
                    val videoSourceId = mode.videoSourceId ?: sceneVideoSourceId
                    ids.add(videoSourceId)
                    previousFaceDetectionTimes[videoSourceId] = presentationTimeStamp
                }
                is VideoEffectDetectionsMode.Interval -> {
                    val videoSourceId = mode.videoSourceId ?: sceneVideoSourceId
                    val currentInterval = detectionsIntervals[videoSourceId]
                    if (currentInterval != null) {
                        if (mode.interval < currentInterval) {
                            detectionsIntervals[videoSourceId] = mode.interval
                        }
                    } else {
                        detectionsIntervals[videoSourceId] = mode.interval
                    }
                }
            }
        }
        for ((videoSourceId, interval) in detectionsIntervals) {
            val previousPresentationTimeStamp = previousFaceDetectionTimes[videoSourceId]
            if (previousPresentationTimeStamp != null) {
                if (presentationTimeStamp - previousPresentationTimeStamp > interval) {
                    ids.add(videoSourceId)
                    previousFaceDetectionTimes[videoSourceId] = presentationTimeStamp
                }
            } else {
                ids.add(videoSourceId)
                previousFaceDetectionTimes[videoSourceId] = presentationTimeStamp
            }
        }
        return ids
    }

    fun needsTextDetections(presentationTimeStamp: Double, sceneVideoSourceId: UUID): Set<UUID> {
        val detectionsIntervals: MutableMap<UUID, Double> = mutableMapOf()
        val ids: MutableSet<UUID> = mutableSetOf()
        for (effect in getEnabledEffects()) {
            when (val mode = effect.needsTextDetections(presentationTimeStamp)) {
                VideoEffectDetectionsMode.Off -> {}
                is VideoEffectDetectionsMode.Now -> {
                    val videoSourceId = mode.videoSourceId ?: sceneVideoSourceId
                    ids.add(videoSourceId)
                    previousTextDetectionTimes[videoSourceId] = presentationTimeStamp
                }
                is VideoEffectDetectionsMode.Interval -> {
                    val videoSourceId = mode.videoSourceId ?: sceneVideoSourceId
                    val currentInterval = detectionsIntervals[videoSourceId]
                    if (currentInterval != null) {
                        if (mode.interval < currentInterval) {
                            detectionsIntervals[videoSourceId] = mode.interval
                        }
                    } else {
                        detectionsIntervals[videoSourceId] = mode.interval
                    }
                }
            }
        }
        for ((videoSourceId, interval) in detectionsIntervals) {
            val previousPresentationTimeStamp = previousTextDetectionTimes[videoSourceId]
            if (previousPresentationTimeStamp != null) {
                if (presentationTimeStamp - previousPresentationTimeStamp > interval) {
                    ids.add(videoSourceId)
                    previousTextDetectionTimes[videoSourceId] = presentationTimeStamp
                }
            } else {
                ids.add(videoSourceId)
                previousTextDetectionTimes[videoSourceId] = presentationTimeStamp
            }
        }
        return ids
    }

    fun render(
        imageBuffer: CVImageBuffer,
        completion: DetectionsCompletion,
        videoUnit: VideoUnit,
        videoOrientation: Int
    ): Pair<CVImageBuffer, MediaSample> {
        val sampleBuffer = completion.sampleBuffer
        if (completion.isFirstAfterAttach) {
            usePendingAfterAttachEffects()
        }
        val enabledEffects = getEnabledEffects()
        if (enabledEffects.isEmpty() &&
            !completion.isSceneSwitchTransition &&
            imageBuffer.cgSize == canvasSize &&
            rotation == 0.0 &&
            !mirror
        ) {
            return Pair(imageBuffer, sampleBuffer)
        }
        val (newImageBuffer, newSampleBuffer) = applyEffects(
            imageBuffer,
            enabledEffects,
            completion,
            videoUnit,
            videoOrientation
        )
        removeEffects()
        return Pair(newImageBuffer ?: imageBuffer, newSampleBuffer ?: sampleBuffer)
    }

    private fun applyEffects(
        imageBuffer: CVImageBuffer,
        enabledEffects: List<VideoEffect>,
        completion: DetectionsCompletion,
        videoUnit: VideoUnit,
        videoOrientation: Int
    ): Pair<CVImageBuffer?, MediaSample?> {
        val sampleBuffer = completion.sampleBuffer
        val info = VideoEffectInfo(
            sceneVideoSourceId = completion.sceneVideoSourceId,
            detectionJobs = completion.detectionJobs,
            detections = completion.detections,
            presentationTimeStamp = sampleBuffer.presentationTimeUs,
            videoUnit = videoUnit,
            isFirstAfterAttach = completion.isFirstAfterAttach,
        )
        return if (isMetalPetalGraphicsEnabled()) {
            applyEffectsMetalPetal(
                imageBuffer,
                sampleBuffer,
                enabledEffects,
                completion.isSceneSwitchTransition,
                videoOrientation,
                info
            )
        } else {
            applyEffectsCoreImage(
                imageBuffer,
                sampleBuffer,
                enabledEffects,
                completion.isSceneSwitchTransition,
                videoOrientation,
                info
            )
        }
    }

    fun isAtEndOfSceneSwitchTransition(): Boolean {
        val latest = latestSampleBufferTime
        if (latest != null) {
            val offset = ContinuousClock.now - latest
            return if (sceneSwitchTransitionName() == "blurandzoom") {
                offset.toDouble(DurationUnit.SECONDS) >= 5
            } else {
                offset.toDouble(DurationUnit.SECONDS) >= 2
            }
        }
        return false
    }

    fun renderSceneSwitchTransitionEnd(
        sampleBuffer: MediaSample,
        imageBuffer: CVImageBuffer,
        outputImageBuffer: CVPixelBuffer
    ): MediaSample {
        if (isMetalPetalGraphicsEnabled()) {
            val image = MTIImage(cvPixelBuffer = imageBuffer, alphaType = MTIAlphaType.alphaIsOne)
            try {
                metalPetalContext?.render(applySceneSwitchTransitionMetalPetal(image), to = outputImageBuffer)
            } catch (error: Throwable) {
                return sampleBuffer
            }
        } else {
            val image = applySceneSwitchTransition(CIImage(cvPixelBuffer = imageBuffer))
            val colorSpace = poolColorSpace
            if (colorSpace != null) {
                context.render(image, to = outputImageBuffer, bounds = image.extent, colorSpace = colorSpace)
            } else {
                context.render(image, to = outputImageBuffer)
            }
        }
        val formatDescription = CMVideoFormatDescriptionCreateForImageBuffer(outputImageBuffer)
        val outputSampleBuffer = create(
            outputImageBuffer,
            formatDescription,
            sampleBuffer.durationUs,
            sampleBuffer.presentationTimeUs,
            sampleBuffer.decodeTimeStampUs
        ) ?: return sampleBuffer
        return outputSampleBuffer
    }

    private fun isMetalPetalGraphicsEnabled(): Boolean {
        return isMetalPetalGraphics || isMetalPetalGraphicsForcedByEffects
    }

    private fun getBufferPool(formatDescription: MediaFormat): CVPixelBufferPool? {
        val formatDescriptionExtension = CMFormatDescriptionGetExtensions(formatDescription)
        val existingPool = pool
        if (existingPool != null && formatDescriptionExtension == poolFormatDescriptionExtension) {
            return existingPool
        }
        val pixelBufferAttributes: MutableMap<String, Any> = mutableMapOf(
            kCVPixelBufferPixelFormatTypeKey to pixelFormatType,
            kCVPixelBufferIOSurfacePropertiesKey to emptyMap<String, Any>(),
            kCVPixelBufferMetalCompatibilityKey to true,
            kCVPixelBufferWidthKey to canvasSize.width.toInt(),
            kCVPixelBufferHeightKey to canvasSize.height.toInt(),
        )
        poolColorSpace = null
        if (formatDescriptionExtension != null) {
            val colorPrimaries = formatDescriptionExtension[kCVImageBufferColorPrimariesKey]
            if (colorPrimaries != null) {
                val colorSpaceProperties: MutableMap<String, Any> = mutableMapOf(
                    kCVImageBufferColorPrimariesKey to colorPrimaries
                )
                val yCbCrMatrix = formatDescriptionExtension[kCVImageBufferYCbCrMatrixKey]
                if (yCbCrMatrix != null) {
                    colorSpaceProperties[kCVImageBufferYCbCrMatrixKey] = yCbCrMatrix
                }
                val transferFunction = formatDescriptionExtension[kCVImageBufferTransferFunctionKey]
                if (transferFunction != null) {
                    colorSpaceProperties[kCVImageBufferTransferFunctionKey] = transferFunction
                }
                pixelBufferAttributes[kCVBufferPropagatedAttachmentsKey] = colorSpaceProperties
            }
            val colorSpace = formatDescriptionExtension[kCVImageBufferCGColorSpaceKey]
            if (colorSpace != null) {
                poolColorSpace = colorSpace as? CGColorSpace
            } else if (colorPrimaries is String) {
                if (colorPrimaries == kCVImageBufferColorPrimaries_P3_D65) {
                    poolColorSpace = CGColorSpace(CGColorSpace.displayP3)
                } else if (formatDescriptionExtension[kCVImageBufferLogTransferFunctionKey] as? String ==
                    kCVImageBufferLogTransferFunction_AppleLog
                ) {
                    poolColorSpace = CGColorSpace(CGColorSpace.itur_2020)
                }
            }
        }
        poolFormatDescriptionExtension = formatDescriptionExtension
        pool = com.moblin.android.platform.video.swapPool(pool, null)
        pool = CVPixelBufferPoolCreate(pixelBufferAttributes)?.also { it.name = "effects" }
        return pool
    }

    private fun createPixelBuffer(sampleBuffer: MediaSample): CVPixelBuffer? {
        val formatDescription = sampleBuffer.format ?: return null
        val pool = getBufferPool(formatDescription) ?: return null
        return CVPixelBufferPoolCreatePixelBuffer(pool)
    }

    private fun getBlackImage(width: Double, height: Double): CIImage {
        var image = blackImage
        if (image == null) {
            image = createBlackImage(width = width, height = height)
            blackImage = image
        }
        return image
    }

    private fun scaleImage(image: CIImage): CIImage {
        val scaleFactor = calcScaleFactor(image.extent.size)
        val x = (canvasSize.width - image.extent.width * scaleFactor) / 2
        val y = (canvasSize.height - image.extent.height * scaleFactor) / 2
        return image
            .transformed(by = CGAffineTransform(scaleX = scaleFactor, y = scaleFactor))
            .transformed(by = CGAffineTransform(translationX = x, y = y))
            .cropped(to = CGRect(x = 0.0, y = 0.0, width = canvasSize.width, height = canvasSize.height))
            .composited(
                over = getBlackImage(
                    width = canvasSize.width,
                    height = canvasSize.height
                )
            )
    }

    private fun getBlackImageMetalPetal(size: CGSize): MTIImage {
        var image = blackImageMetalPetal
        if (image == null) {
            image = MTIImage(color = MTIColor.black, sRGB = false, size = size)
            blackImageMetalPetal = image
        }
        return image
    }

    private fun calcScaleFactor(size: CGSize): Double {
        val imageRatio = size.height / size.width
        val canvasRatio = canvasSize.height / canvasSize.width
        return if ((fillFrame && (canvasRatio < imageRatio)) || (!fillFrame && (canvasRatio > imageRatio))) {
            canvasSize.width / size.width
        } else {
            canvasSize.height / size.height
        }
    }

    private fun scaleImageMetalPetal(image: MTIImage, rotation: Double): MTIImage {
        val shape = MetalPetalWidgetShape(contentRegion = image.extent)
        shape.rotation = rotation
        val scaleFactor = calcScaleFactor(shape.rotated(image.size))
        val size = CGSize(width = image.size.width * scaleFactor, height = image.size.height * scaleFactor)
        val position = CGPoint(x = canvasSize.width / 2, y = canvasSize.height / 2)
        val filter = MTIMultilayerCompositingFilter()
        filter.inputBackgroundImage = getBlackImageMetalPetal(canvasSize)
        filter.layers = listOf(
            MTILayer(
                content = image,
                contentFlipOptions = if (mirror) shape.mirrorFlipOptions() else MTILayer.FlipOptions.donotFlip,
                position = position,
                size = size,
                rotation = shape.rotationRadians()
            ),
        )
        return filter.outputImage ?: image
    }

    private fun rotateCoreImage(image: CIImage, rotation: Double): CIImage {
        return when (rotation) {
            90.0 -> image.oriented(CGImagePropertyOrientation.right)
            180.0 -> image.oriented(CGImagePropertyOrientation.down)
            270.0 -> image.oriented(CGImagePropertyOrientation.left)
            else -> image
        }
    }

    private fun mirrorCoreImage(image: CIImage): CIImage {
        return image
            .transformed(by = CGAffineTransform(scaleX = -1.0, y = 1.0))
            .transformed(by = CGAffineTransform(translationX = image.extent.width, y = 0.0))
    }

    private fun applyEffectsCoreImage(
        imageBuffer: CVImageBuffer,
        sampleBuffer: MediaSample,
        enabledEffects: List<VideoEffect>,
        isSceneSwitchTransition: Boolean,
        videoOrientation: Int,
        info: VideoEffectInfo
    ): Pair<CVImageBuffer?, MediaSample?> {
        var image = CIImage(cvPixelBuffer = imageBuffer)
        val originalImage = image
        if (videoOrientation != AVCaptureVideoOrientation.portrait && imageBuffer.isPortrait()) {
            image = image.oriented(CGImagePropertyOrientation.left)
        }
        image = rotateCoreImage(image, rotation)
        if (mirror) {
            image = mirrorCoreImage(image)
        }
        if (image.extent.size != canvasSize) {
            image = scaleImage(image)
        }
        val extent = image.extent
        if (isSceneSwitchTransition) {
            image = applySceneSwitchTransition(image)
        }
        for (effect in enabledEffects) {
            val effectOutputImage = effect.execute(image, info)
            if (effectOutputImage.extent == extent) {
                image = effectOutputImage
            }
        }
        if (image === originalImage) {
            return Pair(null, null)
        }
        val outputImageBuffer = createPixelBuffer(sampleBuffer) ?: return Pair(null, null)
        val colorSpace = poolColorSpace
        if (colorSpace != null) {
            context.render(image, to = outputImageBuffer, bounds = extent, colorSpace = colorSpace)
        } else {
            context.render(image, to = outputImageBuffer)
        }
        val formatDescription = CMVideoFormatDescriptionCreateForImageBuffer(outputImageBuffer)
        val outputSampleBuffer = create(
            outputImageBuffer,
            formatDescription,
            sampleBuffer.durationUs,
            sampleBuffer.presentationTimeUs,
            sampleBuffer.decodeTimeStampUs
        ) ?: return Pair(null, null)
        return Pair(outputImageBuffer, outputSampleBuffer)
    }

    private fun applyEffectsMetalPetal(
        imageBuffer: CVImageBuffer,
        sampleBuffer: MediaSample,
        enabledEffects: List<VideoEffect>,
        isSceneSwitchTransition: Boolean,
        videoOrientation: Int,
        info: VideoEffectInfo
    ): Pair<CVImageBuffer?, MediaSample?> {
        val imageBufferImage: MTIImage? = MTIImage(cvPixelBuffer = imageBuffer, alphaType = MTIAlphaType.alphaIsOne)
        val originalImage = imageBufferImage
        var image = imageBufferImage ?: return Pair(null, null)
        var rotation = this.rotation
        if (videoOrientation != AVCaptureVideoOrientation.portrait && imageBuffer.isPortrait()) {
            rotation = (rotation + 270) % 360
        }
        if (image.size != canvasSize || rotation != 0.0 || mirror) {
            image = scaleImageMetalPetal(image, rotation)
        }
        if (isSceneSwitchTransition) {
            image = applySceneSwitchTransitionMetalPetal(image)
        }
        for (effect in enabledEffects) {
            image = effect.executeMetalPetal(image, info)
        }
        if (image === originalImage) {
            return Pair(null, null)
        }
        val outputImageBuffer = createPixelBuffer(sampleBuffer) ?: return Pair(null, null)
        try {
            metalPetalContext?.render(image, to = outputImageBuffer)
        } catch (error: Throwable) {
            Log.i("VideoEffectsProcessor", "video-unit: Metal petal error: $error")
            return Pair(null, null)
        }
        val formatDescription = CMVideoFormatDescriptionCreateForImageBuffer(outputImageBuffer)
        val outputSampleBuffer = create(
            outputImageBuffer,
            formatDescription,
            sampleBuffer.durationUs,
            sampleBuffer.presentationTimeUs,
            sampleBuffer.decodeTimeStampUs
        ) ?: return Pair(null, null)
        return Pair(outputImageBuffer, outputSampleBuffer)
    }

    private fun calcBlurRadius(): Float {
        val latest = latestSampleBufferTime
        if (latest != null) {
            val offset = ContinuousClock.now - latest
            return if (sceneSwitchTransitionName() == "blurandzoom") {
                0f + minOf(offset.toDouble(DurationUnit.SECONDS).toFloat(), 5f) * 5
            } else {
                15f + minOf(offset.toDouble(DurationUnit.SECONDS).toFloat(), 2f) * 15
            }
        }
        return 25f
    }

    private fun calcBlurScale(): Double {
        val latest = latestSampleBufferTime
        if (latest != null) {
            val offset = ContinuousClock.now - latest
            return 1.0 - minOf(offset.toDouble(DurationUnit.SECONDS), 5.0) * 0.05
        }
        return 0.75
    }

    private fun applySceneSwitchTransition(image: CIImage): CIImage {
        return when (sceneSwitchTransitionName()) {
            "blur" -> {
                val filter = CIFilter.gaussianBlur()
                filter.inputImage = image
                filter.radius = calcBlurRadius() * (image.extent.size.maximum() / 1920.0).toFloat()
                filter.outputImage?.cropped(to = image.extent) ?: image
            }
            "freeze" -> image
            "blurandzoom" -> {
                val filter = CIFilter.gaussianBlur()
                filter.inputImage = image
                filter.radius = calcBlurRadius() * (image.extent.size.maximum() / 1920.0).toFloat()
                val width = image.extent.width
                val height = image.extent.height
                val cropScaleDownFactor = calcBlurScale()
                val scaleUpFactor = 1 / cropScaleDownFactor
                val smallWidth = width * cropScaleDownFactor
                val smallHeight = height * cropScaleDownFactor
                val smallOffsetX = (width - smallWidth) / 2
                val smallOffsetY = (height - smallHeight) / 2
                filter.outputImage
                    ?.cropped(
                        to = CGRect(
                            x = smallOffsetX,
                            y = smallOffsetY,
                            width = smallWidth,
                            height = smallHeight
                        )
                    )
                    ?.transformed(by = CGAffineTransform(translationX = -smallOffsetX, y = -smallOffsetY))
                    ?.transformed(by = CGAffineTransform(scaleX = scaleUpFactor, y = scaleUpFactor))
                    ?.cropped(to = image.extent)
                    ?: image
            }
            else -> image
        }
    }

    private fun blurMetalPetal(image: MTIImage): MTIImage {
        val filter = MTIMPSGaussianBlurFilter()
        filter.inputImage = image
        filter.radius = calcBlurRadius() * (image.extent.size.maximum() / 1920.0).toFloat()
        return filter.outputImage ?: image
    }

    private fun applySceneSwitchTransitionMetalPetal(image: MTIImage): MTIImage {
        return when (sceneSwitchTransitionName()) {
            "blur" -> blurMetalPetal(image)
            "freeze" -> image
            "blurandzoom" -> {
                val cropScaleDownFactor = calcBlurScale()
                val filter = MTICropFilter()
                filter.inputImage = blurMetalPetal(image)
                filter.cropRegion = MTICropRegion.fractional(
                    CGRect(
                        x = (1 - cropScaleDownFactor) / 2,
                        y = (1 - cropScaleDownFactor) / 2,
                        width = cropScaleDownFactor,
                        height = cropScaleDownFactor
                    )
                )
                filter.scale = (1 / cropScaleDownFactor).toFloat()
                filter.outputImage ?: image
            }
            else -> image
        }
    }

    private fun sceneSwitchTransitionName(): String {
        return sceneSwitchTransition.name.lowercase().replace("_", "")
    }

    private fun sceneSwitchTransitionEntry(name: String): SceneSwitchTransition {
        return SceneSwitchTransition.values().firstOrNull {
            it.name.lowercase().replace("_", "") == name
        } ?: SceneSwitchTransition.values().first()
    }
}
