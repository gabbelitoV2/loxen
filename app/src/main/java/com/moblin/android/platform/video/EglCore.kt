package com.moblin.android.platform.video

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLES30
import android.util.Log
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.core.PipelineThread

object EglCore {
    private const val TAG = "MoblinPipeline"
    private const val EGL_RECORDABLE_ANDROID = 0x3142
    private var displayField: EGLDisplay? = null
    private var configField: EGLConfig? = null
    private var contextField: EGLContext? = null
    private var pbufferSurface: EGLSurface? = null
    private var currentSurface: EGLSurface? = null
    private var setUpDone = false

    @Volatile
    var isReady = false
        private set

    @Volatile
    var glMajorVersion = 0
        private set

    val display: EGLDisplay
        get() {
            ensureSetUp()
            return displayField ?: EGL14.EGL_NO_DISPLAY
        }

    val config: EGLConfig
        get() {
            ensureSetUp()
            return configField ?: throw IllegalStateException("EGL is not available")
        }

    val context: EGLContext
        get() {
            ensureSetUp()
            return contextField ?: EGL14.EGL_NO_CONTEXT
        }

    internal fun setUp() {
        if (setUpDone) {
            return
        }
        setUpDone = true
        PipelineStats.start()
        try {
            setUpInternal()
        } catch (error: Throwable) {
            Log.e(TAG, "EGL setup failed", error)
        }
    }

    fun createWindowSurface(nativeWindow: Any): EGLSurface {
        if (!PipelineThread.isCurrent()) {
            return PipelineThread.runSync { createWindowSurface(nativeWindow) }
        }
        ensureSetUp()
        val config = configField
        if (!isReady || config == null) {
            Log.e(TAG, "createWindowSurface: EGL is not ready")
            return EGL14.EGL_NO_SURFACE
        }
        return try {
            val surface = EGL14.eglCreateWindowSurface(
                displayField,
                config,
                nativeWindow,
                intArrayOf(EGL14.EGL_NONE),
                0
            )
            if (surface == null || surface == EGL14.EGL_NO_SURFACE) {
                Log.e(TAG, "eglCreateWindowSurface failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
                EGL14.EGL_NO_SURFACE
            } else {
                surface
            }
        } catch (error: Throwable) {
            Log.e(TAG, "eglCreateWindowSurface failed for $nativeWindow", error)
            EGL14.EGL_NO_SURFACE
        }
    }

    fun makeCurrent(surface: EGLSurface) {
        if (!PipelineThread.isCurrent()) {
            Log.w(TAG, "makeCurrent called off the pipeline thread")
            PipelineThread.runSync { makeCurrent(surface) }
            return
        }
        ensureSetUp()
        if (!isReady) {
            return
        }
        if (surface == EGL14.EGL_NO_SURFACE) {
            makePbufferCurrent()
            return
        }
        if (currentSurface != surface) {
            if (!EGL14.eglMakeCurrent(displayField, surface, surface, contextField)) {
                Log.e(TAG, "eglMakeCurrent failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
                makePbufferCurrent()
                return
            }
            currentSurface = surface
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun makePbufferCurrent() {
        if (!PipelineThread.isCurrent()) {
            Log.w(TAG, "makePbufferCurrent called off the pipeline thread")
            PipelineThread.runSync { makePbufferCurrent() }
            return
        }
        ensureSetUp()
        if (!isReady) {
            return
        }
        if (currentSurface != pbufferSurface || currentSurface == null) {
            if (!EGL14.eglMakeCurrent(displayField, pbufferSurface, pbufferSurface, contextField)) {
                Log.e(TAG, "eglMakeCurrent pbuffer failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
                return
            }
            currentSurface = pbufferSurface
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun swapBuffers(surface: EGLSurface): Int {
        if (!PipelineThread.isCurrent()) {
            return PipelineThread.runSync { swapBuffers(surface) }
        }
        if (!isReady) {
            return EGL14.EGL_NOT_INITIALIZED
        }
        if (!EGL14.eglSwapBuffers(displayField, surface)) {
            return EGL14.eglGetError()
        }
        return EGL14.EGL_SUCCESS
    }

    fun setPresentationTime(surface: EGLSurface, timeNs: Long) {
        if (!isReady) {
            return
        }
        if (!EGLExt.eglPresentationTimeANDROID(displayField, surface, timeNs)) {
            Log.w(TAG, "eglPresentationTimeANDROID failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
        }
    }

    fun destroySurface(surface: EGLSurface) {
        if (!PipelineThread.isCurrent()) {
            PipelineThread.runSync { destroySurface(surface) }
            return
        }
        if (!isReady || surface == EGL14.EGL_NO_SURFACE || surface == pbufferSurface) {
            return
        }
        if (currentSurface == surface) {
            makePbufferCurrent()
        }
        if (!EGL14.eglDestroySurface(displayField, surface)) {
            Log.w(TAG, "eglDestroySurface failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
        }
    }

    fun querySurfaceSize(surface: EGLSurface): android.util.Size {
        if (!isReady) {
            return android.util.Size(0, 0)
        }
        val value = IntArray(1)
        EGL14.eglQuerySurface(displayField, surface, EGL14.EGL_WIDTH, value, 0)
        val width = value[0]
        EGL14.eglQuerySurface(displayField, surface, EGL14.EGL_HEIGHT, value, 0)
        return android.util.Size(width, value[0])
    }

    private fun ensureSetUp() {
        if (!setUpDone && PipelineThread.isCurrent()) {
            setUp()
        }
    }

    private fun setUpInternal() {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == null || display == EGL14.EGL_NO_DISPLAY) {
            Log.e(TAG, "eglGetDisplay failed")
            return
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
            Log.e(TAG, "eglInitialize failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
            return
        }
        var clientVersion = 3
        var config = chooseConfig(display, EGLExt.EGL_OPENGL_ES3_BIT_KHR, true)
            ?: chooseConfig(display, EGLExt.EGL_OPENGL_ES3_BIT_KHR, false)
        if (config == null) {
            clientVersion = 2
            config = chooseConfig(display, EGL14.EGL_OPENGL_ES2_BIT, true)
                ?: chooseConfig(display, EGL14.EGL_OPENGL_ES2_BIT, false)
        }
        if (config == null) {
            Log.e(TAG, "No recordable RGBA8888 EGL config")
            return
        }
        var context = EGL14.eglCreateContext(
            display,
            config,
            EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, clientVersion, EGL14.EGL_NONE),
            0
        )
        if ((context == null || context == EGL14.EGL_NO_CONTEXT) && clientVersion == 3) {
            clientVersion = 2
            config = chooseConfig(display, EGL14.EGL_OPENGL_ES2_BIT, true)
                ?: chooseConfig(display, EGL14.EGL_OPENGL_ES2_BIT, false)
                ?: config
            context = EGL14.eglCreateContext(
                display,
                config,
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, clientVersion, EGL14.EGL_NONE),
                0
            )
        }
        if (context == null || context == EGL14.EGL_NO_CONTEXT) {
            Log.e(TAG, "eglCreateContext failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
            return
        }
        var pbuffer = EGL14.eglCreatePbufferSurface(
            display,
            config,
            intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE),
            0
        )
        if (pbuffer == null) {
            pbuffer = EGL14.EGL_NO_SURFACE
        }
        if (!EGL14.eglMakeCurrent(display, pbuffer, pbuffer, context)) {
            Log.e(TAG, "eglMakeCurrent failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
            EGL14.eglDestroyContext(display, context)
            return
        }
        displayField = display
        configField = config
        contextField = context
        pbufferSurface = pbuffer
        currentSurface = pbuffer
        glMajorVersion = clientVersion
        var minor = 0
        if (clientVersion >= 3) {
            val values = IntArray(1)
            GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, values, 0)
            if (values[0] > 0) {
                glMajorVersion = values[0]
            }
            GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, values, 0)
            minor = values[0]
        }
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
        GLES20.glPixelStorei(GLES20.GL_PACK_ALIGNMENT, 1)
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
        isReady = true
        Log.i(TAG, "EGL ready (OpenGL ES $glMajorVersion.$minor) on ${Thread.currentThread().name}")
        Log.i(
            TAG,
            "EGL ${version[0]}.${version[1]}, ${GLES20.glGetString(GLES20.GL_VENDOR)} " +
                "${GLES20.glGetString(GLES20.GL_RENDERER)}, ${GLES20.glGetString(GLES20.GL_VERSION)}"
        )
    }

    private fun chooseConfig(display: EGLDisplay, renderableType: Int, withPbuffer: Boolean): EGLConfig? {
        val surfaceType = if (withPbuffer) {
            EGL14.EGL_WINDOW_BIT or EGL14.EGL_PBUFFER_BIT
        } else {
            EGL14.EGL_WINDOW_BIT
        }
        val attributes = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, renderableType,
            EGL14.EGL_SURFACE_TYPE, surfaceType,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        if (!EGL14.eglChooseConfig(display, attributes, 0, configs, 0, configs.size, count, 0)) {
            return null
        }
        if (count[0] <= 0) {
            return null
        }
        return configs[0]
    }
}
