package com.moblin.android.platform.coreimage.kernels

import com.moblin.android.platform.coreimage.CIKernelPort

object CrtBarrelDistortionPort : CIKernelPort {
    override val name: String = "crtBarrelDistortion"
    override val isWarp: Boolean = true
    override val glsl: String = """
vec2 crtBarrelDistortion(vec2 destCoord, float inputWidth, float inputHeight, float strength) {
    vec2 center = vec2(inputWidth / 2.0, inputHeight / 2.0);
    float scale = min(center.x, center.y);
    vec2 coord = (destCoord - center) / scale;
    float r2 = dot(coord, coord);
    float distortion = 1.0 + strength * r2;
    vec2 distorted = coord * distortion;
    return distorted * scale + center;
}
"""
}
