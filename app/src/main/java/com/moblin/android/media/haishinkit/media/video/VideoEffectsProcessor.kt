package com.moblin.android.media.haishinkit.media.video

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.media.Image
import android.media.MediaFormat
import android.os.SystemClock
import android.util.SizeF
import com.moblin.android.media.MediaSample
import com.moblin.android.various.settings.SettingsGraphicsImplementation
import com.moblin.android.videoeffects.VideoSourceEffect
import java.util.UUID

class VideoEffectsProcessor {
    val context: Any? = null
    private var metalPetalContext: Any? = null
    var canvasSize = SizeF(1920f, 1080f)
    var fillFrame = true
    var sceneSwitchTransition: SceneSwitchTransition = SceneSwitchTransition.Blur
    var latestSampleBufferTime: Long? = null
    private var rotation: Double = 0.0
    private var mirror: Boolean = false
    private var effects: MutableList<VideoEffect> = mutableListOf()
    private var pendingAfterAttachEffects: MutableList<VideoEffect>? = null
    private var pendingAfterAttachRotation: Double? = null
    private var pendingAfterAttachMirror: Boolean? = null
    private var isMetalPetalGraphicsForcedByEffects: Boolean = false
    private var isMetalPetalGraphics: Boolean = false
    private var blackImage: Bitmap? = null
    private var blackImageMetalPetal: Image? = null
    private var pool: Any? = null
    private var poolColorSpace: ColorSpace? = null
    private var poolFormatDescriptionExtension: MediaFormat? = null
    private var previousFaceDetectionTimes: MutableMap<UUID, Double> = mutableMapOf()
    private var previousTextDetectionTimes: MutableMap<UUID, Double> = mutableMapOf()

    init {
        metalPetalContext = null
    }

    fun reset() {
        blackImage = null
        blackImageMetalPetal = null
        pool = null
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
        if (index != -1) {
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
            effects = pendingEffects.toMutableList()
            isMetalPetalGraphicsForcedByEffects = effects.any { it.isMetalPetal() }
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
                false
            } else {
                effect.removed()
                true
            }
        }
    }

    fun needsFaceDetections(presentationTimeStamp: Double, sceneVideoSourceId: UUID): Set<UUID> {
        val detectionsIntervals: MutableMap<UUID, Double> = mutableMapOf()
        val ids: MutableSet<UUID> = mutableSetOf()
        for (effect in getEnabledEffects()) {
            when (val mode = effect.needsFaceDetections(presentationTimeStamp)) {
                is VideoEffectDetectionsMode.Off -> Unit
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
                is VideoEffectDetectionsMode.Off -> Unit
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
        imageBuffer: Image,
        completion: DetectionsCompletion,
        videoUnit: VideoUnit,
        videoOrientation: Int
    ): Pair<Image, MediaSample> {
        val sampleBuffer = completion.sampleBuffer
        if (completion.isFirstAfterAttach) {
            usePendingAfterAttachEffects()
        }
        val enabledEffects = getEnabledEffects()
        val imageSize = SizeF(imageBuffer.width.toFloat(), imageBuffer.height.toFloat())
        if (enabledEffects.isEmpty() &&
            !completion.isSceneSwitchTransition &&
            imageSize != canvasSize &&
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
        imageBuffer: Image,
        enabledEffects: List<VideoEffect>,
        completion: DetectionsCompletion,
        videoUnit: VideoUnit,
        videoOrientation: Int
    ): Pair<Image?, MediaSample?> {
        val sampleBuffer = completion.sampleBuffer
        val info = VideoEffectInfo(
            sceneVideoSourceId = completion.sceneVideoSourceId,
            detectionJobs = completion.detectionJobs,
            detections = completion.detections,
            presentationTimeStamp = (sampleBuffer.presentationTimeUs / 1_000_000.0).toLong(),
            videoUnit = videoUnit,
            isFirstAfterAttach = completion.isFirstAfterAttach
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
        val latest = latestSampleBufferTime ?: return false
        val offset = (SystemClock.elapsedRealtimeNanos() - latest) / 1_000_000_000.0
        return if (sceneSwitchTransition == SceneSwitchTransition.BlurAndZoom) {
            offset >= 5
        } else {
            offset >= 2
        }
    }

    fun renderSceneSwitchTransitionEnd(
        sampleBuffer: MediaSample,
        imageBuffer: Image,
        outputImageBuffer: Image
    ): MediaSample = TODO("OpenGL ES port")

    private fun isMetalPetalGraphicsEnabled(): Boolean {
        return isMetalPetalGraphics || isMetalPetalGraphicsForcedByEffects
    }

    private fun getBufferPool(formatDescription: MediaFormat): Any? {
        return TODO("no Android counterpart for CVPixelBufferPool")
    }

    private fun createPixelBuffer(sampleBuffer: MediaSample): Image? {
        return TODO("no Android counterpart for CVPixelBufferPool")
    }

    private fun getBlackImage(width: Double, height: Double): Bitmap {
        val currentBlackImage = blackImage
        if (currentBlackImage == null) {
            val image = createBlackImage(width, height)
            blackImage = image
            return image
        }
        return currentBlackImage
    }

    private fun scaleImage(image: Image): Image {
        return TODO("OpenGL ES port")
    }

    private fun getBlackImageMetalPetal(size: SizeF): Image {
        return TODO("no Android counterpart for MetalPetal")
    }

    private fun calcScaleFactor(size: SizeF): Double {
        val imageRatio = size.height.toDouble() / size.width.toDouble()
        val canvasRatio = canvasSize.height.toDouble() / canvasSize.width.toDouble()
        return if ((fillFrame && (canvasRatio < imageRatio)) || (!fillFrame && (canvasRatio > imageRatio))) {
            canvasSize.width.toDouble() / size.width.toDouble()
        } else {
            canvasSize.height.toDouble() / size.height.toDouble()
        }
    }

    private fun scaleImageMetalPetal(image: Image, rotation: Double): Image {
        return TODO("no Android counterpart for MetalPetal")
    }

    private fun rotateCoreImage(image: Image, rotation: Double): Image {
        return TODO("OpenGL ES port")
    }

    private fun mirrorCoreImage(image: Image): Image {
        return TODO("OpenGL ES port")
    }

    private fun applyEffectsCoreImage(
        imageBuffer: Image,
        sampleBuffer: MediaSample,
        enabledEffects: List<VideoEffect>,
        isSceneSwitchTransition: Boolean,
        videoOrientation: Int,
        info: VideoEffectInfo
    ): Pair<Image?, MediaSample?> {
        return TODO("OpenGL ES port")
    }

    private fun applyEffectsMetalPetal(
        imageBuffer: Image,
        sampleBuffer: MediaSample,
        enabledEffects: List<VideoEffect>,
        isSceneSwitchTransition: Boolean,
        videoOrientation: Int,
        info: VideoEffectInfo
    ): Pair<Image?, MediaSample?> {
        return TODO("no Android counterpart for MetalPetal")
    }

    private fun calcBlurRadius(): Float {
        val latest = latestSampleBufferTime
        if (latest != null) {
            val offset = (SystemClock.elapsedRealtimeNanos() - latest) / 1_000_000_000.0
            return if (sceneSwitchTransition == SceneSwitchTransition.BlurAndZoom) {
                (0f + minOf(offset, 5.0).toFloat() * 5f)
            } else {
                (15f + minOf(offset, 2.0).toFloat() * 15f)
            }
        }
        return 25f
    }

    private fun calcBlurScale(): Double {
        val latest = latestSampleBufferTime
        if (latest != null) {
            val offset = (SystemClock.elapsedRealtimeNanos() - latest) / 1_000_000_000.0
            return 1.0 - minOf(offset, 5.0) * 0.05
        }
        return 0.75
    }

    private fun applySceneSwitchTransition(image: Image): Image {
        return TODO("OpenGL ES port")
    }

    private fun blurMetalPetal(image: Image): Image {
        return TODO("no Android counterpart for MetalPetal")
    }

    private fun applySceneSwitchTransitionMetalPetal(image: Image): Image {
        return TODO("no Android counterpart for MetalPetal")
    }
}
