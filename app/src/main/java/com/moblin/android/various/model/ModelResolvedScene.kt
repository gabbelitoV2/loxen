package com.moblin.android.various.model

import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetType
import java.util.UUID

data class ResolvedSceneWidget(
    val scene: SettingsScene,
    val sceneWidget: SettingsSceneWidget,
    val widget: SettingsWidget,
    val parentsEnabled: Boolean,
    val first: Boolean,
    val remote: Boolean,
) {
    val id: UUID
        get() = sceneWidget.id

    val enabled: Boolean
        get() = parentsEnabled && widget.enabled

    val drawn: Boolean
        get() = first && enabled
}

data class ResolvedScene(
    val widgets: MutableList<ResolvedSceneWidget> = mutableListOf(),
    val sceneIds: MutableSet<UUID> = mutableSetOf(),
) {
    fun firstWidgets(): List<ResolvedSceneWidget> {
        return widgets.filter { it.first }
    }

    fun drawnWidgets(): List<ResolvedSceneWidget> {
        return widgets.filter { it.drawn }
    }

    fun localDrawnWidgets(): List<ResolvedSceneWidget> {
        return widgets.filter { it.drawn && !it.remote }
    }

    fun duplicatedWidgetCounts(): Map<UUID, Int> {
        val counts = mutableMapOf<UUID, Int>()
        for (widget in widgets) {
            if (widget.first) {
                continue
            }
            counts[widget.widget.id] = (counts[widget.widget.id] ?: 0) + 1
        }
        return counts
    }
}

internal sealed class SceneWidgetsChange {
    class Add(val sceneId: UUID, val widgetId: UUID) : SceneWidgetsChange()

    class SetScene(val widgetId: UUID, val sceneId: UUID) : SceneWidgetsChange()
}

private data class SceneResolveKey(
    val sceneId: UUID,
    val includeRemote: Boolean,
    val includeDisabledSceneWidgets: Boolean,
)

class SceneResolver(
    private val scenes: List<SettingsScene>,
    private val widgets: List<SettingsWidget>,
    private val remoteWidgetId: UUID? = null,
) {
    private val scenesById: Map<UUID, SettingsScene> = firstById(scenes) { it.id }
    private val widgetsById: Map<UUID, SettingsWidget> = firstById(widgets) { it.id }
    private val resolved: MutableMap<SceneResolveKey, ResolvedScene> = mutableMapOf()

    fun resolve(
        scene: SettingsScene,
        includeRemote: Boolean = false,
        includeDisabledSceneWidgets: Boolean = false,
    ): ResolvedScene {
        val key = SceneResolveKey(scene.id, includeRemote, includeDisabledSceneWidgets)
        resolved[key]?.let { return it }
        val result = resolve(
            scene,
            if (includeRemote) remoteWidgetId else null,
            includeDisabledSceneWidgets,
            null,
        )
        resolved[key] = result
        return result
    }

    fun addableWidgets(scene: SettingsScene): List<SettingsWidget> {
        val before = resolveAllScenes()
        return widgets.filter { widget ->
            !introducesDuplicatedWidgets(SceneWidgetsChange.Add(scene.id, widget.id), before)
        }
    }

    fun selectableScenes(widget: SettingsWidget): List<SettingsScene> {
        val before = resolveAllScenes()
        return scenes.filter { scene ->
            !introducesDuplicatedWidgets(SceneWidgetsChange.SetScene(widget.id, scene.id), before)
        }
    }

    private fun resolveAllScenes(): List<ResolvedScene> {
        return scenes.map { resolve(it, null, true, null) }
    }

    private fun introducesDuplicatedWidgets(
        change: SceneWidgetsChange,
        before: List<ResolvedScene>,
    ): Boolean {
        return scenes.zip(before).any { (scene, beforeScene) ->
            val relevant = when (change) {
                is SceneWidgetsChange.Add -> beforeScene.sceneIds.contains(change.sceneId)
                is SceneWidgetsChange.SetScene ->
                    beforeScene.widgets.any { it.widget.id == change.widgetId }
            }
            if (!relevant) {
                return@any false
            }
            val beforeCounts = beforeScene.duplicatedWidgetCounts()
            resolve(scene, null, true, change).duplicatedWidgetCounts().any { (widgetId, count) ->
                count > (beforeCounts[widgetId] ?: 0)
            }
        }
    }

    private fun resolve(
        scene: SettingsScene,
        remoteWidgetId: UUID?,
        includeDisabledSceneWidgets: Boolean,
        change: SceneWidgetsChange?,
    ): ResolvedScene {
        val walk = SceneWalk(includeDisabledSceneWidgets, change)
        walk.resolved.sceneIds.add(scene.id)
        walk.visit(scene, sceneWidgets(scene, change), true, false, this)
        if (remoteWidgetId != null) {
            walk.visit(scene, listOf(SettingsSceneWidget(widgetId = remoteWidgetId)), true, true, this)
        }
        return walk.resolved
    }

    internal fun findScene(id: UUID): SettingsScene? {
        return scenesById[id]
    }

    internal fun findWidget(id: UUID): SettingsWidget? {
        return widgetsById[id]
    }

    internal fun sceneWidgets(scene: SettingsScene, change: SceneWidgetsChange?): List<SettingsSceneWidget> {
        if (change is SceneWidgetsChange.Add && change.sceneId == scene.id) {
            return scene.widgets + SettingsSceneWidget(widgetId = change.widgetId)
        }
        return scene.widgets
    }
}

private data class SceneWalk(
    private val includeDisabledSceneWidgets: Boolean,
    private val change: SceneWidgetsChange?,
) {
    var resolved: ResolvedScene = ResolvedScene()
    var widgetIds: MutableSet<UUID> = mutableSetOf()

    fun visit(
        scene: SettingsScene,
        sceneWidgets: List<SettingsSceneWidget>,
        parentsEnabled: Boolean,
        remote: Boolean,
        resolver: SceneResolver,
    ) {
        for (sceneWidget in sceneWidgets) {
            val widget = resolver.findWidget(sceneWidget.widgetId) ?: continue
            resolved.widgets.add(
                ResolvedSceneWidget(
                    scene = scene,
                    sceneWidget = sceneWidget,
                    widget = widget,
                    parentsEnabled = parentsEnabled,
                    first = widgetIds.add(widget.id),
                    remote = remote,
                )
            )
            if (widget.type != SettingsWidgetType.scene || !(widget.enabled || includeDisabledSceneWidgets)) {
                continue
            }
            var nestedSceneId: UUID? = widget.scene.sceneId
            if (change is SceneWidgetsChange.SetScene && change.widgetId == widget.id) {
                nestedSceneId = change.sceneId
            }
            val resolvedNestedSceneId = nestedSceneId ?: continue
            val nestedScene = resolver.findScene(resolvedNestedSceneId) ?: continue
            if (!resolved.sceneIds.add(nestedScene.id)) {
                continue
            }
            visit(
                nestedScene,
                resolver.sceneWidgets(nestedScene, change),
                parentsEnabled && widget.enabled,
                remote,
                resolver,
            )
        }
    }
}

private fun <T> firstById(items: List<T>, id: (T) -> UUID): Map<UUID, T> {
    val map = mutableMapOf<UUID, T>()
    for (item in items) {
        map.getOrPut(id(item)) { item }
    }
    return map
}

fun Model.makeSceneResolver(): SceneResolver {
    return SceneResolver(
        scenes = database.scenes + remoteSceneScenes,
        widgets = database.widgets + remoteSceneWidgets,
        remoteWidgetId = remoteSceneWidgets.firstOrNull()?.id,
    )
}

fun Model.resolveScene(
    scene: SettingsScene,
    includeRemote: Boolean = false,
    includeDisabledSceneWidgets: Boolean = false,
): ResolvedScene {
    return makeSceneResolver().resolve(
        scene,
        includeRemote = includeRemote,
        includeDisabledSceneWidgets = includeDisabledSceneWidgets,
    )
}

fun Model.drawnWidgetsInSelectedScene(): List<SettingsWidget> {
    val scene = getSelectedScene() ?: return emptyList()
    return resolveScene(scene).drawnWidgets().map { it.widget }
}

fun Model.addableWidgets(scene: SettingsScene): List<SettingsWidget> {
    return SceneResolver(scenes = database.scenes, widgets = database.widgets).addableWidgets(scene = scene)
}

fun Model.selectableScenes(widget: SettingsWidget): List<SettingsScene> {
    return SceneResolver(scenes = database.scenes, widgets = database.widgets).selectableScenes(widget = widget)
}
