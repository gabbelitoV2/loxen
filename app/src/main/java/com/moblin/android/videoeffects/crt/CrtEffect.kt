package com.moblin.android.videoeffects.crt

import android.graphics.RectF
import android.media.Image
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo

class CrtEffect : VideoEffect() {
    private val barrelFilter: CrtBarrelDistortionFilter = TODO("OpenGL ES port")
    private val colorControls: Any = TODO("no Android counterpart for CIFilter.colorControls")
    private val vignette: Any = TODO("no Android counterpart for CIFilter.vignette")
    private val crtFilter: Any = TODO("no Android counterpart for MTICrtFilter")

    override fun execute(image: Image, info: VideoEffectInfo): Image =
        TODO("OpenGL ES port")

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image =
        TODO("OpenGL ES port")

    private fun applyBarrelDistortion(image: Image, width: Float): Image =
        TODO("OpenGL ES port")

    private fun applyColors(image: Image): Image =
        TODO("OpenGL ES port")

    private fun applyScanlines(image: Image, cropRect: RectF): Image =
        TODO("OpenGL ES port")
}
