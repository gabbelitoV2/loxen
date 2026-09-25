package com.moblin.android.platform.avfoundation

import android.hardware.camera2.params.RggbChannelVector
import com.moblin.android.platform.core.NSKeyValueObservation
import com.moblin.android.platform.core.NSKeyValueObservedChange
import com.moblin.android.platform.core.NSKeyValueObservingOptions
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class ObservationOwner {
    var observation: NSKeyValueObservation? by NSKeyValueObservation.holder()
}

@RunWith(RobolectricTestRunner::class)
class AVCaptureDeviceObservationSuite {
    private fun deliver(
        device: AVCaptureDevice,
        focusDistance: Float? = null,
        sensitivity: Int? = null,
        exposureTimeNs: Long? = null,
        gains: RggbChannelVector? = null,
    ) {
        FakeCaptureDevices.deliver(
            device,
            FakeCaptureDevices.result(
                focusDistance = focusDistance,
                sensitivity = sensitivity,
                exposureTimeNs = exposureTimeNs,
                gains = gains,
            ),
        )
    }

    @Test
    fun lensPositionFollowsFocusDistanceLikeIos() {
        val device = FakeCaptureDevices.make(minimumFocusDistance = 10f)
        assertEquals(1f, device.lensPosition)
        deliver(device, focusDistance = 2.5f)
        assertEquals(0.75f, device.lensPosition)
        deliver(device, focusDistance = 10f)
        assertEquals(0f, device.lensPosition)
        deliver(device, focusDistance = 12f)
        assertEquals(0f, device.lensPosition)
        deliver(device, focusDistance = 0f)
        assertEquals(1f, device.lensPosition)
    }

    @Test
    fun fixedFocusCameraKeepsLensPosition() {
        val device = FakeCaptureDevices.make(minimumFocusDistance = 0f)
        var calls = 0
        device.observe(AVCaptureDevice::lensPosition) { _, _ -> calls += 1 }
        deliver(device, focusDistance = 0f)
        deliver(device, focusDistance = 3f)
        assertEquals(1f, device.lensPosition)
        assertEquals(0, calls)
    }

    @Test
    fun isoExposureAndWhiteBalanceFollowResults() {
        val device = FakeCaptureDevices.make()
        deliver(
            device,
            sensitivity = 640,
            exposureTimeNs = 8_333_333L,
            gains = RggbChannelVector(4f, 1.5f, 2.5f, 3f),
        )
        assertEquals(640f, device.iso)
        assertEquals(8_333L, device.exposureDuration)
        assertEquals(
            AVCaptureDevice.WhiteBalanceGains(redGain = 2f, greenGain = 1f, blueGain = 1.5f),
            device.deviceWhiteBalanceGains,
        )
        deliver(device, sensitivity = 100)
        assertEquals(100f, device.iso)
        assertEquals(8_333L, device.exposureDuration)
        assertEquals(2f, device.deviceWhiteBalanceGains.redGain)
    }

    @Test
    fun invalidFocusDistanceAndExposureTimeAreIgnored() {
        val device = FakeCaptureDevices.make(minimumFocusDistance = 10f)
        deliver(device, focusDistance = 5f, exposureTimeNs = 4_000_000L)
        var calls = 0
        val observations = listOf(
            device.observe(AVCaptureDevice::lensPosition) { _, _ -> calls += 1 },
            device.observe(AVCaptureDevice::exposureDuration) { _, _ -> calls += 1 },
        )
        deliver(device, focusDistance = Float.NaN, exposureTimeNs = 0L)
        assertEquals(0.5f, device.lensPosition)
        assertEquals(4_000L, device.exposureDuration)
        assertEquals(0, calls)
        observations.forEach { it.invalidate() }
    }

    @Test
    fun unchangedWhiteBalanceGainsKeepTheSameValue() {
        val device = FakeCaptureDevices.make()
        deliver(device, gains = RggbChannelVector(2f, 1f, 1f, 1.5f))
        val first = device.deviceWhiteBalanceGains
        deliver(device, gains = RggbChannelVector(4f, 2f, 2f, 3f))
        assertSame(first, device.deviceWhiteBalanceGains)
        deliver(device, gains = RggbChannelVector(2f, 1f, 1f, 1.6f))
        assertEquals(1.6f, device.deviceWhiteBalanceGains.blueGain)
    }

    @Test
    fun invalidWhiteBalanceGainsAreIgnored() {
        val device = FakeCaptureDevices.make()
        val before = device.deviceWhiteBalanceGains
        deliver(device, gains = RggbChannelVector(0f, 1f, 1f, 1f))
        assertEquals(before, device.deviceWhiteBalanceGains)
    }

    @Test
    fun observersFireOnlyWhenTheirValueChanges() {
        val device = FakeCaptureDevices.make()
        var focusCalls = 0
        var isoCalls = 0
        var exposureCalls = 0
        var whiteBalanceCalls = 0
        val observations = listOf(
            device.observe(AVCaptureDevice::lensPosition) { _, _ -> focusCalls += 1 },
            device.observe(AVCaptureDevice::iso) { _, _ -> isoCalls += 1 },
            device.observe(AVCaptureDevice::exposureDuration) { _, _ -> exposureCalls += 1 },
            device.observe(AVCaptureDevice::deviceWhiteBalanceGains) { _, _ -> whiteBalanceCalls += 1 },
        )
        val gains = RggbChannelVector(2f, 1f, 1f, 1.5f)
        deliver(device, focusDistance = 5f, sensitivity = 200, exposureTimeNs = 10_000_000L, gains = gains)
        deliver(device, focusDistance = 5f, sensitivity = 200, exposureTimeNs = 10_000_000L, gains = gains)
        assertEquals(listOf(1, 1, 1, 1), listOf(focusCalls, isoCalls, exposureCalls, whiteBalanceCalls))
        deliver(device, focusDistance = 4f, sensitivity = 200, exposureTimeNs = 10_000_400L, gains = gains)
        assertEquals(listOf(2, 1, 1, 1), listOf(focusCalls, isoCalls, exposureCalls, whiteBalanceCalls))
        deliver(device, sensitivity = 400, gains = RggbChannelVector(2f, 1f, 1f, 1.6f))
        assertEquals(listOf(2, 2, 1, 2), listOf(focusCalls, isoCalls, exposureCalls, whiteBalanceCalls))
        observations.forEach { it.invalidate() }
    }

    @Test
    fun handlerSeesTheNewValueAndTheDevice() {
        val device = FakeCaptureDevices.make()
        var seen: Pair<AVCaptureDevice, Float>? = null
        val observation = device.observe(AVCaptureDevice::iso) { observed, _ -> seen = observed to observed.iso }
        deliver(device, sensitivity = 800)
        assertSame(device, seen?.first)
        assertEquals(800f, seen?.second)
        observation.invalidate()
    }

    @Test
    fun invalidateStopsOnlyThatObserver() {
        val device = FakeCaptureDevices.make()
        var first = 0
        var second = 0
        val firstObservation = device.observe(AVCaptureDevice::lensPosition) { _, _ -> first += 1 }
        val secondObservation = device.observe(AVCaptureDevice::lensPosition) { _, _ -> second += 1 }
        deliver(device, focusDistance = 1f)
        firstObservation.invalidate()
        assertFalse(firstObservation.isValid)
        deliver(device, focusDistance = 2f)
        firstObservation.invalidate()
        deliver(device, focusDistance = 3f)
        assertEquals(1, first)
        assertEquals(3, second)
        secondObservation.invalidate()
        deliver(device, focusDistance = 4f)
        assertEquals(3, second)
    }

    @Test
    fun holderInvalidatesTheObservationItDrops() {
        val device = FakeCaptureDevices.make()
        var calls = 0
        val owner = ObservationOwner()
        owner.observation = device.observe(AVCaptureDevice::lensPosition) { _, _ -> calls += 1 }
        val first = owner.observation!!
        owner.observation = device.observe(AVCaptureDevice::lensPosition) { _, _ -> calls += 10 }
        assertFalse(first.isValid)
        deliver(device, focusDistance = 1f)
        assertEquals(10, calls)
        val second = owner.observation!!
        owner.observation = second
        assertTrue(second.isValid)
        owner.observation = null
        assertFalse(second.isValid)
        assertNull(owner.observation)
        deliver(device, focusDistance = 2f)
        assertEquals(10, calls)
    }

    @Test
    fun optionsSelectInitialNewAndOldValues() {
        val device = FakeCaptureDevices.make()
        deliver(device, sensitivity = 100)
        val changes = mutableListOf<NSKeyValueObservedChange<Float>>()
        val observation = device.observe(
            AVCaptureDevice::iso,
            options = setOf(
                NSKeyValueObservingOptions.initial,
                NSKeyValueObservingOptions.new,
                NSKeyValueObservingOptions.old,
            ),
        ) { _, change -> changes.add(change) }
        deliver(device, sensitivity = 200)
        assertEquals(listOf(100f to null, 200f to 100f), changes.map { it.newValue to it.oldValue })
        var plain: NSKeyValueObservedChange<Float>? = null
        val plainObservation = device.observe(AVCaptureDevice::iso) { _, change -> plain = change }
        deliver(device, sensitivity = 300)
        assertNull(plain?.newValue)
        assertNull(plain?.oldValue)
        observation.invalidate()
        plainObservation.invalidate()
    }

    @Test
    fun handlerRunsOnTheThreadDeliveringTheResult() {
        val device = FakeCaptureDevices.make()
        var handlerThread: Thread? = null
        val observation = device.observe(AVCaptureDevice::lensPosition) { _, _ -> handlerThread = Thread.currentThread() }
        val cameraThread = Thread { deliver(device, focusDistance = 5f) }
        cameraThread.start()
        cameraThread.join()
        assertSame(cameraThread, handlerThread)
        observation.invalidate()
    }

    @Test
    fun currentIsoAndExposureDurationKeepTheLiveValues() {
        val device = FakeCaptureDevices.make()
        deliver(device, sensitivity = 250, exposureTimeNs = 4_000_000L)
        assertTrue(AVCaptureDevice.currentISO < device.activeFormat.minISO)
        assertTrue(AVCaptureDevice.currentExposureDuration < device.activeFormat.minExposureDuration)
        var completed = false
        device.setExposureModeCustom(
            duration = AVCaptureDevice.currentExposureDuration,
            iso = AVCaptureDevice.currentISO,
        ) { completed = true }
        assertTrue(completed)
        assertEquals(250f, device.iso)
        assertEquals(4_000L, device.exposureDuration)
    }
}
