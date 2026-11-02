package com.moblin.android.view.controlbar.quickbutton.sceneeditor

import com.moblin.android.areEqual
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.various.settings.SettingsAlignment
import com.moblin.android.various.settings.SettingsWidgetLayout
import com.moblin.android.videoeffects.layoutPosition
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val canvasSize = CGSize(width = 1920.0, height = 1080.0)

private fun makeLayout(alignment: SettingsAlignment): SettingsWidgetLayout {
    var layout = SettingsWidgetLayout()
    layout = layout.copy(alignment = alignment)
    return layout
}

private fun isEqual(actual: CGPoint, expected: CGPoint): Boolean =
    areEqual(doubleArrayOf(actual.x, actual.y), doubleArrayOf(expected.x, expected.y), 0.001)

@RunWith(RobolectricTestRunner::class)
class SceneEditorGeometrySuite {
    @Test
    fun layoutPositionMatchesRect() {
        val rects = listOf(
            CGRect(x = 500.0, y = 300.0, width = 400.0, height = 300.0),
            CGRect(x = 0.0, y = 780.0, width = 400.0, height = 300.0),
            CGRect(x = 1520.0, y = 0.0, width = 400.0, height = 300.0),
        )
        val alignments = listOf(
            SettingsAlignment.topLeft,
            SettingsAlignment.topRight,
            SettingsAlignment.bottomLeft,
            SettingsAlignment.bottomRight,
        )
        for (alignment in alignments) {
            for (rect in rects) {
                val layout = sceneEditorLayout(makeLayout(alignment), rect, canvasSize)
                assertEquals(alignment, layout.alignment)
                assertTrue(isEqual(layoutPosition(layout, rect.size, canvasSize), rect.origin))
                assertTrue(layout.x in 0.0..100.0)
                assertTrue(layout.y in 0.0..100.0)
            }
        }
    }

    @Test
    fun moveKeepsAlignment() {
        val rect = CGRect(x = 1500.0, y = 780.0, width = 400.0, height = 300.0)
        val layout = sceneEditorLayout(makeLayout(SettingsAlignment.topLeft), rect, canvasSize)
        assertEquals(SettingsAlignment.topLeft, layout.alignment)
        assertTrue(isEqual(layoutPosition(layout, rect.size, canvasSize), rect.origin))
    }

    @Test
    fun moveKeepsCenteredAxis() {
        var layout = makeLayout(SettingsAlignment.topCenter)
        layout = layout.copy(x = 7.0)
        layout = sceneEditorLayout(layout, CGRect(x = 100.0, y = 200.0, width = 400.0, height = 300.0), canvasSize)
        assertEquals(SettingsAlignment.topCenter, layout.alignment)
        assertTrue(layout.x == 7.0)
        assertTrue(
            isEqual(
                layoutPosition(layout, CGSize(width = 400.0, height = 300.0), canvasSize),
                CGPoint(x = 760.0, y = 200.0),
            ),
        )
    }

    @Test
    fun alignCenteredWidgetKeepsPosition() {
        val rect = CGRect(x = 760.0, y = 390.0, width = 400.0, height = 300.0)
        for (alignment in SettingsAlignment.entries) {
            val layout = sceneEditorLayout(makeLayout(SettingsAlignment.topLeft), rect, canvasSize)
            val aligned = sceneEditorAlignedLayout(layout, alignment, rect.size, canvasSize)
            assertEquals(alignment, aligned.alignment)
            assertTrue(aligned.size == layout.size)
            assertTrue(isEqual(layoutPosition(aligned, rect.size, canvasSize), rect.origin))
        }
    }

    @Test
    fun alignCornerWidgetKeepsPosition() {
        val sizes = listOf(CGSize(width = 400.0, height = 300.0), CGSize.zero)
        val alignments = listOf(
            SettingsAlignment.topLeft,
            SettingsAlignment.topRight,
            SettingsAlignment.bottomLeft,
            SettingsAlignment.bottomRight,
        )
        for (alignment in alignments) {
            for (size in sizes) {
                val rect = CGRect(origin = CGPoint(x = 100.0, y = 150.0), size = size)
                val layout = sceneEditorLayout(makeLayout(SettingsAlignment.bottomRight), rect, canvasSize)
                val aligned = sceneEditorAlignedLayout(layout, alignment, size, canvasSize)
                assertTrue(isEqual(layoutPosition(aligned, size, canvasSize), rect.origin))
            }
        }
    }

    @Test
    fun alignCenteredClearsPositioningLock() {
        var layout = makeLayout(SettingsAlignment.topLeft)
        layout = layout.copy(positioningLock = true)
        assertFalse(sceneEditorAlignedLayout(layout, SettingsAlignment.leftCenter, CGSize.zero, canvasSize).positioningLock)
        assertTrue(sceneEditorAlignedLayout(layout, SettingsAlignment.bottomRight, CGSize.zero, canvasSize).positioningLock)
    }

    @Test
    fun moveWithPositioningLockKeepsCorner() {
        var layout = makeLayout(SettingsAlignment.topLeft)
        layout = layout.copy(positioningLock = true)
        val rect = CGRect(x = 1500.0, y = 780.0, width = 400.0, height = 300.0)
        layout = sceneEditorLayout(layout, rect, canvasSize)
        assertEquals(SettingsAlignment.topLeft, layout.alignment)
        assertTrue(layout.positioningLock)
        assertTrue(isEqual(layoutPosition(layout, rect.size, canvasSize), rect.origin))
    }

    @Test
    fun resizeKeepsAlignedPoint() {
        val rect = CGRect(x = 500.0, y = 300.0, width = 400.0, height = 300.0)
        for (alignment in SettingsAlignment.entries) {
            val fixed = sceneEditorFixedPoint(rect, alignment)
            val resized = sceneEditorResizedRect(rect, fixed, 1.5)
            assertTrue(isEqual(sceneEditorFixedPoint(resized, alignment), fixed))
            assertTrue(resized.size.width == 600.0 && resized.size.height == 450.0)
        }
    }

    @Test
    fun handleIsOppositeAlignedCorner() {
        val rect = CGRect(x = 500.0, y = 300.0, width = 400.0, height = 300.0)
        assertTrue(isEqual(sceneEditorHandlePoint(rect, SettingsAlignment.topLeft), CGPoint(x = 900.0, y = 600.0)))
        assertTrue(isEqual(sceneEditorHandlePoint(rect, SettingsAlignment.topRight), CGPoint(x = 500.0, y = 600.0)))
        assertTrue(isEqual(sceneEditorHandlePoint(rect, SettingsAlignment.bottomLeft), CGPoint(x = 900.0, y = 300.0)))
        assertTrue(isEqual(sceneEditorHandlePoint(rect, SettingsAlignment.bottomRight), CGPoint(x = 500.0, y = 300.0)))
        assertTrue(isEqual(sceneEditorHandlePoint(rect, SettingsAlignment.center), CGPoint(x = 900.0, y = 600.0)))
    }

    @Test
    fun snapAxisKeepsOffsetWithinBounds() {
        assertEquals(0.0, sceneEditorSnapAxis(-300.0, 400.0, 1920.0, 10.0, true, true, emptyList()).position, 0.001)
        assertEquals(1800.0, sceneEditorSnapAxis(1800.0, 400.0, 1920.0, 10.0, true, true, emptyList()).position, 0.001)
        assertEquals(1920.0, sceneEditorSnapAxis(2500.0, 400.0, 1920.0, 10.0, true, true, emptyList()).position, 0.001)
        assertEquals(-300.0, sceneEditorSnapAxis(-300.0, 400.0, 1920.0, 10.0, true, false, emptyList()).position, 0.001)
        assertEquals(-400.0, sceneEditorSnapAxis(-500.0, 400.0, 1920.0, 10.0, true, false, emptyList()).position, 0.001)
        assertEquals(1520.0, sceneEditorSnapAxis(1800.0, 400.0, 1920.0, 10.0, true, false, emptyList()).position, 0.001)
        assertEquals(300.0, sceneEditorSnapAxis(300.0, 2000.0, 1920.0, 10.0, true, true, emptyList()).position, 0.001)
    }

    @Test
    fun snapAxisBoundsMatchLayoutBounds() {
        val size = CGSize(width = 400.0, height = 300.0)
        val alignments = listOf(
            SettingsAlignment.topLeft,
            SettingsAlignment.topRight,
            SettingsAlignment.bottomLeft,
            SettingsAlignment.bottomRight,
        )
        for (alignment in alignments) {
            val positions = listOf(CGPoint(x = -1000.0, y = -1000.0), CGPoint(x = 3000.0, y = 3000.0))
            for (position in positions) {
                val x = sceneEditorSnapAxis(position.x, size.width, 1920.0, 10.0, true, alignment.isLeft(), emptyList())
                val y = sceneEditorSnapAxis(position.y, size.height, 1080.0, 10.0, true, alignment.isTop(), emptyList())
                val rect = CGRect(origin = CGPoint(x = x.position, y = y.position), size = size)
                val layout = sceneEditorLayout(makeLayout(alignment), rect, canvasSize)
                assertTrue(layout.x in 0.0..100.0)
                assertTrue(layout.y in 0.0..100.0)
                assertTrue(layout.x == 0.0 || layout.x == 100.0)
                assertTrue(layout.y == 0.0 || layout.y == 100.0)
            }
        }
    }

    @Test
    fun snapAxisSnapsToEdgesAndCenter() {
        assertEquals(0.0, sceneEditorSnapAxis(-5.0, 400.0, 1920.0, 10.0, true, true, emptyList()).position, 0.001)
        assertEquals(1520.0, sceneEditorSnapAxis(1525.0, 400.0, 1920.0, 10.0, true, true, emptyList()).position, 0.001)
        val center = sceneEditorSnapAxis(765.0, 400.0, 1920.0, 10.0, true, true, emptyList())
        assertEquals(760.0, center.position, 0.001)
        assertTrue(center.guide == 960.0)
        assertEquals(765.0, sceneEditorSnapAxis(765.0, 400.0, 1920.0, 10.0, false, true, emptyList()).position, 0.001)
    }

    @Test
    fun snapAxisSnapsToOtherWidgets() {
        val snap = sceneEditorSnapAxis(1003.0, 400.0, 1920.0, 10.0, true, true, listOf(1000.0))
        assertEquals(1000.0, snap.position, 0.001)
        assertTrue(snap.guide == 1000.0)
    }

    @Test
    fun tapSelectsTopWidget() {
        val small = UUID.randomUUID()
        val big = UUID.randomUUID()
        assertEquals(small, sceneEditorTapSelection(listOf(small, big), null, false))
        assertEquals(small, sceneEditorTapSelection(listOf(small, big), big, false))
        assertNull(sceneEditorTapSelection(emptyList(), big, false))
    }

    @Test
    fun tapOnSelectedInFrontDeselects() {
        val small = UUID.randomUUID()
        val big = UUID.randomUUID()
        assertNull(sceneEditorTapSelection(listOf(small, big), small, false))
        assertNull(sceneEditorTapSelection(listOf(big), big, false))
    }

    @Test
    fun tapAtSameLocationCycles() {
        val small = UUID.randomUUID()
        val medium = UUID.randomUUID()
        val big = UUID.randomUUID()
        val hits = listOf(small, medium, big)
        assertEquals(medium, sceneEditorTapSelection(hits, small, true))
        assertEquals(big, sceneEditorTapSelection(hits, medium, true))
        assertNull(sceneEditorTapSelection(hits, big, true))
        assertEquals(small, sceneEditorTapSelection(hits, null, true))
    }
}
