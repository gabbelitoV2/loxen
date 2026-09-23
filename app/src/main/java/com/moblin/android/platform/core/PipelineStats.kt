package com.moblin.android.platform.core

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

object PipelineStats {
    private const val TAG = "MoblinStats"
    private val counterOrder = listOf(
        "camIn", "camDrop", "vuOut", "preview", "encIn", "encOut", "encDrop", "micIn", "aacOut", "recSeg",
    )
    private val counters = ConcurrentHashMap<String, AtomicLong>()
    private val gauges = ConcurrentHashMap<String, Long>()
    private val reporters = CopyOnWriteArrayList<() -> String?>()
    private val pipelineTicks = CopyOnWriteArrayList<() -> Unit>()
    private var previousSummary = ""
    private var lastPrintMs = 0L
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "MoblinStats").apply { isDaemon = true }
    }

    init {
        executor.scheduleAtFixedRate({ tick() }, 1, 1, TimeUnit.SECONDS)
    }

    fun increment(name: String, n: Long = 1) {
        counters.getOrPut(name) { AtomicLong() }.addAndGet(n)
    }

    fun gauge(name: String, value: Long) {
        gauges[name] = value
    }

    internal fun start() {}

    internal fun addReporter(reporter: () -> String?) {
        reporters.add(reporter)
    }

    internal fun addPipelineTick(tick: () -> Unit) {
        pipelineTicks.add(tick)
    }

    private fun tick() {
        try {
            probePipeline()
            printLine()
        } catch (error: Throwable) {
            Log.w(TAG, "Stats tick failed: $error")
        }
    }

    private fun probePipeline() {
        val postedAtMs = SystemClock.uptimeMillis()
        PipelineThread.post {
            gauge("pipeLagMs", SystemClock.uptimeMillis() - postedAtMs)
            for (pipelineTick in pipelineTicks) {
                pipelineTick()
            }
        }
    }

    private fun printLine() {
        val names = counterOrder.filter { counters.containsKey(it) } +
            counters.keys.filter { it !in counterOrder }.sorted()
        var anyActivity = false
        val parts = mutableListOf<String>()
        for (name in names) {
            val value = counters[name]?.getAndSet(0L) ?: 0L
            if (value != 0L) {
                anyActivity = true
            }
            parts.add("$name=$value")
        }
        val gaugeText = gauges.keys.sorted().joinToString(" ") { "$it=${gauges[it]}" }
        val reportText = reporters.mapNotNull { it() }.filter { it.isNotEmpty() }.joinToString(", ")
        val summary = "$gaugeText|$reportText"
        val nowMs = SystemClock.uptimeMillis()
        if (!anyActivity && summary == previousSummary && nowMs - lastPrintMs < 10_000) {
            return
        }
        if (parts.isEmpty() && gaugeText.isEmpty() && reportText.isEmpty()) {
            return
        }
        previousSummary = summary
        lastPrintMs = nowMs
        val line = buildString {
            append(parts.joinToString(" "))
            if (gaugeText.isNotEmpty()) {
                append(" | ")
                append(gaugeText)
            }
            if (reportText.isNotEmpty()) {
                append(" | ")
                append(reportText)
            }
        }
        Log.i(TAG, line.trim())
    }
}
