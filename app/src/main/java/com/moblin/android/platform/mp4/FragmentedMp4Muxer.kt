package com.moblin.android.platform.mp4

import android.util.Log
import java.util.Locale
import kotlin.math.abs

private const val TAG = "MoblinRecorder"
private const val aacFramesPerPacket = 1024
private const val maximumNumberOfPendingAudioPackets = 3000

class Mp4TrackReport(
    val trackId: Int,
    val isVideo: Boolean,
    val earliestPresentationTimeUs: Long,
    val durationUs: Long,
)

interface FragmentedMp4MuxerListener {
    fun fragmentedMp4MuxerInitializationSegment(data: ByteArray)

    fun fragmentedMp4MuxerMediaSegment(data: ByteArray, reports: List<Mp4TrackReport>)
}

private class PendingVideoSample(
    val data: ByteArray,
    val presentationTime: Long,
    val decodeTime: Long,
    val isKeyFrame: Boolean,
)

private class PendingAudioSample(
    val data: ByteArray,
    val decodeTime: Long,
)

class FragmentedMp4Muxer(
    private val segmentIntervalUs: Long,
    private val listener: FragmentedMp4MuxerListener,
) {
    private var videoTrack: Mp4VideoTrackConfig? = null
    private var audioTrack: Mp4AudioTrackConfig? = null
    private var initializationSegmentWritten = false
    private val pendingVideo = ArrayList<PendingVideoSample>()
    private val pendingAudio = ArrayDeque<PendingAudioSample>()
    private var latestVideoDecodeTime = Long.MIN_VALUE
    private var latestVideoDuration = 3000L
    private var latestAudioDecodeTime: Long? = null
    private var sequenceNumber = 0
    private var numberOfDroppedVideoSamples = 0
    private var numberOfAudioDiscontinuities = 0
    private var finished = false

    val numberOfSegments: Int
        get() = sequenceNumber

    fun setAudioTrack(config: Mp4AudioTrackConfig) {
        if (initializationSegmentWritten) {
            return
        }
        audioTrack = config
        writeInitializationSegmentIfReady()
    }

    fun setVideoTrack(config: Mp4VideoTrackConfig) {
        if (initializationSegmentWritten) {
            val current = videoTrack
            if (current != null && !current.decoderConfigurationRecord.contentEquals(config.decoderConfigurationRecord)) {
                logOnce("Video parameter sets changed after the initialization segment, keeping the first ones")
            }
            return
        }
        videoTrack = config
        writeInitializationSegmentIfReady()
    }

    fun appendVideo(data: ByteArray, presentationTimeUs: Long, decodeTimeUs: Long, isKeyFrame: Boolean) {
        if (finished || videoTrack == null) {
            return
        }
        val decodeTime = usToTimescale(if (decodeTimeUs >= 0) decodeTimeUs else presentationTimeUs, mp4VideoTimescale)
        val presentationTime = usToTimescale(presentationTimeUs, mp4VideoTimescale)
        if (pendingVideo.isEmpty() && sequenceNumber == 0 && !isKeyFrame) {
            numberOfDroppedVideoSamples += 1
            return
        }
        if (decodeTime <= latestVideoDecodeTime) {
            numberOfDroppedVideoSamples += 1
            if (numberOfDroppedVideoSamples <= 5) {
                Log.i(TAG, "Dropping video sample with non-increasing decode time $decodeTimeUs us")
            }
            return
        }
        val first = pendingVideo.firstOrNull()
        if (first != null) {
            val elapsedUs = timescaleToUs(decodeTime - first.decodeTime, mp4VideoTimescale)
            if (isKeyFrame && elapsedUs + 100_000 >= segmentIntervalUs) {
                writeMediaSegment(decodeTime)
            } else if (elapsedUs >= 5 * segmentIntervalUs) {
                logOnce("No key frame for ${elapsedUs / 1000} ms, cutting the segment anyway")
                writeMediaSegment(decodeTime)
            }
        }
        latestVideoDuration = if (latestVideoDecodeTime != Long.MIN_VALUE) {
            (decodeTime - latestVideoDecodeTime).coerceIn(1L, 90_000L)
        } else {
            latestVideoDuration
        }
        latestVideoDecodeTime = decodeTime
        pendingVideo.add(PendingVideoSample(data, presentationTime, decodeTime, isKeyFrame))
    }

    fun appendAudio(data: ByteArray, presentationTimeUs: Long) {
        val audioTrack = audioTrack ?: return
        if (finished || data.isEmpty()) {
            return
        }
        val sampleRate = audioTrack.sampleRate
        val actualDecodeTime = usToTimescale(presentationTimeUs, sampleRate)
        val latestDecodeTime = latestAudioDecodeTime
        val decodeTime = if (latestDecodeTime == null) {
            actualDecodeTime
        } else {
            val expectedDecodeTime = latestDecodeTime + aacFramesPerPacket
            when {
                abs(actualDecodeTime - expectedDecodeTime) <= sampleRate / 20 -> expectedDecodeTime
                actualDecodeTime > expectedDecodeTime -> {
                    numberOfAudioDiscontinuities += 1
                    Log.i(
                        TAG,
                        "Audio discontinuity of ${timescaleToUs(actualDecodeTime - expectedDecodeTime, sampleRate) / 1000} ms",
                    )
                    actualDecodeTime
                }
                else -> {
                    numberOfAudioDiscontinuities += 1
                    if (numberOfAudioDiscontinuities <= 5) {
                        Log.i(TAG, "Dropping overlapping audio packet at $presentationTimeUs us")
                    }
                    return
                }
            }
        }
        latestAudioDecodeTime = decodeTime
        pendingAudio.addLast(PendingAudioSample(data, decodeTime))
        while (pendingAudio.size > maximumNumberOfPendingAudioPackets) {
            pendingAudio.removeFirst()
            logOnce("Too much audio without video, dropping old audio")
        }
    }

    fun finish() {
        if (finished) {
            return
        }
        if (initializationSegmentWritten && pendingVideo.isNotEmpty()) {
            writeMediaSegment(null)
        }
        finished = true
        pendingVideo.clear()
        pendingAudio.clear()
        if (numberOfDroppedVideoSamples > 0) {
            Log.i(TAG, "Dropped $numberOfDroppedVideoSamples video samples in total")
        }
    }

    private fun writeInitializationSegmentIfReady() {
        val videoTrack = videoTrack ?: return
        val data = makeMp4InitializationSegment(videoTrack, audioTrack, System.currentTimeMillis() / 1000)
        initializationSegmentWritten = true
        Log.i(TAG, "init segment ${data.size} bytes")
        listener.fragmentedMp4MuxerInitializationSegment(data)
    }

    private fun writeMediaSegment(nextVideoDecodeTime: Long?) {
        val videoTrack = videoTrack ?: return
        if (!initializationSegmentWritten || pendingVideo.isEmpty()) {
            return
        }
        val firstVideoDecodeTime = pendingVideo.first().decodeTime
        val endVideoDecodeTime = nextVideoDecodeTime ?: (pendingVideo.last().decodeTime + latestVideoDuration)
        val videoSamples = ArrayList<Mp4FragmentSample>(pendingVideo.size)
        var earliestPresentationTime = Long.MAX_VALUE
        for ((index, sample) in pendingVideo.withIndex()) {
            val nextDecodeTime = if (index + 1 < pendingVideo.size) {
                pendingVideo[index + 1].decodeTime
            } else {
                endVideoDecodeTime
            }
            earliestPresentationTime = minOf(earliestPresentationTime, sample.presentationTime)
            videoSamples.add(
                Mp4FragmentSample(
                    data = sample.data,
                    duration = nextDecodeTime - sample.decodeTime,
                    compositionTimeOffset = sample.presentationTime - sample.decodeTime,
                    isSync = sample.isKeyFrame,
                ),
            )
        }
        pendingVideo.clear()
        val fragments = mutableListOf(
            Mp4TrackFragment(
                trackId = videoTrack.trackId,
                baseMediaDecodeTime = firstVideoDecodeTime,
                samples = videoSamples,
                writeSampleFlags = true,
            ),
        )
        val reports = mutableListOf(
            Mp4TrackReport(
                trackId = videoTrack.trackId,
                isVideo = true,
                earliestPresentationTimeUs = timescaleToUs(earliestPresentationTime, mp4VideoTimescale),
                durationUs = timescaleToUs(endVideoDecodeTime - firstVideoDecodeTime, mp4VideoTimescale),
            ),
        )
        val audioTrack = audioTrack
        if (audioTrack != null) {
            val endUs = timescaleToUs(endVideoDecodeTime, mp4VideoTimescale)
            val takenAudio = ArrayList<PendingAudioSample>()
            while (true) {
                val sample = pendingAudio.firstOrNull() ?: break
                if (nextVideoDecodeTime != null && timescaleToUs(sample.decodeTime, audioTrack.sampleRate) >= endUs) {
                    break
                }
                takenAudio.add(pendingAudio.removeFirst())
            }
            if (takenAudio.isNotEmpty()) {
                val audioSamples = ArrayList<Mp4FragmentSample>(takenAudio.size)
                for ((index, sample) in takenAudio.withIndex()) {
                    val nextDecodeTime = when {
                        index + 1 < takenAudio.size -> takenAudio[index + 1].decodeTime
                        else -> pendingAudio.firstOrNull()?.decodeTime ?: (sample.decodeTime + aacFramesPerPacket)
                    }
                    audioSamples.add(
                        Mp4FragmentSample(
                            data = sample.data,
                            duration = nextDecodeTime - sample.decodeTime,
                            compositionTimeOffset = 0,
                            isSync = true,
                        ),
                    )
                }
                val firstAudioDecodeTime = takenAudio.first().decodeTime
                val lastAudio = audioSamples.last()
                fragments.add(
                    Mp4TrackFragment(
                        trackId = audioTrack.trackId,
                        baseMediaDecodeTime = firstAudioDecodeTime,
                        samples = audioSamples,
                        writeSampleFlags = false,
                    ),
                )
                reports.add(
                    Mp4TrackReport(
                        trackId = audioTrack.trackId,
                        isVideo = false,
                        earliestPresentationTimeUs = timescaleToUs(firstAudioDecodeTime, audioTrack.sampleRate),
                        durationUs = timescaleToUs(
                            takenAudio.last().decodeTime + lastAudio.duration - firstAudioDecodeTime,
                            audioTrack.sampleRate,
                        ),
                    ),
                )
            }
        }
        sequenceNumber += 1
        val data = makeMp4MediaSegment(sequenceNumber, fragments)
        Log.i(
            TAG,
            "segment #$sequenceNumber ${String.format(Locale.US, "%.3f", reports.first().durationUs / 1_000_000.0)} s " +
                "(${data.size} bytes, ${videoSamples.size} video and ${fragments.getOrNull(1)?.samples?.size ?: 0} " +
                "audio samples)",
        )
        listener.fragmentedMp4MuxerMediaSegment(data, reports)
    }

    private val loggedMessages = HashSet<String>()

    private fun logOnce(message: String) {
        if (loggedMessages.add(message)) {
            Log.i(TAG, message)
        }
    }
}

internal fun usToTimescale(timeUs: Long, timescale: Int): Long {
    return Math.floorDiv(timeUs * timescale + 500_000L, 1_000_000L)
}

internal fun timescaleToUs(time: Long, timescale: Int): Long {
    if (timescale <= 0) {
        return 0
    }
    return Math.floorDiv(time * 1_000_000L + timescale / 2, timescale.toLong())
}
