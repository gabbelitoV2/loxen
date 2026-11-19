package com.moblin.android.media.haishinkit.rtmp

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import com.moblin.android.MessageQueue
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.ProcessorDelegate
import com.moblin.android.media.haishinkit.media.RecorderDataSegment
import com.moblin.android.platform.avfoundation.AVAudioCompressedBuffer
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val TAG = "RtmpModelMock"

class RtmpModelMock : RtmpStreamDelegate, ProcessorDelegate {
    private val disconnected = MessageQueue<RtmpDisconnectReason>()
    private val connected = MessageQueue<Unit>()
    private val lock = ReentrantLock()
    private val disconnectedReasons = mutableListOf<RtmpDisconnectReason>()

    fun getDisconnectedReasons(): List<RtmpDisconnectReason> = lock.withLock { disconnectedReasons.toList() }

    suspend fun waitForDisconnected(): RtmpDisconnectReason = disconnected.get()

    suspend fun waitForConnected() {
        connected.get()
    }

    override fun rtmpStreamConnected(rtmpStream: RtmpStream) {
        Log.i(TAG, "rtmp-test: Connected")
        connected.put(Unit)
    }

    override fun rtmpStreamDisconnected(rtmpStream: RtmpStream, reason: RtmpDisconnectReason) {
        Log.i(TAG, "rtmp-test: Disconnected $reason")
        lock.withLock { disconnectedReasons.add(reason) }
        disconnected.put(reason)
    }

    override fun streamAudioLevel(audioLevel: Float, numberOfAudioChannels: Int, sampleRate: Double) {}

    override fun streamLowFpsImage(lowFpsImage: ByteArray?, frameNumber: Long) {}

    override fun streamVideoAttachCameraError() {}

    override fun streamVideoCaptureSessionError(message: String) {}

    override fun streamVideoBufferedVideoReady(cameraId: UUID) {}

    override fun streamVideoBufferedVideoRemoved(cameraId: UUID) {}

    override fun streamVideoFps(fps: Int) {}

    override fun streamVideoEncoderResolution(resolution: Size) {}

    override fun streamRecorderInitSegment(data: ByteArray) {}

    override fun streamRecorderDataSegment(segment: RecorderDataSegment) {}

    override fun streamRecorderFinished() {}

    override fun streamAudio(sampleBuffer: MediaSample) {}

    override fun streamNoTorch() {}

    override fun streamPhotoTaken() {}

    override fun streamSetZoomX(x: Float) {}

    override fun streamSetExposureBias(bias: Float) {}

    override fun streamSelectedFps(auto: Boolean) {}
}

fun makeAacFormat(): MediaFormat = makeAudioFormat(
    formatId = MediaFormat.MIMETYPE_AUDIO_AAC,
    formatFlags = MediaCodecInfo.CodecProfileLevel.AACObjectLC,
    framesPerPacket = 1024,
)

fun makeOpusFormat(): MediaFormat = makeAudioFormat(
    formatId = MediaFormat.MIMETYPE_AUDIO_OPUS,
    formatFlags = 0,
    framesPerPacket = 960,
)

@Suppress("UNUSED_PARAMETER")
private fun makeAudioFormat(formatId: String, formatFlags: Int, framesPerPacket: Int): MediaFormat {
    val format = MediaFormat.createAudioFormat(formatId, 48000, 2)
    if (formatId == MediaFormat.MIMETYPE_AUDIO_AAC) {
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, formatFlags)
    }
    return format
}

fun makeAudioBuffer(format: MediaFormat, payload: ByteArray): MediaSample {
    val buffer = AVAudioCompressedBuffer(format = format, packetCapacity = 1, maximumPacketSize = 64)
    buffer.data = payload.copyOf()
    return buffer
}

fun makeH264FormatDescription(): MediaFormat {
    val sps = byteArrayOf(0x67, 0x42, 0x00, 0x0A, 0xF8.toByte(), 0x41, 0xA2.toByte())
    val pps = byteArrayOf(0x68, 0xCE.toByte(), 0x38, 0x80.toByte())
    val avcC = ByteArray(11 + sps.size + pps.size)
    avcC[0] = 0x01
    avcC[1] = sps[1]
    avcC[2] = sps[2]
    avcC[3] = sps[3]
    avcC[4] = 0xFF.toByte()
    avcC[5] = 0xE1.toByte()
    avcC[6] = ((sps.size shr 8) and 0xFF).toByte()
    avcC[7] = (sps.size and 0xFF).toByte()
    sps.copyInto(avcC, 8)
    avcC[8 + sps.size] = 0x01
    avcC[9 + sps.size] = ((pps.size shr 8) and 0xFF).toByte()
    avcC[10 + sps.size] = (pps.size and 0xFF).toByte()
    pps.copyInto(avcC, 11 + sps.size)
    val format = MediaFormat()
    format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_AVC)
    format.setByteBuffer("avcC", ByteBuffer.wrap(avcC))
    return format
}

fun makeVideoSampleBuffer(
    formatDescription: MediaFormat,
    decodeTimeStamp: Long,
    presentationTimeStamp: Long,
    isSync: Boolean,
    payload: ByteArray,
): MediaSample = MediaSample(
    data = payload,
    presentationTimeUs = presentationTimeStamp,
    isKeyFrame = isSync,
    format = formatDescription,
    decodeTimeStampUs = decodeTimeStamp,
)

fun mediaTime(value: Long): Long = value * 1_000_000L / 1024
