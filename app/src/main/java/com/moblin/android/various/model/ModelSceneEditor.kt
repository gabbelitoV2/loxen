package com.moblin.android.various.model

import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.various.settings.*
import java.util.UUID

private fun makeAlignment(left: Boolean?, top: Boolean?): SettingsAlignment {
    return when {
        left == true && top == true -> SettingsAlignment.topLeft
        left == false && top == true -> SettingsAlignment.topRight
        left == true && top == false -> SettingsAlignment.bottomLeft
        left == false && top == false -> SettingsAlignment.bottomRight
        left == null && top == true -> SettingsAlignment.topCenter
        left == null && top == false -> SettingsAlignment.bottomCenter
        left == true && top == null -> SettingsAlignment.leftCenter
        left == false && top == null -> SettingsAlignment.rightCenter
        else -> SettingsAlignment.center
    }
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

fun Model.sceneEditorPresentationChanged() {
    sceneEditor.sceneId.value = null
    media.setShowCameraPreview(updateShowCameraPreview())
    setSceneEditorWidgetOutlinesEnabled(presentingSceneEditor.value)
    updateWidgetOutlines()
}

fun Model.sceneEditorScene(): SettingsScene? {
    val sceneId = sceneEditor.sceneId
    if (sceneId != null) {
        database.scenes.firstOrNull { it.id == sceneId }?.let { return it }
    }
    return getSelectedScene()
}

fun Model.sceneEditorSelectScene(id: UUID) {
    val scene = database.scenes.firstOrNull { it.id == id } ?: return
    if (scene.enabled) {
        sceneEditor.sceneId.value = null
        selectScene(id = id)
    } else {
        sceneEditor.sceneId.value = id
    }
    updateWidgetOutlines()
}

fun Model.sceneEditorMoveWidget(
    sceneWidget: SettingsSceneWidget,
    position: CGPoint,
    size: CGSize,
    centeredHorizontal: Boolean,
    centeredVertical: Boolean,
) {
    val canvasSize = sceneEditorWidgetOutlines.canvasSize.value
    var layout = sceneWidget.layout
    val alignment = layout.alignment
    if (alignment.isHorizontalOrVerticalCenter()) {
        layout = layout.copy(positioningLock = false)
    }
    val left: Boolean? = if (centeredHorizontal) {
        null
    } else if (alignment.isHorizontalCenter()) {
        position.x + size.width / 2 < canvasSize.width / 2
    } else {
        alignment.isLeft()
    }
    val top: Boolean? = if (centeredVertical) {
        null
    } else if (alignment.isVerticalCenter()) {
        position.y + size.height / 2 < canvasSize.height / 2
    } else {
        alignment.isTop()
    }
    layout = layout.copy(alignment = makeAlignment(left, top))
    if (left != null) {
        layout = layout.copy(
            x = layoutOffsetFromPosition(position.x, size.width, canvasSize.width, left),
        ).updatingXString()
    }
    if (top != null) {
        layout = layout.copy(
            y = layoutOffsetFromPosition(position.y, size.height, canvasSize.height, top),
        ).updatingYString()
    }
    sceneWidget.layout = layout
    sceneUpdatedThrottled()
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

fun Model.sceneEditorMoveWidgetInDrawOrder(
    scene: SettingsScene,
    sceneWidget: SettingsSceneWidget,
    up: Boolean,
) {
    val index = scene.widgets.indexOfFirst { it.id == sceneWidget.id }
    if (index == -1) {
        return
    }
    val newIndex = if (up) index + 1 else index - 1
    if (newIndex !in scene.widgets.indices) {
        return
    }
    val movedWidget = scene.widgets[index]
    scene.widgets[index] = scene.widgets[newIndex]
    scene.widgets[newIndex] = movedWidget
    sceneUpdated()
    sceneSettingsPanelSceneId.value += 1
}
