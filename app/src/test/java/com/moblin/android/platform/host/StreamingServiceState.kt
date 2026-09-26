package com.moblin.android.platform.host

import com.moblin.android.platform.capture.Camera2Engine

internal object StreamingServiceState {
    fun reset() {
        (get("reasons") as MutableMap<*, *>).clear()
        set("instance", null)
        set("isForeground", false)
        set("activityNotification", null)
        set("isRunning", false)
        set("locationSessions", 0)
        Camera2Engine.isForegroundServiceRunning = false
    }

    private fun get(name: String): Any? {
        val field = StreamingService::class.java.getDeclaredField(name)
        field.isAccessible = true
        return field.get(null)
    }

    private fun set(name: String, value: Any?) {
        val field = StreamingService::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(null, value)
    }
}
