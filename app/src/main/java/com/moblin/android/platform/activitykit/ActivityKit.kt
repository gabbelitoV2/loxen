package com.moblin.android.platform.activitykit

import android.graphics.Bitmap
import com.moblin.android.AppDelegate
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClass

interface ActivityAttributes

class ActivityContent<State : Any>(
    val state: State,
    val staleDate: Instant?,
    val relevanceScore: Double = 0.0,
)

enum class ActivityState {
    active,
    ended,
    dismissed,
    stale,
    pending,
}

sealed class ActivityUIDismissalPolicy {
    data object default : ActivityUIDismissalPolicy()

    data object immediate : ActivityUIDismissalPolicy()

    data class after(val date: Instant) : ActivityUIDismissalPolicy()
}

class ActivityAuthorizationError private constructor(val reason: String) : Exception(reason) {
    companion object {
        val denied: ActivityAuthorizationError
            get() = ActivityAuthorizationError("denied")
        val unsupported: ActivityAuthorizationError
            get() = ActivityAuthorizationError("unsupported")
    }
}

class ActivityAuthorizationInfo {
    val areActivitiesEnabled: Boolean
        get() = ActivityCenter.areActivitiesEnabled(AppDelegate.context)
}

class Activity<Attributes : ActivityAttributes> internal constructor(
    val attributes: Attributes,
    content: ActivityContent<*>,
) {
    val id: String = UUID.randomUUID().toString()

    @Volatile
    var content: ActivityContent<*> = content
        internal set

    @Volatile
    var activityState: ActivityState = ActivityState.active
        internal set

    @Volatile
    internal var dismissalDate: Instant? = null

    suspend fun update(content: ActivityContent<*>) {
        ActivityCenter.update(this, content)
    }

    suspend fun end(
        content: ActivityContent<*>?,
        dismissalPolicy: ActivityUIDismissalPolicy = ActivityUIDismissalPolicy.default,
    ) {
        ActivityCenter.end(this, content, dismissalPolicy)
    }

    companion object {
        fun <A : ActivityAttributes> request(attributes: A, content: ActivityContent<*>): Activity<A> {
            return ActivityCenter.request(attributes, content)
        }

        inline fun <reified A : ActivityAttributes> activities(): List<Activity<A>> {
            return activities(A::class)
        }

        fun <A : ActivityAttributes> activities(type: KClass<A>): List<Activity<A>> {
            return ActivityCenter.activities(type)
        }
    }
}

class ActivityViewContext<Attributes : ActivityAttributes>(
    val state: Any,
    val attributes: Attributes,
    val activityID: String,
    val isStale: Boolean,
)

class ActivityNotificationRow(val systemImage: String?, val text: String)

class ActivityNotificationView(
    val icon: Bitmap?,
    val title: String,
    val rows: List<ActivityNotificationRow>,
    val shortCriticalText: String?,
)

class ActivityConfiguration<Attributes : ActivityAttributes>(
    val attributesType: KClass<Attributes>,
    val content: (ActivityViewContext<Attributes>) -> ActivityNotificationView,
)
