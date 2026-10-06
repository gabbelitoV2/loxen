package com.moblin.android.integrations.ttsmonster

import java.net.URI
import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.moblin.android.common.various.isSuccessful
import com.moblin.android.various.network.httpGet

private val baseUrl: HttpUrl = "https://api.console.tts.monster".toHttpUrl()

private val json = Json {
    ignoreUnknownKeys = true
}

private fun parseLocale(identifier: String): Locale =
    Locale.forLanguageTag(identifier.replace('_', '-'))

private fun firstMetadataPart(metadata: String?): String? =
    metadata?.split("|")?.firstOrNull()?.takeIf { it.isNotEmpty() }

@Serializable
data class TtsMonsterVoice(
    @SerialName("voice_id") val voice_id: String,
    @SerialName("name") val name: String,
    @SerialName("language") val language: String? = null,
    @SerialName("metadata") val metadata: String? = null,
) {
    fun countryCode(): String? {
        val part = firstMetadataPart(metadata) ?: return null
        return parseLocale(part).country.takeIf { it.isNotEmpty() }
    }

    fun languageCode(): String? {
        if (language != null) {
            return parseLocale(language).language.takeIf { it.isNotEmpty() }
        } else if (metadata != null) {
            val part = firstMetadataPart(metadata) ?: return null
            return parseLocale(part).language.takeIf { it.isNotEmpty() }
        } else {
            return null
        }
    }
}

@Serializable
data class TtsMonsterVoicesResponse(
    @SerialName("voices") val voices: List<TtsMonsterVoice>,
    @SerialName("customVoices") val customVoices: List<TtsMonsterVoice>,
) {
    fun allVoices(): List<TtsMonsterVoice> {
        return customVoices + voices
    }
}

@Serializable
data class TtsMonsterGenerateRequest(
    @SerialName("voice_id") val voice_id: String,
    @SerialName("message") val message: String,
)

@Serializable
data class TtsMonsterGenerateResponse(
    @SerialName("url") var url: String,
)

class TtsMonster(private val apiToken: String) {
    suspend fun getVoices(): TtsMonsterVoicesResponse? {
        val request = createRequest("voices")
        val (data, response) = httpGet(request) ?: return null
        if (!response.isSuccessful) {
            return null
        }
        return runCatching {
            json.decodeFromString(TtsMonsterVoicesResponse.serializer(), data.decodeToString())
        }.getOrNull()
    }

    suspend fun generateTts(voiceId: String, message: String): ByteArray? {
        val body = json.encodeToString(
            TtsMonsterGenerateRequest.serializer(),
            TtsMonsterGenerateRequest(voice_id = voiceId, message = message),
        )
        val request = createRequest("generate")
            .newBuilder()
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        val (data, response) = httpGet(request) ?: return null
        if (!response.isSuccessful) {
            return null
        }
        val generateResponse = runCatching {
            json.decodeFromString(TtsMonsterGenerateResponse.serializer(), data.decodeToString())
        }.getOrNull() ?: return null
        val url = generateResponse.url.toHttpUrlOrNull() ?: return null
        val (audioData, audioResponse) = httpGet(URI(url.toString())) ?: return null
        if (!audioResponse.isSuccessful) {
            return null
        }
        return audioData
    }

    private fun createRequest(component: String): Request {
        val url = baseUrl.newBuilder().addPathSegment(component).build()
        return Request.Builder()
            .url(url)
            .post(ByteArray(0).toRequestBody())
            .header("Authorization", "Bearer $apiToken")
            .build()
    }
}
