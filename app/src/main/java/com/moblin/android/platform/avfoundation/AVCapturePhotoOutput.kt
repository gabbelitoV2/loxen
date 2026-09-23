package com.moblin.android.platform.avfoundation

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import com.moblin.android.AppDelegate
import com.moblin.android.platform.capture.Camera2Engine
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
            put(MediaStore.Images.Media.DISPLAY_NAME, resource.options?.originalFilename ?: "Moblin_$timestamp.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Moblin")
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
    }
}
