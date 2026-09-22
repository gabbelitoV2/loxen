package com.moblin.android.videoeffects

import android.graphics.PointF
import android.util.Size
import com.moblin.android.various.settings.SettingsAlignment
import com.moblin.android.various.settings.SettingsWidgetLayout
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class EffectUtilsSuite {
    private val streamSize = Size(1920, 1080)
    private val size = Size(200, 100)

    private fun layout(alignment: SettingsAlignment): SettingsWidgetLayout {
        val layout = SettingsWidgetLayout()
        layout.x = 10
        layout.y = 20
        layout.alignment = alignment
        return layout
    }

    private fun position(alignment: SettingsAlignment): PointF {
        return metalPetalLayerPosition(layout(alignment), size, streamSize)
    }

    @Test
    fun metalPetalLayerPositionCorners() {
        assertEquals(PointF(292f, 266f), position(SettingsAlignment.topLeft))
        assertEquals(PointF(1628f, 266f), position(SettingsAlignment.topRight))
        assertEquals(PointF(292f, 814f), position(SettingsAlignment.bottomLeft))
        assertEquals(PointF(1628f, 814f), position(SettingsAlignment.bottomRight))
    }

    @Test
    fun metalPetalLayerPositionCenters() {
        assertEquals(PointF(960f, 266f), position(SettingsAlignment.topCenter))
        assertEquals(PointF(960f, 814f), position(SettingsAlignment.bottomCenter))
        assertEquals(PointF(292f, 540f), position(SettingsAlignment.leftCenter))
        assertEquals(PointF(1628f, 540f), position(SettingsAlignment.rightCenter))
        assertEquals(PointF(960f, 540f), position(SettingsAlignment.center))
    }

    @Test
    fun metalPetalLayerPositionMatchesCoreImage() {
        for (alignment in SettingsAlignment.entries) {
            val layout = layout(alignment)
            val expected: PointF = TODO("no Android counterpart for CoreImage")
            val position = position(alignment)
            assertTrue(abs(position.x - expected.x) <= 1f)
            assertTrue(abs(position.y - expected.y) <= 1f)
        }
    }
}
