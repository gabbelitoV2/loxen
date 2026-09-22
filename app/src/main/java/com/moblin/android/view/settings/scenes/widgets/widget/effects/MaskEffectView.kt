package com.moblin.android.view.settings.scenes.widgets.widget.effects

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moblin.android.R
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsMaskBackgroundType
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectMask
import com.moblin.android.various.settings.SettingsVideoEffectMaskEffectPoint
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.videoeffects.makeCatmullRomPath
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.RgbColorPickerView
import kotlin.math.hypot
import com.moblin.android.LocalModel

private val maskPointHandleRadius: Float = 12f
private val maskEdgeHitWidth: Float = 20f
private val maskDragThreshold: Float = 6f
private val maskTapThreshold: Float = 4f
private val maskMinimumPoints: Int = 3

@Composable
private fun MaskCanvasView(
    mask: SettingsVideoEffectMask,
    updateWidget: () -> Unit,
    refreshPreviewImage: () -> Unit,
    previewImage: Bitmap?,
    isPortrait: Boolean,
    selectedPointIndex: Int?,
    onSelectedPointIndexChange: (Int?) -> Unit,
    selectedEdgeIndex: Int?,
    onSelectedEdgeIndexChange: (Int?) -> Unit,
) {
    val points = mask.points
    val tension = mask.tension
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var pendingDragIndex by remember { mutableStateOf<Int?>(null) }
    var panStartPoints by remember { mutableStateOf<List<SettingsVideoEffectMaskEffectPoint>?>(null) }
    var panStartLocation by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    fun canvasPoint(point: SettingsVideoEffectMaskEffectPoint, size: Size): Offset = Offset(
        (point.x / 100.0 * size.width).toFloat(),
        (point.y / 100.0 * size.height).toFloat()
    )

    fun normalizedPoint(location: Offset, size: Size): SettingsVideoEffectMaskEffectPoint =
        SettingsVideoEffectMaskEffectPoint(
            x = (location.x / size.width * 100.0).coerceIn(0.0, 100.0),
            y = (location.y / size.height * 100.0).coerceIn(0.0, 100.0)
        )

    fun closestPointIndex(location: Offset, size: Size): Int? {
        var closest: Int? = null
        var closestDist = maskPointHandleRadius * 1.2f
        mask.points.forEachIndexed { index, point ->
            val pt = canvasPoint(point, size)
            val dist = hypot(location.x - pt.x, location.y - pt.y)
            if (dist < closestDist) {
                closestDist = dist
                closest = index
            }
        }
        return closest
    }

    fun pointToSegmentDistance(p: Offset, a: Offset, b: Offset): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lengthSq = dx * dx + dy * dy
        if (lengthSq <= 0f) {
            return hypot(p.x - a.x, p.y - a.y)
        }
        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lengthSq).coerceIn(0f, 1f)
        val projX = a.x + t * dx
        val projY = a.y + t * dy
        return hypot(p.x - projX, p.y - projY)
    }

    fun closestEdgeIndex(location: Offset, size: Size): Int? {
        val currentPoints = mask.points
        if (currentPoints.size < 2) {
            return null
        }
        val pts = currentPoints.map { canvasPoint(it, size) }
        var closest: Int? = null
        var closestDist = maskEdgeHitWidth
        for (i in pts.indices) {
            val a = pts[i]
            val b = pts[(i + 1) % pts.size]
            val dist = pointToSegmentDistance(location, a, b)
            if (dist < closestDist) {
                closestDist = dist
                closest = i
            }
        }
        return closest
    }

    fun drawPolygon(scope: DrawScope, size: Size) {
        val pts = points.map { canvasPoint(it, size) }
        val path = makeCatmullRomPath(pts.map { PointF(it.x, it.y) }, tension.toFloat()).asComposePath()
        scope.drawPath(path, color = Color.White.copy(alpha = 0.25f))
        scope.drawPath(path, color = Color.White, style = Stroke(width = 1.5f))
    }

    fun drawHighlightedEdge(scope: DrawScope, size: Size) {
        val edgeIndex = selectedEdgeIndex ?: return
        val pts = points.map { canvasPoint(it, size) }
        val numberOfPoints = pts.size
        if (numberOfPoints < 3) {
            return
        }
        val point0 = pts[(edgeIndex - 1 + numberOfPoints) % numberOfPoints]
        val point1 = pts[edgeIndex]
        val point2 = pts[(edgeIndex + 1) % numberOfPoints]
        val point3 = pts[(edgeIndex + 2) % numberOfPoints]
        val t = tension.toFloat()
        val control1 = Offset(
            x = point1.x + (point2.x - point0.x) * t,
            y = point1.y + (point2.y - point0.y) * t
        )
        val control2 = Offset(
            x = point2.x - (point3.x - point1.x) * t,
            y = point2.y - (point3.y - point1.y) * t
        )
        val path = Path()
        path.moveTo(point1.x, point1.y)
        path.cubicTo(control1.x, control1.y, control2.x, control2.y, point2.x, point2.y)
        scope.drawPath(path, color = Color.Yellow, style = Stroke(width = 3f))
    }

    fun drawHandles(scope: DrawScope, size: Size) {
        points.forEachIndexed { index, point ->
            val pt = canvasPoint(point, size)
            val isSelected = index == selectedPointIndex
            val radius = if (isSelected) maskPointHandleRadius * 1.4f else maskPointHandleRadius
            val color = if (isSelected) Color.Yellow else Color.White
            scope.drawCircle(color = color, radius = radius / 2f, center = pt)
            scope.drawCircle(color = Color.Black, radius = radius / 2f, center = pt, style = Stroke(width = 1f))
        }
    }

    fun gestureChanged(startLocation: Offset, location: Offset, size: Size) {
        if (dragIndex == null && pendingDragIndex == null && panStartPoints == null) {
            val index = closestPointIndex(startLocation, size)
            if (index != null) {
                pendingDragIndex = index
                onSelectedPointIndexChange(index)
                onSelectedEdgeIndexChange(null)
            } else {
                panStartPoints = mask.points
                panStartLocation = startLocation
            }
        }
        val pending = pendingDragIndex
        if (pending != null && dragIndex == null) {
            val dist = hypot(location.x - startLocation.x, location.y - startLocation.y)
            if (dist >= maskDragThreshold) {
                dragIndex = pending
                pendingDragIndex = null
            }
        }
        val currentDragIndex = dragIndex
        if (currentDragIndex != null) {
            val updated = mask.points.toMutableList()
            if (currentDragIndex in updated.indices) {
                updated[currentDragIndex] = normalizedPoint(location, size)
                mask.points = updated
                updateWidget()
            }
        } else {
            val startPoints = panStartPoints
            val startLoc = panStartLocation
            if (startPoints != null && startLoc != null) {
                val dx = (location.x - startLoc.x) / size.width * 100.0
                val dy = (location.y - startLoc.y) / size.height * 100.0
                mask.points = startPoints.map { point ->
                    SettingsVideoEffectMaskEffectPoint(
                        x = (point.x + dx).coerceIn(0.0, 100.0),
                        y = (point.y + dy).coerceIn(0.0, 100.0)
                    )
                }
                updateWidget()
            }
        }
    }

    fun gestureEnded(startLocation: Offset, location: Offset, size: Size) {
        val wasDragging = dragIndex != null
        val wasPanning = panStartPoints != null
        dragIndex = null
        pendingDragIndex = null
        panStartPoints = null
        panStartLocation = null
        val dragDistance = hypot(location.x - startLocation.x, location.y - startLocation.y)
        if (!wasDragging && (!wasPanning || dragDistance < maskTapThreshold)) {
            val index = closestPointIndex(startLocation, size)
            if (index != null) {
                onSelectedPointIndexChange(index)
                onSelectedEdgeIndexChange(null)
            } else {
                val edgeIdx = closestEdgeIndex(startLocation, size)
                if (edgeIdx != null) {
                    onSelectedEdgeIndexChange(edgeIdx)
                    onSelectedPointIndexChange(null)
                } else {
                    onSelectedPointIndexChange(null)
                    onSelectedEdgeIndexChange(null)
                }
            }
        }
    }

    fun resizeShape(scale: Double) {
        val currentPoints = mask.points
        if (currentPoints.isEmpty()) {
            return
        }
        val centerX = currentPoints.sumOf { it.x } / currentPoints.size
        val centerY = currentPoints.sumOf { it.y } / currentPoints.size
        mask.points = currentPoints.map { point ->
            SettingsVideoEffectMaskEffectPoint(
                x = (centerX + (point.x - centerX) * scale).coerceIn(0.0, 100.0),
                y = (centerY + (point.y - centerY) * scale).coerceIn(0.0, 100.0)
            )
        }
        updateWidget()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (isPortrait) 9f / 16f else 16f / 9f)
                .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
        ) {
            if (previewImage != null) {
                Image(
                    bitmap = previewImage.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = ColorPainter(Color.Gray),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(mask, canvasSize) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startLocation = down.position
                            var location = startLocation
                            gestureChanged(startLocation, location, canvasSize)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                location = change.position
                                if (!change.pressed) {
                                    break
                                }
                                gestureChanged(startLocation, location, canvasSize)
                            }
                            gestureEnded(startLocation, location, canvasSize)
                        }
                    }
            ) {
                drawPolygon(this, size)
                drawHighlightedEdge(this, size)
                drawHandles(this, size)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { resizeShape(1 / 1.1) }) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = { resizeShape(1.1) }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { refreshPreviewImage() }) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun MaskEditorView(
    mask: SettingsVideoEffectMask,
    updateWidget: () -> Unit,
    selectedPointIndex: Int?,
    onSelectedPointIndexChange: (Int?) -> Unit,
    selectedEdgeIndex: Int?,
    onSelectedEdgeIndexChange: (Int?) -> Unit,
) {
    val points = mask.points
    var xText by remember { mutableStateOf("") }
    var yText by remember { mutableStateOf("") }

    fun updateXYText() {
        val index = selectedPointIndex ?: return
        val allPoints = mask.points
        if (index !in allPoints.indices) {
            return
        }
        val point = allPoints[index]
        xText = formatOneDecimal(point.x)
        yText = formatOneDecimal(point.y)
    }

    fun setX(value: Double) {
        val index = selectedPointIndex ?: return
        val allPoints = mask.points
        if (index !in allPoints.indices) {
            return
        }
        val x = value.coerceIn(0.0, 100.0)
        if (x == allPoints[index].x) {
            return
        }
        val updated = allPoints.toMutableList()
        updated[index] = SettingsVideoEffectMaskEffectPoint(x = x, y = allPoints[index].y)
        mask.points = updated
        updateWidget()
    }

    fun setY(value: Double) {
        val index = selectedPointIndex ?: return
        val allPoints = mask.points
        if (index !in allPoints.indices) {
            return
        }
        val y = value.coerceIn(0.0, 100.0)
        if (y == allPoints[index].y) {
            return
        }
        val updated = allPoints.toMutableList()
        updated[index] = SettingsVideoEffectMaskEffectPoint(x = allPoints[index].x, y = y)
        mask.points = updated
        updateWidget()
    }

    fun commitX() {
        val x = xText.toDoubleOrNull() ?: return
        if (!x.isFinite()) {
            return
        }
        setX(x)
    }

    fun commitY() {
        val y = yText.toDoubleOrNull() ?: return
        if (!y.isFinite()) {
            return
        }
        setY(y)
    }

    fun adjustX(delta: Double) {
        val index = selectedPointIndex ?: return
        val allPoints = mask.points
        if (index !in allPoints.indices) {
            return
        }
        setX(allPoints[index].x + delta)
    }

    fun adjustY(delta: Double) {
        val index = selectedPointIndex ?: return
        val allPoints = mask.points
        if (index !in allPoints.indices) {
            return
        }
        setY(allPoints[index].y + delta)
    }

    val currentPointIndex = selectedPointIndex
    val currentEdgeIndex = selectedEdgeIndex
    if (currentPointIndex != null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "X")
                OutlinedTextField(
                    value = xText,
                    onValueChange = { xText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { commitX() })
                )
                IconButton(onClick = { adjustX(delta = -0.1) }) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { adjustX(delta = 0.1) }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Y")
                OutlinedTextField(
                    value = yText,
                    onValueChange = { yText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { commitY() })
                )
                IconButton(onClick = { adjustY(delta = -0.1) }) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { adjustY(delta = 0.1) }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }
            Button(
                onClick = {
                    val allPoints = mask.points
                    if (currentPointIndex in allPoints.indices) {
                        val updated = allPoints.toMutableList()
                        updated.removeAt(currentPointIndex)
                        mask.points = updated
                    }
                    onSelectedPointIndexChange(null)
                    updateWidget()
                },
                enabled = points.size > maskMinimumPoints,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                HCenter {
                    Text(text = "Delete point")
                }
            }
        }
        LaunchedEffect(selectedPointIndex, points) {
            updateXYText()
        }
    } else if (currentEdgeIndex != null) {
        Button(
            onClick = {
                val allPoints = mask.points
                if (currentEdgeIndex in allPoints.indices) {
                    val point1 = allPoints[currentEdgeIndex]
                    val point2 = allPoints[(currentEdgeIndex + 1) % allPoints.size]
                    val newPoint = SettingsVideoEffectMaskEffectPoint(
                        x = (point1.x + point2.x) / 2,
                        y = (point1.y + point2.y) / 2
                    )
                    val updated = allPoints.toMutableList()
                    updated.add(currentEdgeIndex + 1, newPoint)
                    mask.points = updated
                }
                onSelectedEdgeIndexChange(null)
                onSelectedPointIndexChange(currentEdgeIndex + 1)
                updateWidget()
            }
        ) {
            HCenter {
                Text(text = "Create point")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaskEffectView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    mask: SettingsVideoEffectMask,
) {
    var previewImage by remember { mutableStateOf<Bitmap?>(null) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    var selectedEdgeIndex by remember { mutableStateOf<Int?>(null) }
    var backgroundTypeExpanded by remember { mutableStateOf(false) }

    val inverted = mask.inverted
    val tension = mask.tension
    val backgroundType = mask.backgroundType
    val backgroundColorColor = mask.backgroundColorColor
    val backgroundColorColor2 = mask.backgroundColorColor2

    fun updateWidget() {
        TODO("no Android counterpart for Model.getWidgetMaskEffect")
    }

    fun refreshPreviewImage() {
        TODO("no Android counterpart for Model.takeVideoSourcePreviewImage")
    }

    Column {
        Text(text = "Shape", style = MaterialTheme.typography.titleSmall)
        MaskCanvasView(
            mask = mask,
            updateWidget = { updateWidget() },
            refreshPreviewImage = { refreshPreviewImage() },
            previewImage = previewImage,
            isPortrait = TODO("no Android counterpart for SettingsStream.portrait"),
            selectedPointIndex = selectedPointIndex,
            onSelectedPointIndexChange = { selectedPointIndex = it },
            selectedEdgeIndex = selectedEdgeIndex,
            onSelectedEdgeIndexChange = { selectedEdgeIndex = it }
        )
        MaskEditorView(
            mask = mask,
            updateWidget = { updateWidget() },
            selectedPointIndex = selectedPointIndex,
            onSelectedPointIndexChange = { selectedPointIndex = it },
            selectedEdgeIndex = selectedEdgeIndex,
            onSelectedEdgeIndexChange = { selectedEdgeIndex = it }
        )
    }
    LaunchedEffect(Unit) {
        refreshPreviewImage()
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Inverted", modifier = Modifier.weight(1f))
            Switch(
                checked = inverted,
                onCheckedChange = {
                    mask.inverted = it
                    updateWidget()
                }
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Smoothness")
            Slider(
                value = tension.toFloat(),
                onValueChange = {
                    mask.tension = it.toDouble()
                    updateWidget()
                },
                valueRange = 0f..0.5f,
                modifier = Modifier.weight(1f)
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Background", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Type", modifier = Modifier.weight(1f))
            ExposedDropdownMenuBox(
                expanded = backgroundTypeExpanded,
                onExpandedChange = { backgroundTypeExpanded = it }
            ) {
                OutlinedTextField(
                    value = backgroundType.toString(),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = backgroundTypeExpanded)
                    },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = backgroundTypeExpanded,
                    onDismissRequest = { backgroundTypeExpanded = false }
                ) {
                    SettingsMaskBackgroundType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(text = type.toString()) },
                            onClick = {
                                mask.backgroundType = type
                                backgroundTypeExpanded = false
                                updateWidget()
                            }
                        )
                    }
                }
            }
        }
        if (backgroundType != SettingsMaskBackgroundType.transparent) {
            RgbColorPickerView(
                title = "Color",
                color = backgroundColorColor,
                onColorChanged = {
                    mask.backgroundColorColor = it
                },
                onChange = {
                    mask.backgroundColor = it
                    updateWidget()
                }
            )
        }
        if (backgroundType == SettingsMaskBackgroundType.checkerboard) {
            RgbColorPickerView(
                title = "Color 2",
                color = backgroundColorColor2,
                onColorChanged = {
                    mask.backgroundColorColor2 = it
                },
                onChange = {
                    mask.backgroundColor2 = it
                    updateWidget()
                }
            )
        }
    }
}
