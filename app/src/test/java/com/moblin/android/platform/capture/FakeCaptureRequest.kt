package com.moblin.android.platform.capture

import android.hardware.camera2.CaptureRequest
import android.util.Range
import com.moblin.android.platform.avfoundation.AVCaptureDevice

internal class FakeCaptureRequest : CaptureRequestWriter {
    private val values = LinkedHashMap<CaptureRequest.Key<*>, Any?>()

    val keys: Set<CaptureRequest.Key<*>>
        get() = values.keys

    override fun <T> set(key: CaptureRequest.Key<T>, value: T) {
        values[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: CaptureRequest.Key<T>): T? = values[key] as T?

    fun has(key: CaptureRequest.Key<*>): Boolean = values.containsKey(key)

    companion object {
        fun of(device: AVCaptureDevice, frameRateRange: Range<Int> = Range(30, 30), lowLightBoost: Boolean = false): FakeCaptureRequest {
            val entry = device.camera!!
            val request = FakeCaptureRequest()
            CameraControls.apply(
                request,
                device.controlState(),
                entry.controls,
                frameRateRange,
                lowLightBoost,
                CameraControls.meteringArea(entry, 1f, false, android.util.Size(1920, 1080)),
            )
            return request
        }

        fun trigger(trigger: ControlTrigger): FakeCaptureRequest {
            val request = FakeCaptureRequest()
            CameraControls.applyTrigger(request, trigger)
            return request
        }
    }
}
