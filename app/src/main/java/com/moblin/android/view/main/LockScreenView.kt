package com.moblin.android.view.main

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.moblin.android.various.model.Model
import com.moblin.android.LocalModel

@Composable
fun LockScreenView(model: Model = LocalModel.current) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.01f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        model.toggleLockScreen()
                    },
                )
            },
    ) {
        Text("")
    }
}
