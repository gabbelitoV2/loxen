package com.moblin.android.various.settings

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun copied(value: SettingsStream): SettingsStream = SettingsStream.decode(value.encode())

private fun copied(value: SettingsWidget): SettingsWidget = SettingsWidget.decode(value.encode())

private fun encoded(value: SettingsStream): String = value.encode().toString()

private fun encoded(value: SettingsWidget): String = value.encode().toString()

private fun destination(name: String, url: String): SettingsStreamMultiStreamingDestination {
    val destination = SettingsStreamMultiStreamingDestination()
    destination.name = name
    destination.url = url
    return destination
}

private fun makePhoneStream(): SettingsStream {
    val stream = SettingsStream(name = "Main")
    stream.url = "rtmps://live.example.com/app/stream-url-secret"
    stream.obsWebSocketUrl = "ws://obs.example.com:4455/obs-url-secret"
    stream.obsWebSocketPassword = "obs-password-secret"
    stream.realtimeIrlBaseUrl = "https://rtirl.example.com/rtirl-url-secret"
    stream.realtimeIrlPushKey = "rtirl-push-key-secret"
    stream.discordSnapshotWebhook = "https://discord.com/api/webhooks/1/snapshot-webhook-secret"
    stream.discordChatBotSnapshotWebhook = "https://discord.com/api/webhooks/2/chat-bot-webhook-secret"
    stream.goLiveNotificationDiscordWebhookUrl = "https://discord.com/api/webhooks/3/go-live-webhook-secret"
    stream.whip.headers = mutableListOf(
        SettingsHttpHeader(name = "Authorization", value = "Bearer whip-token-secret"),
        SettingsHttpHeader(name = "X-Custom", value = "whip-custom-secret"),
    )
    stream.previewStream.url = "whips://preview.example.com/whip/preview-url-secret"
    stream.multiStreaming.destinations = mutableListOf(
        destination("First", "rtmp://first.example.com/live/first-destination-secret"),
        destination("Second", "rtmps://second.example.com/live/second-destination-secret"),
    )
    stream.twitchAccessToken = "twitch-token-secret"
    stream.kickAccessToken = "kick-token-secret"
    return stream
}

private fun makePhoneWidget(): SettingsWidget {
    val widget = SettingsWidget(name = "Alerts")
    widget.type = SettingsWidgetType.alerts
    widget.alerts.ai.baseUrl = "https://ai.example.com/ai-url-secret"
    widget.alerts.ai.apiKey = "ai-api-key-secret"
    widget.alerts.ttsMonster.apiToken = "tts-monster-token-secret"
    return widget
}

private fun headers(stream: SettingsStream): String {
    return stream.whip.headers.joinToString(", ") { "${it.name}: ${it.value}" }
}

private fun destinationUrls(stream: SettingsStream): String {
    return stream.multiStreaming.destinations.map { it.url }.joinToString(", ")
}

private val streamSecrets = listOf(
    "stream-url-secret",
    "obs-url-secret",
    "obs-password-secret",
    "rtirl-url-secret",
    "rtirl-push-key-secret",
    "snapshot-webhook-secret",
    "chat-bot-webhook-secret",
    "go-live-webhook-secret",
    "whip-token-secret",
    "whip-custom-secret",
    "preview-url-secret",
    "first-destination-secret",
    "second-destination-secret",
    "twitch-token-secret",
    "kick-token-secret",
)

private val widgetSecrets = listOf(
    "ai-url-secret",
    "ai-api-key-secret",
    "tts-monster-token-secret",
)

@RunWith(RobolectricTestRunner::class)
class SettingsSecretsSuite {
    data class StreamCase(
        val testDescription: String,
        val expected: String,
        val value: (SettingsStream) -> String,
    )

    data class WidgetCase(
        val testDescription: String,
        val expected: String,
        val value: (SettingsWidget) -> String,
    )

    @Test
    fun removedStreamSecretsAreNotEncoded() {
        for (secret in streamSecrets) {
            val stream = copied(makePhoneStream())
            stream.twitchAccessToken = "twitch-token-secret"
            stream.kickAccessToken = "kick-token-secret"
            stream.removeSecrets()
            assertFalse(encoded(stream).contains(secret))
        }
    }

    @Test
    fun removedTokensAreNotEncoded() {
        val stream = copied(makePhoneStream())
        stream.twitchAccessToken = "twitch-token-secret"
        stream.kickAccessToken = "kick-token-secret"
        stream.removeAccessTokens()
        val json = encoded(stream)
        assertTrue(!json.contains("twitch-token-secret") && !json.contains("kick-token-secret"))
        assertTrue(json.contains("stream-url-secret"))
    }

    @Test
    fun removedStreamSecretsKeepSchemes() {
        val testCases = listOf(
            StreamCase("URL", "rtmps://") { it.url },
            StreamCase("Preview stream URL", "whips://") { it.previewStream.url },
            StreamCase("Destination URLs", "rtmp://, rtmps://") { destinationUrls(it) },
            StreamCase("WHIP headers", "") { headers(it) },
            StreamCase("OBS URL", "") { it.obsWebSocketUrl },
            StreamCase("OBS password", "") { it.obsWebSocketPassword },
            StreamCase("RealtimeIRL base URL", "") { it.realtimeIrlBaseUrl },
            StreamCase("Twitch access token", "") { it.twitchAccessToken },
            StreamCase("Kick access token", "") { it.kickAccessToken },
        )
        for (testCase in testCases) {
            val stream = makePhoneStream()
            stream.removeSecrets()
            assertEquals(testCase.expected, testCase.value(stream))
        }
    }

    @Test
    fun keptStreamSecretsAreThePhones() {
        val testCases = listOf(
            StreamCase("URL", "rtmps://live.example.com/app/stream-url-secret") { it.url },
            StreamCase("OBS URL", "ws://obs.example.com:4455/obs-url-secret") { it.obsWebSocketUrl },
            StreamCase("OBS password", "obs-password-secret") { it.obsWebSocketPassword },
            StreamCase("RealtimeIRL base URL", "https://rtirl.example.com/rtirl-url-secret") {
                it.realtimeIrlBaseUrl
            },
            StreamCase("RealtimeIRL push key", "rtirl-push-key-secret") {
                it.realtimeIrlPushKey
            },
            StreamCase("Discord snapshot webhook", "https://discord.com/api/webhooks/1/snapshot-webhook-secret") {
                it.discordSnapshotWebhook
            },
            StreamCase("Discord chat bot webhook", "https://discord.com/api/webhooks/2/chat-bot-webhook-secret") {
                it.discordChatBotSnapshotWebhook
            },
            StreamCase("Go live webhook", "https://discord.com/api/webhooks/3/go-live-webhook-secret") {
                it.goLiveNotificationDiscordWebhookUrl
            },
            StreamCase("WHIP headers", "Authorization: Bearer whip-token-secret, X-Custom: whip-custom-secret") {
                headers(it)
            },
            StreamCase("Preview stream URL", "whips://preview.example.com/whip/preview-url-secret") {
                it.previewStream.url
            },
            StreamCase(
                "Destination URLs matched by name when added or removed",
                "rtmps://second.example.com/live/second-destination-secret, " +
                    "rtmp://first.example.com/live/first-destination-secret, " +
                    "rtmp://my_public_ip:1935/live/foobar"
            ) {
                destinationUrls(it)
            },
            StreamCase("Twitch access token", "twitch-token-secret") { it.twitchAccessToken },
            StreamCase("Kick access token", "kick-token-secret") { it.kickAccessToken },
        )
        for (testCase in testCases) {
            val phone = makePhoneStream()
            val request = copied(phone)
            request.removeSecrets()
            request.multiStreaming.destinations = mutableListOf(
                destination("Second", "rtmps://"),
                destination("First", "rtmp://"),
                destination("Third", "rtmp://"),
            )
            request.keepSecrets(phone)
            assertEquals(testCase.expected, testCase.value(request))
        }
    }

    @Test
    fun renamedDestinationsKeepTheirUrls() {
        val phone = makePhoneStream()
        val request = copied(phone)
        request.removeSecrets()
        request.multiStreaming.destinations[0].name = "Renamed"
        request.keepSecrets(phone)
        assertEquals(
            "rtmp://first.example.com/live/first-destination-secret, " +
                "rtmps://second.example.com/live/second-destination-secret",
            destinationUrls(request)
        )
    }

    @Test
    fun newStreamSecretsAreDefaults() {
        val testCases = listOf(
            StreamCase("URL", "srt://my_public_ip:4000") { it.url },
            StreamCase("OBS URL", "") { it.obsWebSocketUrl },
            StreamCase("OBS password", "") { it.obsWebSocketPassword },
            StreamCase("RealtimeIRL base URL", "https://rtirl.com/api") {
                it.realtimeIrlBaseUrl
            },
            StreamCase("RealtimeIRL push key", "") { it.realtimeIrlPushKey },
            StreamCase("Discord snapshot webhook", "") { it.discordSnapshotWebhook },
            StreamCase("Discord chat bot webhook", "") { it.discordChatBotSnapshotWebhook },
            StreamCase("Go live webhook", "") { it.goLiveNotificationDiscordWebhookUrl },
            StreamCase("WHIP headers", "") { headers(it) },
            StreamCase("Preview stream URL", "") { it.previewStream.url },
            StreamCase(
                "Destination URLs",
                "rtmp://my_public_ip:1935/live/foobar, rtmp://my_public_ip:1935/live/foobar"
            ) {
                destinationUrls(it)
            },
            StreamCase("Twitch access token", "") { it.twitchAccessToken },
            StreamCase("Kick access token", "") { it.kickAccessToken },
        )
        for (testCase in testCases) {
            val stream = makePhoneStream()
            stream.keepSecrets(SettingsStream(name = ""))
            assertEquals(testCase.expected, testCase.value(stream))
        }
    }

    @Test
    fun removedWidgetSecretsAreNotEncoded() {
        for (secret in widgetSecrets) {
            val widget = copied(makePhoneWidget())
            widget.removeSecrets()
            assertFalse(encoded(widget).contains(secret))
        }
    }

    @Test
    fun keptWidgetSecretsAreThePhones() {
        val testCases = listOf(
            WidgetCase("AI base URL", "https://ai.example.com/ai-url-secret") {
                it.alerts.ai.baseUrl
            },
            WidgetCase("AI API key", "ai-api-key-secret") {
                it.alerts.ai.apiKey
            },
            WidgetCase("TTS.Monster token", "tts-monster-token-secret") {
                it.alerts.ttsMonster.apiToken
            },
        )
        for (testCase in testCases) {
            val phone = makePhoneWidget()
            val request = copied(phone)
            request.removeSecrets()
            request.keepSecrets(phone)
            assertEquals(testCase.expected, testCase.value(request))
        }
    }

    @Test
    fun newWidgetSecretsAreDefaults() {
        val testCases = listOf(
            WidgetCase("AI base URL", "https://generativelanguage.googleapis.com/v1beta/openai") {
                it.alerts.ai.baseUrl
            },
            WidgetCase("AI API key", "") {
                it.alerts.ai.apiKey
            },
            WidgetCase("TTS.Monster token", "") {
                it.alerts.ttsMonster.apiToken
            },
        )
        for (testCase in testCases) {
            val widget = makePhoneWidget()
            widget.keepSecrets(SettingsWidget(name = ""))
            assertEquals(testCase.expected, testCase.value(widget))
        }
    }
}
