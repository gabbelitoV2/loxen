package com.moblin.android.platform.mediaplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import com.moblin.android.AppDelegate
import com.moblin.android.platform.core.NotificationCenter
import kotlin.math.roundToInt

private const val TAG = "MoblinSystemVolume"

object SystemVolume {
    const val didChangeNotification = "SystemVolumeDidChange"
    const val explicitVolumeChange = "ExplicitVolumeChange"
    internal const val volumeChangedAction = "android.media.VOLUME_CHANGED_ACTION"
    internal const val extraStreamType = "android.media.EXTRA_VOLUME_STREAM_TYPE"
    internal const val extraStreamValue = "android.media.EXTRA_VOLUME_STREAM_VALUE"
    internal const val stream = AudioManager.STREAM_MUSIC

    private val lock = Any()
    private var observedContext: Context? = null
    private var latestIndex: Int? = null
    private var sequenceNumber = 0

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != volumeChangedAction || intent.getIntExtra(extraStreamType, -1) != stream) {
                return
            }
            val index = intent.getIntExtra(extraStreamValue, -1)
            if (index < 0) {
                return
            }
            val manager = audioManager() ?: return
            post(index = index, minIndex = minIndex(manager), maxIndex = manager.getStreamMaxVolume(stream))
        }
    }

    private val volumeKeyListener = ViewCompat.OnUnhandledKeyEventListenerCompat { _, event ->
        volumeKeyPressed(event)
        false
    }

    var volume: Float
        get() {
            val manager = audioManager() ?: return 0f
            return indexToVolume(
                index = manager.getStreamVolume(stream),
                minIndex = minIndex(manager),
                maxIndex = manager.getStreamMaxVolume(stream),
            )
        }
        set(newValue) {
            val manager = audioManager() ?: return
            val index = volumeToIndex(
                volume = newValue,
                minIndex = minIndex(manager),
                maxIndex = manager.getStreamMaxVolume(stream),
            )
            try {
                manager.setStreamVolume(stream, index, 0)
            } catch (error: RuntimeException) {
                Log.i(TAG, "Failed to set media volume to $index: ${error.message}")
            }
        }

    fun startObserving() {
        val context = applicationContext() ?: return
        synchronized(lock) {
            if (observedContext === context) {
                return
            }
            observedContext = context
        }
        val filter = IntentFilter(volumeChangedAction)
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (error: RuntimeException) {
            Log.i(TAG, "Failed to observe media volume: ${error.message}")
        }
    }

    fun install(activity: ComponentActivity) {
        activity.volumeControlStream = stream
        try {
            ViewCompat.addOnUnhandledKeyEventListener(activity.window.decorView, volumeKeyListener)
        } catch (error: RuntimeException) {
            Log.i(TAG, "Failed to observe volume keys: ${error.message}")
        }
    }

    internal fun volumeKeyPressed(event: KeyEvent) {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) {
            return
        }
        val up = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP
        if (!up && event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return
        }
        val manager = audioManager() ?: return
        try {
            if (manager.mode != AudioManager.MODE_NORMAL || manager.isVolumeFixed) {
                return
            }
            val index = manager.getStreamVolume(stream)
            val minIndex = minIndex(manager)
            val maxIndex = manager.getStreamMaxVolume(stream)
            if ((up && index >= maxIndex) || (!up && index <= minIndex)) {
                post(index = index, minIndex = minIndex, maxIndex = maxIndex, unchanged = true)
            }
        } catch (error: RuntimeException) {
            Log.i(TAG, "Failed to read media volume: ${error.message}")
        }
    }

    internal fun post(index: Int, minIndex: Int, maxIndex: Int, unchanged: Boolean = false) {
        val number = synchronized(lock) {
            if (unchanged || latestIndex != index) {
                latestIndex = index
                sequenceNumber += 1
            }
            sequenceNumber
        }
        NotificationCenter.default.post(
            didChangeNotification,
            null,
            mapOf(
                "Volume" to indexToVolume(index = index, minIndex = minIndex, maxIndex = maxIndex),
                "Reason" to explicitVolumeChange,
                "SequenceNumber" to number,
            ),
        )
    }

    internal fun indexToVolume(index: Int, minIndex: Int, maxIndex: Int): Float {
        if (maxIndex <= minIndex) {
            return 0f
        }
        return ((index - minIndex).toFloat() / (maxIndex - minIndex)).coerceIn(0f, 1f)
    }

    internal fun volumeToIndex(volume: Float, minIndex: Int, maxIndex: Int): Int {
        if (maxIndex <= minIndex || volume.isNaN()) {
            return minIndex
        }
        return minIndex + (volume.coerceIn(0f, 1f) * (maxIndex - minIndex)).roundToInt()
    }

    internal fun minIndex(manager: AudioManager): Int {
        if (Build.VERSION.SDK_INT < 28) {
            return 0
        }
        return try {
            manager.getStreamMinVolume(stream)
        } catch (error: RuntimeException) {
            0
        }
    }

    private fun applicationContext(): Context? {
        return try {
            AppDelegate.context.applicationContext
        } catch (error: UninitializedPropertyAccessException) {
            null
        }
    }

    private fun audioManager(): AudioManager? {
        return applicationContext()?.getSystemService(AudioManager::class.java)
    }
}
