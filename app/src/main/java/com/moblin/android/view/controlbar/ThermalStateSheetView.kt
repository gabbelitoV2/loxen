package com.moblin.android.view.controlbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.view.utils.CloseToolbar

@Composable
private fun FlameStateView(
    color: Color,
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        SystemImage(
            name = "flame",
            fontSize = 17.sp,
            tint = color,
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(Color.Black)
                .padding(4.dp),
        )
        Text(text)
    }
}

@Composable
private fun BulletView(
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        Text("• ")
        Text(text)
    }
}

@Composable
fun ThermalStateSheetView(
    presenting: Boolean,
    onPresentingChange: (Boolean) -> Unit,
) {
    Form(
        title = "Thermal state",
        toolbar = {
            CloseToolbar(
                presenting = presenting,
                onPresentingChange = onPresentingChange,
            )
        },
    ) {
        Section {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                FlameStateView(
                    color = Color.White,
                    text = localized("Your device is cold and should function normally."),
                )
                FlameStateView(
                    color = Color(0xFFFFCC00),
                    text = localized("Your device is warm, but should function normally."),
                )
                FlameStateView(
                    color = formPalette().red,
                    text = localized("Your device is hot and may overheat."),
                )
            }
        }
        Section(header = "Mitigating overheating") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                BulletView(text = localized("Use single lens or low energy cameras"))
                BulletView(text = localized("Lower FPS"))
                BulletView(text = localized("Lower resolution"))
                BulletView(text = localized("Lower bitrate"))
                BulletView(text = localized("No widgets, LUTs or other image effects"))
                BulletView(text = localized("No direct sunlight"))
                BulletView(text = localized("More air flow"))
                BulletView(text = localized("Keep the battery fully charged"))
                BulletView(text = localized("No wireless charging or fast charging"))
                BulletView(text = localized("Use a phone cooler"))
                BulletView(text = localized("Turn off cellular"))
                BulletView(text = localized("Turn off busy chats"))
                BulletView(text = localized("And a lot more..."))
            }
        }
    }
}
