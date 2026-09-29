package com.moblin.android.integrations.emotes

import com.moblin.android.platform.log.Log
import com.moblin.android.common.various.httpGet
import com.moblin.android.localized
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val TAG = "Seventv"

private val EmotesPlatform.rawValue: String
    get() = name.lowercase()

private val json = Json {
    ignoreUnknownKeys = true
}

@Serializable
private data class SeventvFile(
    val name: String,
    @SerialName("static_name") val staticName: String? = null,
)

@Serializable
private data class SeventvHost(
    val url: String,
    val files: List<SeventvFile>,
)

@Serializable
private data class SeventvEmoteData(
    val host: SeventvHost,
)

@Serializable
private data class SeventvEmote(
    val name: String,
    val data: SeventvEmoteData,
)

@Serializable
private data class SeventvEmoteSet(
    val emotes: List<SeventvEmote>? = null,
)

@Serializable
private data class SeventvUser(
    @SerialName("emote_set") val emoteSet: SeventvEmoteSet,
)

suspend fun fetchSeventvEmotes(
    platform: EmotesPlatform,
    channelId: String,
    enabled: Boolean,
): Pair<Map<String, Emote>, String?> {
    if (!enabled) {
        return Pair(emptyMap(), null)
    }
    var message: String? = null
    var emotes: Map<String, Emote> = emptyMap()
    try {
        emotes = emotes + fetchGlobalEmotes()
    } catch (e: Exception) {
        message = localized("Failed to get 7TV global emotes")
    }
    try {
        emotes = emotes + fetchChannelEmotes(platform, channelId)
    } catch (e: Exception) {
        message = localized("Failed to get 7TV channel emotes")
    }
    return Pair(emotes, message)
}

private suspend fun fetchGlobalEmotes(): Map<String, Emote> {
    val url = "https://7tv.io/v3/emote-sets/global"
    val (data, response) = httpGet(url)
    if (!response.isSuccessful) {
        throw IllegalStateException("Not successful")
    }
    val emoteSet = json.decodeFromString<SeventvEmoteSet>(data.decodeToString())
    val emotes = emoteSet.emotes
    if (emotes == null) {
        Log.i(TAG, "emotes: 7TV global emotes missing")
        throw IllegalStateException("Emotes missing")
    }
    if (emotes.isEmpty()) {
        Log.i(TAG, "emotes: 7TV global emotes list empty")
        throw IllegalStateException("Emotes list empty")
    }
    val fetchedEmotes = mutableMapOf<String, Emote>()
    for (emote in emotes) {
        val fetchedEmote = makeEmote(emote.data)
        if (fetchedEmote == null) {
            Log.i(TAG, "emotes: Failed to create URL for 7TV emote ${emote.name}")
            continue
        }
        fetchedEmotes[emote.name] = fetchedEmote
    }
    return fetchedEmotes
}

private fun getWebpFile(files: List<SeventvFile>): SeventvFile? {
    return files.firstOrNull { it.name.endsWith(".webp") }
}

private fun makeEmote(data: SeventvEmoteData): Emote? {
    val file = getWebpFile(data.host.files) ?: return null
    val url = "https:${data.host.url}/${file.name}"
    return Emote(
        url = url,
        stillUrl = file.staticName?.let { "https:${data.host.url}/$it" },
    )
}

private suspend fun fetchChannelEmotes(
    platform: EmotesPlatform,
    channelId: String,
): Map<String, Emote> {
    if (channelId.isEmpty()) {
        return emptyMap()
    }
    val url = "https://7tv.io/v3/users/${platform.rawValue}/$channelId"
    val (data, response) = httpGet(url)
    if (response.code == 404) {
        Log.i(TAG, "emotes: ${platform.rawValue}: $channelId: 7TV channel emotes not found (HTTP 404)")
        return emptyMap()
    }
    if (!response.isSuccessful) {
        Log.i(
            TAG,
            "emotes: ${platform.rawValue}: $channelId: Failed to fetch 7TV channel emotes " +
                "(HTTP ${response.code})",
        )
        throw IllegalStateException("Not successful")
    }
    val user = json.decodeFromString<SeventvUser>(data.decodeToString())
    val emotes = user.emoteSet.emotes
    if (emotes == null) {
        Log.i(TAG, "emotes: ${platform.rawValue}: $channelId: 7TV channel emotes missing")
        throw IllegalStateException("Emotes missing")
    }
    if (emotes.isEmpty()) {
        Log.i(TAG, "emotes: ${platform.rawValue}: $channelId: 7TV channel emotes list empty")
        throw IllegalStateException("Emotes list empty")
    }
    val fetchedEmotes = mutableMapOf<String, Emote>()
    for (emote in emotes) {
        val fetchedEmote = makeEmote(emote.data)
        if (fetchedEmote == null) {
            Log.i(
                TAG,
                "emotes: ${platform.rawValue}: $channelId: Failed to create URL for 7TV emote ${emote.name}",
            )
            continue
        }
        fetchedEmotes[emote.name] = fetchedEmote
    }
    return fetchedEmotes
}
