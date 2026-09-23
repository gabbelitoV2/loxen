package com.moblin.android.platform.coreimage.internal

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLUtils
import android.os.SystemClock
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.core.PipelineThread
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.WeakHashMap
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class PooledTexture(val id: Int, val width: Int, val height: Int, val format: TextureFormat) {
    var framebuffer = 0
    var lastUsedMs = 0L
    val bytes: Long
        get() = width.toLong() * height.toLong() * format.bytesPerPixel
}

internal object TexturePool {
    private const val UNUSED_TIMEOUT_MS = 5000L
    private val free = ArrayList<PooledTexture>()
    private var lastTrimMs = 0L

    fun obtain(width: Int, height: Int, format: TextureFormat, exact: Boolean): PooledTexture? {
        val allocWidth = if (exact) width else bucket(width)
        val allocHeight = if (exact) height else bucket(height)
        for (index in free.indices) {
            val texture = free[index]
            if (texture.width == allocWidth && texture.height == allocHeight && texture.format == format) {
                free.removeAt(index)
                return texture
            }
        }
        return allocate(allocWidth, allocHeight, format)
    }

    fun release(texture: PooledTexture) {
        texture.lastUsedMs = SystemClock.uptimeMillis()
        free.add(texture)
    }

    fun trim() {
        val nowMs = SystemClock.uptimeMillis()
        if (nowMs - lastTrimMs < 1000) {
            return
        }
        lastTrimMs = nowMs
        val iterator = free.iterator()
        while (iterator.hasNext()) {
            val texture = iterator.next()
            if (nowMs - texture.lastUsedMs > UNUSED_TIMEOUT_MS) {
                iterator.remove()
                delete(texture)
            }
        }
    }

    fun framebuffer(texture: PooledTexture): Int {
        if (texture.framebuffer != 0) {
            return texture.framebuffer
        }
        val ids = IntArray(1)
        GLES20.glGenFramebuffers(1, ids, 0)
        val previous = Gl.currentFramebuffer()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, ids[0])
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            texture.id,
            0
        )
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previous)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            GLES20.glDeleteFramebuffers(1, ids, 0)
            throw IllegalStateException(
                "Framebuffer ${texture.width}x${texture.height} ${texture.format} incomplete: 0x${Integer.toHexString(status)}"
            )
        }
        texture.framebuffer = ids[0]
        return ids[0]
    }

    private fun bucket(value: Int): Int {
        return ((max(value, 1) + 31) / 32) * 32
    }

    private fun allocate(width: Int, height: Int, format: TextureFormat): PooledTexture? {
        if (width <= 0 || height <= 0) {
            return null
        }
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        when (format) {
            TextureFormat.rgba8 -> GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                if (Gl.es3) GLES30.GL_RGBA8 else GLES20.GL_RGBA,
                width,
                height,
                0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                null
            )
            TextureFormat.rgba16f -> GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES30.GL_RGBA16F,
                width,
                height,
                0,
                GLES20.GL_RGBA,
                GLES30.GL_HALF_FLOAT,
                null
            )
        }
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return PooledTexture(ids[0], width, height, format)
    }

    private fun delete(texture: PooledTexture) {
        if (texture.framebuffer != 0) {
            GLES20.glDeleteFramebuffers(1, intArrayOf(texture.framebuffer), 0)
        }
        GLES20.glDeleteTextures(1, intArrayOf(texture.id), 0)
    }
}

internal class Intermediate(
    val texture: PooledTexture,
    val originX: Double,
    val originY: Double,
    val scaleX: Double,
    val scaleY: Double,
    val validWidth: Int,
    val validHeight: Int,
) {
    val maxX: Double
        get() = originX + validWidth / scaleX

    val maxY: Double
        get() = originY + validHeight / scaleY

    fun covers(minX: Double, minY: Double, maxX: Double, maxY: Double): Boolean {
        return minX >= originX && minY >= originY && maxX <= this.maxX && maxY <= this.maxY
    }
}

internal class TextureSlot {
    val textures = ArrayList<PooledTexture>()
    var bytes = 0L
    var released = false

    fun add(texture: PooledTexture) {
        textures.add(texture)
        bytes += texture.bytes
        TextureReaper.addResident(texture.bytes)
    }

    fun releaseAll() {
        if (released) {
            return
        }
        released = true
        for (texture in textures) {
            TexturePool.release(texture)
        }
        TextureReaper.addResident(-bytes)
        textures.clear()
        bytes = 0
    }
}

internal object TextureReaper {
    private const val GC_THRESHOLD_BYTES = 96L * 1024 * 1024
    private const val GC_GROWTH_BYTES = 32L * 1024 * 1024
    private const val GC_MIN_INTERVAL_MS = 500L

    private class SlotReference(owner: Any, queue: ReferenceQueue<Any>, val slot: TextureSlot) :
        PhantomReference<Any>(owner, queue)

    private val queue = ReferenceQueue<Any>()
    private val references = HashSet<SlotReference>()
    private var residentBytes = 0L
    private var bytesAtLastGcRequest = 0L
    private var lastGcRequestMs = 0L
    private var tickRegistered = false
    private val gcExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "MoblinTextureGc").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }
    }

    fun register(owner: Any, slot: TextureSlot) {
        synchronized(references) {
            references.add(SlotReference(owner, queue, slot))
        }
        ensureTick()
    }

    fun addResident(bytes: Long) {
        residentBytes += bytes
        PipelineStats.gauge("fxTexMB", residentBytes / (1024 * 1024))
    }

    fun poll() {
        if (!PipelineThread.isCurrent()) {
            return
        }
        while (true) {
            val reference = queue.poll() as? SlotReference ?: break
            synchronized(references) {
                references.remove(reference)
            }
            reference.slot.releaseAll()
        }
        maybeRequestGc()
    }

    private fun ensureTick() {
        if (tickRegistered) {
            return
        }
        tickRegistered = true
        PipelineStats.addPipelineTick {
            poll()
            TexturePool.trim()
        }
    }

    private fun maybeRequestGc() {
        if (residentBytes < GC_THRESHOLD_BYTES) {
            if (residentBytes < bytesAtLastGcRequest) {
                bytesAtLastGcRequest = residentBytes
            }
            return
        }
        if (residentBytes - bytesAtLastGcRequest < GC_GROWTH_BYTES) {
            return
        }
        val nowMs = SystemClock.uptimeMillis()
        if (nowMs - lastGcRequestMs < GC_MIN_INTERVAL_MS) {
            return
        }
        lastGcRequestMs = nowMs
        bytesAtLastGcRequest = residentBytes
        gcExecutor.execute {
            Runtime.getRuntime().gc()
            PipelineThread.post { poll() }
        }
    }
}

internal class BitmapTexture(val texture: PooledTexture, val generationId: Int, val slot: TextureSlot) {
    val pyramidLevels = HashMap<Int, Intermediate>()
}

internal object BitmapTextures {
    private val entries = WeakHashMap<Bitmap, BitmapTexture>()

    fun texture(bitmap: Bitmap): BitmapTexture? {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return null
        }
        val existing = entries[bitmap]
        if (existing != null && existing.generationId == bitmap.generationId && !existing.slot.released) {
            return existing
        }
        existing?.slot?.releaseAll()
        val upload = uploadableBitmap(bitmap) ?: return null
        val texture = TexturePool.obtain(upload.width, upload.height, TextureFormat.rgba8, true)
        if (texture == null) {
            if (upload !== bitmap) {
                upload.recycle()
            }
            return null
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.id)
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
        GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, upload)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        if (upload !== bitmap) {
            upload.recycle()
        }
        PipelineStats.increment("fxUploads")
        val slot = TextureSlot()
        slot.add(texture)
        val entry = BitmapTexture(texture, bitmap.generationId, slot)
        entries[bitmap] = entry
        TextureReaper.register(bitmap, slot)
        return entry
    }

    fun addPyramidLevel(entry: BitmapTexture, level: Int, intermediate: Intermediate) {
        entry.pyramidLevels[level] = intermediate
        entry.slot.add(intermediate.texture)
    }

    private fun uploadableBitmap(bitmap: Bitmap): Bitmap? {
        val maxSize = Gl.maxTextureSize
        val needsScale = bitmap.width > maxSize || bitmap.height > maxSize
        val isPlain = bitmap.config == Bitmap.Config.ARGB_8888 && bitmap.isPremultiplied
        if (!needsScale && isPlain) {
            return bitmap
        }
        val scale = min(1.0, min(maxSize.toDouble() / bitmap.width, maxSize.toDouble() / bitmap.height))
        val width = max(1, (bitmap.width * scale).roundToInt())
        val height = max(1, (bitmap.height * scale).roundToInt())
        val source = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return null
        } else {
            bitmap
        }
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.scale(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
        canvas.drawBitmap(source, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
        if (source !== bitmap) {
            source.recycle()
        }
        return output
    }
}
