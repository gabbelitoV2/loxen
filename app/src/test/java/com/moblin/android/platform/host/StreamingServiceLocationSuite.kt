package com.moblin.android.platform.host

import android.Manifest
import android.app.Application
import android.content.pm.ServiceInfo
import com.moblin.android.platform.corelocation.CLBackgroundActivitySession
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController

@RunWith(RobolectricTestRunner::class)
class StreamingServiceLocationSuite {
    private val camera = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
    private val microphone = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
    private val location = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
    private val mediaPlayback = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
    private lateinit var application: Application
    private val services = mutableListOf<ServiceController<StreamingService>>()

    @Before
    fun setUp() {
        SystemEventsState.reset()
        StreamingServiceState.reset()
        application = RuntimeEnvironment.getApplication()
        shadowOf(application).grantPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.FOREGROUND_SERVICE_CAMERA,
            Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION,
        )
    }

    @After
    fun tearDown() {
        for (service in services) {
            service.destroy()
        }
        StreamingServiceState.reset()
        SystemEventsState.reset()
    }

    private fun runStartedService(): StreamingService {
        val intent = assertNotNull(shadowOf(application).nextStartedService)
        val controller = Robolectric.buildService(StreamingService::class.java, intent)
            .create()
            .startCommand(0, services.size + 1)
        services.add(controller)
        return controller.get()
    }

    private fun grantLocation() {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @Test
    fun streamingWithLocationKeepsLocationUpdatesInTheBackground() {
        grantLocation()
        val session = CLBackgroundActivitySession()
        StreamingService.start(application)
        val service = runStartedService()
        assertEquals(camera or microphone or location, service.foregroundServiceType)
        session.invalidate()
        assertEquals(camera or microphone, service.foregroundServiceType)
    }

    @Test
    fun aSessionStartedWhileStreamingAddsLocation() {
        grantLocation()
        StreamingService.start(application)
        val service = runStartedService()
        assertEquals(camera or microphone, service.foregroundServiceType)
        val session = CLBackgroundActivitySession()
        assertEquals(camera or microphone or location, service.foregroundServiceType)
        session.invalidate()
        session.invalidate()
        assertEquals(camera or microphone, service.foregroundServiceType)
    }

    @Test
    fun locationAloneDoesNotStartTheService() {
        grantLocation()
        val session = CLBackgroundActivitySession()
        assertNull(shadowOf(application).nextStartedService)
        session.invalidate()
    }

    @Test
    fun withoutLocationPermissionTheServiceHasNoLocationType() {
        CLBackgroundActivitySession()
        StreamingService.startBackground(chat = true, printing = false, moblinkRelay = false, context = application)
        val service = runStartedService()
        assertEquals(mediaPlayback, service.foregroundServiceType)
    }

    @Test
    fun twoSessionsKeepLocationUntilBothEnd() {
        grantLocation()
        val first = CLBackgroundActivitySession()
        val second = CLBackgroundActivitySession()
        StreamingService.start(application)
        val service = runStartedService()
        first.invalidate()
        assertEquals(camera or microphone or location, service.foregroundServiceType)
        second.invalidate()
        assertEquals(camera or microphone, service.foregroundServiceType)
    }
}
