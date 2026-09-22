package com.moblin.android.videoeffects

import android.graphics.RectF
import android.media.Image
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo

class FourThreeEffect : VideoEffect() {
    override fun execute(image: Image, info: VideoEffectInfo): Image =
        TODO("OpenGL ES port")

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image =
        TODO("OpenGL ES port")

    fun cropRect(width: Float, height: Float): RectF =
        RectF(
            width / 8f,
            0f,
            width / 8f + 3f * width / 4f,
            height
        )

    fun sideBarRects(width: Float, height: Float): List<RectF> {
        val barWidth = width / 8f
        return listOf(
            RectF(
                0f,
                0f,
                barWidth,
                height
            ),
            RectF(
                width - barWidth,
                0f,
                width,
                height
            )
        )
    }
}
