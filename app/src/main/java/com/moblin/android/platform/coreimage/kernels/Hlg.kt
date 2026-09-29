package com.moblin.android.platform.coreimage.kernels

import com.moblin.android.platform.coreimage.CIKernelPort

object HlgToLinearPort : CIKernelPort {
    override val name: String = "hlgToLinear"
    override val isWarp: Boolean = false
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
