package com.moblin.android.platform.host

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.moblin.android.AppDelegate
import com.moblin.android.platform.capture.Camera2Engine

class StreamingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        synchronized(lock) {
            instance = this
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!enterForeground(fallbackTypes = intent?.getIntExtra(EXTRA_TYPES, 0) ?: 0)) {
            stopSelf()
            return START_NOT_STICKY
        }
        isRunning = true
        if (intent?.getBooleanExtra(EXTRA_REQUESTED, false) == true) {
            stopWhenUnneeded(this)
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        SystemEvents.applicationTaskRemoved(applicationContext)
    }

    override fun onDestroy() {
        synchronized(lock) {
            isForeground = false
            if (instance === this) {
                instance = null
            }
        }
        isRunning = false
        Camera2Engine.isForegroundServiceRunning = false
        Log.i(TAG, "Foreground service stopped")
        super.onDestroy()
    }

    private fun enterForeground(fallbackTypes: Int = 0): Boolean {
        return try {
            synchronized(lock) {
                val base = foregroundTypes().takeIf { it != 0 } ?: fallbackTypes
                val types = if (base == 0) 0 else base or locationTypes(this)
                val location = types and locationType
                try {
                    startForeground(types)
                } catch (error: SecurityException) {
                    if (location == 0) {
                        throw error
                    }
                    Log.i(TAG, "Foreground service cannot use location now: ${error.message}")
                    startForeground(types and location.inv())
                }
                isForeground = true
                Camera2Engine.isForegroundServiceRunning =
                    (types and ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA) != 0 ||
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && Reason.streaming in reasons)
                Log.i(TAG, "Foreground service started with types $types for ${reasons.keys}")
            }
            true
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to start foreground service", error)
            false
        }
    }

    private fun startForeground(types: Int) {
        val notification = activityNotification ?: makeNotification(this, types)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && types != 0) {
            startForeground(NOTIFICATION_ID, notification, types)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private enum class Reason {
        streaming,
        background,
    }

    companion object {
        private const val TAG = "StreamingService"
        internal const val CHANNEL_ID = "moblin-streaming"
        internal const val NOTIFICATION_ID = 4711
        private const val EXTRA_REQUESTED = "requested"
        private const val EXTRA_TYPES = "types"
        private val lock = Any()
        private var instance: StreamingService? = null
        private var isForeground = false
        private var activityNotification: Notification? = null
        private val reasons = mutableMapOf<Reason, Int>()
        private var locationSessions = 0
        private val locationType: Int
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0

        @Volatile
        var isRunning = false
            private set

        internal fun createNotificationChannel(context: Context): NotificationManager? {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return null
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(CHANNEL_ID, "Streaming", NotificationManager.IMPORTANCE_LOW)
                channel.setShowBadge(false)
                manager.createNotificationChannel(channel)
            }
            return manager
        }

        internal fun launchIntent(context: Context): PendingIntent? {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        private fun foregroundTypes(): Int {
            return reasons.values.fold(0) { types, reasonTypes -> types or reasonTypes }
        }

        private fun makeNotification(context: Context, types: Int): Notification {
            createNotificationChannel(context)
            val captureTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            val background = when {
                Reason.streaming in reasons -> false
                Reason.background in reasons -> true
                else -> types != 0 && (types and captureTypes) == 0
            }
            val text = if (background) {
                "Running in background"
            } else {
                "Live or recording"
            }
            val builder = Notification.Builder(context, CHANNEL_ID)
                .setContentTitle("Moblin")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.presence_video_online)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
            launchIntent(context)?.let { builder.setContentIntent(it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            }
            return builder.build()
        }

        internal fun showActivityNotification(context: Context, notification: Notification?) {
            synchronized(lock) {
                val ongoing = notification?.takeIf { (it.flags and Notification.FLAG_ONGOING_EVENT) != 0 }
                activityNotification = ongoing
                val manager = createNotificationChannel(context) ?: return
                when {
                    ongoing != null -> manager.notify(NOTIFICATION_ID, ongoing)
                    isForeground -> manager.notify(NOTIFICATION_ID, makeNotification(context, foregroundTypes()))
                    notification != null -> manager.notify(NOTIFICATION_ID, notification)
                    else -> manager.cancel(NOTIFICATION_ID)
                }
            }
        }

        internal fun cancelStaleNotification(context: Context) {
            synchronized(lock) {
                if (!isForeground && activityNotification == null) {
                    context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
                }
            }
        }

        fun start(context: Context) {
            val types = streamingTypes(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && types == 0) {
                Log.i(TAG, "Not starting foreground service without camera or microphone permission")
                return
            }
            request(context, Reason.streaming, types)
        }

        fun stop(context: Context) {
            release(context, Reason.streaming)
        }

        fun startBackground(
            chat: Boolean,
            printing: Boolean,
            moblinkRelay: Boolean,
            context: Context = AppDelegate.context,
        ) {
            if (!chat && !printing && !moblinkRelay) {
                return
            }
            request(context, Reason.background, backgroundTypes(chat, printing, moblinkRelay))
        }

        fun stopBackground(context: Context = AppDelegate.context) {
            release(context, Reason.background)
        }

        fun startLocation() {
            val running = synchronized(lock) {
                locationSessions += 1
                instance?.takeIf { isForeground && reasons.isNotEmpty() && locationSessions == 1 }
            }
            running?.enterForeground()
        }

        fun stopLocation() {
            val running = synchronized(lock) {
                if (locationSessions == 0) {
                    return
                }
                locationSessions -= 1
                instance?.takeIf { isForeground && reasons.isNotEmpty() && locationSessions == 0 }
            }
            running?.enterForeground()
        }

        internal fun locationTypes(context: Context): Int {
            if (locationSessions == 0 || locationType == 0) {
                return 0
            }
            val permitted = isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                isGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (!permitted) {
                return 0
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                !isGranted(context, Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            ) {
                return 0
            }
            return locationType
        }

        internal fun stopAll(context: Context) {
            synchronized(lock) {
                reasons.clear()
                activityNotification = null
            }
            stopWhenUnneeded(context)
            context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        }

        internal fun backgroundTypes(chat: Boolean, printing: Boolean, moblinkRelay: Boolean): Int {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return 0
            }
            var types = 0
            if (printing || moblinkRelay) {
                types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            }
            if (chat) {
                types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            }
            return types
        }

        private fun request(context: Context, reason: Reason, types: Int) {
            val running = synchronized(lock) {
                reasons[reason] = types
                instance?.takeIf { isForeground }
            }
            if (running != null) {
                running.enterForeground()
                return
            }
            val intent = Intent(context, StreamingService::class.java)
                .putExtra(EXTRA_REQUESTED, true)
                .putExtra(EXTRA_TYPES, types)
            try {
                context.startForegroundService(intent)
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to start foreground service for $reason", error)
                synchronized(lock) {
                    reasons.remove(reason)
                }
            }
        }

        private fun release(context: Context, reason: Reason) {
            val running = synchronized(lock) {
                reasons.remove(reason)
                instance?.takeIf { isForeground && reasons.isNotEmpty() }
            }
            if (running != null) {
                running.enterForeground()
            } else {
                stopWhenUnneeded(context)
            }
        }

        private fun stopWhenUnneeded(context: Context) {
            synchronized(lock) {
                if (reasons.isNotEmpty() || !isForeground) {
                    return
                }
                if (activityNotification != null) {
                    instance?.stopForeground(Service.STOP_FOREGROUND_DETACH)
                }
                isForeground = false
            }
            try {
                context.stopService(Intent(context, StreamingService::class.java))
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to stop foreground service", error)
            }
        }

        private fun streamingTypes(context: Context): Int {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                return 0
            }
            var types = 0
            if (isGranted(context, Manifest.permission.CAMERA) &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                    isGranted(context, Manifest.permission.FOREGROUND_SERVICE_CAMERA))
            ) {
                types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            }
            if (isGranted(context, Manifest.permission.RECORD_AUDIO) &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                    isGranted(context, Manifest.permission.FOREGROUND_SERVICE_MICROPHONE))
            ) {
                types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            return types
        }

        private fun isGranted(context: Context, permission: String): Boolean {
            return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}
