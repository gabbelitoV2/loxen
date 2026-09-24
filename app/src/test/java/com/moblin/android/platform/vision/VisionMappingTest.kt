package com.moblin.android.platform.vision

import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.various.calcFaceAngle
import com.moblin.android.various.calcFaceAngleSide
import com.moblin.android.various.isLeftEyeOpen
import com.moblin.android.various.isMouthOpen
import com.moblin.android.various.isRightEyeOpen
import com.moblin.android.various.stableBoundingBox
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume
import org.junit.Test

class VisionMappingTest {
    private val imageSize = CGSize(1920.0, 1080.0)

    private val mesh: List<DoubleArray> by lazy {
        val stream = javaClass.classLoader!!.getResourceAsStream("canonical_face_model.obj")!!
        stream.bufferedReader().readLines()
            .filter { it.startsWith("v ") }
            .map { line -> line.split(" ").filter { it.isNotEmpty() }.drop(1).take(3).map { it.toDouble() }.toDoubleArray() }
    }

    private class Landmarks(val xs: FloatArray, val ys: FloatArray)

    private fun project(
        rollDegrees: Double = 0.0,
        yawDegrees: Double = 0.0,
        modify: (Int, DoubleArray) -> DoubleArray = { _, point -> point },
    ): Landmarks {
        val yaw = yawDegrees * PI / 180
        val roll = rollDegrees * PI / 180
        val scale = 0.4 * imageSize.height / 18
        val count = 478
        val xs = FloatArray(count)
        val ys = FloatArray(count)
        val points = mesh.mapIndexed { index, vertex -> modify(index, vertex.copyOf()) }
        for ((index, point) in points.withIndex()) {
            val yawedX = point[0] * cos(yaw) + point[2] * sin(yaw)
            val rolledX = yawedX * cos(roll) - point[1] * sin(roll)
            val rolledY = yawedX * sin(roll) + point[1] * cos(roll)
            val pixelX = imageSize.width / 2 + scale * rolledX
            val pixelY = imageSize.height / 2 + scale * rolledY
            xs[index] = (pixelX / imageSize.width).toFloat()
            ys[index] = (1 - pixelY / imageSize.height).toFloat()
        }
        fun center(indices: IntArray, target: IntRange) {
            val x = indices.map { xs[it].toDouble() }.average().toFloat()
            val y = indices.map { ys[it].toDouble() }.average().toFloat()
            for (index in target) {
                xs[index] = x
                ys[index] = y
            }
        }
        center(intArrayOf(33, 133, 159, 145), 468..472)
        center(intArrayOf(263, 362, 386, 374), 473..477)
        return Landmarks(xs, ys)
    }

    private fun observation(landmarks: Landmarks, count: Int = 478): VNFaceObservation {
        return assertNotNull(VisionMapping.faceObservation(landmarks.xs, landmarks.ys, count))
    }

    private fun centerX(points: List<CGPoint>): Double {
        return points.sumOf { it.x } / points.size
    }

    private fun centerY(points: List<CGPoint>): Double {
        return points.sumOf { it.y } / points.size
    }

    @Test
    fun eyesHaveVisionPointOrder() {
        val landmarks = observation(project()).landmarks!!
        for (eye in listOf(landmarks.leftEye!!, landmarks.rightEye!!)) {
            val points = eye.normalizedPoints
            assertEquals(6, eye.pointCount)
            assertTrue(points[0].x < points[1].x && points[1].x < points[2].x && points[2].x < points[3].x)
            assertTrue(points[5].x < points[4].x)
            assertTrue(points[1].y > points[0].y && points[2].y > points[3].y)
            assertTrue(points[5].y < points[0].y && points[4].y < points[3].y)
            assertTrue(abs(points[1].x - points[5].x) < 0.01)
            assertTrue(abs(points[2].x - points[4].x) < 0.01)
        }
    }

    @Test
    fun leftAndRightAreTheSubjectsSides() {
        val landmarks = observation(project()).landmarks!!
        val middle = centerX(landmarks.medianLine!!.normalizedPoints)
        assertTrue(centerX(landmarks.leftEye!!.normalizedPoints) > middle)
        assertTrue(centerX(landmarks.rightEye!!.normalizedPoints) < middle)
        assertTrue(centerX(landmarks.leftEyebrow!!.normalizedPoints) > middle)
        assertTrue(centerX(landmarks.rightEyebrow!!.normalizedPoints) < middle)
        assertTrue(landmarks.leftPupil!!.normalizedPoints.single().x > middle)
        assertTrue(landmarks.rightPupil!!.normalizedPoints.single().x < middle)
    }

    @Test
    fun medianLineRunsFromForeheadToChin() {
        val landmarks = observation(project()).landmarks!!
        val points = landmarks.medianLine!!.normalizedPoints
        assertEquals(points.maxOf { it.y }, points.first().y)
        assertEquals(points.minOf { it.y }, points.last().y)
        val browTop = (landmarks.leftEyebrow!!.normalizedPoints + landmarks.rightEyebrow!!.normalizedPoints)
            .maxOf { it.y }
        assertTrue(points.first().y <= browTop)
        assertTrue(points.first().y > centerY(landmarks.leftEye!!.normalizedPoints))
        assertTrue(abs(points.last().y) < 1e-9)
    }

    @Test
    fun faceContourRunsFromImageRightToImageLeftAtEyeLevel() {
        val landmarks = observation(project()).landmarks!!
        val contour = landmarks.faceContour!!.normalizedPoints
        assertTrue(contour.first().x > contour.last().x)
        val eyeLevel = centerY(landmarks.leftEye!!.normalizedPoints)
        assertTrue(abs(contour.first().y - eyeLevel) < 0.05)
        assertTrue(abs(contour.last().y - eyeLevel) < 0.05)
        assertTrue(contour.minOf { it.y } < 0.01)
    }

    @Test
    fun pointsInImageMatchTheLandmarks() {
        val landmarks = project(rollDegrees = 12.0)
        val face = observation(landmarks)
        val points = face.landmarks!!.innerLips!!.pointsInImage(imageSize = imageSize)
        for ((position, index) in VisionMapping.innerLips.withIndex()) {
            assertEquals(landmarks.xs[index] * imageSize.width, points[position].x, 1e-3)
            assertEquals((1 - landmarks.ys[index]) * imageSize.height, points[position].y, 1e-3)
        }
    }

    @Test
    fun boundingBoxIsTheFaceOvalInVisionSpace() {
        val landmarks = project()
        val box = observation(landmarks).boundingBox
        val oval = VisionMapping.faceOval
        assertEquals(1 - landmarks.ys[152].toDouble(), box.minY, 1e-6)
        assertEquals(1 - landmarks.ys[10].toDouble(), box.maxY, 1e-6)
        assertEquals(oval.minOf { landmarks.xs[it].toDouble() }, box.minX, 1e-6)
        assertEquals(oval.maxOf { landmarks.xs[it].toDouble() }, box.maxX, 1e-6)
        assertEquals(landmarks.xs[127].toDouble(), box.minX, 1e-6)
        assertEquals(landmarks.xs[356].toDouble(), box.maxX, 1e-6)
        val all = observation(landmarks).landmarks!!.allPoints!!.normalizedPoints
        assertTrue(all.all { it.x >= -1e-9 && it.x <= 1 + 1e-9 && it.y >= -1e-9 && it.y <= 1 + 1e-9 })
    }

    @Test
    fun stableBoundingBoxMatchesTheAlertFaceTemplate() {
        val face = observation(project())
        val box = assertNotNull(face.stableBoundingBox(imageSize = imageSize))
        val eyes = face.landmarks!!.leftEye!!.pointsInImage(imageSize = imageSize) +
            face.landmarks!!.rightEye!!.pointsInImage(imageSize = imageSize)
        val eyeFromTop = (box.maxY - centerY(eyes)) / box.height
        assertTrue(eyeFromTop in 0.12..0.22, "eyes at $eyeFromTop of the face height")
        val mouth = face.landmarks!!.innerLips!!.pointsInImage(imageSize = imageSize)
        val mouthFromTop = (box.maxY - centerY(mouth)) / box.height
        assertTrue(mouthFromTop in 0.58..0.72, "mouth at $mouthFromTop of the face height")
        val chin = face.landmarks!!.medianLine!!.pointsInImage(imageSize = imageSize).last()
        assertEquals(box.minY, chin.y, 1e-6)
    }

    @Test
    fun faceAngleFollowsRoll() {
        assertEquals(0.0, observation(project()).calcFaceAngle(imageSize = imageSize)!!, 1e-6)
        for (roll in listOf(-20.0, 20.0)) {
            val angle = observation(project(rollDegrees = roll)).calcFaceAngle(imageSize = imageSize)!!
            assertEquals(roll * PI / 180, angle, 0.01)
        }
    }

    @Test
    fun faceAngleSideFollowsYaw() {
        assertEquals(0.0, observation(project()).calcFaceAngleSide()!!, 1e-6)
        val towardsImageRight = observation(project(yawDegrees = 30.0)).calcFaceAngleSide()!!
        val towardsImageLeft = observation(project(yawDegrees = -30.0)).calcFaceAngleSide()!!
        assertTrue(towardsImageRight < -0.2, "side $towardsImageRight")
        assertTrue(towardsImageLeft > 0.2, "side $towardsImageLeft")
        assertEquals(-towardsImageRight, towardsImageLeft, 1e-3)
    }

    @Test
    fun eyesOpenAndClose() {
        val open = observation(project())
        assertEquals(1.0, open.isLeftEyeOpen(rotationAngle = 0.0, sensitivity = 1.0))
        assertEquals(1.0, open.isRightEyeOpen(rotationAngle = 0.0, sensitivity = 1.0))
        val lowerLid = mapOf(385 to 380, 387 to 373, 386 to 374, 384 to 381, 388 to 390)
        val closed = observation(
            project { index, point ->
                val lower = lowerLid[index]
                if (lower != null) {
                    point[1] = mesh[lower][1]
                }
                point
            }
        )
        assertEquals(0.0, closed.isLeftEyeOpen(rotationAngle = 0.0, sensitivity = 1.0))
        assertEquals(1.0, closed.isRightEyeOpen(rotationAngle = 0.0, sensitivity = 1.0))
    }

    @Test
    fun mouthOpens() {
        val closed = observation(project()).isMouthOpen(rotationAngle = 0.0, sensitivity = 1.0)
        assertTrue(closed < 0.25, "closed mouth $closed")
        val lowerInnerLip = setOf(14, 87, 178, 88, 95, 317, 402, 318, 324)
        val open = observation(
            project { index, point ->
                if (index in lowerInnerLip) {
                    point[1] -= 3.0
                }
                point
            }
        ).isMouthOpen(rotationAngle = 0.0, sensitivity = 1.0)
        assertEquals(1.0, open)
    }

    @Test
    fun pupilsFallBackToEyeCentersWithoutIrisLandmarks() {
        val landmarks = project()
        val face = observation(landmarks, count = VisionMapping.MESH_LANDMARK_COUNT)
        val pupil = face.landmarks!!.leftPupil!!.normalizedPoints.single()
        assertEquals(centerX(face.landmarks!!.leftEye!!.normalizedPoints), pupil.x, 1e-9)
        assertNull(VisionMapping.faceObservation(landmarks.xs, landmarks.ys, 100))
    }

    @Test
    fun analysisSizeKeepsTheLongSideAt640() {
        assertEquals(Pair(640, 360), VisionMapping.analysisSize(1920, 1080))
        assertEquals(Pair(360, 640), VisionMapping.analysisSize(1080, 1920))
        assertEquals(Pair(640, 480), VisionMapping.analysisSize(640, 480))
        assertEquals(Pair(0, 0), VisionMapping.analysisSize(0, 0))
    }

    @Test
    fun textBoxesAreNormalizedWithABottomLeftOrigin() {
        val observation = assertNotNull(
            VisionMapping.textObservation(64, 36, 320, 72, 640, 360, "Moblin", 0.9f, 0.05f)
        )
        assertRect(CGRect(0.1, 0.8, 0.4, 0.1), observation.boundingBox)
        assertEquals("Moblin", observation.topCandidates(1).single().string)
        assertEquals(0.9f, observation.topCandidates(1).single().confidence)
        assertNull(VisionMapping.textObservation(64, 36, 320, 72, 640, 360, "Moblin", 0.9f, 0.2f))
        val clamped = assertNotNull(VisionMapping.textObservation(-10, -10, 700, 400, 640, 360, "", 2f, 0f))
        assertRect(CGRect(0.0, 0.0, 1.0, 1.0), clamped.boundingBox)
        assertEquals(1f, clamped.topCandidates(1).single().confidence)
    }

    @Test
    fun latestResultsAreKeptPerSource() {
        val latest = VisionLatestResults<String>(1000)
        val camera = Any()
        val otherCamera = Any()
        latest.put(camera, "camera", 640, 360)
        assertEquals("camera", latest.get(camera, 640, 360))
        assertNull(latest.get(otherCamera, 640, 360))
        assertNull(latest.get(camera, 360, 640))
        latest.put(otherCamera, "otherCamera", 640, 360)
        assertEquals("camera", latest.get(camera, 640, 360))
        assertEquals("otherCamera", latest.get(otherCamera, 640, 360))
    }

    private fun assertRect(expected: CGRect, actual: CGRect) {
        assertEquals(expected.minX, actual.minX, 1e-12)
        assertEquals(expected.minY, actual.minY, 1e-12)
        assertEquals(expected.width, actual.width, 1e-12)
        assertEquals(expected.height, actual.height, 1e-12)
    }

    private fun points(value: JsonArray): List<CGPoint> {
        return value.map { point ->
            val coordinates = point.jsonArray
            CGPoint(coordinates[0].jsonPrimitive.double, coordinates[1].jsonPrimitive.double)
        }
    }

    private fun visionFace(vision: JsonObject): VNFaceObservation {
        val box = vision["boundingBox"]!!.jsonArray.map { it.jsonPrimitive.double }
        val boundingBox = CGRect(box[0], box[1], box[2], box[3])
        fun region(name: String): VNFaceLandmarkRegion2D? {
            val value = vision[name]?.jsonArray ?: return null
            return VNFaceLandmarkRegion2D(
                normalizedPoints = points(value).map {
                    CGPoint((it.x - boundingBox.minX) / boundingBox.width, (it.y - boundingBox.minY) / boundingBox.height)
                },
                faceBoundingBox = boundingBox
            )
        }
        return VNFaceObservation(
            boundingBox = boundingBox,
            landmarks = VNFaceLandmarks2D(
                faceContour = region("faceContour"),
                leftEye = region("leftEye"),
                rightEye = region("rightEye"),
                leftEyebrow = region("leftEyebrow"),
                rightEyebrow = region("rightEyebrow"),
                nose = region("nose"),
                noseCrest = region("noseCrest"),
                medianLine = region("medianLine"),
                outerLips = region("outerLips"),
                innerLips = region("innerLips"),
                leftPupil = region("leftPupil"),
                rightPupil = region("rightPupil")
            )
        )
    }

    @Test
    fun mappingMatchesVisionOnTheReferenceImage() {
        val stream = javaClass.classLoader!!.getResourceAsStream("vision-reference.json")
        Assume.assumeTrue("vision-reference.json has not been captured on an iPhone yet", stream != null)
        val reference = Json.parseToJsonElement(stream!!.bufferedReader().readText()).jsonObject
        val size = CGSize(reference["imageWidth"]!!.jsonPrimitive.int, reference["imageHeight"]!!.jsonPrimitive.int)
        for (face in reference["faces"]!!.jsonArray) {
            val landmarks = points(face.jsonObject["mediapipe"]!!.jsonArray)
            val xs = FloatArray(landmarks.size) { landmarks[it].x.toFloat() }
            val ys = FloatArray(landmarks.size) { landmarks[it].y.toFloat() }
            val mapped = assertNotNull(VisionMapping.faceObservation(xs, ys, landmarks.size))
            val vision = visionFace(face.jsonObject["vision"]!!.jsonObject)
            val visionBox = assertNotNull(vision.stableBoundingBox(imageSize = size))
            val mappedBox = assertNotNull(mapped.stableBoundingBox(imageSize = size))
            val tolerance = 0.1 * visionBox.height
            assertEquals(visionBox.minX, mappedBox.minX, tolerance)
            assertEquals(visionBox.maxX, mappedBox.maxX, tolerance)
            assertEquals(visionBox.minY, mappedBox.minY, tolerance)
            assertEquals(visionBox.maxY, mappedBox.maxY, tolerance)
            fun center(observation: VNFaceObservation, region: (VNFaceLandmarks2D) -> VNFaceLandmarkRegion2D?): CGPoint {
                val regionPoints = region(observation.landmarks!!)!!.pointsInImage(imageSize = size)
                return CGPoint(centerX(regionPoints), centerY(regionPoints))
            }
            for (region in listOf<(VNFaceLandmarks2D) -> VNFaceLandmarkRegion2D?>(
                { it.leftEye }, { it.rightEye }, { it.outerLips }, { it.innerLips }, { it.leftEyebrow },
                { it.rightEyebrow },
            )) {
                val expected = center(vision, region)
                val actual = center(mapped, region)
                assertEquals(expected.x, actual.x, tolerance)
                assertEquals(expected.y, actual.y, tolerance)
            }
            val visionMedian = vision.landmarks!!.medianLine!!.pointsInImage(imageSize = size)
            val mappedMedian = mapped.landmarks!!.medianLine!!.pointsInImage(imageSize = size)
            assertEquals(visionMedian.first().y, mappedMedian.first().y, tolerance)
            assertEquals(visionMedian.last().y, mappedMedian.last().y, tolerance)
            assertEquals(vision.calcFaceAngle(imageSize = size)!!, mapped.calcFaceAngle(imageSize = size)!!, 0.05)
            assertEquals(vision.calcFaceAngleSide()!!, mapped.calcFaceAngleSide()!!, 0.15)
            assertEquals(vision.boundingBox.height, mapped.boundingBox.height, 0.15 * vision.boundingBox.height)
        }
    }
}
