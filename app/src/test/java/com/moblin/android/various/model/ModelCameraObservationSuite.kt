package com.moblin.android.various.model

import android.hardware.camera2.params.RggbChannelVector
import android.os.Looper
import com.moblin.android.media.haishinkit.media.video.CaptureDevice
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.platform.avfoundation.FakeCaptureDevices
import com.moblin.android.various.utils.clamped
import com.moblin.android.various.utils.factorFromExposure
import com.moblin.android.various.utils.factorFromIso
import com.moblin.android.various.utils.factorFromWhiteBalance
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ModelCameraObservationSuite {
    private lateinit var model: Model
    private lateinit var device: AVCaptureDevice
    private lateinit var captureDevice: CaptureDevice

    @Before
    fun setUp() {
        model = Model()
        device = FakeCaptureDevices.make(minimumFocusDistance = 10f)
        captureDevice = CaptureDevice(device = device, id = UUID.randomUUID(), isVideoMirrored = false)
        model.cameraDevice = captureDevice
    }

    private fun deliver(
        device: AVCaptureDevice = this.device,
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

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun focusFollowsTheLensOnTheMainThread() {
        deliver(focusDistance = 5f)
        model.startObservingFocus()
        assertEquals(0.5f, model.camera.lockedFocus.value)
        deliver(focusDistance = 2.5f)
        assertEquals(0.5f, model.camera.lockedFocus.value)
        runMain()
        assertEquals(0.75f, model.camera.lockedFocus.value)
        assertEquals(0.75f, model.camera.lockedFocuses[captureDevice])
    }

    @Test
    fun focusIsLeftAloneWhileTheSliderIsEdited() {
        model.startObservingFocus()
        model.camera.editingLockedFocus = true
        deliver(focusDistance = 2.5f)
        runMain()
        assertEquals(1f, model.camera.lockedFocus.value)
        assertNull(model.camera.lockedFocuses[captureDevice])
        model.camera.editingLockedFocus = false
        deliver(focusDistance = 5f)
        runMain()
        assertEquals(0.5f, model.camera.lockedFocus.value)
    }

    @Test
    fun stopObservingFocusStopsUpdates() {
        model.startObservingFocus()
        val observation = assertNotNull(model.camera.focusObservation)
        model.stopObservingFocus()
        assertNull(model.camera.focusObservation)
        assertFalse(observation.isValid)
        deliver(focusDistance = 2.5f)
        runMain()
        assertEquals(1f, model.camera.lockedFocus.value)
    }

    @Test
    fun startingAgainReplacesThePreviousObservation() {
        model.startObservingFocus()
        val first = assertNotNull(model.camera.focusObservation)
        model.startObservingFocus()
        assertFalse(first.isValid)
        model.stopObservingFocus()
        deliver(focusDistance = 2.5f)
        runMain()
        assertEquals(1f, model.camera.lockedFocus.value)
    }

    @Test
    fun isoFollowsTheSensor() {
        model.startObservingIso()
        deliver(sensitivity = 1649)
        runMain()
        val expected = factorFromIso(device = device, iso = 1649f)
        assertEquals(expected, model.camera.lockedIso.value)
        assertEquals(expected, model.camera.lockedIsos[captureDevice])
        model.camera.editingLockedIso = true
        deliver(sensitivity = 400)
        runMain()
        assertEquals(expected, model.camera.lockedIso.value)
        model.stopObservingIso()
        model.camera.editingLockedIso = false
        deliver(sensitivity = 800)
        runMain()
        assertEquals(expected, model.camera.lockedIso.value)
    }

    @Test
    fun exposureFollowsTheSensor() {
        model.startObservingExposure()
        deliver(exposureTimeNs = 4_000_000L)
        runMain()
        val expected = factorFromExposure(device = device, exposure = 4_000L)
        assertEquals(4_000L, model.camera.exposure.value)
        assertEquals(expected, model.camera.lockedExposure.value)
        assertEquals(expected, model.camera.lockedExposures[captureDevice])
        model.camera.editingLockedExposure = true
        deliver(exposureTimeNs = 1_000_000L)
        runMain()
        assertEquals(4_000L, model.camera.exposure.value)
        model.stopObservingExposure()
        model.camera.editingLockedExposure = false
        deliver(exposureTimeNs = 2_000_000L)
        runMain()
        assertEquals(4_000L, model.camera.exposure.value)
    }

    @Test
    fun whiteBalanceFollowsTheSensorGains() {
        model.startObservingWhiteBalance()
        deliver(gains = RggbChannelVector(1.5f, 1f, 1f, 2.5f))
        runMain()
        val gains = AVCaptureDevice.WhiteBalanceGains(redGain = 1.5f, greenGain = 1f, blueGain = 2.5f)
        val expected = factorFromWhiteBalance(device = device, gains = gains.clamped(maxGain = device.maxWhiteBalanceGain))
        assertEquals(expected, model.camera.lockedWhiteBalance.value)
        assertEquals(expected, model.camera.lockedWhiteBalances[captureDevice])
        model.camera.editingLockedWhiteBalance = true
        deliver(gains = RggbChannelVector(2.5f, 1f, 1f, 1.5f))
        runMain()
        assertEquals(expected, model.camera.lockedWhiteBalance.value)
        model.stopObservingWhiteBalance()
        model.camera.editingLockedWhiteBalance = false
        deliver(gains = RggbChannelVector(3f, 1f, 1f, 1.2f))
        runMain()
        assertEquals(expected, model.camera.lockedWhiteBalance.value)
    }

    @Test
    fun attachingAnotherCameraMovesTheObservations() {
        model.startObservingIso()
        model.startObservingExposure()
        val other = FakeCaptureDevices.make()
        val otherCaptureDevice = CaptureDevice(device = other, id = UUID.randomUUID(), isVideoMirrored = false)
        model.cameraDevice = otherCaptureDevice
        model.setExposureAndIsoAfterCameraAttach(device = otherCaptureDevice)
        deliver(device = device, sensitivity = 3264, exposureTimeNs = 30_000_000L)
        runMain()
        assertNull(model.camera.lockedIsos[captureDevice])
        assertNull(model.camera.lockedExposures[captureDevice])
        deliver(device = other, sensitivity = 1649, exposureTimeNs = 4_000_000L)
        runMain()
        assertEquals(factorFromIso(device = other, iso = 1649f), model.camera.lockedIsos[otherCaptureDevice])
        assertEquals(4_000L, model.camera.exposure.value)
    }
}
