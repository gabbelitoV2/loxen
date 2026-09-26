package com.moblin.android.intents

import com.moblin.android.platform.appintents.AppIntent
import com.moblin.android.platform.appintents.Dependency
import com.moblin.android.platform.appintents.IntentDescription
import com.moblin.android.platform.appintents.IntentResult
import com.moblin.android.platform.appintents.LocalizedStringResource
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.takeSnapshot

class SnapshotIntent : AppIntent {
    companion object {
        val title: LocalizedStringResource = "Take snapshot"
        val description: IntentDescription? = IntentDescription("Take a snapshot.")
        val openAppWhenRun: Boolean = false
    }

    override suspend fun perform(): IntentResult {
        model.takeSnapshot()
        return IntentResult.result()
    }

    private val model: Model by Dependency<Model>()
}
