package com.moblin.android.platform

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.moblin.android.MoblinApp
import com.moblin.android.various.utils.bestBackCameraId

object AndroidHost {
    private const val TAG = "AndroidHost"
    private val handler = Handler(Looper.getMainLooper())
    private val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    fun onActivityCreated(activity: ComponentActivity) {
        Log.i(TAG, "Activity created")
        val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results[Manifest.permission.CAMERA] == true) {
                startPreviewWhenReady()
            } else {
                Log.i(TAG, "Camera permission denied")
            }
        }
        val missing = permissions.filter { activity.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            startPreviewWhenReady()
        } else {
            launcher.launch(missing.toTypedArray())
        }
    }

    private fun startPreviewWhenReady() {
        val model = MoblinApp.globalModel
        if (model == null) {
            Log.i(TAG, "Waiting for the model")
            handler.postDelayed({ startPreviewWhenReady() }, 300)
            return
        }
        val cameraId = bestBackCameraId
        if (cameraId.isEmpty()) {
            Log.i(TAG, "No back camera found")
            return
        }
        Log.i(TAG, "Attaching preview to camera $cameraId, view available: ${model.streamPreviewView.isAvailable}, attached: ${model.streamPreviewView.isAttachedToWindow}")
        CameraPreview.attach(model.streamPreviewView, cameraId)
    }
}
