package com.moblin.android.platform.video

import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.core.PipelineThread
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "MoblinPipeline"

internal const val UNLEASED = -1

internal object PixelBufferLeases {
    fun retain(buffer: CVPixelBuffer, site: String = "retain"): Boolean {
        val count = buffer.leaseCount
        while (true) {
            val value = count.get()
            if (value == UNLEASED) {
                return true
            }
            if (value <= 0) {
                PixelBufferStale.report(buffer, site)
                return false
            }
            if (count.compareAndSet(value, value + 1)) {
                return true
            }
        }
    }

    fun release(buffer: CVPixelBuffer, site: String = "release") {
        if (buffer.leaseCount.get() == UNLEASED) {
            return
        }
        if (PipelineThread.isCurrent()) {
            PixelBufferTurn.defer(buffer)
        } else {
            releaseNow(buffer, site)
        }
    }

    fun releaseNow(buffer: CVPixelBuffer, site: String) {
        val count = buffer.leaseCount
        while (true) {
            val value = count.get()
            if (value == UNLEASED) {
                return
            }
            if (value <= 0) {
                PixelBufferStale.reportOverRelease(buffer, site)
                return
            }
            if (count.compareAndSet(value, value - 1)) {
                if (value == 1) {
                    PixelBufferReaper.recycle(buffer)
                }
                return
            }
        }
    }
}

internal object PixelBufferTurn {
    private var pending = ArrayList<CVPixelBuffer>()
    private var draining = ArrayList<CVPixelBuffer>()

    fun defer(buffer: CVPixelBuffer) {
        pending.add(buffer)
    }

    fun pendingCount(state: PixelBufferPoolState): Int {
        var count = 0
        for (buffer in pending) {
            if (buffer.poolState === state) {
                count += 1
            }
        }
        return count
    }

    fun end() {
        if (pending.isEmpty() || !PipelineThread.isCurrent()) {
            return
        }
        val buffers = pending
        pending = draining
        draining = buffers
        for (buffer in buffers) {
            PixelBufferLeases.releaseNow(buffer, "end of pipeline turn")
        }
        buffers.clear()
    }
}

internal object PixelBufferStale {
    private const val MAXIMUM_LOGGED_SITES = 256
    private val loggedSites = ConcurrentHashMap.newKeySet<String>()

    fun report(buffer: CVPixelBuffer, site: String) {
        PipelineStats.increment("staleBuffer")
        log(buffer, "Stale pixel buffer used by $site", site)
    }

    fun reportOverRelease(buffer: CVPixelBuffer, site: String) {
        PipelineStats.increment("leaseOverRelease")
        log(buffer, "Pixel buffer lease released too many times by $site", "over-release $site")
    }

    private fun log(buffer: CVPixelBuffer, message: String, site: String) {
        val trace = Throwable(message)
        val caller = trace.stackTrace.firstOrNull { element ->
            val name = element.className
            !name.startsWith("com.moblin.android.platform.") &&
                !name.startsWith("java.") &&
                !name.startsWith("kotlin.") &&
                !name.startsWith("kotlinx.")
        }
        val key = "$site ${caller?.className}.${caller?.methodName}:${caller?.lineNumber}"
        if (loggedSites.size >= MAXIMUM_LOGGED_SITES || !loggedSites.add(key)) {
            return
        }
        Log.w(
            TAG,
            "$message: $buffer, handle generation ${buffer.generation}, texture generation " +
                "${buffer.backing.generation}",
            trace,
        )
    }
}

fun <T : MediaSample?> retainLease(sample: T): T {
    val buffer = sample?.imageBuffer ?: return sample
    PixelBufferLeases.retain(buffer, "retainLease")
    return sample
}

fun <T : CVPixelBuffer?> retainLease(buffer: T): T {
    if (buffer != null) {
        PixelBufferLeases.retain(buffer, "retainLease")
    }
    return buffer
}

fun releaseLease(sample: MediaSample?) {
    val buffer = sample?.imageBuffer ?: return
    PixelBufferLeases.release(buffer, "releaseLease")
}

fun releaseLease(buffer: CVPixelBuffer?) {
    if (buffer != null) {
        PixelBufferLeases.release(buffer, "releaseLease")
    }
}

fun swapLease(old: MediaSample?, new: MediaSample?): MediaSample? {
    if (old === new) {
        return new
    }
    retainLease(new)
    releaseLease(old)
    return new
}

fun <T : Any> swapLease(old: T?, new: T?, buffer: (T) -> CVPixelBuffer?): T? {
    if (old === new) {
        return new
    }
    if (new != null) {
        retainLease(buffer(new))
    }
    if (old != null) {
        releaseLease(buffer(old))
    }
    return new
}

fun swapPool(old: Any?, new: CVPixelBufferPool?): CVPixelBufferPool? {
    val previous = old as? CVPixelBufferPool
    if (previous != null && previous !== new) {
        previous.invalidate()
    }
    return new
}

fun retainLeases(samples: Iterable<MediaSample>) {
    for (sample in samples) {
        retainLease(sample)
    }
}

fun releaseLeases(samples: Iterable<MediaSample>) {
    for (sample in samples) {
        releaseLease(sample)
    }
}

fun retainLeases(sample: MediaSample, buffers: Iterable<CVPixelBuffer>) {
    retainLease(sample)
    for (buffer in buffers) {
        retainLease(buffer)
    }
}

fun releaseLeases(sample: MediaSample, buffers: Iterable<CVPixelBuffer>) {
    releaseLease(sample)
    for (buffer in buffers) {
        releaseLease(buffer)
    }
}

fun drainLeases(samples: MutableCollection<MediaSample>) {
    val drained = samples.toList()
    samples.clear()
    releaseLeases(drained)
}
