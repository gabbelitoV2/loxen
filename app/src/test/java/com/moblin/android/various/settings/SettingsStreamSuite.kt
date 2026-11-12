package com.moblin.android.various.settings

import com.moblin.android.platform.codable.codableJson
import com.moblin.android.platform.core.uuidString
import com.moblin.android.streamingplatforms.kick.loadKickAccessTokenFromKeychain
import com.moblin.android.streamingplatforms.kick.removeKickAccessTokenInKeychain
import com.moblin.android.streamingplatforms.kick.storeKickAccessTokenInKeychain
import com.moblin.android.streamingplatforms.twitch.loadTwitchAccessTokenFromKeychain
import com.moblin.android.streamingplatforms.twitch.removeTwitchAccessTokenInKeychain
import com.moblin.android.streamingplatforms.twitch.storeTwitchAccessTokenInKeychain
import com.moblin.android.streamingplatforms.youtube.removeYouTubeAuthStateInKeychain
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.decodeFromString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun decodeStream(json: String): SettingsStream =
    codableJson.decodeFromString(SettingsStream.serializer(), json)

private fun removeFromKeychain(id: UUID) {
    removeTwitchAccessTokenInKeychain(streamId = id)
    removeKickAccessTokenInKeychain(streamId = id)
    removeYouTubeAuthStateInKeychain(streamId = id)
}

@RunWith(RobolectricTestRunner::class)
class SettingsStreamSuite {
    @Test
    fun decodingDoesNotTouchTheKeychain() {
        val id = UUID.randomUUID()
        try {
            val stream = decodeStream("""{"id":"${id.uuidString}","kickAccessToken":"kick"}""")
            assertEquals("kick", stream.kickAccessToken)
            assertNull(loadKickAccessTokenFromKeychain(streamId = id))
        } finally {
            removeFromKeychain(id)
        }
    }

    @Test
    fun loadingIgnoresPlainTextTokens() {
        val id = UUID.randomUUID()
        try {
            val stream = decodeStream(
                """{"id":"${id.uuidString}","twitchAccessToken":"twitch","kickAccessToken":"kick"}""",
            )
            stream.loadFromKeychain()
            assertEquals("", stream.twitchAccessToken)
            assertEquals("", stream.kickAccessToken)
            assertNull(loadKickAccessTokenFromKeychain(streamId = id))
        } finally {
            removeFromKeychain(id)
        }
    }

    @Test
    fun loadingTakesTheTokensFromTheKeychain() {
        val id = UUID.randomUUID()
        try {
            storeTwitchAccessTokenInKeychain(streamId = id, accessToken = "twitch")
            storeKickAccessTokenInKeychain(streamId = id, accessToken = "kick")
            val stream = decodeStream("""{"id":"${id.uuidString}"}""")
            assertEquals("", stream.twitchAccessToken)
            assertEquals("", stream.kickAccessToken)
            stream.loadFromKeychain()
            assertEquals("twitch", stream.twitchAccessToken)
            assertEquals("kick", stream.kickAccessToken)
        } finally {
            removeFromKeychain(id)
        }
    }
}
