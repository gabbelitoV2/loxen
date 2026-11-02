package com.moblin.android.view.controlbar.quickbutton.sceneeditor

import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.various.settings.SettingsAlignment
import com.moblin.android.various.settings.SettingsWidgetLayout
import com.moblin.android.videoeffects.layoutPosition
import java.util.UUID
import kotlin.math.abs

data class SceneEditorSnap(var position: Double, var guide: Double? = null)

fun sceneEditorSnapAxis(
    position: Double,
    length: Double,
    total: Double,
    threshold: Double,
    canCenter: Boolean,
    near: Boolean,
    targets: List<Double>,
): SceneEditorSnap {
    val positions: ClosedFloatingPointRange<Double> = if (near) {
        0.0..total
    } else {
        -length..(total - length)
    }
    val clampedPosition = position.coerceAtLeast(positions.start).coerceAtMost(positions.endInclusive)
    val candidates = mutableListOf(
        SceneEditorSnap(position = 0.0),
        SceneEditorSnap(position = total - length),
    )
    if (canCenter) {
        candidates.add(SceneEditorSnap(position = (total - length) / 2, guide = total / 2))
    }
    for (target in targets) {
        for (anchor in listOf(0.0, length / 2, length)) {
            candidates.add(SceneEditorSnap(position = target - anchor, guide = target))
        }
    }
    val closest = candidates
        .filter { positions.contains(it.position) }
        .minByOrNull { abs(it.position - clampedPosition) }
    if (closest != null && abs(closest.position - clampedPosition) < threshold) {
        return closest
    }
    return SceneEditorSnap(position = clampedPosition)
}

data class SceneEditorResizeSnap(val factor: Double, val distance: Double, val guide: Double)

fun sceneEditorSnapResizeAxis(
    edges: List<Double>,
    fixed: Double,
    factor: Double,
    factors: ClosedFloatingPointRange<Double>,
    threshold: Double,
    targets: List<Double>,
): SceneEditorResizeSnap? {
    var closest: SceneEditorResizeSnap? = null
    for (edge in edges) {
        if (abs(edge - fixed) <= 1) {
            continue
        }
        for (target in targets) {
            val snapFactor = (target - fixed) / (edge - fixed)
            val distance = abs(snapFactor - factor)
            if (distance < threshold &&
                factors.contains(snapFactor) &&
                distance < (closest?.distance ?: Double.POSITIVE_INFINITY) - 1e-9
            ) {
                closest = SceneEditorResizeSnap(factor = snapFactor, distance = distance, guide = target)
            }
        }
    }
    return closest
}

fun sceneEditorHandlePoint(rect: CGRect, alignment: SettingsAlignment): CGPoint {
    return CGPoint(
        x = if (alignment.mirrorPositionHorizontally()) rect.minX else rect.maxX,
        y = if (alignment.mirrorPositionVertically()) rect.minY else rect.maxY,
    )
}

fun sceneEditorFixedPoint(rect: CGRect, alignment: SettingsAlignment): CGPoint {
    return CGPoint(
        x = if (alignment.isHorizontalCenter()) {
            rect.midX
        } else if (alignment.isLeft()) {
            rect.minX
        } else {
            rect.maxX
        },
        y = if (alignment.isVerticalCenter()) {
            rect.midY
        } else if (alignment.isTop()) {
            rect.minY
        } else {
            rect.maxY
        },
    )
}

fun sceneEditorResizedRect(rect: CGRect, fixed: CGPoint, factor: Double): CGRect {
    return CGRect(
        x = fixed.x + (rect.minX - fixed.x) * factor,
        y = fixed.y + (rect.minY - fixed.y) * factor,
        width = rect.width * factor,
        height = rect.height * factor,
    )
}

private fun layoutOffsetFromPosition(
    position: Double,
    size: Double,
    streamSize: Double,
    isNear: Boolean,
): Double {
    return if (isNear) {
        position * 100 / streamSize
    } else {
        (streamSize - position - size) * 100 / streamSize
    }
}

fun sceneEditorLayout(
    layout: SettingsWidgetLayout,
    rect: CGRect,
    canvasSize: CGSize,
): SettingsWidgetLayout {
    var newLayout = layout
    val alignment = newLayout.alignment
    if (!alignment.isHorizontalCenter()) {
        newLayout = newLayout.copy(
            x = layoutOffsetFromPosition(rect.minX, rect.width, canvasSize.width, alignment.isLeft()),
        ).updatingXString()
    }
    if (!alignment.isVerticalCenter()) {
        newLayout = newLayout.copy(
            y = layoutOffsetFromPosition(rect.minY, rect.height, canvasSize.height, alignment.isTop()),
        ).updatingYString()
    }
    return newLayout
}

fun sceneEditorAlignedLayout(
    layout: SettingsWidgetLayout,
    alignment: SettingsAlignment,
    size: CGSize,
    canvasSize: CGSize,
): SettingsWidgetLayout {
    val rect = CGRect(
        origin = layoutPosition(layout = layout, size = size, streamSize = canvasSize),
        size = size,
    )
    var newLayout = layout.copy(alignment = alignment)
    if (alignment.isHorizontalOrVerticalCenter()) {
        newLayout = newLayout.copy(positioningLock = false)
    }
    return sceneEditorLayout(newLayout, rect, canvasSize)
}

fun sceneEditorTapSelection(hitIds: List<UUID>, selectedId: UUID?, sameLocation: Boolean): UUID? {
    val index = hitIds.indexOfFirst { it == selectedId }
    if (index < 0) {
        return hitIds.firstOrNull()
    }
    return if (sameLocation) {
        if (index + 1 < hitIds.size) hitIds[index + 1] else null
    } else {
        if (index == 0) null else hitIds.firstOrNull()
    }
}
