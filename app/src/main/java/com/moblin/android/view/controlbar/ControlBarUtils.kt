package com.moblin.android.view.controlbar

import com.moblin.android.various.model.Model

fun controlBarScrollTargetBehavior(model: Model, containerWidth: Double, targetPosition: Double): Double {
    val spacing = 8.0
    val originalPagePosition = (model.quickButtons.page - 1).toDouble() * (containerWidth + spacing)
    val distance = targetPosition - originalPagePosition
    if (distance > 15) {
        model.quickButtons.page += 1
    } else if (distance < -15) {
        model.quickButtons.page -= 1
    }
    val pages = model.quickButtons.pairs.count { !it.isEmpty }
    model.quickButtons.page = model.quickButtons.page.coerceIn(1, pages)
    return (model.quickButtons.page - 1).toDouble() * (containerWidth + spacing)
}
