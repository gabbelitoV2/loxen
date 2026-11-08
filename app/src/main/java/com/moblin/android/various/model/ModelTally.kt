package com.moblin.android.various.model

import com.moblin.android.common.various.RgbColor
import kotlinx.coroutines.flow.MutableStateFlow

data class TallyState(
    val message: String,
    val background: RgbColor,
    val foreground: RgbColor,
)

open class Tally {
    val state = MutableStateFlow<TallyState?>(null)

    open fun show(message: String, background: RgbColor, foreground: RgbColor) {
        state.value = TallyState(message = message, background = background, foreground = foreground)
    }

    open fun hide() {
        state.value = null
    }
}
