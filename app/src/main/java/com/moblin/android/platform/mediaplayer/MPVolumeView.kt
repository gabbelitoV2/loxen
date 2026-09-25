package com.moblin.android.platform.mediaplayer

import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.uikit.UISlider

class MPVolumeView(val frame: CGRect = CGRect.zero) {
    val subviews: List<Any> = listOf(MPVolumeSlider())

    init {
        SystemVolume.startObserving()
    }
}

private class MPVolumeSlider : UISlider() {
    override var value: Float
        get() = SystemVolume.volume
        set(newValue) {
            SystemVolume.volume = clamp(newValue)
        }
}
