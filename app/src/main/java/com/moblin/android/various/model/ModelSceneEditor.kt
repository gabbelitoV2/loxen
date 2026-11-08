package com.moblin.android.various.model

import android.graphics.BitmapFactory
import android.media.ExifInterface
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coregraphics.toCGSize
import com.moblin.android.videoeffects.layoutPosition
import com.moblin.android.videoeffects.layoutScale
import com.moblin.android.videoeffects.toPixels
import com.moblin.android.various.settings.*
import com.moblin.android.view.controlbar.quickbutton.sceneeditor.sceneEditorAlignedLayout
import com.moblin.android.view.controlbar.quickbutton.sceneeditor.sceneEditorLayout
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow

data class WidgetOutline(
    val sceneWidget: SettingsSceneWidget,
    val widget: SettingsWidget,
    val rect: CGRect,
    val alignment: SettingsAlignment,
    val sized: Boolean,
    val inScene: Boolean,
    val enabled: Boolean,
) {
    val id: UUID
        get() = sceneWidget.id
}

class SceneEditor {
    val sceneId = MutableStateFlow<UUID?>(null)
    val enabled = MutableStateFlow(false)
    val outlines = MutableStateFlow<List<WidgetOutline>>(emptyList())
    val canvasSize = MutableStateFlow(CGSize(width = 1920.0, height = 1080.0))
    var imageSizes: MutableMap<UUID, CGSize?> = mutableMapOf()
}

fun Model.sceneEditorPresentationChanged() {
    sceneEditor.sceneId.value = null
    media.setShowCameraPreview(updateShowCameraPreview())
    setSceneEditorWidgetOutlinesEnabled(presentingSceneEditor.value)
    updateWidgetOutlines()
}

fun Model.sceneEditorScene(): SettingsScene? {
    val sceneId = sceneEditor.sceneId.value
    if (sceneId != null) {
        database.findScene(id = sceneId)?.let { return it }
    }
    return getSelectedScene()
}

fun Model.sceneEditorSelectScene(id: UUID) {
    val scene = database.findScene(id = id) ?: return
    if (scene.enabled) {
        sceneEditor.sceneId.value = null
        selectScene(id = id)
    } else {
        sceneEditor.sceneId.value = id
    }
    updateWidgetOutlines()
}

fun Model.sceneEditorMoveWidget(sceneWidget: SettingsSceneWidget, rect: CGRect) {
    sceneWidget.layout = sceneEditorLayout(
        sceneWidget.layout,
        rect,
        sceneEditor.canvasSize.value,
    )
    sceneUpdatedThrottled()
}

fun Model.sceneEditorSetWidgetAlignment(
    sceneWidget: SettingsSceneWidget,
    size: CGSize,
    alignment: SettingsAlignment,
) {
    sceneWidget.layout = sceneEditorAlignedLayout(
        sceneWidget.layout,
        alignment,
        size,
        sceneEditor.canvasSize.value,
    )
    sceneUpdated()
}

fun Model.sceneEditorResizeWidget(sceneWidget: SettingsSceneWidget, size: Double) {
    sceneWidget.layout = sceneWidget.layout.copy(
        size = size.coerceIn(1.0, 100.0),
    ).updatingSizeString()
    sceneUpdatedThrottled()
}

fun Model.sceneEditorToggleWidgetEnabled(widget: SettingsWidget) {
    widget.enabled = !widget.enabled
    reloadSpeechToText()
    sceneUpdated(attachCamera = isCaptureDeviceWidget(widget = widget))
}

fun Model.sceneEditorRemoveWidget(scene: SettingsScene, sceneWidget: SettingsSceneWidget) {
    var attachCamera = false
    if (scene.id == getSelectedScene()?.id) {
        val widget = findWidget(id = sceneWidget.widgetId)
        if (widget != null) {
            attachCamera = isCaptureDeviceWidget(widget = widget)
        }
    }
    scene.widgets.removeAll { it.id == sceneWidget.id }
    sceneUpdated(attachCamera = attachCamera)
    sceneSettingsPanelSceneId.value += 1
}

private fun Model.sceneEditorDrawOrderTargetIndex(
    scene: SettingsScene,
    sceneWidget: SettingsSceneWidget,
    up: Boolean,
): Int? {
    val index = scene.widgets.indexOfFirst { it.id == sceneWidget.id }
    if (index == -1) {
        return null
    }
    val shownSceneWidgetIds = sceneEditor.outlines.value
        .filter { it.inScene }
        .map { it.id }
        .toSet()
    val step = if (up) 1 else -1
    var newIndex = index + step
    while (newIndex in scene.widgets.indices && !shownSceneWidgetIds.contains(scene.widgets[newIndex].id)) {
        newIndex += step
    }
    if (newIndex !in scene.widgets.indices) {
        return null
    }
    return newIndex
}

fun Model.sceneEditorCanMoveWidgetInDrawOrder(
    scene: SettingsScene,
    sceneWidget: SettingsSceneWidget,
    up: Boolean,
): Boolean = sceneEditorDrawOrderTargetIndex(scene = scene, sceneWidget = sceneWidget, up = up) != null

fun Model.sceneEditorMoveWidgetInDrawOrder(
    scene: SettingsScene,
    sceneWidget: SettingsSceneWidget,
    up: Boolean,
) {
    val index = scene.widgets.indexOfFirst { it.id == sceneWidget.id }
    val newIndex = sceneEditorDrawOrderTargetIndex(scene = scene, sceneWidget = sceneWidget, up = up)
    if (index == -1 || newIndex == null) {
        return
    }
    val movedWidget = scene.widgets.removeAt(index)
    scene.widgets.add(newIndex, movedWidget)
    sceneUpdated()
    sceneSettingsPanelSceneId.value += 1
}

fun Model.setSceneEditorWidgetOutlinesEnabled(enabled: Boolean) {
    if (enabled == sceneEditor.enabled.value) {
        return
    }
    sceneEditor.enabled.value = enabled
    sceneEditor.imageSizes.clear()
    if (enabled) {
        updateWidgetOutlines()
    } else {
        sceneEditor.outlines.value = emptyList()
    }
}

fun Model.updateWidgetOutlines(resolver: SceneResolver? = null) {
    if (!sceneEditor.enabled.value) {
        return
    }
    val scene = sceneEditorScene()
    if (scene == null) {
        sceneEditor.outlines.value = emptyList()
        return
    }
    val canvasSize = media.getCanvasSize().toCGSize()
    if (canvasSize.width != sceneEditor.canvasSize.value.width ||
        canvasSize.height != sceneEditor.canvasSize.value.height
    ) {
        sceneEditor.canvasSize.value = canvasSize
    }
    val outlines = mutableListOf<WidgetOutline>()
    val usedResolver = resolver ?: makeSceneResolver()
    val resolved = usedResolver.resolve(scene, includeRemote = true, includeDisabledSceneWidgets = true)
    for (drawnWidget in resolved.firstWidgets().filter { it.widget.type == SettingsWidgetType.scene }) {
        if (!scene.widgets.any { it === drawnWidget.sceneWidget }) {
            continue
        }
        outlines.add(
            WidgetOutline(
                sceneWidget = drawnWidget.sceneWidget,
                widget = drawnWidget.widget,
                rect = CGRect(origin = CGPoint.zero, size = canvasSize),
                alignment = drawnWidget.sceneWidget.layout.alignment,
                sized = true,
                inScene = true,
                enabled = drawnWidget.widget.enabled,
            ),
        )
    }
    for (drawnWidget in resolved.firstWidgets()) {
        val widget = drawnWidget.widget
        if (!widget.hasPosition()) {
            continue
        }
        val layout = drawnWidget.sceneWidget.layout
        val size = widgetOutlineSize(widget = widget, layout = layout, canvasSize = canvasSize)
        outlines.add(
            WidgetOutline(
                sceneWidget = drawnWidget.sceneWidget,
                widget = widget,
                rect = CGRect(
                    origin = layoutPosition(layout, size ?: CGSize.zero, canvasSize),
                    size = size ?: CGSize.zero,
                ),
                alignment = layout.alignment,
                sized = size != null,
                inScene = drawnWidget.scene === scene,
                enabled = drawnWidget.enabled,
            ),
        )
    }
    if (!widgetOutlinesEqual(outlines, sceneEditor.outlines.value)) {
        sceneEditor.outlines.value = outlines
    }
}

private fun Model.widgetOutlineSize(
    widget: SettingsWidget,
    layout: SettingsWidgetLayout,
    canvasSize: CGSize,
): CGSize? {
    val box = CGSize(
        width = toPixels(layout.size, canvasSize.width),
        height = toPixels(layout.size, canvasSize.height),
    )
    val side = toPixels(layout.size, canvasSize.minimum())
    return when (widget.type) {
        SettingsWidgetType.map, SettingsWidgetType.qrCode -> CGSize(width = side, height = side)
        SettingsWidgetType.bingoCard -> CGSize(width = side + 10, height = side + 10)
        SettingsWidgetType.browser -> fitInLayout(
            CGSize(
                width = widget.browser.width.toDouble(),
                height = widget.browser.height.toDouble(),
            ),
            layout,
            canvasSize,
        ) ?: box
        SettingsWidgetType.image -> {
            val imageSize = widgetImageSize(widget = widget)
            if (imageSize == null) {
                box
            } else {
                fitInLayout(imageSize, layout, canvasSize) ?: box
            }
        }
        SettingsWidgetType.chatEmoteCombo -> CGSize(width = 2 * side, height = side)
        SettingsWidgetType.pomodoroTimer -> CGSize(
            width = widget.pomodoroTimer.width * side,
            height = 0.64 * side,
        )
        SettingsWidgetType.wheelOfLuck -> {
            val wheelSide = 400 * canvasSize.width / 1920
            CGSize(width = wheelSide + 0.3 * wheelSide / 12, height = wheelSide)
        }
        SettingsWidgetType.chat -> CGSize(
            width = 20 * widget.chat.fontSize.toDouble(),
            height = widget.chat.height.toDouble() * canvasSize.height,
        )
        SettingsWidgetType.videoSource,
        SettingsWidgetType.snapshot,
        SettingsWidgetType.slideshow,
        SettingsWidgetType.vTuber,
        SettingsWidgetType.pngTuber,
        -> box
        SettingsWidgetType.text,
        SettingsWidgetType.alerts,
        SettingsWidgetType.scoreboard,
        SettingsWidgetType.crop,
        SettingsWidgetType.scene,
        -> null
        else -> null
    }
}

private fun Model.fitInLayout(
    size: CGSize,
    layout: SettingsWidgetLayout,
    canvasSize: CGSize,
): CGSize? {
    if (size.width <= 0 || size.height <= 0) {
        return null
    }
    val scale = layoutScale(layout = layout, size = size, streamSize = canvasSize)
    return CGSize(width = size.width * scale, height = size.height * scale)
}

fun Model.imageWidgetUpdated(id: UUID) {
    sceneEditor.imageSizes.remove(id)
    updateWidgetOutlines()
}

private fun Model.widgetImageSize(widget: SettingsWidget): CGSize? {
    if (sceneEditor.imageSizes.containsKey(widget.id)) {
        return sceneEditor.imageSizes[widget.id]
    }
    val size = readImageSize(url = imageStorage.makePath(id = widget.id).toString())
    sceneEditor.imageSizes[widget.id] = size
    return size
}

private fun Model.readImageSize(url: String): CGSize? {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(url, options)
    val width = options.outWidth
    val height = options.outHeight
    if (width <= 0 || height <= 0) {
        return null
    }
    val orientation = runCatching {
        ExifInterface(url).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    return if (orientation >= 5) {
        CGSize(width = height.toDouble(), height = width.toDouble())
    } else {
        CGSize(width = width.toDouble(), height = height.toDouble())
    }
}

private fun Model.widgetOutlinesEqual(
    first: List<WidgetOutline>,
    second: List<WidgetOutline>,
): Boolean {
    if (first.size != second.size) {
        return false
    }
    return first.zip(second).all { (a, b) ->
        a.sceneWidget === b.sceneWidget &&
            a.widget === b.widget &&
            a.rect.origin.x == b.rect.origin.x &&
            a.rect.origin.y == b.rect.origin.y &&
            a.rect.size.width == b.rect.size.width &&
            a.rect.size.height == b.rect.size.height &&
            a.alignment == b.alignment &&
            a.sized == b.sized &&
            a.inScene == b.inScene &&
            a.enabled == b.enabled
    }
}
