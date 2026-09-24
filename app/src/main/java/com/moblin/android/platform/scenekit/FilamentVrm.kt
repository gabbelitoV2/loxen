package com.moblin.android.platform.scenekit

import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import com.google.android.filament.Camera
import com.google.android.filament.ColorGrading
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.SwapChainFlags
import com.google.android.filament.Texture
import com.google.android.filament.ToneMapper
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.moblin.android.platform.core.PipelineStats
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "MoblinEffects"
private const val MAXIMUM_SKIN_JOINTS = 256

internal class FilamentState(
    val engine: Engine,
    val materialProvider: UbershaderProvider,
    val assetLoader: AssetLoader,
    val resourceLoader: ResourceLoader,
)

internal object FilamentHost {
    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var state: FilamentState? = null
    private var failed = false

    private fun handler(): Handler? {
        synchronized(this) {
            if (handler == null && !failed) {
                try {
                    val newThread = HandlerThread("MoblinFilament")
                    newThread.start()
                    thread = newThread
                    handler = Handler(newThread.looper)
                } catch (error: Throwable) {
                    failed = true
                    SceneKitLog.once("filament:thread", "v-tuber: Failed to start the Filament thread: $error")
                }
            }
            return handler
        }
    }

    private fun stateOnThread(): FilamentState? {
        state?.let { return it }
        if (failed) {
            return null
        }
        return try {
            Filament.init()
            Gltfio.init()
            val engine = Engine.create(Engine.Backend.OPENGL)
            val materialProvider = UbershaderProvider(engine)
            val assetLoader = AssetLoader(engine, materialProvider, EntityManager.get())
            val resourceLoader = ResourceLoader(engine)
            Log.i(TAG, "v-tuber: Filament engine created")
            FilamentState(engine, materialProvider, assetLoader, resourceLoader).also { state = it }
        } catch (error: Throwable) {
            failed = true
            SceneKitLog.once("filament:init", "v-tuber: Failed to create the Filament engine: $error")
            null
        }
    }

    fun <T> run(timeoutMs: Long, block: (FilamentState) -> T?): T? {
        val handler = handler() ?: return null
        if (Thread.currentThread() === handler.looper.thread) {
            val current = stateOnThread() ?: return null
            return block(current)
        }
        val latch = CountDownLatch(1)
        val result = AtomicReference<T?>(null)
        val posted = handler.post {
            try {
                val current = stateOnThread()
                if (current != null) {
                    result.set(block(current))
                }
            } catch (error: Throwable) {
                SceneKitLog.once("filament:run:${error.message}", "v-tuber: Filament call failed: $error")
            } finally {
                latch.countDown()
            }
        }
        if (!posted) {
            return null
        }
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            SceneKitLog.once("filament:timeout:$timeoutMs", "v-tuber: Filament call timed out after $timeoutMs ms")
            return null
        }
        return result.get()
    }

    fun post(block: (FilamentState?) -> Unit): Boolean {
        val handler = handler() ?: return false
        return handler.post {
            val current = try {
                stateOnThread()
            } catch (error: Throwable) {
                null
            }
            try {
                block(current)
            } catch (error: Throwable) {
                SceneKitLog.once("filament:post:${error.message}", "v-tuber: Filament call failed: $error")
            }
        }
    }
}

private object FilamentReaper {
    private abstract class Reference(owner: Any, queue: ReferenceQueue<Any>) : PhantomReference<Any>(owner, queue) {
        abstract fun release(state: FilamentState)
    }

    private class AssetReference(owner: Any, queue: ReferenceQueue<Any>, val asset: FilamentAssetHandle) :
        Reference(owner, queue) {
        override fun release(state: FilamentState) {
            asset.destroy(state)
        }
    }

    private class RendererReference(owner: Any, queue: ReferenceQueue<Any>, val resources: RendererHandle) :
        Reference(owner, queue) {
        override fun release(state: FilamentState) {
            resources.destroy(state)
        }
    }

    private val queue = ReferenceQueue<Any>()
    private val references = HashSet<Reference>()
    private var thread: Thread? = null

    fun registerAsset(owner: Any, asset: FilamentAssetHandle) {
        register(AssetReference(owner, queue, asset))
    }

    fun registerRenderer(owner: Any, resources: RendererHandle) {
        register(RendererReference(owner, queue, resources))
    }

    private fun register(reference: Reference) {
        synchronized(this) {
            references.add(reference)
            if (thread == null) {
                thread = Thread({ loop() }, "MoblinFilamentReaper").apply {
                    isDaemon = true
                    start()
                }
            }
        }
    }

    private fun loop() {
        while (true) {
            val reference = try {
                queue.remove() as? Reference
            } catch (_: InterruptedException) {
                return
            } ?: continue
            synchronized(this) {
                references.remove(reference)
            }
            FilamentHost.post { state -> state?.let { reference.release(it) } }
        }
    }
}

internal class FilamentAssetHandle(val asset: FilamentAsset) {
    val entities: IntArray = asset.entities
    var destroyed = false

    fun destroy(state: FilamentState) {
        if (destroyed) {
            return
        }
        destroyed = true
        RendererHandle.detachEverywhere(this)
        state.assetLoader.destroyAsset(asset)
    }
}

internal class FilamentVrmAsset private constructor(
    val handle: FilamentAssetHandle,
    val model: VrmFilamentModelInfo,
    val nodeEntities: IntArray,
) {
    companion object {
        fun load(owner: VRMScene, model: VrmFilamentModel): FilamentVrmAsset? {
            if (model.largestSkin > MAXIMUM_SKIN_JOINTS) {
                Log.i(
                    TAG,
                    "v-tuber: VRM skin has ${model.largestSkin} joints, more than the $MAXIMUM_SKIN_JOINTS " +
                        "Filament can render"
                )
                return null
            }
            val info = VrmFilamentModelInfo(
                nodeCount = model.nodeCount,
                meshCount = model.meshCount,
                nodeMeshes = model.nodeMeshes,
                meshTargetCounts = model.meshTargetCounts,
            )
            val loaded = FilamentHost.run(timeoutMs = 60_000) { state ->
                val buffer = ByteBuffer.allocateDirect(model.glb.size).order(ByteOrder.nativeOrder())
                buffer.put(model.glb)
                buffer.flip()
                val asset = state.assetLoader.createAsset(buffer)
                if (asset == null) {
                    Log.i(TAG, "v-tuber: Filament could not parse the VRM model")
                    return@run null
                }
                state.resourceLoader.loadResources(asset)
                asset.releaseSourceData()
                val renderableManager = state.engine.renderableManager
                val nodeEntities = IntArray(model.nodeCount) { asset.getFirstEntityByName(filamentNodeName(it)) }
                for (entity in asset.renderableEntities) {
                    val instance = renderableManager.getInstance(entity)
                    if (instance != 0) {
                        renderableManager.setCulling(instance, false)
                        renderableManager.setCastShadows(instance, false)
                        renderableManager.setReceiveShadows(instance, false)
                    }
                }
                for (node in 0 until model.nodeCount) {
                    val mesh = model.nodeMeshes[node]
                    val entity = nodeEntities[node]
                    if (mesh < 0 || mesh >= model.meshCount || entity == 0) {
                        continue
                    }
                    val instance = renderableManager.getInstance(entity)
                    if (instance == 0) {
                        continue
                    }
                    val blends = model.meshBlendPrimitives[mesh]
                    val orders = model.meshRenderOrders[mesh]
                    val primitiveCount = renderableManager.getPrimitiveCount(instance)
                    for (primitive in 0 until minOf(primitiveCount, blends.size)) {
                        if (blends[primitive] && primitive < orders.size) {
                            renderableManager.setBlendOrderAt(instance, primitive, orders[primitive] and 0x7fff)
                            renderableManager.setGlobalBlendOrderEnabledAt(instance, primitive, true)
                        }
                    }
                }
                Pair(FilamentAssetHandle(asset), nodeEntities)
            } ?: return null
            val result = FilamentVrmAsset(loaded.first, info, loaded.second)
            FilamentReaper.registerAsset(owner, loaded.first)
            return result
        }
    }
}

internal class VrmFilamentModelInfo(
    val nodeCount: Int,
    val meshCount: Int,
    val nodeMeshes: IntArray,
    val meshTargetCounts: IntArray,
)

internal class RendererHandle {
    var renderer: Renderer? = null
    var view: View? = null
    var scene: Scene? = null
    var cameraEntity = 0
    var camera: Camera? = null
    var colorGrading: ColorGrading? = null
    var swapChain: SwapChain? = null
    var width = 0
    var height = 0
    var currentAsset: FilamentAssetHandle? = null
    var readback: ByteBuffer? = null
    var flipped: ByteBuffer? = null

    fun destroy(state: FilamentState) {
        live.remove(this)
        val engine = state.engine
        scene?.let { scene ->
            currentAsset?.let { if (!it.destroyed) scene.removeEntities(it.entities) }
        }
        currentAsset = null
        renderer?.let { engine.destroyRenderer(it) }
        view?.let { engine.destroyView(it) }
        scene?.let { engine.destroyScene(it) }
        if (cameraEntity != 0) {
            engine.destroyCameraComponent(cameraEntity)
            EntityManager.get().destroy(cameraEntity)
        }
        colorGrading?.let { engine.destroyColorGrading(it) }
        swapChain?.let { engine.destroySwapChain(it) }
        renderer = null
        view = null
        scene = null
        cameraEntity = 0
        camera = null
        colorGrading = null
        swapChain = null
    }

    companion object {
        val live = HashSet<RendererHandle>()

        fun detachEverywhere(asset: FilamentAssetHandle) {
            for (handle in live) {
                if (handle.currentAsset === asset) {
                    handle.scene?.removeEntities(asset.entities)
                    handle.currentAsset = null
                }
            }
        }
    }
}

private class SnapshotState(
    val asset: FilamentVrmAsset,
    val localMatrices: Array<FloatArray?>,
    val meshWeights: Array<FloatArray?>,
    val cameraMatrix: FloatArray,
    val fieldOfView: Double,
    val zNear: Double,
    val zFar: Double,
    val width: Int,
    val height: Int,
)

internal class FilamentSceneRenderer {
    private val handle = RendererHandle()
    private val busy = AtomicBoolean(false)
    private val lastBitmap = AtomicReference<Bitmap?>(null)
    private var slowRenders = 0
    private var lastSlowLogMs = Long.MIN_VALUE / 2

    init {
        FilamentReaper.registerRenderer(this, handle)
    }

    fun render(scene: VRMScene, cameraNode: SCNNode, width: Int, height: Int): Bitmap? {
        val asset = scene.filamentAsset ?: return null
        val camera = cameraNode.camera ?: return null
        if (!busy.compareAndSet(false, true)) {
            return lastBitmap.get()?.let { copyOf(it) }
        }
        val localMatrices = arrayOfNulls<FloatArray>(asset.model.nodeCount)
        for (index in 0 until minOf(asset.model.nodeCount, scene.gltfNodes.size)) {
            localMatrices[index] = scene.gltfNodes[index]?.localMatrix()
        }
        val meshWeights = arrayOfNulls<FloatArray>(asset.model.meshCount)
        for (index in 0 until minOf(asset.model.meshCount, scene.meshNodes.size)) {
            meshWeights[index] = scene.meshNodes[index]?.morphWeights?.copyOf()
        }
        val state = SnapshotState(
            asset = asset,
            localMatrices = localMatrices,
            meshWeights = meshWeights,
            cameraMatrix = cameraNode.worldMatrix(),
            fieldOfView = camera.fieldOfView,
            zNear = camera.zNear,
            zFar = camera.zFar,
            width = width,
            height = height,
        )
        val latch = CountDownLatch(1)
        val result = AtomicReference<Bitmap?>(null)
        val posted = FilamentHost.post { filament ->
            try {
                if (filament != null) {
                    val bitmap = renderOnFilament(filament, state)
                    if (bitmap != null) {
                        lastBitmap.set(bitmap)
                        result.set(bitmap)
                    }
                }
            } catch (error: Throwable) {
                SceneKitLog.once("filament:render:${error.message}", "v-tuber: VRM render failed: $error")
            } finally {
                busy.set(false)
                latch.countDown()
            }
        }
        if (!posted) {
            busy.set(false)
            return null
        }
        if (latch.await(250, TimeUnit.MILLISECONDS)) {
            result.get()?.let { return it }
        } else {
            logSlowRender()
        }
        return lastBitmap.get()?.let { copyOf(it) }
    }

    private fun logSlowRender() {
        PipelineStats.increment("vrmSlow")
        slowRenders += 1
        val nowMs = SystemClock.uptimeMillis()
        if (nowMs - lastSlowLogMs < 5000) {
            return
        }
        lastSlowLogMs = nowMs
        Log.i(TAG, "v-tuber: VRM render took longer than 250 ms ($slowRenders times)")
    }

    private fun copyOf(bitmap: Bitmap): Bitmap? {
        return try {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } catch (_: Throwable) {
            null
        }
    }

    private fun prepare(filament: FilamentState, width: Int, height: Int) {
        val engine = filament.engine
        if (handle.renderer == null) {
            val renderer = engine.createRenderer()
            renderer.clearOptions = Renderer.ClearOptions().apply {
                clear = true
                discard = true
                clearColor = doubleArrayOf(0.0, 0.0, 0.0, 0.0)
            }
            val scene = engine.createScene()
            val cameraEntity = EntityManager.get().create()
            val camera = engine.createCamera(cameraEntity)
            val colorGrading = ColorGrading.Builder()
                .toneMapper(ToneMapper.Linear())
                .build(engine)
            val view = engine.createView()
            view.scene = scene
            view.camera = camera
            view.blendMode = View.BlendMode.TRANSLUCENT
            view.isPostProcessingEnabled = true
            view.antiAliasing = View.AntiAliasing.NONE
            view.dithering = View.Dithering.NONE
            view.setShadowingEnabled(false)
            view.colorGrading = colorGrading
            handle.renderer = renderer
            handle.scene = scene
            handle.cameraEntity = cameraEntity
            handle.camera = camera
            handle.colorGrading = colorGrading
            handle.view = view
            RendererHandle.live.add(handle)
        }
        if (handle.swapChain == null || handle.width != width || handle.height != height) {
            handle.swapChain?.let { engine.destroySwapChain(it) }
            handle.swapChain = engine.createSwapChain(
                width,
                height,
                SwapChainFlags.CONFIG_TRANSPARENT or SwapChainFlags.CONFIG_READABLE
            )
            handle.width = width
            handle.height = height
            handle.view?.viewport = Viewport(0, 0, width, height)
            handle.readback = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
            handle.flipped = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
        }
    }

    private fun renderOnFilament(filament: FilamentState, state: SnapshotState): Bitmap? {
        val asset = state.asset
        if (asset.handle.destroyed) {
            return null
        }
        prepare(filament, state.width, state.height)
        val engine = filament.engine
        val scene = handle.scene ?: return null
        if (handle.currentAsset !== asset.handle) {
            handle.currentAsset?.let { if (!it.destroyed) scene.removeEntities(it.entities) }
            scene.addEntities(asset.handle.entities)
            handle.currentAsset = asset.handle
        }
        val transformManager = engine.transformManager
        transformManager.openLocalTransformTransaction()
        for (index in 0 until asset.model.nodeCount) {
            val matrix = state.localMatrices[index] ?: continue
            val entity = asset.nodeEntities[index]
            if (entity == 0) {
                continue
            }
            val instance = transformManager.getInstance(entity)
            if (instance != 0) {
                transformManager.setTransform(instance, matrix)
            }
        }
        transformManager.commitLocalTransformTransaction()
        val renderableManager = engine.renderableManager
        for (index in 0 until asset.model.nodeCount) {
            val mesh = asset.model.nodeMeshes[index]
            val entity = asset.nodeEntities[index]
            if (mesh < 0 || mesh >= asset.model.meshCount || entity == 0) {
                continue
            }
            val weights = state.meshWeights[mesh] ?: continue
            val instance = renderableManager.getInstance(entity)
            if (instance == 0) {
                continue
            }
            val count = renderableManager.getMorphTargetCount(instance)
            if (count <= 0) {
                continue
            }
            renderableManager.setMorphWeights(instance, FloatArray(count) { weights.getOrElse(it) { 0f } }, 0)
        }
        asset.handle.asset.instance.animator.updateBoneMatrices()
        val camera = handle.camera ?: return null
        camera.setProjection(
            state.fieldOfView,
            state.width.toDouble() / state.height.toDouble(),
            state.zNear,
            state.zFar,
            Camera.Fov.VERTICAL
        )
        camera.setModelMatrix(state.cameraMatrix)
        val renderer = handle.renderer ?: return null
        val swapChain = handle.swapChain ?: return null
        val view = handle.view ?: return null
        val readback = handle.readback ?: return null
        readback.clear()
        val done = CountDownLatch(1)
        if (!renderer.beginFrame(swapChain, 0L)) {
            engine.flushAndWait()
            return null
        }
        renderer.render(view)
        renderer.readPixels(
            0,
            0,
            state.width,
            state.height,
            Texture.PixelBufferDescriptor(
                readback,
                Texture.Format.RGBA,
                Texture.Type.UBYTE,
                1,
                0,
                0,
                state.width,
                Executor { it.run() },
                Runnable { done.countDown() }
            )
        )
        renderer.endFrame()
        var attempts = 0
        while (done.count > 0 && attempts < 3) {
            engine.flushAndWait()
            attempts += 1
        }
        if (!done.await(500, TimeUnit.MILLISECONDS)) {
            return null
        }
        val flipped = handle.flipped ?: return null
        return toBitmap(readback, flipped, state.width, state.height)
    }

    private fun toBitmap(readback: ByteBuffer, flipped: ByteBuffer, width: Int, height: Int): Bitmap {
        val rowBytes = width * 4
        flipped.clear()
        val row = ByteArray(rowBytes)
        for (y in 0 until height) {
            readback.position((height - 1 - y) * rowBytes)
            readback.get(row)
            flipped.put(row)
        }
        flipped.position(0)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(flipped)
        return bitmap
    }
}
