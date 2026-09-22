package com.moblin.android.media.mobcamstream

import com.moblin.android.media.haishinkit.util.ByteReader
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private fun packVideoFrame(presentationTimeStamp: ULong, isSync: Boolean, units: ByteArray): ByteArray {
    return packMobcamStreamVideoFrame(presentationTimeStamp, isSync, units)
}

private fun packAudioFrame(presentationTimeStamp: ULong, unit: ByteArray): ByteArray {
    return packMobcamStreamAudioFrame(presentationTimeStamp, unit)
}

private fun readAll(reader: MobcamStreamMessageReader): List<Pair<MobcamStreamMessageType, ByteArray>> {
    val messages = mutableListOf<Pair<MobcamStreamMessageType, ByteArray>>()
    while (true) {
        val message = reader.read() ?: break
        messages.add(message)
    }
    return messages
}

class MobcamStreamSuite {
    @Test
    fun hostHelloRoundTrip() {
        val reader = MobcamStreamMessageReader()
        reader.append(packMobcamStreamHostHello())
        val messages = readAll(reader)
        assertEquals(1, messages.size)
        assertEquals(MobcamStreamMessageType.hostHello, messages[0].first)
        unpackMobcamStreamHostHello(messages[0].second)
    }

    @Test
    fun hostHelloBadVersion() {
        val message = packMobcamStreamHostHello()
        message[message.size - 1] = (mobcamStreamProtocolVersion.toInt() + 1).toByte()
        val reader = MobcamStreamMessageReader()
        reader.append(message)
        val messages = readAll(reader)
        assertFailsWith<MobcamStreamProtocolError> {
            unpackMobcamStreamHostHello(messages[0].second)
        }
    }

    @Test
    fun deviceHello() {
        val reader = MobcamStreamMessageReader()
        reader.append(packMobcamStreamDeviceHello(MobcamStreamDeviceInfo("Erik", "1.2.3")))
        val messages = readAll(reader)
        assertEquals(1, messages.size)
        assertEquals(MobcamStreamMessageType.deviceHello, messages[0].first)
        val payload = ByteReader(messages[0].second)
        assertEquals(mobcamStreamProtocolVersion.toInt(), payload.readUInt8().toInt())
        val length = payload.readUInt32().toInt()
        val info = Json.decodeFromString<MobcamStreamDeviceInfo>(payload.readBytes(length).decodeToString())
        assertEquals("Erik", info.name)
        assertEquals("1.2.3", info.version)
    }

    @Test
    fun videoConfigAndFrame() {
        val reader = MobcamStreamMessageReader()
        reader.append(
            packMobcamStreamVideoConfig(MobcamStreamVideoCodec.hevc,
                                        1920.toUShort(),
                                        1080.toUShort(),
                                        byteArrayOf(1, 2, 3))
        )
        reader.append(packVideoFrame(0x0102_0304_0506_0708uL, true, byteArrayOf(9, 8, 7)))
        val messages = readAll(reader)
        assertEquals(2, messages.size)
        assertEquals(MobcamStreamMessageType.videoConfig, messages[0].first)
        val config = ByteReader(messages[0].second)
        assertEquals(MobcamStreamVideoCodec.hevc.rawValue.toInt(), config.readUInt8().toInt())
        assertEquals(1920, config.readUInt16().toInt())
        assertEquals(1080, config.readUInt16().toInt())
        assertEquals(3, config.readUInt32().toInt())
        assertContentEquals(byteArrayOf(1, 2, 3), config.readBytes(3))
        assertEquals(MobcamStreamMessageType.videoFrame, messages[1].first)
        val frame = ByteReader(messages[1].second)
        assertEquals(0x0102_0304_0506_0708uL.toLong(), frame.readUInt64().toLong())
        assertEquals(1, frame.readUInt8().toInt())
        assertContentEquals(byteArrayOf(9, 8, 7), frame.readBytes(3))
    }

    @Test
    fun audioConfigAndFrame() {
        val reader = MobcamStreamMessageReader()
        reader.append(
            packMobcamStreamAudioConfig(MobcamStreamAudioCodec.aac,
                                        48000u,
                                        2u,
                                        byteArrayOf(0x11, 0x90.toByte()))
        )
        reader.append(packAudioFrame(42uL, byteArrayOf(1, 2)))
        val messages = readAll(reader)
        assertEquals(2, messages.size)
        val config = ByteReader(messages[0].second)
        assertEquals(MobcamStreamAudioCodec.aac.rawValue.toInt(), config.readUInt8().toInt())
        assertEquals(48000, config.readUInt32().toInt())
        assertEquals(2, config.readUInt8().toInt())
        assertEquals(2, config.readUInt32().toInt())
        assertContentEquals(byteArrayOf(0x11, 0x90.toByte()), config.readBytes(2))
        val frame = ByteReader(messages[1].second)
        assertEquals(42L, frame.readUInt64().toLong())
        assertContentEquals(byteArrayOf(1, 2), frame.readBytes(2))
    }

    @Test
    fun opusAudioConfig() {
        val reader = MobcamStreamMessageReader()
        reader.append(
            packMobcamStreamAudioConfig(
                MobcamStreamAudioCodec.opus,
                48000u,
                2u,
                packMobcamStreamOpusHead(48000u, 2u)
            )
        )
        val messages = readAll(reader)
        assertEquals(1, messages.size)
        val config = ByteReader(messages[0].second)
        assertEquals(MobcamStreamAudioCodec.opus.rawValue.toInt(), config.readUInt8().toInt())
        assertEquals(48000, config.readUInt32().toInt())
        assertEquals(2, config.readUInt8().toInt())
        assertEquals(19, config.readUInt32().toInt())
        assertContentEquals(
            byteArrayOf(
                0x4F, 0x70, 0x75, 0x73, 0x48, 0x65, 0x61, 0x64,
                1,
                2,
                0, 0,
                0x80.toByte(), 0xBB.toByte(), 0x00, 0x00,
                0, 0,
                0,
            ),
            config.readBytes(19)
        )
    }

    @Test
    fun splitOverManyReceives() {
        val message = packVideoFrame(1uL, false, byteArrayOf(1, 2, 3))
        val reader = MobcamStreamMessageReader()
        for (byte in message.dropLast(1)) {
            reader.append(byteArrayOf(byte))
            assertNull(reader.read())
        }
        reader.append(byteArrayOf(message[message.size - 1]))
        val messages = readAll(reader)
        assertEquals(1, messages.size)
        assertEquals(MobcamStreamMessageType.videoFrame, messages[0].first)
    }

    @Test
    fun manyMessagesInOneReceive() {
        var data = ByteArray(0)
        for (index in 0 until 10) {
            data += packAudioFrame(index.toULong(), byteArrayOf(index.toByte()))
        }
        val reader = MobcamStreamMessageReader()
        reader.append(data)
        val messages = readAll(reader)
        assertEquals(10, messages.size)
        for ((index, message) in messages.withIndex()) {
            val frame = ByteReader(message.second)
            assertEquals(index.toLong(), frame.readUInt64().toLong())
            assertEquals(index, frame.readUInt8().toInt())
        }
    }

    @Test
    fun unknownMessageType() {
        val reader = MobcamStreamMessageReader()
        val data = ByteArray(5)
        data[0] = 0x7F
        reader.append(data)
        assertFailsWith<MobcamStreamProtocolError> {
            reader.read()
        }
    }
}
