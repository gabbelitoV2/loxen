package com.moblin.android.platform.core

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Process
import android.util.Log
import com.moblin.android.platform.video.EglCore
import com.moblin.android.platform.video.PixelBufferTurn
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.android.asCoroutineDispatcher

private const val TAG = "MoblinPipeline"

class PipelineTimeoutException(message: String) : RuntimeException(message)

internal class ResilientLooperThread(name: String, private val threadPriority: Int) : Thread(name) {
    private val ready = CountDownLatch(1)

    @Volatile
    private var preparedLooper: Looper? = null

    override fun run() {
        try {
            Process.setThreadPriority(threadPriority)
        } catch (error: Throwable) {
            Log.w(TAG, "Failed to set priority $threadPriority on $name: $error")
        }
        try {
            Looper.prepare()
            preparedLooper = Looper.myLooper()
        } finally {
            ready.countDown()
        }
        while (true) {
            try {
                Looper.loop()
                return
            } catch (error: Throwable) {
                Log.e(TAG, "Uncaught failure on $name, continuing", error)
            }
        }
    }

    fun awaitLooper(): Looper? {
        ready.await()
        return preparedLooper
    }
}

private class PipelineHandler(looper: Looper) : Handler(looper) {
    override fun dispatchMessage(msg: Message) {
        try {
            super.dispatchMessage(msg)
        } finally {
            PixelBufferTurn.end()
        }
    }
}

object PipelineThread {
    const val NAME = "com.haishinkit.HaishinKit.Processor.Pipeline"

    private val thread = ResilientLooperThread(NAME, Process.THREAD_PRIORITY_URGENT_DISPLAY).apply {
        isDaemon = true
        start()
    }

    val looper: Looper? = thread.awaitLooper()

    val handler: Handler = PipelineHandler(looper ?: Looper.getMainLooper())

    val dispatcher: CoroutineDispatcher = handler.asCoroutineDispatcher(NAME)

    init {
        handler.post {
            EglCore.setUp()
        }
    }

    fun isCurrent(): Boolean {
        return Thread.currentThread() === thread
    }

    fun post(block: () -> Unit) {
        handler.post {
            runGuarded(block)
        }
    }

    fun postDelayed(delayMs: Long, block: () -> Unit) {
        handler.postDelayed({ runGuarded(block) }, delayMs)
    }

    fun <T> runSync(timeoutMs: Long = 2000, block: () -> T): T {
        if (isCurrent()) {
            return block()
        }
        val latch = CountDownLatch(1)
        val result = AtomicReference<Result<T>?>(null)
        val posted = handler.post {
            result.set(runCatching(block))
            latch.countDown()
        }
        if (!posted) {
            throw PipelineTimeoutException("Failed to post to $NAME")
        }
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            Log.e(TAG, "runSync timed out after $timeoutMs ms on ${Thread.currentThread().name}")
            throw PipelineTimeoutException("runSync timed out after $timeoutMs ms")
        }
        return result.get()!!.getOrThrow()
    }

    private fun runGuarded(block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            Log.e(TAG, "Posted block failed", error)
        }
    }
}
