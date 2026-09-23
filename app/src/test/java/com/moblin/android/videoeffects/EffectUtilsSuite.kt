package com.moblin.android.videoeffects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.moblin.android.various.settings.SettingsAlignment
import com.moblin.android.various.settings.SettingsWidgetLayout
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EffectUtilsSuite {
    private val streamSize = Size(1920f, 1080f)
    private val size = Size(200f, 100f)

    private fun layout(alignment: SettingsAlignment): SettingsWidgetLayout {
        val layout = SettingsWidgetLayout()
        layout.x = 10.0
        layout.y = 20.0
        layout.alignment = alignment
        return layout
    }

    private fun position(alignment: SettingsAlignment): Offset {
        return metalPetalLayerPosition(layout(alignment), size, streamSize)
    }

    @Test
    fun metalPetalLayerPositionCorners() {
        assertEquals(Offset(292f, 266f), position(SettingsAlignment.topLeft))
        assertEquals(Offset(1628f, 266f), position(SettingsAlignment.topRight))
        assertEquals(Offset(292f, 814f), position(SettingsAlignment.bottomLeft))
        assertEquals(Offset(1628f, 814f), position(SettingsAlignment.bottomRight))
    }

    @Test
    fun metalPetalLayerPositionCenters() {
        assertEquals(Offset(960f, 266f), position(SettingsAlignment.topCenter))
        assertEquals(Offset(960f, 814f), position(SettingsAlignment.bottomCenter))
        assertEquals(Offset(292f, 540f), position(SettingsAlignment.leftCenter))
        assertEquals(Offset(1628f, 540f), position(SettingsAlignment.rightCenter))
        assertEquals(Offset(960f, 540f), position(SettingsAlignment.center))
    }

    @Test
    fun metalPetalLayerPositionMatchesCoreImage() {
        for (alignment in SettingsAlignment.entries) {
            val layout = layout(alignment)
            val expected: Offset = TODO("no Android counterpart for CoreImage")
            val position = position(alignment)
            assertTrue(abs(position.x - expected.x) <= 1f)
            assertTrue(abs(position.y - expected.y) <= 1f)
        }
    }
}
