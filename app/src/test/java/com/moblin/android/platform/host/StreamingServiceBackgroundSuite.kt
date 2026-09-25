package com.moblin.android.platform.host

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import com.moblin.android.moblinliveactivity.shared.LiveActionFunction
import com.moblin.android.moblinliveactivity.shared.LiveActivityAttributes
import com.moblin.android.platform.activitykit.Activity
import com.moblin.android.platform.activitykit.ActivityContent
import com.moblin.android.platform.activitykit.ActivityUIDismissalPolicy
import com.moblin.android.platform.capture.Camera2Engine
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
class StreamingServiceBackgroundSuite {
    private val connectedDevice = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
    private val mediaPlayback = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
    private val camera = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
    private val microphone = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
    private lateinit var application: Application
    private lateinit var notifications: ShadowNotificationManager
    private val services = mutableListOf<ServiceController<StreamingService>>()

    @Before
    fun setUp() {
        SystemEventsState.reset()
        StreamingServiceState.reset()
        application = RuntimeEnvironment.getApplication()
        notifications = shadowOf(application.getSystemService(NotificationManager::class.java))
        notifications.setNotificationsEnabled(true)
    }

    @After
    fun tearDown() {
        runBlocking {
            for (activity in Activity.activities<LiveActivityAttributes>()) {
                activity.end(null, dismissalPolicy = ActivityUIDismissalPolicy.immediate)
            }
        }
        for (service in services) {
            service.destroy()
        }
        StreamingServiceState.reset()
        SystemEventsState.reset()
    }

    private fun startedIntent(): Intent? = shadowOf(application).nextStartedService

    private fun stoppedIntent(): Intent? = shadowOf(application).nextStoppedService

    private fun runStartedService(): StreamingService {
        val intent = assertNotNull(startedIntent())
        val controller = Robolectric.buildService(StreamingService::class.java, intent)
            .create()
            .startCommand(0, services.size + 1)
        services.add(controller)
        return controller.get()
    }

    private fun requestLiveActivity(vararg texts: String): Activity<LiveActivityAttributes> {
        val functions = texts.map { LiveActionFunction(image = "pawprint", text = it) }
        return Activity.request(
            attributes = LiveActivityAttributes(),
            content = ActivityContent(
                state = LiveActivityAttributes.ContentState(functions = functions, showEllipsis = false),
                staleDate = null,
            ),
        )
    }

    private fun posted(): Notification? = notifications.getNotification(StreamingService.NOTIFICATION_ID)

    private fun Notification.title() = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

    private fun Notification.text() = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

    @Test
    fun eachBackgroundFeatureGetsTheForegroundServiceTypeOfWhatItDoes() {
        assertEquals(mediaPlayback, StreamingService.backgroundTypes(chat = true, printing = false, moblinkRelay = false))
        assertEquals(connectedDevice, StreamingService.backgroundTypes(chat = false, printing = true, moblinkRelay = false))
        assertEquals(connectedDevice, StreamingService.backgroundTypes(chat = false, printing = false, moblinkRelay = true))
        assertEquals(
            connectedDevice or mediaPlayback,
            StreamingService.backgroundTypes(chat = true, printing = true, moblinkRelay = true),
        )
    }

    @Test
    fun theManifestDeclaresTheBackgroundTypesAndTheirPermissions() {
        val info = application.packageManager.getServiceInfo(
            android.content.ComponentName(application, StreamingService::class.java),
            0,
        )
        val types = camera or microphone or connectedDevice or mediaPlayback
        assertEquals(types, info.foregroundServiceType)
        val permissions = application.packageManager
            .getPackageInfo(application.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()
            .toSet()
        assertTrue(Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE in permissions)
        assertTrue(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK in permissions)
        assertTrue(Manifest.permission.CHANGE_NETWORK_STATE in permissions)
    }

    @Test
    fun withoutBackgroundFeaturesNothingStarts() {
        StreamingService.startBackground(chat = false, printing = false, moblinkRelay = false, context = application)
        assertNull(startedIntent())
    }

    @Test
    fun theBackgroundServiceRunsInTheForegroundWithItsTypes() {
        StreamingService.startBackground(chat = true, printing = false, moblinkRelay = true, context = application)
        val service = runStartedService()
        assertEquals(connectedDevice or mediaPlayback, service.foregroundServiceType)
        assertEquals(StreamingService.NOTIFICATION_ID, shadowOf(service).lastForegroundNotificationId)
        val notification = assertNotNull(shadowOf(service).lastForegroundNotification)
        assertEquals("Running in background", notification.text())
        assertNull(stoppedIntent())
        assertTrue(StreamingService.isRunning)
        assertFalse(Camera2Engine.isForegroundServiceRunning)
    }

    @Test
    fun theBackgroundServiceSharesTheLiveActivityNotification() {
        requestLiveActivity("Moblink relay")
        StreamingService.startBackground(chat = false, printing = false, moblinkRelay = true, context = application)
        val service = runStartedService()
        val notification = assertNotNull(shadowOf(service).lastForegroundNotification)
        assertEquals("Moblin is running in background", notification.title())
        assertEquals("Moblink relay", notification.text())
        assertEquals(1, notifications.allNotifications.size)
        assertEquals("Moblink relay", assertNotNull(posted()).text())
    }

    @Test
    fun stopBackgroundStopsTheServiceWhenTheLiveActivityHasEnded() {
        val activity = requestLiveActivity("Background chat")
        StreamingService.startBackground(chat = true, printing = false, moblinkRelay = false, context = application)
        val service = runStartedService()
        runBlocking {
            activity.end(null, dismissalPolicy = ActivityUIDismissalPolicy.immediate)
        }
        assertEquals("Running in background", assertNotNull(posted()).text())
        StreamingService.stopBackground(context = application)
        assertNotNull(stoppedIntent())
        assertFalse(shadowOf(service).isForegroundStopped)
        services.single().destroy()
        services.clear()
        assertFalse(StreamingService.isRunning)
    }

    @Test
    fun stopBackgroundKeepsAStillActiveLiveActivityNotification() {
        requestLiveActivity("Background printing")
        StreamingService.startBackground(chat = false, printing = true, moblinkRelay = false, context = application)
        val service = runStartedService()
        StreamingService.stopBackground(context = application)
        assertTrue(shadowOf(service).isForegroundStopped)
        assertFalse(shadowOf(service).notificationShouldRemoved)
        assertNotNull(stoppedIntent())
        assertEquals("Background printing", assertNotNull(posted()).text())
    }

    @Test
    fun stoppingBeforeTheServiceRunsStopsItRightAfterItEntersTheForeground() {
        StreamingService.startBackground(chat = false, printing = false, moblinkRelay = true, context = application)
        StreamingService.stopBackground(context = application)
        assertNull(stoppedIntent())
        val service = runStartedService()
        assertNotNull(shadowOf(service).lastForegroundNotification)
        assertEquals(connectedDevice, service.foregroundServiceType)
        assertNotNull(stoppedIntent())
    }

    @Test
    fun streamingAndBackgroundShareOneServiceAndItsTypes() {
        shadowOf(application).grantPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.FOREGROUND_SERVICE_CAMERA,
            Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
        )
        StreamingService.start(application)
        val service = runStartedService()
        assertEquals(camera or microphone, service.foregroundServiceType)
        assertTrue(Camera2Engine.isForegroundServiceRunning)
        StreamingService.startBackground(chat = true, printing = false, moblinkRelay = false, context = application)
        assertNull(startedIntent())
        assertEquals(camera or microphone or mediaPlayback, service.foregroundServiceType)
        StreamingService.stop(application)
        assertNull(stoppedIntent())
        assertEquals(mediaPlayback, service.foregroundServiceType)
        assertFalse(Camera2Engine.isForegroundServiceRunning)
        assertEquals("Running in background", assertNotNull(shadowOf(service).lastForegroundNotification).text())
        StreamingService.stopBackground(context = application)
        assertNotNull(stoppedIntent())
    }

    @Test
    fun swipingMoblinAwayTerminatesItAndRemovesTheNotification() {
        var terminations = 0
        val terminateProcess = SystemEvents.terminateProcess
        SystemEvents.terminateProcess = { terminations += 1 }
        try {
            requestLiveActivity("Moblink relay")
            StreamingService.startBackground(chat = false, printing = false, moblinkRelay = true, context = application)
            val service = runStartedService()
            service.onTaskRemoved(Intent())
            assertEquals(1, terminations)
            assertNotNull(stoppedIntent())
            assertNull(posted())
        } finally {
            SystemEvents.terminateProcess = terminateProcess
        }
    }
}
