package com.moblin.android.integrations.dji.djidevice

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlin.test.assertEquals
import org.junit.Test
import com.moblin.android.various.settings.SettingsDjiDeviceModel
import com.moblin.android.various.settings.SettingsDjiDeviceResolution
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun ByteArray.hexString(): String = joinToString("") { "%02x".format(it) }

private fun JsonElement.asNsObject(): JsonElement = when (this) {
    is JsonObject -> JsonObject(mapValues { it.value.asNsObject() })
    is JsonPrimitive -> if (isString) this else booleanOrNull?.let { JsonPrimitive(if (it) 1 else 0) } ?: this
    else -> this
}

@RunWith(RobolectricTestRunner::class)
class DjiDeviceSuite {
    @Test
    fun startStreamingOmsoPocket4() {
        val payload = DjiStartStreamingMessagePayload2(
            rtmpUrl = "rtmp://192.168.1.59/live/1",
            resolution = SettingsDjiDeviceResolution.r1080p,
            fps = 30,
            bitrateKbps = 5000u.toUShort(),
            codec = "HEVC",
            enhancedRtmp = true,
            middle = DjiStartStreamingMessagePayload2.osmoPocket4Middle,
        )
        val encoded = payload.encode()
        assertEquals(161, encoded.size)
        assertEquals(1, encoded[0].toInt() and 0xFF)
        assertEquals(158, (encoded[1].toInt() and 0xFF) or ((encoded[2].toInt() and 0xFF) shl 8))
        assertEquals(
            "0a88130201030000009300",
            encoded.copyOfRange(3, 14).hexString(),
        )
        val json = Json.parseToJsonElement(encoded.copyOfRange(14, 161).toString(Charsets.UTF_8))
        assertEquals(
            Json.parseToJsonElement(
                """
                {
                    "EnhancedRTMP": 1,
                    "codec": "HEVC",
                    "orientation": "landscape",
                    "rtmpAddress": "rtmp://192.168.1.59/live/1",
                    "supportStopLive": 0,
                    "watermark": 0
                }
                """.trimIndent(),
            ).asNsObject(),
            json.asNsObject(),
        )
    }

    @Test
    fun startStreamingOmsoAction6() {
        val payload = DjiStartStreamingMessagePayload2(
            rtmpUrl = "rtmp://192.168.1.59/live/123456789",
            resolution = SettingsDjiDeviceResolution.r720p,
            fps = 30,
            bitrateKbps = 7000u.toUShort(),
            codec = "AVC",
            enhancedRtmp = false,
            middle = DjiStartStreamingMessagePayload2.osmoAction6Middle,
        )
        val encoded = payload.encode()
        assertEquals(169, encoded.size)
        assertEquals(1, encoded[0].toInt() and 0xFF)
        assertEquals(166, (encoded[1].toInt() and 0xFF) or ((encoded[2].toInt() and 0xFF) shl 8))
        assertEquals(
            "04581bfe00030000009b00",
            encoded.copyOfRange(3, 14).hexString(),
        )
        val json = Json.parseToJsonElement(encoded.copyOfRange(14, 169).toString(Charsets.UTF_8))
        assertEquals(
            Json.parseToJsonElement(
                """
                {
                    "EnhancedRTMP": 0,
                    "codec": "AVC",
                    "orientation": "landscape",
                    "rtmpAddress": "rtmp://192.168.1.59/live/123456789",
                    "supportStopLive": 0,
                    "watermark": 0
                }
                """.trimIndent(),
            ).asNsObject(),
            json.asNsObject(),
        )
    }

    @Test
    fun startStreamingOsmoAction4() {
        val payload = DjiStartStreamingMessagePayload(
            rtmpUrl = "rtmp://110.144.9.240:1935/publish/live",
            resolution = SettingsDjiDeviceResolution.r1080p,
            bitrateKbps = 6000u.toUShort(),
            fps = 30,
        )
        assertEquals(
            "0031000a7017020003000000260072746d703a2f2f3131302e3134342e392e3234303a313933352f7075626c6973682f6c697665",
            payload.encode().hexString(),
        )
    }

    @Test
    fun modelOsmoAction6() {
        val data = byteArrayOf(
            0xAA.toByte(), 0x08, 0x18, 0x00, 0xFA.toByte(), 0x0C, 0x9A.toByte(), 0xE6.toByte(),
            0xA0.toByte(), 0x97.toByte(), 0x56, 0x00,
        )
        assertEquals(SettingsDjiDeviceModel.osmoAction6, djiModelFromManufacturerData(data))
    }

    @Test
    fun modelOsmoPocket4Pro() {
        var data = byteArrayOf(
            0xAA.toByte(), 0x08, 0x00, 0x00, 0x00, 0xCF.toByte(), 0x00, 0x04,
            0x76, 0xEA.toByte(), 0x8B.toByte(), 0x20, 0xDA.toByte(), 0x00, 0x00, 0x10,
        )
        assertEquals(SettingsDjiDeviceModel.osmoPocket4Pro, djiModelFromManufacturerData(data))
        data = byteArrayOf(
            0xAA.toByte(), 0x08, 0x00, 0x00, 0x00, 0xC1.toByte(), 0x00, 0x04,
            0xFD.toByte(), 0x9C.toByte(), 0xD3.toByte(), 0x20, 0xDA.toByte(), 0x00, 0x00, 0x40,
        )
        assertEquals(SettingsDjiDeviceModel.osmoPocket4Pro, djiModelFromManufacturerData(data))
    }
}
