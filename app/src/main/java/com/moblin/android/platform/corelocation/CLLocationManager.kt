package com.moblin.android.platform.corelocation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.moblin.android.AppDelegate
import java.lang.ref.WeakReference

private const val TAG = "CLLocationManager"

enum class CLAuthorizationStatus {
    notDetermined,
    restricted,
    denied,
    authorizedAlways,
    authorizedWhenInUse,
}

interface CLLocationManagerDelegate {
    fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {}
}

class CLLocationManager {
    private var reportedStatus: CLAuthorizationStatus? = null
    private var registered = false

    var delegate: CLLocationManagerDelegate? = null
        set(value) {
            field = value
            if (value != null && !registered) {
                registered = true
                LocationAuthorization.add(this)
            }
        }

    val authorizationStatus: CLAuthorizationStatus
        get() = LocationAuthorization.status()

    fun requestWhenInUseAuthorization() {
        LocationAuthorization.requestWhenInUse()
    }

    internal fun reportAuthorizationIfChanged() {
        val delegate = delegate ?: return
        val status = authorizationStatus
        if (status == reportedStatus) {
            return
        }
        reportedStatus = status
        delegate.locationManagerDidChangeAuthorization(this)
    }
}

object LocationAuthorization {
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    private val handler = Handler(Looper.getMainLooper())
    private val managers = mutableListOf<WeakReference<CLLocationManager>>()
    private var activity: WeakReference<ComponentActivity>? = null
    private var requested = false
    private var requesting = false

    fun install(activity: ComponentActivity) {
        this.activity = WeakReference(activity)
        requesting = false
        activity.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> reportIfChanged()
                    Lifecycle.Event.ON_DESTROY -> {
                        if (this.activity?.get() === activity) {
                            this.activity = null
                            requesting = false
                        }
                    }
                    else -> Unit
                }
            },
        )
    }

    internal fun add(manager: CLLocationManager) {
        managers.removeAll { it.get() == null }
        managers.add(WeakReference(manager))
        handler.post { manager.reportAuthorizationIfChanged() }
    }

    internal fun status(): CLAuthorizationStatus {
        val activity = activity?.get()
        val context: Context = activity ?: applicationContext() ?: return CLAuthorizationStatus.notDetermined
        if (!isLocationEnabled(context)) {
            return CLAuthorizationStatus.denied
        }
        if (permissions.any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            return CLAuthorizationStatus.authorizedWhenInUse
        }
        if (requested || (activity != null && permissions.any { activity.shouldShowRequestPermissionRationale(it) })) {
            return CLAuthorizationStatus.denied
        }
        return CLAuthorizationStatus.notDetermined
    }

    internal fun requestWhenInUse() {
        if (requesting || status() != CLAuthorizationStatus.notDetermined) {
            return
        }
        val activity = activity?.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }
        var launcher: ActivityResultLauncher<Array<String>>? = null
        val registered = activity.activityResultRegistry.register(
            "CLLocationManager.requestWhenInUseAuthorization",
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            launcher?.unregister()
            requesting = false
            if (results.isNotEmpty()) {
                requested = true
            }
            reportIfChanged()
        }
        launcher = registered
        requesting = true
        try {
            registered.launch(permissions)
        } catch (error: RuntimeException) {
            Log.i(TAG, "Failed to request location permission: ${error.message}")
            requesting = false
            registered.unregister()
        }
    }

    private fun isLocationEnabled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.getSystemService(LocationManager::class.java)?.isLocationEnabled ?: true
            } else {
                @Suppress("DEPRECATION")
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_HIGH_ACCURACY,
                ) != Settings.Secure.LOCATION_MODE_OFF
            }
        } catch (error: RuntimeException) {
            true
        }
    }

    private fun reportIfChanged() {
        managers.removeAll { it.get() == null }
        for (manager in managers.mapNotNull { it.get() }) {
            manager.reportAuthorizationIfChanged()
        }
    }

    private fun applicationContext(): Context? = try {
        AppDelegate.context
    } catch (error: UninitializedPropertyAccessException) {
        null
    }

    internal fun reset() {
        managers.clear()
        activity = null
        requested = false
        requesting = false
    }
}
