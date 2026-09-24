package com.moblin.android.platform.combine

import android.util.Log
import com.moblin.android.platform.offscreen.OffscreenSweep
import com.moblin.android.platform.offscreen.isOffscreenMainThread
import com.moblin.android.platform.offscreen.offscreenMainHandler
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private const val TAG = "MoblinOverlay"

private val combineLogged = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

private fun logCombineOnce(message: String) {
    if (combineLogged.add(message)) {
        Log.w(TAG, message)
    }
}

private object CombineScope {
    val scope: CoroutineScope by lazy {
        val dispatcher: CoroutineDispatcher = runCatching {
            Dispatchers.Main.immediate.also { it.isDispatchNeeded(EmptyCoroutineContext) }
        }.getOrElse { Dispatchers.Unconfined }
        CoroutineScope(
            dispatcher + SupervisorJob() + CoroutineExceptionHandler { _, error ->
                logCombineOnce("subscription failed: $error")
            },
        )
    }
}

class AnyCancellable internal constructor(onCancel: () -> Unit) {
    private val cancelled = AtomicBoolean(false)
    private var onCancel: (() -> Unit)? = onCancel
    private val sweepToken: Any? = runCatching { OffscreenSweep.track(this, onCancel) }.getOrNull()

    fun cancel() {
        if (!cancelled.compareAndSet(false, true)) {
            return
        }
        sweepToken?.let { OffscreenSweep.untrack(it) }
        val action = onCancel
        onCancel = null
        try {
            action?.invoke()
        } catch (error: Throwable) {
            logCombineOnce("cancel failed: $error")
        }
    }

    fun store(into: MutableCollection<AnyCancellable>) {
        into.add(this)
    }
}

class ObservableObjectPublisher {
    private class Subscriber(val receiveValue: () -> Unit)

    private val subscribers = CopyOnWriteArrayList<Subscriber>()

    internal val hasSubscribers: Boolean
        get() = subscribers.isNotEmpty()

    @Volatile
    internal var onSubscribe: (() -> Unit)? = null

    @Volatile
    internal var onUnsubscribe: (() -> Unit)? = null

    fun sink(receiveValue: () -> Unit): AnyCancellable {
        val subscriber = Subscriber(receiveValue)
        subscribers.add(subscriber)
        try {
            onSubscribe?.invoke()
        } catch (error: Throwable) {
            logCombineOnce("objectWillChange subscribe hook failed: $error")
        }
        val publisher = WeakReference(this)
        return AnyCancellable { publisher.get()?.remove(subscriber) }
    }

    private fun remove(subscriber: Subscriber) {
        if (!subscribers.remove(subscriber) || subscribers.isNotEmpty()) {
            return
        }
        try {
            onUnsubscribe?.invoke()
        } catch (error: Throwable) {
            logCombineOnce("objectWillChange unsubscribe hook failed: $error")
        }
    }

    fun send() {
        if (!isOffscreenMainThread()) {
            offscreenMainHandler.post { send() }
            return
        }
        for (subscriber in subscribers) {
            try {
                subscriber.receiveValue()
            } catch (error: Throwable) {
                logCombineOnce("objectWillChange subscriber failed: $error")
            }
        }
    }
}

fun <T> Flow<T>.sink(receiveValue: (T) -> Unit): AnyCancellable {
    val flow = this
    val job = CombineScope.scope.launch {
        flow.collect { value ->
            try {
                receiveValue(value)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                logCombineOnce("sink subscriber failed: $error")
            }
        }
    }
    return AnyCancellable { job.cancel() }
}

fun <T> Flow<T>.dropFirst(count: Int = 1): Flow<T> = drop(count)
