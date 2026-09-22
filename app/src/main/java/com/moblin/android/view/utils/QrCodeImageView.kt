package com.moblin.android.view.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun QrCodeImageView(image: ImageBitmap, height: Double) {
    var isFullScreen by remember { mutableStateOf(false) }
    HCenter {
        Column(
            modifier = Modifier.clickable {
                isFullScreen = true
            },
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
                modifier = Modifier.padding(bottom = 7.dp),
            )
        }
    }
    if (isFullScreen) {
        Dialog(
            onDismissRequest = {
                isFullScreen = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Button(
                onClick = {
                    isFullScreen = false
                },
                modifier = Modifier.background(Color.White),
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
