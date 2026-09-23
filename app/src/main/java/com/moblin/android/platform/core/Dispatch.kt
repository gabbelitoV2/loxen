package com.moblin.android.platform.core

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "MoblinPipeline"

class DispatchTimeInterval private constructor(val nanoseconds: Long) {
    companion object {
        fun seconds(value: Int): DispatchTimeInterval = DispatchTimeInterval(value * 1_000_000_000L)
        fun milliseconds(value: Int): DispatchTimeInterval = DispatchTimeInterval(value * 1_000_000L)
        fun microseconds(value: Int): DispatchTimeInterval = DispatchTimeInterval(value * 1_000L)
        fun nanoseconds(value: Int): DispatchTimeInterval = DispatchTimeInterval(value.toLong())
    }
}

class DispatchTime(val uptimeNanoseconds: Long) : Comparable<DispatchTime> {
    operator fun plus(seconds: Double): DispatchTime {
        return DispatchTime(uptimeNanoseconds + (seconds * 1_000_000_000.0).toLong())
    }

    operator fun plus(interval: DispatchTimeInterval): DispatchTime {
        return DispatchTime(uptimeNanoseconds + interval.nanoseconds)
    }

    operator fun minus(seconds: Double): DispatchTime {
        return DispatchTime(uptimeNanoseconds - (seconds * 1_000_000_000.0).toLong())
    }

    override fun compareTo(other: DispatchTime): Int {
        return uptimeNanoseconds.compareTo(other.uptimeNanoseconds)
    }

    override fun equals(other: Any?): Boolean {
        return other is DispatchTime && other.uptimeNanoseconds == uptimeNanoseconds
    }

    override fun hashCode(): Int {
        return uptimeNanoseconds.hashCode()
    }

    companion object {
        fun now(): DispatchTime {
            return DispatchTime(System.nanoTime())
        }
    }
}

interface DispatchSourceTimer {
    val isCancelled: Boolean
        get() = false

    fun schedule(deadline: DispatchTime, repeating: Double = -1.0)

    fun setEventHandler(handler: () -> Unit)

    fun activate()

    fun resume() {
        activate()
    }

    fun cancel()
}

object DispatchSource {
    fun makeTimerSource(queue: CoroutineDispatcher): DispatchSourceTimer {
        return FixedRateDispatchSourceTimer(queue)
    }
}

private class FixedRateDispatchSourceTimer(queue: CoroutineDispatcher) : DispatchSourceTimer {
    private val lock = Any()
    private val scope = CoroutineScope(queue + SupervisorJob())
    private var deadlineNs = Long.MAX_VALUE
    private var intervalNs = -1L
    private var handler: (() -> Unit)? = null
    private var active = false
    private var generation = 0L
    private var job: Job? = null
    private var lastFailureLogNs = 0L

    @Volatile
    private var cancelled = false

    override val isCancelled: Boolean
        get() = cancelled

    override fun schedule(deadline: DispatchTime, repeating: Double) {
        synchronized(lock) {
            deadlineNs = deadline.uptimeNanoseconds
            intervalNs = if (repeating > 0.0) {
                maxOf((repeating * 1_000_000_000.0).toLong(), 1L)
            } else {
                -1L
            }
            if (active && !cancelled) {
                startLocked()
            }
        }
    }

    override fun setEventHandler(handler: () -> Unit) {
        synchronized(lock) {
            this.handler = handler
        }
    }

    override fun activate() {
        synchronized(lock) {
            if (active || cancelled) {
                return
            }
            active = true
            startLocked()
        }
    }

    override fun cancel() {
        synchronized(lock) {
            cancelled = true
            generation += 1
            job?.cancel()
            job = null
            handler = null
        }
    }

    private fun startLocked() {
        generation += 1
        job?.cancel()
        job = null
        if (deadlineNs == Long.MAX_VALUE) {
            return
        }
        val runGeneration = generation
        val startNs = deadlineNs
        val periodNs = intervalNs
        job = scope.launch {
            runLoop(runGeneration, startNs, periodNs)
        }
    }

    private suspend fun runLoop(runGeneration: Long, startNs: Long, periodNs: Long) {
        var fireIndex = 0L
        while (true) {
            val targetNs = startNs + fireIndex * periodNs.coerceAtLeast(0L)
            val remainingNs = targetNs - System.nanoTime()
            if (remainingNs > 0) {
                delay((remainingNs + 999_999L) / 1_000_000L)
            }
            val currentHandler = synchronized(lock) {
                if (cancelled || generation != runGeneration) {
                    null
                } else {
                    handler
                }
            }
            if (cancelled || generation != runGeneration) {
                return
            }
            if (currentHandler != null) {
                fire(currentHandler)
            }
            if (periodNs <= 0) {
                return
            }
            fireIndex += 1
            val nowNs = System.nanoTime()
            if (nowNs - (startNs + fireIndex * periodNs) > periodNs) {
                fireIndex = (nowNs - startNs) / periodNs
            }
        }
    }

    private fun fire(handler: () -> Unit) {
        try {
            handler()
        } catch (error: Throwable) {
            val nowNs = System.nanoTime()
            if (lastFailureLogNs == 0L || nowNs - lastFailureLogNs > 10_000_000_000L) {
                lastFailureLogNs = nowNs
                Log.e(TAG, "Timer handler failed", error)
            }
        }
    }
}
