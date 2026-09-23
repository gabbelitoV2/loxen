package com.moblin.android.view.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
                    modifier = Modifier.heightIn(max = height.dp),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None,
                )
                Text(
                    text = "Tap the QR code for full screen",
                    fontSize = 17.sp,
                    color = formPalette().label,
                    modifier = Modifier.padding(bottom = 7.dp),
                )
            }
        }
    }
    if (isFullScreen) {
        Dialog(
            onDismissRequest = {
                isFullScreen = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                isFullScreen = false
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                HCenter {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.None,
                    )
                }
            }
        }
    }
}
