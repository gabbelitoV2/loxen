package com.moblin.android.intents

import com.moblin.android.various.model.Model

class UnmuteIntent(private val model: Model) {
    suspend fun perform() {
        TODO("no Android counterpart for AppIntents")
    }

    companion object {
        val title: String = "Unmute"
        val description: String? = "Unmutes audio."
        val openAppWhenRun: Boolean = false
    }
}
