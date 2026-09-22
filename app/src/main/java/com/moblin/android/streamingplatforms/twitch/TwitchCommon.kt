package com.moblin.android.streamingplatforms.twitch

data class TwitchEmoteUrls(val moving: String, val still: String)

fun twitchTierAsNumber(tier: String): Int {
    return when (tier) {
        "1000" -> 1
        "2000" -> 2
        "3000" -> 3
        else -> 1
    }
}

fun makeTwitchEmoteUrls(id: String): TwitchEmoteUrls? {
    val moving = "https://static-cdn.jtvnw.net/emoticons/v2/$id/default/dark/3.0"
    val still = "https://static-cdn.jtvnw.net/emoticons/v2/$id/static/dark/3.0"
    return TwitchEmoteUrls(moving = moving, still = still)
}
