package com.moblin.android.platform.host

import android.app.Activity
import com.moblin.android.platform.capture.Camera2Engine
import com.moblin.android.various.model.Model
import java.util.Collections
import java.util.WeakHashMap

internal object SystemEventsState {
    fun reset() {
        set(SystemEvents::class.java, "installed", false)
        set(SystemEvents::class.java, "model", null)
        set(SystemEvents::class.java, "startedActivities", 0)
        set(SystemEvents::class.java, "resumedActivities", 0)
        set(SystemEvents::class.java, "hasEnteredBackground", false)
        set(SystemEvents::class.java, "hasTerminated", false)
        set(SystemEvents::class.java, "resumedActivity", null)
        set(SystemEvents::class.java, "aliveActivities", Collections.newSetFromMap(WeakHashMap<Activity, Boolean>()))
        set(Camera2Engine::class.java, "isInBackground", false)
    }

    fun bind(model: Model?) {
        set(SystemEvents::class.java, "model", model)
    }

    fun setInBackground(inBackground: Boolean) {
        set(SystemEvents::class.java, "hasEnteredBackground", inBackground)
    }

    private fun set(owner: Class<*>, name: String, value: Any?) {
        val field = owner.getDeclaredField(name)
        field.isAccessible = true
        field.set(null, value)
    }
}
