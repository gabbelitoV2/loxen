package com.moblin.android.various

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainTimer {
    private var timer: Job? = null

    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun startSingleShot(timeout: Double, handler: () -> Unit) {
        stop()
        timer = mainScope.launch {
            delay((timeout * 1000.0).toLong())
            handler()
        }
    }

    fun startPeriodic(
        interval: Double,
        initial: Double? = null,
        handler: () -> Unit
    ) {
        stop()
        timer = mainScope.launch {
            delay(((initial ?: interval) * 1000.0).toLong())
            handler()
            while (true) {
                delay((interval * 1000.0).toLong())
                handler()
            }
        }
    }

    fun stop() {
        timer?.cancel()
        timer = null
    }
}
