package com.moblin.android.streamingplatforms.youtube

import com.moblin.android.various.Keychain
import java.util.UUID

val youTubeIssuer = "https://accounts.google.com"
val youTubeClientId = "863735387781-fsq255rst10hksrga8hr23dtlmlth7ti.apps.googleusercontent.com"
val youTubeRedirectUri =
    "com.googleusercontent.apps.863735387781-fsq255rst10hksrga8hr23dtlmlth7ti:/"
val youTubeScopes = listOf(
    "https://www.googleapis.com/auth/youtube",
)
private val youTubeAuthServer = "www.youtube.com"

fun storeYouTubeAuthStateInKeychain(streamId: UUID, authState: String) {
    createKeychain(streamId.toString()).store(authState)
}

fun loadYouTubeAuthStateFromKeychain(streamId: UUID): String? {
    return createKeychain(streamId.toString()).load()
}

fun removeYouTubeAuthStateInKeychain(streamId: UUID) {
    createKeychain(streamId.toString()).remove()
}

fun removeUnusedYouTubeAuthStatesInKeychain(usedStreamIds: List<UUID>) {
    val used = usedStreamIds.map { it.toString() }.toSet()
    for (streamId in Keychain.loadStreamIds(youTubeAuthServer)) {
        if (used.contains(streamId)) {
            continue
        }
        createKeychain(streamId).remove()
    }
}

private fun createKeychain(streamId: String): Keychain {
    return Keychain(streamId, youTubeAuthServer, "youtube: auth")
}
