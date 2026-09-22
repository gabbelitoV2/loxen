package com.moblin.android.streamingplatforms.youtube

import android.util.Log
import com.moblin.android.localized
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.network.OperationResult
import com.moblin.android.various.network.httpRequest
import com.moblin.android.various.network.makeUrl
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

@Serializable
data class YouTubeApiLiveBroadcastThumbnail(val url: String)

@Serializable
data class YouTubeApiLiveBroadcastThumbnails(val `default`: YouTubeApiLiveBroadcastThumbnail)

@Serializable
data class YouTubeApiLiveBroadcastSnippet(
    val title: String,
    val thumbnails: YouTubeApiLiveBroadcastThumbnails,
    val scheduledStartTime: String? = null,
)

@Serializable
data class YouTubeApiLiveBroadcastStatus(val privacyStatus: String) {
    fun visibility(): YouTubeApiLiveBroadcaseVisibility? =
        YouTubeApiLiveBroadcaseVisibility.fromRawValue(privacyStatus)
}

@Serializable
data class YouTubeApiLiveBroadcastContentDetails(
    val boundStreamId: String? = null,
    val enableAutoStart: Boolean,
    val enableAutoStop: Boolean,
)

@Serializable
data class YouTubeApiLiveBroadcast(
    val id: String,
    val snippet: YouTubeApiLiveBroadcastSnippet,
    val status: YouTubeApiLiveBroadcastStatus,
    val contentDetails: YouTubeApiLiveBroadcastContentDetails,
)

@Serializable
data class YouTubeApiLiveStreamIngestInfo(
    val streamName: String,
    val ingestionAddress: String,
)

@Serializable
data class YouTubeApiLiveStreamCdn(val ingestionInfo: YouTubeApiLiveStreamIngestInfo)

@Serializable
data class YouTubeApiLiveStream(
    val id: String,
    val cdn: YouTubeApiLiveStreamCdn,
)

@Serializable
data class YouTubeApiLiveStreamsListResponse(val items: List<YouTubeApiLiveStream>)

private val iso8601Formatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

private fun serialize(value: JsonElement): ByteArray =
    Json.encodeToString(JsonElement.serializer(), value).encodeToByteArray()

enum class YouTubeApiLiveBroadcaseVisibility(val rawValue: String) {
    `public`("public"),
    `private`("private"),
    unlisted("unlisted"),
    ;

    override fun toString(): String {
        return when (this) {
            `public` -> localized("Public")
            `private` -> localized("Private")
            unlisted -> localized("Unlisted")
        }
    }

    companion object {
        fun fromRawValue(value: String): YouTubeApiLiveBroadcaseVisibility? =
            YouTubeApiLiveBroadcaseVisibility.entries.firstOrNull { it.rawValue == value }
    }
}

@Serializable
data class YouTubeApiListVideoStreamingDetails(
    val concurrentViewers: String? = null,
    val actualStartTime: String? = null,
    val actualEndTime: String? = null,
) {
    fun isLive(): Boolean = actualStartTime != null && actualEndTime == null
}

@Serializable
data class YouTubeApiListVideo(val liveStreamingDetails: YouTubeApiListVideoStreamingDetails)

@Serializable
data class YouTubeApiListVideosResponse(val items: List<YouTubeApiListVideo>)

@Serializable
data class YouTubeApiLiveBroadcastListResponse(val items: List<YouTubeApiLiveBroadcast>)

@Serializable
data class YouTubeApiChannelSnippet(val customUrl: String? = null)

@Serializable
data class YouTubeApiChannel(val snippet: YouTubeApiChannelSnippet)

@Serializable
data class YouTubeApiChannelListResponse(val items: List<YouTubeApiChannel>)

interface YouTubeApiDelegate {
    fun youTubeApiUnauthorized()
}

class YouTubeApi(private val accessToken: String) {
    var delegate: YouTubeApiDelegate? = null

    fun listVideos(
        videoIds: String,
        onCompleted: (NetworkResponse<YouTubeApiListVideosResponse>) -> Unit,
    ) {
        val subPath = makeUrl(
            "videos",
            listOf(
                "part" to "liveStreamingDetails",
                "id" to videoIds,
            ),
        )
        doGet(subPath) { result ->
            when (result) {
                is OperationResult.Success -> {
                    val response = runCatching {
                        Json.decodeFromString(
                            YouTubeApiListVideosResponse.serializer(),
                            String(result.data, Charsets.UTF_8),
                        )
                    }.getOrNull()
                    if (response != null) {
                        onCompleted(NetworkResponse.Success(response))
                    } else {
                        onCompleted(NetworkResponse.Error)
                    }
                }
                is OperationResult.AuthError -> onCompleted(NetworkResponse.AuthError)
                is OperationResult.Error -> onCompleted(NetworkResponse.Error)
            }
        }
    }

    fun listLiveBroadcasts(
        status: String,
        onCompleted: (NetworkResponse<YouTubeApiLiveBroadcastListResponse>) -> Unit,
    ) {
        val subPath = makeUrl(
            "liveBroadcasts",
            listOf(
                "part" to "snippet,contentDetails,status",
                "broadcastStatus" to status,
            ),
        )
        doGet(subPath) { result ->
            when (result) {
                is OperationResult.Success -> {
                    val response = runCatching {
                        Json.decodeFromString(
                            YouTubeApiLiveBroadcastListResponse.serializer(),
                            String(result.data, Charsets.UTF_8),
                        )
                    }.getOrNull()
                    if (response != null) {
                        onCompleted(NetworkResponse.Success(response))
                    } else {
                        onCompleted(NetworkResponse.Error)
                    }
                }
                is OperationResult.AuthError -> onCompleted(NetworkResponse.AuthError)
                is OperationResult.Error -> onCompleted(NetworkResponse.Error)
            }
        }
    }

    fun insertLiveBroadcast(
        title: String,
        visibility: YouTubeApiLiveBroadcaseVisibility,
        autoStop: Boolean,
        onCompleted: (NetworkResponse<YouTubeApiLiveBroadcast>) -> Unit,
    ) {
        val subPath = makeUrl("liveBroadcasts", listOf("part" to "snippet,contentDetails,status"))
        val body: JsonElement = buildJsonObject {
            put(
                "snippet",
                buildJsonObject {
                    put("title", title)
                    put("scheduledStartTime", iso8601Formatter.format(Instant.now()))
                },
            )
            put(
                "status",
                buildJsonObject {
                    put("privacyStatus", visibility.rawValue)
                    put("selfDeclaredMadeForKids", false)
                },
            )
            put(
                "contentDetails",
                buildJsonObject {
                    put("enableAutoStart", true)
                    put("enableAutoStop", autoStop)
                },
            )
        }
        doPost(subPath, serialize(body)) { result ->
            when (result) {
                is OperationResult.Success -> {
                    val response = runCatching {
                        Json.decodeFromString(
                            YouTubeApiLiveBroadcast.serializer(),
                            String(result.data, Charsets.UTF_8),
                        )
                    }.getOrNull()
                    if (response != null) {
                        onCompleted(NetworkResponse.Success(response))
                    } else {
                        onCompleted(NetworkResponse.Error)
                    }
                }
                is OperationResult.AuthError -> onCompleted(NetworkResponse.AuthError)
                is OperationResult.Error -> onCompleted(NetworkResponse.Error)
            }
        }
    }

    fun deleteLiveBroadcast(id: String, onCompleted: (NetworkResponse<Unit>) -> Unit) {
        val subPath = makeUrl("liveBroadcasts", listOf("id" to id))
        doDelete(subPath) { result ->
            when (result) {
                is OperationResult.Success -> onCompleted(NetworkResponse.Success(Unit))
                is OperationResult.AuthError -> onCompleted(NetworkResponse.AuthError)
                is OperationResult.Error -> onCompleted(NetworkResponse.Error)
            }
        }
    }

    fun bindLiveBroadcast(
        boardcastId: String,
        streamId: String,
        onCompleted: (Boolean) -> Unit,
    ) {
        val subPath = makeUrl(
            "liveBroadcasts/bind",
            listOf(
                "id" to boardcastId,
                "streamId" to streamId,
                "part" to "snippet,contentDetails,status",
            ),
        )
        doPost(subPath, ByteArray(0)) { result ->
            when (result) {
                is OperationResult.Success -> onCompleted(true)
                else -> onCompleted(false)
            }
        }
    }

    fun transitionLiveBroadcast(id: String, status: String, onCompleted: (Boolean) -> Unit) {
        val subPath = makeUrl(
            "liveBroadcasts/transition",
            listOf(
                "id" to id,
                "broadcastStatus" to status,
                "part" to "snippet,contentDetails,status",
            ),
        )
        doPost(subPath, ByteArray(0)) { result ->
            when (result) {
                is OperationResult.Success -> onCompleted(true)
                else -> onCompleted(false)
            }
        }
    }

    fun listLiveStreams(onCompleted: (NetworkResponse<YouTubeApiLiveStreamsListResponse>) -> Unit) {
        val subPath = makeUrl(
            "liveStreams",
            listOf(
                "part" to "snippet,cdn,contentDetails,status",
                "mine" to "true",
            ),
        )
        doGet(subPath) { result ->
            when (result) {
                is OperationResult.Success -> {
                    val response = runCatching {
                        Json.decodeFromString(
                            YouTubeApiLiveStreamsListResponse.serializer(),
                            String(result.data, Charsets.UTF_8),
                        )
                    }.getOrNull()
                    if (response != null) {
                        onCompleted(NetworkResponse.Success(response))
                    } else {
                        onCompleted(NetworkResponse.Error)
                    }
                }
                is OperationResult.AuthError -> onCompleted(NetworkResponse.AuthError)
                is OperationResult.Error -> onCompleted(NetworkResponse.Error)
            }
        }
    }

    fun listChannels(onCompleted: (NetworkResponse<YouTubeApiChannelListResponse>) -> Unit) {
        val subPath = makeUrl(
            "channels",
            listOf(
                "part" to "snippet",
                "mine" to "true",
            ),
        )
        doGet(subPath) { result ->
            when (result) {
                is OperationResult.Success -> {
                    val response = runCatching {
                        Json.decodeFromString(
                            YouTubeApiChannelListResponse.serializer(),
                            String(result.data, Charsets.UTF_8),
                        )
                    }.getOrNull()
                    if (response != null) {
                        onCompleted(NetworkResponse.Success(response))
                    } else {
                        onCompleted(NetworkResponse.Error)
                    }
                }
                is OperationResult.AuthError -> onCompleted(NetworkResponse.AuthError)
                is OperationResult.Error -> onCompleted(NetworkResponse.Error)
            }
        }
    }

    private fun doGet(subPath: String, onComplete: (OperationResult) -> Unit) {
        val url = "https://youtube.googleapis.com/youtube/v3/$subPath"
        doRequest(createRequest(url, "GET"), onComplete)
    }

    private fun doPost(subPath: String, body: ByteArray, onComplete: (OperationResult) -> Unit) {
        val url = "https://youtube.googleapis.com/youtube/v3/$subPath"
        doRequest(createRequest(url, "POST", json = true, body = body), onComplete)
    }

    private fun doDelete(subPath: String, onComplete: (OperationResult) -> Unit) {
        val url = "https://youtube.googleapis.com/youtube/v3/$subPath"
        doRequest(createRequest(url, "DELETE"), onComplete)
    }

    private fun doRequest(request: Request, onComplete: (OperationResult) -> Unit) {
        httpRequest(request) { data, response, error ->
            if (error != null || data == null || response?.isSuccessful != true) {
                if (data != null) {
                    Log.i(TAG, "youtube-api: Error response body: ${String(data, Charsets.UTF_8)}")
                }
                if (response?.code == 401) {
                    delegate?.youTubeApiUnauthorized()
                    onComplete(OperationResult.AuthError)
                } else {
                    onComplete(OperationResult.Error)
                }
                return@httpRequest
            }
            onComplete(OperationResult.Success(data))
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
            .header("Authorization", "Bearer $accessToken")
        if (json) {
            builder.header("Content-Type", "application/json")
        }
        when (method) {
            "GET" -> builder.get()
            "DELETE" -> builder.delete()
            else -> builder.method(
                method,
                (body ?: ByteArray(0)).toRequestBody("application/json".toMediaTypeOrNull()),
            )
        }
        return builder.build()
    }

    companion object {
        private const val TAG = "YouTubeApi"
    }
}

private fun Response.isUnauthorized(): Boolean = code == 401
