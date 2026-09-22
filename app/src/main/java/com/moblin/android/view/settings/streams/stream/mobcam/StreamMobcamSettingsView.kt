package com.moblin.android.view.settings.streams.stream.mobcam

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.utils.TextItemLocalizedView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamMobcamSettingsView(stream: SettingsStream) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Mobcam")
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                TextItemLocalizedView(name = "Port", value = stream.mobcamPort().toString())
            }
            item {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        "Connect this device to a computer with a USB cable and run the Moblin Mobcam host " +
                            "tool on the computer to receive the stream. The computer connects to the port " +
                            "above over the cable.",
                    )
                    Text("")
                    Text("Change the port by editing the URL.")
                }
            }
        }
    }
}
