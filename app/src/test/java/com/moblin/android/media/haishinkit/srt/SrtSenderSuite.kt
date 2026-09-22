package com.moblin.android.media.haishinkit.srt

import com.moblin.android.MessageQueue
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

private class ModelMock : SrtSenderDelegate {
    private val connected = MessageQueue<Unit>()
    private val disconnected = MessageQueue<Unit>()
    private val packets = MessageQueue<String>()

    suspend fun waitForConnected() {
        connected.get()
    }

    suspend fun waitForDisconnected() {
        disconnected.get()
    }

    suspend fun waitForPacket(): String {
        return packets.get()
    }

    override fun srtSenderConnected() {
        connected.put(Unit)
    }

    override fun srtSenderDisconnected() {
        disconnected.put(Unit)
    }

    override fun srtSenderOutput(packet: ByteArray) {
        packets.put(packet.hexString())
    }
}

private fun ByteArray.hexString(): String = joinToString(separator = "") { "%02x".format(it.toInt() and 0xff) }

private fun dataFromHexString(hexString: String): ByteArray {
    val data = ByteArray(hexString.length / 2)
    for (index in data.indices) {
        data[index] = hexString.substring(index * 2, index * 2 + 2).toInt(16).toByte()
    }
    return data
}

class SrtSenderSuite {
    @Test
    fun connectDisconnect() {
        runBlocking {
            val sender = SrtSender(streamId = "1234", latency = 2000, experimental = false)
            val model = ModelMock()
            sender.delegate = model
            sender.start()
            checkInductionHandshake(model.waitForPacket())
            sender.input(createInductionHandshake())
            checkConclusionHandshake(model.waitForPacket())
            sender.input(createConclusionHandshake())
            model.waitForConnected()
            sender.send(now = Instant.now().plusSeconds(6))
            model.waitForDisconnected()
        }
    }

    private fun checkInductionHandshake(packet: String): Pair<UInt, UInt> {
        assertEquals(128, packet.length)
        assertEquals("8000000000000000", packet.substring(0, 16))
        val timestamp = packet.substring(16, 24).toUInt(16)
        assertEquals("000000000000000400000002", packet.substring(24, 48))
        val sequenceNumber = packet.substring(48, 56).toUInt(16)
        assertEquals("000005dc0000200000000001", packet.substring(56, 80))
        assertEquals("000000000100007f000000000000000000000000", packet.substring(88, 128))
        return Pair(timestamp, sequenceNumber)
    }

    private fun createInductionHandshake(): ByteArray = dataFromHexString(
        "80000000000000000000000000000000000000040000000200000fe6000005dc" +
            "00002000000000012ab1f77c000000000100007f000000000000000000000000"
    )

    private fun checkConclusionHandshake(packet: String): Pair<UInt, UInt> {
        assertEquals(176, packet.length)
        assertEquals("8000000000000000", packet.substring(0, 16))
        val timestamp = packet.substring(16, 24).toUInt(16)
        assertEquals("000000000000000500000005", packet.substring(24, 48))
        val sequenceNumber = packet.substring(48, 56).toUInt(16)
        assertEquals(
            "000005dc00002000ffffffff2ab1f77c000000000100007f000000000000" +
                "0000000000000001000300010503000000bf07d007d00005000134333231",
            packet.substring(56, 176)
        )
        return Pair(timestamp, sequenceNumber)
    }

    private fun createConclusionHandshake(): ByteArray = dataFromHexString(
        "800000000000000000000000000000000000000500000005000000000000" +
            "05dc00002000ffffffff2ab1f77c000000000100007f0000000000000000" +
            "000000000001000300010503000000bf07d007d00005000134333231"
    )
}
