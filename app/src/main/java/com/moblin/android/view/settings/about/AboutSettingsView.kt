package com.moblin.android.view.settings.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.appVersion
import com.moblin.android.view.CloseButtonTopRightView
import com.moblin.android.view.utils.ExternalUrlButtonView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsView(
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var presentingVersionHistory by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("About") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                TextItemLocalizedView(name = "Version", value = appVersion())
            }
            item {
                Text(
                    text = "Attributions",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("AboutAttributionsSettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            item {
                TextButtonView(text = "Version history", onClick = { presentingVersionHistory = true })
            }
            item {
                ExternalUrlButtonView(url = "https://moblin.app") {
                    Text("Website")
                }
            }
            item {
                ExternalUrlButtonView(url = "https://eerimoq.github.io/moblin/privacy-policy/en.html") {
                    Text("Privacy policy")
                }
            }
            item {
                ExternalUrlButtonView(url = "https://www.apple.com/legal/internet-services/itunes/dev/stdeula/") {
                    Text("End-user license agreement (EULA)")
                }
            }
        }
    }

    if (presentingVersionHistory) {
        ModalBottomSheet(onDismissRequest = { presentingVersionHistory = false }) {
            Box {
                AboutVersionHistorySettingsView()
                CloseButtonTopRightView(onClick = { presentingVersionHistory = false })
            }
        }
    }
}
