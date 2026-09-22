package com.moblin.android.media.haishinkit.util

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Atomic<A>(initialValue: A) {
    private val lock = ReentrantLock()

    private val state = MutableRef(initialValue)

    val value: A
        get() = lock.withLock { state.value }

    fun <R> mutate(transform: (MutableRef<A>) -> R): R {
        return lock.withLock {
            transform(state)
        }
    }

    class MutableRef<T>(var value: T)
}
