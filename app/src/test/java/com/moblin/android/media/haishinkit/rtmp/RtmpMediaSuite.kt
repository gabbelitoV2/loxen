package com.moblin.android.media.haishinkit.rtmp

import android.media.MediaFormat
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderSettings
import com.moblin.android.media.haishinkit.mpeg.avc.MpegTsVideoConfigAvc
import com.moblin.android.media.kCMTimeInvalidUs
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RtmpMediaSuite {
    enum class Track {
        audio,
        video,
    }

    data class TimestampsCase(
        val name: String,
        val inputs: List<Pair<Track, Double>>,
        val expected: List<UInt?>,
    )

    data class VideoFrameCase(
        val format: VideoEncoderSettings.Format,
        val isSync: Boolean,
        val decodeTimeStamp: Long,
        val expectedHeader: ByteArray,
    )

    @Test
    fun timestamps() {
        val cases = listOf(
            TimestampsCase(
                name = "first is zero",
                inputs = listOf(Track.audio to 10.0),
                expected = listOf(0u),
            ),
            TimestampsCase(
                name = "deltas in milliseconds",
                inputs = listOf(Track.audio to 10.0, Track.audio to 10.5, Track.audio to 11.25),
                expected = listOf(0u, 500u, 750u),
            ),
            TimestampsCase(
                name = "fractions are carried over",
                inputs = listOf(
                    Track.video to 10.0,
                    Track.video to 10.03125,
                    Track.video to 10.0625,
                    Track.video to 10.09375,
                    Track.video to 10.125,
                ),
                expected = listOf(0u, 31u, 31u, 31u, 32u),
            ),
            TimestampsCase(
                name = "before base is dropped",
                inputs = listOf(Track.audio to 10.0, Track.audio to 9.5, Track.audio to 10.5),
                expected = listOf(0u, null, 500u),
            ),
            TimestampsCase(
                name = "out of order is dropped",
                inputs = listOf(Track.audio to 10.0, Track.audio to 10.5, Track.audio to 10.25, Track.audio to 11.0),
                expected = listOf(0u, 500u, null, 500u),
            ),
            TimestampsCase(
                name = "base is shared",
                inputs = listOf(Track.video to 10.0, Track.audio to 9.5, Track.audio to 10.25, Track.audio to 10.5),
                expected = listOf(0u, null, 0u, 250u),
            ),
            TimestampsCase(
                name = "tracks have separate deltas",
                inputs = listOf(Track.audio to 10.0, Track.video to 10.5, Track.audio to 10.25, Track.video to 11.0),
                expected = listOf(0u, 0u, 250u, 500u),
            ),
        )
        for (testCase in cases) {
            val timestamps = RtmpTimestamps()
            val actual = testCase.inputs.map { (track, timestamp) ->
                when (track) {
                    Track.audio -> timestamps.audio(timestamp)
                    Track.video -> timestamps.video(timestamp)
                }
            }
            assertEquals(testCase.expected, actual, testCase.name)
        }
    }

    @Test
    fun aacSequenceHeader() {
        assertContentEquals(
            bytes(0xAF, 0x00, 0x11, 0x90),
            makeRtmpAudioSequenceHeader(makeAacFormat()),
        )
    }

    @Test
    fun opusSequenceHeader() {
        assertContentEquals(
            bytes(0x90) + "OpusOpusHead".toByteArray() + bytes(1, 2, 0, 0, 0, 0, 0xBB, 0x80, 0, 0, 0),
            makeRtmpAudioSequenceHeader(makeOpusFormat()),
        )
    }

    @Test
    fun aacFrame() {
        assertContentEquals(
            bytes(0xAF, 0x01, 1, 2, 3),
            makeRtmpAudioFrame(makeAudioBuffer(makeAacFormat(), byteArrayOf(1, 2, 3)), MediaFormat.MIMETYPE_AUDIO_AAC),
        )
    }

    @Test
    fun opusFrame() {
        assertContentEquals(
            bytes(0x91) + "Opus".toByteArray() + bytes(1, 2, 3),
            makeRtmpAudioFrame(makeAudioBuffer(makeOpusFormat(), byteArrayOf(1, 2, 3)), MediaFormat.MIMETYPE_AUDIO_OPUS),
        )
    }

    @Test
    fun h264SequenceHeader() {
        val formatDescription = makeH264FormatDescription()
        val avcC = assertNotNull(MpegTsVideoConfigAvc.getAvcC(formatDescription))
        assertContentEquals(
            bytes(0x17, 0, 0, 0, 0) + avcC,
            makeRtmpVideoSequenceHeader(VideoEncoderSettings.Format.h264, formatDescription),
        )
    }

    @Test
    fun hevcSequenceHeaderWithoutHvcC() {
        assertNull(makeRtmpVideoSequenceHeader(VideoEncoderSettings.Format.hevc, makeH264FormatDescription()))
    }

    @Test
    fun videoFrame() {
        val cases = listOf(
            VideoFrameCase(
                format = VideoEncoderSettings.Format.h264,
                isSync = true,
                decodeTimeStamp = mediaTime(10240),
                expectedHeader = bytes(0x17, 0x01, 0, 0, 62),
            ),
            VideoFrameCase(
                format = VideoEncoderSettings.Format.h264,
                isSync = false,
                decodeTimeStamp = mediaTime(10240),
                expectedHeader = bytes(0x27, 0x01, 0, 0, 62),
            ),
            VideoFrameCase(
                format = VideoEncoderSettings.Format.h264,
                isSync = true,
                decodeTimeStamp = kCMTimeInvalidUs,
                expectedHeader = bytes(0x17, 0x01, 0, 0, 0),
            ),
            VideoFrameCase(
                format = VideoEncoderSettings.Format.hevc,
                isSync = true,
                decodeTimeStamp = mediaTime(10240),
                expectedHeader = bytes(0x91) + "hvc1".toByteArray() + bytes(0, 0, 62),
            ),
            VideoFrameCase(
                format = VideoEncoderSettings.Format.hevc,
                isSync = false,
                decodeTimeStamp = mediaTime(10240),
                expectedHeader = bytes(0xA1) + "hvc1".toByteArray() + bytes(0, 0, 62),
            ),
        )
        for (testCase in cases) {
            val sampleBuffer = assertNotNull(
                makeVideoSampleBuffer(
                    makeH264FormatDescription(),
                    testCase.decodeTimeStamp,
                    mediaTime(10304),
                    testCase.isSync,
                    bytes(0, 0, 0, 1, 7),
                ),
            )
            assertContentEquals(
                testCase.expectedHeader + bytes(0, 0, 0, 1, 7),
                makeRtmpVideoFrame(testCase.format, sampleBuffer, bytes(0, 0, 0, 1, 7)),
                "${testCase.format}, sync ${testCase.isSync}, dts ${testCase.decodeTimeStamp}",
            )
        }
    }

    @Test
    fun videoFrameWithNegativeCompositionTime() {
        val sampleBuffer = assertNotNull(
            makeVideoSampleBuffer(
                makeH264FormatDescription(),
                mediaTime(10304),
                mediaTime(10240),
                true,
                bytes(7),
            ),
        )
        assertContentEquals(
            bytes(0x17, 0x01, 0xFF, 0xFF, 0xC2, 7),
            makeRtmpVideoFrame(VideoEncoderSettings.Format.h264, sampleBuffer, bytes(7)),
        )
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }
}
