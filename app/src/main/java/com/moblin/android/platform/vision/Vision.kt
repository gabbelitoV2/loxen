package com.moblin.android.platform.vision

import android.util.Log
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.coregraphics.CGImagePropertyOrientation
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.video.CVPixelBuffer
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "MoblinVision"

internal object VisionLog {
    private val loggedMessages = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun once(key: String, message: String) {
        if (loggedMessages.size > 1000) {
            loggedMessages.clear()
        }
        if (!loggedMessages.add(key)) {
            return
        }
        try {
            Log.i(TAG, message)
        } catch (_: Throwable) {
        }
    }

    fun notImplemented(member: String) {
        once("notImplemented:$member", "$member not implemented yet")
    }
}

class VNError(message: String) : Exception(message)

open class VNObservation internal constructor(val confidence: Float, val uuid: UUID)

class VNFaceLandmarkRegion2D internal constructor(
    val normalizedPoints: List<CGPoint>,
    internal val faceBoundingBox: CGRect,
    val precisionEstimatesPerPoint: List<Float>? = null,
) {
    val pointCount: Int
        get() = normalizedPoints.size

    fun pointsInImage(imageSize: CGSize): List<CGPoint> {
        val box = faceBoundingBox
        return normalizedPoints.map { point ->
            CGPoint(
                x = (box.origin.x + point.x * box.size.width) * imageSize.width,
                y = (box.origin.y + point.y * box.size.height) * imageSize.height
            )
        }
    }
}

class VNFaceLandmarks2D internal constructor(
    val confidence: Float = 1f,
    val allPoints: VNFaceLandmarkRegion2D? = null,
    val faceContour: VNFaceLandmarkRegion2D? = null,
    val leftEye: VNFaceLandmarkRegion2D? = null,
    val rightEye: VNFaceLandmarkRegion2D? = null,
    val leftEyebrow: VNFaceLandmarkRegion2D? = null,
    val rightEyebrow: VNFaceLandmarkRegion2D? = null,
    val nose: VNFaceLandmarkRegion2D? = null,
    val noseCrest: VNFaceLandmarkRegion2D? = null,
    val medianLine: VNFaceLandmarkRegion2D? = null,
    val outerLips: VNFaceLandmarkRegion2D? = null,
    val innerLips: VNFaceLandmarkRegion2D? = null,
    val leftPupil: VNFaceLandmarkRegion2D? = null,
    val rightPupil: VNFaceLandmarkRegion2D? = null,
)

class VNFaceObservation internal constructor(
    val boundingBox: CGRect,
    val landmarks: VNFaceLandmarks2D? = null,
    val roll: Double? = null,
    val yaw: Double? = null,
    val pitch: Double? = null,
    confidence: Float = 1f,
    uuid: UUID = UUID.randomUUID(),
) : VNObservation(confidence, uuid)

class VNRecognizedText internal constructor(val string: String, val confidence: Float)

class VNRecognizedTextObservation internal constructor(
    val boundingBox: CGRect,
    private val candidates: List<VNRecognizedText>,
    confidence: Float = 1f,
    uuid: UUID = UUID.randomUUID(),
) : VNObservation(confidence, uuid) {
    fun topCandidates(maxCandidateCount: Int): List<VNRecognizedText> {
        return candidates.take(maxCandidateCount.coerceIn(0, 10))
    }
}

class VNPixelBufferObservation internal constructor(
    val pixelBuffer: CVPixelBuffer,
    val featureName: String? = null,
    confidence: Float = 1f,
    uuid: UUID = UUID.randomUUID(),
) : VNObservation(confidence, uuid)

typealias VNRequestCompletionHandler = (VNRequest, Throwable?) -> Unit

abstract class VNRequest internal constructor(val completionHandler: VNRequestCompletionHandler?) {
    open val results: List<VNObservation>?
        get() = null

    var revision: Int = 1
    var preferBackgroundProcessing: Boolean = false
    var usesCPUOnly: Boolean = false

    @Volatile
    internal var isCancelled: Boolean = false

    fun cancel() {
        isCancelled = true
    }

    internal abstract fun clearResults()

    internal abstract fun setNoResults()
}

class VNDetectFaceLandmarksRequest(
    completionHandler: VNRequestCompletionHandler? = null,
) : VNRequest(completionHandler) {
    var inputFaceObservations: List<VNFaceObservation>? = null

    @Volatile
    internal var faceResults: List<VNFaceObservation>? = null

    override val results: List<VNFaceObservation>?
        get() = faceResults

    override fun clearResults() {
        faceResults = null
    }

    override fun setNoResults() {
        faceResults = emptyList()
    }
}

enum class VNRequestTextRecognitionLevel {
    accurate,
    fast,
}

class VNRecognizeTextRequest(
    completionHandler: VNRequestCompletionHandler? = null,
) : VNRequest(completionHandler) {
    var recognitionLevel: VNRequestTextRecognitionLevel = VNRequestTextRecognitionLevel.accurate
    var usesLanguageCorrection: Boolean = true
    var minimumTextHeight: Float = 1f / 32f
    var recognitionLanguages: List<String> = listOf("en-US")
    var customWords: List<String> = emptyList()
    var automaticallyDetectsLanguage: Boolean = false

    @Volatile
    internal var textResults: List<VNRecognizedTextObservation>? = null

    override val results: List<VNRecognizedTextObservation>?
        get() = textResults

    override fun clearResults() {
        textResults = null
    }

    override fun setNoResults() {
        textResults = emptyList()
    }
}

class VNGeneratePersonSegmentationRequest(
    completionHandler: VNRequestCompletionHandler? = null,
) : VNRequest(completionHandler) {
    enum class QualityLevel {
        accurate,
        balanced,
        fast,
    }

    var qualityLevel: QualityLevel = QualityLevel.accurate
    var outputPixelFormat: Int = kCVPixelFormatType_OneComponent8

    @Volatile
    internal var segmentationResults: List<VNPixelBufferObservation>? = null

    override val results: List<VNPixelBufferObservation>?
        get() = segmentationResults

    override fun clearResults() {
        segmentationResults = null
    }

    override fun setNoResults() {
        segmentationResults = emptyList()
    }
}

const val kCVPixelFormatType_OneComponent8 = 0x4C303038

class VNImageRequestHandler(
    val cvPixelBuffer: CVPixelBuffer,
    val options: Map<String, Any> = emptyMap(),
) {
    var orientation: CGImagePropertyOrientation = CGImagePropertyOrientation.up
        private set

    constructor(
        cvPixelBuffer: CVPixelBuffer,
        orientation: CGImagePropertyOrientation,
        options: Map<String, Any> = emptyMap(),
    ) : this(cvPixelBuffer, options) {
        this.orientation = orientation
    }

    fun perform(requests: List<VNRequest>) {
        if (Thread.currentThread().name == PipelineThread.NAME) {
            VisionLog.once("performOnPipeline", "VNImageRequestHandler.perform called on the pipeline thread")
            throw VNError("VNImageRequestHandler.perform must not run on the pipeline thread")
        }
        for (request in requests) {
            request.clearResults()
        }
        VisionLog.notImplemented("VNImageRequestHandler.perform")
        for (request in requests) {
            if (request.isCancelled) {
                request.completionHandler?.invoke(request, VNError("The request was cancelled"))
                continue
            }
            request.setNoResults()
            request.completionHandler?.invoke(request, null)
        }
    }
}
