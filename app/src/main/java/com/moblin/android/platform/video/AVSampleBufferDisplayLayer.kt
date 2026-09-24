package com.moblin.android.platform.video

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLSurface
import android.os.SystemClock
import android.util.Log
import android.view.TextureView
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.video.PreviewView
import com.moblin.android.media.haishinkit.media.video.VideoGravity
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.core.PipelineThread
import java.lang.ref.WeakReference
import java.util.WeakHashMap

private const val TAG = "MoblinPreview"

private val displayLayers = WeakHashMap<PreviewView, AVSampleBufferDisplayLayer>()

val PreviewView.layer: AVSampleBufferDisplayLayer
    get() = synchronized(displayLayers) {
        displayLayers.getOrPut(this) { AVSampleBufferDisplayLayer(this) }
    }

class AVSampleBufferDisplayLayer internal constructor(view: TextureView) {
    enum class Status {
        unknown,
        rendering,
        failed,
    }

    @Volatile
    var status: Status = Status.unknown
        private set

    private class InFlight(val sampleBuffer: MediaSample, var leased: Boolean)

    private val viewRef = WeakReference(view)
    private val name = "layer@${Integer.toHexString(System.identityHashCode(view))}"
    private val lock = Any()
    private val inFlight = ArrayDeque<InFlight>()
    private var pendingBuffer: CVPixelBuffer? = null
    private var pendingRemoveImage = false
    private var pendingFlush = false
    private var isProcessScheduled = false
    private var surfaceTexture: SurfaceTexture? = null
    private var eglSurface: EGLSurface? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var lastBuffer: CVPixelBuffer? = null
    private var latestCreateAttemptTime = 0L
    private var latestFailMessage = ""
    private var latestFailLogTime = 0L

    private val listener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            PipelineThread.post { surfaceAvailable(surface, width, height) }
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            PipelineThread.post { surfaceSizeChanged(surface, width, height) }
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            PipelineThread.post { surfaceDestroyed(surface) }
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        }
    }

    fun setup() {
        val view = viewRef.get() ?: return
        view.surfaceTextureListener = listener
        val surface = view.surfaceTexture
        if (view.isAvailable && surface != null) {
            val width = view.width
            val height = view.height
            PipelineThread.post { surfaceAvailable(surface, width, height) }
        }
    }

    fun enqueue(sampleBuffer: MediaSample) {
        val buffer = sampleBuffer.imageBuffer ?: return
        setPendingBuffer(buffer, sampleBuffer)
    }

    fun retainInFlight(sampleBuffer: MediaSample) {
        val buffer = sampleBuffer.imageBuffer
        val leased = buffer != null && PixelBufferLeases.retain(buffer, "display layer in flight")
        val superseded = synchronized(lock) {
            val newest = if (leased) inFlight.lastOrNull { it.leased } else null
            newest?.leased = false
            inFlight.addLast(InFlight(sampleBuffer, leased))
            newest?.sampleBuffer?.imageBuffer
        }
        releaseLease(superseded)
    }

    fun releaseInFlight(sampleBuffer: MediaSample) {
        val released = synchronized(lock) {
            val index = inFlight.indexOfFirst { it.sampleBuffer === sampleBuffer }
            if (index < 0) {
                return
            }
            val entry = inFlight.removeAt(index)
            if (entry.leased) entry.sampleBuffer.imageBuffer else null
        }
        releaseLease(released)
    }

    fun flush() {
        val replaced = synchronized(lock) {
            val replaced = pendingBuffer
            pendingBuffer = null
            pendingFlush = true
            scheduleProcess()
            replaced
        }
        releaseLease(replaced)
    }

    fun flushAndRemoveImage() {
        val replaced = synchronized(lock) {
            val replaced = pendingBuffer
            pendingBuffer = null
            pendingRemoveImage = true
            scheduleProcess()
            replaced
        }
        releaseLease(replaced)
    }

    fun drawOnPipeline(buffer: CVPixelBuffer) {
        if (!PipelineThread.isCurrent()) {
            setPendingBuffer(buffer)
            return
        }
        if (!buffer.checkReadable("camera preview")) {
            return
        }
        setLastBuffer(buffer)
        if (render(buffer)) {
            PipelineStats.increment("preview")
        }
    }

    private fun setPendingBuffer(buffer: CVPixelBuffer, sampleBuffer: MediaSample? = null) {
        val replaced = synchronized(lock) {
            if (sampleBuffer != null && inFlight.firstOrNull { it.sampleBuffer === sampleBuffer }?.leased == false) {
                return
            }
            if (!PixelBufferLeases.retain(buffer, "display layer enqueue")) {
                return
            }
            val replaced = pendingBuffer
            pendingBuffer = buffer
            scheduleProcess()
            replaced
        }
        releaseLease(replaced)
    }

    private fun setLastBuffer(buffer: CVPixelBuffer?) {
        val old = lastBuffer
        if (buffer === old) {
            return
        }
        if (buffer != null && !PixelBufferLeases.retain(buffer, "display layer")) {
            return
        }
        lastBuffer = buffer
        releaseLease(old)
    }

    private fun redrawBuffer(): CVPixelBuffer? {
        val buffer = lastBuffer ?: return null
        return if (buffer.checkReadable("display layer redraw")) buffer else null
    }

    private fun scheduleProcess() {
        if (isProcessScheduled) {
            return
        }
        isProcessScheduled = true
        PipelineThread.post { process() }
    }

    private fun process() {
        val buffer: CVPixelBuffer?
        val removeImage: Boolean
        val flush: Boolean
        synchronized(lock) {
            buffer = pendingBuffer
            removeImage = pendingRemoveImage
            flush = pendingFlush
            pendingBuffer = null
            pendingRemoveImage = false
            pendingFlush = false
            isProcessScheduled = false
        }
        if (flush && status == Status.failed) {
            status = Status.unknown
        }
        if (removeImage) {
            setLastBuffer(null)
        }
        if (buffer != null) {
            if (buffer.checkReadable("display layer")) {
                setLastBuffer(buffer)
                if (render(buffer)) {
                    PipelineStats.increment("preview")
                }
            }
            releaseLease(buffer)
        } else if (removeImage) {
            render(null)
        }
    }

    private fun surfaceAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (surface === surfaceTexture && eglSurface != null) {
            return
        }
        releaseEglSurface()
        surfaceTexture = surface
        surfaceWidth = width
        surfaceHeight = height
        latestCreateAttemptTime = 0L
        Log.i(TAG, "$name: surface available ${width}x$height")
        render(redrawBuffer())
    }

    private fun surfaceSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        if (surface !== surfaceTexture) {
            return
        }
        surfaceWidth = width
        surfaceHeight = height
        render(redrawBuffer())
    }

    private fun surfaceDestroyed(surface: SurfaceTexture) {
        if (surface === surfaceTexture) {
            releaseEglSurface()
            surfaceTexture = null
            surfaceWidth = 0
            surfaceHeight = 0
            status = Status.unknown
            Log.i(TAG, "$name: surface destroyed")
        }
        surface.release()
    }

    private fun ensureEglSurface(): EGLSurface? {
        eglSurface?.let {
            return it
        }
        val surface = surfaceTexture ?: return null
        val now = SystemClock.uptimeMillis()
        if (latestCreateAttemptTime != 0L && now - latestCreateAttemptTime < 1000) {
            return null
        }
        latestCreateAttemptTime = now
        try {
            val created = EglCore.createWindowSurface(surface)
            if (created == EGL14.EGL_NO_SURFACE) {
                fail("Window surface creation returned no surface")
                return null
            }
            eglSurface = created
            EglCore.makeCurrent(created)
            EGL14.eglSwapInterval(EglCore.display, 0)
            Log.i(TAG, "$name: window surface created ${surfaceWidth}x$surfaceHeight")
            return created
        } catch (e: Exception) {
            fail("Window surface creation failed: ${e.message}")
            return null
        } finally {
            EglCore.makePbufferCurrent()
        }
    }

    private fun releaseEglSurface() {
        val surface = eglSurface ?: return
        eglSurface = null
        try {
            EglCore.makePbufferCurrent()
            EglCore.destroySurface(surface)
        } catch (e: Exception) {
            Log.w(TAG, "$name: window surface destroy failed: ${e.message}")
        }
    }

    private fun render(buffer: CVPixelBuffer?): Boolean {
        val surface = ensureEglSurface() ?: return false
        var rendered = false
        try {
            EglCore.makeCurrent(surface)
            val value = IntArray(1)
            val width = if (EGL14.eglQuerySurface(EglCore.display, surface, EGL14.EGL_WIDTH, value, 0)) {
                value[0]
            } else {
                surfaceWidth
            }
            val height = if (EGL14.eglQuerySurface(EglCore.display, surface, EGL14.EGL_HEIGHT, value, 0)) {
                value[0]
            } else {
                surfaceHeight
            }
            if (width > 0 && height > 0) {
                GlRenderer.bind(null)
                if (buffer == null) {
                    GlRenderer.clear(0f, 0f, 0f, 1f)
                } else {
                    GlRenderer.draw(buffer, width, height, scalingMode(), 0, false, true)
                }
                val error = EglCore.swapBuffers(surface)
                if (error == EGL14.EGL_SUCCESS) {
                    status = Status.rendering
                    rendered = true
                } else {
                    fail("Swap failed with EGL error 0x${Integer.toHexString(error)}")
                    if (error == EGL14.EGL_BAD_SURFACE || error == EGL14.EGL_BAD_NATIVE_WINDOW) {
                        releaseEglSurface()
                    }
                }
            }
        } catch (e: Exception) {
            fail("Render failed: ${e.message}")
        } finally {
            EglCore.makePbufferCurrent()
        }
        return rendered
    }

    private fun scalingMode(): GlRenderer.ScalingMode {
        return when ((viewRef.get() as? PreviewView)?.videoGravity) {
            VideoGravity.resizeAspectFill -> GlRenderer.ScalingMode.fill
            VideoGravity.resize -> GlRenderer.ScalingMode.stretch
            else -> GlRenderer.ScalingMode.fit
        }
    }

    private fun fail(message: String) {
        val now = SystemClock.uptimeMillis()
        if (message != latestFailMessage || now - latestFailLogTime >= 5000) {
            Log.w(TAG, "$name: $message")
            latestFailMessage = message
            latestFailLogTime = now
        }
        status = Status.failed
    }
}
