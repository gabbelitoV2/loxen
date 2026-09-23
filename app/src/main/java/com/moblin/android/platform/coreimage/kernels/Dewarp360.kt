package com.moblin.android.platform.coreimage.kernels

import com.moblin.android.platform.coreimage.CIKernelPort

object Dewarp360Port : CIKernelPort {
    override val name: String = "dewarp360"
    override val isWarp: Boolean = true
    override val glsl: String? = null
}
