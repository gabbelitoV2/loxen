package com.moblin.android.view.controlbar

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.common.various.clamped
import com.moblin.android.common.various.controlBarButtonSize
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model

fun controlBarScrollTargetBehavior(model: Model, containerWidth: Double, targetPosition: Double): Double {
    val spacing = 8.0
    val originalPagePosition = (model.quickButtonsPages.page - 1).toDouble() * (containerWidth + spacing)
    val distance = targetPosition - originalPagePosition
    if (distance > 15) {
        model.quickButtonsPages.page += 1
    } else if (distance < -15) {
        model.quickButtonsPages.page -= 1
    }
    val pages = model.quickButtons.pairs.value.count { !it.isEmpty() }
    model.quickButtonsPages.page = model.quickButtonsPages.page.clamped(to = 1..pages)
    return (model.quickButtonsPages.page - 1).toDouble() * (containerWidth + spacing)
}

@Composable
fun SettingsButtonImage() {
    Box(
        modifier = Modifier
            .size(controlBarButtonSize.dp)
            .border(width = 1.dp, color = formPalette().secondaryLabel, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        SystemImage(name = "gearshape", fontSize = 17.sp, tint = Color.White)
    }
}
