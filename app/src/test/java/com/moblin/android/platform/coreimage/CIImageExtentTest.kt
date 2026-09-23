package com.moblin.android.platform.coreimage

import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGImagePropertyOrientation
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.internal.orientationTransform
import com.moblin.android.various.settings.SettingsAlignment
import com.moblin.android.various.settings.SettingsWidgetLayout
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import org.junit.Test

class CIImageExtentTest {
    private fun toPixels(percentage: Double, total: Double): Double {
        return (percentage * total) / 100
    }

    private fun CIImage.translated(x: Double, y: Double): CIImage {
        return transformed(by = CGAffineTransform(translationX = x, y = y))
    }

    private fun CIImage.scaled(x: Double, y: Double): CIImage {
        return transformed(by = CGAffineTransform(scaleX = x, y = y), highQualityDownsample = false)
    }

    private fun CIImage.move(layout: SettingsWidgetLayout, streamSize: CGSize): CIImage {
        var x: Double
        var y: Double
        if (layout.alignment.isHorizontalCenter()) {
            x = (streamSize.width - extent.width) / 2 - extent.minX
        } else if (layout.alignment.isLeft()) {
            x = toPixels(layout.x, streamSize.width) - extent.minX
        } else {
            x = streamSize.width - toPixels(layout.x, streamSize.width) - extent.width - extent.minX
            if (x != 0.0) {
                x += 1
            }
        }
        if (layout.alignment.isVerticalCenter()) {
            y = (streamSize.height - extent.height) / 2 - extent.minY
        } else if (layout.alignment.isTop()) {
            y = streamSize.height - toPixels(layout.y, streamSize.height) - extent.height - extent.minY
            if (y != 0.0) {
                y += 1
            }
        } else {
            y = toPixels(layout.y, streamSize.height) - extent.minY
        }
        return translated(x = x, y = y)
    }

    private fun CIImage.resizeMirror(layout: SettingsWidgetLayout, streamSize: CGSize, mirror: Boolean): CIImage {
        var scaleX = toPixels(layout.size, streamSize.width) / extent.size.width
        val scaleY = toPixels(layout.size, streamSize.height) / extent.size.height
        val scale = min(scaleX, scaleY)
        scaleX = if (mirror) -scale else scale
        val scaledImage = scaled(x = scaleX, y = scale)
        return if (mirror) {
            scaledImage.translated(x = scaledImage.extent.width, y = 0.0)
        } else {
            scaledImage
        }
    }

    private fun layout(alignment: SettingsAlignment, x: Double = 10.0, y: Double = 20.0): SettingsWidgetLayout {
        val layout = SettingsWidgetLayout()
        layout.x = x
        layout.y = y
        layout.alignment = alignment
        return layout
    }

    private fun canvas(width: Double, height: Double): CIImage {
        return CIImage.black.cropped(to = CGRect(0.0, 0.0, width, height))
    }

    private fun triple(image: CIImage): CIImage {
        val size = image.extent.size
        val width = size.width / 3
        val height = size.height
        val centerImage = image.cropped(to = CGRect(x = width, y = 0.0, width = width, height = height))
        val leftImage = centerImage.translated(x = -width, y = 0.0)
        val rightImage = centerImage.translated(x = width, y = 0.0)
        val center = centerImage.composited(over = leftImage)
        return rightImage.composited(over = center)
    }

    private fun twin(image: CIImage): CIImage {
        val size = image.extent.size
        val width = size.width / 2
        val height = size.height
        val centerImage = image.cropped(to = CGRect(x = width / 2, y = 0.0, width = width, height = height))
        val leftImage = centerImage.translated(x = -width / 2, y = 0.0)
        val rightImage = centerImage.scaled(x = -1.0, y = 1.0).translated(x = 5 * width / 2, y = 0.0)
        return rightImage.composited(over = leftImage)
    }

    @Test
    fun nullAndInfiniteRects() {
        val nullRect = CGRect.nullRect
        assertTrue(nullRect.isNull)
        assertTrue(nullRect.isEmpty)
        assertFalse(nullRect.isInfinite)
        assertTrue(CGRect.infinite.isInfinite)
        assertFalse(CGRect.infinite.isNull)
        val rect = CGRect(1.0, 2.0, 3.0, 4.0)
        assertEquals(rect, nullRect.union(rect))
        assertEquals(rect, rect.union(nullRect))
        assertTrue(rect.intersection(nullRect).isNull)
        assertEquals(rect, CGRect.infinite.intersection(rect))
        assertEquals(rect, rect.intersection(CGRect.infinite))
        assertTrue(CGRect.infinite.union(rect).isInfinite)
        assertTrue(CGRect.infinite.applying(CGAffineTransform(scaleX = 2.0, y = 3.0)).isInfinite)
        assertTrue(nullRect.applying(CGAffineTransform(translationX = 2.0, y = 3.0)).isNull)
        assertTrue(CGRect.infinite.insetBy(dx = 10.0, dy = 10.0).isInfinite)
        assertTrue(nullRect.offsetBy(dx = 10.0, dy = 10.0).isNull)
        assertTrue(CGRect(0.0, 0.0, 10.0, 10.0).intersection(CGRect(20.0, 0.0, 10.0, 10.0)).isNull)
        assertTrue(CGRect(0.0, 0.0, 10.0, 10.0).insetBy(dx = 6.0, dy = 0.0).isNull)
    }

    @Test
    fun negativeZero() {
        assertEquals(CGRect(0.0, 0.0, 10.0, 10.0), CGRect(-0.0, -0.0, 10.0, 10.0))
        assertEquals(CGRect(0.0, 0.0, 10.0, 10.0).hashCode(), CGRect(-0.0, -0.0, 10.0, 10.0).hashCode())
        assertEquals(CGPoint(0.0, 0.0), CGPoint(-0.0, 0.0))
        assertEquals(CGSize(0.0, 1.0), CGSize(-0.0, 1.0))
        val mirrored = canvas(1920.0, 1080.0).scaled(x = -1.0, y = 1.0).translated(x = 1920.0, y = 0.0)
        assertEquals(CGRect(0.0, 0.0, 1920.0, 1080.0), mirrored.extent)
    }

    @Test
    fun unionAndIntersection() {
        val a = CGRect(0.0, 0.0, 10.0, 10.0)
        val b = CGRect(5.0, -5.0, 10.0, 10.0)
        assertEquals(CGRect(0.0, -5.0, 15.0, 15.0), a.union(b))
        assertEquals(CGRect(5.0, 0.0, 5.0, 5.0), a.intersection(b))
        val negative = CGRect(10.0, 10.0, -10.0, -10.0)
        assertEquals(CGRect(0.0, 0.0, 10.0, 10.0), negative.standardized)
        assertEquals(0.0, negative.minX)
        assertEquals(10.0, negative.width)
        assertEquals(CGRect(0.0, 0.0, 10.0, 10.0), negative.intersection(a))
        assertEquals(CGRect(0.0, 0.0, 2.0, 3.0), CGRect(0.25, 0.5, 1.5, 2.25).integral)
    }

    @Test
    fun imageExtents() {
        val image = canvas(100.0, 50.0)
        assertEquals(CGRect(0.0, 0.0, 100.0, 50.0), image.extent)
        assertTrue(CIImage.black.extent.isInfinite)
        assertTrue(CIImage.empty().extent.isNull)
        assertTrue(CIImage.empty().cropped(to = image.extent).extent.isNull)
        assertEquals(CGRect(10.0, 20.0, 100.0, 50.0), image.translated(x = 10.0, y = 20.0).extent)
        assertEquals(CGRect(0.0, 0.0, 200.0, 25.0), image.scaled(x = 2.0, y = 0.5).extent)
        val other = canvas(10.0, 10.0).translated(x = 200.0, y = 0.0)
        assertEquals(CGRect(0.0, 0.0, 210.0, 50.0), other.composited(over = image).extent)
        assertNotSame(image, image.cropped(to = image.extent))
        assertEquals(CGRect(-30.0, -30.0, 160.0, 110.0), image.applyingGaussianBlur(sigma = 10.0).extent)
        val rotated = image.transformed(by = CGAffineTransform(rotationAngle = PI / 2))
        assertTrue(abs(rotated.extent.minX + 50.0) < 1e-9)
        assertTrue(abs(rotated.extent.width - 50.0) < 1e-9)
        assertTrue(abs(rotated.extent.height - 100.0) < 1e-9)
        val epsilon = 0.00001
        val cropped = image.cropped(to = image.extent.insetBy(dx = epsilon, dy = epsilon))
        assertEquals(epsilon, cropped.extent.minX)
        assertEquals(epsilon, cropped.extent.minY)
        assertTrue(abs(cropped.extent.maxX - (100.0 - epsilon)) < 1e-9)
        assertTrue(abs(cropped.extent.maxY - (50.0 - epsilon)) < 1e-9)
    }

    @Test
    fun orientedFormulas() {
        val w = 4.0
        val h = 3.0
        val extent = CGRect(0.0, 0.0, w, h)
        val point = CGPoint(1.0, 2.0)
        val x = point.x
        val y = point.y
        val expected = mapOf(
            CGImagePropertyOrientation.up to CGPoint(x, y),
            CGImagePropertyOrientation.upMirrored to CGPoint(w - x, y),
            CGImagePropertyOrientation.down to CGPoint(w - x, h - y),
            CGImagePropertyOrientation.downMirrored to CGPoint(x, h - y),
            CGImagePropertyOrientation.leftMirrored to CGPoint(h - y, w - x),
            CGImagePropertyOrientation.right to CGPoint(y, w - x),
            CGImagePropertyOrientation.rightMirrored to CGPoint(y, x),
            CGImagePropertyOrientation.left to CGPoint(h - y, x),
        )
        for ((orientation, mapped) in expected) {
            assertEquals(mapped, point.applying(orientationTransform(orientation, extent)), orientation.name)
            val oriented = canvas(w, h).oriented(orientation).extent
            if (orientation.rawValue <= 4) {
                assertEquals(CGRect(0.0, 0.0, w, h), oriented, orientation.name)
            } else {
                assertEquals(CGRect(0.0, 0.0, h, w), oriented, orientation.name)
            }
        }
        val shifted = CGRect(10.0, 20.0, w, h)
        assertEquals(
            CGPoint(y, w - x),
            CGPoint(10.0 + x, 20.0 + y).applying(orientationTransform(CGImagePropertyOrientation.right, shifted))
        )
        assertEquals(
            CGRect(0.0, 0.0, h, w),
            canvas(w, h).translated(x = 10.0, y = 20.0).oriented(CGImagePropertyOrientation.left).extent
        )
    }

    @Test
    fun moveAllAlignments() {
        val streamSize = CGSize(1920.0, 1080.0)
        val image = canvas(200.0, 100.0)
        val expected = mapOf(
            SettingsAlignment.topLeft to CGRect(192.0, 1080.0 - 216.0 - 100.0 + 1, 200.0, 100.0),
            SettingsAlignment.topCenter to CGRect(860.0, 1080.0 - 216.0 - 100.0 + 1, 200.0, 100.0),
            SettingsAlignment.topRight to CGRect(1920.0 - 192.0 - 200.0 + 1, 1080.0 - 216.0 - 100.0 + 1, 200.0, 100.0),
            SettingsAlignment.leftCenter to CGRect(192.0, 490.0, 200.0, 100.0),
            SettingsAlignment.center to CGRect(860.0, 490.0, 200.0, 100.0),
            SettingsAlignment.rightCenter to CGRect(1920.0 - 192.0 - 200.0 + 1, 490.0, 200.0, 100.0),
            SettingsAlignment.bottomLeft to CGRect(192.0, 216.0, 200.0, 100.0),
            SettingsAlignment.bottomCenter to CGRect(860.0, 216.0, 200.0, 100.0),
            SettingsAlignment.bottomRight to CGRect(1920.0 - 192.0 - 200.0 + 1, 216.0, 200.0, 100.0),
        )
        for ((alignment, rect) in expected) {
            assertEquals(rect, image.move(layout(alignment), streamSize).extent, alignment.name)
        }
        assertEquals(
            CGRect(1721.0, 981.0, 200.0, 100.0),
            canvas(200.0, 100.0).move(layout(SettingsAlignment.topRight, 0.0, 0.0), streamSize).extent
        )
        val flush = canvas(200.0, 100.0).translated(x = 1720.0, y = 980.0)
        assertEquals(
            CGRect(1720.0, 980.0, 200.0, 100.0),
            flush.move(layout(SettingsAlignment.topRight, 0.0, 0.0), streamSize).extent
        )
        val offset = canvas(200.0, 100.0).translated(x = 5.0, y = 7.0)
        assertEquals(
            CGRect(192.0, 216.0, 200.0, 100.0),
            offset.move(layout(SettingsAlignment.bottomLeft), streamSize).extent
        )
    }

    @Test
    fun resizeMirrorMoveStaysInCanvas() {
        val streamSize = CGSize(1920.0, 1080.0)
        val background = canvas(1920.0, 1080.0)
        val widget = canvas(400.0, 300.0)
        for (mirror in listOf(false, true)) {
            val layout = layout(SettingsAlignment.topLeft, 0.0, 0.0)
            layout.size = 50.0
            val resized = widget.resizeMirror(layout, streamSize, mirror)
            assertEquals(CGRect(0.0, 0.0, 720.0, 540.0), resized.extent)
            val placed = resized.move(layout, streamSize).cropped(to = background.extent)
            assertEquals(CGRect(0.0, 541.0, 720.0, 539.0), placed.extent)
            assertEquals(background.extent, placed.composited(over = background).extent)
        }
    }

    @Test
    fun tripleAndTwinKeepCanvasExtent() {
        for (size in listOf(CGSize(1920.0, 1080.0), CGSize(1280.0, 720.0), CGSize(1080.0, 1920.0))) {
            val image = canvas(size.width, size.height)
            assertEquals(image.extent, triple(image).extent, "triple $size")
            assertEquals(image.extent, twin(image).extent, "twin $size")
        }
    }

    @Test
    fun affineTransforms() {
        val translate = CGAffineTransform(translationX = 10.0, y = 20.0)
        val scale = CGAffineTransform(scaleX = 2.0, y = 3.0)
        val point = CGPoint(1.0, 1.0)
        assertEquals(CGPoint(22.0, 63.0), point.applying(translate.concatenating(scale)))
        assertEquals(CGPoint(12.0, 23.0), point.applying(scale.concatenating(translate)))
        assertEquals(point, point.applying(scale).applying(scale.inverted()))
        assertEquals(CGPoint(12.0, 23.0), point.applying(translate.scaledBy(x = 2.0, y = 3.0)))
        assertEquals(CGPoint(21.0, 41.0), point.applying(translate.translatedBy(x = 11.0, y = 21.0).translatedBy(x = -11.0, y = -21.0).translatedBy(x = 10.0, y = 20.0)))
        val rotation = CGAffineTransform(rotationAngle = PI / 2)
        val rotated = CGPoint(1.0, 0.0).applying(rotation)
        assertTrue(abs(rotated.x) < 1e-12)
        assertTrue(abs(rotated.y - 1.0) < 1e-12)
        assertTrue(CGAffineTransform.identity.isIdentity)
    }
}
