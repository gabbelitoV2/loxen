package com.moblin.android.view.settings.about

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.appVersion
import com.moblin.android.localized
import com.moblin.android.view.CloseButtonTopRightView
import com.moblin.android.view.settings.Form
import com.moblin.android.view.settings.NavigationLink
import com.moblin.android.view.settings.Section
import com.moblin.android.view.settings.Sheet
import com.moblin.android.view.utils.ExternalUrlButtonView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextItemLocalizedView

@Composable
fun AboutSettingsView(
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var presentingVersionHistory by remember { mutableStateOf(false) }
    Form(title = "About") {
        Section {
            TextItemLocalizedView(name = "Version", value = appVersion())
            NavigationLink("Attributions") {
                AboutAttributionsSettingsView()
            }
        }
        Section {
            TextButtonView("Version history") {
                presentingVersionHistory = true
            }
        }
        ExternalUrlButtonView(url = "https://moblin.app") {
            Text(text = localized("Website"))
        }
        ExternalUrlButtonView(url = "https://eerimoq.github.io/moblin/privacy-policy/en.html") {
            Text(text = localized("Privacy policy"))
        }
        ExternalUrlButtonView(url = "https://www.apple.com/legal/internet-services/itunes/dev/stdeula/") {
            Text(text = localized("End-user license agreement (EULA)"))
        }
    }
    if (presentingVersionHistory) {
        Sheet(onDismissRequest = { presentingVersionHistory = false }) {
            Box {
                AboutVersionHistorySettingsView()
                CloseButtonTopRightView(onClose = { presentingVersionHistory = false })
            }
        }
    }
}
