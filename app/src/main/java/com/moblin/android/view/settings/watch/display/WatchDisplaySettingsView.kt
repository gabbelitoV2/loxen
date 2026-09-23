package com.moblin.android.view.settings.watch.display

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.LocalOnNavigate
import com.moblin.android.moblinwatch.shared.WatchSettingsShow
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.view.settings.watch.display.localoverlays.WatchLocalOverlaysSettingsView

private const val WatchLocalOverlaysSettingsRoute = "WatchLocalOverlaysSettingsView"

@Composable
fun WatchDisplaySettingsView(
    show: WatchSettingsShow,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = "Display") {
        Section {
            NavigationLink(destination = { WatchLocalOverlaysSettingsView(show = show) }) {
                Text("Local overlays")
            }
        }
    }
}
