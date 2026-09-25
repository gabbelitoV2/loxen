package com.moblin.android.platform.darwin

import android.os.Looper
import com.moblin.android.various.utils.ResourceUsage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DarwinTaskInfoSuite {
    private lateinit var originalReader: () -> ULong?

    @Before
    fun setUp() {
        originalReader = TaskVmInfo.reader
        TaskVmInfo.reset()
    }

    @After
    fun tearDown() {
        TaskVmInfo.reader = originalReader
        TaskVmInfo.reset()
    }

    private fun readOnce(value: ULong?): Thread {
        val done = CountDownLatch(1)
        var thread: Thread? = null
        TaskVmInfo.reader = {
            thread = Thread.currentThread()
            done.countDown()
            value
        }
        repeat(250) {
            task_info(mach_task_self_, TASK_VM_INFO, task_vm_info_data_t())
            if (done.await(20, TimeUnit.MILLISECONDS)) {
                waitUntilIdle()
                return thread!!
            }
        }
        throw AssertionError("The memory was never measured")
    }

    private fun waitUntilIdle() {
        val idle = CountDownLatch(1)
        TaskVmInfo.reader = {
            idle.countDown()
            null
        }
        repeat(250) {
            task_info(mach_task_self_, TASK_VM_INFO, task_vm_info_data_t())
            if (idle.await(20, TimeUnit.MILLISECONDS)) {
                return
            }
        }
        throw AssertionError("The memory reader never became idle")
    }

    @Test
    fun memoryIsMeasuredOffTheCallingThreadAndReportedByTheNextCall() {
        val info = task_vm_info_data_t()
        TaskVmInfo.reader = { null }
        val readerThread = readOnce(300uL * 1024u * 1024u)
        assertNotSame(Thread.currentThread(), readerThread)
        assertNotSame(Looper.getMainLooper().thread, readerThread)
        assertEquals(KERN_SUCCESS, task_info(mach_task_self_, TASK_VM_INFO, info))
        assertEquals(300uL * 1024u * 1024u, info.phys_footprint)
    }

    @Test
    fun firstCallFailsLikeAnUnavailableTaskInfo() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        TaskVmInfo.reader = {
            started.countDown()
            release.await(5, TimeUnit.SECONDS)
            1024uL
        }
        val info = task_vm_info_data_t()
        assertEquals(KERN_FAILURE, task_info(mach_task_self_, TASK_VM_INFO, info))
        assertEquals(0uL, info.phys_footprint)
        assertTrue(started.await(5, TimeUnit.SECONDS))
        release.countDown()
        waitUntilIdle()
    }

    @Test
    fun aFailedMeasurementKeepsThePreviousValue() {
        readOnce(64uL * 1024u * 1024u)
        readOnce(null)
        val info = task_vm_info_data_t()
        assertEquals(KERN_SUCCESS, task_info(mach_task_self_, TASK_VM_INFO, info))
        assertEquals(64uL * 1024u * 1024u, info.phys_footprint)
    }

    @Test
    fun otherTasksAndFlavorsFail() {
        readOnce(1024uL)
        assertEquals(KERN_FAILURE, task_info(mach_task_self_ + 1, TASK_VM_INFO, task_vm_info_data_t()))
        assertEquals(KERN_FAILURE, task_info(mach_task_self_, TASK_VM_INFO + 1, task_vm_info_data_t()))
    }

    @Test
    fun resourceUsageShowsTheFootprintInMegabytesAndZeroUntilMeasured() {
        val resourceUsage = ResourceUsage()
        val release = CountDownLatch(1)
        TaskVmInfo.reader = {
            release.await(5, TimeUnit.SECONDS)
            null
        }
        resourceUsage.update(now = 0)
        assertEquals(0, resourceUsage.getMemoryUsage())
        release.countDown()
        waitUntilIdle()
        readOnce(512uL * 1024u * 1024u + 700u * 1024u)
        resourceUsage.update(now = 1_000)
        assertEquals(512, resourceUsage.getMemoryUsage())
    }
}
