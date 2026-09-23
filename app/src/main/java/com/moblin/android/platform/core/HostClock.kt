package com.moblin.android.platform.core

object HostClock {
    fun nowUs(): Long {
        return System.nanoTime() / 1000
    }

    fun nowNs(): Long {
        return System.nanoTime()
    }
}
