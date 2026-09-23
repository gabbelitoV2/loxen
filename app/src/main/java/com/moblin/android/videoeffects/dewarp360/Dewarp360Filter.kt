package com.moblin.android.videoeffects.dewarp360

import com.moblin.android.platform.Bundle
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.coreimage.CIVector
import com.moblin.android.platform.coreimage.CIWarpKernel
import com.moblin.android.platform.simd.SIMD3
import com.moblin.android.platform.simd.float3x3
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

const val graphicsEpsilon = 0.00001

private val kernel: CIWarpKernel? by lazy {
    val url = Bundle.url("default", "metallib") ?: return@lazy null
    val data = runCatching { File(url).readBytes() }.getOrNull() ?: return@lazy null
    runCatching {
        CIWarpKernel(functionName = "dewarp360", fromMetalLibraryData = data)
    }.getOrNull()
}

open class Dewarp360Filter : CIFilter() {
    open var inputImage: CIImage? = null
    open var outputSize: CGSize = CGSize(width = 1920, height = 1080)
    open var fieldOfView: Float = (PI / 2).toFloat()
    open var pan: Float = 0f
    open var tilt: Float = 0f

    override val outputImage: CIImage?
        get() {
            val sourceImage = inputImage ?: return null
            val warpKernel = kernel ?: return null
            return warpKernel.apply(
                extent = CGRect(x = 0.0, y = 0.0, width = outputSize.width, height = outputSize.height),
                roiCallback = { _, rect -> rect },
                image = sourceImage,
                arguments = createArguments(sourceImage)
            )?.cropped(
                to = CGRect(
                    x = 0.0,
                    y = 0.0,
                    width = outputSize.width - graphicsEpsilon,
                    height = outputSize.height - graphicsEpsilon
                )
            )
        }

    private fun createArguments(inputImage: CIImage): List<Any> {
        val outputWidth = outputSize.width.toFloat()
        val outputHeight = outputSize.height.toFloat()
        val fieldOfViewHorizontal = fieldOfView
        val fieldOfViewVertical = outputHeight / outputWidth * fieldOfViewHorizontal
        val fieldOfViewWidth = 2f * tan(fieldOfViewHorizontal / 2f)
        val fieldOfViewHeight = 2f * tan(fieldOfViewVertical / 2f)
        val rotation = createRotationMatrix()
        return listOf(
            inputImage.extent.width.toFloat(),
            inputImage.extent.height.toFloat(),
            outputWidth,
            outputHeight,
            fieldOfViewWidth,
            fieldOfViewHeight,
            rotation[0].toCiVector(),
            rotation[1].toCiVector(),
            rotation[2].toCiVector()
        )
    }

    private fun createRotationMatrix(): float3x3 {
        val cosTheta = cos(pan)
        val sinTheta = sin(pan)
        val cosPhi = cos(-tilt)
        val sinPhi = sin(-tilt)
        val rotationY = float3x3(
            rows = listOf(
                SIMD3(cosPhi, 0f, -sinPhi),
                SIMD3(0f, 1f, 0f),
                SIMD3(sinPhi, 0f, cosPhi)
            )
        )
        val rotationZ = float3x3(
            rows = listOf(
                SIMD3(cosTheta, -sinTheta, 0f),
                SIMD3(sinTheta, cosTheta, 0f),
                SIMD3(0f, 0f, 1f)
            )
        )
        return rotationY * rotationZ
    }
}

fun SIMD3.toCiVector(): CIVector =
    CIVector(values = listOf(x.toDouble(), y.toDouble(), z.toDouble()), count = 3)
