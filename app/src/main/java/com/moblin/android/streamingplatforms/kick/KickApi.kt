package com.moblin.android.streamingplatforms.kick

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.moblin.android.common.various.httpGet
import com.moblin.android.view.controlbar.quickbutton.chat.ChatterInfo
import com.moblin.android.view.controlbar.quickbutton.chat.ChatterRole
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.network.OperationResult
import com.moblin.android.various.network.httpRequest
import com.moblin.android.various.network.makeUrl
import java.net.URI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Serializable
data class BadgeImage(
    val src: String,
)

@Serializable
data class SubscriberBadge(
    val months: Int,
    val badge_image: BadgeImage,
)

@Serializable
data class KickLivestream(
    val viewers: Int,
)

@Serializable
data class KickChatroom(
    val id: Int,
    val channel_id: Int,
    val created_at: String,
)

@Serializable
data class KickChannelUser(
    val profile_pic: String?,
    val bio: String?,
)

@Serializable
data class KickChannel(
    val slug: String,
    val chatroom: KickChatroom,
    val livestream: KickLivestream?,
    val subscriber_badges: List<SubscriberBadge>?,
    val user: KickChannelUser?,
    val followersCount: Int?,
)

@Serializable
data class KickChatterInfoBadge(
    val type: String,
    val count: Int?,
)

@Serializable
data class KickChatterInfo(
    val profile_pic: String?,
    val is_staff: Boolean,
    val is_channel_owner: Boolean,
    val is_moderator: Boolean,
    val badges: List<KickChatterInfoBadge>,
    val following_since: String?,
    val subscribed_for: Int,
) {
    fun toChatterInfo(
        accountCreated: String?,
        bio: String?,
        followers: Int?,
    ): ChatterInfo {
        val role: ChatterRole = when {
            is_channel_owner -> ChatterRole.Owner
            is_staff -> ChatterRole.Staff
            is_moderator -> ChatterRole.Moderator
            else -> ChatterRole.Viewer
        }
        val giftedSubs = badges.firstOrNull { it.type == "sub_gifter" }?.count
        return ChatterInfo(
            profilePicture = profile_pic,
            bio = bio,
            accountCreated = accountCreated,
            role = role,
            followingSince = following_since,
            subscribedMonths = subscribed_for,
            giftedSubs = giftedSubs,
            followers = followers,
        )
    }
}

@Serializable
data class KickUser(
    val username: String,
)

@Serializable
data class KickCategory(
    val id: String,
    val name: String,
    val src: String?,
)

@Serializable
data class KickFollowedChannel(
    val is_live: Boolean,
    val profile_picture: String?,
    val channel_slug: String,
    val viewer_count: Int?,
    val category_name: String?,
    val user_username: String,
    val session_title: String?,
) {
    val id: String
        get() = channel_slug
}

@Serializable
data class KickFollowedChannelsResponse(
    val channels: List<KickFollowedChannel>,
    val nextCursor: Int?,
)

@Serializable
data class KickHostChannelResponse(
    val success: Boolean,
)

@Serializable
data class KickLiveSearchChannel(
    val id: Int,
    val username: String,
    val viewers_count: Int,
    val is_live: Boolean,
    val profile_pic: String?,
    val category: String?,
)

@Serializable
data class KickLiveSearchData(
    val channels: List<KickLiveSearchChannel>,
)

@Serializable
data class KickLiveSearchResponse(
    val data: KickLiveSearchData,
)

@Serializable
data class KickCategorySearchHit(
    val document: KickCategory,
)

@Serializable
data class KickCategorySearchResponse(
    val hits: List<KickCategorySearchHit>,
)

data class KickStreamInfo(
    val title: String,
    val categoryName: String?,
)

private val json = Json { ignoreUnknownKeys = true }

private val userUrl = "https://kick.com/api/v1/user"

private fun makeSlug(channelName: String): String {
    return channelName.replace("_", "-")
}

suspend fun getKickChannelInfo(channelName: String): KickChannel {
    return try {
        getKickChannelInfoInner(channelName)
    } catch (e: Exception) {
        getKickChannelInfoInner(makeSlug(channelName))
    }
}

fun getKickChannelInfo(channelName: String, onComplete: (KickChannel?) -> Unit) {
    getKickChannelInfoInner(channelName) {
        val info = it
        if (info != null) {
            onComplete(info)
        } else {
            getKickChannelInfoInner(makeSlug(channelName)) {
                onComplete(it)
            }
        }
    }
}

private suspend fun getKickChannelInfoInner(slug: String): KickChannel {
    val url = runCatching { URI("https://kick.com/api/v1/channels/$slug") }.getOrNull()
        ?: throw IllegalStateException("Invalid URL")
    val pair = httpGet(url.toString())
    val data = pair.first
    val response = pair.second
    if (!response.isSuccessful) {
        throw IllegalStateException("Not successful")
    }
    return json.decodeFromString(KickChannel.serializer(), data.decodeToString())
}

private fun getKickChannelInfoInner(slug: String, onComplete: (KickChannel?) -> Unit) {
    val url = runCatching { URI("https://kick.com/api/v1/channels/$slug") }.getOrNull()
    if (url == null) {
        onComplete(null)
        return
    }
    val request = Request.Builder().url(url.toString()).build()
    httpRequest(request) { data, response, error ->
        if (error != null || data == null || response?.isSuccessful != true) {
            onComplete(null)
            return@httpRequest
        }
        onComplete(
            runCatching {
                json.decodeFromString(KickChannel.serializer(), data.decodeToString())
            }.getOrNull(),
        )
    }
}

suspend fun fetchKickProfilePicture(username: String): Bitmap? {
    fetchKickProfilePictureWithUsername(username)?.let { return it }
    if (username.contains("_")) {
        val kebabUsername = username.replace("_", "-")
        return fetchKickProfilePictureWithUsername(kebabUsername)
    }
    return null
}

private suspend fun fetchKickProfilePictureWithUsername(username: String): Bitmap? {
    val channelInfo = runCatching { getKickChannelInfo(username) }.getOrNull() ?: return null
    val profilePic = channelInfo.user?.profile_pic ?: return null
    val imageUrl = runCatching { URI(profilePic) }.getOrNull() ?: return null
    val pair = runCatching { httpGet(imageUrl.toString()) }.getOrNull() ?: return null
    val data = pair.first
    val response = pair.second
    if (!response.isSuccessful) {
        return null
    }
    return BitmapFactory.decodeByteArray(data, 0, data.size)
}

interface KickApiDelegate {
    fun kickApiUnauthorized()
}

class KickApi(
    private val channelId: String,
    private val slug: String,
    private val accessToken: String,
) {
    var delegate: KickApiDelegate? = null

    fun sendMessage(message: String) {
        doV2Request(
            method = "POST",
            subPath = "messages/send/$channelId",
            body = mapOf("type" to "message", "content" to message),
        ) { }
    }

    fun deleteMessage(messageId: String) {
        doV2Request(
            method = "DELETE",
            subPath = "chatrooms/$channelId/messages/$messageId",
        ) { }
    }

    fun banUser(
        user: String,
        duration: Int? = null,
        reason: String? = null,
        onComplete: (OperationResult) -> Unit,
    ) {
        val body: MutableMap<String, Any> = mutableMapOf(
            "banned_username" to user,
            "permanent" to (duration == null),
        )
        if (duration != null) {
            body["duration"] = duration / 60
        }
        if (reason != null) {
            body["reason"] = reason
        }
        doV2Request(
            method = "POST",
            subPath = "channels/$slug/bans",
            body = body,
            onComplete = onComplete,
        )
    }

    fun unbanUser(user: String, onComplete: (OperationResult) -> Unit) {
        doV2Request(
            method = "DELETE",
            subPath = "channels/$slug/bans/$user",
            onComplete = onComplete,
        )
    }

    fun addModerator(user: String, onComplete: (OperationResult) -> Unit) {
        doInternalV1Request(
            method = "POST",
            subPath = "channels/$slug/community/moderators",
            body = mapOf("username" to user),
            onComplete = onComplete,
        )
    }

    fun removeModerator(user: String, onComplete: (OperationResult) -> Unit) {
        doInternalV1Request(
            method = "DELETE",
            subPath = "channels/$slug/community/moderators/$user",
            onComplete = onComplete,
        )
    }

    fun addVip(user: String, onComplete: (OperationResult) -> Unit) {
        doInternalV1Request(
            method = "POST",
            subPath = "channels/$slug/community/vips",
            body = mapOf("username" to user),
            onComplete = onComplete,
        )
    }

    fun removeVip(user: String, onComplete: (OperationResult) -> Unit) {
        doInternalV1Request(
            method = "DELETE",
            subPath = "channels/$slug/community/vips/$user",
            onComplete = onComplete,
        )
    }

    fun hostChannel(channel: String, onComplete: (OperationResult) -> Unit) {
        doV2Request(
            method = "POST",
            subPath = "channels/$slug/chat-commands",
            body = mapOf("command" to "host", "parameter" to channel),
        ) { result ->
            when (result) {
                is OperationResult.Success -> {
                    val response = runCatching {
                        json.decodeFromString(
                            KickHostChannelResponse.serializer(),
                            result.data.decodeToString(),
                        )
                    }.getOrNull()
                    if (response != null) {
                        onComplete(
                            if (response.success) {
                                OperationResult.Success(result.data)
                            } else {
                                OperationResult.Error
                            },
                        )
                    } else {
                        onComplete(OperationResult.Error)
                    }
                }
                OperationResult.AuthError -> onComplete(OperationResult.AuthError)
                OperationResult.Error -> onComplete(OperationResult.Error)
            }
        }
    }

    fun enableSlowMode(messageInterval: Int, onComplete: (OperationResult) -> Unit) {
        doV2Request(
            method = "PUT",
            subPath = "channels/$slug/chatroom",
            body = mapOf("slow_mode" to true, "message_interval" to messageInterval),
            onComplete = onComplete,
        )
    }

    fun disableSlowMode(onComplete: (OperationResult) -> Unit) {
        doV2Request(
            method = "PUT",
            subPath = "channels/$slug/chatroom",
            body = mapOf("slow_mode" to false),
            onComplete = onComplete,
        )
    }

    fun enableFollowersMode(minimumDuration: Int, onComplete: (OperationResult) -> Unit) {
        doV2Request(
            method = "PUT",
            subPath = "channels/$slug/chatroom",
            body = mapOf(
                "followers_mode" to true,
                "following_min_duration" to minimumDuration,
            ),
            onComplete = onComplete,
        )
    }

    fun disableFollowersMode(onComplete: (OperationResult) -> Unit) {
        doV2Request(
            method = "PUT",
            subPath = "channels/$slug/chatroom",
            body = mapOf("followers_mode" to false),
            onComplete = onComplete,
        )
    }

    fun setEmoteOnlyMode(enabled: Boolean, onComplete: (OperationResult) -> Unit) {
        doV2Request(
            method = "PUT",
            subPath = "channels/$slug/chatroom",
            body = mapOf("emotes_mode" to enabled),
            onComplete = onComplete,
        )
    }

    fun setShowViewCount(
        channelId: String,
        enabled: Boolean,
        onComplete: (OperationResult) -> Unit,
    ) {
        doWebV1Request(
            method = "PATCH",
            subPath = "channels/$channelId/settings",
            body = mapOf("show_view_count" to enabled),
            onComplete = onComplete,
        )
    }

    fun setSubscribersOnlyMode(enabled: Boolean, onComplete: (OperationResult) -> Unit) {
        doV2Request(
            method = "PUT",
            subPath = "channels/$slug/chatroom",
            body = mapOf("subscribers_mode" to enabled),
            onComplete = onComplete,
        )
    }

    fun createPoll(
        title: String,
        options: List<String>,
        duration: Int,
        resultDisplayDuration: Int,
        onComplete: (OperationResult) -> Unit,
    ) {
        val body: Map<String, Any> = mapOf(
            "title" to title,
            "options" to options,
            "duration" to duration,
            "result_display_duration" to resultDisplayDuration,
        )
        doV2Request(
            method = "POST",
            subPath = "channels/$slug/polls",
            body = body,
            onComplete = onComplete,
        )
    }

    fun deletePoll(onComplete: (OperationResult) -> Unit) {
        doV2Request(
            method = "DELETE",
            subPath = "channels/$slug/polls",
            onComplete = onComplete,
        )
    }

    fun createPrediction(
        title: String,
        outcomes: List<String>,
        duration: Int,
        onComplete: (OperationResult) -> Unit,
    ) {
        val body: Map<String, Any> = mapOf(
            "title" to title,
            "outcomes" to outcomes,
            "duration" to duration,
        )
        doV2Request(
            method = "POST",
            subPath = "channels/$slug/predictions",
            body = body,
            onComplete = onComplete,
        )
    }

    fun getStreamInfo(onComplete: (NetworkResponse<KickStreamInfo>) -> Unit) {
        doV2Request(method = "GET", subPath = "channels/$slug/stream-info") { result ->
            when (result) {
                is OperationResult.Success -> {
                    val jsonObject = runCatching {
                        json.parseToJsonElement(result.data.decodeToString()).jsonObject
                    }.getOrNull()
                    val title = (jsonObject?.get("stream_title") as? JsonPrimitive)?.contentOrNull
                    if (title == null) {
                        onComplete(NetworkResponse.Error)
                        return@doV2Request
                    }
                    val categoryName = (
                        jsonObject.get("category")?.jsonObject?.get("name") as? JsonPrimitive
                        )?.contentOrNull
                    onComplete(
                        NetworkResponse.Success(
                            KickStreamInfo(title = title, categoryName = categoryName),
                        ),
                    )
                }
                OperationResult.AuthError -> onComplete(NetworkResponse.AuthError)
                OperationResult.Error -> onComplete(NetworkResponse.Error)
            }
        }
    }

    fun setStreamTitle(title: String, onComplete: (OperationResult) -> Unit) {
        doV2Request(
            method = "PATCH",
            subPath = "channels/$slug/stream-info",
            body = mapOf("stream_title" to title),
            onComplete = onComplete,
        )
    }

    fun setStreamCategory(categoryId: Int, onComplete: (OperationResult) -> Unit) {
        doV2Request(
            method = "PATCH",
            subPath = "channels/$slug/stream-info",
            body = mapOf("category_id" to categoryId),
            onComplete = onComplete,
        )
    }

    fun searchCategories(query: String, onComplete: (List<KickCategory>?) -> Unit) {
        val subPath = makeUrl(
            "collections/subcategory_index/documents/search",
            listOf("q" to query, "query_by" to "name"),
        )
        val request = Request.Builder()
            .url("https://search.kick.com/$subPath")
            .header("Accept", "application/json, text/plain, */*")
            .header("X-Typesense-Api-Key", "nXIMW0iEN6sMujFYjFuhdrSwVow3pDQu")
            .build()
        httpRequest(request) { data, response, error ->
            if (error != null || data == null || response?.isSuccessful != true) {
                onComplete(null)
                return@httpRequest
            }
            val searchResponse = runCatching {
                json.decodeFromString(
                    KickCategorySearchResponse.serializer(),
                    data.decodeToString(),
                )
            }.getOrNull()
            if (searchResponse == null) {
                onComplete(null)
                return@httpRequest
            }
            onComplete(searchResponse.hits.map { it.document })
        }
    }

    fun searchLiveChannels(query: String, onComplete: (List<KickLiveSearchChannel>?) -> Unit) {
        val subPath = makeUrl("live/search", listOf("q" to query))
        doInternalV1Request(method = "GET", subPath = subPath) { result ->
            when (result) {
                is OperationResult.Success -> {
                    val response = runCatching {
                        json.decodeFromString(
                            KickLiveSearchResponse.serializer(),
                            result.data.decodeToString(),
                        )
                    }.getOrNull()
                    onComplete(response?.data?.channels)
                }
                else -> onComplete(null)
            }
        }
    }

    fun getFollowedChannels(
        cursor: Int? = null,
        onComplete: (KickFollowedChannelsResponse?) -> Unit,
    ) {
        val parameters: MutableList<Pair<String, String>> = mutableListOf()
        if (cursor != null) {
            parameters.add("cursor" to cursor.toString())
        }
        val subPath = makeUrl("channels/followed", parameters)
        doV2Request(method = "GET", subPath = subPath) { result ->
            when (result) {
                is OperationResult.Success -> {
                    onComplete(
                        runCatching {
                            json.decodeFromString(
                                KickFollowedChannelsResponse.serializer(),
                                result.data.decodeToString(),
                            )
                        }.getOrNull(),
                    )
                }
                else -> onComplete(null)
            }
        }
    }

    fun getUser(onComplete: (KickUser?) -> Unit) {
        val request = Request.Builder()
            .url(userUrl)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .build()
        httpRequest(request) { data, response, error ->
            if (error != null || data == null || response?.isSuccessful != true) {
                onComplete(null)
                return@httpRequest
            }
            onComplete(
                runCatching {
                    json.decodeFromString(KickUser.serializer(), data.decodeToString())
                }.getOrNull(),
            )
        }
    }

    fun getChatterInfo(user: String, onComplete: (KickChatterInfo?) -> Unit) {
        doV2Request(method = "GET", subPath = "channels/$slug/users/$user") { result ->
            when (result) {
                is OperationResult.Success -> {
                    onComplete(
                        runCatching {
                            json.decodeFromString(
                                KickChatterInfo.serializer(),
                                result.data.decodeToString(),
                            )
                        }.getOrNull(),
                    )
                }
                else -> onComplete(null)
            }
        }
    }

    private fun doV2Request(
        method: String,
        subPath: String,
        body: Map<String, Any>? = null,
        onComplete: (OperationResult) -> Unit,
    ) {
        val url = runCatching { URI("https://kick.com/api/v2/$subPath") }.getOrNull()
        if (url == null) {
            return
        }
        doRequest(url = url.toString(), method = method, body = body, onComplete = onComplete)
    }

    private fun doInternalV1Request(
        method: String,
        subPath: String,
        body: Map<String, Any>? = null,
        onComplete: (OperationResult) -> Unit,
    ) {
        val url = runCatching { URI("https://kick.com/api/internal/v1/$subPath") }.getOrNull()
        if (url == null) {
            return
        }
        doRequest(url = url.toString(), method = method, body = body, onComplete = onComplete)
    }

    private fun doWebV1Request(
        method: String,
        subPath: String,
        body: Map<String, Any>? = null,
        onComplete: (OperationResult) -> Unit,
    ) {
        val url = runCatching { URI("https://web.kick.com/api/v1/$subPath") }.getOrNull()
        if (url == null) {
            return
        }
        doRequest(url = url.toString(), method = method, body = body, onComplete = onComplete)
    }

    private fun doRequest(
        url: String,
        method: String,
        body: Map<String, Any>? = null,
        onComplete: (OperationResult) -> Unit,
    ) {
        val builder = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $accessToken")
        if (body != null) {
            builder.method(method, JSONObject(body).toString().toRequestBody("application/json".toMediaType()))
        } else {
            builder.method(method, null)
        }
        val request = builder.build()
        httpRequest(request) { data, response, error ->
            if (error != null || data == null || response?.isSuccessful != true) {
                if (data != null) {
                    Log.i("KickApi", "kick-api: Error response body: ${data.decodeToString()}")
                }
                if (response?.code == 401) {
                    delegate?.kickApiUnauthorized()
                    onComplete(OperationResult.AuthError)
                } else {
                    onComplete(OperationResult.Error)
                }
                return@httpRequest
            }
            onComplete(OperationResult.Success(data))
        }
    }
}
