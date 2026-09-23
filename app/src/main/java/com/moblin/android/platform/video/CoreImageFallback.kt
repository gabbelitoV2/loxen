package com.moblin.android.platform.video

import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.avfoundation.AVCaptureVideoOrientation

private const val TAG = "MoblinPipeline"

object CoreImageFallback {
    private var canvasPool: CVPixelBufferPool? = null
    private val skippedEffects: MutableSet<String> = mutableSetOf()
    private var isSceneSwitchTransitionLogged = false

    @Suppress("UNUSED_PARAMETER")
    fun applyEffects(
        imageBuffer: CVPixelBuffer,
        sampleBuffer: MediaSample,
        enabledEffects: List<VideoEffect>,
        isSceneSwitchTransition: Boolean,
        videoOrientation: Int,
        info: VideoEffectInfo,
        canvasSize: com.moblin.android.platform.coregraphics.CGSize,
        fillFrame: Boolean,
        rotation: Double,
        mirror: Boolean,
    ): Pair<CVPixelBuffer?, MediaSample?> {
        logSkipped(enabledEffects, isSceneSwitchTransition)
        var rotationCw = 0
        if (videoOrientation != AVCaptureVideoOrientation.portrait && imageBuffer.isPortrait()) {
            rotationCw = 270
        }
        rotationCw = (rotationCw + rotationToDegreesCw(rotation)) % 360
        val canvasWidth = canvasSize.width.toInt()
        val canvasHeight = canvasSize.height.toInt()
        val isQuarterTurn = rotationCw == 90 || rotationCw == 270
        val orientedWidth = if (isQuarterTurn) imageBuffer.height else imageBuffer.width
        val orientedHeight = if (isQuarterTurn) imageBuffer.width else imageBuffer.height
        val isScaled = orientedWidth != canvasWidth || orientedHeight != canvasHeight
        if (rotationCw == 0 && !mirror && !isScaled) {
            return Pair(null, null)
        }
        if (canvasWidth <= 0 || canvasHeight <= 0) {
            return Pair(null, null)
        }
        val outputImageBuffer = getCanvasPool(canvasWidth, canvasHeight, imageBuffer.pixelFormatType)
            .createPixelBuffer() ?: return Pair(null, null)
        val mode = if (fillFrame) GlRenderer.ScalingMode.fill else GlRenderer.ScalingMode.fit
        GlRenderer.bind(outputImageBuffer)
        GlRenderer.draw(imageBuffer, canvasWidth, canvasHeight, mode, rotationCw, mirror, true)
        GlRenderer.bind(null)
        return Pair(outputImageBuffer, makeSampleBuffer(sampleBuffer, outputImageBuffer))
    }

    fun renderSceneSwitchTransitionEnd(
        sampleBuffer: MediaSample,
        imageBuffer: CVPixelBuffer,
        outputImageBuffer: CVPixelBuffer,
    ): MediaSample {
        VTPixelTransferSessionTransferImage(imageBuffer, outputImageBuffer)
        return makeSampleBuffer(sampleBuffer, outputImageBuffer)
    }

    private fun rotationToDegreesCw(rotation: Double): Int {
        return when (rotation) {
            90.0 -> 90
            180.0 -> 180
            270.0 -> 270
            else -> 0
        }
    }

    private fun getCanvasPool(width: Int, height: Int, pixelFormatType: Int): CVPixelBufferPool {
        val pool = canvasPool
        if (pool != null && pool.width == width && pool.height == height) {
            return pool
        }
        val newPool = CVPixelBufferPool(width, height, pixelFormatType, 8)
        newPool.name = "canvas"
        canvasPool = newPool
        Log.i(TAG, "Canvas pool ${width}x$height")
        return newPool
    }

    private fun makeSampleBuffer(sampleBuffer: MediaSample, imageBuffer: CVPixelBuffer): MediaSample {
        return MediaSample(
            data = ByteArray(0),
            presentationTimeUs = sampleBuffer.presentationTimeUs,
            isKeyFrame = true,
            format = CMVideoFormatDescriptionCreateForImageBuffer(imageBuffer),
            imageBuffer = imageBuffer,
            durationUs = sampleBuffer.durationUs,
            decodeTimeStampUs = sampleBuffer.decodeTimeStampUs,
        )
    }

    private fun logSkipped(enabledEffects: List<VideoEffect>, isSceneSwitchTransition: Boolean) {
        for (effect in enabledEffects) {
            val name = effect.javaClass.simpleName
            if (skippedEffects.add(name)) {
                Log.i(TAG, "effect $name not ported, skipped")
            }
        }
        if (isSceneSwitchTransition && !isSceneSwitchTransitionLogged) {
            isSceneSwitchTransitionLogged = true
            Log.i(TAG, "Scene switch transition not ported, frame frozen without blur")
        }
    }
}
