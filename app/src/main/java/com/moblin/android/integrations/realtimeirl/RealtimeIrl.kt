package com.moblin.android.integrations.realtimeirl

import android.location.Location
import com.moblin.android.various.network.httpRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class RealtimeIrl(baseUrl: String, pushKey: String) {
    private val pushUrl: String = "$baseUrl/push?key=$pushKey"
    private val stopUrl: String = "$baseUrl/stop?key=$pushKey"
    private var updateCount = 0

    fun status(): String {
        return if (updateCount > 0) {
            " ($updateCount)"
        } else {
            ""
        }
    }

    fun update(location: Location) {
        updateCount += 1
        val body = """
        {
          "latitude":${location.latitude},
          "longitude":${location.longitude},
          "speed":${location.speed},
          "altitude":${location.altitude},
          "timestamp":${location.time / 1000.0}
        }
        """.trimIndent()
        val request = Request.Builder()
            .url(pushUrl)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        httpRequest(request)
    }

    fun stop() {
        updateCount = 0
        val request = Request.Builder()
            .url(stopUrl)
            .post("".toRequestBody())
            .build()
        httpRequest(request)
    }
}
