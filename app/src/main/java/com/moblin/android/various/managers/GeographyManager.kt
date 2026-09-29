package com.moblin.android.various.managers

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import com.moblin.android.platform.log.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeographyManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var task: Job? = null
    private var newLocation: Location? = null
    private var location: Location? = null
    private var placemark: Address? = null
    private var enabled = true
    private val geocoder = Geocoder(context)

    fun start() {
        if (task != null) {
            return
        }
        task = scope.launch {
            var delay = 5
            while (true) {
                runCatching {
                    sleep(delay)
                    val target = newLocation
                    if (target != null && enabled &&
                        (location?.distanceTo(target) ?: 1001f) > 1000f
                    ) {
                        Log.d("GeographyManager", "geography-manager: Updating geography data")
                        placemark = withContext(Dispatchers.IO) {
                            geocoder.getFromLocation(target.latitude, target.longitude, 1)
                                ?.firstOrNull()
                        }
                        location = target
                    }
                }
                if (!isActive) {
                    break
                }
                if (placemark != null) {
                    delay = 5 * 60
                }
            }
        }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    fun setLocation(location: Location?) {
        newLocation = location
    }

    fun getLatestPlacemark(): Address? {
        return placemark
    }

    fun stop() {
        task?.cancel()
        task = null
        location = null
        placemark = null
    }
}

private suspend fun sleep(seconds: Int) {
    delay(seconds * 1000L)
}
