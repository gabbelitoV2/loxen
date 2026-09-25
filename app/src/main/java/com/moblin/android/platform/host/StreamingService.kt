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
import com.moblin.android.platform.capture.Camera2Engine

class StreamingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val types = intent?.getIntExtra(EXTRA_TYPES, 0) ?: 0
        try {
            synchronized(lock) {
                val notification = activityNotification ?: makeNotification(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && types != 0) {
                    startForeground(NOTIFICATION_ID, notification, types)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                isForeground = true
            }
            isRunning = true
            Camera2Engine.isForegroundServiceRunning = (types and ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA) != 0 ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.R
            Log.i(TAG, "Foreground service started with types $types")
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to start foreground service", error)
            synchronized(lock) {
                isForeground = false
            }
            isRunning = false
            Camera2Engine.isForegroundServiceRunning = false
            stopSelf()
        }
        return START_NOT_STICKY
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

    companion object {
        private const val TAG = "StreamingService"
        internal const val CHANNEL_ID = "moblin-streaming"
        internal const val NOTIFICATION_ID = 4711
        private const val EXTRA_TYPES = "types"
        private val lock = Any()
        private var instance: StreamingService? = null
        private var isForeground = false
        private var activityNotification: Notification? = null

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

        private fun makeNotification(context: Context): Notification {
            createNotificationChannel(context)
            val builder = Notification.Builder(context, CHANNEL_ID)
                .setContentTitle("Moblin")
                .setContentText("Live or recording")
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
                    isForeground -> manager.notify(NOTIFICATION_ID, makeNotification(context))
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
            val types = foregroundServiceTypes(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && types == 0) {
                Log.i(TAG, "Not starting foreground service without camera or microphone permission")
                return
            }
            val intent = Intent(context, StreamingService::class.java).putExtra(EXTRA_TYPES, types)
            try {
                context.startForegroundService(intent)
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to start foreground service", error)
            }
        }

        fun stop(context: Context) {
            synchronized(lock) {
                if (isForeground && activityNotification != null) {
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

        private fun foregroundServiceTypes(context: Context): Int {
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
