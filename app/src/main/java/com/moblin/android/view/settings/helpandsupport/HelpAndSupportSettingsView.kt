package com.moblin.android.view.settings.helpandsupport

import androidx.compose.runtime.Composable
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.view.settings.streams.stream.DiscordLogoAndNameView
import com.moblin.android.view.settings.streams.stream.GithubLogoAndNameView
import com.moblin.android.view.utils.ExternalUrlButtonView

@Composable
fun HelpAndSupportSettingsView() {
    Form(title = localized("Help and support")) {
        Section(
            footer = localized(
                "Feel free to join Moblin Discord server or write an issue on " +
                    "Github if you need help or want to give feedback.",
            ),
        ) {
            ExternalUrlButtonView(url = "https://discord.moblin.app") {
                DiscordLogoAndNameView()
            }
            ExternalUrlButtonView(url = com.moblin.android.platform.loxen.Loxen.repositoryUrl) {
                GithubLogoAndNameView()
            }
        }
    }
}
