package com.moblin.android.intents

import com.moblin.android.localized
import com.moblin.android.various.model.Model

class SnapshotIntent(private val model: Model) {
    companion object {
        val title: String = localized("Take snapshot")

        val description: String? = localized("Take a snapshot.")

        val openAppWhenRun: Boolean = false

        fun result(): Unit = Unit
    }

    suspend fun perform() {
        Unit
    }
}
