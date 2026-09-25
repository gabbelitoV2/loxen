package com.moblin.android.platform.uikit

import android.content.res.Resources
import android.view.Surface
import androidx.activity.ComponentActivity
import com.moblin.android.platform.host.SystemEvents
import com.moblin.android.platform.host.SystemEventsState
import com.moblin.android.various.utils.DeviceOrientation
import com.moblin.android.various.utils.getOrientation
import com.moblin.android.various.utils.getRootViewController
import com.moblin.android.various.utils.getWindow
import com.moblin.android.various.utils.screenScale
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplay

@RunWith(RobolectricTestRunner::class)
class UIApplicationSuite {
    @Before
    fun setUp() {
        SystemEventsState.reset()
        SystemEvents.install(RuntimeEnvironment.getApplication())
        setDeviceOrientation(UIDeviceOrientation.unknown)
    }

    @After
    fun tearDown() {
        setDeviceOrientation(UIDeviceOrientation.unknown)
        SystemEventsState.reset()
    }

    private fun setDeviceOrientation(orientation: Int) {
        val field = UIDevice::class.java.getDeclaredField("orientation")
        field.isAccessible = true
        field.setInt(UIDevice.current, orientation)
    }

    private fun setDisplayRotation(rotation: Int) {
        shadowOf(ShadowDisplay.getDefaultDisplay()).setRotation(rotation)
    }

    private fun windowScene(): UIWindowScene {
        return assertNotNull(UIApplication.shared.connectedScenes.single() as? UIWindowScene)
    }

    @Test
    fun noSceneIsConnectedBeforeAnActivityResumes() {
        assertTrue(UIApplication.shared.connectedScenes.isEmpty())
        assertNull(getWindow())
        assertNull(getRootViewController())
        Robolectric.buildActivity(ComponentActivity::class.java).create().start()
        assertTrue(UIApplication.shared.connectedScenes.isEmpty())
        assertNull(getRootViewController())
    }

    @Test
    fun theRootViewControllerIsTheResumedActivity() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val scene = windowScene()
        val window = assertNotNull(scene.windows.firstOrNull())
        assertSame(activity, window.rootViewController?.activity)
        assertSame(activity.window, window.window)
        assertSame(window, getWindow())
        assertSame(activity, getRootViewController()?.activity)
        assertSame(scene, windowScene())
    }

    @Test
    fun aPausedActivityStaysConnectedUntilItIsDestroyed() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        controller.pause().stop()
        assertSame(controller.get(), getRootViewController()?.activity)
        controller.destroy()
        assertTrue(UIApplication.shared.connectedScenes.isEmpty())
        assertNull(getWindow())
    }

    @Test
    fun theLatestResumedActivityPresents() {
        val first = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        first.pause()
        val second = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        assertSame(second.get(), getRootViewController()?.activity)
        second.pause()
        first.resume()
        second.stop().destroy()
        assertSame(first.get(), getRootViewController()?.activity)
        first.pause().stop().destroy()
        assertNull(getRootViewController())
    }

    @Test
    fun interfaceOrientationFollowsTheDisplayRotation() {
        Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val expected = if (UIDevice.current.isNaturalOrientationLandscape) {
            mapOf(
                Surface.ROTATION_0 to UIInterfaceOrientation.landscapeRight,
                Surface.ROTATION_90 to UIInterfaceOrientation.portraitUpsideDown,
                Surface.ROTATION_180 to UIInterfaceOrientation.landscapeLeft,
                Surface.ROTATION_270 to UIInterfaceOrientation.portrait,
            )
        } else {
            mapOf(
                Surface.ROTATION_0 to UIInterfaceOrientation.portrait,
                Surface.ROTATION_90 to UIInterfaceOrientation.landscapeRight,
                Surface.ROTATION_180 to UIInterfaceOrientation.portraitUpsideDown,
                Surface.ROTATION_270 to UIInterfaceOrientation.landscapeLeft,
            )
        }
        for ((rotation, orientation) in expected) {
            setDisplayRotation(rotation)
            assertEquals(orientation, windowScene().interfaceOrientation, "rotation $rotation")
        }
        setDisplayRotation(Surface.ROTATION_0)
    }

    @Test
    fun getOrientationFallsBackToTheInterfaceOrientation() {
        assertEquals(DeviceOrientation.UNKNOWN, getOrientation())
        Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val landscapeRightRotation = if (UIDevice.current.isNaturalOrientationLandscape) {
            Surface.ROTATION_0
        } else {
            Surface.ROTATION_90
        }
        val landscapeLeftRotation = if (UIDevice.current.isNaturalOrientationLandscape) {
            Surface.ROTATION_180
        } else {
            Surface.ROTATION_270
        }
        setDisplayRotation(landscapeRightRotation)
        assertEquals(DeviceOrientation.LANDSCAPE_LEFT, getOrientation())
        setDisplayRotation(landscapeLeftRotation)
        assertEquals(DeviceOrientation.LANDSCAPE_RIGHT, getOrientation())
        setDeviceOrientation(UIDeviceOrientation.portrait)
        assertEquals(DeviceOrientation.PORTRAIT, getOrientation())
        setDeviceOrientation(UIDeviceOrientation.unknown)
        setDisplayRotation(Surface.ROTATION_0)
    }

    @Test
    @Config(qualifiers = "xhdpi")
    fun screenScaleIsTheWindowDensity() {
        assertEquals(Resources.getSystem().displayMetrics.density, UIScreen.main.scale)
        assertEquals(UIScreen.main.scale, screenScale())
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        assertEquals(2f, activity.resources.displayMetrics.density)
        assertEquals(2f, windowScene().screen.scale)
        assertEquals(2f, screenScale())
    }
}
