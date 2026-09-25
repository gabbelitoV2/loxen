package com.moblin.android.platform.activitykit

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import com.moblin.android.AppDelegate
import com.moblin.android.moblinliveactivity.shared.LiveActionFunction
import com.moblin.android.moblinliveactivity.shared.LiveActivityAttributes
import com.moblin.android.platform.host.StreamingService
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
class ActivityKitSuite {
    private val live = LiveActionFunction(image = "livephoto", text = "Live")
    private val recording = LiveActionFunction(image = "record.circle", text = "Recording")
    private lateinit var manager: NotificationManager
    private lateinit var notifications: ShadowNotificationManager
    private var service: ServiceController<StreamingService>? = null

    @Before
    fun setUp() {
        manager = AppDelegate.context.getSystemService(NotificationManager::class.java)
        notifications = shadowOf(manager)
        notifications.setNotificationsEnabled(true)
        ActivityCenter.sdkInt = Build.VERSION.SDK_INT
    }

    @After
    fun tearDown() {
        runBlocking {
            for (activity in Activity.activities<LiveActivityAttributes>()) {
                activity.end(null, dismissalPolicy = ActivityUIDismissalPolicy.immediate)
            }
        }
        service?.destroy()
        service = null
        ActivityCenter.sdkInt = Build.VERSION.SDK_INT
    }

    private fun content(
        functions: List<LiveActionFunction>,
        showEllipsis: Boolean = false,
    ): ActivityContent<LiveActivityAttributes.ContentState> {
        return ActivityContent(
            state = LiveActivityAttributes.ContentState(functions = functions, showEllipsis = showEllipsis),
            staleDate = null,
        )
    }

    private fun request(functions: List<LiveActionFunction>): Activity<LiveActivityAttributes> {
        return Activity.request(attributes = LiveActivityAttributes(), content = content(functions))
    }

    private fun posted(): Notification? = notifications.getNotification(StreamingService.NOTIFICATION_ID)

    private fun Notification.title() = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

    private fun Notification.text() = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

    private fun Notification.bigText() = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

    private fun Notification.isOngoing() = (flags and Notification.FLAG_ONGOING_EVENT) != 0

    private fun startService(): StreamingService {
        val intent = Intent(AppDelegate.context, StreamingService::class.java)
        val controller = Robolectric.buildService(StreamingService::class.java, intent).create().startCommand(0, 1)
        service = controller
        return controller.get()
    }

    @Test
    fun requestPostsTheLiveActivityAsAnOngoingNotification() {
        val activity = request(listOf(live))
        val notification = assertNotNull(posted())
        assertEquals("Moblin is running in background", notification.title())
        assertEquals("Live", notification.text())
        assertEquals("Live", notification.bigText())
        assertTrue(notification.isOngoing())
        assertEquals(StreamingService.CHANNEL_ID, notification.channelId)
        assertEquals(Notification.VISIBILITY_PUBLIC, notification.visibility)
        assertEquals(Icon.TYPE_BITMAP, notification.smallIcon.type)
        assertNotNull(notification.getLargeIcon())
        assertNotNull(notification.contentIntent)
        assertEquals(ActivityState.active, activity.activityState)
        assertEquals(listOf(activity), Activity.activities<LiveActivityAttributes>())
    }

    @Test
    fun updateShowsOneRowPerFunctionAndTheEllipsis() {
        val activity = request(listOf(live))
        runBlocking {
            activity.update(content(listOf(live, recording), showEllipsis = true))
        }
        val notification = assertNotNull(posted())
        assertEquals("Live, Recording, ...", notification.text())
        assertEquals("Live\nRecording\n...", notification.bigText())
        assertTrue(notification.isOngoing())
        assertEquals(listOf(live, recording), (activity.content.state as LiveActivityAttributes.ContentState).functions)
    }

    @Test
    fun withoutFunctionsTheNotificationStillExplainsThatMoblinRuns() {
        request(emptyList())
        val notification = assertNotNull(posted())
        assertEquals("Moblin is running in background", notification.title())
        assertEquals("", notification.text().orEmpty())
        assertEquals(Icon.TYPE_RESOURCE, notification.smallIcon.type)
    }

    @Test
    fun endImmediatelyRemovesTheNotificationAndTheActivity() {
        val activity = request(listOf(live))
        runBlocking {
            activity.end(null, dismissalPolicy = ActivityUIDismissalPolicy.immediate)
        }
        assertNull(posted())
        assertEquals(ActivityState.dismissed, activity.activityState)
        assertTrue(Activity.activities<LiveActivityAttributes>().isEmpty())
    }

    @Test
    fun endWithTheDefaultPolicyKeepsTheFinalContentDismissibleForFourHours() {
        val activity = request(listOf(live))
        runBlocking {
            activity.end(content(listOf(recording)), dismissalPolicy = ActivityUIDismissalPolicy.default)
            activity.update(content(listOf(live)))
        }
        val notification = assertNotNull(posted())
        assertFalse(notification.isOngoing())
        assertEquals("Recording", notification.text())
        val timeout = Duration.ofMillis(notification.timeoutAfter)
        assertTrue(timeout <= Duration.ofHours(4) && timeout > Duration.ofHours(4).minusMinutes(1))
        assertEquals(ActivityState.ended, activity.activityState)
        assertEquals(listOf(activity), Activity.activities<LiveActivityAttributes>())
    }

    @Test
    fun endAfterADateDismissesAtThatDate() {
        val activity = request(listOf(live))
        runBlocking {
            activity.end(null, dismissalPolicy = ActivityUIDismissalPolicy.after(Instant.now().plusSeconds(600)))
        }
        val notification = assertNotNull(posted())
        assertFalse(notification.isOngoing())
        val timeout = Duration.ofMillis(notification.timeoutAfter)
        assertTrue(timeout <= Duration.ofMinutes(10) && timeout > Duration.ofMinutes(9))
    }

    @Test
    fun disabledNotificationsDisableLiveActivities() {
        notifications.setNotificationsEnabled(false)
        assertFalse(ActivityAuthorizationInfo().areActivitiesEnabled)
        val error = assertFailsWith<ActivityAuthorizationError> { request(listOf(live)) }
        assertEquals("denied", error.reason)
        assertNull(posted())
        assertTrue(Activity.activities<LiveActivityAttributes>().isEmpty())
    }

    @Test
    fun aBlockedChannelDisablesLiveActivities() {
        assertTrue(ActivityAuthorizationInfo().areActivitiesEnabled)
        manager.createNotificationChannel(
            NotificationChannel(StreamingService.CHANNEL_ID, "Streaming", NotificationManager.IMPORTANCE_NONE),
        )
        assertFalse(ActivityAuthorizationInfo().areActivitiesEnabled)
        manager.deleteNotificationChannel(StreamingService.CHANNEL_ID)
    }

    @Test
    fun onAndroid16ThePromotedLiveUpdateIsRequestedWithTheFirstFunctionAsChipText() {
        ActivityCenter.sdkInt = 36
        request(listOf(recording, live))
        val notification = assertNotNull(posted())
        assertTrue(notification.extras.getBoolean("android.requestPromotedOngoing"))
        assertEquals("Recording", notification.extras.getString("android.shortCriticalText"))
        assertTrue(notification.isOngoing())
        assertFalse(notification.title().isNullOrEmpty())
        assertEquals(Notification.BigTextStyle::class.java.name, notification.extras.getString(Notification.EXTRA_TEMPLATE))
        assertFalse(notification.extras.getBoolean(Notification.EXTRA_COLORIZED))
        assertEquals(0, notification.flags and Notification.FLAG_GROUP_SUMMARY)
        assertNull(notification.contentView)
        assertNull(notification.bigContentView)
        val channel = assertNotNull(manager.getNotificationChannel(StreamingService.CHANNEL_ID))
        assertTrue(channel.importance > NotificationManager.IMPORTANCE_MIN)
    }

    @Test
    fun beforeAndroid16NoPromotionIsRequested() {
        request(listOf(live))
        val notification = assertNotNull(posted())
        assertFalse(notification.extras.containsKey("android.requestPromotedOngoing"))
        assertFalse(notification.extras.containsKey("android.shortCriticalText"))
    }

    @Test
    fun theForegroundServiceNotificationBecomesTheLiveActivityAndComesBack() {
        val service = startService()
        assertEquals("Live or recording", assertNotNull(posted()).text())
        val activity = request(listOf(live))
        assertEquals("Moblin is running in background", assertNotNull(posted()).title())
        runBlocking {
            activity.end(null, dismissalPolicy = ActivityUIDismissalPolicy.immediate)
        }
        val restored = assertNotNull(posted())
        assertEquals("Live or recording", restored.text())
        assertTrue(restored.isOngoing())
        assertFalse(shadowOf(service).isForegroundStopped)
    }

    @Test
    fun theServiceStartsWithTheLiveActivityWhenOneIsShown() {
        request(listOf(recording))
        val service = startService()
        val notification = assertNotNull(shadowOf(service).lastForegroundNotification)
        assertEquals("Moblin is running in background", notification.title())
        assertEquals("Recording", notification.text())
        assertEquals(StreamingService.NOTIFICATION_ID, shadowOf(service).lastForegroundNotificationId)
    }

    @Test
    fun stoppingTheServiceKeepsTheLiveActivityUntilItEnds() {
        val service = startService()
        val activity = request(listOf(live))
        StreamingService.stop(AppDelegate.context)
        assertTrue(shadowOf(service).isForegroundStopped)
        assertFalse(shadowOf(service).notificationShouldRemoved)
        assertFalse(shadowOf(service).isLastForegroundNotificationAttached)
        assertEquals("Live", assertNotNull(posted()).text())
        runBlocking {
            activity.update(content(emptyList()))
        }
        assertEquals("Moblin is running in background", assertNotNull(posted()).title())
        runBlocking {
            activity.end(null, dismissalPolicy = ActivityUIDismissalPolicy.immediate)
        }
        assertNull(posted())
    }

    @Test
    fun aLiveActivityNotificationLeftByAKilledProcessIsRemovedAtStart() {
        val stale = Notification.Builder(AppDelegate.context, StreamingService.CHANNEL_ID)
            .setContentTitle("Moblin is running in background")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setOngoing(true)
            .build()
        manager.notify(StreamingService.NOTIFICATION_ID, stale)
        StreamingService.cancelStaleNotification(AppDelegate.context)
        assertNull(posted())
        request(listOf(live))
        StreamingService.cancelStaleNotification(AppDelegate.context)
        assertEquals("Live", assertNotNull(posted()).text())
    }

    @Test
    fun theServiceNotificationIsNotStaleWhileTheServiceRuns() {
        startService()
        StreamingService.cancelStaleNotification(AppDelegate.context)
        assertEquals("Live or recording", assertNotNull(posted()).text())
    }

    @Test
    fun afterTheServiceStopsEndingTheLiveActivityDoesNotBringBackTheServiceNotification() {
        val service = startService()
        StreamingService.stop(AppDelegate.context)
        assertFalse(shadowOf(service).isForegroundStopped)
        val activity = request(listOf(live))
        assertSame(activity, Activity.activities<LiveActivityAttributes>().single())
        runBlocking {
            activity.end(null, dismissalPolicy = ActivityUIDismissalPolicy.immediate)
        }
        assertNull(posted())
    }
}
