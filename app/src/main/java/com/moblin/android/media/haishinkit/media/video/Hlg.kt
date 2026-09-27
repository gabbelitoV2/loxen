package com.moblin.android.media.haishinkit.media.video

import com.moblin.android.platform.Bundle
import com.moblin.android.platform.coreimage.CIColorKernel
import com.moblin.android.platform.coreimage.CIImage

private class HlgKernels(val toLinear: CIColorKernel, val toHlg: CIColorKernel)

private val hlgKernels: HlgKernels? by lazy {
    val data = Bundle.readBytes("default", "metallib") ?: return@lazy null
    val toLinear = runCatching {
        CIColorKernel(functionName = "hlgToLinear", fromMetalLibraryData = data)
    }.getOrNull() ?: return@lazy null
    val toHlg = runCatching {
        CIColorKernel(functionName = "linearToHlg", fromMetalLibraryData = data)
    }.getOrNull() ?: return@lazy null
    HlgKernels(toLinear, toHlg)
}

fun CIImage.hlgToLinear(): CIImage =
    hlgKernels?.toLinear?.apply(extent = extent, arguments = listOf(this)) ?: this

fun CIImage.linearToHlg(): CIImage =
    hlgKernels?.toHlg?.apply(extent = extent, arguments = listOf(this)) ?: this
