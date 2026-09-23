package com.moblin.android.view.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.FullScreenCover
import com.moblin.android.platform.swiftui.formPalette

@Composable
fun QrCodeImageView(image: ImageBitmap, height: Double) {
    var isFullScreen by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        isFullScreen = true
                    },
                )
            },
    ) {
        HCenter {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    modifier = Modifier
                        .heightIn(max = height.dp)
                        .aspectRatio(image.width.toFloat() / image.height.toFloat()),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None,
                )
                Text(
                    text = localized("Tap the QR code for full screen"),
                    color = formPalette().label,
                    modifier = Modifier.padding(bottom = 7.dp),
                )
            }
        }
    }
    FullScreenCover(isPresented = isFullScreen, onDismissRequest = { isFullScreen = false }) {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .alpha(if (pressed) 0.2f else 1f)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        isFullScreen = false
                    }
                    .background(Color.White),
            ) {
                HCenter {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        modifier = Modifier.aspectRatio(image.width.toFloat() / image.height.toFloat()),
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.None,
                    )
                }
            }
        }
    }
}
