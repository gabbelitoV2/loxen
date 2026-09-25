package com.moblin.android.platform.activitykit

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import com.moblin.android.AppDelegate
import com.moblin.android.platform.host.StreamingService
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KClass

internal object ActivityCenter {
    private const val TAG = "ActivityKit"
    private const val PROMOTED_ONGOING_SDK = 36
    private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
    private const val EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText"
    private const val SMALL_ICON_SIZE = 96
    private const val LARGE_ICON_SIZE = 192
    private val endedDisplayDuration = Duration.ofHours(4)
    private val lock = Any()
    private val activities = mutableListOf<Activity<*>>()
    private val smallIcons = mutableMapOf<String, Icon>()
    private var largeIconSource: Bitmap? = null
    private var largeIcon: Icon? = null
    internal var sdkInt = Build.VERSION.SDK_INT

    fun areActivitiesEnabled(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        if (!manager.areNotificationsEnabled()) {
            return false
        }
        val channel = manager.getNotificationChannel(StreamingService.CHANNEL_ID) ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun <A : ActivityAttributes> request(attributes: A, content: ActivityContent<*>): Activity<A> {
        val context = AppDelegate.context
        if (!areActivitiesEnabled(context)) {
            throw ActivityAuthorizationError.denied
        }
        if (configuration(attributes) == null) {
            throw ActivityAuthorizationError.unsupported
        }
        val activity = Activity(attributes, content)
        synchronized(lock) {
            activities.add(activity)
            show(context)
        }
        logPromotion(context)
        return activity
    }

    fun update(activity: Activity<*>, content: ActivityContent<*>) {
        synchronized(lock) {
            if (activity.activityState != ActivityState.active || activity !in activities) {
                return
            }
            activity.content = content
            show(AppDelegate.context)
        }
    }

    fun end(activity: Activity<*>, content: ActivityContent<*>?, dismissalPolicy: ActivityUIDismissalPolicy) {
        synchronized(lock) {
            if (activity !in activities) {
                return
            }
            if (content != null) {
                activity.content = content
            }
            val now = Instant.now()
            val latest = now.plus(endedDisplayDuration)
            activity.activityState = ActivityState.ended
            activity.dismissalDate = when (dismissalPolicy) {
                ActivityUIDismissalPolicy.default -> latest
                ActivityUIDismissalPolicy.immediate -> now
                is ActivityUIDismissalPolicy.after -> minOf(maxOf(dismissalPolicy.date, now), latest)
            }
            show(AppDelegate.context)
        }
    }

    fun <A : ActivityAttributes> activities(type: KClass<A>): List<Activity<A>> {
        synchronized(lock) {
            removeDismissed(Instant.now())
            @Suppress("UNCHECKED_CAST")
            return activities.filter { type.isInstance(it.attributes) } as List<Activity<A>>
        }
    }

    private fun removeDismissed(now: Instant) {
        val dismissed = activities.filter { activity ->
            activity.dismissalDate?.let { !it.isAfter(now) } ?: false
        }
        for (activity in dismissed) {
            activity.activityState = ActivityState.dismissed
        }
        activities.removeAll(dismissed)
    }

    private fun show(context: Context) {
        val now = Instant.now()
        removeDismissed(now)
        val activity = activities.lastOrNull { it.activityState == ActivityState.active }
            ?: activities.lastOrNull()
        val notification = activity?.let { makeNotification(context, it, now) }
        StreamingService.showActivityNotification(context, notification)
    }

    private fun configuration(attributes: ActivityAttributes): ActivityConfiguration<*>? {
        return activityConfigurations.firstOrNull { it.attributesType.isInstance(attributes) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun render(activity: Activity<*>, now: Instant): ActivityNotificationView? {
        val configuration = configuration(activity.attributes) as? ActivityConfiguration<ActivityAttributes>
            ?: return null
        val content = activity.content
        val context = ActivityViewContext(
            state = content.state,
            attributes = activity.attributes,
            activityID = activity.id,
            isStale = content.staleDate?.let { !it.isAfter(now) } ?: false,
        )
        return configuration.content(context)
    }

    private fun makeNotification(context: Context, activity: Activity<*>, now: Instant): Notification? {
        val view = render(activity, now) ?: return null
        val ongoing = activity.activityState == ActivityState.active
        val texts = view.rows.map { it.text }
        StreamingService.createNotificationChannel(context)
        val builder = Notification.Builder(context, StreamingService.CHANNEL_ID)
            .setContentTitle(view.title)
            .setContentText(texts.joinToString(", "))
            .setStyle(Notification.BigTextStyle().bigText(texts.joinToString("\n")))
            .setSmallIcon(smallIcon(context, view.rows.firstNotNullOfOrNull { it.systemImage }))
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
        view.icon?.let { builder.setLargeIcon(largeIcon(it)) }
        StreamingService.launchIntent(context)?.let { builder.setContentIntent(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        if (ongoing) {
            if (sdkInt >= PROMOTED_ONGOING_SDK) {
                requestPromotion(builder, view.shortCriticalText)
            }
        } else {
            activity.dismissalDate?.let {
                builder.setTimeoutAfter(Duration.between(now, it).toMillis().coerceAtLeast(1))
            }
        }
        return builder.build()
    }

    private fun requestPromotion(builder: Notification.Builder, shortCriticalText: String?) {
        if (!invoke(builder, "setRequestPromotedOngoing", Boolean::class.javaPrimitiveType!!, true)) {
            builder.extras.putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
        }
        if (shortCriticalText != null &&
            !invoke(builder, "setShortCriticalText", String::class.java, shortCriticalText)
        ) {
            builder.extras.putString(EXTRA_SHORT_CRITICAL_TEXT, shortCriticalText)
        }
    }

    private fun invoke(target: Any, name: String, type: Class<*>, value: Any): Boolean {
        return try {
            target.javaClass.getMethod(name, type).invoke(target, value)
            true
        } catch (_: ReflectiveOperationException) {
            false
        }
    }

    private fun logPromotion(context: Context) {
        if (sdkInt < PROMOTED_ONGOING_SDK) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val allowed = try {
            manager.javaClass.getMethod("canPostPromotedNotifications").invoke(manager)
        } catch (_: ReflectiveOperationException) {
            null
        }
        Log.i(TAG, "Live Activity notification requests promotion, promoted notifications allowed: $allowed")
    }

    private fun largeIcon(bitmap: Bitmap): Icon {
        largeIconSource?.takeIf { it === bitmap }?.let { return largeIcon!! }
        val scale = LARGE_ICON_SIZE.toFloat() / maxOf(bitmap.width, bitmap.height)
        val scaled = if (scale < 1) {
            Bitmap.createScaledBitmap(
                bitmap,
                maxOf(1, (bitmap.width * scale).toInt()),
                maxOf(1, (bitmap.height * scale).toInt()),
                true,
            )
        } else {
            bitmap
        }
        val icon = Icon.createWithBitmap(scaled)
        largeIconSource = bitmap
        largeIcon = icon
        return icon
    }

    private fun smallIcon(context: Context, name: String?): Icon {
        if (name == null) {
            return Icon.createWithResource(context, android.R.drawable.presence_video_online)
        }
        return smallIcons.getOrPut(name) { Icon.createWithBitmap(rasterize(name)) }
    }

    private fun rasterize(name: String): Bitmap {
        val vector = com.moblin.android.platform.systemImage(name)
        val bitmap = Bitmap.createBitmap(SMALL_ICON_SIZE, SMALL_ICON_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(SMALL_ICON_SIZE / vector.viewportWidth, SMALL_ICON_SIZE / vector.viewportHeight)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        draw(vector.root, canvas, paint)
        return bitmap
    }

    private fun draw(group: VectorGroup, canvas: Canvas, paint: Paint) {
        canvas.save()
        canvas.translate(group.translationX + group.pivotX, group.translationY + group.pivotY)
        canvas.rotate(group.rotation)
        canvas.scale(group.scaleX, group.scaleY)
        canvas.translate(-group.pivotX, -group.pivotY)
        for (node in group) {
            when (node) {
                is VectorGroup -> draw(node, canvas, paint)
                is VectorPath -> {
                    val path = node.pathData.toPath().asAndroidPath()
                    path.fillType = if (node.pathFillType == PathFillType.EvenOdd) {
                        Path.FillType.EVEN_ODD
                    } else {
                        Path.FillType.WINDING
                    }
                    canvas.drawPath(path, paint)
                }
            }
        }
        canvas.restore()
    }
}
