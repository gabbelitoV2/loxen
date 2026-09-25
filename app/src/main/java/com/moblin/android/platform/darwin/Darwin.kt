package com.moblin.android.platform.darwin

import android.os.Debug
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

const val RUSAGE_SELF = 0
const val PROCESSOR_CPU_LOAD_INFO = 2
const val CPU_STATE_USER = 0
const val CPU_STATE_SYSTEM = 1
const val CPU_STATE_IDLE = 2
const val CPU_STATE_NICE = 3
const val CPU_STATE_MAX = 4
const val KERN_SUCCESS = 0
const val KERN_FAILURE = 5
const val TASK_VM_INFO = 22
const val mach_task_self_ = 0

class timeval(var tv_sec: Long = 0, var tv_usec: Int = 0) {
    val milliseconds: Long
        get() = tv_sec * 1000 + tv_usec / 1000
}

class rusage(var ru_utime: timeval = timeval(), var ru_stime: timeval = timeval())

class processor_info_array_t internal constructor(val numberOfCpus: Int, private val values: IntArray) {
    operator fun get(index: Int): Int = values[index]
}

class task_vm_info_data_t(var phys_footprint: ULong = 0u)

fun mach_host_self(): Int = 0

fun task_info(task: Int, flavor: Int, info: task_vm_info_data_t): Int {
    if (task != mach_task_self_ || flavor != TASK_VM_INFO) {
        return KERN_FAILURE
    }
    info.phys_footprint = TaskVmInfo.physFootprint() ?: return KERN_FAILURE
    return KERN_SUCCESS
}

fun getrusage(who: Int, usage: rusage): Int {
    if (who != RUSAGE_SELF) {
        return -1
    }
    val fields = ProcFs.read(ProcFs.selfStat)?.let { ProcFs.fieldsAfterCommand(it) } ?: return -1
    val userTicks = fields.getOrNull(11)?.toLongOrNull() ?: return -1
    val systemTicks = fields.getOrNull(12)?.toLongOrNull() ?: return -1
    usage.ru_utime = ProcFs.toTimeval(ticks = userTicks)
    usage.ru_stime = ProcFs.toTimeval(ticks = systemTicks)
    return 0
}

fun host_processor_info(host: Int, flavor: Int): processor_info_array_t? {
    if (flavor != PROCESSOR_CPU_LOAD_INFO) {
        return null
    }
    val text = ProcFs.readStat() ?: return null
    return ProcFs.cpuLoadInfo(ProcFs.parseCpuTicks(text))
}

internal object ProcFs {
    private val whitespace = Regex("\\s+")
    private val cpuLine = Regex("^cpu(\\d+)\\s")
    private val lastCpuTicks = sortedMapOf<Int, IntArray>()
    private var statReadable = true

    var selfStat = File("/proc/self/stat")

    var stat = File("/proc/stat")
        set(value) {
            synchronized(this) {
                field = value
                statReadable = true
                lastCpuTicks.clear()
            }
        }

    val clockTicksPerSecond: Long by lazy {
        runCatching { Os.sysconf(OsConstants._SC_CLK_TCK) }.getOrNull()?.takeIf { it > 0 } ?: 100
    }

    fun read(file: File): String? = runCatching { file.readText() }.getOrNull()

    @Synchronized
    fun readStat(): String? {
        if (!statReadable) {
            return null
        }
        return read(stat).also { statReadable = it != null }
    }

    fun fieldsAfterCommand(stat: String): List<String>? {
        val end = stat.lastIndexOf(')')
        if (end < 0) {
            return null
        }
        return stat.substring(end + 1).trim().split(whitespace)
    }

    fun toTimeval(ticks: Long): timeval {
        val microseconds = ticks * 1_000_000 / clockTicksPerSecond
        return timeval(tv_sec = microseconds / 1_000_000, tv_usec = (microseconds % 1_000_000).toInt())
    }

    fun parseCpuTicks(text: String): Map<Int, IntArray> {
        val cpus = mutableMapOf<Int, IntArray>()
        for (line in text.lineSequence()) {
            val cpu = cpuLine.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: continue
            val values = line.trim().split(whitespace).drop(1).map { it.toLongOrNull() ?: 0 }
            if (values.size < 4) {
                continue
            }
            val ticks = IntArray(CPU_STATE_MAX)
            ticks[CPU_STATE_USER] = values[0].toInt()
            ticks[CPU_STATE_NICE] = values[1].toInt()
            ticks[CPU_STATE_SYSTEM] = (values[2] + values.getOrElse(5) { 0 } + values.getOrElse(6) { 0 }).toInt()
            ticks[CPU_STATE_IDLE] = (values[3] + values.getOrElse(4) { 0 }).toInt()
            cpus[cpu] = ticks
        }
        return cpus
    }

    @Synchronized
    fun cpuLoadInfo(cpus: Map<Int, IntArray>): processor_info_array_t? {
        if (cpus.isEmpty()) {
            return null
        }
        lastCpuTicks.putAll(cpus)
        val numberOfCpus = lastCpuTicks.lastKey() + 1
        val values = IntArray(numberOfCpus * CPU_STATE_MAX)
        for ((cpu, ticks) in lastCpuTicks) {
            ticks.copyInto(values, cpu * CPU_STATE_MAX)
        }
        return processor_info_array_t(numberOfCpus, values)
    }
}

internal object TaskVmInfo {
    private val refreshing = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "TaskVmInfo").also { it.isDaemon = true }
    }

    @Volatile
    private var latest: ULong? = null

    @Volatile
    var reader: () -> ULong? = {
        val info = Debug.MemoryInfo()
        Debug.getMemoryInfo(info)
        info.totalPss.toULong() * 1024u
    }

    fun physFootprint(): ULong? {
        if (refreshing.compareAndSet(false, true)) {
            executor.execute {
                try {
                    latest = runCatching { reader() }.getOrNull() ?: latest
                } finally {
                    refreshing.set(false)
                }
            }
        }
        return latest
    }

    fun reset() {
        latest = null
    }
}
