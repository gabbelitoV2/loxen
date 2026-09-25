package com.moblin.android.platform.darwin

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DarwinCpuSuite {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var originalSelfStat: File
    private lateinit var originalStat: File
    private lateinit var selfStat: File
    private lateinit var stat: File

    @Before
    fun setUp() {
        originalSelfStat = ProcFs.selfStat
        originalStat = ProcFs.stat
        selfStat = File(folder.root, "self-stat")
        stat = File(folder.root, "stat")
        ProcFs.selfStat = selfStat
        ProcFs.stat = stat
    }

    @After
    fun tearDown() {
        ProcFs.selfStat = originalSelfStat
        ProcFs.stat = originalStat
    }

    private fun ticks(milliseconds: Long): Long = milliseconds * ProcFs.clockTicksPerSecond / 1000

    private fun writeSelfStat(command: String, userTicks: Long, systemTicks: Long) {
        selfStat.writeText(
            "4711 ($command) S 1 4711 4711 0 -1 4194624 5000 0 3 0 $userTicks $systemTicks 900 800 20 0 " +
                "42 0 12345 9876543210 3000 18446744073709551615 1 1 0 0 0 0 4612 1 1073775864 0 0 0 17 5 0 0\n",
        )
    }

    private fun readCpuLoad(): processor_info_array_t {
        return assertNotNull(host_processor_info(mach_host_self(), PROCESSOR_CPU_LOAD_INFO))
    }

    private fun cpuStates(info: processor_info_array_t, cpu: Int): List<Long> {
        return (0 until CPU_STATE_MAX).map { info[cpu * CPU_STATE_MAX + it].toUInt().toLong() }
    }

    @Test
    fun getrusageReadsProcessUserAndSystemTimeFromProcSelfStat() {
        writeSelfStat(command = "com.moblin.android", userTicks = ticks(12_340), systemTicks = ticks(5_670))
        val usage = rusage()
        assertEquals(0, getrusage(RUSAGE_SELF, usage))
        assertEquals(12_340L, usage.ru_utime.milliseconds)
        assertEquals(5_670L, usage.ru_stime.milliseconds)
        assertEquals(12L, usage.ru_utime.tv_sec)
        assertEquals(340_000, usage.ru_utime.tv_usec)
    }

    @Test
    fun getrusageFindsTheFieldsAfterACommandWithSpacesAndParentheses() {
        writeSelfStat(command = "Render (main) 1) S 2", userTicks = ticks(1_000), systemTicks = ticks(2_000))
        val usage = rusage()
        assertEquals(0, getrusage(RUSAGE_SELF, usage))
        assertEquals(1_000L, usage.ru_utime.milliseconds)
        assertEquals(2_000L, usage.ru_stime.milliseconds)
    }

    @Test
    fun getrusageFailsWithoutTouchingUsageWhenProcSelfStatIsUnreadableOrBroken() {
        val usage = rusage(ru_utime = timeval(tv_sec = 3, tv_usec = 500_000), ru_stime = timeval(tv_sec = 1))
        assertEquals(-1, getrusage(RUSAGE_SELF, usage))
        selfStat.writeText("4711 com.moblin.android S 1 2 3\n")
        assertEquals(-1, getrusage(RUSAGE_SELF, usage))
        selfStat.writeText("4711 (com.moblin.android) S 1 2 3\n")
        assertEquals(-1, getrusage(RUSAGE_SELF, usage))
        assertEquals(3_500L, usage.ru_utime.milliseconds)
        assertEquals(1_000L, usage.ru_stime.milliseconds)
    }

    @Test
    fun getrusageOnlySupportsTheCallingProcess() {
        writeSelfStat(command = "com.moblin.android", userTicks = 1, systemTicks = 1)
        assertEquals(-1, getrusage(-1, rusage()))
    }

    @Test
    fun timevalMillisecondsTruncatesMicroseconds() {
        assertEquals(0L, timeval().milliseconds)
        assertEquals(1_999L, timeval(tv_sec = 1, tv_usec = 999_999).milliseconds)
        assertEquals(86_400_000L, timeval(tv_sec = 86_400).milliseconds)
    }

    @Test
    fun hostProcessorInfoReportsEachCpuInMachStateOrder() {
        stat.writeText(
            """
            cpu  350 20 150 1900 60 10 10 0 0 0
            cpu0 100 10 50 800 40 5 5 7 0 0
            cpu1 250 10 100 1100 20 5 5 0 0 0
            intr 12345 0 0
            ctxt 67890
            btime 1700000000
            processes 4242
            procs_running 3
            procs_blocked 0
            """.trimIndent() + "\n",
        )
        val info = readCpuLoad()
        assertEquals(2, info.numberOfCpus)
        assertEquals(listOf(100L, 60L, 840L, 10L), cpuStates(info, 0))
        assertEquals(listOf(250L, 110L, 1120L, 10L), cpuStates(info, 1))
        assertEquals(0, CPU_STATE_USER)
        assertEquals(1, CPU_STATE_SYSTEM)
        assertEquals(2, CPU_STATE_IDLE)
        assertEquals(3, CPU_STATE_NICE)
    }

    @Test
    fun hostProcessorInfoReadsOldKernelsWithOnlyFourFields() {
        stat.writeText("cpu  30 0 20 50\ncpu0 30 0 20 50\n")
        assertEquals(listOf(30L, 20L, 50L, 0L), cpuStates(readCpuLoad(), 0))
    }

    @Test
    fun hostProcessorInfoWrapsTicksLikeNaturalT() {
        stat.writeText("cpu0 4294967300 0 4294967296 8589934593\n")
        assertEquals(listOf(4L, 0L, 1L, 0L), cpuStates(readCpuLoad(), 0))
    }

    @Test
    fun hostProcessorInfoKeepsTheLastTicksOfAnOfflineCpu() {
        stat.writeText("cpu0 10 0 10 80\ncpu1 20 0 20 60\ncpu2 30 0 30 40\ncpu3 40 0 40 20\n")
        readCpuLoad()
        stat.writeText("cpu0 20 0 20 160\ncpu1 40 0 40 120\ncpu3 80 0 80 40\n")
        val info = readCpuLoad()
        assertEquals(4, info.numberOfCpus)
        assertEquals(listOf(30L, 30L, 40L, 0L), cpuStates(info, 2))
        assertEquals(listOf(80L, 80L, 40L, 0L), cpuStates(info, 3))
    }

    @Test
    fun hostProcessorInfoFailsAndStopsReadingWhenProcStatIsClosedToTheApp() {
        assertNull(host_processor_info(mach_host_self(), PROCESSOR_CPU_LOAD_INFO))
        stat.writeText("cpu0 1 2 3 4\n")
        assertNull(host_processor_info(mach_host_self(), PROCESSOR_CPU_LOAD_INFO))
        ProcFs.stat = stat
        assertEquals(1, readCpuLoad().numberOfCpus)
    }

    @Test
    fun hostProcessorInfoFailsWithoutPerCpuLinesOrForOtherFlavors() {
        stat.writeText("cpu  1 2 3 4\nintr 1\n")
        assertNull(host_processor_info(mach_host_self(), PROCESSOR_CPU_LOAD_INFO))
        stat.writeText("cpu0 1 2 3 4\n")
        assertNull(host_processor_info(mach_host_self(), PROCESSOR_CPU_LOAD_INFO + 1))
        assertEquals(1, readCpuLoad().numberOfCpus)
    }

    @Test
    fun readsTheRealProcFilesOnLinux() {
        assumeTrue(originalSelfStat.canRead())
        ProcFs.selfStat = originalSelfStat
        val usage = rusage()
        assertEquals(0, getrusage(RUSAGE_SELF, usage))
        assertTrue(usage.ru_utime.milliseconds + usage.ru_stime.milliseconds > 0)
    }
}
