package com.moblin.android.platform

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.moblin.android.AppDelegate
import com.moblin.android.MoblinApp
import com.moblin.android.platform.capture.Camera2Engine
import com.moblin.android.platform.host.StreamingService
import com.moblin.android.platform.host.SystemEvents
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.reloadAudioSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import com.moblin.android.various.model.handleSettingsUrls

object AndroidHost {
    private const val TAG = "AndroidHost"
    private val handler = Handler(Looper.getMainLooper())
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var boundModel: Model? = null
    private var streamingStateJob: Job? = null

    private val permissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        }

    fun onActivityCreated(activity: ComponentActivity) {
        Log.i(TAG, "Activity created")
        SystemEvents.install(activity.application)
        com.moblin.android.platform.offscreen.OffscreenDisplay.prewarm()
        val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results[Manifest.permission.CAMERA] == true) {
                onCameraPermissionGranted()
            } else if (results.containsKey(Manifest.permission.CAMERA)) {
                Log.i(TAG, "Camera permission denied")
            }
            if (results[Manifest.permission.RECORD_AUDIO] == true) {
                onMicrophonePermissionGranted()
            } else if (results.containsKey(Manifest.permission.RECORD_AUDIO)) {
                Log.i(TAG, "Microphone permission denied")
            }
        }
        val missing = permissions.filter { activity.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            launcher.launch(missing.toTypedArray())
        }
        pendingUrls.addAll(settingsUrls(activity.intent))
        activity.addOnNewIntentListener { intent ->
            pendingUrls.addAll(settingsUrls(intent))
            deliverSettingsUrls()
        }
        bindModelWhenReady()
    }

    private val pendingUrls = mutableListOf<Uri>()

    private fun settingsUrls(intent: Intent?): List<Uri> {
        val data = intent?.data ?: return emptyList()
        return if (data.scheme == "moblin") listOf(data) else emptyList()
    }

    private fun deliverSettingsUrls() {
        val model = boundModel ?: return
        if (pendingUrls.isEmpty()) {
            return
        }
        val urls = pendingUrls.toSet()
        pendingUrls.clear()
        Log.i(TAG, "Handling ${urls.size} settings URL(s)")
        model.handleSettingsUrls(urls)
    }

    private fun bindModelWhenReady() {
        val model = MoblinApp.globalModel
        if (model == null) {
            handler.postDelayed({ bindModelWhenReady() }, 300)
            return
        }
        if (boundModel === model) {
            return
        }
        boundModel = model
        Log.i(TAG, "Binding to the model")
        SystemEvents.start(model)
        observeStreamingState(model)
        deliverSettingsUrls()
    }

    private fun observeStreamingState(model: Model) {
        streamingStateJob?.cancel()
        streamingStateJob = mainScope.launch {
            combine(model.isLive, model.isRecording) { isLive, isRecording -> isLive || isRecording }
                .distinctUntilChanged()
                .collect { active ->
                    if (active) {
                        StreamingService.start(AppDelegate.context)
                    } else {
                        StreamingService.stop(AppDelegate.context)
                    }
                }
        }
    }

    private fun onCameraPermissionGranted() {
        Log.i(TAG, "Camera permission granted")
        Camera2Engine.cameraPermissionGranted()
    }

    private fun onMicrophonePermissionGranted() {
        Log.i(TAG, "Microphone permission granted")
        withModel { model ->
            model.reloadAudioSession()
        }
    }

    private fun withModel(block: (Model) -> Unit) {
        val model = MoblinApp.globalModel
        if (model == null) {
            handler.postDelayed({ withModel(block) }, 300)
            return
        }
        block(model)
    }
}
