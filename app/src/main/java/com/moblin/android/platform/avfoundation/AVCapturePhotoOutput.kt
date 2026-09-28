package com.moblin.android.platform.avfoundation

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.moblin.android.AppDelegate
import com.moblin.android.platform.capture.Camera2Engine
import com.moblin.android.platform.loxen.Loxen
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

private const val TAG = "MoblinCamera"

class AVCapturePhotoSettings {
    var maxPhotoDimensions: Size = Size(0, 0)

    var photoQualityPrioritization: Int = AVCapturePhotoOutput.QualityPrioritization.balanced

    var isShutterSoundSuppressionEnabled = false

    var flashMode: AVCaptureDevice.TorchMode = AVCaptureDevice.TorchMode.off
}

class AVCapturePhoto internal constructor(private val data: ByteArray?) {
    fun fileDataRepresentation(): ByteArray? = data
}

interface AVCapturePhotoCaptureDelegate {
    fun photoOutput(output: AVCapturePhotoOutput, didFinishProcessingPhoto: AVCapturePhoto, error: Throwable?)
}

class AVCapturePhotoOutput : AVCaptureOutput() {
    object QualityPrioritization {
        const val speed = 1
        const val balanced = 2
        const val quality = 3
    }

    var maxPhotoDimensions: Size = Size(0, 0)

    var maxPhotoQualityPrioritization: Int = QualityPrioritization.balanced

    val isShutterSoundSuppressionSupported = true

    fun capturePhoto(settings: AVCapturePhotoSettings, delegate: AVCapturePhotoCaptureDelegate) {
        Camera2Engine.capturePhoto(this, settings, delegate)
    }
}

enum class PHAssetResourceType {
    photo,
    video,
    audio,
}

class PHAssetResourceCreationOptions {
    var originalFilename: String? = null
}

class PHAssetCreationRequest private constructor() {
    internal class Resource(val type: PHAssetResourceType, val data: ByteArray, val options: PHAssetResourceCreationOptions?)

    internal val resources = ArrayList<Resource>()

    fun addResource(with: PHAssetResourceType, data: ByteArray, options: PHAssetResourceCreationOptions?) {
        resources.add(Resource(with, data, options))
    }

    companion object {
        internal val pending = ThreadLocal<MutableList<PHAssetCreationRequest>?>()

        fun forAsset(): PHAssetCreationRequest {
            val request = PHAssetCreationRequest()
            val batch = pending.get()
            if (batch == null) {
                Log.i(TAG, "PHAssetCreationRequest.forAsset called outside performChanges")
            } else {
                batch.add(request)
            }
            return request
        }
    }
}

class PHPhotoLibrary private constructor() {
    private val executor = Executors.newSingleThreadExecutor()

    fun performChanges(changeBlock: () -> Unit, completionHandler: ((Boolean, Throwable?) -> Unit)? = null) {
        executor.execute {
            val requests = ArrayList<PHAssetCreationRequest>()
            var error: Throwable? = null
            PHAssetCreationRequest.pending.set(requests)
            try {
                changeBlock()
            } catch (changeError: Throwable) {
                error = changeError
            } finally {
                PHAssetCreationRequest.pending.set(null)
            }
            if (error == null) {
                try {
                    for (request in requests) {
                        for (resource in request.resources) {
                            save(resource)
                        }
                    }
                } catch (saveError: Throwable) {
                    error = saveError
                }
            }
            try {
                completionHandler?.invoke(error == null, error)
            } catch (handlerError: Throwable) {
                Log.e(TAG, "Photo library completion handler failed", handlerError)
            }
        }
    }

    private fun save(resource: PHAssetCreationRequest.Resource) {
        if (resource.type != PHAssetResourceType.photo) {
            throw AVError(AVError.unknown, "Only photos can be saved")
        }
        val resolver = AppDelegate.context.contentResolver
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, resource.options?.originalFilename ?: "${Loxen.appName}_$timestamp.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/${Loxen.appName}")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw AVError(AVError.unknown, "Failed to create photo")
        try {
            resolver.openOutputStream(uri)?.use { it.write(resource.data) }
                ?: throw AVError(AVError.unknown, "Failed to open photo")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            }
            Log.i(TAG, "Saved photo $uri")
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    companion object {
        private val shared = PHPhotoLibrary()

        fun shared(): PHPhotoLibrary = shared

        fun requestAuthorization(`for`: PHAccessLevel, handler: (PHAuthorizationStatus) -> Unit) {
            PhotoLibraryAuthorization.request(handler)
        }
    }
}

enum class PHAccessLevel(val rawValue: Int) {
    addOnly(1),
    readWrite(2),
}

enum class PHAuthorizationStatus(val rawValue: Int) {
    notDetermined(0),
    restricted(1),
    denied(2),
    authorized(3),
    limited(4),
}

object PhotoLibraryAuthorization {
    private const val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
    private val handler = Handler(Looper.getMainLooper())
    private val waiting = mutableListOf<(PHAuthorizationStatus) -> Unit>()
    private var activity: WeakReference<ComponentActivity>? = null
    private var requested = false
    private var requesting = false
    internal var sdkInt = Build.VERSION.SDK_INT

    fun install(activity: ComponentActivity) {
        this.activity = WeakReference(activity)
        requesting = false
        activity.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY && this.activity?.get() === activity) {
                    this.activity = null
                    requesting = false
                    finish(status())
                }
            },
        )
    }

    internal fun status(): PHAuthorizationStatus {
        if (sdkInt >= Build.VERSION_CODES.Q) {
            return PHAuthorizationStatus.authorized
        }
        val activity = activity?.get()
        val context: Context = activity ?: applicationContext() ?: return PHAuthorizationStatus.notDetermined
        if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            return PHAuthorizationStatus.authorized
        }
        if (requested || (activity != null && activity.shouldShowRequestPermissionRationale(permission))) {
            return PHAuthorizationStatus.denied
        }
        return PHAuthorizationStatus.notDetermined
    }

    internal fun request(completion: (PHAuthorizationStatus) -> Unit) {
        handler.post {
            val status = status()
            if (status != PHAuthorizationStatus.notDetermined) {
                completion(status)
                return@post
            }
            waiting.add(completion)
            if (!requesting) {
                launchRequest()
            }
        }
    }

    private fun launchRequest() {
        val activity = activity?.get()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            finish(status())
            return
        }
        var launcher: ActivityResultLauncher<Array<String>>? = null
        val registered = activity.activityResultRegistry.register(
            "PHPhotoLibrary.requestAuthorization",
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            launcher?.unregister()
            requesting = false
            if (results.isNotEmpty()) {
                requested = true
            }
            finish(status())
        }
        launcher = registered
        requesting = true
        try {
            registered.launch(arrayOf(permission))
        } catch (error: RuntimeException) {
            Log.i(TAG, "Failed to request photo library permission: ${error.message}")
            requesting = false
            registered.unregister()
            finish(status())
        }
    }

    private fun finish(status: PHAuthorizationStatus) {
        val completions = waiting.toList()
        waiting.clear()
        for (completion in completions) {
            completion(status)
        }
    }

    private fun applicationContext(): Context? = try {
        AppDelegate.context
    } catch (error: UninitializedPropertyAccessException) {
        null
    }

    internal fun reset() {
        waiting.clear()
        activity = null
        requested = false
        requesting = false
        sdkInt = Build.VERSION.SDK_INT
    }
}
