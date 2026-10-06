package com.moblin.android.integrations.emotes

import com.moblin.android.platform.log.Log
import com.moblin.android.localized
import java.net.URI
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import com.moblin.android.common.various.isSuccessful
import com.moblin.android.various.network.httpGet

private const val TAG = "Bttv"

private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class BttvEmote(
    val id: String,
    val code: String,
)

@Serializable
private data class BttvChannel(
    val channelEmotes: List<BttvEmote>? = null,
    val sharedEmotes: List<BttvEmote>? = null,
)

suspend fun fetchBttvEmotes(
    platform: EmotesPlatform,
    channelId: String,
    enabled: Boolean,
): Pair<Map<String, Emote>, String?> {
    if (!enabled) {
        return Pair(emptyMap(), null)
    }
    var message: String? = null
    val emotes = mutableMapOf<String, Emote>()
    try {
        emotes.putAll(fetchGlobalEmotes())
    } catch (_: Exception) {
        message = localized("Failed to get BTTV emotes")
    }
    try {
        emotes.putAll(fetchChannelEmotes(platform = platform, channelId = channelId))
    } catch (_: Exception) {
        message = localized("Failed to get BTTV emotes")
    }
    return Pair(emotes, message)
}

private fun makeUrl(emote: BttvEmote): String? {
    val url = "https://cdn.betterttv.net/emote/${emote.id}/1x"
    if (runCatching { URI(url) }.isFailure) {
        Log.i(TAG, "emotes: Failed to create URL for BTTV emote ${emote.code}")
        return null
    }
    return url
}

private suspend fun fetchGlobalEmotes(): Map<String, Emote> {
    val emotes = mutableMapOf<String, Emote>()
    val url = "https://api.betterttv.net/3/cached/emotes/global"
    if (runCatching { URI(url) }.isFailure) {
        return emptyMap()
    }
    val (data, response) = httpGet(URI(url))
    if (!response.isSuccessful) {
        throw Exception("Not successful")
    }
    val decoded = json.decodeFromString(ListSerializer(BttvEmote.serializer()), String(data, Charsets.UTF_8))
    for (emote in decoded) {
        val emoteUrl = makeUrl(emote) ?: continue
        emotes[emote.code] = Emote(url = emoteUrl)
    }
    return emotes
}

private suspend fun fetchChannelEmotes(
    platform: EmotesPlatform,
    channelId: String,
): Map<String, Emote> {
    if (channelId.isEmpty()) {
        return emptyMap()
    }
    val emotes = mutableMapOf<String, Emote>()
    val url = "https://api.betterttv.net/3/cached/users/${platform.name.lowercase()}/$channelId"
    if (runCatching { URI(url) }.isFailure) {
        return emptyMap()
    }
    val (data, response) = httpGet(URI(url))
    if (response.code == 404) {
        Log.i(
            TAG,
            "emotes: ${platform.name.lowercase()}: $channelId: BTTV channel emotes not found (HTTP 404)",
        )
        return emptyMap()
    }
    if (!response.isSuccessful) {
        throw Exception(" Not successful")
    }
    val channel = json.decodeFromString(BttvChannel.serializer(), String(data, Charsets.UTF_8))
    for (emote in channel.sharedEmotes ?: emptyList()) {
        val emoteUrl = makeUrl(emote) ?: continue
        emotes[emote.code] = Emote(url = emoteUrl)
    }
    for (emote in channel.channelEmotes ?: emptyList()) {
        val emoteUrl = makeUrl(emote) ?: continue
        emotes[emote.code] = Emote(url = emoteUrl)
    }
    return emotes
}
