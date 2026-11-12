package com.moblin.android.remotecontrol

import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidHttpUrl
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamCodec
import com.moblin.android.various.settings.SettingsStreamMultiStreaming
import com.moblin.android.various.settings.SettingsStreamPreviewStream
import com.moblin.android.various.settings.SettingsStreamProtocol
import com.moblin.android.various.settings.SettingsStreamRecording
import com.moblin.android.various.settings.SettingsStreamReplay
import com.moblin.android.various.settings.SettingsStreamResolution
import com.moblin.android.various.settings.SettingsStreamSrt
import com.moblin.android.various.settings.SettingsStreamSrtAdaptiveBitrate
import com.moblin.android.various.settings.SettingsStreamSrtConnectionPriorities
import com.moblin.android.various.settings.SettingsStreamWhip
import com.moblin.android.various.settings.SettingsTwitchAlerts
import com.moblin.android.various.settings.defaultRtmpStreamUrl
import com.moblin.android.various.settings.fpss
import com.moblin.android.view.settings.streams.stream.srt.maximumSrtConnectionPriority
import com.moblin.android.view.settings.streams.stream.srt.minimumSrtConnectionPriority

data class RemoteControlStreamState(
    var isLive: Boolean = false,
    var isRecording: Boolean = false,
)

private data class StreamLocks(
    val live: Boolean,
    val recording: Boolean,
    val liveOrRecording: Boolean,
)

private fun <T : Any> isValidChange(
    old: T,
    new: T,
    disabled: Boolean = false,
    isValid: (T) -> Boolean = { true },
): Boolean {
    return old == new || (!disabled && isValid(new))
}

fun isValidRemoteControlStreamUpdate(
    stream: SettingsStream,
    new: SettingsStream,
    streams: List<SettingsStream>,
    state: RemoteControlStreamState,
    creating: Boolean,
): Boolean {
    val locks = StreamLocks(
        live = stream.enabled && state.isLive,
        recording = stream.enabled && state.isRecording,
        liveOrRecording = stream.enabled && (state.isLive || state.isRecording),
    )
    return isValidStreamName(stream, new, streams, creating) &&
        isValidChange(
            stream.url,
            cleanUrl(new.url),
            disabled = !creating && (state.isLive || state.isRecording),
        ) {
            isValidUrl(it) == null
        } &&
        isValidChange(stream.portrait, new.portrait, disabled = locks.liveOrRecording) &&
        isValidChange(stream.estimatedViewerDelay, new.estimatedViewerDelay) { it.toInt() in 0..15 } &&
        isValidChange(stream.realtimeIrlBaseUrl, new.realtimeIrlBaseUrl) { isValidHttpUrl(it) == null } &&
        isValidChange(stream.obsWebSocketUrl, cleanUrl(new.obsWebSocketUrl)) {
            isValidWebSocketUrl(it) == null
        } &&
        isValidChange(stream.openStreamingPlatformUrl, new.openStreamingPlatformUrl) {
            isValidWebSocketUrl(it) == null
        } &&
        isValidChange(
            stream.obsBrbSceneVideoSourceBroken,
            new.obsBrbSceneVideoSourceBroken,
            disabled = new.obsBrbScene.isEmpty(),
        ) &&
        isValidTwitchAlertsUpdate(stream.twitchChatAlerts, new.twitchChatAlerts) &&
        isValidTwitchAlertsUpdate(stream.twitchToastAlerts, new.twitchToastAlerts) &&
        isValidVideoUpdate(stream, new, locks) &&
        isValidChange(stream.audioCodec, new.audioCodec, disabled = locks.live) &&
        isValidChange(stream.audioBitrate, new.audioBitrate, disabled = locks.live) {
            SettingsStream.isValidAudioBitrate(bitrate = it)
        } &&
        isValidSrtUpdate(stream.srt, new.srt, locks) &&
        isValidChange(
            stream.rtmp.adaptiveBitrateEnabled,
            new.rtmp.adaptiveBitrateEnabled,
            disabled = locks.live,
        ) &&
        isValidChange(
            stream.rist.adaptiveBitrateEnabled,
            new.rist.adaptiveBitrateEnabled,
            disabled = locks.live,
        ) &&
        isValidChange(stream.rist.bonding, new.rist.bonding, disabled = locks.live) &&
        isValidWhipUpdate(stream.whip, new.whip, locks) &&
        isValidMultiStreamingUpdate(stream.multiStreaming, new.multiStreaming, locks) &&
        isValidRecordingUpdate(stream.recording, new.recording, locks) &&
        isValidPreviewStreamUpdate(stream.previewStream, new.previewStream) &&
        isValidReplayUpdate(stream.replay, new.replay)
}

private fun isValidStreamName(
    stream: SettingsStream,
    new: SettingsStream,
    streams: List<SettingsStream>,
    creating: Boolean,
): Boolean {
    if (!creating && stream.name == new.name) {
        return true
    }
    return new.name.isNotEmpty() && !streams.any { it.id != new.id && it.name == new.name }
}

private fun isValidTwitchAlertsUpdate(
    alerts: SettingsTwitchAlerts,
    newAlerts: SettingsTwitchAlerts,
): Boolean {
    return isValidChange(alerts.minimumWatchStreak, newAlerts.minimumWatchStreak) {
        it in listOf(1, 3, 5, 10, 15, 20, 25)
    }
}

private fun isValidVideoUpdate(
    stream: SettingsStream,
    new: SettingsStream,
    locks: StreamLocks,
): Boolean {
    return isValidChange(stream.resolution, new.resolution, disabled = locks.liveOrRecording) &&
        isValidChange(stream.fps, new.fps, disabled = locks.liveOrRecording) { fpss.contains(it) } &&
        isValidChange(stream.codec, new.codec, disabled = locks.live) &&
        isValidChange(stream.h264Profile, new.h264Profile, disabled = locks.live) &&
        isValidChange(stream.rateControl, new.rateControl, disabled = locks.live) &&
        isValidChange(stream.bitrate, new.bitrate) { SettingsStream.isValidBitrate(bitrate = it) } &&
        isValidChange(stream.maxKeyFrameInterval, new.maxKeyFrameInterval, disabled = locks.live) {
            SettingsStream.isValidMaxKeyFrameInterval(interval = it)
        } &&
        isValidChange(stream.bFrames, new.bFrames, disabled = locks.live) &&
        isValidChange(stream.colorRange, new.colorRange, disabled = locks.liveOrRecording) &&
        isValidChange(
            stream.adaptiveEncoderResolution,
            new.adaptiveEncoderResolution,
            disabled = locks.live,
        ) &&
        isValidChange(
            stream.adaptiveEncoderResolutionThreashold,
            new.adaptiveEncoderResolutionThreashold,
            disabled = locks.live,
        ) { it.toInt() in 1..3 } &&
        isValidChange(
            stream.timecodesEnabled,
            new.timecodesEnabled,
            disabled = new.getProtocol() !in listOf(SettingsStreamProtocol.srt, SettingsStreamProtocol.rist) ||
                new.codec != SettingsStreamCodec.h265hevc ||
                locks.live,
        ) &&
        isValidChange(
            stream.ntpPoolAddress,
            new.ntpPoolAddress,
            disabled = new.codec != SettingsStreamCodec.h265hevc || locks.live,
        )
}

private fun isValidSrtUpdate(
    srt: SettingsStreamSrt,
    newSrt: SettingsStreamSrt,
    locks: StreamLocks,
): Boolean {
    return isValidChange(srt.latency, newSrt.latency, disabled = locks.live) {
        SettingsStreamSrt.isValidLatency(latency = it)
    } &&
        isValidChange(srt.adaptiveBitrateEnabled, newSrt.adaptiveBitrateEnabled, disabled = locks.live) &&
        isValidChange(
            srt.maximumBandwidthFollowInput,
            newSrt.maximumBandwidthFollowInput,
            disabled = locks.live,
        ) &&
        isValidChange(srt.overheadBandwidth, newSrt.overheadBandwidth, disabled = locks.live) {
            it in 5..100
        } &&
        isValidChange(srt.bigPackets, newSrt.bigPackets, disabled = locks.live) &&
        isValidChange(srt.dnsLookupStrategy, newSrt.dnsLookupStrategy, disabled = locks.live) &&
        isValidChange(srt.implementation, newSrt.implementation, disabled = locks.live) &&
        isValidSrtAdaptiveBitrateUpdate(srt.adaptiveBitrate, newSrt.adaptiveBitrate) &&
        isValidConnectionPrioritiesUpdate(srt.connectionPriorities, newSrt.connectionPriorities)
}

private fun isValidSrtAdaptiveBitrateUpdate(
    adaptiveBitrate: SettingsStreamSrtAdaptiveBitrate,
    newAdaptiveBitrate: SettingsStreamSrtAdaptiveBitrate,
): Boolean {
    val fastIrl = adaptiveBitrate.fastIrlSettings
    val newFastIrl = newAdaptiveBitrate.fastIrlSettings
    val custom = adaptiveBitrate.customSettings
    val newCustom = newAdaptiveBitrate.customSettings
    val isValidMinimumBitrate: (Float) -> Boolean = { it in 50f..2000f }
    return isValidChange(fastIrl.packetsInFlight, newFastIrl.packetsInFlight) { it in 200..700 } &&
        isValidChange(fastIrl.minimumBitrate, newFastIrl.minimumBitrate, isValid = isValidMinimumBitrate) &&
        isValidChange(custom.pifDiffIncreaseFactor, newCustom.pifDiffIncreaseFactor) {
            it.toInt() in 5..500
        } &&
        isValidChange(custom.rttDiffHighDecreaseFactor, newCustom.rttDiffHighDecreaseFactor) {
            it in 0.87f..0.98f
        } &&
        isValidChange(custom.rttDiffHighMinimumDecrease, newCustom.rttDiffHighMinimumDecrease) {
            it.toInt() in 5..400
        } &&
        isValidChange(custom.packetsInFlight, newCustom.packetsInFlight) { it in 50..5000 } &&
        isValidChange(custom.rttDiffHighAllowedSpike, newCustom.rttDiffHighAllowedSpike) {
            it.toInt() in 25..1000
        } &&
        isValidChange(custom.minimumBitrate, newCustom.minimumBitrate, isValid = isValidMinimumBitrate) &&
        isValidChange(
            adaptiveBitrate.belaboxSettings.minimumBitrate,
            newAdaptiveBitrate.belaboxSettings.minimumBitrate,
            isValid = isValidMinimumBitrate,
        )
}

private fun isValidConnectionPrioritiesUpdate(
    priorities: SettingsStreamSrtConnectionPriorities,
    newPriorities: SettingsStreamSrtConnectionPriorities,
): Boolean {
    if (newPriorities.priorities.map { it.id }.toSet().size != newPriorities.priorities.size) {
        return false
    }
    return newPriorities.priorities.all { newPriority ->
        val priority = priorities.priorities.firstOrNull { it.id == newPriority.id } ?: return@all false
        isValidChange(priority.priority, newPriority.priority) {
            it in minimumSrtConnectionPriority..maximumSrtConnectionPriority
        }
    } &&
        priorities.priorities.all { priority ->
            priority.name !in listOf("Cellular", "WiFi") ||
                newPriorities.priorities.any { it.id == priority.id }
        }
}

private fun isValidWhipUpdate(
    whip: SettingsStreamWhip,
    newWhip: SettingsStreamWhip,
    locks: StreamLocks,
): Boolean {
    val headers = whip.headers.map { listOf(it.name, it.value) }
    val newHeaders = newWhip.headers.map { listOf(it.name, it.value) }
    val isAuthorization: (List<String>) -> Boolean = { header -> header[0] == "Authorization" }
    return isValidChange(headers, newHeaders, disabled = locks.live) {
        newHeaders.filter { !isAuthorization(it) } == headers.filter { !isAuthorization(it) } &&
            newHeaders.count { isAuthorization(it) } <= maxOf(headers.count { isAuthorization(it) }, 1) &&
            newHeaders.filter { isAuthorization(it) }.all { header ->
                headers.contains(header) || header[1].startsWith("Bearer ")
            }
    } &&
        isValidChange(whip.httpTransport, newWhip.httpTransport, disabled = locks.live)
}

private fun isValidMultiStreamingUpdate(
    multiStreaming: SettingsStreamMultiStreaming,
    newMultiStreaming: SettingsStreamMultiStreaming,
    locks: StreamLocks,
): Boolean {
    val destinations = multiStreaming.destinations
    val newDestinations = newMultiStreaming.destinations
    if (locks.liveOrRecording && newDestinations.size != destinations.size) {
        return false
    }
    return newDestinations.withIndex().all { (index, newDestination) ->
        val destination = if (index < destinations.size) destinations[index] else null
        isValidChange(destination?.name ?: "", newDestination.name) { name ->
            name.isNotEmpty() && newDestinations.count { it.name == name } == 1
        } &&
            isValidChange(
                destination?.url ?: defaultRtmpStreamUrl,
                cleanUrl(newDestination.url),
                disabled = locks.liveOrRecording,
            ) {
                isValidUrl(it, allowedSchemes = listOf("rtmp", "rtmps")) == null
            } &&
            isValidChange(
                destination?.enabled ?: false,
                newDestination.enabled,
                disabled = locks.liveOrRecording,
            )
    }
}

private fun isValidRecordingUpdate(
    recording: SettingsStreamRecording,
    newRecording: SettingsStreamRecording,
    locks: StreamLocks,
): Boolean {
    return isValidChange(
        recording.overrideStream,
        newRecording.overrideStream,
        disabled = locks.liveOrRecording,
    ) &&
        isValidChange(recording.resolution, newRecording.resolution, disabled = locks.liveOrRecording) &&
        isValidChange(recording.videoCodec, newRecording.videoCodec, disabled = locks.recording) &&
        isValidChange(recording.videoBitrate, newRecording.videoBitrate, disabled = locks.recording) {
            it <= 50_000_000
        } &&
        isValidChange(
            recording.maxKeyFrameInterval,
            newRecording.maxKeyFrameInterval,
            disabled = locks.recording,
        ) {
            SettingsStream.isValidMaxKeyFrameInterval(interval = it)
        } &&
        isValidChange(recording.audioBitrate, newRecording.audioBitrate, disabled = locks.recording) {
            it <= 320_000 && it % 32000 == 0
        }
}

private fun isValidPreviewStreamUpdate(
    previewStream: SettingsStreamPreviewStream,
    newPreviewStream: SettingsStreamPreviewStream,
): Boolean {
    return isValidChange(previewStream.url, cleanUrl(newPreviewStream.url)) {
        it.isEmpty() || isValidUrl(it, allowedSchemes = listOf("whip", "whips")) == null
    } &&
        isValidChange(previewStream.resolution, newPreviewStream.resolution) {
            it in listOf(
                SettingsStreamResolution.r854x480,
                SettingsStreamResolution.r640x360,
                SettingsStreamResolution.r426x240,
            )
        } &&
        isValidChange(previewStream.bitrate, newPreviewStream.bitrate) {
            it in listOf(2_000_000, 1_500_000, 1_000_000, 500_000, 250_000)
        }
}

private fun isValidReplayUpdate(replay: SettingsStreamReplay, newReplay: SettingsStreamReplay): Boolean {
    return isValidChange(replay.postTriggerDelay, newReplay.postTriggerDelay) { it in 2..5 } &&
        isValidChange(replay.layout.x, newReplay.layout.x) { it.toDouble() in 0.0..100.0 } &&
        isValidChange(replay.layout.y, newReplay.layout.y) { it.toDouble() in 0.0..100.0 } &&
        isValidChange(replay.layout.size, newReplay.layout.size) { it.toDouble() in 1.0..100.0 } &&
        isValidChange(replay.inStinger.transitionPoint, newReplay.inStinger.transitionPoint) {
            it.toDouble() in 0.0..1.0
        } &&
        isValidChange(replay.outStinger.transitionPoint, newReplay.outStinger.transitionPoint) {
            it.toDouble() in 0.0..1.0
        }
}
