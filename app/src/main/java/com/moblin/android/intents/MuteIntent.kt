package com.moblin.android.intents

import com.moblin.android.various.model.Model

class MuteIntent(private val model: Model) {

    companion object {
        const val title: String = "Mute"
        val description: String? = "Mutes audio."
        const val openAppWhenRun: Boolean = false
    }

    suspend fun perform() {
        Unit
    }
}
