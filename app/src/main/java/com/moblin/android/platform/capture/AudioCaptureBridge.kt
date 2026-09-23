package com.moblin.android.platform.capture

import android.util.Log
import com.moblin.android.platform.avfoundation.AVAudioSession
import com.moblin.android.platform.avfoundation.AVCaptureAudioDataOutput
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.platform.avfoundation.AVCaptureOutput
import com.moblin.android.platform.avfoundation.AVCaptureSession
import com.moblin.android.platform.avfoundation.AudioCaptureEngine

internal object AudioCaptureBridge {
    private const val TAG = "MoblinAudio"
    private var owner: AVCaptureSession? = null
    private var configuredDevice: AVCaptureDevice? = null
    private var configuredOutput: AVCaptureAudioDataOutput? = null

    @Synchronized
    fun update(session: AVCaptureSession, device: AVCaptureDevice?, output: AVCaptureOutput?, running: Boolean) {
        val audioOutput = output as? AVCaptureAudioDataOutput
        if (!running || device == null || audioOutput == null) {
            if (owner === session) {
                stopLocked()
            }
            return
        }
        if (owner === session && configuredDevice === device && configuredOutput === audioOutput) {
            return
        }
        try {
            if (owner != null && owner !== session) {
                AudioCaptureEngine.stop()
            }
            AudioCaptureEngine.configure(device, audioOutput)
            owner = session
            configuredDevice = device
            configuredOutput = audioOutput
            AudioCaptureEngine.start()
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to start audio capture", error)
        }
    }

    @Synchronized
    fun stop(session: AVCaptureSession) {
        if (owner === session) {
            stopLocked()
        }
    }

    fun preferredCaptureDevice(): AVCaptureDevice? {
        return try {
            AVAudioSession.sharedInstance().preferredCaptureDevice()
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to get the preferred audio capture device", error)
            null
        }
    }

    private fun stopLocked() {
        try {
            AudioCaptureEngine.stop()
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to stop audio capture", error)
        }
        owner = null
        configuredDevice = null
        configuredOutput = null
    }
}
