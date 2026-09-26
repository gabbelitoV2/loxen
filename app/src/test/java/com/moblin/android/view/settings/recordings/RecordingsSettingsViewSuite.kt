package com.moblin.android.view.settings.recordings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.moblin.android.LocalModel
import com.moblin.android.various.Media
import com.moblin.android.various.MediaDelegate
import com.moblin.android.various.model.Model
import java.lang.reflect.Proxy
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecordingsSettingsViewSuite {
    @get:Rule
    val rule = createComposeRule()

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

    @Test
    fun anUnavailableRecordingsDirectoryIsARowLikeTheOthers() {
        val model = Model()
        model.media = Media(delegate = mediaDelegate())
        model.stream.value.recording.recordingPath = "no longer granted".toByteArray()
        rule.setContent {
            CompositionLocalProvider(LocalModel provides model) {
                RecordingsSettingsView(model = model)
            }
        }
        rule.waitForIdle()
        val row = rule.onNodeWithText("Default recordings directory").fetchSemanticsNode().boundsInRoot
        val unavailable = rule.onNodeWithText("Current recordings directory unavailable")
            .fetchSemanticsNode()
            .boundsInRoot
        assertEquals(row.left, unavailable.left)
    }
}
