package com.moblin.android.media.haishinkit.rtmp

import com.moblin.android.media.haishinkit.util.ByteWriter
import java.time.Instant
import kotlin.random.Random

object RtmpHandshake {
    const val sigSize: Int = 1536
    const val protocolVersion: UByte = 3u
    private const val timestamp: Double = 0.0

    fun createC0C1Packet(): ByteArray {
        val writer = ByteWriter()
        writer.writeUInt8(protocolVersion)
        writer.writeInt32(timestamp.toInt())
        writer.writeBytes(byteArrayOf(0x00, 0x00, 0x00, 0x00))
        repeat(sigSize - 8) {
            writer.writeUInt8(Random.nextInt(0, 256).toUByte())
        }
        return writer.data
    }

    fun createC2Packet(s0s1packet: ByteArray): ByteArray {
        val writer = ByteWriter()
        writer.writeBytes(s0s1packet.copyOfRange(1, 5))
        writer.writeUInt32((Instant.now().epochSecond - timestamp.toLong()).toUInt())
        writer.writeBytes(s0s1packet.copyOfRange(9, sigSize + 1))
        return writer.data
    }
}
