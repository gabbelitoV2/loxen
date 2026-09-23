package com.moblin.android.videoeffects

import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coreimage.CIImage
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
    private val streamSize = CGSize(width = 1920, height = 1080)
    private val size = CGSize(width = 200, height = 100)

    private fun layout(alignment: SettingsAlignment): SettingsWidgetLayout {
        return SettingsWidgetLayout(x = 10.0, y = 20.0, alignment = alignment)
    }

    private fun position(alignment: SettingsAlignment): CGPoint {
        return metalPetalLayerPosition(layout(alignment), size, streamSize)
    }

    private fun assertPosition(expected: CGPoint, actual: CGPoint) {
        assertEquals(expected.x, actual.x)
        assertEquals(expected.y, actual.y)
    }

    @Test
    fun metalPetalLayerPositionCorners() {
        assertPosition(CGPoint(x = 292.0, y = 266.0), position(SettingsAlignment.topLeft))
        assertPosition(CGPoint(x = 1628.0, y = 266.0), position(SettingsAlignment.topRight))
        assertPosition(CGPoint(x = 292.0, y = 814.0), position(SettingsAlignment.bottomLeft))
        assertPosition(CGPoint(x = 1628.0, y = 814.0), position(SettingsAlignment.bottomRight))
    }

    @Test
    fun metalPetalLayerPositionCenters() {
        assertPosition(CGPoint(x = 960.0, y = 266.0), position(SettingsAlignment.topCenter))
        assertPosition(CGPoint(x = 960.0, y = 814.0), position(SettingsAlignment.bottomCenter))
        assertPosition(CGPoint(x = 292.0, y = 540.0), position(SettingsAlignment.leftCenter))
        assertPosition(CGPoint(x = 1628.0, y = 540.0), position(SettingsAlignment.rightCenter))
        assertPosition(CGPoint(x = 960.0, y = 540.0), position(SettingsAlignment.center))
    }

    @Test
    fun metalPetalLayerPositionMatchesCoreImage() {
        for (alignment in SettingsAlignment.entries) {
            val layout = layout(alignment)
            val extent = CIImage.black
                .cropped(to = CGRect(origin = CGPoint.zero, size = size))
                .move(layout, streamSize)
                .extent
            val expected = CGPoint(x = extent.midX, y = streamSize.height - extent.midY)
            val position = position(alignment)
            assertTrue(abs(position.x - expected.x) <= 1.0)
            assertTrue(abs(position.y - expected.y) <= 1.0)
        }
    }
}
