package com.moblin.android.view.settings.streams.stream.url

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.moblin.android.various.model.Model
import com.moblin.android.various.network.DefaultTcpPorts
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamMultiStreamingDestination
import com.moblin.android.view.settings.ingests.rtspclient.UrlSettingsView
import com.moblin.android.LocalModel

private val rtmpExamples: List<Pair<String, String>> = listOf(
    "Twitch" to "rtmp://arn03.contribute.live-video.net/app/live_123321_sdfopjfwjfpawjefpjawef",
    "YouTube" to "rtmp://a.rtmp.youtube.com/live2/1bk2-0d03-9683-7k65-e4d3",
    "Facebook" to "rtmps://live-api-s.facebook.com:443/rtmp/FB-11152522122511115-0-BctNCp9jzzz-AAA",
    "Kick" to "rtmps://fa723fc1b171.global-contribute.live-video.net/sk_us-west-123hu43ui34hrkjh",
    "RTMP server" to "rtmp://foobar.org:3321/app/5678",
)

private val srtExamples: List<Pair<String, String>> = listOf(
    "OBS Media Source (SRT)" to "srt://134.20.342.12:5000",
    "BELABOX cloud SRTLA" to "srtla://uk.srt.belabox.net:5000?streamid=NtlPUqXGFV4Bcm448wgc4fUuLdvDB3",
    "BELABOX cloud SRT" to "srt://uk.srt.belabox.net:4000?streamid=NtlPUqXGFV4Bcm448wgc4fUuLdvDB3",
    "SRTLA server" to "srtla://foobar.org:4432",
    "SRT Live Server (SLS)" to "srt://120.12.32.12:4000?streamid=publish/live/feed",
)

val whipExamples: List<Pair<String, String>> = listOf(
    "MediaMTX WHIP" to "whip://120.12.32.12:8889/mystream/whip",
    "MESHCAST.IO WHIP" to "whips://de1.meshcast.io/whip/mystream",
)

private val mobcamExamples: List<Pair<String, String>> = listOf(
    "Computer over USB cable" to "mobcam://localhost:${DefaultTcpPorts.mobcamStream}",
)

@Composable
fun StreamUrlSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
) {
    val isLive = model.isLive.collectAsState().value
    val isRecording = model.isRecording.collectAsState().value
    val url = stream.url
    UrlSettingsView(
        disabled = isLive || isRecording,
        url = url,
        onChangeUrl = { stream.url = it },
        value = url,
        placeholder = "srtla://foobar.org:4432",
        allowedSchemes = null,
        examples = rtmpExamples + srtExamples + whipExamples + mobcamExamples,
        onSubmitted = {
            Unit
        },
        onDismiss = {
        },
    )
}

@Composable
fun StreamMultiStreamingUrlView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    destination: SettingsStreamMultiStreamingDestination,
) {
    val isLive = model.isLive.collectAsState().value
    val isRecording = model.isRecording.collectAsState().value
    val url = destination.url
    UrlSettingsView(
        disabled = isLive || isRecording,
        url = url,
        onChangeUrl = { destination.url = it },
        value = url,
        placeholder = "rtmp://foobar.org:3321/app/5678",
        allowedSchemes = listOf("rtmp", "rtmps"),
        examples = rtmpExamples,
        onSubmitted = {
            Unit
        },
        onDismiss = {
        },
    )
}
