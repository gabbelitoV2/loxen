package com.moblin.android.media.haishinkit.media.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import com.moblin.android.media.MediaSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)
private val globalScope = CoroutineScope(Dispatchers.Default)

class VideoSnapshots(private val context: Context) {
    private var cleanSnapshots = false
    private var takeSnapshotAge: Float = 0.0f
    private var takeSnapshotComplete: ((Bitmap, Bitmap, Bitmap) -> Unit)? = null
    private val takeSnapshotSampleBuffers = ArrayDeque<MediaSample>()

    fun setCleanSnapshots(enabled: Boolean) {
        cleanSnapshots = enabled
    }

    fun takeSnapshot(age: Float, onComplete: (Bitmap, Bitmap, Bitmap) -> Unit) {
        takeSnapshotAge = age
        takeSnapshotComplete = onComplete
    }

    fun takeVideoSourceSnapshot(imageBuffer: Image, onComplete: (Bitmap?) -> Unit) {
        val bitmap = createBitmap(imageBuffer)
        if (bitmap == null) {
            mainScope.launch { onComplete(null) }
            return
        }
        mainScope.launch { onComplete(bitmap) }
    }

    fun handleTakeSnapshot(
        cleanSampleBuffer: MediaSample,
        modSampleBuffer: MediaSample,
        presentationTimeStamp: Double,
        makeCopy: (MediaSample) -> MediaSample?,
    ) {
        val sampleBuffer = if (cleanSnapshots) cleanSampleBuffer else modSampleBuffer
        val latestPresentationTimeStamp = takeSnapshotSampleBuffers.lastOrNull()
            ?.let { it.presentationTimeUs / 1_000_000.0 } ?: 0.0
        if (presentationTimeStamp > latestPresentationTimeStamp + 3.0) {
            val copy = makeCopy(sampleBuffer) ?: return
            takeSnapshotSampleBuffers.addLast(copy)
            if (takeSnapshotSampleBuffers.size > 3) {
                takeSnapshotSampleBuffers.removeFirst()
            }
        }
        val complete = takeSnapshotComplete ?: return
        val copy = makeCopy(sampleBuffer) ?: return
        val sampleBuffers = ArrayDeque(takeSnapshotSampleBuffers)
        val age = takeSnapshotAge
        globalScope.launch {
            takeSnapshot(copy, sampleBuffers, presentationTimeStamp, age, complete)
        }
        takeSnapshotComplete = null
    }

    private fun findBestSnapshot(
        sampleBuffer: MediaSample,
        sampleBuffers: ArrayDeque<MediaSample>,
        presentationTimeStamp: Double,
        age: Float,
        onCompleted: (Image?) -> Unit,
    ) {
        if (age == 0.0f) {
            mainScope.launch {
                onCompleted(imageBufferOf(sampleBuffer))
            }
        } else {
            val requestedPresentationTimeStamp = presentationTimeStamp - age.toDouble()
            val sampleBufferAtAge = sampleBuffers.lastOrNull {
                it.presentationTimeUs / 1_000_000.0 <= requestedPresentationTimeStamp
            } ?: sampleBuffers.firstOrNull() ?: sampleBuffer
            val allSampleBuffers = ArrayDeque(sampleBuffers)
            allSampleBuffers.addLast(sampleBuffer)
            findBestSnapshotUsingAesthetics(sampleBufferAtAge, allSampleBuffers, onCompleted)
        }
    }

    private fun findBestSnapshotUsingAesthetics(
        preferredSampleBuffer: MediaSample,
        sampleBuffers: ArrayDeque<MediaSample>,
        onComplete: (Image?) -> Unit,
    ) {
        globalScope.launch {
            var bestSampleBuffer = preferredSampleBuffer
            var bestResult = runCatching { imageAestheticsScore(preferredSampleBuffer) }.getOrNull()
            for (sampleBuffer in sampleBuffers) {
                val result = runCatching { imageAestheticsScore(sampleBuffer) }.getOrNull() ?: continue
                val currentBest = bestResult
                if (currentBest == null || result > currentBest + 0.2f) {
                    bestSampleBuffer = sampleBuffer
                    bestResult = result
                }
            }
            mainScope.launch {
                onComplete(imageBufferOf(bestSampleBuffer))
            }
        }
    }

    private suspend fun imageAestheticsScore(sampleBuffer: MediaSample): Float =
        TODO("no Android counterpart for Vision CalculateImageAestheticsScoresRequest")

    private fun takeSnapshot(
        sampleBuffer: MediaSample,
        sampleBuffers: ArrayDeque<MediaSample>,
        presentationTimeStamp: Double,
        age: Float,
        onComplete: (Bitmap, Bitmap, Bitmap) -> Unit,
    ) {
        findBestSnapshot(sampleBuffer, sampleBuffers, presentationTimeStamp, age) { imageBuffer ->
            if (imageBuffer == null) {
                return@findBestSnapshot
            }
            val image = createBitmap(imageBuffer) ?: return@findBestSnapshot
            var portraitImage = image
            if (!imageBuffer.isPortrait()) {
                portraitImage = orientedLeft(portraitImage)
            }
            onComplete(image, image, portraitImage)
        }
    }

    private fun imageBufferOf(sampleBuffer: MediaSample): Image? =
        TODO("MediaSample holds encoded frames; decoding to android.media.Image needs a MediaCodec decoder")

    private fun createBitmap(imageBuffer: Image): Bitmap? =
        TODO("Core Image CIContext.createCGImage has no Android counterpart")

    private fun orientedLeft(source: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(-90.0f)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
