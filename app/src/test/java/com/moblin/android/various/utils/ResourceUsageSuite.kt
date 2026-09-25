package com.moblin.android.various.utils

import com.moblin.android.platform.darwin.ProcFs
import java.io.File
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResourceUsageSuite {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var originalSelfStat: File
    private lateinit var originalStat: File
    private lateinit var selfStat: File
    private lateinit var stat: File
    private val resourceUsage = ResourceUsage()

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

    private fun writeProcess(userMilliseconds: Long, systemMilliseconds: Long) {
        val hz = ProcFs.clockTicksPerSecond
        selfStat.writeText(
            "4711 (com.moblin.android) R 1 4711 4711 0 -1 4194624 5000 0 3 0 " +
                "${userMilliseconds * hz / 1000} ${systemMilliseconds * hz / 1000} 777 888 20 0 42 0 12345\n",
        )
    }

    private fun writeCpus(vararg cpus: String) {
        stat.writeText(
            "cpu  0 0 0 0 0 0 0 0 0 0\n" +
                cpus.mapIndexed { index, values -> "cpu$index $values\n" }.joinToString("") +
                "intr 1 2 3\nctxt 4\n",
        )
    }

    @Test
    fun firstUpdateHasNothingToCompareWith() {
        writeProcess(userMilliseconds = 5_000, systemMilliseconds = 1_000)
        writeCpus("100 0 100 800 0 0 0 0 0 0")
        resourceUsage.update(now = 10_000)
        assertEquals(0, resourceUsage.getAppCpuUsage())
        assertEquals(0, resourceUsage.getCpuUsage())
    }

    @Test
    fun appCpuIsProcessCpuTimeOverWallTimeWhereOneCoreIsHundred() {
        writeProcess(userMilliseconds = 5_000, systemMilliseconds = 1_000)
        resourceUsage.update(now = 10_000)
        writeProcess(userMilliseconds = 5_500, systemMilliseconds = 1_250)
        resourceUsage.update(now = 11_000)
        assertEquals(75, resourceUsage.getAppCpuUsage())
        writeProcess(userMilliseconds = 7_300, systemMilliseconds = 1_450)
        resourceUsage.update(now = 12_000)
        assertEquals(200, resourceUsage.getAppCpuUsage())
        writeProcess(userMilliseconds = 7_310, systemMilliseconds = 1_450)
        resourceUsage.update(now = 14_000)
        assertEquals(0, resourceUsage.getAppCpuUsage())
    }

    @Test
    fun appCpuKeepsItsLastValueWhenProcSelfStatCannotBeRead() {
        writeProcess(userMilliseconds = 0, systemMilliseconds = 0)
        resourceUsage.update(now = 0)
        writeProcess(userMilliseconds = 400, systemMilliseconds = 100)
        resourceUsage.update(now = 1_000)
        assertEquals(50, resourceUsage.getAppCpuUsage())
        selfStat.delete()
        resourceUsage.update(now = 2_000)
        assertEquals(50, resourceUsage.getAppCpuUsage())
        writeProcess(userMilliseconds = 700, systemMilliseconds = 100)
        resourceUsage.update(now = 3_000)
        assertEquals(15, resourceUsage.getAppCpuUsage())
    }

    @Test
    fun cpuIsTheSumOfEveryCpusBusyPercentageLikeHostProcessorInfo() {
        writeProcess(userMilliseconds = 0, systemMilliseconds = 0)
        writeCpus("1000 0 500 8000 100 0 0 0 0 0", "2000 50 1000 6000 0 0 0 0 0 0", "0 0 0 10000 0 0 0 0 0 0")
        resourceUsage.update(now = 0)
        writeCpus("1030 0 510 8030 120 5 5 50 0 0", "2090 60 1000 6000 0 0 0 0 0 0", "0 0 0 10100 0 0 0 0 0 0")
        resourceUsage.update(now = 1_000)
        assertEquals(150, resourceUsage.getCpuUsage())
    }

    @Test
    fun cpuHandlesTicksWrappingAroundThirtyTwoBits() {
        writeProcess(userMilliseconds = 0, systemMilliseconds = 0)
        writeCpus("4294967290 0 0 4294967200 0 0 0 0 0 0")
        resourceUsage.update(now = 0)
        writeCpus("4294967300 0 0 4294967280 0 0 0 0 0 0")
        resourceUsage.update(now = 1_000)
        assertEquals(11, resourceUsage.getCpuUsage())
    }

    @Test
    fun cpuSkipsOneUpdateWhenTheNumberOfCpusChanges() {
        writeProcess(userMilliseconds = 0, systemMilliseconds = 0)
        writeCpus("0 0 0 100 0 0 0 0 0 0", "0 0 0 100 0 0 0 0 0 0")
        resourceUsage.update(now = 0)
        writeCpus("100 0 0 100 0 0 0 0 0 0", "0 0 0 200 0 0 0 0 0 0")
        resourceUsage.update(now = 1_000)
        assertEquals(100, resourceUsage.getCpuUsage())
        writeCpus("150 0 0 150 0 0 0 0 0 0", "0 0 0 300 0 0 0 0 0 0", "0 0 0 0 0 0 0 0 0 0")
        resourceUsage.update(now = 2_000)
        assertEquals(100, resourceUsage.getCpuUsage())
        writeCpus("150 0 0 250 0 0 0 0 0 0", "100 0 0 300 0 0 0 0 0 0", "25 0 25 50 0 0 0 0 0 0")
        resourceUsage.update(now = 3_000)
        assertEquals(150, resourceUsage.getCpuUsage())
    }

    @Test
    fun cpuIsTheAppCpuWhenAndroidClosesProcStat() {
        writeProcess(userMilliseconds = 1_000, systemMilliseconds = 1_000)
        resourceUsage.update(now = 0)
        assertEquals(0, resourceUsage.getCpuUsage())
        writeProcess(userMilliseconds = 1_900, systemMilliseconds = 1_300)
        resourceUsage.update(now = 1_000)
        assertEquals(120, resourceUsage.getAppCpuUsage())
        assertEquals(120, resourceUsage.getCpuUsage())
    }
}
