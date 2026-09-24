package com.moblin.android.various.managers

import android.location.Location
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private suspend fun sleep(seconds: Double) {
    delay((seconds * 1000.0).toLong())
}

class WeatherManager {
    private val mainScope = CoroutineScope(Dispatchers.Main)
    val weatherService: Any? = null
    private var task: Job? = null
    private var location: Location? = null
    private var weather: Any? = null
    private var enabled = true

    fun start() {
        if (task != null) {
            return
        }
        task = mainScope.launch {
            var delaySeconds = 5.0
            while (true) {
                try {
                    sleep(seconds = delaySeconds)
                    val currentLocation = location
                    if (currentLocation != null && enabled) {
                        Log.d(TAG, "weather-manager: Updating weather data")
                        Unit
                    }
                } catch (_: Exception) {
                }
                if (!isActive) {
                    break
                }
                if (weather != null) {
                    delaySeconds = 10.0 * 60.0
                }
            }
        }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    fun setLocation(location: Location?) {
        this.location = location
    }

    fun getLatestWeather(): Any? {
        return weather
    }

    fun stop() {
        task?.cancel()
        task = null
        location = null
        weather = null
    }

    companion object {
        private const val TAG = "WeatherManager"
    }
}
