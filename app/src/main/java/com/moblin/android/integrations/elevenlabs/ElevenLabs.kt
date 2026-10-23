package com.moblin.android.integrations.elevenlabs

import com.moblin.android.platform.codable.codableJson
import com.moblin.android.various.network.httpGet
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale

private const val baseUrl = "https://api.elevenlabs.io/v1"

@Serializable
data class ElevenLabsVerifiedLanguage(val language: String, val locale: String? = null)

@Serializable
data class ElevenLabsVoice(
    val voice_id: String,
    val name: String,
    val verified_languages: List<ElevenLabsVerifiedLanguage>? = null,
) {
    fun countryCode(languageCode: String): String? {
        val locale = verified_languages?.firstOrNull { it.language == languageCode }?.locale ?: return null
        val region = Locale.forLanguageTag(locale.replace('_', '-')).country
        return region.ifEmpty { null }
    }
}

@Serializable
data class ElevenLabsVoicesResponse(val voices: List<ElevenLabsVoice>)

@Serializable
data class ElevenLabsGenerateRequest(val text: String)

class ElevenLabs(private val apiKey: String) {
    suspend fun getVoices(): ElevenLabsVoicesResponse? {
        val request = createRequest("$baseUrl/voices", "GET").build()
        val (data, response) = runCatching { httpGet(request) }.getOrNull() ?: return null
        if (!response.isSuccessful) {
            return null
        }
        return runCatching {
            codableJson.decodeFromString(
                ElevenLabsVoicesResponse.serializer(),
                data.decodeToString().removePrefix("\uFEFF"),
            )
        }.getOrNull()
    }

    suspend fun generateTts(voiceId: String, message: String): ByteArray? {
        val body = runCatching {
            codableJson.encodeToString(
                ElevenLabsGenerateRequest.serializer(),
                ElevenLabsGenerateRequest(text = message),
            )
        }.getOrNull() ?: return null
        val request = createRequest("$baseUrl/text-to-speech/$voiceId", "POST")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("Accept", "audio/mpeg")
            .build()
        val (data, response) = runCatching { httpGet(request) }.getOrNull() ?: return null
        if (!response.isSuccessful) {
            return null
        }
        return data
    }

    private fun createRequest(url: String, method: String): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .header("xi-api-key", apiKey)
        builder.method(method, if (method == "GET") null else ByteArray(0).toRequestBody(null))
        return builder
    }
}
