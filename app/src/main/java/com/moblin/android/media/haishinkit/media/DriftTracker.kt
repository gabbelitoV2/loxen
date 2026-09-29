package com.moblin.android.media.haishinkit.media

import com.moblin.android.platform.log.Log
import com.moblin.android.common.various.formatThreeDecimals

enum class DriftTrackerMedia {
    audio,
    video,
}

private class DriftTrackerMediaState(val targetFillLevel: Double) {
    var fillLevels: ArrayDeque<Double> = ArrayDeque()
    var latestFillLevelPresentationTimeStamp = 0.0

    fun lowWaterMark(): Double = if (targetFillLevel >= 0.1) {
        maxOf(-0.2, -targetFillLevel / 2)
    } else {
        -targetFillLevel
    }
}

private data class Deviations(val lower: Double, val upper: Double)

class DriftTracker(private val name: String) {
    private var audio: DriftTrackerMediaState? = null
    private var video: DriftTrackerMediaState? = null
    private var latestAdjustDriftPresentationTimeStamp = -1.0
    private var drift = 0.0

    fun addMedia(media: DriftTrackerMedia, targetFillLevel: Double) {
        Log.d(
            TAG,
            "drift-tracker: $name: Adding $media with target fill level " +
                formatThreeDecimals(targetFillLevel),
        )
        when (media) {
            DriftTrackerMedia.audio -> audio = DriftTrackerMediaState(targetFillLevel)
            DriftTrackerMedia.video -> video = DriftTrackerMediaState(targetFillLevel)
        }
    }

    fun getDrift(): Double = drift

    fun setDrift(drift: Double) {
        this.drift = drift
    }

    fun update(
        media: DriftTrackerMedia,
        outputPresentationTimeStamp: Double,
        newestPresentationTimeStamp: Double,
    ) {
        val mediaState = state(media) ?: return
        if (outputPresentationTimeStamp <= mediaState.latestFillLevelPresentationTimeStamp + 0.5) {
            return
        }
        mediaState.latestFillLevelPresentationTimeStamp = outputPresentationTimeStamp
        mediaState.fillLevels.addLast(newestPresentationTimeStamp - outputPresentationTimeStamp)
        if (mediaState.fillLevels.size > 60) {
            mediaState.fillLevels.removeFirst()
        }
        if (latestAdjustDriftPresentationTimeStamp == -1.0) {
            latestAdjustDriftPresentationTimeStamp = outputPresentationTimeStamp
        }
        if (outputPresentationTimeStamp <= latestAdjustDriftPresentationTimeStamp + 20.0) {
            return
        }
        latestAdjustDriftPresentationTimeStamp = outputPresentationTimeStamp
        val deviations = listOfNotNull(audio, video)
            .filter {
                it.fillLevels.size >= 30 &&
                    outputPresentationTimeStamp - it.latestFillLevelPresentationTimeStamp < 20.0
            }
            .map { it to estimateDeviations(it) }
        val (minState, minDeviations) = deviations.minByOrNull { it.second.upper } ?: return
        val lowerDeviation = deviations.minOfOrNull { it.second.lower } ?: return
        if (minDeviations.upper < minState.lowWaterMark()) {
            adjustDrift(minDeviations.upper)
        } else if (lowerDeviation > 0.2) {
            adjustDrift(lowerDeviation)
        }
    }

    private fun state(media: DriftTrackerMedia): DriftTrackerMediaState? = when (media) {
        DriftTrackerMedia.audio -> audio
        DriftTrackerMedia.video -> video
    }

    private fun estimateDeviations(state: DriftTrackerMediaState): Deviations {
        val deviations = state.fillLevels.sorted().map { it + drift - state.targetFillLevel }
        return Deviations(
            lower = deviations[deviations.size / 4],
            upper = deviations[3 * deviations.size / 4],
        )
    }

    private fun adjustDrift(deviation: Double) {
        val drift = this.drift - deviation
        Log.d(
            TAG,
            "drift-tracker: $name: Estimated deviation from target " +
                formatThreeDecimals(deviation) + ", Drift " + formatThreeDecimals(this.drift) +
                " -> " + formatThreeDecimals(drift),
        )
        this.drift = drift
    }

    companion object {
        private const val TAG = "DriftTracker"
    }
}
