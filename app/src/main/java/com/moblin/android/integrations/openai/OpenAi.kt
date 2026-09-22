package com.moblin.android.integrations.openai

import com.moblin.android.various.network.httpRequest
import java.net.URI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

private val json = Json {
    ignoreUnknownKeys = true
}

@Serializable
private data class Message(
    val role: String,
    val content: String,
)

@Serializable
private data class Choice(
    var message: Message,
)

@Serializable
private data class Request(
    val model: String,
    val messages: List<Message>,
)

@Serializable
private data class Response(
    val choices: List<Choice>,
)

@Serializable
private data class ResponseErrorItemError(
    val message: String,
)

@Serializable
private data class ResponseErrorItem(
    val error: ResponseErrorItemError,
)

@Serializable
private data class ResponseError(
    val errors: List<ResponseErrorItem>,
)

sealed class OpenAiError : Exception() {
    abstract val description: String

    override fun toString(): String = description

    object MalformedRequest : OpenAiError() {
        override val description: String = "malformed request"
    }

    class RequestFailed(override val message: String) : OpenAiError() {
        override val description: String = "request failed: $message"
    }

    object RateLimited : OpenAiError() {
        override val description: String = "too many requests"
    }

    class HttpError(val statusCode: Int, override val message: String) : OpenAiError() {
        override val description: String = "HTTP error $statusCode: $message"
    }

    object MalformedResponse : OpenAiError() {
        override val description: String = "malformed response"
    }

    object NoAnswer : OpenAiError() {
        override val description: String = "no answer"
    }
}

class OpenAi(baseUrl: URI, private val apiKey: String) {
    private val url: String = baseUrl.toString().trimEnd('/') + "/chat/completions"
    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun ask(
        content: String,
        model: String,
        role: String,
        onComplete: (Result<String>) -> Unit,
    ) {
        val body = runCatching {
            json.encodeToString(
                Request.serializer(),
                Request(
                    model = model,
                    messages = listOf(
                        Message(role = "system", content = role),
                        Message(role = "user", content = content),
                    ),
                ),
            )
        }.getOrNull()
        if (body == null) {
            onComplete(Result.failure(OpenAiError.MalformedRequest))
            return
        }
        val request = okhttp3.Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .build()
        httpRequest(request) { data, response, error ->
            mainScope.launch {
                if (error != null) {
                    onComplete(Result.failure(OpenAiError.RequestFailed(error.message ?: "")))
                    return@launch
                }
                if (response == null || data == null) {
                    onComplete(Result.failure(OpenAiError.MalformedResponse))
                    return@launch
                }
                if (response.statusCode !in 200..299) {
                    if (response.statusCode == 429) {
                        onComplete(Result.failure(OpenAiError.RateLimited))
                    } else {
                        val message = runCatching {
                            json.decodeFromString(
                                ResponseError.serializer(),
                                data.decodeToString(),
                            )
                        }.getOrNull()?.errors?.firstOrNull()?.error?.message ?: ""
                        onComplete(
                            Result.failure(OpenAiError.HttpError(response.statusCode, message)),
                        )
                    }
                    return@launch
                }
                val choices = runCatching {
                    json.decodeFromString(Response.serializer(), data.decodeToString())
                }.getOrNull()?.choices
                if (choices == null) {
                    onComplete(Result.failure(OpenAiError.MalformedResponse))
                    return@launch
                }
                val answer = choices.firstOrNull()?.message?.content
                if (answer == null || answer.isEmpty()) {
                    onComplete(Result.failure(OpenAiError.NoAnswer))
                    return@launch
                }
                onComplete(Result.success(answer))
            }
        }
    }
}
