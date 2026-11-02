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
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.Visibility
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.view.CloseButtonView
import com.moblin.android.view.settings.scenes.scene.SceneSettingsView
import com.moblin.android.view.settings.scenes.scene.SceneWidgetSettingsView
import com.moblin.android.view.settings.scenes.scene.SceneWizardSheetView
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
import com.moblin.android.various.settings.SettingsWidgetType
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.min
import kotlinx.coroutines.flow.MutableStateFlow
import com.moblin.android.LocalModel
import com.moblin.android.various.model.addableWidgets
import com.moblin.android.various.model.appendWidgetToScene
import com.moblin.android.various.model.deleteScene
import com.moblin.android.various.model.deleteWidget
import com.moblin.android.various.model.sceneEditorMoveWidget
import com.moblin.android.various.model.sceneEditorMoveWidgetInDrawOrder
import com.moblin.android.various.model.sceneEditorPresentationChanged
import com.moblin.android.various.model.sceneEditorRemoveWidget
import com.moblin.android.various.model.sceneEditorResizeWidget
import com.moblin.android.various.model.sceneEditorScene
import com.moblin.android.various.model.sceneEditorSelectScene
import com.moblin.android.various.model.sceneEditorSetWidgetAlignment
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
    val guideX: Double? = null,
    val guideY: Double? = null,
)

private class SceneEditorSelection {
    val showingSettings = MutableStateFlow(false)
    val sceneWidgetIdFlow = MutableStateFlow<UUID?>(null)

    var sceneWidgetId: UUID?
        get() = sceneWidgetIdFlow.value
        set(value) {
            sceneWidgetIdFlow.value = value
        }
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
    var lastTapLocation by remember { mutableStateOf<CGPoint?>(null) }

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
        val alignment = outline.alignment
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

    fun selectOutline(location: CGPoint) {
        val sameLocation = lastTapLocation?.let { distance(it, location) < 10 } ?: false
        lastTapLocation = location
        selection.sceneWidgetId = sceneEditorTapSelection(
            hitIds = outlinesAt(location).map { it.id },
            selectedId = selectedId,
            sameLocation = sameLocation,
        )
    }

    fun isResizable(outline: WidgetOutline): Boolean = outline.sized && outline.widget.hasSize()

    fun fixedPoint(outline: WidgetOutline): CGPoint = sceneEditorFixedPoint(outline.rect, outline.alignment)

    fun handlePoint(outline: WidgetOutline, rect: CGRect): CGPoint = sceneEditorHandlePoint(rect, outline.alignment)

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
        val maximum = min(canvasSize.width, canvasSize.height)
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
            if (!outline.widget.hasPosition()) {
                return
            }
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
            val others = allOutlines.filter { it.id != currentEdit.outline.id }
            val x = if (layout.alignment.isHorizontalCenter()) {
                SceneEditorSnap(position = rect.minX)
            } else {
                sceneEditorSnapAxis(
                    position = position.x,
                    length = rect.width,
                    total = canvasSize.width,
                    threshold = snapThreshold,
                    canCenter = currentEdit.outline.sized,
                    near = layout.alignment.isLeft(),
                    targets = others.flatMap { snapTargetsX(it) },
                )
            }
            val y = if (layout.alignment.isVerticalCenter()) {
                SceneEditorSnap(position = rect.minY)
            } else {
                sceneEditorSnapAxis(
                    position = position.y,
                    length = rect.height,
                    total = canvasSize.height,
                    threshold = snapThreshold,
                    canCenter = currentEdit.outline.sized,
                    near = layout.alignment.isTop(),
                    targets = others.flatMap { snapTargetsY(it) },
                )
            }
            currentEdit = currentEdit.copy(
                rect = currentEdit.rect.copy(x = x.position, y = y.position),
                guideX = x.guide,
                guideY = y.guide,
            )
        }
        edit = currentEdit
        model.sceneEditorMoveWidget(sceneWidget = currentEdit.outline.sceneWidget, rect = currentEdit.rect)
    }

    fun resize(factor: Double) {
        var currentEdit = edit ?: return
        if (currentEdit.mode != SceneEditorEditMode.resize) {
            return
        }
        val rect = currentEdit.outline.rect
        val fixed = fixedPoint(currentEdit.outline)
        val factors = (1 / currentEdit.startSize)..(100 / currentEdit.startSize)
        var newFactor = factor.coerceIn(factors)
        val others = allOutlines.filter { it.id != currentEdit.outline.id }
        val threshold = snapThreshold / distance(handlePoint(currentEdit.outline, rect), fixed)
        val x = sceneEditorSnapResizeAxis(
            edges = listOf(rect.minX, rect.maxX, rect.midX),
            fixed = fixed.x,
            factor = newFactor,
            factors = factors,
            threshold = threshold,
            targets = listOf(0.0, canvasSize.width / 2, canvasSize.width) + others.flatMap { snapTargetsX(it) },
        )
        val y = sceneEditorSnapResizeAxis(
            edges = listOf(rect.minY, rect.maxY, rect.midY),
            fixed = fixed.y,
            factor = newFactor,
            factors = factors,
            threshold = threshold,
            targets = listOf(0.0, canvasSize.height / 2, canvasSize.height) + others.flatMap { snapTargetsY(it) },
        )
        currentEdit = currentEdit.copy(guideX = null, guideY = null)
        if (x != null && x.distance < (y?.distance ?: Double.POSITIVE_INFINITY)) {
            newFactor = x.factor
            currentEdit = currentEdit.copy(guideX = x.guide)
        } else if (y != null) {
            newFactor = y.factor
            currentEdit = currentEdit.copy(guideY = y.guide)
        }
        currentEdit = currentEdit.copy(rect = sceneEditorResizedRect(rect, fixed, newFactor))
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
        selectOutline(location)
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
                val color = if (selected) palette.accent else if (outline.inScene) Color.White else palette.gray
                val dash = if (outline.inScene && outline.enabled) null else floatArrayOf(4f, 4f)
                val strokeWidth = if (selected) 2.dp.toPx() else 1.dp.toPx()
                if (outline.sized) {
                    val topLeft = Offset(rect.minX.dp.toPx(), rect.minY.dp.toPx())
                    val rectSize = Size(rect.width.dp.toPx(), rect.height.dp.toPx())
                    if (selected) {
                        drawRect(color = selectedFill, topLeft = topLeft, size = rectSize)
                    } else if (outline.widget.type == SettingsWidgetType.scene) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.1f),
                            topLeft = topLeft,
                            size = rectSize,
                        )
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
                    drawCircle(
                        color = color,
                        radius = radius,
                        center = center,
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = dash?.let { values ->
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
                val guideX = currentEdit.guideX
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
                val guideY = currentEdit.guideY
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

private fun alignmentImage(alignment: SettingsAlignment): String = when (alignment) {
    SettingsAlignment.topLeft -> "square.grid.3x3.topleft.filled"
    SettingsAlignment.topCenter -> "square.grid.3x3.topmiddle.filled"
    SettingsAlignment.topRight -> "square.grid.3x3.topright.filled"
    SettingsAlignment.leftCenter -> "square.grid.3x3.middleleft.filled"
    SettingsAlignment.center -> "square.grid.3x3.middle.filled"
    SettingsAlignment.rightCenter -> "square.grid.3x3.middleright.filled"
    SettingsAlignment.bottomLeft -> "square.grid.3x3.bottomleft.filled"
    SettingsAlignment.bottomCenter -> "square.grid.3x3.bottommiddle.filled"
    SettingsAlignment.bottomRight -> "square.grid.3x3.bottomright.filled"
}

@Composable
private fun SceneEditorAlignmentButtonView(
    model: Model = LocalModel.current,
    sceneWidget: SettingsSceneWidget,
    size: CGSize,
) {
    var presentingAlignment by remember { mutableStateOf(false) }
    val option: @Composable (SettingsAlignment) -> Unit = { alignment ->
        Button(action = {
            model.sceneEditorSetWidgetAlignment(sceneWidget = sceneWidget, size = size, alignment = alignment)
        }) {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                SystemImage(
                    name = if (sceneWidget.layout.alignment == alignment) "square.fill" else "square",
                    fontSize = 28.sp,
                )
            }
        }
    }
    SceneEditorButtonView(image = alignmentImage(sceneWidget.layout.alignment)) {
        presentingAlignment = true
    }
    Sheet(isPresented = presentingAlignment, onDismissRequest = { presentingAlignment = false }) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                option(SettingsAlignment.topLeft)
                option(SettingsAlignment.topCenter)
                option(SettingsAlignment.topRight)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                option(SettingsAlignment.leftCenter)
                option(SettingsAlignment.center)
                option(SettingsAlignment.rightCenter)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                option(SettingsAlignment.bottomLeft)
                option(SettingsAlignment.bottomCenter)
                option(SettingsAlignment.bottomRight)
            }
        }
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
    var presentingCreateWizard by remember { mutableStateOf(false) }
    SceneEditorButtonView(image = "photo.on.rectangle") {
        presentingScenes = true
    }
    Sheet(isPresented = presentingScenes, onDismissRequest = { presentingScenes = false }) {
        SceneEditorPopoverListView {
            SceneEditorPopoverRowView(
                image = "plus.square.dashed",
                text = localized("Create scene"),
            ) {
                presentingScenes = false
                model.createSceneWizard.reset()
                presentingCreateWizard = true
            }
            Divider()
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
    Sheet(isPresented = presentingCreateWizard, onDismissRequest = { presentingCreateWizard = false }) {
        SceneWizardSheetView(
            database = database,
            presentingCreateWizard = presentingCreateWizard,
            onPresentingCreateWizardChange = { presentingCreateWizard = it },
            onCreated = { createdScene ->
                selection.sceneWidgetId = null
                model.sceneEditorSelectScene(id = createdScene.id)
            },
            model = model,
        )
    }
}

@Composable
private fun SceneEditorDeleteSceneButtonView(
    model: Model = LocalModel.current,
    scene: SettingsScene,
    selection: SceneEditorSelection,
) {
    var presentingDeleteSceneConfirmation by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalTint provides formPalette().red) {
        SceneEditorButtonView(image = "trash", role = ButtonRole.destructive) {
            presentingDeleteSceneConfirmation = true
        }
    }
    ConfirmationDialog(
        title = localized("Delete scene ${scene.name}?"),
        isPresented = presentingDeleteSceneConfirmation,
        onDismissRequest = { presentingDeleteSceneConfirmation = false },
        titleVisibility = Visibility.visible,
    ) {
        Button(title = "Delete", role = ButtonRole.destructive) {
            selection.sceneWidgetId = null
            model.deleteScene(scene = scene)
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
            model.addableWidgets(scene = scene).forEach { widget ->
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
    selected: WidgetOutline?,
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
                val sceneWidget = selected.sceneWidget
                val widget = selected.widget
                SceneEditorEnableButtonView(model = model, widget = widget)
                if (widget.hasAlignment()) {
                    SceneEditorAlignmentButtonView(
                        model = model,
                        sceneWidget = sceneWidget,
                        size = selected.rect.size,
                    )
                }
                SceneEditorButtonView(image = "square.2.layers.3d.top.filled") {
                    model.sceneEditorMoveWidgetInDrawOrder(scene = scene, sceneWidget = sceneWidget, up = true)
                }
                SceneEditorButtonView(image = "square.2.layers.3d.bottom.filled") {
                    model.sceneEditorMoveWidgetInDrawOrder(scene = scene, sceneWidget = sceneWidget, up = false)
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
                    title = widget.name,
                    isPresented = presentingRemoveWidgetConfirmation,
                    onDismissRequest = { presentingRemoveWidgetConfirmation = false },
                    titleVisibility = Visibility.visible,
                ) {
                    Button(title = "Remove widget from scene", role = ButtonRole.destructive) {
                        model.sceneEditorRemoveWidget(scene = scene, sceneWidget = sceneWidget)
                        selection.sceneWidgetId = null
                    }
                    Button(title = "Delete widget", role = ButtonRole.destructive) {
                        selection.sceneWidgetId = null
                        model.deleteWidget(widget = widget)
                    }
                }
            } else {
                SceneEditorAddWidgetButtonView(
                    model = model,
                    database = database,
                    scene = scene,
                    selection = selection,
                )
                SceneEditorButtonView(image = "slider.horizontal.3") {
                    selection.showingSettings.value = !selection.showingSettings.value
                }
                SceneEditorDeleteSceneButtonView(model = model, scene = scene, selection = selection)
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
    scene: SettingsScene?,
    selected: WidgetOutline?,
    selection: SceneEditorSelection,
) {
    Box(contentAlignment = Alignment.TopEnd) {
        key(selected?.sceneWidget?.id ?: scene?.id) {
            NavigationStack {
                if (selected != null) {
                    SceneWidgetSettingsView(
                        model = model,
                        database = database,
                        sceneWidget = selected.sceneWidget,
                        widget = selected.widget,
                    )
                } else if (scene != null) {
                    SceneSettingsView(database = database, scene = scene)
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = localized("No widget selected"),
                            color = formPalette().secondaryLabel,
                        )
                    }
                }
            }
        }
        CloseButtonView {
            selection.showingSettings.value = false
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
    val allOutlines by widgetOutlines.outlines.collectAsState()

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

    fun selectedOutline(): WidgetOutline? =
        allOutlines.firstOrNull { it.id == selectedSceneWidgetId && it.inScene }

    val scene = model.sceneEditorScene()
    val selected = selectedOutline()

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
                if (showingSettings) {
                    Box(modifier = Modifier.width(settingsHalfWidth.dp)) {
                        settingsPanel(
                            model = model,
                            database = database,
                            scene = scene,
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
                if (showingSettings) {
                    Box(modifier = Modifier.height((metricsHeight * 0.45).dp)) {
                        settingsPanel(
                            model = model,
                            database = database,
                            scene = scene,
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
