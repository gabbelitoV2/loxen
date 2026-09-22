package com.moblin.android.media.haishinkit.media.video

import com.moblin.android.media.haishinkit.media.Processor

class VideoFpsEstimator {
    var processor: Processor? = null
    private var framesCounter = 0
    private var latestReportedFps = -1
    private var nextFpsReportTime: Double = 0.0

    fun update(presentationTimeStamp: Double, fps: Double) {
        if (nextFpsReportTime == 0.0) {
            reportAndResetFps(fps.toInt(), presentationTimeStamp)
        } else {
            framesCounter += 1
            if (presentationTimeStamp > nextFpsReportTime) {
                reportAndResetFps(framesCounter / 2, presentationTimeStamp)
            }
        }
    }

    private fun reportAndResetFps(fps: Int, presentationTimeStamp: Double) {
        if (fps != latestReportedFps) {
            processor?.delegate?.streamVideoFps(fps)
            latestReportedFps = fps
        }
        framesCounter = 0
        nextFpsReportTime = presentationTimeStamp + 2.0
    }
}
