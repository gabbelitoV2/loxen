package com.moblin.android.various.model

fun Model.updateShowTaps() {
    showTaps.update(enabled = database.debug.showTaps.value)
}
