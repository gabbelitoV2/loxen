package com.moblin.android.integrations.dji.djidevice

import com.moblin.android.integrations.dji.djiPackString
import com.moblin.android.integrations.dji.djiPackUrl
import com.moblin.android.media.haishinkit.util.ByteWriter
import com.moblin.android.various.settings.SettingsDjiDeviceImageStabilization
import com.moblin.android.various.settings.SettingsDjiDeviceResolution
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class DjiPairMessagePayload(var pairPinCode: String) {
    companion object {
        val payload: ByteArray = byteArrayOf(
            0x20, 0x32, 0x38, 0x34, 0x61, 0x65, 0x35, 0x62,
            0x38, 0x64, 0x37, 0x36, 0x62, 0x33, 0x33, 0x37,
            0x35, 0x61, 0x30, 0x34, 0x61, 0x36, 0x34, 0x31,
            0x37, 0x61, 0x64, 0x37, 0x31, 0x62, 0x65, 0x61,
            0x33,
        )
    }

    fun encode(): ByteArray {
        val writer = ByteWriter()
        writer.writeBytes(payload)
        writer.writeBytes(djiPackString(pairPinCode))
        return writer.data
    }
}

object DjiPreparingToLivestreamMessagePayload {
    val payload: ByteArray = byteArrayOf(0x1A)

    fun encode(): ByteArray = payload
}

data class DjiSetupWifiMessagePayload(
    var wifiSsid: String,
    var wifiPassword: String,
) {
    fun encode(): ByteArray {
        val writer = ByteWriter()
        writer.writeBytes(djiPackString(wifiSsid))
        writer.writeBytes(djiPackString(wifiPassword))
        return writer.data
    }
}

data class DjiStartStreamingMessagePayload(
    var rtmpUrl: String,
    var resolution: SettingsDjiDeviceResolution,
    var bitrateKbps: UShort,
    var fps: Int,
    var oa5: Boolean,
) {
    companion object {
        val payload1: ByteArray = byteArrayOf(0x00)
        val payload2: ByteArray = byteArrayOf(0x00)
        val payload3: ByteArray = byteArrayOf(0x02, 0x00)
        val payload4: ByteArray = byteArrayOf(0x00, 0x00, 0x00)
    }

    fun encode(): ByteArray {
        val oa5Byte: UByte = if (oa5) {
            0x2A.toUByte()
        } else {
            0x2E.toUByte()
        }
        val writer = ByteWriter()
        writer.writeBytes(payload1)
        writer.writeUInt8(oa5Byte)
        writer.writeBytes(payload2)
        writer.writeUInt8(toDjiResolution(resolution))
        writer.writeUInt16Le(bitrateKbps)
        writer.writeBytes(payload3)
        writer.writeUInt8(toDjiFps(fps))
        writer.writeBytes(payload4)
        writer.writeBytes(djiPackUrl(rtmpUrl))
        return writer.data
    }
}

object DjiConfirmStartStreamingMessagePayload {
    val payload: ByteArray = byteArrayOf(0x01, 0x01, 0x1A, 0x00, 0x01, 0x01)

    fun encode(): ByteArray = payload
}

@Serializable
private data class StartStreamingPayload(
    val codec: String,
    val EnhancedRTMP: Boolean,
    val supportStopLive: Boolean,
    val watermark: Int,
    val rtmpAddress: String,
    val orientation: String,
)

data class DjiStartStreamingMessagePayload2(
    var rtmpUrl: String,
    var resolution: SettingsDjiDeviceResolution,
    var bitrateKbps: UShort,
    var fps: Int,
    var codec: String,
    var enhancedRtmp: Boolean,
    val header: ByteArray,
    val middle: ByteArray,
) {
    companion object {
        val osmoAction6Header: ByteArray = byteArrayOf(0x01, 0x9C.toByte(), 0x00)
        val osmoAction6Middle: ByteArray = byteArrayOf(0xFE.toByte(), 0x00)
        val osmoPocket4Header: ByteArray = byteArrayOf(0x01, 0xB5.toByte(), 0x00)
        val osmoPocket4Middle: ByteArray = byteArrayOf(0x02, 0x01)
        private val padding: ByteArray = byteArrayOf(0x00, 0x00, 0x00)
    }

    fun encode(): ByteArray {
        val payload = StartStreamingPayload(
            codec = codec,
            EnhancedRTMP = enhancedRtmp,
            supportStopLive = false,
            watermark = 0,
            rtmpAddress = rtmpUrl,
            orientation = "landscape",
        )
        val data = runCatching { Json.encodeToString(payload) }
            .getOrNull()
            ?.encodeToByteArray()
            ?: ByteArray(0)
        val writer = ByteWriter()
        writer.writeBytes(header)
        writer.writeUInt8(toDjiResolution(resolution))
        writer.writeUInt16Le(bitrateKbps)
        writer.writeBytes(middle)
        writer.writeUInt8(toDjiFps(fps))
        writer.writeBytes(padding)
        writer.writeUInt16Le(data.size.toUShort())
        writer.writeBytes(data)
        return writer.data
    }
}

object DjiStopStreamingMessagePayload {
    val payload: ByteArray = byteArrayOf(0x01, 0x01, 0x1A, 0x00, 0x01, 0x02)

    fun encode(): ByteArray = payload
}

data class DjiConfigureMessagePayload(
    var imageStabilization: SettingsDjiDeviceImageStabilization,
    var oa5: Boolean,
) {
    companion object {
        val payload1: ByteArray = byteArrayOf(0x01, 0x01)
        val payload2: ByteArray = byteArrayOf(0x00, 0x01)
    }

    fun encode(): ByteArray {
        val imageStabilizationByte: UByte = when (imageStabilization) {
            SettingsDjiDeviceImageStabilization.OFF -> 0.toUByte()
            SettingsDjiDeviceImageStabilization.ROCK_STEADY -> 1.toUByte()
            SettingsDjiDeviceImageStabilization.ROCK_STEADY_PLUS -> 3.toUByte()
            SettingsDjiDeviceImageStabilization.HORIZON_BALANCING -> 4.toUByte()
            SettingsDjiDeviceImageStabilization.HORIZON_STEADY -> 2.toUByte()
        }
        val byte1: UByte = if (oa5) {
            0x1A.toUByte()
        } else {
            0x08.toUByte()
        }
        val writer = ByteWriter()
        writer.writeBytes(payload1)
        writer.writeUInt8(byte1)
        writer.writeBytes(payload2)
        writer.writeUInt8(imageStabilizationByte)
        return writer.data
    }
}

data class DjiStatusMessagePayload private constructor(val batteryPercentage: UByte) {
    companion object {
        operator fun invoke(payload: ByteArray): DjiStatusMessagePayload? {
            if (payload.size < 21) {
                return null
            }
            return DjiStatusMessagePayload(payload[20].toUByte())
        }
    }
}

private fun toDjiFps(fps: Int): UByte = when (fps) {
    25 -> 2.toUByte()
    30 -> 3.toUByte()
    else -> 0.toUByte()
}

private fun toDjiResolution(resolution: SettingsDjiDeviceResolution): UByte = when (resolution) {
    SettingsDjiDeviceResolution.R480P -> 0x47.toUByte()
    SettingsDjiDeviceResolution.R720P -> 0x04.toUByte()
    SettingsDjiDeviceResolution.R1080P -> 0x0A.toUByte()
}
