package com.moblin.android.platform.vision

import android.graphics.Bitmap
import android.opengl.GLES20
import android.os.SystemClock
import com.google.android.gms.tasks.Tasks
import com.google.mediapipe.framework.image.ByteBufferImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.moblin.android.AppDelegate
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.coregraphics.CGImagePropertyOrientation
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.EglCore
import com.moblin.android.platform.video.GlRenderer
import com.moblin.android.platform.video.PixelBufferBacking
import com.moblin.android.platform.video.PixelBufferGl
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.WeakHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.max

internal class VisionFrame(val width: Int, val height: Int) {
    val pixels: ByteBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
}

internal object VisionFrames {
    private const val MAX_FREE_FRAMES = 4
    private val lock = Any()
    private val free = ArrayList<VisionFrame>()

    fun acquire(width: Int, height: Int): VisionFrame {
        synchronized(lock) {
            val index = free.indexOfFirst { it.width == width && it.height == height }
            if (index >= 0) {
                return free.removeAt(index)
            }
        }
        return VisionFrame(width, height)
    }

    fun release(frame: VisionFrame) {
        synchronized(lock) {
            free.removeAll { it.width != frame.width || it.height != frame.height }
            if (free.size < MAX_FREE_FRAMES) {
                free.add(frame)
            }
        }
    }
}

internal class VisionLatestResults<T>(private val maxAgeMs: Long) {
    private class Entry<T>(val value: T, val width: Int, val height: Int, val atMs: Long)

    private val entries = WeakHashMap<Any, Entry<T>>()

    fun put(source: Any, value: T, width: Int, height: Int) {
        synchronized(entries) {
            entries[source] = Entry(value, width, height, SystemClock.uptimeMillis())
        }
    }

    fun get(source: Any, width: Int, height: Int): T? {
        synchronized(entries) {
            val entry = entries[source] ?: return null
            if (entry.width != width || entry.height != height ||
                SystemClock.uptimeMillis() - entry.atMs > maxAgeMs
            ) {
                return null
            }
            return entry.value
        }
    }
}

internal object VisionReadback {
    private const val MAX_SCRATCHES = 3
    private val scratches = ArrayList<PixelBufferBacking>()

    fun read(buffer: CVPixelBuffer, frame: VisionFrame): Boolean {
        return try {
            PipelineThread.runSync(timeoutMs = 3000) {
                readOnPipeline(buffer, frame)
            }
        } catch (error: Throwable) {
            VisionLog.once("readback:${error.javaClass.name}", "Vision readback failed: $error")
            false
        }
    }

    private fun scratchFor(width: Int, height: Int): PixelBufferBacking? {
        val index = scratches.indexOfFirst { it.width == width && it.height == height }
        if (index >= 0) {
            val existing = scratches.removeAt(index)
            scratches.add(existing)
            return existing
        }
        val created = PixelBufferGl.allocate(width, height) ?: return null
        scratches.add(created)
        while (scratches.size > MAX_SCRATCHES) {
            PixelBufferGl.delete(scratches.removeAt(0))
        }
        return created
    }

    private fun readOnPipeline(buffer: CVPixelBuffer, frame: VisionFrame): Boolean {
        if (!EglCore.isReady) {
            return false
        }
        if (!buffer.checkReadable("vision")) {
            return false
        }
        val target = scratchFor(frame.width, frame.height) ?: return false
        val previousFramebuffer = GlRenderer.currentFramebuffer()
        val viewport = IntArray(4)
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0)
        val blend = GLES20.glIsEnabled(GLES20.GL_BLEND)
        val scissor = GLES20.glIsEnabled(GLES20.GL_SCISSOR_TEST)
        try {
            GLES20.glDisable(GLES20.GL_BLEND)
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
            GLES20.glColorMask(true, true, true, true)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, target.framebuffer)
            GlRenderer.drawTexture(
                texture = buffer.texture,
                oes = false,
                texMatrix = null,
                sourceWidth = buffer.width,
                sourceHeight = buffer.height,
                targetWidth = frame.width,
                targetHeight = frame.height,
                mode = GlRenderer.ScalingMode.stretch,
                rotationDegreesCw = 0,
                mirror = false,
                flipVertical = true,
            )
            frame.pixels.rewind()
            GLES20.glReadPixels(
                0,
                0,
                frame.width,
                frame.height,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                frame.pixels
            )
            frame.pixels.rewind()
            GlRenderer.checkError("visionReadback")
            return true
        } finally {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFramebuffer)
            GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3])
            if (blend) {
                GLES20.glEnable(GLES20.GL_BLEND)
            }
            if (scissor) {
                GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
            }
        }
    }
}

internal object VisionFaceLandmarkers {
    private const val MODEL_ASSET = "face_landmarker.task"
    private const val MAX_INSTANCES_PER_SIZE = 2
    private const val MAX_FACES = 5
    private const val ACQUIRE_TIMEOUT_MS = 100L
    private const val LATEST_MAX_AGE_MS = 500L
    private const val OTHER_SIZE_IDLE_CLOSE_MS = 10_000L
    private const val IDLE_CHECK_MS = 10_000L
    private const val IDLE_RELEASE_MS = 30_000L
    private const val CREATE_RETRY_MS = 5000L

    private class Slot(val width: Int, val height: Int, val landmarker: FaceLandmarker, val delegate: Delegate) {
        var busy = false
        var lastTimestampMs = 0L
        var lastUsedMs = SystemClock.uptimeMillis()
        var lastSource: WeakReference<Any>? = null
    }

    private val lock = Object()
    private val slots = ArrayList<Slot>()
    private val creating = HashMap<Long, Int>()
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "MoblinVisionInit").apply { isDaemon = true }
    }

    @Volatile
    private var gpuFailed = false

    @Volatile
    private var modelFailed = false
    private var model: ByteBuffer? = null

    @Volatile
    private var createFailedAtMs = -CREATE_RETRY_MS

    private val latest = VisionLatestResults<List<List<NormalizedLandmark>>>(LATEST_MAX_AGE_MS)

    init {
        executor.scheduleWithFixedDelay({ closeIdle() }, IDLE_CHECK_MS, IDLE_CHECK_MS, TimeUnit.MILLISECONDS)
    }

    fun detect(frame: VisionFrame, source: Any): List<List<NormalizedLandmark>>? {
        val slot = acquire(frame.width, frame.height, source)
            ?: return latest.get(source, frame.width, frame.height)
        var failed = false
        try {
            frame.pixels.rewind()
            val image = ByteBufferImageBuilder(frame.pixels, frame.width, frame.height, MPImage.IMAGE_FORMAT_RGBA)
                .build()
            try {
                val timestampMs = max(slot.lastTimestampMs + 1, SystemClock.uptimeMillis())
                slot.lastTimestampMs = timestampMs
                val faces = slot.landmarker.detectForVideo(image, timestampMs).faceLandmarks()
                latest.put(source, faces, frame.width, frame.height)
                return faces
            } finally {
                image.close()
            }
        } catch (error: Throwable) {
            failed = true
            VisionLog.once(
                "landmarker:${slot.delegate}:${error.javaClass.name}",
                "FaceLandmarker (${slot.delegate}) failed: $error"
            )
            if (slot.delegate == Delegate.GPU) {
                gpuFailed = true
            } else {
                createFailedAtMs = SystemClock.uptimeMillis()
            }
            return null
        } finally {
            release(slot, failed)
        }
    }

    private fun sizeKey(width: Int, height: Int): Long {
        return (width.toLong() shl 32) or height.toLong()
    }

    private fun acquire(width: Int, height: Int, source: Any): Slot? {
        val key = sizeKey(width, height)
        val deadline = SystemClock.uptimeMillis() + ACQUIRE_TIMEOUT_MS
        val closing = ArrayList<Slot>()
        try {
            synchronized(lock) {
                while (true) {
                    val now = SystemClock.uptimeMillis()
                    val idle = slots.filter {
                        !it.busy && (it.width != width || it.height != height) &&
                            now - it.lastUsedMs > OTHER_SIZE_IDLE_CLOSE_MS
                    }
                    slots.removeAll(idle)
                    closing.addAll(idle)
                    val free = slots.filter { !it.busy && it.width == width && it.height == height }
                        .maxByOrNull { if (it.lastSource?.get() === source) 1 else 0 }
                    if (free != null) {
                        free.busy = true
                        free.lastUsedMs = now
                        free.lastSource = WeakReference(source)
                        return free
                    }
                    val ready = slots.count { it.width == width && it.height == height }
                    val pending = creating[key] ?: 0
                    if (pending == 0 && ready < MAX_INSTANCES_PER_SIZE && now - createFailedAtMs >= CREATE_RETRY_MS) {
                        creating[key] = 1
                        startCreate(width, height, key)
                    }
                    if (ready == 0) {
                        return null
                    }
                    val remaining = deadline - now
                    if (remaining <= 0) {
                        PipelineStats.increment("visionBusy")
                        VisionLog.once("landmarker:busy", "All FaceLandmarker instances are busy; reusing the latest faces")
                        return null
                    }
                    lock.wait(remaining)
                }
            }
        } finally {
            close(closing)
        }
    }

    private fun startCreate(width: Int, height: Int, key: Long) {
        executor.execute {
            val created = create()
            val slot = created?.let { Slot(width, height, it.first, it.second) }
            if (slot != null) {
                warmUp(slot)
            }
            synchronized(lock) {
                val count = (creating[key] ?: 1) - 1
                if (count <= 0) {
                    creating.remove(key)
                } else {
                    creating[key] = count
                }
                if (slot == null) {
                    createFailedAtMs = SystemClock.uptimeMillis()
                } else {
                    slot.lastUsedMs = SystemClock.uptimeMillis()
                    slots.add(slot)
                }
                lock.notifyAll()
            }
        }
    }

    private fun warmUp(slot: Slot) {
        try {
            val blank = ByteBuffer.allocateDirect(slot.width * slot.height * 4).order(ByteOrder.nativeOrder())
            val image = ByteBufferImageBuilder(blank, slot.width, slot.height, MPImage.IMAGE_FORMAT_RGBA).build()
            val timestampMs = SystemClock.uptimeMillis()
            try {
                slot.landmarker.detectForVideo(image, timestampMs)
            } finally {
                image.close()
            }
            slot.lastTimestampMs = timestampMs
        } catch (error: Throwable) {
            VisionLog.once("landmarker:warmUp:${error.javaClass.name}", "FaceLandmarker warm up failed: $error")
        }
    }

    private fun release(slot: Slot, failed: Boolean) {
        var discarded: Slot? = null
        synchronized(lock) {
            slot.busy = false
            slot.lastUsedMs = SystemClock.uptimeMillis()
            if (failed) {
                slots.remove(slot)
                discarded = slot
            }
            lock.notifyAll()
        }
        discarded?.let { close(listOf(it)) }
    }

    private fun closeIdle() {
        try {
            val closing = ArrayList<Slot>()
            synchronized(lock) {
                val now = SystemClock.uptimeMillis()
                val idle = slots.filter { !it.busy && now - it.lastUsedMs > IDLE_RELEASE_MS }
                slots.removeAll(idle)
                closing.addAll(idle)
            }
            if (closing.isNotEmpty()) {
                VisionLog.info("Closing ${closing.size} idle FaceLandmarker instances")
            }
            close(closing)
        } catch (error: Throwable) {
            VisionLog.once("landmarker:closeIdle:${error.javaClass.name}", "FaceLandmarker idle close failed: $error")
        }
    }

    private fun close(closing: List<Slot>) {
        for (slot in closing) {
            try {
                slot.landmarker.close()
            } catch (error: Throwable) {
                VisionLog.once("landmarker:close", "FaceLandmarker close failed: $error")
            }
        }
    }

    private fun model(): ByteBuffer? {
        if (modelFailed) {
            return null
        }
        synchronized(this) {
            model?.let {
                return it
            }
            return try {
                val bytes = AppDelegate.context.assets.open(MODEL_ASSET).use { it.readBytes() }
                val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
                buffer.put(bytes)
                buffer.rewind()
                model = buffer
                buffer
            } catch (error: Throwable) {
                modelFailed = true
                VisionLog.once("model", "Could not load $MODEL_ASSET: $error")
                null
            }
        }
    }

    private fun create(): Pair<FaceLandmarker, Delegate>? {
        val buffer = model() ?: return null
        val delegates = if (gpuFailed) listOf(Delegate.CPU) else listOf(Delegate.GPU, Delegate.CPU)
        for (delegate in delegates) {
            try {
                val modelBuffer = buffer.duplicate()
                modelBuffer.rewind()
                val baseOptions = BaseOptions.builder()
                    .setModelAssetBuffer(modelBuffer)
                    .setDelegate(delegate)
                    .build()
                val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.VIDEO)
                    .setNumFaces(MAX_FACES)
                    .setOutputFaceBlendshapes(false)
                    .setOutputFacialTransformationMatrixes(false)
                    .build()
                val landmarker = FaceLandmarker.createFromOptions(AppDelegate.context, options)
                VisionLog.once("landmarker:created:$delegate", "FaceLandmarker created with the $delegate delegate")
                return Pair(landmarker, delegate)
            } catch (error: Throwable) {
                VisionLog.once(
                    "landmarker:create:$delegate:${error.javaClass.name}",
                    "FaceLandmarker with the $delegate delegate failed: $error"
                )
                if (delegate == Delegate.GPU) {
                    gpuFailed = true
                }
            }
        }
        return null
    }
}

internal object VisionTextRecognizer {
    private const val TIMEOUT_MS = 1000L
    private const val WARM_UP_SIDE = 64
    private const val WARM_UP_RETRY_MS = 5000L
    private const val LATEST_MAX_AGE_MS = 1000L
    private const val IDLE_CHECK_MS = 10_000L
    private const val IDLE_RELEASE_MS = 30_000L

    private val lock = Any()
    private var recognizer: TextRecognizer? = null
    private var warmUpStarted = false
    private var warmUpFailedAtMs = -WARM_UP_RETRY_MS
    private var running = false
    private var lastUsedMs = 0L
    private var bitmap: Bitmap? = null
    private val latest = VisionLatestResults<List<VNRecognizedTextObservation>>(LATEST_MAX_AGE_MS)
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "MoblinVisionTextInit").apply { isDaemon = true }
    }

    init {
        executor.scheduleWithFixedDelay({ closeIdle() }, IDLE_CHECK_MS, IDLE_CHECK_MS, TimeUnit.MILLISECONDS)
    }

    fun recognize(frame: VisionFrame, source: Any): List<VNRecognizedTextObservation> {
        val client: TextRecognizer
        synchronized(lock) {
            lastUsedMs = SystemClock.uptimeMillis()
            val current = recognizer
            if (current == null) {
                startWarmUpLocked()
                return emptyList()
            }
            if (running) {
                return latest.get(source, frame.width, frame.height) ?: emptyList()
            }
            running = true
            client = current
        }
        var observations: List<VNRecognizedTextObservation> = emptyList()
        var finishedLater = false
        try {
            val task = client.process(InputImage.fromBitmap(bitmapFor(frame), 0))
            try {
                observations = observations(Tasks.await(task, TIMEOUT_MS, TimeUnit.MILLISECONDS), frame)
            } catch (error: TimeoutException) {
                finishedLater = true
                task.addOnCompleteListener(executor) {
                    finish()
                }
                throw error
            }
        } catch (error: Throwable) {
            VisionLog.once("text:${error.javaClass.name}", "Text recognition failed: $error")
        } finally {
            latest.put(source, observations, frame.width, frame.height)
            if (!finishedLater) {
                finish()
            }
        }
        return observations
    }

    private fun finish() {
        synchronized(lock) {
            running = false
            lastUsedMs = SystemClock.uptimeMillis()
        }
    }

    private fun bitmapFor(frame: VisionFrame): Bitmap {
        val existing = bitmap
        val result = if (existing != null && existing.width == frame.width && existing.height == frame.height) {
            existing
        } else {
            Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888).also { bitmap = it }
        }
        frame.pixels.rewind()
        result.copyPixelsFromBuffer(frame.pixels)
        frame.pixels.rewind()
        return result
    }

    private fun observations(text: Text, frame: VisionFrame): List<VNRecognizedTextObservation> {
        val observations = ArrayList<VNRecognizedTextObservation>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox ?: continue
                VisionMapping.textObservation(
                    left = box.left,
                    top = box.top,
                    right = box.right,
                    bottom = box.bottom,
                    imageWidth = frame.width,
                    imageHeight = frame.height,
                    string = line.text,
                    confidence = line.confidence,
                    minimumTextHeight = 0f
                )?.let { observations.add(it) }
            }
        }
        return observations
    }

    private fun startWarmUpLocked() {
        if (warmUpStarted || SystemClock.uptimeMillis() - warmUpFailedAtMs < WARM_UP_RETRY_MS) {
            return
        }
        warmUpStarted = true
        executor.execute {
            var client: TextRecognizer? = null
            try {
                client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val blank = Bitmap.createBitmap(WARM_UP_SIDE, WARM_UP_SIDE, Bitmap.Config.ARGB_8888)
                Tasks.await(client.process(InputImage.fromBitmap(blank, 0)), 30, TimeUnit.SECONDS)
                synchronized(lock) {
                    recognizer = client
                    lastUsedMs = SystemClock.uptimeMillis()
                }
                VisionLog.info("Text recognizer ready")
            } catch (error: Throwable) {
                VisionLog.once("text:warmUp:${error.javaClass.name}", "Text recognizer warm up failed: $error")
                try {
                    client?.close()
                } catch (_: Throwable) {
                }
                synchronized(lock) {
                    warmUpStarted = false
                    warmUpFailedAtMs = SystemClock.uptimeMillis()
                }
            }
        }
    }

    private fun closeIdle() {
        try {
            val closing: TextRecognizer
            synchronized(lock) {
                val current = recognizer ?: return
                if (running || SystemClock.uptimeMillis() - lastUsedMs <= IDLE_RELEASE_MS) {
                    return
                }
                recognizer = null
                warmUpStarted = false
                bitmap = null
                closing = current
            }
            VisionLog.info("Closing the idle text recognizer")
            closing.close()
        } catch (error: Throwable) {
            VisionLog.once("text:closeIdle:${error.javaClass.name}", "Text recognizer idle close failed: $error")
        }
    }
}

internal object VisionEngine {
    fun perform(handler: VNImageRequestHandler, requests: List<VNRequest>) {
        val faceRequests = requests.filterIsInstance<VNDetectFaceLandmarksRequest>().filter { !it.isCancelled }
        val textRequests = requests.filterIsInstance<VNRecognizeTextRequest>().filter { !it.isCancelled }
        val segmentationRequests = requests.filterIsInstance<VNGeneratePersonSegmentationRequest>()
            .filter { !it.isCancelled }
        for (request in segmentationRequests) {
            VisionLog.notImplemented("VNGeneratePersonSegmentationRequest")
            request.setNoResults()
        }
        if (faceRequests.isEmpty() && textRequests.isEmpty()) {
            return
        }
        if (handler.orientation != CGImagePropertyOrientation.up) {
            VisionLog.once(
                "orientation:${handler.orientation}",
                "VNImageRequestHandler orientation ${handler.orientation} is treated as up"
            )
        }
        val startMs = SystemClock.elapsedRealtime()
        val buffer = handler.cvPixelBuffer
        val source: Any = buffer.poolState ?: buffer.backing
        val (width, height) = VisionMapping.analysisSize(buffer.width, buffer.height)
        if (width <= 0 || height <= 0) {
            throw VNError("The pixel buffer is empty")
        }
        val frame = VisionFrames.acquire(width, height)
        try {
            if (!VisionReadback.read(buffer, frame)) {
                throw VNError("Could not read the pixel buffer")
            }
            if (faceRequests.isNotEmpty()) {
                val faces = detectFaces(frame, source)
                for (request in faceRequests) {
                    request.faceResults = faces
                }
            }
            if (textRequests.isNotEmpty()) {
                val text = VisionTextRecognizer.recognize(frame, source)
                for (request in textRequests) {
                    request.textResults = text.filter { it.boundingBox.height >= request.minimumTextHeight }
                }
            }
        } finally {
            VisionFrames.release(frame)
            PipelineStats.gauge("visionMs", SystemClock.elapsedRealtime() - startMs)
        }
    }

    private fun detectFaces(frame: VisionFrame, source: Any): List<VNFaceObservation> {
        val faces = VisionFaceLandmarkers.detect(frame, source) ?: return emptyList()
        val observations = ArrayList<VNFaceObservation>(faces.size)
        for (landmarks in faces) {
            val count = landmarks.size
            val xs = FloatArray(count)
            val ys = FloatArray(count)
            for (index in 0 until count) {
                val landmark = landmarks[index]
                xs[index] = landmark.x()
                ys[index] = landmark.y()
            }
            VisionMapping.faceObservation(xs, ys, count)?.let { observations.add(it) }
        }
        return observations
    }
}
