package com.moblin.android.various.model

fun Model.updateDisconnectProtectionVideoSourceConnected() {
    val fallbackSceneId = database.disconnectProtection.fallbackSceneId ?: return
    if (sceneSelector.selectedSceneId != fallbackSceneId) {
        return
    }
    val liveSceneId = database.disconnectProtection.liveSceneId ?: return
    if (!isSceneVideoSourceActive(sceneId = liveSceneId)) {
        return
    }
    selectScene(id = liveSceneId)
}

fun Model.updateDisconnectProtectionVideoSourceDisconnected() {
    if (isSceneVideoSourceActive(sceneId = sceneSelector.selectedSceneId)) {
        return
    }
    val liveSceneId = database.disconnectProtection.liveSceneId ?: return
    if (sceneSelector.selectedSceneId != liveSceneId) {
        return
    }
    val fallbackSceneId = database.disconnectProtection.fallbackSceneId ?: return
    selectScene(id = fallbackSceneId)
}
