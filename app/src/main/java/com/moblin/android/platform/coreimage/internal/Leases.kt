package com.moblin.android.platform.coreimage.internal

import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.PixelBufferLeases
import java.util.Collections
import java.util.IdentityHashMap

internal class RenderLeases private constructor(private val site: String) {
    private val buffers = ArrayList<CVPixelBuffer>(4)

    val count: Int
        get() = buffers.size

    fun holds(buffer: CVPixelBuffer): Boolean {
        return buffers.any { it === buffer }
    }

    fun release() {
        for (buffer in buffers) {
            PixelBufferLeases.release(buffer, site)
        }
        buffers.clear()
    }

    private fun take(buffer: CVPixelBuffer) {
        if (!buffer.isValid || holds(buffer)) {
            return
        }
        if (PixelBufferLeases.retain(buffer, site)) {
            buffers.add(buffer)
        }
    }

    companion object {
        fun acquire(root: ImageNode, target: CVPixelBuffer?, site: String): RenderLeases {
            val leases = RenderLeases(site)
            if (target != null) {
                leases.take(target)
            }
            for (buffer in pixelBufferSources(root)) {
                leases.take(buffer)
            }
            PipelineStats.gauge("fxLeases", leases.count.toLong())
            return leases
        }
    }
}

internal fun pixelBufferSources(root: ImageNode): List<CVPixelBuffer> {
    val sources = ArrayList<CVPixelBuffer>(2)
    val visited: MutableSet<ImageNode> = Collections.newSetFromMap(IdentityHashMap())
    val pending = ArrayList<ImageNode>()
    pending.add(root)
    while (pending.isNotEmpty()) {
        val node = pending.removeAt(pending.size - 1)
        if (!visited.add(node)) {
            continue
        }
        if (node is PixelBufferNode) {
            if (sources.none { it === node.buffer }) {
                sources.add(node.buffer)
            }
            continue
        }
        pending.addAll(node.inputs)
    }
    return sources
}
