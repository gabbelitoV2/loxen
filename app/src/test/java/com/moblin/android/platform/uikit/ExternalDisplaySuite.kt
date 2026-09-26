package com.moblin.android.platform.uikit

import android.hardware.display.DisplayManager
import android.os.Looper
import android.view.Display
import androidx.activity.ComponentActivity
import com.moblin.android.MoblinApp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsExternalDisplayContent
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDisplayManager

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExternalDisplaySuite {
    private lateinit var controller: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var model: Model

    @Before
    fun setUp() {
        model = Model()
        MoblinApp.globalModel = model
        controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        activity = controller.get()
        runMain()
    }

    @After
    fun tearDown() {
        ExternalDisplay.uninstall()
        if (!activity.isDestroyed) {
            controller.pause().stop().destroy()
        }
        runMain()
        MoblinApp.globalModel = null
    }

    private fun runMain() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun addPresentationDisplay(): Int {
        val displayId = ShadowDisplayManager.addDisplay("w1280dp-h720dp", 2)
        val display = activity.getSystemService(DisplayManager::class.java).getDisplay(displayId)
        shadowOf(display).setFlags(Display.FLAG_PRESENTATION)
        runMain()
        return displayId
    }

    private fun externalDisplayWindow(): Any? {
        val field = Model::class.java.getDeclaredField("externalDisplayWindow")
        field.isAccessible = true
        return field.get(model)
    }

    private fun setContent(content: SettingsExternalDisplayContent) {
        model.database.externalDisplayContent = content
        val update = Model::class.java.getDeclaredMethod("updateExternalMonitorWindow")
        update.isAccessible = true
        update.invoke(model)
        runMain()
    }

    @Test
    fun presentationDisplayShowsExternalScreenContentAndModelKeepsTheWindow() {
        ExternalDisplay.install(activity)
        assertNull(ExternalDisplay.window)
        val displayId = addPresentationDisplay()
        val window = assertNotNull(ExternalDisplay.window)
        assertEquals(displayId, window.displayId)
        assertSame(window, externalDisplayWindow())
        assertTrue(model.externalDisplayPreview)
        assertFalse(window.isHidden)
        assertTrue(window.presentation.isShowing)
        assertEquals(displayId, window.presentation.display.displayId)
    }

    @Test
    fun mirrorHidesThePresentationAndOtherContentShowsIt() {
        ExternalDisplay.install(activity)
        addPresentationDisplay()
        val window = assertNotNull(ExternalDisplay.window)
        setContent(SettingsExternalDisplayContent.mirror)
        assertTrue(window.isHidden)
        assertFalse(window.presentation.isShowing)
        setContent(SettingsExternalDisplayContent.chat)
        assertFalse(window.isHidden)
        setContent(SettingsExternalDisplayContent.mirror)
        assertTrue(window.isHidden)
        setContent(SettingsExternalDisplayContent.cleanStream)
        assertFalse(window.isHidden)
        setContent(SettingsExternalDisplayContent.stream)
        assertTrue(window.presentation.isShowing)
    }

    @Test
    fun removingTheDisplayDisconnectsTheModel() {
        ExternalDisplay.install(activity)
        val displayId = addPresentationDisplay()
        val window = assertNotNull(ExternalDisplay.window)
        ShadowDisplayManager.removeDisplay(displayId)
        runMain()
        assertNull(ExternalDisplay.window)
        assertNull(externalDisplayWindow())
        assertFalse(model.externalDisplayPreview)
        assertFalse(window.presentation.isShowing)
    }

    @Test
    fun displayThatIsAlreadyConnectedIsUsedOnceTheModelExists() {
        MoblinApp.globalModel = null
        addPresentationDisplay()
        ExternalDisplay.install(activity)
        assertNull(ExternalDisplay.window)
        MoblinApp.globalModel = model
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(400))
        assertNotNull(ExternalDisplay.window)
        assertTrue(model.externalDisplayPreview)
    }

    @Test
    fun displaysThatAreNotForPresentationAreIgnoredAndDestroyingTheActivityDisconnects() {
        ExternalDisplay.install(activity)
        ShadowDisplayManager.addDisplay("w640dp-h480dp", 5)
        runMain()
        assertNull(ExternalDisplay.window)
        addPresentationDisplay()
        assertNotNull(ExternalDisplay.window)
        controller.pause().stop().destroy()
        runMain()
        assertNull(ExternalDisplay.window)
        assertFalse(model.externalDisplayPreview)
    }
}
