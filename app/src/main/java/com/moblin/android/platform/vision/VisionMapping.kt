package com.moblin.android.platform.vision

import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import kotlin.math.max
import kotlin.math.roundToInt

internal object VisionMapping {
    const val MESH_LANDMARK_COUNT = 468
    const val ANALYSIS_LONG_SIDE = 640
    const val RIGHT_IRIS_CENTER = 468
    const val LEFT_IRIS_CENTER = 473

    val faceOval = intArrayOf(
        10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288, 397, 365, 379, 378, 400, 377,
        152, 148, 176, 149, 150, 136, 172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109,
    )
    val faceContour = intArrayOf(
        356, 454, 323, 361, 288, 397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136, 172, 58, 132, 93,
        234, 127,
    )
    val leftEye = intArrayOf(362, 385, 387, 263, 373, 380)
    val rightEye = intArrayOf(33, 160, 158, 133, 153, 144)
    val leftEyebrow = intArrayOf(336, 296, 334, 293, 300, 276)
    val rightEyebrow = intArrayOf(46, 70, 63, 105, 66, 107)
    val nose = intArrayOf(168, 122, 129, 64, 98, 97, 2, 326, 327, 294, 358, 351)
    val noseCrest = intArrayOf(168, 6, 197, 195, 5, 4)
    val medianLine = intArrayOf(9, 8, 168, 6, 197, 195, 4, 2, 0, 17, 200, 152)
    val outerLips = intArrayOf(
        61, 185, 40, 39, 37, 0, 267, 269, 270, 409, 291, 375, 321, 405, 314, 17, 84, 181, 91, 146,
    )
    val innerLips = intArrayOf(
        78, 191, 80, 81, 82, 13, 312, 311, 310, 415, 308, 324, 318, 402, 317, 14, 87, 178, 88, 95,
    )

    fun analysisSize(width: Int, height: Int, maxLongSide: Int = ANALYSIS_LONG_SIDE): Pair<Int, Int> {
        val longSide = max(width, height)
        if (width <= 0 || height <= 0 || longSide <= maxLongSide) {
            return Pair(max(width, 0), max(height, 0))
        }
        val scale = maxLongSide.toDouble() / longSide
        return Pair(max(1, (width * scale).roundToInt()), max(1, (height * scale).roundToInt()))
    }

    fun faceObservation(xs: FloatArray, ys: FloatArray, count: Int): VNFaceObservation? {
        if (count < MESH_LANDMARK_COUNT || xs.size < count || ys.size < count) {
            return null
        }
        val visionX = DoubleArray(count) { xs[it].toDouble() }
        val visionY = DoubleArray(count) { 1.0 - ys[it].toDouble() }
        for (index in 0 until count) {
            if (visionX[index].isNaN() || visionY[index].isNaN()) {
                return null
            }
        }
        var minX = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        for (index in faceOval) {
            minX = minOf(minX, visionX[index])
            maxX = maxOf(maxX, visionX[index])
            minY = minOf(minY, visionY[index])
            maxY = maxOf(maxY, visionY[index])
        }
        val width = maxX - minX
        val height = maxY - minY
        if (!(width > 0.0) || !(height > 0.0)) {
            return null
        }
        val boundingBox = CGRect(minX, minY, width, height)
        fun normalized(x: Double, y: Double): CGPoint {
            return CGPoint((x - minX) / width, (y - minY) / height)
        }
        fun region(indices: IntArray): VNFaceLandmarkRegion2D {
            return VNFaceLandmarkRegion2D(
                normalizedPoints = indices.map { normalized(visionX[it], visionY[it]) },
                faceBoundingBox = boundingBox
            )
        }
        fun pupil(center: Int, eye: IntArray): VNFaceLandmarkRegion2D {
            if (center < count) {
                return region(intArrayOf(center))
            }
            val x = eye.sumOf { visionX[it] } / eye.size
            val y = eye.sumOf { visionY[it] } / eye.size
            return VNFaceLandmarkRegion2D(normalizedPoints = listOf(normalized(x, y)), faceBoundingBox = boundingBox)
        }
        val leftPupil = pupil(LEFT_IRIS_CENTER, leftEye)
        val rightPupil = pupil(RIGHT_IRIS_CENTER, rightEye)
        val regions = listOf(
            faceContour, leftEyebrow, rightEyebrow, leftEye, rightEye, nose, noseCrest, medianLine, outerLips,
            innerLips,
        )
        val allIndices = LinkedHashSet<Int>()
        for (indices in regions) {
            for (index in indices) {
                allIndices.add(index)
            }
        }
        val allPoints = VNFaceLandmarkRegion2D(
            normalizedPoints = allIndices.map { normalized(visionX[it], visionY[it]) } +
                leftPupil.normalizedPoints + rightPupil.normalizedPoints,
            faceBoundingBox = boundingBox
        )
        val landmarks = VNFaceLandmarks2D(
            confidence = 1f,
            allPoints = allPoints,
            faceContour = region(faceContour),
            leftEye = region(leftEye),
            rightEye = region(rightEye),
            leftEyebrow = region(leftEyebrow),
            rightEyebrow = region(rightEyebrow),
            nose = region(nose),
            noseCrest = region(noseCrest),
            medianLine = region(medianLine),
            outerLips = region(outerLips),
            innerLips = region(innerLips),
            leftPupil = leftPupil,
            rightPupil = rightPupil
        )
        return VNFaceObservation(boundingBox = boundingBox, landmarks = landmarks)
    }

    fun textObservation(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        imageWidth: Int,
        imageHeight: Int,
        string: String,
        confidence: Float,
        minimumTextHeight: Float,
    ): VNRecognizedTextObservation? {
        if (imageWidth <= 0 || imageHeight <= 0) {
            return null
        }
        val clampedLeft = left.coerceIn(0, imageWidth)
        val clampedRight = right.coerceIn(0, imageWidth)
        val clampedTop = top.coerceIn(0, imageHeight)
        val clampedBottom = bottom.coerceIn(0, imageHeight)
        if (clampedRight <= clampedLeft || clampedBottom <= clampedTop) {
            return null
        }
        val width = imageWidth.toDouble()
        val height = imageHeight.toDouble()
        val boundingBox = CGRect(
            clampedLeft / width,
            1.0 - clampedBottom / height,
            (clampedRight - clampedLeft) / width,
            (clampedBottom - clampedTop) / height
        )
        if (boundingBox.height < minimumTextHeight) {
            return null
        }
        val textConfidence = if (confidence.isNaN()) 0f else confidence.coerceIn(0f, 1f)
        return VNRecognizedTextObservation(
            boundingBox = boundingBox,
            candidates = listOf(VNRecognizedText(string, textConfidence)),
            confidence = textConfidence
        )
    }
}
