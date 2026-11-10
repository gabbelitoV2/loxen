package com.moblin.android.various

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test
import com.moblin.android.localized
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MoblinSettingsUrlSuite {
    class InvalidCase(
        val description: String,
        val query: String,
        val message: String,
    )

    @Test
    fun invalid() {
        val cases = listOf(
            InvalidCase(
                "stream url without port",
                """{"streams":[{"name":"a","url":"srt://host"}]}""",
                localized("SRT(LA) port number missing"),
            ),
            InvalidCase(
                "negative srt latency",
                """{"streams":[{"name":"a","url":"srt://host:5000","srt":{"latency":-1}}]}""",
                localized("SRT latency out of range"),
            ),
            InvalidCase(
                "too big srt latency",
                """{"streams":[{"name":"a","url":"srt://host:5000","srt":{"latency":65536}}]}""",
                localized("SRT latency out of range"),
            ),
            InvalidCase(
                "obs websocket url with http scheme",
                """{"streams":[{"name":"a","url":"srt://host:5000",
"obs":{"webSocketUrl":"http://host:4455","webSocketPassword":""}}]}""",
                localized("Unsupported scheme ${"http"}"),
            ),
            InvalidCase(
                "moblin cloud url with ftp scheme",
                """{"moblinCloud":{"enabled":true,"key":"k","url":"ftp://host"}}""",
                localized("Unsupported scheme ${"ftp"}"),
            ),
            InvalidCase(
                "unsupported video fps",
                """{"streams":[{"name":"a","url":"srt://host:5000","video":{"fps":24}}]}""",
                localized("Unsupported video FPS"),
            ),
            InvalidCase(
                "too small video bitrate",
                """{"streams":[{"name":"a","url":"srt://host:5000","video":{"bitrate":49999}}]}""",
                localized("Video bitrate out of range"),
            ),
            InvalidCase(
                "too big video bitrate",
                """{"streams":[{"name":"a","url":"srt://host:5000","video":{"bitrate":50000001}}]}""",
                localized("Video bitrate out of range"),
            ),
            InvalidCase(
                "negative max key frame interval",
                """{"streams":[{"name":"a","url":"srt://host:5000","video":{"maxKeyFrameInterval":-1}}]}""",
                localized("Max key frame interval out of range"),
            ),
            InvalidCase(
                "too big max key frame interval",
                """{"streams":[{"name":"a","url":"srt://host:5000","video":{"maxKeyFrameInterval":11}}]}""",
                localized("Max key frame interval out of range"),
            ),
            InvalidCase(
                "audio bitrate not a multiple of 32000",
                """{"streams":[{"name":"a","url":"srt://host:5000","audio":{"bitrate":100000}}]}""",
                localized("Unsupported audio bitrate"),
            ),
            InvalidCase(
                "too big audio bitrate",
                """{"streams":[{"name":"a","url":"srt://host:5000","audio":{"bitrate":352000}}]}""",
                localized("Unsupported audio bitrate"),
            ),
            InvalidCase(
                "remote control streamer url with http scheme",
                """{"remoteControl":{"password":"p","streamer":{"enabled":true,"url":"http://host:2345"}}}""",
                localized("Unsupported scheme ${"http"}"),
            ),
            InvalidCase(
                "remote control relay base url with https scheme",
                """{"remoteControl":{"password":"p","assistant":{"enabled":true,"port":2345,
"relay":{"enabled":true,"baseUrl":"https://relay","bridgeId":"b"}}}}""",
                localized("Unsupported scheme ${"https"}"),
            ),
            InvalidCase(
                "empty remote control relay bridge id",
                """{"remoteControl":{"password":"p","assistant":{"enabled":true,"port":2345,
"relay":{"enabled":true,"baseUrl":"wss://relay","bridgeId":" "}}}}""",
                localized("Empty remote control relay bridge id"),
            ),
            InvalidCase(
                "remote control assistant port 0",
                """{"remoteControl":{"password":"p","assistant":{"enabled":true,"port":0}}}""",
                localized("Invalid remote control assistant port"),
            ),
            InvalidCase(
                "quick button page 0",
                """{"quickButtons":{"buttons":[{"type":"Torch","page":0}]}}""",
                localized("Quick button page out of range"),
            ),
            InvalidCase(
                "unknown quick button type",
                """{"quickButtons":{"buttons":[{"type":"Torhc"}]}}""",
                localized("Unknown quick button type"),
            ),
            InvalidCase(
                "unsupported video resolution",
                """{"streams":[{"name":"a","url":"srt://host:5000","video":{"resolution":"1234x5"}}]}""",
                localized("Unsupported video resolution"),
            ),
            InvalidCase(
                "unsupported video codec",
                """{"streams":[{"name":"a","url":"srt://host:5000","video":{"codec":"VP8"}}]}""",
                localized("Unsupported video codec"),
            ),
            InvalidCase(
                "unsupported srt dns lookup strategy",
                """{"streams":[{"name":"a","url":"srt://host:5000","srt":{"dnsLookupStrategy":"IPv5"}}]}""",
                localized("Unsupported SRT DNS lookup strategy"),
            ),
            InvalidCase(
                "quick button page 6",
                """{"quickButtons":{"buttons":[{"type":"Torch","page":6}]}}""",
                localized("Quick button page out of range"),
            ),
        )
        for (invalidCase in cases) {
            val error = assertFailsWith<Throwable> {
                MoblinSettingsUrl.fromString(query = invalidCase.query)
            }
            assertEquals(invalidCase.message, error.message)
        }
    }

    class ValidCase(
        val description: String,
        val query: String,
    )

    @Test
    fun valid() {
        val cases = listOf(
            ValidCase(
                "video fps 30",
                """{"streams":[{"name":"a","url":"srt://host:5000","video":{"fps":30}}]}""",
            ),
            ValidCase(
                "smallest video bitrate",
                """{"streams":[{"name":"a","url":"srt://host:5000","video":{"bitrate":50000}}]}""",
            ),
            ValidCase(
                "biggest video bitrate",
                """{"streams":[{"name":"a","url":"srt://host:5000","video":{"bitrate":50000000}}]}""",
            ),
            ValidCase(
                "smallest max key frame interval",
                """{"streams":[{"name":"a","url":"srt://host:5000","video":{"maxKeyFrameInterval":0}}]}""",
            ),
            ValidCase(
                "biggest max key frame interval",
                """{"streams":[{"name":"a","url":"srt://host:5000","video":{"maxKeyFrameInterval":10}}]}""",
            ),
            ValidCase(
                "smallest audio bitrate",
                """{"streams":[{"name":"a","url":"srt://host:5000","audio":{"bitrate":32000}}]}""",
            ),
            ValidCase(
                "biggest audio bitrate",
                """{"streams":[{"name":"a","url":"srt://host:5000","audio":{"bitrate":320000}}]}""",
            ),
            ValidCase(
                "empty remote control streamer url",
                """{"remoteControl":{"password":"p","streamer":{"enabled":false,"url":""}}}""",
            ),
            ValidCase(
                "remote control assistant port 1",
                """{"remoteControl":{"password":"p","assistant":{"enabled":true,"port":1}}}""",
            ),
            ValidCase(
                "quick button page 1",
                """{"quickButtons":{"buttons":[{"type":"Torch","page":1}]}}""",
            ),
            ValidCase(
                "quick button page 5",
                """{"quickButtons":{"buttons":[{"type":"Torch","page":5}]}}""",
            ),
        )
        for (validCase in cases) {
            MoblinSettingsUrl.fromString(query = validCase.query)
        }
    }

    class CleanUrlCase(
        val url: String,
        val cleanedUrl: String,
    )

    @Test
    fun streamUrlIsCleaned() {
        val cases = listOf(
            CleanUrlCase("srt://host:5000", "srt://host:5000"),
            CleanUrlCase("SRTLA://host:5000", "srtla://host:5000"),
            CleanUrlCase("srt://host :5000", "srt://host:5000"),
            CleanUrlCase(" srt://host:5000\n", "srt://host:5000"),
        )
        for (cleanUrlCase in cases) {
            val query = """{"streams":[{"name":"a","url":"${cleanUrlCase.url}"}]}"""
            assertEquals(
                cleanUrlCase.cleanedUrl,
                MoblinSettingsUrl.fromString(query = query).streams?.firstOrNull()?.url,
            )
        }
    }

    @Test
    fun obsWebSocketUrlIsCleaned() {
        val cases = listOf(
            CleanUrlCase("ws://host:4455", "ws://host:4455"),
            CleanUrlCase("WSS://host:4455", "wss://host:4455"),
            CleanUrlCase("ws://host :4455", "ws://host:4455"),
            CleanUrlCase(" ws://host:4455\n", "ws://host:4455"),
        )
        for (cleanUrlCase in cases) {
            val query = """{"streams":[{"name":"a","url":"srt://host:5000",
"obs":{"webSocketUrl":"${cleanUrlCase.url}","webSocketPassword":""}}]}"""
            assertEquals(
                cleanUrlCase.cleanedUrl,
                MoblinSettingsUrl.fromString(query = query).streams?.firstOrNull()?.obs?.webSocketUrl,
            )
        }
    }

    @Test
    fun remoteControlPasswordIsTrimmed() {
        val query = """{"remoteControl":{"password":" p "}}"""
        assertEquals(
            "p",
            MoblinSettingsUrl.fromString(query = query).remoteControl?.password,
        )
    }

    @Test
    fun remoteControlStreamerUrlIsTrimmed() {
        val query = """{"remoteControl":{"password":"p","streamer":{"enabled":true,"url":" ws://host:2345\n"}}}"""
        assertEquals(
            "ws://host:2345",
            MoblinSettingsUrl.fromString(query = query).remoteControl?.streamer?.url,
        )
    }

    @Test
    fun remoteControlRelayBaseUrlIsTrimmed() {
        val query = """{"remoteControl":{"password":"p","assistant":{"enabled":true,"port":2345,
"relay":{"enabled":true,"baseUrl":" wss://relay\n","bridgeId":"b"}}}}"""
        assertEquals(
            "wss://relay",
            MoblinSettingsUrl.fromString(query = query).remoteControl?.assistant?.relay?.baseUrl,
        )
    }

    @Test
    fun remoteControlRelayBridgeIdIsTrimmed() {
        val query = """{"remoteControl":{"password":"p","assistant":{"enabled":true,"port":2345,
"relay":{"enabled":true,"baseUrl":"wss://relay","bridgeId":" b "}}}}"""
        assertEquals(
            "b",
            MoblinSettingsUrl.fromString(query = query).remoteControl?.assistant?.relay?.bridgeId,
        )
    }
}
