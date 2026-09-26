package com.moblin.android.platform.weatherkit

import android.location.Location
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerialName
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class Wind(val speed: Double, val gust: Double?)

class CurrentWeather(
    val symbolName: String,
    val condition: String,
    val temperature: Double,
    val apparentTemperature: Double,
    val wind: Wind,
    val isDaylight: Boolean,
)

class Weather(val currentWeather: CurrentWeather)

class WeatherError(message: String, cause: Throwable? = null) : IOException(message, cause)

class WeatherService(baseUrl: String = OPEN_METEO_URL) {
    private val forecastUrl: HttpUrl = baseUrl.toHttpUrl().newBuilder().addPathSegments("v1/forecast").build()

    suspend fun weather(`for`: Location): Weather {
        val url = forecastUrl.newBuilder()
            .addQueryParameter("latitude", coordinate(`for`.latitude))
            .addQueryParameter("longitude", coordinate(`for`.longitude))
            .addQueryParameter("current", CURRENT_FIELDS)
            .addQueryParameter("wind_speed_unit", "ms")
            .build()
        val call = client.newCall(Request.Builder().url(url).build())
        val body = suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching {
                        response.use {
                            val text = it.body?.string().orEmpty()
                            if (!it.isSuccessful) {
                                throw WeatherError("Open-Meteo responded ${it.code}: ${text.take(200)}")
                            }
                            text
                        }
                    }
                    continuation.resumeWith(result)
                }
            })
        }
        return parseForecast(body)
    }

    companion object {
        val shared: WeatherService by lazy { WeatherService() }

        const val OPEN_METEO_URL = "https://api.open-meteo.com/"
        private const val CURRENT_FIELDS =
            "temperature_2m,apparent_temperature,weather_code,wind_speed_10m,wind_gusts_10m,is_day"
        private val client = OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
        private val json = Json { ignoreUnknownKeys = true }

        private fun coordinate(value: Double): String = String.format(Locale.ROOT, "%.4f", value)

        internal fun parseForecast(body: String): Weather {
            val current = try {
                json.decodeFromString<OpenMeteoForecast>(body).current
            } catch (e: IllegalArgumentException) {
                throw WeatherError("Malformed Open-Meteo response", e)
            }
            val isDaylight = current.isDay != 0
            val (condition, symbolName) = wmoCondition(code = current.weatherCode, isDaylight = isDaylight)
            return Weather(
                currentWeather = CurrentWeather(
                    symbolName = symbolName,
                    condition = condition,
                    temperature = current.temperature,
                    apparentTemperature = current.apparentTemperature,
                    wind = Wind(speed = current.windSpeed, gust = current.windGust),
                    isDaylight = isDaylight,
                ),
            )
        }

        internal fun wmoCondition(code: Int, isDaylight: Boolean): Pair<String, String> = when (code) {
            0 -> "Clear" to if (isDaylight) "sun.max" else "moon.stars"
            1 -> "MostlyClear" to if (isDaylight) "sun.max" else "moon.stars"
            2 -> "PartlyCloudy" to if (isDaylight) "cloud.sun" else "cloud.moon"
            3 -> "Cloudy" to "cloud"
            45, 48 -> "Foggy" to "cloud.fog"
            51, 53, 55 -> "Drizzle" to "cloud.drizzle"
            56, 57 -> "FreezingDrizzle" to "cloud.sleet"
            61, 63, 80, 81 -> "Rain" to "cloud.rain"
            65, 82 -> "HeavyRain" to "cloud.heavyrain"
            66, 67 -> "FreezingRain" to "cloud.sleet"
            71 -> "Flurries" to "cloud.snow"
            73, 77, 85 -> "Snow" to "cloud.snow"
            75, 86 -> "HeavySnow" to "snowflake"
            95 -> "Thunderstorms" to "cloud.bolt.rain"
            96, 99 -> "Hail" to "cloud.hail"
            else -> "Cloudy" to "cloud"
        }
    }
}

@Serializable
private class OpenMeteoForecast(val current: OpenMeteoCurrent)

@Serializable
private class OpenMeteoCurrent(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("wind_gusts_10m") val windGust: Double? = null,
    @SerialName("is_day") val isDay: Int = 1,
)
