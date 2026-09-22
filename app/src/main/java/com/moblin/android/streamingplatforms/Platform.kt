package com.moblin.android.streamingplatforms

import com.moblin.android.localized

enum class Platform(val rawValue: String) {
    soop("soop"),
    kick("kick"),
    openStreamingPlatform("openStreamingPlatform"),
    twitch("twitch"),
    youTube("youTube");

    fun displayName(): String {
        return when (this) {
            soop -> localized("SOOP")
            kick -> localized("Kick")
            openStreamingPlatform -> localized("Open Streaming Platform")
            twitch -> localized("Twitch")
            youTube -> localized("YouTube")
        }
    }

    fun imageName(): String {
        return when (this) {
            soop -> "SoopLogo"
            kick -> "KickLogo"
            openStreamingPlatform -> "OpenStreamingPlatform"
            twitch -> "TwitchLogo"
            youTube -> "YouTubeLogo"
        }
    }

    companion object {
        fun fromRawValue(rawValue: String): Platform? =
            Platform.entries.firstOrNull { it.rawValue == rawValue }

        fun allCases(): List<Platform> = Platform.entries.toList()
    }
}
