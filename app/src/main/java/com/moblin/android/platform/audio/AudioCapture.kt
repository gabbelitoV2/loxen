package com.moblin.android.platform.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioRouting
import android.media.AudioTimestamp
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import com.moblin.android.AppDelegate
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.avfoundation.AVAudioSession
import com.moblin.android.platform.core.PipelineStats
import kotlin.math.abs

private const val TAG = "MoblinAudio"

internal data class AudioCaptureConfig(
    val portUid: String,
    val deviceId: Int?,
    val dataSourceId: Int?,
    val isBuiltIn: Boolean,
    val microphoneDirection: Int,
    val channelCount: Int,
    val useChannelIndexMask: Boolean,
    val audioSource: Int,
    val isBluetooth: Boolean,
) {
    var deviceInfo: AudioDeviceInfo? = null
        private set

    var captureDeviceUniqueId: String? = null
        private set

    companion object {
        fun resolve(captureDeviceUniqueId: String?): AudioCaptureConfig? {
            val session = AVAudioSession.sharedInstance()
            val selection = session.captureSelection()
            if (selection == null) {
                Log.w(TAG, "No audio input available")
                return null
            }
            val port = selection.port
            val dataSource = selection.dataSource
            val isBuiltIn = port.portType == AVAudioSession.Port.builtInMic
            val deviceInfo = if (isBuiltIn) {
                session.builtInMicDevice(dataSource?.builtInMicAddress)
            } else {
                port.audioDeviceInfo
            }
            val isBluetooth = AVAudioSession.isBluetoothInput(deviceInfo)
            var channelCount = 1
            var useChannelIndexMask = false
            if (isBuiltIn) {
                channelCount = if (selection.stereo) 2 else 1
            } else if (!isBluetooth) {
                val maximumChannels = deviceInfo?.channelCounts?.maxOrNull() ?: 1
                if (maximumChannels > 2) {
                    channelCount = minOf(maximumChannels, 8)
                    useChannelIndexMask = true
                } else if (maximumChannels == 2) {
                    channelCount = 2
                }
            }
            val audioSource = if (isBluetooth) {
                MediaRecorder.AudioSource.VOICE_COMMUNICATION
            } else {
                MediaRecorder.AudioSource.CAMCORDER
            }
            val config = AudioCaptureConfig(
                portUid = port.uid,
                deviceId = deviceInfo?.id,
                dataSourceId = dataSource?.dataSourceID,
                isBuiltIn = isBuiltIn,
                microphoneDirection = if (isBuiltIn) {
                    dataSource?.microphoneDirection ?: AVAudioSession.MicrophoneDirection.unspecified
                } else {
                    AVAudioSession.MicrophoneDirection.unspecified
                },
                channelCount = channelCount,
                useChannelIndexMask = useChannelIndexMask,
                audioSource = audioSource,
                isBluetooth = isBluetooth,
            )
            config.deviceInfo = deviceInfo
            config.captureDeviceUniqueId = captureDeviceUniqueId
            return config
        }
    }
}

internal class AudioCapture private constructor(
    val config: AudioCaptureConfig,
    private val record: AudioRecord,
    private val channelCount: Int,
    private val usesCommunicationDevice: Boolean,
    private val onFailure: (AudioCapture) -> Unit,
) {
    @Volatile
    var sink: ((MediaSample) -> Unit)? = null

    @Volatile
    private var running = true

    @Volatile
    private var failed = false

    private val format = makePcmFormat(audioCaptureSampleRate, channelCount)
    private val thread = Thread({ readLoop() }, "audio-capture")
    private val routingListener = object : AudioRouting.OnRoutingChangedListener {
        override fun onRoutingChanged(router: AudioRouting) {
            if (running) {
                AVAudioSession.sharedInstance().captureRoutedDeviceChanged(router.routedDevice)
            }
        }
    }

    fun isHealthy(): Boolean {
        return running && !failed && thread.isAlive
    }

    fun stop() {
        running = false
        try {
            record.stop()
        } catch (error: Exception) {
            Log.d(TAG, "AudioRecord stop failed: $error")
        }
        try {
            thread.join(500)
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        try {
            record.removeOnRoutingChangedListener(routingListener)
        } catch (error: Exception) {
            Log.d(TAG, "Failed to remove routing listener: $error")
        }
        try {
            record.release()
        } catch (error: Exception) {
            Log.d(TAG, "AudioRecord release failed: $error")
        }
        if (usesCommunicationDevice) {
            clearCommunicationDevice()
        }
        AVAudioSession.sharedInstance().captureStopped()
        Log.i(TAG, "AudioRecord stopped (${config.portUid})")
    }

    private fun begin(): Boolean {
        try {
            record.addOnRoutingChangedListener(routingListener, Handler(Looper.getMainLooper()))
        } catch (error: Exception) {
            Log.d(TAG, "Failed to add routing listener: $error")
        }
        try {
            record.startRecording()
        } catch (error: Exception) {
            Log.w(TAG, "AudioRecord startRecording failed: $error")
            return false
        }
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            Log.w(TAG, "AudioRecord is not recording after start (state ${record.recordingState})")
            try {
                record.stop()
            } catch (error: Exception) {
                Log.d(TAG, "AudioRecord stop failed: $error")
            }
            return false
        }
        thread.isDaemon = true
        thread.start()
        Log.i(
            TAG,
            "AudioRecord $audioCaptureSampleRate Hz $channelCount ch source=${sourceName(config.audioSource)} " +
                "device=${config.portUid}" +
                (config.dataSourceId?.let { " dataSource=$it" } ?: "") +
                " direction=${config.microphoneDirection} routed=${routedDeviceName()}",
        )
        return true
    }

    private fun routedDeviceName(): String {
        return try {
            record.routedDevice?.let { AVAudioSession.describe(it) } ?: "none"
        } catch (error: Exception) {
            "unknown"
        }
    }

    private fun readLoop() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        } catch (error: Exception) {
            Log.w(TAG, "Failed to raise audio capture priority: $error")
        }
        val bytesPerFrame = channelCount * 2
        val chunkBytes = audioCaptureFramesPerBuffer * bytesPerFrame
        val frameDurationNs = 1_000_000_000L * audioCaptureFramesPerBuffer / audioCaptureSampleRate
        val timestamp = AudioTimestamp()
        var framesRead = 0L
        var lastPresentationTimeUs = Long.MIN_VALUE
        var zeroReads = 0
        while (running) {
            val data = ByteArray(chunkBytes)
            var offset = 0
            while (offset < chunkBytes && running) {
                val result = try {
                    record.read(data, offset, chunkBytes - offset, AudioRecord.READ_BLOCKING)
                } catch (error: Exception) {
                    Log.w(TAG, "AudioRecord read threw: $error")
                    AudioRecord.ERROR
                }
                if (result > 0) {
                    offset += result
                    zeroReads = 0
                } else if (result == 0) {
                    zeroReads += 1
                    if (zeroReads > 200) {
                        fail(result)
                        return
                    }
                    sleepQuietly(5)
                } else {
                    if (running) {
                        fail(result)
                    }
                    return
                }
            }
            if (!running || offset < chunkBytes) {
                break
            }
            val nowNs = System.nanoTime()
            var presentationTimeNs = nowNs - frameDurationNs
            val status = try {
                record.getTimestamp(timestamp, AudioTimestamp.TIMEBASE_MONOTONIC)
            } catch (error: Exception) {
                AudioRecord.ERROR
            }
            if (status == AudioRecord.SUCCESS) {
                val timestampNs = timestamp.nanoTime +
                    (framesRead - timestamp.framePosition) * 1_000_000_000L / audioCaptureSampleRate
                if (abs(timestampNs - presentationTimeNs) < 500_000_000L) {
                    presentationTimeNs = timestampNs
                }
            }
            framesRead += audioCaptureFramesPerBuffer
            var presentationTimeUs = presentationTimeNs / 1000
            if (presentationTimeUs <= lastPresentationTimeUs) {
                presentationTimeUs = lastPresentationTimeUs + 1
            }
            lastPresentationTimeUs = presentationTimeUs
            PipelineStats.increment("micIn")
            try {
                sink?.invoke(MediaSample(data, presentationTimeUs, true, format))
            } catch (error: Throwable) {
                Log.e(TAG, "Audio sink failed", error)
            }
        }
    }

    private fun fail(result: Int) {
        if (failed) {
            return
        }
        failed = true
        Log.w(TAG, "AudioRecord read failed with $result (${config.portUid})")
        onFailure(this)
    }

    companion object {
        fun start(config: AudioCaptureConfig, onFailure: (AudioCapture) -> Unit): AudioCapture? {
            if (!hasRecordPermission()) {
                Log.i(TAG, "RECORD_AUDIO is not granted, audio capture not started")
                return null
            }
            val usesCommunicationDevice = config.isBluetooth && setCommunicationDevice(config.deviceInfo)
            val attempts = mutableListOf(config.channelCount to config.useChannelIndexMask)
            if (config.channelCount > 2) {
                attempts.add(2 to false)
            }
            if (config.channelCount > 1) {
                attempts.add(1 to false)
            }
            var createdRecord: AudioRecord? = null
            var channelCount = config.channelCount
            for ((count, useIndexMask) in attempts) {
                createdRecord = buildRecord(config.audioSource, count, useIndexMask)
                if (createdRecord != null) {
                    channelCount = count
                    break
                }
            }
            val record = createdRecord
            if (record == null) {
                Log.w(TAG, "Failed to create AudioRecord for ${config.portUid}")
                if (usesCommunicationDevice) {
                    clearCommunicationDevice()
                }
                return null
            }
            config.deviceInfo?.let { deviceInfo ->
                val preferred = try {
                    record.setPreferredDevice(deviceInfo)
                } catch (error: Exception) {
                    false
                }
                if (!preferred) {
                    Log.w(TAG, "setPreferredDevice(${AVAudioSession.describe(deviceInfo)}) was rejected")
                }
            }
            if (config.isBuiltIn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val directionSet = try {
                    record.setPreferredMicrophoneDirection(config.microphoneDirection)
                } catch (error: Exception) {
                    false
                }
                if (!directionSet) {
                    Log.i(TAG, "setPreferredMicrophoneDirection(${config.microphoneDirection}) was rejected")
                }
            }
            val capture = AudioCapture(config, record, channelCount, usesCommunicationDevice, onFailure)
            if (!capture.begin()) {
                try {
                    record.release()
                } catch (error: Exception) {
                    Log.d(TAG, "AudioRecord release failed: $error")
                }
                if (usesCommunicationDevice) {
                    clearCommunicationDevice()
                }
                return null
            }
            return capture
        }

        private fun hasRecordPermission(): Boolean {
            return try {
                AppDelegate.context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
            } catch (error: Throwable) {
                false
            }
        }

        @SuppressLint("MissingPermission")
        private fun buildRecord(audioSource: Int, channelCount: Int, useChannelIndexMask: Boolean): AudioRecord? {
            val channelMask = if (channelCount == 2) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
            val formatBuilder = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(audioCaptureSampleRate)
            if (useChannelIndexMask) {
                formatBuilder.setChannelIndexMask((1 shl channelCount) - 1)
            } else {
                formatBuilder.setChannelMask(channelMask)
            }
            val minimumBufferSize = if (useChannelIndexMask) {
                0
            } else {
                AudioRecord.getMinBufferSize(audioCaptureSampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
            }
            val bufferSize = maxOf(minimumBufferSize * 2, audioCaptureFramesPerBuffer * channelCount * 2 * 8)
            return try {
                val record = AudioRecord.Builder()
                    .setAudioSource(audioSource)
                    .setAudioFormat(formatBuilder.build())
                    .setBufferSizeInBytes(bufferSize)
                    .build()
                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    record.release()
                    Log.w(TAG, "AudioRecord with $channelCount channels did not initialize")
                    null
                } else {
                    record
                }
            } catch (error: Exception) {
                Log.w(TAG, "AudioRecord with $channelCount channels failed: $error")
                null
            }
        }

        private fun setCommunicationDevice(deviceInfo: AudioDeviceInfo?): Boolean {
            if (deviceInfo == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return false
            }
            val manager = AVAudioSession.audioManager() ?: return false
            return try {
                val address = AVAudioSession.addressOf(deviceInfo)
                val communicationDevice = manager.availableCommunicationDevices.firstOrNull {
                    it.type == deviceInfo.type && AVAudioSession.addressOf(it) == address
                } ?: manager.availableCommunicationDevices.firstOrNull { it.type == deviceInfo.type }
                if (communicationDevice == null) {
                    Log.w(TAG, "No communication device for ${AVAudioSession.describe(deviceInfo)}")
                    false
                } else {
                    manager.setCommunicationDevice(communicationDevice)
                }
            } catch (error: Exception) {
                Log.w(TAG, "setCommunicationDevice failed: $error")
                false
            }
        }

        private fun clearCommunicationDevice() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return
            }
            try {
                AVAudioSession.audioManager()?.clearCommunicationDevice()
            } catch (error: Exception) {
                Log.d(TAG, "clearCommunicationDevice failed: $error")
            }
        }

        private fun sourceName(audioSource: Int): String {
            return when (audioSource) {
                MediaRecorder.AudioSource.CAMCORDER -> "CAMCORDER"
                MediaRecorder.AudioSource.MIC -> "MIC"
                MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
                MediaRecorder.AudioSource.UNPROCESSED -> "UNPROCESSED"
                else -> audioSource.toString()
            }
        }

        private fun sleepQuietly(milliseconds: Long) {
            try {
                Thread.sleep(milliseconds)
            } catch (error: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }
}
