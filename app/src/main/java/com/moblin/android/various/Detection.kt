package com.moblin.android.various

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class VNFaceLandmarkRegion2D(
    val normalizedPoints: List<Offset> = emptyList(),
) {
    fun pointsInImage(imageSize: Size): List<Offset> =
        emptyList()
}

class VNFaceLandmarks2D(
    val medianLine: VNFaceLandmarkRegion2D? = null,
    val faceContour: VNFaceLandmarkRegion2D? = null,
    val innerLips: VNFaceLandmarkRegion2D? = null,
    val leftEye: VNFaceLandmarkRegion2D? = null,
    val rightEye: VNFaceLandmarkRegion2D? = null,
    val leftEyebrow: VNFaceLandmarkRegion2D? = null,
    val rightEyebrow: VNFaceLandmarkRegion2D? = null,
)

class VNFaceObservation(
    val landmarks: VNFaceLandmarks2D? = null,
)

fun VNFaceObservation.stableBoundingBox(imageSize: Size, rotationAngle: Double = 0.0): Rect? {
    var allPoints = getFacePoints(imageSize)
    if (rotationAngle != 0.0) {
        allPoints = rotateFace(allPoints, -rotationAngle.toFloat())
    }
    val firstPoint = allPoints.firstOrNull() ?: return null
    var faceMinX = firstPoint.x
    var faceMaxX = firstPoint.x
    var faceMinY = firstPoint.y
    var faceMaxY = firstPoint.y
    for (point in allPoints) {
        faceMinX = min(point.x, faceMinX)
        faceMaxX = max(point.x, faceMaxX)
        faceMinY = min(point.y, faceMinY)
        faceMaxY = max(point.y, faceMaxY)
    }
    val faceWidth = faceMaxX - faceMinX
    val faceHeight = faceMaxY - faceMinY
    return Rect(left = faceMinX, top = faceMinY, right = faceMinX + faceWidth, bottom = faceMinY + faceHeight)
}

fun VNFaceObservation.calcFaceAngle(imageSize: Size): Float? {
    val medianLine = landmarks?.medianLine ?: return null
    val medianLinePoints = medianLine.pointsInImage(imageSize)
    val firstPoint = medianLinePoints.firstOrNull() ?: return null
    val lastPoint = medianLinePoints.lastOrNull() ?: return null
    val deltaX = firstPoint.x - lastPoint.x
    val deltaY = firstPoint.y - lastPoint.y
    return -atan(deltaX / deltaY)
}

fun VNFaceObservation.calcFaceAngleSide(): Float? {
    val landmarks = landmarks ?: return null
    val centerPoint = landmarks.medianLine?.normalizedPoints?.firstOrNull() ?: return null
    val faceContour = landmarks.faceContour?.normalizedPoints ?: return null
    val leftPoint = faceContour.firstOrNull() ?: return null
    val rightPoint = faceContour.lastOrNull() ?: return null
    val leftWidth = max(leftPoint.x - centerPoint.x, 0f)
    val rightWidth = max(centerPoint.x - rightPoint.x, 0f)
    return if (leftWidth < rightWidth) {
        -(1f - leftWidth / rightWidth)
    } else if (leftWidth > rightWidth) {
        1f - rightWidth / leftWidth
    } else {
        0f
    }
}

fun VNFaceObservation.isMouthOpen(rotationAngle: Double, sensitivity: Double): Double {
    val points = landmarks?.innerLips?.normalizedPoints
    if (points != null) {
        val rotatedPoints = rotateFace(points, -rotationAngle.toFloat())
        val boundingBox = calcBoundingBox(rotatedPoints)
        if (boundingBox != null) {
            return min(boundingBox.height * sensitivity * 6, 1.0)
        }
    }
    return 0.0
}

fun VNFaceObservation.isLeftEyeOpen(rotationAngle: Double, sensitivity: Double): Double {
    return isEyeOpen(eye = landmarks?.leftEye, rotationAngle = rotationAngle, sensitivity = sensitivity)
}

fun VNFaceObservation.isRightEyeOpen(rotationAngle: Double, sensitivity: Double): Double {
    return isEyeOpen(eye = landmarks?.rightEye, rotationAngle = rotationAngle, sensitivity = sensitivity)
}

private fun VNFaceObservation.isEyeOpen(eye: VNFaceLandmarkRegion2D?, rotationAngle: Double,
                                        sensitivity: Double): Double
{
    val points = eye?.normalizedPoints
    if (points != null && points.size == 6) {
        val rotatedPoints = rotateFace(points, -rotationAngle.toFloat())
        val height = rotatedPoints[1].y - rotatedPoints[5].y
        return if (height * sensitivity > 0.015) 1.0 else 0.0
    }
    return 1.0
}

private fun VNFaceObservation.getFacePoints(imageSize: Size): List<Offset> {
    val points = mutableListOf<Offset>()
    points += landmarks?.medianLine?.pointsInImage(imageSize) ?: emptyList()
    points += landmarks?.leftEyebrow?.pointsInImage(imageSize) ?: emptyList()
    points += landmarks?.rightEyebrow?.pointsInImage(imageSize) ?: emptyList()
    return points
}

fun rotateFace(allPoints: List<Offset>, rotationAngle: Float): List<Offset> {
    return allPoints.map { rotatePoint(point = it, alpha = rotationAngle) }
}

fun rotatePoint(point: Offset, alpha: Float): Offset {
    val z = sqrt(point.x.pow(2) + point.y.pow(2))
    val beta = atan2(point.y, point.x)
    return Offset(z * cos(alpha + beta), z * sin(alpha + beta))
}

fun calcBoundingBox(points: List<Offset>): Rect? {
    val firstPoint = points.firstOrNull() ?: return null
    var minX = firstPoint.x
    var maxX = firstPoint.x
    var minY = firstPoint.y
    var maxY = firstPoint.y
    for (point in points) {
        minX = min(point.x, minX)
        maxX = max(point.x, maxX)
        minY = min(point.y, minY)
        maxY = max(point.y, maxY)
    }
    val width = maxX - minX
    val height = maxY - minY
    return Rect(left = minX, top = maxY, right = minX + width, bottom = maxY + height)
}
