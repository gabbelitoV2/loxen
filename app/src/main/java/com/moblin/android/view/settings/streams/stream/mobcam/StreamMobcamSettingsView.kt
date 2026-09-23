package com.moblin.android.view.settings.streams.stream.mobcam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.utils.TextItemLocalizedView

@Composable
fun StreamMobcamSettingsView(stream: SettingsStream) {
    Form(title = "Mobcam") {
        Section(
            footerContent = {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Connect this device to a computer with a USB cable and run the Moblin Mobcam host " +
                            "tool on the computer to receive the stream. The computer connects to the port " +
                            "above over the cable.",
                    )
                    Text("")
                    Text("Change the port by editing the URL.")
                }
            },
        ) {
            TextItemLocalizedView(name = "Port", value = stream.mobcamPort().toString())
        }
    }
}
