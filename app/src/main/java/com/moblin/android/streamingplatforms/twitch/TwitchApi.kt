package com.moblin.android.streamingplatforms.twitch

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.network.OperationResult
import com.moblin.android.various.network.httpRequest
import com.moblin.android.various.network.makeUrl
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private val json = Json { ignoreUnknownKeys = true }

private val mainScope = CoroutineScope(Dispatchers.Main)

private val profilePictureHttpClient = OkHttpClient.Builder()
    .callTimeout(10, TimeUnit.SECONDS)
    .build()

private fun serialize(value: Map<String, Any?>): ByteArray =
    (JSONObject.wrap(value) as JSONObject).toString().toByteArray(Charsets.UTF_8)

@Serializable
data class TwitchApiUser(
    val id: String,
    val login: String,
    val profile_image_url: String,
)

@Serializable
data class TwitchApiUsers(
    val data: List<TwitchApiUser>,
)

@Serializable
data class TwitchApiStreamKeyData(
    val stream_key: String,
)

@Serializable
data class TwitchApiStreamKey(
    val data: List<TwitchApiStreamKeyData>,
)

@Serializable
data class TwitchApiChannelPointsCustomRewardsData(
    val id: String,
    val title: String,
)

@Serializable
data class TwitchApiChannelPointsCustomRewards(
    val data: List<TwitchApiChannelPointsCustomRewardsData>,
)

@Serializable
data class TwitchApiChannelInformationData(
    val title: String,
    val game_name: String,
)

@Serializable
data class TwitchApiChannelInformation(
    val data: List<TwitchApiChannelInformationData>,
)

@Serializable
data class TwitchApiStartCommercialData(
    val length: Int,
)

@Serializable
data class TwitchApiStartCommercial(
    val data: List<TwitchApiStartCommercialData>,
)

@Serializable
class TwitchApiCreateStreamMarkerData

@Serializable
data class TwitchApiCreateStreamMarker(
    val data: List<TwitchApiCreateStreamMarkerData>,
)

@Serializable
data class TwitchApiStreamData(
    val user_id: String,
    val user_name: String,
    val game_name: String,
    val title: String,
    val viewer_count: Int,
)

@Serializable
data class TwitchApiStreams(
    val data: List<TwitchApiStreamData>,
)

@Serializable
data class TwitchApiGameData(
    val id: String,
    val name: String,
    val box_art_url: String? = null,
) {
    fun boxArtUrl(width: Int, height: Int): String? =
        box_art_url
            ?.replace("{width}", width.toString())
            ?.replace("{height}", height.toString())
}

@Serializable
data class TwitchApiGames(
    val data: List<TwitchApiGameData>,
)

@Serializable
data class TwitchApiChannel(
    val id: String,
    val broadcaster_login: String,
    val display_name: String,
    val game_name: String,
    val title: String,
    val thumbnail_url: String,
)

@Serializable
data class TwitchApiSearchChannels(
    val data: List<TwitchApiChannel>,
)

@Serializable
data class TwitchApiGetBroadcasterSubscriptionsData(
    val tier: String,
) {
    fun tierAsNumber(): Int = twitchTierAsNumber(tier)
}

@Serializable
data class TwitchApiGetBroadcasterSubscriptions(
    val data: List<TwitchApiGetBroadcasterSubscriptionsData>,
)

@Serializable
data class TwitchApiGetCheermotesDataTiersImagesThemeKind(
    @SerialName("2") val two: String,
)

@Serializable
data class TwitchApiGetCheermotesDataTiersImagesTheme(
    @SerialName("static") val static_: TwitchApiGetCheermotesDataTiersImagesThemeKind,
)

@Serializable
data class TwitchApiGetCheermotesDataTiersImages(
    val dark: TwitchApiGetCheermotesDataTiersImagesTheme,
)

@Serializable
data class TwitchApiGetCheermotesDataTier(
    val min_bits: Int,
    val images: TwitchApiGetCheermotesDataTiersImages,
)

@Serializable
data class TwitchApiGetCheermotesData(
    val prefix: String,
    val tiers: List<TwitchApiGetCheermotesDataTier>,
)

@Serializable
data class TwitchApiGetCheermotes(
    val data: List<TwitchApiGetCheermotesData>,
)

@Serializable
data class TwitchApiChatBadgesVersion(
    val id: String,
    val image_url_2x: String,
)

@Serializable
data class TwitchApiChatBadgesData(
    val set_id: String,
    val versions: List<TwitchApiChatBadgesVersion>,
)

@Serializable
data class TwitchApiChatBadges(
    val data: List<TwitchApiChatBadgesData>,
)

enum class TwitchApiPollStatus(val rawValue: String) {
    terminated("TERMINATED"),
    archived("ARCHIVED"),
    ;

    companion object {
        fun fromRawValue(rawValue: String): TwitchApiPollStatus? =
            TwitchApiPollStatus.entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
data class TwitchApiPollChoice(
    val id: String,
    val title: String,
    val votes: Int? = null,
)

@Serializable
data class TwitchApiPollData(
    val id: String,
    val title: String,
    val choices: List<TwitchApiPollChoice>,
    val status: String,
    val ends_at: String? = null,
) {
    fun isActive(): Boolean = status == "ACTIVE"
}

@Serializable
data class TwitchApiPolls(
    val data: List<TwitchApiPollData>,
)

enum class TwitchApiPredictionStatus(val rawValue: String) {
    resolved("RESOLVED"),
    canceled("CANCELED"),
    locked("LOCKED"),
    ;

    companion object {
        fun fromRawValue(rawValue: String): TwitchApiPredictionStatus? =
            TwitchApiPredictionStatus.entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
data class TwitchApiPredictionOutcome(
    val id: String,
    val title: String,
    val color: String,
    val users: Int? = null,
    val channel_points: Int? = null,
)

@Serializable
data class TwitchApiPredictionData(
    val id: String,
    val title: String,
    val outcomes: List<TwitchApiPredictionOutcome>,
    val status: String,
    val locked_at: String? = null,
) {
    fun isActive(): Boolean = status == "ACTIVE"

    fun isLocked(): Boolean = status == "LOCKED"
}

@Serializable
data class TwitchApiPredictions(
    val data: List<TwitchApiPredictionData>,
)

@Serializable
data class TwitchApiValidateTokenData(
    val expires_in: Int,
)

suspend fun fetchTwitchProfilePicture(username: String): ImageBitmap? = withContext(Dispatchers.IO) {
    val profileUrlString = runCatching {
        val request = Request.Builder()
            .url("https://decapi.me/twitch/avatar/$username")
            .build()
        profilePictureHttpClient.newCall(request).execute().use { it.body?.bytes() }
    }.getOrNull()?.decodeToString()?.trim() ?: return@withContext null
    val imageData = runCatching {
        val request = Request.Builder()
            .url(profileUrlString)
            .build()
        profilePictureHttpClient.newCall(request).execute().use { it.body?.bytes() }
    }.getOrNull() ?: return@withContext null
    BitmapFactory.decodeByteArray(imageData, 0, imageData.size)?.asImageBitmap()
}

class TwitchApi(accessToken: String) {
    private val clientId: String = twitchMoblinAppClientId
    private val accessToken: String = accessToken

    var onUnauthorized: (() -> Unit)? = null

    fun sendChatMessage(
        broadcasterId: String,
        message: String,
        onComplete: (OperationResult) -> Unit,
    ) {
        val body = mapOf(
            "broadcaster_id" to broadcasterId,
            "sender_id" to broadcasterId,
            "message" to message.take(500),
        )
        doPost(subPath = "chat/messages", body = serialize(body), onComplete = onComplete)
    }

    fun validateToken(onComplete: (TwitchApiValidateTokenData?) -> Unit) {
        doRequest(createRequest(url = "https://id.twitch.tv/oauth2/validate", method = "GET")) { result ->
            when (result) {
                is NetworkResponse.Success -> onComplete(
                    runCatching {
                        json.decodeFromString<TwitchApiValidateTokenData>(result.value.decodeToString())
                    }.getOrNull(),
                )
                else -> onComplete(null)
            }
        }
    }

    fun getUsers(onComplete: (TwitchApiUsers?) -> Unit) {
        doGet(subPath = "users") { result ->
            when (result) {
                is NetworkResponse.Success -> onComplete(
                    runCatching {
                        json.decodeFromString<TwitchApiUsers>(result.value.decodeToString())
                    }.getOrNull(),
                )
                else -> onComplete(null)
            }
        }
    }

    fun getUserInfo(onComplete: (TwitchApiUser?) -> Unit) {
        getUsers { users ->
            onComplete(users?.data?.firstOrNull())
        }
    }

    fun getUserByLogin(login: String, onComplete: (TwitchApiUser?) -> Unit) {
        doGet(subPath = makeUrl("users", listOf("login" to login))) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val users = runCatching {
                        json.decodeFromString<TwitchApiUsers>(result.value.decodeToString())
                    }.getOrNull()
                    onComplete(users?.data?.firstOrNull())
                }
                else -> onComplete(null)
            }
        }
    }

    fun getUserById(id: String, onComplete: (TwitchApiUser?) -> Unit) {
        doGet(subPath = makeUrl("users", listOf("id" to id))) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val users = runCatching {
                        json.decodeFromString<TwitchApiUsers>(result.value.decodeToString())
                    }.getOrNull()
                    onComplete(users?.data?.firstOrNull())
                }
                else -> onComplete(null)
            }
        }
    }

    fun createEventSubSubscription(body: String, onComplete: (OperationResult) -> Unit) {
        doPost(
            subPath = "eventsub/subscriptions",
            body = body.encodeToByteArray(),
            forbiddenIsAuthError = true,
            onComplete = onComplete,
        )
    }

    fun getStreamKey(broadcasterId: String, onComplete: (String?) -> Unit) {
        doGet(subPath = makeUrl("streams/key", listOf("broadcaster_id" to broadcasterId))) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val response = runCatching {
                        json.decodeFromString<TwitchApiStreamKey>(result.value.decodeToString())
                    }.getOrNull()
                    onComplete(response?.data?.firstOrNull()?.stream_key)
                }
                else -> onComplete(null)
            }
        }
    }

    fun getChannelPointsCustomRewards(
        broadcasterId: String,
        onComplete: (TwitchApiChannelPointsCustomRewards?) -> Unit,
    ) {
        doGet(
            subPath = makeUrl("channel_points/custom_rewards", listOf("broadcaster_id" to broadcasterId)),
        ) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiChannelPointsCustomRewards>(result.value.decodeToString())
                    }.getOrNull()
                    onComplete(message)
                }
                else -> onComplete(null)
            }
        }
    }

    fun getChannelInformation(
        broadcasterId: String,
        onComplete: (TwitchApiChannelInformationData?) -> Unit,
    ) {
        doGet(subPath = makeUrl("channels", listOf("broadcaster_id" to broadcasterId))) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiChannelInformation>(result.value.decodeToString())
                    }.getOrNull()
                    onComplete(message?.data?.firstOrNull())
                }
                else -> onComplete(null)
            }
        }
    }

    fun startCommercial(
        broadcasterId: String,
        length: Int,
        onComplete: (NetworkResponse<TwitchApiStartCommercialData>) -> Unit,
    ) {
        val body = mapOf(
            "broadcaster_id" to broadcasterId,
            "length" to length,
        )
        doPost(subPath = "channels/commercial", body = serialize(body)) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiStartCommercial>(result.value.decodeToString())
                    }.getOrNull()?.data?.firstOrNull()
                    if (message != null) {
                        onComplete(NetworkResponse.Success(message))
                    } else {
                        onComplete(NetworkResponse.Error)
                    }
                }
                is NetworkResponse.AuthError -> onComplete(NetworkResponse.AuthError)
                else -> onComplete(NetworkResponse.Error)
            }
        }
    }

    fun banUser(
        broadcasterId: String,
        userId: String,
        duration: Int?,
        reason: String?,
        onComplete: (OperationResult) -> Unit,
    ) {
        val data = mutableMapOf<String, Any?>("user_id" to userId)
        if (duration != null) {
            data["duration"] = duration
        }
        if (reason != null) {
            data["reason"] = reason
        }
        val subPath = makeUrl(
            "moderation/bans",
            listOf(
                "broadcaster_id" to broadcasterId,
                "moderator_id" to broadcasterId,
            ),
        )
        doPost(subPath = subPath, body = serialize(mapOf("data" to data)), onComplete = onComplete)
    }

    fun unbanUser(broadcasterId: String, userId: String, onComplete: (OperationResult) -> Unit) {
        val subPath = makeUrl(
            "moderation/bans",
            listOf(
                "broadcaster_id" to broadcasterId,
                "moderator_id" to broadcasterId,
                "user_id" to userId,
            ),
        )
        doDelete(subPath = subPath, onComplete = onComplete)
    }

    fun addModerator(
        broadcasterId: String,
        userId: String,
        onComplete: (OperationResult) -> Unit,
    ) {
        val subPath = makeUrl(
            "moderation/moderators",
            listOf(
                "broadcaster_id" to broadcasterId,
                "user_id" to userId,
            ),
        )
        doPost(subPath = subPath, body = ByteArray(0), onComplete = onComplete)
    }

    fun removeModerator(
        broadcasterId: String,
        userId: String,
        onComplete: (OperationResult) -> Unit,
    ) {
        val subPath = makeUrl(
            "moderation/moderators",
            listOf(
                "broadcaster_id" to broadcasterId,
                "user_id" to userId,
            ),
        )
        doDelete(subPath = subPath, onComplete = onComplete)
    }

    fun addVip(broadcasterId: String, userId: String, onComplete: (OperationResult) -> Unit) {
        val subPath = makeUrl("channels/vips", listOf("broadcaster_id" to broadcasterId, "user_id" to userId))
        doPost(subPath = subPath, body = ByteArray(0), onComplete = onComplete)
    }

    fun removeVip(broadcasterId: String, userId: String, onComplete: (OperationResult) -> Unit) {
        val subPath = makeUrl("channels/vips", listOf("broadcaster_id" to broadcasterId, "user_id" to userId))
        doDelete(subPath = subPath, onComplete = onComplete)
    }

    fun sendAnnouncement(
        broadcasterId: String,
        message: String,
        color: String,
        onComplete: (OperationResult) -> Unit,
    ) {
        val subPath = makeUrl(
            "chat/announcements",
            listOf(
                "broadcaster_id" to broadcasterId,
                "moderator_id" to broadcasterId,
            ),
        )
        val body = mapOf(
            "message" to message,
            "color" to color,
        )
        doPost(subPath = subPath, body = serialize(body), onComplete = onComplete)
    }

    fun updateChatSettings(
        broadcasterId: String,
        settings: Map<String, Any?>,
        onComplete: (OperationResult) -> Unit,
    ) {
        val subPath = makeUrl(
            "chat/settings",
            listOf(
                "broadcaster_id" to broadcasterId,
                "moderator_id" to broadcasterId,
            ),
        )
        doPatch(subPath = subPath, body = serialize(settings), onComplete = onComplete)
    }

    fun deleteChatMessage(broadcasterId: String, messageId: String, onComplete: (Boolean) -> Unit) {
        val subPath = makeUrl(
            "moderation/chat",
            listOf(
                "broadcaster_id" to broadcasterId,
                "moderator_id" to broadcasterId,
                "message_id" to messageId,
            ),
        )
        doDelete(subPath = subPath) { result ->
            onComplete(result.isSuccessful())
        }
    }

    fun createStreamMarker(
        userId: String,
        onComplete: (TwitchApiCreateStreamMarkerData?) -> Unit,
    ) {
        val body = mapOf(
            "user_id" to userId,
        )
        doPost(subPath = "streams/markers", body = serialize(body)) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiCreateStreamMarker>(result.value.decodeToString())
                    }.getOrNull()
                    onComplete(message?.data?.firstOrNull())
                }
                else -> onComplete(null)
            }
        }
    }

    fun getStream(userId: String, onComplete: (NetworkResponse<TwitchApiStreamData?>) -> Unit) {
        doGet(subPath = makeUrl("streams", listOf("user_id" to userId, "type" to "live"))) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiStreams>(result.value.decodeToString())
                    }.getOrNull()
                    onComplete(NetworkResponse.Success(message?.data?.firstOrNull()))
                }
                is NetworkResponse.AuthError -> onComplete(NetworkResponse.AuthError)
                else -> onComplete(NetworkResponse.Error)
            }
        }
    }

    fun getFollowedStreams(
        userId: String,
        onComplete: (NetworkResponse<List<TwitchApiStreamData>>) -> Unit,
    ) {
        doGet(subPath = makeUrl("streams/followed", listOf("user_id" to userId, "first" to "100"))) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiStreams>(result.value.decodeToString())
                    }.getOrNull()
                    if (message != null) {
                        onComplete(NetworkResponse.Success(message.data))
                    } else {
                        onComplete(NetworkResponse.Error)
                    }
                }
                is NetworkResponse.AuthError -> onComplete(NetworkResponse.AuthError)
                else -> onComplete(NetworkResponse.Error)
            }
        }
    }

    fun getStreams(userIds: List<String>, live: Boolean, onComplete: (List<TwitchApiStreamData>?) -> Unit) {
        if (userIds.isEmpty()) {
            onComplete(emptyList())
            return
        }
        val parameters = userIds.take(100).map { "user_id" to it }.toMutableList()
        if (live) {
            parameters.add("type" to "live")
        }
        doGet(subPath = makeUrl("streams", parameters)) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiStreams>(result.value.decodeToString())
                    }.getOrNull()
                    onComplete(message?.data)
                }
                else -> onComplete(null)
            }
        }
    }

    fun getUsersByIds(ids: List<String>, onComplete: (List<TwitchApiUser>?) -> Unit) {
        if (ids.isEmpty()) {
            onComplete(emptyList())
            return
        }
        doGet(subPath = makeUrl("users", ids.take(100).map { "id" to it })) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiUsers>(result.value.decodeToString())
                    }.getOrNull()
                    onComplete(message?.data)
                }
                else -> onComplete(null)
            }
        }
    }

    fun getGames(names: List<String>, onComplete: (List<TwitchApiGameData>?) -> Unit) {
        doGet(subPath = makeUrl("games", names.map { "name" to it })) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiGames>(result.value.decodeToString())
                    }.getOrNull()
                    onComplete(message?.data)
                }
                else -> onComplete(null)
            }
        }
    }

    fun startRaid(
        broadcasterId: String,
        toBroadcasterId: String,
        onComplete: (OperationResult) -> Unit,
    ) {
        val subPath = makeUrl(
            "raids",
            listOf("from_broadcaster_id" to broadcasterId, "to_broadcaster_id" to toBroadcasterId),
        )
        doPost(subPath = subPath, body = ByteArray(0), onComplete = onComplete)
    }

    fun cancelRaid(
        broadcasterId: String,
        onComplete: (OperationResult) -> Unit,
    ) {
        doDelete(subPath = makeUrl("raids", listOf("broadcaster_id" to broadcasterId))) { result ->
            onComplete(result)
        }
    }

    fun sendShoutout(
        broadcasterId: String,
        toBroadcasterId: String,
        onComplete: (OperationResult) -> Unit,
    ) {
        val subPath = makeUrl(
            "chat/shoutouts",
            listOf(
                "from_broadcaster_id" to broadcasterId,
                "to_broadcaster_id" to toBroadcasterId,
                "moderator_id" to broadcasterId,
            ),
        )
        doPost(subPath = subPath, body = ByteArray(0), onComplete = onComplete)
    }

    fun searchCategories(query: String, onComplete: (List<TwitchApiGameData>?) -> Unit) {
        doGet(subPath = makeUrl("search/categories", listOf("query" to query, "first" to "10"))) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiGames>(result.value.decodeToString())
                    }.getOrNull()
                    onComplete(message?.data)
                }
                else -> onComplete(null)
            }
        }
    }

    fun searchChannel(channelName: String, onComplete: (TwitchApiChannel?) -> Unit) {
        searchChannels(filter = channelName, liveOnly = false) { result ->
            when (result) {
                is NetworkResponse.Success -> onComplete(
                    result.value.firstOrNull {
                        it.broadcaster_login.lowercase() == channelName.lowercase()
                    },
                )
                else -> onComplete(null)
            }
        }
    }

    fun searchChannels(
        filter: String,
        liveOnly: Boolean,
        onComplete: (NetworkResponse<List<TwitchApiChannel>>) -> Unit,
    ) {
        val parameters = mutableListOf("query" to filter)
        if (liveOnly) {
            parameters.add("live_only" to "true")
        }
        doGet(subPath = makeUrl("search/channels", parameters)) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiSearchChannels>(result.value.decodeToString())
                    }.getOrNull()
                    if (message != null) {
                        onComplete(NetworkResponse.Success(message.data))
                    } else {
                        onComplete(NetworkResponse.Error)
                    }
                }
                is NetworkResponse.AuthError -> onComplete(NetworkResponse.AuthError)
                else -> onComplete(NetworkResponse.Error)
            }
        }
    }

    fun modifyChannelInformation(
        broadcasterId: String,
        categoryId: String?,
        title: String?,
        onComplete: (Boolean) -> Unit,
    ) {
        val body = mutableMapOf<String, String>()
        if (categoryId != null) {
            body["game_id"] = categoryId
        }
        if (title != null) {
            body["title"] = title
        }
        doPatch(
            subPath = makeUrl("channels", listOf("broadcaster_id" to broadcasterId)),
            body = serialize(body),
        ) { result ->
            onComplete(result.isSuccessful())
        }
    }

    fun getGlobalChatBadges(onComplete: (List<TwitchApiChatBadgesData>?) -> Unit) {
        doGet(subPath = "chat/badges/global") { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiChatBadges>(result.value.decodeToString())
                    }.getOrNull()
                    onComplete(message?.data)
                }
                else -> onComplete(null)
            }
        }
    }

    fun getChannelChatBadges(
        broadcasterId: String,
        onComplete: (List<TwitchApiChatBadgesData>?) -> Unit,
    ) {
        doGet(subPath = makeUrl("chat/badges", listOf("broadcaster_id" to broadcasterId))) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiChatBadges>(result.value.decodeToString())
                    }.getOrNull()
                    onComplete(message?.data)
                }
                else -> onComplete(null)
            }
        }
    }

    fun getBroadcasterSubscriptions(
        broadcasterId: String,
        userId: String,
        onComplete: (TwitchApiGetBroadcasterSubscriptionsData?) -> Unit,
    ) {
        doGet(subPath = makeUrl("subscriptions", listOf("broadcaster_id" to broadcasterId, "user_id" to userId))) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiGetBroadcasterSubscriptions>(result.value.decodeToString())
                    }.getOrNull()
                    onComplete(message?.data?.firstOrNull())
                }
                else -> onComplete(null)
            }
        }
    }

    fun getCheermotes(
        broadcasterId: String,
        onComplete: (List<TwitchApiGetCheermotesData>?) -> Unit,
    ) {
        doGet(subPath = makeUrl("bits/cheermotes", listOf("broadcaster_id" to broadcasterId))) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiGetCheermotes>(result.value.decodeToString())
                    }.getOrNull()
                    onComplete(message?.data)
                }
                else -> onComplete(null)
            }
        }
    }

    fun getPolls(
        broadcasterId: String,
        onComplete: (NetworkResponse<List<TwitchApiPollData>>) -> Unit,
    ) {
        doGet(subPath = makeUrl("polls", listOf("broadcaster_id" to broadcasterId))) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiPolls>(result.value.decodeToString())
                    }.getOrNull()
                    if (message != null) {
                        onComplete(NetworkResponse.Success(message.data))
                    } else {
                        onComplete(NetworkResponse.Error)
                    }
                }
                is NetworkResponse.AuthError -> onComplete(NetworkResponse.AuthError)
                else -> onComplete(NetworkResponse.Error)
            }
        }
    }

    fun createPoll(
        broadcasterId: String,
        title: String,
        choices: List<String>,
        duration: Int,
        onComplete: (OperationResult) -> Unit,
    ) {
        val body: Map<String, Any?> = mapOf(
            "broadcaster_id" to broadcasterId,
            "title" to title,
            "choices" to choices.map { mapOf("title" to it) },
            "duration" to duration,
        )
        doPost(subPath = "polls", body = serialize(body), onComplete = onComplete)
    }

    fun endPoll(
        broadcasterId: String,
        id: String,
        status: TwitchApiPollStatus,
        onComplete: (OperationResult) -> Unit,
    ) {
        val body: Map<String, Any?> = mapOf(
            "broadcaster_id" to broadcasterId,
            "id" to id,
            "status" to status.rawValue,
        )
        doPatch(subPath = "polls", body = serialize(body), onComplete = onComplete)
    }

    fun getPredictions(
        broadcasterId: String,
        onComplete: (NetworkResponse<List<TwitchApiPredictionData>>) -> Unit,
    ) {
        doGet(subPath = makeUrl("predictions", listOf("broadcaster_id" to broadcasterId))) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val message = runCatching {
                        json.decodeFromString<TwitchApiPredictions>(result.value.decodeToString())
                    }.getOrNull()
                    if (message != null) {
                        onComplete(NetworkResponse.Success(message.data))
                    } else {
                        onComplete(NetworkResponse.Error)
                    }
                }
                is NetworkResponse.AuthError -> onComplete(NetworkResponse.AuthError)
                else -> onComplete(NetworkResponse.Error)
            }
        }
    }

    fun createPrediction(
        broadcasterId: String,
        title: String,
        outcomes: List<String>,
        predictionWindow: Int,
        onComplete: (OperationResult) -> Unit,
    ) {
        val body: Map<String, Any?> = mapOf(
            "broadcaster_id" to broadcasterId,
            "title" to title,
            "outcomes" to outcomes.map { mapOf("title" to it) },
            "prediction_window" to predictionWindow,
        )
        doPost(subPath = "predictions", body = serialize(body), onComplete = onComplete)
    }

    fun endPrediction(
        broadcasterId: String,
        id: String,
        status: TwitchApiPredictionStatus,
        winningOutcomeId: String?,
        onComplete: (OperationResult) -> Unit,
    ) {
        val body = mutableMapOf<String, Any?>(
            "broadcaster_id" to broadcasterId,
            "id" to id,
            "status" to status.rawValue,
        )
        if (winningOutcomeId != null) {
            body["winning_outcome_id"] = winningOutcomeId
        }
        doPatch(subPath = "predictions", body = serialize(body), onComplete = onComplete)
    }

    private fun doGet(subPath: String, onComplete: (OperationResult) -> Unit) {
        doRequest(createRequest(url = makeHelixUrl(subPath), method = "GET"), onComplete = onComplete)
    }

    private fun doPost(
        subPath: String,
        body: ByteArray,
        forbiddenIsAuthError: Boolean = false,
        onComplete: (OperationResult) -> Unit,
    ) {
        val request = createRequest(url = makeHelixUrl(subPath), method = "POST", json = true, body = body)
        doRequest(request, forbiddenIsAuthError = forbiddenIsAuthError, onComplete = onComplete)
    }

    private fun doPatch(subPath: String, body: ByteArray, onComplete: (OperationResult) -> Unit) {
        val request = createRequest(url = makeHelixUrl(subPath), method = "PATCH", json = true, body = body)
        doRequest(request, onComplete = onComplete)
    }

    private fun doDelete(subPath: String, onComplete: (OperationResult) -> Unit) {
        doRequest(createRequest(url = makeHelixUrl(subPath), method = "DELETE"), onComplete = onComplete)
    }

    private fun makeHelixUrl(subPath: String): String = "https://api.twitch.tv/helix/$subPath"

    private fun doRequest(
        request: Request,
        forbiddenIsAuthError: Boolean = false,
        onComplete: (OperationResult) -> Unit,
    ) {
        httpRequest(request) { data, response, error ->
            mainScope.launch {
                if (error != null || data == null || response?.isSuccessful != true) {
                    if (data != null) {
                        Log.i("TwitchApi", "twitch-api: Error response body: ${data.decodeToString()}")
                    }
                    val isForbidden = forbiddenIsAuthError && response?.code == 403
                    if (response?.code == 401 || isForbidden) {
                        onUnauthorized?.invoke()
                        onComplete(NetworkResponse.AuthError)
                    } else {
                        onComplete(NetworkResponse.Error)
                    }
                    return@launch
                }
                onComplete(NetworkResponse.Success(data))
            }
        }
    }

    private fun createRequest(
        url: String,
        method: String,
        json: Boolean = false,
        body: ByteArray? = null,
    ): Request {
        val builder = Request.Builder()
            .url(url)
            .header("client-id", clientId)
            .header("Authorization", "Bearer $accessToken")
        if (json) {
            builder.header("Content-Type", "application/json")
        }
        val requestBody = body?.toRequestBody(if (json) "application/json".toMediaType() else null)
        return builder.method(method, requestBody).build()
    }
}
