package com.moblin.android.platform.live2d

import com.moblin.android.platform.video.CVPixelBuffer

class Live2DRenderer private constructor() {
    fun render(model: AyagamiModel, into: CVPixelBuffer) {
        Live2DLog.notImplemented("Live2DRenderer.render")
    }

    companion object {
        operator fun invoke(model: AyagamiModel): Live2DRenderer? {
            Live2DLog.notImplemented("Live2DRenderer.init")
            return null
        }
    }
}
