package com.moblin.android.view.controlbar.quickbutton.sceneeditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.swiftui.Button
import com.moblin.android.platform.swiftui.ButtonRole
import com.moblin.android.platform.swiftui.ConfirmationDialog
import com.moblin.android.platform.swiftui.Divider
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.NavigationStack
import com.moblin.android.platform.swiftui.NavigationToolbar
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.Visibility
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.view.settings.scenes.scene.SceneWidgetSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSheetView
import com.moblin.android.view.settings.settingsHalfWidth
import com.moblin.android.view.stream.StreamPreviewView
import com.moblin.android.view.stream.WidgetOutlineLabelView
import com.moblin.android.view.stream.widgetOutlineViewRect
import com.moblin.android.view.utils.IconAndTextView
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.SceneEditor
import com.moblin.android.various.model.SceneSelector
import com.moblin.android.various.model.StreamOverlay
import com.moblin.android.various.model.WidgetOutline
import com.moblin.android.various.model.WidgetOutlines
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsAlignment
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsSceneCameraPosition
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidget
import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.flow.MutableStateFlow
import com.moblin.android.LocalModel
import com.moblin.android.various.model.appendWidgetToScene
import com.moblin.android.various.model.findWidget
import com.moblin.android.various.model.sceneEditorMoveWidget
import com.moblin.android.various.model.sceneEditorMoveWidgetInDrawOrder
import com.moblin.android.various.model.sceneEditorPresentationChanged
import com.moblin.android.various.model.sceneEditorRemoveWidget
import com.moblin.android.various.model.sceneEditorResizeWidget
import com.moblin.android.various.model.sceneEditorScene
import com.moblin.android.various.model.sceneEditorSelectScene
import com.moblin.android.various.model.sceneEditorToggleWidgetEnabled

private enum class SceneEditorEditMode {
    move,
    resize,
}

private data class SceneEditorEdit(
    val outline: WidgetOutline,
    val mode: SceneEditorEditMode,
    val startSize: Double,
    val rect: CGRect,
    val snapX: SceneEditorSnap? = null,
    val snapY: SceneEditorSnap? = null,
)

private data class SceneEditorSelectedWidget(
    val sceneWidget: SettingsSceneWidget,
    val widget: SettingsWidget,
)

private class SceneEditorSelection {
    val showingSettings = MutableStateFlow(false)
    val sceneWidgetIdFlow = MutableStateFlow<UUID?>(null)

    var sceneWidgetId: UUID?
        get() = sceneWidgetIdFlow.value
        set(value) {
            sceneWidgetIdFlow.value = value
            if (value == null) {
                showingSettings.value = false
            }
        }
}

private data class SceneEditorSnap(
    val position: Double,
    val centered: Boolean = false,
    val guide: Double? = null,
)

private fun snapAxis(
    position: Double,
    length: Double,
    total: Double,
    threshold: Double,
    canCenter: Boolean,
    targets: List<Double>,
): SceneEditorSnap {
    val maximum = max(total - length, 0.0)
    val positionClamped = position.coerceIn(0.0, maximum)
    val candidates = mutableListOf(
        SceneEditorSnap(position = 0.0),
        SceneEditorSnap(position = maximum),
    )
    if (canCenter) {
        candidates.add(SceneEditorSnap(position = maximum / 2, centered = true, guide = total / 2))
    }
    for (target in targets) {
        for (anchor in listOf(0.0, length / 2, length)) {
            if (target - anchor >= 0.0 && target - anchor <= maximum) {
                candidates.add(SceneEditorSnap(position = target - anchor, guide = target))
            }
        }
    }
    val closest = candidates.minByOrNull { abs(it.position - positionClamped) }
    if (closest != null && abs(closest.position - positionClamped) < threshold) {
        return closest
    }
    return SceneEditorSnap(position = positionClamped)
}

private data class SceneEditorResizeSnap(
    val factor: Double,
    val distance: Double,
    val guide: Double,
)

private fun snapResizeAxis(
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
            val snapDistance = abs(snapFactor - factor)
            if (snapDistance < threshold &&
                snapFactor >= factors.start && snapFactor <= factors.endInclusive &&
                snapDistance < (closest?.distance ?: Double.POSITIVE_INFINITY) - 1e-9
            ) {
                closest = SceneEditorResizeSnap(factor = snapFactor, distance = snapDistance, guide = target)
            }
        }
    }
    return closest
}

private fun distance(first: CGPoint, second: CGPoint): Double =
    hypot(first.x - second.x, first.y - second.y)

private fun movesDiagonally(layout: com.moblin.android.various.settings.SettingsWidgetLayout): Boolean =
    layout.positioningLock && !layout.alignment.isHorizontalOrVerticalCenter()

private fun diagonalDirection(alignment: SettingsAlignment): CGSize =
    CGSize(width = if (alignment.isLeft()) 1.0 else -1.0, height = if (alignment.isTop()) 1.0 else -1.0)

@Composable
private fun SceneEditorCanvasView(
    model: Model = LocalModel.current,
    widgetOutlines: WidgetOutlines,
    mirror: Boolean,
    size: CGSize,
    selection: SceneEditorSelection,
) {
    val palette = formPalette()
    val allOutlines by widgetOutlines.outlines.collectAsState()
    val canvasSize by widgetOutlines.canvasSize.collectAsState()
    val selectedId by selection.sceneWidgetIdFlow.collectAsState()
    var edit by remember { mutableStateOf<SceneEditorEdit?>(null) }

    val scale = if (size.width != 0.0) canvasSize.width / size.width else 1.0
    val snapThreshold = 8 * scale
    val editableOutlines = allOutlines.filter { it.inScene }
    val selectedOutline = editableOutlines.firstOrNull { it.id == selectedId }
    val selectedFill = palette.accent.copy(alpha = 0.25f)

    fun viewRect(rect: CGRect): CGRect = widgetOutlineViewRect(rect, canvasSize, mirror, size)

    fun viewPoint(point: CGPoint): CGPoint = viewRect(CGRect(point.x, point.y, 0.0, 0.0)).origin

    fun canvasDelta(translation: CGSize): CGSize = CGSize(
        width = translation.width * scale * (if (mirror) -1 else 1),
        height = translation.height * scale,
    )

    fun currentRect(outline: WidgetOutline): CGRect {
        val currentEdit = edit
        if (currentEdit != null && currentEdit.outline.id == outline.id) {
            return currentEdit.rect
        }
        return outline.rect
    }

    fun hitRect(outline: WidgetOutline): CGRect {
        val rect = viewRect(outline.rect)
        if (outline.sized) {
            return rect.insetBy(
                dx = min(0.0, (rect.width - 44) / 2),
                dy = min(0.0, (rect.height - 44) / 2),
            )
        }
        val alignment = outline.sceneWidget.layout.alignment
        val width = 120.0
        val height = 60.0
        val x = if (alignment.isHorizontalCenter()) {
            rect.minX - width / 2
        } else if (alignment.isLeft() != mirror) {
            rect.minX - 22
        } else {
            rect.minX - width + 22
        }
        val y = if (alignment.isVerticalCenter()) {
            rect.minY - height / 2
        } else if (alignment.isTop()) {
            rect.minY - 22
        } else {
            rect.minY - height + 22
        }
        return CGRect(x = x, y = y, width = width, height = height)
    }

    fun outlinesAt(location: CGPoint): List<WidgetOutline> = editableOutlines
        .asReversed()
        .map { it to hitRect(it) }
        .filter { it.second.contains(location) }
        .sortedBy { it.second.width * it.second.height }
        .map { it.first }

    fun outlineForTap(location: CGPoint): WidgetOutline? {
        val hits = outlinesAt(location)
        val index = hits.indexOfFirst { it.id == selectedId }
        if (index < 0) {
            return hits.firstOrNull()
        }
        if (index + 1 >= hits.size) {
            return null
        }
        return hits[index + 1]
    }

    fun isResizable(outline: WidgetOutline): Boolean = outline.sized && outline.widget.hasSize()

    fun fixedPoint(outline: WidgetOutline): CGPoint {
        val rect = outline.rect
        val alignment = outline.sceneWidget.layout.alignment
        val x = if (alignment.isHorizontalCenter()) rect.midX else if (alignment.isLeft()) rect.minX else rect.maxX
        val y = if (alignment.isVerticalCenter()) rect.midY else if (alignment.isTop()) rect.minY else rect.maxY
        return CGPoint(x = x, y = y)
    }

    fun handlePoint(outline: WidgetOutline, rect: CGRect): CGPoint {
        val alignment = outline.sceneWidget.layout.alignment
        val x = if (alignment.mirrorPositionHorizontally()) rect.minX else rect.maxX
        val y = if (alignment.mirrorPositionVertically()) rect.minY else rect.maxY
        return CGPoint(x = x, y = y)
    }

    fun startEdit(outline: WidgetOutline, mode: SceneEditorEditMode) {
        edit = SceneEditorEdit(
            outline = outline,
            mode = mode,
            startSize = outline.sceneWidget.layout.size,
            rect = outline.rect,
        )
    }

    fun diagonalPosition(
        alignment: SettingsAlignment,
        widgetSize: CGSize,
        position: CGPoint,
        threshold: Double,
    ): CGPoint {
        val direction = diagonalDirection(alignment)
        val start = CGPoint(
            x = if (alignment.isLeft()) 0.0 else canvasSize.width - widgetSize.width,
            y = if (alignment.isTop()) 0.0 else canvasSize.height - widgetSize.height,
        )
        val maximum = max(min(canvasSize.width - widgetSize.width, canvasSize.height - widgetSize.height), 0.0)
        var offset = ((position.x - start.x) * direction.width + (position.y - start.y) * direction.height) / 2
        offset = offset.coerceIn(0.0, maximum)
        if (offset < threshold) {
            offset = 0.0
        }
        return CGPoint(x = start.x + direction.width * offset, y = start.y + direction.height * offset)
    }

    fun snapTargetsX(outline: WidgetOutline): List<Double> =
        if (outline.sized) listOf(outline.rect.minX, outline.rect.midX, outline.rect.maxX) else listOf(outline.rect.minX)

    fun snapTargetsY(outline: WidgetOutline): List<Double> =
        if (outline.sized) listOf(outline.rect.minY, outline.rect.midY, outline.rect.maxY) else listOf(outline.rect.minY)

    fun moveChanged(value: CGSize) {
        if (edit == null) {
            val outline = selectedOutline ?: return
            startEdit(outline, SceneEditorEditMode.move)
        }
        var currentEdit = edit ?: return
        if (currentEdit.mode != SceneEditorEditMode.move) {
            return
        }
        val delta = canvasDelta(value)
        val rect = currentEdit.outline.rect
        val position = CGPoint(x = rect.minX + delta.width, y = rect.minY + delta.height)
        val layout = currentEdit.outline.sceneWidget.layout
        if (movesDiagonally(layout)) {
            val moved = diagonalPosition(layout.alignment, rect.size, position, snapThreshold)
            currentEdit = currentEdit.copy(rect = currentEdit.rect.copy(x = moved.x, y = moved.y))
        } else {
            val others = editableOutlines.filter { it.id != currentEdit.outline.id }
            val x = snapAxis(
                position = position.x,
                length = rect.width,
                total = canvasSize.width,
                threshold = snapThreshold,
                canCenter = currentEdit.outline.sized,
                targets = others.flatMap { snapTargetsX(it) },
            )
            val y = snapAxis(
                position = position.y,
                length = rect.height,
                total = canvasSize.height,
                threshold = snapThreshold,
                canCenter = currentEdit.outline.sized,
                targets = others.flatMap { snapTargetsY(it) },
            )
            currentEdit = currentEdit.copy(
                rect = currentEdit.rect.copy(x = x.position, y = y.position),
                snapX = x,
                snapY = y,
            )
        }
        edit = currentEdit
        model.sceneEditorMoveWidget(
            sceneWidget = currentEdit.outline.sceneWidget,
            position = currentEdit.rect.origin,
            size = rect.size,
            centeredHorizontal = currentEdit.snapX?.centered ?: false,
            centeredVertical = currentEdit.snapY?.centered ?: false,
        )
    }

    fun resize(factor: Double) {
        var currentEdit = edit ?: return
        if (currentEdit.mode != SceneEditorEditMode.resize) {
            return
        }
        val factors = (1 / currentEdit.startSize)..(100 / currentEdit.startSize)
        var newFactor = factor.coerceIn(factors)
        val rect = currentEdit.outline.rect
        val fixed = fixedPoint(currentEdit.outline)
        val others = editableOutlines.filter { it.id != currentEdit.outline.id }
        val threshold = snapThreshold / distance(handlePoint(currentEdit.outline, rect), fixed)
        val x = snapResizeAxis(
            edges = listOf(rect.minX, rect.maxX, rect.midX),
            fixed = fixed.x,
            factor = newFactor,
            factors = factors,
            threshold = threshold,
            targets = listOf(0.0, canvasSize.width / 2, canvasSize.width) + others.flatMap { snapTargetsX(it) },
        )
        val y = snapResizeAxis(
            edges = listOf(rect.minY, rect.maxY, rect.midY),
            fixed = fixed.y,
            factor = newFactor,
            factors = factors,
            threshold = threshold,
            targets = listOf(0.0, canvasSize.height / 2, canvasSize.height) + others.flatMap { snapTargetsY(it) },
        )
        currentEdit = currentEdit.copy(snapX = null, snapY = null)
        if (x != null && x.distance < (y?.distance ?: Double.POSITIVE_INFINITY)) {
            newFactor = x.factor
            currentEdit = currentEdit.copy(snapX = SceneEditorSnap(position = x.guide, guide = x.guide))
        } else if (y != null) {
            newFactor = y.factor
            currentEdit = currentEdit.copy(snapY = SceneEditorSnap(position = y.guide, guide = y.guide))
        }
        currentEdit = currentEdit.copy(
            rect = CGRect(
                x = fixed.x + (rect.minX - fixed.x) * newFactor,
                y = fixed.y + (rect.minY - fixed.y) * newFactor,
                width = rect.width * newFactor,
                height = rect.height * newFactor,
            ),
        )
        edit = currentEdit
        model.sceneEditorResizeWidget(
            sceneWidget = currentEdit.outline.sceneWidget,
            size = currentEdit.startSize * newFactor,
        )
    }

    fun handleChanged(outline: WidgetOutline, translation: CGSize) {
        if (edit == null) {
            startEdit(outline, SceneEditorEditMode.resize)
        }
        val editedOutline = edit?.outline ?: return
        val fixed = fixedPoint(editedOutline)
        val start = handlePoint(editedOutline, editedOutline.rect)
        val delta = canvasDelta(translation)
        val current = CGPoint(x = start.x + delta.width, y = start.y + delta.height)
        val startDistance = distance(start, fixed)
        if (startDistance <= 0) {
            return
        }
        resize(distance(current, fixed) / startDistance)
    }

    val onCanvasTap by rememberUpdatedState(newValue = { location: CGPoint ->
        selection.sceneWidgetId = outlineForTap(location)?.id
    })
    val onMoveDrag by rememberUpdatedState(newValue = { translation: CGSize ->
        moveChanged(translation)
    })
    val onHandleDrag by rememberUpdatedState(newValue = { outline: WidgetOutline, translation: CGSize ->
        handleChanged(outline, translation)
    })

    Box(
        modifier = Modifier
            .size(size.width.dp, size.height.dp)
            .clipToBounds()
            .pointerInput(Unit) {
                var translation = Offset.Zero
                detectDragGestures(
                    onDragStart = { translation = Offset.Zero },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        translation += dragAmount
                        onMoveDrag(
                            CGSize(
                                width = translation.x.toDp().value.toDouble(),
                                height = translation.y.toDp().value.toDouble(),
                            ),
                        )
                    },
                    onDragEnd = { edit = null },
                    onDragCancel = { edit = null },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { point ->
                    onCanvasTap(
                        CGPoint(
                            x = point.x.toDp().value.toDouble(),
                            y = point.y.toDp().value.toDouble(),
                        ),
                    )
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (outline in allOutlines) {
                val selected = outline.id == selectedId
                val rect = viewRect(currentRect(outline))
                val color = if (selected) palette.accent else Color.White
                val dash = if (outline.inScene && outline.enabled) null else floatArrayOf(4f, 4f)
                val strokeWidth = if (selected) 2.dp.toPx() else 1.dp.toPx()
                if (outline.sized) {
                    val topLeft = Offset(rect.minX.dp.toPx(), rect.minY.dp.toPx())
                    val rectSize = Size(rect.width.dp.toPx(), rect.height.dp.toPx())
                    if (selected) {
                        drawRect(color = selectedFill, topLeft = topLeft, size = rectSize)
                    }
                    drawRect(
                        color = color,
                        topLeft = topLeft,
                        size = rectSize,
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = dash?.let { values ->
                                PathEffect.dashPathEffect(values.map { it.dp.toPx() }.toFloatArray())
                            },
                        ),
                    )
                } else {
                    val center = Offset(rect.minX.dp.toPx(), rect.minY.dp.toPx())
                    val radius = 16.dp.toPx()
                    if (selected) {
                        drawCircle(color = selectedFill, radius = radius, center = center)
                    }
                    val circleDash = if (selected) dash else floatArrayOf(4f, 4f)
                    drawCircle(
                        color = color,
                        radius = radius,
                        center = center,
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = circleDash?.let { values ->
                                PathEffect.dashPathEffect(values.map { it.dp.toPx() }.toFloatArray())
                            },
                        ),
                    )
                    drawCircle(
                        color = color,
                        radius = (if (selected) 12.dp.toPx() else 8.dp.toPx()) / 2,
                        center = center,
                    )
                }
            }
            val currentEdit = edit
            if (currentEdit != null) {
                val guideX = currentEdit.snapX?.guide
                if (guideX != null) {
                    val a = viewPoint(CGPoint(guideX, 0.0))
                    val b = viewPoint(CGPoint(guideX, canvasSize.height))
                    drawLine(
                        color = Color.Yellow,
                        start = Offset(a.x.dp.toPx(), a.y.dp.toPx()),
                        end = Offset(b.x.dp.toPx(), b.y.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                val guideY = currentEdit.snapY?.guide
                if (guideY != null) {
                    val a = viewPoint(CGPoint(0.0, guideY))
                    val b = viewPoint(CGPoint(canvasSize.width, guideY))
                    drawLine(
                        color = Color.Yellow,
                        start = Offset(a.x.dp.toPx(), a.y.dp.toPx()),
                        end = Offset(b.x.dp.toPx(), b.y.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                val layout = currentEdit.outline.sceneWidget.layout
                if (currentEdit.mode == SceneEditorEditMode.move && movesDiagonally(layout)) {
                    val direction = diagonalDirection(layout.alignment)
                    val length = min(canvasSize.width, canvasSize.height)
                    val corner = CGPoint(
                        x = if (layout.alignment.isLeft()) 0.0 else canvasSize.width,
                        y = if (layout.alignment.isTop()) 0.0 else canvasSize.height,
                    )
                    val end = CGPoint(
                        x = corner.x + direction.width * length,
                        y = corner.y + direction.height * length,
                    )
                    val a = viewPoint(corner)
                    val b = viewPoint(end)
                    drawLine(
                        color = Color.Yellow,
                        start = Offset(a.x.dp.toPx(), a.y.dp.toPx()),
                        end = Offset(b.x.dp.toPx(), b.y.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
        }
        for (outline in allOutlines) {
            key(outline.id) {
                val rect = viewRect(currentRect(outline))
                val labelY = if (rect.minY > 24 && !outline.sized) rect.minY - 24 else rect.minY + 4
                Box(
                    modifier = Modifier.offset(x = (rect.minX + 4).dp, y = labelY.dp),
                ) {
                    WidgetOutlineLabelView(widget = outline.widget)
                }
            }
        }
        if (selectedOutline != null && isResizable(selectedOutline) && edit?.mode != SceneEditorEditMode.move) {
            val point = viewPoint(handlePoint(selectedOutline, currentRect(selectedOutline)))
            Box(
                modifier = Modifier
                    .offset(x = (point.x - 22).dp, y = (point.y - 22).dp)
                    .size(44.dp)
                    .pointerInput(selectedOutline.id) {
                        var translation = Offset.Zero
                        detectDragGestures(
                            onDragStart = { translation = Offset.Zero },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                translation += dragAmount
                                onHandleDrag(
                                    selectedOutline,
                                    CGSize(
                                        width = translation.x.toDp().value.toDouble(),
                                        height = translation.y.toDp().value.toDouble(),
                                    ),
                                )
                            },
                            onDragEnd = { edit = null },
                            onDragCancel = { edit = null },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    drawCircle(color = Color.White, radius = min(this.size.width, this.size.height) / 2)
                    drawCircle(color = palette.accent, radius = min(this.size.width, this.size.height) / 2 - 2.dp.toPx())
                }
            }
        }
    }
}

@Composable
private fun SceneEditorButtonView(image: String, role: ButtonRole? = null, action: () -> Unit) {
    Button(action = action, role = role) {
        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            SystemImage(name = image, fontSize = 20.sp)
        }
    }
}

@Composable
private fun SceneEditorEnableButtonView(model: Model = LocalModel.current, widget: SettingsWidget) {
    val enabled = widget.enabled
    SceneEditorButtonView(image = if (enabled) "eye" else "eye.slash") {
        model.sceneEditorToggleWidgetEnabled(widget = widget)
    }
}

@Composable
private fun SceneEditorPopoverRowView(
    image: String,
    text: String,
    checked: Boolean = false,
    action: () -> Unit,
) {
    Box(modifier = Modifier.padding(11.dp)) {
        Button(action = { action() }) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconAndTextView(image = image, text = text)
                Spacer(modifier = Modifier.weight(1f))
                if (checked) {
                    SystemImage(name = "checkmark", fontSize = 17.sp, tint = formPalette().accent)
                }
            }
        }
    }
}

@Composable
private fun SceneEditorPopoverListView(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(5.dp)
            .widthIn(min = 220.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            content()
        }
    }
}

@Composable
private fun SceneEditorScenesButtonView(
    model: Model = LocalModel.current,
    database: Database,
    scene: SettingsScene?,
    selection: SceneEditorSelection,
) {
    var presentingScenes by remember { mutableStateOf(false) }
    SceneEditorButtonView(image = "photo.on.rectangle") {
        presentingScenes = true
    }
    Sheet(isPresented = presentingScenes, onDismissRequest = { presentingScenes = false }) {
        SceneEditorPopoverListView {
            database.scenes.forEach { itemScene ->
                key(itemScene.id) {
                    CompositionLocalProvider(LocalTint provides formPalette().label) {
                        SceneEditorPopoverRowView(
                            image = if (itemScene.enabled) "photo.on.rectangle" else "eye.slash",
                            text = itemScene.name,
                            checked = itemScene === scene,
                        ) {
                            val widgetId = scene?.widgets
                                ?.firstOrNull { it.id == selection.sceneWidgetId }?.widgetId
                            selection.sceneWidgetId = itemScene.widgets.firstOrNull { it.widgetId == widgetId }?.id
                            model.sceneEditorSelectScene(id = itemScene.id)
                            presentingScenes = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SceneEditorAddWidgetButtonView(
    model: Model = LocalModel.current,
    database: Database,
    scene: SettingsScene,
    selection: SceneEditorSelection,
) {
    var presentingAddWidget by remember { mutableStateOf(false) }
    var presentingCreateWizard by remember { mutableStateOf(false) }
    SceneEditorButtonView(image = "plus") {
        presentingAddWidget = true
    }
    Sheet(isPresented = presentingAddWidget, onDismissRequest = { presentingAddWidget = false }) {
        SceneEditorPopoverListView {
            SceneEditorPopoverRowView(
                image = "plus.square.dashed",
                text = localized("Create widget"),
            ) {
                presentingAddWidget = false
                model.createWidgetWizard.reset(scene = scene)
                presentingCreateWizard = true
            }
            Divider()
            database.widgets.forEach { widget ->
                key(widget.id) {
                    CompositionLocalProvider(LocalTint provides formPalette().label) {
                        SceneEditorPopoverRowView(image = widget.image(), text = widget.name) {
                            model.appendWidgetToScene(scene = scene, widget = widget)
                            model.sceneSettingsPanelSceneId.value += 1
                            selection.sceneWidgetId = scene.widgets.lastOrNull()?.id
                            presentingAddWidget = false
                        }
                    }
                }
            }
        }
    }
    Sheet(isPresented = presentingCreateWizard, onDismissRequest = { presentingCreateWizard = false }) {
        WidgetWizardSheetView(
            model = model,
            database = database,
            presentingCreateWizard = presentingCreateWizard,
            onPresentingCreateWizardChange = { presentingCreateWizard = it },
        )
    }
}

@Composable
private fun SceneEditorToolbarDivider(vertical: Boolean) {
    val palette = formPalette()
    Box(
        modifier = Modifier
            .then(if (vertical) Modifier.width(32.dp).height(1.dp) else Modifier.width(1.dp).height(32.dp))
            .background(palette.gray),
    )
}

@Composable
private fun SceneEditorToolbarView(
    model: Model = LocalModel.current,
    database: Database,
    scene: SettingsScene?,
    selected: SceneEditorSelectedWidget?,
    selection: SceneEditorSelection,
    vertical: Boolean,
) {
    var presentingRemoveWidgetConfirmation by remember { mutableStateOf(false) }
    val palette = formPalette()

    val buttons: @Composable () -> Unit = {
        SceneEditorButtonView(image = "xmark") {
            model.presentingSceneEditor.value = false
        }
        SceneEditorToolbarDivider(vertical = vertical)
        SceneEditorScenesButtonView(model = model, database = database, scene = scene, selection = selection)
        if (scene != null) {
            if (selected != null) {
                SceneEditorToolbarDivider(vertical = vertical)
                SceneEditorEnableButtonView(model = model, widget = selected.widget)
                SceneEditorButtonView(image = "square.2.layers.3d.top.filled") {
                    model.sceneEditorMoveWidgetInDrawOrder(scene = scene, sceneWidget = selected.sceneWidget, up = true)
                }
                SceneEditorButtonView(image = "square.2.layers.3d.bottom.filled") {
                    model.sceneEditorMoveWidgetInDrawOrder(scene = scene, sceneWidget = selected.sceneWidget, up = false)
                }
                SceneEditorButtonView(image = "slider.horizontal.3") {
                    selection.showingSettings.value = !selection.showingSettings.value
                }
                CompositionLocalProvider(LocalTint provides palette.red) {
                    SceneEditorButtonView(image = "trash", role = ButtonRole.destructive) {
                        presentingRemoveWidgetConfirmation = true
                    }
                }
                ConfirmationDialog(
                    title = localized("Remove widget ${selected.widget.name} from scene ${scene.name}?"),
                    isPresented = presentingRemoveWidgetConfirmation,
                    onDismissRequest = { presentingRemoveWidgetConfirmation = false },
                    titleVisibility = Visibility.visible,
                ) {
                    Button(title = "Remove", role = ButtonRole.destructive) {
                        model.sceneEditorRemoveWidget(scene = scene, sceneWidget = selected.sceneWidget)
                        selection.sceneWidgetId = null
                    }
                }
            } else {
                SceneEditorAddWidgetButtonView(
                    model = model,
                    database = database,
                    scene = scene,
                    selection = selection,
                )
            }
        }
    }

    if (vertical) {
        Column(
            modifier = Modifier
                .width(52.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            buttons()
        }
    } else {
        Row(
            modifier = Modifier
                .height(52.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            buttons()
        }
    }
}

@Composable
private fun videoArea(
    model: Model = LocalModel.current,
    widgetOutlines: WidgetOutlines,
    scene: SettingsScene?,
    selection: SceneEditorSelection,
    mirror: Boolean,
) {
    val palette = formPalette()
    val canvasSize by widgetOutlines.canvasSize.collectAsState()

    fun fitSize(available: CGSize): CGSize {
        if (available.width <= 0 || available.height <= 0 || canvasSize.width <= 0 || canvasSize.height <= 0) {
            return CGSize.zero
        }
        val scale = min(available.width / canvasSize.width, available.height / canvasSize.height)
        return CGSize(width = canvasSize.width * scale, height = canvasSize.height * scale)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val availableWidth = maxWidth.value.toDouble()
        val availableHeight = maxHeight.value.toDouble()
        val size = fitSize(CGSize(width = availableWidth - 16, height = availableHeight - 16))
        Box(
            modifier = Modifier
                .size(size.width.dp, size.height.dp)
                .border(1.dp, palette.gray.copy(alpha = 0.5f)),
        ) {
            val preview = remember { StreamPreviewView(model = model) }
            preview.sceneEditor = true
            preview.body()
            if (scene != null && !scene.enabled) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                    Text(
                        text = localized("Disabled scene"),
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
            SceneEditorCanvasView(
                model = model,
                widgetOutlines = widgetOutlines,
                mirror = mirror,
                size = size,
                selection = selection,
            )
        }
    }
}

@Composable
private fun settingsPanel(
    model: Model = LocalModel.current,
    database: Database,
    selected: SceneEditorSelectedWidget,
    selection: SceneEditorSelection,
) {
    key(selected.sceneWidget.id) {
        NavigationStack {
            SceneWidgetSettingsView(
                model = model,
                database = database,
                sceneWidget = selected.sceneWidget,
                widget = selected.widget,
            )
            NavigationToolbar {
                Button(action = {
                    selection.showingSettings.value = false
                }) {
                    SystemImage(name = "xmark", fontSize = 17.sp)
                }
            }
        }
    }
}

@Composable
fun SceneEditorView(
    model: Model = LocalModel.current,
    database: Database,
    sceneSelector: SceneSelector,
    widgetOutlines: WidgetOutlines,
    streamOverlay: StreamOverlay,
    sceneEditor: SceneEditor,
) {
    val selection = remember { SceneEditorSelection() }
    val showingSettings by selection.showingSettings.collectAsState()
    val selectedSceneWidgetId by selection.sceneWidgetIdFlow.collectAsState()
    val isFrontCameraSelected by streamOverlay.isFrontCameraSelected.collectAsState()
    val canvasSize by widgetOutlines.canvasSize.collectAsState()

    fun mirror(scene: SettingsScene?): Boolean {
        if (database.mirrorFrontCameraOnStream) {
            return false
        }
        if (scene != null && !scene.enabled) {
            return scene.videoSource.cameraPosition == SettingsSceneCameraPosition.front
        }
        return isFrontCameraSelected
    }

    fun fitSize(available: CGSize): CGSize {
        if (available.width <= 0 || available.height <= 0 || canvasSize.width <= 0 || canvasSize.height <= 0) {
            return CGSize.zero
        }
        val scale = min(available.width / canvasSize.width, available.height / canvasSize.height)
        return CGSize(width = canvasSize.width * scale, height = canvasSize.height * scale)
    }

    fun selectedWidget(scene: SettingsScene?): SceneEditorSelectedWidget? {
        val sceneWidget = scene?.widgets?.firstOrNull { it.id == selectedSceneWidgetId } ?: return null
        val widget = model.findWidget(id = sceneWidget.widgetId) ?: return null
        return SceneEditorSelectedWidget(sceneWidget = sceneWidget, widget = widget)
    }

    val scene = model.sceneEditorScene()
    val selected = selectedWidget(scene)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val metricsWidth = maxWidth.value.toDouble()
        val metricsHeight = maxHeight.value.toDouble()
        val landscape = metricsWidth > metricsHeight
        LaunchedEffect(Unit) {
            model.sceneEditorPresentationChanged()
        }
        val preview = fitSize(CGSize(width = metricsWidth - 16, height = metricsHeight - 16))
        if (landscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    videoArea(
                        model = model,
                        widgetOutlines = widgetOutlines,
                        scene = scene,
                        selection = selection,
                        mirror = mirror(scene),
                    )
                }
                if (showingSettings && selected != null) {
                    Box(modifier = Modifier.width(settingsHalfWidth.dp)) {
                        settingsPanel(
                            model = model,
                            database = database,
                            selected = selected,
                            selection = selection,
                        )
                    }
                }
                SceneEditorToolbarView(
                    model = model,
                    database = database,
                    scene = scene,
                    selected = selected,
                    selection = selection,
                    vertical = landscape,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    videoArea(
                        model = model,
                        widgetOutlines = widgetOutlines,
                        scene = scene,
                        selection = selection,
                        mirror = mirror(scene),
                    )
                }
                if (showingSettings && selected != null) {
                    Box(modifier = Modifier.height((metricsHeight * 0.45).dp)) {
                        settingsPanel(
                            model = model,
                            database = database,
                            selected = selected,
                            selection = selection,
                        )
                    }
                }
                SceneEditorToolbarView(
                    model = model,
                    database = database,
                    scene = scene,
                    selected = selected,
                    selection = selection,
                    vertical = landscape,
                )
            }
        }
        preview.hashCode()
    }
}
