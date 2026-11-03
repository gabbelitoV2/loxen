package com.moblin.android.various.model

import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.various.settings.*
import com.moblin.android.view.controlbar.quickbutton.sceneeditor.sceneEditorAlignedLayout
import com.moblin.android.view.controlbar.quickbutton.sceneeditor.sceneEditorLayout
import java.util.UUID

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

fun Model.sceneEditorMoveWidget(sceneWidget: SettingsSceneWidget, rect: CGRect) {
    sceneWidget.layout = sceneEditorLayout(
        sceneWidget.layout,
        rect,
        sceneEditorWidgetOutlines.canvasSize.value,
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
        sceneEditorWidgetOutlines.canvasSize.value,
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
    val shownSceneWidgetIds = sceneEditorWidgetOutlines.outlines.value
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
