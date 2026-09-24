package com.moblin.android.platform.coreimage.internal

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLES30
import android.os.SystemClock
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.EglCore
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.platform.video.kCVPixelFormatType_32RGBA
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal class Target(
    val framebuffer: Int,
    val allocWidth: Int,
    val allocHeight: Int,
    val originX: Double,
    val originY: Double,
    val scaleX: Double,
    val scaleY: Double,
    val validWidth: Int,
    val validHeight: Int,
)

internal class MaterializeRequest(var region: CGRect, var maxWidth: Double, var maxHeight: Double)

internal class RenderContext(val mode: RenderMode) {
    private val materializedNodes = IdentityHashMap<ImageNode, Intermediate>()
    private val forced: MutableSet<ImageNode> = Collections.newSetFromMap(IdentityHashMap())
    private val temporaries = ArrayList<PooledTexture>()
    var draws = 0
    var temps = 0

    fun isForcedMaterialization(node: ImageNode): Boolean {
        return forced.contains(node)
    }

    fun force(node: ImageNode) {
        forced.add(node)
    }

    fun materialized(node: ImageNode): Intermediate? {
        return materializedNodes[node]
    }

    fun setMaterialized(node: ImageNode, intermediate: Intermediate) {
        materializedNodes[node] = intermediate
    }

    fun allocate(width: Int, height: Int, format: TextureFormat = mode.workingFormat): PooledTexture? {
        val texture = TexturePool.obtain(width, height, format, false) ?: return null
        temporaries.add(texture)
        temps += 1
        return texture
    }

    fun adopt(texture: PooledTexture) {
        temporaries.add(texture)
    }

    fun finish() {
        val nowMs = SystemClock.uptimeMillis()
        for (texture in temporaries) {
            TexturePool.release(texture, nowMs)
        }
        temporaries.clear()
    }
}

internal sealed class DrawItem {
    class Chain(val node: ImageNode) : DrawItem()
    class Layer(val spec: LayerSpec, val canvasWidth: Double, val canvasHeight: Double) : DrawItem()
}

internal enum class OutputEncoding {
    opaque,
    keepAlpha,
}

internal object Renderer {
    private const val MAX_BLUR_SIGMA_PER_LEVEL = 4.0
    private const val MAX_TAPS = 16
    private val renderTimesUs = LongArray(31)
    private val sortedRenderTimesUs = LongArray(31)
    private var renderTimeIndex = 0
    private var renderTimeCount = 0

    fun isOpaqueFormat(pixelFormatType: Int): Boolean {
        return pixelFormatType != kCVPixelFormatType_32BGRA && pixelFormatType != kCVPixelFormatType_32RGBA
    }

    fun <T> onPipeline(label: String, fallback: T, block: () -> T): T {
        if (PipelineThread.isCurrent()) {
            return block()
        }
        val site = Throwable().stackTrace.getOrNull(2)?.toString() ?: label
        EffectsLog.once("offthread:$site", "$label called off the pipeline thread from $site")
        return try {
            PipelineThread.runSync(timeoutMs = 3000, block = block)
        } catch (error: Throwable) {
            EffectsLog.once("offthreadfail:$label:${error.javaClass.name}", "$label failed: $error")
            fallback
        }
    }

    fun renderCoreImageToBuffer(root: ImageNode, bounds: CGRect, buffer: CVPixelBuffer) {
        if (!buffer.checkReadable("CIContext.render target")) {
            PipelineStats.increment("fxStale")
            return
        }
        if (!buffer.checkRenderable("CIContext.render target")) {
            return
        }
        guarded("CIContext.render", root, target = buffer) {
            renderCoreImage(
                root = root,
                boundsX = bounds.minX,
                boundsY = bounds.minY,
                framebuffer = buffer.framebuffer,
                width = buffer.width,
                height = buffer.height,
                output = if (isOpaqueFormat(buffer.pixelFormatType)) OutputEncoding.opaque else OutputEncoding.keepAlpha
            )
        }
    }

    fun createBitmapFromCoreImage(root: ImageNode, rect: CGRect): Bitmap? {
        return guarded("CIContext.createCGImage", root, target = null) {
            val width = max(1, Math.round(rect.width).toInt())
            val height = max(1, Math.round(rect.height).toInt())
            if (rect.isNull || rect.isInfinite || width > Gl.maxTextureSize || height > Gl.maxTextureSize) {
                return@guarded null
            }
            val texture = TexturePool.obtain(width, height, TextureFormat.rgba8, false)
                ?: return@guarded null
            try {
                val framebuffer = TexturePool.framebuffer(texture)
                renderCoreImage(
                    root = root,
                    boundsX = rect.minX,
                    boundsY = rect.minY,
                    framebuffer = framebuffer,
                    width = width,
                    height = height,
                    output = OutputEncoding.keepAlpha
                )
                readBitmap(framebuffer, width, height)
            } finally {
                TexturePool.release(texture)
            }
        }
    }

    fun renderMetalPetalToBuffer(root: ImageNode, imageWidth: Int, imageHeight: Int, buffer: CVPixelBuffer) {
        if (!buffer.checkReadable("MTIContext.render target")) {
            PipelineStats.increment("fxStale")
            return
        }
        if (!buffer.checkRenderable("MTIContext.render target")) {
            return
        }
        guarded("MTIContext.render", root, target = buffer) {
            renderMetalPetal(root, imageWidth, imageHeight, buffer.framebuffer, buffer.width, buffer.height, buffer)
        }
    }

    fun createBitmapFromMetalPetal(root: ImageNode, imageWidth: Int, imageHeight: Int): Bitmap? {
        return guarded("MTIContext.makeCGImage", root, target = null) {
            if (imageWidth <= 0 || imageHeight <= 0 || imageWidth > Gl.maxTextureSize || imageHeight > Gl.maxTextureSize) {
                return@guarded null
            }
            val texture = TexturePool.obtain(imageWidth, imageHeight, TextureFormat.rgba8, false)
                ?: return@guarded null
            try {
                val framebuffer = TexturePool.framebuffer(texture)
                renderMetalPetal(root, imageWidth, imageHeight, framebuffer, imageWidth, imageHeight, null)
                readBitmap(framebuffer, imageWidth, imageHeight)
            } finally {
                TexturePool.release(texture)
            }
        }
    }

    fun clearCaches() {
        if (!EglCore.isReady) {
            return
        }
        TextureReaper.evictAll()
        TexturePool.trimAll()
    }

    private fun <T> guarded(label: String, root: ImageNode, target: CVPixelBuffer?, block: () -> T): T? {
        if (!EglCore.isReady) {
            EffectsLog.once("notready:$label", "$label: EGL is not ready")
            return null
        }
        Gl.ensureCapabilities()
        val startNs = System.nanoTime()
        val previousFramebuffer = Gl.currentFramebuffer()
        val viewport = IntArray(4)
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0)
        var leases: RenderLeases? = null
        return try {
            leases = RenderLeases.acquire(root, target, label)
            TextureReaper.beginRender()
            TexturePool.trim()
            block()
        } catch (error: Throwable) {
            EffectsLog.once("error:$label:${error.message}", "$label failed: $error", error)
            PipelineStats.increment("fxErrors")
            val clearOnError = target
            if (clearOnError != null && clearOnError.isValid) {
                try {
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, clearOnError.framebuffer)
                    GLES20.glViewport(0, 0, clearOnError.width, clearOnError.height)
                    GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
                    GLES20.glColorMask(true, true, true, true)
                    GLES20.glClearColor(0f, 0f, 0f, 1f)
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                } catch (_: Throwable) {
                }
            }
            null
        } finally {
            restoreState(previousFramebuffer, viewport)
            leases?.release()
            recordTime((System.nanoTime() - startNs) / 1000)
        }
    }

    private fun recordTime(us: Long) {
        renderTimesUs[renderTimeIndex] = us
        renderTimeIndex = (renderTimeIndex + 1) % renderTimesUs.size
        renderTimeCount = min(renderTimeCount + 1, renderTimesUs.size)
        System.arraycopy(renderTimesUs, 0, sortedRenderTimesUs, 0, renderTimeCount)
        sortedRenderTimesUs.sort(0, renderTimeCount)
        PipelineStats.gauge("fxUs", sortedRenderTimesUs[renderTimeCount / 2])
    }

    private fun restoreState(previousFramebuffer: Int, viewport: IntArray) {
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
        GLES20.glColorMask(true, true, true, true)
        GLES20.glUseProgram(0)
        for (unit in 0 until min(Gl.maxTextureUnits, 16)) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            if (Gl.es3) {
                GLES20.glBindTexture(GLES30.GL_TEXTURE_3D, 0)
            }
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFramebuffer)
        GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3])
        var errors = 0
        while (errors < 16) {
            val error = GLES20.glGetError()
            if (error == GLES20.GL_NO_ERROR) {
                break
            }
            EffectsLog.once("glError:$error", "GL error 0x${Integer.toHexString(error)} during effects rendering")
            errors += 1
        }
    }

    private fun coreImageMode(): RenderMode {
        return if (Gl.linearSupported) RenderMode.linear else RenderMode.encoded
    }

    private fun renderCoreImage(
        root: ImageNode,
        boundsX: Double,
        boundsY: Double,
        framebuffer: Int,
        width: Int,
        height: Int,
        output: OutputEncoding,
    ) {
        val context = RenderContext(coreImageMode())
        try {
            val region = CGRect(boundsX, boundsY, width.toDouble(), height.toDouble())
            if (context.mode == RenderMode.linear) {
                val work = context.allocate(width, height) ?: return
                val workTarget = Target(
                    TexturePool.framebuffer(work),
                    work.width,
                    work.height,
                    boundsX,
                    boundsY,
                    1.0,
                    1.0,
                    width,
                    height
                )
                drawGraph(context, workTarget, root, region, floatArrayOf(0f, 0f, 0f, 0f), true)
                encodePass(context, work, framebuffer, width, height, output)
            } else {
                val target = Target(framebuffer, width, height, boundsX, boundsY, 1.0, 1.0, width, height)
                if (output == OutputEncoding.opaque) {
                    drawGraph(context, target, root, region, floatArrayOf(0f, 0f, 0f, 1f), false)
                } else {
                    drawGraph(context, target, root, region, floatArrayOf(0f, 0f, 0f, 0f), true)
                }
            }
        } finally {
            PipelineStats.gauge("fxDraws", context.draws.toLong())
            PipelineStats.gauge("fxTemps", context.temps.toLong())
            context.finish()
        }
    }

    private fun renderMetalPetal(
        root: ImageNode,
        imageWidth: Int,
        imageHeight: Int,
        framebuffer: Int,
        width: Int,
        height: Int,
        buffer: CVPixelBuffer?,
    ) {
        val context = RenderContext(RenderMode.metalPetal)
        try {
            var node = root
            if (imageWidth != width || imageHeight != height) {
                node = TransformNode(
                    node,
                    CGAffineTransform(
                        width.toDouble() / imageWidth,
                        0.0,
                        0.0,
                        height.toDouble() / imageHeight,
                        0.0,
                        0.0
                    ),
                    false
                )
            }
            val target = Target(framebuffer, width, height, 0.0, 0.0, 1.0, 1.0, width, height)
            val region = CGRect(0.0, 0.0, width.toDouble(), height.toDouble())
            val opaque = buffer != null && isOpaqueFormat(buffer.pixelFormatType)
            if (opaque) {
                drawGraph(context, target, node, region, floatArrayOf(0f, 0f, 0f, 1f), false)
            } else {
                drawGraph(context, target, node, region, floatArrayOf(0f, 0f, 0f, 0f), true)
            }
        } finally {
            PipelineStats.gauge("fxDraws", context.draws.toLong())
            PipelineStats.gauge("fxTemps", context.temps.toLong())
            context.finish()
        }
    }

    fun drawGraph(
        context: RenderContext,
        target: Target,
        root: ImageNode,
        region: CGRect,
        clearColor: FloatArray?,
        writeAlpha: Boolean,
    ) {
        val items = ArrayList<DrawItem>()
        flatten(root, items)
        val requests = IdentityHashMap<ImageNode, MaterializeRequest>()
        for (item in items) {
            when (item) {
                is DrawItem.Chain -> {
                    enforceTextureBudget(context, item.node, min(Gl.maxTextureUnits, 16) - 1)
                    roi(context, item.node, region.intersection(item.node.extent), requests, null)
                }
                is DrawItem.Layer -> {
                    enforceTextureBudget(context, item.spec.content, min(Gl.maxTextureUnits, 16) - 3)
                    layerRoi(context, item, region, requests)
                }
            }
        }
        materializeAll(context, requests)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, target.framebuffer)
        GLES20.glViewport(0, 0, target.allocWidth, target.allocHeight)
        var firstItem = 0
        var clear = clearColor
        if (clear != null && items.isNotEmpty()) {
            val covering = coveringConstant(context, items[0], region)
            if (covering != null) {
                firstItem = 1
                val alpha = covering[3]
                clear = floatArrayOf(
                    covering[0] + clear[0] * (1 - alpha),
                    covering[1] + clear[1] * (1 - alpha),
                    covering[2] + clear[2] * (1 - alpha),
                    if (writeAlpha) alpha + clear[3] * (1 - alpha) else clear[3]
                )
            }
        }
        if (clear != null) {
            val pixels = pixelRect(target, region)
            if (pixels != null) {
                GLES20.glColorMask(true, true, true, true)
                GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
                GLES20.glScissor(pixels[0], pixels[1], pixels[2] - pixels[0], pixels[3] - pixels[1])
                GLES20.glClearColor(clear[0], clear[1], clear[2], clear[3])
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
            }
        }
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glColorMask(true, true, true, writeAlpha)
        for (index in firstItem until items.size) {
            val item = items[index]
            when (item) {
                is DrawItem.Chain -> drawChain(context, target, item.node, region)
                is DrawItem.Layer -> drawLayer(context, target, item, region)
            }
        }
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glColorMask(true, true, true, true)
    }

    private fun coveringConstant(context: RenderContext, item: DrawItem, region: CGRect): FloatArray? {
        val chain = item as? DrawItem.Chain ?: return null
        var node = chain.node
        var bounds = CGRect.infinite
        while (true) {
            when (node) {
                is CropNode -> {
                    bounds = bounds.intersection(node.rect)
                    node = node.input
                }
                is ConstantNode -> {
                    val covered = bounds.intersection(node.extent)
                    if (covered.isNull || !covered.contains(region)) {
                        return null
                    }
                    return workingColor(context.mode, node.red, node.green, node.blue, node.alpha, node.encoding)
                }
                else -> return null
            }
        }
    }

    private fun flatten(node: ImageNode, items: MutableList<DrawItem>) {
        when (node) {
            is CompositeNode -> {
                flatten(node.background, items)
                flatten(node.foreground, items)
            }
            is LayersNode -> {
                flatten(node.background, items)
                for (layer in node.layers) {
                    items.add(DrawItem.Layer(layer, node.canvasWidth, node.canvasHeight))
                }
            }
            is TransformNode -> {
                if (node.pyramidInput() == null && isComposite(node.input)) {
                    val inner = ArrayList<DrawItem>()
                    flatten(node.input, inner)
                    if (inner.all { it is DrawItem.Chain }) {
                        for (item in inner) {
                            items.add(DrawItem.Chain(TransformNode((item as DrawItem.Chain).node, node.transform, false)))
                        }
                        return
                    }
                }
                items.add(DrawItem.Chain(node))
            }
            is CropNode -> {
                val inputExtent = node.input.extent
                if (!inputExtent.isNull && !inputExtent.isInfinite && node.rect.contains(inputExtent)) {
                    flatten(node.input, items)
                    return
                }
                if (isComposite(node.input)) {
                    val inner = ArrayList<DrawItem>()
                    flatten(node.input, inner)
                    if (inner.all { it is DrawItem.Chain }) {
                        for (item in inner) {
                            items.add(DrawItem.Chain(CropNode((item as DrawItem.Chain).node, node.rect)))
                        }
                        return
                    }
                }
                items.add(DrawItem.Chain(node))
            }
            else -> items.add(DrawItem.Chain(node))
        }
    }

    private fun isComposite(node: ImageNode): Boolean {
        return when (node) {
            is CompositeNode -> true
            is TransformNode -> node.pyramidInput() == null && isComposite(node.input)
            is CropNode -> isComposite(node.input)
            else -> false
        }
    }

    private fun addRequest(
        requests: IdentityHashMap<ImageNode, MaterializeRequest>,
        node: ImageNode,
        region: CGRect,
        maxSize: CGRect?,
    ) {
        var bounded = region
        if (!node.extent.isInfinite) {
            bounded = bounded.intersection(node.extent)
        }
        if (bounded.isNull || bounded.isEmpty || bounded.isInfinite) {
            return
        }
        val maxWidth = maxSize?.let { 2 * it.width } ?: Double.MAX_VALUE
        val maxHeight = maxSize?.let { 2 * it.height } ?: Double.MAX_VALUE
        val existing = requests[node]
        if (existing == null) {
            requests[node] = MaterializeRequest(bounded, maxWidth, maxHeight)
        } else {
            existing.region = existing.region.union(bounded)
            existing.maxWidth = max(existing.maxWidth, maxWidth)
            existing.maxHeight = max(existing.maxHeight, maxHeight)
        }
    }

    private fun roi(
        context: RenderContext,
        node: ImageNode,
        region: CGRect,
        requests: IdentityHashMap<ImageNode, MaterializeRequest>,
        warpOutput: CGRect?,
    ) {
        if (region.isNull || region.isEmpty) {
            return
        }
        var bounded = region
        if (!node.extent.isInfinite) {
            bounded = bounded.intersection(node.extent)
            if (bounded.isNull || bounded.isEmpty) {
                return
            }
        }
        if (bounded.isInfinite) {
            return
        }
        if (!node.isFusable || context.isForcedMaterialization(node)) {
            addRequest(requests, node, bounded.insetBy(-1.0, -1.0), warpOutput)
            return
        }
        when (node) {
            is TransformNode -> {
                val inverse = node.transform.inverted()
                if (inverse === node.transform && !node.transform.isIdentity) {
                    return
                }
                val inputRegion = bounded.applying(inverse).insetBy(-1.0, -1.0)
                val pyramid = node.pyramidInput()
                if (pyramid != null) {
                    val margin = (1 shl pyramid.level).toDouble()
                    addRequest(requests, pyramid, inputRegion.insetBy(-margin, -margin), warpOutput)
                } else {
                    roi(context, node.input, inputRegion, requests, warpOutput)
                }
            }
            is CropNode -> roi(context, node.input, bounded.intersection(node.rect), requests, warpOutput)
            is ClampNode -> {
                val extent = node.input.extent
                if (extent.isNull) {
                    return
                }
                if (extent.isInfinite) {
                    roi(context, node.input, bounded, requests, warpOutput)
                    return
                }
                val minX = bounded.minX.coerceIn(extent.minX, extent.maxX)
                val maxX = bounded.maxX.coerceIn(extent.minX, extent.maxX)
                val minY = bounded.minY.coerceIn(extent.minY, extent.maxY)
                val maxY = bounded.maxY.coerceIn(extent.minY, extent.maxY)
                val clamped = CGRect(minX, minY, maxX - minX, maxY - minY).insetBy(-1.0, -1.0)
                roi(context, node.input, clamped, requests, warpOutput)
            }
            is SamplingNode -> roi(context, node.input, bounded.insetBy(-1.0, -1.0), requests, warpOutput)
            is ColorOpNode -> roi(context, node.input, node.op.roi(bounded), requests, warpOutput)
            is WarpNode -> {
                val inputExtent = node.input.extent
                val inputRegion = node.warp.roi(bounded, inputExtent)
                    ?: if (!inputExtent.isInfinite && !inputExtent.isNull) {
                        inputExtent
                    } else {
                        bounded.insetBy(-bounded.width / 2, -bounded.height / 2)
                    }
                roi(context, node.input, inputRegion, requests, bounded)
            }
            is CombineNode -> {
                node.inputs.forEachIndexed { index, input ->
                    roi(context, input, node.combiner.roi(index, bounded), requests, warpOutput)
                }
            }
            is CompositeNode -> {
                roi(context, node.foreground, bounded, requests, warpOutput)
                roi(context, node.background, bounded, requests, warpOutput)
            }
            else -> {}
        }
    }

    private fun layerRoi(
        context: RenderContext,
        item: DrawItem.Layer,
        region: CGRect,
        requests: IdentityHashMap<ImageNode, MaterializeRequest>,
    ) {
        val spec = item.spec
        if (layerDrawRect(item, region) == null) {
            return
        }
        val contentRegion = CGRect(
            spec.regionMinX,
            spec.contentHeight - spec.regionMaxY,
            spec.regionMaxX - spec.regionMinX,
            spec.regionMaxY - spec.regionMinY
        ).insetBy(-1.0, -1.0)
        roi(context, spec.content, contentRegion, requests, null)
        spec.mask?.let {
            roi(context, it.content, CGRect(0.0, 0.0, it.width, it.height), requests, null)
        }
        spec.compositingMask?.let {
            roi(context, it.content, CGRect(0.0, 0.0, it.width, it.height), requests, null)
        }
    }

    internal fun countTextures(context: RenderContext, node: ImageNode, seen: MutableSet<ImageNode>): Int {
        if (!seen.add(node)) {
            return 0
        }
        if (!node.isFusable || context.isForcedMaterialization(node)) {
            return 1
        }
        return when (node) {
            is PixelBufferNode -> node.buffer.planeCount
            is BitmapNode -> 1
            is TransformNode -> if (node.pyramidInput() != null) 1 else countTextures(context, node.input, seen)
            else -> node.inputs.sumOf { countTextures(context, it, seen) }
        }
    }

    internal fun enforceTextureBudget(context: RenderContext, root: ImageNode, limit: Int) {
        var guard = 0
        while (countTextures(context, root, Collections.newSetFromMap(IdentityHashMap())) > limit && guard < 64) {
            guard += 1
            if (!splitHeaviest(context, root, limit)) {
                break
            }
        }
    }

    private fun splitHeaviest(context: RenderContext, root: ImageNode, limit: Int): Boolean {
        var node = root
        while (true) {
            if (!node.isFusable || context.isForcedMaterialization(node)) {
                return false
            }
            val inputs = if (node is TransformNode && node.pyramidInput() != null) emptyList() else node.inputs
            if (inputs.isEmpty()) {
                return false
            }
            if (inputs.size == 1) {
                node = inputs[0]
                continue
            }
            val counts = inputs.map { countTextures(context, it, Collections.newSetFromMap(IdentityHashMap())) }
            val heaviestIndex = counts.indices.maxByOrNull { counts[it] } ?: return false
            val heaviest = inputs[heaviestIndex]
            if (counts[heaviestIndex] > limit) {
                node = heaviest
                continue
            }
            context.force(heaviest)
            return true
        }
    }

    private fun materializeAll(context: RenderContext, requests: IdentityHashMap<ImageNode, MaterializeRequest>) {
        for ((node, request) in requests) {
            val existing = context.materialized(node)
            val region = request.region
            if (existing != null && existing.covers(region.minX, region.minY, region.maxX, region.maxY)) {
                continue
            }
            val result = materialize(context, node, request) ?: continue
            context.setMaterialized(node, result)
        }
    }

    private fun materialize(context: RenderContext, node: ImageNode, request: MaterializeRequest): Intermediate? {
        return when (node) {
            is BlurNode -> blur(context, node, request.region)
            is PyramidNode -> pyramid(context, node, request.region)
            is CiBoundaryNode -> boundary(context, node)
            is CustomNode -> {
                val region = alignedRegion(request.region, node.extent) ?: return null
                val scale = capScale(region, request)
                val texture = allocateFor(context, region, scale) ?: return null
                val intermediate = Intermediate(
                    texture.first,
                    region.minX,
                    region.minY,
                    scale,
                    scale,
                    texture.second,
                    texture.third
                )
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, TexturePool.framebuffer(texture.first))
                GLES20.glViewport(0, 0, texture.first.width, texture.first.height)
                GLES20.glClearColor(0f, 0f, 0f, 0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                node.render(context, intermediate)
                intermediate
            }
            else -> {
                val region = alignedRegion(request.region, node.extent) ?: return null
                renderToIntermediate(context, node, region, capScale(region, request))
            }
        }
    }

    private fun alignedRegion(region: CGRect, extent: CGRect): CGRect? {
        var bounded = region
        if (!extent.isInfinite) {
            bounded = bounded.intersection(extent.insetBy(-1.0, -1.0))
        }
        if (bounded.isNull || bounded.isEmpty || bounded.isInfinite) {
            return null
        }
        return bounded.integral
    }

    private fun capScale(region: CGRect, request: MaterializeRequest): Double {
        var scale = 1.0
        val maxSize = Gl.maxTextureSize.toDouble()
        scale = min(scale, maxSize / region.width)
        scale = min(scale, maxSize / region.height)
        if (request.maxWidth < region.width) {
            scale = min(scale, request.maxWidth / region.width)
        }
        if (request.maxHeight < region.height) {
            scale = min(scale, request.maxHeight / region.height)
        }
        return max(scale, 1.0 / 64)
    }

    private fun allocateFor(context: RenderContext, region: CGRect, scale: Double): Triple<PooledTexture, Int, Int>? {
        val width = max(1, ceil(region.width * scale - 1e-6).toInt())
        val height = max(1, ceil(region.height * scale - 1e-6).toInt())
        val texture = context.allocate(width, height) ?: return null
        return Triple(texture, width, height)
    }

    fun materializeNode(context: RenderContext, node: ImageNode, region: CGRect): Intermediate? {
        val bounded = if (node.extent.isInfinite) region else region.intersection(node.extent)
        if (bounded.isNull || bounded.isEmpty || bounded.isInfinite) {
            return null
        }
        val existing = context.materialized(node)
        if (existing != null && existing.covers(bounded.minX, bounded.minY, bounded.maxX, bounded.maxY)) {
            return existing
        }
        val result = materialize(context, node, MaterializeRequest(bounded, Double.MAX_VALUE, Double.MAX_VALUE))
        if (result != null) {
            context.setMaterialized(node, result)
        }
        return result
    }

    fun target(intermediate: Intermediate): Target {
        return Target(
            TexturePool.framebuffer(intermediate.texture),
            intermediate.texture.width,
            intermediate.texture.height,
            intermediate.originX,
            intermediate.originY,
            intermediate.scaleX,
            intermediate.scaleY,
            intermediate.validWidth,
            intermediate.validHeight
        )
    }

    fun drawCustom(
        context: RenderContext,
        target: Target,
        builder: ShaderBuilder,
        main: String,
        rect: CGRect,
        blend: Boolean,
    ) {
        val pixels = pixelRect(target, rect) ?: return
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, target.framebuffer)
        GLES20.glViewport(0, 0, target.allocWidth, target.allocHeight)
        GLES20.glColorMask(true, true, true, true)
        if (blend) {
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        } else {
            GLES20.glDisable(GLES20.GL_BLEND)
        }
        drawProgram(context, target, builder, main, pixels)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    fun renderToIntermediate(context: RenderContext, node: ImageNode, region: CGRect, scale: Double): Intermediate? {
        val allocation = allocateFor(context, region, scale) ?: return null
        val texture = allocation.first
        val target = Target(
            TexturePool.framebuffer(texture),
            texture.width,
            texture.height,
            region.minX,
            region.minY,
            scale,
            scale,
            allocation.second,
            allocation.third
        )
        drawGraph(context, target, node, region, floatArrayOf(0f, 0f, 0f, 0f), true)
        return Intermediate(texture, region.minX, region.minY, scale, scale, allocation.second, allocation.third)
    }

    private fun pixelRect(target: Target, rect: CGRect): IntArray? {
        if (rect.isNull || rect.isEmpty) {
            return null
        }
        val x0 = floor((rect.minX - target.originX) * target.scaleX + 1e-6).coerceIn(0.0, target.validWidth.toDouble())
        val y0 = floor((rect.minY - target.originY) * target.scaleY + 1e-6).coerceIn(0.0, target.validHeight.toDouble())
        val x1 = ceil((rect.maxX - target.originX) * target.scaleX - 1e-6).coerceIn(0.0, target.validWidth.toDouble())
        val y1 = ceil((rect.maxY - target.originY) * target.scaleY - 1e-6).coerceIn(0.0, target.validHeight.toDouble())
        if (x0.isNaN() || y0.isNaN() || x1.isNaN() || y1.isNaN() || x1 <= x0 || y1 <= y0) {
            return null
        }
        return intArrayOf(x0.toInt(), y0.toInt(), x1.toInt(), y1.toInt())
    }

    private fun drawProgram(
        context: RenderContext,
        target: Target,
        builder: ShaderBuilder,
        main: String,
        pixels: IntArray,
    ) {
        val program = Gl.program(builder.source(main)) ?: return
        GLES20.glUseProgram(program.id)
        builder.textures.forEachIndexed { unit, binding ->
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit)
            GLES20.glBindTexture(binding.target, binding.texture)
        }
        for (setter in builder.setters) {
            setter(program)
        }
        GLES20.glUniform2f(program.location("uOrigin"), target.originX.toFloat(), target.originY.toFloat())
        GLES20.glUniform2f(program.location("uScale"), target.scaleX.toFloat(), target.scaleY.toFloat())
        val x0 = pixels[0].toFloat() / target.allocWidth * 2f - 1f
        val y0 = pixels[1].toFloat() / target.allocHeight * 2f - 1f
        val x1 = pixels[2].toFloat() / target.allocWidth * 2f - 1f
        val y1 = pixels[3].toFloat() / target.allocHeight * 2f - 1f
        Gl.drawQuad(program, x0, y0, x1, y1)
        context.draws += 1
        for (unit in builder.textures.indices) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit)
            GLES20.glBindTexture(builder.textures[unit].target, 0)
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    }

    private fun drawChain(context: RenderContext, target: Target, node: ImageNode, region: CGRect) {
        val rect = region.intersection(node.extent)
        val pixels = pixelRect(target, rect) ?: return
        val builder = ShaderBuilder(context)
        val function = builder.emit(node)
        val main = "uniform vec2 uOrigin;\nuniform vec2 uScale;\n" +
            "void main() {\n    vec2 p = uOrigin + gl_FragCoord.xy / uScale;\n    fragColor = $function(p);\n}\n"
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, target.framebuffer)
        GLES20.glViewport(0, 0, target.allocWidth, target.allocHeight)
        drawProgram(context, target, builder, main, pixels)
    }

    private fun layerDrawRect(item: DrawItem.Layer, region: CGRect): CGRect? {
        val spec = item.spec
        if (spec.width <= 0 || spec.height <= 0) {
            return null
        }
        val cosine = cos(-spec.rotation)
        val sine = sin(-spec.rotation)
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        for (corner in 0 until 4) {
            val lx = if (corner % 2 == 0) -spec.width / 2 else spec.width / 2
            val ly = if (corner < 2) -spec.height / 2 else spec.height / 2
            val x = spec.centerX + cosine * lx - sine * ly
            val y = spec.centerY + sine * lx + cosine * ly
            minX = min(minX, x)
            minY = min(minY, y)
            maxX = max(maxX, x)
            maxY = max(maxY, y)
        }
        val canvas = CGRect(0.0, 0.0, item.canvasWidth, item.canvasHeight)
        val rect = CGRect(minX, minY, maxX - minX, maxY - minY).intersection(region).intersection(canvas)
        if (rect.isNull || rect.isEmpty) {
            return null
        }
        return rect
    }

    private fun drawLayer(context: RenderContext, target: Target, item: DrawItem.Layer, region: CGRect) {
        val rect = layerDrawRect(item, region) ?: return
        val pixels = pixelRect(target, rect) ?: return
        val spec = item.spec
        if (spec.blend != LayerBlend.normal) {
            EffectsLog.once("layerblend", "MTILayer blend modes other than normal are drawn as normal")
        }
        val builder = ShaderBuilder(context)
        val content = builder.emit(spec.content)
        val maskFunction = spec.mask?.let { builder.emit(it.content) }
        val compositingMaskFunction = spec.compositingMask?.let { builder.emit(it.content) }
        builder.includeOnce("corner", CORNER_LIBRARY)
        val center = builder.uniform2f(spec.centerX.toFloat(), spec.centerY.toFloat())
        val rotation = builder.uniform2f(cos(spec.rotation).toFloat(), sin(spec.rotation).toFloat())
        val size = builder.uniform2f(spec.width.toFloat(), spec.height.toFloat())
        val u0 = if (spec.flipHorizontally) spec.regionMaxX / spec.contentWidth else spec.regionMinX / spec.contentWidth
        val u1 = if (spec.flipHorizontally) spec.regionMinX / spec.contentWidth else spec.regionMaxX / spec.contentWidth
        val v0 = if (spec.flipVertically) spec.regionMaxY / spec.contentHeight else spec.regionMinY / spec.contentHeight
        val v1 = if (spec.flipVertically) spec.regionMinY / spec.contentHeight else spec.regionMaxY / spec.contentHeight
        val texture = builder.uniform4f(u0.toFloat(), v0.toFloat(), u1.toFloat(), v1.toFloat())
        val contentSize = builder.uniform2f(spec.contentWidth.toFloat(), spec.contentHeight.toFloat())
        val opacity = builder.uniform1f(spec.opacity.toFloat())
        val main = StringBuilder()
        main.append("uniform vec2 uOrigin;\nuniform vec2 uScale;\n")
        main.append("void main() {\n")
        main.append("    vec2 p = uOrigin + gl_FragCoord.xy / uScale;\n")
        main.append("    vec2 d = p - $center;\n")
        main.append("    vec2 l = vec2($rotation.x * d.x - $rotation.y * d.y, $rotation.y * d.x + $rotation.x * d.y);\n")
        main.append("    vec2 pil = vec2((l.x + $size.x * 0.5) / $size.x, ($size.y * 0.5 - l.y) / $size.y);\n")
        main.append("    if (pil.x < 0.0 || pil.y < 0.0 || pil.x >= 1.0 || pil.y >= 1.0) {\n        discard;\n    }\n")
        main.append("    float u = mix($texture.x, $texture.z, pil.x);\n")
        main.append("    float v = mix($texture.y, $texture.w, pil.y);\n")
        main.append("    vec4 c = mbUnpremultiply($content(vec2(u * $contentSize.x, $contentSize.y - v * $contentSize.y)));\n")
        val mask = spec.mask
        if (mask != null && maskFunction != null) {
            val maskSize = builder.uniform2f(mask.width.toFloat(), mask.height.toFloat())
            main.append(
                "    vec4 m = mbUnpremultiply($maskFunction(vec2(pil.x * $maskSize.x, $maskSize.y - pil.y * $maskSize.y)));\n"
            )
            main.append("    float mv = m[${mask.component}];\n")
            main.append(if (mask.oneMinus) "    c.a *= 1.0 - mv;\n" else "    c.a *= mv;\n")
        }
        val compositingMask = spec.compositingMask
        if (compositingMask != null && compositingMaskFunction != null) {
            val maskSize = builder.uniform2f(compositingMask.width.toFloat(), compositingMask.height.toFloat())
            val canvas = builder.uniform2f(item.canvasWidth.toFloat(), item.canvasHeight.toFloat())
            main.append(
                "    vec4 cm = mbUnpremultiply($compositingMaskFunction(p / $canvas * $maskSize));\n"
            )
            main.append("    float cmv = cm[${compositingMask.component}];\n")
            main.append(if (compositingMask.oneMinus) "    c.a *= 1.0 - cmv;\n" else "    c.a *= cmv;\n")
        }
        if (spec.tint[3] > 0f) {
            val tint = builder.uniform4f(spec.tint[0], spec.tint[1], spec.tint[2], spec.tint[3])
            main.append("    c.rgb = $tint.rgb;\n    c.a *= $tint.a;\n")
        }
        if (spec.cornerRadius.any { it > 0f }) {
            val expansion = if (spec.continuousCorners) 1.528665f else 1f
            val radius = builder.uniform4f(
                spec.cornerRadius[0] * expansion,
                spec.cornerRadius[1] * expansion,
                spec.cornerRadius[2] * expansion,
                spec.cornerRadius[3] * expansion
            )
            val function = if (spec.continuousCorners) "mbContinuousCornerMask" else "mbCircularCornerMask"
            main.append("    c.a *= $function($size, pil, $radius);\n")
        }
        main.append("    c.a = clamp(c.a * $opacity, 0.0, 1.0);\n")
        main.append("    fragColor = vec4(c.rgb * c.a, c.a);\n")
        main.append("}\n")
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, target.framebuffer)
        GLES20.glViewport(0, 0, target.allocWidth, target.allocHeight)
        drawProgram(context, target, builder, main.toString(), pixels)
    }

    private fun fixedProgram(source: String): GlProgram? {
        return Gl.program(Glsl.prelude() + source)
    }

    private fun runPass(
        context: RenderContext,
        program: GlProgram,
        source: PooledTexture,
        sourceWidth: Int,
        sourceHeight: Int,
        target: PooledTexture,
        targetWidth: Int,
        targetHeight: Int,
        setup: (GlProgram) -> Unit,
    ) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, TexturePool.framebuffer(target))
        GLES20.glViewport(0, 0, target.width, target.height)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glColorMask(true, true, true, true)
        GLES20.glUseProgram(program.id)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, source.id)
        GLES20.glUniform1i(program.location("uSource"), 0)
        GLES20.glUniform4f(
            program.location("uSize"),
            sourceWidth.toFloat(),
            sourceHeight.toFloat(),
            source.width.toFloat(),
            source.height.toFloat()
        )
        setup(program)
        Gl.drawQuad(
            program,
            -1f,
            -1f,
            targetWidth.toFloat() / target.width * 2f - 1f,
            targetHeight.toFloat() / target.height * 2f - 1f
        )
        context.draws += 1
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    private fun downsample(context: RenderContext, source: Intermediate, clampEdges: Boolean): Intermediate? {
        val width = max(1, (source.validWidth + 1) / 2)
        val height = max(1, (source.validHeight + 1) / 2)
        val texture = context.allocate(width, height) ?: return null
        val program = fixedProgram(if (clampEdges) DOWNSAMPLE_CLAMP else DOWNSAMPLE_ZERO) ?: return null
        runPass(
            context,
            program,
            source.texture,
            source.validWidth,
            source.validHeight,
            texture,
            width,
            height
        ) {}
        return Intermediate(
            texture,
            source.originX,
            source.originY,
            source.scaleX / 2,
            source.scaleY / 2,
            width,
            height
        )
    }

    private fun blurPass(
        context: RenderContext,
        source: Intermediate,
        horizontal: Boolean,
        offsets: FloatArray,
        weights: FloatArray,
        clampEdges: Boolean,
    ): Intermediate? {
        val texture = context.allocate(source.validWidth, source.validHeight) ?: return null
        val program = fixedProgram(if (clampEdges) BLUR_CLAMP else BLUR_ZERO) ?: return null
        runPass(
            context,
            program,
            source.texture,
            source.validWidth,
            source.validHeight,
            texture,
            source.validWidth,
            source.validHeight
        ) {
            GLES20.glUniform2f(it.location("uDirection"), if (horizontal) 1f else 0f, if (horizontal) 0f else 1f)
            GLES20.glUniform1fv(it.location("uOffsets"), MAX_TAPS, offsets, 0)
            GLES20.glUniform1fv(it.location("uWeights"), MAX_TAPS, weights, 0)
        }
        return Intermediate(
            texture,
            source.originX,
            source.originY,
            source.scaleX,
            source.scaleY,
            source.validWidth,
            source.validHeight
        )
    }

    private fun gaussianTaps(sigma: Double): Pair<FloatArray, FloatArray> {
        val offsets = FloatArray(MAX_TAPS)
        val weights = FloatArray(MAX_TAPS)
        val radius = min(ceil(3 * sigma).toInt(), (MAX_TAPS - 1))
        val discrete = DoubleArray(radius + 1) { exp(-(it * it) / (2 * sigma * sigma)) }
        var total = discrete[0]
        for (n in 1..radius) {
            total += 2 * discrete[n]
        }
        var count = 0
        offsets[count] = 0f
        weights[count] = (discrete[0] / total).toFloat()
        count += 1
        var n = 1
        while (n <= radius && count + 2 <= MAX_TAPS) {
            val w1 = discrete[n]
            val w2 = if (n + 1 <= radius) discrete[n + 1] else 0.0
            val weight = w1 + w2
            val offset = if (weight > 0) (n * w1 + (n + 1) * w2) / weight else n.toDouble()
            offsets[count] = offset.toFloat()
            weights[count] = (weight / total).toFloat()
            offsets[count + 1] = -offset.toFloat()
            weights[count + 1] = (weight / total).toFloat()
            count += 2
            n += 2
        }
        for (index in count until MAX_TAPS) {
            offsets[index] = 0f
            weights[index] = 0f
        }
        return Pair(offsets, weights)
    }

    private fun blur(context: RenderContext, node: BlurNode, region: CGRect): Intermediate? {
        val sigma = node.sigma
        val input = node.input
        if (sigma <= 0.01) {
            val aligned = alignedRegion(region, input.extent) ?: return null
            return renderToIntermediate(context, input, aligned, 1.0)
        }
        var level = 0
        while (sigma / (1 shl level) > MAX_BLUR_SIGMA_PER_LEVEL && level < 10) {
            level += 1
        }
        val padding = 3 * sigma + (2 shl level)
        val inputRegion = alignedRegion(region.insetBy(-padding, -padding), input.extent) ?: return null
        var current = renderToIntermediate(context, input, inputRegion, 1.0) ?: return null
        for (index in 0 until level) {
            current = downsample(context, current, node.clampEdges) ?: return null
        }
        val downsampleVariance = 0.25 * ((1 shl (2 * level)) - 1) / 3.0
        val levelScale = (1 shl level).toDouble()
        val remaining = max(sigma * sigma - downsampleVariance, 0.0)
        val levelSigma = sqrt(remaining) / levelScale
        if (levelSigma < 0.2) {
            return current
        }
        val (offsets, weights) = gaussianTaps(levelSigma)
        val horizontal = blurPass(context, current, true, offsets, weights, node.clampEdges) ?: return null
        return blurPass(context, horizontal, false, offsets, weights, node.clampEdges)
    }

    private fun pyramid(context: RenderContext, node: PyramidNode, region: CGRect): Intermediate? {
        val input = node.input
        if (input is BitmapNode) {
            val entry = BitmapTextures.texture(input.bitmap)
            if (entry != null) {
                val key = node.level * 4 + context.mode.ordinal
                entry.pyramidLevels[key]?.let {
                    return it
                }
                val full = CGRect(0.0, 0.0, input.extent.width, input.extent.height)
                var current = renderToIntermediate(context, input, full, 1.0) ?: return null
                for (index in 0 until node.level) {
                    current = downsample(context, current, false) ?: return null
                }
                val cached = TexturePool.obtain(current.validWidth, current.validHeight, context.mode.workingFormat, true)
                    ?: return current
                try {
                    copyTexture(context, current, cached)
                } catch (error: Throwable) {
                    TexturePool.release(cached)
                    throw error
                }
                val result = Intermediate(
                    cached,
                    current.originX,
                    current.originY,
                    current.scaleX,
                    current.scaleY,
                    current.validWidth,
                    current.validHeight
                )
                BitmapTextures.addPyramidLevel(entry, key, result)
                return result
            }
        }
        val aligned = alignedRegion(region, input.extent) ?: return null
        var current = renderToIntermediate(context, input, aligned, 1.0) ?: return null
        for (index in 0 until node.level) {
            current = downsample(context, current, false) ?: return null
        }
        return current
    }

    private fun copyTexture(context: RenderContext, source: Intermediate, target: PooledTexture) {
        val program = fixedProgram(COPY) ?: return
        runPass(
            context,
            program,
            source.texture,
            source.validWidth,
            source.validHeight,
            target,
            source.validWidth,
            source.validHeight
        ) {}
    }

    private fun boundary(context: RenderContext, node: CiBoundaryNode): Intermediate? {
        val cached = node.cached
        val cachedSlot = node.slot
        if (cached != null && cachedSlot != null && !cachedSlot.released && node.cachedMode == coreImageMode()) {
            TextureReaper.touch(cachedSlot)
            return cached
        }
        val texture = TexturePool.obtain(node.width, node.height, TextureFormat.rgba8, true) ?: return null
        try {
            renderCoreImage(
                root = node.ciNode,
                boundsX = node.bounds.minX,
                boundsY = node.bounds.minY,
                framebuffer = TexturePool.framebuffer(texture),
                width = node.width,
                height = node.height,
                output = if (node.opaque) OutputEncoding.opaque else OutputEncoding.keepAlpha
            )
        } catch (error: Throwable) {
            TexturePool.release(texture)
            throw error
        }
        val intermediate = Intermediate(texture, 0.0, 0.0, 1.0, 1.0, node.width, node.height)
        if (node.ciNode.isStatic) {
            val slot = TextureSlot()
            slot.add(texture)
            node.slot?.releaseAll()
            node.slot = slot
            node.cached = intermediate
            node.cachedMode = coreImageMode()
            TextureReaper.register(slot)
        } else {
            context.adopt(texture)
        }
        return intermediate
    }

    private fun encodePass(
        context: RenderContext,
        source: PooledTexture,
        framebuffer: Int,
        width: Int,
        height: Int,
        output: OutputEncoding,
    ) {
        val program = fixedProgram(if (output == OutputEncoding.opaque) ENCODE_OPAQUE else ENCODE_ALPHA) ?: return
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glColorMask(true, true, true, true)
        GLES20.glUseProgram(program.id)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, source.id)
        GLES20.glUniform1i(program.location("uSource"), 0)
        GLES20.glUniform4f(
            program.location("uSize"),
            width.toFloat(),
            height.toFloat(),
            source.width.toFloat(),
            source.height.toFloat()
        )
        Gl.drawQuad(program, -1f, -1f, 1f, 1f)
        context.draws += 1
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    private fun readBitmap(framebuffer: Int, width: Int, height: Int): Bitmap? {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer)
        val rowBytes = width * 4
        val pixels = ByteBuffer.allocateDirect(rowBytes * height).order(ByteOrder.nativeOrder())
        GLES20.glPixelStorei(GLES20.GL_PACK_ALIGNMENT, 1)
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels)
        val flipped = ByteBuffer.allocate(rowBytes * height).order(ByteOrder.nativeOrder())
        val row = ByteArray(rowBytes)
        for (y in 0 until height) {
            pixels.position((height - 1 - y) * rowBytes)
            pixels.get(row, 0, rowBytes)
            flipped.put(row)
        }
        flipped.rewind()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(flipped)
        return bitmap
    }

    private const val COPY = """
uniform sampler2D uSource;
uniform vec4 uSize;
void main() {
    fragColor = FETCH(uSource, ivec2(gl_FragCoord.xy), uSize.zw);
}
"""

    private const val ENCODE_OPAQUE = """
uniform sampler2D uSource;
uniform vec4 uSize;
void main() {
    vec4 c = FETCH(uSource, ivec2(gl_FragCoord.xy), uSize.zw);
    fragColor = vec4(mbLinearToSrgb(clamp(c.rgb, 0.0, 1.0)), 1.0);
}
"""

    private const val ENCODE_ALPHA = """
uniform sampler2D uSource;
uniform vec4 uSize;
void main() {
    vec4 c = FETCH(uSource, ivec2(gl_FragCoord.xy), uSize.zw);
    c.a = clamp(c.a, 0.0, 1.0);
    fragColor = mbEncode(c);
}
"""

    private const val DOWNSAMPLE_ZERO = """
uniform sampler2D uSource;
uniform vec4 uSize;
void main() {
    fragColor = mbSampleZero(uSource, gl_FragCoord.xy * 2.0, uSize);
}
"""

    private const val DOWNSAMPLE_CLAMP = """
uniform sampler2D uSource;
uniform vec4 uSize;
void main() {
    fragColor = mbSampleClamp(uSource, gl_FragCoord.xy * 2.0, uSize);
}
"""

    private const val BLUR_ZERO = """
uniform sampler2D uSource;
uniform vec4 uSize;
uniform vec2 uDirection;
uniform float uOffsets[16];
uniform float uWeights[16];
void main() {
    vec2 q = gl_FragCoord.xy;
    vec4 sum = vec4(0.0);
    for (int i = 0; i < 16; i++) {
        if (uWeights[i] > 0.0) {
            sum += uWeights[i] * mbSampleZero(uSource, q + uDirection * uOffsets[i], uSize);
        }
    }
    fragColor = sum;
}
"""

    private const val BLUR_CLAMP = """
uniform sampler2D uSource;
uniform vec4 uSize;
uniform vec2 uDirection;
uniform float uOffsets[16];
uniform float uWeights[16];
void main() {
    vec2 q = gl_FragCoord.xy;
    vec4 sum = vec4(0.0);
    for (int i = 0; i < 16; i++) {
        if (uWeights[i] > 0.0) {
            sum += uWeights[i] * mbSampleClamp(uSource, q + uDirection * uOffsets[i], uSize);
        }
    }
    fragColor = sum;
}
"""

    private const val CORNER_LIBRARY = """
float mbCircularCornerSdf(vec2 p, float dp) {
    vec2 uv = clamp(p, 0.0, 1.0);
    if (uv.x == 0.0 || uv.y == 0.0) {
        return 1.0;
    }
    float d = length(uv);
    float dx = abs(length(uv + vec2(dp, 0.0)) - d);
    float dy = abs(length(uv + vec2(0.0, dp)) - d);
    float w = max(dx + dy, 1e-4);
    return clamp((w * 0.5 + (1.0 - d)) / w, 0.0, 1.0);
}
float mbContinuousCornerDistance(vec2 p) {
    vec2 uv = max(abs(p) * 1.199 - vec2(0.199), 0.0);
    return pow(uv.x, 2.68) + pow(uv.y, 2.68);
}
float mbContinuousCornerSdf(vec2 p, float dp) {
    vec2 uv = clamp(p, 0.0, 1.0);
    if (uv.x == 0.0 || uv.y == 0.0) {
        return 1.0;
    }
    float d = mbContinuousCornerDistance(uv);
    float dx = abs(mbContinuousCornerDistance(uv + vec2(dp, 0.0)) - d);
    float dy = abs(mbContinuousCornerDistance(uv + vec2(0.0, dp)) - d);
    float w = max(dx + dy, 1e-4);
    return clamp((w * 0.5 + (1.0 - d)) / w, 0.0, 1.0);
}
float mbCircularCornerMask(vec2 size, vec2 pil, vec4 radius) {
    vec2 t = pil * size;
    float f0 = 1.0;
    float f1 = 1.0;
    float f2 = 1.0;
    float f3 = 1.0;
    if (radius.x > 0.0) {
        f0 = mbCircularCornerSdf(vec2(1.0 - t.x / radius.x, 1.0 - t.y / radius.x), 1.0 / radius.x);
    }
    if (radius.y > 0.0) {
        vec2 rt = vec2(size.x - radius.y, radius.y);
        f1 = mbCircularCornerSdf(vec2((t.x - rt.x) / radius.y, 1.0 - t.y / radius.y), 1.0 / radius.y);
    }
    if (radius.z > 0.0) {
        vec2 rb = vec2(size.x - radius.z, size.y - radius.z);
        f2 = mbCircularCornerSdf(vec2((t.x - rb.x) / radius.z, (t.y - rb.y) / radius.z), 1.0 / radius.z);
    }
    if (radius.w > 0.0) {
        vec2 lb = vec2(radius.w, size.y - radius.w);
        f3 = mbCircularCornerSdf(vec2(1.0 - t.x / radius.w, (t.y - lb.y) / radius.w), 1.0 / radius.w);
    }
    return min(min(f0, f1), min(f2, f3));
}
float mbContinuousCornerMask(vec2 size, vec2 pil, vec4 radius) {
    vec2 t = pil * size;
    float f0 = 1.0;
    float f1 = 1.0;
    float f2 = 1.0;
    float f3 = 1.0;
    if (radius.x > 0.0) {
        f0 = mbContinuousCornerSdf(vec2(1.0 - t.x / radius.x, 1.0 - t.y / radius.x), 1.0 / radius.x);
    }
    if (radius.y > 0.0) {
        vec2 rt = vec2(size.x - radius.y, radius.y);
        f1 = mbContinuousCornerSdf(vec2((t.x - rt.x) / radius.y, 1.0 - t.y / radius.y), 1.0 / radius.y);
    }
    if (radius.z > 0.0) {
        vec2 rb = vec2(size.x - radius.z, size.y - radius.z);
        f2 = mbContinuousCornerSdf(vec2((t.x - rb.x) / radius.z, (t.y - rb.y) / radius.z), 1.0 / radius.z);
    }
    if (radius.w > 0.0) {
        vec2 lb = vec2(radius.w, size.y - radius.w);
        f3 = mbContinuousCornerSdf(vec2(1.0 - t.x / radius.w, (t.y - lb.y) / radius.w), 1.0 / radius.w);
    }
    return min(min(f0, f1), min(f2, f3));
}
"""
}
