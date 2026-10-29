package com.moblin.android.various.model

import android.graphics.BitmapFactory
import android.media.ExifInterface
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coregraphics.toCGSize
import com.moblin.android.various.settings.*
import com.moblin.android.videoeffects.layoutPosition
import com.moblin.android.videoeffects.layoutScale
import com.moblin.android.videoeffects.toPixels
import java.util.UUID

fun Model.showWidgetOutlines() {
    if (showingPanel.value != ShowingPanel.settings) {
        return
    }
    setWidgetOutlinesEnabled(true)
}

fun Model.hideWidgetOutlines() {
    setWidgetOutlinesEnabled(false)
}

fun Model.updateWidgetOutlinesForShowingPanel() {
    setWidgetOutlinesEnabled(
        listOf(ShowingPanel.sceneWidgets, ShowingPanel.sceneSettings).contains(showingPanel.value),
    )
}

private fun Model.setWidgetOutlinesEnabled(enabled: Boolean) {
    if (enabled == widgetOutlines.enabled.value) {
        return
    }
    widgetOutlines.enabled.value = enabled
    widgetOutlines.imageSizes.clear()
    if (enabled) {
        updateWidgetOutlines()
    } else {
        widgetOutlines.outlines.value = emptyList()
    }
}

fun Model.updateWidgetOutlines() {
    if (!widgetOutlines.enabled.value) {
        return
    }
    val scene = getSelectedScene()
    if (scene == null) {
        widgetOutlines.outlines.value = emptyList()
        return
    }
    val canvasSize = media.getCanvasSize().toCGSize()
    if (canvasSize.width != widgetOutlines.canvasSize.value.width ||
        canvasSize.height != widgetOutlines.canvasSize.value.height
    ) {
        widgetOutlines.canvasSize.value = canvasSize
    }
    val outlines = mutableListOf<WidgetOutline>()
    for (drawnWidget in getDrawnSceneWidgets(scene = scene)) {
        val widget = drawnWidget.widget
        if (!widget.hasPosition()) {
            continue
        }
        val layout = drawnWidget.sceneWidget.layout
        val size = widgetOutlineSize(widget = widget, layout = layout, canvasSize = canvasSize)
        val outlineSize = size ?: CGSize.zero
        outlines.add(
            WidgetOutline(
                sceneWidget = drawnWidget.sceneWidget,
                widget = widget,
                rect = CGRect(
                    origin = layoutPosition(layout = layout, size = outlineSize, streamSize = canvasSize),
                    size = outlineSize,
                ),
                sized = size != null,
                inScene = drawnWidget.scene === scene,
            ),
        )
    }
    if (!widgetOutlinesEqual(outlines, widgetOutlines.outlines.value)) {
        widgetOutlines.outlines.value = outlines
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
                (fitInLayout(imageSize, layout, canvasSize) ?: box)
            }
        }
        SettingsWidgetType.chatEmoteCombo -> CGSize(width = 2 * side, height = side)
        SettingsWidgetType.pomodoroTimer -> CGSize(
            width = widget.pomodoroTimer.width.toDouble() * side,
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
    widgetOutlines.imageSizes.remove(id)
    updateWidgetOutlines()
}

private fun Model.widgetImageSize(widget: SettingsWidget): CGSize? {
    val cached = widgetOutlines.imageSizes[widget.id]
    if (cached != null) {
        return cached
    }
    val size = readImageSize(url = imageStorage.makePath(id = widget.id).path)
    if (size != null) {
        widgetOutlines.imageSizes[widget.id] = size
    }
    return size
}

@Suppress("DEPRECATION")
private fun readImageSize(url: String): CGSize? {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(url, options)
    val width = options.outWidth
    val height = options.outHeight
    if (width <= 0 || height <= 0) {
        return null
    }
    val orientation = runCatching {
        ExifInterface(url).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    return if (orientation >= 5) {
        CGSize(width = height, height = width)
    } else {
        CGSize(width = width, height = height)
    }
}

private fun widgetOutlinesEqual(first: List<WidgetOutline>, second: List<WidgetOutline>): Boolean {
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
            a.sized == b.sized &&
            a.inScene == b.inScene
    }
}
