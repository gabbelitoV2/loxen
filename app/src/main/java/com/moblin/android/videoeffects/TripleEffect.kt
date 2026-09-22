package com.moblin.android.videoeffects

import android.graphics.Bitmap
import android.graphics.Rect
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo

class TripleEffect : VideoEffect() {
    override fun execute(image: Bitmap, info: VideoEffectInfo): Bitmap {
        val width = image.width / 3
        val height = image.height
        val centerRegion = Rect(width, 0, width * 2, height)
        val leftRegion = Rect(0, 0, width, height)
        val rightRegion = Rect(width * 2, 0, width * 3, height)
        return TODO(
            "OpenGL ES port: source-over composite center=$centerRegion, left=$leftRegion, right=$rightRegion"
        )
    }

    override fun executeMetalPetal(image: Bitmap, info: VideoEffectInfo): Bitmap {
        val width = image.width / 3
        val height = image.height
        val centerRegion = Rect(width, 0, width * 2, height)
        return TODO(
            "OpenGL ES port: multilayer composite three layers of $centerRegion, width=$width, height=$height"
        )
    }
}
