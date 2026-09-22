package com.moblin.android.integrations.emotes

import android.util.Log
import com.moblin.android.common.various.httpGet
import com.moblin.android.localized
import com.moblin.android.various.network.NetworkResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI

private const val TAG = "Ffz"

@Serializable
private data class FfzImages(
    @SerialName("1x") var onex: String? = null,
    @SerialName("2x") var twox: String? = null,
    @SerialName("4x") var fourx: String? = null,
)

@Serializable
private data class FfzEmote(
    var code: String,
    var images: FfzImages,
)

suspend fun fetchFfzEmotes(
    platform: EmotesPlatform,
    channelId: String,
    enabled: Boolean,
): Pair<Map<String, Emote>, String?> {
    if (!enabled) {
        return emptyMap<String, Emote>() to null
    }
    var message: String? = null
    var emotes: Map<String, Emote> = emptyMap()
    try {
        emotes = emotes + fetchGlobalEmotes(platform)
    } catch (e: Exception) {
        message = localized("Failed to get FFZ emotes")
    }
    try {
        emotes = emotes + fetchChannelEmotes(platform, channelId)
    } catch (e: Exception) {
        message = localized("Failed to get FFZ emotes")
    }
    return emotes to message
}

private fun makeUrl(emote: FfzEmote): String? {
    val url = emote.images.onex ?: emote.images.twox ?: emote.images.fourx ?: return null
    return if (runCatching { URI(url) }.isSuccess) url else null
}

private suspend fun fetchGlobalEmotes(platform: EmotesPlatform): Map<String, Emote> {
    return fetchEmotes(
        url = "https://api.betterttv.net/3/cached/frankerfacez/emotes/global",
        platform = platform,
    )
}

private suspend fun fetchChannelEmotes(
    platform: EmotesPlatform,
    channelId: String,
): Map<String, Emote> {
    if (channelId.isEmpty()) {
        return emptyMap()
    }
    return fetchEmotes(
        url = "https://api.betterttv.net/3/cached/frankerfacez/users/$platform/$channelId",
        platform = platform,
    )
}

private suspend fun fetchEmotes(
    url: String,
    platform: EmotesPlatform,
): Map<String, Emote> {
    val emotes = mutableMapOf<String, Emote>()
    if (runCatching { URI(url) }.isFailure) {
        return emptyMap()
    }
    val (data, response) = httpGet(url)
    if (response.isNotFound) {
        Log.i(TAG, "emotes: $platform: FFZ emotes not found (HTTP 404)")
        return emptyMap()
    }
    if (!response.isSuccessful) {
        throw Exception(" Not successful")
    }
    for (emote in Json.decodeFromString<List<FfzEmote>>(data.decodeToString())) {
        val emoteUrl = makeUrl(emote)
        if (emoteUrl == null) {
            Log.i(TAG, "emotes: $platform: Failed to create URL for FFZ emote ${emote.code}")
            continue
        }
        emotes[emote.code] = Emote(url = emoteUrl)
    }
    return emotes
}
