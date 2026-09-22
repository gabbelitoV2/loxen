package com.moblin.android.videoeffects.dewarp360

import android.media.Image
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

val graphicsEpsilon: Float = 0.00001f

private val kernel: Any? by lazy {
    Unit
}

data class SizeF(val width: Float, val height: Float)

data class Float3(val x: Float, val y: Float, val z: Float) {
    operator fun get(index: Int): Float = when (index) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IndexOutOfBoundsException("Float3 index $index")
    }
}

class Float3x3(private val rows: List<Float3>) {
    init {
        require(rows.size == 3) { "A 3x3 matrix needs three rows" }
    }

    operator fun get(index: Int): Float3 = rows[index]

    operator fun times(other: Float3x3): Float3x3 {
        val result = List(3) { i ->
            val row = rows[i]
            val values = FloatArray(3)
            for (j in 0 until 3) {
                var sum = 0f
                for (k in 0 until 3) {
                    sum += row[k] * other[k][j]
                }
                values[j] = sum
            }
            Float3(values[0], values[1], values[2])
        }
        return Float3x3(result)
    }
}

class Dewarp360Filter {
    var inputImage: Image? = null
    var outputSize: SizeF = SizeF(1920f, 1080f)
    var fieldOfView: Float = (PI / 2).toFloat()
    var pan: Float = 0f
    var tilt: Float = 0f

    val outputImage: Image?
        get() = null

    private fun createArguments(inputImage: Image): List<Any> {
        val outputWidth = outputSize.width
        val outputHeight = outputSize.height
        val fieldOfViewHorizontal = fieldOfView
        val fieldOfViewVertical = outputHeight / outputWidth * fieldOfViewHorizontal
        val fieldOfViewWidth = 2 * tan((fieldOfViewHorizontal / 2).toDouble()).toFloat()
        val fieldOfViewHeight = 2 * tan((fieldOfViewVertical / 2).toDouble()).toFloat()
        val rotation = createRotationMatrix()
        return listOf<Any>(
            inputImage.width.toFloat(),
            inputImage.height.toFloat(),
            outputWidth,
            outputHeight,
            fieldOfViewWidth,
            fieldOfViewHeight,
            rotation[0].toCiVector(),
            rotation[1].toCiVector(),
            rotation[2].toCiVector()
        )
    }

    private fun createRotationMatrix(): Float3x3 {
        val cosTheta = cos(pan.toDouble()).toFloat()
        val sinTheta = sin(pan.toDouble()).toFloat()
        val cosPhi = cos((-tilt).toDouble()).toFloat()
        val sinPhi = sin((-tilt).toDouble()).toFloat()
        val rotationY = Float3x3(
            listOf(
                Float3(cosPhi, 0f, -sinPhi),
                Float3(0f, 1f, 0f),
                Float3(sinPhi, 0f, cosPhi)
            )
        )
        val rotationZ = Float3x3(
            listOf(
                Float3(cosTheta, -sinTheta, 0f),
                Float3(sinTheta, cosTheta, 0f),
                Float3(0f, 0f, 1f)
            )
        )
        return rotationY * rotationZ
    }
}

fun Float3.toCiVector(): FloatArray = floatArrayOf(x, y, z)
