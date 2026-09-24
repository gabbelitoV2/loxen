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
import com.moblin.android.common.various.formatSpeed
import com.moblin.android.various.settings.SettingsLocationDesiredAccuracy
import com.moblin.android.various.settings.SettingsLocationDistanceFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "Location"

private class BackgroundActivity {
    private var backgroundSession: Any? = null

    fun start() {
        Unit
    }

    fun stop() {
        Unit
    }
}

class Location(private val context: Context) : LocationListener {
    val isDenied = MutableStateFlow(false)
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
            SettingsLocationDesiredAccuracy.best ->
                criteria.horizontalAccuracy = Criteria.ACCURACY_FINE
            SettingsLocationDesiredAccuracy.nearestTenMeters ->
                criteria.horizontalAccuracy = Criteria.ACCURACY_FINE
            SettingsLocationDesiredAccuracy.hundredMeters ->
                criteria.horizontalAccuracy = Criteria.ACCURACY_COARSE
        }
        val provider = manager.getBestProvider(criteria, true) ?: LocationManager.GPS_PROVIDER
        val minDistance = when (distanceFilter) {
            SettingsLocationDistanceFilter.none -> 0.0f
            SettingsLocationDistanceFilter.oneMeter -> 1.0f
            SettingsLocationDistanceFilter.threeMeters -> 3.0f
            SettingsLocationDistanceFilter.fiveMeters -> 5.0f
            SettingsLocationDistanceFilter.tenMeters -> 10.0f
            SettingsLocationDistanceFilter.twentyMeters -> 20.0f
            SettingsLocationDistanceFilter.fiftyMeters -> 50.0f
            SettingsLocationDistanceFilter.hundredMeters -> 100.0f
            SettingsLocationDistanceFilter.twoHundredMeters -> 200.0f
            else -> 0.0f
        }
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            locationManagerDidChangeAuthorization(manager)
            return
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
        return formatSpeed(location.speed.toDouble())
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
        isDenied.value = !granted
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
