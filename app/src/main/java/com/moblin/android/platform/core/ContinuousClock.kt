package com.moblin.android.platform.core

import android.os.SystemClock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

object ContinuousClock {
    private val source: () -> Long = if (SystemClock.elapsedRealtimeNanos() != 0L) {
        { SystemClock.elapsedRealtimeNanos() }
    } else {
        { System.nanoTime() }
    }

    val now: Instant
        get() = Instant(source())

    class Instant(val nanoseconds: Long) : Comparable<Instant> {
        fun advanced(by: Duration): Instant = Instant(nanoseconds + by.inWholeNanoseconds)

        fun advanced(bySeconds: Double): Instant = Instant(nanoseconds + (bySeconds * 1_000_000_000.0).toLong())

        fun duration(to: Instant): Duration = (to.nanoseconds - nanoseconds).nanoseconds

        operator fun minus(other: Instant): Duration = (nanoseconds - other.nanoseconds).nanoseconds

        operator fun plus(d: Duration): Instant = advanced(by = d)

        override fun compareTo(other: Instant): Int = nanoseconds.compareTo(other.nanoseconds)

        override fun equals(other: Any?): Boolean = other is Instant && other.nanoseconds == nanoseconds

        override fun hashCode(): Int = nanoseconds.hashCode()

        override fun toString(): String = "ContinuousClock.Instant(${nanoseconds}ns)"
    }
}
