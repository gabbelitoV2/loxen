package com.moblin.android.platform.coreimage.kernels

import com.moblin.android.platform.coreimage.CIColorKernelCpu
import com.moblin.android.platform.coreimage.CIKernelPort
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

private const val hlgA = 0.17883277
private const val hlgB = 0.28466892
private const val hlgC = 0.55991073
private const val referenceWhite = 203.0 / 1000.0
private val bt2020Luminance = doubleArrayOf(0.2627, 0.6780, 0.0593)
private val bt709ToBt2020 = arrayOf(
    doubleArrayOf(0.627404, 0.329283, 0.043313),
    doubleArrayOf(0.069097, 0.919540, 0.011362),
    doubleArrayOf(0.016391, 0.088013, 0.895595),
)
private val bt2020ToBt709 = arrayOf(
    doubleArrayOf(1.660491, -0.587641, -0.072850),
    doubleArrayOf(-0.124550, 1.132900, -0.008349),
    doubleArrayOf(-0.018151, -0.100579, 1.118730),
)

private fun hlgInverseOetf(value: Double): Double {
    if (value <= 0.5) {
        return value * value / 3.0
    }
    return (exp((value - hlgC) / hlgA) + hlgB) / 12.0
}

private fun hlgOetf(value: Double): Double {
    if (value <= 1.0 / 12.0) {
        return sqrt(3.0 * value)
    }
    return hlgA * ln(12.0 * value - hlgB) + hlgC
}

private fun multiply(matrix: Array<DoubleArray>, vector: DoubleArray): DoubleArray {
    return DoubleArray(3) { row ->
        matrix[row][0] * vector[0] + matrix[row][1] * vector[1] + matrix[row][2] * vector[2]
    }
}

private fun luminance(vector: DoubleArray): Double {
    return vector[0] * bt2020Luminance[0] + vector[1] * bt2020Luminance[1] + vector[2] * bt2020Luminance[2]
}

private fun hlgToLinear(sample: DoubleArray): DoubleArray {
    val scene = DoubleArray(3) { hlgInverseOetf(sample[it].coerceIn(0.0, 1.0)) }
    val luminance = luminance(scene)
    val gain = if (luminance > 0.0) luminance.pow(0.2) else 0.0
    val display = DoubleArray(3) { scene[it] * gain }
    val linear = multiply(bt2020ToBt709, display)
    return doubleArrayOf(
        linear[0] / referenceWhite,
        linear[1] / referenceWhite,
        linear[2] / referenceWhite,
        sample[3],
    )
}

private fun linearToHlg(sample: DoubleArray): DoubleArray {
    val scaled = doubleArrayOf(sample[0] * referenceWhite, sample[1] * referenceWhite, sample[2] * referenceWhite)
    val display = multiply(bt709ToBt2020, scaled).map { it.coerceIn(0.0, 1.0) }.toDoubleArray()
    val luminance = luminance(display)
    val gain = if (luminance > 0.0) luminance.pow(-1.0 / 6.0) else 0.0
    return doubleArrayOf(
        hlgOetf((display[0] * gain).coerceIn(0.0, 1.0)),
        hlgOetf((display[1] * gain).coerceIn(0.0, 1.0)),
        hlgOetf((display[2] * gain).coerceIn(0.0, 1.0)),
        sample[3],
    )
}

object HlgToLinearPort : CIKernelPort {
    override val name: String = "hlgToLinear"
    override val isWarp: Boolean = false
    override val cpu: CIColorKernelCpu = CIColorKernelCpu { inputs, _ -> hlgToLinear(inputs[0]) }
    override val glsl: String = """
float hlgInverseOetf(float value) {
    if (value <= 0.5) {
        return value * value / 3.0;
    }
    return (exp((value - 0.55991073) / 0.17883277) + 0.28466892) / 12.0;
}

vec4 hlgToLinear(vec2 destCoord, vec4 sampleValue) {
    const float referenceWhite = 203.0 / 1000.0;
    const vec3 bt2020Luminance = vec3(0.2627, 0.6780, 0.0593);
    const mat3 bt2020ToBt709 = mat3(
        vec3(1.660491, -0.124550, -0.018151),
        vec3(-0.587641, 1.132900, -0.100579),
        vec3(-0.072850, -0.008349, 1.118730)
    );
    vec3 signal = clamp(sampleValue.rgb, 0.0, 1.0);
    vec3 scene = vec3(hlgInverseOetf(signal.r), hlgInverseOetf(signal.g), hlgInverseOetf(signal.b));
    float luminance = dot(scene, bt2020Luminance);
    vec3 display = luminance > 0.0 ? scene * pow(luminance, 0.2) : vec3(0.0);
    return vec4(bt2020ToBt709 * display / referenceWhite, sampleValue.a);
}
"""
}

object LinearToHlgPort : CIKernelPort {
    override val name: String = "linearToHlg"
    override val isWarp: Boolean = false
    override val cpu: CIColorKernelCpu = CIColorKernelCpu { inputs, _ -> linearToHlg(inputs[0]) }
    override val glsl: String = """
float hlgOetf(float value) {
    if (value <= 1.0 / 12.0) {
        return sqrt(3.0 * value);
    }
    return 0.17883277 * log(12.0 * value - 0.28466892) + 0.55991073;
}

vec4 linearToHlg(vec2 destCoord, vec4 sampleValue) {
    const float referenceWhite = 203.0 / 1000.0;
    const vec3 bt2020Luminance = vec3(0.2627, 0.6780, 0.0593);
    const mat3 bt709ToBt2020 = mat3(
        vec3(0.627404, 0.069097, 0.016391),
        vec3(0.329283, 0.919540, 0.088013),
        vec3(0.043313, 0.011362, 0.895595)
    );
    vec3 display = clamp(bt709ToBt2020 * sampleValue.rgb * referenceWhite, 0.0, 1.0);
    float luminance = dot(display, bt2020Luminance);
    vec3 scene = luminance > 0.0 ? display * pow(luminance, -1.0 / 6.0) : vec3(0.0);
    scene = clamp(scene, 0.0, 1.0);
    return vec4(hlgOetf(scene.r), hlgOetf(scene.g), hlgOetf(scene.b), sampleValue.a);
}
"""
}
