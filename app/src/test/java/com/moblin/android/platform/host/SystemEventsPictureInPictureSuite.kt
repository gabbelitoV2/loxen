package com.moblin.android.platform.host

import android.app.PictureInPictureParams
import android.os.Looper
import androidx.activity.ComponentActivity
import com.moblin.android.platform.capture.Camera2Engine
import com.moblin.android.various.model.Model
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class SystemEventsPictureInPictureSuite {
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity

    @Before
    fun setUp() {
        SystemEventsState.reset()
        SystemEvents.install(RuntimeEnvironment.getApplication())
        activityController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        activity = activityController.get()
        ShadowLog.clear()
    }

    @After
    fun tearDown() {
        SystemEventsState.reset()
    }

    private fun count(message: String): Int {
        return ShadowLog.getLogsForTag("SystemEvents").count { it.msg == message }
    }

    private fun enterPictureInPicture() {
        assertTrue(activity.enterPictureInPictureMode(PictureInPictureParams.Builder().build()))
        activityController.pause()
    }

    private fun leavePictureInPictureMode() {
        val shadow = shadowOf(activity)
        val field = shadow.javaClass.getDeclaredField("isInPictureInPictureMode")
        field.isAccessible = true
        field.setBoolean(shadow, false)
    }

    @Test
    fun pictureInPictureIsBackgroundAndExpandingIsForeground() {
        enterPictureInPicture()
        assertTrue(SystemEvents.isInBackground)
        assertTrue(Camera2Engine.isInBackground)
        assertEquals(1, count("Application did enter background"))
        leavePictureInPictureMode()
        activityController.resume()
        assertFalse(SystemEvents.isInBackground)
        assertFalse(Camera2Engine.isInBackground)
        assertEquals(1, count("Application will enter foreground"))
    }

    @Test
    fun expandingWhenTheActivityResumesBeforePictureInPictureEndsIsForegroundOnce() {
        enterPictureInPicture()
        activityController.resume()
        assertTrue(SystemEvents.isInBackground)
        assertEquals(0, count("Application will enter foreground"))
        leavePictureInPictureMode()
        SystemEvents.pictureInPictureModeChanged(isInPictureInPictureMode = false)
        assertFalse(SystemEvents.isInBackground)
        assertFalse(Camera2Engine.isInBackground)
        assertEquals(1, count("Application will enter foreground"))
        activityController.pause().resume()
        assertEquals(1, count("Application will enter foreground"))
    }

    @Test
    fun pictureInPictureModeChangeFromTheWindowIsBackgroundOnce() {
        SystemEvents.pictureInPictureModeChanged(isInPictureInPictureMode = true)
        enterPictureInPicture()
        SystemEvents.pictureInPictureModeChanged(isInPictureInPictureMode = false)
        assertTrue(SystemEvents.isInBackground)
        assertEquals(1, count("Application did enter background"))
    }

    @Test
    fun closingPictureInPictureStaysInBackgroundUntilTheAppIsOpenedAgain() {
        enterPictureInPicture()
        leavePictureInPictureMode()
        activityController.stop()
        assertTrue(SystemEvents.isInBackground)
        assertEquals(1, count("Application did enter background"))
        assertEquals(0, count("Application will enter foreground"))
        activityController.restart().resume()
        assertFalse(SystemEvents.isInBackground)
        assertEquals(1, count("Application will enter foreground"))
    }

    @Test
    fun pausingWithoutPictureInPictureIsNotBackground() {
        activityController.pause()
        assertFalse(SystemEvents.isInBackground)
        activityController.resume()
        assertEquals(0, count("Application did enter background"))
        assertEquals(0, count("Application will enter foreground"))
    }

    @Test
    fun goingHomeWithoutPictureInPictureIsBackgroundOnce() {
        activityController.pause().stop()
        assertTrue(SystemEvents.isInBackground)
        assertEquals(1, count("Application did enter background"))
        activityController.restart().resume()
        assertFalse(SystemEvents.isInBackground)
        assertEquals(1, count("Application will enter foreground"))
    }

    @Test
    fun pausingAndResumingTellTheModelTheAppResignsAndBecomesActive() {
        val model = Model()
        shadowOf(Looper.getMainLooper()).idle()
        SystemEventsState.bind(model)
        activityController.pause()
        assertFalse(model.isAppActive)
        activityController.resume()
        assertTrue(model.isAppActive)
    }
}
