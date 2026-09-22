package com.moblin.android.media.haishinkit.mpeg

import com.moblin.android.media.haishinkit.mpeg.hevc.calendar
import java.time.Instant
import java.util.Calendar
import java.util.Date
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

private val Instant.timeIntervalSince1970: Double
    get() = epochSecond.toDouble() + nano.toDouble() / 1_000_000_000.0

private val noon = Instant.ofEpochSecond(1_755_864_000L).timeIntervalSince1970

private const val invalidTimeStamp = Long.MIN_VALUE

private fun presentationTimeStamp(frameNumber: Int, fps: Int): Long {
    return frameNumber.toLong() * 1_000_000L / fps.toLong()
}

private fun makeTimecodes(
    generator: MpegTsTimecodeGenerator,
    fps: Int,
    count: Int,
    firstFrameNumber: Int = 0,
): List<MpegTsTimecode> {
    return (firstFrameNumber until firstFrameNumber + count).mapNotNull {
        val timeStamp = presentationTimeStamp(it, fps)
        generator.makeTimecode(timeStamp, timeStamp)
    }
}

private fun makeGenerator(base: Double = noon): MpegTsTimecodeGenerator {
    val generator = MpegTsTimecodeGenerator()
    generator.setReference(now = base, presentationTimeStamp = 0.0)
    return generator
}

private fun timeOfDay(timecode: MpegTsTimecode, fps: Int): Double {
    calendar.time = Date.from(timecode.clock)
    val seconds = 3600 * calendar.get(Calendar.HOUR_OF_DAY) +
        60 * calendar.get(Calendar.MINUTE) +
        calendar.get(Calendar.SECOND)
    return seconds.toDouble() + timecode.frame.toDouble() / fps.toDouble()
}

class MpegTsTimecodeGeneratorSuite {
    @Test
    fun noTimecodesBeforeReferenceIsSet() {
        val generator = MpegTsTimecodeGenerator()
        assertEquals(false, generator.hasReference())
        assertNull(generator.makeTimecode(presentationTimeStamp(0, 30), presentationTimeStamp(0, 30)))
        generator.setReference(now = noon, presentationTimeStamp = 0.0)
        assertEquals(true, generator.hasReference())
        assertNotNull(generator.makeTimecode(presentationTimeStamp(0, 30), presentationTimeStamp(0, 30)))
    }

    @Test
    fun resetForgetsTheReference() {
        val generator = makeGenerator()
        makeTimecodes(generator, 30, 30)
        generator.reset()
        assertEquals(false, generator.hasReference())
        assertNull(generator.makeTimecode(presentationTimeStamp(0, 30), presentationTimeStamp(0, 30)))
    }

    @Test
    fun clockIsReferenceTimePlusPresentationTimeStamp() {
        val generator = makeGenerator()
        val timecodes = makeTimecodes(generator, 30, 300)
        assertEquals(noon, timecodes[0].clock.timeIntervalSince1970)
        for ((frameNumber, timecode) in timecodes.withIndex().drop(1)) {
            val offset = timecode.clock.timeIntervalSince1970 - noon - frameNumber.toDouble() / 30
            assertTrue(offset >= 0, "frame $frameNumber")
            assertTrue(offset < 1.0 / 30, "frame $frameNumber")
        }
    }

    @Test
    fun referenceIsPresentationTimeStampBaseNotTheFirstFrame() {
        val generator = MpegTsTimecodeGenerator()
        generator.setReference(now = noon, presentationTimeStamp = 100.0)
        val timecodes = makeTimecodes(generator, 30, 1, firstFrameNumber = 30 * 110)
        assertEquals(noon + 10, timecodes[0].clock.timeIntervalSince1970)
    }

    @Test
    fun clockIsSplitIntoHoursMinutesAndSecondsInUtc() {
        val generator = makeGenerator()
        val timecodes = makeTimecodes(generator, 30, 30 * 62)
        assertEquals(timeOfDay(timecodes[0], 30), 12 * 3600.0)
        assertEquals(timeOfDay(timecodes[30 * 61], 30), 12 * 3600.0 + 61)
    }

    @Test
    fun frameNumberCountsUpAndWrapsOncePerSecond() {
        for (fps in listOf(24, 25, 30, 50, 60)) {
            val generator = makeGenerator()
            makeTimecodes(generator, fps, fps)
            val frames = makeTimecodes(generator, fps, 5 * fps, firstFrameNumber = fps).map { it.frame }
            assertTrue(frames.all { it.toLong() < fps.toLong() }, "fps $fps")
            assertEquals(5 * fps, frames.size, "fps $fps")
            for ((previous, current) in frames.zip(frames.drop(1))) {
                assertEquals((previous.toLong() + 1L) % fps.toLong(), current.toLong(), "fps $fps")
            }
            assertEquals(5, frames.filter { it.toLong() == 0L }.size, "fps $fps")
        }
    }

    @Test
    fun timeOfDayTracksThePresentationTimeStamp() {
        for (fps in listOf(24, 25, 30, 50, 60)) {
            val generator = makeGenerator()
            makeTimecodes(generator, fps, fps)
            val timecodes = makeTimecodes(generator, fps, 5 * fps, firstFrameNumber = fps)
            val offsets = timecodes.mapIndexed { index, timecode ->
                timeOfDay(timecode, fps) - (fps + index).toDouble() / fps.toDouble()
            }
            val median = offsets.sorted()[offsets.size / 2]
            assertTrue(offsets.all { abs(it - median) < 1.0 / fps.toDouble() }, "fps $fps")
            assertTrue(abs(median - 12 * 3600.0) < 1.0 / fps.toDouble(), "fps $fps")
        }
    }

    @Test
    fun timeOfDayTracksThePresentationTimeStampOverAMinute() {
        val generator = makeGenerator()
        val timecodes = makeTimecodes(generator, 30, 30 * 60)
        val offsets = timecodes.mapIndexed { index, timecode ->
            timeOfDay(timecode, 30) - index.toDouble() / 30.0
        }
        val first = offsets.take(30).sorted()[15]
        val last = offsets.takeLast(30).sorted()[15]
        assertTrue(abs(last - first) < 2.0 / 30)
    }

    @Test
    fun invalidDecodeTimeStampFallsBackToPresentationTimeStamp() {
        val withDecodeTimeStamps = makeGenerator()
        val withoutDecodeTimeStamps = makeGenerator()
        for (frameNumber in 0 until 90) {
            val timeStamp = presentationTimeStamp(frameNumber, 30)
            val withDecodeTimeStamp = withDecodeTimeStamps.makeTimecode(timeStamp, timeStamp)
            val withoutDecodeTimeStamp = withoutDecodeTimeStamps.makeTimecode(timeStamp, invalidTimeStamp)
            assertEquals(withDecodeTimeStamp?.clock, withoutDecodeTimeStamp?.clock)
            assertEquals(withDecodeTimeStamp?.frame, withoutDecodeTimeStamp?.frame)
        }
    }

    @Test
    fun reorderedPresentationTimeStampsDoNotBreakFrameNumbers() {
        val generator = makeGenerator()
        val timecodes = mutableListOf<MpegTsTimecode>()
        for (groupOfPictures in 0 until 20) {
            for ((presentationOffset, decodeOffset) in listOf(2 to 0, 0 to 1, 1 to 2)) {
                val presentation = presentationTimeStamp(3 * groupOfPictures + presentationOffset, 30)
                val decode = presentationTimeStamp(3 * groupOfPictures + decodeOffset, 30)
                val timecode = generator.makeTimecode(presentation, decode) ?: continue
                timecodes.add(timecode)
            }
        }
        assertEquals(60, timecodes.size)
        assertTrue(timecodes.all { it.frame.toLong() < 30L })
        for ((index, timecode) in timecodes.withIndex().drop(30)) {
            assertTrue(
                abs(timeOfDay(timecode, 30) - timecode.clock.timeIntervalSince1970 % 86400.0) < 1.0 / 30,
                "frame $index",
            )
        }
    }
}
