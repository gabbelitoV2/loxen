package com.moblin.android.various.model

import android.content.pm.PackageManager
import android.os.Looper
import com.moblin.android.AppDelegate
import com.moblin.android.platform.avkit.CMVideoDimensions
import com.moblin.android.platform.host.SystemEventsState
import com.moblin.android.platform.video.layer
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ModelPictureInPictureSuite {
    private lateinit var model: Model

    @Before
    fun setUp() {
        SystemEventsState.reset()
        setPictureInPictureFeature(true)
        model = Model()
        runMain()
    }

    @After
    fun tearDown() {
        model.pipController?.contentSource = null
        SystemEventsState.reset()
    }

    private fun setPictureInPictureFeature(supported: Boolean) {
        shadowOf(AppDelegate.context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE, supported)
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun allowBackgroundStreamingWithPictureInPicture() {
        model.stream.value.backgroundStreaming = true
        model.stream.value.backgroundStreamingPiP = true
    }

    @Test
    fun setupMakesAnAutomaticControllerWithoutContentWhileNotStreaming() {
        model.setupPictureInPicture()
        val controller = assertNotNull(model.pipController)
        assertTrue(controller.canStartPictureInPictureAutomaticallyFromInline)
        assertNull(controller.contentSource)
        assertFalse(model.pictureInPictureEnabled())
    }

    @Test
    fun setupWhileLiveWithBackgroundStreamingAndPipShowsTheStreamPreview() {
        allowBackgroundStreamingWithPictureInPicture()
        model.isLive.value = true
        model.setupPictureInPicture()
        val contentSource = assertNotNull(model.pipController?.contentSource)
        assertSame(model.streamPreviewView.layer, contentSource.sampleBufferDisplayLayer)
        assertTrue(model.pictureInPictureEnabled())
    }

    @Test
    fun contentFollowsBackgroundStreamingShowPipAndLiveOrRecording() {
        model.setupPictureInPicture()
        allowBackgroundStreamingWithPictureInPicture()
        model.updatePictureInPicture()
        assertFalse(model.pictureInPictureEnabled())
        model.isLive.value = true
        model.updatePictureInPicture()
        assertTrue(model.pictureInPictureEnabled())
        model.stream.value.backgroundStreamingPiP = false
        model.updatePictureInPicture()
        assertFalse(model.pictureInPictureEnabled())
        model.stream.value.backgroundStreamingPiP = true
        model.isLive.value = false
        model.isRecording.value = true
        model.updatePictureInPicture()
        assertTrue(model.pictureInPictureEnabled())
        model.stream.value.backgroundStreaming = false
        model.updatePictureInPicture()
        assertFalse(model.pictureInPictureEnabled())
        assertNull(model.pipController?.contentSource)
    }

    @Test
    fun goingLiveAndRecordingUpdatePictureInPictureLikeSwift() {
        model.setupPictureInPicture()
        allowBackgroundStreamingWithPictureInPicture()
        model.setIsLive(value = true)
        assertTrue(model.pictureInPictureEnabled())
        model.setIsLive(value = false)
        assertFalse(model.pictureInPictureEnabled())
        model.setIsRecording(value = true)
        assertTrue(model.pictureInPictureEnabled())
        model.setIsRecording(value = false)
        assertFalse(model.pictureInPictureEnabled())
    }

    @Test
    fun contentIsKeptWhileStillAllowed() {
        allowBackgroundStreamingWithPictureInPicture()
        model.isRecording.value = true
        model.setupPictureInPicture()
        val contentSource = assertNotNull(model.pipController?.contentSource)
        model.updatePictureInPicture()
        assertSame(contentSource, model.pipController?.contentSource)
    }

    @Test
    fun deviceWithoutPictureInPictureHasNoController() {
        setPictureInPictureFeature(false)
        allowBackgroundStreamingWithPictureInPicture()
        model.isLive.value = true
        model.setupPictureInPicture()
        assertNull(model.pipController)
        model.updatePictureInPicture()
        assertFalse(model.pictureInPictureEnabled())
    }

    @Test
    fun playbackDelegateAnswersLikeSwift() {
        allowBackgroundStreamingWithPictureInPicture()
        model.isLive.value = true
        model.setupPictureInPicture()
        val controller = assertNotNull(model.pipController)
        val delegate = assertNotNull(controller.contentSource).playbackDelegate
        assertFalse(delegate.pictureInPictureControllerIsPlaybackPaused(controller))
        val timeRange = delegate.pictureInPictureControllerTimeRangeForPlayback(controller)
        assertEquals(Long.MIN_VALUE, timeRange.start)
        assertEquals(Long.MAX_VALUE, timeRange.duration)
        var completed = false
        delegate.pictureInPictureController(controller, skipByInterval = 10_000_000L) { completed = true }
        assertTrue(completed)
        delegate.pictureInPictureController(controller, setPlaying = false)
        delegate.pictureInPictureController(controller, didTransitionToRenderSize = CMVideoDimensions(320, 180))
    }
}
