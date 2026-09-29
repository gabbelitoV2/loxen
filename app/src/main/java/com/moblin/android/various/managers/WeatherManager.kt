package com.moblin.android.various.managers

import android.location.Location
import com.moblin.android.platform.log.Log
import com.moblin.android.common.various.sleepSeconds
import com.moblin.android.platform.weatherkit.Weather
import com.moblin.android.platform.weatherkit.WeatherService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "WeatherManager"

open class WeatherManager {
    open val weatherService = WeatherService()
    private var task: Job? = null
    private var location: Location? = null
    private var weather: Weather? = null
    private var enabled = true
    private val mainScope = CoroutineScope(Dispatchers.Main.immediate)

    open fun start() {
        if (task != null) {
            return
        }
        task = mainScope.launch {
            var delay = 5
            while (true) {
                try {
                    sleepSeconds(seconds = delay)
                    val location = location
                    if (location != null && enabled) {
                        Log.d(TAG, "weather-manager: Updating weather data")
                        weather = weatherService.weather(`for` = location)
                    }
                } catch (error: Throwable) {
                }
                if (!isActive) {
                    break
                }
                if (weather != null) {
                    delay = 10 * 60
                }
            }
        }
    }

    open fun setEnabled(value: Boolean) {
        enabled = value
    }

    open fun setLocation(location: Location?) {
        this.location = location
    }

    open fun getLatestWeather(): Weather? {
        return weather
    }

    open fun stop() {
        task?.cancel()
        task = null
        location = null
        weather = null
    }
}
