package com.moblin.android.integrations.dji.djidevice

import kotlinx.serialization.json.Json
import kotlin.test.assertEquals
import org.junit.Test
import com.moblin.android.various.settings.SettingsDjiDeviceResolution
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun ByteArray.hexString(): String = joinToString("") { "%02x".format(it) }

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
            header = DjiStartStreamingMessagePayload2.osmoPocket4Header,
            middle = DjiStartStreamingMessagePayload2.osmoPocket4Middle,
        )
        val encoded = payload.encode()
        assertEquals(161, encoded.size)
        assertEquals(
            "01b5000a88130201030000009300",
            encoded.copyOfRange(0, 14).hexString(),
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
            ),
            json,
        )
    }

    @Test
    fun startStreamingOmsoAction6() {
        val payload = DjiStartStreamingMessagePayload2(
            rtmpUrl = "rtmp://192.168.1.59/live/2",
            resolution = SettingsDjiDeviceResolution.r720p,
            fps = 30,
            bitrateKbps = 7000u.toUShort(),
            codec = "AVC",
            enhancedRtmp = false,
            header = DjiStartStreamingMessagePayload2.osmoAction6Header,
            middle = DjiStartStreamingMessagePayload2.osmoAction6Middle,
        )
        val encoded = payload.encode()
        assertEquals(161, encoded.size)
        assertEquals(
            "019c0004581bfe00030000009300",
            encoded.copyOfRange(0, 14).hexString(),
        )
        val json = Json.parseToJsonElement(encoded.copyOfRange(14, 161).toString(Charsets.UTF_8))
        assertEquals(
            Json.parseToJsonElement(
                """
                {
                    "EnhancedRTMP": 0,
                    "codec": "AVC",
                    "orientation": "landscape",
                    "rtmpAddress": "rtmp://192.168.1.59/live/2",
                    "supportStopLive": 0,
                    "watermark": 0
                }
                """.trimIndent(),
            ),
            json,
        )
    }
}
