package com.moblin.android.various

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.moblin.android.platform.log.Log
import com.moblin.android.AppDelegate

private const val TAG = "Haptics"

interface HapticsDelegate {
    fun hapticsActiveChanged()
}

open class Haptics {
    open var delegate: HapticsDelegate? = null

    var isActive = false
        private set

    private var engine: Vibrator? = null
    private var player: Vibrator? = null
    private var generation = 0
    private val stopTimer = MainTimer()

    open fun start(duration: Double?, intensity: Float, sharpness: Float) {
        stop()
        val context = AppDelegate.context
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
        if (vibrator == null || !vibrator.hasVibrator()) {
            return
        }
        generation += 1
        isActive = true
        delegate?.hapticsActiveChanged()
        try {
            val engine = vibrator
            val timings = longArrayOf(0L, 30_000L)
            val effect = if (engine.hasAmplitudeControl()) {
                val amplitude = (intensity.coerceIn(0f, 1f) * 255f).toInt().coerceIn(1, 255)
                VibrationEffect.createWaveform(timings, intArrayOf(0, amplitude), 0)
            } else {
                VibrationEffect.createWaveform(timings, 0)
            }
            engine.vibrate(effect)
            this.engine = engine
            this.player = engine
        } catch (error: Exception) {
            Log.i(TAG, "haptics: Failed to start with error: $error")
            stop()
            return
        }
        if (duration != null) {
            stopTimer.startSingleShot(timeout = duration) {
                this.stop()
            }
        }
    }

    open fun stop() {
        stopTimer.stop()
        player?.cancel()
        player = null
        engine?.cancel()
        engine = null
        if (isActive) {
            isActive = false
            delegate?.hapticsActiveChanged()
        }
    }

    private fun stop(generation: Int) {
        if (generation != this.generation) {
            return
        }
        stop()
    }
}
