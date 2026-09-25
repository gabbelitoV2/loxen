package com.moblin.android.platform.avkit

import android.app.ActivityManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Looper
import android.util.Rational
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import com.moblin.android.AppDelegate
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.video.PreviewView
import com.moblin.android.platform.avfoundation.CMTimeRange
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.host.SystemEvents
import com.moblin.android.platform.host.SystemEventsState
import com.moblin.android.platform.video.CMVideoFormatDescriptionCreateForImageBuffer
import com.moblin.android.platform.video.CVPixelBufferPool
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.platform.video.layer
import java.time.Duration
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAppTask

@RunWith(RobolectricTestRunner::class)
class AVPictureInPictureControllerSuite {
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var container: FrameLayout
    private lateinit var preview: PreviewView
    private val controllers = mutableListOf<AVPictureInPictureController>()
    private val pools = mutableListOf<CVPixelBufferPool>()
    private val renderSizes = mutableListOf<CMVideoDimensions>()
    private var presentationTimeUs = 10_000_000L

    private val delegate = object : AVPictureInPictureSampleBufferPlaybackDelegate {
        override fun pictureInPictureController(
            pictureInPictureController: AVPictureInPictureController,
            setPlaying: Boolean,
        ) {
        }

        override fun pictureInPictureControllerTimeRangeForPlayback(
            pictureInPictureController: AVPictureInPictureController,
        ): CMTimeRange {
            return CMTimeRange(start = Long.MIN_VALUE, duration = Long.MAX_VALUE)
        }

        override fun pictureInPictureControllerIsPlaybackPaused(
            pictureInPictureController: AVPictureInPictureController,
        ): Boolean {
            return false
        }

        override fun pictureInPictureController(
            pictureInPictureController: AVPictureInPictureController,
            didTransitionToRenderSize: CMVideoDimensions,
        ) {
            renderSizes.add(didTransitionToRenderSize)
        }

        override fun pictureInPictureController(
            pictureInPictureController: AVPictureInPictureController,
            skipByInterval: Long,
            completion: () -> Unit,
        ) {
            completion()
        }
    }

    @Before
    fun setUp() {
        SystemEventsState.reset()
        setPictureInPictureFeature(true)
        activityController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        activity = activityController.get()
        val root = FrameLayout(activity)
        container = FrameLayout(activity)
        container.addView(View(activity))
        preview = PreviewView(activity)
        container.addView(preview)
        container.addView(View(activity))
        root.addView(container)
        activity.setContentView(root)
        PictureInPictureWindow.install(activity)
        runMain()
    }

    @After
    fun tearDown() {
        for (controller in controllers) {
            controller.contentSource = null
        }
        PictureInPictureWindow.sdkInt = Build.VERSION.SDK_INT
        for (pool in pools) {
            pool.invalidate()
        }
        SystemEventsState.reset()
    }

    private fun setPictureInPictureFeature(supported: Boolean) {
        shadowOf(AppDelegate.context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE, supported)
    }

    private fun runMain() {
        repeat(3) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            PipelineThread.runSync(timeoutMs = 5000) {}
        }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
    }

    private fun makeController(armed: Boolean = true): AVPictureInPictureController {
        val controller = AVPictureInPictureController(
            contentSource = AVPictureInPictureController.ContentSource(
                sampleBufferDisplayLayer = preview.layer,
                playbackDelegate = delegate,
            ),
        )
        controller.canStartPictureInPictureAutomaticallyFromInline = armed
        controllers.add(controller)
        return controller
    }

    private fun showFrame(width: Int, height: Int) {
        val pool = CVPixelBufferPool(width, height, kCVPixelFormatType_32BGRA, 2)
        pools.add(pool)
        PipelineThread.runSync(timeoutMs = 5000) {
            val buffer = pool.createPixelBuffer()!!
            presentationTimeUs += 33_333
            val sample = MediaSample(
                ByteArray(0),
                presentationTimeUs,
                true,
                CMVideoFormatDescriptionCreateForImageBuffer(buffer),
                buffer,
            )
            preview.enqueue(sample, isFirstAfterAttach = false)
        }
        runMain()
    }

    private fun enterPictureInPicture() {
        assertTrue(activity.enterPictureInPictureMode(PictureInPictureWindow.appliedParams!!))
        activity.onPictureInPictureModeChanged(true, Configuration(activity.resources.configuration))
        runMain()
    }

    private fun expandPictureInPicture() {
        val shadow = shadowOf(activity)
        val field = shadow.javaClass.getDeclaredField("isInPictureInPictureMode")
        field.isAccessible = true
        field.setBoolean(shadow, false)
        activity.onPictureInPictureModeChanged(false, Configuration(activity.resources.configuration))
        runMain()
    }

    private fun contentFrame(): ViewGroup {
        return activity.findViewById(android.R.id.content)
    }

    @Test
    fun supportFollowsThePictureInPictureSystemFeature() {
        assertTrue(AVPictureInPictureController.isPictureInPictureSupported())
        setPictureInPictureFeature(false)
        assertFalse(AVPictureInPictureController.isPictureInPictureSupported())
    }

    @Test
    fun automaticStartWithContentTurnsOnAutoEnterWithoutSeamlessResize() {
        val controller = makeController()
        var params = assertNotNull(PictureInPictureWindow.appliedParams)
        assertTrue(params.isAutoEnterEnabled)
        assertFalse(params.isSeamlessResizeEnabled)
        controller.contentSource = null
        params = assertNotNull(PictureInPictureWindow.appliedParams)
        assertFalse(params.isAutoEnterEnabled)
        controller.contentSource = AVPictureInPictureController.ContentSource(
            sampleBufferDisplayLayer = preview.layer,
            playbackDelegate = delegate,
        )
        assertTrue(PictureInPictureWindow.appliedParams!!.isAutoEnterEnabled)
        controller.canStartPictureInPictureAutomaticallyFromInline = false
        assertFalse(PictureInPictureWindow.appliedParams!!.isAutoEnterEnabled)
    }

    @Test
    fun aspectRatioAndSourceRectHintFollowTheShownStreamFrames() {
        makeController()
        assertNull(PictureInPictureWindow.appliedParams!!.aspectRatio)
        showFrame(1280, 720)
        var params = PictureInPictureWindow.appliedParams!!
        assertEquals(Rational(16, 9), params.aspectRatio)
        val location = IntArray(2)
        preview.getLocationInWindow(location)
        assertTrue(preview.width > 0 && preview.height > 0)
        val hint = assertNotNull(params.sourceRectHint)
        assertEquals(preview.width, hint.width())
        assertEquals((preview.width / (16.0 / 9)).roundToInt(), hint.height())
        assertEquals(location[0], hint.left)
        assertEquals(location[1] + (preview.height - hint.height()) / 2, hint.top)
        showFrame(720, 1280)
        params = PictureInPictureWindow.appliedParams!!
        assertEquals(Rational(9, 16), params.aspectRatio)
        assertEquals((preview.height * (9.0 / 16)).roundToInt(), params.sourceRectHint!!.width())
        assertEquals(preview.height, params.sourceRectHint!!.height())
    }

    @Test
    fun aspectRatioStaysWithinWhatAndroidAccepts() {
        assertEquals(Rational(239, 100), PictureInPictureWindow.aspectRatio(Size(3000, 1000)))
        assertEquals(Rational(100, 239), PictureInPictureWindow.aspectRatio(Size(1000, 3000)))
        assertEquals(Rational(4, 3), PictureInPictureWindow.aspectRatio(Size(640, 480)))
        assertNull(PictureInPictureWindow.aspectRatio(Size(0, 480)))
    }

    @Test
    fun pictureInPictureShowsOnlyTheStreamPreviewAndLeavingPutsItBack() {
        val controller = makeController()
        showFrame(1280, 720)
        assertFalse(controller.isPictureInPictureActive)
        enterPictureInPicture()
        assertTrue(controller.isPictureInPictureActive)
        val content = contentFrame()
        val window = content.getChildAt(content.childCount - 1) as ViewGroup
        assertNotSame(container.parent, window)
        assertSame(window, preview.parent)
        assertEquals(1, window.childCount)
        assertEquals(Color.BLACK, (window.background as ColorDrawable).color)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, preview.layoutParams.width)
        assertEquals(CMVideoDimensions(width = window.width, height = window.height), renderSizes.last())
        expandPictureInPicture()
        assertFalse(controller.isPictureInPictureActive)
        assertSame(container, preview.parent)
        assertEquals(1, container.indexOfChild(preview))
        assertEquals(3, container.childCount)
        assertNull(window.parent)
        assertTrue(content.indexOfChild(window) < 0)
    }

    @Test
    fun removingTheContentWhilePictureInPictureClosesTheWindow() {
        val controller = makeController()
        enterPictureInPicture()
        assertTrue(activity.isInPictureInPictureMode)
        controller.contentSource = null
        assertFalse(activity.isInPictureInPictureMode)
        assertFalse(controller.isPictureInPictureActive)
        assertSame(container, preview.parent)
        assertEquals(1, container.indexOfChild(preview))
    }

    @Test
    fun pictureInPictureWithoutAutomaticStartShowsNothingSpecial() {
        val controller = makeController(armed = false)
        enterPictureInPicture()
        assertFalse(controller.isPictureInPictureActive)
        assertSame(container, preview.parent)
    }

    @Test
    fun pictureInPictureCountsAsBackgroundLikeIos() {
        makeController()
        assertFalse(SystemEvents.isInBackground)
        enterPictureInPicture()
        assertTrue(SystemEvents.isInBackground)
    }

    @Test
    fun leavingTheAppEntersPictureInPictureBeforeAndroid12WhenArmed() {
        PictureInPictureWindow.sdkInt = Build.VERSION_CODES.R
        val controller = makeController()
        controller.contentSource = null
        activityController.userLeaving()
        assertFalse(activity.isInPictureInPictureMode)
        controller.contentSource = AVPictureInPictureController.ContentSource(
            sampleBufferDisplayLayer = preview.layer,
            playbackDelegate = delegate,
        )
        activityController.userLeaving()
        assertTrue(activity.isInPictureInPictureMode)
    }

    private fun setOwnTaskTop(topActivity: ComponentName) {
        val info = ActivityManager.RecentTaskInfo()
        info.baseActivity = activity.componentName
        info.topActivity = topActivity
        val task = ShadowAppTask.newInstance()
        val shadowTask: ShadowAppTask = Shadow.extract(task)
        shadowTask.setTaskInfo(info)
        shadowOf(activity.getSystemService(ActivityManager::class.java)).setAppTasks(listOf(task))
    }

    @Test
    fun openingAScreenInTheAppsOwnTaskDoesNotEnterPictureInPictureBeforeAndroid12() {
        PictureInPictureWindow.sdkInt = Build.VERSION_CODES.R
        makeController()
        setOwnTaskTop(ComponentName("com.android.permissioncontroller", "GrantPermissionsActivity"))
        activityController.userLeaving()
        assertFalse(activity.isInPictureInPictureMode)
        setOwnTaskTop(activity.componentName)
        activityController.userLeaving()
        assertTrue(activity.isInPictureInPictureMode)
    }

    @Test
    fun contentChangedOffTheMainThreadIsAppliedOnTheMainThread() {
        val controller = makeController()
        showFrame(1280, 720)
        enterPictureInPicture()
        assertSame<Any?>(contentFrame().getChildAt(contentFrame().childCount - 1), preview.parent)
        val thread = Thread { controller.contentSource = null }
        thread.start()
        thread.join()
        assertTrue(controller.isPictureInPictureActive)
        runMain()
        assertFalse(controller.isPictureInPictureActive)
        assertFalse(activity.isInPictureInPictureMode)
        assertSame(container, preview.parent)
        assertFalse(PictureInPictureWindow.appliedParams!!.isAutoEnterEnabled)
    }

    @Test
    fun leavingTheAppLeavesEnteringToTheSystemOnAndroid12AndLater() {
        makeController()
        activityController.userLeaving()
        assertFalse(activity.isInPictureInPictureMode)
    }

    @Test
    fun unsupportedDeviceNeverEntersPictureInPicture() {
        PictureInPictureWindow.sdkInt = Build.VERSION_CODES.R
        setPictureInPictureFeature(false)
        makeController()
        activityController.userLeaving()
        assertFalse(activity.isInPictureInPictureMode)
    }
}
