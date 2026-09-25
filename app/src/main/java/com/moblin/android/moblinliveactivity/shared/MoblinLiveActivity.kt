package com.moblin.android.moblinliveactivity.shared

import com.moblin.android.platform.activitykit.ActivityAttributes
import kotlinx.serialization.Serializable

@Serializable
data class LiveActionFunction(
    val image: String,
    val text: String,
)

class LiveActivityAttributes : ActivityAttributes {
    @Serializable
    data class ContentState(
        val functions: List<LiveActionFunction>,
        val showEllipsis: Boolean,
    )
}
