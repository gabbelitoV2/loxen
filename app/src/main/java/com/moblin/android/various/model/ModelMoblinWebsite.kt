package com.moblin.android.various.model

import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.storages.SimpleStringStorage
import com.moblin.android.various.utils.isMac
import java.net.URI
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

private val baseUrl: String = ""
private val liveUrl = URI("$baseUrl/streamers/live")
private val challengeUrl = URI("$baseUrl/streamers/live/challenge")
private val appAttestStorage = SimpleStringStorage("moblinWebsiteAppAttest")

private val json = Json { ignoreUnknownKeys = true }
private val mainScope = CoroutineScope(Dispatchers.Main)
private val httpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

@Serializable
private data class MoblinWebsiteChannel(
    val platform: String,
    val name: String,
)

@Serializable
private data class MoblinWebsiteLive(
    val channels: List<MoblinWebsiteChannel>,
    val challenge: String,
    val keyId: String,
    val attestation: String,
    val attestationChallenge: String,
)

@Serializable
private data class MoblinWebsiteChallenge(
    val challenge: String,
)

@Serializable
private data class MoblinWebsiteAppAttest(
    val keyId: String,
    val challenge: ByteArray,
    var attestation: ByteArray? = null,
)

private sealed class MoblinWebsiteError(message: String) : Exception(message) {
    class KeyRejected(reason: String) : MoblinWebsiteError(reason)

    class BadResponse(reason: String) : MoblinWebsiteError(reason)
}

private fun describe(response: Response, data: ByteArray): String {
    return "${response.code} ${data.decodeToString()}"
}

private fun loadAppAttest(): MoblinWebsiteAppAttest? {
    return runCatching {
        json.decodeFromString(MoblinWebsiteAppAttest.serializer(), appAttestStorage.get())
    }.getOrNull()
}

private fun storeAppAttest(appAttest: MoblinWebsiteAppAttest?) {
    if (appAttest == null) {
        appAttestStorage.set("")
        return
    }
    val data = runCatching {
        json.encodeToString(MoblinWebsiteAppAttest.serializer(), appAttest)
    }.getOrNull()
    appAttestStorage.set(data ?: "")
}

private suspend fun attestedKey(): MoblinWebsiteAppAttest {
    TODO("no Android counterpart for DeviceCheck App Attest")
}

private suspend fun generateAssertion(keyId: String, clientDataHash: ByteArray): ByteArray {
    TODO("no Android counterpart for DeviceCheck App Attest")
}

private suspend fun fetchChallenge(): String = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url(challengeUrl.toString())
        .post(ByteArray(0).toRequestBody(null))
        .build()
    httpClient.newCall(request).execute().use { response ->
        val data = response.body?.bytes() ?: ByteArray(0)
        if (!response.isSuccessful) {
            throw MoblinWebsiteError.BadResponse("challenge: ${describe(response, data)}")
        }
        json.decodeFromString(MoblinWebsiteChallenge.serializer(), data.decodeToString()).challenge
    }
}

private suspend fun postLive(channels: List<MoblinWebsiteChannel>, appAttest: MoblinWebsiteAppAttest) {
    val challenge = fetchChallenge()
    val live = MoblinWebsiteLive(
        channels = channels,
        challenge = challenge,
        keyId = appAttest.keyId,
        attestation = Base64.getEncoder().encodeToString(appAttest.attestation!!),
        attestationChallenge = Base64.getEncoder().encodeToString(appAttest.challenge),
    )
    val body = json.encodeToString(MoblinWebsiteLive.serializer(), live).toByteArray()
    val clientDataHash = MessageDigest.getInstance("SHA-256").digest(body)
    val assertion = try {
        generateAssertion(appAttest.keyId, clientDataHash)
    } catch (e: Exception) {
        throw MoblinWebsiteError.KeyRejected("assertion: $e")
    }
    withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(liveUrl.toString())
            .header("Content-Type", "application/json")
            .header("Moblin-Assertion", Base64.getEncoder().encodeToString(assertion))
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(request).execute().use { response ->
            val data = response.body?.bytes() ?: ByteArray(0)
            if (response.code == 401) {
                throw MoblinWebsiteError.KeyRejected("live: ${describe(response, data)}")
            }
            if (!response.isSuccessful) {
                throw MoblinWebsiteError.BadResponse("live: ${describe(response, data)}")
            }
        }
    }
}

private suspend fun sendLive(channels: List<MoblinWebsiteChannel>) {
    TODO("no Android counterpart for DeviceCheck App Attest")
}

fun Model.sendLiveToMoblinWebsite(onCompleted: (() -> Unit)? = null) {
    if (isMac() || !stream.value.goLiveNotificationMoblinWebsite) {
        onCompleted?.invoke()
        return
    }
    val stream = this.stream.value
    mainScope.launch {
        try {
            val channels = mutableListOf<MoblinWebsiteChannel>()
            if (stream.twitchLoggedIn) {
                val name = fetchTwitchChannelName(stream)
                if (!name.isNullOrEmpty()) {
                    channels.add(MoblinWebsiteChannel(platform = "twitch", name = name))
                }
            }
            if (stream.isYouTubeAuthorized()) {
                val name = fetchYouTubeHandle(stream)
                if (!name.isNullOrEmpty()) {
                    channels.add(MoblinWebsiteChannel(platform = "youtube", name = name))
                }
            }
            if (stream.kickLoggedIn) {
                val name = fetchKickChannelName(stream)
                if (!name.isNullOrEmpty()) {
                    channels.add(MoblinWebsiteChannel(platform = "kick", name = name))
                }
            }
            if (channels.isEmpty()) {
                return@launch
            }
            sendLive(channels)
        } finally {
            onCompleted?.invoke()
        }
    }
}

private suspend fun Model.fetchTwitchChannelName(stream: SettingsStream): String? {
    return suspendCancellableCoroutine { continuation ->
        createTwitchApi(stream).getUserInfo { info ->
            continuation.resume(info?.login?.trim())
        }
    }
}

private suspend fun Model.fetchYouTubeHandle(stream: SettingsStream): String? {
    return suspendCancellableCoroutine { continuation ->
        getYouTubeApi(stream) { youTubeApi ->
            if (youTubeApi == null) {
                continuation.resume(null)
            } else {
                youTubeApi.listChannels {
                    TODO("no Android counterpart for the YouTube channel list API")
                }
            }
        }
    }
}

private suspend fun Model.fetchKickChannelName(stream: SettingsStream): String? {
    return suspendCancellableCoroutine { continuation ->
        createKickApi(stream).getUser { user ->
            continuation.resume(user?.username?.trim())
        }
    }
}
