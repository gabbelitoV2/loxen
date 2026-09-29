package com.moblin.android.various

import com.moblin.android.platform.log.Log
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
            runCatching { handler() }.onFailure { Log.e("MainTimer", "Timer handler failed", it) }
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
            runCatching { handler() }.onFailure { Log.e("MainTimer", "Timer handler failed", it) }
            while (true) {
                delay((interval * 1000.0).toLong())
                runCatching { handler() }.onFailure { Log.e("MainTimer", "Timer handler failed", it) }
            }
        }
    }

    fun stop() {
        timer?.cancel()
        timer = null
    }
}
