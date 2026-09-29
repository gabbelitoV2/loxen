package com.moblin.android.various.settings

import androidx.compose.runtime.snapshots.Snapshot
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.color
import com.moblin.android.platform.codable.codableJson
import com.moblin.android.platform.codable.decode
import com.moblin.android.streamingplatforms.youtube.YouTubeApiLiveBroadcaseVisibility
import com.moblin.android.various.network.DefaultTcpPorts
import com.moblin.android.various.network.DefaultUdpPorts
import java.time.Instant
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val sampleId = "E621E1F8-C36C-495A-93FC-0C247A3E6E5F"
private const val sampleRelayId = "0B8C4B35-6D8E-4E0F-9D83-2F1A7C3B5E11"
private val uuidPattern = Regex("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")

private fun <T> encodeJson(serializer: KSerializer<T>, value: T): String {
    return codableJson.encodeToString(serializer, value)
}

private fun <T> decodeJson(serializer: KSerializer<T>, text: String): T {
    return codableJson.decodeFromString(serializer, text)
}

private fun withoutIds(text: String): String {
    return text.replace(uuidPattern, "UUID")
}

private fun <T> assertRoundTrip(serializer: KSerializer<T>, value: T) {
    val first = encodeJson(serializer, value)
    val second = encodeJson(serializer, decodeJson(serializer, first))
    assertEquals(first, second)
}

private fun <T> assertEmptyDecodesTo(serializer: KSerializer<T>, expected: T, adjust: (T) -> Unit = {}) {
    val decoded = decodeJson(serializer, "{}")
    adjust(decoded)
    assertEquals(withoutIds(encodeJson(serializer, expected)), withoutIds(encodeJson(serializer, decoded)))
}

private fun <T> assertEmptyFails(serializer: KSerializer<T>) {
    assertFailsWith<SerializationException> {
        decodeJson(serializer, "{}")
    }
}

private fun <T> fieldOf(serializer: KSerializer<T>, value: T, key: String): String {
    return codableJson.encodeToJsonElement(serializer, value).jsonObject[key].toString()
}

private fun migratedSrt(): SettingsStreamSrt {
    val srt = SettingsStreamSrt()
    srt.bigPacketsMigrated = true
    srt.implemenationMigrated = true
    return srt
}

private fun migratedPermissions(): SettingsChatBotPermissions {
    val permissions = SettingsChatBotPermissions()
    permissions.migrated = true
    return permissions
}

@RunWith(RobolectricTestRunner::class)
class StreamCodableSuite {
    @Test
    fun streamSettingsDefaultsRoundTrip() {
        assertRoundTrip(SettingsStreamSrtConnectionPriority.serializer(), SettingsStreamSrtConnectionPriority())
        assertRoundTrip(SettingsStreamSrtConnectionPriorities.serializer(), SettingsStreamSrtConnectionPriorities())
        assertRoundTrip(
            SettingsStreamSrtAdaptiveBitrateFastIrlSettings.serializer(),
            SettingsStreamSrtAdaptiveBitrateFastIrlSettings(),
        )
        assertRoundTrip(
            SettingsStreamSrtAdaptiveBitrateCustomSettings.serializer(),
            SettingsStreamSrtAdaptiveBitrateCustomSettings(),
        )
        assertRoundTrip(
            SettingsStreamSrtAdaptiveBitrateBelaboxSettings.serializer(),
            SettingsStreamSrtAdaptiveBitrateBelaboxSettings(),
        )
        assertRoundTrip(SettingsStreamSrtAdaptiveBitrate.serializer(), SettingsStreamSrtAdaptiveBitrate())
        assertRoundTrip(SettingsStreamSrt.serializer(), migratedSrt())
        assertRoundTrip(SettingsStreamRtmp.serializer(), SettingsStreamRtmp())
        assertRoundTrip(SettingsStreamRist.serializer(), SettingsStreamRist())
        assertRoundTrip(SettingsHttpHeader.serializer(), SettingsHttpHeader())
        assertRoundTrip(SettingsStreamWhip.serializer(), SettingsStreamWhip())
        assertRoundTrip(SettingsStreamChat.serializer(), SettingsStreamChat())
        assertRoundTrip(SettingsStreamRecording.serializer(), SettingsStreamRecording())
        assertRoundTrip(SettingsStreamPreviewStream.serializer(), SettingsStreamPreviewStream())
        assertRoundTrip(SettingsStreamReplayStinger.serializer(), SettingsStreamReplayStinger())
        assertRoundTrip(SettingsStreamReplay.serializer(), SettingsStreamReplay())
        assertRoundTrip(SettingsStreamTwitchReward.serializer(), SettingsStreamTwitchReward())
        assertRoundTrip(SettingsStreamTwitchRaidChannel.serializer(), SettingsStreamTwitchRaidChannel())
        assertRoundTrip(
            SettingsStreamMultiStreamingDestination.serializer(),
            SettingsStreamMultiStreamingDestination(),
        )
        assertRoundTrip(SettingsStreamMultiStreaming.serializer(), SettingsStreamMultiStreaming())
        assertRoundTrip(SettingsTwitchAlerts.serializer(), SettingsTwitchAlerts())
        assertRoundTrip(SettingsKickAlerts.serializer(), SettingsKickAlerts())
        val stream = SettingsStream(name = "Main")
        stream.srt = migratedSrt()
        assertRoundTrip(SettingsStream.serializer(), stream)
    }

    @Test
    fun chatSettingsDefaultsRoundTrip() {
        assertRoundTrip(SettingsChatFilter.serializer(), SettingsChatFilter())
        assertRoundTrip(SettingsChatBotPermissionsCommand.serializer(), SettingsChatBotPermissionsCommand())
        assertRoundTrip(SettingsChatBotPermissions.serializer(), migratedPermissions())
        assertRoundTrip(SettingsChatBotAlias.serializer(), SettingsChatBotAlias())
        assertRoundTrip(SettingsChatBotCustomCommand.serializer(), SettingsChatBotCustomCommand())
        assertRoundTrip(SettingsChatPredefinedMessage.serializer(), SettingsChatPredefinedMessage())
        assertRoundTrip(SettingsChatPredefinedMessagesFilter.serializer(), SettingsChatPredefinedMessagesFilter())
        assertRoundTrip(SettingsChatNickname.serializer(), SettingsChatNickname())
        assertRoundTrip(SettingsChatNicknames.serializer(), SettingsChatNicknames())
        assertRoundTrip(SettingsOpenAi.serializer(), SettingsOpenAi())
        assertRoundTrip(SettingsVoiceApple.serializer(), SettingsVoiceApple())
        assertRoundTrip(SettingsVoiceTtsMonster.serializer(), SettingsVoiceTtsMonster())
        assertRoundTrip(SettingsVoice.serializer(), SettingsVoice())
        val chat = SettingsChat()
        chat.botCommandPermissions = migratedPermissions()
        assertRoundTrip(SettingsChat.serializer(), chat)
    }

    @Test
    fun ingestSettingsDefaultsRoundTrip() {
        assertRoundTrip(SettingsRtmpServerStream.serializer(), SettingsRtmpServerStream())
        assertRoundTrip(SettingsRtmpServer.serializer(), SettingsRtmpServer())
        assertRoundTrip(SettingsSrtlaServerStream.serializer(), SettingsSrtlaServerStream())
        assertRoundTrip(SettingsSrtlaServer.serializer(), SettingsSrtlaServer())
        assertRoundTrip(SettingsSrtClientStream.serializer(), SettingsSrtClientStream())
        assertRoundTrip(SettingsSrtClient.serializer(), SettingsSrtClient())
        assertRoundTrip(SettingsRistServerStream.serializer(), SettingsRistServerStream())
        assertRoundTrip(SettingsRistServer.serializer(), SettingsRistServer())
        assertRoundTrip(SettingsRtspClientStream.serializer(), SettingsRtspClientStream())
        assertRoundTrip(SettingsRtspClient.serializer(), SettingsRtspClient())
        assertRoundTrip(SettingsWhipServerStream.serializer(), SettingsWhipServerStream())
        assertRoundTrip(SettingsWhipServer.serializer(), SettingsWhipServer())
        assertRoundTrip(SettingsWhepClientStream.serializer(), SettingsWhepClientStream())
        assertRoundTrip(SettingsWhepClient.serializer(), SettingsWhepClient())
    }

    @Test
    fun publishedPropertiesAreObservable() {
        val stream = SettingsStream(name = "Main")
        val srt = stream.srt
        val adaptiveBitrate = srt.adaptiveBitrate
        val whip = stream.whip
        val recording = stream.recording
        val previewStream = stream.previewStream
        val replay = stream.replay
        val twitchAlerts = stream.twitchChatAlerts
        val kickAlerts = stream.kickToastAlerts
        val multiStreaming = stream.multiStreaming
        val destination = SettingsStreamMultiStreamingDestination(name = "Backup")
        val chat = SettingsChat()
        val filter = SettingsChatFilter()
        val nicknames = chat.nicknames
        val predefinedMessagesFilter = chat.predefinedMessagesFilter
        val command = SettingsChatBotPermissionsCommand(moderatorsEnabled = false)
        val openAi = SettingsOpenAi(personality = "Short")
        val rtmpServer = SettingsRtmpServer()
        val srtlaServer = SettingsSrtlaServer()
        val ristStream = SettingsRistServerStream()
        val rtspClient = SettingsRtspClient()
        val rtspStream = SettingsRtspClientStream()
        val whipStream = SettingsWhipServerStream()
        val whepClient = SettingsWhepClient()
        assertEquals("Main", stream.name)
        assertEquals("Backup", destination.name)
        assertFalse(command.moderatorsEnabled)
        assertEquals("Short", openAi.personality)
        multiStreaming.destinations.add(destination)
        rtspClient.streams.add(rtspStream)
        val cases: List<Pair<() -> Any?, () -> Unit>> = listOf(
            { stream.name } to { stream.name = "Other" },
            { stream.bitrate } to { stream.bitrate = 1_000_000 },
            { stream.kickChannelId } to { stream.kickChannelId = "1" },
            { stream.multiStreaming } to { stream.multiStreaming = SettingsStreamMultiStreaming() },
            { stream.twitchRaidsSent.size } to {
                stream.twitchRaidsSent.add(SettingsStreamTwitchRaidChannel("1", "a"))
            },
            { stream.twitchRaidsReceived.size } to {
                stream.twitchRaidsReceived = mutableListOf(SettingsStreamTwitchRaidChannel("2", "b"))
            },
            { adaptiveBitrate.algorithm } to {
                adaptiveBitrate.algorithm = SettingsStreamSrtAdaptiveBitrateAlgorithm.fastIrl
            },
            { srt.latency } to { srt.latency = 500 },
            { whip.headers.size } to { whip.headers.add(SettingsHttpHeader("Authorization", "Bearer x")) },
            { recording.autoStartRecording } to { recording.autoStartRecording = true },
            { previewStream.url } to { previewStream.url = "rtmp://x" },
            { replay.transitionType } to { replay.transitionType = SettingsStreamReplayTransitionType.none },
            { replay.layout } to { replay.layout = SettingsWidgetLayout(x = 5.0) },
            { twitchAlerts.follows } to { twitchAlerts.follows = false },
            { kickAlerts.minimumKicks } to { kickAlerts.minimumKicks = 10 },
            { multiStreaming.destinations.size } to { multiStreaming.destinations.removeAt(0) },
            { destination.name } to { destination.name = "Renamed" },
            { filter.enabled } to { filter.enabled = true },
            { chat.filters.size } to { chat.filters.add(filter) },
            { chat.textToSpeechLanguageVoices.size } to { chat.textToSpeechLanguageVoices["en"] = SettingsVoice() },
            { nicknames.nicknames.size } to { nicknames.nicknames.add(SettingsChatNickname()) },
            { predefinedMessagesFilter.redTag } to { predefinedMessagesFilter.redTag = true },
            { chat.fontSize } to { chat.fontSize = 25f },
            { chat.aliases.size } to { chat.aliases = mutableListOf(SettingsChatBotAlias()) },
            { command.moderatorsEnabled } to { command.moderatorsEnabled = true },
            { command.cooldown } to { command.cooldown = 30 },
            { openAi.personality } to { openAi.personality = "Long" },
            { rtmpServer.streams.size } to { rtmpServer.streams.add(SettingsRtmpServerStream()) },
            { rtmpServer.streams.first().name } to { rtmpServer.streams[0] = SettingsRtmpServerStream() },
            { srtlaServer.srtlaPort } to { srtlaServer.srtlaPort = 5123 },
            { ristStream.virtualDestinationPort } to { ristStream.virtualDestinationPort = 7 },
            { rtspClient.streams.size } to { rtspClient.streams.remove(rtspStream) },
            { rtspStream.transport } to { rtspStream.transport = SettingsRtspTransport.rtpUdp },
            { whipStream.latency } to { whipStream.latency = 200 },
            { whepClient.streams.isEmpty() } to { whepClient.streams.add(0, SettingsWhepClientStream()) },
        )
        cases.forEachIndexed { index, (read, write) ->
            assertObservedChange(index, read, write)
        }
        assertObservedUnchanged({ stream.bitrate }) { stream.fps = 60 }
        assertEquals(1, chat.filters.size)
        assertEquals(1, rtmpServer.streams.size)
        assertTrue(rtspClient.streams.isEmpty())
        assertEquals(1, whepClient.streams.size)
        assertEquals(SettingsStreamSrtAdaptiveBitrateAlgorithm.fastIrl, stream.srt.adaptiveBitrate.algorithm)
    }

    private fun observedReads(read: () -> Any?): List<Any> {
        val observed = mutableListOf<Any>()
        Snapshot.observe(readObserver = { observed.add(it) }) {
            read()
        }
        return observed
    }

    private fun changedStates(write: () -> Unit): List<Any> {
        Snapshot.sendApplyNotifications()
        val changed = mutableListOf<Any>()
        val handle = Snapshot.registerApplyObserver { modified, _ -> changed.addAll(modified) }
        try {
            write()
            Snapshot.sendApplyNotifications()
        } finally {
            handle.dispose()
        }
        return changed
    }

    private fun assertObservedChange(index: Int, read: () -> Any?, write: () -> Unit) {
        val observed = observedReads(read)
        assertTrue(observed.isNotEmpty(), "Read $index was not observed")
        val changed = changedStates(write)
        assertTrue(observed.any { state -> changed.any { it === state } }, "Write $index was not observed")
    }

    private fun assertObservedUnchanged(read: () -> Any?, write: () -> Unit) {
        val observed = observedReads(read)
        val changed = changedStates(write)
        assertFalse(observed.any { state -> changed.any { it === state } })
    }

    @Test
    fun streamSettingsDecodeEmptyObjectLikeSwift() {
        assertEmptyDecodesTo(SettingsStreamSrtConnectionPriority.serializer(), SettingsStreamSrtConnectionPriority())
        assertEmptyDecodesTo(
            SettingsStreamSrtAdaptiveBitrateFastIrlSettings.serializer(),
            SettingsStreamSrtAdaptiveBitrateFastIrlSettings(),
        )
        assertEmptyDecodesTo(
            SettingsStreamSrtAdaptiveBitrateCustomSettings.serializer(),
            SettingsStreamSrtAdaptiveBitrateCustomSettings(),
        )
        assertEmptyDecodesTo(
            SettingsStreamSrtAdaptiveBitrateBelaboxSettings.serializer(),
            SettingsStreamSrtAdaptiveBitrateBelaboxSettings(),
        )
        assertEmptyDecodesTo(SettingsStreamSrtAdaptiveBitrate.serializer(), SettingsStreamSrtAdaptiveBitrate())
        assertEmptyDecodesTo(SettingsStreamSrt.serializer(), migratedSrt())
        assertEmptyDecodesTo(SettingsStreamWhip.serializer(), SettingsStreamWhip())
        assertEmptyDecodesTo(SettingsStreamRecording.serializer(), SettingsStreamRecording())
        assertEmptyDecodesTo(SettingsStreamPreviewStream.serializer(), SettingsStreamPreviewStream())
        assertEmptyDecodesTo(SettingsStreamReplay.serializer(), SettingsStreamReplay())
        val channel = SettingsStreamTwitchRaidChannel()
        assertEmptyDecodesTo(SettingsStreamTwitchRaidChannel.serializer(), channel) {
            it.timestamp = channel.timestamp
        }
        assertEmptyDecodesTo(
            SettingsStreamMultiStreamingDestination.serializer(),
            SettingsStreamMultiStreamingDestination(),
        )
        assertEmptyDecodesTo(SettingsStreamMultiStreaming.serializer(), SettingsStreamMultiStreaming())
        assertEmptyDecodesTo(SettingsTwitchAlerts.serializer(), SettingsTwitchAlerts())
        assertEmptyDecodesTo(SettingsKickAlerts.serializer(), SettingsKickAlerts())
        assertEmptyDecodesTo(SettingsStream.serializer(), SettingsStream())
        assertEmptyFails(SettingsStreamSrtConnectionPriorities.serializer())
        assertEmptyFails(SettingsStreamRtmp.serializer())
        assertEmptyFails(SettingsStreamRist.serializer())
        assertEmptyFails(SettingsHttpHeader.serializer())
        assertEmptyFails(SettingsStreamChat.serializer())
        assertEmptyFails(SettingsStreamReplayStinger.serializer())
        assertEmptyFails(SettingsStreamTwitchReward.serializer())
    }

    @Test
    fun chatSettingsDecodeEmptyObjectLikeSwift() {
        val filter = SettingsChatFilter()
        filter.enabled = true
        assertEmptyDecodesTo(SettingsChatFilter.serializer(), filter)
        assertEmptyDecodesTo(SettingsChatBotPermissionsCommand.serializer(), SettingsChatBotPermissionsCommand())
        assertEmptyDecodesTo(SettingsChatBotPermissions.serializer(), migratedPermissions())
        val alias = SettingsChatBotAlias()
        alias.alias = ""
        alias.replacement = ""
        assertEmptyDecodesTo(SettingsChatBotAlias.serializer(), alias)
        val command = SettingsChatBotCustomCommand()
        command.name = ""
        assertEmptyDecodesTo(SettingsChatBotCustomCommand.serializer(), command)
        assertEmptyDecodesTo(SettingsChatPredefinedMessage.serializer(), SettingsChatPredefinedMessage())
        assertEmptyDecodesTo(
            SettingsChatPredefinedMessagesFilter.serializer(),
            SettingsChatPredefinedMessagesFilter(),
        )
        assertEmptyDecodesTo(SettingsChatNickname.serializer(), SettingsChatNickname())
        assertEmptyDecodesTo(SettingsChatNicknames.serializer(), SettingsChatNicknames())
        assertEmptyDecodesTo(SettingsOpenAi.serializer(), SettingsOpenAi())
        val chat = SettingsChat()
        chat.displayStyle = SettingsChatDisplayStyle.internationalName
        assertEmptyDecodesTo(SettingsChat.serializer(), chat)
        assertEmptyFails(SettingsVoiceApple.serializer())
        assertEmptyFails(SettingsVoiceTtsMonster.serializer())
        assertEmptyFails(SettingsVoice.serializer())
    }

    @Test
    fun ingestSettingsDecodeEmptyObjectLikeSwift() {
        assertEmptyDecodesTo(SettingsRtmpServerStream.serializer(), SettingsRtmpServerStream())
        assertEmptyDecodesTo(SettingsRtmpServer.serializer(), SettingsRtmpServer())
        assertEmptyDecodesTo(SettingsSrtlaServerStream.serializer(), SettingsSrtlaServerStream())
        assertEmptyDecodesTo(SettingsSrtlaServer.serializer(), SettingsSrtlaServer())
        assertEmptyDecodesTo(SettingsSrtClientStream.serializer(), SettingsSrtClientStream())
        assertEmptyDecodesTo(SettingsSrtClient.serializer(), SettingsSrtClient())
        assertEmptyDecodesTo(SettingsRistServerStream.serializer(), SettingsRistServerStream())
        assertEmptyDecodesTo(SettingsRistServer.serializer(), SettingsRistServer())
        assertEmptyDecodesTo(SettingsRtspClientStream.serializer(), SettingsRtspClientStream())
        assertEmptyDecodesTo(SettingsRtspClient.serializer(), SettingsRtspClient())
        assertEmptyDecodesTo(SettingsWhipServerStream.serializer(), SettingsWhipServerStream())
        assertEmptyDecodesTo(SettingsWhipServer.serializer(), SettingsWhipServer())
        assertEmptyDecodesTo(SettingsWhepClientStream.serializer(), SettingsWhepClientStream())
        assertEmptyDecodesTo(SettingsWhepClient.serializer(), SettingsWhepClient())
    }

    @Test
    fun enumsEncodeLikeSwift() {
        val recording = SettingsStreamRecording()
        assertEquals("\"H.265/HEVC\"", fieldOf(SettingsStreamRecording.serializer(), recording, "videoCodec"))
        assertEquals("\"1920x1080\"", fieldOf(SettingsStreamRecording.serializer(), recording, "resolution"))
        assertEquals("null", fieldOf(SettingsStreamRecording.serializer(), recording, "recordingPath"))
        val adaptiveBitrate = SettingsStreamSrtAdaptiveBitrate()
        adaptiveBitrate.algorithm = SettingsStreamSrtAdaptiveBitrateAlgorithm.customIrl
        assertEquals(
            "{\"customIrl\":{}}",
            fieldOf(SettingsStreamSrtAdaptiveBitrate.serializer(), adaptiveBitrate, "algorithm"),
        )
        assertEquals(
            "{\"headers\":[],\"httpTransport\":{\"standard\":{}}}",
            encodeJson(SettingsStreamWhip.serializer(), SettingsStreamWhip()),
        )
        val srt = SettingsStreamSrt()
        assertEquals("\"System\"", fieldOf(SettingsStreamSrt.serializer(), srt, "dnsLookupStrategy"))
        assertEquals("\"Moblin\"", fieldOf(SettingsStreamSrt.serializer(), srt, "implementation"))
        val stream = SettingsStream()
        assertEquals("\"ABR\"", fieldOf(SettingsStream.serializer(), stream, "bitrateRateControl"))
        assertEquals("\"Main\"", fieldOf(SettingsStream.serializer(), stream, "h264Profile"))
        assertEquals("\"AAC\"", fieldOf(SettingsStream.serializer(), stream, "audioCodec"))
        assertEquals("\"public\"", fieldOf(SettingsStream.serializer(), stream, "youTubeScheduleStreamVisibility"))
        assertEquals("null", fieldOf(SettingsStream.serializer(), stream, "twitchShowFollows"))
        assertEquals("null", fieldOf(SettingsStream.serializer(), stream, "kickChannelId"))
        assertEquals(
            "\"internationalNameAndUsername\"",
            fieldOf(SettingsChat.serializer(), SettingsChat(), "displayStyle"),
        )
        assertEquals("\"apple\"", fieldOf(SettingsVoice.serializer(), SettingsVoice(), "type"))
        val rtspStream = SettingsRtspClientStream()
        assertEquals("\"rtpRtspTcp\"", fieldOf(SettingsRtspClientStream.serializer(), rtspStream, "transport"))
        assertEquals("\"fade\"", fieldOf(SettingsStreamReplay.serializer(), SettingsStreamReplay(), "transitionType"))
        val priority = SettingsStreamSrtConnectionPriority("WiFi")
        priority.id = UUID.fromString(sampleId)
        assertEquals(
            "{\"id\":\"$sampleId\",\"name\":\"WiFi\",\"priority\":1,\"enabled\":true,\"relayId\":null}",
            encodeJson(SettingsStreamSrtConnectionPriority.serializer(), priority),
        )
        val destination = SettingsStreamMultiStreamingDestination()
        assertFalse(encodeJson(SettingsStreamMultiStreamingDestination.serializer(), destination).contains("\"id\""))
        val alias = SettingsChatBotAlias()
        assertEquals(
            "{\"alias\":\"!myalias\",\"replacement\":\"!moblin\"}",
            encodeJson(SettingsChatBotAlias.serializer(), alias),
        )
    }

    @Test
    fun enumsDecodeLikeSwift() {
        val codec = decodeJson(SettingsStreamRecording.serializer(), "{\"videoCodec\":\"VP9\"}")
        assertEquals(SettingsStreamCodec.h264avc, codec.videoCodec)
        val codecNumber = decodeJson(SettingsStreamRecording.serializer(), "{\"videoCodec\":5}")
        assertEquals(SettingsStreamCodec.h265hevc, codecNumber.videoCodec)
        assertEquals(SettingsStreamProtocol.rtmp, decodeJson(SettingsStreamProtocol.serializer(), "\"FTP\""))
        assertEquals(SettingsStreamProtocol.whip, decodeJson(SettingsStreamProtocol.serializer(), "\"WHIP\""))
        val replay = decodeJson(SettingsStreamReplay.serializer(), "{\"transitionType\":\"wipe\"}")
        assertEquals(SettingsStreamReplayTransitionType.fade, replay.transitionType)
        val voice = decodeJson(
            SettingsVoice.serializer(),
            "{\"type\":\"google\",\"apple\":{\"voice\":\"x\"},\"ttsMonster\":{\"name\":\"\",\"voiceId\":\"\"}}",
        )
        assertEquals(SettingsVoiceType.apple, voice.type)
        assertEquals("x", voice.apple.voice)
        val transport = SettingsStreamWhipHttpTransport.serializer()
        assertEquals(SettingsStreamWhipHttpTransport.remoteControl, decodeJson(transport, "{\"remoteControl\":{}}"))
        assertEquals(SettingsStreamWhipHttpTransport.standard, decodeJson(transport, "{\"standard\":{},\"x\":1}"))
        assertFailsWith<SerializationException> { decodeJson(transport, "\"standard\"") }
        assertFailsWith<SerializationException> { decodeJson(transport, "{}") }
        assertFailsWith<SerializationException> { decodeJson(transport, "{\"standard\":{},\"remoteControl\":{}}") }
        assertFailsWith<SerializationException> { decodeJson(transport, "{\"standard\":1}") }
        val algorithm = SettingsStreamSrtAdaptiveBitrateAlgorithm.serializer()
        assertEquals(SettingsStreamSrtAdaptiveBitrateAlgorithm.slowIrl, decodeJson(algorithm, "{\"slowIrl\":{}}"))
        assertEquals(SettingsStreamSrtAdaptiveBitrateAlgorithm.fastIrl, decodeJson(algorithm, "{\"fastIrl\":null}"))
        assertEquals(SettingsStreamSrtAdaptiveBitrateAlgorithm.belabox, decodeJson(algorithm, "{\"other\":{}}"))
        val adaptiveBitrate = decodeJson(SettingsStreamSrtAdaptiveBitrate.serializer(), "{\"algorithm\":\"fastIrl\"}")
        assertEquals(SettingsStreamSrtAdaptiveBitrateAlgorithm.belabox, adaptiveBitrate.algorithm)
        val stream = decodeJson(
            SettingsStream.serializer(),
            "{\"youTubeScheduleStreamVisibility\":\"unlisted\",\"resolution\":\"999x999\"}",
        )
        assertEquals(YouTubeApiLiveBroadcaseVisibility.unlisted, stream.youTubeScheduleStreamVisibility)
        assertEquals(SettingsStreamResolution.r1920x1080, stream.resolution)
    }

    @Test
    fun streamSample() {
        val json = """
            {
              "name": "IRL",
              "id": "$sampleId",
              "enabled": true,
              "url": "srtla://example.com:5000",
              "twitchChannelName": "streamer",
              "twitchShowFollows": false,
              "twitchLoggedIn": true,
              "twitchRaidsSent": [{"channelId": "1", "channelName": "one", "timestamp": 1000.5}],
              "twitchChatAlerts": {"cheers": false, "minimumCheerBits": 100},
              "kickChannelId": "k1",
              "kickAccessToken": "secret",
              "kickChatAlerts": {"minimumKicks": 7},
              "youTubeVideoId": "a,b",
              "youTubeScheduleStreamVisibility": "private",
              "afreecaTvChannelName": "soop",
              "afreecaTvStreamId": "soopId",
              "resolution": "1280x720",
              "fps": 60,
              "autoFps": true,
              "bitrate": 8000000,
              "bitrateRateControl": "CBR",
              "codec": "H.264/AVC",
              "h264Profile": "High",
              "adaptiveEncoderResolutionThreashold": 0.5,
              "srt": {
                "latency": 500,
                "mpegtsPacketsPerPacket": 6,
                "adaptiveBitrate": {"algorithm": {"fastIrl": {}}, "fastIrlSettings": {"packetsInFlight": 300}},
                "connectionPriorities": {
                  "enabled": true,
                  "priorities": [
                    {"id": "$sampleId", "name": "WiFi", "priority": 3, "enabled": false, "relayId": "$sampleRelayId"}
                  ]
                },
                "dnsLookupStrategy": "IPv6"
              },
              "rtmp": {"adaptiveBitrateEnabled": false},
              "rist": {"adaptiveBitrateEnabled": false},
              "whip": {"headers": [{"name": "A", "value": "B"}], "httpTransport": {"remoteControl": {}}},
              "audioCodec": "OPUS",
              "audioBitrate": 96000,
              "chat": {"bttvEmotes": true, "ffzEmotes": true, "seventvEmotes": false},
              "recording": {"overrideStream": true, "videoBitrate": 3000000, "recordingPath": "AQID"},
              "replay": {"enabled": true, "fade": false, "x": 10.0, "size": 50.0, "alignment": "BottomRight",
                         "inStinger": {"id": "$sampleId", "name": "in.mov", "transitionPoint": 0.25}},
              "multiStreaming": {"destinations": [{"name": "D", "url": "rtmp://x/y", "enabled": true}]},
              "previewStream": {"url": "srt://p", "resolution": "426x240", "bitrate": 250000},
              "estimatedViewerDelay": 3.5,
              "autoGoLive": true
            }
        """.trimIndent()
        val stream = decodeJson(SettingsStream.serializer(), json)
        assertEquals("IRL", stream.name)
        assertEquals(UUID.fromString(sampleId), stream.id)
        assertTrue(stream.enabled)
        assertEquals("srtla://example.com:5000", stream.url)
        assertNull(stream.twitchShowFollows)
        assertFalse(stream.twitchChatAlerts.follows)
        assertFalse(stream.twitchToastAlerts.follows)
        assertFalse(stream.twitchChatAlerts.cheers)
        assertEquals(100, stream.twitchChatAlerts.minimumCheerBits)
        assertTrue(stream.twitchWantsToBeLoggedIn)
        assertEquals(1, stream.twitchRaidsSent.size)
        assertEquals("one", stream.twitchRaidsSent[0].channelName)
        assertEquals(Instant.ofEpochSecond(978_307_200L + 1000L, 500_000_000L), stream.twitchRaidsSent[0].timestamp)
        assertEquals("k1", stream.kickChannelId)
        assertEquals("", stream.kickAccessToken)
        assertEquals(7, stream.kickChatAlerts.minimumKicks)
        assertEquals(listOf("a", "b"), stream.getYouTubeVideoIds())
        assertEquals(YouTubeApiLiveBroadcaseVisibility.private, stream.youTubeScheduleStreamVisibility)
        assertEquals("soop", stream.soopChannelName)
        assertEquals("soopId", stream.soopStreamId)
        assertEquals(SettingsStreamResolution.r1280x720, stream.resolution)
        assertEquals(60, stream.fps)
        assertTrue(stream.lowLightBoost)
        assertEquals(8_000_000, stream.bitrate)
        assertEquals(SettingsStreamRateControl.cbr, stream.rateControl)
        assertEquals(SettingsStreamCodec.h264avc, stream.codec)
        assertEquals(SettingsStreamH264Profile.high, stream.h264Profile)
        assertEquals(0.5, stream.adaptiveEncoderResolutionThreashold)
        assertEquals(500, stream.srt.latency)
        assertFalse(stream.srt.bigPackets)
        assertTrue(stream.srt.bigPacketsMigrated)
        assertEquals(SettingsStreamSrtImplementation.official, stream.srt.implementation)
        assertEquals(SettingsStreamSrtAdaptiveBitrateAlgorithm.fastIrl, stream.srt.adaptiveBitrate.algorithm)
        assertEquals(300, stream.srt.adaptiveBitrate.fastIrlSettings.packetsInFlight)
        assertEquals(250f, stream.srt.adaptiveBitrate.fastIrlSettings.minimumBitrate)
        assertTrue(stream.srt.connectionPriorities.enabled)
        assertEquals(1, stream.srt.connectionPriorities.priorities.size)
        assertEquals(3, stream.srt.connectionPriorities.priorities[0].priority)
        assertFalse(stream.srt.connectionPriorities.priorities[0].enabled)
        assertEquals(UUID.fromString(sampleRelayId), stream.srt.connectionPriorities.priorities[0].relayId)
        assertEquals(SettingsDnsLookupStrategy.ipv6, stream.srt.dnsLookupStrategy)
        assertFalse(stream.rtmp.adaptiveBitrateEnabled)
        assertTrue(stream.rist.adaptiveBitrateEnabled)
        assertTrue(stream.rist.bonding)
        assertEquals(listOf(SettingsHttpHeader("A", "B")), stream.whip.headers)
        assertEquals(SettingsStreamWhipHttpTransport.remoteControl, stream.whip.httpTransport)
        assertEquals(SettingsStreamAudioCodec.opus, stream.audioCodec)
        assertEquals(96000, stream.audioBitrate)
        assertTrue(stream.chat.bttvEmotes)
        assertTrue(stream.chat.ffzEmotes)
        assertTrue(stream.recording.overrideStream)
        assertEquals(3_000_000, stream.recording.videoBitrate)
        assertContentEquals(byteArrayOf(1, 2, 3), stream.recording.recordingPath)
        assertTrue(stream.replay.enabled)
        assertEquals(SettingsStreamReplayTransitionType.none, stream.replay.transitionType)
        assertEquals(10.0, stream.replay.layout.x)
        assertEquals("10.0", stream.replay.layout.xString)
        assertEquals(50.0, stream.replay.layout.size)
        assertEquals(SettingsAlignment.bottomRight, stream.replay.layout.alignment)
        assertEquals("in.mov", stream.replay.inStinger.name)
        assertEquals(0.25, stream.replay.inStinger.transitionPoint)
        assertEquals(1, stream.multiStreaming.destinations.size)
        assertEquals("rtmp://x/y", stream.multiStreaming.destinations[0].url)
        assertTrue(stream.multiStreaming.destinations[0].enabled)
        assertEquals("srt://p", stream.previewStream.url)
        assertEquals(SettingsStreamResolution.r426x240, stream.previewStream.resolution)
        assertEquals(250_000, stream.previewStream.bitrate)
        assertEquals(3.5f, stream.estimatedViewerDelay)
        assertTrue(stream.autoGoLive)
        val encoded = codableJson.encodeToJsonElement(SettingsStream.serializer(), stream).jsonObject
        assertEquals("\"a,b\"", encoded["youTubeVideoId"].toString())
        assertEquals("\"soop\"", encoded["afreecaTvChannelName"].toString())
        assertEquals("true", encoded["autoFps"].toString())
        assertEquals("\"CBR\"", encoded["bitrateRateControl"].toString())
        assertEquals("\"H.264/AVC\"", encoded["codec"].toString())
        assertEquals("\"\"", encoded["kickAccessToken"].toString())
        assertEquals("null", encoded["twitchShowFollows"].toString())
        assertEquals("\"AQID\"", encoded["recording"]!!.jsonObject["recordingPath"].toString())
        assertEquals("1000.5", encoded["twitchRaidsSent"].toString().substringAfter("\"timestamp\":").substringBefore("}"))
        assertEquals("{\"fastIrl\":{}}", encoded["srt"]!!.jsonObject["adaptiveBitrate"]!!.jsonObject["algorithm"].toString())
        assertEquals("10.0", encoded["replay"]!!.jsonObject["x"].toString())
        assertFalse(encoded["replay"]!!.jsonObject.containsKey("fade"))
        assertRoundTrip(SettingsStream.serializer(), stream)
    }

    @Test
    fun srtMigrationsLikeSwift() {
        val unmigrated = decodeJson(
            SettingsStreamSrt.serializer(),
            "{\"latency\":800,\"mpegtsPacketsPerPacket\":7,\"bigPackets\":false,\"implementation\":\"Moblin\"}",
        )
        assertTrue(unmigrated.bigPackets)
        assertTrue(unmigrated.bigPacketsMigrated)
        assertEquals(SettingsStreamSrtImplementation.official, unmigrated.implementation)
        assertTrue(unmigrated.implemenationMigrated)
        val migrated = decodeJson(
            SettingsStreamSrt.serializer(),
            "{\"latency\":800,\"mpegtsPacketsPerPacket\":7,\"bigPackets\":false,\"implementation\":\"Moblin\"," +
                "\"bigPacketsMigrated\":true,\"implemenationMigrated\":true}",
        )
        assertFalse(migrated.bigPackets)
        assertEquals(SettingsStreamSrtImplementation.moblin, migrated.implementation)
        val fresh = SettingsStreamSrt()
        assertFalse(fresh.bigPacketsMigrated)
        assertFalse(fresh.implemenationMigrated)
        val reloaded = decodeJson(SettingsStreamSrt.serializer(), encodeJson(SettingsStreamSrt.serializer(), fresh))
        assertEquals(
            withoutIds(encodeJson(SettingsStreamSrt.serializer(), migratedSrt())),
            withoutIds(encodeJson(SettingsStreamSrt.serializer(), reloaded)),
        )
    }

    @Test
    fun replaySample() {
        val fadeWins = decodeJson(
            SettingsStreamReplay.serializer(),
            "{\"fade\":true,\"transitionType\":\"none\",\"postTriggerDelay\":5,\"y\":20.0," +
                "\"positioningLock\":true,\"enterForegroundCountAtLatestUsage\":4}",
        )
        assertEquals(SettingsStreamReplayTransitionType.fade, fadeWins.transitionType)
        assertEquals(5, fadeWins.postTriggerDelay)
        assertEquals(20.0, fadeWins.layout.y)
        assertEquals("20.0", fadeWins.layout.yString)
        assertTrue(fadeWins.layout.positioningLock)
        assertEquals(4, fadeWins.enterForegroundCountAtLatestUsage)
        val invalidFade = decodeJson(
            SettingsStreamReplay.serializer(),
            "{\"fade\":\"yes\",\"transitionType\":\"stingers\",\"inStinger\":{\"name\":\"a\"}}",
        )
        assertEquals(SettingsStreamReplayTransitionType.stingers, invalidFade.transitionType)
        assertEquals("", invalidFade.inStinger.name)
        assertEquals(0.5, invalidFade.inStinger.transitionPoint)
    }

    @Test
    fun chatSample() {
        val json = """
            {
              "fontSize": 25.5,
              "usernameColor": {"red": 1, "green": 2, "blue": 3},
              "timestampColorEnabled": true,
              "height": 0.5,
              "maximumAge": 60,
              "usernamesToIgnore": [
                {"id": "$sampleId", "enabled": false, "value": "bot", "messageWords": ["!a", "b"], "poll": true}
              ],
              "textToSpeechDefaultLanguage": "sv",
              "textToSpeechRate": 0.7,
              "textToSpeechLanguageVoices": {"en": "com.apple.voice.en", "de": "com.apple.voice.de"},
              "ttsMonster": {"apiToken": "token"},
              "botEnabled": true,
              "botCommandPermissions": {
                "scene": {"moderatorsEnabled": true, "othersEnabled": true, "cooldown": 5},
                "macro": {"subscribersEnabled": true},
                "migrated": true
              },
              "botCommandAi": {"role": "Be nice", "model": "m"},
              "aliases": [{"alias": "!a", "replacement": "!b"}],
              "customCommands": [{"id": "$sampleId", "name": "cmd", "formatString": "{x}", "permissions": {"othersEnabled": true}}],
              "predefinedMessages": [{"id": "$sampleId", "text": "hi", "redTag": true}],
              "predefinedMessagesFilter": {"blueTag": true},
              "nicknames": {"nicknames": [{"id": "$sampleId", "user": "u", "nickname": "n"}]},
              "displayStyle": "username",
              "bigGifScale": 3.0
            }
        """.trimIndent()
        val chat = decodeJson(SettingsChat.serializer(), json)
        assertEquals(25.5f, chat.fontSize)
        assertEquals(RgbColor(red = 1, green = 2, blue = 3), chat.usernameColor)
        assertEquals(RgbColor(red = 1, green = 2, blue = 3).color(), chat.usernameColorColor)
        assertTrue(chat.timestampColorEnabled)
        assertEquals(0.5, chat.height)
        assertEquals(60, chat.maximumAge)
        assertEquals(1, chat.filters.size)
        val filter = chat.filters[0]
        assertEquals(UUID.fromString(sampleId), filter.id)
        assertFalse(filter.enabled)
        assertEquals("bot", filter.user)
        assertEquals(listOf("!a", "b"), filter.messageStartWords)
        assertEquals("!a b", filter.messageStart)
        assertTrue(filter.poll)
        assertEquals("sv", chat.textToSpeechDefaultLanguage)
        assertEquals(0.7f, chat.textToSpeechRate)
        assertEquals(setOf("en", "de"), chat.textToSpeechLanguageVoices.keys)
        assertEquals("com.apple.voice.en", chat.textToSpeechLanguageVoices["en"]!!.apple.voice)
        assertEquals(SettingsVoiceType.apple, chat.textToSpeechLanguageVoices["de"]!!.type)
        assertEquals("token", chat.ttsMonster.apiToken)
        assertTrue(chat.botEnabled)
        val permissions = chat.botCommandPermissions
        assertTrue(permissions.migrated)
        assertTrue(permissions.scene.moderatorsEnabled)
        assertTrue(permissions.scene.othersEnabled)
        assertEquals(5, permissions.scene.cooldown)
        assertTrue(permissions.stream.moderatorsEnabled)
        assertTrue(permissions.macro.moderatorsEnabled)
        assertTrue(permissions.macro.subscribersEnabled)
        assertTrue(permissions.tts.moderatorsEnabled)
        assertEquals("Be nice", chat.botCommandAi.personality)
        assertEquals("m", chat.botCommandAi.model)
        assertEquals("https://generativelanguage.googleapis.com/v1beta/openai", chat.botCommandAi.baseUrl)
        assertEquals("!a", chat.aliases[0].alias)
        assertEquals("!b", chat.aliases[0].replacement)
        assertEquals("cmd", chat.customCommands[0].name)
        assertEquals("{x}", chat.customCommands[0].formatString)
        assertTrue(chat.customCommands[0].permissions.othersEnabled)
        assertEquals("hi", chat.predefinedMessages[0].text)
        assertEquals(SettingsChatPredefinedMessage.tagRed, chat.predefinedMessages[0].tagsString())
        assertTrue(chat.predefinedMessagesFilter.isEnabled())
        assertEquals("n", chat.nicknames.getNickname("u"))
        assertEquals(SettingsChatDisplayStyle.username, chat.displayStyle)
        assertEquals(3.0f, chat.bigGifScale)
        val encoded = codableJson.encodeToJsonElement(SettingsChat.serializer(), chat).jsonObject
        assertEquals(
            "{\"type\":\"apple\",\"apple\":{\"voice\":\"com.apple.voice.en\"},\"ttsMonster\":{\"name\":\"\",\"voiceId\":\"\"}}",
            encoded["textToSpeechLanguageVoices"]!!.jsonObject["en"].toString(),
        )
        assertEquals("\"Be nice\"", encoded["botCommandAi"]!!.jsonObject["role"].toString())
        assertEquals("\"bot\"", encoded["usernamesToIgnore"]!!.toString().substringAfter("\"value\":").substringBefore(","))
        assertRoundTrip(SettingsChat.serializer(), chat)
    }

    @Test
    fun chatVoicesAndPermissionsMigrationLikeSwift() {
        val voices = decodeJson(
            SettingsChat.serializer(),
            "{\"textToSpeechLanguageVoices\":{\"sv\":{\"type\":\"ttsMonster\",\"apple\":{\"voice\":\"\"}," +
                "\"ttsMonster\":{\"name\":\"N\",\"voiceId\":\"V\"}}}}",
        )
        val voice = voices.textToSpeechLanguageVoices["sv"]!!
        assertEquals(SettingsVoiceType.ttsMonster, voice.type)
        assertEquals("N", voice.ttsMonster.name)
        assertEquals("V", voice.ttsMonster.voiceId)
        val mixed = decodeJson(
            SettingsChat.serializer(),
            "{\"textToSpeechLanguageVoices\":{\"en\":\"x\",\"sv\":{\"type\":\"apple\"," +
                "\"apple\":{\"voice\":\"\"},\"ttsMonster\":{\"name\":\"\",\"voiceId\":\"\"}}}}",
        )
        assertTrue(mixed.textToSpeechLanguageVoices.isEmpty())
        val permissions = decodeJson(
            SettingsChatBotPermissions.serializer(),
            "{\"scene\":{\"moderatorsEnabled\":true},\"stream\":{\"moderatorsEnabled\":true}}",
        )
        assertFalse(permissions.scene.moderatorsEnabled)
        assertFalse(permissions.stream.moderatorsEnabled)
        assertFalse(permissions.macro.moderatorsEnabled)
        assertTrue(permissions.migrated)
        val fresh = SettingsChatBotPermissions()
        assertFalse(fresh.migrated)
    }

    @Test
    fun ingestsSample() {
        val rtmp = decodeJson(
            SettingsRtmpServer.serializer(),
            "{\"enabled\":true,\"port\":1936,\"streams\":[{\"id\":\"$sampleId\",\"name\":\"Cam\"," +
                "\"streamKey\":\"key\",\"latency\":500}]}",
        )
        assertTrue(rtmp.enabled)
        assertEquals(1936, rtmp.port)
        assertEquals(UUID.fromString(sampleId), rtmp.streams[0].id)
        assertEquals("Cam", rtmp.streams[0].name)
        assertEquals("key", rtmp.streams[0].streamKey)
        assertEquals(0.5, rtmp.streams[0].latencySeconds())
        val srtla = decodeJson(
            SettingsSrtlaServer.serializer(),
            "{\"srtPort\":4001,\"srtlaPort\":65535,\"streams\":[{\"name\":\"S\",\"streamId\":\"sid\"}]}",
        )
        assertEquals(4001, srtla.srtPort)
        assertEquals(65535, srtla.srtlaPort)
        assertEquals("sid", srtla.streams[0].streamId)
        val rist = decodeJson(
            SettingsRistServer.serializer(),
            "{\"port\":7000,\"streams\":[{\"virtualDestinationPort\":3,\"latency\":100}]}",
        )
        assertEquals(7000, rist.port)
        assertEquals(3, rist.streams[0].virtualDestinationPort)
        assertEquals(100, rist.streams[0].latency)
        assertEquals(1, rist.makeUniqueVirtualDestinationPort())
        val rtsp = decodeJson(
            SettingsRtspClient.serializer(),
            "{\"streams\":[{\"url\":\"rtsp://x\",\"enabled\":true,\"transport\":\"rtpUdp\"}]}",
        )
        assertEquals("rtsp://x", rtsp.streams[0].url)
        assertTrue(rtsp.streams[0].enabled)
        assertEquals(SettingsRtspTransport.rtpUdp, rtsp.streams[0].transport)
        val whip = decodeJson(
            SettingsWhipServer.serializer(),
            "{\"port\":8311,\"streams\":[{\"streamKey\":\"w\",\"latency\":200,\"syncTimestamps\":false}]}",
        )
        assertEquals(8311, whip.port)
        assertEquals("w", whip.streams[0].streamKey)
        assertFalse(whip.streams[0].syncTimestamps)
        val whep = decodeJson(
            SettingsWhepClient.serializer(),
            "{\"streams\":[{\"url\":\"https://x\",\"latency\":300}]}",
        )
        assertEquals(300, whep.streams[0].latency)
        val srtClient = decodeJson(SettingsSrtClient.serializer(), "{\"streams\":[{\"url\":\"srt://x\"}]}")
        assertEquals("srt://x", srtClient.streams[0].url)
        assertRoundTrip(SettingsRtmpServer.serializer(), rtmp)
        assertRoundTrip(SettingsRistServer.serializer(), rist)
        assertRoundTrip(SettingsRtspClient.serializer(), rtsp)
    }

    @Test
    fun wrongTypedFieldsFallBackToDefaults() {
        val rtmp = decodeJson(
            SettingsRtmpServer.serializer(),
            "{\"enabled\":\"yes\",\"port\":70000,\"streams\":5}",
        )
        assertFalse(rtmp.enabled)
        assertEquals(DefaultTcpPorts.rtmpServer, rtmp.port)
        assertTrue(rtmp.streams.isEmpty())
        assertEquals(DefaultTcpPorts.rtmpServer, decodeJson(SettingsRtmpServer.serializer(), "{\"port\":-1}").port)
        val rist = decodeJson(
            SettingsRistServerStream.serializer(),
            "{\"virtualDestinationPort\":-3,\"latency\":\"slow\",\"name\":7}",
        )
        assertEquals(1, rist.virtualDestinationPort)
        assertEquals(2000, rist.latency)
        assertEquals(SettingsRistServerStream.baseName, rist.name)
        val srt = decodeJson(
            SettingsStreamSrt.serializer(),
            "{\"latency\":\"x\",\"overheadBandwidth\":1.5,\"implementation\":\"Nope\",\"adaptiveBitrate\":[]," +
                "\"connectionPriorities\":{\"enabled\":true}}",
        )
        assertEquals(defaultSrtLatency, srt.latency)
        assertEquals(25, srt.overheadBandwidth)
        assertEquals(SettingsStreamSrtImplementation.moblin, srt.implementation)
        assertEquals(SettingsStreamSrtAdaptiveBitrateAlgorithm.belabox, srt.adaptiveBitrate.algorithm)
        assertFalse(srt.connectionPriorities.enabled)
        assertEquals(listOf("Cellular", "WiFi"), srt.connectionPriorities.priorities.map { it.name })
        val stream = decodeJson(
            SettingsStream.serializer(),
            "{\"bitrate\":-5,\"id\":\"not-a-uuid\",\"srt\":\"x\",\"kickChannelId\":5," +
                "\"rist\":{\"adaptiveBitrateEnabled\":\"no\",\"bonding\":false},\"twitchRewards\":[{}]}",
        )
        assertEquals(5_000_000, stream.bitrate)
        assertEquals(defaultSrtLatency, stream.srt.latency)
        assertNull(stream.kickChannelId)
        assertTrue(stream.rist.bonding)
        assertTrue(stream.twitchRewards.isEmpty())
        val chat = decodeJson(
            SettingsChat.serializer(),
            "{\"fontSize\":\"big\",\"usernamesToIgnore\":{},\"displayStyle\":3,\"textToSpeechDefaultLanguage\":1}",
        )
        assertEquals(19.0f, chat.fontSize)
        assertTrue(chat.filters.isEmpty())
        assertEquals(SettingsChatDisplayStyle.internationalName, chat.displayStyle)
        assertNull(chat.textToSpeechDefaultLanguage)
        val recording = decodeJson(
            SettingsStreamRecording.serializer(),
            "{\"recordingPath\":\"%%%\",\"audioBitrate\":-1,\"maxKeyFrameInterval\":3000000000}",
        )
        assertNull(recording.recordingPath)
        assertEquals(128_000, recording.audioBitrate)
        assertEquals(0, recording.maxKeyFrameInterval)
        val command = decodeJson(SettingsChatBotPermissionsCommand.serializer(), "{\"cooldown\":\"5\",\"minimumSubscriberTier\":true}")
        assertEquals(1, command.minimumSubscriberTier)
        val filter = decodeJson(SettingsChatFilter.serializer(), "{\"messageWords\":[1,2]}")
        assertTrue(filter.messageStartWords.isEmpty())
    }

    @Test
    fun serializersWorkInsideParentContainers() {
        val streams = listOf(SettingsStream(name = "A"), SettingsStream(name = "B"))
        val json = codableJson.encodeToString(ListSerializer(SettingsStream.serializer()), streams)
        val decoded = codableJson.decodeFromString(ListSerializer(SettingsStream.serializer()), json)
        assertEquals(listOf("A", "B"), decoded.map { it.name })
        assertEquals(streams.map { it.id }, decoded.map { it.id })
        val container = codableJson.parseToJsonElement("{\"chat\":{\"enabled\":false}}") as JsonObject
        assertFalse(container.decode("chat", SettingsChat.serializer(), SettingsChat()).enabled)
        assertTrue(container.decode("rtmpServer", SettingsRtmpServer.serializer(), SettingsRtmpServer()).streams.isEmpty())
    }

    @Test
    fun encodedKeysFollowSwiftEncodeOrder() {
        assertKeys(
            SettingsStreamSrtConnectionPriority.serializer(),
            SettingsStreamSrtConnectionPriority(),
            "id name priority enabled relayId",
        )
        assertKeys(
            SettingsStreamSrtConnectionPriorities.serializer(),
            SettingsStreamSrtConnectionPriorities(),
            "enabled priorities",
        )
        assertKeys(
            SettingsStreamSrtAdaptiveBitrateFastIrlSettings.serializer(),
            SettingsStreamSrtAdaptiveBitrateFastIrlSettings(),
            "packetsInFlight minimumBitrate",
        )
        assertKeys(
            SettingsStreamSrtAdaptiveBitrateCustomSettings.serializer(),
            SettingsStreamSrtAdaptiveBitrateCustomSettings(),
            "packetsInFlight pifDiffIncreaseFactor rttDiffHighDecreaseFactor rttDiffHighAllowedSpike " +
                "rttDiffHighMinimumDecrease minimumBitrate",
        )
        assertKeys(
            SettingsStreamSrtAdaptiveBitrateBelaboxSettings.serializer(),
            SettingsStreamSrtAdaptiveBitrateBelaboxSettings(),
            "minimumBitrate",
        )
        assertKeys(
            SettingsStreamSrtAdaptiveBitrate.serializer(),
            SettingsStreamSrtAdaptiveBitrate(),
            "algorithm fastIrlSettings customSettings belaboxSettings",
        )
        assertKeys(
            SettingsStreamSrt.serializer(),
            SettingsStreamSrt(),
            "latency maximumBandwidthFollowInput overheadBandwidth adaptiveBitrateEnabled adaptiveBitrate " +
                "connectionPriorities mpegtsPacketsPerPacket dnsLookupStrategy implementation bigPackets " +
                "bigPacketsMigrated implemenationMigrated",
        )
        assertKeys(SettingsStreamRtmp.serializer(), SettingsStreamRtmp(), "adaptiveBitrateEnabled")
        assertKeys(SettingsStreamRist.serializer(), SettingsStreamRist(), "adaptiveBitrateEnabled bonding")
        assertKeys(SettingsHttpHeader.serializer(), SettingsHttpHeader(), "name value")
        assertKeys(SettingsStreamWhip.serializer(), SettingsStreamWhip(), "headers httpTransport")
        assertKeys(SettingsStreamChat.serializer(), SettingsStreamChat(), "bttvEmotes ffzEmotes seventvEmotes")
        assertKeys(
            SettingsStreamRecording.serializer(),
            SettingsStreamRecording(),
            "overrideStream resolution fps videoCodec videoBitrate maxKeyFrameInterval audioBitrate " +
                "autoStartRecording autoStopRecording cleanRecordings cleanSnapshots recordingPath",
        )
        assertKeys(SettingsStreamPreviewStream.serializer(), SettingsStreamPreviewStream(), "url resolution bitrate")
        assertKeys(SettingsStreamReplayStinger.serializer(), SettingsStreamReplayStinger(), "id name transitionPoint")
        assertKeys(
            SettingsStreamReplay.serializer(),
            SettingsStreamReplay(),
            "enabled transitionType inStinger outStinger postTriggerDelay x y size alignment positioningLock " +
                "enterForegroundCountAtLatestUsage",
        )
        assertKeys(SettingsStreamTwitchReward.serializer(), SettingsStreamTwitchReward(), "id rewardId title alert")
        assertKeys(
            SettingsStreamTwitchRaidChannel.serializer(),
            SettingsStreamTwitchRaidChannel(),
            "channelId channelName timestamp",
        )
        assertKeys(
            SettingsStreamMultiStreamingDestination.serializer(),
            SettingsStreamMultiStreamingDestination(),
            "name url enabled",
        )
        assertKeys(SettingsStreamMultiStreaming.serializer(), SettingsStreamMultiStreaming(), "destinations")
        assertKeys(
            SettingsTwitchAlerts.serializer(),
            SettingsTwitchAlerts(),
            "follows subscriptions giftSubscriptions resubscriptions rewards raids cheers minimumCheerBits " +
                "watchStreaks minimumWatchStreak sharedChat",
        )
        assertKeys(
            SettingsKickAlerts.serializer(),
            SettingsKickAlerts(),
            "subscriptions giftedSubscriptions rewards hosts bans kicks minimumKicks",
        )
        assertKeys(
            SettingsStream.serializer(),
            SettingsStream(),
            """
            name id enabled url twitchChannelName twitchChannelId twitchShowFollows twitchAccessToken
            twitchLoggedIn twitchWantsToBeLoggedIn twitchNotLoggedInCount twitchRewards twitchRaidsSent
            twitchRaidsReceived twitchSendMessagesTo twitchChatAlerts twitchToastAlerts kickChannelName
            kickChannelId kickChatroomChannelId kickSlug kickAccessToken kickLoggedIn kickWantsToBeLoggedIn
            kickNotLoggedInCount kickSendMessagesTo kickChatAlerts kickToastAlerts youTubeVideoId
            youTubeWantsToBeLoggedIn youTubeNotLoggedInCount youTubeHandle youTubeScheduleStreamTitle
            youTubeScheduleStreamVisibility youTubeScheduleStreamAutoStop afreecaTvChannelName afreecaTvStreamId
            openStreamingPlatformUrl openStreamingPlatformChannelId obsWebSocketEnabled obsWebSocketUrl
            obsWebSocketPassword obsSourceName obsMainScene obsBrbScene obsBrbSceneVideoSourceBroken
            obsAutoStartStream obsAutoStopStream obsAutoStartRecording obsAutoStopRecording streamingDirectlyToObs
            discordSnapshotWebhook discordChatBotSnapshotWebhook discordSnapshotWebhookOnlyWhenLive resolution fps
            autoFps bitrate bitrateRateControl codec h264Profile colorRange bFrames adaptiveEncoderResolution
            adaptiveEncoderResolutionThreashold adaptiveBitrate srt rtmp rist whip maxKeyFrameInterval audioCodec
            audioBitrate chat recording realtimeIrlEnabled realtimeIrlBaseUrl realtimeIrlPushKey portrait
            backgroundStreaming backgroundStreamingPiP estimatedViewerDelay ntpPoolAddress timecodesEnabled replay
            goLiveNotificationDiscordMessage goLiveNotificationDiscordWebhookUrl goLiveNotificationMoblinWebsite
            multiStreaming previewStream autoGoLive
            """,
        )
        assertKeys(
            SettingsChatFilter.serializer(),
            SettingsChatFilter(),
            "id enabled value messageWords showOnScreen textToSpeech chatBot poll print",
        )
        assertKeys(
            SettingsChatBotPermissionsCommand.serializer(),
            SettingsChatBotPermissionsCommand(),
            "moderatorsEnabled subscribersEnabled minimumSubscriberTier othersEnabled sendChatMessages cooldown",
        )
        assertKeys(
            SettingsChatBotPermissions.serializer(),
            SettingsChatBotPermissions(),
            "tts fix map alert fax snapshot filter zoom tesla audio reaction scene stream widget location ai " +
                "twitch gimbal macro send music migrated",
        )
        assertKeys(SettingsChatBotAlias.serializer(), SettingsChatBotAlias(), "alias replacement")
        assertKeys(
            SettingsChatBotCustomCommand.serializer(),
            SettingsChatBotCustomCommand(),
            "id name formatString permissions",
        )
        assertKeys(
            SettingsChatPredefinedMessage.serializer(),
            SettingsChatPredefinedMessage(),
            "id text blueTag greenTag yellowTag orangeTag redTag",
        )
        assertKeys(
            SettingsChatPredefinedMessagesFilter.serializer(),
            SettingsChatPredefinedMessagesFilter(),
            "redTag greenTag blueTag yellowTag orangeTag",
        )
        assertKeys(SettingsChatNickname.serializer(), SettingsChatNickname(), "id user nickname")
        assertKeys(SettingsChatNicknames.serializer(), SettingsChatNicknames(), "nicknames")
        assertKeys(SettingsOpenAi.serializer(), SettingsOpenAi(), "baseUrl apiKey model role")
        assertKeys(SettingsVoiceApple.serializer(), SettingsVoiceApple(), "voice")
        assertKeys(SettingsVoiceTtsMonster.serializer(), SettingsVoiceTtsMonster(), "name voiceId")
        assertKeys(SettingsVoice.serializer(), SettingsVoice(), "type apple ttsMonster")
        assertKeys(
            SettingsChat.serializer(),
            SettingsChat(),
            """
            fontSize fontFamily fontStyle usernameColor sameUsernameColor messageColor backgroundColor
            backgroundColorEnabled shadowColor shadowColorEnabled boldUsername boldMessage animatedEmotes
            timestampColor timestampColorEnabled height width activityFeed activityFeedHeight maximumAge
            maximumAgeEnabled meInUsernameColor enabled usernamesToIgnore textToSpeechEnabled
            textToSpeechDefaultLanguage textToSpeechDetectLanguagePerMessage textToSpeechSayUsername textToSpeechRate
            textToSpeechSayVolume textToSpeechLanguageVoices textToSpeechSubscribersOnly textToSpeechFilter
            textToSpeechFilterMentions textToSpeechBluetoothSpeakerOnly ttsMonster mirrored botEnabled
            botCommandPermissions botSendLowBatteryWarning botCommandAi badges showFirstTimeChatterMessage
            showNewFollowerMessage bottomPoints newMessagesAtTop textToSpeechPauseBetweenMessages showDeletedMessages
            aliases customCommands predefinedMessages predefinedMessagesFilter nicknames displayStyle background
            sharedChatIcons bigGifScale compactEvents
            """,
        )
        assertKeys(SettingsRtmpServerStream.serializer(), SettingsRtmpServerStream(), "id name streamKey latency")
        assertKeys(SettingsRtmpServer.serializer(), SettingsRtmpServer(), "enabled port streams")
        assertKeys(SettingsSrtlaServerStream.serializer(), SettingsSrtlaServerStream(), "id name streamId")
        assertKeys(SettingsSrtlaServer.serializer(), SettingsSrtlaServer(), "enabled srtPort srtlaPort streams")
        assertKeys(SettingsSrtClientStream.serializer(), SettingsSrtClientStream(), "id name url enabled")
        assertKeys(SettingsSrtClient.serializer(), SettingsSrtClient(), "streams")
        assertKeys(
            SettingsRistServerStream.serializer(),
            SettingsRistServerStream(),
            "id name virtualDestinationPort latency",
        )
        assertKeys(SettingsRistServer.serializer(), SettingsRistServer(), "enabled port streams")
        assertKeys(
            SettingsRtspClientStream.serializer(),
            SettingsRtspClientStream(),
            "id name url enabled latency transport",
        )
        assertKeys(SettingsRtspClient.serializer(), SettingsRtspClient(), "streams")
        assertKeys(
            SettingsWhipServerStream.serializer(),
            SettingsWhipServerStream(),
            "id name streamKey latency syncTimestamps",
        )
        assertKeys(SettingsWhipServer.serializer(), SettingsWhipServer(), "enabled port streams")
        assertKeys(
            SettingsWhepClientStream.serializer(),
            SettingsWhepClientStream(),
            "id name url enabled latency syncTimestamps",
        )
        assertKeys(SettingsWhepClient.serializer(), SettingsWhepClient(), "streams")
    }

    @Test
    fun enumCasesUseSwiftRawValues() {
        assertRawValues(SettingsStreamCodec.serializer(), SettingsStreamCodec.entries, "H.265/HEVC", "H.264/AVC")
        assertRawValues(
            SettingsStreamH264Profile.serializer(),
            SettingsStreamH264Profile.entries,
            "Baseline",
            "Main",
            "High",
        )
        assertRawValues(SettingsStreamRateControl.serializer(), SettingsStreamRateControl.entries, "ABR", "CBR", "VBR")
        assertRawValues(
            SettingsStreamResolution.serializer(),
            SettingsStreamResolution.entries,
            "4032x3024",
            "3840x2160",
            "2560x1440",
            "1920x1440",
            "1920x1080",
            "1664x936",
            "1280x720",
            "1024x768",
            "960x540",
            "854x480",
            "640x360",
            "426x240",
        )
        assertRawValues(
            SettingsStreamSrtImplementation.serializer(),
            SettingsStreamSrtImplementation.entries,
            "Moblin",
            "Official",
        )
        assertRawValues(SettingsStreamAudioCodec.serializer(), SettingsStreamAudioCodec.entries, "AAC", "OPUS")
        assertRawValues(
            SettingsStreamProtocol.serializer(),
            SettingsStreamProtocol.entries,
            "RTMP",
            "SRT",
            "RIST",
            "WHIP",
            "Mobcam",
        )
        assertRawValues(
            SettingsStreamReplayTransitionType.serializer(),
            SettingsStreamReplayTransitionType.entries,
            "fade",
            "stingers",
            "none",
        )
        assertRawValues(
            SettingsChatDisplayStyle.serializer(),
            SettingsChatDisplayStyle.entries,
            "internationalName",
            "internationalNameAndUsername",
            "username",
        )
        assertRawValues(SettingsVoiceType.serializer(), SettingsVoiceType.entries, "apple", "ttsMonster")
        assertRawValues(SettingsRtspTransport.serializer(), SettingsRtspTransport.entries, "rtpRtspTcp", "rtpUdp")
        assertCaseNames(
            SettingsStreamSrtAdaptiveBitrateAlgorithm.serializer(),
            SettingsStreamSrtAdaptiveBitrateAlgorithm.entries,
            "belabox",
            "fastIrl",
            "slowIrl",
            "customIrl",
        )
        assertCaseNames(
            SettingsStreamWhipHttpTransport.serializer(),
            SettingsStreamWhipHttpTransport.entries,
            "standard",
            "remoteControl",
        )
        val escaped = decodeJson(SettingsStreamRecording.serializer(), "{\"videoCodec\":\"H.264\\/AVC\"}")
        assertEquals(SettingsStreamCodec.h264avc, escaped.videoCodec)
    }

    @Test
    fun unknownRawValuesFallBackLikeSwift() {
        val stream = decodeJson(
            SettingsStream.serializer(),
            "{\"h264Profile\":\"Ultra\",\"bitrateRateControl\":\"XBR\",\"audioCodec\":\"MP3\",\"codec\":\"VP9\"," +
                "\"youTubeScheduleStreamVisibility\":\"secret\"}",
        )
        assertEquals(SettingsStreamH264Profile.main, stream.h264Profile)
        assertEquals(SettingsStreamRateControl.abr, stream.rateControl)
        assertEquals(SettingsStreamAudioCodec.aac, stream.audioCodec)
        assertEquals(SettingsStreamCodec.h264avc, stream.codec)
        assertEquals(YouTubeApiLiveBroadcaseVisibility.public, stream.youTubeScheduleStreamVisibility)
        val srt = decodeJson(SettingsStreamSrt.serializer(), "{\"dnsLookupStrategy\":\"IPv5\"}")
        assertEquals(SettingsDnsLookupStrategy.system, srt.dnsLookupStrategy)
        val rtsp = decodeJson(SettingsRtspClientStream.serializer(), "{\"transport\":\"tcp\"}")
        assertEquals(SettingsRtspTransport.rtpRtspTcp, rtsp.transport)
        val chat = decodeJson(SettingsChat.serializer(), "{\"displayStyle\":\"bogus\"}")
        assertEquals(SettingsChatDisplayStyle.internationalName, chat.displayStyle)
        val replay = decodeJson(SettingsStreamReplay.serializer(), "{\"transitionType\":\"none\",\"alignment\":\"Middle\"}")
        assertEquals(SettingsStreamReplayTransitionType.none, replay.transitionType)
        assertEquals(SettingsAlignment.topLeft, replay.layout.alignment)
        val voices = decodeJson(
            SettingsChat.serializer(),
            "{\"textToSpeechLanguageVoices\":{\"sv\":{\"type\":1,\"apple\":{\"voice\":\"\"}," +
                "\"ttsMonster\":{\"name\":\"\",\"voiceId\":\"\"}}}}",
        )
        assertTrue(voices.textToSpeechLanguageVoices.isEmpty())
    }

    @Test
    fun loginFlagsDefaultLikeSwift() {
        val derived = decodeJson(SettingsStream.serializer(), "{\"twitchLoggedIn\":true,\"kickLoggedIn\":true}")
        assertTrue(derived.twitchWantsToBeLoggedIn)
        assertTrue(derived.kickWantsToBeLoggedIn)
        assertTrue(derived.kickLoggedIn)
        assertFalse(derived.youTubeWantsToBeLoggedIn)
        val explicit = decodeJson(
            SettingsStream.serializer(),
            "{\"twitchLoggedIn\":true,\"twitchWantsToBeLoggedIn\":false,\"kickLoggedIn\":true," +
                "\"kickWantsToBeLoggedIn\":\"no\",\"youTubeWantsToBeLoggedIn\":true}",
        )
        assertFalse(explicit.twitchWantsToBeLoggedIn)
        assertTrue(explicit.kickWantsToBeLoggedIn)
        assertTrue(explicit.youTubeWantsToBeLoggedIn)
        val follows = decodeJson(
            SettingsStream.serializer(),
            "{\"twitchShowFollows\":true,\"twitchChatAlerts\":{\"follows\":false}," +
                "\"twitchToastAlerts\":{\"follows\":false,\"raids\":false}}",
        )
        assertTrue(follows.twitchChatAlerts.follows)
        assertTrue(follows.twitchToastAlerts.follows)
        assertFalse(follows.twitchToastAlerts.raids)
        assertNull(follows.twitchShowFollows)
        val noFollows = decodeJson(
            SettingsStream.serializer(),
            "{\"twitchShowFollows\":null,\"twitchChatAlerts\":{\"follows\":false}}",
        )
        assertFalse(noFollows.twitchChatAlerts.follows)
        assertTrue(noFollows.twitchToastAlerts.follows)
    }

    @Test
    fun unsignedFieldsKeepSwiftRanges() {
        val maximum = decodeJson(
            SettingsStream.serializer(),
            "{\"bitrate\":4294967295,\"recording\":{\"videoBitrate\":4294967295,\"audioBitrate\":3000000000}," +
                "\"previewStream\":{\"bitrate\":2147483648}}",
        )
        val encoded = codableJson.encodeToJsonElement(SettingsStream.serializer(), maximum).jsonObject
        assertEquals("4294967295", encoded["bitrate"].toString())
        assertEquals("4294967295", encoded["recording"]!!.jsonObject["videoBitrate"].toString())
        assertEquals("3000000000", encoded["recording"]!!.jsonObject["audioBitrate"].toString())
        assertEquals("2147483648", encoded["previewStream"]!!.jsonObject["bitrate"].toString())
        val outOfRange = decodeJson(
            SettingsStream.serializer(),
            "{\"bitrate\":4294967296,\"recording\":{\"videoBitrate\":-1},\"previewStream\":{\"bitrate\":1.5}}",
        )
        assertEquals(5_000_000, outOfRange.bitrate)
        assertEquals(0, outOfRange.recording.videoBitrate)
        assertEquals(500_000, outOfRange.previewStream.bitrate)
        val srtla = decodeJson(SettingsSrtlaServer.serializer(), "{\"srtPort\":65536,\"srtlaPort\":0}")
        assertEquals(DefaultUdpPorts.srtServer, srtla.srtPort)
        assertEquals(0, srtla.srtlaPort)
        assertEquals(65535, decodeJson(SettingsWhipServer.serializer(), "{\"port\":65535}").port)
        assertEquals(
            DefaultUdpPorts.ristServer,
            decodeJson(SettingsRistServer.serializer(), "{\"port\":65536}").port,
        )
        val rist = decodeJson(SettingsRistServerStream.serializer(), "{\"virtualDestinationPort\":65536}")
        assertEquals(1, rist.virtualDestinationPort)
        val srt = decodeJson(SettingsStreamSrt.serializer(), "{\"latency\":2147483648,\"overheadBandwidth\":-2147483648}")
        assertEquals(defaultSrtLatency, srt.latency)
        assertEquals(Int.MIN_VALUE, srt.overheadBandwidth)
    }

    @Test
    fun clonesCopyValueTypesLikeSwift() {
        val replay = SettingsStreamReplay()
        replay.layout = replay.layout.copy(x = 5.0)
        replay.inStinger = replay.inStinger.copy(name = "in.mov")
        val replayCopy = replay.clone()
        replayCopy.layout = replayCopy.layout.copy(x = 7.0)
        replayCopy.inStinger = replayCopy.inStinger.copy(name = "other.mov")
        assertEquals(5.0, replay.layout.x)
        assertEquals("in.mov", replay.inStinger.name)
        assertEquals(7.0, replayCopy.layout.x)
        val whip = SettingsStreamWhip()
        whip.headers.add(SettingsHttpHeader("A", "B"))
        val whipCopy = whip.clone()
        whipCopy.headers[0] = whipCopy.headers[0].copy(value = "C")
        assertEquals("B", whip.headers[0].value)
        assertEquals("C", whipCopy.headers[0].value)
    }

    @Test
    fun swiftEncodedStreamRoundTripsUnchanged() {
        val alert = codableJson.encodeToString(SettingsWidgetAlertsAlert.serializer(), SettingsWidgetAlertsAlert())
        val twitchAlerts = "{\"follows\":false,\"subscriptions\":false,\"giftSubscriptions\":false," +
            "\"resubscriptions\":false,\"rewards\":false,\"raids\":false,\"cheers\":false,\"minimumCheerBits\":100," +
            "\"watchStreaks\":false,\"minimumWatchStreak\":10,\"sharedChat\":true}"
        val kickAlerts = "{\"subscriptions\":false,\"giftedSubscriptions\":false,\"rewards\":false,\"hosts\":false," +
            "\"bans\":false,\"kicks\":false,\"minimumKicks\":50}"
        val json = """
            {
              "name": "IRL",
              "id": "$sampleId",
              "enabled": true,
              "url": "srtla:\/\/example.com:5000",
              "twitchChannelName": "tc",
              "twitchChannelId": "123",
              "twitchShowFollows": null,
              "twitchAccessToken": "tok",
              "twitchLoggedIn": true,
              "twitchWantsToBeLoggedIn": false,
              "twitchNotLoggedInCount": 2,
              "twitchRewards": [{"id": "$sampleId2", "rewardId": "r1", "title": "Hydrate", "alert": $alert}],
              "twitchRaidsSent": [{"channelId": "1", "channelName": "one", "timestamp": 1000.5}],
              "twitchRaidsReceived": [{"channelId": "2", "channelName": "two", "timestamp": 2000}],
              "twitchSendMessagesTo": false,
              "twitchChatAlerts": $twitchAlerts,
              "twitchToastAlerts": $twitchAlerts,
              "kickChannelName": "kc",
              "kickChannelId": "k1",
              "kickChatroomChannelId": null,
              "kickSlug": "slug",
              "kickAccessToken": "",
              "kickLoggedIn": true,
              "kickWantsToBeLoggedIn": false,
              "kickNotLoggedInCount": 3,
              "kickSendMessagesTo": false,
              "kickChatAlerts": $kickAlerts,
              "kickToastAlerts": $kickAlerts,
              "youTubeVideoId": "a,b",
              "youTubeWantsToBeLoggedIn": true,
              "youTubeNotLoggedInCount": 4,
              "youTubeHandle": "@h",
              "youTubeScheduleStreamTitle": "Title",
              "youTubeScheduleStreamVisibility": "unlisted",
              "youTubeScheduleStreamAutoStop": false,
              "afreecaTvChannelName": "soop",
              "afreecaTvStreamId": "soopId",
              "openStreamingPlatformUrl": "https:\/\/osp",
              "openStreamingPlatformChannelId": "osp1",
              "obsWebSocketEnabled": true,
              "obsWebSocketUrl": "ws:\/\/obs",
              "obsWebSocketPassword": "pw",
              "obsSourceName": "src",
              "obsMainScene": "main",
              "obsBrbScene": "brb",
              "obsBrbSceneVideoSourceBroken": true,
              "obsAutoStartStream": true,
              "obsAutoStopStream": true,
              "obsAutoStartRecording": true,
              "obsAutoStopRecording": true,
              "streamingDirectlyToObs": true,
              "discordSnapshotWebhook": "https:\/\/d1",
              "discordChatBotSnapshotWebhook": "https:\/\/d2",
              "discordSnapshotWebhookOnlyWhenLive": false,
              "resolution": "1280x720",
              "fps": 60,
              "autoFps": true,
              "bitrate": 3000000000,
              "bitrateRateControl": "VBR",
              "codec": "H.264\/AVC",
              "h264Profile": "High",
              "colorRange": "Limited",
              "bFrames": true,
              "adaptiveEncoderResolution": true,
              "adaptiveEncoderResolutionThreashold": 0.5,
              "adaptiveBitrate": false,
              "srt": {
                "latency": 500,
                "maximumBandwidthFollowInput": false,
                "overheadBandwidth": 30,
                "adaptiveBitrateEnabled": false,
                "adaptiveBitrate": {
                  "algorithm": {"customIrl": {}},
                  "fastIrlSettings": {"packetsInFlight": 300, "minimumBitrate": 100},
                  "customSettings": {
                    "packetsInFlight": 250,
                    "pifDiffIncreaseFactor": 50,
                    "rttDiffHighDecreaseFactor": 0.75,
                    "rttDiffHighAllowedSpike": 25,
                    "rttDiffHighMinimumDecrease": 125,
                    "minimumBitrate": 300
                  },
                  "belaboxSettings": {"minimumBitrate": 500}
                },
                "connectionPriorities": {
                  "enabled": true,
                  "priorities": [
                    {"id": "$sampleId", "name": "WiFi", "priority": 3, "enabled": false, "relayId": "$sampleRelayId"},
                    {"id": "$sampleId2", "name": "Cellular", "priority": 1, "enabled": true, "relayId": null}
                  ]
                },
                "mpegtsPacketsPerPacket": 6,
                "dnsLookupStrategy": "IPv4 and IPv6",
                "implementation": "Official",
                "bigPackets": false,
                "bigPacketsMigrated": true,
                "implemenationMigrated": true
              },
              "rtmp": {"adaptiveBitrateEnabled": false},
              "rist": {"adaptiveBitrateEnabled": false, "bonding": false},
              "whip": {"headers": [{"name": "A", "value": "B"}], "httpTransport": {"remoteControl": {}}},
              "maxKeyFrameInterval": 4,
              "audioCodec": "OPUS",
              "audioBitrate": 96000,
              "chat": {"bttvEmotes": true, "ffzEmotes": false, "seventvEmotes": true},
              "recording": {
                "overrideStream": true,
                "resolution": "3840x2160",
                "fps": 25,
                "videoCodec": "H.264\/AVC",
                "videoBitrate": 3000000,
                "maxKeyFrameInterval": 1,
                "audioBitrate": 64000,
                "autoStartRecording": true,
                "autoStopRecording": true,
                "cleanRecordings": true,
                "cleanSnapshots": true,
                "recordingPath": "AQID"
              },
              "realtimeIrlEnabled": true,
              "realtimeIrlBaseUrl": "https:\/\/rtirl.example\/api",
              "realtimeIrlPushKey": "push",
              "portrait": true,
              "backgroundStreaming": true,
              "backgroundStreamingPiP": false,
              "estimatedViewerDelay": 3.5,
              "ntpPoolAddress": "pool.ntp.org",
              "timecodesEnabled": true,
              "replay": {
                "enabled": true,
                "transitionType": "stingers",
                "inStinger": {"id": "$sampleId", "name": "in.mov", "transitionPoint": 0.25},
                "outStinger": {"id": "$sampleId2", "name": "out.mov", "transitionPoint": 0.75},
                "postTriggerDelay": 5,
                "x": 10,
                "y": 20.5,
                "size": 50,
                "alignment": "BottomRight",
                "positioningLock": true,
                "enterForegroundCountAtLatestUsage": 4
              },
              "goLiveNotificationDiscordMessage": "Live!",
              "goLiveNotificationDiscordWebhookUrl": "https:\/\/d3",
              "goLiveNotificationMoblinWebsite": true,
              "multiStreaming": {"destinations": [{"name": "D", "url": "rtmp:\/\/x\/y", "enabled": true}]},
              "previewStream": {"url": "srt:\/\/p", "resolution": "426x240", "bitrate": 250000},
              "autoGoLive": true
            }
        """.trimIndent()
        assertSwiftJsonRoundTrips(SettingsStream.serializer(), json)
        val stream = decodeJson(SettingsStream.serializer(), json)
        assertEquals("srtla://example.com:5000", stream.url)
        assertEquals("tok", stream.twitchAccessToken)
        assertFalse(stream.twitchWantsToBeLoggedIn)
        assertEquals("Hydrate", stream.twitchRewards[0].title)
        assertEquals(UUID.fromString(sampleId2), stream.twitchRewards[0].id)
        assertEquals(10, stream.twitchChatAlerts.minimumWatchStreak)
        assertTrue(stream.kickToastAlerts.minimumKicks == 50 && !stream.kickToastAlerts.bans)
        assertNull(stream.kickChatroomChannelId)
        assertEquals("slug", stream.kickSlug)
        assertTrue(stream.youTubeWantsToBeLoggedIn)
        assertEquals(SettingsStreamRateControl.vbr, stream.rateControl)
        assertEquals(3_000_000_000u, stream.bitrate.toUInt())
        assertEquals(SettingsStreamSrtAdaptiveBitrateAlgorithm.customIrl, stream.srt.adaptiveBitrate.algorithm)
        assertEquals(0.75f, stream.srt.adaptiveBitrate.customSettings.rttDiffHighDecreaseFactor)
        assertEquals(500f, stream.srt.adaptiveBitrate.belaboxSettings.minimumBitrate)
        assertNull(stream.srt.connectionPriorities.priorities[1].relayId)
        assertEquals(SettingsDnsLookupStrategy.ipv4AndIpv6, stream.srt.dnsLookupStrategy)
        assertFalse(stream.rist.bonding)
        assertTrue(stream.chat.seventvEmotes)
        assertEquals(SettingsStreamResolution.r3840x2160, stream.recording.resolution)
        assertEquals(64_000, stream.recording.audioBitrate)
        assertTrue(stream.recording.cleanSnapshots)
        assertEquals("https://rtirl.example/api", stream.realtimeIrlBaseUrl)
        assertEquals(SettingsStreamReplayTransitionType.stingers, stream.replay.transitionType)
        assertEquals("out.mov", stream.replay.outStinger.name)
        assertEquals(20.5, stream.replay.layout.y)
        assertTrue(stream.replay.layout.positioningLock)
        assertEquals(4, stream.replay.enterForegroundCountAtLatestUsage)
    }

    @Test
    fun swiftEncodedChatRoundTripsUnchanged() {
        val commandNames = listOf(
            "tts", "fix", "map", "alert", "fax", "snapshot", "filter", "zoom", "tesla", "audio", "reaction",
            "scene", "stream", "widget", "location", "ai", "twitch", "gimbal", "macro", "send", "music",
        )
        val permissions = commandNames.mapIndexed { index, name ->
            "\"$name\":{\"moderatorsEnabled\":${index % 2 == 0},\"subscribersEnabled\":${index % 2 == 1}," +
                "\"minimumSubscriberTier\":${index % 3 + 1},\"othersEnabled\":${index % 3 == 0}," +
                "\"sendChatMessages\":${index % 5 == 0},\"cooldown\":${if (index % 4 == 0) "null" else "$index"}}"
        }.joinToString(",", "{", ",\"migrated\":true}")
        val command = "{\"moderatorsEnabled\":false,\"subscribersEnabled\":true,\"minimumSubscriberTier\":2," +
            "\"othersEnabled\":true,\"sendChatMessages\":true,\"cooldown\":30}"
        val json = """
            {
              "fontSize": 25.5,
              "fontFamily": "Georgia",
              "fontStyle": "Georgia-Bold",
              "usernameColor": {"red": 1, "green": 2, "blue": 3, "opacity": 0.5},
              "sameUsernameColor": true,
              "messageColor": {"red": 4, "green": 5, "blue": 6, "opacity": 1},
              "backgroundColor": {"red": 7, "green": 8, "blue": 9, "opacity": 0.25},
              "backgroundColorEnabled": true,
              "shadowColor": {"red": 10, "green": 11, "blue": 12, "opacity": 0.75},
              "shadowColorEnabled": false,
              "boldUsername": false,
              "boldMessage": false,
              "animatedEmotes": true,
              "timestampColor": {"red": 13, "green": 14, "blue": 15, "opacity": 1},
              "timestampColorEnabled": true,
              "height": 0.5,
              "width": 0.75,
              "activityFeed": false,
              "activityFeedHeight": 0.3,
              "maximumAge": 60,
              "maximumAgeEnabled": true,
              "meInUsernameColor": false,
              "enabled": false,
              "usernamesToIgnore": [
                {
                  "id": "$sampleId",
                  "enabled": false,
                  "value": "bot",
                  "messageWords": ["!a", "b"],
                  "showOnScreen": true,
                  "textToSpeech": true,
                  "chatBot": true,
                  "poll": true,
                  "print": true
                }
              ],
              "textToSpeechEnabled": true,
              "textToSpeechDefaultLanguage": "sv",
              "textToSpeechDetectLanguagePerMessage": true,
              "textToSpeechSayUsername": false,
              "textToSpeechRate": 0.7,
              "textToSpeechSayVolume": 0.5,
              "textToSpeechLanguageVoices": {
                "sv": {"type": "ttsMonster", "apple": {"voice": "a"}, "ttsMonster": {"name": "N", "voiceId": "V"}}
              },
              "textToSpeechSubscribersOnly": true,
              "textToSpeechFilter": false,
              "textToSpeechFilterMentions": false,
              "textToSpeechBluetoothSpeakerOnly": true,
              "ttsMonster": {"apiToken": "token"},
              "mirrored": true,
              "botEnabled": true,
              "botCommandPermissions": $permissions,
              "botSendLowBatteryWarning": true,
              "botCommandAi": {"baseUrl": "https:\/\/ai", "apiKey": "k", "model": "m", "role": "Be nice"},
              "badges": false,
              "showFirstTimeChatterMessage": false,
              "showNewFollowerMessage": false,
              "bottomPoints": 90,
              "newMessagesAtTop": true,
              "textToSpeechPauseBetweenMessages": 2.5,
              "showDeletedMessages": true,
              "aliases": [{"alias": "!a", "replacement": "!b"}],
              "customCommands": [{"id": "$sampleId", "name": "cmd", "formatString": "{x}", "permissions": $command}],
              "predefinedMessages": [
                {
                  "id": "$sampleId",
                  "text": "hi",
                  "blueTag": true,
                  "greenTag": false,
                  "yellowTag": true,
                  "orangeTag": false,
                  "redTag": true
                }
              ],
              "predefinedMessagesFilter": {
                "redTag": true,
                "greenTag": false,
                "blueTag": true,
                "yellowTag": false,
                "orangeTag": true
              },
              "nicknames": {"nicknames": [{"id": "$sampleId", "user": "u", "nickname": "n"}]},
              "displayStyle": "username",
              "background": true,
              "sharedChatIcons": false,
              "bigGifScale": 3,
              "compactEvents": false
            }
        """.trimIndent()
        assertSwiftJsonRoundTrips(SettingsChat.serializer(), json)
        val chat = decodeJson(SettingsChat.serializer(), json)
        assertEquals(SettingsFont(family = "Georgia", style = "Georgia-Bold"), chat.font)
        assertTrue(chat.textToSpeechBluetoothSpeakerOnly)
        assertEquals(RgbColor(red = 7, green = 8, blue = 9, opacity = 0.25), chat.backgroundColor)
        assertEquals(0.3, chat.activityFeedHeight)
        assertTrue(chat.filters[0].print)
        assertEquals(SettingsVoiceType.ttsMonster, chat.textToSpeechLanguageVoices["sv"]!!.type)
        assertEquals("a", chat.textToSpeechLanguageVoices["sv"]!!.apple.voice)
        assertFalse(chat.botCommandPermissions.scene.moderatorsEnabled)
        assertTrue(chat.botCommandPermissions.stream.moderatorsEnabled)
        assertNull(chat.botCommandPermissions.tts.cooldown)
        assertEquals(1, chat.botCommandPermissions.fix.cooldown)
        assertEquals("https://ai", chat.botCommandAi.baseUrl)
        assertEquals(30, chat.customCommands[0].permissions.cooldown)
        assertEquals(2, chat.customCommands[0].permissions.minimumSubscriberTier)
        assertEquals("🐳🐥🌹", chat.predefinedMessages[0].tagsString())
        assertEquals(90.0, chat.bottomPoints)
        assertEquals(2.5, chat.textToSpeechPauseBetweenMessages)
        assertFalse(chat.compactEvents)
    }

    @Test
    fun swiftEncodedIngestsRoundTripUnchanged() {
        assertSwiftJsonRoundTrips(
            SettingsRtmpServer.serializer(),
            "{\"enabled\":true,\"port\":1936,\"streams\":[{\"id\":\"$sampleId\",\"name\":\"Cam\"," +
                "\"streamKey\":\"key\",\"latency\":500}]}",
        )
        assertSwiftJsonRoundTrips(
            SettingsSrtlaServer.serializer(),
            "{\"enabled\":true,\"srtPort\":4001,\"srtlaPort\":5001,\"streams\":[{\"id\":\"$sampleId\"," +
                "\"name\":\"S\",\"streamId\":\"sid\"}]}",
        )
        assertSwiftJsonRoundTrips(
            SettingsSrtClient.serializer(),
            "{\"streams\":[{\"id\":\"$sampleId\",\"name\":\"C\",\"url\":\"srt:\\/\\/x\",\"enabled\":true}]}",
        )
        assertSwiftJsonRoundTrips(
            SettingsRistServer.serializer(),
            "{\"enabled\":true,\"port\":6501,\"streams\":[{\"id\":\"$sampleId\",\"name\":\"R\"," +
                "\"virtualDestinationPort\":3,\"latency\":100}]}",
        )
        assertSwiftJsonRoundTrips(
            SettingsRtspClient.serializer(),
            "{\"streams\":[{\"id\":\"$sampleId\",\"name\":\"T\",\"url\":\"rtsp:\\/\\/x\",\"enabled\":true," +
                "\"latency\":300,\"transport\":\"rtpUdp\"}]}",
        )
        assertSwiftJsonRoundTrips(
            SettingsWhipServer.serializer(),
            "{\"enabled\":true,\"port\":8311,\"streams\":[{\"id\":\"$sampleId\",\"name\":\"W\"," +
                "\"streamKey\":\"w\",\"latency\":200,\"syncTimestamps\":false}]}",
        )
        assertSwiftJsonRoundTrips(
            SettingsWhepClient.serializer(),
            "{\"streams\":[{\"id\":\"$sampleId\",\"name\":\"P\",\"url\":\"https:\\/\\/x\",\"enabled\":true," +
                "\"latency\":150,\"syncTimestamps\":false}]}",
        )
        val whep = decodeJson(
            SettingsWhepClient.serializer(),
            "{\"streams\":[{\"id\":\"$sampleId\",\"url\":\"https:\\/\\/x\",\"latency\":150}]}",
        )
        assertEquals("https://x", whep.streams[0].url)
        assertEquals(0.15, whep.streams[0].latencySeconds())
        assertTrue(whep.streams[0].syncTimestamps)
        assertEquals(SettingsWhepClientStream.baseName, whep.streams[0].name)
    }

    private fun <T> assertKeys(serializer: KSerializer<T>, value: T, keys: String) {
        val actual = codableJson.encodeToJsonElement(serializer, value).jsonObject.keys.toList()
        assertEquals(keys.trim().split(Regex("\\s+")), actual)
    }

    private fun <T> assertRawValues(serializer: KSerializer<T>, entries: List<T>, vararg rawValues: String) {
        assertEquals(rawValues.size, entries.size)
        entries.forEachIndexed { index, value ->
            val json = "\"${rawValues[index]}\""
            assertEquals(json, encodeJson(serializer, value))
            assertEquals(value, decodeJson(serializer, json))
        }
    }

    private fun <T> assertCaseNames(serializer: KSerializer<T>, entries: List<T>, vararg names: String) {
        assertEquals(names.size, entries.size)
        entries.forEachIndexed { index, value ->
            val json = "{\"${names[index]}\":{}}"
            assertEquals(json, encodeJson(serializer, value))
            assertEquals(value, decodeJson(serializer, json))
        }
    }

    private fun <T> assertSwiftJsonRoundTrips(serializer: KSerializer<T>, json: String) {
        val swift = codableJson.parseToJsonElement(json)
        val kotlin = codableJson.encodeToJsonElement(serializer, codableJson.decodeFromJsonElement(serializer, swift))
        assertEquals(comparable(swift), comparable(kotlin))
    }

    private fun comparable(element: JsonElement): Any? {
        return when (element) {
            is JsonNull -> null
            is JsonObject -> element.mapValues { comparable(it.value) }
            is JsonArray -> element.map { comparable(it) }
            is JsonPrimitive -> if (element.isString) {
                element.content
            } else {
                element.booleanOrNull ?: element.content.toDouble()
            }
        }
    }
}

private const val sampleId2 = "3F2504E0-4F89-41D3-9A0C-0305E82C3301"

