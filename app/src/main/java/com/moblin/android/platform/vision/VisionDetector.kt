package com.moblin.android.platform.vision

import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.video.CVPixelBuffer

object VisionDetector {
    fun detect(
        imageBuffer: CVPixelBuffer,
        detectFaces: Boolean,
        detectText: Boolean,
        completion: (List<VNFaceObservation>, List<CGRect>) -> Unit,
    ) {
        val faceDetections = mutableListOf<VNFaceObservation>()
        val textDetections = mutableListOf<CGRect>()
        try {
            var faceLandmarksRequest: VNDetectFaceLandmarksRequest? = null
            var textRequest: VNRecognizeTextRequest? = null
            val requests = mutableListOf<VNRequest>()
            if (detectFaces) {
                faceLandmarksRequest = VNDetectFaceLandmarksRequest()
                requests.add(faceLandmarksRequest)
            }
            if (detectText) {
                textRequest = VNRecognizeTextRequest()
                textRequest.recognitionLevel = VNRequestTextRecognitionLevel.fast
                textRequest.usesLanguageCorrection = false
                textRequest.minimumTextHeight = 0.05f
                requests.add(textRequest)
            }
            val imageRequestHandler = VNImageRequestHandler(cvPixelBuffer = imageBuffer)
            if (runCatching { imageRequestHandler.perform(requests) }.isSuccess) {
                faceLandmarksRequest?.results
                    ?.sortedByDescending { it.boundingBox.height }
                    ?.take(5)
                    ?.let { faceDetections += it }
                textRequest?.results?.let { results ->
                    for (result in results.sortedByDescending { it.boundingBox.height }.take(10)) {
                        val text = result.topCandidates(1).firstOrNull()
                        if (text != null && text.confidence >= 0.5f) {
                            textDetections.add(result.boundingBox)
                        }
                    }
                }
            }
        } catch (error: Throwable) {
            VisionLog.once("detect:${error.javaClass.name}", "Detection failed: $error")
            faceDetections.clear()
            textDetections.clear()
        }
        completion(faceDetections, textDetections)
    }
}
