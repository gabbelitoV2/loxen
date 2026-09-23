package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.view.settings.Section
import com.moblin.android.view.settings.formPalette

@Composable
fun InfoBannerView(text: String, modifier: Modifier = Modifier) {
    val palette = formPalette()
    Section {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SystemImage(name = "info.circle.fill", fontSize = 22.sp, tint = palette.accent)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = localized(text))
        }
    }
}
