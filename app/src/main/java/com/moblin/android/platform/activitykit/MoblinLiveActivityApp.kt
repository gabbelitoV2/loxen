package com.moblin.android.platform.activitykit

import com.moblin.android.localized
import com.moblin.android.moblinliveactivity.shared.LiveActivityAttributes
import com.moblin.android.platform.Bundle

internal object MoblinLiveActivityApp {
    private val moblinLiveActivityIcon by lazy { Bundle.image("AppIcon") }

    val body = ActivityConfiguration(LiveActivityAttributes::class) { context ->
        val state = context.state as LiveActivityAttributes.ContentState
        ActivityNotificationView(
            icon = moblinLiveActivityIcon,
            title = localized("Moblin is running in background"),
            rows = moblinLiveActivityStatusLabel(state),
            shortCriticalText = state.functions.firstOrNull()?.text,
        )
    }

    private fun moblinLiveActivityStatusLabel(
        state: LiveActivityAttributes.ContentState,
    ): List<ActivityNotificationRow> {
        val rows = state.functions.map { ActivityNotificationRow(systemImage = it.image, text = it.text) }
        if (!state.showEllipsis) {
            return rows
        }
        return rows + ActivityNotificationRow(systemImage = null, text = "...")
    }
}

internal val activityConfigurations: List<ActivityConfiguration<*>> by lazy {
    listOf(MoblinLiveActivityApp.body)
}
