package com.moblin.android.various.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location as AndroidLocation
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import com.moblin.android.common.various.format
import com.moblin.android.various.settings.SettingsLocationDesiredAccuracy
import com.moblin.android.various.settings.SettingsLocationDistanceFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "Location"

private class BackgroundActivity {
    private var backgroundSession: Any? = null

    fun start() {
        TODO("no Android counterpart for CLBackgroundActivitySession")
    }

    fun stop() {
        TODO("no Android counterpart for CLBackgroundActivitySession")
    }
}

class Location(private val context: Context) : LocationListener {
    private val _isDenied = MutableStateFlow(false)
    val isDenied: StateFlow<Boolean> = _isDenied.asStateFlow()
    private val manager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var onUpdate: ((AndroidLocation) -> Unit)? = null
    private var latestLocation: AndroidLocation? = null
    private val backgroundActivity = BackgroundActivity()

    fun start(
        accuracy: SettingsLocationDesiredAccuracy,
        distanceFilter: SettingsLocationDistanceFilter,
        onUpdate: (AndroidLocation) -> Unit,
    ) {
        Log.d(TAG, "location: Start with accuracy $accuracy and distance filter $distanceFilter")
        this.onUpdate = onUpdate
        val criteria = Criteria()
        when (accuracy) {
            SettingsLocationDesiredAccuracy.BEST ->
                criteria.horizontalAccuracy = Criteria.ACCURACY_FINE
            SettingsLocationDesiredAccuracy.NEAREST_TEN_METERS ->
                criteria.horizontalAccuracy = Criteria.ACCURACY_FINE
            SettingsLocationDesiredAccuracy.HUNDRED_METERS ->
                criteria.horizontalAccuracy = Criteria.ACCURACY_COARSE
        }
        val provider = manager.getBestProvider(criteria, true) ?: LocationManager.GPS_PROVIDER
        val minDistance = when (distanceFilter) {
            SettingsLocationDistanceFilter.NONE -> 0.0f
            SettingsLocationDistanceFilter.ONE_METER -> 1.0f
            SettingsLocationDistanceFilter.THREE_METERS -> 3.0f
            SettingsLocationDistanceFilter.FIVE_METERS -> 5.0f
            SettingsLocationDistanceFilter.TEN_METERS -> 10.0f
            SettingsLocationDistanceFilter.TWENTY_METERS -> 20.0f
            SettingsLocationDistanceFilter.FIFTY_METERS -> 50.0f
            SettingsLocationDistanceFilter.HUNDRED_METERS -> 100.0f
            SettingsLocationDistanceFilter.TWO_HUNDRED_METERS -> 200.0f
            else -> 0.0f
        }
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            TODO("request ACCESS_FINE_LOCATION permission in the Activity layer")
        }
        manager.requestLocationUpdates(provider, 0L, minDistance, this, Looper.getMainLooper())
        backgroundActivity.start()
    }

    fun stop() {
        Log.d(TAG, "location: Stop")
        onUpdate = null
        manager.removeUpdates(this)
        backgroundActivity.stop()
    }

    fun status(): String {
        val location = latestLocation ?: return ""
        return format(speed = location.speed.toDouble())
    }

    fun getLatestKnownLocation(): AndroidLocation? {
        return latestLocation
    }

    fun locationManagerDidChangeAuthorization(manager: LocationManager) {
        val granted = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "location: Auth did change $granted")
        _isDenied.value = !granted
    }

    override fun onProviderDisabled(provider: String) {
        Log.i(TAG, "location: Error, provider $provider disabled")
    }

    override fun onLocationChanged(location: AndroidLocation) {
        latestLocation = location
        onUpdate?.invoke(location)
    }

    override fun onProviderEnabled(provider: String) {
    }

    @Suppress("DEPRECATION")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }
}
