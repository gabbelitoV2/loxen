package com.moblin.android.platform.swiftui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.moblin.android.platform.Bundle

@Composable
fun rememberAssetImage(name: String): ImageBitmap? {
    return remember(name) { Bundle.image(name)?.asImageBitmap() }
}

@Composable
fun AssetImage(
    name: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
) {
    val bitmap = rememberAssetImage(name) ?: return
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}
