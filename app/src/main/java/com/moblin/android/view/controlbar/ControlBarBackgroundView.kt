package com.moblin.android.view.controlbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.moblin.android.various.model.ControlBar

@Composable
fun ControlBarBackgroundView(controlBar: ControlBar) {
    val backgroundImage by controlBar.backgroundImage.collectAsState()
    val backgroundImageOpacity by controlBar.backgroundImageOpacity.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds(),
    ) {
        backgroundImage?.let { image ->
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(backgroundImageOpacity.toFloat()),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
