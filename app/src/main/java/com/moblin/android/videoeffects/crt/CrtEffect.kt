package com.moblin.android.videoeffects.crt

import android.graphics.RectF
import android.media.Image
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo

class CrtEffect : VideoEffect() {
    private val barrelFilter: CrtBarrelDistortionFilter? = null
    private val colorControls: Any? = null
    private val vignette: Any? = null
    private val crtFilter: Any? = null

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
