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
        return layoutCenter(layout(alignment), size, streamSize)
    }

    private fun assertPosition(expected: CGPoint, actual: CGPoint) {
        assertEquals(expected.x, actual.x)
        assertEquals(expected.y, actual.y)
    }

    @Test
    fun layoutCenterCorners() {
        assertPosition(CGPoint(x = 292.0, y = 266.0), position(SettingsAlignment.topLeft))
        assertPosition(CGPoint(x = 1628.0, y = 266.0), position(SettingsAlignment.topRight))
        assertPosition(CGPoint(x = 292.0, y = 814.0), position(SettingsAlignment.bottomLeft))
        assertPosition(CGPoint(x = 1628.0, y = 814.0), position(SettingsAlignment.bottomRight))
    }

    @Test
    fun layoutCenterCenters() {
        assertPosition(CGPoint(x = 960.0, y = 266.0), position(SettingsAlignment.topCenter))
        assertPosition(CGPoint(x = 960.0, y = 814.0), position(SettingsAlignment.bottomCenter))
        assertPosition(CGPoint(x = 292.0, y = 540.0), position(SettingsAlignment.leftCenter))
        assertPosition(CGPoint(x = 1628.0, y = 540.0), position(SettingsAlignment.rightCenter))
        assertPosition(CGPoint(x = 960.0, y = 540.0), position(SettingsAlignment.center))
    }

    @Test
    fun layoutCenterMatchesCoreImage() {
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

    @Test
    fun layoutScaleFitsInsideLayoutSize() {
        var layout = SettingsWidgetLayout()
        layout = layout.copy(size = 50.0)
        assertEquals(0.5, layoutScale(layout, CGSize(width = 1920.0, height = 1080.0), streamSize))
        assertEquals(0.5, layoutScale(layout, CGSize(width = 480.0, height = 1080.0), streamSize))
        assertEquals(0.5, layoutScale(layout, CGSize(width = 1920.0, height = 270.0), streamSize))
    }

    @Test
    fun widgetShapePlacement() {
        var layout = layout(SettingsAlignment.bottomRight)
        layout = layout.copy(size = 50.0)
        val shape = WidgetShape(contentRegion = CGRect(x = 100.0, y = 50.0, width = 400.0, height = 200.0))
        val placement = shape.placement(layout, streamSize)
        assertEquals(2.4, placement.scale)
        assertEquals(CGSize(width = 960.0, height = 480.0), placement.size)
        assertEquals(0.0, placement.borderWidth)
        assertEquals(placement.size, placement.borderSize)
        assertPosition(CGPoint(x = 1248.0, y = 624.0), placement.center)
    }

    @Test
    fun widgetShapePlacementNoResize() {
        val shape = WidgetShape(contentRegion = CGRect(origin = CGPoint.zero, size = size))
        val placement = shape.placement(layout(SettingsAlignment.topLeft), streamSize, false)
        assertEquals(1.0, placement.scale)
        assertEquals(size, placement.size)
        assertPosition(position(SettingsAlignment.topLeft), placement.center)
    }

    @Test
    fun widgetShapePlacementRotatedWithBorder() {
        var layout = layout(SettingsAlignment.topLeft)
        layout = layout.copy(size = 50.0)
        val shape = WidgetShape(contentRegion = CGRect(x = 0.0, y = 0.0, width = 400.0, height = 200.0), rotation = 90.0)
        shape.borderWidth = 2.0
        val placement = shape.placement(layout, streamSize)
        assertEquals(1.35, placement.scale)
        assertEquals(CGSize(width = 540.0, height = 270.0), placement.size)
        assertEquals(13.5, placement.borderWidth)
        assertEquals(CGSize(width = 567.0, height = 297.0), placement.borderSize)
        assertPosition(CGPoint(x = 340.5, y = 499.5), placement.center)
        assertEquals(0.0f, shape.cornerRadiusPixels(placement.size))
    }

    @Test
    fun widgetShapeApplySettings() {
        val shape = WidgetShape(contentRegion = CGRect(x = 100.0, y = 50.0, width = 400.0, height = 200.0))
        shape.apply(ShapeEffectSettings(cornerRadius = 0.5f,
                                        borderWidth = 2.0,
                                        cropEnabled = true,
                                        cropX = 0.25,
                                        cropY = 0.5,
                                        cropWidth = 0.5,
                                        cropHeight = 0.25))
        assertEquals(CGRect(x = 200.0, y = 150.0, width = 200.0, height = 50.0), shape.contentRegion)
        assertEquals(2.0, shape.borderWidth)
        assertEquals(12.5f, shape.cornerRadiusPixels(CGSize(width = 200.0, height = 50.0)))
    }
}
