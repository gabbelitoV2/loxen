package com.moblin.android.platform.coreimage.kernels

import com.moblin.android.platform.coreimage.CIKernelPort

object CrtBarrelDistortionPort : CIKernelPort {
    override val name: String = "crtBarrelDistortion"
    override val isWarp: Boolean = true
    override val glsl: String? = null
}
