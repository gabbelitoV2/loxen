package com.moblin.android.various.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location as AndroidLocation
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import com.moblin.android.platform.log.Log
import com.moblin.android.common.various.formatSpeed
import com.moblin.android.platform.corelocation.CLAuthorizationStatus
import com.moblin.android.platform.corelocation.CLBackgroundActivitySession
import com.moblin.android.platform.corelocation.CLLocationManager
import com.moblin.android.platform.corelocation.CLLocationManagerDelegate
import com.moblin.android.various.settings.SettingsLocationDesiredAccuracy
import com.moblin.android.various.settings.SettingsLocationDistanceFilter
import kotlinx.coroutines.flow.MutableStateFlow

private class BackgroundActivity {
    private var backgroundSession: CLBackgroundActivitySession? = null

    fun start() {
        backgroundSession = CLBackgroundActivitySession()
    }

    fun stop() {
        backgroundSession?.invalidate()
    }
}

open class Location(private val context: Context) : LocationListener, CLLocationManagerDelegate {
    val isDenied = MutableStateFlow(false)
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val authorization = CLLocationManager()
    private var provider: String = LocationManager.GPS_PROVIDER
    private var minDistance: Float = 0.0f
    private var onUpdate: ((AndroidLocation) -> Unit)? = null
    private var latestLocation: AndroidLocation? = null
    private val backgroundActivity = BackgroundActivity()

    open fun start(
        accuracy: SettingsLocationDesiredAccuracy,
        distanceFilter: SettingsLocationDistanceFilter,
        onUpdate: (AndroidLocation) -> Unit,
    ) {
        Log.d(TAG, "location: Start with accuracy $accuracy and distance filter $distanceFilter")
        this.onUpdate = onUpdate
        authorization.delegate = this
        when (accuracy) {
            SettingsLocationDesiredAccuracy.best -> updateProvider(Criteria.ACCURACY_FINE)
            SettingsLocationDesiredAccuracy.nearestTenMeters -> updateProvider(Criteria.ACCURACY_FINE)
            SettingsLocationDesiredAccuracy.hundredMeters -> updateProvider(Criteria.ACCURACY_COARSE)
        }
        when (distanceFilter) {
            SettingsLocationDistanceFilter.none -> minDistance = 0.0f
            SettingsLocationDistanceFilter.oneMeter -> minDistance = 1.0f
            SettingsLocationDistanceFilter.threeMeters -> minDistance = 3.0f
            SettingsLocationDistanceFilter.fiveMeters -> minDistance = 5.0f
            SettingsLocationDistanceFilter.tenMeters -> minDistance = 10.0f
            SettingsLocationDistanceFilter.twentyMeters -> minDistance = 20.0f
            SettingsLocationDistanceFilter.fiftyMeters -> minDistance = 50.0f
            SettingsLocationDistanceFilter.hundredMeters -> minDistance = 100.0f
            SettingsLocationDistanceFilter.twoHundredMeters -> minDistance = 200.0f
        }
        authorization.requestWhenInUseAuthorization()
        startUpdatingLocation()
        backgroundActivity.start()
    }

    open fun stop() {
        Log.d(TAG, "location: Stop")
        onUpdate = null
        manager.removeUpdates(this)
        backgroundActivity.stop()
    }

    open fun status(): String {
        val location = latestLocation ?: return ""
        return formatSpeed(location.speed.toDouble())
    }

    open fun getLatestKnownLocation(): AndroidLocation? = latestLocation

    private fun updateProvider(accuracy: Int) {
        provider = manager.getBestProvider(Criteria().apply { this.accuracy = accuracy }, true)
            ?: LocationManager.GPS_PROVIDER
    }

    private fun startUpdatingLocation() {
        val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            return
        }
        manager.requestLocationUpdates(provider, 0L, minDistance, this, Looper.getMainLooper())
    }

    open override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        Log.d(TAG, "location: Auth did change ${manager.authorizationStatus}")
        when (manager.authorizationStatus) {
            CLAuthorizationStatus.denied, CLAuthorizationStatus.restricted -> isDenied.value = true
            else -> isDenied.value = false
        }
        if (onUpdate != null &&
            (
                manager.authorizationStatus == CLAuthorizationStatus.authorizedWhenInUse ||
                    manager.authorizationStatus == CLAuthorizationStatus.authorizedAlways
                )
        ) {
            startUpdatingLocation()
        }
    }

    open override fun onProviderDisabled(provider: String) {
        Log.i(TAG, "location: Error, provider $provider disabled")
    }

    open override fun onProviderEnabled(provider: String) {
    }

    @Suppress("DEPRECATION")
    open override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {
    }

    open override fun onLocationChanged(location: AndroidLocation) {
        latestLocation = location
        onUpdate?.invoke(location)
    }

    private companion object {
        const val TAG = "Location"
    }
}
