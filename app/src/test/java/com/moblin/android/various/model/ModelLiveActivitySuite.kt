package com.moblin.android.various.model

import android.app.Notification
import android.app.NotificationManager
import android.os.Looper
import com.moblin.android.AppDelegate
import com.moblin.android.moblinliveactivity.shared.LiveActivityAttributes
import com.moblin.android.platform.activitykit.Activity
import com.moblin.android.platform.activitykit.ActivityUIDismissalPolicy
import com.moblin.android.platform.host.StreamingService
import com.moblin.android.platform.host.SystemEventsState
import com.moblin.android.various.Media
import com.moblin.android.various.MediaDelegate
import java.lang.reflect.Proxy
import kotlin.test.assertEquals
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
class ModelLiveActivitySuite {
    private lateinit var model: Model
    private lateinit var notifications: ShadowNotificationManager

    @Before
    fun setUp() {
        SystemEventsState.reset()
        notifications = shadowOf(AppDelegate.context.getSystemService(NotificationManager::class.java))
        notifications.setNotificationsEnabled(true)
        model = Model()
        model.media = Media(delegate = mediaDelegate())
        runMain()
    }

    @After
    fun tearDown() {
        runBlocking {
            for (activity in Activity.activities<LiveActivityAttributes>()) {
                activity.end(null, dismissalPolicy = ActivityUIDismissalPolicy.immediate)
            }
        }
        SystemEventsState.reset()
    }

    private fun mediaDelegate(): MediaDelegate {
        return Proxy.newProxyInstance(MediaDelegate::class.java.classLoader, arrayOf(MediaDelegate::class.java)) { _, method, _ ->
            when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Double.TYPE -> 0.0
                java.lang.Float.TYPE -> 0f
                else -> null
            }
        } as MediaDelegate
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun state(): LiveActivityAttributes.ContentState {
        val activity = assertNotNull(model.liveActivity)
        return activity.content.state as LiveActivityAttributes.ContentState
    }

    private fun images() = state().functions.map { it.image }

    private fun posted(): Notification? = notifications.getNotification(StreamingService.NOTIFICATION_ID)

    private fun enableAllFunctions() {
        model.isLive.value = true
        model.isRecording.value = true
        model.database.chat.background = true
        model.database.moblink.relay.enabled.value = true
        model.database.catPrinters.backgroundPrinting.value = true
    }

    @Test
    fun startShowsTheLiveFunctionInTheNotification() {
        model.isLive.value = true
        model.startLiveActivity()
        assertEquals(listOf("livephoto"), images())
        assertEquals(listOf("Live"), state().functions.map { it.text })
        assertFalse(state().showEllipsis)
        val notification = assertNotNull(posted())
        assertEquals(
            "Moblin is running in background",
            notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
        )
        assertEquals("Live", notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
    }

    @Test
    fun functionsFollowTheSwiftOrder() {
        enableAllFunctions()
        model.isLive.value = false
        model.isRecording.value = false
        model.database.chat.background = false
        model.startLiveActivity()
        assertEquals(listOf("app.connected.to.app.below.fill", "pawprint"), images())
        assertEquals(listOf("Moblink relay", "Background printing"), state().functions.map { it.text })
    }

    @Test
    fun threeFunctionsAreShownWithoutEllipsis() {
        model.isLive.value = true
        model.isRecording.value = true
        model.database.chat.background = true
        model.startLiveActivity()
        assertEquals(listOf("livephoto", "record.circle", "bubble.left"), images())
        assertEquals(listOf("Live", "Recording", "Background chat"), state().functions.map { it.text })
        assertFalse(state().showEllipsis)
        val notification = assertNotNull(posted())
        assertEquals(
            "Live\nRecording\nBackground chat",
            notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
        )
    }

    @Test
    fun fourFunctionsShowTheFirstTwoAndAnEllipsis() {
        enableAllFunctions()
        model.isLive.value = false
        model.startLiveActivity()
        assertEquals(listOf("record.circle", "bubble.left"), images())
        assertTrue(state().showEllipsis)
    }

    @Test
    fun fiveFunctionsShowTheFirstTwoAndAnEllipsis() {
        enableAllFunctions()
        model.startLiveActivity()
        assertEquals(listOf("livephoto", "record.circle"), images())
        assertTrue(state().showEllipsis)
        val notification = assertNotNull(posted())
        assertEquals("Live\nRecording\n...", notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString())
        assertEquals("Live, Recording, ...", notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
    }

    @Test
    fun startingTwiceKeepsOneLiveActivity() {
        model.startLiveActivity()
        val activity = assertNotNull(model.liveActivity)
        model.startLiveActivity()
        assertSame(activity, model.liveActivity)
        assertSame(activity, Activity.activities<LiveActivityAttributes>().single())
    }

    @Test
    fun disabledNotificationsStartNoLiveActivity() {
        notifications.setNotificationsEnabled(false)
        model.isLive.value = true
        model.startLiveActivity()
        assertNull(model.liveActivity)
        assertNull(posted())
        assertTrue(Activity.activities<LiveActivityAttributes>().isEmpty())
    }

    @Test
    fun goingLiveAndRecordingUpdateTheLiveActivity() {
        model.startLiveActivity()
        assertEquals(emptyList(), images())
        model.setIsLive(value = true)
        runMain()
        assertEquals(listOf("livephoto"), images())
        model.setIsRecording(value = true)
        runMain()
        assertEquals(listOf("livephoto", "record.circle"), images())
        model.setIsLive(value = false)
        runMain()
        assertEquals(listOf("record.circle"), images())
        assertEquals("Recording", posted()?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString())
    }

    @Test
    fun stopEndsTheLiveActivity() {
        model.isLive.value = true
        model.startLiveActivity()
        model.stopLiveActivity()
        assertNull(model.liveActivity)
        assertNull(posted())
        assertTrue(Activity.activities<LiveActivityAttributes>().isEmpty())
        model.updateLiveActivity()
        runMain()
        assertNull(posted())
    }

    @Test
    fun backgroundStreamingStartsTheLiveActivityInTheBackgroundAndForegroundStopsIt() {
        model.stream.value.backgroundStreaming = true
        model.isLive.value = true
        model.handleApplicationDidEnterBackground()
        runMain()
        assertEquals(listOf("livephoto"), images())
        model.handleApplicationWillEnterForeground()
        runMain()
        assertNull(model.liveActivity)
        assertNull(posted())
    }

    @Test
    fun backgroundChatStartsTheLiveActivityInTheBackground() {
        model.database.chat.background = true
        model.handleApplicationDidEnterBackground()
        runMain()
        assertEquals(listOf("bubble.left"), images())
        assertEquals(listOf("Background chat"), state().functions.map { it.text })
        model.handleApplicationWillEnterForeground()
        runMain()
        assertNull(model.liveActivity)
    }
}
