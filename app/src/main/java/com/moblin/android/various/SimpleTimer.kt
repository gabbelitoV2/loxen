package com.moblin.android.various

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SimpleTimer(private val queue: CoroutineDispatcher) : AutoCloseable {
    private val scope = CoroutineScope(queue)
    private var timer: Job? = null

    fun startSingleShot(timeout: Double, handler: () -> Unit) {
        stop()
        timer = scope.launch {
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
        timer = scope.launch {
            delay(((initial ?: interval) * 1000.0).toLong())
            while (true) {
                handler()
                delay((interval * 1000.0).toLong())
            }
        }
    }

    fun stop() {
        timer?.cancel()
        timer = null
    }

    override fun close() {
        stop()
    }
}
