package com.moblin.android.platform.coreimage

import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coreimage.internal.CombineNode
import com.moblin.android.platform.coreimage.internal.CombineOp
import com.moblin.android.platform.coreimage.internal.EffectsLog
import com.moblin.android.platform.coreimage.internal.ShaderBuilder
import com.moblin.android.platform.coreimage.internal.WarpNode
import com.moblin.android.platform.coreimage.internal.WarpOp
import com.moblin.android.platform.coreimage.kernels.CrtBarrelDistortionPort
import com.moblin.android.platform.coreimage.kernels.Dewarp360Port
import com.moblin.android.platform.coreimage.kernels.HlgToLinearPort
import com.moblin.android.platform.coreimage.kernels.LinearToHlgPort

class CIKernelException(message: String) : Exception(message)

interface CIKernelPort {
    val name: String
    val isWarp: Boolean
    val glsl: String?
}

object CIKernelLibrary {
    private class Entry(val name: String, val isWarp: Boolean, val glsl: String)

    private val entries = HashMap<String, Entry>()

    init {
        for (port in kernelPorts()) {
            val glsl = port.glsl ?: continue
            if (port.isWarp) {
                registerWarp(port.name, glsl)
            } else {
                registerColor(port.name, glsl)
            }
        }
    }

    fun registerWarp(name: String, glsl: String) {
        synchronized(entries) {
            entries[name] = Entry(name, true, glsl)
        }
    }

    fun registerColor(name: String, glsl: String) {
        synchronized(entries) {
            entries[name] = Entry(name, false, glsl)
        }
    }

    internal fun glsl(name: String, isWarp: Boolean): String? {
        return synchronized(entries) {
            entries[name]?.takeIf { it.isWarp == isWarp }?.glsl
        }
    }
}

private fun kernelPorts(): List<CIKernelPort> {
    return listOf(CrtBarrelDistortionPort, Dewarp360Port, HlgToLinearPort, LinearToHlgPort)
}

open class CIKernel(functionName: String, @Suppress("UNUSED_PARAMETER") fromMetalLibraryData: ByteArray) {
    val name: String = functionName
    internal val glsl: String

    init {
        glsl = CIKernelLibrary.glsl(functionName, this is CIWarpKernel)
            ?: throw CIKernelException("No GLSL port of the kernel $functionName").also {
                EffectsLog.once("kernel:$functionName", "CIKernel $functionName has no GLSL port")
            }
    }
}

internal class KernelArgument(val glslType: String, val values: FloatArray?, val image: CIImage?)

internal fun kernelArgument(value: Any): KernelArgument? {
    return when (value) {
        is CIImage -> KernelArgument("vec4", null, value)
        is Float -> KernelArgument("float", floatArrayOf(value), null)
        is Double -> KernelArgument("float", floatArrayOf(value.toFloat()), null)
        is Int -> KernelArgument("float", floatArrayOf(value.toFloat()), null)
        is Number -> KernelArgument("float", floatArrayOf(value.toFloat()), null)
        is CGPoint -> KernelArgument("vec2", floatArrayOf(value.x.toFloat(), value.y.toFloat()), null)
        is CIColor -> KernelArgument(
            "vec4",
            floatArrayOf(value.red.toFloat(), value.green.toFloat(), value.blue.toFloat(), value.alpha.toFloat()),
            null
        )
        is CIVector -> {
            val floats = FloatArray(value.count) { value.value(at = it).toFloat() }
            when (value.count) {
                1 -> KernelArgument("float", floats, null)
                2 -> KernelArgument("vec2", floats, null)
                3 -> KernelArgument("vec3", floats, null)
                4 -> KernelArgument("vec4", floats, null)
                else -> null
            }
        }
        else -> null
    }
}

internal fun bindKernelArgument(builder: ShaderBuilder, argument: KernelArgument): String {
    val values = argument.values ?: floatArrayOf(0f)
    return when (argument.glslType) {
        "float" -> builder.uniform1f(values[0])
        "vec2" -> builder.uniform2f(values[0], values[1])
        "vec3" -> builder.uniform3f(values[0], values[1], values[2])
        else -> builder.uniform4f(values[0], values[1], values[2], values[3])
    }
}

internal class KernelWarpOp(
    private val kernelName: String,
    private val glsl: String,
    private val arguments: List<KernelArgument>,
) : WarpOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        builder.includeOnce("kernel:$kernelName", glsl + "\n")
        val names = arguments.map { bindKernelArgument(builder, it) }
        val call = (listOf("p") + names).joinToString(", ")
        builder.function("vec2 $name(vec2 p) {\n    return $kernelName($call);\n}\n")
    }
}

internal class KernelColorOp(
    private val kernelName: String,
    private val glsl: String,
    private val arguments: List<KernelArgument>,
) : CombineOp() {
    override fun emit(builder: ShaderBuilder, name: String, inputs: List<String>) {
        builder.includeOnce("kernel:$kernelName", glsl + "\n")
        var imageIndex = 0
        val names = arguments.map {
            if (it.image != null) {
                val input = inputs[imageIndex]
                imageIndex += 1
                "$input(p)"
            } else {
                bindKernelArgument(builder, it)
            }
        }
        val call = (listOf("p") + names).joinToString(", ")
        builder.function("vec4 $name(vec2 p) {\n    return $kernelName($call);\n}\n")
    }
}

class CIWarpKernel(functionName: String, fromMetalLibraryData: ByteArray) :
    CIKernel(functionName, fromMetalLibraryData) {
    fun apply(
        extent: CGRect,
        roiCallback: (Int, CGRect) -> CGRect,
        image: CIImage,
        arguments: List<Any>,
    ): CIImage? {
        val converted = arguments.map { kernelArgument(it) }
        if (converted.any { it == null || it.image != null }) {
            EffectsLog.once("warpArguments:$name", "CIWarpKernel $name: unsupported arguments")
            return null
        }
        return CIImage(WarpNode(image.node, KernelWarpOp(name, glsl, converted.filterNotNull()), extent))
    }
}

class CIColorKernel(functionName: String, fromMetalLibraryData: ByteArray) :
    CIKernel(functionName, fromMetalLibraryData) {
    fun apply(extent: CGRect, arguments: List<Any>): CIImage? {
        val converted = arguments.map { kernelArgument(it) }
        if (converted.any { it == null }) {
            EffectsLog.once("colorArguments:$name", "CIColorKernel $name: unsupported arguments")
            return null
        }
        val images = converted.mapNotNull { it?.image?.node }
        return CIImage(CombineNode(images, KernelColorOp(name, glsl, converted.filterNotNull()), extent))
    }
}
