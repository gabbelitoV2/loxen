package com.moblin.android.platform.weatherkit

import android.location.Location
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import com.moblin.android.platform.systemImage
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeatherServiceSuite {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun location(latitude: Double, longitude: Double): Location {
        val location = Location("test")
        location.latitude = latitude
        location.longitude = longitude
        return location
    }

    private fun forecast(code: Int, isDay: Int, gust: String = "9.4"): String = """
        {"latitude":59.33,"longitude":18.07,"elevation":28.0,
         "current_units":{"time":"iso8601","temperature_2m":"°C","wind_speed_10m":"m/s"},
         "current":{"time":"2026-09-26T12:00","interval":900,"temperature_2m":14.6,
         "apparent_temperature":12.2,"weather_code":$code,"wind_speed_10m":4.3,
         "wind_gusts_10m":$gust,"is_day":$isDay}}
    """.trimIndent()

    @Test
    fun currentWeatherComesFromOpenMeteoInCelsiusAndMetersPerSecond() {
        server.enqueue(MockResponse().setBody(forecast(code = 2, isDay = 1)))
        val service = WeatherService(baseUrl = server.url("/").toString())
        val weather = runBlocking { service.weather(`for` = location(59.3293, 18.0686)) }.currentWeather
        assertEquals("PartlyCloudy", weather.condition)
        assertEquals("cloud.sun", weather.symbolName)
        assertEquals(14.6, weather.temperature)
        assertEquals(12.2, weather.apparentTemperature)
        assertEquals(4.3, weather.wind.speed)
        assertEquals(9.4, weather.wind.gust)
        assertTrue(weather.isDaylight)
        val request = server.takeRequest().requestUrl!!
        assertEquals("/v1/forecast", request.encodedPath)
        assertEquals("59.3293", request.queryParameter("latitude"))
        assertEquals("18.0686", request.queryParameter("longitude"))
        assertEquals("ms", request.queryParameter("wind_speed_unit"))
        assertEquals(
            "temperature_2m,apparent_temperature,weather_code,wind_speed_10m,wind_gusts_10m,is_day",
            request.queryParameter("current"),
        )
    }

    @Test
    fun nightUsesMoonSymbolsAndMissingGustIsNull() {
        server.enqueue(MockResponse().setBody(forecast(code = 0, isDay = 0, gust = "null")))
        val service = WeatherService(baseUrl = server.url("/").toString())
        val weather = runBlocking { service.weather(`for` = location(-33.8688, 151.2093)) }.currentWeather
        assertEquals("Clear", weather.condition)
        assertEquals("moon.stars", weather.symbolName)
        assertFalse(weather.isDaylight)
        assertNull(weather.wind.gust)
        assertEquals("-33.8688", server.takeRequest().requestUrl!!.queryParameter("latitude"))
    }

    @Test
    fun errorResponsesAndMalformedBodiesThrow() {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":true,"reason":"Latitude"}"""))
        server.enqueue(MockResponse().setBody("""{"current":{"temperature_2m":null}}"""))
        val service = WeatherService(baseUrl = server.url("/").toString())
        assertFailsWith<WeatherError> { runBlocking { service.weather(`for` = location(0.0, 0.0)) } }
        assertFailsWith<WeatherError> { runBlocking { service.weather(`for` = location(0.0, 0.0)) } }
    }

    @Test
    fun wmoCodesMapToWeatherKitConditionsWithMappedSymbols() {
        val expected = mapOf(
            0 to "Clear",
            1 to "MostlyClear",
            2 to "PartlyCloudy",
            3 to "Cloudy",
            45 to "Foggy",
            48 to "Foggy",
            51 to "Drizzle",
            56 to "FreezingDrizzle",
            61 to "Rain",
            65 to "HeavyRain",
            66 to "FreezingRain",
            71 to "Flurries",
            73 to "Snow",
            75 to "HeavySnow",
            77 to "Snow",
            80 to "Rain",
            82 to "HeavyRain",
            85 to "Snow",
            86 to "HeavySnow",
            95 to "Thunderstorms",
            96 to "Hail",
            99 to "Hail",
        )
        for ((code, condition) in expected) {
            val (day, daySymbol) = WeatherService.wmoCondition(code = code, isDaylight = true)
            val (night, nightSymbol) = WeatherService.wmoCondition(code = code, isDaylight = false)
            assertEquals(condition, day, "code $code")
            assertEquals(condition, night, "code $code")
            for (symbol in listOf(daySymbol, nightSymbol, "$daySymbol.fill", "$nightSymbol.fill")) {
                assertNotSame(Icons.Filled.RadioButtonUnchecked, systemImage(symbol), symbol)
            }
        }
    }
}
