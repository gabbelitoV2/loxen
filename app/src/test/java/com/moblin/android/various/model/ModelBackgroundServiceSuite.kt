package com.moblin.android.various.model

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Looper
import androidx.activity.ComponentActivity
import com.moblin.android.moblinliveactivity.shared.LiveActivityAttributes
import com.moblin.android.platform.AndroidHost
import com.moblin.android.platform.activitykit.Activity
import com.moblin.android.platform.activitykit.ActivityUIDismissalPolicy
import com.moblin.android.platform.host.StreamingService
import com.moblin.android.platform.host.StreamingServiceState
import com.moblin.android.platform.host.SystemEvents
import com.moblin.android.platform.host.SystemEventsState
import com.moblin.android.various.Media
import com.moblin.android.various.MediaDelegate
import com.moblin.android.various.settings.SettingsAppMode
import java.lang.reflect.Proxy
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
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
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
class ModelBackgroundServiceSuite {
    private lateinit var model: Model
    private lateinit var application: Application
    private lateinit var notifications: ShadowNotificationManager
    private val services = mutableListOf<ServiceController<StreamingService>>()
    private var terminations = 0
    private var chatBackground = false
    private var moblinkRelay = false
    private var backgroundPrinting = false
    private var backgroundStreaming = false
    private var appMode = SettingsAppMode.streaming
    private val terminateProcess = SystemEvents.terminateProcess

    @Before
    fun setUp() {
        SystemEventsState.reset()
        StreamingServiceState.reset()
        SystemEvents.terminateProcess = { terminations += 1 }
        application = RuntimeEnvironment.getApplication()
        notifications = shadowOf(application.getSystemService(NotificationManager::class.java))
        notifications.setNotificationsEnabled(true)
        model = Model()
        model.media = Media(delegate = mediaDelegate())
        runMain()
        chatBackground = model.database.chat.background
        moblinkRelay = model.database.moblink.relay.enabled.value
        backgroundPrinting = model.database.catPrinters.backgroundPrinting.value
        backgroundStreaming = model.stream.value.backgroundStreaming
        appMode = model.database.appMode
        setBackgroundFeatures(chat = false, relay = false, printing = false)
    }

    private fun setBackgroundFeatures(chat: Boolean, relay: Boolean, printing: Boolean) {
        model.database.chat.background = chat
        model.database.moblink.relay.enabled.value = relay
        model.database.catPrinters.backgroundPrinting.value = printing
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
        setBackgroundFeatures(chat = chatBackground, relay = moblinkRelay, printing = backgroundPrinting)
        model.stream.value.backgroundStreaming = backgroundStreaming
        model.database.appMode = appMode
        model.isLive.value = false
        model.storeSettings()
        SystemEvents.terminateProcess = terminateProcess
        StreamingServiceState.reset()
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

    private fun posted(): Notification? = notifications.getNotification(StreamingService.NOTIFICATION_ID)

    private fun Notification.title() = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

    private fun Notification.text() = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

    private fun enterBackground(): StreamingService {
        model.handleApplicationDidEnterBackground()
        runMain()
        assertTrue(model.inServiceBackground)
        return runStartedService()
    }

    private fun assertSharesTheLiveActivity(service: StreamingService, text: String) {
        val notification = assertNotNull(shadowOf(service).lastForegroundNotification)
        assertEquals("Moblin is running in background", notification.title())
        assertEquals(text, notification.text())
        assertEquals(StreamingService.NOTIFICATION_ID, shadowOf(service).lastForegroundNotificationId)
        assertEquals(1, notifications.allNotifications.size)
    }

    @Test
    fun theMoblinkRelayKeepsMoblinRunningWithAConnectedDeviceService() {
        model.database.moblink.relay.enabled.value = true
        val service = enterBackground()
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE, service.foregroundServiceType)
        assertSharesTheLiveActivity(service, "Moblink relay")
        assertNull(stoppedIntent())
        model.handleApplicationWillEnterForeground()
        runMain()
        assertFalse(model.inServiceBackground)
        assertNotNull(stoppedIntent())
        assertNull(model.liveActivity)
        assertNotEquals("Moblin is running in background", posted()?.title())
    }

    @Test
    fun backgroundChatKeepsMoblinRunningWithAMediaPlaybackService() {
        model.database.chat.background = true
        val service = enterBackground()
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, service.foregroundServiceType)
        assertSharesTheLiveActivity(service, "Background chat")
        model.handleApplicationWillEnterForeground()
        runMain()
        assertNotNull(stoppedIntent())
    }

    @Test
    fun backgroundPrintingKeepsMoblinRunningWithAConnectedDeviceService() {
        model.database.catPrinters.backgroundPrinting.value = true
        val service = enterBackground()
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE, service.foregroundServiceType)
        assertSharesTheLiveActivity(service, "Background printing")
    }

    @Test
    fun allBackgroundFeaturesShareOneServiceWithBothTypes() {
        model.database.chat.background = true
        model.database.moblink.relay.enabled.value = true
        model.database.catPrinters.backgroundPrinting.value = true
        val service = enterBackground()
        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            service.foregroundServiceType,
        )
        assertNull(startedIntent())
    }

    @Test
    fun theServiceStopsWhereSwiftStopsTheSilentAudio() {
        model.database.chat.background = true
        enterBackground()
        assertNull(stoppedIntent())
        model.stopChatPhoneBackgroundAudio()
        assertNotNull(stoppedIntent())
    }

    @Test
    fun withoutBackgroundFeaturesNoServiceStarts() {
        model.handleApplicationDidEnterBackground()
        runMain()
        assertFalse(model.inServiceBackground)
        assertNull(startedIntent())
        assertNull(model.liveActivity)
    }

    @Test
    fun backgroundStreamingLeavesTheServiceToTheStream() {
        model.database.moblink.relay.enabled.value = true
        model.stream.value.backgroundStreaming = true
        model.isLive.value = true
        model.handleApplicationDidEnterBackground()
        runMain()
        assertFalse(model.inServiceBackground)
        assertNull(startedIntent())
        assertNotNull(model.liveActivity)
    }

    @Test
    fun swipingMoblinAwayEndsTheLiveActivityStopsTheServiceAndTerminates() {
        SystemEventsState.bind(model)
        model.database.moblink.relay.enabled.value = true
        val service = enterBackground()
        assertNotNull(posted())
        service.onTaskRemoved(Intent())
        runMain()
        assertEquals(1, terminations)
        assertNull(model.liveActivity)
        assertTrue(Activity.activities<LiveActivityAttributes>().isEmpty())
        assertNull(posted())
        assertNotNull(stoppedIntent())
    }

    @Test
    fun terminationRunsOnceWhenTheActivityIsDestroyedToo() {
        SystemEvents.install(application)
        SystemEventsState.bind(model)
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        ShadowLog.clear()
        controller.get().finish()
        controller.pause().stop().destroy()
        runMain()
        assertEquals(1, ShadowLog.getLogsForTag("SystemEvents").count { it.msg == "Application will terminate" })
        SystemEvents.applicationTaskRemoved(application)
        runMain()
        assertEquals(1, ShadowLog.getLogsForTag("SystemEvents").count { it.msg == "Application will terminate" })
        assertEquals(1, terminations)
    }

    private fun startStreaming(): StreamingService {
        shadowOf(application).grantPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.FOREGROUND_SERVICE_CAMERA,
            Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
        )
        model.isLive.value = true
        AndroidHost.streamingStateChanged(model, active = true)
        val service = runStartedService()
        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            service.foregroundServiceType,
        )
        return service
    }

    private fun enterBackgroundWhileLive() {
        SystemEventsState.setInBackground(true)
        model.handleApplicationDidEnterBackground()
        runMain()
    }

    @Test
    fun aStreamEndingInTheBackgroundHandsTheServiceToTheBackgroundFeatures() {
        model.database.moblink.relay.enabled.value = true
        model.stream.value.backgroundStreaming = true
        val service = startStreaming()
        enterBackgroundWhileLive()
        assertNotNull(model.liveActivity)
        model.isLive.value = false
        AndroidHost.streamingStateChanged(model, active = false)
        assertNull(stoppedIntent())
        assertNull(startedIntent())
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE, service.foregroundServiceType)
        assertEquals("Moblin is running in background", assertNotNull(shadowOf(service).lastForegroundNotification).title())
        assertEquals(1, notifications.allNotifications.size)
        SystemEventsState.setInBackground(false)
        model.handleApplicationWillEnterForeground()
        runMain()
        assertNotNull(stoppedIntent())
    }

    @Test
    fun aStreamEndingInTheBackgroundWithoutBackgroundFeaturesStopsTheService() {
        model.stream.value.backgroundStreaming = true
        startStreaming()
        enterBackgroundWhileLive()
        model.isLive.value = false
        AndroidHost.streamingStateChanged(model, active = false)
        assertNull(startedIntent())
        assertNotNull(stoppedIntent())
    }

    @Test
    fun aStreamStoppedBecauseMoblinEntersTheBackgroundStopsTheService() {
        model.database.moblink.relay.enabled.value = true
        model.stream.value.backgroundStreaming = false
        startStreaming()
        SystemEventsState.setInBackground(true)
        model.isLive.value = false
        AndroidHost.streamingStateChanged(model, active = false)
        assertNull(startedIntent())
        assertNotNull(stoppedIntent())
    }

    @Test
    fun chatPhoneModeIsTheAppModeNotItsLocalizedName() {
        model.database.appMode = SettingsAppMode.streaming
        assertFalse(model.isChatPhone())
        model.database.appMode = SettingsAppMode.chatPhone
        assertTrue(model.isChatPhone())
    }
}
