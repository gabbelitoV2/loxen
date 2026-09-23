package com.moblin.android.various

import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.vision.VNFaceLandmarkRegion2D
import com.moblin.android.platform.vision.VNFaceObservation
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

fun VNFaceObservation.stableBoundingBox(imageSize: CGSize, rotationAngle: Double = 0.0): CGRect? {
    var allPoints = getFacePoints(imageSize = imageSize)
    if (rotationAngle != 0.0) {
        allPoints = rotateFace(allPoints = allPoints, rotationAngle = -rotationAngle)
    }
    val firstPoint = allPoints.firstOrNull() ?: return null
    var faceMinX = firstPoint.x
    var faceMaxX = firstPoint.x
    var faceMinY = firstPoint.y
    var faceMaxY = firstPoint.y
    for (point in allPoints) {
        faceMinX = minOf(point.x, faceMinX)
        faceMaxX = maxOf(point.x, faceMaxX)
        faceMinY = minOf(point.y, faceMinY)
        faceMaxY = maxOf(point.y, faceMaxY)
    }
    val faceWidth = faceMaxX - faceMinX
    val faceHeight = faceMaxY - faceMinY
    return CGRect(x = faceMinX, y = faceMinY, width = faceWidth, height = faceHeight)
}

fun VNFaceObservation.calcFaceAngle(imageSize: CGSize): Double? {
    val medianLine = landmarks?.medianLine ?: return null
    val medianLinePoints = medianLine.pointsInImage(imageSize = imageSize)
    val firstPoint = medianLinePoints.firstOrNull() ?: return null
    val lastPoint = medianLinePoints.lastOrNull() ?: return null
    val deltaX = firstPoint.x - lastPoint.x
    val deltaY = firstPoint.y - lastPoint.y
    return -atan(deltaX / deltaY)
}

fun VNFaceObservation.calcFaceAngleSide(): Double? {
    val landmarks = landmarks ?: return null
    val centerPoint = landmarks.medianLine?.normalizedPoints?.firstOrNull() ?: return null
    val faceContour = landmarks.faceContour?.normalizedPoints ?: return null
    val leftPoint = faceContour.firstOrNull() ?: return null
    val rightPoint = faceContour.lastOrNull() ?: return null
    val leftWidth = maxOf(leftPoint.x - centerPoint.x, 0.0)
    val rightWidth = maxOf(centerPoint.x - rightPoint.x, 0.0)
    if (leftWidth < rightWidth) {
        return -(1 - leftWidth / rightWidth)
    } else if (leftWidth > rightWidth) {
        return 1 - rightWidth / leftWidth
    } else {
        return 0.0
    }
}

fun VNFaceObservation.isMouthOpen(rotationAngle: Double, sensitivity: Double): Double {
    val points = landmarks?.innerLips?.normalizedPoints
    if (points != null) {
        val rotatedPoints = rotateFace(allPoints = points, rotationAngle = -rotationAngle)
        val boundingBox = calcBoundingBox(points = rotatedPoints)
        if (boundingBox != null) {
            return minOf(boundingBox.height * sensitivity * 6, 1.0)
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
        val rotatedPoints = rotateFace(allPoints = points, rotationAngle = -rotationAngle)
        val height = rotatedPoints[1].y - rotatedPoints[5].y
        return if (height * sensitivity > 0.015) 1.0 else 0.0
    }
    return 1.0
}

private fun VNFaceObservation.getFacePoints(imageSize: CGSize): List<CGPoint> {
    val points = mutableListOf<CGPoint>()
    points += landmarks?.medianLine?.pointsInImage(imageSize = imageSize) ?: emptyList()
    points += landmarks?.leftEyebrow?.pointsInImage(imageSize = imageSize) ?: emptyList()
    points += landmarks?.rightEyebrow?.pointsInImage(imageSize = imageSize) ?: emptyList()
    return points
}

fun rotateFace(allPoints: List<CGPoint>, rotationAngle: Double): List<CGPoint> {
    return allPoints.map { rotatePoint(point = it, alpha = rotationAngle) }
}

fun rotatePoint(point: CGPoint, alpha: Double): CGPoint {
    val z = sqrt(point.x.pow(2) + point.y.pow(2))
    val beta = atan2(point.y, point.x)
    return CGPoint(x = z * cos(alpha + beta), y = z * sin(alpha + beta))
}

fun calcBoundingBox(points: List<CGPoint>): CGRect? {
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
    val width = maxX - minX
    val height = maxY - minY
    return CGRect(x = minX, y = maxY, width = width, height = height)
}
