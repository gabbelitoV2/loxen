package com.moblin.android.platform.corebluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.moblin.android.AppDelegate
import java.lang.ref.WeakReference

private const val TAG = "CoreBluetooth"

internal fun bluetoothPermissions(sdkInt: Int): Array<String> {
    return if (sdkInt >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

object BluetoothAuthorization {
    private val handler = Handler(Looper.getMainLooper())
    private val managers = mutableListOf<WeakReference<CBCentralManager>>()
    private var activity: WeakReference<ComponentActivity>? = null
    private var receiverContext: Context? = null
    private var requested = false
    private var requesting = false
    private var wanted = false

    private val adapterStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                reportIfChanged()
            }
        }
    }

    internal val permissions: Array<String>
        get() = bluetoothPermissions(Build.VERSION.SDK_INT)

    fun install(activity: ComponentActivity) {
        this.activity = WeakReference(activity)
        requesting = false
        registerReceiver(activity.applicationContext)
        activity.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        reportIfChanged()
                        requestIfWanted()
                    }
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

    internal fun add(manager: CBCentralManager) {
        synchronized(managers) {
            managers.removeAll { it.get() == null }
            managers.add(WeakReference(manager))
        }
        handler.post {
            applicationContext()?.let { registerReceiver(it) }
            if (authorization() == CBManagerAuthorization.notDetermined) {
                wanted = true
                requestIfWanted()
            }
            manager.update(state())
        }
    }

    internal fun remove(manager: CBCentralManager) {
        synchronized(managers) {
            managers.removeAll { it.get() == null || it.get() === manager }
        }
    }

    internal fun applicationContext(): Context? = try {
        AppDelegate.context
    } catch (error: UninitializedPropertyAccessException) {
        null
    }

    internal fun adapter(): BluetoothAdapter? {
        val context = applicationContext() ?: return null
        return try {
            context.getSystemService(BluetoothManager::class.java)?.adapter
        } catch (error: RuntimeException) {
            null
        }
    }

    internal fun isAllowed(): Boolean {
        val context = applicationContext() ?: return false
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    internal fun authorization(): CBManagerAuthorization {
        if (isAllowed()) {
            return CBManagerAuthorization.allowedAlways
        }
        val activity = activity?.get()
        if (requested || (activity != null && permissions.any { activity.shouldShowRequestPermissionRationale(it) })) {
            return CBManagerAuthorization.denied
        }
        return CBManagerAuthorization.notDetermined
    }

    internal fun state(): CBManagerState {
        val context = applicationContext() ?: return CBManagerState.unknown
        val adapter = adapter() ?: return CBManagerState.unsupported
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return CBManagerState.unsupported
        }
        if (!isAllowed()) {
            return CBManagerState.unauthorized
        }
        val adapterState = bluetoothCall(BluetoothAdapter.STATE_OFF) { adapter.state }
        return if (adapterState == BluetoothAdapter.STATE_ON) {
            CBManagerState.poweredOn
        } else {
            CBManagerState.poweredOff
        }
    }

    private fun requestIfWanted() {
        if (!wanted || requesting) {
            return
        }
        if (authorization() != CBManagerAuthorization.notDetermined) {
            wanted = false
            return
        }
        val activity = activity?.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }
        if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            return
        }
        var launcher: ActivityResultLauncher<Array<String>>? = null
        val registered = activity.activityResultRegistry.register(
            "CBCentralManager.authorization",
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            launcher?.unregister()
            requesting = false
            if (results.isNotEmpty()) {
                requested = true
                wanted = false
            }
            reportIfChanged()
        }
        if (!wanted) {
            registered.unregister()
            return
        }
        launcher = registered
        requesting = true
        try {
            registered.launch(permissions)
        } catch (error: RuntimeException) {
            Log.i(TAG, "Failed to request the Bluetooth permission: ${error.message}")
            requesting = false
            registered.unregister()
        }
    }

    private fun reportIfChanged() {
        val state = state()
        val current = synchronized(managers) {
            managers.removeAll { it.get() == null }
            managers.mapNotNull { it.get() }
        }
        for (manager in current) {
            manager.update(state)
        }
    }

    private fun registerReceiver(context: Context) {
        if (receiverContext != null) {
            return
        }
        val appContext = context.applicationContext ?: context
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(adapterStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                appContext.registerReceiver(adapterStateReceiver, filter)
            }
            receiverContext = appContext
        } catch (error: RuntimeException) {
            Log.i(TAG, "Failed to observe the Bluetooth state: ${error.message}")
        }
    }

    internal fun reset() {
        handler.removeCallbacksAndMessages(null)
        receiverContext?.let { context ->
            runCatching { context.unregisterReceiver(adapterStateReceiver) }
        }
        receiverContext = null
        synchronized(managers) {
            managers.clear()
        }
        activity = null
        requested = false
        requesting = false
        wanted = false
    }
}
