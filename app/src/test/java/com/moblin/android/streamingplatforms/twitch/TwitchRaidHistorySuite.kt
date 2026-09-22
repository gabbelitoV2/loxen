package com.moblin.android.streamingplatforms.twitch

import com.moblin.android.various.settings.SettingsStreamTwitchRaidChannel
import com.moblin.android.various.settings.appendTwitchRaidChannel
import com.moblin.android.various.settings.maximumNumberOfTwitchRaidChannels
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class TwitchRaidHistorySuite {
    @Test
    fun appendNewestFirst() {
        var channels: List<SettingsStreamTwitchRaidChannel> = emptyList()
        channels = appendTwitchRaidChannel(channels, channelId = "1", channelName = "One")
        channels = appendTwitchRaidChannel(channels, channelId = "2", channelName = "Two")
        assertEquals(listOf("2", "1"), channels.map { it.channelId })
        assertEquals(listOf("Two", "One"), channels.map { it.channelName })
    }

    @Test
    fun appendExistingMovesToFront() {
        var channels: List<SettingsStreamTwitchRaidChannel> = emptyList()
        channels = appendTwitchRaidChannel(channels, channelId = "1", channelName = "One")
        channels = appendTwitchRaidChannel(channels, channelId = "2", channelName = "Two")
        channels = appendTwitchRaidChannel(channels, channelId = "1", channelName = "New name")
        assertEquals(2, channels.size)
        assertEquals(listOf("1", "2"), channels.map { it.channelId })
        assertEquals("New name", channels[0].channelName)
    }

    @Test
    fun appendDropsOldest() {
        var channels: List<SettingsStreamTwitchRaidChannel> = emptyList()
        for (index in 0 until maximumNumberOfTwitchRaidChannels + 5) {
            channels = appendTwitchRaidChannel(channels, channelId = "$index", channelName = "$index")
        }
        assertEquals(maximumNumberOfTwitchRaidChannels, channels.size)
        assertEquals("${maximumNumberOfTwitchRaidChannels + 4}", channels.firstOrNull()?.channelId)
        assertEquals("5", channels.lastOrNull()?.channelId)
    }
}
