package com.moblin.android.view.settings.deeplinkcreator

import androidx.compose.runtime.Composable
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.settings.DeepLinkCreatorWebBrowser
import com.moblin.android.view.utils.TextEditNavigationView

private fun submitHome(webBrowser: DeepLinkCreatorWebBrowser, value: String) {
    webBrowser.home = value
}

@Composable
fun DeepLinkCreatorWebBrowserSettingsView(webBrowser: DeepLinkCreatorWebBrowser) {
    Form(title = localized("Web browser")) {
        Section {
            TextEditNavigationView(
                title = localized("Home"),
                value = webBrowser.home,
                onSubmit = { value ->
                    submitHome(webBrowser, value)
                },
                onChange = { newValue ->
                    submitHome(webBrowser, newValue)
                    null
                }
            )
        }
    }
}
