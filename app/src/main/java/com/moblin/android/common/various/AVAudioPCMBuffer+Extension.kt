package com.moblin.android.common.various

import android.media.AudioFormat
import android.media.MediaFormat
import com.moblin.android.media.MediaSample
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun ShortArray.makeSampleBuffer(
    presentationTimeStamp: Long,
    sampleRate: Int,
    channels: Int
): MediaSample? {
    val frameLength = if (channels > 0) {
        size / channels
    } else {
        0
    }
    if (frameLength <= 0) {
        return null
    }
    val byteBuffer = ByteBuffer.allocate(size * 2).order(ByteOrder.LITTLE_ENDIAN)
    for (sample in this) {
        byteBuffer.putShort(sample)
    }
    val format = MediaFormat.createAudioFormat(
        MediaFormat.MIMETYPE_AUDIO_RAW,
        sampleRate,
        channels
    )
    format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
    return MediaSample(
        data = byteBuffer.array(),
        presentationTimeUs = presentationTimeStamp,
        isKeyFrame = true,
        format = format
    )
}
