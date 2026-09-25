package com.moblin.android.platform.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log
import com.moblin.android.AppDelegate
import java.io.OutputStream

private const val TAG = "SpeechRecognition"

interface SpeechRecognitionEngine {
    fun startListening(intent: Intent, listener: RecognitionListener)

    fun stopListening()

    fun cancel()

    fun destroy()

    fun triggerModelDownload(intent: Intent)
}

interface SpeechRecognitionBackend {
    val sdkInt: Int

    val packageName: String

    fun hasRecordAudioPermission(): Boolean

    fun isRecognitionAvailable(): Boolean

    fun isOnDeviceRecognitionAvailable(): Boolean

    fun createEngine(onDevice: Boolean): SpeechRecognitionEngine?

    fun createAudioPipe(): Array<ParcelFileDescriptor>

    fun openAudioOutput(writeSide: ParcelFileDescriptor): OutputStream {
        return ParcelFileDescriptor.AutoCloseOutputStream(writeSide)
    }
}

object SpeechRecognitionSystem {
    private var override: SpeechRecognitionBackend? = null
    private val android by lazy { AndroidSpeechRecognitionBackend(AppDelegate.context) }
    internal val onDeviceUnavailableLanguageTags = mutableSetOf<String>()

    var backend: SpeechRecognitionBackend
        get() = override ?: android
        set(value) {
            override = value
        }

    fun reset() {
        override = null
        onDeviceUnavailableLanguageTags.clear()
    }
}

internal class AndroidSpeechRecognitionBackend(private val context: Context) : SpeechRecognitionBackend {
    override val sdkInt: Int
        get() = Build.VERSION.SDK_INT

    override val packageName: String
        get() = context.packageName

    override fun hasRecordAudioPermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    override fun isRecognitionAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (error: RuntimeException) {
            false
        }
    }

    override fun isOnDeviceRecognitionAvailable(): Boolean {
        if (sdkInt < Build.VERSION_CODES.S) {
            return false
        }
        return try {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        } catch (error: RuntimeException) {
            false
        }
    }

    override fun createEngine(onDevice: Boolean): SpeechRecognitionEngine? {
        val recognizer = try {
            if (onDevice && sdkInt >= Build.VERSION_CODES.S) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        } catch (error: RuntimeException) {
            Log.i(TAG, "Failed to create speech recognizer (on device: $onDevice): $error")
            null
        } ?: return null
        return AndroidSpeechRecognitionEngine(recognizer, sdkInt)
    }

    override fun createAudioPipe(): Array<ParcelFileDescriptor> {
        return ParcelFileDescriptor.createPipe()
    }
}

private class AndroidSpeechRecognitionEngine(
    private val recognizer: SpeechRecognizer,
    private val sdkInt: Int,
) : SpeechRecognitionEngine {
    override fun startListening(intent: Intent, listener: RecognitionListener) {
        recognizer.setRecognitionListener(listener)
        recognizer.startListening(intent)
    }

    override fun stopListening() {
        recognizer.stopListening()
    }

    override fun cancel() {
        recognizer.cancel()
    }

    override fun destroy() {
        try {
            recognizer.destroy()
        } catch (error: RuntimeException) {
            Log.i(TAG, "Failed to destroy speech recognizer: $error")
        }
    }

    override fun triggerModelDownload(intent: Intent) {
        if (sdkInt < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        try {
            recognizer.triggerModelDownload(intent)
        } catch (error: RuntimeException) {
            Log.i(TAG, "Failed to trigger speech model download: $error")
        }
    }
}
