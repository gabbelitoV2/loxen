package com.moblin.android.various.model

import android.location.Location
import android.os.Looper
import com.moblin.android.platform.weatherkit.WeatherService
import com.moblin.android.various.managers.WeatherManager
import java.time.Duration
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rSE")
class ModelWeatherSuite {
    private lateinit var model: Model
    private lateinit var server: MockWebServer
    private val format = "{conditions} {temperature:c} {feelsLikeTemperature:c} {wind:m/s}"

    @Before
    fun setUp() {
        Locale.setDefault(Locale.forLanguageTag("en-SE"))
        server = MockWebServer()
        server.start()
        model = Model()
        shadowOf(Looper.getMainLooper()).idle()
        val field = WeatherManager::class.java.getDeclaredField("weatherService")
        field.isAccessible = true
        field.set(model.weatherManager, WeatherService(baseUrl = server.url("/").toString()))
    }

    @After
    fun tearDown() {
        model.weatherManager.stop()
        server.shutdown()
    }

    private fun location(): Location {
        val location = Location("test")
        location.latitude = 59.3293
        location.longitude = 18.0686
        return location
    }

    private fun waitForWeather() {
        val looper = shadowOf(Looper.getMainLooper())
        looper.idleFor(Duration.ofSeconds(5))
        val deadline = System.currentTimeMillis() + 5_000
        while (model.weatherManager.getLatestWeather() == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
            looper.idle()
        }
    }

    @Test
    fun textWidgetWeatherVariablesShowTheWeatherAtTheLatestLocation() {
        assertEquals("- - - -", model.formatPlainText(format))
        server.enqueue(
            MockResponse().setBody(
                """
                {"current":{"time":"2026-09-26T12:00","interval":900,"temperature_2m":14.6,
                "apparent_temperature":12.2,"weather_code":2,"wind_speed_10m":4.3,
                "wind_gusts_10m":9.4,"is_day":1}}
                """.trimIndent(),
            ),
        )
        model.weatherManager.setLocation(location = location())
        model.weatherManager.start()
        waitForWeather()
        assertNotNull(model.weatherManager.getLatestWeather())
        assertEquals("⛅ 15°C 12°C 4 (9) m/s", model.formatPlainText(format))
        val request = server.takeRequest().requestUrl!!
        assertEquals("59.3293", request.queryParameter("latitude"))
        assertEquals("18.0686", request.queryParameter("longitude"))
    }

    @Test
    fun noLocationMeansNoRequestAndNoWeather() {
        model.weatherManager.setLocation(location = null)
        model.weatherManager.start()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(12))
        assertEquals(0, server.requestCount)
        assertNull(model.weatherManager.getLatestWeather())
        assertEquals("- - - -", model.formatPlainText(format))
    }
}
