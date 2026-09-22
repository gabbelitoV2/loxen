package com.moblin.android.media.mobcamstream

import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.media.haishinkit.util.ByteWriter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val mobcamStreamProtocolVersion: UByte = 1u

enum class MobcamStreamMessageType(val rawValue: UByte) {
    hostHello(0x01u),
    deviceHello(0x02u),
    videoConfig(0x03u),
    videoFrame(0x04u),
    audioConfig(0x05u),
    audioFrame(0x06u);

    companion object {
        fun fromRawValue(rawValue: UByte): MobcamStreamMessageType? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

enum class MobcamStreamVideoCodec(val rawValue: UByte) {
    h264(0u),
    hevc(1u);

    companion object {
        fun fromRawValue(rawValue: UByte): MobcamStreamVideoCodec? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

enum class MobcamStreamAudioCodec(val rawValue: UByte) {
    aac(0u),
    opus(1u);

    companion object {
        fun fromRawValue(rawValue: UByte): MobcamStreamAudioCodec? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

sealed class MobcamStreamProtocolError(message: String) : Exception(message) {
    class UnsupportedVersion(val version: UByte) :
        MobcamStreamProtocolError("Unsupported version: $version")

    class UnknownMessageType(val type: UByte) :
        MobcamStreamProtocolError("Unknown message type: $type")
}

@Serializable
data class MobcamStreamDeviceInfo(
    val name: String,
    val version: String,
)

private fun packMobcamStreamMessage(
    type: MobcamStreamMessageType,
    payloadLength: Int,
): ByteWriter {
    val writer = ByteWriter(ByteArray(5 + payloadLength))
    writer.writeUInt8(type.rawValue)
    writer.writeUInt32(payloadLength.toUInt())
    return writer
}

fun packMobcamStreamHostHello(): ByteArray {
    val writer = packMobcamStreamMessage(MobcamStreamMessageType.hostHello, 1)
    writer.writeUInt8(mobcamStreamProtocolVersion)
    return writer.data
}

fun unpackMobcamStreamHostHello(payload: ByteArray) {
    val reader = ByteReader(payload)
    val version = reader.readUInt8()
    if (version != mobcamStreamProtocolVersion) {
        throw MobcamStreamProtocolError.UnsupportedVersion(version)
    }
}

fun packMobcamStreamDeviceHello(info: MobcamStreamDeviceInfo): ByteArray {
    val encoded = runCatching { Json.encodeToString(info) }
        .getOrNull()
        ?.encodeToByteArray()
        ?: ByteArray(0)
    val writer = packMobcamStreamMessage(MobcamStreamMessageType.deviceHello, 5 + encoded.size)
    writer.writeUInt8(mobcamStreamProtocolVersion)
    writer.writeUInt32(encoded.size.toUInt())
    writer.writeBytes(encoded)
    return writer.data
}

fun packMobcamStreamVideoConfig(
    codec: MobcamStreamVideoCodec,
    width: UShort,
    height: UShort,
    configurationRecord: ByteArray,
): ByteArray {
    val writer = packMobcamStreamMessage(
        MobcamStreamMessageType.videoConfig,
        9 + configurationRecord.size,
    )
    writer.writeUInt8(codec.rawValue)
    writer.writeUInt16(width)
    writer.writeUInt16(height)
    writer.writeUInt32(configurationRecord.size.toUInt())
    writer.writeBytes(configurationRecord)
    return writer.data
}

fun packMobcamStreamVideoFrame(
    presentationTimeStamp: ULong,
    isSync: Boolean,
    units: ByteArray,
): ByteArray {
    val writer = packMobcamStreamMessage(MobcamStreamMessageType.videoFrame, 9 + units.size)
    writer.writeUInt64(presentationTimeStamp)
    writer.writeUInt8(if (isSync) 1u else 0u)
    writer.writeBytes(units)
    return writer.data
}

fun packMobcamStreamAudioConfig(
    codec: MobcamStreamAudioCodec,
    sampleRate: UInt,
    channels: UByte,
    configurationRecord: ByteArray,
): ByteArray {
    val writer = packMobcamStreamMessage(
        MobcamStreamMessageType.audioConfig,
        10 + configurationRecord.size,
    )
    writer.writeUInt8(codec.rawValue)
    writer.writeUInt32(sampleRate)
    writer.writeUInt8(channels)
    writer.writeUInt32(configurationRecord.size.toUInt())
    writer.writeBytes(configurationRecord)
    return writer.data
}

fun packMobcamStreamOpusHead(sampleRate: UInt, channels: UByte): ByteArray {
    val writer = ByteWriter(ByteArray(19))
    writer.writeUTF8Bytes("OpusHead")
    writer.writeUInt8(1u)
    writer.writeUInt8(channels)
    writer.writeUInt16Le(0u)
    writer.writeUInt32Le(sampleRate)
    writer.writeUInt16Le(0u)
    writer.writeUInt8(0u)
    return writer.data
}

fun packMobcamStreamAudioFrame(presentationTimeStamp: ULong, unit: ByteArray): ByteArray {
    val writer = packMobcamStreamMessage(MobcamStreamMessageType.audioFrame, 8 + unit.size)
    writer.writeUInt64(presentationTimeStamp)
    writer.writeBytes(unit)
    return writer.data
}

class MobcamStreamMessageReader {
    private var buffer = ByteArray(0)

    fun append(data: ByteArray) {
        buffer += data
    }

    fun read(): Pair<MobcamStreamMessageType, ByteArray>? {
        if (buffer.size < 5) {
            return null
        }
        val reader = ByteReader(buffer)
        val rawType = reader.readUInt8()
        val type = MobcamStreamMessageType.fromRawValue(rawType)
            ?: throw MobcamStreamProtocolError.UnknownMessageType(rawType)
        val length = reader.readUInt32().toInt()
        if (reader.bytesAvailable < length) {
            return null
        }
        val payload = reader.readBytes(length)
        buffer = buffer.copyOfRange(reader.position, buffer.size)
        return type to payload
    }
}
