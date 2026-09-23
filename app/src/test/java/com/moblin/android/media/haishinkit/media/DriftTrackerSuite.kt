package com.moblin.android.media.haishinkit.media

import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DriftTrackerSuite {
    @Test
    fun steadyLevelDoesNotAdjust() {
        val tracker = createTracker()
        val adjustments = drive(tracker, 0.0, 120.0, video = { time -> time + 1.0 })
        assertTrue(adjustments.isEmpty())
        assertEquals(0.0, tracker.getDrift())
    }

    @Test
    fun stallBurstDoesNotAdjust() {
        val tracker = createTracker()
        var adjustments = drive(tracker, 0.0, 50.0, video = { time -> time + 1.0 })
        adjustments += drive(tracker, 60.0, 65.0, video = { time -> 51.0 + (time - 60.0) * 3.0 })
        adjustments += drive(tracker, 65.0, 150.0, video = { time -> time + 1.0 })
        assertTrue(adjustments.isEmpty())
        assertEquals(0.0, tracker.getDrift())
    }

    @Test
    fun slowCatchUpAfterStallDoesNotAdjust() {
        val tracker = createTracker()
        var adjustments = drive(tracker, 0.0, 60.0, video = { time -> time + 1.0 })
        adjustments += drive(
            tracker,
            61.3,
            87.3,
            video = { time -> time - 0.3 + (time - 61.3) * 0.05 },
        )
        adjustments += drive(tracker, 87.3, 200.0, video = { time -> time + 1.0 })
        assertTrue(adjustments.isEmpty())
        assertEquals(0.0, tracker.getDrift())
    }

    @Test
    fun sustainedJumpAdjustsToTarget() {
        val tracker = createTracker()
        var adjustments = drive(tracker, 0.0, 50.0, video = { time -> time + 1.0 })
        adjustments += drive(tracker, 50.0, 150.0, video = { time -> time })
        assertEquals(1, adjustments.size)
        assertTrue(abs(tracker.getDrift() - 1.0) < 0.01)
    }

    @Test
    fun initialDriftIsIncludedInFillLevel() {
        val tracker = createTracker()
        tracker.setDrift(0.5)
        val adjustments = drive(tracker, 0.0, 120.0, video = { time -> time + 0.5 })
        assertTrue(adjustments.isEmpty())
        assertEquals(0.5, tracker.getDrift())
    }

    @Test
    fun audioAndVideoShareOneDrift() {
        val tracker = createTracker()
        var adjustments = drive(
            tracker,
            0.0,
            50.0,
            audio = { time -> time + 1.0 },
            video = { time -> time + 1.0 },
        )
        adjustments += drive(tracker, 50.0, 150.0, audio = { time -> time }, video = { time -> time })
        assertEquals(1, adjustments.size)
        assertTrue(abs(tracker.getDrift() - 1.0) < 0.01)
    }

    @Test
    fun leadingMediaKeepsExtraBuffer() {
        val tracker = createTracker()
        val adjustments = drive(
            tracker,
            0.0,
            200.0,
            audio = { time -> time + 1.5 },
            video = { time -> time + 1.0 },
        )
        assertTrue(adjustments.isEmpty())
        assertEquals(0.0, tracker.getDrift())
    }

    @Test
    fun laggingMediaIsHeldAtTarget() {
        val tracker = createTracker()
        val adjustments = drive(
            tracker,
            0.0,
            200.0,
            audio = { time -> time + 1.0 },
            video = { time -> time + 0.5 },
        )
        assertEquals(1, adjustments.size)
        assertTrue(abs(tracker.getDrift() - 0.5) < 0.01)
    }

    @Test
    fun staleMediaIsIgnored() {
        val tracker = createTracker()
        var adjustments = drive(
            tracker,
            0.0,
            50.0,
            audio = { time -> time + 1.0 },
            video = { time -> time + 1.0 },
        )
        adjustments += drive(tracker, 50.0, 150.0, video = { time -> time + 2.0 })
        assertEquals(1, adjustments.size)
        assertTrue(abs(tracker.getDrift() + 1.0) < 0.01)
    }

    @Test
    fun mediaWithTooFewSamplesIsIgnored() {
        val tracker = createTracker()
        var adjustments = drive(tracker, 0.0, 50.0, video = { time -> time + 1.0 })
        adjustments += drive(
            tracker,
            50.0,
            55.0,
            audio = { time -> time + 3.0 },
            video = { time -> time },
        )
        adjustments += drive(tracker, 55.0, 150.0, video = { time -> time })
        assertEquals(1, adjustments.size)
        assertTrue(abs(tracker.getDrift() - 1.0) < 0.01)
    }

    @Test
    fun unknownMediaIsIgnored() {
        val tracker = DriftTracker("")
        tracker.addMedia(DriftTrackerMedia.video, 1.0)
        val adjustments = drive(tracker, 0.0, 120.0, audio = { time -> time + 5.0 })
        assertTrue(adjustments.isEmpty())
        assertEquals(0.0, tracker.getDrift())
    }
}

private fun createTracker(): DriftTracker {
    val tracker = DriftTracker("")
    tracker.addMedia(DriftTrackerMedia.audio, 1.0)
    tracker.addMedia(DriftTrackerMedia.video, 1.0)
    return tracker
}

private fun drive(
    tracker: DriftTracker,
    from: Double,
    to: Double,
    audio: ((Double) -> Double)? = null,
    video: ((Double) -> Double)? = null,
): List<Double> {
    val adjustments = mutableListOf<Double>()
    var time = from
    while (time < to) {
        for ((media, newest) in listOf(
            DriftTrackerMedia.audio to audio,
            DriftTrackerMedia.video to video,
        )) {
            val newestTime = newest ?: continue
            val drift = tracker.getDrift()
            tracker.update(media, time, newestTime(time))
            if (tracker.getDrift() != drift) {
                adjustments.add(tracker.getDrift())
            }
        }
        time += 0.6
    }
    return adjustments
}
