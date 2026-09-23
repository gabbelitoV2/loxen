package com.moblin.android.various

import com.moblin.android.platform.core.DispatchSource
import com.moblin.android.platform.core.DispatchSourceTimer
import com.moblin.android.platform.core.DispatchTime
import kotlinx.coroutines.CoroutineDispatcher

class SimpleTimer(private val queue: CoroutineDispatcher) : AutoCloseable {
    private var timer: DispatchSourceTimer? = null

    fun startSingleShot(timeout: Double, handler: () -> Unit) {
        stop()
        timer = DispatchSource.makeTimerSource(queue = queue)
        timer!!.schedule(deadline = DispatchTime.now() + timeout)
        timer!!.setEventHandler(handler = handler)
        timer!!.activate()
    }

    fun startPeriodic(interval: Double, initial: Double? = null, handler: () -> Unit) {
        stop()
        timer = DispatchSource.makeTimerSource(queue = queue)
        timer!!.schedule(deadline = DispatchTime.now() + (initial ?: interval), repeating = interval)
        timer!!.setEventHandler(handler = handler)
        timer!!.activate()
    }

    fun stop() {
        timer?.cancel()
        timer = null
    }

    override fun close() {
        stop()
    }
}
