package com.moblin.android.view.settings.helpandsupport

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.view.settings.streams.stream.DiscordLogoAndNameView
import com.moblin.android.view.settings.streams.stream.GithubLogoAndNameView
import com.moblin.android.view.utils.ExternalUrlButtonView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpAndSupportSettingsView() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Help and support")) })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                ExternalUrlButtonView(url = "https://discord.moblin.app") {
                    DiscordLogoAndNameView()
                }
            }
            item {
                ExternalUrlButtonView(url = "https://github.com/eerimoq/moblin") {
                    GithubLogoAndNameView()
                }
            }
            item {
                Text(
                    localized(
                        "Feel free to join Moblin Discord server or write an issue on " +
                            "Github if you need help or want to give feedback.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
