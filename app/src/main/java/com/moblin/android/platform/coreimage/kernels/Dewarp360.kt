package com.moblin.android.platform.coreimage.kernels

import com.moblin.android.platform.coreimage.CIKernelPort

object Dewarp360Port : CIKernelPort {
    override val name: String = "dewarp360"
    override val isWarp: Boolean = true
    override val glsl: String = """
vec2 dewarp360(
    vec2 destCoord,
    float inputWidth,
    float inputHeight,
    float outputWidth,
    float outputHeight,
    float fieldOfViewWidth,
    float fieldOfViewHeight,
    vec3 rotationRow1,
    vec3 rotationRow2,
    vec3 rotationRow3
) {
    const float dewarpPi = 3.14159265358979323846;
    float x = 1.0;
    float y = (destCoord.x / outputWidth - 0.5) * fieldOfViewWidth;
    float z = (destCoord.y / outputHeight - 0.5) * fieldOfViewHeight;
    float r = sqrt(x * x + y * y + z * z);
    x /= r;
    y /= r;
    z /= r;
    float x1 = rotationRow1.x * x + rotationRow1.y * y + rotationRow1.z * z;
    float y1 = rotationRow2.x * x + rotationRow2.y * y + rotationRow2.z * z;
    float z1 = rotationRow3.x * x + rotationRow3.y * y + rotationRow3.z * z;
    float inputX = (atan(y1, x1) / dewarpPi / 2.0 + 0.5) * inputWidth;
    float inputY = (asin(clamp(z1, -1.0, 1.0)) / dewarpPi + 0.5) * inputHeight;
    return vec2(inputX, inputY);
}
"""
}
