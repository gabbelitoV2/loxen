package com.moblin.android.various.model

import com.moblin.android.platform.log.Log
import com.moblin.android.localized
import com.moblin.android.platform.activitykit.Activity
import com.moblin.android.platform.activitykit.ActivityAuthorizationError
import com.moblin.android.platform.activitykit.ActivityAuthorizationInfo
import com.moblin.android.platform.activitykit.ActivityContent
import com.moblin.android.platform.activitykit.ActivityUIDismissalPolicy
import kotlinx.coroutines.runBlocking
import com.moblin.android.moblinliveactivity.shared.LiveActionFunction
import com.moblin.android.moblinliveactivity.shared.LiveActivityAttributes

fun Model.startLiveActivity() {
    if (!ActivityAuthorizationInfo().areActivitiesEnabled) {
        return
    }
    if (liveActivity != null) {
        return
    }
    try {
        liveActivity = Activity.request(
            attributes = LiveActivityAttributes(),
            content = ActivityContent(state = makeState(), staleDate = null)
        )
        Log.i("Model", "live-activity: Started")
    } catch (error: ActivityAuthorizationError) {
        Log.i("Model", "live-activity: Start failed with error: $error")
    }
}

fun Model.stopLiveActivity() {
    runBlocking {
        for (activity in Activity.activities<LiveActivityAttributes>()) {
            activity.end(null, dismissalPolicy = ActivityUIDismissalPolicy.immediate)
        }
    }
    liveActivity = null
}

fun Model.updateLiveActivity() {
    val state = makeState()
    runBlocking {
        liveActivity?.update(ActivityContent(state = state, staleDate = null))
    }
}

private fun Model.makeState(): LiveActivityAttributes.ContentState {
    val functions = mutableListOf<LiveActionFunction>()
    if (isLive.value) {
        functions.add(
            LiveActionFunction(
                image = "livephoto",
                text = localized("Live")
            )
        )
    }
    if (isRecording.value) {
        functions.add(
            LiveActionFunction(
                image = "record.circle",
                text = localized("Recording")
            )
        )
    }
    if (database.chat.background) {
        functions.add(
            LiveActionFunction(
                image = "bubble.left",
                text = localized("Background chat")
            )
        )
    }
    if (database.moblink.relay.enabled.value) {
        functions.add(
            LiveActionFunction(
                image = "app.connected.to.app.below.fill",
                text = localized("Moblink relay")
            )
        )
    }
    if (database.catPrinters.backgroundPrinting.value) {
        functions.add(
            LiveActionFunction(
                image = "pawprint",
                text = localized("Background printing")
            )
        )
    }
    return if (functions.size <= 3) {
        LiveActivityAttributes.ContentState(functions = functions, showEllipsis = false)
    } else {
        LiveActivityAttributes.ContentState(functions = functions.take(2), showEllipsis = true)
    }
}
