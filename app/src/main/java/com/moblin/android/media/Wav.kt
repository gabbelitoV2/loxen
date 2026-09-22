package com.moblin.android.media

import com.moblin.android.media.haishinkit.util.ByteWriter

fun createWav(sampleRate: Int, samples: List<List<Short>>): ByteArray? {
    val numberOfChannels = samples.size
    val dataSize: UInt
    when (numberOfChannels) {
        1 -> {
            dataSize = (samples[0].size * 2).toUInt()
        }
        2 -> {
            if (samples[0].size != samples[1].size) {
                return null
            }
            dataSize = (samples[0].size * 2 * 2).toUInt()
        }
        else -> {
            return null
        }
    }
    val writer = ByteWriter()
    writer.writeUTF8Bytes("RIFF")
    writer.writeUInt32Le(36u + dataSize)
    writer.writeUTF8Bytes("WAVE")
    writer.writeUTF8Bytes("fmt ")
    writer.writeUInt32Le(16u)
    writer.writeUInt16Le(1.toUShort())
    writer.writeUInt16Le(numberOfChannels.toUShort())
    writer.writeUInt32Le(sampleRate.toUInt())
    writer.writeUInt32Le(0x02EE00u)
    writer.writeUInt16Le(4.toUShort())
    writer.writeUInt16Le(16.toUShort())
    writer.writeUTF8Bytes("data")
    writer.writeUInt32Le(dataSize)
    when (numberOfChannels) {
        1 -> {
            for (sample in samples[0]) {
                writer.writeUInt16Le(sample.toUShort())
            }
        }
        2 -> {
            for ((sampleRight, sampleLeft) in samples[0].zip(samples[1])) {
                writer.writeUInt16Le(sampleRight.toUShort())
                writer.writeUInt16Le(sampleLeft.toUShort())
            }
        }
        else -> {
            return null
        }
    }
    return writer.data
}
