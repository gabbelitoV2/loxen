package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.iconWidth
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage

@Composable
fun IconAndTextView(image: String, text: String, longDivider: Boolean = false) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (longDivider) {
            Text("")
        }
        SystemImage(
            name = image,
            fontSize = LocalTextStyle.current.fontSize,
            modifier = Modifier.width(iconWidth.dp),
            tint = LocalContentColor.current,
        )
        Text(text)
    }
}

@Composable
fun IconAndTextLocalizedView(image: String, text: String, longDivider: Boolean = false) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (longDivider) {
            Text("")
        }
        SystemImage(
            name = image,
            fontSize = LocalTextStyle.current.fontSize,
            modifier = Modifier.width(iconWidth.dp),
            tint = LocalContentColor.current,
        )
        Text(localized(text))
    }
}

@Composable
fun IconAndTextSettingView(image: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("")
        Row(
            modifier = Modifier.width(25.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            SystemImage(name = image, fontSize = LocalTextStyle.current.fontSize, tint = LocalContentColor.current)
            Spacer(Modifier.weight(1f))
        }
        Text(localized(text), modifier = Modifier.padding(start = 3.dp))
    }
}
