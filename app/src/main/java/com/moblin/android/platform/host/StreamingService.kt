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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val types = intent?.getIntExtra(EXTRA_TYPES, 0) ?: 0
        try {
            val notification = makeNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && types != 0) {
                startForeground(NOTIFICATION_ID, notification, types)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isRunning = true
            Camera2Engine.isForegroundServiceRunning = (types and ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA) != 0 ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.R
            Log.i(TAG, "Foreground service started with types $types")
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to start foreground service", error)
            isRunning = false
            Camera2Engine.isForegroundServiceRunning = false
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        Camera2Engine.isForegroundServiceRunning = false
        Log.i(TAG, "Foreground service stopped")
        super.onDestroy()
    }

    private fun makeNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager?.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(CHANNEL_ID, "Streaming", NotificationManager.IMPORTANCE_LOW)
            channel.setShowBadge(false)
            manager?.createNotificationChannel(channel)
        }
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Moblin")
            .setContentText("Live or recording")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            builder.setContentIntent(
                PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE)
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    companion object {
        private const val TAG = "StreamingService"
        private const val CHANNEL_ID = "moblin-streaming"
        private const val NOTIFICATION_ID = 4711
        private const val EXTRA_TYPES = "types"

        @Volatile
        var isRunning = false
            private set

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
