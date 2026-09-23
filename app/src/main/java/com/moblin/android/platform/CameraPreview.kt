package com.moblin.android.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import com.moblin.android.AppDelegate

object CameraPreview {
    private const val TAG = "CameraPreview"
    private const val WIDTH = 1920
    private const val HEIGHT = 1080
    private val thread = HandlerThread("CameraPreview").apply { start() }
    private val handler = Handler(thread.looper)
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var textureView: TextureView? = null
    private var cameraId: String? = null
    private var sensorOrientation = 0

    fun attach(view: TextureView, cameraId: String) {
        textureView = view
        this.cameraId = cameraId
        view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                Log.i(TAG, "Surface available ${width}x$height")
                open()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                view.post { configureTransform(width, height) }
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                close()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
        }
        if (view.isAvailable) {
            open()
        }
    }

    fun stop() {
        close()
    }

    private fun open() {
        val context = AppDelegate.context
        val id = cameraId ?: return
        if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Camera permission not granted")
            return
        }
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        close()
        try {
            sensorOrientation = manager.getCameraCharacteristics(id).get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            manager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.i(TAG, "Camera opened, sensor orientation $sensorOrientation")
                    device = camera
                    startSession(camera)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    device = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error $error")
                    camera.close()
                    device = null
                }
            }, handler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera $id", e)
        }
    }

    private fun startSession(camera: CameraDevice) {
        val view = textureView ?: return
        val texture = view.surfaceTexture ?: return
        texture.setDefaultBufferSize(WIDTH, HEIGHT)
        val surface = Surface(texture)
        view.post { configureTransform(view.width, view.height) }
        val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(surface)
            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        }
        @Suppress("DEPRECATION")
        camera.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(configured: CameraCaptureSession) {
                    Log.i(TAG, "Preview session configured")
                    session = configured
                    runCatching { configured.setRepeatingRequest(request.build(), null, handler) }
                        .onFailure { Log.e(TAG, "Failed to start preview", it) }
                }

                override fun onConfigureFailed(failed: CameraCaptureSession) {
                    Log.e(TAG, "Preview session configuration failed")
                }
            },
            handler,
        )
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val view = textureView ?: return
        if (viewWidth == 0 || viewHeight == 0) {
            return
        }
        val manager = AppDelegate.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        val displayRotation = manager.defaultDisplay.rotation
        val displayDegrees = when (displayRotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        val rotation = (sensorOrientation - displayDegrees + 360) % 360
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (rotation == 90 || rotation == 270) {
            val bufferRect = RectF(0f, 0f, viewHeight.toFloat(), viewWidth.toFloat())
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = maxOf(viewHeight.toFloat() / viewWidth.toFloat(), viewWidth.toFloat() / viewHeight.toFloat())
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate(rotation.toFloat(), centerX, centerY)
        } else if (rotation == 180) {
            matrix.postRotate(180f, centerX, centerY)
        }
        view.setTransform(matrix)
    }

    private fun close() {
        session?.close()
        session = null
        device?.close()
        device = null
    }
}
